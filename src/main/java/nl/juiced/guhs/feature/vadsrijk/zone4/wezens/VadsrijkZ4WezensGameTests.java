package nl.juiced.guhs.feature.vadsrijk.zone4.wezens;

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
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Kristallen;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.loot.Z4Loot;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.mob.Kust;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.mob.Z4Wezen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Zone-4 wezens: every kind spawns and gets cheered up (the ontvadst ones hop around pink, the Mika is gone in smoke), the loot
 * (sets, jewellery, weapons, crystals), the light spawner plan, the Mopperkrab's claws and the Zeurslak's shell. Run:
 * {@code runGameTestServer -Pgt=VadsrijkZ4Wezens}.
 */
public class VadsrijkZ4WezensGameTests {
    private static final String BATCH = "vadsrijk_z4wezens";

    private static void vloer(GameTestHelper helper, int y) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(new BlockPos(x, y, z), Blocks.SAND.defaultBlockState());
            }
        }
    }

    /** All 10 kinds: level 52 with real Vads, a loot table; beaten: cheered up (stays a while, hopping) or the Mika gone. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void alleWezensOpgevrolijkt(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        vloer(helper, 1);
        ServerPlayer p = GuhMockPlayer.of(helper);
        Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
        helper.assertTrue(Z4WezensRegistry.SOORTEN.size() == 10, "10 kinds, got " + Z4WezensRegistry.SOORTEN.size());
        List<Z4Wezen> alle = new ArrayList<>();
        for (String soort : Z4WezensRegistry.SOORTEN.keySet()) {
            Entity e = Z4WezensCommando.maak(sl, soort, pos, 0, 52, false, EnumSet.noneOf(Affix.class));
            helper.assertTrue(e instanceof Z4Wezen, "made " + soort);
            Z4Wezen w = (Z4Wezen) e;
            helper.assertTrue(w.mobLevel() == 52 && w.maxVads() > 300, soort + ": level 52 with real Vads, got " + w.maxVads());
            helper.assertTrue(w.ontvadst() != w.mika(), soort + ": ontvadst unless a Mika");
            helper.assertTrue(LootTabellen.van(w.lootTabel()) != null && LootTabellen.van(w.lootTabel() + "_elite") != null, soort + ": loot tables");
            w.zetHouding(0);
            w.krijgSchade(w.vads() + 1, p);
            helper.assertTrue(w.geplofd(), soort + ": cheered up / beaten");
            alle.add(w);
        }
        helper.runAfterDelay(20, () -> {
            for (Z4Wezen w : alle) {
                if (w.mika()) {
                    helper.assertTrue(w.isRemoved(), w.naam() + ": the Mika fled in smoke");
                } else {
                    helper.assertTrue(!w.isRemoved(), w.naam() + ": cheered up, still hopping around happily");
                }
            }
            alle.forEach(Entity::discard);
            helper.succeed();
        });
    }

    /** 11 gear types + 6 weapons with items; the tables roll real lv-54 gear; 2 crystals on the gem system. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void lootKlopt(GameTestHelper helper) {
        RandomSource r = RandomSource.create(440);
        int items = 0;
        for (String soort : Z4WezensRegistry.SOORTEN.keySet()) {
            LootTabel e = LootTabellen.van("z4wezens:" + soort + "_elite");
            for (int i = 0; i < 20; i++) {
                LootTabel.Resultaat res = e.rol(54, r);
                helper.assertTrue(res.munten() > 0, "coins");
                for (ItemStack s : res.items()) {
                    Uitrusting u = Rollen.van(s);
                    helper.assertTrue(u != null && BasisTypes.van(u.type()) != null && u.level() == 54, "a rolled piece of " + soort);
                    items++;
                }
            }
        }
        helper.assertTrue(items > 60, "elites drop plenty, got " + items);
        List<String> typen = new ArrayList<>(Z4Loot.STUKKEN.keySet());
        typen.addAll(Z4Loot.WAPENS);
        for (String id : typen) {
            var t = BasisTypes.van(id);
            helper.assertTrue(t != null, "base type " + id);
            helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(net.minecraft.resources.Identifier.parse(t.itemId())).isPresent(),
                    "item " + t.itemId());
        }
        for (String k : Z4Loot.KRISTALLEN) {
            helper.assertTrue(Kristallen.SOORTEN.containsKey(k) && !Kristallen.stack(k, 1, 1).isEmpty(), "crystal " + k);
        }
        helper.succeed();
    }

    /** Light combat: few spawners, small numbers, none of them in a safe spot (village, party, flowers, the pier). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void spawnersLicht(GameTestHelper helper) {
        helper.assertTrue(!Z4WezensSpawners.ALLE.isEmpty(), "spawners exist");
        helper.assertTrue(Z4WezensSpawners.maxTotaal() <= 50, "at most ~50 alive in the whole zone, got " + Z4WezensSpawners.maxTotaal());
        for (Spawners.Spawner s : Z4WezensSpawners.ALLE) {
            helper.assertTrue(Spawners.van(s.id()) == s, "kern knows " + s.id());
            helper.assertTrue(!Z4WezensPlekken.veilig(s.centrum().getX(), s.centrum().getY(), s.centrum().getZ()), s.id() + " is not in a safe spot");
            helper.assertTrue(s.max() <= 3 && s.minLevel() >= 50 && s.maxLevel() <= 56, s.id() + ": small and lv 50-56");
        }
        helper.assertTrue(Z4WezensPlekken.veilig(1850, 66, -470) && Z4WezensPlekken.veilig(1935, 66, -345), "Lampionhaven and the Knuffeltij-strand are safe");
        helper.succeed();
    }

    /** The Mopperkrab's claws: a hit from the front does half while they're up; from behind full. The Zeurslak's shell. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void krabEnSlak(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        vloer(helper, 1);
        Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 2, 4)));
        ServerPlayer p = GuhMockPlayer.of(helper);
        Kust.Mopperkrab k = (Kust.Mopperkrab) Z4WezensCommando.maak(sl, "mopperkrab", pos, 0, 52, false, EnumSet.noneOf(Affix.class));
        k.setNoAi(true);
        k.zetHouding(Kust.SCHILD);
        Vec3 voor = k.position().add(0, 0, 3), achter = k.position().add(0, 0, -3);   // yaw 0 faces +z
        p.snapTo(voor.x, voor.y, voor.z);
        double v = k.vads();
        Schade.doe(new Schade.Klap(p, k, 100, Element.GEEN, Schade.Soort.MELEE, false, "test"));
        double vanVoren = v - k.vads();
        p.snapTo(achter.x, achter.y, achter.z);
        v = k.vads();
        Schade.doe(new Schade.Klap(p, k, 100, Element.GEEN, Schade.Soort.MELEE, false, "test"));
        double vanAchter = v - k.vads();
        helper.assertTrue(vanVoren > 0 && vanVoren < vanAchter * 0.6, "claws up: half from the front (" + vanVoren + " vs " + vanAchter + ")");
        k.discard();
        Kust.Zeurslak s = (Kust.Zeurslak) Z4WezensCommando.maak(sl, "zeurslak", pos, 0, 52, false, EnumSet.noneOf(Affix.class));
        s.setNoAi(true);
        s.krijgSchade(s.maxVads() * 0.55, p);
        helper.assertTrue(s.inSchelp(), "below half: into its shell");
        v = s.vads();
        s.krijgSchade(500, p);
        helper.assertTrue(s.vads() == v, "in its shell: no damage");
        s.discard();
        helper.succeed();
    }
}
