package nl.juiced.guhs.feature.vadsrijk.grotkerker;

import java.util.List;
import java.util.stream.Stream;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.PluimKleur;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.GrotKerker.Run;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.GrotKerker.Staat;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.KerkerData.Kamer;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.KerkerData.Kerker;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.vint.RijdierBuit;
import nl.juiced.guhs.feature.vadsrijk.wezens.WezensRegistry;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * U-GROTKERKER: the cave runs. {@code data}: six caves (zone 6 only with u-z6wereld), two rooms + a boss room each, the gates of zone
 * 1-5 inside their caves, a boss def + pet + mount source per cave. {@code runEnReset}: a whole run in a small test cave (stands in
 * for the Zwamgrot): start, the packs exactly once (ticking again adds nothing, an old run's creature is refused on load), the gate
 * shuts, both rooms beaten -> the gate opens and the boss wakes, the boss beaten -> cleared + a clear counted, 10 min later the
 * cave sleeps again with nothing left over. {@code lootPerClear}: every clear a chest with coins + gear + ore, the pet's pity and the
 * mount rule (sure within 16 tries), nothing twice.
 * Run: {@code runGameTestServer -Pgt=VadsrijkGrotKerker}.
 */
public class VadsrijkGrotKerkerGameTests {
    private static final String BATCH = "vadsrijk_grotkerker";

    @GuhTest(template = "empty", batch = BATCH)
    public static void data(GameTestHelper helper) {
        helper.assertTrue(KerkerData.ALLE.size() == 6, "six cave runs in the data");
        helper.assertTrue(Kerkers.alle().size() == (Kerkers.zone6Aanwezig() ? 6 : 5), "zone 6 only with u-z6wereld: " + Kerkers.alle().size());
        for (Kerker k : KerkerData.ALLE) {
            helper.assertTrue(k.kamers().size() == 2, k.id() + ": two pack rooms");
            helper.assertTrue(k.baasLv() >= k.eliteLv() && k.eliteLv() >= k.maxLv() && k.maxLv() >= k.minLv(), k.id() + ": levels");
            for (Kamer a : k.kamers()) {
                helper.assertTrue(k.bevat(a.x() + 0.5, a.y() + 0.5, a.z() + 0.5), k.id() + ": room " + a.id() + " inside the cave box");
            }
            helper.assertTrue(k.bevat(k.baas().x() + 0.5, k.baas().y() + 0.5, k.baas().z() + 0.5), k.id() + ": boss room inside the box");
            helper.assertTrue(k.achter(k.baas().x() + 0.5, k.baas().y() + 0.5, k.baas().z() + 0.5), k.id() + ": the boss room is behind the gate");
            if (k.zone() < 6) {
                helper.assertTrue(k.poort().size() >= 3 && k.bord() != null, k.id() + ": a gate");
                for (int[] c : k.poort()) {
                    helper.assertTrue(k.bevat(c[0] + 0.5, c[1] + 0.5, c[2] + 0.5) && c[3] > c[1], k.id() + ": gate column in the cave");
                    helper.assertFalse(k.achter(c[0] + 0.5, c[1] + 0.5, c[2] + 0.5), k.id() + ": the gate is not behind itself");
                }
                for (Kamer a : k.kamers()) {
                    helper.assertFalse(k.achter(a.x() + 0.5, a.y() + 0.5, a.z() + 0.5), k.id() + ": pack room " + a.id() + " is in front of the gate");
                }
            }
            GrotBazen.Def d = GrotBazen.def(k.id());
            helper.assertTrue(d != null && GrotBazen.TYPES.get(k.id()) != null && GrotBazen.PAKKEN.get(k.id()) != null, k.id() + ": boss def, type, packs");
            helper.assertTrue(d.aanvallen().size() >= 3 && d.aanvallen().stream().anyMatch(a -> GrotBaas.minFase(a) == 2), k.id() + ": 3+ attacks, a phase-2 signature");
            helper.assertTrue(Huisdieren.van(d.pet()) != null, k.id() + ": its pet " + d.pet());
            helper.assertTrue(RijdierBuit.BRONNEN.containsKey(d.mountBron()), k.id() + ": its mount source " + d.mountBron());
            helper.assertTrue(GrotBazen.van(GrotBazen.TYPES.get(k.id()).get()) == d, k.id() + ": def by type");
        }
        helper.succeed();
    }

    /** A small test cave on the test's spot standing in for the Zwamgrot: two rooms, a boss room, a 3-column gate between. */
    static Kerker testKerker(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos o = helper.absolutePos(BlockPos.ZERO);
        for (int x = -1; x <= 13; x++) {
            for (int z = -1; z <= 13; z++) {
                sl.setBlock(o.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
                for (int y = 0; y < 5; y++) {
                    sl.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        int X = o.getX(), Y = o.getY(), Z = o.getZ();
        return new Kerker("zwamgrot", 1, X - 1, Y - 2, Z - 1, X + 13, Y + 8, Z + 13, 11, 13, 13, 14,
                List.of(new Kamer("kamer_1", X + 3, Y, Z + 3, 2), new Kamer("kamer_2", X + 9, Y, Z + 3, 2)),
                new Kamer("baaskamer", X + 6, Y, Z + 10, 3),
                List.of(new int[] {X + 5, Y, Z + 7, Y + 4}, new int[] {X + 6, Y, Z + 7, Y + 4}, new int[] {X + 7, Y, Z + 7, Y + 4}),
                new BlockPos(X + 6, Y, Z + 7),
                List.of(new int[] {X + 6, Y, Z + 10, 3}));
    }

    static List<VadsrijkMob> runWezens(ServerLevel sl, Kerker k) {
        return sl.getEntitiesOfClass(VadsrijkMob.class, new AABB(k.minX(), k.minY(), k.minZ(), k.maxX() + 1, k.maxY() + 1, k.maxZ() + 1),
                m -> m.entityTags().contains(GrotKerker.TAG) && !m.geplofd() && !m.isRemoved());
    }

    @GuhTest(template = "empty", batch = BATCH + "_run", timeoutTicks = 200)
    public static void runEnReset(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        Kerker k = testKerker(helper);
        Kerkers.TEST.put(k.id(), k);
        Run r = GrotKerker.run(k.id());
        GrotKerker.reset(sl, k, r, null);
        ServerPlayer p = GuhMockPlayer.of(helper);
        Spelers.data(p).extra.remove(GrotKerker.DATA);
        GrotKerker.TEST_KLAAR = true;
        helper.runAfterDelay(5, () -> {
            try {
                // start: the packs, exactly once
                GrotKerker.tick(sl, k, r);
                helper.assertTrue(r.staat == Staat.BEZIG, "the run started when a player stood in the cave: " + r.staat);
                int plan = r.plan.size();
                helper.assertTrue(plan == 3 + 4 + 1, "solo: 3 + 4 creatures + the elite, got " + plan);
                helper.assertTrue(runWezens(sl, k).size() == plan, "all spawned: " + runWezens(sl, k).size() + "/" + plan);
                for (int i = 0; i < 3; i++) {
                    GrotKerker.tick(sl, k, r);
                }
                helper.assertTrue(runWezens(sl, k).size() == plan, "ticking again spawns no doubles: " + runWezens(sl, k).size());
                // the gate is shut (barriers in its air)
                helper.assertTrue(r.dicht && sl.getBlockState(new BlockPos(k.poort().get(1)[0], k.poort().get(1)[1] + 1, k.poort().get(1)[2])).is(Blocks.BARRIER),
                        "the gate shut");
                // a creature of an older run comes back from disk: refused
                VadsrijkMob oud = WezensRegistry.GLIMZWAM.get().create(sl, EntitySpawnReason.SPAWNER);
                oud.snapTo(k.kamers().get(0).x() + 0.5, k.kamers().get(0).y(), k.kamers().get(0).z() + 0.5, 0, 0);
                oud.addTag(GrotKerker.TAG);
                oud.addTag(GrotKerker.runTag(k, r.nonce - 1));
                sl.addLegacyChunkEntities(Stream.of(oud));
                helper.assertTrue(sl.getEntity(oud.getUUID()) == null, "an old run's creature is refused on load");
                // both rooms beaten
                for (VadsrijkMob m : runWezens(sl, k)) {
                    m.verslagen(p);
                }
                helper.assertTrue(GrotKerker.alleGedaan(r), "every planned creature counted as beaten: " + r.verslagen);
                GrotKerker.tick(sl, k, r);
                helper.assertTrue(r.staat == Staat.BAAS, "both rooms clear: the boss wakes: " + r.staat);
                helper.assertFalse(r.dicht, "the gate opened");
                helper.assertTrue(sl.getBlockState(new BlockPos(k.poort().get(1)[0], k.poort().get(1)[1] + 1, k.poort().get(1)[2])).isAir(), "no barrier left");
                GrotBaas b = GrotKerker.baas(sl, k, r);
                helper.assertTrue(b != null, "the boss is there");
                GrotKerker.tick(sl, k, r);
                helper.assertTrue(sl.getEntitiesOfClass(GrotBaas.class, new AABB(k.minX(), k.minY(), k.minZ(), k.maxX() + 1, k.maxY() + 1, k.maxZ() + 1)).size() == 1,
                        "one boss, not two");
                // the boss beaten (the player did all the damage): cleared, a clear counted
                b.bijdragen().schade(p.getUUID(), 500);
                b.verslagen(p);
                helper.assertTrue(r.staat == Staat.KLAAR, "cleared: " + r.staat);
                helper.assertTrue(GrotKerker.clears(p, k.id()) == 1, "one clear counted");
                // 10 minutes later the cave falls asleep, nothing of the run left
                r.klaarOp -= GrotKerker.RESET_NA;
                GrotKerker.tick(sl, k, r);
                helper.assertTrue(r.staat == Staat.SLAAPT, "asleep again after 10 min: " + r.staat);
                helper.assertTrue(runWezens(sl, k).isEmpty(), "no run creature left");
                // and a new run starts again (repeatable), with a new nonce
                long oudNonce = 0;
                GrotKerker.tick(sl, k, r);
                helper.assertTrue(r.staat == Staat.BEZIG && r.nonce != oudNonce && runWezens(sl, k).size() == plan, "the next run");
                helper.assertTrue(r.dicht, "the gate re-closed for the next run");
                GrotKerker.reset(sl, k, r, null);
                helper.assertFalse(r.dicht, "reset opens the gate");
                helper.succeed();
            } finally {
                GrotKerker.reset(sl, k, r, null);
                GrotKerker.TEST_KLAAR = false;
                Kerkers.TEST.remove(k.id());
                Spelers.data(p).extra.remove(GrotKerker.DATA);
            }
        });
    }

    @GuhTest(template = "empty", batch = BATCH + "_buit")
    public static void lootPerClear(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Spelers.data(p).extra.remove(GrotKerker.DATA);
        Huisdieren.reset(p);
        Rijdieren.reset(p);
        try {
            for (int i = 0; i < 2; i++) {
                LootTabel.Resultaat r = KerkerLoot.kist("zwamgrot", 14, p, false);
                helper.assertTrue(r.munten() == 400 + 300, "zone 1: 700 brons, got " + r.munten());
                helper.assertTrue(r.items().size() >= 2, "a gear piece + ore every clear, got " + r.items().size());
            }
            GrotBazen.Def d = GrotBazen.def("zwamgrot");
            // the pet: pity
            Spelers.data(p).extra(GrotKerker.DATA).putInt("zwamgrot.petpech", KerkerLoot.PET_PITY);
            helper.assertTrue(KerkerLoot.rolPet(p, d) && Huisdieren.heeft(p, d.pet()), "the pet after its pity");
            helper.assertFalse(KerkerLoot.rolPet(p, d), "never twice");
            // the mount (the user's rule: 10 % + sure after 15): only with a Pluimguh, then within 16 tries
            helper.assertFalse(RijdierBuit.rol(p, d.mountBron()), "no cave colour without a Pluimguh");
            Rijdieren.geef(p, "pluimguh:honing");
            int pogingen = 0;
            while (!Rijdieren.heeftKleur(p, PluimKleur.GLIMZWAM) && pogingen < 20) {
                RijdierBuit.rol(p, d.mountBron());
                pogingen++;
            }
            helper.assertTrue(Rijdieren.heeftKleur(p, PluimKleur.GLIMZWAM) && pogingen <= RijdierBuit.PITY + 1, "the cave Pluimguh within 16 tries: " + pogingen);
            helper.assertFalse(RijdierBuit.rol(p, d.mountBron()), "owned: no more rolls");
            helper.succeed();
        } finally {
            Huisdieren.reset(p);
            Rijdieren.reset(p);
            Spelers.data(p).extra.remove(GrotKerker.DATA);
        }
    }
}
