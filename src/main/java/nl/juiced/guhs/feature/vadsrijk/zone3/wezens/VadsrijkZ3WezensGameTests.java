package nl.juiced.guhs.feature.vadsrijk.zone3.wezens;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.loot.Z3Loot;
import nl.juiced.guhs.feature.vadsrijk.zone3.wezens.mob.Z3Wezen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Zone-3 wezens: spawn + defeat of every kind, loot, the contract elites, the soft lantern rule, the layered spawners.
 * Run: {@code runGameTestServer -Pgt=VadsrijkZ3Wezens}.
 */
public class VadsrijkZ3WezensGameTests {
    private static final String BATCH = "vadsrijk_z3wezens";

    private static void vloer(GameTestHelper helper, int y) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(new BlockPos(x, y, z), Blocks.MOSS_BLOCK.defaultBlockState());
            }
        }
    }

    /** Every kind (and the add) spawns with its level, element and loot table; defeated it vanishes (no happy hop-away). */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void alleWezensSpawnenEnVerdwijnen(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        vloer(helper, 1);
        ServerPlayer p = GuhMockPlayer.of(helper);
        Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
        List<Z3Wezen> alle = new ArrayList<>();
        List<String> namen = new ArrayList<>(Z3WezensRegistry.SOORTEN.keySet());
        helper.assertTrue(namen.size() == 14, "14 kinds, got " + namen.size());
        namen.addAll(Z3WezensRegistry.ADDS.keySet());
        for (String soort : namen) {
            Entity e = Z3WezensCommando.maak(sl, soort, pos, 0, 42, false, EnumSet.noneOf(Affix.class));
            helper.assertTrue(e instanceof Z3Wezen, "made " + soort);
            Z3Wezen w = (Z3Wezen) e;
            helper.assertTrue(w.mobLevel() == 42 && w.maxVads() > 150, soort + ": level 42 with real Vads, got " + w.maxVads());
            helper.assertTrue(w.element() == w.standaardElement(), soort + ": its element");
            if (Z3WezensRegistry.SOORTEN.containsKey(soort)) {
                helper.assertTrue(LootTabellen.van(w.lootTabel()) != null, soort + ": loot table");
                helper.assertTrue(LootTabellen.van(w.lootTabel() + "_elite") != null, soort + ": elite table");
            }
            w.zetHouding(0);
            w.krijgSchade(w.vads() + 1, p);
            helper.assertTrue(w.geplofd(), soort + ": defeated");
            alle.add(w);
        }
        helper.runAfterDelay(10, () -> {
            for (Z3Wezen w : alle) {
                helper.assertTrue(w.isRemoved(), w.naam() + " vanished");
            }
            helper.succeed();
        });
    }

    /** The tables roll real zone-3 gear; 15 base types with items, 3 jewellery; 8 contract tables + trophies. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void lootKlopt(GameTestHelper helper) {
        RandomSource r = RandomSource.create(330);
        int items = 0, sieraden = 0;
        for (String soort : Z3WezensRegistry.SOORTEN.keySet()) {
            LootTabel e = LootTabellen.van("z3wezens:" + soort + "_elite");
            for (int i = 0; i < 20; i++) {
                LootTabel.Resultaat res = e.rol(45, r);
                helper.assertTrue(res.munten() > 0, "coins");
                for (ItemStack s : res.items()) {
                    Uitrusting u = Rollen.van(s);
                    helper.assertTrue(u != null && BasisTypes.van(u.type()) != null && u.level() == 45, "a rolled piece of " + soort);
                    items++;
                }
            }
        }
        for (String id : Z3Loot.STUKKEN.keySet()) {
            var t = BasisTypes.van(id);
            helper.assertTrue(t != null, "base type " + id);
            helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(net.minecraft.resources.Identifier.parse(t.itemId())).isPresent(),
                    "item " + t.itemId());
            if (t.slot().sieraad()) {
                sieraden++;
            }
        }
        helper.assertTrue(sieraden == 3, "3 jewellery pieces");
        helper.assertTrue(items > 60, "elites drop plenty, got " + items);
        for (String id : Z3Contracten.IDS) {
            LootTabel t = LootTabellen.van("z3wezens:contract_" + id);
            helper.assertTrue(t != null && t.rol(48, r).items().size() >= 2, "contract table " + id);
            helper.assertTrue(!Z3Contracten.trofee(id).isEmpty(), "trophy " + id);
        }
        helper.assertTrue(Z3Contracten.IDS.size() == 8, "8 contract targets");
        helper.succeed();
    }

    /**
     * A contract elite: elite with its 2 affixes, ~6x health, named; only its own player hurts it (another player's hit does
     * nothing); defeated, the board's hook fires for its player.
     */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void contractElite(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        vloer(helper, 1);
        ServerPlayer eigen = GuhMockPlayer.of(helper), ander = GuhMockPlayer.of(helper);
        Z3Contracten.Doelwit d = Z3Contracten.DOELWITTEN.get("ouwe_rotkeiler");
        Z3Wezen w = Z3Contracten.maak(sl, d, helper.absolutePos(new BlockPos(4, 2, 4)), List.of(eigen.getUUID()));
        helper.assertTrue(w != null && w.elite() && "ouwe_rotkeiler".equals(w.contract()), "made the contract elite");
        helper.assertTrue(w.affixen().contains(Affix.SNEL) && w.affixen().contains(Affix.GLOEIEND), "its fixed affixes");
        helper.assertTrue(w.hasCustomName(), "named");
        Z3Wezen gewoon = (Z3Wezen) Z3WezensCommando.maak(sl, "rotzwijn", Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1))), 0, 48, false,
                EnumSet.noneOf(Affix.class));
        helper.assertTrue(w.maxVads() > gewoon.maxVads() * 5.5, "~6x health: " + w.maxVads() + " vs " + gewoon.maxVads());
        gewoon.discard();
        helper.assertTrue(w.voor(eigen) && !w.voor(ander), "only for its own player");
        double voor = w.vads();
        Schade.doe(new Schade.Klap(ander, w, 500, Element.GEEN, Schade.Soort.MELEE, false, "test"));
        helper.assertTrue(w.vads() == voor, "another player's hit does nothing");
        Schade.doe(new Schade.Klap(eigen, w, 500, Element.GEEN, Schade.Soort.MELEE, false, "test"));
        helper.assertTrue(w.vads() < voor, "its own player's hit counts");
        List<String> gemeld = new ArrayList<>();
        java.util.function.BiConsumer<ServerPlayer, String> h = (p, id) -> gemeld.add(id);
        Z3Contracten.VERSLAGEN.add(h);
        w.krijgSchade(w.vads() + 1, eigen);
        Z3Contracten.VERSLAGEN.remove(h);
        helper.assertTrue(w.geplofd() && gemeld.contains("ouwe_rotkeiler"), "defeated, the board is told");
        helper.succeed();
    }

    /** The soft lantern rule: a creature outside the light takes half damage; bright block light counts as light. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void lantaarnlicht(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        vloer(helper, 1);
        ServerPlayer p = GuhMockPlayer.of(helper);
        Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
        Z3Wezen a = (Z3Wezen) Z3WezensCommando.maak(sl, "wortelschim", pos, 0, 47, false, EnumSet.noneOf(Affix.class));
        Z3Wezen b = (Z3Wezen) Z3WezensCommando.maak(sl, "wortelschim", pos.add(2, 0, 0), 0, 47, false, EnumSet.noneOf(Affix.class));
        a.zetDonker(true);
        b.zetDonker(false);
        double va = a.vads(), vb = b.vads();
        Schade.doe(new Schade.Klap(p, a, 100, Element.GEEN, Schade.Soort.MELEE, false, "test"));
        Schade.doe(new Schade.Klap(p, b, 100, Element.GEEN, Schade.Soort.MELEE, false, "test"));
        double da = va - a.vads(), db = vb - b.vads();
        helper.assertTrue(db > 0 && Math.abs(da - db * Lantaarnlicht.DONKER_FACTOR) < db * 0.02, "dark = half: " + da + " vs " + db);
        BlockPos licht = helper.absolutePos(new BlockPos(7, 2, 7));
        helper.setBlock(new BlockPos(7, 2, 7), Blocks.PEARLESCENT_FROGLIGHT.defaultBlockState());
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(Lantaarnlicht.inLicht(sl, Vec3.atCenterOf(licht.above())), "a froglight is light");
            helper.assertTrue(!Lantaarnlicht.inLicht(sl, Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 2, 0))).add(0, 40, 0)), "high in the air, far from light = dark");
            a.discard();
            b.discard();
            helper.succeed();
        });
    }

    /**
     * The layered spawners: every layer holds 55-105 creatures at most; every spawner is in cave mode inside its layer's
     * heights, keeps 5 blocks from every path at its height and out of the safe spots; contract targets are never there.
     * Plus the mechanism: a cave-mode spawner over two floors puts its creature on the floor inside its y-range.
     */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void spawnersPerLaag(GameTestHelper helper) {
        com.mojang.logging.LogUtils.getLogger().info("[z3wezens] spawner plan (max alive per layer): {} ({} spawners, world plekken: {})",
                Z3WezensSpawners.MAX_PER_GEBIED, Z3WezensSpawners.ALLE.size(), Z3WezensPlekken.wereldBekend());
        for (Z3WezensPlekken.Laag l : Z3WezensPlekken.LAGEN) {
            int max = Z3WezensSpawners.MAX_PER_GEBIED.getOrDefault(l.id(), 0);
            helper.assertTrue(max >= 55 && max <= 105, l.id() + ": max alive " + max);
        }
        for (Spawners.Spawner s : Z3WezensSpawners.ALLE) {
            helper.assertTrue(Spawners.van(s.id()) != null, "registered " + s.id());
            helper.assertTrue(s.grot(), "layer (cave) mode: " + s.id());
            Z3WezensPlekken.Laag l = Z3WezensSpawners.laagVan(s);
            helper.assertTrue(l != null, "a layer: " + s.id());
            for (String c : Z3Contracten.IDS) {
                helper.assertTrue(!s.id().contains(c), "no contract target in a spawner: " + s.id());
            }
            if (s.id().contains("leider_") || s.id().contains("steiger")) {
                continue;
            }
            helper.assertTrue(s.minY() >= l.minY() - 15 && s.maxY() <= l.maxY() + 15, "inside its layer's heights: " + s.id());
            double pad = Z3WezensPlekken.padAfstand(s.centrum().getX(), s.centrum().getZ(), s.minY(), s.maxY());
            helper.assertTrue(pad >= s.straal() + Z3WezensPlekken.PAD_VRIJ - 0.01, "5 from paths: " + s.id() + " (" + pad + ")");
            helper.assertTrue(!Z3WezensPlekken.veilig(s.centrum().getX(), (s.minY() + s.maxY()) / 2.0, s.centrum().getZ(), 0), "not in a safe spot: " + s.id());
        }
        // the mechanism: two floors (y 1 and y 8); the spawner's range covers only the lower one
        vloer(helper, 1);
        vloer(helper, 8);
        for (int y = 2; y <= 16; y++) {
            if (y != 8) {
                helper.setBlock(new BlockPos(4, y, 4), Blocks.AIR.defaultBlockState());
            }
        }
        BlockPos c = helper.absolutePos(new BlockPos(4, 4, 4));
        int laag = helper.absolutePos(new BlockPos(0, 2, 0)).getY();
        Spawners.Spawner sp = new Spawners.Spawner("z3wezens:test_laag", c, 3, Z3WezensRegistry.ROTZWIJN, 1, 20, 44, 44, null, 0).grot(laag - 1, laag + 4);
        try {
            // kern's floor search of a cave-mode spawner (package-private; via reflection so no player nearby can get in the way)
            java.lang.reflect.Method vloer = Spawners.class.getDeclaredMethod("vloer", ServerLevel.class, int.class, int.class, int.class, int.class);
            vloer.setAccessible(true);
            int y = (Integer) vloer.invoke(null, helper.getLevel(), c.getX(), c.getZ(), sp.minY(), sp.maxY());
            helper.assertTrue(y == laag, "the floor inside the range (lower floor " + laag + "), got " + y);
            int boven = (Integer) vloer.invoke(null, helper.getLevel(), c.getX(), c.getZ(), laag + 2, laag + 12);
            helper.assertTrue(boven == laag + 7, "the upper floor for the upper range, got " + boven);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        helper.succeed();
    }
}
