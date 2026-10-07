package nl.juiced.guhs.feature.huisje;

import java.util.List;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandVlaggen;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.feature.piep.PiepFeature;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Game tests of "Huisje betreden" (1.3.2). The game test server has no datapack dimensions, so every test gives its huisje
 * a room in its own test area ({@link Binnen#testKamer}); everything else is the real code. Covered: cells (a counter,
 * saved, never shared), in and out with the way back (also with a blocked door spot, also through the fade), the safe
 * rules and "back on the mat", a player without a valid room, the stand-ins (made, kept up to date, removed), owner and
 * visitor, tucking in once per night and the hearts for a guh that is not loaded, the huisje breaking while somebody is
 * inside, and the sleepers' maths. (Template huisje_binnen_test: 32 x 24 grass.)
 */
public class HuisjeBinnenGameTests {
    private static final String VELD = "huisje_binnen_test";
    private static final String BATCH = "huisje_binnen";
    /** Where the test's room stands (relative), next to the huisje. */
    private static final BlockPos KAMER = new BlockPos(14, 2, 2);

    private static Huisje huis(GameTestHelper helper, HuisjeMaat maat, ServerPlayer eigenaar) {
        Huisje h = HuisjeGameTests.bouw(helper, new BlockPos(4, 2, 4), maat, eigenaar);
        Binnen.testKamer(h, helper.getLevel(), helper.absolutePos(KAMER));
        return h;
    }

    private static void klaar(GameTestHelper helper, Huisje h, ServerPlayer... spelers) {
        HuisjeGoal.TEST_DAGDEEL.remove(h.pos());
        for (ServerPlayer p : spelers) {
            GuhQuests.saved(p).remove(Binnen.DATA);
        }
        Binnen.testKlaar(h);
        HuisjeGameTests.weg(helper, spelers);
    }

    private static GuhEntity bewoner(GameTestHelper helper, Huisje h, ServerPlayer eigenaar, String naam, boolean binnen) {
        GuhEntity guh = HuisjeGameTests.guh(helper, eigenaar, new BlockPos(6, 2, 10));
        guh.setCustomName(net.minecraft.network.chat.Component.literal(naam));
        helper.assertTrue(Huisjes.trekIn(h, guh), naam + " moves in");
        if (binnen) {
            Huisjes.naarBinnen(guh, h);
        } else {
            guh.setOrderedToSit(true);
        }
        return guh;
    }

    private static String bordje(ServerLevel level, Huisje h, BinnenKamer k, int bed) {
        return level.getBlockEntity(Binnen.oorsprong(h.cel()).offset(k.bedden().get(bed).bord())) instanceof SignBlockEntity bord
                ? bord.getFrontText().getMessage(1, false).getString() : "(no sign)";
    }

    // =====================================================================================================================

    @GuhTest(template = VELD, batch = BATCH)
    public static void binnenCellenBotsenNooit(GameTestHelper helper) {
        ServerPlayer p = HuisjeGameTests.speler(helper, new BlockPos(2, 2, 12));
        MinecraftServer s = helper.getLevel().getServer();
        Huisje a = HuisjeGameTests.bouw(helper, new BlockPos(4, 2, 4), HuisjeMaat.KLEIN, p);
        Huisje b = HuisjeGameTests.bouw(helper, new BlockPos(9, 2, 4), HuisjeMaat.KLEIN, p);
        helper.assertTrue(a.cel() < 0 && b.cel() < 0, "a huisje has no room until somebody goes in (so do huisjes from before the update)");
        Huisje oud = Huisje.load(a.save());
        helper.assertTrue(oud != null && oud.cel() < 0, "an old save without a cell loads as 'no room yet'");
        Binnen.testKamer(a, helper.getLevel(), helper.absolutePos(KAMER));
        Binnen.testKamer(b, helper.getLevel(), helper.absolutePos(KAMER.offset(0, 0, 12)));
        helper.assertTrue(a.cel() >= 0 && b.cel() == a.cel() + 1, "cells come from a counter: " + a.cel() + ", " + b.cel());
        Huisje kopie = Huisje.load(a.save());
        helper.assertTrue(kopie != null && kopie.cel() == a.cel(), "the cell is saved with the huisje");
        Huisjes herladen = Huisjes.load(Huisjes.get(s).save(new CompoundTag(), null), null);
        helper.assertTrue(Huisjes.nieuweCel(s) > b.cel() && herladen.huisjesVoorTest().stream().anyMatch(h -> h.cel() == b.cel()),
                "the counter and the cells survive a save and load");
        // the grid of the real dimension: every cell its own spot, far from the others
        int ca = a.cel(), cb = b.cel();
        Binnen.testKlaar(a);
        Binnen.testKlaar(b);
        BlockPos oa = Binnen.oorsprong(ca), ob = Binnen.oorsprong(cb), rij = Binnen.oorsprong(ca + Binnen.PER_RIJ);
        helper.assertTrue(oa.getY() == Binnen.Y && Math.abs(oa.getX() - ob.getX()) + Math.abs(oa.getZ() - ob.getZ()) >= Binnen.CEL,
                "neighbouring cells lie " + Binnen.CEL + " blocks apart: " + oa + " " + ob);
        helper.assertTrue(rij.getX() == oa.getX() && rij.getZ() == oa.getZ() + Binnen.CEL, "the next row");
        helper.assertTrue(BinnenKamer.MAX.getX() < Binnen.CEL, "a room is far smaller than a cell");
        // a huisje that is broken and built again on the same spot is a new huisje with a new cell
        helper.getLevel().destroyBlock(a.pos(), false);
        Huisje nieuw = HuisjeGameTests.bouw(helper, new BlockPos(4, 2, 4), HuisjeMaat.KLEIN, p);
        Binnen.testKamer(nieuw, helper.getLevel(), helper.absolutePos(KAMER));
        helper.assertTrue(nieuw.cel() > cb, "a rebuilt huisje never gets an old cell: " + nieuw.cel());
        klaar(helper, nieuw, p);
        helper.succeed();
    }

    @GuhTest(template = VELD, batch = BATCH, timeoutTicks = 200)
    public static void binnenNaarBinnenEnTerug(GameTestHelper helper) {
        ServerPlayer p = HuisjeGameTests.speler(helper, new BlockPos(2, 2, 12));
        ServerLevel level = helper.getLevel();
        MinecraftServer s = level.getServer();
        Huisje h = huis(helper, HuisjeMaat.KLEIN, p);
        BinnenKamer k = BinnenKamer.van(s, HuisjeMaat.KLEIN);
        helper.assertTrue(k != null && k.bedden().size() == 3 && BinnenKamer.versie(s) >= 1, "kamers.json is read: three beds in a klein huisje");
        Vec3 stond = p.position();
        Vec3 deur = Vec3.atBottomCenterOf(h.deur());
        helper.assertTrue(Binnen.betreed(p, h), "the owner goes in");
        helper.assertTrue(Binnen.binnenIn(p) == h && p.position().distanceTo(Binnen.mat(h, k)) < 0.01, "and stands on the doormat");
        BlockPos o = Binnen.oorsprong(h.cel());
        helper.assertTrue(level.getBlockState(o.offset(k.bedden().get(0).bed())).is(HuisjeFeature.BEDJE.get())
                && level.getBlockEntity(o.offset(k.prikbord())) instanceof SignBlockEntity, "the room was stamped: a bed and the prikbord stand");
        helper.assertTrue(GuhQuests.saved(p).contains(Binnen.DATA), "the way back is in the player's own saved data");
        Binnen.naarBuiten(p, null);
        helper.assertTrue(!Binnen.in(p) && p.position().distanceTo(deur) < 0.01, "out again: just in front of the door, not " + p.position());
        helper.assertTrue(!GuhQuests.saved(p).contains(Binnen.DATA), "the way back is used up");
        // the door spot is blocked: back to where the player stood when they went in
        p.snapTo(stond.x, stond.y, stond.z);
        helper.assertTrue(Binnen.betreed(p, h), "in again");
        level.setBlockAndUpdate(h.deur(), Blocks.STONE.defaultBlockState());
        Binnen.naarBuiten(p, null);
        helper.assertTrue(p.position().distanceTo(stond) < 0.01, "a blocked door spot: back where they stood, not " + p.position());
        level.setBlockAndUpdate(h.deur(), Blocks.AIR.defaultBlockState());
        // through the button and the door: the fade first, then the step
        helper.assertTrue(Binnen.vraag(p, h) && !Binnen.in(p), "the button: the doorbell rings, the player is still outside");
        helper.assertTrue(!Binnen.vraag(p, h), "a second click while the fade runs does nothing");
        helper.runAfterDelay(Binnen.FADE + 3, () -> {
            helper.assertTrue(Binnen.binnenIn(p) == h, "after the fade: inside");
            helper.assertTrue(Binnen.verlaat(p) && Binnen.in(p), "the door inside: still inside while the fade runs");
            helper.runAfterDelay(Binnen.FADE + 3, () -> {
                helper.assertTrue(!Binnen.in(p) && p.position().distanceTo(deur) < 0.01, "and out in front of the door");
                klaar(helper, h, p);
                helper.succeed();
            });
        });
    }

    @GuhTest(template = VELD, batch = BATCH, timeoutTicks = 200)
    public static void binnenKamerIsVeilig(GameTestHelper helper) {
        ServerPlayer p = HuisjeGameTests.speler(helper, new BlockPos(2, 2, 12));
        ServerPlayer vreemde = HuisjeGameTests.speler(helper, new BlockPos(3, 2, 12));
        ServerLevel level = helper.getLevel();
        MinecraftServer s = level.getServer();
        Huisje h = huis(helper, HuisjeMaat.KLEIN, p);
        BinnenKamer k = BinnenKamer.van(s, HuisjeMaat.KLEIN);
        helper.assertTrue(Binnen.betreed(p, h), "in");
        BlockPos o = Binnen.oorsprong(h.cel());
        BlockPos muur = o.offset(0, 1, 2), bed = o.offset(k.bedden().get(0).bed());
        helper.assertTrue(!p.gameMode.destroyBlock(muur) && !level.getBlockState(muur).isAir(), "a wall cannot be broken");
        helper.assertTrue(!p.gameMode.destroyBlock(bed) && level.getBlockState(bed).is(HuisjeFeature.BEDJE.get()), "a bed cannot be broken");
        float leven = p.getHealth();
        p.hurtServer(level, level.damageSources().generic(), 6f);
        helper.assertTrue(p.getHealth() == leven, "nothing hurts the player in the room");
        // out of the room (a fall, an ender pearl): back on the mat; somebody without a room: outside
        Vec3 mat = Binnen.mat(h, k);
        p.snapTo(o.getX() + 13.5, o.getY() + 1, o.getZ() + 13.5);
        vreemde.snapTo(mat.x, mat.y, mat.z);
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(p.position().distanceTo(mat) < 0.01, "outside the room: back on the doormat, not " + p.position());
            helper.assertTrue(!Binnen.in(vreemde), "a player in a room without having come through the door is put outside");
            helper.assertTrue(Binnen.binnenIn(p) == h, "the owner is still inside");
            klaar(helper, h, p, vreemde);
            helper.succeed();
        });
    }

    @GuhTest(template = VELD, batch = BATCH, timeoutTicks = 300)
    public static void binnenStandInsEnBedden(GameTestHelper helper) {
        ServerPlayer p = HuisjeGameTests.speler(helper, new BlockPos(2, 2, 12));
        ServerLevel level = helper.getLevel();
        MinecraftServer s = level.getServer();
        Huisje h = huis(helper, HuisjeMaat.MEDIUM, p);
        BinnenKamer k = BinnenKamer.van(s, HuisjeMaat.MEDIUM);
        HuisjeGoal.TEST_DAGDEEL.put(h.pos(), Dagdeel.NACHT);
        GuhEntity slaper = bewoner(helper, h, p, "Dutje", true);
        GuhEntity zitter = bewoner(helper, h, p, "Zitje", false);
        TamableAnimal muis = helper.spawn(PiepFeature.PIEPPIEPMUISJE.get(), new BlockPos(7, 2, 10));
        muis.tame(p);
        helper.assertTrue(Huisjes.trekIn(h, muis), "the muisje moves in");
        Huisjes.naarBinnen(muis, h);
        Vec3 echt = slaper.position();
        helper.assertTrue(BinnenInrichting.dingen(level, h).isEmpty(), "nobody inside: nothing stands in the room");
        helper.assertTrue(Binnen.betreed(p, h), "in");
        List<BinnenInrichting.Slot> slots = BinnenInrichting.slots(s, h);
        helper.assertTrue(slots.size() == 3 && slots.get(0).waar() == BinnenInrichting.Waar.SLAAPT && slots.get(1).waar() == BinnenInrichting.Waar.ZIT
                && slots.get(2).waar() == BinnenInrichting.Waar.SLAAPT && !slots.get(2).guh(), "who is where: " + slots.stream().map(x -> x.waar().name()).toList());
        List<Entity> dingen = BinnenInrichting.dingen(level, h);
        List<BinnenGuh> standIns = dingen.stream().filter(e -> e instanceof BinnenGuh).map(e -> (BinnenGuh) e).toList();
        helper.assertTrue(standIns.size() == 1 && Band.id(slaper).equals(standIns.get(0).bewoner()), "one sleeping stand-in, for the guh that sleeps inside");
        BinnenGuh standIn = standIns.get(0);
        Vec3 bed0 = Vec3.atBottomCenterOf(Binnen.oorsprong(h.cel()).offset(k.bedden().get(0).bed()));
        helper.assertTrue(standIn.slaapt() && standIn.getVariant() == slaper.getVariant() && "Dutje".equals(standIn.getName().getString())
                && Math.abs(standIn.getX() - bed0.x) < 0.01 && Math.abs(standIn.getZ() - bed0.z) < 0.01, "it sleeps in the first bed and looks like the real one");
        helper.assertTrue(!standIn.shouldBeSaved() && !Band.isBandGuh(standIn), "a stand-in is never saved and is nobody's guh");
        helper.assertTrue(dingen.stream().filter(e -> e instanceof Display.TextDisplay).count() == 1, "one note: on the bed of the guh that is away");
        helper.assertTrue(dingen.stream().filter(e -> e instanceof Display.BlockDisplay).count() == 3, "three blankets");
        helper.assertTrue("Dutje".equals(bordje(level, h, k, 0)) && "Zitje".equals(bordje(level, h, k, 1)), "the name signs: " + bordje(level, h, k, 0));
        helper.assertTrue(slaper.level() == level && slaper.position().equals(echt) && Huisjes.isBinnen(slaper)
                && BandVlaggen.heeft(slaper, BandVlaggen.HUISJE_BINNEN), "the real guh is where and how it was");
        // a resident moves out while somebody is inside: the beds follow within a second
        Huisjes.trekUit(zitter);
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(BinnenInrichting.slots(s, h).size() == 2 && !BinnenInrichting.slots(s, h).get(1).guh(), "the muisje has the second bed now");
            helper.assertTrue(BinnenInrichting.dingen(level, h).stream().noneMatch(e -> e instanceof Display.TextDisplay), "the note is gone");
            helper.assertTrue(BinnenInrichting.dingen(level, h).stream().filter(e -> e instanceof Display.BlockDisplay).count() == 2
                    && BinnenInrichting.dingen(level, h).stream().filter(e -> e instanceof Display.ItemDisplay).count() >= 2, "two blankets and the things on the two bedside tables stand: "
                    + BinnenInrichting.dingen(level, h).stream().map(e -> e.getType().toShortString()).toList());
            helper.assertTrue(bordje(level, h, k, 2).equals(net.minecraft.network.chat.Component.translatable("gui.guhs.huisje.binnen.bordje.vrij").getString()),
                    "the third bed is free again: " + bordje(level, h, k, 2));
            // the last one leaves: everything is removed; somebody comes in: it is made again
            Binnen.naarBuiten(p, null);
            helper.runAfterDelay(3, () -> {
                helper.assertTrue(BinnenInrichting.dingen(level, h).isEmpty() && standIn.isRemoved(), "the last player left: the stand-ins are gone");
                helper.assertTrue(Binnen.betreed(p, h), "in again");
                helper.assertTrue(BinnenInrichting.dingen(level, h).stream().filter(e -> e instanceof BinnenGuh).count() == 1, "and they are back");
                Binnen.naarBuiten(p, null);
                klaar(helper, h, p);
                helper.succeed();
            });
        });
    }

    @GuhTest(template = VELD, batch = BATCH, timeoutTicks = 200)
    public static void binnenEigenaarEnBezoeker(GameTestHelper helper) {
        ServerPlayer p = HuisjeGameTests.speler(helper, new BlockPos(2, 2, 12));
        ServerPlayer gast = HuisjeGameTests.speler(helper, new BlockPos(3, 2, 12));
        ServerLevel level = helper.getLevel();
        MinecraftServer s = level.getServer();
        Huisje h = huis(helper, HuisjeMaat.KLEIN, p);
        HuisjeGoal.TEST_DAGDEEL.put(h.pos(), Dagdeel.NACHT);
        GuhEntity thuis = bewoner(helper, h, p, "Thuisje", true);
        GuhEntity weg = bewoner(helper, h, p, "Wegje", true);
        // the second sleeper's chunk "unloads": the entity is gone, the band's records still say it sleeps in its huisje
        UUID wegId = Band.id(weg);
        weg.discard();
        helper.assertTrue(Binnen.betreed(p, h) && Binnen.betreed(gast, h), "the owner and a visitor go in (everybody may look)");
        helper.assertTrue(Binnen.spelers(s, h).size() == 2, "two players in one room");
        List<BinnenGuh> standIns = BinnenInrichting.dingen(level, h).stream().filter(e -> e instanceof BinnenGuh).map(e -> (BinnenGuh) e).toList();
        helper.assertTrue(standIns.size() == 2, "two stand-ins: also for the guh that is not loaded (from the band's looks), found " + standIns.size());
        BinnenGuh a = standIns.stream().filter(g -> Band.id(thuis).equals(g.bewoner())).findFirst().orElseThrow();
        BinnenGuh b = standIns.stream().filter(g -> wegId.equals(g.bewoner())).findFirst().orElseThrow();
        Huisjes data = Huisjes.get(s);
        int voor = Band.hartjes(thuis);
        // the visitor may look, not touch
        BinnenInrichting.guhKlik(gast, a);
        BinnenInrichting.bedKlik(gast, h, 0);
        helper.assertTrue(Band.hartjes(thuis) == voor && !data.ingestopt.containsKey(Band.id(thuis)), "a visitor cannot tuck in or pet");
        // the owner tucks in: once per night, hearts for the REAL guh
        BinnenInrichting.guhKlik(p, a);
        int naInstoppen = Band.hartjes(thuis);
        helper.assertTrue(naInstoppen > voor && data.ingestopt.get(Band.id(thuis)) == Band.dag(s), "tucked in: hearts for the real guh (" + voor + " -> " + naInstoppen + ")");
        helper.assertTrue(!a.isRemoved() && a.slaapt(), "it does not wake up");
        BinnenInrichting.guhKlik(p, a);
        int naAaien = Band.hartjes(thuis);
        helper.assertTrue(naAaien - naInstoppen >= 1 && naAaien - naInstoppen < naInstoppen - voor, "the next click is a soft pet, not a second tuck-in");
        BinnenInrichting.guhKlik(p, a);
        helper.assertTrue(Band.hartjes(thuis) == naAaien, "petting has a little rest between two pets");
        // the guh that is not loaded: remembered, and given when it loads again
        BinnenInrichting.guhKlik(p, b);
        helper.assertTrue(data.teGoed.containsKey(wegId) && data.teGoed.get(wegId)[0] == 1, "tucked in while not loaded: remembered");
        GuhEntity terug = ModEntities.GUH.get().create(level, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        terug.setUUID(wegId);
        BlockPos plek = helper.absolutePos(new BlockPos(8, 2, 10));
        terug.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5);
        level.addFreshEntity(terug);
        terug.tame(p);
        int wegVoor = Band.hartjes(terug);
        BinnenInrichting.teGoed(terug);
        helper.assertTrue(Band.hartjes(terug) > wegVoor && !data.teGoed.containsKey(wegId), "it loads again: the hearts arrive (" + wegVoor + " -> "
                + Band.hartjes(terug) + ")");
        Binnen.naarBuiten(p, null);
        Binnen.naarBuiten(gast, null);
        terug.discard();
        klaar(helper, h, p, gast);
        helper.succeed();
    }

    @GuhTest(template = VELD, batch = BATCH, timeoutTicks = 200)
    public static void binnenHuisjeWegTerwijlBinnen(GameTestHelper helper) {
        ServerPlayer p = HuisjeGameTests.speler(helper, new BlockPos(2, 2, 12));
        ServerLevel level = helper.getLevel();
        MinecraftServer s = level.getServer();
        Huisje h = huis(helper, HuisjeMaat.KLEIN, p);
        BinnenKamer k = BinnenKamer.van(s, HuisjeMaat.KLEIN);
        HuisjeGoal.TEST_DAGDEEL.put(h.pos(), Dagdeel.NACHT);
        GuhEntity guh = bewoner(helper, h, p, "Dutje", true);
        Vec3 deur = Vec3.atBottomCenterOf(h.deur());
        helper.assertTrue(Binnen.betreed(p, h), "in");
        BlockPos o = Binnen.oorsprong(h.cel());
        BlockPos mat = o.offset(k.mat());
        helper.assertTrue(!level.getBlockState(mat).isAir() && !BinnenInrichting.dingen(level, h).isEmpty(), "the room stands, with a sleeper in it");
        level.destroyBlock(h.pos(), false);   // (the huisje is broken while its owner is inside)
        helper.assertTrue(Huisjes.op(s, level.dimension(), h.pos()) == null, "the huisje is gone");
        helper.assertTrue(!Binnen.in(p) && p.position().distanceTo(deur) < 0.01, "whoever was inside stands where its door was, not " + p.position());
        helper.assertTrue(!GuhQuests.saved(p).contains(Binnen.DATA) && Binnen.kamerVan(p) == null, "and has no room any more");
        helper.assertTrue(level.getBlockState(mat).isAir() && level.getBlockState(o.offset(k.bedden().get(0).bed())).isAir(), "the room is cleared");
        helper.assertTrue(level.getEntities((Entity) null, new net.minecraft.world.phys.AABB(o).inflate(20), e -> e.entityTags().contains(Binnen.TAG)).isEmpty(),
                "and so are its stand-ins");
        helper.assertTrue(!Huisjes.isBinnen(guh) && !Huisjes.isBewoner(guh) && !guh.isInvisible(), "the real guh came out, as always when a huisje breaks");
        klaar(helper, h, p);
        helper.succeed();
    }

    /** Point 4: by day a resident with nothing to finish goes home (a nap, the rain) and comes out again afterwards. */
    @GuhTest(template = VELD, batch = BATCH, timeoutTicks = 600)
    public static void binnenOverdagThuis(GameTestHelper helper) {
        ServerPlayer p = HuisjeGameTests.speler(helper, new BlockPos(20, 2, 20));
        ServerLevel level = helper.getLevel();
        Huisje h = huis(helper, HuisjeMaat.KLEIN, p);
        HuisjeGoal.TEST_DAGDEEL.put(h.pos(), Dagdeel.DAG);
        GuhEntity guh = HuisjeGameTests.guh(helper, p, new BlockPos(5, 2, 8));
        helper.assertTrue(Huisjes.trekIn(h, guh), "moves in");
        helper.assertTrue(!HuisjeGoal.dagThuis(level, h, guh), "an ordinary day: outside");
        java.util.concurrent.atomic.AtomicInteger fase = new java.util.concurrent.atomic.AtomicInteger();
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(!Huisjes.isBinnen(guh), "by day it is outside");
            HuisjeGoal.TEST_THUIS.add(h.pos());
            fase.set(1);
        });
        helper.onEachTick(() -> {
            if (fase.get() == 1 && Huisjes.isBinnen(guh)) {
                helper.assertTrue(Binnen.betreed(p, h), "the owner looks inside");
                helper.assertTrue(BinnenInrichting.slots(level.getServer(), h).get(0).waar() == BinnenInrichting.Waar.SLAAPT, "and finds it asleep in its bed by day");
                Binnen.naarBuiten(p, null);
                HuisjeGoal.TEST_THUIS.remove(h.pos());
                fase.set(2);
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(fase.get() == 2, "went home by day");
            helper.assertTrue(!Huisjes.isBinnen(guh) && !guh.isInvisible() && Huisjes.isBewoner(guh), "and came out again when the nap was over");
            HuisjeGoal.TEST_THUIS.remove(h.pos());
            klaar(helper, h, p);
        });
    }

    @GuhTest(template = VELD, batch = BATCH)
    public static void binnenSlapersRekenen(GameTestHelper helper) {
        helper.assertTrue(BinnenSlaap.nodig(1, 100) == 1 && BinnenSlaap.nodig(3, 100) == 3 && BinnenSlaap.nodig(3, 50) == 2 && BinnenSlaap.nodig(4, 50) == 2
                && BinnenSlaap.nodig(0, 100) == 1 && BinnenSlaap.nodig(5, 0) == 1, "the vanilla rule: at least one, the percentage rounded up");
        ServerPlayer p = HuisjeGameTests.speler(helper, new BlockPos(2, 2, 12));
        Huisje h = huis(helper, HuisjeMaat.KLEIN, p);
        MinecraftServer s = helper.getLevel().getServer();
        ServerLevel doel = BinnenSlaap.doel(s, h);
        helper.assertTrue(doel.dimensionType().defaultClock().isPresent(), "a huisje's sleep counts for a world with a day clock: " + doel.dimension().identifier());
        helper.assertTrue(helper.getLevel().dimension() != net.minecraft.world.level.Level.OVERWORLD || doel == s.overworld(), "in the overworld: the overworld");
        klaar(helper, h, p);
        helper.succeed();
    }
}
