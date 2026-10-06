package nl.juiced.guhs.feature.bestaand;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A bridge fire of the Spiesburcht: an iron bowl of coals on a short stem (it stands on a carved pedestal, template
 * bestaand_brugvuur_&lt;nr&gt;). {@code nr} says which bridge it belongs to (0..3: a player's flag {@code vuur_<nr>}).
 * <p>
 * In the world it is ALWAYS out. {@code lit=true} is only ever sent to the player who lit it ({@link Schijn}): flames,
 * light and crackling for them alone, while the next player still finds it cold and can light it too. It is a quest prop:
 * no item, no drops, not broken in survival. All the rules are in {@link Vuren}.
 */
public class VuurkorfBlock extends Block implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final IntegerProperty NR = IntegerProperty.create("nr", 0, 3);
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 12, 14);

    public VuurkorfBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(NR, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT, NR);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VuurkorfBlockEntity(pos, state);
    }

    /** Any click on the bowl is about the fire: with something that lights it, or else a word about what it wants. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        boolean aansteker = stack.is(BestaandFeature.AANSTEKERS);
        if (!aansteker && hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer p) {
            Vuren.klik(p, pos, state, aansteker);
            Schijn.straks(p);   // (a click on a bowl that burns for this player: the game answers with the cold one)
        }
        return InteractionResult.SUCCESS;
    }

    /** Lit (which only the player who lit it ever sees): flames, a wisp of smoke, sparks and a crackle, like a campfire. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5, y = pos.getY() + 0.62, z = pos.getZ() + 0.5;
        if (random.nextInt(10) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.5f + random.nextFloat(), random.nextFloat() * 0.7f + 0.6f, false);
        }
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.FLAME, x + (random.nextDouble() - 0.5) * 0.5, y + random.nextDouble() * 0.2, z + (random.nextDouble() - 0.5) * 0.5,
                    0, 0.02 + random.nextDouble() * 0.02, 0);
        }
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x + (random.nextDouble() - 0.5) * 0.4, y + 0.7, z + (random.nextDouble() - 0.5) * 0.4, 0, 0.05, 0);
        }
        if (random.nextInt(12) == 0) {
            level.addParticle(ParticleTypes.LAVA, x, y + 0.1, z, 0, 0, 0);
        }
    }
}
