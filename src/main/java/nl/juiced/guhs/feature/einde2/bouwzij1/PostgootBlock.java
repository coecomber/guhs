package nl.juiced.guhs.feature.einde2.bouwzij1;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;

/**
 * The postgoot: a block of the mail chutes of Omashguh. Whoever stands on it (a parcel, a guh, a player) slides on in the
 * direction the block faces and is nudged to the middle of the trough, so a row of them is a slide you can really ride:
 * round corners and down steps of one block. Sneaking is braking: a sneaking rider is left alone and can step out.
 * <p>
 * The push is done on the side that moves the entity (a player: their own client), so nobody rubber-bands.
 * The chutes themselves are built by tools/features/einde2_bouw_zij1_omashguh.py, which also rides every one of them from
 * its first block to its last (gereedschap.glijcheck) by the same rule as {@link #duw}.
 */
public class PostgootBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<PostgootBlock> CODEC = simpleCodec(PostgootBlock::new);
    /** Blocks per tick along the chute (after the friction of the ice a little less is left). */
    public static final double SNELHEID = 0.45;
    /** How hard a rider is pulled to the middle of the trough, and the most that pull may be (blocks per tick). */
    private static final double NAAR_MIDDEN = 0.3, MAX_DWARS = 0.15;

    public PostgootBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** Placed by hand it slides away from you: the way you look. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        duw(state, pos, entity);
        super.stepOn(level, pos, state, entity);
    }

    /** May a chute push this entity right now? (Not while it sneaks, rides something or only looks on.) */
    public static boolean glijdt(Entity entity) {
        return !entity.isSteppingCarefully() && !entity.isPassenger() && !entity.isSpectator() && !entity.noPhysics;
    }

    /** One tick of sliding: on along the facing at {@link #SNELHEID}, sideways only towards the middle of the block. */
    public static void duw(BlockState state, BlockPos pos, Entity entity) {
        if (!glijdt(entity)) {
            return;
        }
        Direction d = state.getValue(FACING);
        Vec3 v = entity.getDeltaMovement();
        boolean langsX = d.getAxis() == Direction.Axis.X;
        double uitMidden = langsX ? pos.getZ() + 0.5 - entity.getZ() : pos.getX() + 0.5 - entity.getX();
        double dwars = Mth.clamp(uitMidden * NAAR_MIDDEN, -MAX_DWARS, MAX_DWARS);
        entity.setDeltaMovement(langsX ? d.getStepX() * SNELHEID : dwars, v.y, langsX ? dwars : d.getStepZ() * SNELHEID);
        entity.needsSync = true;
        entity.resetFallDistance();
    }

    /** Does this entity stand on a chute block? (hook fase B: the question the slide race asks.) */
    public static boolean opGoot(Entity entity) {
        return entity.onGround() && entity.level().getBlockState(entity.getOnPosLegacy()).getBlock() instanceof PostgootBlock;
    }
}
