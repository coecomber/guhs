package nl.juiced.guhs.gametest;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import nl.juiced.guhs.gametest.GuhTest;

/**
 * Runs only some gametests (2.9, for the slices): {@code ./gradlew runGameTestServer -Pgt=KnusGameTests,HighscoresGameTests}
 * passes {@code -Dguhs.gametests=<list>} (build.gradle) and only test methods whose class simple name OR batch starts with
 * one of the entries (ignoring case) are registered (mixin.GameTestRegistryMixin). Without the property (or empty)
 * everything runs, as before.
 */
public final class GametestFilter {
    public static final String PROPERTY = "guhs.gametests";
    private static final List<String> ENTRIES = parse(System.getProperty(PROPERTY, ""));
    private static final AtomicInteger KEPT = new AtomicInteger(), SKIPPED = new AtomicInteger();
    private static volatile boolean announced;

    static List<String> parse(String value) {
        return Arrays.stream(value.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(s -> s.toLowerCase(Locale.ROOT)).toList();
    }

    /** Is the filter on? */
    public static boolean active() {
        return !ENTRIES.isEmpty();
    }

    /** Does this test method pass the filter (always true without a filter; non-test methods always pass)? */
    public static boolean allowed(Method method) {
        GuhTest test = method.getAnnotation(GuhTest.class);
        if (!active() || test == null) {
            return true;
        }
        boolean ok = matches(ENTRIES, method.getDeclaringClass().getSimpleName(), test.batch());
        (ok ? KEPT : SKIPPED).incrementAndGet();
        if (!announced) {
            announced = true;
            org.slf4j.LoggerFactory.getLogger("guhs").info("Guhs gametest filter -D{}={}: only these classes/batches run", PROPERTY, ENTRIES);
        }
        return ok;
    }

    /** Does a test of this class (simple name) and batch pass these (lower-case) entries? */
    public static boolean matches(List<String> entries, String className, String batch) {
        String c = className.toLowerCase(Locale.ROOT), b = batch == null ? "" : batch.toLowerCase(Locale.ROOT);
        return entries.stream().anyMatch(e -> c.startsWith(e) || b.startsWith(e));
    }

    /** How many test methods the filter kept / skipped so far. */
    public static int kept() {
        return KEPT.get();
    }

    public static int skipped() {
        return SKIPPED.get();
    }

    private GametestFilter() {
    }
}
