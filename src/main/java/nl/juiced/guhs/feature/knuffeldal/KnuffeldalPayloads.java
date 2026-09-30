package nl.juiced.guhs.feature.knuffeldal;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;

/**
 * The Knuffeldal's talking screen: guhs:knuffeldal_open (server -> client: who talks, what they say, the Knusfeest
 * list or the answers you can give) and guhs:knuffeldal_action (client -> server: the answer you picked).
 */
public final class KnuffeldalPayloads {
    /**
     * Opens (or updates) the talking screen. data: "Scherm" (burgemeester | cocotje | verhaal), "Tekst" (lang key, "Args" list),
     * "Taken" (the Knusfeest list: [{Id, Stap}]), "Opties" (answers: [{Id (int), Tekst (lang key)}]), "Sluit" (true: close).
     * 3.0 (verhaal.Praat): "Sleutel" (answers go to its listener; closing sends -1), "Paginas" ([{Spreker (entity id, -1 none),
     * Naam (lang key, "" = the speaker's name), Tekst, Args}]: a scene with "Verder »", the answers on the last page).
     * npcId -1: no speaker (the screen then doesn't close by distance).
     */
    public record Open(int npcId, CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("knuffeldal_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Open::npcId, ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open payload, IPayloadContext context) {
            nl.juiced.guhs.feature.knuffeldal.client.KnuffeldalClient.open(payload);
        }
    }

    /** The answer (option id) the player picked in the talking screen of that NPC. */
    public record Action(int npcId, int action) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(Guhs.id("knuffeldal_action"));
        public static final StreamCodec<FriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Action::npcId, ByteBufCodecs.VAR_INT, Action::action, Action::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Action p, IPayloadContext context) {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            net.minecraft.world.entity.Entity spreker = p.npcId() >= 0 ? player.level().getEntity(p.npcId()) : null;
            if (spreker instanceof GuhNpcEntity npc && npc.distanceTo(player) <= 10) {
                if (npc.getKind() == GuhNpcEntity.Kind.COCOTJE) {
                    Cocotje.kies(npc, player, p.action());
                    return;
                } else if (npc.getKind() == GuhNpcEntity.Kind.BURGEMEESTERGUH) {
                    Burgemeester.actie(npc, player, p.action());
                    return;
                }
            }
            // 3.0: every other talking screen (nl.juiced.guhs.feature.verhaal.Praat: a sleutel listener or the NPC's role)
            nl.juiced.guhs.feature.verhaal.Praat.antwoord(player, spreker, p.action());
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Action.TYPE, Action.STREAM_CODEC, Action::handle);
    }

    private KnuffeldalPayloads() {
    }
}
