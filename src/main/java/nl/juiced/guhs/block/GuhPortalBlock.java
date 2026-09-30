package nl.juiced.guhs.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.world.GuhPortalForcer;
import nl.juiced.guhs.world.GuhPortalShape;
import org.joml.Vector3f;

/** The swirly pink portal inside a Block-of-Kaasknabbels frame. Overworld (or anywhere) <-> Guhmension. */
public class GuhPortalBlock extends Block implements Portal {
    public static final MapCodec<GuhPortalBlock> CODEC = simpleCodec(GuhPortalBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    /** Ticks a player has to stand in the portal (nether portal: 80). */
    public static final int PLAYER_TRAVEL_TICKS = 30;

    private static final VoxelShape X_AXIS_AABB = Block.box(0, 0, 6, 16, 16, 10);
    private static final VoxelShape Z_AXIS_AABB = Block.box(6, 0, 0, 10, 16, 16);
    private static final DustParticleOptions PINK = new DustParticleOptions(new Vector3f(1.0f, 0.6f, 0.75f), 1.0f);
    private static final DustParticleOptions CHEESE = new DustParticleOptions(new Vector3f(1.0f, 0.62f, 0.15f), 1.0f);

    public GuhPortalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.Z ? Z_AXIS_AABB : X_AXIS_AABB;
    }

    /** Breaks (chain reaction) as soon as the frame is no longer complete. */
    @Override
    protected BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos pos, BlockPos facingPos) {
        Direction.Axis portalAxis = state.getValue(AXIS);
        boolean sideways = facing.getAxis() != portalAxis && facing.getAxis().isHorizontal();
        if (!sideways && !facingState.is(this) && !new GuhPortalShape(level, pos, portalAxis).isComplete()) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, facing, facingState, level, pos, facingPos);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return entity instanceof Player player && !player.getAbilities().invulnerable ? PLAYER_TRAVEL_TICKS : 1;
    }

    @Nullable
    @Override
    public DimensionTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        return GuhPortalForcer.getDestination(level, entity, pos);
    }

    @Override
    public Transition getLocalTransition() {
        return Transition.CONFUSION;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(100) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.PORTAL_AMBIENT,
                    SoundSource.BLOCKS, 0.4f, random.nextFloat() * 0.3f + 1.3f, false);
        }
        for (int i = 0; i < 2; i++) {
            double x = pos.getX() + random.nextDouble();
            double y = pos.getY() + random.nextDouble();
            double z = pos.getZ() + random.nextDouble();
            level.addParticle(random.nextInt(3) == 0 ? CHEESE : PINK, x, y, z, 0, 0.02, 0);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
