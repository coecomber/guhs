package nl.juiced.guhs.feature.techbuis;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * The one message of the Knabbelbuizen: {@code guhs:techbuis_rol} (to the client), "this item now rolls from this piece
 * along these tubes". The client draws the ride by itself ({@code client.BuisRitten}); the server never sends where the
 * item is. (The item is no entity and the tubes have no block entity: one small message per ride is all there is.)
 */
public final class BuisPayloads {
    /** Client handler (set by client.TechbuisClient). */
    public static volatile Consumer<Rol> ontvanger = p -> {
    };

    /**
     * @param start   the piece the item leaves from
     * @param stappen the way: one byte per step ({@link Direction#get3DDataValue}), from the piece through every tube, the
     *                last step into the block where it ends
     * @param stack   what rolls
     */
    public record Rol(BlockPos start, byte[] stappen, ItemStack stack) implements CustomPacketPayload {
        public static final Type<Rol> TYPE = new Type<>(Guhs.id("techbuis_rol"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Rol> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Rol::start,
                ByteBufCodecs.byteArray(Buizen.MAX_PAD + 2), Rol::stappen,
                ItemStack.STREAM_CODEC, Rol::stack, Rol::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        /** The ride along this route. */
        public static Rol van(BuisRoutes.Route route, ItemStack stack) {
            List<BlockPos> pad = route.pad();
            byte[] stappen = new byte[pad.size()];
            for (int i = 0; i + 1 < pad.size(); i++) {
                BlockPos a = pad.get(i), b = pad.get(i + 1);
                Direction stap = Direction.getApproximateNearest(b.getX() - a.getX(), b.getY() - a.getY(), b.getZ() - a.getZ());
                stappen[i] = (byte) stap.get3DDataValue();
            }
            stappen[pad.size() - 1] = (byte) route.kant().getOpposite().get3DDataValue();
            return new Rol(pad.get(0), stappen, stack.copy());
        }

        public static void handle(Rol p, IPayloadContext context) {
            ontvanger.accept(p);
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Rol.TYPE, Rol.STREAM_CODEC, Rol::handle);
    }

    private BuisPayloads() {
    }
}
