package nl.juiced.guhs.feature.mijnhulp;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.juiced.guhs.Guhs;

/**
 * 1.7.0 (ronde 44): the Wolkenliftje, an elevator for your mine. Stand on one and jump: you float up to the next
 * Wolkenliftje straight above you (at most {@link #MAX} blocks, with two free blocks above it to stand in); sneak on it to
 * go down to the next one below. The client notices the jump / the sneak (the keys are client-side,
 * client.MijnhulpClient) and asks the server ({@link Rit}), which checks everything again and moves you, in a little pink
 * cloud: a trail of pink puffs along the shaft, a soft poof at both ends. It is a jump of the player, not a slow ride (the
 * rock between two floors would be in the way), so it works through solid ground as well.
 */
public final class Wolkenliftje {
    /** How far the next Wolkenliftje may be. */
    public static final int MAX = 256;
    /** Ticks between two rides of one player. */
    private static final int PAUZE = 8;
    private static final Map<UUID, Long> LAATST = new HashMap<>();
    private static final DustParticleOptions ROZE = new DustParticleOptions(0xFFB3DA, 1.6f);

    /** The Wolkenliftje under {@code e}'s feet (a little slack: it may have just jumped off it), or null. */
    @Nullable
    public static BlockPos onder(Entity e) {
        for (double dy : new double[]{0.1, 0.6, 1.2}) {
            BlockPos p = BlockPos.containing(e.getX(), e.getY() - dy, e.getZ());
            if (e.level().getBlockState(p).is(MijnhulpFeature.WOLKENLIFTJE.get())) {
                return p;
            }
        }
        return null;
    }

    /** The next Wolkenliftje above (or below) {@code van} with room to stand on it, or null. */
    @Nullable
    public static BlockPos doel(Level level, BlockPos van, boolean omhoog) {
        for (int d = 1; d <= MAX; d++) {
            BlockPos q = van.above(omhoog ? d : -d);
            if (q.getY() < level.getMinY() || q.getY() > level.getMaxY() || !level.isLoaded(q)) {
                return null;
            }
            if (level.getBlockState(q).is(MijnhulpFeature.WOLKENLIFTJE.get()) && vrij(level, q.above()) && vrij(level, q.above(2))) {
                return q;
            }
        }
        return null;
    }

    private static boolean vrij(BlockGetter level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        return s.getCollisionShape(level, p).isEmpty() && !s.getFluidState().is(net.minecraft.tags.FluidTags.LAVA);
    }

    /** Moves the player (server). False: not on a Wolkenliftje, nothing in that direction, or too soon. */
    public static boolean rijd(ServerPlayer p, boolean omhoog) {
        if (!p.isAlive() || p.isSpectator() || p.isPassenger() || p.isSleeping()) {
            return false;
        }
        ServerLevel level = p.level();
        long nu = level.getGameTime();
        Long vorig = LAATST.get(p.getUUID());
        if (vorig != null && nu - vorig < PAUZE && nu >= vorig) {
            return false;
        }
        BlockPos van = onder(p);
        if (van == null) {
            return false;
        }
        BlockPos naar = doel(level, van, omhoog);
        if (naar == null) {
            return false;
        }
        LAATST.put(p.getUUID(), nu);
        double x = naar.getX() + 0.5, y = naar.getY() + 1.0, z = naar.getZ() + 0.5;
        wolkje(level, van.getX() + 0.5, van.getY() + 1.0, van.getZ() + 0.5, x, y, z);
        p.teleportTo(x, y, z);
        p.setDeltaMovement(0, omhoog ? 0.15 : 0, 0);
        p.hurtMarked = true;
        p.resetFallDistance();
        level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.9f, omhoog ? 1.5f : 1.1f);
        return true;
    }

    /** The pink cloud: a puff at both ends and a trail of pink dust along the way. */
    private static void wolkje(ServerLevel level, double x0, double y0, double z0, double x1, double y1, double z1) {
        level.playSound(null, x0, y0, z0, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 1.0f, 1.4f);
        level.sendParticles(ParticleTypes.CLOUD, x0, y0 + 0.2, z0, 10, 0.3, 0.1, 0.3, 0.02);
        level.sendParticles(ParticleTypes.CLOUD, x1, y1 + 0.2, z1, 10, 0.3, 0.1, 0.3, 0.02);
        int n = (int) Math.min(48, Math.abs(y1 - y0) * 2);
        for (int i = 0; i <= n; i++) {
            double f = n == 0 ? 0 : i / (double) n;
            level.sendParticles(ROZE, x0, y0 + 0.5 + (y1 - y0) * f, z0, 2, 0.25, 0.2, 0.25, 0);
        }
    }

    /** Client to server: "I jumped (true) / sneaked (false) on a Wolkenliftje". */
    public record Rit(boolean omhoog) implements CustomPacketPayload {
        public static final Type<Rit> TYPE = new Type<>(Guhs.id("mijnhulp_wolkenliftje"));
        public static final StreamCodec<ByteBuf, Rit> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, Rit::omhoog, Rit::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Rit r, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer p) {
                rijd(p, r.omhoog());
            }
        }
    }

    private Wolkenliftje() {
    }
}
