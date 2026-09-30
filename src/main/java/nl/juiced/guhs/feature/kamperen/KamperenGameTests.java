package nl.juiced.guhs.feature.kamperen;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Binnenkort;
import nl.juiced.guhs.feature.Features;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;

/**
 * Game tests of the kampeerplekjes: a whole campfire story (every line, the verhalenbundel, kaasknabbels +1, one per
 * night, the guhs come and listen), pyjamas by a burning campfire at night, the slaapzak (a bed that isn't a home,
 * uitgerust), Opa Guh's role and shop and his campfire, the template.
 */
public class KamperenGameTests {
    private static final String EMPTY = "empty";

    @SuppressWarnings("removal")
    private static ServerPlayer player(GameTestHelper helper) {
        ServerPlayer p = helper.makeMockServerPlayerInLevel();
        p.setGameMode(GameType.SURVIVAL);
        p.getInventory().clearContent();
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return p;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
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

    private static GuhNpcEntity opa(GameTestHelper helper, BlockPos rel) {
        GuhNpcEntity npc = helper.spawn(ModEntities.GUH_NPC.get(), rel);
        npc.setKind(GuhNpcEntity.Kind.OPA_GUH);
        return npc;
    }

    /**
     * A whole story (sped up): every line, then the verhalenbundel and kaasknabbels; the guh nearby listens; one story
     * per night (also who heard one tonight can't join another); who joins too late listens along but gets nothing.
     */
    @GuhTest(template = EMPTY, timeoutTicks = 400)
    public static void kamperenVerhaal(GameTestHelper helper) {
        ServerPlayer p = player(helper);
        ServerPlayer gehoord = player(helper);
        ServerPlayer laat = player(helper);
        Verhalen.klaar(gehoord, null, 5, 0);             // already heard a story tonight
        int gehoordKnabbels = count(gehoord, ModItems.KAAS_KNABBELS.get());
        GuhNpcEntity opa = opa(helper, new BlockPos(2, 1, 2));
        helper.setBlock(new BlockPos(2, 1, 4), Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(5, 1, 5));
        helper.assertTrue(Verhalen.volgende(p) == 0 && !Verhalen.vannachtGehoord(p), "a new listener: the first story");
        OpaGuh.praat(opa, p, false);
        helper.assertTrue(!Verhalen.vertelt(opa), "in the daytime: a tip, no story");
        OpaGuh.praat(opa, p, true);
        helper.assertTrue(Verhalen.vertelt(opa) && Verhalen.luistert(p) && Verhalen.verhaal(opa) == 0, "in the evening: a story");
        helper.assertTrue(Verhalen.vannachtGehoord(p), "tonight's story");
        OpaGuh.praat(opa, gehoord, true);
        helper.assertTrue(!Verhalen.luistert(gehoord), "heard one tonight already: can't join this one");
        int laatVanaf = Verhalen.REGELS - Verhalen.GENOEG + 1;   // from this line on, less than half is left
        boolean[] geluisterd = {false};
        boolean[] aangeschoven = {false};
        helper.onEachTick(() -> {
            int regel = Verhalen.regel(opa);
            if (!aangeschoven[0] && regel >= laatVanaf && Verhalen.vertelt(opa)) {
                OpaGuh.praat(opa, laat, true);
                aangeschoven[0] = true;
                helper.assertTrue(Verhalen.luistert(laat) && !Verhalen.vannachtGehoord(laat), "too late: listens along, not tonight's story");
            }
            if (aangeschoven[0] || regel < laatVanaf) {
                Verhalen.nu(opa);                     // (no waiting between the lines)
            }
            geluisterd[0] |= LuisterGoal.luistert(guh) && GuhHooks.isBezig(guh);
        });
        helper.succeedWhen(() -> {
            helper.assertTrue(!Verhalen.vertelt(opa), "still telling");
            helper.assertTrue(KnusVoortgang.heeft(p, Verhalen.BUNDEL, "eerste_kaasknabbel"), "in the verhalenbundel");
            helper.assertTrue(count(p, ModItems.KAAS_KNABBELS.get()) == Verhalen.knabbels(true), "kaasknabbels for a new story");
            helper.assertTrue(KnusVoortgang.teller(p, Verhalen.GEHOORD) == 1 && KnusVoortgang.teller(p, Verhalen.AANTAL) == 1, "counted");
            helper.assertTrue(geluisterd[0], "the guh came to listen");
            helper.assertTrue(aangeschoven[0] && count(laat, ModItems.KAAS_KNABBELS.get()) == 0 && KnusVoortgang.ontdekt(laat, Verhalen.BUNDEL).isEmpty()
                    && !Verhalen.vannachtGehoord(laat), "the late listener: only the end, no reward, may still hear a whole one tonight");
            helper.assertTrue(count(gehoord, ModItems.KAAS_KNABBELS.get()) == gehoordKnabbels
                    && !KnusVoortgang.heeft(gehoord, Verhalen.BUNDEL, "eerste_kaasknabbel"), "no second story tonight");
            OpaGuh.praat(opa, p, true);
            helper.assertTrue(!Verhalen.vertelt(opa), "one story per night");
            helper.assertTrue(Verhalen.volgende(p) == 1, "next night the next story");
            Verhalen.vergeetVannacht(p);
            leave(helper, p, gehoord, laat);
            opa.discard();
            guh.discard();
            helper.setBlock(new BlockPos(2, 1, 4), Blocks.AIR);
        });
    }

    /**
     * Marshmallows at the campfire (#guhs:knus/marshmallow; the tag is filled by another slice, so a stand-in item
     * goes through the roasting itself): one eaten, a bite of food, counted, the guh comes to the fire, Opa is there.
     */
    @GuhTest(template = EMPTY)
    public static void kamperenMarshmallow(GameTestHelper helper) {
        BlockPos vuur = new BlockPos(2, 1, 2);
        helper.setBlock(vuur, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        GuhNpcEntity opa = opa(helper, new BlockPos(4, 1, 2));
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(6, 1, 6));
        ServerPlayer p = player(helper);
        try {
            helper.assertTrue(!KampvuurMarshmallow.isMarshmallow(new ItemStack(Items.SUGAR)) && !KampvuurMarshmallow.isMarshmallow(ItemStack.EMPTY),
                    "only #guhs:knus/marshmallow items");
            p.getFoodData().setFoodLevel(10);
            ItemStack stand = new ItemStack(Items.SUGAR, 2);
            BlockPos abs = helper.absolutePos(vuur);
            helper.assertTrue(KampvuurMarshmallow.opa(helper.getLevel(), abs) == opa, "Opa Guh is at this fire");
            int n = KampvuurMarshmallow.rooster(p, abs, stand);
            helper.assertTrue(n == 1 && stand.getCount() == 1 && KnusVoortgang.teller(p, KampvuurMarshmallow.GEROOSTERD) == 1, "one roasted and eaten");
            helper.assertTrue(p.getFoodData().getFoodLevel() > 10 && p.getCooldowns().isOnCooldown(Items.SUGAR), "a bite of food, then wait a moment");
            helper.assertTrue(LuisterGoal.luistert(guh), "the guh smells it and comes to the fire");
            helper.assertTrue(count(p, ModItems.KAAS_KNABBELS.get()) == 0, "no kaasknabbels (no knabbel machine)");
            helper.assertTrue(KnusVoortgang.mijlpalen("kamperen").stream().anyMatch(m -> m.id().equals("kamperen_marshmallows")), "a milestone");
        } finally {
            leave(helper, p);
            opa.discard();
            guh.discard();
            helper.setBlock(vuur, Blocks.AIR);
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void kamperenVerhalenEnBeloning(GameTestHelper helper) {
        helper.assertTrue(Verhalen.IDS.size() == 12 && new HashSet<>(Verhalen.IDS).size() == 12, "twelve stories");
        var bundel = KnusVoortgang.verzameling(Verhalen.BUNDEL);
        helper.assertTrue(bundel != null && bundel.items().equals(Verhalen.IDS) && bundel.onderdeel().equals("kamperen"), "the verhalenbundel");
        helper.assertTrue(KnusVoortgang.mijlpalen("kamperen").size() >= 2, "milestones");
        helper.assertTrue(Verhalen.knabbels(false) == 1 + 1 && Verhalen.knabbels(true) == 3 + 1, "+1 on every reward");
        ServerPlayer p = player(helper);
        try {
            for (int i = 0; i < 12; i++) {
                helper.assertTrue(Verhalen.volgende(p) == i, "in order: " + i);
                Verhalen.klaar(p, null, i, i == 11 ? 5 : 0);
            }
            helper.assertTrue(KnusVoortgang.ontdekt(p, Verhalen.BUNDEL).size() == 12 && KnusVoortgang.teller(p, Verhalen.PYJAMAGUHS) == 5,
                    "the whole bundel, and a pyjama party");
            int v = Verhalen.volgende(p);
            helper.assertTrue(v >= 0 && v < 12, "all heard: any one again");
        } finally {
            leave(helper, p);
        }
        helper.succeed();
    }

    /** Pyjamas: at night, by a burning campfire; not in the daytime, not far off, not by a campfire that's out. */
    @GuhTest(template = EMPTY)
    public static void kamperenPyjama(GameTestHelper helper) {
        BlockPos vuur = new BlockPos(2, 1, 2);
        helper.setBlock(vuur, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(4, 1, 2));
        helper.assertTrue(KampvuurPyjama.werk(guh, true) && GuhHooks.heeft(guh, GuhHooks.PYJAMA), "night by the fire: pyjama on");
        helper.assertTrue(!KampvuurPyjama.werk(guh, false) && !GuhHooks.heeft(guh, GuhHooks.PYJAMA), "daytime: off");
        helper.setBlock(vuur, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        helper.assertTrue(!KampvuurPyjama.werk(guh, true), "a campfire that's out: no pyjama");
        helper.setBlock(vuur, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, true));
        KampvuurPyjama.TEST_NACHT.add(guh.getUUID());
        try {
            helper.assertTrue(KampvuurPyjama.isNacht(guh), "(test night)");
            guh.teleportTo(guh.getX() + 20, guh.getY(), guh.getZ());
            helper.assertTrue(!KampvuurPyjama.werk(guh, true), "far from the fire: no pyjama");
        } finally {
            KampvuurPyjama.TEST_NACHT.remove(guh.getUUID());
        }
        // Opa lights his campfire again when it's out
        helper.setBlock(vuur, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false));
        helper.assertTrue(OpaGuh.steekAan(helper.getLevel(), helper.absolutePos(new BlockPos(3, 1, 3)))
                && helper.getBlockState(vuur).getValue(CampfireBlock.LIT), "Opa lights the campfire");
        helper.setBlock(vuur, Blocks.AIR);
        guh.discard();
        helper.succeed();
    }

    /** The slaapzak: a bed (occupied, a direction), but not a home; a whole night in it: uitgerust. */
    @GuhTest(template = EMPTY)
    public static void kamperenSlaapzak(GameTestHelper helper) {
        BlockPos rel = new BlockPos(2, 1, 2);
        helper.setBlock(rel, KamperenFeature.SLAAPZAK.get().defaultBlockState().setValue(SlaapzakBlock.FACING, Direction.EAST));
        BlockPos abs = helper.absolutePos(rel);
        var level = helper.getLevel();
        var state = level.getBlockState(abs);
        ServerPlayer p = player(helper);
        try {
            helper.assertTrue(state.isBed(level, abs, p) && state.getBedDirection(level, abs) == Direction.EAST, "a bed with a direction");
            state.setBedOccupied(level, abs, p, true);
            helper.assertTrue(level.getBlockState(abs).getValue(SlaapzakBlock.OCCUPIED), "occupied");
            level.getBlockState(abs).setBedOccupied(level, abs, p, false);
            helper.assertTrue(!level.getBlockState(abs).getValue(SlaapzakBlock.OCCUPIED), "free again");
            var voor = p.getRespawnPosition();
            p.setRespawnPosition(level.dimension(), abs, 0, false, false);
            helper.assertTrue(java.util.Objects.equals(p.getRespawnPosition(), voor), "it doesn't become your spawn point");
            KamperenEvents.uitgeslapen(p);
            helper.assertTrue(p.hasEffect(KamperenFeature.UITGERUST) && KnusVoortgang.teller(p, Verhalen.UITGESLAPEN) == 1, "uitgerust");
        } finally {
            leave(helper, p);
        }
        helper.setBlock(rel, Blocks.AIR);
        helper.succeed();
    }

    /** The kampeerplekje (and the festival and the sterrenwacht, the same code) can't be broken or built in; creative players may. */
    @GuhTest(template = EMPTY)
    public static void kamperenBeschermd(GameTestHelper helper) {
        BlockPos rel = new BlockPos(2, 1, 2);
        helper.setBlock(rel, Blocks.OAK_PLANKS);
        BlockPos abs = helper.absolutePos(rel);
        var box = new net.minecraft.world.level.levelgen.structure.BoundingBox(abs.getX() - 2, abs.getY() - 1, abs.getZ() - 2,
                abs.getX() + 2, abs.getY() + 3, abs.getZ() + 2);
        ServerPlayer p = player(helper);
        try {
            for (var b : java.util.List.of(KamperenFeature.BESCHERMING, nl.juiced.guhs.feature.ballon.BallonFeature.BESCHERMING,
                    nl.juiced.guhs.feature.sterrenwacht.SterrenwachtFeature.BESCHERMING)) {
                b.testGebied(box);
                helper.assertTrue(b.in(helper.getLevel(), abs) && nl.juiced.guhs.feature.Protected.at(helper.getLevel(), abs), "protected");
                p.gameMode.destroyBlock(abs);
                helper.assertTrue(helper.getBlockState(rel).is(Blocks.OAK_PLANKS), "a survival player can't break it");
                b.testWissen();
                helper.assertTrue(!b.in(helper.getLevel(), abs), "only there");
            }
            p.setGameMode(GameType.CREATIVE);
            KamperenFeature.BESCHERMING.testGebied(box);
            p.gameMode.destroyBlock(abs);
            helper.assertTrue(helper.getBlockState(rel).isAir(), "creative may");
        } finally {
            KamperenFeature.BESCHERMING.testWissen();
            leave(helper, p);
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void kamperenOpaEnWinkel(GameTestHelper helper) {
        helper.assertTrue(Features.role(GuhNpcEntity.Kind.OPA_GUH) == OpaGuh.INSTANCE && OpaGuh.INSTANCE != Binnenkort.ROLE, "Opa Guh's own role");
        helper.assertTrue(GuhDex.ENTRIES.contains(GuhVariant.OPA_GUH), "his Guhdex page");
        GuhNpcEntity npc = opa(helper, new BlockPos(1, 1, 1));
        Set<Item> koop = new HashSet<>();
        for (MerchantOffer o : OpaGuh.INSTANCE.offers(npc)) {
            helper.assertTrue(o.getBaseCostA().is(ModItems.KAAS_KNABBELS.get()), "paid with kaasknabbels");
            koop.add(o.getResult().getItem());
        }
        helper.assertTrue(koop.contains(KamperenFeature.SLAAPZAK_ITEM.get()) && koop.contains(ModItems.clothingItem(GuhClothes.SLAAPMUTSJE))
                && koop.contains(ModItems.clothingItem(GuhClothes.PYJAMA_PAKJE)), "the shop");
        npc.discard();
        var t = helper.getLevel().getStructureManager().get(Guhs.id("kampeerplekje"));
        helper.assertTrue(t.isPresent() && t.get().getSize().getX() <= 25 && t.get().getSize().getZ() <= 25 && t.get().getSize().getY() <= 16,
                "the campsite template (25 x 16 x 25 at most)");
        var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
        helper.assertTrue(t.get().filterBlocks(BlockPos.ZERO, settings, Blocks.CAMPFIRE, true).size() == 1, "a campfire");
        helper.assertTrue(t.get().filterBlocks(BlockPos.ZERO, settings, KamperenFeature.SLAAPZAK.get(), true).size() >= 6, "sleeping bags");
        helper.succeed();
    }
}
