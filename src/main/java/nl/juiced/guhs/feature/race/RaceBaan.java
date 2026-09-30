package nl.juiced.guhs.feature.race;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.spelen.Niveau;

/**
 * A race track the {@link RaceGame} engine can run (2.9): the old guh racebaan (index 0, the Raceguh) and the three tracks
 * of the Guh-Circuit (Regenboogbaan, Vadsbaan, Kaasbergbaan: indexes 1-3, Coach Vahoegvroem, see feature/circuit).
 * A track knows its laps, its medal times (at medium; the other levels are slower / faster), its coin, which boards and
 * records it uses per level ({@link Niveau}), how its markers are found ({@link Zoeker}) and its extras ({@link Extra}:
 * Mika-pikkers, rolling kaasknabbels, the looping, the rainbow trail...).
 * <p>
 * The markers of one track all carry its index (RaceBlocks: race_start / race_checkpoint {@code baan}).
 */
public final class RaceBaan {
    /** Medal time factors of the levels: makkelijk has a calmer race guh (more time), lastig a faster one. */
    public static final float MAKKELIJK_TIJD = 1.16f, LASTIG_TIJD = 0.92f;

    private static final Map<String, RaceBaan> BANEN = new ConcurrentHashMap<>();
    private static final List<RaceBaan> ORDER = Collections.synchronizedList(new ArrayList<>());

    /** Where a track's markers are, for a race guh NPC (null: not found). */
    public interface Zoeker {
        @Nullable
        BoundingBox box(ServerLevel level, GuhNpcEntity npc);
    }

    /** What a track does beyond rings and pads. All hooks run on the server, from the race tick. */
    public interface Extra {
        default void start(RaceGame game, ServerLevel level, ServerPlayer racer) {
        }

        /** Every race tick after the countdown (also during a scripted ride). */
        default void tick(RaceGame game, ServerLevel level, ServerPlayer racer, RaceGuhEntity mount) {
        }

        /** The race is over (finished or not): take away what the track put out. */
        default void end(RaceGame game, ServerLevel level) {
        }

        /** A finished race (before the cleanup): advancements and such. */
        default void finish(RaceGame game, ServerLevel level, ServerPlayer racer, int total, RaceGame.Medal medal, boolean record) {
        }
    }

    /** The old guh racebaan (2.4): its boards and records keep their ids at medium. */
    public static final RaceBaan RACEBAAN = register(new RaceBaan("racebaan", 0, RaceGame.LAPS, new int[]{20 * 72, 20 * 85, 20 * 105},
            Minigames.RACE, () -> RaceFeature.RACEPRIJSJE.get(), null));

    public final String id;
    public final int index;
    public final int laps;
    /** Gold, silver and bronze at medium (ticks). */
    private final int[] medals;
    public final String minigame;
    private final Supplier<Item> coin;
    @Nullable
    private final Zoeker zoeker;
    @Nullable
    private volatile Extra extra;

    public RaceBaan(String id, int index, int laps, int[] medals, String minigame, Supplier<Item> coin, @Nullable Zoeker zoeker) {
        this.id = id;
        this.index = index;
        this.laps = laps;
        this.medals = medals;
        this.minigame = minigame;
        this.coin = coin;
        this.zoeker = zoeker;
    }

    public static RaceBaan register(RaceBaan baan) {
        if (BANEN.putIfAbsent(baan.id, baan) == null) {
            ORDER.add(baan);
        }
        return BANEN.get(baan.id);
    }

    @Nullable
    public static RaceBaan byId(String id) {
        return BANEN.get(id);
    }

    public static List<RaceBaan> all() {
        synchronized (ORDER) {
            return List.copyOf(ORDER);
        }
    }

    public boolean isRacebaan() {
        return index == 0;
    }

    public Item coin() {
        return coin.get();
    }

    @Nullable
    public Zoeker zoeker() {
        return zoeker;
    }

    /**
     * The track's extras, plus every listener of {@link #luister} (2.10, samen: your own guh rides along). Null only
     * when there is neither.
     */
    @Nullable
    public Extra extra() {
        Extra own = extra;
        return LUISTERAARS.isEmpty() ? own : new Samen(own);
    }

    /** 2.10: listeners that hear every track's race (start, tick, end, finish), next to the track's own extras. */
    private static final List<Extra> LUISTERAARS = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** Adds a listener for the races on every track (called after the track's own extras; a failing one is only logged). */
    public static void luister(Extra luisteraar) {
        if (!LUISTERAARS.contains(luisteraar)) {
            LUISTERAARS.add(luisteraar);
        }
    }

    /** The track's own extras (if any) and then the listeners. */
    private record Samen(@Nullable Extra own) implements Extra {
        private static void veilig(Runnable r) {
            try {
                r.run();
            } catch (RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().warn("RaceBaan listener failed", e);
            }
        }

        @Override
        public void start(RaceGame game, ServerLevel level, ServerPlayer racer) {
            if (own != null) {
                own.start(game, level, racer);
            }
            LUISTERAARS.forEach(l -> veilig(() -> l.start(game, level, racer)));
        }

        @Override
        public void tick(RaceGame game, ServerLevel level, ServerPlayer racer, RaceGuhEntity mount) {
            if (own != null) {
                own.tick(game, level, racer, mount);
            }
            LUISTERAARS.forEach(l -> veilig(() -> l.tick(game, level, racer, mount)));
        }

        @Override
        public void end(RaceGame game, ServerLevel level) {
            if (own != null) {
                own.end(game, level);
            }
            LUISTERAARS.forEach(l -> veilig(() -> l.end(game, level)));
        }

        @Override
        public void finish(RaceGame game, ServerLevel level, ServerPlayer racer, int total, RaceGame.Medal medal, boolean record) {
            if (own != null) {
                own.finish(game, level, racer, total, medal, record);
            }
            LUISTERAARS.forEach(l -> veilig(() -> l.finish(game, level, racer, total, medal, record)));
        }
    }

    /** Sets the track's extras (the circuit gives the old racebaan its lastig Mika-pikkers this way). */
    public RaceBaan extra(@Nullable Extra extra) {
        this.extra = extra;
        return this;
    }

    /** The name of the track: gui.guhs.race.baan.&lt;id&gt;. */
    public Component naam() {
        return Component.translatable("gui.guhs.race.baan." + id);
    }

    /** Gold / silver / bronze time on this level (0, 1, 2). */
    public int medalTicks(int medal, Niveau niveau) {
        float f = niveau == Niveau.MAKKELIJK ? MAKKELIJK_TIJD : niveau == Niveau.LASTIG ? LASTIG_TIJD : 1f;
        return Math.round(medals[medal] * f);
    }

    /** The world's top 3 of whole races: race_total(_makkelijk/_lastig) or circuit_&lt;id&gt;_&lt;level&gt;. */
    public String boardTotal(Niveau niveau) {
        return isRacebaan() ? niveau.board(RaceRole.BOARD_TOTAL) : "circuit_" + id + "_" + niveau.id();
    }

    /** The world's top 3 of the fastest laps: race_lap(_makkelijk/_lastig) or circuit_&lt;id&gt;_&lt;level&gt;_ronde. */
    public String boardLap(Niveau niveau) {
        return isRacebaan() ? niveau.board(RaceRole.BOARD_LAP) : "circuit_" + id + "_" + niveau.id() + "_ronde";
    }

    /** The personal records' key suffix: "" for the old racebaan at medium (its 2.4 records), else _&lt;id&gt;_&lt;level&gt;. */
    public String records(Niveau niveau) {
        return isRacebaan() && niveau == Niveau.MEDIUM ? "" : "_" + id + "_" + niveau.id();
    }

    /** The key of this track's scanned markers in the NPC's roleData. */
    String trackKey() {
        return isRacebaan() ? "Track" : "Track_" + id;
    }

    @Override
    public String toString() {
        return id;
    }
}
