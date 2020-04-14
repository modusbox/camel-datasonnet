package com.modus.camel.datasonnet;

import com.modus.camel.datasonnet.language.model.DatasonnetExpression;
import org.apache.camel.Expression;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.builder.ValueBuilder;

public abstract class DatasonnetRouteBuilder extends RouteBuilder {
    public ValueBuilder datasonnet(String value) {
        return datasonnet(value, "application/json", "application/json");
    }
    public ValueBuilder datasonnet(Expression expression) {
        return datasonnet(expression, "application/json", "application/json");
    }
    public ValueBuilder datasonnet(String value, String inputMimeType, String outputMimeType) {
        DatasonnetExpression exp = new DatasonnetExpression(value);
        exp.setInputMimeType(inputMimeType);
        exp.setOutputMimeType(outputMimeType);
        return new ValueBuilder(exp);
    }
    public ValueBuilder datasonnet(Expression expression, String inputMimeType, String outputMimeType) {
        DatasonnetExpression exp = new DatasonnetExpression(expression);
        exp.setInputMimeType(inputMimeType);
        exp.setOutputMimeType(outputMimeType);
        return new ValueBuilder(exp);
    }
}
