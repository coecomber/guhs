package nl.juiced.guhs.feature.eilanden;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import nl.juiced.guhs.gametest.GuhTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhPersonality;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.world.GuhWorldData;

import net.minecraft.world.entity.EntitySpawnReason;
/** GameTests of the floating guh islands: the Wolkguh, the wolkenlift, falling safely and the protection. */
public class EilandenGameTests {
    private static final String EMPTY = "empty";
    private static final String ISLANDS = "zwevende_eilanden";
    /** Template layout (tools/features/eilanden.py): the main island's top block and the lift columns' height. */
    private static final int S0 = 62, TOP = 64;

    @GuhTest(template = EMPTY)
    public static void wolkguhOnlyTrustsYouWhenItsVadsEnough(GameTestHelper helper) {
        GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 1, 2));
        guh.setVariant(GuhVariant.WOLK);
        guh.setPersonality(GuhPersonality.VADSIG); // (1 in 2 per knabbel, once it trusts you)
        ServerPlayer player = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        player.setGameMode(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KAAS_KNABBELS.get(), 64));
        try {
            feedUntilTame(helper, guh, player);
        } finally {
            leave(helper, player);
        }
        helper.succeed();
    }

    private static void feedUntilTame(GameTestHelper helper, GuhEntity guh, ServerPlayer player) {
        for (int i = 1; i < EilandenEvents.KNABBELS_NEEDED; i++) {
            player.interactOn(guh, InteractionHand.MAIN_HAND, guh.position());
            helper.assertTrue(!guh.isTame(), "not tame after " + i + " knabbels");
        }
        helper.assertTrue(EilandenEvents.knabbelsFed(guh) == EilandenEvents.KNABBELS_NEEDED - 1, "every knabbel counts");
        helper.assertTrue(player.getMainHandItem().getCount() == 64 - (EilandenEvents.KNABBELS_NEEDED - 1), "and gets eaten");
        for (int i = 0; i < 50 && !guh.isTame(); i++) {
            player.interactOn(guh, InteractionHand.MAIN_HAND, guh.position());
        }
        helper.assertTrue(guh.isTame() && player.getUUID().equals(guh.getOwnerUUID()), "vads enough: tamed in the end");
        helper.assertTrue(GuhWorldData.get(player.level().getServer()).player(player.getUUID()).tamed.contains(GuhVariant.WOLK), "a Guhdex star");
    }

    /** The Wolkguh always has its little cloud on its head (a variant bone, not a hat: it stays when you undress it). */
    @GuhTest(template = EMPTY)
    public static void wolkguhHasACloudOnItsHead(GameTestHelper helper) {
        String bone = "wolk_wolkje";
        helper.assertTrue(GuhVariant.WOLK.shows(bone), "the Wolkguh should show its head cloud");
        helper.assertTrue(!GuhVariant.SNOW.shows(bone) && !GuhVariant.NORMAL.shows(bone), "only the Wolkguh has a head cloud");
        helper.assertTrue(GuhClothes.slotBones(GuhClothes.Slot.HEAD).stream().noneMatch(bone::startsWith),
                "a hat must not hide the head cloud");
        try (var in = Guhs.class.getResourceAsStream("/assets/guhs/geckolib/models/entity/guh.geo.json")) {
            helper.assertTrue(in != null && new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)
                    .contains("\"" + bone + "\""), "the guh model should have the head cloud bone");
        } catch (java.io.IOException e) {
            helper.fail("can't read the guh model: " + e);
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void wolkenstroomLiftsPuffsOffAndSinks(GameTestHelper helper) {
        BlockPos pad = helper.absolutePos(new BlockPos(2, 0, 2));
        BlockPos ceiling = pad.above(4);
        helper.getLevel().setBlockAndUpdate(ceiling, Blocks.STONE.defaultBlockState());
        BlockState up = EilandenFeature.WOLKENLIFT.get().defaultBlockState().setValue(WolkenliftBlock.FACING, Direction.EAST);
        helper.getLevel().setBlockAndUpdate(pad, up);
        helper.assertTrue(WolkenliftBlock.buildColumn(helper.getLevel(), pad, up) == 3, "a column up to the ceiling");
        BlockState stream = helper.getLevel().getBlockState(pad.above());
        helper.assertTrue(stream.is(EilandenFeature.WOLKENSTROOM.get()) && stream.getValue(WolkenstroomBlock.FACING) == Direction.EAST
                && !stream.getValue(WolkenstroomBlock.DOWN), "the stream follows its pad");
        helper.assertTrue(!WolkenstroomBlock.isTop(helper.getLevel(), pad.above()) && WolkenstroomBlock.isTop(helper.getLevel(), pad.above(3)), "top");

        ItemEntity item = new ItemEntity(helper.getLevel(), pad.getX() + 0.5, pad.getY() + 1.1, pad.getZ() + 0.5, new ItemStack(Items.FEATHER));
        item.setDeltaMovement(0.5, -0.5, 0);
        item.fallDistance = 10;
        WolkenstroomBlock.push(stream, helper.getLevel(), pad.above(), item);
        Vec3 v = item.getDeltaMovement();
        helper.assertTrue(v.y > 0.1 && Math.abs(v.x) <= WolkenstroomBlock.SIDEWAYS && item.fallDistance == 0, "lower part: up, no drifting off: " + v);
        WolkenstroomBlock.push(stream, helper.getLevel(), pad.above(3), item);
        v = item.getDeltaMovement();
        helper.assertTrue(v.x == WolkenstroomBlock.TOP_PUSH && v.y == WolkenstroomBlock.TOP_UP && v.z == 0, "the top puffs you off (east): " + v);
        BlockState down = stream.setValue(WolkenstroomBlock.DOWN, true);
        item.setDeltaMovement(0, -2, 0);
        WolkenstroomBlock.push(down, helper.getLevel(), pad.above(), item);
        helper.assertTrue(item.getDeltaMovement().y == -WolkenstroomBlock.SINK_SPEED, "a down stream: a soft float down");

        // the real thing: an item on the pad goes up and is puffed off to the east
        ItemEntity rider = new ItemEntity(helper.getLevel(), pad.getX() + 0.5, pad.getY() + 1.02, pad.getZ() + 0.5, new ItemStack(Items.FEATHER));
        rider.setDeltaMovement(Vec3.ZERO);
        rider.setPickUpDelay(1000);
        helper.getLevel().addFreshEntity(rider);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(rider.getX() > pad.getX() + 1.2, "puffed off east: " + rider.position());
            // break the pad: the column puffs away
            helper.getLevel().setBlockAndUpdate(pad, Blocks.AIR.defaultBlockState());
            helper.succeedWhen(() -> {
                for (int i = 1; i <= 3; i++) {
                    helper.assertTrue(!helper.getLevel().getBlockState(pad.above(i)).is(EilandenFeature.WOLKENSTROOM.get()), "stream gone at +" + i);
                }
                rider.discard();
                helper.getLevel().setBlockAndUpdate(ceiling, Blocks.AIR.defaultBlockState());
            });
        });
    }

    /** A wild Wolkguh can't be hurt (only tamed), it stays unique (its babies are snow guhs), and water can't wash a lift away. */
    @GuhTest(template = EMPTY)
    public static void wolkguhIsUntouchableAndLiftsAreWaterproof(GameTestHelper helper) {
        ServerPlayer player = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        try {
            player.setGameMode(GameType.SURVIVAL);
            GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(1, 1, 1));
            guh.setVariant(GuhVariant.WOLK);
            float health = guh.getHealth();
            guh.hurt(helper.getLevel().damageSources().playerAttack(player), 5);
            guh.hurt(helper.getLevel().damageSources().fall(), 5);
            helper.assertTrue(guh.getHealth() == health && guh.isAlive(), "a wild Wolkguh can't be hurt: " + guh.getHealth());
            guh.tame(player);
            guh.invulnerableTime = 0;
            health = guh.getHealth();
            guh.hurt(helper.getLevel().damageSources().playerAttack(player), 2);
            helper.assertTrue(guh.getHealth() < health, "a tamed one is a normal guh again");

            GuhEntity baby = ModEntities.GUH.get().create(helper.getLevel(), EntitySpawnReason.TRIGGERED);
            baby.setVariant(GuhVariant.WOLK);
            BabyEntitySpawnEvent birth = new BabyEntitySpawnEvent(guh, guh, baby);
            EilandenEvents.onBaby(birth);
            helper.assertTrue(baby.getVariant() == GuhVariant.SNOW, "only one Wolkguh: its baby is a snow guh");
            baby.discard();
        } finally {
            leave(helper, player);
        }

        BlockPos pad = helper.absolutePos(new BlockPos(3, 0, 3));
        helper.getLevel().setBlockAndUpdate(pad.above(4), Blocks.STONE.defaultBlockState());
        BlockState up = EilandenFeature.WOLKENLIFT.get().defaultBlockState();
        helper.getLevel().setBlockAndUpdate(pad, up);
        WolkenliftBlock.buildColumn(helper.getLevel(), pad, up);
        helper.getLevel().setBlockAndUpdate(pad.above(2).east(), Blocks.WATER.defaultBlockState());
        helper.runAfterDelay(30, () -> {
            for (int i = 1; i <= 3; i++) {
                helper.assertTrue(helper.getLevel().getBlockState(pad.above(i)).is(EilandenFeature.WOLKENSTROOM.get()),
                        "the stream is still there at +" + i + " (the water flows around it)");
            }
            helper.getLevel().setBlockAndUpdate(pad.above(2).east(), Blocks.AIR.defaultBlockState());
            helper.getLevel().setBlockAndUpdate(pad.above(4), Blocks.AIR.defaultBlockState());
            helper.succeed();
        });
    }

    @GuhTest(template = EMPTY)
    public static void yourWolkguhCatchesYou(GameTestHelper helper) {
        ServerPlayer owner = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        ServerPlayer other = nl.juiced.guhs.gametest.GuhMockPlayer.of(helper);
        try {
            owner.setGameMode(GameType.SURVIVAL);
            other.setGameMode(GameType.SURVIVAL);
            GuhEntity guh = helper.spawn(ModEntities.GUH.get(), new BlockPos(2, 1, 2));
            guh.setVariant(GuhVariant.WOLK);
            guh.tame(owner);
            guh.snapTo(owner.position());
            other.snapTo(owner.position());
            helper.assertTrue(EilandenEvents.ownWolkguhs(owner).size() == 1 && EilandenEvents.ownWolkguhs(other).isEmpty(), "only the owner's");
            helper.assertTrue(CommonHooks.onLivingFall(owner, 12, 1).getDamageMultiplier() == 0, "the owner is caught: no fall damage");
            helper.assertTrue(CommonHooks.onLivingFall(other, 12, 1).getDamageMultiplier() == 1, "someone else does get hurt");
            helper.assertTrue(CommonHooks.onLivingFall(owner, 2, 1).getDamageMultiplier() == 1, "(a tiny hop isn't a fall)");
            // the clouds of the islands: slow falling
            other.fallDistance = 8;
            EilandenEvents.catchFalling(other);
            helper.assertTrue(other.hasEffect(MobEffects.SLOW_FALLING) && other.fallDistance == 0, "the clouds catch a falling player");
        } finally {
            leave(helper, owner, other);
        }
        helper.succeed();
    }

    @GuhTest(template = EMPTY)
    public static void protectionCoversTheSkyAndTheLiftSquare(GameTestHelper helper) {
        BoundingBox piece = new BoundingBox(1000, 60, 2000, 1100, 151, 2100);   // the islands: 101 x 92 x 101
        helper.assertTrue(EilandenProtection.protectedPart(piece, new BlockPos(1010, 60 + EilandenProtection.ISLANDS_FROM, 2010)), "the sky part");
        helper.assertTrue(EilandenProtection.protectedPart(piece, new BlockPos(1050, 150, 2090)), "all the way up");
        helper.assertTrue(EilandenProtection.protectedPart(piece, new BlockPos(1050 - EilandenProtection.SQUARE_RADIUS, 61, 2050)), "the lift square");
        helper.assertTrue(!EilandenProtection.protectedPart(piece, new BlockPos(1010, 62, 2010)), "the ground far from the lifts is free");
        helper.assertTrue(!EilandenProtection.protectedPart(piece, new BlockPos(1050, 152, 2050)), "above the structure is free");
        helper.assertTrue(!EilandenProtection.protectedAt(helper.getLevel(), helper.absolutePos(BlockPos.ZERO)), "no islands here");
        helper.succeed();
    }

    /** The real structure: one wild Wolkguh in its outfit, two chests, and both lifts carry things up and softly down. */
    @GuhTest(template = ISLANDS, timeoutTicks = 700)
    public static void islandsHaveTheirWolkguhAndWorkingLifts(GameTestHelper helper) {
        var level = helper.getLevel();
        List<BlockPos> ups = new ArrayList<>(), downs = new ArrayList<>(), chests = new ArrayList<>();
        BlockPos.betweenClosedStream(helper.getBounds()).forEach(p -> {
            BlockState state = level.getBlockState(p);
            if (state.is(EilandenFeature.WOLKENLIFT.get())) {
                (state.getValue(WolkenliftBlock.DOWN) ? downs : ups).add(p.immutable());
            } else if (level.getBlockEntity(p) instanceof ChestBlockEntity) {
                chests.add(p.immutable());
            }
        });
        helper.assertTrue(ups.size() == 9 && downs.size() == 9, "two 3x3 lifts: " + ups.size() + " / " + downs.size());
        helper.assertTrue(chests.size() == 3, "two treasure chests and the wolkenkist (2.9): " + chests.size());
        List<GuhEntity> wolk = level.getEntitiesOfClass(GuhEntity.class, helper.getBounds(), g -> g.getVariant() == GuhVariant.WOLK);
        helper.assertTrue(wolk.size() == 1, "one Wolkguh: " + wolk.size());
        GuhEntity guh = wolk.get(0);
        helper.assertTrue(!guh.isTame() && guh.isPersistenceRequired(), "wild and here to stay");
        helper.assertTrue(guh.getClothes(GuhClothes.Slot.HEAD) == GuhClothes.WOLKENMUTS && guh.getClothes(GuhClothes.Slot.NECK) == GuhClothes.WOLKENKRAAG,
                "wearing its cloud hat and collar");
        BlockPos up = centre(ups), down = centre(downs);
        for (int y = 1; y <= TOP; y++) {
            helper.assertTrue(level.getBlockState(up.above(y)).is(EilandenFeature.WOLKENSTROOM.get())
                    && level.getBlockState(down.above(y)).is(EilandenFeature.WOLKENSTROOM.get()), "whole lift columns (y+" + y + ")");
        }
        ItemEntity riser = item(helper, Vec3.atBottomCenterOf(up.above()));
        ItemEntity sinker = item(helper, Vec3.atBottomCenterOf(down.above(TOP)));
        double[] fastest = {0};
        helper.onEachTick(() -> fastest[0] = Math.min(fastest[0], sinker.getDeltaMovement().y));
        helper.succeedWhen(() -> {
            helper.assertTrue(riser.onGround() && riser.getY() >= up.getY() + S0 + 0.9 && riser.getZ() > up.getZ() + 2,
                    "the up lift puts things on the pier: " + riser.position());
            helper.assertTrue(sinker.onGround() && sinker.getY() < down.getY() + 1.5, "the down lift brings things to the ground: " + sinker.position());
            helper.assertTrue(fastest[0] >= -WolkenstroomBlock.SINK_SPEED - 0.05, "softly: " + fastest[0]);
            riser.discard();
            sinker.discard();
        });
    }

    private static BlockPos centre(List<BlockPos> column) {
        int x = 0, y = 0, z = 0;
        for (BlockPos p : column) {
            x += p.getX();
            y += p.getY();
            z += p.getZ();
        }
        return new BlockPos(x / column.size(), y / column.size(), z / column.size());
    }

    private static ItemEntity item(GameTestHelper helper, Vec3 at) {
        ItemEntity item = new ItemEntity(helper.getLevel(), at.x, at.y + 0.02, at.z, new ItemStack(Items.FEATHER));
        item.setDeltaMovement(Vec3.ZERO);
        item.setPickUpDelay(10000);
        item.setUnlimitedLifetime();
        helper.getLevel().addFreshEntity(item);
        return item;
    }

    private static void leave(GameTestHelper helper, ServerPlayer... players) {
        for (ServerPlayer p : players) {
            helper.getLevel().removePlayerImmediately(p, Entity.RemovalReason.DISCARDED);
        }
    }
}
