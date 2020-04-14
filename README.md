# Camel DataSonnet Module

The DataSonnet module adds [DataSonnet mapping language](http://datasonnet.com) support to Camel 3.x

# Camel supported versions
Camel >= 3.0.0

# Installation

To use the DataSonnet module in your application, add the following dependency to your `pom.xml`:

```
<dependency>
    <groupId>com.modus.camel.datasonnet</groupId>
    <artifactId>camel-datasonnet</artifactId>
    <version>1.0.2-SNAPSHOT</version>
</dependency>
```
# Usage

To transform a payload using DataSonnet, use the following processor example:

```
<bean id="basicTransform" class="com.modus.camel.datasonnet.DatasonnetProcessor" init-method="init">
    <property name="inputMimeType" value="application/json"/>
    <property name="outputMimeType" value="application/json"/>
    <property name="datasonnetFile" value="simpleMapping.ds"/>
</bean>

<camelContext id="main" xmlns="http://camel.apache.org/schema/spring">
    <route id="basicTransform">
        <from uri="direct:basicTransform"/>

        <setProperty name="test">
            <constant>HelloWorld</constant>
        </setProperty>
        <setProperty name="count">
            <constant>1</constant>
        </setProperty>
        <setProperty name="isActive">
            <constant>true</constant>
        </setProperty>

        <process ref="basicTransform"/>

        <to uri="mock:direct:end"/>
    </route>
</camelContext>

```

The `Datasonnet` bean has the following properties:

| Attribute | Mandatory | Description |
| --------- | --------- | ----------- |
| `datasonnetFile` | no | name of the DataSonnet transformation file (must be within the application classpath) |
| `datasonnetScript` | no | string containing DataSonnet transformation script. Either `datasonnetFile` or `datasonnetScript` attribute must be provided|
| `inputMimeType` | no | expected mime type of the inbound payload. Default is `application/json`|
| `outputMimeType` | no | the mime type of the resulting transformation. Default is `application/json`|
| `librariesPath` | no | list of directories separated by system path separator where the processor will search for named imports (i.e. all files with extension `.libsonnet`. If not set, the processor will search in the classpath (including JARs).  |

# Controlling Input and Output MIME Types
By default, Datasonnet consumes and produces data of the `application/json` type. However, if other data types are expected, this behavior can be changed by setting properties or headers explicitly. The following are supported:

|||
| --------------- | --------------------
| Input MIME type | header.Content-Type, header.inputMimeType, property.inputMimeType |
| Output MIME type | header.outputMimeType, property.outputMimeType |

# Named Imports Support
By default, named imports are resolved by scanning the application classpath and resolving the paths relative
to the classpath element or to the root of the jar where the library is located.
For example, consider the following application structure:

```
CAMEL_HOME
   ****/
      ├─ classes
      │  ├─ dslibs
      │  │  └─ lib2.libsonnet
      │  └─ lib1.libsonnet
      └─ lib
         └─ dslibs.jar      
```

The `dslibs.jar` contains libraries `lib3.libsonnet` and `morelibs/lib4.libsonnet`.

The imports section of the mapping should look like:

```
local lib1 = import 'lib1.libsonnet';
local lib2 = import 'dslibs/lib2.libsonnet';
local lib3 = import 'lib3.libsonnet';
local lib4 = import 'morelibs/lib4.libsonnet';
```

This behavior can be overridden by setting the `librariesPath` property of the processor bean. The value of this attribute is a set of absolute or relative paths separated by the `:` (colon) character.

# Expression Language Support

Datasonnet can be used as an inline expression language. For example:

```
<route id="expressionLanguage">
    <from uri="direct:expressionLanguage"/>

    <setHeader name="outputMimeType">
        <constant>text/plain</constant>
    </setHeader>
    <setHeader name="HelloHeader">
        <language language="datasonnet">"Hello, " + payload</language>
    </setHeader>

    <setHeader name="outputMimeType">
        <constant>application/json</constant>
    </setHeader>
    <setBody>
        <language language="datasonnet">
            {
                test: headers.HelloHeader
            }
        </language>
    </setBody>
    <to uri="mock:direct:end"/>
</route>
```

Since there are no additional attributes or parameters allowed for the `<language>` element, the input and output MIME types can be controlled by setting headers `Content-Type` and `outputMimeType` prior to calling an expression.

If you want to use Datasonnet expressions in the Camel Java DSL, you can use the `DatasonnetRouterBuilder` class and one of its `datasonnet()` functions, for example:

```
new DatasonnetRouteBuilder() {
    @Override
    public void configure() throws Exception {
        from("direct:expressionsInJava")
            .choice()
                .when(datasonnet("payload == 'World'"))
                    .setBody(datasonnet("'Hello, ' + payload", "text/plain", "text/plain"))
                .otherwise()
                    .setBody(datasonnet("{ \"message\":\"Good bye!\"}"))
            .end()
            .to("mock:direct:response");
    }
}
```

Chaining of expressions is also allowed, e.g.:

```
@Override
public void configure() throws Exception {
    from("direct:chainExpressions")
        .setHeader("ScriptHeader", constant("{ hello: \"World\"}"))
        .setBody(datasonnet(simple("${header.ScriptHeader}")))
        .to("mock:direct:response");
}
```

See the `DatasonnetRouterBuilder` class for more details.


