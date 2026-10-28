package nl.juiced.guhs.feature.zingendeweide;

import java.util.List;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.feature.blub.BlubGeluid;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;

/**
 * The little stage in the middle of the Koorkom. At dusk the wild Blubs around (40 blocks) hop to it, and once three of them
 * (or after half a minute, however many came) stand around it they squeak their lullaby together: the Blub lullaby
 * ({@code blubguh.liedje}, one stream), once per evening per Koorkom, so it never stacks. Notes rise over the singers for as
 * long as the song lasts.
 */
public class KoorkomPodium extends BaseEntityBlock {
    public static final MapCodec<KoorkomPodium> CODEC = simpleCodec(KoorkomPodium::new);
    /** Dusk: the part of the day the choir gathers (ticks of the day clock). */
    public static final int SCHEMER_VAN = 11800, SCHEMER_TOT = 13600;
    /** How long the song lasts (the stream is ~24 s). */
    public static final int LIED_TICKS = 24 * 20;

    public KoorkomPodium(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ZingendeWeideFeature.KOORKOM_BE.get(), (l, p, s, be) -> be.tick((ServerLevel) l, p));
    }

    public static boolean schemer(Level level) {
        long t = Math.floorMod(level.getDefaultClockTime(), 24000L);
        return t >= SCHEMER_VAN && t <= SCHEMER_TOT;
    }

    public static class Entity extends BlockEntity {
        /** The day (clock / 24000) of the last song, and the ticks left of the one now (0: quiet). */
        long gezongen = Long.MIN_VALUE;
        int lied;
        int wachten;

        public Entity(BlockPos pos, BlockState state) {
            super(ZingendeWeideFeature.KOORKOM_BE.get(), pos, state);
        }

        void tick(ServerLevel level, BlockPos pos) {
            if (lied > 0) {
                lied--;
                if (lied % 10 == 0) {
                    for (BlubGuhEntity b : zangers(level, pos, 9)) {
                        level.sendParticles(ParticleTypes.NOTE, b.getX(), b.getY() + b.getBbHeight() + 0.4, b.getZ(), 1, 0, 0, 0, 0);
                    }
                }
                return;
            }
            if (level.getGameTime() % 20 != 0) {
                return;
            }
            long dag = Math.floorDiv(level.getDefaultClockTime(), 24000L);
            if (!schemer(level) || dag == gezongen) {
                wachten = 0;
                return;
            }
            List<BlubGuhEntity> wild = level.getEntitiesOfClass(BlubGuhEntity.class, new AABB(pos).inflate(40), b -> !b.isTame() && !b.isPassenger());
            if (wild.isEmpty()) {
                return;
            }
            for (BlubGuhEntity b : wild) {
                if (b.distanceToSqr(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5) > 25) {
                    b.getNavigation().moveTo(pos.getX() + 0.5 + (b.getRandom().nextDouble() - 0.5) * 6, pos.getY() + 1,
                            pos.getZ() + 0.5 + (b.getRandom().nextDouble() - 0.5) * 6, 1.0);
                }
            }
            wachten++;
            if (zangers(level, pos, 7).size() >= 3 || wachten >= 30) {
                zing(level, pos, dag);
            }
        }

        /** The song: once, for everyone near (the stream carries 16 blocks); the singers' ears up and notes over them. */
        public void zing(ServerLevel level, BlockPos pos, long dag) {
            gezongen = dag;
            lied = LIED_TICKS;
            wachten = 0;
            level.playSound(null, pos, BlubGeluid.LIEDJE.get(), SoundSource.NEUTRAL, 1.4f, 1.0f);
            for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(20))) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.weide.koor"));
            }
            setChanged();
        }

        static List<BlubGuhEntity> zangers(ServerLevel level, BlockPos pos, double straal) {
            return level.getEntitiesOfClass(BlubGuhEntity.class, new AABB(pos).inflate(straal), b -> !b.isTame());
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            output.putLong("gezongen", gezongen);
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            gezongen = input.getLongOr("gezongen", Long.MIN_VALUE);
        }
    }
}
