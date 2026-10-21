package nl.juiced.guhs.feature.safari.gedrag;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.safari.BezigheidGoal;
import nl.juiced.guhs.feature.safari.Safari;

/**
 * Round 4 (design ch. 6, ronde 49 "eerst meten"): what are the park animals really doing?
 * <ul>
 *   <li>{@code /guhs safari debug dieren}: a label over every animal (activity, behaviour, animation, asleep).</li>
 *   <li>{@code /guhs safari debug meet}: every minute a log summary per species (activities started and failed, active and
 *       sleeping share, blocks walked per animal per minute, behaviours done).</li>
 *   <li>{@code /guhs safari debug waarnemer <pos> [r]}: a virtual onlooker (counts as a player nearby for the animals,
 *       and its chunks are kept loaded) so a dev server without players can be measured.</li>
 * </ul>
 */
public final class SafariDebug {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Labels over the animals. */
    public static volatile boolean labels;
    /** Measuring (the per-minute log). */
    public static volatile boolean meten;
    /** Virtual onlookers in the park dimension. */
    public static final List<BlockPos> WAARNEMERS = new ArrayList<>();

    /** Per species: one minute of numbers. */
    public static final class Stat {
        public final EnumMap<BezigheidGoal.Soort, Integer> begonnen = new EnumMap<>(BezigheidGoal.Soort.class);
        public int mislukt, ticks, slaapTicks, actieveTicks;
        public double afstand;
        public final Map<String, Integer> acties = new TreeMap<>();
        public final java.util.Set<Integer> dieren = new java.util.HashSet<>();
    }

    public static final Map<String, Stat> STATS = new TreeMap<>();

    private static Stat stat(String soort) {
        return STATS.computeIfAbsent(soort, s -> new Stat());
    }

    public static synchronized void begon(String soort, BezigheidGoal.Soort s, boolean ok, String actie) {
        if (!meten) {
            return;
        }
        Stat st = stat(soort);
        if (!ok || s == null) {
            st.mislukt++;
            return;
        }
        st.begonnen.merge(s, 1, Integer::sum);
        if (!actie.isEmpty()) {
            st.acties.merge(actie, 1, Integer::sum);
        }
    }

    public static synchronized void tik(String soort, int id, double afstand, boolean slaapt, boolean bezig) {
        if (!meten) {
            return;
        }
        Stat st = stat(soort);
        st.ticks++;
        st.dieren.add(id);
        st.afstand += afstand;
        if (slaapt) {
            st.slaapTicks++;
        } else if (bezig) {
            st.actieveTicks++;
        }
    }

    /** Is a virtual onlooker within r of x,y,z (park dimension only)? */
    public static boolean waarnemer(ServerLevel level, double x, double y, double z, double r) {
        if (WAARNEMERS.isEmpty() || !Safari.echt(level)) {
            return false;
        }
        for (BlockPos p : WAARNEMERS) {
            if (p.distToCenterSqr(x, y, z) <= r * r) {
                return true;
            }
        }
        return false;
    }

    /** The per-minute summary (also the tests' view of the numbers). */
    public static synchronized List<String> samenvatting() {
        List<String> uit = new ArrayList<>();
        for (Map.Entry<String, Stat> e : STATS.entrySet()) {
            Stat st = e.getValue();
            int n = Math.max(1, st.dieren.size());
            double minuten = Math.max(1, st.ticks) / (20.0 * n) / 60.0;
            uit.add(String.format(java.util.Locale.ROOT, "%-18s n=%d slaap=%d%% bezig=%d%% gelopen=%.1f blok/dier/min begonnen=%s mislukt=%d acties=%s",
                    e.getKey(), st.dieren.size(), 100 * st.slaapTicks / Math.max(1, st.ticks), 100 * st.actieveTicks / Math.max(1, st.ticks),
                    st.afstand / n / Math.max(1e-6, minuten), st.begonnen, st.mislukt, st.acties));
        }
        return uit;
    }

    public static synchronized void wisStats() {
        STATS.clear();
    }

    public static void serverTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (meten && server.getTickCount() % 1200 == 0 && !STATS.isEmpty()) {
            LOGGER.info("Safari debug: one minute of animals ({} species)", STATS.size());
            for (String r : samenvatting()) {
                LOGGER.info("  {}", r);
            }
            wisStats();
        }
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("safari").then(Commands.literal("debug")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("dieren").executes(ctx -> {
                    labels = !labels;
                    ctx.getSource().sendSuccess(() -> Component.literal("Safari debug labels: " + (labels ? "aan" : "uit")), false);
                    return 1;
                }))
                .then(Commands.literal("meet").executes(ctx -> {
                    meten = !meten;
                    wisStats();
                    ctx.getSource().sendSuccess(() -> Component.literal("Safari debug meten: " + (meten ? "aan (elke minuut in de log)" : "uit")), false);
                    return 1;
                }))
                .then(Commands.literal("nu").executes(ctx -> {
                    for (String r : samenvatting()) {
                        ctx.getSource().sendSuccess(() -> Component.literal(r), false);
                    }
                    return 1;
                }))
                .then(Commands.literal("waarnemer").then(Commands.argument("pos", BlockPosArgument.blockPos())
                        .executes(ctx -> waarnemer(ctx.getSource().getServer(), BlockPosArgument.getBlockPos(ctx, "pos"), 3))
                        .then(Commands.argument("r", IntegerArgumentType.integer(0, 8))
                                .executes(ctx -> waarnemer(ctx.getSource().getServer(), BlockPosArgument.getBlockPos(ctx, "pos"),
                                        IntegerArgumentType.getInteger(ctx, "r"))))))
                .then(Commands.literal("wis").executes(ctx -> {
                    ServerLevel level = Safari.level(ctx.getSource().getServer());
                    if (level != null) {
                        for (BlockPos p : WAARNEMERS) {
                            forceer(level, p, 3, false);
                        }
                    }
                    WAARNEMERS.clear();
                    meten = false;
                    labels = false;
                    wisStats();
                    ctx.getSource().sendSuccess(() -> Component.literal("Safari debug uit"), false);
                    return 1;
                })))));
    }

    private static int waarnemer(MinecraftServer server, BlockPos p, int r) {
        ServerLevel level = Safari.level(server);
        if (level == null) {
            return 0;
        }
        WAARNEMERS.add(p.immutable());
        forceer(level, p, r, true);
        return 1;
    }

    private static void forceer(ServerLevel level, BlockPos p, int r, boolean aan) {
        int cx = p.getX() >> 4, cz = p.getZ() >> 4;
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                level.setChunkForced(x, z, aan);
            }
        }
    }

    private SafariDebug() {
    }
}
