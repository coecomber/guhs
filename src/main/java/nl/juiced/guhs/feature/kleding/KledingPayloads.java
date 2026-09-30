package nl.juiced.guhs.feature.kleding;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * The network messages of the kleding feature: guhs:kleding_data (phase 1: your unlocks), guhs:kleding_ontgrendeld (the
 * piece pops up on your screen), guhs:kleding_favorieten (your favourite outfits), guhs:kleding_kleed (the wardrobe's
 * "Aantrekken!") and guhs:kleding_bewaar (save a favourite).
 */
public final class KledingPayloads {
    /** Server -> client: the clothes this player has unlocked (clothes ids, see {@link KledingUnlocks}). */
    public record KledingData(List<String> unlocks) implements CustomPacketPayload {
        public static final Type<KledingData> TYPE = new Type<>(Guhs.id("kleding_data"));
        public static final StreamCodec<FriendlyByteBuf, KledingData> STREAM_CODEC =
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(KledingData::new, KledingData::unlocks).cast();

        public KledingData {
            unlocks = List.copyOf(unlocks);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(KledingData payload, IPayloadContext context) {
            KledingUnlocks.Client.set(payload.unlocks());
        }
    }

    /** Server -> client: you just unlocked this piece (it pops up big on your screen, like a totem). */
    public record Ontgrendeld(String id) implements CustomPacketPayload {
        public static final Type<Ontgrendeld> TYPE = new Type<>(Guhs.id("kleding_ontgrendeld"));
        public static final StreamCodec<FriendlyByteBuf, Ontgrendeld> STREAM_CODEC =
                ByteBufCodecs.STRING_UTF8.map(Ontgrendeld::new, Ontgrendeld::id).cast();

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Ontgrendeld payload, IPayloadContext context) {
            nl.juiced.guhs.feature.kleding.client.KledingClient.ontgrendeld(payload.id());
        }
    }

    /** Server -> client: your favourite outfits ({@link KledingFavorieten}). */
    public record Favorieten(List<String> outfits) implements CustomPacketPayload {
        public static final Type<Favorieten> TYPE = new Type<>(Guhs.id("kleding_favorieten"));
        public static final StreamCodec<FriendlyByteBuf, Favorieten> STREAM_CODEC =
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()).map(Favorieten::new, Favorieten::outfits).cast();

        public Favorieten {
            outfits = List.copyOf(outfits);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Favorieten payload, IPayloadContext context) {
            KledingFavorieten.Client.set(payload.outfits());
        }
    }

    /** Client -> server: dress this guh in this outfit (one clothes id per wardrobe slot, "" = nothing). */
    public record Kleed(int entityId, List<String> outfit) implements CustomPacketPayload {
        public static final Type<Kleed> TYPE = new Type<>(Guhs.id("kleding_kleed"));
        public static final StreamCodec<FriendlyByteBuf, Kleed> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Kleed::entityId, ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16)), Kleed::outfit, Kleed::new);

        public Kleed {
            outfit = List.copyOf(outfit);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Kleed payload, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player) || !(player.level().getEntity(payload.entityId()) instanceof GuhEntity guh)) {
                return;
            }
            double reach = 8.0 + guh.getBbWidth() * 2;
            if (player.distanceToSqr(guh) > reach * reach) {
                return;
            }
            KledingKast.kleed(player, guh, KledingPayloads.outfit(payload.outfit()));
        }
    }

    /** Client -> server: save favourite {@code index} (one clothes id per wardrobe slot, "" = nothing). */
    public record Bewaar(int index, List<String> outfit) implements CustomPacketPayload {
        public static final Type<Bewaar> TYPE = new Type<>(Guhs.id("kleding_bewaar"));
        public static final StreamCodec<FriendlyByteBuf, Bewaar> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Bewaar::index, ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(16)), Bewaar::outfit, Bewaar::new);

        public Bewaar {
            outfit = List.copyOf(outfit);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Bewaar payload, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player) {
                KledingFavorieten.bewaar(player, payload.index(), KledingPayloads.outfit(payload.outfit()));
            }
        }
    }

    /** Ids (one per wardrobe slot) to pieces: unknown ids, and pieces in the wrong slot, become nothing. */
    public static List<GuhClothes> outfit(List<String> ids) {
        List<GuhClothes.Slot> slots = GuhClothes.Slot.kleding();
        List<GuhClothes> out = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            GuhClothes c = i < ids.size() ? GuhClothes.byId(ids.get(i)) : null;
            out.add(c != null && c.slot == slots.get(i) ? c : null);
        }
        return out;
    }

    /** Pieces (one per wardrobe slot) to ids ("" = nothing). */
    public static List<String> ids(List<GuhClothes> outfit) {
        List<String> out = new ArrayList<>();
        for (GuhClothes c : outfit) {
            out.add(c == null ? "" : c.id());
        }
        return out;
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(KledingData.TYPE, KledingData.STREAM_CODEC, KledingData::handle);
        registrar.playToClient(Ontgrendeld.TYPE, Ontgrendeld.STREAM_CODEC, Ontgrendeld::handle);
        registrar.playToClient(Favorieten.TYPE, Favorieten.STREAM_CODEC, Favorieten::handle);
        registrar.playToServer(Kleed.TYPE, Kleed.STREAM_CODEC, Kleed::handle);
        registrar.playToServer(Bewaar.TYPE, Bewaar.STREAM_CODEC, Bewaar::handle);
    }

    private KledingPayloads() {
    }
}
