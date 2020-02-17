package eu.bolt.gradle.requiresannotation.plugin

class RequiresAnnotationProcessorPluginExtension {
    static final String NAME = "requiresAnnotationProcessor"
    Map<String, String[]> requires
    String[] ignore
}
