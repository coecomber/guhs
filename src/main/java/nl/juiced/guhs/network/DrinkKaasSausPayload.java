package nl.juiced.guhs.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.registry.ModFluids;

/**
 * Client -> server: "I right-clicked kaas saus with an empty hand". The server checks it (the player really is
 * looking at kaas saus close by) and fills the hunger bar. The sauce itself is not used up.
 */
public record DrinkKaasSausPayload() implements CustomPacketPayload {
    public static final DrinkKaasSausPayload INSTANCE = new DrinkKaasSausPayload();
    public static final Type<DrinkKaasSausPayload> TYPE = new Type<>(Guhs.id("drink_kaas_saus"));
    public static final StreamCodec<ByteBuf, DrinkKaasSausPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Is the player looking at kaas saus within reach? Used on both sides. */
    public static BlockPos lookedAtSaus(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1f).scale(player.blockInteractionRange()));
        BlockHitResult hit = player.level().clip(new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
        if (hit.getType() == HitResult.Type.BLOCK && player.level().getFluidState(hit.getBlockPos()).getFluidType() == ModFluids.KAAS_SAUS_TYPE.get()) {
            return hit.getBlockPos();
        }
        return null;
    }

    public static void handle(DrinkKaasSausPayload payload, IPayloadContext context) {
        Player player = context.player();
        BlockPos pos = lookedAtSaus(player);
        if (pos == null || !player.getMainHandItem().isEmpty() || !player.getFoodData().needsFood()) {
            return;
        }
        player.getFoodData().eat(20, 0.6f);
        player.level().playSound(null, player.blockPosition(), SoundEvents.GENERIC_DRINK.value(), SoundSource.PLAYERS, 1f, 0.9f);
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.6f, 1.2f);
        if (player.level() instanceof ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0);
        }
    }
}
