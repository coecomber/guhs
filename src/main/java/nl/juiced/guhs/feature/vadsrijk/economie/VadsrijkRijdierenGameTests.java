package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort.PluimKleur;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * G-MOUNTS: the mount system's rules. Ownership and selection (per player, colours of the Pluimguh, buying at the stal), the zone-3
 * starter choice (once, only a starter colour, the quest hook fires), and the dismount rules (a hit = off + a "geschrokken"
 * cooldown, fainting = off, a boss arena = off, towns/open world = keep riding).
 * I-MOUNTS: the roster without the Hoedhupper/Klodderros (old saves ignored), the speeds/jumps, the 2-block step.
 * Run: {@code runGameTestServer -Pgt=VadsrijkRijdieren}.
 */
public class VadsrijkRijdierenGameTests {
    private static final String BATCH = "vadsrijk_rijdieren";

    @GuhTest(template = "empty", batch = BATCH)
    public static void eigendomEnKeuze(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        helper.assertTrue(Rijdieren.actief(p) == null, "no mount yet");
        for (RijdierSoort s : RijdierSoort.values()) {
            helper.assertTrue(!Rijdieren.heeft(p, s), "nothing owned: " + s.id());
        }
        // (I-MOUNTS) mounts start with the zone-3 starter: the stal sells nothing before your first mount
        Spelers.data(p).munten = 1_000_000;
        helper.assertTrue(Stal.koop(p, RijdierSoort.WOLKENSCHAAPJE, null).equals("vadsrijk.economie.stal.eerst_starter")
                && !Rijdieren.heeft(p, RijdierSoort.WOLKENSCHAAPJE) && Spelers.data(p).munten == 1_000_000, "no buying before the first mount");
        helper.assertTrue(Rijdieren.geef(p, RijdierSoort.WOLKENSCHAAPJE) && !Rijdieren.geef(p, RijdierSoort.WOLKENSCHAAPJE), "given once");
        helper.assertTrue(Rijdieren.actief(p) == RijdierSoort.WOLKENSCHAAPJE, "the first mount becomes active");
        helper.assertTrue(Rijdieren.geefKleur(p, PluimKleur.MINT) && !Rijdieren.geefKleur(p, PluimKleur.MINT), "mint once");
        helper.assertTrue(Rijdieren.heeft(p, RijdierSoort.PLUIMGUH) && Rijdieren.heeftKleur(p, PluimKleur.MINT)
                && !Rijdieren.heeftKleur(p, PluimKleur.ROZE), "pluimguh in mint only");
        helper.assertTrue(Rijdieren.actief(p) == RijdierSoort.WOLKENSCHAAPJE, "a new mount does not steal the active slot");
        helper.assertTrue(!Rijdieren.kies(p, RijdierSoort.PLUIMGUH, PluimKleur.ROZE), "no riding a colour you lack");
        helper.assertTrue(!Rijdieren.kies(p, RijdierSoort.SLAKKIE, null), "no riding a mount you lack");
        helper.assertTrue(Rijdieren.kies(p, RijdierSoort.PLUIMGUH, PluimKleur.MINT), "pick the mint pluimguh");
        helper.assertTrue(Rijdieren.actief(p) == RijdierSoort.PLUIMGUH && Rijdieren.kleur(p, RijdierSoort.PLUIMGUH) == PluimKleur.MINT, "active + colour");
        helper.assertTrue(Spelers.data(p).extra("economie").getCompoundOrEmpty("rijdieren").contains("pluimguh"), "stored per player");
        // buying at the stal
        Spelers.data(p).munten = 0;
        helper.assertTrue(Stal.koop(p, RijdierSoort.SLAKKIE, null).equals("vadsrijk.economie.smid.te_weinig") && !Rijdieren.heeft(p, RijdierSoort.SLAKKIE),
                "no money, no snail");
        Spelers.data(p).munten = RijdierSoort.SLAKKIE.prijs + 7;
        helper.assertTrue(Stal.koop(p, RijdierSoort.SLAKKIE, null).equals("vadsrijk.economie.stal.gekocht") && Rijdieren.heeft(p, RijdierSoort.SLAKKIE)
                && Spelers.data(p).munten == 7, "bought for exactly its price");
        helper.assertTrue(Stal.koop(p, RijdierSoort.WOLKENSCHAAPJE, null).equals("vadsrijk.economie.rijdier.al"), "not twice");
        helper.assertTrue(Stal.koop(p, RijdierSoort.KNUFFELHERT, null).isEmpty() && !Rijdieren.heeft(p, RijdierSoort.KNUFFELHERT), "quest mounts are not sold");
        helper.assertTrue(Stal.koop(p, RijdierSoort.PLUIMGUH, PluimKleur.STERRENNACHT).isEmpty(), "boss colours are not sold");
        Spelers.data(p).munten = PluimKleur.ROZE.prijs;
        helper.assertTrue(Stal.koop(p, RijdierSoort.PLUIMGUH, PluimKleur.ROZE).equals("vadsrijk.economie.stal.gekocht") && Rijdieren.heeftKleur(p, PluimKleur.ROZE),
                "another starter colour for sale once you have a pluimguh");
        helper.assertTrue(Rijdieren.geef(p, "guhwolf") && Rijdieren.heeft(p, RijdierSoort.GUHWOLF) && !Rijdieren.geef(p, "guhwolf"), "quest API by id");
        helper.assertTrue(Rijdieren.geef(p, "pluimguh:honing") && Rijdieren.heeftKleur(p, PluimKleur.HONING) && !Rijdieren.geef(p, "bestaatniet"),
                "colour by id, unknown id refused");
        Rijdieren.alles(p);
        for (RijdierSoort s : RijdierSoort.values()) {
            helper.assertTrue(Rijdieren.heeft(p, s) && EcoSpul.soort(EcoSpul.type(s)) == s, "alle + type: " + s.id());
        }
        for (PluimKleur k : PluimKleur.values()) {
            helper.assertTrue(Rijdieren.heeftKleur(p, k), "alle colours: " + k.id());
        }
        Rijdieren.reset(p);
        helper.assertTrue(Rijdieren.actief(p) == null && !Rijdieren.heeft(p, RijdierSoort.PLUIMGUH), "reset");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void starterKeuze(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        AtomicInteger gekozen = new AtomicInteger();
        BiConsumer<ServerPlayer, PluimKleur> haak = (q, k) -> {
            if (q == p) {
                gekozen.incrementAndGet();
            }
        };
        Rijdieren.STARTER_GEKOZEN.add(haak);
        try {
            helper.assertTrue(!Rijdieren.heeftStarter(p), "no starter yet");
            Spelers.data(p).munten = 1_000_000;
            helper.assertTrue(Stal.koop(p, RijdierSoort.PLUIMGUH, PluimKleur.MINT).equals("vadsrijk.economie.stal.eerst_starter")
                    && !Rijdieren.heeftKleur(p, PluimKleur.MINT), "no buying a starter colour before the free choice");
            helper.assertTrue(!Rijdieren.geefStarter(p, PluimKleur.HONING), "honey is no starter");
            helper.assertTrue(Rijdieren.kiesStarter(p), "the choice opens");
            helper.assertTrue(Rijdieren.geefStarter(p, PluimKleur.LAVENDEL), "pick lavender");
            helper.assertTrue(Rijdieren.heeftStarter(p) && Rijdieren.heeftKleur(p, PluimKleur.LAVENDEL) && !Rijdieren.heeftKleur(p, PluimKleur.ROZE)
                    && !Rijdieren.heeftKleur(p, PluimKleur.MINT), "only lavender for free");
            helper.assertTrue(Rijdieren.actief(p) == RijdierSoort.PLUIMGUH && Rijdieren.kleur(p, RijdierSoort.PLUIMGUH) == PluimKleur.LAVENDEL,
                    "the starter is ridden at once");
            helper.assertTrue(gekozen.get() == 1, "the quest hook fired once");
            helper.assertTrue(!Rijdieren.geefStarter(p, PluimKleur.MINT) && !Rijdieren.heeftKleur(p, PluimKleur.MINT), "only one free starter");
            helper.assertTrue(!Rijdieren.kiesStarter(p), "the choice does not open again");
            helper.assertTrue(gekozen.get() == 1, "no second hook");
            long voor = Spelers.data(p).munten;
            helper.assertTrue(Stal.koop(p, RijdierSoort.PLUIMGUH, PluimKleur.MINT).equals("vadsrijk.economie.stal.gekocht")
                    && Spelers.data(p).munten == voor - PluimKleur.MINT.prijs, "the other starters at the stal");
            Rijdieren.reset(p);
            helper.assertTrue(!Rijdieren.heeftStarter(p), "dev reset forgets the choice");
        } finally {
            Rijdieren.STARTER_GEKOZEN.remove(haak);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void rooster(GameTestHelper helper) {
        java.util.List<String> ids = java.util.Arrays.stream(RijdierSoort.values()).map(RijdierSoort::id).toList();
        helper.assertTrue(ids.equals(java.util.List.of("pluimguh", "knuffelhert", "guhwolf", "wolkenschaapje", "slakkie", "zeilbootje")), "the roster: " + ids);
        helper.assertTrue(RijdierSoort.van("hoedhupper") == null && RijdierSoort.van("klodderros") == null, "the two are gone");
        ServerPlayer p = GuhMockPlayer.of(helper);
        Rijdieren.reset(p);
        // an old save: owns the Hoedhupper (active) and the Klodderros
        net.minecraft.nbt.CompoundTag eco = Spelers.data(p).extra("economie");
        net.minecraft.nbt.CompoundTag oud = new net.minecraft.nbt.CompoundTag();
        net.minecraft.nbt.CompoundTag m = new net.minecraft.nbt.CompoundTag();
        m.putInt("level", 7);
        m.putLong("xp", 3);
        oud.put("hoedhupper", m.copy());
        oud.put("klodderros", m.copy());
        eco.put("rijdieren", oud);
        eco.putString("actief", "hoedhupper");
        helper.assertTrue(Rijdieren.actief(p) == null && !Rijdieren.heeftEen(p), "old mounts are ignored: no active mount");
        Rijdieren.toets(p);   // (must not crash; outside the Vadsrijk it just says so)
        helper.assertTrue(Rijdieren.hudStaat(p)[0] == Rijdieren.HUD_GEEN, "HUD: no mount");
        net.minecraft.nbt.CompoundTag d = Stal.data(p, true);
        helper.assertTrue(d.getListOrEmpty("rijdieren").size() == RijdierSoort.values().length, "the screen lists only the 5");
        helper.assertTrue(!Rijdieren.geef(p, "hoedhupper") && !Rijdieren.geef(p, "klodderros"), "can't be given any more");
        helper.assertTrue(Rijdieren.geef(p, RijdierSoort.GUHWOLF) && Rijdieren.actief(p) == RijdierSoort.GUHWOLF, "a real mount takes over");
        // the lv-8 quest reward: the Wolkenschaapje, or its price back when you bought it already
        helper.assertTrue(Rijdieren.questBeloning(p) && Rijdieren.heeft(p, RijdierSoort.WOLKENSCHAAPJE), "quest gives the Wolkenschaapje");
        long voor = Spelers.data(p).munten;
        helper.assertTrue(Rijdieren.questBeloning(p) && Spelers.data(p).munten == voor + RijdierSoort.WOLKENSCHAAPJE.prijs, "already had it: money back");
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(nl.juiced.guhs.Guhs.id("vr_item_zadel_klodderros")).isEmpty()
                && net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(nl.juiced.guhs.Guhs.id("vr_eco_hoedhupper")).isEmpty(),
                "items + entities removed");
        Rijdieren.reset(p);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void snelheden(GameTestHelper helper) {
        // % of the walking speed (0.1) at level 1
        int[][] pct = {{RijdierSoort.PLUIMGUH.ordinal(), 350}, {RijdierSoort.KNUFFELHERT.ordinal(), 320}, {RijdierSoort.GUHWOLF.ordinal(), 370},
                {RijdierSoort.WOLKENSCHAAPJE.ordinal(), 300}, {RijdierSoort.SLAKKIE.ordinal(), 85}};
        for (int[] x : pct) {
            RijdierSoort s = RijdierSoort.values()[x[0]];
            helper.assertTrue(Math.round(s.snelheid(1) / 0.1 * 100) == x[1], s.id() + ": " + s.snelheid(1));
        }
        double hert = RijdierSoort.springHoogte(RijdierSoort.KNUFFELHERT.sprong(1)), pluim = RijdierSoort.springHoogte(RijdierSoort.PLUIMGUH.sprong(1)),
                wolf = RijdierSoort.springHoogte(RijdierSoort.GUHWOLF.sprong(1));
        helper.assertTrue(hert > pluim && pluim > wolf, "jumps balance the speeds: hert " + hert + " > pluim " + pluim + " > wolf " + wolf);
        helper.assertTrue(hert > 4.5 && hert < 5.6 && wolf > 2.0 && wolf < 2.8, "hert ~5 blocks, wolf ~2.4: " + hert + " / " + wolf);
        for (RijdierSoort s : RijdierSoort.values()) {
            double groei = s.snelheid(RijdierSoort.MAX_LEVEL) / s.snelheid(1);
            helper.assertTrue(groei > 1.05 && groei < 1.2, s.id() + ": max level not absurd (x" + groei + ")");
            helper.assertTrue(RijdierSoort.springHoogte(s.sprong(RijdierSoort.MAX_LEVEL)) - RijdierSoort.springHoogte(s.sprong(1)) < 1.0, s.id() + ": jump growth");
            helper.assertTrue(s.snelheid(99) == s.snelheid(RijdierSoort.MAX_LEVEL), "capped at the max level");
        }
        // the balans knob
        try {
            RijdierSoort.Balans.zet(2, 1, 0, 0);
            helper.assertTrue(Math.abs(RijdierSoort.PLUIMGUH.snelheid(5) - 0.70) < 1e-9, "x2, no growth");
        } finally {
            RijdierSoort.Balans.zet(1, 1, RijdierSoort.GROEI_SNELHEID, RijdierSoort.GROEI_SPRONG);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void stapHoogte(GameTestHelper helper) {
        net.minecraft.server.level.ServerLevel sl = helper.getLevel();
        for (int x = 0; x < 12; x++) {
            for (int z = 0; z < 12; z++) {
                helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                for (int y = 4; y < 9; y++) {   // (the test structure has a barrier ceiling at y 4: head room for the step)
                    helper.setBlock(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
            }
        }
        // lane A (z 0..4): a 2-block ledge at x >= 5; lane B (z 7..11): 2 blocks + a slab (2.5) = a wall
        for (int x = 5; x < 12; x++) {
            for (int z = 0; z < 12; z++) {
                if (z == 5 || z == 6) {
                    continue;
                }
                helper.setBlock(new BlockPos(x, 2, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                helper.setBlock(new BlockPos(x, 3, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                if (z >= 7) {
                    helper.setBlock(new BlockPos(x, 4, z), net.minecraft.world.level.block.Blocks.STONE_SLAB.defaultBlockState());
                }
            }
        }
        double vloer = helper.absolutePos(new BlockPos(0, 2, 0)).getY();
        // (the test structure may be rotated: walk along relative +x in world terms)
        BlockPos o0 = helper.absolutePos(new BlockPos(0, 2, 0)), o1 = helper.absolutePos(new BlockPos(1, 2, 0));
        net.minecraft.world.phys.Vec3 dir = new net.minecraft.world.phys.Vec3(o1.getX() - o0.getX(), 0, o1.getZ() - o0.getZ());
        java.util.function.ToDoubleFunction<RijdierEntity> verder = e -> (e.getX() - (o0.getX() + 0.5)) * dir.x + (e.getZ() - (o0.getZ() + 0.5)) * dir.z;
        RijdierEntity a = stapper(helper, sl, 2, 2), b = stapper(helper, sl, 2, 9);
        try {
            for (RijdierEntity r : new RijdierEntity[] {a, b}) {
                helper.assertTrue(r.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT) == RijdierSoort.STAP,
                        "step height attribute " + RijdierSoort.STAP);
                r.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(0, -0.2, 0));
                helper.assertTrue(r.onGround() && Math.abs(r.getY() - vloer) < 0.01, "on the floor: " + r.getY());
                for (int i = 0; i < 25; i++) {
                    r.move(net.minecraft.world.entity.MoverType.SELF, new net.minecraft.world.phys.Vec3(dir.x * 0.25, -0.08, dir.z * 0.25));
                }
            }
            helper.assertTrue(Math.abs(a.getY() - (vloer + 2)) < 0.01 && verder.applyAsDouble(a) > 5,
                    "walks up a 2-block ledge without jumping: y " + a.getY() + " (floor " + vloer + "), x " + verder.applyAsDouble(a));
            helper.assertTrue(Math.abs(b.getY() - vloer) < 0.01 && verder.applyAsDouble(b) < 5,
                    "but no climbing a 2.5-block wall: y " + b.getY() + ", x " + verder.applyAsDouble(b));
        } finally {
            a.discard();
            b.discard();
        }
        helper.succeed();
    }

    private static RijdierEntity stapper(GameTestHelper helper, net.minecraft.server.level.ServerLevel sl, int x, int z) {
        RijdierEntity r = EcoSpul.type(RijdierSoort.WOLKENSCHAAPJE).create(sl, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        BlockPos o = helper.absolutePos(new BlockPos(x, 2, z));
        r.snapTo(o.getX() + 0.5, o.getY() + 0.1, o.getZ() + 0.5, -90, 0);
        r.setNoAi(true);
        sl.addFreshEntity(r);
        return r;
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void afstijgRegels(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Rijdieren.geefKleur(p, PluimKleur.ROZE);
        RijdierEntity r = Rijdieren.roep(p, RijdierSoort.PLUIMGUH);
        helper.assertTrue(r != null && p.getVehicle() == r && r.kleur() == PluimKleur.ROZE, "summoned and seated, in its colour");
        helper.assertTrue(Rijdieren.hudStaat(p)[0] == Rijdieren.HUD_RIJDT, "HUD: riding");
        Rijdieren.geraakt(p);
        helper.assertTrue(!r.isAlive() && p.getVehicle() == null, "a hit: off it goes");
        helper.assertTrue(Rijdieren.cooldown(p) > 0 && Rijdieren.hudStaat(p)[0] == Rijdieren.HUD_GESCHROKKEN, "HUD: scared, cooldown");
        RijdierEntity r2 = Rijdieren.roep(p, RijdierSoort.PLUIMGUH);
        helper.assertTrue(r2 != null && p.getVehicle() == r2, "seated again");
        Haken.FLAUW.forEach(h -> h.accept(p));
        helper.assertTrue(!r2.isAlive() && p.getVehicle() == null, "fainting: off");
        Arena a = null;
        for (String id : Arenas.ids()) {
            a = Arenas.van(id);
            if (a != null) {
                break;
            }
        }
        helper.assertTrue(a != null, "there are boss arenas");
        BlockPos c = a.centrum();
        helper.assertTrue("arena".equals(Rijdieren.afReden(true, c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5)), "in an arena: off");
        helper.assertTrue(Rijdieren.afReden(true, c.getX() + a.straal() + 40, c.getY(), c.getZ()) == null, "outside the arena: keep riding");
        BlockPos stad = EconomiePlekken.MARKT;
        helper.assertTrue(Rijdieren.afReden(true, stad.getX() + 0.5, stad.getY(), stad.getZ() + 0.5) == null, "in town: keep riding");
        helper.assertTrue("rijk".equals(Rijdieren.afReden(false, 0, 64, 0)), "outside the Vadsrijk: off");
        Rijdieren.geraakt(p);   // (a hit while not riding does nothing)
        helper.succeed();
    }
}
