package com.modus.camel.datasonnet.language;

import com.modus.camel.datasonnet.DatasonnetProcessor;
import org.apache.camel.*;
import org.apache.camel.spi.GeneratedPropertyConfigurer;
import org.apache.camel.support.ExpressionAdapter;
import org.apache.camel.support.component.PropertyConfigurerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasonnetExpression extends ExpressionAdapter implements GeneratedPropertyConfigurer {
    private final String expression;

    private String inputMimeType;
    private String outputMimeType;

    private DatasonnetProcessor processor;

    private Expression innerExpression;

    private static Logger logger = LoggerFactory.getLogger(DatasonnetExpression.class);

    public DatasonnetExpression(String expression) {
        this.expression = expression;
        processor = new DatasonnetProcessor();
        processor.setDatasonnetScript(expression);
        try {
            processor.init();
        } catch (Exception e) {
            throw new RuntimeExpressionException("Unable to initialize DataSonnet processor : ", e);
        }
    }

    @Override
    public String toString() {
        return "datasonnet: " + expression;
    }

    @Override
    public <T> T evaluate(Exchange exchange, Class<T> type) {
        try {
            if (innerExpression != null) {
                String script = innerExpression.evaluate(exchange, String.class);
                processor.setDatasonnetScript(script);
            }
            processor.setInputMimeType(getInputMimeType());
            processor.setOutputMimeType(getOutputMimeType());
            Object value = processor.processMapping(exchange);
            return exchange.getContext().getTypeConverter().convertTo(type, value);
        } catch (Exception e) {
            throw new RuntimeExpressionException("Unable to evaluate DataSonnet expression : " + expression, e);
        }
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

    public Expression getInnerExpression() {
        return innerExpression;
    }

    public void setInnerExpression(Expression innerExpression) {
        this.innerExpression = innerExpression;
    }
}