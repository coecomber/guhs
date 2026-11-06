package nl.juiced.guhs.feature.vadsrijk.zone4.boot;

import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;

/**
 * T-Z4BOOT: the Zeilbootje. The sailing maths (sail up / glide / back / turning, the wind), De Kringstroom (pushes back in from
 * every direction, never lets you through, never gets you stuck), floating on real water, finding water for the key, getting the
 * boat (quest flag + hook), the looks (own / choose / buy), docks + "logged out at sea", which mount the key calls, and the ban on
 * other boats. Run: {@code runGameTestServer -Pgt=VadsrijkZ4Boot}.
 */
public class VadsrijkZ4BootGameTests {
    private static final String BATCH = "vadsrijk_z4boot";

    @GuhTest(template = "empty", batch = BATCH)
    public static void zeilen(GameTestHelper helper) {
        double top = RijdierSoort.ZEILBOOTJE.snelheid(1) * BootFysica.TOP_FACTOR;
        helper.assertTrue(Math.abs(top - 0.6) < 1e-6, "top speed 0.6 blocks a tick at level 1: " + top);
        helper.assertTrue(RijdierSoort.ZEILBOOTJE.water() && !RijdierSoort.PLUIMGUH.water(), "only the boat sails");
        // sail up: the speed builds up, not at once
        double v = 0;
        BootFysica.Stap s = BootFysica.stap(0, 0, 0, v, 1, 0, top, 0, null);
        helper.assertTrue(s.vaart() > 0 && s.vaart() <= BootFysica.ACC + 1e-9, "one tick of sail: a little speed " + s.vaart());
        for (int i = 0; i < 80; i++) {
            v = BootFysica.stap(0, 0, 0, v, 1, 0, top, 0, null).vaart();
        }
        helper.assertTrue(v > top * 0.84 && v < top * 1.16, "after 4 s: (near) the top speed with the wind: " + v);
        // let go: it glides on, slowly slower
        double los = BootFysica.stap(0, 0, 0, v, 0, 0, top, 0, null).vaart();
        helper.assertTrue(los < v && los > v * 0.95, "gliding: a bit slower " + los);
        // S: brakes, then backs slowly
        double terug = v;
        for (int i = 0; i < 200; i++) {
            terug = BootFysica.stap(0, 0, 0, terug, -1, 0, top, 0, null).vaart();
        }
        helper.assertTrue(Math.abs(terug + top * BootFysica.ACHTERUIT) < 1e-6, "backwards at a quarter speed: " + terug);
        // A (stuur +1) turns left = yaw down; slower turns at full speed
        float traag = BootFysica.stap(0, 0, 0, 0, 0, 1, top, 0, null).draai(), snel = BootFysica.stap(0, 0, 0, top, 1, 1, top, 0, null).draai();
        helper.assertTrue(traag < 0 && snel < 0 && Math.abs(snel) < Math.abs(traag), "turning: " + traag + " / " + snel);
        // yaw 0 = south (+z)
        BootFysica.Stap zuid = BootFysica.stap(0, 0, 0, top, 1, 0, top, 0, null);
        helper.assertTrue(zuid.dz() > 0 && Math.abs(zuid.dx()) < 1e-9, "yaw 0 sails to +z");
        // the wind: 0.85 .. 1.15, behind you is best
        for (long t = 0; t < 96000; t += 1700) {
            float w = BootFysica.windRichting(t);
            helper.assertTrue(Math.abs(BootFysica.windFactor(w, t) - 1.15) < 1e-6 && Math.abs(BootFysica.windFactor(w + 180, t) - 0.85) < 1e-6,
                    "wind behind / against at t " + t);
        }
        helper.assertTrue(BootFysica.windRichting(0) != BootFysica.windRichting(24000), "the wind turns over the day");
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void kringstroom(GameTestHelper helper) {
        BootFysica.Kringstroom k = BootZee.kring();
        double top = RijdierSoort.ZEILBOOTJE.snelheid(RijdierSoort.MAX_LEVEL) * BootFysica.TOP_FACTOR * 1.2;   // (faster than any boat)
        // the islands of WERELDINDELING.md are inside, free of the current
        double[][] eilanden = {{1850, -470}, {1935, -345}, {1995, -605}, {2085, -405}, {2185, -565}, {2300, -665}, {2165, -700}, {2400, -500},
                {2290, -420}, {2480, -330}, {2525, -565}};
        for (double[] e : eilanden) {
            helper.assertTrue(k.r(e[0], e[1]) < 0.93, "island inside the current: " + e[0] + ", " + e[1] + " r " + k.r(e[0], e[1]));
        }
        helper.assertTrue(k.diepte(BootZee.CX, BootZee.CZ) == 0, "no current in the middle");
        // from 16 directions: start in the band, sail straight OUT at full sail for 30 s: never past the outer line, and back in
        for (int i = 0; i < 16; i++) {
            double hoek = i * Math.PI / 8;
            double x = BootZee.CX + Math.cos(hoek) * BootZee.A * 0.95, z = BootZee.CZ + Math.sin(hoek) * BootZee.B * 0.95;
            double[] uit = k.naarBuiten(x, z);
            float yaw = (float) Math.toDegrees(Math.atan2(-uit[0], uit[1]));   // heading outward
            double v = top, maxR = 0;
            for (int t = 0; t < 600; t++) {
                BootFysica.Stap s = BootFysica.stap(x, z, yaw, v, 1, 0, top, t * 37L, k);
                v = s.vaart();
                yaw = (float) Math.toDegrees(Math.atan2(-uit[0], uit[1]));   // (the player keeps steering out)
                x += s.dx();
                z += s.dz();
                uit = k.naarBuiten(x, z);
                maxR = Math.max(maxR, k.r(x, z));
            }
            helper.assertTrue(maxR < 1.0, "direction " + i + ": never through the current (max r " + maxR + ")");
        }
        // beyond the line (a teleport): pushed back in, also without any key; and never stuck
        for (int i = 0; i < 8; i++) {
            double hoek = i * Math.PI / 4 + 0.3;
            double x = BootZee.CX + Math.cos(hoek) * BootZee.A * 1.1, z = BootZee.CZ + Math.sin(hoek) * BootZee.B * 1.1;
            float yaw = 0;
            double v = 0;
            for (int t = 0; t < 400; t++) {
                BootFysica.Stap s = BootFysica.stap(x, z, yaw, v, 0, 0, top, t, k);
                v = s.vaart();
                yaw += s.draai();
                x += s.dx();
                z += s.dz();
            }
            helper.assertTrue(k.r(x, z) < BootZee.BINNEN + 0.01, "pushed back inside the band from direction " + i + ": r " + k.r(x, z));
        }
        // in the band the bow turns inward by itself
        double bx = BootZee.CX + BootZee.A * 0.98, bz = BootZee.CZ;
        BootFysica.Stap s = BootFysica.stap(bx, bz, -90, 0, 0, 0, top, 0, k);   // yaw -90 = heading +x = outward
        helper.assertTrue(s.draai() != 0 && s.dx() < 0, "the current turns you and pushes you in: " + s.draai() + " / " + s.dx());
        // dev: the current can be moved (t-z4wereld)
        try {
            BootZee.zetKringstroom(0, 0, 100, 100, 0.9);
            helper.assertTrue(BootZee.opZee(50, 0) && !BootZee.opZee(150, 0), "moved current");
        } finally {
            BootZee.zetKringstroom(BootZee.CX, BootZee.CZ, BootZee.A, BootZee.B, BootZee.BINNEN);
        }
        helper.succeed();
    }

    /** A 9x9 pond (1 deep) in the test box; returns the absolute middle of the water block. */
    private static BlockPos vijver(GameTestHelper helper) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE.defaultBlockState());
                helper.setBlock(new BlockPos(x, 1, z), Blocks.WATER.defaultBlockState());
                helper.setBlock(new BlockPos(x, 2, z), Blocks.AIR.defaultBlockState());
                helper.setBlock(new BlockPos(x, 3, z), Blocks.AIR.defaultBlockState());
            }
        }
        for (int z = 0; z < 9; z++) {   // a strip of land
            helper.setBlock(new BlockPos(10, 1, z), Blocks.STONE.defaultBlockState());
        }
        return helper.absolutePos(new BlockPos(4, 1, 4));
    }

    @GuhTest(template = "empty", batch = BATCH, timeoutTicks = 200)
    public static void drijven(GameTestHelper helper) {
        BlockPos midden = vijver(helper);
        ServerLevel sl = helper.getLevel();
        double opp = midden.getY() + sl.getFluidState(midden).getHeight(sl, midden);
        // the key's water search: from the land strip it finds the pond; not on dry land far away
        Vec3 plek = Boot.zoekWater(sl, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(10, 2, 4))), Boot.ZOEK);
        helper.assertTrue(plek != null && Math.abs(plek.y - (opp - BootEntity.DIEPGANG)) < 1e-6, "water found next to the land: " + plek);
        helper.assertTrue(Boot.zoekWater(sl, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 1, 4)).above(40)), 3) == null, "no water up in the air");
        Entity e = BootSpul.ZEILBOOTJE.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
        helper.assertTrue(e instanceof BootEntity, "the boat entity");
        BootEntity b = (BootEntity) e;
        b.snapTo(midden.getX() + 0.5, midden.getY() + 1.6, midden.getZ() + 0.5, 0, 0);   // (dropped in from a bit above)
        sl.addFreshEntity(b);
        helper.assertTrue(EcoSpul.soort(b.getType()) == RijdierSoort.ZEILBOOTJE, "the soort of the entity");
        helper.runAfterDelay(60, () -> {
            double doel = opp - BootEntity.DIEPGANG;
            helper.assertTrue(b.isAlive() && Math.abs(b.getY() - doel) < 0.1, "floats on the surface: y " + b.getY() + " ~ " + doel);
            helper.assertTrue(b.opWater() && b.getAirSupply() > 0, "on the water, no drowning");
            b.discard();
            helper.succeed();
        });
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void krijgenEnUiterlijk(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Rijdieren.reset(p);
        Boot.reset(p);
        AtomicInteger hook = new AtomicInteger();
        java.util.function.Consumer<ServerPlayer> h = x -> hook.incrementAndGet();
        Boot.GEKREGEN.add(h);
        try {
            helper.assertTrue(!Boot.heeft(p) && !Boot.gekregen(p), "nothing yet");
            helper.assertTrue(Boot.geef(p) && Boot.heeft(p) && Boot.gekregen(p), "the story gives the boat + the quest flag");
            helper.assertTrue(!Boot.geef(p) && hook.get() == 1, "only once (the hook fired once)");
            helper.assertTrue(Rijdieren.level(p, RijdierSoort.ZEILBOOTJE) == 1, "a mount at level 1");
            helper.assertTrue(Boot.laatsteSteiger(p).id().equals(BootZee.THUIS), "home dock Lampionhaven");
            // looks: the start ones, choosing, buying, rewards
            for (BootVariant.As as : BootVariant.As.values()) {
                helper.assertTrue(Boot.gekozen(p, as) == as.standaard() && Boot.heeftVariant(p, as.standaard()), "start look " + as);
            }
            helper.assertTrue(!Boot.kies(p, BootVariant.ZEIL_MINT), "can't choose what you don't have");
            Spelers.data(p).munten = 0;
            helper.assertTrue(Boot.koop(p, BootVariant.ZEIL_MINT).equals("vadsrijk.economie.smid.te_weinig") && !Boot.heeftVariant(p, BootVariant.ZEIL_MINT), "no money");
            Spelers.data(p).munten = BootVariant.ZEIL_MINT.prijs + 5;
            helper.assertTrue(Boot.koop(p, BootVariant.ZEIL_MINT).equals("vadsrijk.z4boot.scherm.gekocht") && Spelers.data(p).munten == 5
                    && Boot.gekozen(p, BootVariant.As.ZEIL) == BootVariant.ZEIL_MINT, "bought + chosen");
            helper.assertTrue(Boot.koop(p, BootVariant.BOEG_DRAAKJE).isEmpty() && !Boot.heeftVariant(p, BootVariant.BOEG_DRAAKJE), "rewards are not sold");
            helper.assertTrue(Boot.geefVariant(p, "boeg:draakje") && Boot.kies(p, BootVariant.BOEG_DRAAKJE) && !Boot.geefVariant(p, BootVariant.BOEG_DRAAKJE),
                    "a reward by id");
            helper.assertTrue(Boot.gekozen(p, BootVariant.As.BOEG) == BootVariant.BOEG_DRAAKJE && Boot.gekozen(p, BootVariant.As.WIMPEL) == BootVariant.WIMPEL_HARTJE,
                    "axes are separate");
            helper.assertTrue(BootVariant.van(BootVariant.As.ZEIL, BootVariant.ZEIL_HONING.index()) == BootVariant.ZEIL_HONING, "index round trip");
            var d = Boot.schermData(p);
            helper.assertTrue(d.getBooleanOr("heeft", false) && d.getListOrEmpty("varianten").size() == BootVariant.values().length, "screen data");
            Boot.reset(p);
            helper.assertTrue(!Boot.heeft(p) && !Boot.gekregen(p) && Boot.gekozen(p, BootVariant.As.ZEIL) == BootVariant.ZEIL_ROZE, "reset");
        } finally {
            Boot.GEKREGEN.remove(h);
        }
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void welkRijdier(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Rijdieren.reset(p);
        Boot.reset(p);
        helper.assertTrue(Boot.kiesSoort(p, RijdierSoort.PLUIMGUH, true) == RijdierSoort.PLUIMGUH, "no boat: the key keeps its mount");
        Boot.geef(p);
        helper.assertTrue(Boot.kiesSoort(p, RijdierSoort.ZEILBOOTJE, false) == null, "only the boat, on land: nothing (a message)");
        Rijdieren.geefKleur(p, RijdierSoort.PluimKleur.MINT);
        helper.assertTrue(Boot.kiesSoort(p, RijdierSoort.PLUIMGUH, true) == RijdierSoort.ZEILBOOTJE, "at the water: the boat");
        helper.assertTrue(Boot.kiesSoort(p, RijdierSoort.PLUIMGUH, false) == RijdierSoort.PLUIMGUH, "on land: the land mount");
        helper.assertTrue(Boot.kiesSoort(p, RijdierSoort.ZEILBOOTJE, false) == RijdierSoort.PLUIMGUH, "boat chosen, on land: the land mount");
        // the mount screen lists the boat with the others
        helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul.icoon(RijdierSoort.ZEILBOOTJE, RijdierSoort.PluimKleur.ROZE).is(BootSpul.ICOON.get()),
                "its icon");
        Boot.reset(p);
        Rijdieren.reset(p);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void steigers(GameTestHelper helper) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        Boot.reset(p);
        helper.assertTrue(!BootZee.steigers().isEmpty() && BootZee.thuis().id().equals(BootZee.THUIS), "docks + home");
        BootZee.Steiger lh = BootZee.thuis();
        helper.assertTrue(BootZee.dichtbij(lh.plek().getX() + 4, lh.plek().getZ() - 3, BootZee.AANLEG_AFSTAND) == lh, "close to the dock");
        helper.assertTrue(BootZee.dichtbij(BootZee.CX, BootZee.CZ + 900, BootZee.AANLEG_AFSTAND) == null, "nothing out at sea");
        for (BootZee.Steiger s : BootZee.steigers()) {
            helper.assertTrue(BootZee.kring().r(s.plek().getX(), s.plek().getZ()) < BootZee.BINNEN, "dock " + s.id() + " is inside the current");
        }
        BootZee.Steiger vt = BootZee.steiger("vuurtoren");
        if (vt != null) {
            Boot.aangemeerd(p, vt);
            helper.assertTrue(Boot.laatsteSteiger(p) == vt && !Boot.opZee(p), "moored: remembered, not at sea");
        }
        // logged out while sailing: the flag stays; a step off the boat clears it; at login outside the Vadsrijk nothing moves
        Boot.tag(p).putBoolean("opzee", true);
        helper.assertTrue(Boot.opZee(p), "at sea");
        helper.assertTrue(!Boot.ingelogd(p) && !Boot.opZee(p), "login (test world, not the Vadsrijk): flag cleared, no teleport");
        Boot.tag(p).putBoolean("opzee", true);
        Boot.spelerTik(p);
        helper.assertTrue(!Boot.opZee(p), "not on the boat any more: not at sea");
        Boot.reset(p);
        helper.succeed();
    }

    @GuhTest(template = "empty", batch = BATCH)
    public static void geenVreemdeBoten(GameTestHelper helper) {
        ServerLevel sl = helper.getLevel();
        Entity eik = EntityType.OAK_BOAT.create(sl, EntitySpawnReason.MOB_SUMMONED);
        Entity vlot = EntityType.BAMBOO_RAFT.create(sl, EntitySpawnReason.MOB_SUMMONED);
        Entity kist = EntityType.OAK_CHEST_BOAT.create(sl, EntitySpawnReason.MOB_SUMMONED);
        Entity ons = BootSpul.ZEILBOOTJE.get().create(sl, EntitySpawnReason.MOB_SUMMONED);
        Entity koe = EntityType.COW.create(sl, EntitySpawnReason.MOB_SUMMONED);
        helper.assertTrue(BootRegistry.vreemdeBoot(eik) && BootRegistry.vreemdeBoot(vlot) && BootRegistry.vreemdeBoot(kist), "vanilla boats are refused");
        helper.assertTrue(!BootRegistry.vreemdeBoot(ons) && !BootRegistry.vreemdeBoot(koe), "our boat and a cow are fine");
        helper.assertTrue(BootRegistry.vreemdBootItem(new ItemStack(Items.OAK_BOAT)) && BootRegistry.vreemdBootItem(new ItemStack(Items.BAMBOO_RAFT)),
                "boat items can't be used");
        helper.assertTrue(!BootRegistry.vreemdBootItem(new ItemStack(Items.STICK)) && !BootRegistry.vreemdBootItem(new ItemStack(BootSpul.ICOON.get()))
                && !BootRegistry.vreemdBootItem(ItemStack.EMPTY), "other items are fine");
        helper.assertTrue(nl.juiced.guhs.feature.vadsrijk.kern.Buitenstaanders.hoortErbij(ons), "the boat may be in the Vadsrijk");
        helper.succeed();
    }
}
