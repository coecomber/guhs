package nl.juiced.guhs.feature.beroepen;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.knuffeldal.KnuffeldalFeature;

/**
 * Snotkruid (the Apotheek's kruidentuin): a pale green herb with dewy droplets, good against snotneuzen. Grown
 * ({@link #AGE} 3) you pick a snotkruidje with a right-click; it grows back by itself.
 */
public class SnotkruidBlock extends VegetationBlock {   // 1.21.1 BushBlock (26.1 BushBlock is a concrete bonemealable bush)
    public static final MapCodec<SnotkruidBlock> CODEC = simpleCodec(SnotkruidBlock::new);
    public static final int MAX = 3;
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, MAX);

    public SnotkruidBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(AGE, MAX));
    }

    @Override
    protected MapCodec<? extends VegetationBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(BlockTags.DIRT) || state.getBlock() instanceof FarmlandBlock || state.is(KnuffeldalFeature.KNUFFELGRAS.get());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Block.box(3, 0, 3, 13, 4 + state.getValue(AGE) * 3, 13);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < MAX;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(AGE) < MAX && random.nextInt(3) == 0) {
            level.setBlock(pos, state.setValue(AGE, state.getValue(AGE) + 1), 2);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(AGE) < MAX) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer sp) {
            pluk((ServerLevel) level, pos, sp);
        }
        return InteractionResult.SUCCESS;
    }

    /** Picks a snotkruidje (the plant starts over): true when there was one. */
    public static boolean pluk(ServerLevel level, BlockPos pos, ServerPlayer player) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SnotkruidBlock) || state.getValue(AGE) < MAX) {
            return false;
        }
        level.setBlock(pos, state.setValue(AGE, 0), 2);
        Minigames.give(player, new ItemStack(BeroepenFeature.SNOTKRUIDJE.get()));
        level.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 0.8f, 1.3f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0.02);
        return true;
    }
}
