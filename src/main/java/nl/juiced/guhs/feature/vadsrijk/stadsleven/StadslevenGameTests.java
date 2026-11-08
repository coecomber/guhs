package nl.juiced.guhs.feature.vadsrijk.stadsleven;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-LEVEN-STAD: town life. A little test town is built in the test level (a lawn, a pond, a bench cluster, a lantern, a roof
 * and a playground corner) and the manager is driven by the test ({@link StadsLeven#testModus}).
 * <ul>
 *   <li>{@code budget}: many planning passes with a player in town: every kind stays under its cap (VOL), the totals under the
 *       dimension caps, unique scenes (playground, cart, musician) exactly once, ZACHT halves, UIT spawns nothing, nobody near =
 *       everything gone, and no town-life entity is left over;</li>
 *   <li>{@code geenDubbelen}: a foreign town-life entity is refused, a "reload" (the manager forgets its scenes) is cleaned by the
 *       sweep, nothing is ever saved;</li>
 *   <li>{@code speeltuin}: the playground's children really play (seesaw ends move opposite, swings swing, the merry-go-round
 *       keeps its radius, a slider climbs up and slides down), 9 children at VOL / 5 at ZACHT, none at night;</li>
 *   <li>{@code stedenEnSpeeltuinen}: the towns map to their areas and every playground in speeltuinen.txt lies in its town.</li>
 * </ul>
 * Run: {@code runGameTestServer -Pgt=Stadsleven}.
 */
public class StadslevenGameTests {
    private static final Steden.Stad TEST = new Steden.Stad("test", 1, Set.of(), false);

    /** The test town: a 28 x 28 lawn round the helper origin, with everything the scenes look for. */
    private static BlockPos bouwStad(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = helper.absolutePos(new BlockPos(0, 0, 0)).offset(-12, 0, -12);
        for (int x = 0; x < 28; x++) {
            for (int z = 0; z < 28; z++) {
                sl.setBlock(o.offset(x, -1, z), Blocks.DIRT.defaultBlockState(), 2);
                sl.setBlock(o.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 2);
                for (int y = 1; y < 8; y++) {
                    sl.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        for (int x = 18; x < 25; x++) {   // the pond
            for (int z = 2; z < 9; z++) {
                sl.setBlock(o.offset(x, 0, z), Blocks.WATER.defaultBlockState(), 2);
            }
        }
        Block bank = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("guhs", "guh_bankje_roze"));
        for (int x = 3; x <= 5; x++) {   // a terrace: three benches
            BlockState b = bank.defaultBlockState();
            if (b.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
                b = b.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
            }
            sl.setBlock(o.offset(x, 1, 4), b, 2);
        }
        sl.setBlock(o.offset(10, 1, 3), Blocks.LANTERN.defaultBlockState(), 2);   // a lantern for the birds
        for (int x = 14; x <= 16; x++) {   // a little roof on posts (for the cat)
            for (int z = 12; z <= 14; z++) {
                sl.setBlock(o.offset(x, 4, z), Blocks.OAK_PLANKS.defaultBlockState(), 2);
            }
        }
        for (int y = 1; y <= 3; y++) {
            sl.setBlock(o.offset(14, y, 12), Blocks.OAK_FENCE.defaultBlockState(), 2);
            sl.setBlock(o.offset(16, y, 14), Blocks.OAK_FENCE.defaultBlockState(), 2);
        }
        return o;
    }

    private static void begin(GameTestHelper helper, BlockPos o) {
        StadsLeven.allesWeg();
        StadsLeven.testModus = true;
        StadsLeven.testNacht = false;
        StadsLeven.testRegen = false;
        StadsLeven.devNiveau = null;
        Steden.testStad = TEST;
        Steden.testDoos = new AABB(o.getX(), o.getY() - 2, o.getZ(), o.getX() + 28, o.getY() + 10, o.getZ() + 28);
        Speeltuin.zet("test", o.offset(1, 0, 15));
        Plekken.wis();
        Plekken.budget = 100;
        Plekken.verken(helper.getLevel(), Vec3.atCenterOf(o.offset(14, 1, 14)), 3);
    }

    /** Runs the body once every chunk of the test town is really entity-ticking (checked every 5 ticks, at most 200 ticks). */
    private static void wacht(GameTestHelper helper, BlockPos o, int keer, Runnable body) {
        boolean klaar = true;
        for (int x = -1; x <= 29 && klaar; x += 15) {
            for (int z = -1; z <= 29 && klaar; z += 15) {
                klaar = nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.klaar(helper.getLevel(), o.offset(Math.max(0, x), 0, Math.max(0, z)));
            }
        }
        if (klaar || keer >= 40) {
            body.run();
        } else {
            helper.runAfterDelay(5, () -> wacht(helper, o, keer + 1, body));
        }
    }

    /** The whole test town entity-ticking (or not any more). */
    private static void forceer(GameTestHelper helper, BlockPos o, boolean aan) {
        for (int cx = (o.getX() >> 4) - 1; cx <= (o.getX() + 28 >> 4) + 1; cx++) {
            for (int cz = (o.getZ() >> 4) - 1; cz <= (o.getZ() + 28 >> 4) + 1; cz++) {
                helper.getLevel().setChunkForced(cx, cz, aan);
            }
        }
    }

    private static void einde(GameTestHelper helper, BlockPos o) {
        forceer(helper, o, false);
        Speeltuin.vergeet("test");
        StadsLeven.allesWeg();
        StadsLeven.testModus = false;
        StadsLeven.testNacht = null;
        StadsLeven.testRegen = null;
        Steden.testStad = null;
        Steden.testDoos = null;
        Plekken.wis();
    }

    private static List<StadsLeven.Kijker> kijker(BlockPos o, StadsLeven.Niveau n) {
        return List.of(new StadsLeven.Kijker(Vec3.atBottomCenterOf(o.offset(14, 1, 14)), TEST, n));
    }

    private static int tel(ServerLevel sl, AABB doos) {
        return sl.getEntitiesOfClass(Entity.class, doos, StadsLeven::isLeven).size();
    }

    private static void draai(ServerLevel sl, BlockPos o, StadsLeven.Niveau n, int passes) {
        for (int i = 0; i < passes; i++) {
            StadsLeven.plan(sl, kijker(o, n), List.of(Vec3.atBottomCenterOf(o.offset(14, 1, 14))));
            for (int t = 0; t < 10; t++) {
                StadsLeven.tickGroepen(sl);
            }
        }
    }

    @GuhTest(template = "empty", batch = "vadsrijk_stadsleven_budget", timeoutTicks = 600)
    public static void budget(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = bouwStad(helper);
        AABB doos = new AABB(o).inflate(80);
        forceer(helper, o, true);
        wacht(helper, o, 0, () -> {   // (once the forced chunks are entity-ticking; the body runs in one go)
            begin(helper, o);
            try {
                draai(sl, o, StadsLeven.Niveau.VOL, 40);
                Map<Groep.Soort, Integer> tel = StadsLeven.tel(TEST);
                for (Groep.Soort s : Groep.Soort.values()) {
                    helper.assertTrue(tel.getOrDefault(s, 0) <= s.max(StadsLeven.Niveau.VOL), s + " over its cap: " + tel.get(s));
                }
                helper.assertTrue(tel.getOrDefault(Groep.Soort.DIERTJE, 0) >= 4, "critters scurry about: " + tel);
                helper.assertTrue(tel.getOrDefault(Groep.Soort.SPEELTUIN, 0) == 1, "exactly one playground: " + tel);
                helper.assertTrue(tel.getOrDefault(Groep.Soort.KAR, 0) <= 1 && tel.getOrDefault(Groep.Soort.MUZIKANT, 0) <= 1, "unique cart/musician");
                helper.assertTrue(tel.getOrDefault(Groep.Soort.TERRAS, 0) >= 1, "a terrace on the benches or a street chat: " + tel);
                int ents = 0, guhs = 0;
                for (Groep g : StadsLeven.GROEPEN.values()) {
                    ents += g.leden.size();
                    guhs += g.guhs();
                }
                helper.assertTrue(ents <= StadsLeven.MAX_ENTITEITEN && guhs <= StadsLeven.MAX_GUHS, "dimension caps: " + ents + " / " + guhs);
                helper.assertTrue(tel(sl, doos) == ents, "every town-life entity belongs to a scene: " + tel(sl, doos) + " vs " + ents);
                int blobs = 0, muizen = 0;
                for (Groep g : StadsLeven.GROEPEN.values()) {
                    if (g instanceof Beestjes.Diertje d) {
                        if (d.blob) {
                            blobs++;
                        } else {
                            muizen++;
                        }
                    }
                }
                helper.assertTrue(muizen >= 1, "pieppiepmuisjes in town: " + muizen + " mice, " + blobs + " blobs");

                // ZACHT: half
                StadsLeven.allesWeg();
                draai(sl, o, StadsLeven.Niveau.ZACHT, 40);
                tel = StadsLeven.tel(TEST);
                for (Groep.Soort s : Groep.Soort.values()) {
                    helper.assertTrue(tel.getOrDefault(s, 0) <= s.max(StadsLeven.Niveau.ZACHT), "ZACHT: " + s + " over its cap: " + tel.get(s));
                }
                // UIT: nothing
                StadsLeven.allesWeg();
                draai(sl, o, StadsLeven.Niveau.UIT, 10);
                helper.assertTrue(StadsLeven.GROEPEN.isEmpty() && tel(sl, doos) == 0, "UIT: no town life at all");
                // VOL again, then the player leaves: all gone
                draai(sl, o, StadsLeven.Niveau.VOL, 10);
                helper.assertTrue(!StadsLeven.GROEPEN.isEmpty(), "back again");
                StadsLeven.plan(sl, List.of(), List.of(Vec3.atCenterOf(o).add(500, 0, 500)));
                StadsLeven.tickGroepen(sl);
                helper.assertTrue(StadsLeven.GROEPEN.isEmpty() && tel(sl, doos) == 0, "nobody near: everything gone, nothing left behind");
            } finally {
                einde(helper, o);
            }
            helper.succeed();
        });
    }

    @GuhTest(template = "empty", batch = "vadsrijk_stadsleven_dubbel", timeoutTicks = 500)
    public static void geenDubbelen(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = bouwStad(helper);
        AABB doos = new AABB(o).inflate(80);
        forceer(helper, o, true);
        wacht(helper, o, 0, () -> {   // (once the forced chunks are entity-ticking; the body runs in one go)
            begin(helper, o);
            try {
                // a town-life entity nobody spawned (as if from disk) is refused
                LevenProp vreemd = StadslevenRegistry.PROP.get().create(sl, EntitySpawnReason.EVENT);
                vreemd.snapTo(o.getX() + 3, o.getY() + 1, o.getZ() + 3, 0, 0);
                helper.assertTrue(!sl.addFreshEntity(vreemd), "a foreign town-life entity is refused");
                LevenGuh vreemdeGuh = StadslevenRegistry.GUH.get().create(sl, EntitySpawnReason.EVENT);
                vreemdeGuh.snapTo(o.getX() + 3, o.getY() + 1, o.getZ() + 3, 0, 0);
                helper.assertTrue(!sl.addFreshEntity(vreemdeGuh), "a foreign town guh is refused");
                helper.assertTrue(!vreemd.shouldBeSaved() || vreemd.getType().canSerialize() == false, "never saved (noSave type)");

                draai(sl, o, StadsLeven.Niveau.VOL, 20);
                int ents = tel(sl, doos);
                helper.assertTrue(ents > 0, "town life came");
                // the same unique scene twice? never
                Groep tweede = Speeltuin.maak(sl, TEST, Vec3.atCenterOf(o.offset(14, 1, 14)), sl.getRandom(), StadsLeven.Niveau.VOL);
                helper.assertTrue(tweede == null, "a second playground scene is refused");
                // "reload": the manager forgets its scenes, the entities stay -> the sweep removes them, the next pass rebuilds
                List<Groep> oud = new ArrayList<>(StadsLeven.GROEPEN.values());
                StadsLeven.GROEPEN.clear();
                int weg = StadsLeven.veeg(sl);
                helper.assertTrue(weg == ents && tel(sl, doos) == 0, "the sweep removes every orphan: " + weg + " of " + ents);
                for (Groep g : oud) {
                    for (Entity e : g.leden) {
                        helper.assertTrue(e.isRemoved(), "old entity gone");
                    }
                }
                draai(sl, o, StadsLeven.Niveau.VOL, 20);
                int n = 0;
                for (Groep g : StadsLeven.GROEPEN.values()) {
                    n += g.leden.size();
                }
                helper.assertTrue(tel(sl, doos) == n, "after the reload: exactly the scenes' entities, no doubles: " + tel(sl, doos) + " vs " + n);
            } finally {
                einde(helper, o);
            }
            helper.succeed();
        });
    }

    @GuhTest(template = "empty", batch = "vadsrijk_stadsleven_speeltuin", timeoutTicks = 500)
    public static void speeltuin(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = bouwStad(helper);
        forceer(helper, o, true);
        wacht(helper, o, 0, () -> {   // (once the forced chunks are entity-ticking; the body runs in one go)
            begin(helper, o);
            try {
                Speeltuin s = Speeltuin.maak(sl, TEST, Vec3.atCenterOf(o.offset(14, 1, 14)), sl.getRandom(), StadsLeven.Niveau.VOL);
                BlockPos hk = Speeltuin.van("test");
                helper.assertTrue(s != null, "the playground scene starts (corner " + hk + ", klaar " + (hk != null
                        && nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.klaar(sl, hk)) + ", al " + StadsLeven.heeft("speeltuin:test") + ", scenes "
                        + StadsLeven.GROEPEN.keySet() + ")");
                StadsLeven.GROEPEN.put(s.sleutel, s);
                helper.assertTrue(s.kinderenAantal() == 9 && s.ouder != null, "VOL: 9 children + a grown-up: " + s.kinderenAantal());
                BlockPos h = s.hoek;
                double minA = 99, maxA = -99, maxGlij = -99, minGlij = 99, molenR = 0;
                boolean tegengesteld = true;
                for (int t = 0; t < 160; t++) {
                    StadsLeven.tickGroepen(sl);
                    double a = s.wipA.getY(), b = s.wipB.getY(), mid = h.getY() + Speeltuin.WIP_Y + 0.12;
                    if (Math.abs(a - mid) > 0.05) {
                        tegengesteld &= Math.signum(a - mid) == -Math.signum(b - mid);
                    }
                    double z = s.schommelKind[0].getZ() - (h.getZ() + Speeltuin.SCHOMMEL_Z);
                    minA = Math.min(minA, z);
                    maxA = Math.max(maxA, z);
                    LevenGuh g = s.glijers.get(0);
                    maxGlij = Math.max(maxGlij, g.getY() - h.getY());
                    minGlij = Math.min(minGlij, g.getY() - h.getY());
                    double dx = s.molenA.getX() - (h.getX() + Speeltuin.MOLEN_X), dz = s.molenA.getZ() - (h.getZ() + Speeltuin.MOLEN_Z);
                    molenR = Math.max(molenR, Math.abs(Math.sqrt(dx * dx + dz * dz) - Speeltuin.MOLEN_R));
                }
                helper.assertTrue(tegengesteld, "the seesaw ends go opposite ways");
                helper.assertTrue(minA < -0.8 && maxA > 0.8, "the swing swings: " + minA + " .. " + maxA);
                helper.assertTrue(molenR < 0.05, "the merry-go-round keeps its children on the ring: " + molenR);
                helper.assertTrue(maxGlij > 2.9 && minGlij < 1.1, "a child climbs up (F+3) and comes down: " + minGlij + " .. " + maxGlij);
                // night: the children go home (nobody watching), the equipment stays
                StadsLeven.testNacht = true;
                StadsLeven.tickGroepen(sl);
                helper.assertTrue(s.kinderenAantal() == 0 && !s.wip.isRemoved(), "at night the playground sleeps");
                // ZACHT: fewer children
                StadsLeven.testNacht = false;
                StadsLeven.allesWeg();
                Speeltuin z = Speeltuin.maak(sl, TEST, Vec3.atCenterOf(o.offset(14, 1, 14)), sl.getRandom(), StadsLeven.Niveau.ZACHT);
                helper.assertTrue(z != null && z.kinderenAantal() == 5, "ZACHT: 5 children: " + (z == null ? -1 : z.kinderenAantal()));
                if (z != null) {
                    z.weg();
                }
            } finally {
                einde(helper, o);
            }
            helper.succeed();
        });
    }

    @GuhTest(template = "empty", batch = "vadsrijk_stadsleven_steden")
    public static void stedenEnSpeeltuinen(GameTestHelper helper) {
        for (Steden.Stad s : Steden.ALLE.values()) {
            for (String g : s.gebieden()) {
                helper.assertTrue(Steden.vanGebied(g) == s, "area " + g + " -> " + s.id());
            }
        }
        Map<String, BlockPos> tuinen = Speeltuin.alle();
        for (String id : new String[] {"knuffelkamp", "zwamzoet", "kandijkom", "honingtak", "lampionhaven", "hoogvads"}) {
            helper.assertTrue(tuinen.containsKey(id), "a playground in " + id + " (speeltuinen.txt)");
        }
        for (Map.Entry<String, BlockPos> e : tuinen.entrySet()) {
            Steden.Stad s = Steden.van(e.getKey());
            helper.assertTrue(s != null, "playground of a known town: " + e.getKey());
            BlockPos h = e.getValue();
            Vec3 m = new Vec3(h.getX() + 6.5, h.getY() + 1, h.getZ() + 5);
            Gebieden.Gebied g = Gebieden.op(m.x, m.y, m.z);
            if (g != null && !g.id().equals("ring")) {   // (zone 4-6 areas are registered by their own slices: only checked when present)
                helper.assertTrue(s.gebieden().contains(g.id()) || Steden.op(m) == s, "playground " + e.getKey() + " lies in its town (area " + g.id() + ")");
            }
        }
        helper.succeed();
    }
}
