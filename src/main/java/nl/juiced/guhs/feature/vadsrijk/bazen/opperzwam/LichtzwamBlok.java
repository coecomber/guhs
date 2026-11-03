package nl.juiced.guhs.feature.vadsrijk.bazen.opperzwam;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;

/**
 * A Lichtzwam (light mushroom): right-click to light it. Used by the Lichtzwamkamer puzzle, the Hendelkamer hints and the
 * Opperzwam's dark phase. Listeners in {@link #AANGESTOKEN} decide what a lighting means (they may put it out again).
 */
public class LichtzwamBlok extends Block {
    public static final MapCodec<LichtzwamBlok> CODEC = simpleCodec(LichtzwamBlok::new);
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape VORM = Block.box(2, 0, 2, 14, 14, 14);

    @FunctionalInterface
    public interface Aansteker {
        void aangestoken(ServerLevel sl, BlockPos pos, ServerPlayer p);
    }

    public static final List<Aansteker> AANGESTOKEN = new CopyOnWriteArrayList<>();

    public LichtzwamBlok(BlockBehaviour.Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(LIT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return VORM;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer p)) {
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(LIT)) {
            return InteractionResult.SUCCESS;
        }
        zet(sl, pos, true);
        Tips.toon(p, "vadsrijk.bazen.tip.lichtzwam");
        for (Aansteker a : AANGESTOKEN) {
            a.aangestoken(sl, pos, p);
        }
        return InteractionResult.SUCCESS;
    }

    /** Lights or douses the mushroom at pos (only if one is there). */
    public static void zet(ServerLevel sl, BlockPos pos, boolean aan) {
        BlockState s = sl.getBlockState(pos);
        if (!(s.getBlock() instanceof LichtzwamBlok) || s.getValue(LIT) == aan) {
            return;
        }
        sl.setBlock(pos, s.setValue(LIT, aan), 3);
        if (aan) {
            sl.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.02);
            sl.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1f, 1.4f);
        } else {
            sl.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.01);
        }
    }

    public static boolean aan(ServerLevel sl, BlockPos pos) {
        BlockState s = sl.getBlockState(pos);
        return s.getBlock() instanceof LichtzwamBlok && s.getValue(LIT);
    }
}
