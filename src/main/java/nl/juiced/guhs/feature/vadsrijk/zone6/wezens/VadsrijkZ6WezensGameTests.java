package nl.juiced.guhs.feature.vadsrijk.zone6.wezens;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.geluid.GeluidPlan;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Uitrusting;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Aankleden;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Kristallen;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Doel;
import nl.juiced.guhs.feature.vadsrijk.zone6.bultguh.Bultguh;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Astrolabiumwachter;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Glimmertje;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Sterrenguh;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Wormelien;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Z6Baas;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Z6BazenLoot;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas.Zoutkolos;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.loot.Z6Loot;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.loot.Z6Sets;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.mob.Z6Wezen;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.mob.Zout;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * Zone-6 wezens (U-Z6WEZENS): every creature spawns and ends right (cheered up / Mika smoke / a construct crumbles), the loot (sets
 * with the new slots, jewellery, spec weapons, crystals, boss chests), Koningin Wormelien (dive, dune spit, salt crust), De
 * Zoutkolos (crack lines, singing crystals, the glowing heart), the dungeon (rooms, the aligned rings, the fire bowls, the
 * telescopes + the Sterrenkompas), the Bultguh (two seats, sand speed) and the music hooks. Run:
 * {@code runGameTestServer -Pgt=VadsrijkZ6Wezens}. Each test its own batch.
 */
public class VadsrijkZ6WezensGameTests {
    private static final String BATCH = "vadsrijk_z6wezens";

    private static void vloer(GameTestHelper helper, int r) {
        ServerLevel sl = helper.getLevel();
        BlockPos c = helper.absolutePos(new BlockPos(1, 1, 1));
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                sl.setBlock(c.offset(x, 0, z), Blocks.SAND.defaultBlockState(), 2);
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

    private static <T extends Z6Baas> T zet(ServerLevel sl, EntityType<T> type, Vec3 c, int lv) {
        T b = type.create(sl, EntitySpawnReason.COMMAND);
        b.snapTo(c.x, c.y, c.z, 0, 0);
        b.stelIn(lv, null, false, Set.of());
        b.zetThuis(c);
        sl.addFreshEntity(b);
        return b;
    }

    /** All 14 kinds (13 + the cave elite): level 64 with real Vads and loot tables; beaten they end the right way. */
    @GuhTest(template = "empty", batch = BATCH + "_wezens", timeoutTicks = 200)
    public static void alleWezens(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        vloer(helper, 6);
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 6))), false);
        Vec3 pos = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
        helper.assertTrue(Z6WezensRegistry.SOORTEN.size() == 14, "13 creatures + the elite, got " + Z6WezensRegistry.SOORTEN.size());
        List<Z6Wezen> alle = new ArrayList<>();
        for (String soort : Z6WezensRegistry.SOORTEN.keySet()) {
            Entity e = Z6WezensCommando.maak(sl, soort, pos, 0, 64, false, EnumSet.noneOf(Affix.class));
            helper.assertTrue(e instanceof Z6Wezen, "made " + soort);
            Z6Wezen w = (Z6Wezen) e;
            helper.assertTrue(w.mobLevel() == 64 && w.maxVads() > 200, soort + ": level 64 with real Vads, got " + w.maxVads());
            helper.assertTrue(w.ontvadst() == (w.afloop() == Z6Wezen.Afloop.OPVROLIJKEN), soort + ": ontvadst only when cheered up");
            helper.assertTrue(LootTabellen.van(w.lootTabel()) != null && LootTabellen.van(w.lootTabel() + "_elite") != null, soort + ": loot tables");
            w.zetHouding(0);
            if (w instanceof Zout.Spiegelslang s && s.kopie()) {
                continue;
            }
            w.krijgSchade(w.vads() + 1, p);
            helper.assertTrue(w.geplofd(), soort + ": beaten / cheered up");
            alle.add(w);
        }
        long opgevrolijkt = alle.stream().filter(w -> w.afloop() == Z6Wezen.Afloop.OPVROLIJKEN).count();
        helper.assertTrue(opgevrolijkt >= 8, "most of them are cheered up (sweet zone): " + opgevrolijkt);
        helper.runAfterDelay(20, () -> {
            for (Z6Wezen w : alle) {
                boolean blijft = w.afloop() == Z6Wezen.Afloop.OPVROLIJKEN;
                helper.assertTrue(w.isRemoved() != blijft, w.naam() + (blijft ? ": cheered up, still hopping around" : ": gone (smoke / crumbled)"));
            }
            alle.forEach(Entity::discard);
            helper.succeed();
        });
    }

    /** The mirage snake makes two copies that never hurt and burst at a touch; the salt crayfish is soft only from behind. */
    @GuhTest(template = "empty", batch = BATCH + "_zout", timeoutTicks = 100)
    public static void spiegelslangEnKreeft(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        vloer(helper, 6);
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 5))), false);
        Zout.Spiegelslang s = (Zout.Spiegelslang) Z6WezensCommando.maak(sl, "spiegelslang", Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1))), 0,
                64, false, EnumSet.noneOf(Affix.class));
        s.spiegel(sl);
        helper.assertTrue(s.kopieen() == 2, "two mirage copies");
        List<Zout.Spiegelslang> kopie = sl.getEntitiesOfClass(Zout.Spiegelslang.class, new AABB(s.blockPosition()).inflate(8), Zout.Spiegelslang::kopie);
        helper.assertTrue(kopie.size() == 2, "the copies exist: " + kopie.size());
        helper.assertTrue(kopie.get(0).klap(p, 1, nl.juiced.guhs.feature.vadsrijk.kern.Schade.Soort.MELEE, "test") == null, "a copy never hurts");
        kopie.get(0).krijgSchade(1, p);
        helper.assertTrue(kopie.get(0).isRemoved(), "a copy bursts at the first touch");
        Zout.Zoutkreeft k = (Zout.Zoutkreeft) Z6WezensCommando.maak(sl, "zoutkreeft", Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 2, 1))), 0,
                64, false, EnumSet.noneOf(Affix.class));
        k.setYRot(0);   // facing +z
        ServerPlayer voor = speler(helper, k.position().add(0, 0, 3), false), achter = speler(helper, k.position().add(0, 0, -3), false);
        helper.assertTrue(k.schildFactor(voor) < 0.5 && k.schildFactor(achter) > 1.2, "crust in front (" + k.schildFactor(voor) + "), soft behind ("
                + k.schildFactor(achter) + ")");
        s.discard();
        k.discard();
        helper.succeed();
    }

    /** Loot: every piece has a base type (sets incl. gloves/cape/armband, rings for ring 1 + 2), weapons with a spec, crystals, boss chests. */
    @GuhTest(template = "empty", batch = BATCH + "_loot", timeoutTicks = 60)
    public static void buit(GameTestHelper helper) {
        ServerPlayer p = speler(helper, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1))), false);
        for (String id : Z6Loot.STUKKEN.keySet()) {
            helper.assertTrue(BasisTypes.van(id) != null && Z6WezensRegistry.ITEMS_PER_TYPE.containsKey(id), "base type + item: " + id);
        }
        for (String w : Z6Loot.WAPENS) {
            helper.assertTrue(BasisTypes.van(w) != null && Z6Loot.SPEC.containsKey(w), "weapon with a spec style: " + w);
        }
        helper.assertTrue(Z6Sets.stukkenVan(Z6Sets.DUINWANDELAAR).size() == 6 && Z6Sets.stukkenVan(Z6Sets.STERRENWACHT).size() == 6
                && Z6Sets.stukkenVan(Z6Sets.KARAVAANWACHT).size() == 4, "set sizes 6/6/4/6");
        helper.assertTrue(BasisTypes.van("woestijnroosring").slot() == nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot.RING
                && nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot.RING2.past(BasisTypes.van("woestijnroosring").slot()), "rings fit ring 2");
        for (String k : Z6Loot.KRISTALLEN) {
            helper.assertTrue(Kristallen.SOORTEN.containsKey(k), "crystal " + k);
        }
        for (String baas : Z6BazenLoot.BAZEN) {
            LootTabel.Resultaat r = Z6BazenLoot.kist(baas, p, 66, p.getRandom());
            boolean setStuk = false, benoemd = false;
            for (ItemStack s : r.items()) {
                Uitrusting u = Rollen.van(s);
                if (u != null && Z6Sets.setVan(u.type()) != null && u.zeldzaamheid().ordinal() >= Zeldzaamheid.EPISCH.ordinal()) {
                    setStuk = true;
                }
                if (u != null && u.type().startsWith("z6baas_") && u.zeldzaamheid() == Zeldzaamheid.LEGENDARISCH) {
                    benoemd = true;
                }
            }
            helper.assertTrue(setStuk && benoemd, baas + ": first chest has an epic set piece and a named Legendary");
        }
        helper.succeed();
    }

    /** Koningin Wormelien: she dives (immune), the dune spit buries whoever is outside the pink spots, the salt crust, cheered up. */
    @GuhTest(template = "empty", batch = BATCH + "_wormelien", timeoutTicks = 260)
    public static void wormelien(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        vloer(helper, 14);
        Vec3 c = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
        Wormelien w = zet(sl, Z6WezensRegistry.WORMELIEN.get(), c, 63);
        ServerPlayer p = speler(helper, c.add(6, 0, 0), true);
        w.testFilter = q -> q == p;
        helper.runAfterDelay(30, () -> {
            Statussen.geef(w, Status.VERDOOFD, 2000, 1, null);
            w.duik(sl, p);
            double v = w.vads();
            w.krijgSchade(1000, p);
            helper.assertTrue(w.onder() && w.vads() == v, "under the sand: can't be hit");
            w.opduiken(sl);
            helper.assertTrue(!w.onder() && w.duiken == 1, "up again");
            w.duinspuug(sl);   // the player stands somewhere: most likely not in a pink spot
        });
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(w.begraven + 1 > 0, "the dune spit ran");
            w.korstOp(sl);
            helper.assertTrue(w.heeftKorst(), "fase 3: the salt crust");
            double v = w.vads();
            w.krijgSchade(1000, p);
            helper.assertTrue(w.vads() == v, "immune while the crust sits");
            List<Z3Doel> korst = sl.getEntitiesOfClass(Z3Doel.class, w.getBoundingBox().inflate(6), d -> d.getType() == Z6WezensRegistry.ZOUTKORST.get());
            helper.assertTrue(korst.size() == 1, "one crust on her back");
            korst.get(0).krijgSchade(korst.get(0).vads() + 1, p);
            helper.assertTrue(!w.heeftKorst(), "crust knocked off");
        });
        helper.runAfterDelay(140, () -> {
            double v = w.vads();
            w.krijgSchade(100, p);
            helper.assertTrue(v - w.vads() > 120, "after the crust: +50 % (" + (v - w.vads()) + ")");
            w.krijgSchade(w.vads() + 10, p);
            helper.assertTrue(w.geplofd() && w.blij(), "cheered up: pink, asleep in the sand");
            w.discard();
            helper.succeed();
        });
    }

    /** De Zoutkolos: crack lines, singing crystals, and the glowing heart doubles the damage of someone who looks at it. */
    @GuhTest(template = "empty", batch = BATCH + "_zoutkolos", timeoutTicks = 220)
    public static void zoutkolos(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        vloer(helper, 14);
        Vec3 c = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
        Zoutkolos k = zet(sl, Z6WezensRegistry.ZOUTKOLOS.get(), c, 66);
        ServerPlayer p = speler(helper, c.add(7, 0, 0), true);
        k.testFilter = q -> q == p;
        helper.runAfterDelay(20, () -> {
            Statussen.geef(k, Status.VERDOOFD, 2000, 1, null);
            k.zoutgolven(sl);
            k.kristallenGooien(sl);
        });
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(k.golven >= 1 && k.kristallen() == 3, "crack lines + 3 crystals (" + k.kristallen() + ")");
            k.zing(sl);
        });
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(k.gezongen >= 1, "the crystals sang");
            k.krijgSchade(k.vads() - k.maxVads() * 0.25, p);
            helper.assertTrue(k.fase() == 3 && k.gloeit(), "fase 3: the heart glows");
            p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, k.position().add(0, k.getBbHeight() * 0.55, 0));
            helper.assertTrue(k.hartFactor(p) >= 2.0, "looking at the heart: double damage");
            k.krijgSchade(k.vads() + 10, p);
            helper.assertTrue(k.geplofd(), "it falls apart into salt");
            helper.succeed();
        });
    }

    /** The dungeon: the rooms (arena centres inside), the rings only take full damage while aligned, the dark bowls, the telescopes. */
    @GuhTest(template = "empty", batch = BATCH + "_kerker", timeoutTicks = 260)
    public static void kerker(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        Set<Long> ruimte = Z6Bouw.ruimte();
        for (BlockPos b : List.of(Z6WezensPlekken.KERKER_AANKOMST, Z6WezensPlekken.RINGENZAAL, Z6WezensPlekken.KAARTENZAAL, Z6WezensPlekken.VUURSCHALENZAAL,
                Z6WezensPlekken.KOEPEL, Z6WezensPlekken.KERKER_UITGANG)) {
            helper.assertTrue(ruimte.contains(b.asLong()) && ruimte.contains(b.above().asLong()), "room air at " + b.toShortString());
            helper.assertTrue(!ruimte.contains(b.below().asLong()), "a floor under " + b.toShortString());
        }
        vloer(helper, 14);
        Vec3 c = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
        ServerPlayer p = speler(helper, c.add(6, 0, 0), true);
        Astrolabiumwachter a = zet(sl, Z6WezensRegistry.ASTROLABIUMWACHTER.get(), c, 66);
        a.testFilter = q -> q == p;
        helper.runAfterDelay(10, () -> {
            Statussen.geef(a, Status.VERDOOFD, 2000, 1, null);
            double v = a.vads();
            a.krijgSchade(400, p);
            double scheef = v - a.vads();
            a.lijnUit(sl);
            v = a.vads();
            a.krijgSchade(400, p);
            double recht = v - a.vads();
            helper.assertTrue(recht > scheef * 3, "aligned: full damage (" + recht + " vs " + scheef + ")");
            a.ringzwaai(sl);
            a.discard();
        });
        Glimmertje g = zet(sl, Z6WezensRegistry.GLIMMERTJE.get(), c.add(0, 0, 4), 67);
        g.testFilter = q -> q == p;
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(g.schalen().size() == 4, "4 fire bowls (" + g.schalen().size() + ")");
            for (Z3Doel d : g.schalen()) {
                g.doof(sl, d, 0);
            }
            double v = g.vads();
            g.krijgSchade(500, p);
            helper.assertTrue(g.uit() == 4 && g.vads() == v, "all out: he hides in the dark");
            Z3Doel d = g.schalen().get(0);
            g.doelGebruikt(d, p);
            g.doelGebruikt(d, p);
            helper.assertTrue(g.uit() == 3, "two clicks light a bowl");
            g.krijgSchade(500, p);
            helper.assertTrue(g.vads() < v, "hittable again");
            g.discard();
            helper.succeed();
        });
    }

    /** The Verzonken Sterrenguh: four telescopes, all aimed = his heart lights (x2), cheered up he gives the Sterrenkompas. */
    @GuhTest(template = "empty", batch = BATCH + "_sterrenguh", timeoutTicks = 200)
    public static void sterrenguh(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        vloer(helper, 14);
        Vec3 c = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
        ServerPlayer p = speler(helper, c.add(6, 0, 0), true);
        Sterrenguh s = zet(sl, Z6WezensRegistry.STERRENGUH.get(), c, 68);
        s.testFilter = q -> q == p;
        helper.runAfterDelay(90, () -> {
            helper.assertTrue(s.telescopen().size() == 4 && s.gericht() == 0, "4 telescopes, none aimed yet");
            for (Z3Doel d : s.telescopen()) {
                for (int i = 0; i < 4 && !s.hartAan(); i++) {
                    if (s.gericht() == 4) {
                        break;
                    }
                    int voor = s.gericht();
                    s.doelGebruikt(d, p);
                    if (s.gericht() > voor) {
                        break;
                    }
                }
            }
            helper.assertTrue(s.hartAan() && s.harten == 1, "all four aimed: his heart lights up");
            double v = s.vads();
            s.krijgSchade(100, p);
            helper.assertTrue(v - s.vads() > 190, "heart lit: double damage (" + (v - s.vads()) + ")");
            s.krijgSchade(s.vads() + 10, p);
            helper.assertTrue(s.geplofd() && s.blij(), "cheered up");
            helper.assertTrue(p.getInventory().contains(new ItemStack(Z6WezensRegistry.STERRENKOMPAS.get())), "the Sterrenkompas");
            s.discard();
            helper.succeed();
        });
    }

    /** The Bultguh: one species line, two seats (the owner steers, a passenger sits behind), faster on sand, coats + the quest API. */
    @GuhTest(template = "empty", batch = BATCH + "_bultguh", timeoutTicks = 100)
    public static void bultguh(GameTestHelper helper) {
        vloer(helper, 6);
        Vec3 c = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(1, 2, 1)));
        ServerPlayer baas = speler(helper, c, false), mee = speler(helper, c.add(1, 0, 0), false);
        helper.assertTrue(RijdierSoort.BULTGUH.zitplaatsen() == 2 && RijdierSoort.PLUIMGUH.zitplaatsen() == 1, "two seats on the Bultguh only");
        helper.assertTrue(Uiterlijk.van(RijdierSoort.BULTGUH, Uiterlijk.As.VACHT).size() == Bultguh.VACHTEN.size(), "the coats");
        helper.assertTrue(Bultguh.geef(baas, "saffraan") && Rijdieren.heeft(baas, RijdierSoort.BULTGUH), "the quest API gives it");
        helper.assertTrue(Aankleden.uiterlijk(baas, RijdierSoort.BULTGUH).startsWith("saffraan"), "in the chosen coat: " + Aankleden.uiterlijk(baas, RijdierSoort.BULTGUH));
        RijdierEntity r = Rijdieren.roep(baas, RijdierSoort.BULTGUH);
        helper.assertTrue(r != null && baas.getVehicle() == r, "the owner rides it");
        // (all at once: outside the Vadsrijk a mount hops home on its first tick)
        boolean op = mee.startRiding(r);
        helper.assertTrue(op && r.getPassengers().size() == 2, "a second player sits behind");
        helper.assertTrue(r.getControllingPassenger() == baas, "only the owner steers");
        ServerPlayer derde = speler(helper, c.add(-1, 0, 0), false);
        helper.assertTrue(!derde.startRiding(r), "no third seat");
        helper.assertTrue(r.opZand(), "it stands on sand");
        baas.stopRiding();
        helper.assertTrue(r.getControllingPassenger() != mee, "the passenger never steers");
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(r.isRemoved() && !mee.isPassenger(), "owner off: it trots home, the passenger lands");
            helper.succeed();
        });
    }

    /** The music hooks: every zone-6 boss names a theme that exists (its own when rendered, else the stand-in). */
    @GuhTest(template = "empty", batch = BATCH + "_muziek", timeoutTicks = 40)
    public static void muziek(GameTestHelper helper) {
        for (String b : Z6WezensRegistry.BAZEN.keySet()) {
            helper.assertTrue(Z6Muziek.THEMA.containsKey(b), "a theme id for " + b);
            helper.assertTrue(GeluidPlan.BAAS_THEMAS.contains(Z6Muziek.thema(b)), b + " plays an existing theme: " + Z6Muziek.thema(b));
        }
        helper.succeed();
    }
}
