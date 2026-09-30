package nl.juiced.guhs.feature.guhwaii;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.piep.PiepFeature;
import nl.juiced.guhs.feature.piep.PoepschillyEntity;

/**
 * Schilly-eitjes: 1-4 round turtle eggs (pale green with little dark-green spots) in the warm beach sand of Guhwai'i. They
 * slowly get ready ({@link GuhwaiiBlokken#RIJP} 0..2, faster at night, like vanilla turtle eggs), wiggle, and then hatch:
 * every egg becomes a baby Poepschilly or Schilly (from the piep feature), wild, which grows up in about twenty minutes and
 * can be tamed like its parents. Unlike vanilla turtle eggs they never break when you walk on them (lief!).
 */
public class SchillyEitjesBlock extends Block {
    public static final MapCodec<SchillyEitjesBlock> CODEC = simpleCodec(SchillyEitjesBlock::new);
    private static final VoxelShape EEN = Block.box(3, 0, 3, 12, 7, 12), MEER = Block.box(1, 0, 1, 15, 7, 15);
    /** How far players see the hatching (message + advancement). */
    public static final int ZIEN = 16;

    public SchillyEitjesBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(GuhwaiiBlokken.EITJES, 1).setValue(GuhwaiiBlokken.RIJP, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GuhwaiiBlokken.EITJES, GuhwaiiBlokken.RIJP);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(GuhwaiiBlokken.EITJES) > 1 ? MEER : EEN;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState al = context.getLevel().getBlockState(context.getClickedPos());
        if (al.is(this)) {
            return al.setValue(GuhwaiiBlokken.EITJES, Math.min(4, al.getValue(GuhwaiiBlokken.EITJES) + 1));
        }
        return super.getStateForPlacement(context);
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        return !context.isSecondaryUseActive() && context.getItemInHand().is(asItem()) && state.getValue(GuhwaiiBlokken.EITJES) < 4
                || super.canBeReplaced(state, context);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState onder = level.getBlockState(pos.below());
        return onder.is(BlockTags.SAND) || onder.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction dir, BlockState other, LevelAccessor level, BlockPos pos, BlockPos otherPos) {
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /** Only in sand they get ready (warm!); at night more often. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockState(pos.below()).is(BlockTags.SAND)) {
            return;
        }
        if (random.nextInt(level.isNight() ? 6 : 18) == 0) {
            broed(level, pos, state);
        }
    }

    /**
     * One step closer to hatching (a crack: sound and a few shell crumbs); at the last step they hatch. Returns the babies
     * that hatched now (empty while they're still getting ready).
     */
    public static List<AgeableMob> broed(ServerLevel level, BlockPos pos, BlockState state) {
        int rijp = state.getValue(GuhwaiiBlokken.RIJP);
        if (rijp < 2) {
            level.setBlock(pos, state.setValue(GuhwaiiBlokken.RIJP, rijp + 1), Block.UPDATE_CLIENTS);
            level.playSound(null, pos, SoundEvents.TURTLE_EGG_CRACK, SoundSource.BLOCKS, 0.7f, 1.1f + level.getRandom().nextFloat() * 0.2f);
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                    6, 0.2, 0.1, 0.2, 0.05);
            return List.of();
        }
        return komUit(level, pos, state);
    }

    /** They hatch: a baby Poepschilly or Schilly per egg, a little shell burst, and everybody nearby sees it. */
    public static List<AgeableMob> komUit(ServerLevel level, BlockPos pos, BlockState state) {
        int n = state.getValue(GuhwaiiBlokken.EITJES);
        level.removeBlock(pos, false);
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_HATCH, SoundSource.BLOCKS, 0.8f, 1.1f);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                18, 0.3, 0.15, 0.3, 0.08);
        level.sendParticles(ParticleTypes.HEART, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 3, 0.4, 0.2, 0.4, 0.02);
        List<AgeableMob> babies = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            PoepschillyEntity baby = (level.getRandom().nextBoolean() ? PiepFeature.SCHILLY : PiepFeature.POEPSCHILLY).get().create(level, EntitySpawnReason.TRIGGERED);
            if (baby == null) {
                continue;
            }
            baby.setAge(-24000);
            baby.snapTo(pos.getX() + 0.3 + level.getRandom().nextDouble() * 0.4, pos.getY(), pos.getZ() + 0.3 + level.getRandom().nextDouble() * 0.4,
                    level.getRandom().nextFloat() * 360f, 0f);
            baby.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.BREEDING, null);
            baby.setAge(-24000);
            level.addFreshEntity(baby);
            babies.add(baby);
        }
        for (ServerPlayer p : level.players()) {
            if (p.blockPosition().closerThan(pos, ZIEN)) {
                nl.juiced.guhs.quest.GuhQuests.hint(p, "gui.guhs.guhwaii.eitjes_uit");
                GuhwaiiFeature.advancement(p, "eitjes");
            }
        }
        return babies;
    }
}
