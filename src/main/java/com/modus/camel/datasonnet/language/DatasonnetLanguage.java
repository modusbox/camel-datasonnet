package com.modus.camel.datasonnet.language;

import org.apache.camel.spi.annotations.Language;
import org.apache.camel.support.LanguageSupport;

@Language("datasonnet")
public class DatasonnetLanguage extends LanguageSupport {

    @Override
    public DatasonnetExpression createPredicate(String expression) {
        return createExpression(expression);
    }

    @Override
    public DatasonnetExpression createExpression(String expression) {
        return new DatasonnetExpression(expression);
    }
}
