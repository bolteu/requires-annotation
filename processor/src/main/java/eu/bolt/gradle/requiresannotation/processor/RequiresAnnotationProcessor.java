/*
 * Copyright (c) 2020 Bolt Technology OÜ. All rights reserved.
 */

package eu.bolt.gradle.requiresannotation.processor;

import com.sun.tools.javac.code.Attribute;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.VariableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequiresAnnotationProcessor extends AbstractProcessor {
    private static final String PLUGIN_PARAM_EXTRA_PROCESSOR_REQUIRES = "requires";
    private static final String PLUGIN_PARAM_EXTRA_PROCESSOR_IGNORE = "ignore";
    private static final String PLUGIN_PARAM_EXTRA_PROCESSOR_SEPARATOR = "_";
    private static final String REQUIRES_ANNOTATION = "eu.bolt.gradle.requiresannotation.annotation.RequiresAnnotation";
    private static final List<String> IGNORED_ELEMENT_PACKAGES = Arrays.asList("java.lang", "java.util", "android.os", "com.google", "io.reactivex");
    private static final int DEPT_LEVEL_METHOD = 0;
    private static final int DEPT_LEVEL_CLASS = 1;

    private final Map<String, List<String>> extraAnnotationsToScan = new HashMap<>();
    private final List<String> ignoreClassesToScan = new ArrayList<>();
    private final Pattern regexPatternForGenerics = Pattern.compile("([a-zA-Z0-9_\\.]+)");

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        final Map<String, String> envOptions = processingEnv.getOptions();
        if (envOptions != null) {
            for (String key : envOptions.keySet()) {
                String value = envOptions.get(key);
                if (envOptions.get(key) != null) {
                    if (key.contains(PLUGIN_PARAM_EXTRA_PROCESSOR_REQUIRES)) {
                        final String[] keySplit = key.split(PLUGIN_PARAM_EXTRA_PROCESSOR_SEPARATOR);
                        if (keySplit.length > 1) {
                            extraAnnotationsToScan.put(keySplit[1], Arrays.asList(value.split(PLUGIN_PARAM_EXTRA_PROCESSOR_SEPARATOR)));
                        }
                    } else if(key.contains(PLUGIN_PARAM_EXTRA_PROCESSOR_IGNORE)) {
                        ignoreClassesToScan.addAll(Arrays.asList(value.split(PLUGIN_PARAM_EXTRA_PROCESSOR_SEPARATOR)));
                    }
                }
            }
        }
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> supportedAnnotationTypes = new HashSet<>();
        supportedAnnotationTypes.add(REQUIRES_ANNOTATION);
        supportedAnnotationTypes.addAll(extraAnnotationsToScan.keySet());
        return supportedAnnotationTypes;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv != null) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(processingEnv.getElementUtils().getTypeElement(REQUIRES_ANNOTATION));
            if (elements != null && !elements.isEmpty()) {
                for (Element element : elements) {
                    final List<String> requiredAnnotations = new ArrayList<>();
                    final List<String> ignoreClasses = new ArrayList<>();
                    final List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
                    if (annotationMirrors != null && !annotationMirrors.isEmpty()) {
                        // as long as our parameters are Class[], we have to get them from annotation mirrors.
                        for (AnnotationMirror annotationMirror : annotationMirrors) {
                            Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirror.getElementValues();
                            if (elementValues != null && !elementValues.isEmpty()) {
                                for (ExecutableElement executableElement : elementValues.keySet()) {
                                    final AnnotationValue annotationValue = elementValues.get(executableElement);
                                    switch (executableElement.getSimpleName().toString()) {
                                        case "requires":
                                            for (Attribute attribute : ((Attribute.Array) annotationValue).values) {
                                                requiredAnnotations.add(attribute.getValue().toString());
                                            }
                                            break;
                                        case "ignore":
                                            for (Attribute attribute : ((Attribute.Array) annotationValue).values) {
                                                ignoreClasses.add(attribute.getValue().toString());
                                            }
                                            break;
                                    }
                                }
                            }
                        }
                    }

                    if (requiredAnnotations.isEmpty()) {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "RequiresAnnotation requires at least one annotation as 'requires'");
                        return false;
                    }
                    handleElement(element, requiredAnnotations, ignoreClasses);
                }
            }

            for (String extraAnnotationToScan : extraAnnotationsToScan.keySet()) {
                elements = roundEnv.getElementsAnnotatedWith(processingEnv.getElementUtils().getTypeElement(extraAnnotationToScan));
                if (elements != null && !elements.isEmpty()) {
                    for (Element element : elements) {
                        handleElement(element, extraAnnotationsToScan.get(extraAnnotationToScan), ignoreClassesToScan);
                    }
                }
            }
        }

        return true;
    }

    private void handleElement(Element element, List<String> requiredAnnotations, List<String> ignoreClasses) {
        if (element.getKind() == ElementKind.CLASS) {
            final List<ExecutableElement> executableElements = ElementFilter.methodsIn(element.getEnclosedElements());
            if (!executableElements.isEmpty()) {
                for (ExecutableElement executableElement : executableElements) {
                    handleMethodElement(executableElement, requiredAnnotations, ignoreClasses, DEPT_LEVEL_CLASS);
                }
            }
        } else if (element.getKind() == ElementKind.METHOD) {
            handleMethodElement((ExecutableElement) element, requiredAnnotations, ignoreClasses, DEPT_LEVEL_METHOD);
        }
    }

    private void handleMethodElement(ExecutableElement execElement, List<String> requiredAnnotations, List<String> ignoreClasses, int deptLevel) {
        final List<String> visitedClassesRecursively = new ArrayList<>();
        final List<? extends VariableElement> parameters = execElement.getParameters();
        if (parameters != null && !parameters.isEmpty()) {
            for (VariableElement paramElement : parameters) {
                checkParamElementForAnnotations(execElement, paramElement, requiredAnnotations, ignoreClasses, deptLevel, visitedClassesRecursively);
            }
        }
        processTypeElement(execElement, execElement.getReturnType().toString(), requiredAnnotations, ignoreClasses, deptLevel, visitedClassesRecursively);
    }

    private void checkParamElementForAnnotations(ExecutableElement execElement, VariableElement paramElement, List<String> requiredAnnotations, List<String> ignoreClasses, int deptLevel, List<String> visitedClassesRecursively) {
        if (!paramElement.getModifiers().contains(Modifier.STATIC) && !paramElement.getModifiers().contains(Modifier.TRANSIENT) && !ignoreClasses.contains(paramElement.getSimpleName().toString())) {
            // if this is not a trusted type, we should also check inside of this class for annotations
            if (!paramElement.asType().getKind().isPrimitive() && !checkIsJavaTypes(paramElement.asType().toString()) && !visitedClassesRecursively.contains(paramElement.asType().toString())) {
                visitedClassesRecursively.add(paramElement.asType().toString());
                processTypeElement(execElement, paramElement.asType().toString(), requiredAnnotations, ignoreClasses, deptLevel, visitedClassesRecursively);
            }
            // if deptLevel is 0, that means we are on the method parameter level which we should ignore its annotations.
            if (deptLevel > 0) {
                boolean foundRequiredAnnotation = false;
                final List<? extends AnnotationMirror> annotationMirrors = paramElement.getAnnotationMirrors();
                if (annotationMirrors != null && !annotationMirrors.isEmpty()) {
                    for (AnnotationMirror annotationMirror : annotationMirrors) {
                        if (requiredAnnotations.contains(annotationMirror.getAnnotationType().asElement().asType().toString())) {
                            foundRequiredAnnotation = true;
                            break;
                        }
                    }
                }
                if (!foundRequiredAnnotation) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, paramElement + " at " + execElement + " should have one of these annotations: " + requiredAnnotations, paramElement);
                }
            }
        }
    }

    private void processTypeElement(ExecutableElement execElement, String typeElementStr, List<String> requiredAnnotations, List<String> ignoreClasses, int deptLevel, List<String> visitedClassesRecursively) {
        if (typeElementStr != null) {
            Matcher m = regexPatternForGenerics.matcher(typeElementStr);
            while(m.find()) {
                for (int i=0 ; i<m.groupCount() ; i++) {
                    String typeStr = m.group(i);
                    if (!checkIsJavaTypes(typeStr) && !ignoreClasses.contains(typeStr)) {
                        TypeElement typeElement = processingEnv.getElementUtils().getTypeElement(typeStr);
                        if (typeElement != null && typeElement.getKind() != ElementKind.ENUM){
                            final List<? extends Element> enclosedElements = typeElement.getEnclosedElements();
                            if (enclosedElements != null && !enclosedElements.isEmpty()) {
                                for (Element enclosedElement : enclosedElements) {
                                    if (enclosedElement.getKind() == ElementKind.FIELD) {
                                        checkParamElementForAnnotations(execElement, (VariableElement) enclosedElement, requiredAnnotations, ignoreClasses, deptLevel + 1, visitedClassesRecursively);
                                    }
                                }
                            }
                        }
                    }
                }
            }

        }
    }

    private boolean checkIsJavaTypes(String packageName) {
        for (String ignoredElementPackage : IGNORED_ELEMENT_PACKAGES) {
            if (packageName.contains(ignoredElementPackage)) {
                return true;
            }
        }
        return false;
    }
}
