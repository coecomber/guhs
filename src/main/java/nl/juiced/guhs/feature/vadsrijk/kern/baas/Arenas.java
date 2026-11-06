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
        final Set<UUID> binnen = new HashSet<>();

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

    static void verslagen(Arena a) {
        Toestand t = ALLE.get(a.id());
        if (t != null && t.baas != null) {
            t.baas = null;
            t.respawnOp = -2;   // set on the next tick (we may not have the level here)
        }
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

    public static void tick(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        long nu = sl.getGameTime();
        for (Toestand t : ALLE.values()) {
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
                continue;
            }
            // U-DUBBEL: the chunk is there but its entities may not be yet (disk load / visibility a tick later): a boss that
            // is still coming back would look missing and get a twin. Wait until they are loaded and visible.
            if (!nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.klaar(sl, a.centrum())) {
                continue;
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
            if (b == null && (t.respawnOp < 0 || nu >= t.respawnOp)) {
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
    }

    private static void bord(ServerLevel sl, Toestand t, @Nullable VadsrijkBaas b, long nu) {
        if (nu % 20 != 0) {
            return;
        }
        Component naam = Component.translatable(t.arena.titelKey()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        Component regel;
        if (b != null) {
            regel = Component.translatable("vadsrijk.kern.arena.wakker").withStyle(ChatFormatting.GOLD);
        } else {
            long s = Math.max(0, (t.respawnOp - nu) / 20);
            regel = Component.translatable("vadsrijk.kern.arena.terug", String.format("%d:%02d", s / 60, s % 60)).withStyle(ChatFormatting.LIGHT_PURPLE);
        }
        Component tekst = Component.empty().append(naam).append("\n").append(regel);
        if (t.bord == null || t.bord.isRemoved()) {
            BlockPos p = t.arena.bord();
            t.bord = Getallen.hologram(sl, p.getX() + 0.5, p.getY() + 1.6, p.getZ() + 0.5, tekst);
        } else {
            Getallen.zetTekst(t.bord, tekst);
        }
    }
}
