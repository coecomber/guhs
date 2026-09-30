package nl.juiced.guhs.gametest;

import java.lang.annotation.ElementType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforgespi.language.ModFileScanData;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * 26.1 port: the gametest registrar. Vanilla's annotation system ({@code @GameTest}, {@code GameTestRegistry}) is gone;
 * tests are registry entries now. This class keeps the 1.0.0 way of writing tests: every
 * {@code public static void name(GameTestHelper)} method annotated with {@link GuhTest} anywhere in the mod is found
 * through FML's scan data (no class list to keep up to date) and registered:
 * <ul>
 *     <li>the method as a test function ({@code Registries.TEST_FUNCTION}, static registry, so this happens at mod
 *     construction through a {@link DeferredRegister});</li>
 *     <li>its batch as a test environment {@code guhs:batch/<batch>} (tests of one batch still run together);</li>
 *     <li>the test itself ({@link FunctionGameTestInstance}) in {@link RegisterGameTestsEvent}, with the old attributes
 *     (template {@code guhs:<template>}, timeoutTicks, setupTicks, required, rotationSteps, attempts, ...).</li>
 * </ul>
 * Test ids stay the 1.0.0 names: {@code guhs:<class name>.<method name>} in lower case (e.g.
 * {@code /test run guhs:guhgametests.tamingworks}). The {@code -Pgt=} filter ({@link GametestFilter}) is applied here
 * (it was a mixin on GameTestRegistry in 1.0.0). Nothing is registered unless NeoForge has gametests enabled
 * ({@link GameTestHooks#isGametestEnabled()}: dev runs and the gametest server, never a normal game).
 */
public final class GuhsGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, Guhs.MODID);

    /** One test method. */
    record Entry(Identifier id, Method method, GuhTest test) {
    }

    private static final List<Entry> ENTRIES = new ArrayList<>();

    private GuhsGameTests() {
    }

    /** Called from the mod constructor. */
    public static void register(IEventBus modBus) {
        if (!GameTestHooks.isGametestEnabled()) {
            return;
        }
        scan();
        for (Entry e : ENTRIES) {
            Method method = e.method();
            FUNCTIONS.register(e.id().getPath(), () -> helper -> invoke(method, helper));
        }
        FUNCTIONS.register(modBus);
        modBus.addListener(GuhsGameTests::onRegisterTests);
    }

    /** All @GuhTest methods of the mod (sorted by id), filtered by -Pgt. */
    private static void scan() {
        ModFileScanData scan = ModList.get().getModFileById(Guhs.MODID).getFile().getScanResult();
        String annotation = org.objectweb.asm.Type.getType(GuhTest.class).getDescriptor();
        Map<String, Entry> byId = new HashMap<>();
        int skipped = 0;
        for (ModFileScanData.AnnotationData data : scan.getAnnotations()) {
            if (data.targetType() != ElementType.METHOD || !annotation.equals(data.annotationType().getDescriptor())) {
                continue;
            }
            String className = data.clazz().getClassName();
            String member = data.memberName();
            String methodName = member.contains("(") ? member.substring(0, member.indexOf('(')) : member;
            Method method;
            try {
                Class<?> cls = Class.forName(className, false, GuhsGameTests.class.getClassLoader());
                method = cls.getDeclaredMethod(methodName, GameTestHelper.class);
            } catch (ReflectiveOperationException | LinkageError ex) {
                LOGGER.error("Guhs gametests: can't load test {}.{}", className, methodName, ex);
                continue;
            }
            if (!Modifier.isStatic(method.getModifiers())) {
                LOGGER.error("Guhs gametests: {}.{} is not static, skipped", className, methodName);
                continue;
            }
            method.setAccessible(true);
            GuhTest test = method.getAnnotation(GuhTest.class);
            if (test == null) {
                continue;
            }
            if (!GametestFilter.allowed(method)) {
                skipped++;
                continue;
            }
            Identifier id = Guhs.id(sanitize(method.getDeclaringClass().getSimpleName() + "." + methodName));
            Entry old = byId.put(id.toString(), new Entry(id, method, test));
            if (old != null) {
                LOGGER.error("Guhs gametests: two tests with the id {} ({} and {})", id, old.method(), method);
            }
        }
        ENTRIES.clear();
        ENTRIES.addAll(byId.values());
        ENTRIES.sort(Comparator.comparing(e -> e.id().toString()));
        LOGGER.info("Guhs gametests: {} test methods found{}", ENTRIES.size(),
                GametestFilter.active() ? " (-D" + GametestFilter.PROPERTY + " filter: " + skipped + " skipped)" : "");
    }

    /** Registry paths are [a-z0-9_./-]. */
    static String sanitize(String name) {
        StringBuilder out = new StringBuilder();
        for (char c : name.toLowerCase(Locale.ROOT).toCharArray()) {
            out.append((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == '/' || c == '-' ? c : '_');
        }
        return out.toString();
    }

    /** The environment id of a batch ("defaultBatch" -> guhs:batch/default). */
    static Identifier batchId(String batch) {
        String b = batch == null || batch.isEmpty() || batch.equals("defaultBatch") ? "default" : batch;
        return Guhs.id("batch/" + sanitize(b));
    }

    /** The structure of a test: "empty" -> guhs:empty (1.0.0: @PrefixGameTestTemplate(false) in namespace guhs). */
    static Identifier structureId(String template) {
        return template.contains(":") ? Identifier.parse(template) : Guhs.id(template);
    }

    private static void invoke(Method method, GameTestHelper helper) {
        try {
            method.invoke(null, helper);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;          // GameTestAssertException and friends reach the framework as before
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException(cause);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The game rules 1.21.1's gametest server had and 26.1's no longer sets (it only turns off mob spawning and weather):
     * no random ticks and no fire ticks. All Guhs tests were written for them (plants that must not grow by themselves,
     * fire that stays put), so every batch environment sets them again (and puts the old values back afterwards).
     */
    private static final net.minecraft.world.level.gamerules.GameRuleMap OLD_TEST_RULES = new net.minecraft.world.level.gamerules.GameRuleMap.Builder()
            .set(net.minecraft.world.level.gamerules.GameRules.RANDOM_TICK_SPEED, 0)
            .set(net.minecraft.world.level.gamerules.GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0)
            .build();

    /**
     * Every batch starts in the day (data/guhs/function/gametest/batch_start.mcfunction: {@code time set 3000}, Dagdeel DAG). All batches of
     * a run share one running world clock, and tests that sleep, nap or wake up assumed a day like the start of a 1.21.1
     * test run; without this they depended on how long the batches before them took and what time those left behind.
     */
    private static final Identifier BATCH_START = Guhs.id("gametest/batch_start");

    private static void onRegisterTests(RegisterGameTestsEvent event) {
        Map<Identifier, Holder<TestEnvironmentDefinition<?>>> environments = new HashMap<>();
        for (Entry e : ENTRIES) {
            GuhTest t = e.test();
            Holder<TestEnvironmentDefinition<?>> env = environments.computeIfAbsent(batchId(t.batch()),
                    id -> event.registerEnvironment(id, new TestEnvironmentDefinition.SetGameRules(OLD_TEST_RULES),
                            new TestEnvironmentDefinition.Functions(java.util.Optional.of(BATCH_START), java.util.Optional.empty())));
            TestData<Holder<TestEnvironmentDefinition<?>>> data = new TestData<>(env, structureId(t.template()), Math.max(1, t.timeoutTicks()),
                    (int) t.setupTicks(), t.required(), Rotation.values()[t.rotationSteps() & 3], t.manualOnly(), Math.max(1, t.attempts()),
                    Math.max(1, t.requiredSuccesses()), t.skyAccess(), 0);
            event.registerTest(e.id(), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, e.id()), data));
        }
    }

    /** How many tests are registered (for the log / gametests of the registrar itself). */
    public static int count() {
        return ENTRIES.size();
    }
}
