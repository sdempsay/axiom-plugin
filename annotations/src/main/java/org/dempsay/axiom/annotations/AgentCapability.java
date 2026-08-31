package org.dempsay.axiom.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Optional source annotation for an intent. Harvest is off unless the Maven plugin
 * sets {@code harvestAnnotations=true}. Harvest does not invent snippets.
 *
 * @author Shawn Dempsay {@literal <shawn@dempsay.org>}
 * @since 0.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface AgentCapability {

    /**
     * @return stable intent id
     */
    String id();

    /**
     * @return human title
     */
    String title();

    /**
     * @return required or available
     */
    Severity severity() default Severity.AVAILABLE;

    /**
     * @return lookup hints
     */
    String[] triggers() default {};

    /**
     * @return vernacular source regexes (not snippets)
     */
    String[] antiPatterns() default {};
}
