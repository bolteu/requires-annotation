[![Bolt](https://bolt.eu/favicon.ico)](https://bolt.eu/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Version](https://img.shields.io/nexus/r/eu.bolt/requiresannotation.plugin?server=https%3A%2F%2Foss.sonatype.org)](https://opensource.org/licenses/MIT)


# Requires Annotation and Plugin
Gradle plugin and Java annotation that checks (and fails the build if there are missing annotations) class fields for the annotations. 

The main story of this plugin and the annotation started with the missing `SerializedName` annotations for network model classes. Because of the missing `SerializedName` annotation in the network model classes, our JSON *serialization/deserialization* operations were failing. 

It is a developer mistake to forget some required annotation but this mistake can be avoidable with this annotation and the plugin.


## Requires Annotation
### Using the annotation processor
To use the annotation:
- Add maven repository `maven { url "https://oss.sonatype.org/content/repositories/releases/" }` as a repository source.
- Add dependency as an implementation and annotation processor:
```groovy
implementation "eu.bolt:requiresannotation.processor:1.0"
annotationProcessor "eu.bolt:requiresannotation.processor:1.0"
// lets declare both annotationProcessor and kapt of them https://issuetracker.google.com/issues/80270236
kapt "eu.bolt:requiresannotation.processor:1.0"
```

### Configuring the Annotation using the plugin
You can use `@RequiresAnnotation` for the methods and the classes. For classes, it will also search for the required annotation in the defined method's parameters.
```java
@RequiresAnnotation(requires = [ANNOTATION_CLASS_ARRAY], ignore = [CLASS_ARRAY])
```
- requires: Annotations that at least one of them is required in the parameters of this class' methods.
- ignore: Ignore the classes for required annotation checks.

### Example use-case of the annotation
Let's assume that we have a class and we want every method parameter in this class should be non-null and should be annotated with `@NonNull` annotation
```java
public class SomeClass {
    public void someStringMethod(String stringParam){}
    public void someIntegerMethod(@NonNull Integer integerParam){}
    public void someCustomClassMethod(MyClass integerParam){}
}
```

In this case `someStringMethod`'s `stringParam` don't have `@NonNull` annotation. But on the other hand, inside this class we only accept `someCustomClassMethod`'s `MyClass` parameter as a null. For informing the developer about missing annotation, we can fail the build using this annotation with following configruation:
```java
@RequiresAnnotation(requires = [NonNull.class], ignore = [MyClass.class])
```
and we can place this annotation on the class. So the last version of the class will look like:
```java
@RequiresAnnotation(requires = [NonNull.class], ignore = [MyClass.class])
public class SomeClass {
    public void someStringMethod(String stringParam){}
    public void someIntegerMethod(@NonNull Integer integerParam){}
    public void someCustomClassMethod(MyClass integerParam){}
}
```
When we start the build, it will be failed because of the `someStringMethod` and it will ignore the `someCustomClassMethod` because of the ignored `MyClass`.

## Requires Annotation Plugin
### Using the plugin
The plugin of the annotation allows you to use other annotations also for the check. To use the plugin, you must first apply it and then configure the parameter described below. To apply the plugin in the 
- Add maven repository `maven { url "https://oss.sonatype.org/content/repositories/releases/" }` to the `buildscript` in the root project's `build.gradle`
- Add dependency `classpath "eu.bolt:requiresannotation.plugin:1.0"` to the `buildscript` in the root project's `build.gradle`
```groovy
buildscript {
    repositories {
        ...
        maven { url 'https://oss.sonatype.org/content/repositories/releases/' }
        ...
    }
    dependencies {
        ...
        classpath "eu.bolt:requiresannotation.plugin:1.0"
        ...
    }
}
```
- Apply plugin `apply plugin: 'requiresannotation.plugin'` to module in the module's `build.gradle`

### Configuring the Annotation using the plugin
You can configure Requires Annotation using this plugin from module's `build.gradle` file. With the configuration, the annotation processor can also process the other annotations.
```groovy
requiresAnnotationProcessor {
  requires = ['TARGET_ANNOTATION_PACKAGE_NAME': ['REQUIRED_ANNOTATION_PACKAGE_NAMES']]
  ignore = ['IGNORED_PACKAGE_NAMES']
}
```

### Example use-case of the plugin
Let's assume that we are using `Retrofit` library for the API calls.
````java
@POST("/driverPhoneDetails")
Single<ServerResponse> sendSomeRequest(@Body SomeRequestModel body);
````
We want to fail the build if some of the fields in the `SomeRequestModel` class don't have `SerializedName` annotation.
```kotlin
data class SomeRequestModel(
    @SerializedName("string_field")
    val stringField: String,
    val intField: Int)
```
If we make an API call with this model, most probably it will fail on the release build because of the obfuscation causes the field name changes and the JSON serialization/deserialization library uses field names if JSON property name not given using the `SerializedName` annotation. In this case, there is a missing `SerializedName` annotation on the `intField` field. 

So we need to check all the class fields used in the methods annotated with `@POST` for the `@SerializedName` annotation. Requires Annotation can do this check with the following configuration on the module's `build.gradle` file:
````groovy
requiresAnnotationProcessor {
  requires = ['retrofit2.http.POST': ['com.google.gson.annotations.SerializedName']]
  ignore = ['com.example.MyIgnoreClass']
}
````

