package com.modus.camel.datasonnet.test;

import com.modus.camel.datasonnet.language.model.DatasonnetExpression;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.ValueBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.javaconfig.SingleRouteCamelConfiguration;
import org.apache.camel.test.spring.CamelSpringDelegatingTestContextLoader;
import org.apache.camel.test.spring.CamelSpringRunner;
import org.apache.camel.test.spring.MockEndpoints;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

@RunWith(CamelSpringRunner.class)
@ContextConfiguration(
        classes = {ExpressionsInJavaTest.TestConfig.class},
        loader = CamelSpringDelegatingTestContextLoader.class
)
@MockEndpoints
public class ExpressionsInJavaTest {
    @EndpointInject("mock:direct:response")
    protected MockEndpoint endEndpoint;

    @Produce("direct:expressionsInJava")
    protected ProducerTemplate testProducer;

    @Configuration
    public static class TestConfig extends SingleRouteCamelConfiguration {
        @Bean
        @Override
        public RouteBuilder route() {
            return new RouteBuilder() {
                public ValueBuilder datasonnet(String value) {
                    DatasonnetExpression exp = new DatasonnetExpression(value);
                    return new ValueBuilder(exp);
                }
                public ValueBuilder datasonnet(String value, String inputMimeType, String outputMimeType) {
                    DatasonnetExpression exp = new DatasonnetExpression(value);
                    exp.setInputMimeType(inputMimeType);
                    exp.setOutputMimeType(outputMimeType);
                    return new ValueBuilder(exp);
                }

                @Override
                public void configure() throws Exception {
                    from("direct:expressionsInJava")
                            //TODO how do I chain two datasonnet expressions here?
                            //.setBody(datasonnet(datasonnet("header.DataSonnetScript")))
                            .to("mock:direct:response");
                }
            };
        }
    }

    @Test
    public void testExpressionLanguageInJava() throws Exception {
        endEndpoint.expectedMessageCount(1);
        testProducer.sendBodyAndHeader("{ \"name\" : \"World\"",
                                        "DataSonnetScript",
                                        "{ greeting: \"Hello, \" + payload.name }");
        Exchange exchange = endEndpoint.assertExchangeReceived(0);
        String response = exchange.getIn().getBody().toString();
        JSONAssert.assertEquals("{\"greeting\":\"Hello, World\"}", response, true);
    }
}
