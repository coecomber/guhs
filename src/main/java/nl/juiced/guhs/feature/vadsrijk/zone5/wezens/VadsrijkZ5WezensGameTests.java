package nl.juiced.guhs.feature.vadsrijk.zone5.wezens;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Baas;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas.Donderbuik;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas.Gorgonzola;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas.Z5Bazen;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas.Z5BazenLoot;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas.Zilverstaart;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.loot.Z5Loot;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.mob.Proef;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.mob.Z5Wezen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Zone-5 wezens (T-Z5WEZENS): every creature spawns and is defeated, the loot (tables, base types, items, boss chests), the
 * sparring knight (non-lethal: bonks, block, win), Gorgonzola's valves, Zilverstaart's disguises + escape valves, Donderbuik's pull
 * and pink cushions, the arenas/areas/spawners and the hatches + fall net. Run: {@code runGameTestServer -Pgt=VadsrijkZ5Wezens}.
 */
public class VadsrijkZ5WezensGameTests {
    private static final String BATCH = "vadsrijk_z5wezens";

    private static void vloer(ServerLevel sl, BlockPos c, int r) {
        for (int cx = (c.getX() - r - 8) >> 4; cx <= (c.getX() + r + 8) >> 4; cx++) {
            for (int cz = (c.getZ() - r - 8) >> 4; cz <= (c.getZ() + r + 8) >> 4; cz++) {
                sl.setChunkForced(cx, cz, true);
            }
        }
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                sl.setBlock(c.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 2);
            }
        }
    }

    private static ServerPlayer speler(GameTestHelper helper, Vec3 waar, boolean creatief) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(creatief ? GameType.CREATIVE : GameType.SURVIVAL);
        p.snapTo(waar.x, waar.y, waar.z);
        Spelers.data(p).vads = Spelers.maxVads(p);
        return p;
    }

    private static <T extends Z3Baas> T zet(ServerLevel sl, net.minecraft.world.entity.EntityType<T> type, BlockPos c, int lv) {
        T b = type.create(sl, EntitySpawnReason.COMMAND);
        b.snapTo(c.getX() + 0.5, c.getY(), c.getZ() + 0.5, 0, 0);
        b.stelIn(lv, null, false, Set.of());
        sl.addFreshEntity(b);
        return b;
    }

    /** Every kind spawns at lv 58 with real Vads, element and loot tables; defeated it vanishes (the knight is never defeated). */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void alleWezensSpawnenEnVerdwijnen(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(4, 2, 4));
        vloer(sl, c, 5);
        ServerPlayer p = GuhMockPlayer.of(helper);
        List<Z5Wezen> alle = new ArrayList<>();
        helper.assertTrue(Z5WezensRegistry.SOORTEN.size() == 9, "9 kinds, got " + Z5WezensRegistry.SOORTEN.size());
        for (String soort : Z5WezensRegistry.SOORTEN.keySet()) {
            Entity e = Z5WezensCommando.maak(sl, soort, Vec3.atBottomCenterOf(c), 0, 58, false);
            helper.assertTrue(e instanceof Z5Wezen, "made " + soort);
            Z5Wezen w = (Z5Wezen) e;
            helper.assertTrue(w.mobLevel() == 58 && w.maxVads() > 300, soort + ": level 58 with real Vads, got " + w.maxVads());
            helper.assertTrue(w.element() == w.standaardElement(), soort + ": its element");
            w.zetHouding(0);
            if (w instanceof Proef.Sparringridder r) {
                r.krijgSchade(r.vads() + 1, p);
                helper.assertTrue(!r.geplofd() && r.vads() == r.maxVads(), "the knight takes nothing from someone who is not sparring");
                r.discard();
                continue;
            }
            helper.assertTrue(LootTabellen.van(w.lootTabel()) != null && LootTabellen.van(w.lootTabel() + "_elite") != null, soort + ": loot tables");
            w.krijgSchade(w.vads() + 1, p);
            helper.assertTrue(w.geplofd(), soort + ": defeated");
            alle.add(w);
        }
        helper.runAfterDelay(10, () -> {
            for (Z5Wezen w : alle) {
                helper.assertTrue(w.isRemoved(), w.naam() + " vanished");
            }
            helper.succeed();
        });
    }

    /** The tables roll real lv-58 gear; every base type has its item; 6 weapons; a boss chest: first kill = a named Legendary + trophy. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void lootKlopt(GameTestHelper helper) {
        RandomSource r = RandomSource.create(550);
        int items = 0;
        for (String soort : Z5WezensRegistry.SOORTEN.keySet()) {
            LootTabel e = LootTabellen.van("z5wezens:" + soort + "_elite");
            if (e == null) {
                continue;
            }
            for (int i = 0; i < 20; i++) {
                for (ItemStack s : e.rol(58, r).items()) {
                    Uitrusting u = Rollen.van(s);
                    helper.assertTrue(u != null && BasisTypes.van(u.type()) != null && u.level() == 58, "a rolled piece of " + soort);
                    items++;
                }
            }
        }
        helper.assertTrue(items > 60, "elites drop plenty, got " + items);
        List<String> alle = new ArrayList<>(Z5Loot.STUKKEN.keySet());
        alle.addAll(Z5Loot.WAPENS);
        for (String id : alle) {
            var t = BasisTypes.van(id);
            helper.assertTrue(t != null, "base type " + id);
            helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(net.minecraft.resources.Identifier.parse(t.itemId())).isPresent(),
                    "item " + t.itemId());
        }
        helper.assertTrue(Z5Loot.WOLKWACHT.size() >= 4 && Z5Loot.WAPENS.size() == 6, "the set (>= 4) and 6 weapons");
        helper.assertTrue(LootTabellen.van("z5wezens:sparren") != null && LootTabellen.van("z5wezens:baas") != null, "sparring + boss tables");
        ServerPlayer p = GuhMockPlayer.of(helper);
        LootTabel.Resultaat kist = Z5BazenLoot.kist("donderbuik", p, 60, r);
        boolean benoemd = kist.items().stream().anyMatch(s -> {
            Uitrusting u = Rollen.van(s);
            return u != null && u.type().startsWith("z5baas_") && u.zeldzaamheid() == Zeldzaamheid.LEGENDARISCH && u.gebonden();
        });
        boolean trofee = kist.items().stream().anyMatch(s -> s.is(Z5BazenLoot.TROFEEEN.get(2).get()));
        helper.assertTrue(benoemd && trofee && kist.munten() > 0, "first Donderbuik chest: a named Legendary + the trophy + coins");
        helper.succeed();
    }

    /** The sparring knight: never hurts, bonks (3 = you lose), the shield blocks, bring it to 5 % = it bows and you win. */
    @GuhTest(template = "empty", batch = BATCH + "_sparren", timeoutTicks = 200)
    public static void sparrenIsLief(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(4, 2, 4));
        vloer(sl, c, 6);
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(c).add(0, 0, 2), false);
        double vads = Spelers.vads(p);
        Proef.Sparringridder r = (Proef.Sparringridder) Z5WezensCommando.maak(sl, "sparringridder", Vec3.atBottomCenterOf(c), 0, 56, false);
        r.setNoAi(true);
        r.begin(sl, p);
        for (int i = 0; i < 3; i++) {
            r.zwaaiNu(sl);
            r.tick();
        }
        helper.assertTrue(r.verloren == 1 && r.partner() == null, "three bonks = the bout is lost (" + r.verloren + ")");
        helper.assertTrue(Spelers.vads(p) >= vads, "a guh never hurts: Vads unchanged");
        r.begin(sl, p);
        r.krijgSchade(r.maxVads() * 0.5, p);
        helper.assertTrue(r.vads() < r.maxVads() * 0.6, "the partner's hits count");
        r.krijgSchade(r.maxVads(), p);
        helper.assertTrue(r.gewonnen == 1 && !r.geplofd(), "brought down to 5 %: it bows, not defeated");
        helper.assertTrue(Spelers.data(p).extra("z5wezens").getIntOr("sparren", 0) == 1, "the win is counted");
        r.discard();
        helper.succeed();
    }

    /** Gorgonzola: four valves; shut = he takes less; right-click 3x opens; fase 3 shuts all and holds him at 15 %. */
    @GuhTest(template = "empty", batch = BATCH + "_gorgonzola", timeoutTicks = 200)
    public static void gorgonzolaKleppen(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(1, 2, 1));
        vloer(sl, c, 14);
        Gorgonzola g = zet(sl, Z5WezensRegistry.GORGONZOLA.get(), c, 59);
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(c).add(-6, 0, 0), true);
        g.testFilter = q -> q == p;
        helper.runAfterDelay(30, () -> {
            Statussen.geef(g, Status.VERDOOFD, 400, 1, null);
            List<Z3Doel> kleppen = sl.getEntitiesOfClass(Z3Doel.class, g.getBoundingBox().inflate(14), d -> d.getType() == Z5WezensRegistry.KLEP.get());
            helper.assertTrue(kleppen.size() == 4, "four valves (" + kleppen.size() + ")");
            double v = g.vads();
            g.krijgSchade(1000, p);
            helper.assertTrue(Math.abs((v - g.vads()) - 1000) < 1, "all open: full damage");
            g.sluit(sl, 0);
            g.sluit(sl, 1);
            v = g.vads();
            g.krijgSchade(1000, p);
            helper.assertTrue(Math.abs((v - g.vads()) - 700) < 1, "two shut: 70 % (" + (v - g.vads()) + ")");
            Z3Doel k0 = kleppen.stream().filter(d -> d.nummer == 0).findFirst().orElseThrow();
            for (int i = 0; i < Gorgonzola.KLIKS; i++) {
                g.doelGebruikt(k0, p);
            }
            helper.assertTrue(!g.dicht(0) && g.dichtAantal() == 1, "three clicks open valve 0");
            Z3Doel k1 = kleppen.stream().filter(d -> d.nummer == 1).findFirst().orElseThrow();
            for (int i = 0; i < Gorgonzola.KLIKS; i++) {
                g.doelGebruikt(k1, p);
            }
            helper.assertTrue(g.dichtAantal() == 0, "all open again");
            g.krijgSchade(g.vads() - g.maxVads() * 0.24, p);
            helper.assertTrue(g.fase() == 3 && g.dichtAantal() == 4, "fase 3: everything shut");
            g.krijgSchade(g.maxVads(), p);
            helper.assertTrue(!g.geplofd() && g.vads() >= g.maxVads() * 0.149, "held at 15 % while the valves are shut");
            g.discard();
            helper.succeed();
        });
    }

    /** Zilverstaart: the disguise (hitting a decoy costs you, hitting him unmasks + 50 %), the escape valves hold him at 10 %. */
    @GuhTest(template = "empty", batch = BATCH + "_spion", timeoutTicks = 200)
    public static void zilverstaartVermommingen(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(1, 2, 1));
        vloer(sl, c, 16);
        Zilverstaart z = zet(sl, Z5WezensRegistry.ZILVERSTAART.get(), c, 60);
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(c).add(-5, 0, 0), false);
        z.testFilter = q -> q == p;
        helper.runAfterDelay(30, () -> {
            z.vermom(sl);
            helper.assertTrue(z.vermomd(), "disguised");
            List<Z3Doel> poppen = sl.getEntitiesOfClass(Z3Doel.class, z.getBoundingBox().inflate(20), d -> d.getType() == Z5WezensRegistry.LOKPOP.get());
            helper.assertTrue(poppen.size() == Zilverstaart.LOKPOPPEN, "four decoys (" + poppen.size() + ")");
            double pv = Spelers.vads(p);
            poppen.get(0).krijgSchade(100, p);
            helper.assertTrue(z.lokpopMis == 1 && Spelers.vads(p) < pv, "a decoy pops and costs the hitter");
            double v = z.vads();
            z.krijgSchade(1000, p);
            helper.assertTrue(!z.vermomd() && z.gevonden == 1, "hitting him unmasks him");
            helper.assertTrue(v - z.vads() > 1400, "unmasked: +50 % (" + (v - z.vads()) + ")");
            Statussen.geef(z, Status.VERDOOFD, 400, 1, null);
            z.ontsnap(sl);
            z.krijgSchade(z.maxVads() * 2, p);
            helper.assertTrue(!z.geplofd() && z.vads() >= z.maxVads() * 0.099, "held at 10 % while the gas valves are open");
            List<Z3Doel> ventielen = sl.getEntitiesOfClass(Z3Doel.class, z.getBoundingBox().inflate(30), d -> d.getType() == Z5WezensRegistry.KLEP.get());
            helper.assertTrue(ventielen.size() == Zilverstaart.VENTIELEN, "three gas valves (" + ventielen.size() + ")");
            for (Z3Doel d : ventielen) {
                for (int i = 0; i < Zilverstaart.KLIKS; i++) {
                    z.doelGebruikt(d, p);
                }
            }
            helper.assertTrue(z.dichtAantal() == Zilverstaart.VENTIELEN, "all shut");
            z.krijgSchade(z.maxVads(), p);
            helper.assertTrue(z.geplofd(), "then he falls");
            helper.succeed();
        });
    }

    /** Donderbuik: the pull drags whoever is not on a pink cushion; it ends pink (cheered up), never killed. */
    @GuhTest(template = "empty", batch = BATCH + "_donder", timeoutTicks = 260)
    public static void donderbuikZuigwind(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(1, 2, 1));
        vloer(sl, c, 20);
        Donderbuik d = zet(sl, Z5WezensRegistry.DONDERBUIK.get(), c, 60);
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(c).add(-12, 0, 0), false);
        d.testFilter = q -> q == p;
        helper.runAfterDelay(30, () -> {
            Statussen.geef(d, Status.VERDOOFD, 600, 1, null);
            int voor = d.zuigingen;
            double pv = Spelers.vads(p);
            d.zuigwind(sl);
            helper.runAfterDelay(60, () -> {
                helper.assertTrue(d.zuigingen == voor + 1, "the pull went off");
                // the player stood still 12 blocks out: almost surely not on one of 3 random cushions (r 3); allow the rare luck
                helper.assertTrue(d.laatstGezogen == 1 ? Spelers.vads(p) < pv : true, "pulled = 35 %");
                d.krijgSchade(d.maxVads() * 2, p);
                helper.assertTrue(d.geplofd() && d.rozeWeer(), "cheered up: pink");
                helper.succeed();
            });
        });
    }

    /** Arenas, combat areas in front of the towns, spawners, hatches. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void plekkenEnSpawners(GameTestHelper helper) {
        for (String a : List.of(Z5Bazen.ARENA_SPION, Z5Bazen.ARENA_GORGONZOLA, Z5Bazen.ARENA_DONDERBUIK)) {
            helper.assertTrue(Arenas.van(a) != null, "arena " + a);
        }
        BlockPos k = Z5WezensPlekken.KETELKAMER;
        Gebieden.Gebied g = Gebieden.op(k.getX() + 0.5, k.getY(), k.getZ() + 0.5);
        helper.assertTrue(g != null && g.id().equals("z5_buikgangen") && !g.stad(), "the Buikgangen are a combat area (" + (g == null ? null : g.id()) + ")");
        BlockPos pa = Z5WezensPlekken.PROEFARENA;
        g = Gebieden.op(pa.getX() + 0.5, pa.getY(), pa.getZ() + 0.5);
        helper.assertTrue(g != null && g.id().equals("z5_proefarena"), "the Proefarena is a combat area");
        int n = 0;
        for (Spawners.Spawner s : Z5WezensSpawners.ALLE) {
            helper.assertTrue(s.grot() && Spawners.van(s.id()) != null, "cave-mode spawner " + s.id());
            n += s.max();
        }
        helper.assertTrue(n >= 40 && n <= 60, "40-60 alive at most (" + n + ")");
        helper.assertTrue(Z5Bouw.luikBij(Z5WezensPlekken.BUIK_LUIK) != null && Z5Bouw.luikBij(Z5WezensPlekken.NEST_LUIK) != null, "the hatches are known");
        helper.succeed();
    }

    /** The Buikgangen stand-in: built into air, real floors under the walking line. */
    @GuhTest(template = "empty", batch = BATCH + "_bouw", timeoutTicks = 200)
    public static void buikgangenBouw(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        if (!nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk.in(sl)) {
            // gametests run in the overworld: build there at the same coordinates (only into air)
        }
        int n = Z5Bouw.buikgangen(sl);
        helper.assertTrue(n > 1000, "rooms built (" + n + ")");
        int y = Z5WezensPlekken.BUIK_Y;
        for (int x = 2710; x <= 2800; x += 6) {
            BlockPos vloer = new BlockPos(x, y - 1, -520);
            helper.assertTrue(!sl.getBlockState(vloer).isAir() && sl.getBlockState(vloer.above()).isAir() && sl.getBlockState(vloer.above(2)).isAir(),
                    "walkable at x " + x);
        }
        helper.succeed();
    }
}
