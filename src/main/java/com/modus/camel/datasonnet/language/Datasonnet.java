package com.modus.camel.datasonnet.language;

import org.apache.camel.support.language.LanguageAnnotation;
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Documented
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@LanguageAnnotation(language = "datasonnet")
public @interface Datasonnet {
    String value();
}
