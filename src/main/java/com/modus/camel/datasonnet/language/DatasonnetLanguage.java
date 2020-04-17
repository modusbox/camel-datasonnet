package com.modus.camel.datasonnet.language;

import com.modus.camel.datasonnet.DatasonnetProcessor;
import org.apache.camel.spi.annotations.Language;
import org.apache.camel.support.LanguageSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Language("datasonnet")
public class DatasonnetLanguage extends LanguageSupport {
    private static Logger logger = LoggerFactory.getLogger(DatasonnetProcessor.class);

    @Override
    public DatasonnetExpression createPredicate(String expression) {
        return createExpression(expression);
    }

    @Override
    public DatasonnetExpression createExpression(String expression) {
        DatasonnetExpression datasonnetExpression = new DatasonnetExpression(expression);
        return datasonnetExpression;
    }
}
