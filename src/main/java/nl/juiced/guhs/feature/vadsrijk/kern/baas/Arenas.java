package nl.juiced.guhs.feature.vadsrijk.kern.baas;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.kern.KernPayloads;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * The boss arenas: spawning, the intro when you walk in, HP scaling, the boss bar for everyone inside, the respawn board
 * ("terug over 0:42"). Register an arena with a factory; kern ticks it every 5 ticks while its centre chunk is loaded.
 */
public final class Arenas {
    private static final class Toestand {
        final Arena arena;
        final Function<ServerLevel, VadsrijkBaas> maker;
        @Nullable
        UUID baas;
        long respawnOp = -1;
        /** respawnOp read from {@link ArenaData} yet (once per server start)? */
        boolean geladen;
        @Nullable
        Display.TextDisplay bord;
        /** 1.10.3: game time of the last board update (own clock: see {@link #bord}). */
        long bordOp = -1000;
        final Set<UUID> binnen = new HashSet<>();
        /** 1.10.1: who took part in the last kill (helpers + everyone inside the arena then). */
        final Set<UUID> erbij = new HashSet<>();

        Toestand(Arena a, Function<ServerLevel, VadsrijkBaas> maker) {
            this.arena = a;
            this.maker = maker;
        }
    }

    private static final Map<String, Toestand> ALLE = new LinkedHashMap<>();

    private Arenas() {
    }

    public static synchronized void registreer(Arena a, Function<ServerLevel, VadsrijkBaas> maker) {
        ALLE.put(a.id(), new Toestand(a, maker));
    }

    @Nullable
    public static Arena van(String id) {
        Toestand t = ALLE.get(id);
        return t == null ? null : t.arena;
    }

    /** The arena this position is in, or null. */
    @Nullable
    public static Arena op(double x, double y, double z) {
        for (Toestand t : ALLE.values()) {
            if (t.arena.binnen(x, y, z)) {
                return t.arena;
            }
        }
        return null;
    }

    @Nullable
    public static VadsrijkBaas baas(ServerLevel sl, String arenaId) {
        Toestand t = ALLE.get(arenaId);
        return t == null ? null : zoek(sl, t);
    }

    /**
     * 1.10.1: a player who was NOT at the last kill and stands in the arena while it waits for its respawn does not wait the
     * full time (up to 20 min): the boss comes back within this many seconds. Those who were at the kill (farming) still wait.
     */
    public static final int NIEUWKOMER_SECONDEN = 30;

    static void verslagen(Arena a) {
        verslagen(a, java.util.List.of());
    }

    /** The boss of this arena was beaten with these players there (helpers + everyone inside). */
    static void verslagen(Arena a, java.util.Collection<UUID> erbij) {
        Toestand t = ALLE.get(a.id());
        // 1.10.3: also when the arena did not know this boss yet (a reloaded copy), unless a respawn is already pending
        if (t != null && (t.baas != null || t.respawnOp == -1)) {
            t.baas = null;
            t.respawnOp = -2;   // set on the next tick (we may not have the level here)
            t.erbij.clear();
            t.erbij.addAll(erbij);
        }
    }

    /** 1.10.1: the respawn time of this arena (game time; -1 = none pending). Tests. */
    public static long respawnOp(String arenaId) {
        Toestand t = ALLE.get(arenaId);
        return t == null ? -1 : t.respawnOp;
    }

    /** Dev: the boss of this arena away and back right now (full HP, phase 1). */
    public static boolean reset(MinecraftServer server, String id) {
        Toestand t = ALLE.get(id);
        ServerLevel sl = Vadsrijk.level(server);
        if (t == null || sl == null) {
            return false;
        }
        VadsrijkBaas b = zoek(sl, t);
        if (b != null) {
            b.discard();
        }
        t.baas = null;
        t.respawnOp = 0;
        t.geladen = true;
        ArenaData.get(server).zet(id, -1);
        t.binnen.clear();
        return true;
    }

    /** U-DUBBEL: forget a (test) arena again. */
    public static synchronized void afmelden(String id) {
        ALLE.remove(id);
    }

    /** U-DUBBEL: the unique key of an arena's boss. */
    public static String sleutel(String arenaId) {
        return "arena:" + arenaId;
    }

    public static List<String> ids() {
        return new ArrayList<>(ALLE.keySet());
    }

    @Nullable
    private static VadsrijkBaas zoek(ServerLevel sl, Toestand t) {
        if (t.baas != null && sl.getEntity(t.baas) instanceof VadsrijkBaas b && !b.isRemoved() && !b.geplofd()) {
            // 1.10.3 (live doubles): after a chunk reload this is a NEW object with the same uuid and no arena; without it the boss
            // never reported its defeat, and the arena spawned a fresh one at once next to the cheered-up one
            if (b.arena() != t.arena) {
                b.zetArena(t.arena);
            }
            return b;
        }
        // U-DUBBEL: the boss with this arena's unique key wherever it flew to (a Kruinroofvogel leaves the box below)
        if (nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.zoek(sl, sleutel(t.arena.id())) instanceof VadsrijkBaas b && !b.geplofd()) {
            t.baas = b.getUUID();
            b.zetArena(t.arena);
            return b;
        }
        BlockPos c = t.arena.centrum();
        int r = t.arena.straal() + 8;
        VadsrijkBaas gevonden = null;
        for (VadsrijkBaas b : sl.getEntitiesOfClass(VadsrijkBaas.class, new AABB(c).inflate(r, 24, r), x -> t.arena.id().equals(x.arenaId) && !x.geplofd())) {
            if (gevonden == null) {
                gevonden = b;
            } else if (b.getTarget() == null) {
                b.discard();   // U-DUBBEL: a second boss of one arena (an old double): only one stays
            }
        }
        if (gevonden != null) {
            t.baas = gevonden.getUUID();
            gevonden.zetArena(t.arena);
            nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.merk(gevonden, sleutel(t.arena.id()));
            return gevonden;
        }
        return null;
    }

    /** 1.10.3: is a beaten (geplofd, not yet vanished) boss of this arena still in the level? */
    private static boolean geplofdeKopie(ServerLevel sl, Toestand t) {
        if (t.baas != null && sl.getEntity(t.baas) instanceof VadsrijkBaas b && !b.isRemoved() && b.geplofd()) {
            return true;
        }
        if (nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.zoek(sl, sleutel(t.arena.id())) instanceof VadsrijkBaas b && b.geplofd()) {
            return true;
        }
        BlockPos c = t.arena.centrum();
        int r = t.arena.straal() + 8;
        return !sl.getEntitiesOfClass(VadsrijkBaas.class, new AABB(c).inflate(r, 24, r),
                x -> !x.isRemoved() && x.geplofd() && t.arena.id().equals(x.arenaId)).isEmpty();
    }

    public static void tick(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        long nu = sl.getGameTime();
        for (Toestand t : ALLE.values()) {
            tickArena(server, sl, t, nu);
        }
    }

    /** Tests: tick one arena in this level at this game time (the real tick passes the Vadsrijk level and its game time). */
    static void tickEen(ServerLevel sl, String arenaId, long nu) {
        Toestand t = ALLE.get(arenaId);
        if (t != null) {
            tickArena(sl.getServer(), sl, t, nu);
        }
    }

    /** Tests: no known boss, this respawn time (-1 = the boss is due), as if already loaded from {@link ArenaData}. */
    static void testStaat(String arenaId, long respawnOp) {
        Toestand t = ALLE.get(arenaId);
        if (t != null) {
            t.baas = null;
            t.respawnOp = respawnOp;
            t.geladen = true;
        }
    }

    /** Tests: the board this arena holds now (may be a stale reference). */
    @Nullable
    static Display.TextDisplay bordVan(String arenaId) {
        Toestand t = ALLE.get(arenaId);
        return t == null ? null : t.bord;
    }

    /** Tests: force the board reference (e.g. a display that never made it into the level). */
    static void zetBord(String arenaId, @Nullable Display.TextDisplay d) {
        Toestand t = ALLE.get(arenaId);
        if (t != null) {
            t.bord = d;
        }
    }

    private static void tickArena(MinecraftServer server, ServerLevel sl, Toestand t, long nu) {
        Arena a = t.arena;
        if (!sl.isLoaded(a.centrum())) {
            t.bord = null;
            // (integration) a far teleport unloads the arena: clear the bar of whoever was inside, or it stays on screen
            for (UUID weg : t.binnen) {
                if (sl.getEntity(weg) instanceof ServerPlayer p) {
                    KernPayloads.stuur(p, new KernPayloads.BaasBalk(-1, Component.empty(), 0, 0, 0));
                }
            }
            t.binnen.clear();
            return;
        }
        // U-DUBBEL: the chunk is there but its entities may not be yet (disk load / visibility a tick later): a boss that
        // is still coming back would look missing and get a twin. Wait until they are loaded and visible.
        if (!nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.klaar(sl, a.centrum())) {
            return;
        }
        ArenaData data = ArenaData.get(server);
        if (!t.geladen) {
            t.geladen = true;
            if (t.respawnOp != -2) {
                t.respawnOp = data.respawnOp(a.id());
            }
        }
        VadsrijkBaas b = zoek(sl, t);
        if (t.respawnOp == -2) {
            t.respawnOp = nu + a.respawnSeconden() * 20L;
            data.zet(a.id(), t.respawnOp);
        }
        // 1.10.1: a newcomer (not at the last kill) waits in the arena: the boss is back soon (quests for several players)
        if (b == null && t.respawnOp > nu + NIEUWKOMER_SECONDEN * 20L && nieuwkomerBinnen(sl, t)) {
            t.respawnOp = nu + NIEUWKOMER_SECONDEN * 20L;
            data.zet(a.id(), t.respawnOp);
        }
        // 1.10.3: a beaten (cheered-up) copy of this arena's boss is still hopping around: never a fresh one next to it. A kill that
        // was not reported (respawn still "alive") starts the respawn time now.
        if (b == null && (t.respawnOp < 0 || nu >= t.respawnOp) && geplofdeKopie(sl, t)) {
            if (t.respawnOp < 0) {
                t.baas = null;
                t.respawnOp = nu + a.respawnSeconden() * 20L;
                data.zet(a.id(), t.respawnOp);
            }
        } else if (b == null && (t.respawnOp < 0 || nu >= t.respawnOp)) {
            b = t.maker.apply(sl);
            if (b != null) {
                b.zetArena(a);
                nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.merk(b, sleutel(a.id()));   // U-DUBBEL
                b.snapTo(a.centrum().getX() + 0.5, a.centrum().getY(), a.centrum().getZ() + 0.5, 180, 0);
                sl.addFreshEntity(b);
                t.baas = b.getUUID();
                t.respawnOp = -1;
                data.zet(a.id(), -1);
                t.binnen.clear();
            }
        }
        // players inside: intro for newcomers, HP scaling, boss bar
        List<ServerPlayer> binnen = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && a.binnen(p.getX(), p.getY(), p.getZ()) && (b == null || b.magIntro(p))) {   // (M-BAZEN) magIntro
                binnen.add(p);
            }
        }
        Set<UUID> nuBinnen = new HashSet<>();
        for (ServerPlayer p : binnen) {
            nuBinnen.add(p.getUUID());
            if (b != null && !t.binnen.contains(p.getUUID())) {
                boolean eerste = Spelers.data(p).gezien.add("baas:" + a.id());
                b.intro(p, eerste);
            }
            if (b != null) {
                KernPayloads.stuur(p, new KernPayloads.BaasBalk(b.getId(), Component.translatable(b.titelKey()), b.fase(), (float) b.vads(), (float) b.maxVads(),
                        b.muziek() == null ? "" : b.muziek().toString()));
            }
        }
        for (UUID weg : t.binnen) {
            if (!nuBinnen.contains(weg) && sl.getEntity(weg) instanceof ServerPlayer p) {
                KernPayloads.stuur(p, new KernPayloads.BaasBalk(-1, Component.empty(), 0, 0, 0));
            }
        }
        if (b == null) {
            for (ServerPlayer p : binnen) {
                KernPayloads.stuur(p, new KernPayloads.BaasBalk(-1, Component.empty(), 0, 0, 0));
            }
        }
        t.binnen.clear();
        t.binnen.addAll(nuBinnen);
        if (b != null) {
            b.schaal(binnen.size());
        }
        bord(sl, t, b, nu);
    }

    /** 1.10.1 tests: does a player who was not at the last kill stand in this arena now (= the respawn gets short)? */
    public static boolean nieuwkomerBinnen(ServerLevel sl, String arenaId) {
        Toestand t = ALLE.get(arenaId);
        return t != null && nieuwkomerBinnen(sl, t);
    }

    private static boolean nieuwkomerBinnen(ServerLevel sl, Toestand t) {
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && p.isAlive() && t.arena.binnen(p.getX(), p.getY(), p.getZ()) && !t.erbij.contains(p.getUUID())) {
                return true;
            }
        }
        return false;
    }

    /** 1.10.1: the (non-spectator, living) players inside this arena now. */
    public static List<ServerPlayer> spelersBinnen(ServerLevel sl, Arena a) {
        List<ServerPlayer> uit = new ArrayList<>();
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && p.isAlive() && a.binnen(p.getX(), p.getY(), p.getZ())) {
                uit.add(p);
            }
        }
        return uit;
    }

    /** Tag of an arena's board (+ arena id): how a board whose reference got lost is found again. */
    static final String BORD_MERK = "guhs_vr_arenabord:";

    /**
     * The respawn board, about once a second. 1.10.3 (live: the boards were nowhere): this ran inside the 5-SERVER-tick rhythm
     * (tick count, 0 at every start) but waited for a GAME time (saved, keeps counting) that is a multiple of 20; when the two were
     * out of step (4 in 5 server starts) it never came. Now it keeps its own clock, and {@link Getallen#houdHologram} keeps exactly
     * one board: a stale reference (unloaded / never added) is replaced, a lost one is found again.
     */
    private static void bord(ServerLevel sl, Toestand t, @Nullable VadsrijkBaas b, long nu) {
        boolean weg = !Getallen.inLevel(sl, t.bord);
        if (!weg && nu >= t.bordOp && nu - t.bordOp < 20) {
            return;
        }
        BlockPos p = t.arena.bord();
        if (!sl.isLoaded(p)) {
            t.bord = null;
            return;
        }
        t.bordOp = nu;
        Component naam = Component.translatable(t.arena.titelKey()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        Component regel;
        if (b != null) {
            regel = Component.translatable("vadsrijk.kern.arena.wakker").withStyle(ChatFormatting.GOLD);
        } else {
            long s = Math.max(0, (t.respawnOp - nu) / 20);
            regel = Component.translatable("vadsrijk.kern.arena.terug", String.format("%d:%02d", s / 60, s % 60)).withStyle(ChatFormatting.LIGHT_PURPLE);
        }
        Component tekst = Component.empty().append(naam).append("\n").append(regel);
        t.bord = Getallen.houdHologram(sl, weg ? null : t.bord, BORD_MERK + t.arena.id(), p.getX() + 0.5, p.getY() + 1.6, p.getZ() + 0.5, tekst);
    }
}
