package nl.juiced.guhs.feature.gatenkaas;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.registry.ModSounds;

/**
 * A knabbelschreeuwer: a Mika alarm with a big wide-open mouth full of teeth (the sculk shrieker of the Stille
 * Voorraadkelder). When a knabbelsensor near it hears you, it screams "NJEEEG!" and counts a warning for you
 * ({@link Knabbelgeluid}). The third warning wakes the Vadswaker. Only the ones built by the Mika's themselves
 * (can_summon) can call him; one you place yourself only screams.
 */
public class KnabbelschreeuwerBlock extends Block {
    public static final MapCodec<KnabbelschreeuwerBlock> CODEC = simpleCodec(KnabbelschreeuwerBlock::new);
    public static final BooleanProperty SHRIEKING = BlockStateProperties.SHRIEKING;
    public static final BooleanProperty CAN_SUMMON = BlockStateProperties.CAN_SUMMON;
    /** How far from a sensor it listens, and how long a scream lasts (ticks). */
    public static final int RANGE = 8, SHRIEK_TICKS = 90;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);

    public KnabbelschreeuwerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(SHRIEKING, false).setValue(CAN_SUMMON, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHRIEKING, CAN_SUMMON);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();          // (placed by a player: it can't wake the Vadswaker)
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    /** Scream (if it isn't screaming already). */
    public static boolean shriek(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof KnabbelschreeuwerBlock) || state.getValue(SHRIEKING)) {
            return false;
        }
        level.setBlock(pos, state.setValue(SHRIEKING, true), 3);
        level.scheduleTick(pos, state.getBlock(), SHRIEK_TICKS);
        level.playSound(null, pos, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 2f, 1.25f);
        level.playSound(null, pos, ModSounds.MIKA_AMBIENT.get(), SoundSource.BLOCKS, 2f, 0.6f);
        for (int i = 0; i < 6; i++) {
            level.sendParticles(new ShriekParticleOption(i * 5), pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
        }
        return true;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(SHRIEKING)) {
            level.setBlock(pos, state.setValue(SHRIEKING, false), 3);
        }
    }
}
