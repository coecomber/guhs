package nl.juiced.guhs.feature.emotes;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * Client -> server, from the emote picker: do an emote now (once), keep doing it, stop, or set the favourite
 * (emote -1 = no favourite). Only the owner, close by; handled on the server thread.
 */
public record EmotePayload(int entityId, int action, int emote) implements CustomPacketPayload {
    public static final int NOW = 0, LOOP = 1, STOP = 2, FAVORITE = 3;

    public static final Type<EmotePayload> TYPE = new Type<>(Guhs.id("emote"));
    public static final StreamCodec<FriendlyByteBuf, EmotePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EmotePayload::entityId, ByteBufCodecs.VAR_INT, EmotePayload::action,
            ByteBufCodecs.VAR_INT, EmotePayload::emote, EmotePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EmotePayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            apply(player, payload);
        }
    }

    /** Does what the picker asked; returns false if it was refused (not the owner, too far, can't right now). */
    public static boolean apply(ServerPlayer player, EmotePayload p) {
        if (!(player.level().getEntity(p.entityId()) instanceof GuhEntity guh)) {
            return false;
        }
        double reach = 8.0 + guh.getBbWidth() * 2;
        if (!guh.isOwnedBy(player) || player.distanceToSqr(guh) > reach * reach) {
            return false;
        }
        switch (p.action()) {
            case NOW, LOOP -> {
                Emote emote = Emote.byIndex(p.emote());
                if (emote == null) {
                    return false;
                }
                if (!nl.juiced.guhs.feature.samen.SamenBeloning.heeft(player, emote)) {   // 2.10: the hartjes emotes are unlocks
                    nl.juiced.guhs.feature.samen.SamenBeloning.opSlot(player, emote);
                    return false;
                }
                if (!Emote.magVoor(emote, guh)) {   // 3.0: the ukelele is the 626-guh's
                    player.sendOverlayMessage(Component.translatable("gui.guhs.emotes.alleen_voor",
                            Component.translatable("entity.guhs.guh." + Emote.alleenVoor(emote).id())));
                    return false;
                }
                if (!guh.emotes.start(emote, p.action() == LOOP, GuhEmotes.Source.OWNER)) {
                    player.sendOverlayMessage(Component.translatable("gui.guhs.emotes.busy"));
                    return false;
                }
                GuhEmotes.countForPlayer(player, emote);
                return true;
            }
            case STOP -> {
                guh.emotes.stop();
                return true;
            }
            case FAVORITE -> {
                Emote emote = Emote.byIndex(p.emote()); // anything else: no favourite
                if (emote != null && !nl.juiced.guhs.feature.samen.SamenBeloning.heeft(player, emote)) {
                    nl.juiced.guhs.feature.samen.SamenBeloning.opSlot(player, emote);
                    return false;
                }
                if (emote != null && !Emote.magVoor(emote, guh)) {   // 3.0: the ukelele is the 626-guh's
                    return false;
                }
                guh.emotes.setFavorite(emote);
                if (emote != null) {
                    nl.juiced.guhs.quest.GuhAdvancements.grant(player, "emote_lievelings");
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}
