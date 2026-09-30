package nl.juiced.guhs.feature.theehuis;

import java.util.EnumSet;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.knus.Feesttaak;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.Knusfeest;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the Knabbelthee-huisje: making tea in a theepotje, the teas' effects, a whole theekransje (the guests sit
 * down on the chairs, wishes, pouring tea and serving cake, a gezellige tafel: the effect, the theemutsje, the milestones,
 * the feest_theeservies while THEESERVIES is open, and everybody stands up again), no kransje without tamed guhs, the
 * points, Mevrouw Theelepel's role and the building template. The room theehuis_test_kamer: four tables and eight chairs.
 */
public class TheehuisGameTests {
    private static final String KAMER = "theehuis_test_kamer";
    private static final String EMPTY = "empty";

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper, BlockPos at) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos abs = helper.absolutePos(at);
        p.snapTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return p;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static GuhNpcEntity theelepel(GameTestHelper helper) {
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), new BlockPos(12, 2, 12));
        npc.setKind(GuhNpcEntity.Kind.THEEGUH);
        return npc;
    }

    @GuhTest(template = KAMER)
    public static void theehuisTheeZetten(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(3, 2, 3));
        try {
            ServerLevel level = helper.getLevel();
            helper.assertTrue(TheeBlocks.thee(new ItemStack(ModItems.KAAS_KNABBELS.get())) == TheeBlocks.Soort.KNABBELTHEE
                    && TheeBlocks.thee(new ItemStack(Items.PAPER)) == null, "kaasknabbels make knabbelthee, paper nothing");
            ItemStack knabbels = new ItemStack(ModItems.KAAS_KNABBELS.get(), 3);
            p.setItemInHand(InteractionHand.MAIN_HAND, knabbels);
            TheeBlocks.zet(p, level, helper.absolutePos(new BlockPos(2, 2, 2)), knabbels, TheeBlocks.Soort.KNABBELTHEE);
            helper.assertTrue(knabbels.getCount() == 2, "one knabbel in the pot");
            helper.assertTrue(GuhQuests.count(p, TheehuisFeature.thee(TheeBlocks.Soort.KNABBELTHEE)) == TheeBlocks.Theepotje.KOPJES, "two cups");
            helper.assertTrue(KnusVoortgang.heeft(p, TheehuisVoortgang.THEESOORTEN, "knabbelthee"), "in the theesoorten collection");
            helper.assertTrue(new ItemStack(TheehuisFeature.thee(TheeBlocks.Soort.GUHBLOEMENTHEE)).is(KnusTags.THEE), "tag guhs:knus/thee");
            // drinking a cup
            ItemStack cup = new ItemStack(TheehuisFeature.thee(TheeBlocks.Soort.GUHBLOEMENTHEE));
            cup.finishUsingItem(level, p);
            helper.assertTrue(p.hasEffect(TheehuisFeature.GEZELLIG), "guhbloementhee: gezellig");
            helper.assertTrue(KnusVoortgang.ontdekt(p, TheehuisVoortgang.THEESOORTEN).size() == 2, "two teas known");
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    @GuhTest(template = KAMER)
    public static void theehuisGezelligTheekransje(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(10, 2, 10));
        GuhNpcEntity npc = theelepel(helper);
        GuhEntity a = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 2, 12));
        GuhEntity b = helper.spawn(ModEntities.GUH.get(), new BlockPos(3, 2, 12));
        Theekransje k = Theekransje.of(npc);
        try {
            ServerLevel level = helper.getLevel();
            a.tame(p);
            b.tame(p);
            Knusfeest.nieuweRonde(p, 0, EnumSet.of(Feesttaak.THEESERVIES));
            helper.assertTrue(Features.role(GuhNpcEntity.Kind.THEEGUH) == TheehuisFeature.THEELEPEL, "Mevrouw Theelepel's role");
            helper.assertTrue(TheehuisFeature.THEELEPEL.offers(npc) == null, "no shop in the tea house");
            TheehuisFeature.THEELEPEL.talk(npc, p);
            helper.assertTrue(k.start(npc, p), "a theekransje starts");
            helper.assertTrue(k.tafels.size() == 4 && k.stoelen.size() == 8 && k.gasten.size() == 2, "4 tables, 8 chairs, 2 guests");
            helper.assertTrue(Minigames.THEEHUIS.equals(Minigames.playing(p)), "a kransje counts as a game");
            helper.assertTrue(GuhQuests.count(p, TheehuisFeature.thee(TheeBlocks.Soort.KNABBELTHEE)) == Theekransje.HUISTHEE, "tea of the house");
            helper.assertTrue(level.getBlockState(helper.absolutePos(new BlockPos(6, 2, 6))).getValue(TheeBlocks.Theetafel.GEDEKT), "the table is laid");
            for (int i = 0; i <= Theekransje.LOOP_TICKS; i++) {
                k.tick(npc);
            }
            helper.assertTrue(k.gasten.stream().allMatch(g -> g.zit), "everybody sits down");
            helper.assertTrue(a.isOrderedToSit() && GuhHooks.isBezig(a), "on a chair, busy with the kransje");
            Theekransje.Gast ga = k.gast(a), gb = k.gast(b);
            // tea for a
            ga.wens = Theekransje.Wens.THEE;
            int voor = k.gezelligheid();
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get()));
            Theekransje.klikOpGuh(a, p, InteractionHand.MAIN_HAND);
            helper.assertTrue(k.gezelligheid() == voor && ga.wens == Theekransje.Wens.THEE, "kaasknabbels aren't tea");
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TheehuisFeature.thee(TheeBlocks.Soort.KNABBELTHEE), 2));
            Theekransje.klikOpGuh(a, p, InteractionHand.MAIN_HAND);
            helper.assertTrue(k.gezelligheid() == voor + Theekransje.PUNT_THEE && ga.wens == Theekransje.Wens.GEEN
                    && p.getMainHandItem().getCount() == 1, "poured: +" + Theekransje.PUNT_THEE);
            // cake for b
            gb.wens = Theekransje.Wens.GEBAK;
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.COOKIE, 1));
            Theekransje.klikOpGuh(b, p, InteractionHand.MAIN_HAND);
            helper.assertTrue(k.gezelligheid() == voor + Theekransje.PUNT_THEE + Theekransje.PUNT_GEBAK && p.getMainHandItem().isEmpty(), "a cookie");
            helper.assertTrue(KnusVoortgang.teller(p, TheehuisVoortgang.INGESCHONKEN) == 1, "one cup poured");
            // almost there: one more cup makes the table gezellig
            Theekransje.gezelligheidVoorTest(k, Theekransje.DOEL - 5);
            ga.wens = Theekransje.Wens.THEE;
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(TheehuisFeature.thee(TheeBlocks.Soort.KAASMELKTHEE), 1));
            Theekransje.klikOpGuh(a, p, InteractionHand.MAIN_HAND);
            helper.assertTrue(!k.isBezig() && !Theekransje.isGastheer(p), "gezellig: the kransje is over");
            helper.assertTrue(p.hasEffect(TheehuisFeature.GEZELLIG) && a.hasEffect(TheehuisFeature.GEZELLIG), "everybody gezellig");
            helper.assertTrue(GuhQuests.count(p, ModItems.clothingItem(GuhClothes.THEEMUTSJE)) == 1, "a theemutsje the first time");
            helper.assertTrue(GuhQuests.count(p, TheehuisFeature.FEEST_THEESERVIES.get()) == 1
                    && Knusfeest.stap(p, Feesttaak.THEESERVIES) == Knusfeest.Stap.GEMAAKT, "the feest-theeservies for the Knusfeest");
            helper.assertTrue(new ItemStack(TheehuisFeature.FEEST_THEESERVIES.get()).is(KnusTags.THEESERVIES), "tag guhs:knus/theeservies");
            helper.assertTrue(KnusVoortgang.teller(p, TheehuisVoortgang.KRANSJES) == 1, "Knus counter");
            helper.assertTrue(!a.isOrderedToSit() && !b.isOrderedToSit() && !GuhHooks.isBezig(a), "everybody stands up again");
            helper.assertTrue(!level.getBlockState(helper.absolutePos(new BlockPos(6, 2, 6))).getValue(TheeBlocks.Theetafel.GEDEKT), "cleared");
        } finally {
            if (k.isBezig()) {
                k.klaar(helper.getLevel(), false);
            }
            Theekransje.vergeet(npc);
            Knusfeest.vergeet(p);
            npc.discard();
            a.discard();
            b.discard();
            leave(helper, p);
        }
        helper.succeed();
    }

    @GuhTest(template = KAMER)
    public static void theehuisGeenGuhsGeenKransje(GameTestHelper helper) {
        ServerPlayer p = player(helper, new BlockPos(10, 2, 10));
        GuhNpcEntity npc = theelepel(helper);
        try {
            helper.assertTrue(!Theekransje.of(npc).start(npc, p) && !Theekransje.isGastheer(p), "no tamed guhs: no kransje");
        } finally {
            Theekransje.vergeet(npc);
            npc.discard();
            leave(helper, p);
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void theehuisPunten(GameTestHelper helper) {
        helper.assertTrue(Theekransje.punten(true, false) == Theekransje.PUNT_THEE && Theekransje.punten(true, true) == Theekransje.PUNT_BIJZONDER
                && Theekransje.punten(false, false) == Theekransje.PUNT_GEBAK && Theekransje.punten(false, true) == Theekransje.PUNT_ZELFGEBAKKEN,
                "points");
        helper.assertTrue(Theekransje.PUNT_ZELFGEBAKKEN > Theekransje.PUNT_GEBAK && Theekransje.PUNT_BIJZONDER > Theekransje.PUNT_THEE, "special counts more");
        helper.assertTrue(Theekransje.isGebak(new ItemStack(Items.COOKIE)) && !Theekransje.isGebak(new ItemStack(Items.PAPER)), "sweets");
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void theehuisGebouw(GameTestHelper helper) {
        StructureTemplate t = helper.getLevel().getStructureManager().get(Guhs.id("knuffeldal_stadje/theehuis")).orElse(null);
        helper.assertTrue(t != null, "the template exists");
        helper.assertTrue(t.getSize().getX() == 31 && t.getSize().getZ() == 31 && t.getSize().getY() >= 16 && t.getSize().getY() <= 48,
                "size " + t.getSize());
        var settings = new StructurePlaceSettings();
        var jigsaws = t.filterBlocks(BlockPos.ZERO, settings, Blocks.JIGSAW);
        helper.assertTrue(jigsaws.size() == 1 && jigsaws.get(0).pos().equals(new BlockPos(15, 4, 30)), "one jigsaw at (15, 4, 30)");
        helper.assertTrue(t.filterBlocks(BlockPos.ZERO, settings, TheehuisFeature.THEETAFEL.get()).size() >= 4, "tea tables");
        helper.assertTrue(t.filterBlocks(BlockPos.ZERO, settings, nl.juiced.guhs.registry.ModBlocks.GUH_STOEL.get()).size() >= 6, "chairs");
        helper.assertTrue(t.filterBlocks(BlockPos.ZERO, settings, Blocks.CAMPFIRE).size() >= 1, "the steaming spout");
        helper.succeed();
    }
}
