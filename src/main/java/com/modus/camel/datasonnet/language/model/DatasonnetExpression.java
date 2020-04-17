package com.modus.camel.datasonnet.language.model;

import org.apache.camel.CamelContext;
import org.apache.camel.Expression;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.spi.Metadata;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * To use Datasonnet scripts in Camel expressions or predicates.
 */
@Metadata(firstVersion = "1.0.0", label = "language,datasonnet", title = "Datasonnet")
@XmlRootElement(name = "datasonnet")
@XmlAccessorType(XmlAccessType.FIELD)
public class DatasonnetExpression extends ExpressionDefinition {

    @XmlAttribute(name = "inputMimeType")
    private String inputMimeType;

    @XmlAttribute(name = "outputMimeType")
    private String outputMimeType;

    private Expression innerExpression;

    public DatasonnetExpression() {
    }

    public DatasonnetExpression(String expression) {
        super(expression);
    }

    public DatasonnetExpression(Expression expression) {
        super();
        this.innerExpression = expression;
    }

    @Override
    public Expression createExpression(CamelContext camelContext) {
        com.modus.camel.datasonnet.language.DatasonnetExpression datasonnetExpression = (com.modus.camel.datasonnet.language.DatasonnetExpression)super.createExpression(camelContext);
        datasonnetExpression.setOutputMimeType(getOutputMimeType());
        datasonnetExpression.setInputMimeType(getInputMimeType());
        datasonnetExpression.setInnerExpression(getInnerExpression());
        return datasonnetExpression;
    }

    @Override
    public String getLanguage() {
        return "datasonnet";
    }

    @Override
    public String getExpression() {
        String exp = super.getExpression();
        if (exp == null && this.innerExpression != null) {
            exp = innerExpression.toString();
        }
        return exp;
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
