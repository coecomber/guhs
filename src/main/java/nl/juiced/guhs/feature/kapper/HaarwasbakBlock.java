package nl.juiced.guhs.feature.kapper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.block.GuhFurnitureBlock;

/** The haarwasbak: a pink basin with a golden tap. Right-click: the tap runs and the basin fills with foam. */
public class HaarwasbakBlock extends GuhFurnitureBlock {
    public HaarwasbakBlock(Properties properties) {
        super(properties, -1, new double[]{5, 0, 5, 11, 9, 11}, new double[]{3, 0, 3, 13, 1, 13}, new double[]{1, 9, 1, 15, 15, 15},
                new double[]{7, 15, 12, 9, 20, 14});
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            schuim(server, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** Water from the tap, bubbles and foam in the basin. */
    public static void schuim(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.6f, 1.4f);
        level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.8f, 1.2f);
        level.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.25, 0.05, 0.25, 0.1);
        level.sendParticles(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 10, 0.3, 0.1, 0.3, 0.02);
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 4, 0.25, 0.02, 0.25, 0.0);
    }
}
