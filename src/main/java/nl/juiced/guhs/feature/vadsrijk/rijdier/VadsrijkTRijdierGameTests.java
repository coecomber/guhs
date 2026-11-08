package nl.juiced.guhs.feature.vadsrijk.rijdier;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.PluimKleur;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdier;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierEntity;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren;
import nl.juiced.guhs.feature.vadsrijk.huisdieren.HuisdierenRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.As;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.Variant;
import nl.juiced.guhs.feature.vadsrijk.zone3.systemen.GoudenVeren;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * T-RIJDIER: the new mounts and the mount looks (roster, traits, every texture/model on the classpath, owning + wearing + the synced
 * look, stal / Gouden Veren / boss / achievement / secret sources, the sturdy Zwamzwijntje), and the pet roles (every pet has an
 * ability, levels, the Steuner heals/shields/energy, the Snuffelaar picks up loot and sniffs out spots, Vahoegje grows).
 * Run: {@code runGameTestServer -Pgt=VadsrijkTRijdier}.
 */
public class VadsrijkTRijdierGameTests {
    private static final String BATCH = "vadsrijk_t_rijdier";

    private static boolean bestaat(String pad) {
        return VadsrijkTRijdierGameTests.class.getResource("/assets/guhs/" + pad) != null;
    }

    private static void schoon(ServerPlayer p) {
        Rijdieren.reset(p);
        Aankleden.reset(p);
        Huisdieren.reset(p);
        Maatjes.wisCooldowns(p);
        Maatjes.zetGevecht(p, false);
        Spelers.data(p).extra("huisdieren").remove("lv");
        Spelers.data(p).extra("huisdieren").remove("xp");
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void roosterEnBestanden(GameTestHelper helper) {
        for (RijdierSoort s : new RijdierSoort[] {RijdierSoort.DUINHUPPER, RijdierSoort.WOLKWALKALFJE, RijdierSoort.ZWAMZWIJNTJE}) {
            helper.assertTrue(EcoSpul.type(s) != null && EcoSpul.ZADELS.get(s) != null, "entity + saddle: " + s.id());
            helper.assertTrue(BuiltInRegistries.ITEM.getOptional(Guhs.id("vr_item_zadel_" + s.id())).isPresent(), "saddle id " + s.id());
            helper.assertTrue(bestaat("geckolib/models/entity/vr_eco_" + s.id() + ".geo.json") && bestaat("geckolib/animations/entity/vr_eco_" + s.id() + ".animation.json"),
                    "model + animations " + s.id());
            helper.assertTrue(Aankleden.SOORT_BRON.containsKey(s), "a source for " + s.id());
        }
        helper.assertTrue(RijdierSoort.WOLKWALKALFJE.zweeft() && RijdierSoort.WOLKENSCHAAPJE.zweeft() && !RijdierSoort.DUINHUPPER.zweeft(), "who floats");
        helper.assertTrue(RijdierSoort.ZWAMZWIJNTJE.stevig() && !RijdierSoort.PLUIMGUH.stevig(), "who is sturdy");
        for (RijdierSoort s : RijdierSoort.values()) {
            if (s != RijdierSoort.DUINHUPPER) {
                helper.assertTrue(RijdierSoort.DUINHUPPER.sprong(1) > s.sprong(1), "the Duinhupper jumps highest (vs " + s.id() + ")");
            }
        }
        helper.assertTrue(PluimKleur.values().length == 9 && EcoSpul.PLUIMEI.get(PluimKleur.KRISTAL) != null, "9 Pluimguh colours with eggs");
        // every look's files exist (coats, patterns, tack items)
        for (Variant v : Uiterlijk.alle()) {
            if (v.as() == As.VACHT && !v.id().equals(Uiterlijk.NATUREL)) {
                helper.assertTrue(bestaat("textures/entity/vr_eco_" + v.soort().id() + "_" + v.id() + ".png"), "coat texture " + v.sleutel());
            }
            if (v.as() == As.TUIG) {
                helper.assertTrue(RijdierExtraRegistry.TUIG.containsKey(v.id()) && bestaat("models/item/vr_rijtuig_" + v.id() + ".json"), "tack item " + v.id());
            }
        }
        for (RijdierSoort s : RijdierSoort.values()) {
            if (!Uiterlijk.heeftVarianten(s)) {
                continue;
            }
            helper.assertTrue(s.kleuren() || Uiterlijk.van(s, As.VACHT, Uiterlijk.NATUREL) != null, "a naturel coat: " + s.id());
            for (Variant v : Uiterlijk.van(s, As.PATROON)) {
                helper.assertTrue(bestaat("textures/entity/vr_rijpatroon_" + s.id() + "_" + v.id() + ".png"), "pattern texture " + s.id() + " " + v.id());
            }
        }
        for (PluimKleur k : PluimKleur.values()) {
            helper.assertTrue(bestaat("textures/entity/vr_eco_pluimguh_" + k.id() + ".png"), "Pluimguh texture " + k.id());
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void aankledenEnKopen(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        try {
            helper.assertTrue(Aankleden.inhalen(p) == 0, "nothing before the first mount");
            Rijdieren.geefKleur(p, PluimKleur.ROZE);
            Rijdieren.geef(p, RijdierSoort.KNUFFELHERT);
            helper.assertTrue(Aankleden.uiterlijk(p, RijdierSoort.KNUFFELHERT).equals("||"), "plain look at first");
            helper.assertTrue(Aankleden.heeft(p, Uiterlijk.DEKJE) && !Aankleden.heeft(p, Uiterlijk.HARTJES), "the blanket comes with every mount");
            helper.assertTrue(!Aankleden.kies(p, RijdierSoort.KNUFFELHERT, As.PATROON, "hartjes"), "can't wear what you don't own");
            helper.assertTrue(Aankleden.kies(p, RijdierSoort.KNUFFELHERT, As.TUIG, "dekje"), "the blanket on");
            // the stal: coins
            Spelers.data(p).munten = 0;
            CompoundTag a = new CompoundTag();
            a.putString("soort", "knuffelhert");
            a.putString("as", "vacht");
            a.putString("id", "sneeuw");
            helper.assertTrue("vadsrijk.rijdier.alleen_stal".equals(Aankleden.doe(p, false, "koop_look", a)), "buying only at the stal");
            helper.assertTrue("vadsrijk.economie.smid.te_weinig".equals(Aankleden.doe(p, true, "koop_look", a)), "too poor");
            Spelers.data(p).munten = 5000;
            helper.assertTrue("vadsrijk.rijdier.gekocht".equals(Aankleden.doe(p, true, "koop_look", a)) && Spelers.data(p).munten == 3500, "bought for 15 zilver");
            helper.assertTrue("vadsrijk.rijdier.aangekleed".equals(Aankleden.doe(p, false, "kleed", a)), "put on via the screen action");
            helper.assertTrue(Aankleden.uiterlijk(p, RijdierSoort.KNUFFELHERT).equals("sneeuw||dekje"), "coat + tack: " + Aankleden.uiterlijk(p, RijdierSoort.KNUFFELHERT));
            // Gouden Veren
            GoudenVeren.geef(p, 26);
            CompoundTag r = new CompoundTag();
            r.putString("soort", "knuffelhert");
            r.putString("as", "patroon");
            r.putString("id", "hartjes");
            helper.assertTrue("vadsrijk.rijdier.geruild".equals(Aankleden.doe(p, false, "ruil", r)) && Aankleden.heeft(p, Uiterlijk.HARTJES), "hearts for 5 feathers");
            CompoundTag w = new CompoundTag();
            w.putString("soort", "wolkwalkalfje");
            w.putString("as", "soort");
            helper.assertTrue("vadsrijk.rijdier.geruild".equals(Aankleden.doe(p, false, "ruil", w)) && Rijdieren.heeft(p, RijdierSoort.WOLKWALKALFJE),
                    "the Wolkwalkalfje for 20 feathers");
            CompoundTag l = new CompoundTag();
            l.putString("soort", "pluimguh");
            l.putString("as", "pluimkleur");
            l.putString("id", "lampion");
            helper.assertTrue("vadsrijk.rijdier.te_weinig_veren".equals(Aankleden.doe(p, false, "ruil", l)) && !Rijdieren.heeftKleur(p, PluimKleur.LAMPION),
                    "1 feather left: no lantern Pluimguh");
            // the synced look on the summoned mount, and a change while riding
            Aankleden.kies(p, RijdierSoort.KNUFFELHERT, As.PATROON, "hartjes");
            RijdierEntity re = Rijdieren.roep(p, RijdierSoort.KNUFFELHERT);
            helper.assertTrue(re != null && re.uiterlijk().equals("sneeuw|hartjes|dekje"), "the mount wears it: " + (re == null ? null : re.uiterlijk()));
            Aankleden.kies(p, RijdierSoort.KNUFFELHERT, As.TUIG, "");
            helper.assertTrue(re.uiterlijk().equals("sneeuw|hartjes|"), "taking off the tack shows at once");
            Aankleden.kies(p, RijdierSoort.KNUFFELHERT, As.VACHT, Uiterlijk.NATUREL);
            helper.assertTrue(re.uiterlijk().startsWith("|"), "naturel = the plain coat");
            Rijdieren.poef(re);
            // the screen data carries the looks
            CompoundTag t = new CompoundTag();
            Aankleden.schermData(p, RijdierSoort.KNUFFELHERT, t);
            helper.assertTrue(t.getListOrEmpty("looks").size() == Uiterlijk.van(RijdierSoort.KNUFFELHERT, As.VACHT).size()
                    + Uiterlijk.van(RijdierSoort.KNUFFELHERT, As.PATROON).size() + Uiterlijk.van(RijdierSoort.KNUFFELHERT, As.TUIG).size(), "screen data");
        } finally {
            schoon(p);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void bronnen(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        try {
            Rijdieren.geefKleur(p, PluimKleur.ROZE);
            Rijdieren.geef(p, RijdierSoort.GUHWOLF);
            // bosses
            Spelers.data(p).gezien.add("baaskill:guhs:vr_baas_opperzwam");
            Spelers.data(p).gezien.add("baaskill:guhs:vr_z2_baas_spiegelzucht");
            Spelers.data(p).gezien.add("baaskill:guhs:vr_z3_baas_vahoegnir");
            int n = Aankleden.inhalen(p);
            // V-INT (user's loot rule): these two mounts are 10 % drops with pity now (vint.RijdierBuit), not first-kill gifts
            helper.assertTrue(!Rijdieren.heeft(p, RijdierSoort.ZWAMZWIJNTJE), "the Opperzwam's Zwamzwijntje is a drop now");
            helper.assertTrue(!Rijdieren.heeftKleur(p, PluimKleur.KRISTAL), "Spiegelzucht's crystal Pluimguh is a drop now");
            helper.assertTrue(Aankleden.heeft(p, Uiterlijk.van(RijdierSoort.GUHWOLF, As.VACHT, "middernacht")), "and the midnight Guhwolf coat");
            helper.assertTrue(Aankleden.heeft(p, Uiterlijk.GLITTER), "Vahoegnir gives glitter");
            helper.assertTrue(n >= 2 && Aankleden.inhalen(p) == 0, "only once (" + n + ")");   // V-INT: 2 mounts became drops
            // an achievement: a mount at level 10
            helper.assertTrue(!Aankleden.heeft(p, Uiterlijk.KROONTJE), "no crown yet");
            Rijdieren.geefXp(p, RijdierSoort.GUHWOLF, 100000);
            Aankleden.inhalen(p);
            helper.assertTrue(Aankleden.heeft(p, Uiterlijk.KROONTJE), "Rijdiermeester: the crown");
            // a secret: 10 hidden knuffels
            CompoundTag k = Questlog.map(p, "knuffels");
            for (int i = 0; i < 6; i++) {
                k.putBoolean("t" + i, true);
            }
            CompoundTag k2 = Questlog.map(p, "z2_knuffels");
            for (int i = 0; i < 3; i++) {
                k2.putBoolean("t" + i, true);
            }
            Aankleden.inhalen(p);
            helper.assertTrue(!Rijdieren.heeft(p, RijdierSoort.DUINHUPPER) && Prestaties.geheimen(p, "knuffels") == 9, "9 knuffels: not yet");
            k2.putBoolean("t9", true);
            Aankleden.inhalen(p);
            helper.assertTrue(Rijdieren.heeft(p, RijdierSoort.DUINHUPPER), "10 knuffels (all zones): the Duinhupper");
        } finally {
            for (String m : new String[] {"knuffels", "z2_knuffels"}) {
                CompoundTag t = Questlog.map(p, m);
                for (String key : List.copyOf(t.keySet())) {
                    t.remove(key);
                }
            }
            schoon(p);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void stevigZwijntje(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        try {
            Rijdieren.geef(p, RijdierSoort.ZWAMZWIJNTJE);
            RijdierEntity r = Rijdieren.roep(p, RijdierSoort.ZWAMZWIJNTJE);
            helper.assertTrue(r != null && p.getVehicle() == r, "riding the piglet");
            ServerLevel sl = helper.getLevel();
            r.hurtServer(sl, sl.damageSources().generic(), 1);
            helper.assertTrue(r.isAlive() && p.getVehicle() == r, "the first hit: it stays");
            r.hurtServer(sl, sl.damageSources().generic(), 1);
            helper.assertTrue(!r.isAlive() && p.getVehicle() == null, "the second hit: it runs off");
            Rijdieren.geef(p, RijdierSoort.DUINHUPPER);
            RijdierEntity d = Rijdieren.roep(p, RijdierSoort.DUINHUPPER);
            d.hurtServer(sl, sl.damageSources().generic(), 1);
            helper.assertTrue(!d.isAlive(), "a normal mount runs off at the first hit");
        } finally {
            schoon(p);
        }
        helper.succeed();
    }

    private static HuisdierEntity dier(GameTestHelper helper, ServerPlayer p, String id) {
        Huisdieren.geef(p, id, 0);
        Huisdieren.kies(p, id);
        HuisdierEntity e = Huisdieren.roepVoorTest(p, Huisdieren.van(id));
        helper.assertTrue(e != null, "pet summoned: " + id);
        return e;
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void huisdierRollen(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        schoon(p);
        try {
            for (Huisdier h : Huisdieren.alle()) {
                Maatjes.Kracht k = Maatjes.van(h);
                helper.assertTrue(k != null && k.effect() != null, "every pet has an ability: " + h.id());
            }
            helper.assertTrue(Maatjes.van(Huisdieren.van("vahoegje")) == Maatjes.DRAKENWARMTE && Maatjes.DRAKENWARMTE.rol() == Maatjes.Rol.STEUNER, "Vahoegje supports");
            helper.assertTrue(Maatjes.van(Huisdieren.van("piepje")).rol() == Maatjes.Rol.SNUFFELAAR, "Piepje sniffs");
            helper.assertTrue(Maatjes.van(new Huisdier("onbekend_t", "guhs:x", "m", "t", "a", 1f, null)) == Maatjes.STANDAARD, "an unknown pet gets the default");
            // levels
            Huisdieren.geef(p, "koning_klodder", 0);
            helper.assertTrue(Maatjes.level(p, "koning_klodder") == 1, "level 1");
            helper.assertTrue(Maatjes.geefXp(p, "koning_klodder", 12) == 2, "12 xp: level 2");
            helper.assertTrue(Maatjes.geefXp(p, "koning_klodder", 100000) == Maatjes.MAX && Maatjes.xp(p, "koning_klodder") == 0, "max level");
            // Steuner: the Koning heals out of combat
            HuisdierEntity e = dier(helper, p, "koning_klodder");
            double max = Spelers.maxVads(p);
            Spelers.data(p).vads = max * 0.5;
            Maatjes.KUSSENTJE.effect().tick(p, e);
            helper.assertTrue(Spelers.vads(p) > max * 0.5, "regen out of combat");
            Maatjes.zetGevecht(p, true);
            double v = Spelers.vads(p);
            Maatjes.KUSSENTJE.effect().tick(p, e);
            helper.assertTrue(Spelers.vads(p) == v, "no regen in combat");
            helper.assertTrue(Maatjes.KUSSENTJE.effect().extraStats(p).get(Stat.HEALKRACHT) > 0, "passive: healing");
            // Steuner: the Klodderreus shields when you're low, with a cooldown
            HuisdierEntity e2 = dier(helper, p, "klodderreus");
            Spelers.data(p).vads = max * 0.3;
            Maatjes.KLODDERSCHILD.effect().tick(p, e2);
            helper.assertTrue(Statussen.heeft(p, Status.KNUFFELSCHILD), "a blob shield");
            Statussen.wis(p, Status.KNUFFELSCHILD);
            Maatjes.KLODDERSCHILD.effect().tick(p, e2);
            helper.assertTrue(!Statussen.heeft(p, Status.KNUFFELSCHILD), "cooldown: not again right away");
            // Vahoegje: small heals; grown also shields
            HuisdierEntity e3 = dier(helper, p, "vahoegje");
            Maatjes.wisCooldowns(p);
            Maatjes.zetGevecht(p, true);
            Vahoegje.zetDev(p, Vahoegje.KLEIN);
            Spelers.data(p).vads = max * 0.2;
            Maatjes.DRAKENWARMTE.effect().tick(p, e3);
            helper.assertTrue(Spelers.vads(p) > max * 0.3 && !Statussen.heeft(p, Status.KNUFFELSCHILD), "small: a pink flame heals");
            Maatjes.wisCooldowns(p);
            Maatjes.zetGevecht(p, true);
            Vahoegje.zetDev(p, Vahoegje.MIDDEL);
            Spelers.data(p).vads = max * 0.2;
            Maatjes.DRAKENWARMTE.effect().tick(p, e3);
            helper.assertTrue(Spelers.vads(p) > max * 0.38 && Statussen.heeft(p, Status.KNUFFELSCHILD), "grown: heals more + a shield");
            helper.assertTrue(Maatjes.DRAKENWARMTE.effect().extraStats(p).get(Stat.LOOPSNELHEID) > 0, "grown: a bigger passive");
            Vahoegje.zetDev(p, -1);
            Statussen.wis(p, Status.KNUFFELSCHILD);
            // Steuner: the Galmwolf sings energy
            HuisdierEntity e4 = dier(helper, p, "galmwolf");
            Maatjes.zetGevecht(p, true);
            Spelers.data(p).energie = 0;
            Maatjes.GALMLIED.effect().tick(p, e4);
            helper.assertTrue(Spelers.energie(p) > 0, "energy in combat");
            // Snuffelaar: Piepje picks up your loot (vanilla pickup rules)
            HuisdierEntity e5 = dier(helper, p, "piepje");
            ServerLevel sl = helper.getLevel();
            ItemEntity it = new ItemEntity(sl, p.getX() + 3, p.getY(), p.getZ(), new ItemStack(Items.GOLD_NUGGET, 3));
            it.setTarget(p.getUUID());
            it.setNoPickUpDelay();
            sl.addFreshEntity(it);
            int voor = p.getInventory().countItem(Items.GOLD_NUGGET);
            int n = Maatjes.raap(p, e5, 6);
            helper.assertTrue(n == 1 && p.getInventory().countItem(Items.GOLD_NUGGET) == voor + 3, "Piepje picked up the loot");
            // Snuffelaar: sniffing a spot (an extra source), announced once
            BlockPos plek = p.blockPosition().offset(8, 0, 0);
            java.util.function.BiFunction<ServerPlayer, Double, List<BlockPos>> bron = (sp, r) -> sp == p ? List.of(plek) : List.of();
            Snuffelen.BRONNEN.add(bron);
            try {
                helper.assertTrue(plek.equals(Snuffelen.dichtstbij(p, 24)), "the nearest spot");
                java.util.Set<Long> gemeld = new java.util.HashSet<>();
                helper.assertTrue(plek.equals(Snuffelen.snuffel(p, e5, 24, gemeld)) && gemeld.size() == 1, "sniffed + announced");
                Snuffelen.snuffel(p, e5, 24, gemeld);
                helper.assertTrue(gemeld.size() == 1, "announced only once");
            } finally {
                Snuffelen.BRONNEN.remove(bron);
            }
            // the P screen data
            CompoundTag d = new CompoundTag();
            Maatjes.schermData(p, d);
            helper.assertTrue(d.getCompoundOrEmpty("levels").getCompoundOrEmpty("koning_klodder").getIntOr("lv", 0) == Maatjes.MAX, "levels in the screen data");
            // the pet entity: never hurt
            helper.assertTrue(!e5.hurtServer(sl, sl.damageSources().generic(), 100) && e5.isAlive(), "pets are never hurt");
            HuisdierEntity proef = HuisdierenRegistry.HUISDIER.get().create(sl, EntitySpawnReason.COMMAND);
            helper.assertTrue(proef != null && proef.grootte() == 1f, "normal size by default");
        } finally {
            schoon(p);
        }
        helper.succeed();
    }
}
