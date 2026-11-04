package nl.juiced.guhs.feature.vadsrijk.zone2.bazen.spiegel;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;

/**
 * A mirror on a stand (Spiegelburcht puzzles + Vrouwe Spiegelzucht's hall). Right-click turns it: stand 0 = "/" (seen from
 * above, north up), 1 = "\", 2 = folded flat (light passes straight on). {@link Lichtstraal} does the reflecting.
 */
public class SpiegelBlok extends Block {
    public static final MapCodec<SpiegelBlok> CODEC = simpleCodec(SpiegelBlok::new);
    public static final IntegerProperty STAND = IntegerProperty.create("stand", 0, 2);
    private static final VoxelShape VORM = Block.box(1, 0, 1, 15, 15, 15), PLAT = Block.box(1, 0, 1, 15, 5, 15);

    public SpiegelBlok(BlockBehaviour.Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(STAND, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(STAND);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return state.getValue(STAND) == 2 ? PLAT : VORM;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel sl && player instanceof ServerPlayer p) {
            int nieuw = (state.getValue(STAND) + 1) % 3;
            sl.setBlock(pos, state.setValue(STAND, nieuw), 3);
            sl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1f, 0.8f + nieuw * 0.3f);
            Tips.toon(p, "vadsrijk.z2bazen.tip.spiegel");
        }
        return InteractionResult.SUCCESS;
    }

    /** The stand of the mirror at pos, or -1 if there is none. */
    public static int stand(BlockGetter level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return s.getBlock() instanceof SpiegelBlok ? s.getValue(STAND) : -1;
    }
}
