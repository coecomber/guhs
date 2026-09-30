package nl.juiced.guhs.feature.sterrenwacht;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Binnenkort;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.knus.Feesttaak;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.Knusfeest;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the Guh-Sterrenwacht: the constellations, connecting one (wrong and right lines, the atlas, the
 * wenssterren: every reward one more), the rare ones only during a sterrenregen, the Knusfeest's sterrenlantaarns,
 * wishes, Professor Sterretje's role and shop, the blocks and the template.
 */
public class SterrenwachtGameTests {
    private static final String EMPTY = "empty";

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer p = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return p;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            Sterrenkijken.stop(p);
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    private static int count(ServerPlayer p, Item item) {
        int n = 0;
        for (ItemStack s : p.getInventory().getNonEquipmentItems()) {
            if (s.is(item)) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static boolean advancement(ServerPlayer p, String name) {
        var holder = p.level().getServer().getAdvancements().get(Guhs.id(name));
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    private static List<Integer> lijnen(Sterrenbeeld b) {
        List<Integer> out = new ArrayList<>();
        for (int[] l : b.lijnen()) {
            out.add(l[1]);          // (the other way round: the order of a line doesn't matter)
            out.add(l[0]);
        }
        return out;
    }

    @GuhTest(template = EMPTY)
    public static void sterrenwachtSterrenbeelden(GameTestHelper helper) {
        helper.assertTrue(Sterrenbeeld.values().length == 15 && Sterrenbeeld.gewoon().size() == 12 && Sterrenbeeld.zeldzame().size() == 3,
                "12 + 3 constellations");
        Set<String> ids = new HashSet<>();
        for (Sterrenbeeld b : Sterrenbeeld.values()) {
            helper.assertTrue(ids.add(b.id()) && Sterrenbeeld.byId(b.id()) == b, "unique id " + b);
            helper.assertTrue(b.sterren() >= 5 && b.lijnen().size() >= 5, b + " has enough stars and lines");
            Set<Integer> gebruikt = new HashSet<>();
            for (int[] l : b.lijnen()) {
                helper.assertTrue(l[0] >= 0 && l[1] < b.sterren() && l[0] != l[1], b + ": a line between two of its stars");
                gebruikt.add(l[0]);
                gebruikt.add(l[1]);
                float[] s = b.ster(l[0]);
                helper.assertTrue(s[0] >= 0 && s[0] <= 1 && s[1] >= 0 && s[1] <= 1, b + ": stars in 0..1");
            }
            helper.assertTrue(gebruikt.size() == b.sterren(), b + ": every star is on a line");
            helper.assertTrue(b.sleutels().size() == b.lijnen().size(), b + ": no line twice");
        }
        var atlas = KnusVoortgang.verzameling(Sterrenkijken.VERZAMELING);
        helper.assertTrue(atlas != null && atlas.items().equals(Sterrenbeeld.ids()) && atlas.onderdeel().equals("sterrenwacht"), "the sterrenatlas: 15 entries");
        helper.assertTrue(KnusVoortgang.mijlpalen("sterrenwacht").size() >= 3, "at least 3 milestones");
        helper.succeed();
    }

    /** Wrong lines don't count; the right ones do (in any order): the atlas, wenssterren (+1), the counters, the game ends. */
    @GuhTest(template = EMPTY)
    public static void sterrenwachtVerbinden(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        try {
            BlockPos tele = helper.absolutePos(new BlockPos(2, 1, 2));
            Sterrenkijken.start(p, tele, Sterrenbeeld.GROTE_KNABBEL);
            helper.assertTrue(Minigames.STERRENWACHT.equals(Minigames.playing(p)), "looking through the telescope is a game");
            helper.assertTrue(Sterrenkijken.klaar(p, Sterrenbeeld.GROTE_KNABBEL.ordinal(), List.of(0, 1, 1, 2)) == -1, "not all lines: nothing");
            helper.assertTrue(!Sterrenkijken.kijkt(p) && Minigames.playing(p) == null, "a 'no' ends the look (the screen has closed)");
            Sterrenkijken.start(p, tele, Sterrenbeeld.GROTE_KNABBEL);
            helper.assertTrue(Sterrenkijken.klaar(p, Sterrenbeeld.GROTE_KNABBEL.ordinal(), List.of(0, 3, 0, 1, 1, 2, 2, 0, 3, 4, 4, 5)) == -1,
                    "a wrong line: nothing");
            Sterrenkijken.start(p, tele, Sterrenbeeld.GROTE_KNABBEL);
            helper.assertTrue(Sterrenkijken.klaar(p, Sterrenbeeld.KLEINE_VADS.ordinal(), lijnen(Sterrenbeeld.KLEINE_VADS)) == -1,
                    "another constellation than the sky shows: nothing");
            helper.assertTrue(Sterrenkijken.klaar(p, Sterrenbeeld.GROTE_KNABBEL.ordinal(), lijnen(Sterrenbeeld.GROTE_KNABBEL)) == -1
                    && count(p, SterrenwachtFeature.WENSSTER.get()) == 0, "no look (e.g. it took too long): nothing");
            Sterrenkijken.start(p, tele, Sterrenbeeld.GROTE_KNABBEL);
            helper.assertTrue(Sterrenkijken.kijkt(p), "looking again");
            int n = Sterrenkijken.klaar(p, Sterrenbeeld.GROTE_KNABBEL.ordinal(), lijnen(Sterrenbeeld.GROTE_KNABBEL));
            helper.assertTrue(n == Sterrenkijken.wenssterren(true, false) && count(p, SterrenwachtFeature.WENSSTER.get()) == n, "new: " + n + " wenssterren");
            helper.assertTrue(KnusVoortgang.heeft(p, Sterrenkijken.VERZAMELING, "grote_knabbel"), "in the atlas");
            helper.assertTrue(KnusVoortgang.teller(p, Sterrenkijken.STERRENBEELDEN) == 1 && KnusVoortgang.teller(p, Sterrenkijken.ATLAS) == 1, "counters");
            helper.assertTrue(!Sterrenkijken.kijkt(p) && Minigames.playing(p) == null, "the look is over");
            Sterrenkijken.start(p, tele, Sterrenbeeld.GROTE_KNABBEL);
            int again = Sterrenkijken.klaar(p, Sterrenbeeld.GROTE_KNABBEL.ordinal(), lijnen(Sterrenbeeld.GROTE_KNABBEL));
            helper.assertTrue(again == Sterrenkijken.wenssterren(false, false) && again < n, "known: fewer wenssterren (" + again + ")");
            helper.assertTrue(Sterrenkijken.vannacht(p) == 2, "two tonight");
            // every reward rule is one more ("+1 per reward")
            helper.assertTrue(Sterrenkijken.wenssterren(false, false) == 1 + 1 && Sterrenkijken.wenssterren(true, false) == 2 + 1
                    && Sterrenkijken.wenssterren(true, true) == 4 + 1 && Sterrenkijken.wenssterren(false, true) == 3 + 1, "+1 on every reward");
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    /** The rare ones only during a sterrenregen (and then first); otherwise a new one first. */
    @GuhTest(template = EMPTY)
    public static void sterrenwachtZeldzaamAlleenBijSterrenregen(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        try {
            RandomSource r = RandomSource.create(28);
            for (int i = 0; i < 200; i++) {
                helper.assertTrue(!Sterrenkijken.kies(p, false, r).zeldzaam, "no rare one without a sterrenregen");
            }
            helper.assertTrue(Sterrenkijken.kies(p, true, r).zeldzaam, "a sterrenregen: a rare one first");
            for (Sterrenbeeld b : Sterrenbeeld.gewoon()) {
                if (b != Sterrenbeeld.KAMPVUUR) {
                    KnusVoortgang.ontdek(p, Sterrenkijken.VERZAMELING, b.id());
                }
            }
            for (int i = 0; i < 20; i++) {
                helper.assertTrue(Sterrenkijken.kies(p, false, r) == Sterrenbeeld.KAMPVUUR, "the last new one first");
            }
            KnusVoortgang.ontdek(p, Sterrenkijken.VERZAMELING, Sterrenbeeld.KAMPVUUR.id());
            for (int i = 0; i < 100; i++) {
                helper.assertTrue(!Sterrenkijken.kies(p, false, r).zeldzaam, "all known: still never a rare one without a sterrenregen");
            }
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    /** While the Knusfeest task "sterrenlantaarns" is open, a constellation gives three sterrenlantaarns (tag guhs:knus/sterrenlantaarns). */
    @GuhTest(template = EMPTY)
    public static void sterrenwachtKnusfeestLantaarns(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        try {
            Knusfeest.vergeet(p);
            Sterrenkijken.beloon(p, Sterrenbeeld.KNUFFELHART, null);
            helper.assertTrue(count(p, SterrenwachtFeature.STERRENLANTAARN_ITEM.get()) == 0, "no feest, no lanterns");
            Knusfeest.nieuweRonde(p, 0, EnumSet.of(Feesttaak.STERRENLANTAARNS));
            Sterrenkijken.beloon(p, Sterrenbeeld.KNUFFELHART, null);
            helper.assertTrue(count(p, SterrenwachtFeature.STERRENLANTAARN_ITEM.get()) == Sterrenkijken.LANTAARNS, "three lanterns for the feest");
            helper.assertTrue(Knusfeest.stap(p, Feesttaak.STERRENLANTAARNS) == Knusfeest.Stap.GEMAAKT, "the task is made");
            helper.assertTrue(new ItemStack(SterrenwachtFeature.STERRENLANTAARN_ITEM.get()).is(KnusTags.STERRENLANTAARNS), "the lantern is in the feest tag");
            Sterrenkijken.beloon(p, Sterrenbeeld.KNUFFELHART, null);
            helper.assertTrue(count(p, SterrenwachtFeature.STERRENLANTAARN_ITEM.get()) == Sterrenkijken.LANTAARNS, "not more while you carry them");
        } finally {
            Knusfeest.vergeet(p);
            leave(helper, p);
        }
        // lanterns from the recipe or a wish (made with wenssterren) while the task is asked also count as made
        ServerPlayer gemaakt = player(helper);
        ServerPlayer gekocht = player(helper);
        try {
            Knusfeest.nieuweRonde(gemaakt, 0, EnumSet.of(Feesttaak.STERRENLANTAARNS));
            Sterrenkijken.lantaarnGekregen(gemaakt);
            helper.assertTrue(Knusfeest.stap(gemaakt, Feesttaak.STERRENLANTAARNS) == Knusfeest.Stap.GEMAAKT
                    && advancement(gemaakt, "quest/knusfeest_sterrenlantaarns_gemaakt"), "crafted/wished lanterns: made");
            // bought ones handed in straight away at the Burgemeester: the skipped 'made' step still counts
            Knusfeest.nieuweRonde(gekocht, 0, EnumSet.of(Feesttaak.STERRENLANTAARNS));
            Knusfeest.zet(gekocht, Feesttaak.STERRENLANTAARNS, Knusfeest.Stap.GEBRACHT);
            helper.assertTrue(advancement(gekocht, "quest/knusfeest_sterrenlantaarns_gemaakt"), "delivered: the 'made' quest too");
        } finally {
            Knusfeest.vergeet(gemaakt);
            Knusfeest.vergeet(gekocht);
            leave(helper, gemaakt, gekocht);
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void sterrenwachtWensster(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        try {
            RandomSource r = RandomSource.create(7);
            for (int i = 0; i < 12; i++) {
                String wat = WenssterItem.wens(p, r);
                helper.assertTrue(wat.startsWith("gui.guhs.sterrenwacht.wens."), "a wish comes true: " + wat);
            }
            boolean iets = !p.getInventory().isEmpty() || !p.getActiveEffects().isEmpty();
            helper.assertTrue(iets && KnusVoortgang.teller(p, Sterrenkijken.WENSEN) == 12, "presents, and 12 wishes counted");
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void sterrenwachtProfessorEnWinkel(GameTestHelper helper) {
        helper.assertTrue(Features.role(GuhNpcEntity.Kind.STERRENKIJKERGUH) == SterrenwachtRole.INSTANCE
                && Features.role(GuhNpcEntity.Kind.STERRENKIJKERGUH) != Binnenkort.ROLE, "Professor Sterretje's own role");
        helper.assertTrue(GuhDex.ENTRIES.contains(GuhVariant.STERRENKIJKERGUH), "his Guhdex page");
        GuhNpcEntity npc = helper.spawn(nl.juiced.guhs.registry.ModEntities.GUH_NPC.get(), new BlockPos(3, 1, 3));
        npc.setKind(GuhNpcEntity.Kind.STERRENKIJKERGUH);
        var offers = SterrenwachtRole.INSTANCE.offers(npc);
        Set<Item> koop = new HashSet<>();
        for (MerchantOffer o : offers) {
            helper.assertTrue(o.getBaseCostA().is(SterrenwachtFeature.WENSSTER.get()), "paid with wenssterren");
            koop.add(o.getResult().getItem());
        }
        helper.assertTrue(koop.contains(ModItems.clothingItem(GuhClothes.STERRENKIJKERSMUTS)) && koop.contains(ModItems.clothingItem(GuhClothes.STERRENCAPE))
                && koop.contains(SterrenwachtFeature.TELESCOOP_ITEM.get()) && koop.contains(SterrenwachtFeature.STERRENLANTAARN_ITEM.get()), "the shop");
        ServerPlayer p = player(helper);
        try {
            SterrenwachtRole.INSTANCE.talk(npc, p);
            helper.assertTrue(KnusVoortgang.teller(p, Sterrenkijken.GEVONDEN) == 1, "talking to him counts");
        } finally {
            if (p.containerMenu != p.inventoryMenu) {
                p.closeContainer();
            }
            leave(helper, p);
        }
        npc.discard();
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void sterrenwachtBlokkenEnGebouw(GameTestHelper helper) {
        BlockPos at = new BlockPos(2, 1, 2);
        helper.setBlock(at, SterrenwachtFeature.STERRENLANTAARN.get());
        helper.assertTrue(helper.getBlockState(at).getLightEmission() == 15, "a sterrenlantaarn shines");
        helper.setBlock(at.east(), SterrenwachtFeature.TELESCOOP.get().defaultBlockState().setValue(TelescoopBlock.FACING, net.minecraft.core.Direction.EAST));
        helper.assertTrue(helper.getBlockState(at.east()).getValue(TelescoopBlock.FACING) == net.minecraft.core.Direction.EAST, "a telescope faces a way");
        helper.setBlock(at, Blocks.AIR);
        helper.setBlock(at.east(), Blocks.AIR);
        var t = helper.getLevel().getStructureManager().get(Guhs.id("guh_sterrenwacht"));
        helper.assertTrue(t.isPresent() && t.get().getSize().getX() == 48 && t.get().getSize().getZ() == 48 && t.get().getSize().getY() <= 72,
                "the observatory template (48 x H x 48)");
        var jigsaws = t.get().filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                Blocks.JIGSAW, true);
        helper.assertTrue(jigsaws.size() == 1 && jigsaws.get(0).nbt().getStringOr("name", "").equals("guhs:guh_sterrenwacht_midden"), "its anchor");
        var telescopen = t.get().filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                SterrenwachtFeature.TELESCOOP.get(), true);
        helper.assertTrue(telescopen.size() >= 5, "telescopes in it: " + telescopen.size());
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.STRUCTURE_TYPE.getValue(Guhs.id("sterrenwacht_hoog")) == SterrenwachtFeature.HOGE_JIGSAW.get(),
                "the 'only high up' structure type");
        helper.succeed();
    }
}
