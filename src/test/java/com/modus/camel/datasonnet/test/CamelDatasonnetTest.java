package com.modus.camel.datasonnet.test;

import com.modus.camel.datasonnet.test.javatest.Gizmo;
import com.modus.camel.datasonnet.test.javatest.Manufacturer;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.apache.camel.test.spring.junit5.MockEndpoints;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.test.context.ContextConfiguration;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;

@CamelSpringBootTest
@SpringBootApplication
@ContextConfiguration(locations = {"classpath:camel-context.xml"})
@MockEndpoints("direct:end")
public class CamelDatasonnetTest {
    @Autowired
    private ProducerTemplate template;

    @EndpointInject("mock:direct:end")
    private MockEndpoint mock;

    @Test
    public void testTransform() throws Exception {
        runCamelTest(loadResourceAsString("simpleMapping_payload.json"),
                loadResourceAsString("simpleMapping_result.json"),
                "direct:basicTransform");
    }

    @Test
    public void testTransformXML() throws Exception {
        runCamelTest(loadResourceAsString("payload.xml"),
                loadResourceAsString("readXMLExtTest.json"),
                "direct:transformXML");
    }

    @Test
    public void testTransformCSV() throws Exception {
        runCamelTest(loadResourceAsString("payload.csv"),
                "{\"account\":\"123\"}",
                "direct:transformCSV");
    }

    @Test
    public void testDatasonnetScript() throws Exception {
        runCamelTest(loadResourceAsString("simpleMapping_payload.json"),
                loadResourceAsString("simpleMapping_result.json"),
                "direct:datasonnetScript");
    }

    @Test
    public void testNamedImports() throws Exception {
        runCamelTest("{}",
                loadResourceAsString("namedImports_result.json"),
                "direct:namedImports");
    }

    @Test
    public void testExpressionLanguage() throws Exception {
        runCamelTest("World",
                "{ \"test\":\"Hello, World\"}",
                "direct:expressionLanguage");
    }

    @Test
    public void testNullInput() throws Exception {
        runCamelTest("",
                "{ \"test\":\"Hello, World\"}",
                "direct:nullInput");
        runCamelTest(null,
                "{ \"test\":\"Hello, World\"}",
                "direct:nullInput");
    }

    @Test
    public void testReadJava() throws Exception {

        Gizmo theGizmo = new Gizmo();
        theGizmo.setName("gizmo");
        theGizmo.setQuantity(123);
        theGizmo.setInStock(true);
        theGizmo.setColors(Arrays.asList("red","white","blue"));

        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setManufacturerName("ACME Corp.");
        manufacturer.setManufacturerCode("ACME123");
        theGizmo.setManufacturer(manufacturer);

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        theGizmo.setDate(df.parse("2020-01-06"));

        runCamelTest(theGizmo,
                loadResourceAsString("javaTest.json"),
                "direct:readJava");
    }

    @Test
    public void testWriteJava() throws Exception {
        Gizmo theGizmo = new Gizmo();
        theGizmo.setName("gizmo");
        theGizmo.setQuantity(123);
        theGizmo.setInStock(true);
        theGizmo.setColors(Arrays.asList("red","white","blue"));

        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setManufacturerName("ACME Corp.");
        manufacturer.setManufacturerCode("ACME123");
        theGizmo.setManufacturer(manufacturer);

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        theGizmo.setDate(df.parse("2020-01-06"));

        String payload = loadResourceAsString("javaTest.json");

        Callable testDS = new Callable<String>() {
            @Override
            public String call() {
                try {
                    template.sendBody("direct:writeJava", payload);
                    Exchange exchange = mock.assertExchangeReceived(mock.getReceivedCounter() - 1);
                    Object response = exchange.getIn().getBody();
                    assertEquals(response, theGizmo);
                    return "OK";
                } catch (Exception e) {
                    return e.getMessage();
                }
            }
        };
        ConcurrentUtil.testConcurrent(testDS, 100);
    }

    private void runCamelTest(Object payload, String expectedJson, String uri) throws Exception {
        Callable testDS = new Callable<String>() {
            @Override
            public String call() {
                try {
                    template.sendBody(uri, payload);
                    Exchange exchange = mock.assertExchangeReceived(mock.getReceivedCounter() - 1);
                    String response = exchange.getIn().getBody().toString();
                    JSONAssert.assertEquals(expectedJson, response, true);
                    return "OK";
                } catch (Exception e) {
                    return e.getMessage();
                }
            }
        };
        ConcurrentUtil.testConcurrent(testDS, 100);
    }

    private String loadResourceAsString(String name) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(name);
        return IOUtils.toString(is);
    }
}
