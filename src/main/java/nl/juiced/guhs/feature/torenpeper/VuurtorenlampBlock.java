package nl.juiced.guhs.feature.torenpeper;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The lamp of the Rookguh-vuurtoren. Whether it burns is never saved or switched by hand: it burns ({@code lit}) while a
 * player who lit it in the questline (step 3 or further of {@link TorenpeperFeature#VUURTOREN}) is within
 * {@link Vuurtoren#BEREIK} blocks, and goes out when the last one leaves, so every new player finds a dark tower and
 * nothing in the world is ever used up. A click is the questline's "light the lamp" ({@link Vuurtoren#klikLamp}).
 * While it burns the client sweeps two beams of sparks round it.
 */
public class VuurtorenlampBlock extends BaseEntityBlock {
    public static final MapCodec<VuurtorenlampBlock> CODEC = simpleCodec(VuurtorenlampBlock::new);
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    /** One turn of the beams (ticks) and how far they reach (blocks). */
    public static final int RONDJE = 240, STRAAL = 22;
    private static final DustParticleOptions GLOED = new DustParticleOptions(0xFFE08A, 1.4f);

    public VuurtorenlampBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Lamp(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? createTickerHelper(type, TorenpeperFeature.VUURTORENLAMP_BE.get(), (l, pos, s, be) -> Lamp.clientTick(l, pos, s))
                : createTickerHelper(type, TorenpeperFeature.VUURTORENLAMP_BE.get(), (l, pos, s, be) -> Lamp.serverTick((ServerLevel) l, pos, s));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hit) {
        return hand == InteractionHand.MAIN_HAND ? InteractionResult.TRY_WITH_EMPTY_HAND : InteractionResult.PASS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer sp) {
            Vuurtoren.klikLamp(sp, pos);
        }
        return InteractionResult.SUCCESS;
    }

    /** The lamp's block entity: it holds nothing, it only ticks (the server decides lit or not, the client draws the beams). */
    public static class Lamp extends BlockEntity {
        public Lamp(BlockPos pos, BlockState state) {
            super(TorenpeperFeature.VUURTORENLAMP_BE.get(), pos, state);
        }

        static void serverTick(ServerLevel level, BlockPos pos, BlockState state) {
            if ((level.getGameTime() + pos.asLong()) % 20 != 0) {
                return;
            }
            Vuurtoren.meld(level, pos);
            boolean aan = Vuurtoren.moetBranden(level, pos);
            if (state.getValue(LIT) != aan) {
                level.setBlock(pos, state.setValue(LIT, aan), Block.UPDATE_ALL);
                if (aan) {
                    level.playSound(null, pos, TorenpeperFeature.LAMP_AAN.get(), SoundSource.BLOCKS, 1.5f, 1f);
                }
            }
        }

        /** Two beams of sparks, opposite each other, turning once every {@link #RONDJE} ticks; a warm glow round the lamp itself. */
        static void clientTick(Level level, BlockPos pos, BlockState state) {
            if (!state.getValue(LIT)) {
                return;
            }
            double x = pos.getX() + 0.5, y = pos.getY() + 0.5, z = pos.getZ() + 0.5;
            double hoek = (level.getGameTime() % RONDJE) * (Math.PI * 2 / RONDJE);
            double dx = Math.cos(hoek), dz = Math.sin(hoek);
            for (double r = 1.0; r <= STRAAL; r += 0.75) {
                // (the beams widen a little towards their ends)
                double w = r * 0.02;
                for (int kant = -1; kant <= 1; kant += 2) {
                    level.addAlwaysVisibleParticle(ParticleTypes.ELECTRIC_SPARK, true, x + kant * dx * r + (level.getRandom().nextDouble() - 0.5) * w,
                            y + (level.getRandom().nextDouble() - 0.5) * w, z + kant * dz * r + (level.getRandom().nextDouble() - 0.5) * w, 0, 0, 0);
                }
            }
            if (level.getGameTime() % 4 == 0) {
                level.addAlwaysVisibleParticle(GLOED, true, x + (level.getRandom().nextDouble() - 0.5) * 1.6, y + (level.getRandom().nextDouble() - 0.5) * 1.2,
                        z + (level.getRandom().nextDouble() - 0.5) * 1.6, 0, 0.01, 0);
            }
        }

        @Override
        public void setRemoved() {
            if (level instanceof ServerLevel server) {
                Vuurtoren.vergeet(server, worldPosition);
            }
            super.setRemoved();
        }
    }
}
