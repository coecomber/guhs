package nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.Z5WezensPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.Z5WezensRegistry;

/**
 * The zone-5 boss arenas and THE hook to the story (t-z5verhaal's {@code Z5Finale}):
 * <ul>
 *   <li>{@code z5wezens:zilverstaart} (the Wolkwaldroom): Kootje Zilverstaart only spawns when asked ({@link #start}); the
 *       story finds {@code start(ServerPlayer, String)} here by reflection ("zone5.wezens.Z5Bazen#start" is in t-z5verhaal's list;
 *       this class also registers itself with {@code Z5Finale.registreer(Fase.SPION, ...)} by reflection when that class exists).
 *       Beaten: {@code Z5Finale.spionVerslagen(p)} for every helper ({@link #spionVerslagen}).</li>
 *   <li>{@code z5wezens:gorgonzola} (the Ketelkamer in the Buikgangen): respawn 5 min.</li>
 *   <li>{@code z5wezens:donderbuik} (the tail fluke): world boss, respawn 20 min.</li>
 * </ul>
 * The DROOM phase (de Grauwe Droom van Moeder Wolkwal) is NOT built here: {@link #start} answers false for it, so the story plays
 * its own scene.
 */
public final class Z5Bazen {
    public static final String ARENA_SPION = "z5wezens:zilverstaart", ARENA_GORGONZOLA = "z5wezens:gorgonzola", ARENA_DONDERBUIK = "z5wezens:donderbuik";
    public static final Map<String, Arena> ARENAS = new LinkedHashMap<>();
    private static volatile boolean spionGevraagd;

    private Z5Bazen() {
    }

    private static void arena(Arena a, int lv, Element e, Function<ServerLevel, ? extends Z5Baas> maak) {
        ARENAS.put(a.id(), a);
        Arenas.registreer(a, sl -> {
            Z5Baas b = maak.apply(sl);
            if (b != null) {
                b.stelIn(lv, e, false, Set.of());
            }
            return b;
        });
    }

    /** From Z5WezensRegistry.register (common setup). */
    public static void registreer() {
        arena(new Arena(ARENA_SPION, Z5WezensPlekken.WOLKWALDROOM, Z5WezensPlekken.DROOM_STRAAL, Z5WezensPlekken.WOLKWALDROOM_DEUR, 10,
                Z5WezensPlekken.WOLKWALDROOM.below(40), "vadsrijk.z5wezens.baas.zilverstaart").met(Z5WezensPlekken.DROOM_STRAAL, 6, 14),
                60, Element.VUUR, sl -> {
                    if (!spionGevraagd) {
                        return null;
                    }
                    spionGevraagd = false;
                    return Z5WezensRegistry.ZILVERSTAART.get().create(sl, EntitySpawnReason.SPAWNER);
                });
        arena(new Arena(ARENA_GORGONZOLA, Z5WezensPlekken.KETELKAMER, Z5WezensPlekken.KETEL_STRAAL, Z5WezensPlekken.KETEL_DEUR, 300,
                Z5WezensPlekken.KETEL_DEUR.offset(-3, 0, 3), "vadsrijk.z5wezens.baas.gorgonzola").met(Z5WezensPlekken.KETEL_STRAAL, 4, 10),
                59, Element.VUUR, sl -> Z5WezensRegistry.GORGONZOLA.get().create(sl, EntitySpawnReason.SPAWNER));
        arena(new Arena(ARENA_DONDERBUIK, Z5WezensPlekken.DONDERBUIK, Z5WezensPlekken.DONDER_STRAAL, Z5WezensPlekken.DONDERBUIK_DEUR, 1200,
                Z5WezensPlekken.DONDERBUIK_DEUR.offset(-2, 0, 0), "vadsrijk.z5wezens.baas.donderbuik").met(Z5WezensPlekken.DONDER_STRAAL, 8, 20),
                60, Element.WATER, sl -> Z5WezensRegistry.DONDERBUIK.get().create(sl, EntitySpawnReason.SPAWNER));
        bijVerhaalAanmelden();
    }

    /**
     * THE story hook (found by t-z5verhaal by reflection): start a phase of the finale for this player. "spion": Kootje
     * Zilverstaart lands in the Wolkwaldroom (on the arena's next tick) and the player (+ party within 24) is brought to its door
     * when further than 40 away. "droom": not built here (false: the story's own scene plays).
     */
    public static boolean start(ServerPlayer p, String fase) {
        if (!"spion".equals(fase) || !(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(sl)) {
            return false;
        }
        VadsrijkBaas al = Arenas.baas(sl, ARENA_SPION);
        if (al == null) {
            spionGevraagd = true;
            Arenas.reset(sl.getServer(), ARENA_SPION);
        }
        BlockPos deur = Z5WezensPlekken.WOLKWALDROOM_DEUR;
        if (p.position().distanceToSqr(deur.getX(), deur.getY(), deur.getZ()) > 40 * 40) {
            for (ServerPlayer q : sl.players()) {
                if (q == p || q.distanceTo(p) < 24 && nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen.zelfdeParty(p, q)) {
                    sl.getChunk(deur);
                    q.teleportTo(sl, deur.getX() + 0.5, deur.getY() + 0.1, deur.getZ() + 0.5, Set.of(), 90, 0, true);
                }
            }
        }
        return true;
    }

    /** Is the spy asked for (dev). */
    public static boolean spionGevraagd() {
        return spionGevraagd;
    }

    // ---- t-z5verhaal by reflection (no compile dependency) ----------------------------------------------------------------------

    @Nullable
    private static Class<?> finale() {
        try {
            return Class.forName("nl.juiced.guhs.feature.vadsrijk.zone5.verhaal.Z5Finale");
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    /** Registers our SPION start with Z5Finale.registreer when the story slice is in this tree. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void bijVerhaalAanmelden() {
        Class<?> f = finale();
        if (f == null) {
            return;
        }
        try {
            Class<?> faseKlasse = Class.forName("nl.juiced.guhs.feature.vadsrijk.zone5.verhaal.Z5Finale$Fase");
            Class<?> startKlasse = Class.forName("nl.juiced.guhs.feature.vadsrijk.zone5.verhaal.Z5Finale$Start");
            Object spion = Enum.valueOf((Class) faseKlasse, "SPION");
            Object start = java.lang.reflect.Proxy.newProxyInstance(Z5Bazen.class.getClassLoader(), new Class<?>[] {startKlasse}, (proxy, m, args) -> {
                if ("start".equals(m.getName()) && args != null && args.length == 1 && args[0] instanceof ServerPlayer p) {
                    return start(p, "spion");
                }
                if ("hashCode".equals(m.getName())) {
                    return System.identityHashCode(proxy);
                }
                if ("equals".equals(m.getName())) {
                    return proxy == args[0];
                }
                return "toString".equals(m.getName()) ? "Z5Bazen.spion" : null;
            });
            Method reg = f.getMethod("registreer", faseKlasse, startKlasse);
            reg.invoke(null, spion, start);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("guhs/z5wezens").warn("could not register with Z5Finale", e);
        }
    }

    /** The spy is beaten: tell the story (Z5Finale.spionVerslagen, which starts the dream phase). */
    public static void spionVerslagen(ServerPlayer p) {
        Class<?> f = finale();
        if (f == null) {
            return;
        }
        try {
            f.getMethod("spionVerslagen", ServerPlayer.class).invoke(null, p);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            org.slf4j.LoggerFactory.getLogger("guhs/z5wezens").warn("Z5Finale.spionVerslagen failed", e);
        }
    }
}
