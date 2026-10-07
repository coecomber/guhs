package nl.juiced.guhs.feature.ring;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.barbecuether.GrillPortalBlock;
import nl.juiced.guhs.feature.ringh5.RingH5Feature;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Reiskaarten;
import nl.juiced.guhs.feature.verhaal.Rustpunten;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.verhaal.VerhaalDemo;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.ModDimensions;

/**
 * bbq2 (ring-kern): the core of the Knabbelring, server side (mock players get no packets and are not ticked: the tests
 * call the once-a-second upkeep themselves). Template ring_test_kamer (tools/features/ring_bouw.py): a floor of 21 x 21, a
 * Rustvuurtje at (4, 2, 4), a pillar with an Elfentouwhaak at (16, 6, 5).
 */
public final class RingGameTests {
    private static final String KAMER = "ring_test_kamer", BATCH = "ring";
    private static final BlockPos VUUR = new BlockPos(4, 2, 4), HAAK = new BlockPos(16, 6, 5);

    private static ServerPlayer speler(GameTestHelper helper, int x, int z) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(new BlockPos(x, 2, z));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        p.setOnGround(true);
        Ring.OVERAL = true;
        Negen.OVERAL = true;
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            Ring.wis(p);
            Gaven.vergeet(p);
            Ring.vergeet(p.getUUID());
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static void klaarTot(ServerPlayer p, int hoofdstuk) {
        Ring.lijn(1).begin(p);
        for (int n = 1; n <= hoofdstuk; n++) {
            Verhaallijn l = Ring.lijn(n);
            l.zet(p, l.stappen());
        }
    }

    private static int tel(ServerPlayer p, Item item) {
        return GuhQuests.count(p, item);
    }

    private static String key(Component c) {
        return c != null && c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t ? t.getKey() : "";
    }

    /**
     * The portal lock on the merged tree (PHASE3 R01): the grill portal Guhmensie -> Barbecuether is closed for EVERYBODY
     * (a creative player too, a spectator not) until the LAST step of the real chapter 1; the way back is never asked; a
     * player who is in the Barbecuether without chapter 1 can leave and is not moved by anything; and both questions ("may
     * start", "may use the portal") have one answer each: {@link Ring#magBeginnen} and {@link Ring#magDoorPortaal}.
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void ringPortaalslot(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer nieuw = speler(helper, 2, 2), bezig = speler(helper, 3, 2), klaar = speler(helper, 4, 2), bouwer = speler(helper, 5, 2),
                kijker = speler(helper, 6, 2);
        Verhaallijn h1 = Ring.lijn(1);
        helper.assertTrue(h1.stappen() == 6 && "ring_h1".equals(h1.id()), "the real chapter 1 (six steps) is the key of the lock");
        // (1) may start: only the Grillguh's quest opens it, and only Ring.magBeginnen says so
        helper.assertTrue(!Ring.magBeginnen(nieuw) && !Ring.begonnen(nieuw), "a new player may not start");
        nl.juiced.guhs.feature.barbecuether.Grillguh.setStep(bezig, nl.juiced.guhs.feature.barbecuether.Grillguh.DONE);
        helper.assertTrue(Ring.magBeginnen(bezig) && !Ring.magDoorPortaal(bezig), "the Grillguh's quest lets a player START, it does not open the portal");
        // (2) closed for everybody until the LAST step
        helper.assertTrue(RingFeature.PORTAAL_DICHT.equals(key(GrillPortalBlock.slot(ModDimensions.GUHMENSION, level, nieuw))), "refused: the story has not begun");
        h1.begin(bezig);
        for (int stap = 0; stap < h1.stappen(); stap++) {
            h1.zet(bezig, stap);
            helper.assertTrue(!Ring.magDoorPortaal(bezig) && RingFeature.PORTAAL_DICHT.equals(key(GrillPortalBlock.slot(ModDimensions.GUHMENSION, level, bezig))),
                    "refused at step " + stap + " of chapter 1");
        }
        h1.zet(bezig, h1.stappen());
        helper.assertTrue(Ring.magDoorPortaal(bezig) && GrillPortalBlock.slot(ModDimensions.GUHMENSION, level, bezig) == null, "the last step opens it");
        klaarTot(klaar, 1);
        helper.assertTrue(GrillPortalBlock.slot(ModDimensions.GUHMENSION, level, klaar) == null, "open: all of chapter 1 is done");
        bouwer.setGameMode(GameType.CREATIVE);
        helper.assertTrue(!Ring.magDoorPortaal(bouwer) && GrillPortalBlock.slot(ModDimensions.GUHMENSION, level, bouwer) != null, "a creative player is refused too");
        kijker.setGameMode(GameType.SPECTATOR);
        helper.assertTrue(Ring.magDoorPortaal(kijker) && GrillPortalBlock.slot(ModDimensions.GUHMENSION, level, kijker) == null, "a spectator passes");
        Entity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        helper.assertTrue(GrillPortalBlock.slot(ModDimensions.GUHMENSION, level, guh) == null, "only players are stopped");
        // (3) the way back is never asked, whoever it is; and the portal does lead back
        for (ServerPlayer p : List.of(nieuw, bouwer, kijker, klaar)) {
            helper.assertTrue(GrillPortalBlock.slot(BarbecuetherFeature.BARBECUETHER, level, p) == null, "the way back is never blocked");
        }
        helper.assertTrue(nl.juiced.guhs.feature.barbecuether.GrillPortalForcer.targetDimension(BarbecuetherFeature.BARBECUETHER) == ModDimensions.GUHMENSION,
                "a grill portal in the Barbecuether leads to the Guhmensie");
        // (4) somebody who is in the Barbecuether without chapter 1 (they lived there before the update) is left alone: the
        // whole upkeep of the story runs for them (Ring.OVERAL: this level counts as a story world) and they stay put
        Vec3 stond = nieuw.position();
        for (int i = 0; i < 60; i++) {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(nieuw));
            RingEvents.seconde(nieuw);
        }
        helper.assertTrue(nieuw.position().distanceToSqr(stond) < 1.0e-6 && nieuw.level() == level && !Ring.begonnen(nieuw) && !Ring.heeft(nieuw),
                "a player without chapter 1 is not moved and nothing starts for them: " + nieuw.position());
        weg(helper, nieuw, bezig, klaar, bouwer, kijker);
        helper.succeed();
    }

    /**
     * PHASE3 R14: a grill portal that makes its own frame on arrival never builds into a protected building: the spot it
     * wanted lies in a protected box, so the frame comes next to it, and not one block inside the box changes. (That the
     * search starts outside Guhdalfs sluiers is asked in ring-h2's world test, with a real wall.)
     */
    @GuhTest(template = KAMER, batch = BATCH + "_portaalplek")
    public static void ringPortaalNooitInEenGebouw(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos midden = helper.absolutePos(new BlockPos(10, 2, 10));
        BoundingBox doos = new BoundingBox(midden.getX() - 3, midden.getY() - 1, midden.getZ() - 3, midden.getX() + 3, midden.getY() + 6, midden.getZ() + 3);
        // (what the forcer may touch: put back afterwards, block for block)
        java.util.Map<BlockPos, net.minecraft.world.level.block.state.BlockState> was = new java.util.HashMap<>();
        for (BlockPos pos : BlockPos.betweenClosed(midden.offset(-20, -3, -20), midden.offset(20, 8, 20))) {
            was.put(pos.immutable(), level.getBlockState(pos));
        }
        String naam = "ring_test_portaaldoos";
        Bescherming.zetDoos(level, naam, doos);
        try {
            helper.assertTrue(!nl.juiced.guhs.feature.barbecuether.GrillPortalForcer.magHier(level, midden)
                    && nl.juiced.guhs.feature.barbecuether.GrillPortalForcer.magHier(level, helper.absolutePos(new BlockPos(2, 2, 16))), "inside the box no frame, outside it one may");
            var gemaakt = nl.juiced.guhs.feature.barbecuether.GrillPortalForcer.createPortal(level, midden, net.minecraft.core.Direction.Axis.X);
            helper.assertTrue(gemaakt.isPresent(), "a portal is made");
            BlockPos hoek = gemaakt.get().minCorner;
            helper.assertTrue(level.getBlockState(hoek).is(BarbecuetherFeature.BARBECUETHER_PORTAAL.get()) && !Bescherming.beschermd(level, hoek)
                    && hoek.closerThan(midden, 16), "it stands next to the protected box, not in it: " + hoek.subtract(midden));
            for (BlockPos pos : BlockPos.betweenClosed(doos.minX(), doos.minY(), doos.minZ(), doos.maxX(), doos.maxY(), doos.maxZ())) {
                helper.assertTrue(level.getBlockState(pos) == was.get(pos), "a block inside the protected box changed: " + pos.subtract(midden));
            }
        } finally {
            Bescherming.wisDozen(level, naam);
            was.forEach((pos, state) -> {
                if (level.getBlockState(pos) != state) {
                    level.setBlock(pos, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
                }
            });
        }
        helper.succeed();
    }

    /** The story as a whole: chapters, "reached", whose turn it is, the travel map, the title. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void ringVerhaal(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 2, 2), vriend = speler(helper, 3, 2);
        helper.assertTrue(Ring.hoofdstuk(p) == 0 && !Ring.begonnen(p) && !Ring.opReis(p) && Ring.bezigMet(p) == null, "not started");
        Ring.lijn(1).begin(p);
        helper.assertTrue(Ring.hoofdstuk(p) == 1 && Ring.opReis(p) && Ring.bezigMet(p) == Ring.lijn(1), "chapter 1");
        helper.assertTrue(Ring.bereikt(p, 1) && !Ring.bereikt(p, 2) && !Ring.bereikt(p, Ring.SAUSUMAN_NR), "only chapter 1 is reached");
        klaarTot(p, 4);
        helper.assertTrue(Ring.hoofdstuk(p) == 5 && Ring.bereikt(p, 5) && !Ring.bereikt(p, 6) && Ring.bereikt(p, Ring.SAUSUMAN_NR), "after chapter 4: 5 and the tower");
        // walk along with a friend: only the player who is exactly at a step solves its puzzle
        klaarTot(vriend, 6);
        helper.assertTrue(Ring.aanZet(p, Ring.lijn(5), 0) && !Ring.aanZet(vriend, Ring.lijn(5), 0) && !Ring.aanZet(p, Ring.lijn(6), 0),
                "a puzzle of chapter 5 is p's, not the friend's who is done; chapter 6 is not open for p");
        helper.assertTrue(Ring.klaar(vriend) && Ring.hoofdstuk(vriend) == 7 && !Ring.opReis(vriend), "the friend is done");
        Titels.Titel titel = Titels.van(RingFeature.TITEL);
        helper.assertTrue(titel != null && titel.behaald().test(vriend) && !titel.behaald().test(p), "the title Ringdrager");
        var kaart = Reiskaarten.van(Ring.REISKAART);
        helper.assertTrue(kaart != null && kaart.haltes().size() == 7 && kaart.haltes().get(0).lijn().equals("ring_h1")
                && kaart.haltes().get(6).lijn().equals("ring_sausuman"), "the travel map has the six chapters and the tower");
        helper.assertTrue("zw".equals(Ring.windstreek(-5, 5)) && "n".equals(Ring.windstreek(0, -9)) && "o".equals(Ring.windstreek(7, 0)), "compass words");
        weg(helper, p, vriend);
        helper.succeed();
    }

    /**
     * CONTRACT_130 13.9: a sluier is ONE protected box, and what is inside does not leak: a creature in it is not sent to
     * a player for whom it is closed, nor a sound at a spot in it. Own entities (Zicht) are their player's alone.
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void ringSluierEnZicht(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, 2, 2), open = speler(helper, 3, 2);
        VerhaalDemo.lijn().zet(open, VerhaalDemo.lijn().stappen());
        BlockPos o = helper.absolutePos(new BlockPos(12, 2, 12));
        Sluiers.wisPlekken(level, VerhaalDemo.PLEK);
        Sluiers.Zone zone = Sluiers.zetPlek(level, VerhaalDemo.PLEK, new BoundingBox(o.getX() - 2, o.getY(), o.getZ() - 2, o.getX() + 2, o.getY() + 3, o.getZ() + 2));
        try {
            GuhNpcEntity binnen = Cast.zet(level, GuhNpcEntity.Kind.GUHROND, Vec3.atBottomCenterOf(o), 0f, null);
            GuhNpcEntity buiten = Cast.zet(level, GuhNpcEntity.Kind.GIMGUH, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(4, 2, 12))), 0f, null);
            helper.assertTrue(binnen != null && buiten != null, "cast characters can be put down");
            helper.assertTrue(!Zicht.magZien(p, binnen) && Zicht.magZien(open, binnen) && Zicht.magZien(p, buiten), "a creature behind a closed sluier is not sent");
            helper.assertTrue(!binnen.broadcastToPlayer(p) && binnen.broadcastToPlayer(open) && buiten.broadcastToPlayer(p), "the entity tracker asks (the mixin is in)");
            helper.assertTrue(open.broadcastToPlayer(p), "players are always seen");
            helper.assertTrue(!Sluiers.magHoren(p, level, o.getX() + 0.5, o.getY(), o.getZ() + 0.5) && Sluiers.magHoren(open, level, o.getX() + 0.5, o.getY(), o.getZ() + 0.5)
                    && Sluiers.magHoren(p, level, o.getX() + 9.5, o.getY(), o.getZ() + 0.5) && Sluiers.heeftZones(level.dimension()), "a sound inside is not sent either");
            // one box: the corners of the smoke are protected, one block outside is not
            helper.assertTrue(Bescherming.beschermd(level, new BlockPos(zone.x0(), zone.y0(), zone.z0())) && Bescherming.beschermd(level, new BlockPos(zone.x1(), zone.y1(), zone.z1()))
                    && !Bescherming.beschermd(level, new BlockPos(zone.x1() + 1, zone.y1(), zone.z1()))
                    && VerhaalDemo.PLEK.equals(Bescherming.structuurBij(level, o)), "the protected box is the box of smoke");
            helper.assertTrue(!Bescherming.magWijzigen(level, o, null), "machines know the box too");
            // an entity of ONE player; a character that only exists at certain steps
            Zicht.alleenVoor(buiten, p.getUUID());
            helper.assertTrue(Zicht.magZien(p, buiten) && !Zicht.magZien(open, buiten) && p.getUUID().equals(Zicht.eigenaar(buiten)), "only its own player sees it");
            Zicht.voorIedereen(buiten);
            Zicht.alleenBij(buiten, "ring_h2", 1, 99);
            helper.assertTrue(!Zicht.magZien(p, buiten), "not at that step: not there");
            klaarTot(p, 2);
            helper.assertTrue(Zicht.magZien(p, buiten), "the story got there: it is there");
            Sluiers.wisPlekken(level, VerhaalDemo.PLEK);
            helper.assertTrue(Zicht.magZien(p, binnen) && !Bescherming.beschermd(level, o), "no sluier: seen, and free again");
            binnen.discard();
            buiten.discard();
        } finally {
            Sluiers.wisPlekken(level, VerhaalDemo.PLEK);
            VerhaalDemo.lijn().wis(open);
        }
        weg(helper, p, open);
        helper.succeed();
    }

    /** Wearing the ring: a Mika can't target you; the weight slows you down; the ring comes off when it leaves your pockets. */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void ringOmEnZwaarte(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, 4, 10);
        helper.assertTrue(!Ring.doeOm(p, true) && !Ring.om(p), "no ring: nothing to put on");
        helper.assertTrue(Ring.geef(p) && !Ring.geef(p) && tel(p, RingFeature.KNABBELRING.get()) == 1 && Ring.kreeg(p), "one ring per player");
        MikaEntity mika = ModEntities.MIKA.get().create(level, EntitySpawnReason.TRIGGERED);
        BlockPos naast = helper.absolutePos(new BlockPos(8, 2, 10));
        mika.snapTo(naast.getX() + 0.5, naast.getY(), naast.getZ() + 0.5);
        level.addFreshEntity(mika);
        mika.setTarget(p);
        helper.assertTrue(mika.getTarget() == p, "a Mika sees a player without the ring on");
        mika.setTarget(null);
        helper.assertTrue(Ring.doeOm(p, true) && Ring.om(p) && Ring.onzichtbaarVoorMikas(p) && Ring.oogZiet(p), "ring on: the Eye sees you");
        mika.setTarget(p);
        helper.assertTrue(mika.getTarget() == null, "but a Mika doesn't");
        mika.discard();
        RingEvents.seconde(p);
        helper.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.INVISIBILITY), "worn: not to be seen");
        // its weight
        double los = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        Ring.zetZwaarte(p, 1.0, 200);
        Ring.pasZwaarteToe(p);
        double zwaar = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
        helper.assertTrue(Math.abs(zwaar - los * (1 - Ring.ZWAAR_MAX)) < 1e-4 && Ring.zwaarte(p) == 1.0, "the heaviest ring: " + zwaar + " of " + los);
        Ring.zetZwaarte(p, 0, 200);
        Ring.pasZwaarteToe(p);
        helper.assertTrue(Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED) - los) < 1e-6, "light again");
        // it whispers; without the ring in the pockets it is off
        RingEvents.zetTrekNu(p);
        RingEvents.seconde(p);
        Ring.neem(p);
        helper.assertTrue(!Ring.om(p) && !Ring.heeft(p) && !Ring.kreeg(p) && Ring.zwaarte(p) == 0, "taken: off and gone");
        weg(helper, p);
        helper.succeed();
    }

    /**
     * (merge ring-kern) The test room is a floor three blocks above the flat test world and only its own chunks tick, so a
     * hunter that appeared on the ground around it never rides in: whether one landed on the floor was a matter of the
     * random angle and of which neighbouring chunks happened to be loaded. Every hunter outside the room is put on the far
     * side of the floor (still at a distance); in the real game they ride in from wherever they appeared.
     */
    private static List<KnekelRuiterEntity> naarBinnen(GameTestHelper helper, List<KnekelRuiterEntity> ruiters) {
        AABB kamer = helper.getBounds();
        int buiten = 0;
        for (KnekelRuiterEntity r : ruiters) {
            if (!kamer.contains(r.position())) {
                BlockPos plek = helper.absolutePos(new BlockPos(13 + buiten % 3 * 2, 2, 13 + buiten / 3 * 2));
                r.snapTo(plek.getX() + 0.5, plek.getY(), plek.getZ() + 0.5, r.getYRot(), 0f);
                buiten++;
            }
        }
        return ruiters;
    }

    /** The Nine: they come for whoever wears the ring, only that player sees them, caught = back at the rest point, unharmed. */
    @GuhTest(template = KAMER, batch = BATCH + "_negen", timeoutTicks = 500)
    public static void ringNegenPakken(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 3, 3), ander = speler(helper, 17, 17);
        Ring.lijn(1).begin(p);
        Ring.geef(p);
        Vec3 rust = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 18)));
        Rustpunten.zet(p, Ring.RUST, helper.getLevel().dimension(), rust, 0f);
        BlockPos hier = helper.absolutePos(new BlockPos(3, 2, 3));
        Ring.doeOm(p, true);
        helper.assertTrue(!Negen.wordtGejaagd(p), "not at once");
        int n = Negen.jaag(p);
        List<KnekelRuiterEntity> ruiters = Negen.ruiters(p);
        helper.assertTrue(n >= 1 && ruiters.size() == n && Negen.wordtGejaagd(p), "riders came: " + n);
        for (KnekelRuiterEntity r : ruiters) {
            helper.assertTrue(r.isJager() && Zicht.magZien(p, r) && !Zicht.magZien(ander, r) && r.ziet(p), "a hunter is its prey's alone and smells the ring");
            helper.assertTrue(r.distanceToSqr(p) >= 9 * 9, "they appear at a distance");
        }
        naarBinnen(helper, ruiters);
        float leven = p.getHealth();
        helper.succeedWhen(() -> {
            helper.assertTrue(p.position().distanceTo(rust) < 0.5, "caught: back at the rest point (now " + BlockPos.containing(p.position()) + ", from " + hier + ")");
            helper.assertTrue(!Ring.om(p) && Ring.heeft(p) && p.getHealth() == leven && !Negen.wordtGejaagd(p) && Negen.ruiters(p).isEmpty(),
                    "the ring is off, nothing is lost, nobody is hurt, the hunt is over");
            weg(helper, p, ander);
        });
    }

    /** Ways out: the Lichtflesje blinds them, and with the ring off they lose the scent and leave. */
    @GuhTest(template = KAMER, batch = BATCH + "_negen2", timeoutTicks = 400)
    public static void ringNegenKwijt(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 3, 3);
        Ring.lijn(1).begin(p);
        Ring.geef(p);
        Ring.doeOm(p, true);
        int n = Negen.jaag(p);
        // (only the test's own chunks tick: the riders that came inside the room are the ones that can be watched)
        AABB kamer = helper.getBounds();
        List<KnekelRuiterEntity> binnen = naarBinnen(helper, Negen.ruiters(p)).stream().filter(r -> kamer.contains(r.position())).toList();
        helper.assertTrue(n >= 1 && !binnen.isEmpty(), "riders came, some of them into the room: " + binnen.size() + " of " + n);
        // the Lichtflesje: a rider within its flash is blind for a while, the ones far away are not
        KnekelRuiterEntity dichtbij = Negen.patrouille(helper.getLevel(), List.of(helper.absolutePos(new BlockPos(7, 2, 3)), helper.absolutePos(new BlockPos(7, 2, 6))));
        helper.assertTrue(dichtbij != null && dichtbij.ziet(p), "a rider next to a worn ring smells it");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(RingFeature.LICHTFLESJE.get()));
        Gaven.flits(p);
        helper.assertTrue(dichtbij.isBlind() && !dichtbij.ziet(p) && dichtbij.staat() == KnekelRuiterEntity.BLIND, "the flash blinds the rider near");
        helper.assertTrue(Negen.ruiters(p).stream().noneMatch(KnekelRuiterEntity::isBlind), "(the hunters are still too far away for it)");
        dichtbij.discard();
        Ring.doeOm(p, false);
        helper.succeedWhen(() -> {
            helper.assertTrue(binnen.stream().allMatch(Entity::isRemoved), "ring off: they lose the scent and are gone");
            Negen.ruiters(p).forEach(KnekelRuiterEntity::verdwijn);   // (the ones outside the ticking chunks)
            Negen.tick(p);
            helper.assertTrue(!Negen.wordtGejaagd(p), "the hunt is over");
            weg(helper, p);
        });
    }

    /** A patrol sees a player in front of it and puts them back; a rock under the Elfenmanteltje it rides past. */
    @GuhTest(template = KAMER, batch = BATCH + "_patrouille", timeoutTicks = 500)
    public static void ringPatrouille(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, 10, 14);
        Ring.lijn(1).begin(p);
        Vec3 rust = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 18)));
        Rustpunten.zet(p, Ring.RUST, level.dimension(), rust, 0f);
        KnekelRuiterEntity r = Negen.patrouille(level, List.of(helper.absolutePos(new BlockPos(4, 2, 10)), helper.absolutePos(new BlockPos(16, 2, 10))));
        helper.assertTrue(r != null && !r.isJager() && r.route().size() == 2, "a patrol with its round");
        // a rock is not seen
        p.getInventory().add(new ItemStack(RingFeature.ELFENMANTELTJE.get()));
        p.setShiftKeyDown(true);
        for (int i = 0; i <= Gaven.ROTS_NA + 1; i++) {
            Gaven.rots(p);
        }
        helper.assertTrue(Gaven.isRots(p) && !Ring.oogZiet(p) && !r.ziet(p), "crouching still under the cloak: a rock, the Eye and the patrol look past it");
        List<Display.BlockDisplay> rotsen = level.getEntitiesOfClass(Display.BlockDisplay.class, new AABB(p.blockPosition()).inflate(2), e -> e.entityTags().contains(Gaven.ROTS_TAG));
        helper.assertTrue(rotsen.size() == 1 && p.isInvisible(), "a boulder stands where the player crouches");
        p.setShiftKeyDown(false);
        Gaven.rots(p);
        helper.assertTrue(!Gaven.isRots(p) && rotsen.get(0).isRemoved() && !p.isInvisible(), "standing up: a player again");
        // (the mock player is not ticked: no rock again by itself) now the patrol finds them
        helper.succeedWhen(() -> {
            helper.assertTrue(p.position().distanceTo(rust) < 0.5, "seen and caught by the patrol: back at the rest point");
            helper.assertTrue(r.isAlive() && r.prooi() == null, "the patrol stays and goes back to its round");
            r.discard();
            weg(helper, p);
        });
    }

    /** A rest fire becomes the rest point, Sam-guh is there (only for his own player), cooks once a day and follows. */
    @GuhTest(template = KAMER, batch = BATCH + "_sam", timeoutTicks = 600)
    public static void ringRustpuntEnSam(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, 7, 6), ander = speler(helper, 16, 16);
        BlockPos vuur = helper.absolutePos(VUUR);
        helper.assertTrue(level.getBlockState(vuur).getBlock() instanceof Rustpunt.Vuur, "the template has its Rustvuurtje");
        Rustpunt.zoek(p);
        helper.assertTrue(Ring.rustpunt(p) == null && Sam.van(p) == null, "not on the trip: a fire is just a fire, no Sam-guh");
        Ring.lijn(1).begin(p);
        Sam.roep(p);
        GuhEntity sam = Sam.van(p);
        helper.assertTrue(sam != null && sam.getVariant() == GuhVariant.SAM_GUH && Sam.isSam(sam) && VerhaalGuhs.isKopie(sam) && Sam.looptMee(p),
                "Sam-guh walks along");
        helper.assertTrue(Zicht.magZien(p, sam) && !Zicht.magZien(ander, sam) && Sam.van(ander) == null, "everybody has their own Sam-guh");
        Sam.tick(p);
        helper.assertTrue(Sam.van(p) == sam, "and only one");
        Rustpunt.zoek(p);
        Rustpunten.Punt punt = Ring.rustpunt(p);
        helper.assertTrue(punt != null && punt.plek().distanceTo(p.position()) < 0.01 && Rustpunt.aantal(p) == 1, "walking up to the fire: the rest point is where you stand");
        helper.assertTrue(Sam.doet(sam).equals(Sam.KOOKT), "Sam-guh starts cooking");
        Rustpunt.zoek(p);
        helper.assertTrue(Rustpunt.aantal(p) == 1, "the same fire counts once");
        helper.succeedWhen(() -> {
            helper.assertTrue(tel(p, RingFeature.STOOFPOTJE.get()) == 1, "a Stoofpotje from Sam-guh");
            helper.assertTrue(Sam.alGekookt(p, vuur) && Sam.doet(sam).equals(Sam.VOLGT), "once per fire per day");
            // put back on the rest point from anywhere, Sam comes along
            BlockPos ver = helper.absolutePos(new BlockPos(17, 2, 3));
            p.snapTo(ver.getX() + 0.5, ver.getY(), ver.getZ() + 0.5);
            helper.assertTrue(Ring.terugNaarRustpunt(p, Ring.GEVALLEN, null) && p.position().distanceTo(punt.plek()) < 0.5, "back at the rest point");
            helper.assertTrue(sam.distanceTo(p) < 4, "and Sam-guh hops along");
            // carried
            helper.assertTrue(Sam.draag(p) && p.getVehicle() == sam && Sam.doet(sam).equals(Sam.DRAAGT), "Sam-guh carries his player");
            Ring.geef(p);
            Ring.zetZwaarte(p, 1.0, 100);
            helper.assertTrue(Ring.zwaarte(p) == 1.0, "(the ring is heavy)");
            double los = p.getAttribute(Attributes.MOVEMENT_SPEED).getBaseValue();
            Ring.pasZwaarteToe(p);
            helper.assertTrue(Math.abs(p.getAttributeValue(Attributes.MOVEMENT_SPEED) - los) < 1e-6, "carried: the ring weighs nothing");
            p.stopRiding();
            Sam.tick(p);
            helper.assertTrue(Sam.doet(sam).equals(Sam.VOLGT), "off his back: he follows again");
            Sam.stuurWeg(p);
            helper.assertTrue(Sam.van(p) == null && sam.isRemoved(), "sent home");
            weg(helper, p, ander);
        });
    }

    /**
     * PHASE3 R09: the ring and the games do not mix. In a game (a level of Super Guhrio, a race...: Minigames.playing) the
     * ring can't be put on, a ring that was on comes off, a running hunt of the Nine ends and no new one starts.
     */
    @GuhTest(template = KAMER, batch = BATCH + "_spel")
    public static void ringNietInEenSpel(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 10, 10);
        klaarTot(p, 1);
        Ring.geef(p);
        helper.assertTrue(Ring.magOm(p) && Ring.doeOm(p, true) && Ring.om(p), "outside a game the ring goes on");
        String spel = "ring_test_spel";
        try {
            nl.juiced.guhs.feature.Minigames.registerGame(spel, pl -> pl == p);
            helper.assertTrue(!Ring.magOm(p), "in a game");
            RingEvents.seconde(p);
            helper.assertTrue(!Ring.om(p), "the ring comes off when a game begins");
            helper.assertTrue(!Ring.doeOm(p, true) && !Ring.om(p), "and can't be put on in it");
            for (int i = 0; i < 12; i++) {
                RingEvents.seconde(p);
            }
            helper.assertTrue(!Negen.wordtGejaagd(p), "the Nine have no business in a game");
        } finally {
            nl.juiced.guhs.feature.Minigames.registerGame(spel, pl -> false);
        }
        helper.assertTrue(Ring.doeOm(p, true) && Ring.om(p), "after the game the ring works as before");
        Ring.doeOm(p, false);
        weg(helper, p);
        helper.succeed();
    }

    /** The three gifts: the flask's lamp walks along, the rope pulls you to a hook. */
    @GuhTest(template = KAMER, batch = BATCH + "_gaven", timeoutTicks = 300)
    public static void ringGaven(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, 16, 12);
        Gaven.geef(p);
        Gaven.geef(p);
        helper.assertTrue(tel(p, RingFeature.LICHTFLESJE.get()) == 1 && tel(p, RingFeature.ELFENMANTELTJE.get()) == 1 && tel(p, RingFeature.ELFENTOUW.get()) == 1,
                "the three gifts, once");
        // the lamp
        p.getInventory().clearContent();
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(RingFeature.LICHTFLESJE.get()));
        Gaven.lamp(p);
        BlockPos oog = BlockPos.containing(p.getX(), p.getEyeY(), p.getZ());
        helper.assertTrue(level.getBlockState(oog).is(Blocks.LIGHT), "the flask in the hand: a light at your head");
        BlockPos verder = helper.absolutePos(new BlockPos(12, 2, 12));
        p.snapTo(verder.getX() + 0.5, verder.getY(), verder.getZ() + 0.5);
        Gaven.lamp(p);
        helper.assertTrue(level.getBlockState(oog).isAir() && level.getBlockState(BlockPos.containing(p.getX(), p.getEyeY(), p.getZ())).is(Blocks.LIGHT),
                "it walks along and leaves nothing behind");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Gaven.lamp(p);
        helper.assertTrue(level.getBlockState(BlockPos.containing(p.getX(), p.getEyeY(), p.getZ())).isAir(), "put away: dark again");
        // PHASE3 R11: the lamp is remembered with the player; one that a crash left behind is put out at the next login
        ItemStack flesje = new ItemStack(RingFeature.LICHTFLESJE.get());
        BlockPos lamp = BlockPos.containing(p.getX(), p.getEyeY(), p.getZ());
        p.setItemInHand(InteractionHand.MAIN_HAND, flesje);
        Gaven.lamp(p);
        helper.assertTrue(level.getBlockState(lamp).is(Blocks.LIGHT) && GuhQuests.saved(p).getLongOr("guhs_ring_lamp_pos", 0L) == lamp.asLong(),
                "the lamp's spot is written in the player's saved data");
        Gaven.wisAlles(null);                                   // (a crash: the server forgets every lamp, the block stays)
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Gaven.lamp(p);
        helper.assertTrue(level.getBlockState(lamp).is(Blocks.LIGHT), "after a crash the light block is still there");
        Gaven.doofBewaard(p);
        helper.assertTrue(level.getBlockState(lamp).isAir() && !GuhQuests.saved(p).contains("guhs_ring_lamp_pos"), "the next login puts it out and forgets it");
        // no lamp where nothing may be placed: inside a protected building
        String doos = "ring_test_lampdoos";
        Bescherming.zetDoos(level, doos, new BoundingBox(lamp.getX() - 1, lamp.getY() - 2, lamp.getZ() - 1, lamp.getX() + 1, lamp.getY() + 2, lamp.getZ() + 1));
        p.setItemInHand(InteractionHand.MAIN_HAND, flesje);
        Gaven.lamp(p);
        helper.assertTrue(level.getBlockState(lamp).isAir() && !Gaven.magLamp(level, lamp) && !GuhQuests.saved(p).contains("guhs_ring_lamp_pos"),
                "no light block inside a protected building");
        Bescherming.wisDozen(level, doos);
        Gaven.lamp(p);
        helper.assertTrue(level.getBlockState(lamp).is(Blocks.LIGHT), "outside it the lamp burns again");
        p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        Gaven.lamp(p);
        helper.assertTrue(level.getBlockState(lamp).isAir(), "and goes out");
        // the rope
        BlockPos haak = helper.absolutePos(HAAK);
        helper.assertTrue(level.getBlockState(haak).getBlock() instanceof Gaven.Haak, "the template has its hook");
        BlockPos onder = helper.absolutePos(new BlockPos(16, 2, 12));
        p.snapTo(onder.getX() + 0.5, onder.getY(), onder.getZ() + 0.5);
        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(haak));
        helper.assertTrue(haak.equals(Gaven.zoekHaak(p)), "the rope finds the hook you look at");
        Gaven.klim(p, haak);
        helper.assertTrue(Gaven.klimt(p), "it catches");
        float leven = p.getHealth();
        helper.onEachTick(() -> {
            if (Gaven.klimt(p)) {
                // (a mock player is not moved by the game: do what its own game would do with the pull)
                Gaven.klimTick(p);
                if (Gaven.klimt(p)) {
                    p.setPos(p.position().add(p.getDeltaMovement()));
                }
            }
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(!Gaven.klimt(p) && Vec3.atCenterOf(haak).distanceTo(p.position()) < 2.6, "pulled up to the hook");
            helper.assertTrue(p.getHealth() == leven && p.fallDistance == 0, "unharmed, no fall waiting");
            weg(helper, p);
        });
    }

    /** Smikagol: a guide of ONE player who leads the way; afterwards a buddy, once, who hands over his fish. */
    @GuhTest(template = KAMER, batch = BATCH + "_smikagol", timeoutTicks = 500)
    public static void ringSmikagol(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 8, 8), ander = speler(helper, 16, 16);
        Ring.lijn(1).begin(p);
        SmikagolEntity gids = Smikagol.roep(p, null);
        helper.assertTrue(gids != null && Smikagol.van(p) == gids && !gids.isMaatje() && Smikagol.isGids(p), "the guide comes");
        helper.assertTrue(Zicht.magZien(p, gids) && !Zicht.magZien(ander, gids), "only his own player sees him");
        Smikagol.tick(p);
        helper.assertTrue(Smikagol.van(p) == gids, "and only one");
        boolean[] aangekomen = {false};
        Vec3 doel = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(10, 2, 10)));
        helper.assertTrue(Smikagol.leid(p, List.of(Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(11, 2, 7))), doel), q -> aangekomen[0] = true) && gids.leidt(),
                "he leads along a route");
        helper.succeedWhen(() -> {
            helper.assertTrue(aangekomen[0] && !gids.leidt() && gids.position().distanceTo(doel) < 2.5, "he arrived and said so");
            Smikagol.stuurWeg(p);
            helper.assertTrue(Smikagol.van(p) == null && gids.isRemoved() && !Smikagol.isGids(p), "sent away");
            // the buddy
            SmikagolEntity maatje = Smikagol.maakMaatje(p);
            helper.assertTrue(maatje != null && maatje.isMaatje() && Smikagol.heeftMaatje(p) && Smikagol.maakMaatje(p) == null, "a buddy, once per player");
            helper.assertTrue(tel(p, RingFeature.VISSENBOTJE.get()) == 1 && Zicht.magZien(ander, maatje), "with the Vissenbotje; everybody sees a buddy");
            maatje.zetVissen(3);
            maatje.interact(ander, InteractionHand.MAIN_HAND, maatje.position());
            helper.assertTrue(maatje.vissen() == 3, "somebody else gets nothing");
            maatje.interact(p, InteractionHand.MAIN_HAND, maatje.position());
            helper.assertTrue(maatje.vissen() == 0 && tel(p, Items.COD) + tel(p, Items.SALMON) == 3, "lekkere vissss");
            p.setShiftKeyDown(true);
            maatje.interact(p, InteractionHand.MAIN_HAND, maatje.position());
            helper.assertTrue(maatje.blijft(), "crouch + click: he stays");
            p.setShiftKeyDown(false);
            maatje.discard();
            helper.assertTrue(Smikagol.roepMaatje(p) && Smikagol.maatje(p) != null && Smikagol.maatje(p) != maatje, "lost: the Vissenbotje brings a new one");
            Smikagol.maatje(p).discard();
            weg(helper, p, ander);
        });
    }

    /** The end of the story gives everything once; Sam-guh comes home for good; one treat a day at the party. */
    @GuhTest(template = KAMER, batch = BATCH + "_einde")
    public static void ringBeloningEnFeest(GameTestHelper helper) {
        ServerPlayer p = speler(helper, 8, 8);
        helper.assertTrue(!RingBeloning.geef(p) && !RingBeloning.feest(p, null), "nothing before the story is done");
        Ring.geef(p);
        Sam.roep(p);
        klaarTot(p, 5);
        Sam.tick(p);
        helper.assertTrue(!RingBeloning.gegeven(p) && Sam.van(p) != null, "five chapters: not yet");
        klaarTot(p, 6);
        helper.assertTrue(RingBeloning.gegeven(p) && !RingBeloning.geef(p), "the last step of chapter 6 gives the rewards, once");
        for (GuhClothes c : RingBeloning.KLEDING) {
            helper.assertTrue(tel(p, ModItems.clothingItem(c)) == 1 && "ring".equals(nl.juiced.guhs.feature.kleding.KledingBronnen.bron(c)), "outfit " + c);
        }
        helper.assertTrue(tel(p, RingH5Feature.OOG_VAN_SAUSRON_BEELDJE_ITEM.get()) == 1 && tel(p, RingFeature.ELFENTOUW_HAAK_ITEM.get()) == RingBeloning.HAKEN
                && tel(p, RingFeature.VISSENBOTJE.get()) == 1, "the statuette, hooks, the Vissenbotje");
        helper.assertTrue(Smikagol.heeftMaatje(p) && Smikagol.maatje(p) != null, "Smikagol is a buddy");
        Smikagol.maatje(p).discard();
        helper.assertTrue(VerhaalGuhs.magTemmen(p, VerhaalGuh.SAM_GUH), "Sam-guh may come home");
        Sam.tick(p);
        GuhEntity sam = Sam.van(p);
        helper.assertTrue(sam != null && Sam.looptMee(p), "he still stands next to his player");
        Sam.klik(sam, p, InteractionHand.MAIN_HAND);
        helper.assertTrue(VerhaalGuhs.heeftGetemd(p, VerhaalGuh.SAM_GUH) && sam.isRemoved() && Sam.van(p) == null && !Sam.looptMee(p), "a click: tamed, once");
        List<GuhEntity> eigen = new ArrayList<>(helper.getLevel().getEntitiesOfClass(GuhEntity.class, new AABB(p.blockPosition()).inflate(6),
                g -> g.getVariant() == GuhVariant.SAM_GUH && g.isOwnedBy(p)));
        helper.assertTrue(eigen.size() == 1, "an own Sam-guh");
        eigen.get(0).discard();
        // the daily party
        helper.assertTrue(RingBeloning.magFeesten(p) && RingBeloning.feest(p, null) && tel(p, RingFeature.FEESTKNABBEL.get()) == 1, "today's treat");
        helper.assertTrue(!RingBeloning.magFeesten(p) && !RingBeloning.feest(p, null) && tel(p, RingFeature.FEESTKNABBEL.get()) == 1, "one per day");
        weg(helper, p);
        helper.succeed();
    }

    /** Every cast kind has its role with a line for every phase of the story; wild guhs trot after the ring. */
    @GuhTest(template = KAMER, batch = BATCH + "_cast", timeoutTicks = 400)
    public static void ringCastEnKwijlen(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerPlayer p = speler(helper, 4, 14);
        for (GuhNpcEntity.Kind kind : Cast.KINDS) {
            GuhNpcEntity npc = Cast.zet(level, kind, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(18, 2, 18))), 0f, null);
            helper.assertTrue(npc != null && NpcRollen.van(npc) instanceof Cast.Rol, kind + " has its role");
            NpcRollen.van(npc).talk(npc, p);
            npc.discard();
        }
        helper.assertTrue(Cast.fase(p).equals(Cast.VOOR), "before the story");
        Ring.lijn(1).begin(p);
        helper.assertTrue(Cast.fase(p).equals(Cast.REIS), "on the trip");
        Ring.geef(p);
        RingEvents.seconde(p);
        GuhEntity wild = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        BlockPos ver = helper.absolutePos(new BlockPos(12, 2, 14));
        wild.snapTo(ver.getX() + 0.5, ver.getY(), ver.getZ() + 0.5);
        wild.setPersonality(nl.juiced.guhs.entity.GuhPersonality.BRAVE);
        level.addFreshEntity(wild);
        // (after the merge of 1.3.2) a stand-in that another feature builds on GuhEntity, here the sleeper in a Guhhuisje's room,
        // is nobody's guh either: it does not smell the ring and stays in its bed
        nl.juiced.guhs.feature.huisje.BinnenGuh slaper = nl.juiced.guhs.feature.huisje.HuisjeFeature.SLAPER.get().create(level, EntitySpawnReason.TRIGGERED);
        BlockPos bed = helper.absolutePos(new BlockPos(10, 2, 17));
        slaper.snapTo(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5);
        level.addFreshEntity(slaper);
        Vec3 lag = slaper.position();
        // (PHASE3 R09 / R10) a plain guh that stands somewhere as a prop (no AI: the guh that carries the Guhshi look in a
        // level of Super Guhrio, a show guh) is not lured away either
        GuhEntity pop = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        BlockPos sokkel = helper.absolutePos(new BlockPos(9, 2, 11));
        pop.snapTo(sokkel.getX() + 0.5, sokkel.getY(), sokkel.getZ() + 0.5);
        pop.setNoAi(true);
        level.addFreshEntity(pop);
        Vec3 stond = pop.position();
        long begin = level.getGameTime();
        helper.succeedWhen(() -> {
            helper.assertTrue(wild.distanceTo(p) < 4.2, "a wild guh that smells the ring trots after its bearer");
            helper.assertTrue(level.getGameTime() - begin >= 60, "three looks of every guh have passed");
            helper.assertTrue(slaper.isAlive() && slaper.position().distanceTo(lag) < 0.3 && slaper.distanceTo(p) > 4.2,
                    "the stand-in stays where it was put: " + slaper.position().distanceTo(lag));
            helper.assertTrue(pop.isAlive() && pop.position().distanceTo(stond) < 0.05 && !pop.getNavigation().isInProgress(), "a guh without AI is a prop: it stays");
            wild.discard();
            slaper.discard();
            pop.discard();
            weg(helper, p);
        });
    }

    private RingGameTests() {
    }
}
