package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.bazen.BazenRegistry;
import nl.juiced.guhs.feature.vadsrijk.bazen.koning.KoningKlodder;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.GrotBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisType;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.DropBalans;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.ZonePools;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.MobBalans;
import nl.juiced.guhs.feature.vadsrijk.klassen.KlasseBalans;
import nl.juiced.guhs.feature.vadsrijk.wezens.WezensRegistry;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.Klodders;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * 1101-balans: the zone loot pools, the mob multipliers + config migration, drop rates (fixed seed), the Vads regen in/out of
 * combat, the class catch-up and the flatter main stat. Run: {@code runGameTestServer -Pgt=VadsrijkBalans1101}.
 */
public class VadsrijkBalans1101GameTests {
    private static final String BATCH = "vadsrijk_balans1101";

    /** Every zone's pool: not empty, only that zone's creature-table types, no named boss/raid/dream items, no Maatwerk. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void lootPoolsPerZone(GameTestHelper helper) {
        for (int z = 1; z <= ZonePools.ZONES; z++) {
            Set<String> ids = ZonePools.ids(z);
            helper.assertTrue(ids.size() >= 10, "zone " + z + " pool has types: " + ids.size());
            for (String id : ids) {
                helper.assertFalse(ZonePools.benoemd(id) || ZonePools.maatwerk(id), "zone " + z + ": no named/craft-only item: " + id);
                int zz = ZonePools.zoneVan(id);
                helper.assertTrue(zz >= 1 && zz <= z, "zone " + z + ": " + id + " belongs to zone " + zz);
                boolean inTabel = false;
                for (var e : LootTabellen.alle().entrySet()) {
                    if (e.getKey().startsWith(ZonePools.prefix(z)) && e.getValue().regels().stream().anyMatch(r -> r.basisType().equals(id))) {
                        inTabel = true;
                        break;
                    }
                }
                helper.assertTrue(inTabel, "zone " + z + ": " + id + " is in one of the zone's creature tables");
            }
        }
        helper.assertFalse(ZonePools.ids(1).contains("z5baas_moersleutel"), "no moersleutel in zone 1");
        helper.assertTrue(ZonePools.zoneVan("z5baas_moersleutel") == 5 && ZonePools.zoneVan("baas_klodderknots") == 1
                && ZonePools.zoneVan("mw_t1_zwaard") == 0, "named items by prefix, Maatwerk unknown");
        RandomSource r = RandomSource.create(1101);
        for (String klasse : new String[] {Klassen.RIDDER, Klassen.JAGER, Klassen.MAGIER}) {
            for (int i = 0; i < 500; i++) {
                BasisType t = ZonePools.stuk(1, klasse, r);
                helper.assertTrue(t != null && ZonePools.zoneVan(t.id()) == 1, "zone 1 pick stays zone 1: " + (t == null ? null : t.id()));
                helper.assertTrue(t.slot() != Slot.WAPEN || klasse.equals(t.klasse()), "only own-class weapons: " + t.id());
            }
        }
        helper.succeed();
    }

    /** 1102: x1.75 HP and the 0.45 slope on open-world mobs (elites x2.5 on top); bosses the main-stat compensation x [bazen] leven 1.25; damage x2.028. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void mobVermenigvuldigers(GameTestHelper helper) {
        KlasseBalans.standaardAlles();
        helper.assertTrue(Math.abs(Balans.mobSchade() - 2.028) < 1e-9 && Math.abs(Balans.mobLeven() - 1.75) < 1e-9, "defaults 2.028 / 1.75");
        helper.assertTrue(Math.abs(Balans.bazenLeven() - 1.25) < 1e-9 && Math.abs(Balans.bazenSchade() - 1.2) < 1e-9, "[bazen] defaults 1.25 / 1.2");
        ServerLevel sl = helper.getLevel();
        BlockPos b = helper.absolutePos(new BlockPos(1, 1, 1));
        Klodders.Klodder k = WezensRegistry.KLODDER.get().create(sl, EntitySpawnReason.COMMAND);
        k.snapTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, 0, 0);
        k.stelIn(14, Element.WATER, false, Set.of());
        double verwacht = 36 * (1 + 0.45 * 13) * 1.75;
        helper.assertTrue(Math.abs(k.maxVads() - verwacht) < 0.05, "klodder lv 14: 36 x (1 + 0.45 x 13) x 1.75 = " + verwacht + ", got " + k.maxVads());
        k.stelIn(14, Element.WATER, true, Set.of());
        helper.assertTrue(Math.abs(k.maxVads() - verwacht * 2.5) < 0.1, "elite x2.5: " + k.maxVads());
        k.stelIn(1, Element.WATER, false, Set.of());
        helper.assertTrue(Math.abs(k.maxVads() - 36 * 1.75) < 0.05, "lv 1: only x1.75: " + k.maxVads());
        KoningKlodder kk = BazenRegistry.KONING_KLODDER.get().create(sl, EntitySpawnReason.COMMAND);
        kk.snapTo(b.getX() + 0.5, b.getY(), b.getZ() + 0.5, 0, 0);
        kk.stelIn(8, Element.WATER, false, Set.of());
        double baas = (2000 + 200 * 8) * MobBalans.compensatie(8) * 1.25;
        helper.assertTrue(Math.abs(kk.maxVads() - baas) < 0.5, "boss: no [mobs] leven, the compensation x [bazen] leven 1.25: " + baas + " vs " + kk.maxVads());
        // the knob is live
        KlasseBalans.zet(KlasseBalans.MOBS, "leven", 2.0, false);
        k.stelIn(1, Element.WATER, false, Set.of());
        helper.assertTrue(Math.abs(k.maxVads() - 72) < 0.05, "[mobs] leven 2.0: " + k.maxVads());
        KlasseBalans.zet(KlasseBalans.BAZEN, "leven", 2.0, false);
        kk.stelIn(8, Element.WATER, false, Set.of());
        helper.assertTrue(Math.abs(kk.maxVads() - (2000 + 200 * 8) * MobBalans.compensatie(8) * 2.0) < 0.5, "[bazen] leven 2.0 (and not [mobs] leven): " + kk.maxVads());
        KlasseBalans.standaardAlles();
        k.discard();
        kk.discard();
        helper.succeed();
    }

    /** Old files (1.10.0 = versie 1, 1.10.1 = versie 2): untouched defaults move to the 1.10.2 ones, hand-set values stay, a current file is left alone. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void configMigratie(GameTestHelper helper) {
        try {
            // versie 1 -> 3
            String r = KlasseBalans.testMigreer(Map.of("schade", 1.3));
            helper.assertTrue(r != null && Math.abs(KlasseBalans.get(KlasseBalans.MOBS, "schade") - 2.028) < 1e-9
                    && Math.abs(KlasseBalans.get(KlasseBalans.MOBS, "leven") - 1.75) < 1e-9 && KlasseBalans.get(KlasseBalans.MOBS, "versie") == 3,
                    "1.10.0 defaults -> 2.028 / 1.75 / versie 3: " + r);
            helper.assertTrue(Math.abs(Balans.mobSchade() - 2.028) < 1e-9 && Math.abs(Balans.mobLeven() - 1.75) < 1e-9
                    && Math.abs(Balans.bazenLeven() - 1.25) < 1e-9 && Math.abs(Balans.bazenSchade() - 1.2) < 1e-9, "applied to kern");
            r = KlasseBalans.testMigreer(Map.of("schade", 2.0));
            helper.assertTrue(r != null && Math.abs(KlasseBalans.get(KlasseBalans.MOBS, "schade") - 2.0) < 1e-9
                    && Math.abs(KlasseBalans.get(KlasseBalans.MOBS, "leven") - 1.75) < 1e-9, "v1: a hand-set schade stays: " + r);
            // versie 2 (the live server: 1.10.1 defaults) -> 3
            r = KlasseBalans.testMigreer(Map.of("schade", 1.69, "leven", 1.4, "versie", 2.0));
            helper.assertTrue(r != null && r.contains("[bazen]") && Math.abs(KlasseBalans.get(KlasseBalans.MOBS, "schade") - 2.028) < 1e-9
                    && Math.abs(KlasseBalans.get(KlasseBalans.MOBS, "leven") - 1.75) < 1e-9 && KlasseBalans.get(KlasseBalans.MOBS, "versie") == 3
                    && Math.abs(KlasseBalans.get(KlasseBalans.BAZEN, "leven") - 1.25) < 1e-9 && Math.abs(KlasseBalans.get(KlasseBalans.BAZEN, "schade") - 1.2) < 1e-9,
                    "1.10.1 defaults -> 2.028 / 1.75 / versie 3 + [bazen] 1.25 / 1.2: " + r);
            helper.assertTrue(KlasseBalans.toon().stream().anyMatch(t -> t.equals("[mobs] schade=2.03 leven=1.75 versie=3.00"))
                    && KlasseBalans.toon().stream().anyMatch(t -> t.equals("[bazen] leven=1.25 schade=1.20")), "balans toon: " + KlasseBalans.toon());
            r = KlasseBalans.testMigreer(Map.of("schade", 1.5, "leven", 1.2, "versie", 2.0));
            helper.assertTrue(r != null && Math.abs(Balans.mobSchade() - 1.5) < 1e-9 && Math.abs(Balans.mobLeven() - 1.2) < 1e-9
                    && Math.abs(Balans.bazenLeven() - 1.25) < 1e-9, "v2: hand-set schade/leven stay, [bazen] added: " + r);
            r = KlasseBalans.testMigreer(Map.of("schade", 1.69, "leven", 1.3, "versie", 2.0));
            helper.assertTrue(r != null && Math.abs(Balans.mobSchade() - 2.028) < 1e-9 && Math.abs(Balans.mobLeven() - 1.3) < 1e-9,
                    "v2: only the untouched key moves: " + r);
            r = KlasseBalans.testMigreer(Map.of("schade", 1.5, "leven", 1.2, "versie", 3.0));
            helper.assertTrue(r == null && Math.abs(Balans.mobSchade() - 1.5) < 1e-9 && Math.abs(Balans.mobLeven() - 1.2) < 1e-9,
                    "versie 3: nothing changes: " + r);
            helper.succeed();
        } finally {
            KlasseBalans.standaardAlles();
        }
    }

    /** Fixed seed: a normal klodder drops gear at 0.16 x 0.2, an elite 2 rolls at min(1, 0.56) x 0.45 with luck 1.6. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void dropKansen(GameTestHelper helper) {
        LootTabel gewoon = LootTabellen.van("wezens:klodder"), elite = LootTabellen.van("wezens:klodder_elite");
        helper.assertTrue(Math.abs(gewoon.kans() - 0.032) < 1e-9 && gewoon.geluk() == 1.0, "normal: 0.16 x 0.2: " + gewoon.kans());
        helper.assertTrue(Math.abs(elite.kans() - 0.56 * 0.45) < 1e-9 && Math.abs(elite.geluk() - 1.6) < 1e-9 && elite.rollen() == 2,
                "elite: 2 x 0.252, luck 1.6: " + elite.kans() + " / " + elite.geluk());
        helper.assertTrue(LootTabellen.van("z6wezens:" + eersteMob("z6wezens:")).geluk() == 1.4
                && Math.abs(LootTabellen.van("z6wezens:" + eersteMob("z6wezens:") + "_elite").geluk() - DropBalans.eliteGeluk(3.4)) < 1e-9,
                "zone 6: normal luck unchanged (1.4), elite 3.4 x 0.64");
        RandomSource r = RandomSource.create(1101);
        int n = 20000, g = 0, e = 0, legendarisch = 0;
        for (int i = 0; i < n; i++) {
            g += gewoon.rol(5, r).items().size();
            for (ItemStack s : elite.rol(5, r).items()) {
                e++;
                Uitrusting u = Rollen.van(s);
                if (u != null && u.zeldzaamheid().ordinal() >= Zeldzaamheid.LEGENDARISCH.ordinal()) {
                    legendarisch++;
                }
            }
        }
        double pg = g / (double) n, pe = e / (double) n, pl = legendarisch / (double) Math.max(1, e);
        helper.assertTrue(Math.abs(pg - 0.032) < 0.006, "normal ~3.2 % per kill (was 16 %): " + pg);
        helper.assertTrue(Math.abs(pe - 0.504) < 0.03, "elite ~0.50 items per kill (was 1.12): " + pe);
        helper.assertTrue(pl > 0.025 && pl < 0.075, "elite Legendarisch+ ~4.9 % of its drops (was 17.8 %): " + pl);
        helper.succeed();
    }

    private static String eersteMob(String prefix) {
        for (String id : LootTabellen.alle().keySet()) {
            if (id.startsWith(prefix) && !id.endsWith("_elite") && !id.contains("contract_") && !id.endsWith(":wapens")) {
                return id.substring(prefix.length());
            }
        }
        return "?";
    }

    /** 0.3 %/s while fighting (5 s after the last hit), 1 %/s outside. */
    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void regenInEnUitGevecht(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Gevecht.zet(p, false);
        helper.assertTrue(!Gevecht.in(p) && Gevecht.regen(p) == 0.01, "outside combat: 1 %/s");
        Gevecht.gevochten(p);
        helper.assertTrue(Gevecht.in(p) && Gevecht.regen(p) == 0.003, "in combat: 0.3 %/s");
        helper.runAfterDelay(Gevecht.RUST_TICKS + 2, () -> {
            helper.assertTrue(!Gevecht.in(p) && Gevecht.regen(p) == 0.01, "5 s after the last hit: 1 %/s again");
            helper.succeed();
        });
    }

    /** +15 % class xp per level behind the highest class, max x3; generic xp unchanged. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void inhaalXp(GameTestHelper helper) {
        helper.assertTrue(Spelers.inhaalFactor(20, 20) == 1 && Math.abs(Spelers.inhaalFactor(20, 16) - 1.6) < 1e-9
                && Spelers.inhaalFactor(20, 1) == 3.0 && Spelers.inhaalFactor(5, 9) == 1, "1 / 1.6 / max 3 / never below 1");
        ServerPlayer p = GuhMockPlayer.of(helper);
        SpelerData d = Spelers.data(p);
        String hoofd = d.klasse;
        String tweede = hoofd.equals(Klassen.JAGER) ? Klassen.MAGIER : Klassen.JAGER;
        try {
            Spelers.zetLevel(p, 10);
            d.xp.put(tweede, Levels.totaalVoor(4));
            double f = Spelers.inhaalFactor(d, tweede);
            helper.assertTrue(Math.abs(f - (1 + 0.15 * 6)) < 1e-9, "6 levels behind: x1.9, got " + f);
            helper.assertTrue(Spelers.inhaalFactor(d, hoofd) == 1, "the highest class: x1");
            long g0 = d.generiekXp, k0 = d.xp(tweede);
            Spelers.geefXp(p, 100, "test", tweede);
            long dg = d.generiekXp - g0, dk = d.xp(tweede) - k0;
            helper.assertTrue(dg > 0 && Math.abs(dk - Math.round(dg * f)) <= 1, "class gets x1.9 of the generic xp: " + dk + " vs " + dg);
            helper.succeed();
        } finally {
            d.xp.remove(tweede);
            Spelers.zetLevel(p, 1);
        }
    }

    /** Main stat 1 % per point x 1.7 for players; the HP slope 0.45; boss compensation; dungeon boss xp 0.8 level per run. */
    @GuhTest(template = "empty", batch = BATCH)
    public static void statFormule(GameTestHelper helper) {
        helper.assertTrue(Math.abs(Schade.hoofdFactor(0) - 1.7) < 1e-9 && Math.abs(Schade.hoofdFactor(100) - 3.4) < 1e-9, "1.7 x (1 + 1 % x stat)");
        helper.assertTrue(Math.abs(Schade.aanval(10, Stats.van(Stat.KRACHT, 100), Schade.Soort.MELEE, false, Element.GEEN, null, false) - 20) < 1e-9,
                "pure aanval: 1 % per point (mobs have no base)");
        helper.assertTrue(Math.abs(MobBalans.helling(1) - 1) < 1e-9 && Math.abs(MobBalans.helling(14) - 6.85 / 4.9) < 1e-9, "slope fix");
        double c1 = MobBalans.compensatie(1), c70 = MobBalans.compensatie(70);
        helper.assertTrue(c1 > 1.4 && c1 < 1.55 && c70 > 0.95 && c70 < 1.05, "boss compensation ~x1.48 at lv 1, ~x1.0 at lv 70: " + c1 + " / " + c70);
        // zone-1 time to kill vs 1.10.0 (model: Kracht 14 + 3.2 lv, mob HP base x slope): ~same at lv 1, longer later, still < x2.2
        for (int lv : new int[] {1, 5, 10, 14, 20}) {
            double k = Schade.modelHoofd(lv);
            double oud = (1 + 0.3 * (lv - 1)) / (1 + 0.02 * k);
            double nieuw = 1.4 * (1 + 0.45 * (lv - 1)) / Schade.hoofdFactor(k);
            double x = nieuw / oud;
            helper.assertTrue(x > 0.9 && x < 1.8, "TTK lv " + lv + " x" + x);
        }
        helper.assertTrue(GrotBaas.xpFactor(1) == 0.5 && GrotBaas.xpFactor(2) == 0.5 && GrotBaas.xpFactor(3) == 0.63, "dungeon boss share of a level");
        long zwamgrot = Math.round(Levels.xpVoor(14) * 0.5);
        helper.assertTrue(zwamgrot > 850 && zwamgrot < 900, "Zwamgrot boss lv 14: ~870 xp (was 262): " + zwamgrot);
        helper.succeed();
    }
}
