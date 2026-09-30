package nl.juiced.guhs.gametest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 26.1 port: our own replacement for the removed vanilla {@code @GuhTest} annotation (same attributes and defaults as
 * 1.21.1). A reflection registrar (gametest/, owner B) scans the *GameTests classes and registers every annotated
 * {@code public static void x(GameTestHelper)} method through NeoForge's {@code RegisterGameTestsEvent}, applying the
 * {@code -Pgt=} filter ({@link GametestFilter}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GuhTest {
    /** Structure template (data/guhs/structure/&lt;template&gt;.nbt), e.g. "empty". */
    String template() default "";

    int timeoutTicks() default 100;

    String batch() default "defaultBatch";

    int rotationSteps() default 0;

    boolean required() default true;

    boolean manualOnly() default false;

    long setupTicks() default 0L;

    int attempts() default 1;

    int requiredSuccesses() default 1;

    boolean skyAccess() default false;
}
