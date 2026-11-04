package nl.juiced.guhs.feature.vadsrijk.zone2.systemen;

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

/** The juwelier screen's network: the screen data to the client, an action back ("zet", "haal", "dicht"). */
public final class JuwelierPayloads {
    public record Scherm(CompoundTag data, List<ItemStack> items) implements CustomPacketPayload {
        public static final Type<Scherm> TYPE = new Type<>(Guhs.id("vr_z2_systemen_juwelier"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Scherm> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, Scherm::data, ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), Scherm::items, Scherm::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Doe(String actie, CompoundTag args) implements CustomPacketPayload {
        public static final Type<Doe> TYPE = new Type<>(Guhs.id("vr_z2_systemen_juwelier_doe"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Doe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, Doe::actie, ByteBufCodecs.COMPOUND_TAG, Doe::args, Doe::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Set by the client class. */
    public static Consumer<Scherm> ontvanger = s -> {
    };

    private JuwelierPayloads() {
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Scherm.TYPE, Scherm.STREAM_CODEC, (p, c) -> ontvanger.accept(p));
        r.playToServer(Doe.TYPE, Doe.STREAM_CODEC, (p, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                if (p.actie().equals("dicht")) {
                    Juwelier.dicht(sp);
                } else {
                    Juwelier.doe(sp, p.actie(), p.args());
                }
            }
        });
    }

    static void stuur(ServerPlayer p, CompoundTag d, List<ItemStack> items) {
        PacketDistributor.sendToPlayer(p, new Scherm(d, items));
    }
}
