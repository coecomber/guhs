package nl.juiced.guhs.feature.barbecuether;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.ModDimensions;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Game tests of the Barbecuether: the grillkool portal (frame sizes, lighting, going out, 1:8 linking), the frying
 * sauce (burns, glows, cools into grillkool / houtskoolsteen, kaassaus bakes coal), the blocks, the Grillguh's
 * questline and shop, the Mika drop, the pit templates and the worldgen data.
 * <p>
 * The GameTest server has no datapack dimensions: {@link #portalLinksBothWays} then only checks the maths; run
 * {@code /test runall} in a dev server (runServer) to also go through a real portal pair.
 */
public class BarbecuetherGameTests {
    private static final String EMPTY = "empty";
    private static final String ROOM = "bbq_testkamer";

    private static ServerPlayer player(GameTestHelper helper) {
        @SuppressWarnings("removal")
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        return player;
    }

    private static void remove(GameTestHelper helper, ServerPlayer player) {
        helper.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
    }

    /** A grillkool frame along x: inside `width` x `height`, bottom-left corner of the frame at `corner` (relative). */
    private static void frame(GameTestHelper helper, BlockPos corner, int width, int height, boolean corners) {
        BlockState kool = BarbecuetherFeature.GRILLKOOL.get().defaultBlockState();
        for (int i = 0; i < width + 2; i++) {
            for (int j = 0; j < height + 2; j++) {
                boolean edge = i == 0 || j == 0 || i == width + 1 || j == height + 1;
                boolean isCorner = (i == 0 || i == width + 1) && (j == 0 || j == height + 1);
                helper.setBlock(corner.offset(i, j, 0), edge && (corners || !isCorner) ? kool : Blocks.AIR.defaultBlockState());
            }
        }
    }

    /** A stone floor under the test room (y 0). */
    private static void floor(GameTestHelper helper) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** Frames from 4x5 to 23x23 (corners optional) light up; too small or too big ones don't; a portal goes out when the frame breaks. */
    @GuhTest(template = ROOM, timeoutTicks = 100)
    public static void grillkoolFramesLikeANetherPortal(GameTestHelper helper) {
        floor(helper);
        frame(helper, new BlockPos(1, 1, 4), 2, 3, true);
        var shape = GrillPortalShape.findEmptyShape(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 4)));
        helper.assertTrue(shape.isPresent() && shape.get().width() == 2 && shape.get().height() == 3, "a 4x5 frame is a portal frame");
        shape.get().createPortalBlocks();
        helper.assertBlockPresent(BarbecuetherFeature.BARBECUETHER_PORTAAL.get(), new BlockPos(3, 4, 4));
        helper.assertTrue(GrillPortalShape.findEmptyShape(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 4))).isEmpty(),
                "a lit frame is not an empty frame any more");
        // a frame without corners works too
        frame(helper, new BlockPos(1, 9, 2), 3, 4, false);
        helper.assertTrue(GrillPortalShape.findEmptyShape(helper.getLevel(), helper.absolutePos(new BlockPos(2, 10, 2))).isPresent(),
                "corners are optional");
        // too narrow: inside 1 wide
        frame(helper, new BlockPos(5, 1, 7), 1, 3, true);
        helper.assertTrue(GrillPortalShape.findEmptyShape(helper.getLevel(), helper.absolutePos(new BlockPos(6, 2, 7))).isEmpty(),
                "a frame 1 wide inside is too small");
        // break the frame: the fire goes out
        helper.setBlock(new BlockPos(0 + 1, 3, 4), Blocks.AIR);
        helper.succeedWhen(() -> helper.assertBlockNotPresent(BarbecuetherFeature.BARBECUETHER_PORTAAL.get(), new BlockPos(2, 2, 4)));
    }

    /** The biggest frame (23x23 with the frame) is fine, one bigger isn't. */
    @GuhTest(template = EMPTY)
    public static void grillkoolFramesUpTo23(GameTestHelper helper) {
        helper.assertTrue(GrillPortalShape.MAX_SIZE == 21 && GrillPortalShape.MIN_WIDTH == 2 && GrillPortalShape.MIN_HEIGHT == 3,
                "inside 2x3 to 21x21, like vanilla (frame 4x5 to 23x23)");
        helper.succeed();
    }

    /** The Aanmaakblokje only opens the barbecue portal in the Guhmensie and the Barbecuether; elsewhere it's a lighter. */
    @GuhTest(template = ROOM, timeoutTicks = 60)
    public static void aanmaakblokjeOnlyInTheGuhmensie(GameTestHelper helper) {
        floor(helper);
        helper.assertTrue(GrillPortalForcer.targetDimension(Level.OVERWORLD) == null, "no barbecue portal in the overworld");
        helper.assertTrue(GrillPortalForcer.targetDimension(Level.NETHER) == null, "nor in the Nether");
        helper.assertTrue(GrillPortalForcer.targetDimension(ModDimensions.GUHMENSION) == BarbecuetherFeature.BARBECUETHER, "Guhmensie -> Barbecuether");
        helper.assertTrue(GrillPortalForcer.targetDimension(BarbecuetherFeature.BARBECUETHER) == ModDimensions.GUHMENSION, "and back");
        ServerPlayer player = player(helper);
        frame(helper, new BlockPos(1, 1, 4), 2, 3, true);
        ItemStack blokje = new ItemStack(BarbecuetherFeature.AANMAAKBLOKJE.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, blokje);
        BlockPos bottom = helper.absolutePos(new BlockPos(2, 1, 4));
        var hit = new BlockHitResult(Vec3.atCenterOf(bottom), Direction.UP, bottom, false);
        blokje.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertBlockNotPresent(BarbecuetherFeature.BARBECUETHER_PORTAAL.get(), new BlockPos(2, 2, 4));
        helper.assertBlockNotPresent(Blocks.FIRE, new BlockPos(2, 2, 4));
        // on a plain block it is just a lighter
        helper.setBlock(new BlockPos(6, 1, 1), Blocks.STONE);
        BlockPos stone = helper.absolutePos(new BlockPos(6, 1, 1));
        blokje.getItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(stone), Direction.UP, stone, false)));
        helper.assertBlockPresent(Blocks.FIRE, new BlockPos(6, 2, 1));
        helper.setBlock(new BlockPos(6, 2, 1), Blocks.AIR);
        remove(helper, player);
        helper.succeed();
    }

    /** 1:8 like the Nether; with the dimensions there (dev server) a real portal pair is made and linked both ways. */
    @GuhTest(template = EMPTY, timeoutTicks = 1200)
    public static void portalLinksBothWays(GameTestHelper helper) {
        try {
            portalLinks(helper);
        } catch (RuntimeException e) {
            // (in a dev server the result only goes to the chat: log it too)
            org.slf4j.LoggerFactory.getLogger("guhs").warn("portalLinksBothWays: {}", e.getMessage());
            throw e;
        }
    }

    private static void portalLinks(GameTestHelper helper) {
        var types = helper.getLevel().registryAccess().lookupOrThrow(Registries.DIMENSION_TYPE);
        var bbq = types.getValue(Guhs.id("barbecuether"));
        var guh = types.getValue(Guhs.id("guhmension"));
        helper.assertTrue(bbq != null && guh != null, "both dimension types are there");
        // (1.1.0: "hot" = the Nether's fast lava / water evaporates attributes, "no beds" = the bed rule attribute)
        helper.assertTrue(bbq.coordinateScale() == 8.0 && bbq.hasCeiling()
                && bbq.attributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.FAST_LAVA, false)
                && bbq.attributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.WATER_EVAPORATES, false)
                && !bbq.hasSkyLight() && bbq.height() == 128
                && bbq.attributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.BED_RULE,
                        net.minecraft.world.attribute.BedRule.CAN_SLEEP_WHEN_DARK).explodes(),
                "the Barbecuether is a Nether: scale 8, a ceiling, hot, 128 high, no beds");
        var border = helper.getLevel().getWorldBorder();
        BlockPos in = GrillPortalForcer.scaledTarget(guh, bbq, border, 800.5, 70, -1608.5);
        helper.assertTrue(in.getX() == 100 && in.getZ() == -202, "8 blocks in the Guhmensie are 1 in the Barbecuether: " + in);
        BlockPos out = GrillPortalForcer.scaledTarget(bbq, guh, border, 100.5, 40, -201.5);
        helper.assertTrue(out.getX() == 804 && out.getZ() == -1612, "and back times 8: " + out);

        ServerLevel guhmensie = helper.getLevel().getServer().getLevel(ModDimensions.GUHMENSION);
        ServerLevel ether = helper.getLevel().getServer().getLevel(BarbecuetherFeature.BARBECUETHER);
        if (guhmensie == null || ether == null) {
            helper.succeed();      // (the GameTest server has no datapack dimensions: see the dev server run)
            org.slf4j.LoggerFactory.getLogger("guhs").info("portalLinksBothWays: maths only (no Guhmensie here)");
            return;
        }
        // a lit frame far out in the Guhmensie
        BlockPos base = new BlockPos(40000, 0, 40000);
        base = base.atY(guhmensie.getChunk(base).getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 0, 0) + 1);
        for (int i = -1; i <= 2; i++) {
            for (int j = -1; j <= 3; j++) {
                boolean edge = i == -1 || i == 2 || j == -1 || j == 3;
                guhmensie.setBlockAndUpdate(base.offset(i, j, 0), edge ? BarbecuetherFeature.GRILLKOOL.get().defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
        }
        var shape = GrillPortalShape.findEmptyShape(guhmensie, base);
        helper.assertTrue(shape.isPresent(), "the frame in the Guhmensie");
        shape.get().createPortalBlocks();
        ArmorStand walker = new ArmorStand(EntityType.ARMOR_STAND, guhmensie);
        walker.snapTo(base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
        var there = GrillPortalForcer.getDestination(guhmensie, walker, base);
        helper.assertTrue(there != null && there.newLevel() == ether, "it leads to the Barbecuether");
        BlockPos arrive = BlockPos.containing(there.position());
        helper.assertTrue(Math.abs(arrive.getX() - 5000) <= 20 && Math.abs(arrive.getZ() - 5000) <= 20, "at 1/8: " + arrive);
        helper.assertTrue(ether.getBlockState(arrive).is(BarbecuetherFeature.BARBECUETHER_PORTAAL.get())
                || ether.getBlockState(arrive.above()).is(BarbecuetherFeature.BARBECUETHER_PORTAAL.get()), "a return portal was built there");
        // and back: the same portal in the Guhmensie again (not a new one)
        ArmorStand back = new ArmorStand(EntityType.ARMOR_STAND, ether);
        back.snapTo(there.position().x, there.position().y, there.position().z);
        BlockPos exitPortal = ether.getBlockState(arrive).is(BarbecuetherFeature.BARBECUETHER_PORTAAL.get()) ? arrive : arrive.above();
        var home = GrillPortalForcer.getDestination(ether, back, exitPortal);
        helper.assertTrue(home != null && home.newLevel() == guhmensie, "back to the Guhmensie");
        helper.assertTrue(BlockPos.containing(home.position()).distManhattan(base) <= 4, "through the portal you came from: " + home.position() + " vs " + base);
        // a second trip from the Guhmensie uses the portal that is already there
        var again = GrillPortalForcer.getDestination(guhmensie, walker, base);
        helper.assertTrue(again != null && BlockPos.containing(again.position()).distManhattan(arrive) <= 3, "the same return portal again");
        org.slf4j.LoggerFactory.getLogger("guhs").info("portalLinksBothWays: Guhmensie {} -> Barbecuether {} -> back {}", base, arrive,
                BlockPos.containing(home.position()));
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------------
    @GuhTest(template = ROOM, timeoutTicks = 200)
    public static void kaasfrituursausBurnsAndGlows(GameTestHelper helper) {
        helper.assertTrue(BarbecuetherFeature.KAASFRITUURSAUS_BLOCK.get().defaultBlockState().getLightEmission() == 15, "the sauce glows like lava");
        helper.assertTrue(BarbecuetherFeature.KAASFRITUURSAUS_BUCKET.get() instanceof BucketItem bucket
                && Kaasfrituursaus.isSauce(bucket.content), "there is a bucket of it");
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.STONE);
                helper.setBlock(new BlockPos(x, 1, z), BarbecuetherFeature.KAASFRITUURSAUS_BLOCK.get());
            }
        }
        var pig = helper.spawn(EntityType.PIG, new BlockPos(2, 1, 2));
        float health = pig.getHealth();
        helper.succeedWhen(() -> helper.assertTrue(pig.isOnFire() && (pig.getHealth() < health || !pig.isAlive()), "a pig in it catches fire and gets hurt"));
    }

    @GuhTest(template = ROOM, timeoutTicks = 100)
    public static void sauceAndWaterMakeGrillkool(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(new BlockPos(2, 1, 2), BarbecuetherFeature.KAASFRITUURSAUS_BLOCK.get());
        helper.setBlock(new BlockPos(3, 1, 2), Blocks.WATER);
        helper.setBlock(new BlockPos(6, 1, 6), BarbecuetherFeature.KAASFRITUURSAUS_BLOCK.get());
        helper.setBlock(new BlockPos(6, 1, 5), ModBlocks.KAAS_SAUS.get());
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(BarbecuetherFeature.GRILLKOOL.get(), new BlockPos(2, 1, 2));
            helper.assertBlockPresent(BarbecuetherFeature.GRILLKOOL.get(), new BlockPos(6, 1, 6));
        });
    }

    /** The recipe from the Guhmensie: kaassaus flowing onto a block of coal bakes it into grillkool. */
    @GuhTest(template = ROOM, timeoutTicks = 100)
    public static void kaassausBakesCoalIntoGrillkool(GameTestHelper helper) {
        floor(helper);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.COAL_BLOCK);
        helper.setBlock(new BlockPos(2, 2, 2), ModBlocks.KAAS_SAUS.get());
        helper.setBlock(new BlockPos(5, 1, 5), Blocks.COAL_BLOCK);
        helper.setBlock(new BlockPos(4, 1, 5), ModBlocks.KAAS_SAUS.get());
        helper.succeedWhen(() -> {
            helper.assertBlockPresent(BarbecuetherFeature.GRILLKOOL.get(), new BlockPos(2, 1, 2));
            helper.assertBlockPresent(BarbecuetherFeature.GRILLKOOL.get(), new BlockPos(5, 1, 5));
        });
    }

    // ------------------------------------------------------------------------------------------------------------------
    @GuhTest(template = ROOM)
    public static void theBlocksBehaveLikeTheirNetherCousins(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(BarbecuetherFeature.AS_BLOK.get().getSpeedFactor() < 0.5f, "ash slows you down like soul sand");
        helper.assertTrue(BarbecuetherFeature.PINDASAUSPLASJE.get().getSpeedFactor() < 0.5f, "peanut sauce is sticky");
        BlockPos pos = helper.absolutePos(new BlockPos(2, 1, 2));
        helper.assertTrue(BarbecuetherFeature.HOUTSKOOLSTEEN.get().defaultBlockState().isFireSource(level, pos, Direction.UP),
                "fire burns forever on houtskoolsteen");
        helper.assertTrue(BarbecuetherFeature.GLOEIKOOL.get().defaultBlockState().getLightEmission() == 15, "gloeikool glows");
        helper.assertTrue(BarbecuetherFeature.GRILLKOOL.get().defaultBlockState().getBlock().getExplosionResistance() >= 1200f, "grillkool is as tough as obsidian");
        // covered nylium turns back into houtskoolsteen
        helper.setBlock(new BlockPos(2, 1, 2), BarbecuetherFeature.PINDASAUS_NYLIUM.get());
        helper.setBlock(new BlockPos(2, 2, 2), Blocks.STONE);
        helper.getBlockState(new BlockPos(2, 1, 2)).randomTick(level, pos, level.getRandom());
        helper.assertBlockPresent(BarbecuetherFeature.HOUTSKOOLSTEEN.get(), new BlockPos(2, 1, 2));
        // gloeikool drops its dust, the ore drops kaasknabbels
        var drops = net.minecraft.world.level.block.Block.getDrops(BarbecuetherFeature.GLOEIKOOL.get().defaultBlockState(), level, pos, null);
        helper.assertTrue(!drops.isEmpty() && drops.stream().allMatch(d -> d.is(BarbecuetherFeature.GLOEIKOOLGRUIS.get())), "gloeikool -> gruis: " + drops);
        drops = net.minecraft.world.level.block.Block.getDrops(BarbecuetherFeature.HOUTSKOOLSTEEN_KAASKNABBELERTS.get().defaultBlockState(), level, pos, null,
                null, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(!drops.isEmpty() && drops.stream().allMatch(d -> d.is(ModItems.KAAS_KNABBELS.get())), "the ore -> kaasknabbels: " + drops);
        helper.succeed();
    }

    /** Bone meal on a saté sprout on pindasaus nylium grows a giant saté skewer. */
    @GuhTest(template = ROOM, timeoutTicks = 40)
    public static void sateSproutGrowsIntoASkewer(GameTestHelper helper) {
        for (int x = 0; x < 9; x++) {
            for (int z = 0; z < 9; z++) {
                helper.setBlock(new BlockPos(x, 0, z), BarbecuetherFeature.HOUTSKOOLSTEEN.get());
            }
        }
        helper.setBlock(new BlockPos(4, 0, 4), BarbecuetherFeature.PINDASAUS_NYLIUM.get());
        helper.setBlock(new BlockPos(4, 1, 4), BarbecuetherFeature.SATE_ZWAMMETJE.get());
        var sprout = BarbecuetherFeature.SATE_ZWAMMETJE.get();
        BlockPos at = helper.absolutePos(new BlockPos(4, 1, 4));
        helper.assertTrue(sprout.isValidBonemealTarget(helper.getLevel(), at, helper.getBlockState(new BlockPos(4, 1, 4))), "it can be bone-mealed");
        sprout.performBonemeal(helper.getLevel(), helper.getLevel().getRandom(), at, helper.getBlockState(new BlockPos(4, 1, 4)));
        int stick = 0, meat = 0;
        for (BlockPos p : BlockPos.betweenClosed(new BlockPos(0, 1, 0), new BlockPos(8, 19, 8))) {
            var s = helper.getBlockState(p);
            stick += s.is(BarbecuetherFeature.SATE_STAM.get()) ? 1 : 0;
            meat += s.is(BarbecuetherFeature.SATE_VLEES.get()) ? 1 : 0;
        }
        helper.assertTrue(stick >= 6 && meat >= 8, "a skewer: stick " + stick + ", meat " + meat);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** The whole questline: meet him, mend the frame, bring an Aanmaakblokje, light the pit; then his recipe works. */
    @GuhTest(template = ROOM, batch = "barbecuether_quest", timeoutTicks = 100)
    public static void grillguhQuestline(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper);
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
        npc.setKind(GuhNpcEntity.Kind.GRILLGUH);
        BlockPos at = helper.absolutePos(new BlockPos(7, 1, 7));
        npc.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        helper.getLevel().addFreshEntity(npc);
        var role = BarbecuetherFeature.role();
        helper.assertTrue(role == nl.juiced.guhs.feature.Features.role(GuhNpcEntity.Kind.GRILLGUH), "the Grillguh has his role");

        role.talk(npc, player);
        helper.assertTrue(Grillguh.step(player) == Grillguh.MET && Grillguh.questActive(player), "met him: the quest runs");
        // a broken frame: nothing happens yet
        frame(helper, new BlockPos(1, 1, 3), 2, 3, true);
        helper.setBlock(new BlockPos(1, 3, 3), BarbecuetherFeature.GEBARSTEN_HOUTSKOOLSTEEN_STENEN.get());
        role.talk(npc, player);
        helper.assertTrue(Grillguh.step(player) == Grillguh.MET, "the frame is still broken");
        helper.setBlock(new BlockPos(1, 3, 3), BarbecuetherFeature.GRILLKOOL.get());
        role.talk(npc, player);
        helper.assertTrue(Grillguh.step(player) == Grillguh.FRAME, "the frame is whole again");
        role.talk(npc, player);
        helper.assertTrue(Grillguh.step(player) == Grillguh.FRAME, "no Aanmaakblokje yet");
        // the shop only opens after the quest (but it knows what it sells)
        var offers = npc.getOffers();
        for (GuhClothes c : new GuhClothes[]{GuhClothes.GRILL_KOKSMUTS, GuhClothes.GRILL_SCHORT, GuhClothes.GRILL_HALSDOEK}) {
            helper.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(ModItems.clothingItem(c))), "the chef outfit is sold: " + c.id());
        }
        helper.assertTrue(offers.stream().anyMatch(o -> o.getResult().is(BarbecuetherFeature.AANMAAKBLOKJE.get())), "and Aanmaakblokjes");

        player.getInventory().add(new ItemStack(BarbecuetherFeature.AANMAAKBLOKJE.get()));
        role.talk(npc, player);
        helper.assertTrue(Grillguh.step(player) == Grillguh.BLOKJES, "the Aanmaakblokjes are back");
        var shape = GrillPortalShape.findEmptyShape(helper.getLevel(), helper.absolutePos(new BlockPos(2, 2, 3)));
        helper.assertTrue(shape.isPresent(), "the frame can be lit");
        AanmaakblokjeItem.light(helper.getLevel(), shape.get(), helper.absolutePos(new BlockPos(2, 2, 3)), player);
        helper.assertTrue(Grillguh.step(player) == Grillguh.DONE && !Grillguh.questActive(player), "VAHOEG, the barbecue burns again");
        helper.assertTrue(GuhQuests.count(player, BarbecuetherFeature.GRILLGUH_RECEPT.get()) == 1, "his secret recipe");
        helper.assertTrue(GuhQuests.count(player, ModItems.clothingItem(GuhClothes.GRILL_KOKSMUTS)) == 0
                && GuhQuests.count(player, ModItems.GEFRITUURDE_KAASKNABBELS.get()) >= 16,
                "and a bag of fried knabbels (2.9: the chef's hat is only sold in his shop now)");

        // with the recipe the Aanmaakblokje can be crafted, and the recipe stays
        var input = CraftingInput.of(2, 2, List.of(new ItemStack(BarbecuetherFeature.GRILLGUH_RECEPT.get()), new ItemStack(Items.CHARCOAL),
                new ItemStack(Items.FLINT), new ItemStack(ModItems.KAAS_KNABBELS.get())));
        var recipe = helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
        helper.assertTrue(recipe.isPresent() && recipe.get().value().assemble(input).is(BarbecuetherFeature.AANMAAKBLOKJE.get()),
                "recipe + charcoal + flint + kaasknabbel = Aanmaakblokje");
        NonNullList<ItemStack> left = recipe.get().value().getRemainingItems(input);
        helper.assertTrue(left.stream().anyMatch(s -> s.is(BarbecuetherFeature.GRILLGUH_RECEPT.get())), "the recipe stays in the grid");
        var noRecipe = CraftingInput.of(2, 2, List.of(new ItemStack(Items.PAPER), new ItemStack(Items.CHARCOAL),
                new ItemStack(Items.FLINT), new ItemStack(ModItems.KAAS_KNABBELS.get())));
        helper.assertTrue(helper.getLevel().recipeAccess().getRecipeFor(RecipeType.CRAFTING, noRecipe, helper.getLevel()).isEmpty(),
                "without his recipe: no Aanmaakblokje");
        // lost the recipe? he gives another
        GuhQuests.take(player, BarbecuetherFeature.GRILLGUH_RECEPT.get(), 1);
        role.talk(npc, player);
        helper.assertTrue(GuhQuests.count(player, BarbecuetherFeature.GRILLGUH_RECEPT.get()) == 1, "a new copy of the recipe");
        player.closeContainer();
        npc.discard();
        remove(helper, player);
        helper.succeed();
    }

    /** Mikas in a Mika-kamp drop an Aanmaakblokje, but only while the quest runs (and not a pile of them). */
    @GuhTest(template = EMPTY)
    public static void mikasDropTheStolenAanmaakblokjes(GameTestHelper helper) {
        ServerPlayer player = player(helper);
        BlockPos in = helper.absolutePos(new BlockPos(2, 1, 2));
        AABB camp = new AABB(in).inflate(2);
        try {
            BarbecuetherEvents.TEST_CAMPS.add(camp);
            helper.assertTrue(BarbecuetherEvents.questDrop(player, helper.getLevel(), in).isEmpty(), "no quest: no drop");
            Grillguh.setStep(player, Grillguh.MET);
            helper.assertTrue(BarbecuetherEvents.questDrop(player, helper.getLevel(), in).is(BarbecuetherFeature.AANMAAKBLOKJE.get()), "quest: a drop");
            helper.assertTrue(BarbecuetherEvents.questDrop(player, helper.getLevel(), in.offset(40, 0, 0)).isEmpty(), "only in a Mika-kamp");
            player.getInventory().add(new ItemStack(BarbecuetherFeature.AANMAAKBLOKJE.get(), BarbecuetherEvents.MAX_CARRIED));
            helper.assertTrue(BarbecuetherEvents.questDrop(player, helper.getLevel(), in).isEmpty(), "not more than a few");
            player.getInventory().clearContent();
            Grillguh.setStep(player, Grillguh.DONE);
            helper.assertTrue(BarbecuetherEvents.questDrop(player, helper.getLevel(), in).isEmpty(), "quest done: no more drops");
        } finally {
            BarbecuetherEvents.TEST_CAMPS.remove(camp);
        }
        BarbecuetherEvents.tooHot(player);    // (the bed message; beds in the Barbecuether itself: see the dev server)
        remove(helper, player);
        helper.succeed();
    }

    // ------------------------------------------------------------------------------------------------------------------
    /** The big pit as generated: the Grillguh on his spot, a chest, a broken frame that becomes whole with grillkool. */
    @GuhTest(template = "barbecueput_groot", timeoutTicks = 100)
    public static void theBigPitHasTheGrillguhAndAFrameToMend(GameTestHelper helper) {
        var npcs = helper.getLevel().getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds().inflate(1),
                n -> n.getKind() == GuhNpcEntity.Kind.GRILLGUH);
        helper.assertTrue(npcs.size() == 1, "one Grillguh: " + npcs.size());
        BlockPos npc = npcs.get(0).blockPosition();
        helper.assertTrue(helper.getLevel().getBlockState(npc.below()).isSolid(), "he sits on the floor");
        helper.assertTrue(!Grillguh.frameNear(helper.getLevel(), npc), "the frame is broken");
        // (the template's coordinates, see tools/features/barbecuether_put.py: NPC (27, 5, 14), frame x16..19, y5..9, z26)
        BlockPos f = npc.offset(-11, 0, 12);
        int kool = 0;
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 5; j++) {
                if (i == 0 || i == 3 || j == 0 || j == 4) {
                    BlockPos p = f.offset(i, j, 0);
                    kool += helper.getLevel().getBlockState(p).is(BarbecuetherFeature.GRILLKOOL.get()) ? 1 : 0;
                    helper.getLevel().setBlockAndUpdate(p, BarbecuetherFeature.GRILLKOOL.get().defaultBlockState());
                }
            }
        }
        helper.assertTrue(kool >= 8 && kool < 14, "a broken frame: " + kool + " of 14 grillkool");
        helper.assertTrue(Grillguh.frameNear(helper.getLevel(), npc), "mended with grillkool it's a whole frame");
        int chests = 0, faces = 0;
        for (BlockPos p : BlockPos.betweenClosed(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(36, 19, 36)))) {
            var s = helper.getLevel().getBlockState(p);
            chests += s.is(Blocks.CHEST) ? 1 : 0;
            faces += s.is(BarbecuetherFeature.GEBEITELDE_HOUTSKOOLSTEEN_STENEN.get()) ? 1 : 0;
        }
        helper.assertTrue(chests >= 1 && faces >= 12, "a chest and guh faces everywhere: " + chests + ", " + faces);
        helper.succeed();
    }

    /** The pit chests help you build a portal (grillkool, coal, and sometimes an Aanmaakblokje). */
    @GuhTest(template = EMPTY)
    public static void pitChestsHaveGrillkool(GameTestHelper helper) {
        var level = helper.getLevel();
        var table = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, Guhs.id("chests/barbecueput_klein")));
        var params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, helper.absoluteVec(new Vec3(1, 1, 1)))
                .create(LootContextParamSets.CHEST);
        boolean kool = false, blokje = false;
        for (int i = 0; i < 300 && !(kool && blokje); i++) {
            for (ItemStack s : table.getRandomItems(params)) {
                kool |= s.is(BarbecuetherFeature.GRILLKOOL_ITEM.get());
                blokje |= s.is(BarbecuetherFeature.AANMAAKBLOKJE.get());
            }
        }
        helper.assertTrue(kool && blokje, "grillkool and Aanmaakblokjes in the small pits");
        helper.succeed();
    }

    /** The dimension's data is all there: biomes, the structure, the compass category, the Guhdex page. */
    @GuhTest(template = EMPTY)
    public static void theBarbecuetherDataIsComplete(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        for (String b : new String[]{"houtskoolvlakte", "satebos", "worstenwoud", "asdal", "rookdelta"}) {
            helper.assertTrue(access.lookupOrThrow(Registries.BIOME).containsKey(Guhs.id(b)), "biome " + b);
        }
        helper.assertTrue(access.lookupOrThrow(Registries.STRUCTURE).containsKey(Guhs.id("barbecueput")), "the barbecueput structure");
        Structure put = access.lookupOrThrow(Registries.STRUCTURE).getValue(Guhs.id("barbecueput"));
        helper.assertTrue(put instanceof BarbecuePutStructure, "of its own type");
        helper.assertTrue(access.lookupOrThrow(Registries.CONFIGURED_FEATURE).containsKey(BarbecuetherFeature.SATE_GEKWEEKT.identifier())
                && access.lookupOrThrow(Registries.CONFIGURED_FEATURE).containsKey(BarbecuetherFeature.WORST_GEKWEEKT.identifier()), "the grown features");
        helper.assertTrue(SuperkompasItem.CATEGORIES.stream().anyMatch(c -> c.id().equals("barbecue"))
                && SuperkompasItem.allowed("barbecueput"), "the super compass looks for barbecue pits");
        helper.assertTrue(GuhDex.ENTRIES.contains(GuhVariant.GRILLGUH) && GuhVariant.GRILLGUH.isCharacter()
                && GuhVariant.GRILLGUH.npcKind() == GuhNpcEntity.Kind.GRILLGUH, "the Grillguh has a Guhdex page");
        helper.succeed();
    }
}
