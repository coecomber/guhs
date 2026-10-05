package nl.juiced.guhs.feature.guhpixel.lobby;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A hidden golden knabbel of the Guhpixel lobby ({@code guhs:lobby_gouden_knabbel}, {@code nummer} 0..9). It is never
 * taken away: every player who right-clicks it finds it once for themselves ({@link Knabbels#pak}). Unbreakable, no item.
 */
public class GoudenKnabbelBlock extends Block {
    public static final MapCodec<GoudenKnabbelBlock> CODEC = simpleCodec(GoudenKnabbelBlock::new);
    public static final IntegerProperty NUMMER = IntegerProperty.create("nummer", 0, Knabbels.AANTAL - 1);
    private static final VoxelShape VORM = Block.box(4, 0, 4, 12, 7, 12);

    public GoudenKnabbelBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(NUMMER, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NUMMER);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            Knabbels.pak(sp, pos, state.getValue(NUMMER));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.WAX_ON, pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() + 0.3 + random.nextDouble() * 0.4,
                    pos.getZ() + 0.25 + random.nextDouble() * 0.5, 0, 0.02, 0);
        }
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return ItemStack.EMPTY;
    }
}
