package nl.juiced.guhs.feature.droomzand;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 1.10.10: a little heap of droomzand on a dream island (three per island, placed by {@link Zandhoopjes}). Unbreakable deco;
 * right-click it in a dream and you scoop {@value Zandhoopjes#MIN}-{@value Zandhoopjes#MAX} droomzand out of it, once per
 * dream per heap. A heap you can still scoop sparkles for you (the server sends those sparkles to you alone).
 */
public class Zandhoopje extends Block {
    public static final MapCodec<Zandhoopje> CODEC = simpleCodec(Zandhoopje::new);
    static final VoxelShape VORM = Shapes.or(Block.box(1, 0, 1, 15, 3, 15), Block.box(3, 3, 3, 13, 5, 13), Block.box(5, 5, 5, 11, 7, 11));

    public Zandhoopje(Properties p) {
        super(p);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return VORM;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            Zandhoopjes.klik(sp, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** A soft glitter now and then (for everyone; the bright "still full" sparkles are per player, from the server). */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.WAX_OFF, pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.35 + random.nextDouble() * 0.2,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0, 0.02, 0);
        }
    }
}
