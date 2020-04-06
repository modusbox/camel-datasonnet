package com.modus.camel.datasonnet.language;

import com.modus.camel.datasonnet.DatasonnetProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.support.ExpressionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatasonnetExpression extends ExpressionSupport {
    private final String text;

    private DatasonnetProcessor processor;

    private static Logger logger = LoggerFactory.getLogger(DatasonnetExpression.class);

    public DatasonnetExpression(String text) {
        this.text = text;
        processor = new DatasonnetProcessor();
        processor.init();
        processor.setDatasonnetScript(text);
    }

    @Override
    public String toString() {
        return "datasonnet: " + text;
    }

    @Override
    protected String assertionFailureMessage(Exchange exchange) {
        return this.toString();
    }

    @Override
    public <T> T evaluate(Exchange exchange, Class<T> type) {
        try {
            Object value = processor.processMapping(exchange);
            return exchange.getContext().getTypeConverter().convertTo(type, value);
        } catch (Exception e) {
            logger.error("Unable to evaluate expression: ", e);
            return null;
        }
    }

}
