package com.modus.camel.datasonnet.language;

import com.modus.camel.datasonnet.DatasonnetProcessor;
import org.apache.camel.AfterPropertiesConfigured;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.spi.GeneratedPropertyConfigurer;
import org.apache.camel.support.ExpressionAdapter;
import org.apache.camel.support.ExpressionSupport;
import org.apache.camel.support.component.PropertyConfigurerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasonnetExpression extends ExpressionAdapter implements AfterPropertiesConfigured, GeneratedPropertyConfigurer {
    private final String expression;

    private String inputMimeType;
    private String outputMimeType;

    private DatasonnetProcessor processor;

    private static Logger logger = LoggerFactory.getLogger(DatasonnetExpression.class);

    public DatasonnetExpression(String expression) {
        this.expression = expression;
        processor = new DatasonnetProcessor();
        processor.setDatasonnetScript(expression);
        processor.init();
    }

    @Override
    public String toString() {
        return "datasonnet: " + expression;
    }

    @Override
    public <T> T evaluate(Exchange exchange, Class<T> type) {
        try {
            processor.setInputMimeType(getInputMimeType());
            processor.setOutputMimeType(getOutputMimeType());
            Object value = processor.processMapping(exchange);
            return exchange.getContext().getTypeConverter().convertTo(type, value);
        } catch (Exception e) {
            logger.error("Unable to evaluate expression: ", e);
            return null;
        }
    }

    @Override
    public void afterPropertiesConfigured(CamelContext camelContext) {
        processor.init();
    }

    @Override
    public boolean configure(CamelContext camelContext, Object target, String name, Object value, boolean ignoreCase) {
        if (target != this) {
            throw new IllegalStateException("Can only configure our own instance !");
        }
        switch (ignoreCase ? name.toLowerCase() : name) {
            case "inputMimeType":
            case "inputmimetype":
                setInputMimeType(PropertyConfigurerSupport.property(camelContext, String.class, value));
                return true;
            case "outputMimeType":
            case "outputmimetype":
                setOutputMimeType(PropertyConfigurerSupport.property(camelContext, String.class, value));
                return true;
        }

        return false;
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

}