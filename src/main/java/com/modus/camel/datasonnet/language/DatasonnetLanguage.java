package com.modus.camel.datasonnet.language;

import org.apache.camel.spi.annotations.Language;
import org.apache.camel.support.LanguageSupport;

import javax.xml.bind.annotation.XmlAttribute;

@Language("datasonnet")
public class DatasonnetLanguage extends LanguageSupport {
    private String inputMimeType;
    private String outputMimeType;

    @Override
    public DatasonnetExpression createPredicate(String expression) {
        return createExpression(expression);
    }

    @Override
    public DatasonnetExpression createExpression(String expression) {
        DatasonnetExpression datasonnetExpression = new DatasonnetExpression(expression);
        datasonnetExpression.setInputMimeType(inputMimeType);
        datasonnetExpression.setOutputMimeType(outputMimeType);
        return datasonnetExpression;
    }

    public static DatasonnetExpression datasonnet(String expression, String inputMimeType, String outputMimeType) {
        DatasonnetLanguage datasonnetLanguage = new DatasonnetLanguage();
        datasonnetLanguage.setInputMimeType(inputMimeType);
        datasonnetLanguage.setOutputMimeType(outputMimeType);
        return datasonnetLanguage.createExpression(expression);
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
