package com.modus.camel.datasonnet.test;

import com.modus.camel.datasonnet.DatasonnetRouteBuilder;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.ValueBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.spring.javaconfig.CamelConfiguration;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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
    protected ProducerTemplate expressionsInJavaProducer;

    @Produce("direct:chainExpressions")
    protected ProducerTemplate chainExpressionsProducer;

    @Configuration
    public static class TestConfig extends CamelConfiguration {
        @Bean
        public List<RouteBuilder> routes() {
            return Arrays.asList(
                new DatasonnetRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        from("direct:chainExpressions")
                                .setHeader("ScriptHeader", constant("{ hello: \"World\"}"))
                                .setBody(datasonnet(simple("${header.ScriptHeader}")))
                                .to("mock:direct:response");
                    }
                },
                new DatasonnetRouteBuilder() {
                    @Override
                    public void configure() throws Exception {
                        from("direct:expressionsInJava")
                                .choice()
                                .when(datasonnet("payload == 'World'", "text/plain", "application/json"))
                                .setBody(datasonnet("'Hello, ' + payload", "text/plain", "text/plain"))
                                .otherwise()
                                .setBody(datasonnet("'Good bye, ' + payload", "text/plain", "text/plain"))
                                .end()
                                .to("mock:direct:response");
                    }
                }
            );
        }
    }

    @Test
    public void testExpressionLanguageInJava() throws Exception {
        endEndpoint.expectedMessageCount(1);
        expressionsInJavaProducer.sendBody("World");
        Exchange exchange = endEndpoint.assertExchangeReceived(endEndpoint.getReceivedCounter() - 1);
        String response = exchange.getIn().getBody().toString();
        assertEquals("Hello, World", response);
    }

    @Test
    public void testChainExpressions() throws Exception {
        endEndpoint.expectedMessageCount(1);
        chainExpressionsProducer.sendBody("{}");
        Exchange exchange = endEndpoint.assertExchangeReceived(endEndpoint.getReceivedCounter() - 1);
        String response = exchange.getIn().getBody().toString();
        JSONAssert.assertEquals("{\"hello\":\"World\"}", response, true);
    }
}
