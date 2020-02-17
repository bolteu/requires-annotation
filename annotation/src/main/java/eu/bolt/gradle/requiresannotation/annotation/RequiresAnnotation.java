package eu.bolt.gradle.requiresannotation.annotation;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequiresAnnotation {
    /**
     * @return the required annotation for this method or class
     */
    Class<? extends Annotation>[] requires();

    /**
     * @return the ignored classes
     */
    Class[] ignore() default {};
}
