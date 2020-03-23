package com.modus.camel.datasonnet;

import com.datasonnet.Mapper;
import com.datasonnet.document.Document;
import com.datasonnet.document.JavaObjectDocument;
import com.datasonnet.document.StringDocument;
import com.datasonnet.spi.DataFormatPlugin;
import com.datasonnet.spi.DataFormatService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.Resource;
import io.github.classgraph.ResourceList;
import io.github.classgraph.ScanResult;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class DatasonnetProcessor implements Processor {

    private List<String> librariesPath;

    private Map<String, String> namedImports = new HashMap<>();
    private List<String> supportedMimeTypes = new ArrayList<>(Arrays.asList(new String[] { "application/json" }));

    private static Logger logger = LoggerFactory.getLogger(DatasonnetProcessor.class);

    private String inputMimeType;
    private String outputMimeType;

    private String datasonnetFile;
    private String datasonnetScript;

    public void process(Exchange exchange) throws Exception {

        //Try to auto-detect input mime type
        String overriddenInputMimeType = (String) exchange.getIn().getHeader(Exchange.CONTENT_TYPE, (String) exchange.getIn().getHeader("mimeType", "UNKNOWN_MIME_TYPE"));
        if (!"UNKNOWN_MIME_TYPE".equalsIgnoreCase(overriddenInputMimeType) && overriddenInputMimeType != null) {
            inputMimeType = overriddenInputMimeType;
        }
        if (!supportedMimeTypes.contains(inputMimeType)) {
            logger.warn("Input Mime Type " + inputMimeType + " is not supported or suitable plugin not found, using application/json");
            inputMimeType = "application/json";
        }
        logger.debug("Input mime type is: " + inputMimeType);

        if (!supportedMimeTypes.contains(outputMimeType)) {
            logger.warn("Output Mime Type " + outputMimeType + " is not supported or suitable plugin not found, using application/json");
            outputMimeType = "application/json";
        }
        logger.debug("Output mime type is: " + outputMimeType);

        String mapping = "{}";

        if (getDatasonnetFile() != null) {
            //TODO - support URLs like 'file://' and/or 'classpath:'
            InputStream mappingStream = getClass().getClassLoader().getResourceAsStream(getDatasonnetFile());
            mapping = IOUtils.toString(mappingStream);
        } else if (getDatasonnetScript() != null) {
            mapping = getDatasonnetScript();
        } else {
            throw new IllegalArgumentException("Either datasonnetFile or datasonnetScript property must be set!");
        }

        ObjectMapper jacksonMapper = new ObjectMapper();
        Map<String, Document> jsonnetVars = new HashMap<>();

        for (String varName : exchange.getProperties().keySet()) {
            Object varValue = exchange.getProperty(varName);

            if (varValue instanceof Serializable) {
                String varValueStr = varValue.toString();
                try {
                    JsonNode jsonNode = jacksonMapper.readTree(varValueStr);
                    //This is valid JSON
                } catch (Exception e) {
                    //Not a valid JSON, convert
                    varValueStr = jacksonMapper.writeValueAsString(varValueStr);
                }
                //TODO - how do we support Java, XML and CSV properties?
                jsonnetVars.put(varName, new StringDocument(varValueStr, "application/json"));

            } else {
                logger.warn("Exchange property {} is not serializable, skipping.", varName);
            }
        }

        //TODO - is there a better way to handle this?
        String headersJson = jacksonMapper.writeValueAsString(exchange.getMessage().getHeaders());
        jsonnetVars.put("headers", new StringDocument(headersJson, "application/json"));

        //TODO we need a better solution going forward but for now we just differentiate between Java and text-based formats
        Document payload = inputMimeType.contains("java") ?
                createDocument(exchange.getMessage().getBody(), inputMimeType) :
                createDocument(exchange.getMessage().getBody(java.lang.String.class), inputMimeType);

        Mapper mapper = new Mapper(mapping, jsonnetVars.keySet(), namedImports, true, true);

        logger.debug("Variables are: " + jsonnetVars);
        logger.debug("Output mime type is: " + outputMimeType);
        logger.debug("Document is: " + (payload.canGetContentsAs(String.class) ? payload.getContentsAsString() : payload.getContentsAsObject()));

        Document mappedDoc = mapper.transform(payload, jsonnetVars, getOutputMimeType());
        Object mappedBody = mappedDoc.canGetContentsAs(String.class) ? mappedDoc.getContentsAsString() : mappedDoc.getContentsAsObject();

        exchange.getIn().setBody(mappedBody);
    }

    public void init() {
        logger.debug("Initializing mapping bean...");

        DataFormatService dataFormatService = new DataFormatService();
        List<DataFormatPlugin> pluginsList = dataFormatService.findPlugins();
        for (DataFormatPlugin plugin : pluginsList) {
            supportedMimeTypes.addAll(Arrays.asList(plugin.getSupportedIdentifiers()));
        }

        if (getLibrariesPath() == null) {
            logger.debug("Explicit library path is not set, searching in the classpath...");
            try (ScanResult scanResult = new ClassGraph().whitelistPaths("/").scan()) {
                scanResult.getResourcesWithExtension("libsonnet")
                        .forEachByteArray(new ResourceList.ByteArrayConsumer() {
                            @Override
                            public void accept(Resource resource, byte[] bytes) {
                                logger.debug("Loading DataSonnet library: " + resource.getPath());
                                namedImports.put(
                                        resource.getPath(), new String(bytes, StandardCharsets.UTF_8));
                            }
                        });
            }
        } else {
            logger.debug("Explicit library path is " + getLibrariesPath());

            for (String nextPath : getLibrariesPath()) {
                final File nextLibDir = new File(nextPath);
                if (nextLibDir.isDirectory()) {
                    try {
                        Files.walkFileTree(nextLibDir.toPath(), new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                                File f = file.toFile();
                                if (!f.isDirectory() && f.getName().toLowerCase().endsWith(".libsonnet")) {
                                    String content = IOUtils.toString(file.toUri());
                                    Path relative = nextLibDir.toPath().relativize(file);
                                    logger.debug("Loading DataSonnet library: " + relative);
                                    namedImports.put(relative.toString(), content);
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        logger.error("Unable to load libraries from " + nextPath, e);
                    }
                }
            }
        }
    }

    public List<String> getLibrariesPath() {
        return librariesPath;
    }

    public void setLibrariesPath(List<String> librariesPath) {
        this.librariesPath = librariesPath;
    }

    public String getInputMimeType() {
        return inputMimeType;
    }

    public void setInputMimeType(String inputMimeType) {
        this.inputMimeType = inputMimeType;
    }

    public String getOutputMimeType() {
        return outputMimeType;
    }

    public void setOutputMimeType(String outputMimeType) {
        this.outputMimeType = outputMimeType;
    }

    public String getDatasonnetFile() {
        return datasonnetFile;
    }

    public void setDatasonnetFile(String datasonnetFile) {
        this.datasonnetFile = datasonnetFile;
    }

    public String getDatasonnetScript() {
        return datasonnetScript;
    }

    public void setDatasonnetScript(String datasonnetScript) {
        this.datasonnetScript = datasonnetScript;
    }

    private Document createDocument(Object content, String type) throws JsonProcessingException {
        ObjectMapper jacksonMapper = new ObjectMapper();

        Document document = null;
        boolean isObject = false;
        String mimeType = type;
        String documentContent = content.toString();

        if (mimeType.contains("/xml")) {
            mimeType = "application/xml";
        } else if (mimeType.contains("/csv")) {
            mimeType = "application/csv";
        } else if (mimeType.contains("/java")) {
            mimeType = "application/java";
            isObject = true;
        } else {
            mimeType = "application/json";
            try {
                JsonNode jsonNode = jacksonMapper.readTree(content.toString());
                //This is valid JSON
            } catch (Exception e) {
                //Not a valid JSON, convert
                documentContent = jacksonMapper.writeValueAsString(content);
            }
        }

        document = isObject ? new JavaObjectDocument(content) : new StringDocument(documentContent, mimeType);

        return document;
    }
}
