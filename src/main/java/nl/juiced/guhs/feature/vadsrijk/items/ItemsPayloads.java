package nl.juiced.guhs.feature.vadsrijk.items;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/** T-ITEMS: the drankjesriem packets (server -> client: the 3 slots; client -> server: drink / click / shift-click). */
public final class ItemsPayloads {
    public static Consumer<Riem> riemOntvanger = r -> {
    };

    public record Riem(List<ItemStack> items) implements CustomPacketPayload {
        public static final Type<Riem> TYPE = new Type<>(Guhs.id("vr_t_items_riem"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Riem> STREAM_CODEC = StreamCodec.ofMember(
                (t, b) -> b.writeCollection(t.items, (bb, s) -> ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) bb, s)),
                b -> new Riem(b.readList(bb -> ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) bb))));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** actie: 0 = drink from slot i, 1 = click on belt slot i (cursor in / out), 2 = shift-click inventory slot i into the belt. */
    public record RiemActie(int actie, int i) implements CustomPacketPayload {
        public static final Type<RiemActie> TYPE = new Type<>(Guhs.id("vr_t_items_riem_actie"));
        public static final StreamCodec<RegistryFriendlyByteBuf, RiemActie> STREAM_CODEC = StreamCodec.ofMember((t, b) -> {
            b.writeVarInt(t.actie);
            b.writeVarInt(t.i);
        }, b -> new RiemActie(b.readVarInt(), b.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private ItemsPayloads() {
    }

    static void register(PayloadRegistrar r) {
        r.playToClient(Riem.TYPE, Riem.STREAM_CODEC, (p, c) -> riemOntvanger.accept(p));
        r.playToServer(RiemActie.TYPE, RiemActie.STREAM_CODEC, (p, c) -> {
            if (!(c.player() instanceof ServerPlayer sp) || !nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk.in(sp)) {
                return;
            }
            switch (p.actie()) {
                case 0 -> Drankjesriem.drink(sp, p.i());
                case 1 -> Drankjesriem.klik(sp, p.i());
                case 2 -> Drankjesriem.legUitInventaris(sp, p.i());
                default -> {
                }
            }
        });
    }

    static void stuurRiem(ServerPlayer p, List<ItemStack> l) {
        if (p.connection != null) {
            PacketDistributor.sendToPlayer(p, new Riem(l.stream().map(ItemStack::copy).toList()));
        }
    }
}
