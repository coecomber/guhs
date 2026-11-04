package nl.juiced.guhs.feature.vadsrijk.kern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.block.Blocks;
import nl.juiced.guhs.feature.vadsrijk.economie.Drinken;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoItems;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.Winkels;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.wezens.WezensSpawners;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Z2Winkel;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.Z2WezensPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.Z2WezensSpawners;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Balans 2026-11 (second playtest): the level-relative Verdediging, the mob damage knob, the flat potions with their drink time
 * and shared cooldown, and no wild spawns next to paths. Run: {@code runGameTestServer -Pgt=VadsrijkBalans}.
 */
public class VadsrijkBalansGameTests {
    private static final String BATCH = "vadsrijk_balans";

    /** def / (def + 25 + 15 x attacker level), capped at 60 %, then Elementweerstand; armour and bonus rolls ~25 % lower. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void verdedigingPerLevel(GameTestHelper helper) {
        helper.assertTrue(Schade.verdedigingFactor(0, 10) == 0, "no def, nothing off");
        helper.assertTrue(Math.abs(Schade.verdedigingFactor(50, 10) - 50.0 / 225) < 1e-9, "50 def vs level 10: 22 %");
        helper.assertTrue(Math.abs(Schade.verdedigingFactor(50, 5) - 50.0 / 150) < 1e-9, "50 def vs level 5: 33 %");
        helper.assertTrue(Math.abs(Schade.verdedigingFactor(50, 1) - 50.0 / 90) < 1e-9 && Schade.verdedigingFactor(100, 1) == Schade.MAX_VERDEDIGING, "50 def vs level 1: 56 %, 100 def: capped at 60 %");
        helper.assertTrue(Schade.verdedigingFactor(10000, 30) == Schade.MAX_VERDEDIGING, "never more than 60 %");
        helper.assertTrue(Schade.verdedigingFactor(40, 20) < Schade.verdedigingFactor(40, 10), "the same def does less against higher levels");
        double n = Schade.verdediging(100, Stats.van(Stat.VERDEDIGING, 75, Stat.ELEMENTWEERSTAND, 20), Element.VUUR, 10);
        helper.assertTrue(Math.abs(n - 100 * (1 - 75.0 / 250) * 0.8) < 1e-9, "def first, then 20 % element resist: " + n);
        helper.assertTrue(Math.abs(Schade.verdediging(100, Stats.van(Stat.ELEMENTWEERSTAND, 200), Element.VUUR, 10) - 25) < 1e-9, "resist capped at 75 %");
        // armour: the knuffelharnas was registered with 7 def (+1.75/level), now x0.75
        BasisType h = BasisTypes.van("knuffelharnas");
        helper.assertTrue(h != null && Math.abs(h.basis().get(Stat.VERDEDIGING) - 5.25) < 0.01
                && Math.abs(h.perLevel().get(Stat.VERDEDIGING) - 1.31) < 0.011, "armour def x0.75: " + (h == null ? null : h.basis()));
        helper.assertTrue(BasisTypes.van("kussenzwaard") == null || BasisTypes.van("kussenzwaard").basis().get(Stat.VERDEDIGING) == 0, "weapons untouched");
        double[] b = nl.juiced.guhs.feature.vadsrijk.kern.loot.Bonussen.bereik(Stat.VERDEDIGING, 1);
        helper.assertTrue(b[0] == 1 && b[1] == 2, "the Verdediging bonus rolls 1-2 at level 1");
        helper.assertTrue(Balans.mobSchade() > 1.29 && Balans.mobSchade() < 1.31, "mob damage +30 % by default: " + Balans.mobSchade());
        helper.succeed();
    }

    /** Flat numbers, the 2 s drink, the shared 10 s cooldown on the item overlay, the shop prices. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void drankjesVastEnGedeeld(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);   // (creative never uses up the stack)
        Spelers.zetLevel(p, 20);
        double max = Spelers.maxVads(p);
        helper.assertTrue(max > 300, "room to heal: " + max);
        int[][] proef = {{0, Drinken.KLEIN_VADS}, {1, Drinken.VADS}};
        EcoItems.Drankje[] dr = {EcoSpul.KLEIN_VADSDRANKJE.get(), EcoSpul.VADSDRANKJE.get()};
        for (int[] t : proef) {
            Spelers.data(p).vads = 10;
            dr[t[0]].werk(p);
            helper.assertTrue(Math.abs(Spelers.vads(p) - Math.min(max, 10 + t[1])) < 1e-6, dr[t[0]] + ": +" + t[1] + " flat, got " + (Spelers.vads(p) - 10));
        }
        Spelers.data(p).vads = 10;
        Z2Winkel.GROTE_VADSDRANK.get().werk(p);
        helper.assertTrue(Math.abs(Spelers.vads(p) - Math.min(max, 710)) < 1e-6, "Groot: +700 (up to the max)");
        Spelers.data(p).vads = 10;
        Z2Winkel.WARME_CHOCOMELK.get().werk(p);
        helper.assertTrue(Math.abs(Spelers.vads(p) - Math.min(max, 260)) < 1e-6, "Chocomelk: +250");
        Spelers.data(p).energie = 0;
        EcoSpul.ENERGIEDRANKJE.get().werk(p);
        helper.assertTrue(Math.abs(Spelers.energie(p) - Math.min(Spelers.maxEnergie(p), 50)) < 1e-6, "Energiedrankje: +50");
        Spelers.data(p).energie = 0;
        Z2Winkel.GROTE_ENERGIEDRANK.get().werk(p);
        helper.assertTrue(Math.abs(Spelers.energie(p) - Math.min(Spelers.maxEnergie(p), 120)) < 1e-6, "Groot Energiedrankje: +120");
        // 2 s of the drink animation
        for (Item i : new Item[] {EcoSpul.KLEIN_VADSDRANKJE.get(), EcoSpul.VADSDRANKJE.get(), EcoSpul.ENERGIEDRANKJE.get(), Z2Winkel.GROTE_VADSDRANK.get(),
                Z2Winkel.GROTE_ENERGIEDRANK.get(), Z2Winkel.WARME_CHOCOMELK.get(), EcoSpul.VAHOEGDRANKJE.get()}) {
            ItemStack st = new ItemStack(i);
            var c = st.get(DataComponents.CONSUMABLE);
            helper.assertTrue(c != null && c.consumeTicks() == 40 && i.getUseAnimation(st) == ItemUseAnimation.DRINK, i + ": a 2 s drink");
        }
        // drinking one (outside the Vadsrijk: no effect, but the cooldown) puts the whole group on the cooldown overlay
        ItemStack klein = new ItemStack(EcoSpul.KLEIN_VADSDRANKJE.get(), 3);
        ItemStack rest = klein.finishUsingItem(helper.getLevel(), p);
        helper.assertTrue(rest.getCount() == 2, "one drunk: " + rest.getCount());
        for (Item i : new Item[] {EcoSpul.VADSDRANKJE.get(), EcoSpul.ENERGIEDRANKJE.get(), Z2Winkel.GROTE_VADSDRANK.get(), Z2Winkel.WARME_CHOCOMELK.get()}) {
            helper.assertTrue(p.getCooldowns().isOnCooldown(new ItemStack(i)), i + " shares the potion cooldown");
        }
        helper.assertTrue(!p.getCooldowns().isOnCooldown(new ItemStack(EcoSpul.VAHOEGDRANKJE.get())), "buff potions keep their own");
        var uc = new ItemStack(EcoSpul.VADSDRANKJE.get()).get(DataComponents.USE_COOLDOWN);
        helper.assertTrue(uc != null && uc.ticks() == 200 && uc.cooldownGroup().orElseThrow().equals(Drinken.GROEP), "10 s, one group");
        // prices
        var w = Winkels.winkel(Winkels.DRANKJES);
        helper.assertTrue(w != null && w.waar("klein_vadsdrankje").prijs() == 8 && w.waar("vadsdrankje").prijs() == 40, "zone-1 prices");
        var k = Winkels.winkel(Z2Winkel.KANDIJKRAAM);
        helper.assertTrue(k != null && k.waar("grote_vadsdrank").prijs() == 300, "Groot Vadsdrankje: 3 zilver");
        helper.succeed();
    }

    /** No spawner circle reaches a path (zone 1 + 2), kern refuses spots next to a path at its height, the plan keeps its size. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 100)
    public static void geenSpawnsOpPaden(GameTestHelper helper) {
        helper.assertTrue(Spawners.PADEN.size() >= 20, "zone 1 + zone 2 paths registered: " + Spawners.PADEN.size());
        helper.assertTrue(Spawners.padAfstand(90, -40) < 0, "on the weide path");
        int teDichtZ1 = 0, opp = 0;
        StringBuilder voorbeelden = new StringBuilder();
        for (Spawners.Spawner s : Spawners.alle()) {
            if (s.grot()) {
                continue;
            }
            boolean z1 = s.id().startsWith("wezens:");
            if (!z1) {
                continue;
            }
            opp++;
            if (Spawners.padAfstand(s.centrum().getX(), s.centrum().getZ()) < s.straal() + Spawners.PAD_VRIJ) {
                teDichtZ1++;
                if (voorbeelden.length() < 300) {
                    voorbeelden.append(s.id()).append(' ');
                }
            }
        }
        int teDichtZ2 = 0;
        for (Spawners.Spawner s : Z2WezensSpawners.ALLE) {
            if (s.id().contains("burcht") || s.id().contains("kloofbodem")) {
                continue;
            }
            if (Spawners.padAfstand(s.centrum().getX(), s.centrum().getZ()) < s.straal() + Spawners.PAD_VRIJ) {
                teDichtZ2++;
            }
        }
        com.mojang.logging.LogUtils.getLogger().info("[balans] paths: zone 1 {} open-air spawners, {} moved, {} could not move (left: {}); zone 2 {} moved, {} could not"
                + " move (left: {}); plan {} / {}", opp, WezensSpawners.verschoven, WezensSpawners.nietVerschoven, teDichtZ1, Z2WezensSpawners.verschoven,
                Z2WezensSpawners.nietVerschoven, teDichtZ2, WezensSpawners.PLAN, Z2WezensSpawners.MAX_PER_GEBIED);
        helper.assertTrue(teDichtZ1 == WezensSpawners.nietVerschoven, "zone 1: only the unmovable ones still reach a path: " + voorbeelden);
        helper.assertTrue(teDichtZ1 <= opp / 20, "zone 1: almost every spawner clear of the paths (" + teDichtZ1 + " of " + opp + "): " + voorbeelden);
        helper.assertTrue(teDichtZ2 == Z2WezensSpawners.nietVerschoven && teDichtZ2 <= Z2WezensSpawners.ALLE.size() / 20, "zone 2 clear: " + teDichtZ2);
        for (Z2WezensPlekken.Gebied g : Z2WezensPlekken.GEBIEDEN) {
            int max = Z2WezensSpawners.MAX_PER_GEBIED.getOrDefault(g.id(), 0);
            helper.assertTrue(max >= 55 && max <= 105, g.id() + ": still as dense, max alive " + max);
        }
        helper.assertTrue(WezensSpawners.PLAN.getOrDefault("klodderweide", 0) >= 160 && WezensSpawners.PLAN.getOrDefault("sporenwoud", 0) >= 170,
                "zone 1 still as dense: " + WezensSpawners.PLAN);
        // kern's spot check: a test path straight through the structure, a floor at y 1
        ServerLevel sl = helper.getLevel();
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 5; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE.defaultBlockState());
            }
        }
        BlockPos o = helper.absolutePos(new BlockPos(2, 2, 2));   // on the path (half width 2: its edge is 2 to the east)
        Spawners.registreerPad("test:balans", new int[][] {{o.getX(), o.getZ()}, {o.getX(), o.getZ() + 1}}, 2);
        try {
            helper.assertTrue(Spawners.bijPad(sl, o), "on the path: no");
            helper.assertTrue(Spawners.bijPad(sl, o.east(5)), "3 from the edge: no");
            helper.assertTrue(!Spawners.bijPad(sl, o.east(8)), "6 from the edge: fine");
            helper.assertTrue(!Spawners.bijPad(sl, o.below(10)), "a cave under the path: fine");
        } finally {
            Spawners.PADEN.remove("test:balans");
        }
        helper.succeed();
    }
}
