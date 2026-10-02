package nl.juiced.guhs.feature.guhoven;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The Guhoven block: a vanilla-style furnace block (FACING, LIT) whose block entity bakes on guh power (see {@link GuhovenFeature}). */
public class GuhOvenBlock extends AbstractFurnaceBlock {
    public static final MapCodec<GuhOvenBlock> CODEC = simpleCodec(GuhOvenBlock::new);
    private static final DustParticleOptions ROZE = new DustParticleOptions(0xFF73BF, 0.7f);

    public GuhOvenBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<GuhOvenBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GuhOvenBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level instanceof ServerLevel server
                ? createTickerHelper(type, GuhovenFeature.GUH_OVEN_BE.get(), (l, pos, s, be) -> GuhOvenBlockEntity.serverTick(server, pos, s, be))
                : null;
    }

    @Override
    protected void openContainer(Level level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof GuhOvenBlockEntity be) {
            player.openMenu((MenuProvider) be);
            player.awardStat(Stats.INTERACT_WITH_FURNACE);
        }
    }

    /** Guhdraad bends towards the oven (like redstone dust towards a lamp), so you can see it is connected. */
    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction != null;
    }

    /** Like the vanilla furnace while it bakes (flames, smoke, crackle), plus a pink guh sparkle now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;
        if (random.nextDouble() < 0.1) {
            level.playLocalSound(x, y, z, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }
        Direction facing = state.getValue(FACING);
        Direction.Axis axis = facing.getAxis();
        double side = random.nextDouble() * 0.6 - 0.3;
        double dx = axis == Direction.Axis.X ? facing.getStepX() * 0.52 : side;
        double dy = random.nextDouble() * 6.0 / 16.0;
        double dz = axis == Direction.Axis.Z ? facing.getStepZ() * 0.52 : side;
        level.addParticle(ParticleTypes.SMOKE, x + dx, y + dy, z + dz, 0.0, 0.0, 0.0);
        level.addParticle(ParticleTypes.FLAME, x + dx, y + dy, z + dz, 0.0, 0.0, 0.0);
        if (random.nextInt(4) == 0) {
            level.addParticle(ROZE, x + random.nextDouble() * 0.8 - 0.4, y + 1.05, z + random.nextDouble() * 0.8 - 0.4, 0.0, 0.0, 0.0);
        }
    }
}
