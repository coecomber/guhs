package nl.juiced.guhs.feature.vadsrijk.wezens;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.Anderen;

/**
 * The zone-1 spawners (CONTRACT_VADSRIJK 5). Surface areas go to kern ({@link Spawners#registreer}: heightmap). Caves and the
 * temple use this slice's adapter with a FLOOR SEARCH (kern's spawner always picks the top surface, which would put cave
 * mobs on the cave roof): same rules as kern (max alive, respawn ticks, elites with 1-2 affixes), but the spot is the first
 * floor found from y down to y-24 under the spawner's centre (then up to y+6), and "a player near" is measured flat (64 blocks, 40 up/down).
 * NOTITIES_WEZENS: kern can lift this as a spawner option.
 * <p>Balance: "lively, not crowded": 2-5 alive per spawner, 25-45 s respawn, 6 % elites (the Moederklodder is always one).
 */
public final class WezensSpawners {
    private static final List<Spawners.Spawner> VLOER = new ArrayList<>();
    private static final Map<String, Long> VOLGENDE = new HashMap<>();
    /** Spawners of the adapter that use the heightmap (mountain slopes: kern's spawner measures "player near" in 3D from y 65). */
    private static final java.util.Set<String> HOOGTE = new java.util.HashSet<>();
    public static final double ELITE = 0.06;
    private static int oppervlak;

    private WezensSpawners() {
    }

    private static void opp(String naam, BlockPos[] plekken, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int straal, int max, int respawn,
            int min, int maxLvl, Element e, double elite) {
        for (int i = 0; i < plekken.length; i++) {
            Spawners.Spawner s = new Spawners.Spawner("wezens:" + naam + "_" + i, plekken[i], straal, type, max, respawn, min, maxLvl, e, elite);
            Spawners.registreer(s);
            Anderen.WezensSpawnersMax.zet(s.id(), max);
            oppervlak++;
        }
    }

    private static void vloer(String naam, BlockPos[] plekken, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int straal, int max, int respawn,
            int min, int maxLvl, Element e, double elite) {
        for (int i = 0; i < plekken.length; i++) {
            Spawners.Spawner s = new Spawners.Spawner("wezens:" + naam + "_" + i, plekken[i], straal, type, max, respawn, min, maxLvl, e, elite);
            VLOER.add(s);
            Anderen.WezensSpawnersMax.zet(s.id(), max);
        }
    }

    /** The adapter with the heightmap (the mountain path: any height, "player near" measured flat). */
    private static void berg(String naam, BlockPos[] plekken, Supplier<? extends EntityType<? extends VadsrijkMob>> type, int straal, int max, int respawn,
            int min, int maxLvl, Element e, double elite) {
        vloer(naam, plekken, type, straal, max, respawn, min, maxLvl, e, elite);
        for (int i = 0; i < plekken.length; i++) {
            HOOGTE.add("wezens:" + naam + "_" + i);
        }
    }

    /** From WezensRegistry.register. */
    public static void registreer() {
        // Klodderweide, lv 1-6
        opp("klodder", WezensPlekken.KLODDER, WezensRegistry.KLODDER, 18, 4, 600, 1, 4, Element.WATER, ELITE);
        opp("bubbelklodder", WezensPlekken.BUBBELKLODDER, WezensRegistry.BUBBELKLODDER, 16, 3, 700, 2, 5, Element.WATER, ELITE);
        opp("stuiterklodder", WezensPlekken.STUITERKLODDER, WezensRegistry.STUITERKLODDER, 16, 3, 700, 3, 6, Element.WATER, ELITE);
        opp("moederklodder", WezensPlekken.MOEDERKLODDER, WezensRegistry.MOEDERKLODDER, 10, 1, 3000, 5, 6, Element.WATER, 1.0);
        // Sporenwoud, lv 6-13
        opp("hoedjesbol", WezensPlekken.HOEDJESBOL, WezensRegistry.HOEDJESBOL, 18, 4, 600, 6, 10, Element.NATUUR, ELITE);
        opp("sporenpuffer", WezensPlekken.SPORENPUFFER, WezensRegistry.SPORENPUFFER, 16, 3, 700, 7, 11, Element.NATUUR, ELITE);
        opp("zwamwachter", WezensPlekken.ZWAMWACHTER, WezensRegistry.ZWAMWACHTER, 14, 2, 900, 9, 13, Element.NATUUR, ELITE);
        opp("mosgeest", WezensPlekken.MOSGEEST, WezensRegistry.MOSGEEST, 16, 3, 700, 8, 13, Element.NATUUR, ELITE);
        // Druipgrotten, lv 12-18: surface + the caves (floor search)
        vloer("druipslijm", WezensPlekken.DRUIPSLIJM, WezensRegistry.DRUIPSLIJM, 12, 3, 700, 12, 15, Element.WATER, ELITE);
        vloer("kristalklodder_grot", WezensPlekken.KRISTALKLODDER_GROT, WezensRegistry.KRISTALKLODDER, 8, 2, 800, 15, 18, Element.WATER, ELITE);
        opp("kristalklodder", WezensPlekken.KRISTALKLODDER, WezensRegistry.KRISTALKLODDER, 14, 3, 700, 13, 16, Element.WATER, ELITE);
        opp("knaag", WezensPlekken.KNAAG, WezensRegistry.KNAAG, 14, 5, 800, 12, 16, Element.GEEN, ELITE);
        vloer("knaag_grot", WezensPlekken.KNAAG_GROT, WezensRegistry.KNAAG, 10, 4, 800, 13, 17, Element.GEEN, ELITE);
        opp("glimzwam", WezensPlekken.GLIMZWAM, WezensRegistry.GLIMZWAM, 12, 2, 900, 13, 17, Element.NATUUR, ELITE);
        vloer("glimzwam_grot", WezensPlekken.GLIMZWAM_GROT, WezensRegistry.GLIMZWAM, 6, 1, 900, 16, 18, Element.NATUUR, ELITE);
        // Zwamtempel, lv 18-20: the temple rooms (floor search) + the mountain path
        vloer("tempelridder", WezensPlekken.TEMPELRIDDER, WezensRegistry.TEMPELRIDDER, 6, 2, 900, 19, 20, Element.NATUUR, ELITE);
        vloer("sporenpriester", WezensPlekken.SPORENPRIESTER, WezensRegistry.SPORENPRIESTER, 5, 1, 1200, 19, 20, Element.NATUUR, ELITE);
        berg("tempelridder_pad", WezensPlekken.TEMPELRIDDER_PAD, WezensRegistry.TEMPELRIDDER, 14, 2, 900, 18, 19, Element.NATUUR, ELITE);
        berg("sporenpriester_pad", WezensPlekken.SPORENPRIESTER_PAD, WezensRegistry.SPORENPRIESTER, 12, 1, 1200, 18, 19, Element.NATUUR, ELITE);
    }

    public static int aantalOppervlak() {
        return oppervlak;
    }

    public static int aantalVloer() {
        return VLOER.size();
    }

    /** Every second (WezensRegistry), in the Vadsrijk level only. */
    public static void tick(ServerLevel sl) {
        long nu = sl.getGameTime();
        for (Spawners.Spawner s : VLOER) {
            BlockPos c = s.centrum();
            boolean hoogte = HOOGTE.contains(s.id());
            if (!sl.isLoaded(c) || !spelerInBuurt(sl, c, hoogte)) {
                continue;
            }
            int r = s.straal() + 16;
            int levend = sl.getEntitiesOfClass(VadsrijkMob.class, new AABB(c).inflate(r, hoogte ? 200 : 32, r), m -> s.id().equals(m.spawner) && !m.geplofd()).size();
            if (levend >= s.max()) {
                VOLGENDE.remove(s.id());
                continue;
            }
            long volgende = VOLGENDE.computeIfAbsent(s.id(), k -> nu + s.respawnTicks());
            if (nu < volgende && levend > 0) {
                continue;
            }
            VOLGENDE.put(s.id(), nu + s.respawnTicks());
            spawn(sl, s);
        }
    }

    private static boolean spelerInBuurt(ServerLevel sl, BlockPos c, boolean hoogte) {
        for (ServerPlayer p : sl.players()) {
            double dx = p.getX() - c.getX(), dz = p.getZ() - c.getZ();
            if (!p.isSpectator() && dx * dx + dz * dz < 64 * 64 && (hoogte || Math.abs(p.getY() - c.getY()) < 40)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The floor (solid below, two free blocks, no liquid) under y: first from y down to y-24 (inside a cave that is the cave
     * floor, never the roof above it), then from y+1 up to y+6 (the centre was a little too low), or null.
     */
    @Nullable
    public static BlockPos vloerPlek(ServerLevel sl, int x, int y, int z) {
        for (int dy = 0; dy >= -24; dy--) {
            BlockPos b = new BlockPos(x, y + dy, z);
            if (vloer(sl, b)) {
                return b;
            }
        }
        for (int dy = 1; dy <= 6; dy++) {
            BlockPos b = new BlockPos(x, y + dy, z);
            if (vloer(sl, b)) {
                return b;
            }
        }
        return null;
    }

    @Nullable
    private static BlockPos oppervlak(ServerLevel sl, int x, int z) {
        int y = sl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos b = new BlockPos(x, y, z);
        return y <= sl.getMinY() + 1 || !sl.getFluidState(b.below()).isEmpty() ? null : b;
    }

    private static boolean vloer(ServerLevel sl, BlockPos b) {
        return vrij(sl, b) && vrij(sl, b.above()) && !sl.getBlockState(b.below()).getCollisionShape(sl, b.below()).isEmpty()
                && sl.getFluidState(b).isEmpty() && sl.getFluidState(b.below()).isEmpty();
    }

    private static boolean vrij(ServerLevel sl, BlockPos b) {
        return sl.getBlockState(b).getCollisionShape(sl, b).isEmpty();
    }

    @Nullable
    public static VadsrijkMob spawn(ServerLevel sl, Spawners.Spawner s) {
        RandomSource r = sl.getRandom();
        for (int poging = 0; poging < 10; poging++) {
            double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * s.straal();
            int x = s.centrum().getX() + (int) Math.round(Math.cos(a) * d), z = s.centrum().getZ() + (int) Math.round(Math.sin(a) * d);
            if (!sl.isLoaded(new BlockPos(x, s.centrum().getY(), z))) {
                continue;
            }
            BlockPos plek = HOOGTE.contains(s.id()) ? oppervlak(sl, x, z) : vloerPlek(sl, x, s.centrum().getY(), z);
            if (plek == null) {
                continue;
            }
            VadsrijkMob m = s.type().get().create(sl, EntitySpawnReason.SPAWNER);
            if (m == null) {
                return null;
            }
            boolean elite = r.nextDouble() < s.eliteKans();
            Set<Affix> af = EnumSet.noneOf(Affix.class);
            if (elite) {
                Affix[] alle = Affix.values();
                af.add(alle[r.nextInt(alle.length)]);
                if (r.nextBoolean()) {
                    af.add(alle[r.nextInt(alle.length)]);
                }
            }
            int lvl = s.minLevel() + (s.maxLevel() > s.minLevel() ? r.nextInt(s.maxLevel() - s.minLevel() + 1) : 0);
            m.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, r.nextFloat() * 360, 0);
            m.stelIn(lvl, s.element(), elite, af);
            m.spawner = s.id();
            sl.addFreshEntity(m);
            return m;
        }
        return null;
    }
}
