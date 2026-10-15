package nl.juiced.guhs.feature.bio.bouwwolk2;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Onweerswolk: the dark cloud of the Bliksemsmidse. Now and then a small soft flash shows on one of its open sides, and
 * sometimes it rumbles softly. All of it on the client, for whoever is near: nothing strikes, nothing burns, nothing is
 * sent or saved.
 */
public class OnweerswolkBlock extends Block {
    /** One in this many looks at an open side flashes; one in {@link #ROMMEL_KANS} flashes rumbles. */
    public static final int FLITS_KANS = 220, ROMMEL_KANS = 4;

    public OnweerswolkBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction kant = Direction.getRandom(random);
        if (kant == Direction.UP || !level.getBlockState(pos.relative(kant)).isAir() || random.nextInt(FLITS_KANS) != 0) {
            return;
        }
        double x = pos.getX() + 0.5 + kant.getStepX() * 0.56 + (kant.getStepX() == 0 ? random.nextDouble() - 0.5 : 0);
        double y = pos.getY() + 0.5 + kant.getStepY() * 0.56 + (kant.getStepY() == 0 ? random.nextDouble() - 0.5 : 0);
        double z = pos.getZ() + 0.5 + kant.getStepZ() * 0.56 + (kant.getStepZ() == 0 ? random.nextDouble() - 0.5 : 0);
        level.addParticle(BouwWolk2Slice.FLITS.get(), x, y, z, 0, 0, 0);
        if (random.nextInt(ROMMEL_KANS) == 0) {
            level.playLocalSound(pos, BouwWolk2Slice.ROMMEL.get(), SoundSource.AMBIENT, 0.35f, 0.85f + random.nextFloat() * 0.3f, false);
        }
    }
}
