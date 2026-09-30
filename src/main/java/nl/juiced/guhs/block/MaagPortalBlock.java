package nl.juiced.guhs.block;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.world.MaagManager;
import org.joml.Vector3f;

/**
 * The fleshy portals in the guh stomachs: "to the mouth" and "to the intestines" in every stomach, a portal per
 * stomach in the mouth, and the way back to the world in the mouth. Where they lead: {@link MaagManager}.
 */
public class MaagPortalBlock extends Block implements Portal {
    public static final MapCodec<MaagPortalBlock> CODEC = simpleCodec(MaagPortalBlock::new);

    public enum Kind implements StringRepresentable {
        /** In a stomach: to the mouth (the public lobby). */
        MOND,
        /** In a stomach: back to where you came from in the world. */
        DARM,
        /** In the mouth: into one player's stomach (which one follows from where the portal stands). */
        MAAG,
        /** In the mouth: back to the world. */
        EXIT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final EnumProperty<Kind> KIND = EnumProperty.create("kind", Kind.class);
    private static final VoxelShape SHAPE = Block.box(0, 0, 6, 16, 16, 10);
    private static final DustParticleOptions FLESH = new DustParticleOptions(new Vector3f(0.95f, 0.35f, 0.5f), 1.0f);

    public MaagPortalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(KIND, Kind.MOND));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KIND);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (entity instanceof Player && entity.canUsePortal(false)) {
            entity.setAsInsidePortal(this, pos);
        }
    }

    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return 10;
    }

    @Nullable
    @Override
    public DimensionTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        return MaagManager.portalDestination(level, entity, pos, level.getBlockState(pos).getValue(KIND));
    }

    @Override
    public Transition getLocalTransition() {
        return Transition.NONE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 2; i++) {
            level.addParticle(FLESH, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + 0.5, 0, 0.03, 0);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return ItemStack.EMPTY;
    }
}
