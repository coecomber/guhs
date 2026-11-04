package nl.juiced.guhs.feature.vadsrijk.klassen;

import java.util.Set;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.ProefKlodder;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcPlekken;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * h-melee (playtest round 3, "melee is much harder than ranged/magic"): the starter sword does double damage, every melee
 * click that comes when the weapon is ready does its damage (no vanilla-cooldown dependence, early clicks buffered, the
 * class switch doesn't eat the hit), and the Klodderreus pond arena is big enough. Run: {@code -Pgt=VadsrijkMelee}.
 */
public class VadsrijkMeleeGameTests {
    private static final String BATCH = "vadsrijk_melee";

    /** The bound level-1 kussenzwaard from Knuffelkamp does 2x; a tutorial Proefklodder (lv 1) goes down in at most 3 hits. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void startzwaardDubbel(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        d.klasse = Klassen.RIDDER;
        KlasseBalie.startwapen(p, Klassen.RIDDER);
        ItemStack zwaard = ItemStack.EMPTY;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            Uitrusting u = Rollen.van(p.getInventory().getItem(i));
            if (u != null && u.type().equals("kussenzwaard")) {
                zwaard = p.getInventory().getItem(i).copy();
            }
        }
        helper.assertTrue(!zwaard.isEmpty(), "the ridder got his starter sword");
        Uitrusting u = Rollen.van(zwaard);
        helper.assertTrue(Rollen.isStartzwaard(u), "it counts as the starter sword (bound, Gewoon, lv 1)");
        Uitrusting los = u.metGebonden(false);
        helper.assertTrue(!Rollen.isStartzwaard(los), "a loose (loot/shop) kussenzwaard is not");
        helper.assertTrue(Math.abs(Rollen.wapenSchade(u) - 2 * Rollen.wapenSchade(los)) < 1e-9, "2x the damage: " + Rollen.wapenSchade(u) + " vs " + Rollen.wapenSchade(los));
        p.setItemInHand(InteractionHand.MAIN_HAND, zwaard);
        ProefKlodder m = helper.spawn(KernRegistry.PROEFKLODDER.get(), 2, 1, 2);
        m.stelIn(1, Element.GEEN, false, Set.of());
        m.setNoAi(true);
        double vol = m.maxVads();
        int klappen = 0;
        while (!m.geplofd() && klappen < 10) {
            Schade.VOLGENDE_KLAP.remove(p.getUUID());
            helper.assertTrue(Schade.basisaanval(p, m), "hit " + (klappen + 1) + " lands");
            klappen++;
        }
        com.mojang.logging.LogUtils.getLogger().info("[h-melee] starter sword: per hit {} (rating {}), Proefklodder lv1 {} Vads, down in {} hits", Spelers.klapSchade(p),
                Rollen.wapenSchade(u), vol, klappen);
        helper.assertTrue(m.geplofd() && klappen <= 3, "a lv-1 Proefklodder (" + vol + " Vads) goes down in at most 3 starter hits, took " + klappen);
        Schade.VOLGENDE_KLAP.remove(p.getUUID());
        helper.succeed();
    }

    /**
     * Repeated melee clicks at the weapon's interval ALWAYS do damage, even with the vanilla attack cooldown reset each time
     * (the server's vanilla ticker is reset by every click, also misses); 1 tick early (client/server jitter) counts; a click
     * up to {@link Schade#BUFFER} ticks early lands when ready; a spam click in between does nothing; the class switch on
     * attack doesn't eat the first hit.
     */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void nabijKlapElkeInterval(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        d.klasse = Klassen.JAGER;   // (the first hit with a sword switches to ridder: that hit must still land)
        p.setItemInHand(InteractionHand.MAIN_HAND, Rollen.stack(Rollen.rol(nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes.van("kussenzwaard"), 1,
                nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid.GEWOON, p.getRandom()).metBonussen(java.util.List.of())));
        ProefKlodder m = helper.spawn(KernRegistry.PROEFKLODDER.get(), 2, 1, 2);
        m.stelIn(20, Element.GEEN, false, Set.of());
        m.setNoAi(true);
        Schade.VOLGENDE_KLAP.remove(p.getUUID());
        Schade.WACHTEND.remove(p.getUUID());
        int n = Schade.intervalTicks(p);
        helper.assertTrue(n == 20, "sword interval 20 ticks, got " + n);
        double[] v = new double[1];
        // like VadsrijkMob#hurtServer: class check/switch first, then the hit; the vanilla ticker reset as by Neo's onAttack
        java.util.function.BooleanSupplier klik = () -> {
            v[0] = m.vads();
            boolean raak = Spelers.voorAanval(p) && Schade.basisaanval(p, m);
            p.resetAttackStrengthTicker();
            return raak;
        };
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(klik.getAsBoolean() && m.vads() < v[0], "hit 1 (with the class switch) lands");
            helper.assertTrue(d.klasse.equals(Klassen.RIDDER), "switched to ridder");
        });
        helper.runAfterDelay(2 + n / 2, () -> helper.assertTrue(!klik.getAsBoolean() && m.vads() == v[0] && !Schade.WACHTEND.containsKey(p.getUUID()),
                "a spam click halfway does nothing and isn't remembered"));
        helper.runAfterDelay(2 + n, () -> helper.assertTrue(klik.getAsBoolean() && m.vads() < v[0], "hit 2 exactly on the interval"));
        helper.runAfterDelay(2 + 2 * n - Schade.SPELING, () -> helper.assertTrue(klik.getAsBoolean() && m.vads() < v[0], "hit 3, one tick early (jitter)"));
        int klaar3 = 2 + 3 * n - Schade.SPELING;   // ready for hit 4 from here on
        helper.runAfterDelay(klaar3 - 5, () -> {
            helper.assertTrue(!klik.getAsBoolean() && m.vads() == v[0] && Schade.WACHTEND.containsKey(p.getUUID()), "5 ticks early: remembered");
        });
        for (int t = klaar3 - 4; t < klaar3 + 3; t++) {
            int tt = t;
            helper.runAfterDelay(t, () -> {
                double voor = m.vads();
                Schade.wachtendeKlap(p);
                if (tt < klaar3 - Schade.SPELING) {
                    helper.assertTrue(m.vads() == voor, "the remembered hit waits for the weapon (t " + tt + ")");
                }
            });
        }
        helper.runAfterDelay(klaar3 + 3, () -> {
            helper.assertTrue(!Schade.WACHTEND.containsKey(p.getUUID()) && m.vads() < v[0], "hit 4 (the early click) landed when ready");
        });
        int klaar4 = klaar3 - Schade.SPELING + n;
        for (int k = 0; k < 3; k++) {
            int t = klaar4 + k * n;
            helper.runAfterDelay(t, () -> helper.assertTrue(klik.getAsBoolean() && m.vads() < v[0], "hit at the interval (t " + t + ")"));
        }
        helper.runAfterDelay(klaar4 + 2 * n + 2, () -> {
            Schade.VOLGENDE_KLAP.remove(p.getUUID());
            m.discard();
            helper.succeed();
        });
    }

    /** The Klodderreus pond arena has room for melee: radius 26 (a ~11 block dry ring past the 15x10 pond). */
    @GuhTest(template = "empty", batch = BATCH)
    public static void kampvijverArenaRuim(GameTestHelper helper) {
        helper.assertTrue(NpcPlekken.KAMPVIJVER_STRAAL >= 24, "arena radius " + NpcPlekken.KAMPVIJVER_STRAAL);
        var a = Arenas.van(nl.juiced.guhs.feature.vadsrijk.npc.Verhaal.ARENA);
        if (a != null) {
            helper.assertTrue(a.straal() == NpcPlekken.KAMPVIJVER_STRAAL, "the registered arena uses it");
            double cx = a.centrum().getX() + 0.5, cz = a.centrum().getZ() + 0.5;
            // the pond is 15 (x) by 10 (z) blocks from the centre; melee fighters stand on the shore a few blocks further
            helper.assertTrue(a.binnen(cx + 21, a.centrum().getY(), cz) && a.binnen(cx - 21, a.centrum().getY(), cz)
                    && a.binnen(cx, a.centrum().getY(), cz + 18), "standing on the shore around the pond is inside the fight");
        }
        helper.succeed();
    }
}
