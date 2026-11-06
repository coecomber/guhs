package nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6Bouw;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6WezensPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6WezensRegistry;

/**
 * The zone-6 boss arenas (kern {@link Arenas}: one boss per arena, the U-DUBBEL uniqueness key {@code arena:<id>}, respawn, the
 * personal chest) and THE hook for the story (u-z6verhaal):
 * <ul>
 *   <li>{@code z6wezens:wormelien} (De Roze Duinen): field boss, only by DAY (06:00-18:00), respawn 20 min;</li>
 *   <li>{@code z6wezens:zoutkolos} (De Zoutvlakte): field boss, only at NIGHT, respawn 20 min;</li>
 *   <li>{@code z6wezens:astrolabiumwachter}, {@code :glimmertje}, {@code :sterrenguh}: the three rooms of the dungeon De Verzonken
 *       Sterrenwacht ({@link Z6Bouw}), respawn 5 min.</li>
 * </ul>
 * {@link #start(ServerPlayer, String)} is static so the story can find it by reflection
 * ({@code zone6.wezens.baas.Z6Bazen#start}).
 */
public final class Z6Bazen {
    public static final String WORMELIEN = "z6wezens:wormelien", ZOUTKOLOS = "z6wezens:zoutkolos", ASTROLABIUM = "z6wezens:astrolabiumwachter",
            GLIMMERTJE = "z6wezens:glimmertje", STERRENGUH = "z6wezens:sterrenguh";
    public static final int LV_WORMELIEN = 63, LV_ZOUTKOLOS = 66, LV_ASTROLABIUM = 66, LV_GLIMMERTJE = 67, LV_STERRENGUH = 68;
    public static final Map<String, Arena> ARENAS = new LinkedHashMap<>();
    /** Dev/story: spawn the day/night field boss even at the wrong time of day (once). */
    public static volatile boolean forceerWormelien, forceerZoutkolos;

    private Z6Bazen() {
    }

    private static void arena(Arena a, int lv, Element e, Function<ServerLevel, ? extends Z6Baas> maak) {
        ARENAS.put(a.id(), a);
        Arenas.registreer(a, sl -> {
            Z6Baas b = maak.apply(sl);
            if (b != null) {
                b.stelIn(lv, e, false, Set.of());
            }
            return b;
        });
    }

    /** Is it day in the Vadsrijk (06:00-18:00)? */
    public static boolean dag(ServerLevel sl) {
        return sl.isBrightOutside();
    }

    /** From Z6WezensRegistry.register (common setup). */
    public static void registreer() {
        if (!ARENAS.isEmpty()) {
            return;
        }
        BlockPos w = Z6WezensPlekken.WORMELIEN, z = Z6WezensPlekken.ZOUTKOLOS;
        arena(new Arena(WORMELIEN, w, Z6WezensPlekken.WORMELIEN_STRAAL, Z6WezensPlekken.WORMELIEN_DEUR, 1200, Z6WezensPlekken.WORMELIEN_BORD,
                "vadsrijk.z6wezens.baas.wormelien").met(Z6WezensPlekken.WORMELIEN_STRAAL, 20, 30), LV_WORMELIEN, Element.NATUUR, sl -> {
                    if (!dag(sl) && !forceerWormelien) {
                        return null;
                    }
                    forceerWormelien = false;
                    return Z6WezensRegistry.WORMELIEN.get().create(sl, EntitySpawnReason.SPAWNER);
                });
        arena(new Arena(ZOUTKOLOS, z, Z6WezensPlekken.ZOUTKOLOS_STRAAL, Z6WezensPlekken.ZOUTKOLOS_DEUR, 1200, Z6WezensPlekken.ZOUTKOLOS_BORD,
                "vadsrijk.z6wezens.baas.zoutkolos").met(Z6WezensPlekken.ZOUTKOLOS_STRAAL, 20, 30), LV_ZOUTKOLOS, Element.WATER, sl -> {
                    if (dag(sl) && !forceerZoutkolos) {
                        return null;
                    }
                    forceerZoutkolos = false;
                    return Z6WezensRegistry.ZOUTKOLOS.get().create(sl, EntitySpawnReason.SPAWNER);
                });
        BlockPos r = Z6WezensPlekken.RINGENZAAL, v = Z6WezensPlekken.VUURSCHALENZAAL, k = Z6WezensPlekken.KOEPEL;
        arena(new Arena(ASTROLABIUM, r, Z6WezensPlekken.RINGEN_STRAAL, r.offset(-Z6WezensPlekken.RINGEN_STRAAL + 1, 0, 0), 300, r.offset(-8, 0, -3),
                "vadsrijk.z6wezens.baas.astrolabiumwachter").met(Z6WezensPlekken.RINGEN_STRAAL, 2, 10), LV_ASTROLABIUM, Element.ROZE,
                sl -> Z6WezensRegistry.ASTROLABIUMWACHTER.get().create(sl, EntitySpawnReason.SPAWNER));
        int vr = Z6WezensPlekken.VUUR_HALF + 3;
        arena(new Arena(GLIMMERTJE, v, vr, v.offset(-Z6WezensPlekken.VUUR_HALF + 1, 0, 0), 300, v.offset(-8, 0, -4),
                "vadsrijk.z6wezens.baas.glimmertje").met(vr, 2, 10), LV_GLIMMERTJE, Element.VUUR,
                sl -> Z6WezensRegistry.GLIMMERTJE.get().create(sl, EntitySpawnReason.SPAWNER));
        arena(new Arena(STERRENGUH, k, Z6WezensPlekken.KOEPEL_STRAAL, k.offset(-Z6WezensPlekken.KOEPEL_STRAAL + 1, 0, 0), 300, k.offset(-9, 0, -4),
                "vadsrijk.z6wezens.baas.sterrenguh").met(Z6WezensPlekken.KOEPEL_STRAAL, 2, 14), LV_STERRENGUH, Element.ROZE,
                sl -> Z6WezensRegistry.STERRENGUH.get().create(sl, EntitySpawnReason.SPAWNER));
    }

    /**
     * THE story hook (u-z6verhaal finds it by reflection): "wormelien" / "zoutkolos" = the field boss appears now (also at the
     * wrong time of day) and the player is brought to the arena edge when further than 60 away; "sterrenwacht" = the dungeon rooms
     * are built if needed and the player (+ party within 24) goes in. Returns false for an unknown phase.
     */
    public static boolean start(ServerPlayer p, String fase) {
        if (!(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(sl)) {
            return false;
        }
        BlockPos naar;
        switch (fase) {
            case "wormelien" -> {
                forceerWormelien = true;
                if (Arenas.baas(sl, WORMELIEN) == null) {
                    Arenas.reset(sl.getServer(), WORMELIEN);
                }
                naar = Z6WezensPlekken.WORMELIEN_DEUR;
            }
            case "zoutkolos" -> {
                forceerZoutkolos = true;
                if (Arenas.baas(sl, ZOUTKOLOS) == null) {
                    Arenas.reset(sl.getServer(), ZOUTKOLOS);
                }
                naar = Z6WezensPlekken.ZOUTKOLOS_DEUR;
            }
            case "sterrenwacht" -> {
                Z6Bouw.sterrenwacht(sl);
                naar = Z6WezensPlekken.KERKER_AANKOMST;
            }
            default -> {
                return false;
            }
        }
        if (p.position().distanceToSqr(naar.getX(), naar.getY(), naar.getZ()) > 60 * 60 || fase.equals("sterrenwacht")) {
            for (ServerPlayer q : sl.players()) {
                if (q == p || q.distanceTo(p) < 24 && nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen.zelfdeParty(p, q)) {
                    sl.getChunk(naar);
                    q.teleportTo(sl, naar.getX() + 0.5, naar.getY() + 0.1, naar.getZ() + 0.5, Set.of(), 270, 0, true);
                }
            }
        }
        return true;
    }
}
