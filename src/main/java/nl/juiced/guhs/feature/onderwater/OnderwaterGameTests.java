package nl.juiced.guhs.feature.onderwater;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.GuhWorldData;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Game tests of the Guhbubbel: kaaskoraal air, the shells and their pearls, the duikhelm, the Zeemeerguh's shop and
 * Guhdex page, the Zeemeerguh variant (swimming, riding, breathing), the protection, the template as placed by the game
 * (air-tight, on the sea floor, everything in place) and the worldgen settings; plus an optional check on a real
 * Guhmension world that a generated bubble really lies on the bottom of a Diepe Guhzee, completely under water.
 */
@GameTestHolder(Guhs.MODID)
@PrefixGameTestTemplate(false)
public class OnderwaterGameTests {
    private static final String EMPTY = "empty";
    /** Template layout (tools/features/onderwater.py). */
    private static final int F = 8, G = 36, CX = 64, CZ = 64, TX = 64, TZ = 114;

    private static ServerPlayer diver(GameTestHelper helper, BlockPos at) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        BlockPos abs = helper.absolutePos(at);
        player.moveTo(abs.getX() + 0.5, abs.getY(), abs.getZ() + 0.5);
        return player;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }

    /** A little pool: water in the test's 5x5 room up to this height (floor at y = 0 stays). */
    private static void pool(GameTestHelper helper, int height) {
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                for (int y = 1; y <= height; y++) {
                    boolean wall = x == 0 || z == 0 || x == 4 || z == 4;
                    helper.setBlock(new BlockPos(x, y, z), wall ? Blocks.GLASS : Blocks.WATER);
                }
            }
        }
    }

    @GameTest(template = EMPTY)
    public static void kaaskoraalGivesAirUnderWater(GameTestHelper helper) {
        pool(helper, 4);
        ServerPlayer player = diver(helper, new BlockPos(2, 1, 2));
        try {
            player.setAirSupply(10);
            helper.assertTrue(OnderwaterFeature.underWater(player), "the diver is under water");
            helper.assertTrue(!OnderwaterFeature.breathe(player), "no coral, no air");
            helper.setBlock(new BlockPos(3, 1, 3), OnderwaterFeature.KAASKORAAL.get().defaultBlockState().setValue(KaaskoraalBlock.WATERLOGGED, true));
            helper.assertTrue(helper.getBlockState(new BlockPos(3, 1, 3)).getFluidState().is(Fluids.WATER), "it lives in the water");
            helper.assertTrue(OnderwaterFeature.breathe(player), "a breath from the kaaskoraal");
            helper.assertTrue(player.getAirSupply() == 10 + OnderwaterFeature.AIR_PER_BREATH, "air: " + player.getAirSupply());
            player.setAirSupply(player.getMaxAirSupply());
            helper.assertTrue(!OnderwaterFeature.breathe(player), "full lungs: nothing to breathe");
        } finally {
            leave(helper, player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void kaaskoraalNeedsSomethingToStandOn(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos.below(), Blocks.SAND);
        helper.setBlock(pos, OnderwaterFeature.KAASKORAAL.get());
        helper.assertBlockPresent(OnderwaterFeature.KAASKORAAL.get(), pos);
        helper.setBlock(pos.below(), Blocks.AIR);
        helper.assertBlockNotPresent(OnderwaterFeature.KAASKORAAL.get(), pos);
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void shellsGivePearlsAndGrowNewOnes(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, OnderwaterFeature.REUZENSCHELP.get().defaultBlockState());
        ServerPlayer player = diver(helper, new BlockPos(1, 1, 1));
        try {
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
            BlockPos abs = helper.absolutePos(pos);
            helper.assertTrue(ReuzenschelpBlock.take(player, abs), "an open shell gives its pearl");
            helper.assertTrue(GuhQuests.count(player, OnderwaterFeature.PAREL.get()) == 1, "one pearl");
            helper.assertTrue(!helper.getBlockState(pos).getValue(ReuzenschelpBlock.PAREL), "the shell closes");
            helper.assertTrue(!ReuzenschelpBlock.take(player, abs), "a closed shell gives nothing");
            helper.assertTrue(GuhQuests.count(player, OnderwaterFeature.PAREL.get()) == 1, "still one pearl");
            var random = helper.getLevel().getRandom();
            for (int i = 0; i < 400 && !helper.getBlockState(pos).getValue(ReuzenschelpBlock.PAREL); i++) {
                helper.getBlockState(pos).randomTick(helper.getLevel(), abs, random);
            }
            helper.assertTrue(helper.getBlockState(pos).getValue(ReuzenschelpBlock.PAREL), "a new pearl grows");
            // (with something in your paw it works too: the block reacts, not the item)
            helper.getBlockState(pos).useItemOn(player.getMainHandItem(), helper.getLevel(), player, InteractionHand.MAIN_HAND,
                    new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(abs), net.minecraft.core.Direction.UP, abs, false));
            helper.assertTrue(GuhQuests.count(player, OnderwaterFeature.PAREL.get()) == 2, "two pearls");
        } finally {
            leave(helper, player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void duikhelmLetsYouBreatheUnderWater(GameTestHelper helper) {
        pool(helper, 4);
        ServerPlayer player = diver(helper, new BlockPos(2, 1, 2));
        try {
            ItemStack helm = new ItemStack(OnderwaterFeature.DUIKHELM.get());
            helper.assertTrue(!DuikhelmItem.divingTick(player, helm), "in your inventory it does nothing");
            player.setItemSlot(EquipmentSlot.HEAD, helm);
            helper.assertTrue(DuikhelmItem.divingTick(player, player.getItemBySlot(EquipmentSlot.HEAD)), "worn under water");
            helper.assertTrue(player.hasEffect(MobEffects.WATER_BREATHING) && player.hasEffect(MobEffects.NIGHT_VISION), "you breathe and see");
            player.removeAllEffects();
            player.moveTo(player.getX(), player.getY() + 10, player.getZ());
            helper.assertTrue(!DuikhelmItem.divingTick(player, player.getItemBySlot(EquipmentSlot.HEAD)), "not above water");
            helper.assertTrue(!player.hasEffect(MobEffects.WATER_BREATHING), "no effect in the air");
        } finally {
            leave(helper, player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void zeemeerguhSellsTheDiveGearForPearls(GameTestHelper helper) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(helper.getLevel());
        npc.setKind(GuhNpcEntity.Kind.ZEEMEERGUH);
        BlockPos at = helper.absolutePos(new BlockPos(3, 1, 3));
        npc.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        helper.getLevel().addFreshEntity(npc);
        ServerPlayer player = diver(helper, new BlockPos(1, 1, 1));
        try {
            helper.assertTrue(nl.juiced.guhs.feature.Features.role(GuhNpcEntity.Kind.ZEEMEERGUH) instanceof ZeemeerguhRole, "she has a role");
            OnderwaterFeature.role().talk(npc, player);
            helper.assertTrue(GuhQuests.saved(player).getBoolean(ZeemeerguhRole.MET_KEY), "she remembers you");
            helper.assertTrue(GuhWorldData.get(player.server).player(player.getUUID()).seen.contains(GuhVariant.ZEEMEERGUH),
                    "meeting her fills in the Zeemeerguh page of the Guhdex");
            var offers = OnderwaterFeature.role().offers(npc);
            List<net.minecraft.world.item.Item> sold = new ArrayList<>();
            for (var offer : offers) {
                helper.assertTrue(offer.getCostA().is(OnderwaterFeature.PAREL.get()) && offer.getCostB().isEmpty(), "paid in pearls only");
                sold.add(offer.getResult().getItem());
            }
            for (var item : List.of(OnderwaterFeature.DUIKHELM.get(), ModItems.clothingItem(GuhClothes.DUIKBRIL), ModItems.clothingItem(GuhClothes.SNORKEL),
                    ModItems.clothingItem(GuhClothes.ZWEMBAND), Items.SADDLE, OnderwaterFeature.KAASKORAAL.get().asItem(),
                    OnderwaterFeature.PARELMOER.get().asItem())) {
                helper.assertTrue(sold.contains(item), "she sells " + item);
            }
            helper.assertTrue(GuhDex.ENTRIES.contains(GuhVariant.ZEEMEERGUH) && GuhDex.TAMEABLE.contains(GuhVariant.ZEEMEERGUH),
                    "the Zeemeerguh has a Guhdex page, and can be tamed");
            helper.assertTrue(GuhClothes.DUIKBRIL.slot == GuhClothes.Slot.EYES && GuhClothes.SNORKEL.slot == GuhClothes.Slot.HEAD
                    && GuhClothes.ZWEMBAND.slot == GuhClothes.Slot.BODY, "the duikpakje: goggles, snorkel, swim ring");
        } finally {
            npc.discard();
            leave(helper, player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 200)
    public static void zeemeerguhDivesAndBreathesUnderWater(GameTestHelper helper) {
        pool(helper, 4);
        GuhEntity guh = Zeemeerguh.spawnAt(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2)), helper.getLevel().getRandom());
        helper.assertTrue(guh != null, "a Zeemeerguh can spawn in water");
        helper.assertTrue(guh.getVariant() == GuhVariant.ZEEMEERGUH && guh.isZeemeer(), "with a fish tail");
        helper.assertTrue(guh.getGuhScale() >= GuhEntity.RIDEABLE_SCALE && !guh.isWearingClothes(), "big enough to ride, no clothes");
        helper.assertTrue(!guh.canDrownInFluidType(net.neoforged.neoforge.common.NeoForgeMod.WATER_TYPE.value()) && guh.getNavigation() instanceof AmphibiousPathNavigation, "it breathes and swims");
        guh.setAirSupply(guh.getMaxAirSupply());
        // a normal guh floats up; the Zeemeerguh stays down there
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(guh.isAlive() && guh.isInWater(), "still in the water after 4 seconds");
            helper.assertTrue(guh.getAirSupply() == guh.getMaxAirSupply() && guh.getHealth() == guh.getMaxHealth(), "and never short of air");
            var tag = new net.minecraft.nbt.CompoundTag();
            guh.saveWithoutId(tag);
            GuhEntity copy = ModEntities.GUH.get().create(helper.getLevel());
            copy.load(tag);
            helper.assertTrue(copy.isZeemeer() && copy.getNavigation() instanceof AmphibiousPathNavigation, "saved and loaded, it still swims");
            guh.discard();
            helper.succeed();
        });
    }

    @GameTest(template = EMPTY)
    public static void ridingAZeemeerguhUnderWater(GameTestHelper helper) {
        pool(helper, 4);
        ServerPlayer player = diver(helper, new BlockPos(2, 1, 2));
        GuhEntity guh = ModEntities.GUH.get().create(helper.getLevel());
        try {
            BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2));
            guh.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            Zeemeerguh.become(guh, helper.getLevel().getRandom());
            helper.getLevel().addFreshEntity(guh);
            guh.tame(player);
            guh.equipSaddle(new ItemStack(Items.SADDLE), null);
            helper.assertTrue(guh.isRideable() && guh.isSaddled(), "tamed and saddled");
            guh.onOwnerTap(player);
            helper.assertTrue(player.getVehicle() == guh && guh.getControllingPassenger() == player, "you ride it");
            player.setAirSupply(0);
            Zeemeerguh.keepBreathing(player);
            helper.assertTrue(player.hasEffect(MobEffects.WATER_BREATHING) && player.getAirSupply() == player.getMaxAirSupply(),
                    "the Zeemeerguh keeps its rider breathing");
            helper.assertTrue(!guh.dismountsUnderwater(), "and doesn't throw you off under water");
            // where the rider looks: straight down, full speed ahead
            Vec3 v = Vec3.ZERO;
            for (int i = 0; i < 20; i++) {
                v = Zeemeerguh.rideVelocity(v, new Vec3(0, -1, 0), 1f, 0f, false, false);
            }
            helper.assertTrue(v.y < -0.4 && Math.abs(v.x) < 0.01, "diving down: " + v);
            Vec3 w = Vec3.ZERO;
            for (int i = 0; i < 20; i++) {
                w = Zeemeerguh.rideVelocity(w, new Vec3(1, 0, 0), 1f, 0f, true, true);
            }
            helper.assertTrue(w.x > 0.7 && w.y > 0.3, "sprinting ahead and up (fast): " + w);
        } finally {
            player.stopRiding();
            guh.discard();
            leave(helper, player);
        }
        helper.succeed();
    }

    @GameTest(template = EMPTY)
    public static void theBubbleCantBeBrokenOrFlooded(GameTestHelper helper) {
        BlockPos corner = helper.absolutePos(BlockPos.ZERO);
        AABB area = new AABB(corner.getX(), corner.getY(), corner.getZ(), corner.getX() + 5, corner.getY() + 4, corner.getZ() + 5);
        OnderwaterProtection.TEST_AREAS.add(area);
        ServerPlayer player = diver(helper, new BlockPos(1, 1, 1));
        try {
            helper.setBlock(new BlockPos(3, 1, 3), Blocks.GLASS);
            helper.assertTrue(!player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(3, 1, 3))), "the glass can't be broken");
            helper.assertBlockPresent(Blocks.GLASS, new BlockPos(3, 1, 3));
            helper.assertTrue(OnderwaterProtection.inBubble(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2))), "the test room is a bubble");
            helper.assertTrue(nl.juiced.guhs.feature.Protected.at(helper.getLevel(), helper.absolutePos(new BlockPos(2, 1, 2))),
                    "and protected against fire and floods");
            // a pearl is still yours to take
            helper.setBlock(new BlockPos(2, 1, 3), OnderwaterFeature.REUZENSCHELP.get());
            helper.assertTrue(ReuzenschelpBlock.take(player, helper.absolutePos(new BlockPos(2, 1, 3))), "shells work in the bubble");
            OnderwaterProtection.TEST_AREAS.remove(area);
            helper.assertTrue(player.gameMode.destroyBlock(helper.absolutePos(new BlockPos(3, 1, 3))), "outside the bubble glass breaks as usual");
        } finally {
            OnderwaterProtection.TEST_AREAS.remove(area);
            leave(helper, player);
        }
        helper.succeed();
    }

    /** The worldgen settings: in the Diepe Guhzee only, placed by the Guhbubbel's own type; Zeemeerguhs in both guh seas. */
    @GameTest(template = EMPTY)
    public static void theBubbleGeneratesInTheDeepSea(GameTestHelper helper) {
        var registry = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE);
        var structure = registry.get(OnderwaterProtection.BUBBLE);
        helper.assertTrue(structure instanceof GuhbubbelStructure, "the Guhbubbel's own structure type: " + structure);
        var biomes = helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME);
        var deep = biomes.getHolder(Zeemeerguh.DIEPE_GUHZEE);
        helper.assertTrue(deep.isPresent() && structure.biomes().contains(deep.get()), "in the deep guh sea");
        helper.assertTrue(structure.biomes().size() == 1, "and nowhere else");
        var sea = biomes.getHolder(Zeemeerguh.GUH_SEA);
        helper.assertTrue(sea.isPresent() && sea.get().is(Zeemeerguh.GUHZEEEN) && deep.get().is(Zeemeerguh.GUHZEEEN),
                "wild Zeemeerguhs turn up in both guh seas");
        var set = helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE_SET).get(Guhs.id("onderwater"));
        helper.assertTrue(set != null && set.placement() instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement spread
                && spread.spacing() == ((GuhbubbelStructure) structure).cellChunks(), "one start chunk per cell");
        var template = helper.getLevel().getStructureManager().get(Guhs.id("onderwater"));
        helper.assertTrue(template.isPresent() && template.get().getSize().getX() == 128 && template.get().getSize().getY() > G,
                "the template: " + template.map(t -> t.getSize().toString()).orElse("missing"));
        helper.succeed();
    }

    /**
     * The whole Guhbubbel as the game places it: after a few seconds of water physics the dome, the tube and the tunnel
     * are still dry, the dome is on the sea floor with water above it, and everything is where it belongs. (In the world
     * the sea goes on around the template's round piece of sea; here a stone ring keeps the water in.)
     */
    @GameTest(template = "onderwater", timeoutTicks = 400)
    public static void theBubbleIsAirTightOnTheSeaFloor(GameTestHelper helper) {
        for (int x = 0; x < 128; x++) {
            for (int z = 0; z < 128; z++) {
                double r = Math.hypot(x - CX, z - CZ);
                if (r < 61 || r > 66) {
                    continue;
                }
                for (int y = F - 3; y <= G; y++) {
                    if (helper.getBlockState(new BlockPos(x, y, z)).isAir()) {
                        helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
                    }
                }
            }
        }
        helper.runAfterDelay(100, () -> {
            ServerLevel level = helper.getLevel();
            var npcs = level.getEntitiesOfClass(GuhNpcEntity.class, helper.getBounds().inflate(1), n -> n.getKind() == GuhNpcEntity.Kind.ZEEMEERGUH);
            helper.assertTrue(npcs.size() == 1, "one Zeemeerguh: " + npcs.size());
            BlockPos o = npcs.get(0).blockPosition().offset(-CX, -(F + 1), -CZ);
            helper.assertTrue(level.getBlockState(o.offset(CX, F, CZ)).isSolid(), "she sits on the floor of the dome");
            // dry inside: the dome (above the ponds), the stair tube and the tunnel
            List<BlockPos> wet = new ArrayList<>();
            for (BlockPos p : BlockPos.betweenClosed(o.offset(CX - 30, F + 1, CZ - 30), o.offset(CX + 30, F + 14, CZ + 30))) {
                double r = Math.hypot(p.getX() - o.getX() - CX, p.getZ() - o.getZ() - CZ);
                int inside = F + (int) (18 * Math.sqrt(Math.max(0, 1 - (r / 37) * (r / 37)))) - 1;     // (under the dome's glass)
                if (r <= 30 && p.getY() - o.getY() <= inside && level.getFluidState(p).is(Fluids.WATER)
                        && !level.getBlockState(p).hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)) {
                    wet.add(p.subtract(o));
                }
            }
            for (BlockPos p : BlockPos.betweenClosed(o.offset(TX - 2, F + 1, TZ - 2), o.offset(TX + 2, G - 1, TZ + 2))) {
                if (level.getFluidState(p).is(Fluids.WATER)) {
                    wet.add(p.subtract(o));
                }
            }
            for (BlockPos p : BlockPos.betweenClosed(o.offset(CX - 1, F + 1, CZ + 40), o.offset(CX + 1, F + 3, TZ - 3))) {
                if (level.getFluidState(p).is(Fluids.WATER)) {
                    wet.add(p.subtract(o));
                }
            }
            helper.assertTrue(wet.isEmpty(), "the bubble stays dry: " + wet.size() + " wet blocks, e.g. " + wet.subList(0, Math.min(12, wet.size())));
            // on the sea floor: air in the middle of the dome, water above its glass all the way to the surface
            helper.assertTrue(level.getBlockState(o.offset(CX, F + 10, CZ)).isAir(), "air in the dome");
            int glass = -1;
            for (int y = F + 10; y < G && glass < 0; y++) {
                if (!level.getBlockState(o.offset(CX, y, CZ)).isAir()) {
                    glass = y;
                }
            }
            helper.assertTrue(glass > F + 10 && glass < G - 4, "the top of the dome, deep under water: " + glass);
            for (int y = glass + 1; y <= G; y++) {
                helper.assertTrue(level.getBlockState(o.offset(CX, y, CZ)).is(Blocks.WATER), "water above the dome at " + y);
            }
            helper.assertTrue(level.getBlockState(o.offset(CX + 60, G, CZ)).is(Blocks.WATER) && level.getBlockState(o.offset(CX + 60, G + 1, CZ)).isAir(),
                    "the sea's surface on the template's edge");
            helper.assertTrue(level.getBlockState(o.offset(TX + 9, G, TZ)).isSolid() && level.getBlockState(o.offset(TX, G, TZ + 11)).isSolid()
                    && level.getBlockState(o.offset(TX + 14, G, TZ)).is(Blocks.WATER), "the Duikpost's island, level with the water");
            helper.assertTrue(level.getBlockState(o.offset(CX + 44, F, CZ)).isSolid() && level.getBlockState(o.offset(CX + 44, F + 1, CZ)).getFluidState().is(Fluids.WATER)
                    || level.getBlockState(o.offset(CX + 44, F + 2, CZ)).getFluidState().is(Fluids.WATER), "the sea floor around the dome");
            // the way down and up
            helper.assertTrue(level.getBlockState(o.offset(TX, G, TZ + 2)).is(Blocks.PRISMARINE_BRICK_STAIRS), "the top of the spiral stairs");
            helper.assertTrue(level.getBlockState(o.offset(TX + 5, F, TZ)).is(Blocks.SOUL_SAND)
                    && level.getBlockState(o.offset(TX + 5, G, TZ)).is(Blocks.BUBBLE_COLUMN), "the bubble lift goes all the way up");
            // shells, doors, the wreck's chest, kaaskoraal
            int shells = 0, doors = 0, coral = 0;
            List<BlockPos> chests = new ArrayList<>();
            for (BlockPos p : BlockPos.betweenClosed(o, o.offset(127, G + 1, 127))) {
                BlockState state = level.getBlockState(p);
                if (state.getBlock() instanceof ReuzenschelpBlock) {
                    shells++;
                } else if (state.getBlock() instanceof DoorBlock) {
                    doors++;
                } else if (state.is(OnderwaterFeature.KAASKORAAL.get())) {
                    coral++;
                } else if (state.getBlock() instanceof ChestBlock && level.getBlockEntity(p) instanceof ChestBlockEntity) {
                    chests.add(p.immutable());
                }
            }
            helper.assertTrue(shells >= 12, "giant shells: " + shells);
            helper.assertTrue(doors >= 8, "dive doors and the lift door: " + doors);
            helper.assertTrue(coral > 100, "kaaskoraal: " + coral);
            helper.assertTrue(chests.size() == 1 && level.getBlockState(chests.get(0)).getValue(ChestBlock.WATERLOGGED), "the wreck's treasure chest: " + chests);
            helper.succeed();
        });
    }

    /**
     * On a real Guhmension world (dev server: /test runall): the nearest Guhbubbel really lies on the bottom of a Diepe
     * Guhzee, its water surface level with the sea's, the sea going on around it, and the dome is dry.
     */
    @GameTest(template = EMPTY, required = false, timeoutTicks = 2400)
    public static void aGeneratedBubbleLiesOnTheBottomOfADeepSea(GameTestHelper helper) {
        ServerLevel guhmension = helper.getLevel().getServer().getLevel(ModDimensions.GUHMENSION);
        helper.assertTrue(guhmension != null, "the Guhmension exists");
        var holder = guhmension.registryAccess().registryOrThrow(Registries.STRUCTURE).getHolderOrThrow(OnderwaterProtection.BUBBLE);
        var found = guhmension.getChunkSource().getGenerator().findNearestMapStructure(guhmension, net.minecraft.core.HolderSet.direct(holder),
                BlockPos.ZERO, 200, false);
        helper.assertTrue(found != null, "there is a Guhbubbel");
        BlockPos start = found.getFirst();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                guhmension.getChunk((start.getX() >> 4) + dx, (start.getZ() >> 4) + dz);
            }
        }
        net.minecraft.world.level.levelgen.structure.StructureStart bubble = null;
        for (int y = guhmension.getMinBuildHeight(); y < guhmension.getMaxBuildHeight() && bubble == null; y += 4) {
            var here = guhmension.structureManager().getStructureWithPieceAt(start.atY(y), holder.value());
            bubble = here.isValid() ? here : null;
        }
        helper.assertTrue(bubble != null, "the bubble's piece is there");
        var box = bubble.getBoundingBox();
        BlockPos mid = new BlockPos(box.getCenter().getX(), box.minY(), box.getCenter().getZ());
        helper.assertTrue(guhmension.getBlockState(mid.above(F + 10)).isAir(), "air in the dome");
        helper.assertTrue(guhmension.getBlockState(mid.above(G - 2)).is(Blocks.WATER), "water above it");
        helper.assertTrue(guhmension.getBlockState(mid.above(F)).isSolid(), "the dome stands on the floor");
        int water = ((GuhbubbelStructure) holder.value()).waterLevel();
        helper.assertTrue(box.minY() + G + 1 == water, "the bubble's water surface is the sea's: " + (box.minY() + G + 1) + " vs " + water);
        helper.assertTrue(guhmension.getBiome(mid.above(G)).is(Zeemeerguh.DIEPE_GUHZEE), "in the deep guh sea");
        for (int[] d : new int[][]{{72, 0}, {-72, 0}, {0, 72}, {0, -72}}) {
            BlockPos out = mid.offset(d[0], G, d[1]);
            guhmension.getChunk(out);
            helper.assertTrue(guhmension.getBlockState(out).is(Blocks.WATER) && guhmension.getBlockState(out.above()).isAir(),
                    "the sea goes on around it: " + out + " " + guhmension.getBlockState(out));
        }
        LOGGER.info("The Guhbubbel at {} lies on the bottom of a Diepe Guhzee (surface y {}, dome floor y {})", mid, water - 1, mid.getY() + F);
        helper.succeed();
    }

    // --- worldgen: the Diepe Guhzee's share of the Guhmension, and one Guhbubbel per sea -----------------------------------------

    /**
     * Samples the Guhmension's surface biomes with and without the Diepe Guhzee (from the dimension JSON and its noise
     * settings, three seeds): the deep sea gets a fair share (not too much) and every other biome keeps most of its own.
     */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void theDeepSeaTakesItsShareAndLeavesTheRest(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var access = server.registryAccess();
        com.google.gson.JsonObject source;
        try (var reader = server.getResourceManager().getResource(Guhs.id("dimension/guhmension.json")).orElseThrow().openAsReader()) {
            source = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("generator").getAsJsonObject("biome_source");
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        }
        com.google.gson.JsonObject without = source.deepCopy();
        com.google.gson.JsonArray kept = new com.google.gson.JsonArray();
        for (var e : without.getAsJsonArray("biomes")) {
            if (!e.getAsJsonObject().get("biome").getAsString().equals("guhs:diepe_guhzee")) {
                kept.add(e);
            }
        }
        without.add("biomes", kept);
        var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, access);
        var with = net.minecraft.world.level.biome.BiomeSource.CODEC.parse(ops, source).getOrThrow();
        var before = net.minecraft.world.level.biome.BiomeSource.CODEC.parse(ops, without).getOrThrow();
        var settings = access.registryOrThrow(Registries.NOISE_SETTINGS).get(Guhs.id("guhmension"));
        java.util.Map<String, Integer> now = new java.util.HashMap<>(), then = new java.util.HashMap<>();
        int samples = 0;
        for (long seed : new long[]{1L, 20270501L, -778899L}) {
            var sampler = net.minecraft.world.level.levelgen.RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), seed).sampler();
            for (int x = -4000; x < 4000; x += 40) {
                for (int z = -4000; z < 4000; z += 40) {
                    int qx = net.minecraft.core.QuartPos.fromBlock(x), qy = net.minecraft.core.QuartPos.fromBlock(80), qz = net.minecraft.core.QuartPos.fromBlock(z);
                    now.merge(with.getNoiseBiome(qx, qy, qz, sampler).unwrapKey().orElseThrow().location().getPath(), 1, Integer::sum);
                    then.merge(before.getNoiseBiome(qx, qy, qz, sampler).unwrapKey().orElseThrow().location().getPath(), 1, Integer::sum);
                    samples++;
                }
            }
        }
        StringBuilder report = new StringBuilder();
        for (String b : new java.util.TreeSet<>(then.keySet())) {
            report.append(String.format("%s %.1f%% -> %.1f%%; ", b, 100.0 * then.get(b) / samples, 100.0 * now.getOrDefault(b, 0) / samples));
        }
        double share = now.getOrDefault("diepe_guhzee", 0) / (double) samples;
        report.append(String.format("diepe_guhzee %.1f%%", 100 * share));
        LOGGER.info("Guhmension surface biomes (before -> with the Diepe Guhzee): {}", report);
        // (1.0.0: diepzee.ZEE_KRIMP made the seas fewer and smaller: ~11.5% in 3.0, now ~7.5%)
        helper.assertTrue(share >= 0.05 && share <= 0.095, "the Diepe Guhzee's share: " + report);
        for (var e : then.entrySet()) {
            double was = e.getValue() / (double) samples, is = now.getOrDefault(e.getKey(), 0) / (double) samples;
            // (2.9) the Guhpolder sits in the deepest corner of the depth parameter too: without the deep sea it takes the
            // deep-sea cells as well, so its "before" share isn't a real world. It is checked on its own below.
            // (3.0) the same for Guhwai'i: its islands sit inside the sea (weirdness band between the sea and the land)
            if (was >= 0.01 && !e.getKey().equals("guhpolder") && !e.getKey().equals("guhwaii")) {
                helper.assertTrue(is >= 0.75 * was, e.getKey() + " keeps most of its share: " + report);
            }
        }
        double polder = now.getOrDefault("guhpolder", 0) / (double) samples;
        helper.assertTrue(polder >= 0.01 && polder <= 0.05, "the Guhpolder keeps its small share with the deep sea: " + report);
        helper.succeed();
    }

    /**
     * The Guhbubbel's spots (GuhbubbelStructure, on the real sea noise of three seeds): there are plenty, each on a deep
     * peak, and no two of them in the same sea.
     */
    @GameTest(template = EMPTY, timeoutTicks = 400)
    public static void oneBubblePerDeepSea(GameTestHelper helper) {
        var access = helper.getLevel().getServer().registryAccess();
        var bubble = (GuhbubbelStructure) access.registryOrThrow(Registries.STRUCTURE).get(OnderwaterProtection.BUBBLE);
        var settings = access.registryOrThrow(Registries.NOISE_SETTINGS).get(Guhs.id("guhmension"));
        int size = bubble.cellChunks(), cells = 40, total = 0;
        StringBuilder report = new StringBuilder();
        for (long seed : new long[]{1L, 20270501L, -778899L}) {
            var noise = net.minecraft.world.level.levelgen.RandomState.create(settings, access.lookupOrThrow(Registries.NOISE), seed)
                    .getOrCreateNoise(bubble.seaNoise());
            List<GuhbubbelStructure.Peak> found = new ArrayList<>();
            for (int cx = -cells; cx < cells; cx++) {
                for (int cz = -cells; cz < cells; cz++) {
                    var peak = GuhbubbelStructure.peak(seed, noise, size, cx, cz);
                    if (peak.value() >= bubble.minValue()
                            && GuhbubbelStructure.highestOfItsSea(seed, noise, size, bubble.neighbourhood(), bubble.seaValue(), cx, cz)) {
                        found.add(peak);
                    }
                }
            }
            for (int i = 0; i < found.size(); i++) {
                for (int j = i + 1; j < found.size(); j++) {
                    var a = found.get(i);
                    var b = found.get(j);
                    helper.assertTrue(Math.hypot(a.x() - b.x(), a.z() - b.z()) > 128, "two bubbles far apart: " + a + " " + b);
                    helper.assertTrue(!GuhbubbelStructure.sameSea(noise, a, b, bubble.seaValue()) || Math.hypot(a.x() - b.x(), a.z() - b.z()) > size * 16 * (bubble.neighbourhood() + 1),
                            "never two in the same sea: " + a + " " + b);
                }
            }
            total += found.size();
            report.append(seed).append(": ").append(found.size()).append("; ");
        }
        LOGGER.info("Guhbubbel spots in {} x {} blocks: {}", cells * 2 * size * 16, cells * 2 * size * 16, report);
        helper.assertTrue(total >= 9, "Guhbubbel spots: " + report);
        helper.succeed();
    }

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("guhs");
}
