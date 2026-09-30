package nl.juiced.guhs.feature.creche;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.creche.WiegjeBlock.Baby;
import nl.juiced.guhs.feature.creche.WiegjeBlock.Wens;
import nl.juiced.guhs.feature.knus.Feesttaak;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.Knusfeest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the Knuffelcreche: the care round (step by step, wrong items, the lullaby, the rewards and Knus
 * counters), the knutsel round for the Grote Knusfeest (feestslingers + Knusfeest.gemaakt), the minigame (a baby crawls
 * out, is picked up and put back for points, one escapes, the end with speenmunten and the highscore), every reward +1,
 * rocking a crib and the babyflesje for your own baby guh, Juf Knuffel's role and shop, and the building template.
 * The room creche_test_kamer: six cribs (template y 1 = helper y 2).
 */
@GameTestHolder(Guhs.MODID)
@PrefixGameTestTemplate(false)
public class CrecheGameTests {
    private static final String KAMER = "creche_test_kamer";
    private static final String EMPTY = "empty";

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static GuhNpcEntity juf(GameTestHelper helper) {
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(7, 2, 7));
        npc.setKind(GuhNpcEntity.Kind.JUF_KNUFFEL);
        return npc;
    }

    private static ItemStack stack(ServerPlayer p, Item item) {
        for (ItemStack s : p.getInventory().items) {
            if (s.is(item)) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    private static void klaarMet(GameTestHelper helper, GuhNpcEntity npc, ServerPlayer... players) {
        CrecheGame.of(npc).reset(helper.getLevel());
        CrecheGame.vergeet(npc);
        npc.discard();
        for (ServerPlayer p : players) {
            Knusfeest.vergeet(p);
        }
        leave(helper, players);
    }

    /** Takes a baby through all its steps (with the right items; the song sung well). */
    private static void verzorg(GameTestHelper helper, CrecheGame game, GuhNpcEntity npc, ServerPlayer p, BlockPos pos) {
        ServerLevel level = helper.getLevel();
        for (Wens stap : CrecheGame.stappen(game.feest)) {
            helper.assertTrue(CrecheGame.wens(level, pos) == stap, "the crib shows " + stap + ", not " + CrecheGame.wens(level, pos));
            switch (stap) {
                case KNUTSELEN -> CrecheGame.opWiegje(level, pos, p, stack(p, Items.PAPER));
                case HONGER -> CrecheGame.opWiegje(level, pos, p, stack(p, CrecheFeature.BABYFLESJE.get()));
                case LUIER -> CrecheGame.opWiegje(level, pos, p, stack(p, CrecheFeature.SCHONE_LUIER.get()));
                case SLAAP -> CrecheGame.opWiegje(level, pos, p, stack(p, CrecheFeature.KNUFFELDEKENTJE.get()));
                case LIEDJE -> {
                    CrecheGame.opWiegje(level, pos, p, ItemStack.EMPTY);   // (opens the lullaby screen)
                    game.liedjeKlaar(npc, p, pos, game.liedje.ordinal(), game.liedje.aantal());
                }
                default -> {
                }
            }
        }
    }

    @GameTest(template = KAMER)
    public static void crecheVerzorgronde(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(7, 2, 5));
        GuhNpcEntity npc = juf(helper);
        try {
            ServerLevel level = helper.getLevel();
            helper.assertTrue(Features.role(GuhNpcEntity.Kind.JUF_KNUFFEL) == CrecheFeature.JUF_KNUFFEL, "Juf Knuffel's role");
            CrecheFeature.JUF_KNUFFEL.talk(npc, p);
            CrecheGame game = CrecheGame.of(npc);
            helper.assertTrue(game.startVerzorgen(npc, p), "the care round starts");
            helper.assertTrue(game.wiegjes.size() == 6 && game.zorg.size() == CrecheGame.VERZORG_BABYS, "six cribs, three babies awake");
            helper.assertTrue(!game.feest && game.liedje == Slaapliedje.STERRETJES, "no feest; the first song is sterretjes");
            helper.assertTrue(Minigames.playing(p) != null && Minigames.CRECHE.equals(Minigames.playing(p)), "a care round counts as a game");
            helper.assertTrue(GuhQuests.count(p, CrecheFeature.BABYFLESJE.get()) == 3 && GuhQuests.count(p, CrecheFeature.SCHONE_LUIER.get()) == 3
                    && GuhQuests.count(p, CrecheFeature.KNUFFELDEKENTJE.get()) == 1, "bottles, nappies and a blanket");
            List<BlockPos> babys = List.copyOf(game.zorg.keySet());
            BlockPos eerste = babys.get(0);
            // the wrong thing does nothing
            CrecheGame.opWiegje(level, eerste, p, stack(p, CrecheFeature.SCHONE_LUIER.get()));
            helper.assertTrue(CrecheGame.wens(level, eerste) == Wens.HONGER && GuhQuests.count(p, CrecheFeature.SCHONE_LUIER.get()) == 3,
                    "a nappy for a hungry baby: nothing happens");
            // a lullaby sung badly: again
            CrecheGame.opWiegje(level, eerste, p, stack(p, CrecheFeature.BABYFLESJE.get()));
            CrecheGame.opWiegje(level, eerste, p, stack(p, CrecheFeature.SCHONE_LUIER.get()));
            CrecheGame.opWiegje(level, eerste, p, stack(p, CrecheFeature.KNUFFELDEKENTJE.get()));
            helper.assertTrue(CrecheGame.wens(level, eerste) == Wens.LIEDJE && CrecheGame.baby(level, eerste) == Baby.INGESTOPT, "tucked in, now a song");
            helper.assertTrue(GuhQuests.count(p, CrecheFeature.KNUFFELDEKENTJE.get()) == 1, "the blanket stays yours");
            game.liedjeKlaar(npc, p, eerste, game.liedje.ordinal(), 2);
            helper.assertTrue(CrecheGame.wens(level, eerste) == Wens.LIEDJE, "2 notes isn't enough");
            game.liedjeKlaar(npc, p, eerste, game.liedje.ordinal(), game.liedje.aantal());
            helper.assertTrue(CrecheGame.wens(level, eerste) == Wens.GEEN && CrecheGame.baby(level, eerste) == Baby.INGESTOPT, "asleep");
            helper.assertTrue(KnusVoortgang.heeft(p, CrecheVoortgang.SLAAPLIEDJES, "sterretjes"), "the song is in the collection");
            verzorg(helper, game, npc, p, babys.get(1));
            verzorg(helper, game, npc, p, babys.get(2));
            helper.assertTrue(!game.isRunning() && !CrecheGame.isPlaying(p), "all asleep: the round is over");
            helper.assertTrue(GuhQuests.count(p, CrecheFeature.SPEENMUNT.get()) == CrecheGame.VERZORG_MUNTEN + CrecheGame.FIRST_COINS,
                    "speenmunten (and the first-time present): " + GuhQuests.count(p, CrecheFeature.SPEENMUNT.get()));
            helper.assertTrue(KnusVoortgang.teller(p, CrecheVoortgang.RONDES) == 1 && KnusVoortgang.teller(p, CrecheVoortgang.INGESTOPT) == 3,
                    "Knus counters");
            helper.assertTrue(GuhQuests.count(p, CrecheFeature.FEESTSLINGERS_ITEM.get()) == 0, "no feestslingers outside the Knusfeest");
            // the next round teaches the next song
            helper.assertTrue(CrecheGame.volgendLiedje(p, level) == Slaapliedje.MAANTJE, "next time: maantje");
        } finally {
            klaarMet(helper, npc, p);
        }
        helper.succeed();
    }

    @GameTest(template = KAMER)
    public static void crecheSlingersVoorHetKnusfeest(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(7, 2, 5));
        GuhNpcEntity npc = juf(helper);
        try {
            Knusfeest.nieuweRonde(p, 0, EnumSet.of(Feesttaak.FEESTSLINGERS));
            CrecheGame game = CrecheGame.of(npc);
            helper.assertTrue(game.startVerzorgen(npc, p) && game.feest, "the knutsel round");
            helper.assertTrue(GuhQuests.count(p, Items.PAPER) == 3, "paper to knutsel with");
            for (BlockPos pos : List.copyOf(game.zorg.keySet())) {
                verzorg(helper, game, npc, p, pos);
            }
            ItemStack slingers = stack(p, CrecheFeature.FEESTSLINGERS_ITEM.get());
            helper.assertTrue(!slingers.isEmpty() && slingers.is(KnusTags.FEESTSLINGERS), "feestslingers (tag guhs:knus/feestslingers)");
            helper.assertTrue(Knusfeest.stap(p, Feesttaak.FEESTSLINGERS) == Knusfeest.Stap.GEMAAKT, "the task is made");
        } finally {
            klaarMet(helper, npc, p);
        }
        helper.succeed();
    }

    @GameTest(template = KAMER)
    public static void crecheTerugbrengen(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(7, 2, 7));
        ServerPlayer ander = player(helper, new BlockPos(9, 2, 7));
        GuhNpcEntity npc = juf(helper);
        try {
            ServerLevel level = helper.getLevel();
            CrecheGame game = CrecheGame.of(npc);
            helper.assertTrue(game.startTerugbrengen(npc, p), "the minigame starts");
            helper.assertTrue(!game.startTerugbrengen(npc, ander), "one player at a time");
            helper.assertTrue(game.wiegjes.stream().allMatch(w -> CrecheGame.baby(level, w) == Baby.WAKKER), "all babies in bed, awake");
            game.aftellen = 0;
            game.wordWakker(level);
            helper.assertTrue(game.babys.size() == 1, "a baby crawls out");
            CrecheBabyguh baby = (CrecheBabyguh) level.getEntity(game.babys.iterator().next());
            BlockPos leeg = game.wiegjes.stream().filter(w -> CrecheGame.baby(level, w) == Baby.LEEG).findFirst().orElseThrow();
            CrecheGame.pakOp(baby, ander);
            helper.assertTrue(!baby.isGedragen(), "someone else can't pick it up");
            baby.mobInteract(p, InteractionHand.MAIN_HAND);
            helper.assertTrue(baby.isGedragen() && p.getUUID().equals(baby.drager()), "picked up");
            helper.assertTrue(CrecheGame.opWiegje(level, leeg, p, ItemStack.EMPTY), "into the empty crib");
            helper.assertTrue(game.terug == 1 && game.punten >= 20 && game.babys.isEmpty() && CrecheGame.baby(level, leeg) == Baby.WAKKER,
                    "back in bed: points " + game.punten);
            // one crawls away too long: the Juf fetches it
            game.wordWakker(level);
            CrecheBabyguh weg = (CrecheBabyguh) level.getEntity(game.babys.iterator().next());
            weg.uitSinds = level.getGameTime() - CrecheGame.WEGLOOP_TICKS - 5;
            game.tick(npc);
            helper.assertTrue(game.ontsnapt == 1 && game.babys.isEmpty() && game.reeks == 0, "the Juf brought it back");
            int punten = game.punten;
            // the end
            game.timer = 1;
            game.tick(npc);
            helper.assertTrue(!game.isRunning() && !CrecheGame.isPlaying(p), "over");
            helper.assertTrue(GuhQuests.count(p, CrecheFeature.SPEENMUNT.get()) == CrecheGame.munten(punten) + CrecheGame.FIRST_COINS,
                    "speenmunten " + GuhQuests.count(p, CrecheFeature.SPEENMUNT.get()));
            helper.assertTrue(CrecheGame.best(p) == punten, "the highscore is kept: " + CrecheGame.best(p));
            helper.assertTrue(KnusVoortgang.teller(p, CrecheVoortgang.TERUG) == 1 && KnusVoortgang.teller(p, CrecheVoortgang.SPELLETJES) == 1,
                    "Knus counters");
            helper.assertTrue(game.wiegjes.stream().allMatch(w -> CrecheGame.baby(level, w) == Baby.INGESTOPT), "everybody asleep again");
        } finally {
            klaarMet(helper, npc, p, ander);
        }
        helper.succeed();
    }

    /** Every reward rule gives one more than it would have before the "+1 per reward" of 2.7.0. */
    @GameTest(template = EMPTY)
    public static void crecheBeloningenPlusEen(GameTestHelper helper) {
        for (int score = 0; score <= 1000; score += 5) {
            int old = score <= 0 ? 0 : Math.min(12, score / 60) + 1;
            helper.assertTrue(CrecheGame.munten(score) == (score > 0 ? old + 1 : 0), "munten " + score + ": " + CrecheGame.munten(score));
        }
        helper.assertTrue(CrecheGame.VERZORG_MUNTEN == 2 + 1 && CrecheGame.FIRST_COINS == 3 + 1 && CrecheGame.RECORD_BONUS == 1 + 1,
                "care round, first time, record");
        helper.assertTrue(CrecheGame.punten(0, 0) == 20 && CrecheGame.punten(30, 0) == 10 && CrecheGame.punten(0, 9) == 30, "points per baby");
        for (Slaapliedje s : Slaapliedje.values()) {
            helper.assertTrue(s.aantal() >= 10 && !s.gelukt(s.aantal() / 2) && s.gelukt(s.aantal()), "song " + s.id());
        }
        helper.succeed();
    }

    @GameTest(template = KAMER)
    public static void crecheWiegenEnBabyflesje(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(7, 2, 7));
        try {
            ServerLevel level = helper.getLevel();
            GuhEntity baby = helper.spawn(ModEntities.GUH.get(), new BlockPos(7, 2, 5));
            baby.setAge(-24000);
            BlockPos wieg = helper.absolutePos(new BlockPos(7, 2, 3));
            CrecheGame.wieg(level, wieg, p);
            int na = baby.getAge();
            helper.assertTrue(na > -24000, "rocking: the baby guh grows a little (" + na + ")");
            CrecheGame.wieg(level, wieg, p);
            helper.assertTrue(baby.getAge() == na, "not again right away");
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(CrecheFeature.BABYFLESJE.get(), 2));
            helper.assertTrue(GuhHooks.runKlik(baby, p, InteractionHand.MAIN_HAND).consumesAction(), "a bottle for the baby");
            helper.assertTrue(baby.getAge() > na && p.getMainHandItem().getCount() == 1, "it drank and grew");
            GuhEntity groot = helper.spawn(ModEntities.GUH.get(), new BlockPos(9, 2, 5));
            groot.setAge(0);
            GuhHooks.runKlik(groot, p, InteractionHand.MAIN_HAND);
            helper.assertTrue(p.getMainHandItem().getCount() == 1, "a grown guh doesn't take a bottle");
            baby.discard();
            groot.discard();
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void crecheWinkelEnTags(GameTestHelper helper) {
        var offers = CrecheFeature.shop();
        helper.assertTrue(offers.stream().allMatch(o -> o.getCostA().is(CrecheFeature.SPEENMUNT.get())), "paid in speenmunten");
        helper.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(ModItems.clothingItem(GuhClothes.BABYMUTSJE)))
                && offers.stream().anyMatch(o -> o.getResult().is(ModItems.clothingItem(GuhClothes.ROMPERTJE)))
                && offers.stream().anyMatch(o -> o.getResult().is(ModItems.clothingItem(GuhClothes.SPEENKETTINKJE))), "the baby clothes");
        helper.assertTrue(offers.stream().noneMatch(o -> o.getResult().is(CrecheFeature.FEESTSLINGERS_ITEM.get())), "feestslingers aren't for sale");
        helper.assertTrue(new ItemStack(CrecheFeature.SPEENMUNT.get()).is(KnusTags.GRIJPTICKETS), "a speenmunt plays the grijpmachine");
        helper.assertTrue(GuhClothes.ROMPERTJE.slot == GuhClothes.Slot.BODY && GuhClothes.BABYMUTSJE.slot == GuhClothes.Slot.HEAD
                && GuhClothes.SPEENKETTINKJE.slot == GuhClothes.Slot.NECK, "clothes slots");
        helper.succeed();
    }

    /** The building (the plein slot template): 31 x 40 x 31, one jigsaw at (15, 4, 30), cribs round the Juf. */
    @GameTest(template = EMPTY)
    public static void crecheGebouw(GameTestHelper helper) {
        StructureTemplate t = helper.getLevel().getStructureManager().get(Guhs.id("knuffeldal_stadje/creche")).orElse(null);
        helper.assertTrue(t != null, "the template exists");
        helper.assertTrue(t.getSize().getX() == 31 && t.getSize().getZ() == 31 && t.getSize().getY() >= 16 && t.getSize().getY() <= 48,
                "size " + t.getSize());
        var settings = new StructurePlaceSettings();
        var jigsaws = t.filterBlocks(BlockPos.ZERO, settings, Blocks.JIGSAW);
        helper.assertTrue(jigsaws.size() == 1 && jigsaws.get(0).pos().equals(new BlockPos(15, 4, 30)), "one jigsaw at (15, 4, 30)");
        var wiegjes = t.filterBlocks(BlockPos.ZERO, settings, CrecheFeature.GUH_WIEGJE.get());
        helper.assertTrue(wiegjes.size() >= 8, "cribs: " + wiegjes.size());
        for (var w : wiegjes) {
            BlockState s = w.state();
            helper.assertTrue(s.getValue(WiegjeBlock.BABY) == Baby.INGESTOPT, "babies asleep in the template");
        }
        helper.assertTrue(t.filterBlocks(BlockPos.ZERO, settings, nl.juiced.guhs.feature.knuffeldal.KnuffeldalFeature.KNUFFELSTEEN_GEZICHT.get()).size() >= 6,
                "guh faces");
        helper.succeed();
    }
}
