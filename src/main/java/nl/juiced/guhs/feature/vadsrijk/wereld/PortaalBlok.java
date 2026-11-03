package nl.juiced.guhs.feature.vadsrijk.wereld;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Vadsrijk portal (a thin pane, like a nether portal). {@code wakker=false}: the sleeping portal of the ruined order
 * temple in the Guhmension (grey-purple star dust); touching it the first time wakes it (scene, the dragon roars far away)
 * and takes you in. Awake: straight in. Inside the Vadsrijk the same block takes you back to where you came from.
 * When its frame breaks, the portal goes out (like a nether portal). All logic: {@link Portalen}.
 */
public class PortaalBlok extends Block {
    public static final MapCodec<PortaalBlok> CODEC = simpleCodec(PortaalBlok::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final BooleanProperty WAKKER = BooleanProperty.create("wakker");
    private static final VoxelShape X = Block.box(0, 0, 6, 16, 16, 10), Z = Block.box(6, 0, 0, 10, 16, 16);

    public PortaalBlok(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X).setValue(WAKKER, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(AXIS, WAKKER);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.X ? X : Z;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!level.isClientSide() && entity instanceof ServerPlayer p) {
            Portalen.aanraking(p, pos, state);
        }
    }

    /** The frame broke (an in-plane neighbour is gone): the portal goes out, one block per tick. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir, BlockPos nPos,
                                     BlockState n, RandomSource random) {
        boolean inVlak = dir.getAxis() == Direction.Axis.Y || dir.getAxis() == state.getValue(AXIS);
        if (inVlak && n.isAir()) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        boolean wakker = state.getValue(WAKKER);
        int kleur = wakker ? ARGB.colorFromFloat(1f, 1f, 0.45f + random.nextFloat() * 0.3f, 0.8f)
                : ARGB.colorFromFloat(1f, 0.62f + random.nextFloat() * 0.2f, 0.58f, 0.72f);
        if (random.nextInt(wakker ? 1 : 3) == 0) {
            level.addParticle(new DustParticleOptions(kleur, wakker ? 1.1f : 0.7f), pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(), 0, 0.01, 0);
        }
        if (wakker && random.nextInt(8) == 0) {
            level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, (random.nextDouble() - 0.5) * 0.05, 0.02,
                    (random.nextDouble() - 0.5) * 0.05);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }

    @Override
    protected boolean canBeReplaced(BlockState state, Fluid fluid) {
        return false;
    }
}
