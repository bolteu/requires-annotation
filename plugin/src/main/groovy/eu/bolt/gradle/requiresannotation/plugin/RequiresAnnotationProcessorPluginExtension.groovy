/*
 * Copyright (c) 2020 Bolt Technology OÜ. All rights reserved.
 */

package eu.bolt.gradle.requiresannotation.plugin

class RequiresAnnotationProcessorPluginExtension {
    static final String NAME = "requiresAnnotationProcessor"
    Map<String, String[]> requires
    String[] ignore
}
