package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * The Ambachten payloads: {@link Sync} (server -> client every second: the player's own gathering spots near them + the food buff
 * and rest for the HUD), {@link Scherm} (a screen: the Ambachten book, a workshop, the boards) and {@link Doe} (client -> server:
 * a screen action or a harvest, see {@link AmbachtSchermen#doe}).
 */
public final class AmbachtPayloads {
    public record Sync(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(Guhs.id("vr_amb_sync"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Sync::data, Sync::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Scherm(CompoundTag data, List<ItemStack> items) implements CustomPacketPayload {
        public static final Type<Scherm> TYPE = new Type<>(Guhs.id("vr_amb_scherm"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Scherm> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, Scherm::data, ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), Scherm::items, Scherm::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Doe(String actie, CompoundTag args) implements CustomPacketPayload {
        public static final Type<Doe> TYPE = new Type<>(Guhs.id("vr_amb_doe"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Doe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Doe::actie, ByteBufCodecs.COMPOUND_TAG, Doe::args, Doe::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Set by the client class. */
    public static Consumer<Sync> syncOntvanger = s -> {
    };
    public static Consumer<Scherm> schermOntvanger = s -> {
    };

    private AmbachtPayloads() {
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Sync.TYPE, Sync.STREAM_CODEC, (p, c) -> syncOntvanger.accept(p));
        r.playToClient(Scherm.TYPE, Scherm.STREAM_CODEC, (p, c) -> schermOntvanger.accept(p));
        r.playToServer(Doe.TYPE, Doe.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                AmbachtSchermen.doe(sp, p.actie(), p.args());
            }
        });
    }

    public static void stuur(ServerPlayer p, CompoundTag data, List<ItemStack> items) {
        PacketDistributor.sendToPlayer(p, new Scherm(data, items));
    }

    static void sync(ServerPlayer p, CompoundTag data) {
        PacketDistributor.sendToPlayer(p, new Sync(data));
    }
}
