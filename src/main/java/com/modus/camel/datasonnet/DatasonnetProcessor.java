package com.modus.camel.datasonnet;

import com.datasonnet.Mapper;
import com.datasonnet.document.Document;
import com.datasonnet.document.JavaObjectDocument;
import com.datasonnet.document.StringDocument;
import com.datasonnet.spi.DataFormatPlugin;
import com.datasonnet.spi.DataFormatService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DatasonnetProcessor implements Processor {

    private List<String> librariesPath;

    private Map<String, String> namedImports = new HashMap<>();
    private List<String> supportedMimeTypes = new ArrayList<>(Arrays.asList("application/json"));

    private static Logger logger = LoggerFactory.getLogger(DatasonnetProcessor.class);

    private String inputMimeType;
    private String outputMimeType;

    private String datasonnetFile;
    private String datasonnetScript;

    ObjectMapper jacksonMapper = new ObjectMapper();

    public void process(Exchange exchange) throws Exception {
        Object mappedBody = processMapping(exchange);
        exchange.getIn().setBody(mappedBody);
    }

    public void init() throws Exception {
        logger.debug("Initializing mapping bean...");

        jacksonMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        jacksonMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        if (getDatasonnetFile() != null) {
            //TODO - support URLs like 'file://' and/or 'classpath:'
            InputStream mappingStream = getClass().getClassLoader().getResourceAsStream(getDatasonnetFile());
            setDatasonnetScript(IOUtils.toString(mappingStream));
        }

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

    public Object processMapping(Exchange exchange) throws Exception {
        if (inputMimeType == null || "".equalsIgnoreCase(inputMimeType.trim())) {
            //Try to auto-detect input mime type if it was not explicitly set
            String overriddenInputMimeType = (String) exchange.getProperty("inputMimeType",
                                                                            (String) exchange.getIn().getHeader(Exchange.CONTENT_TYPE,
                                                                "UNKNOWN_MIME_TYPE"));
            if (!"UNKNOWN_MIME_TYPE".equalsIgnoreCase(overriddenInputMimeType) && overriddenInputMimeType != null) {
                inputMimeType = overriddenInputMimeType;
            }
        }
        if (!supportedMimeTypes.contains(inputMimeType)) {
            logger.warn("Input Mime Type " + inputMimeType + " is not supported or suitable plugin not found, using application/json");
            inputMimeType = "application/json";
        }

        if (outputMimeType == null || "".equalsIgnoreCase(outputMimeType.trim())) {
            //Try to auto-detect output mime type if it was not explicitly set
            String overriddenOutputMimeType = (String) exchange.getProperty("outputMimeType",
                                                                            (String) exchange.getIn().getHeader("outputMimeType",
                                                                            "UNKNOWN_MIME_TYPE"));
            if (!"UNKNOWN_MIME_TYPE".equalsIgnoreCase(overriddenOutputMimeType) && overriddenOutputMimeType != null) {
                outputMimeType = overriddenOutputMimeType;
            }
        }
        if (!supportedMimeTypes.contains(outputMimeType)) {
            logger.warn("Output Mime Type " + outputMimeType + " is not supported or suitable plugin not found, using application/json");
            outputMimeType = "application/json";
        }

        if (getDatasonnetScript() == null) {
            throw new IllegalArgumentException("Either datasonnetFile or datasonnetScript property must be set!");
        }

        Map<String, Document> jsonnetVars = new HashMap<>();

        Document headersDocument = mapToDocument(exchange.getMessage().getHeaders());
        jsonnetVars.put("headers", headersDocument);
        jsonnetVars.put("header", headersDocument);

        Document propertiesDocument = mapToDocument(exchange.getProperties());
        jsonnetVars.put("exchangeProperty", propertiesDocument);

        Object body = (inputMimeType.contains("java") ? exchange.getMessage().getBody() : exchange.getMessage().getBody(java.lang.String.class));

        logger.debug("Input MIME type is " + inputMimeType);
        logger.debug("Output MIME type is: " + outputMimeType);
        logger.debug("Message Body is " + body);
        logger.debug("Variables are: " + jsonnetVars);

        //TODO we need a better solution going forward but for now we just differentiate between Java and text-based formats
        Document payload = createDocument(body, inputMimeType);
        jsonnetVars.put("body", payload);

        logger.debug("Document is: " + (payload.canGetContentsAs(String.class) ? payload.getContentsAsString() : payload.getContentsAsObject()));

        Mapper mapper = new Mapper(getDatasonnetScript(), jsonnetVars.keySet(), namedImports, true, true);
        Document mappedDoc = mapper.transform(payload, jsonnetVars, getOutputMimeType());
        Object mappedBody = mappedDoc.canGetContentsAs(String.class) ? mappedDoc.getContentsAsString() : mappedDoc.getContentsAsObject();

        return mappedBody;
    }

    private Document createDocument(Object content, String type) throws JsonProcessingException {
        Document document = null;
        boolean isObject = false;
        String mimeType = type;
        String documentContent = (content == null ? "" : content.toString());

        logger.debug("Before create Document Content is: " + documentContent);
        logger.debug("Before create mimeType is: " + mimeType);

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
                if (documentContent == null || "".equalsIgnoreCase(documentContent.toString())) {
                    documentContent = "{}";
                }
                jacksonMapper.readTree(documentContent);
                logger.debug("Content is valid JSON");
                //This is valid JSON
            } catch (Exception e) {
                //Not a valid JSON, convert
                logger.debug("Content is not valid JSON, converting to JSON string");
                documentContent = jacksonMapper.writeValueAsString(content);
            }
        }

        logger.debug("Document Content is: " + documentContent);

        document = isObject ? new JavaObjectDocument(content) : new StringDocument(documentContent, mimeType);

        return document;
    }

    private Document mapToDocument(Map<String, Object> map) throws Exception {
        Iterator<Map.Entry<String, Object>> entryIterator = map.entrySet().iterator();
        Map<String, Object> propsMap = new HashMap<>();

        while (entryIterator.hasNext()) {
            Map.Entry<String, Object> entry = entryIterator.next();

            Object entryValue = entry.getValue();
            String entryClassName = (entryValue != null ? entryValue.getClass().getName() : " NULL ");

            if (entryValue != null && entryValue instanceof Serializable) {
                try {
                    jacksonMapper.writeValueAsString(entryValue);
                    propsMap.put(entry.getKey(), entryValue);
                } catch (Exception e) {
                    logger.debug("Header or property " + entry.getKey() + " cannot be serialized as JSON; removing : " + e.getMessage());
                }
            } else {
                logger.debug("Header or property " + entry.getKey() + " is null or not Serializable : " + entryClassName);
            }
        }

        Document document = new JavaObjectDocument(propsMap);
        return document;
    }
}
