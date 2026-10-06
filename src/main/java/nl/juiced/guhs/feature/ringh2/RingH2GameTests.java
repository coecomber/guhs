package nl.juiced.guhs.feature.ringh2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.ring.Cast;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.ring.RingFeature;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Sluiers;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.feature.wereld.Kopieen;
import nl.juiced.guhs.gametest.GuhMockPlayer;
import nl.juiced.guhs.gametest.GuhTest;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.storage.Nbt;

/**
 * bbq2 (ring-h2): chapter 2 of the Knabbelring, server side. Template ringh2_test_kamer (tools/features/ring_h2.py): a bare
 * floor of 25 x 25. The test server has no Barbecuether, so a copy of Guhvendel is a structure start made by hand
 * ({@link RingH2Commands#kopie}) around the room, placed so that the council ring's stone table lands on {@link #TAFEL}; the
 * characters are put down by the test. Mock players get no packets: a test posts their tick event itself (a mock "watches"
 * a card or a scene for two ticks) and calls the chapter's once-a-second upkeep by hand (with the posted ticks it also runs
 * by itself now and then: the asserts hold either way).
 */
public final class RingH2GameTests {
    private static final String KAMER = "ringh2_test_kamer", BATCH = "ringh2";
    /** Where the stone table of the council ring is in the test room. */
    private static final BlockPos TAFEL = new BlockPos(12, 2, 12);

    private static ServerPlayer speler(GameTestHelper helper, int x, int z) {
        ServerPlayer p = GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(new BlockPos(x, 2, z));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        p.setOnGround(true);
        Ring.OVERAL = true;
        return p;
    }

    private static void weg(GameTestHelper helper, ServerPlayer... ps) {
        for (ServerPlayer p : ps) {
            Ring.wis(p);
            Guhvendel.wis(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    /** Chapter 1 is done for this player (so chapter 2 is theirs). */
    private static void naHoofdstuk1(ServerPlayer p) {
        Ring.lijn(1).begin(p);
        Ring.lijn(1).zet(p, Ring.lijn(1).stappen());
    }

    private static void tik(ServerPlayer p) {
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(p));
    }

    private static int tel(ServerPlayer p, Item item) {
        return GuhQuests.count(p, item);
    }

    /** A copy of Guhvendel around the test room, turned, with the stone table on {@link #TAFEL}. */
    private static void kopie(GameTestHelper helper, Rotation draai) {
        ServerLevel level = helper.getLevel();
        BlockPos tafel = helper.absolutePos(TAFEL);
        BlockPos hoek = tafel.subtract(StructureTemplate.transform(Guhvendel.KRING, Mirror.NONE, draai, BlockPos.ZERO));
        BlockPos a = helper.absolutePos(new BlockPos(0, 0, 0)), b = helper.absolutePos(new BlockPos(24, 8, 24));
        RingH2Commands.kopie(level, hoek, draai, BoundingBox.fromCorners(a, b));
    }

    private static GuhNpcEntity zet(GameTestHelper helper, GuhNpcEntity.Kind kind, int x, int z) {
        BlockPos at = helper.absolutePos(new BlockPos(x, 2, z));
        GuhNpcEntity npc = Cast.zet(helper.getLevel(), kind, Vec3.atBottomCenterOf(at), 0f, Guhvendel.PLEK);
        helper.assertTrue(npc != null && NpcRollen.van(npc) instanceof GuhvendelRol, "a " + kind.id() + " of Guhvendel with the house's role");
        return npc;
    }

    private static void praat(GuhNpcEntity npc, ServerPlayer p) {
        NpcRollen.van(npc).talk(npc, p);
    }

    /**
     * The whole chapter for one player, step by step: the card, the arrival, Guhrond, the six (Araguh's sneaking lesson,
     * the snack from the bag), the bell with the council, volunteering with the fellowship, the provisions once, the
     * farewell. A friend who stands next to them all the time stays exactly where their own story is.
     */
    @GuhTest(template = KAMER, batch = BATCH, timeoutTicks = 300)
    public static void ringh2HetHeleHoofdstuk(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Verhaallijn lijn = RingH2Feature.LIJN;
        kopie(helper, Rotation.NONE);
        ServerPlayer p = speler(helper, 6, 12), vriend = speler(helper, 6, 14), vreemde = speler(helper, 6, 16);
        naHoofdstuk1(p);
        naHoofdstuk1(vriend);
        helper.assertTrue(lijn.stappen() == Guhvendel.STAPPEN && lijn.aanDeBeurt(p) && !lijn.aanDeBeurt(vreemde) && lijn.stap(p) == Guhvendel.REIS,
                "six steps; the chapter is the player's once chapter 1 is done");
        Vec3 binnen = p.position();
        Guhvendel.Oord o = Guhvendel.oord(level, p.blockPosition());
        helper.assertTrue(o != null && o.anker().equals(helper.absolutePos(TAFEL)) && o.bel().equals(helper.absolutePos(TAFEL.offset(-5, 1, -3)))
                && o.draai() == Rotation.NONE && o.inKom(binnen), "the copy is found: the stone table, the bell, the cirque");
        // the player is still on the way (far above the room: no copy near them)
        p.snapTo(binnen.x, binnen.y + 90, binnen.z);
        helper.assertTrue(Guhvendel.oord(level, p.blockPosition()) == null && !Guhvendel.binnen(p), "far away: no Guhvendel here");
        helper.setBlock(TAFEL.offset(-5, 1, -3), Blocks.BELL);
        List<GuhNpcEntity> npcs = new ArrayList<>();
        GuhNpcEntity guhrond = zet(helper, GuhNpcEntity.Kind.GUHROND, 10, 4), guhdalf = zet(helper, GuhNpcEntity.Kind.GUHDALF, 12, 4);
        GuhNpcEntity araguh = zet(helper, GuhNpcEntity.Kind.ARAGUH, 14, 4), leguhlas = zet(helper, GuhNpcEntity.Kind.LEGUHLAS, 16, 4);
        GuhNpcEntity gimguh = zet(helper, GuhNpcEntity.Kind.GIMGUH, 18, 4), boromika = zet(helper, GuhNpcEntity.Kind.BOROMIKA, 20, 4);
        GuhNpcEntity merrie = zet(helper, GuhNpcEntity.Kind.MERRIE, 10, 20), pippguh = zet(helper, GuhNpcEntity.Kind.PIPPGUH, 12, 20);
        npcs.addAll(List.of(guhrond, guhdalf, araguh, leguhlas, gimguh, boromika, merrie, pippguh));

        // somebody whose chapter 1 is not done: nothing here is theirs
        Guhvendel.seconde(vreemde);
        praat(guhrond, vreemde);
        helper.assertTrue(lijn.stap(vreemde) == 0 && !Cutscenes.bezig(vreemde) && !Guhvendel.luid(vreemde, o) && !lijn.begonnen(vreemde),
                "a player who did not finish chapter 1 gets no card, no arrival, no council");

        // step 0: the card first, then walking into the cirque is the arrival
        Guhvendel.seconde(p);
        helper.assertTrue(Cutscenes.bezig(p) && !Verteller.gezien(p, Guhvendel.KAART) && lijn.stap(p) == Guhvendel.REIS, "the narrator card of the chapter shows");
        // (the friend stands in the cirque all the time and reads the card too: from then on only what THEY do counts)
        Guhvendel.seconde(vriend);
        helper.onEachTick(() -> {
            tik(p);
            tik(vriend);
        });
        helper.runAfterDelay(6, () -> {
            helper.assertTrue(!Cutscenes.bezig(p) && Verteller.gezien(p, Guhvendel.KAART) && lijn.begonnen(p) && lijn.stap(p) == Guhvendel.REIS,
                    "the card was read: the chapter has begun, the player is not there yet (watching " + Cutscenes.bezig(p) + ", read "
                            + Verteller.gezien(p, Guhvendel.KAART) + ", begun " + lijn.begonnen(p) + ", step " + lijn.stap(p) + ")");
            Guhvendel.seconde(p);
            helper.assertTrue(lijn.stap(p) == Guhvendel.REIS, "still on the way: not arrived");
            p.snapTo(binnen.x, binnen.y, binnen.z);
            helper.assertTrue(Guhvendel.binnen(p), "the player walks into the cirque");
            Guhvendel.seconde(p);
            helper.assertTrue(lijn.stap(p) == Guhvendel.WELKOM, "inside the cirque: arrived (step " + lijn.stap(p) + ")");
            Guhvendel.seconde(p);
            helper.assertTrue(lijn.stap(p) == Guhvendel.WELKOM, "arriving happens once");

            // the six may be met before Guhrond's welcome; the step only moves on after it
            praat(leguhlas, p);
            helper.assertTrue(Guhvendel.heeftOntmoet(p, GuhNpcEntity.Kind.LEGUHLAS) && Guhvendel.ontmoet(p) == 1 && lijn.stap(p) == Guhvendel.WELKOM,
                    "Leguhlas is met, the step waits for Guhrond");
            helper.assertTrue(!Guhvendel.luid(p, o) && !Cutscenes.bezig(p), "the bell before Guhrond: no council");
            praat(guhrond, p);
            helper.assertTrue(lijn.stap(p) == Guhvendel.KENNIS, "Guhrond's welcome: step 2");
            helper.assertTrue(!Guhvendel.luid(p, o) && !Cutscenes.bezig(p) && lijn.stap(p) == Guhvendel.KENNIS, "the bell before everybody is met: no council");
            // Araguh wants to see sneaking first
            praat(araguh, p);
            praat(araguh, p);
            helper.assertTrue(!Guhvendel.heeftOntmoet(p, GuhNpcEntity.Kind.ARAGUH), "Araguh only counts once the player sneaks");
            p.setShiftKeyDown(true);
            praat(araguh, p);
            p.setShiftKeyDown(false);
            helper.assertTrue(Guhvendel.heeftOntmoet(p, GuhNpcEntity.Kind.ARAGUH), "crouching and clicking again: met");
            praat(gimguh, p);
            praat(boromika, p);
            praat(leguhlas, p);
            helper.assertTrue(Guhvendel.ontmoet(p) == 4, "meeting somebody twice counts once: " + Guhvendel.ontmoet(p));
            // Merrie and Pippguh snack one kaasknabbel from the bag, once, together
            p.getInventory().add(new ItemStack(ModItems.KAAS_KNABBELS.get(), 3));
            praat(merrie, p);
            helper.assertTrue(tel(p, ModItems.KAAS_KNABBELS.get()) == 2 && lijn.stap(p) == Guhvendel.KENNIS, "one knabbel is snacked");
            praat(pippguh, p);
            helper.assertTrue(tel(p, ModItems.KAAS_KNABBELS.get()) == 2, "and only one, ever");
            helper.assertTrue(Guhvendel.ontmoet(p) == 6 && lijn.stap(p) == Guhvendel.RAADSBEL, "everybody met: on to the bell (step " + lijn.stap(p) + ")");
            // the friend stood next to it all: nothing of this is theirs
            helper.assertTrue(lijn.stap(vriend) <= Guhvendel.WELKOM && Guhvendel.ontmoet(vriend) == 0 && tel(vriend, ModItems.KAAS_KNABBELS.get()) == 0,
                    "the friend met nobody and got nothing: step " + lijn.stap(vriend));
            helper.assertTrue(!Guhvendel.luid(vriend, o) && !Cutscenes.bezig(vriend), "the bell does not start a council for the friend");
            // step 3: the bell starts the council for the player whose turn it is
            helper.assertTrue(Guhvendel.luid(p, o) && Cutscenes.bezig(p) && lijn.stap(p) == Guhvendel.RAADSBEL, "the bell: the council plays, the step waits for its end");
            helper.assertTrue(!Guhvendel.luid(p, o), "one council at a time");
        });
        helper.runAfterDelay(12, () -> {
            helper.assertTrue(!Cutscenes.bezig(p) && Cutscenes.gezien(p, RingH2Scenes.RAAD.id()) && lijn.stap(p) == Guhvendel.MELDEN,
                    "after the council: step 4 (" + lijn.stap(p) + ")");
            // step 4: Guhrond asks who carries the ring
            NpcRollen.van(guhrond).antwoord(guhrond, p, GuhvendelRol.NOG_NIET);
            helper.assertTrue(lijn.stap(p) == Guhvendel.MELDEN && !Cutscenes.bezig(p), "'not yet': nothing happens");
            NpcRollen.van(guhdalf).antwoord(guhdalf, p, GuhvendelRol.JA);
            helper.assertTrue(lijn.stap(p) == Guhvendel.MELDEN, "Guhdalf's 'op weg' is not for this step");
            NpcRollen.van(guhrond).antwoord(guhrond, vriend, GuhvendelRol.JA);
            helper.assertTrue(!Cutscenes.bezig(vriend) && lijn.stap(vriend) <= Guhvendel.WELKOM, "the friend can't volunteer in somebody else's council");
            NpcRollen.van(guhrond).antwoord(guhrond, p, GuhvendelRol.JA);
            helper.assertTrue(Cutscenes.bezig(p) && lijn.stap(p) == Guhvendel.MELDEN, "'ik neem de ring wel mee': the fellowship plays");
        });
        helper.runAfterDelay(18, () -> {
            helper.assertTrue(!Cutscenes.bezig(p) && Cutscenes.gezien(p, RingH2Scenes.GENOOTSCHAP.id()) && lijn.stap(p) == Guhvendel.VERTREK,
                    "after the fellowship: step 5 (" + lijn.stap(p) + ")");
            helper.assertTrue(tel(p, ModItems.KAAS_KNABBELS.get()) == 8 && tel(p, RingFeature.STOOFPOTJE.get()) == 2, "Guhrond's provisions: 6 knabbels and 2 stews");
            Guhvendel.naGenootschap(p);
            helper.assertTrue(tel(p, ModItems.KAAS_KNABBELS.get()) == 8 && tel(p, RingFeature.STOOFPOTJE.get()) == 2, "the provisions are given once");
            // step 5: the farewell is Guhdalf's
            NpcRollen.van(guhrond).antwoord(guhrond, p, GuhvendelRol.JA);
            helper.assertTrue(lijn.stap(p) == Guhvendel.VERTREK, "Guhrond does not end the chapter");
            NpcRollen.van(guhdalf).antwoord(guhdalf, p, GuhvendelRol.NOG_NIET);
            helper.assertTrue(lijn.stap(p) == Guhvendel.VERTREK, "'ik kijk nog even rond'");
            NpcRollen.van(guhdalf).antwoord(guhdalf, p, GuhvendelRol.JA);
            helper.assertTrue(lijn.klaar(p) && Ring.bereikt(p, 3) && Ring.hoofdstuk(p) == 3, "'op weg': the chapter is done, chapter 3 is the player's");
            // afterwards everybody just talks (the default lines), nothing moves
            for (GuhNpcEntity npc : npcs) {
                praat(npc, p);
                NpcRollen.van(npc).antwoord(npc, p, GuhvendelRol.JA);
            }
            Guhvendel.seconde(p);
            helper.assertTrue(lijn.klaar(p) && !Cutscenes.bezig(p) && !Guhvendel.luid(p, o), "nothing starts again after the chapter");
            helper.assertTrue(lijn.stap(vriend) <= Guhvendel.WELKOM && !Cutscenes.gezien(vriend, RingH2Scenes.RAAD.id()) && tel(vriend, RingFeature.STOOFPOTJE.get()) == 0,
                    "and the friend still has it all ahead");
            for (GuhNpcEntity npc : npcs) {
                npc.discard();
            }
            Kopieen.testWissen(level);
            weg(helper, p, vriend, vreemde);
            helper.succeed();
        });
    }

    /**
     * The real template holds what the Java side expects where it expects it; the structure exists exactly once per world,
     * behind a sluier that opens with chapter 1, in the Superkompas; the card and both scenes are registered and say what
     * the engine can play.
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void ringh2GuhvendelBestaat(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Optional<StructureTemplate> sjabloon = level.getStructureManager().get(Guhs.id(Guhvendel.STRUCTUUR));
        helper.assertTrue(sjabloon.isPresent(), "the template guhs:guhvendel");
        StructureTemplate t = sjabloon.get();
        StructurePlaceSettings zo = new StructurePlaceSettings();
        List<StructureTemplate.StructureBlockInfo> bellen = t.filterBlocks(BlockPos.ZERO, zo, Blocks.BELL);
        helper.assertTrue(bellen.size() == 1 && bellen.get(0).pos().equals(Guhvendel.BEL), "one bell, at " + Guhvendel.BEL + ": " + bellen.size());
        helper.assertTrue(t.filterBlocks(BlockPos.ZERO, zo, Blocks.CHISELED_QUARTZ_BLOCK).stream().anyMatch(i -> i.pos().equals(Guhvendel.KRING)),
                "the stone table at " + Guhvendel.KRING);
        helper.assertTrue(!t.filterBlocks(BlockPos.ZERO, zo, RingFeature.RUSTVUUR.get()).isEmpty(), "a Rustvuurtje in the house");
        helper.assertTrue(t.getSize().getX() > 2 * Guhvendel.KOM && t.getSize().getZ() > 2 * Guhvendel.KOM && t.getSize().getY() >= 30,
                "a cirque of " + Guhvendel.KOM + " blocks fits in the template: " + t.getSize());
        helper.assertTrue(Guhvendel.MIDDEN.getX() - Guhvendel.KOM >= 0 && Guhvendel.MIDDEN.getX() + Guhvendel.KOM < t.getSize().getX(), "the cirque lies inside it");
        helper.assertTrue(Kopieen.structuur(level, Guhvendel.STRUCTUUR) != null, "the structure guhs:guhvendel");
        // every character once, the cast split over the three moments of the chapter
        helper.assertTrue(Guhvendel.BEWONERS.size() == 17 && Guhvendel.BEWONERS.stream().map(Guhvendel.Bewoner::id).distinct().count() == 17, "seventeen characters");
        for (GuhNpcEntity.Kind kind : Guhvendel.GEZELSCHAP) {
            helper.assertTrue(Guhvendel.BEWONERS.stream().filter(b -> b.kind() == kind).count() == 2, kind.id() + " before and at the council");
            helper.assertTrue(NpcRollen.rol(kind) != null, kind.id() + " has a role");
        }
        helper.assertTrue(Guhvendel.BEWONERS.stream().filter(b -> b.tot() > Guhvendel.STAPPEN).allMatch(b -> b.kind() == GuhNpcEntity.Kind.GUHROND),
                "only Guhrond lives here after the chapter");
        // hidden until the story gets here, then open and in the Superkompas
        ServerPlayer nieuw = speler(helper, 2, 2), verder = speler(helper, 3, 2);
        naHoofdstuk1(verder);
        helper.assertTrue(Sluiers.structuren().contains(Guhvendel.STRUCTUUR) && !Sluiers.open(nieuw, Guhvendel.STRUCTUUR) && Sluiers.isVerborgen(nieuw, Guhvendel.STRUCTUUR),
                "closed and hidden before chapter 1 is done");
        helper.assertTrue(Sluiers.open(verder, Guhvendel.STRUCTUUR) && !Sluiers.isVerborgen(verder, Guhvendel.STRUCTUUR), "open once chapter 1 is done");
        helper.assertTrue(SuperkompasItem.CATEGORIES.stream().anyMatch(c -> c.toString().contains(Guhvendel.STRUCTUUR)), "in the Superkompas");
        // the card and the scenes
        helper.assertTrue(Verteller.van(Guhvendel.KAART) != null && Verteller.van(Guhvendel.KAART).regels() == 4, "the narrator card with four lines");
        for (Cutscene scene : Guhvendel.scenes()) {
            helper.assertTrue(Cutscene.van(scene.id()) == scene && scene.duur() > 600 && scene.acteur(Cutscene.SPELER) != null && scene.acteur("ring") != null
                    && scene.acteur("guhrond") != null && scene.cameraOp(0) != null, scene.id() + " is registered with its actors and a camera");
            helper.assertTrue(scene.zinOp(scene.duur() - 60) != null || scene.zinOp(scene.duur() - 80) != null, scene.id() + " talks to the end");
        }
        helper.assertTrue(RingH2Scenes.RAAD.plek("ring", 0).y < 1.0 && RingH2Scenes.RAAD.plek("ring", 400).y > 1.2,
                "the ring waits inside the stone and lies on it once the player laid it there");
        helper.assertTrue(RingH2Scenes.GENOOTSCHAP.plek("sam", 0).distanceTo(RingH2Scenes.GENOOTSCHAP.plek("sam", 600)) > 5, "Sam-guh runs out of the bushes");
        helper.assertTrue(RingH2Scenes.GENOOTSCHAP.acteur("merrie") != null && RingH2Scenes.RAAD.acteur("merrie") == null,
                "Merrie and Pippguh are not at the secret council, they are at the fellowship");
        // the ring on the stone is a real item display of the Knabbelring
        Entity ring = EntityType.ITEM_DISPLAY.create(level, EntitySpawnReason.LOAD);
        CompoundTag tag = new CompoundTag();
        RingH2Scenes.ring().accept(tag);
        Nbt.load(ring, tag);
        helper.assertTrue(ring instanceof Display.ItemDisplay && ring.getSlot(0) != null && ring.getSlot(0).get().is(RingFeature.KNABBELRING.get()),
                "the ring actor holds the Knabbelring");
        weg(helper, nieuw, verder);
        helper.succeed();
    }

    /**
     * The cast only exists in its own scenes: a character of the template is made as the template has it (turned with the
     * copy, the house's plek) and is only shown to players whose own step is in its range. And a turned copy maps the bell
     * and the stone table the way a template is turned.
     */
    @GuhTest(template = KAMER, batch = BATCH)
    public static void ringh2AlleenInHunEigenScene(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Verhaallijn lijn = RingH2Feature.LIJN;
        kopie(helper, Rotation.CLOCKWISE_90);
        ServerPlayer voor = speler(helper, 4, 4), raad = speler(helper, 5, 4), na = speler(helper, 6, 4);
        for (ServerPlayer p : List.of(voor, raad, na)) {
            naHoofdstuk1(p);
        }
        lijn.zet(voor, Guhvendel.KENNIS);
        lijn.zet(raad, Guhvendel.MELDEN);
        lijn.zet(na, Guhvendel.STAPPEN);
        Guhvendel.Oord o = Guhvendel.oord(level, voor.blockPosition());
        BlockPos tafel = helper.absolutePos(TAFEL);
        // (a quarter turn clockwise: template +x becomes world +z, template +z becomes world -x)
        helper.assertTrue(o != null && o.draai() == Rotation.CLOCKWISE_90 && o.anker().equals(tafel) && o.bel().equals(tafel.offset(3, 1, -5)),
                "a turned copy: the bell is where a turned template has it: " + (o == null ? null : o.bel().subtract(tafel)));
        List<GuhNpcEntity> gemaakt = new ArrayList<>();
        for (Guhvendel.Bewoner b : Guhvendel.BEWONERS) {
            GuhNpcEntity npc = Guhvendel.maak(b, level, Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(12, 2, 12))), Rotation.CLOCKWISE_90);
            helper.assertTrue(npc != null && npc.getKind() == b.kind() && Guhvendel.PLEK.equals(npc.roleData.getStringOr(NpcRollen.PLEK, ""))
                    && npc.isInvulnerable() && NpcRollen.van(npc) instanceof GuhvendelRol, b.id() + " is made with the house's role");
            helper.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(npc.getYRot() - (b.yaw() + 90f))) < 0.01f, b.id() + " is turned with the copy");
            gemaakt.add(npc);
            boolean zietVoor = Zicht.magZien(voor, npc), zietRaad = Zicht.magZien(raad, npc), zietNa = Zicht.magZien(na, npc);
            helper.assertTrue(zietVoor == (b.van() <= Guhvendel.KENNIS && Guhvendel.KENNIS <= b.tot()), b.id() + " for a player who is meeting everybody: " + zietVoor);
            helper.assertTrue(zietRaad == (b.van() <= Guhvendel.MELDEN && Guhvendel.MELDEN <= b.tot()), b.id() + " for a player at the council: " + zietRaad);
            helper.assertTrue(zietNa == (b.tot() > Guhvendel.STAPPEN), b.id() + " for a player who is done: " + zietNa);
        }
        // nobody sees two of the same character at once, and at every moment there is a Guhrond
        for (ServerPlayer p : List.of(voor, raad, na)) {
            for (GuhNpcEntity.Kind kind : Cast.KINDS) {
                long n = gemaakt.stream().filter(npc -> npc.getKind() == kind && Zicht.magZien(p, npc)).count();
                helper.assertTrue(n <= 1, n + " x " + kind.id() + " for one player");
            }
            helper.assertTrue(gemaakt.stream().anyMatch(npc -> npc.getKind() == GuhNpcEntity.Kind.GUHROND && Zicht.magZien(p, npc)), "a Guhrond for everybody");
        }
        helper.assertTrue(gemaakt.stream().filter(npc -> Zicht.magZien(na, npc)).count() == 1, "after the chapter the fellowship is gone");
        for (GuhNpcEntity npc : gemaakt) {
            npc.discard();
        }
        Kopieen.testWissen(level);
        weg(helper, voor, raad, na);
        helper.succeed();
    }

    private RingH2GameTests() {
    }
}
