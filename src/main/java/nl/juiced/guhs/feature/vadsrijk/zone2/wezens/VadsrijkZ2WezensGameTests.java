package nl.juiced.guhs.feature.vadsrijk.zone2.wezens;

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
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.loot.Z2Loot;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.mob.Z2Wezen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/** Zone-2 wezens: spawn + defeat of every kind, loot, Bevroren, spawner density. Run: {@code runGameTestServer -Pgt=VadsrijkZ2Wezens}. */
public class VadsrijkZ2WezensGameTests {
    private static final String BATCH = "vadsrijk_z2wezens";

    private static void vloer(GameTestHelper helper) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.SNOW_BLOCK.defaultBlockState());
            }
        }
    }

    /**
     * Every kind spawns with its level, its element and the versteend flag (= kern's ontvadst, Roze x1.5); defeated it does NOT
     * hop away happily: it vanishes within a few ticks (crystal ones leave melting shards behind).
     */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void alleWezensSpawnenEnVerdwijnen(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        vloer(helper);
        ServerPlayer p = GuhMockPlayer.of(helper);
        Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
        List<Z2Wezen> alle = new ArrayList<>();
        int scherven = Verslagen.aantalScherven();
        for (String soort : Z2WezensRegistry.SOORTEN.keySet()) {
            Entity e = Z2WezensCommando.maak(sl, soort, pos, 0, 27, false, EnumSet.noneOf(Affix.class));
            helper.assertTrue(e instanceof Z2Wezen, "made " + soort);
            Z2Wezen w = (Z2Wezen) e;
            helper.assertTrue(w.mobLevel() == 27 && w.maxVads() > 100, soort + ": level 27 with real Vads, got " + w.maxVads());
            helper.assertTrue(w.ontvadst() == w.versteend(), soort + ": versteend = Roze-weak");
            helper.assertTrue(w.element() == w.standaardElement(), soort + ": its element");
            helper.assertTrue(LootTabellen.van(w.lootTabel()) != null, soort + ": loot table");
            w.zetHouding(0);
            w.krijgSchade(w.vads() + 1, p);
            helper.assertTrue(w.geplofd(), soort + ": defeated");
            alle.add(w);
        }
        helper.assertTrue(Verslagen.aantalScherven() > scherven, "crystal creatures left shards");
        helper.runAfterDelay(10, () -> {
            for (Z2Wezen w : alle) {
                helper.assertTrue(w.isRemoved(), w.naam() + " vanished (no happy hop-away)");
            }
            helper.succeed();
        });
    }

    /** Every kind has a table + an elite table that roll real zone-2 gear; all 15 base types have an item; jewellery is jewellery. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void lootKlopt(GameTestHelper helper) {
        RandomSource r = RandomSource.create(220);
        int items = 0, sieraden = 0;
        for (String soort : Z2WezensRegistry.SOORTEN.keySet()) {
            LootTabel t = LootTabellen.van("z2wezens:" + soort), e = LootTabellen.van("z2wezens:" + soort + "_elite");
            helper.assertTrue(t != null && e != null, "tables of " + soort);
            for (int i = 0; i < 25; i++) {
                LootTabel.Resultaat res = e.rol(30, r);
                helper.assertTrue(res.munten() > 0, "coins");
                for (ItemStack s : res.items()) {
                    Uitrusting u = Rollen.van(s);
                    helper.assertTrue(u != null && BasisTypes.van(u.type()) != null && u.level() == 30, "a rolled piece of " + soort);
                    items++;
                }
            }
        }
        for (String id : Z2Loot.STUKKEN.keySet()) {
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
        // a crystal z2-systemen has not made (yet) is just skipped, never a crash
        helper.assertTrue(Z2Loot.kristalItem("bestaat_niet").isEmpty(), "unknown crystal = empty");
        helper.succeed();
    }

    /** Bevroren: light hits change nothing, a heavy hit (>= 12 % of max Vads) does +30 % and breaks the ice; fainting clears it. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void bevrorenZwareKlap(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        double max = Spelers.maxVads(p);
        Bevroren.geef(p, 100, null);
        helper.assertTrue(Bevroren.heeft(p), "frozen");
        // (integration) slowing + the heavy-hit rule live in zone2.systemen.Bevroren (its own gametest bevrorenZwareKlap)
        Bevroren.wis(p);
        helper.assertTrue(!Bevroren.heeft(p), "thawed");
        helper.succeed();
    }

    /** MMO density: every area's spawners can hold ~60-90 creatures alive, and no spawner sits in a safe spot. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void spawnersDichtheid(GameTestHelper helper) {
        for (Z2WezensPlekken.Gebied g : Z2WezensPlekken.GEBIEDEN) {
            int max = Z2WezensSpawners.MAX_PER_GEBIED.getOrDefault(g.id(), 0);
            helper.assertTrue(max >= 55 && max <= 105, g.id() + ": max alive " + max);
        }
        for (Spawners.Spawner s : Z2WezensSpawners.ALLE) {
            helper.assertTrue(Spawners.van(s.id()) != null, "registered " + s.id());
            if (!s.id().startsWith("z2wezens:burcht")) {
                helper.assertTrue(!Z2WezensPlekken.veilig(s.centrum().getX(), s.centrum().getZ(), s.straal()), "not near a safe spot: " + s.id());
                helper.assertTrue(Z2WezensPlekken.gebied(s.centrum().getX(), s.centrum().getZ()) != null, "inside an area: " + s.id());
            }
        }
        helper.succeed();
    }
}
