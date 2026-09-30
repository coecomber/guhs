package nl.juiced.guhs.feature.gatenkaas;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;

/**
 * A knabbelsensor: a little lump of old cheese with two big listening guh ears (the sculk sensor of the Stille
 * Voorraadkelder). It hears anyone within {@link #RANGE} blocks walk, eat or dig ({@link Knabbelgeluid}): its ears
 * light up and it tells the knabbelschreeuwers around it. Sneaking past is fine; with guhs:stil it hears nothing at all.
 */
public class KnabbelsensorBlock extends Block {
    public static final MapCodec<KnabbelsensorBlock> CODEC = simpleCodec(KnabbelsensorBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    /** How far it hears, and how long the ears stay lit (ticks). */
    public static final int RANGE = 8, ACTIVE_TICKS = 30;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 8, 16);

    public KnabbelsensorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ACTIVE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    /** Heard something: ears up and glowing for a moment. Returns false when it was still busy listening. */
    public static boolean activate(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof KnabbelsensorBlock) || state.getValue(ACTIVE)) {
            return false;
        }
        level.setBlock(pos, state.setValue(ACTIVE, true), 3);
        level.scheduleTick(pos, state.getBlock(), ACTIVE_TICKS);
        level.playSound(null, pos, SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 1f, 1.35f);
        level.sendParticles(new DustParticleOptions(new Vector3f(1f, 0.82f, 0.3f), 1f), pos.getX() + 0.5, pos.getY() + 0.8,
                pos.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.01);
        return true;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE)) {
            level.setBlock(pos, state.setValue(ACTIVE, false), 3);
            level.playSound(null, pos, SoundEvents.SCULK_CLICKING_STOP, SoundSource.BLOCKS, 1f, 1.35f);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(ACTIVE) && random.nextInt(3) == 0) {
            level.addParticle(new DustParticleOptions(new Vector3f(1f, 0.85f, 0.35f), 0.8f), pos.getX() + 0.2 + random.nextDouble() * 0.6,
                    pos.getY() + 0.9, pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.02, 0);
        }
    }
}
