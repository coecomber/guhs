package nl.juiced.guhs.feature.dexdromen;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.blub.BlubGuhEntity;
import nl.juiced.guhs.feature.blub.BlubTrekking;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * 1.8.0: the Blub-meterkaart (Guhdex, on the Blub-guh page). Per soort a row of ten boxes, one per tenth of the Blub-Guh-Meter
 * (0-9, 10-19 ... 90-100), plus two golden boxes: a Pure Blub (meter 0) and a Volle Guh (meter 100). A box fills when the
 * player tames a Blub (or a babyblubje of theirs is born) with that soort and meter: 5 x 12 = 60 boxes.
 * <p>
 * Saved per player in GuhQuests.saved ({@link #KEY} + soort: 12 bits, bits 0-9 the tenths, {@link #PUUR} and {@link #VOL} the
 * golden ones), synced to the client when the Guhdex opens and on every new box.
 */
public final class BlubMeterkaart {
    public static final String KEY = "guhs_blub_meterkaart_";
    /** The bits of the two golden boxes. */
    public static final int PUUR = 10, VOL = 11;
    /** Every box of a row: the ten tenths and the two golden ones. */
    public static final int RIJ = (1 << 12) - 1, TIENDEN = (1 << 10) - 1;
    public static final int VAKJES = BlubTrekking.Soort.values().length * 12;

    private BlubMeterkaart() {
    }

    /** The bits a Blub with this meter fills: its tenth, and (exactly 0 or 100) its golden box. */
    public static int bits(int meter) {
        int bits = 1 << Math.min(9, Math.max(0, meter) / 10);
        if (meter == 0) {
            bits |= 1 << PUUR;
        } else if (meter == 100) {
            bits |= 1 << VOL;
        }
        return bits;
    }

    public static int rij(ServerPlayer player, BlubTrekking.Soort soort) {
        return GuhQuests.saved(player).getIntOr(KEY + soort.name().toLowerCase(java.util.Locale.ROOT), 0);
    }

    public static int[] kaart(ServerPlayer player) {
        BlubTrekking.Soort[] soorten = BlubTrekking.Soort.values();
        int[] kaart = new int[soorten.length];
        for (int i = 0; i < soorten.length; i++) {
            kaart[i] = rij(player, soorten[i]);
        }
        return kaart;
    }

    public static int gevuld(int[] kaart) {
        int n = 0;
        for (int rij : kaart) {
            n += Integer.bitCount(rij & RIJ);
        }
        return n;
    }

    /** A Blub of this player's (tamed, or a babyblubje born to them): fills its box. True when that was a new box. */
    public static boolean vul(ServerPlayer player, BlubGuhEntity blub) {
        BlubTrekking.Soort soort = blub.waarden().soort();
        int oud = rij(player, soort), nieuw = oud | bits(blub.meter());
        if (nieuw == oud) {
            return false;
        }
        GuhQuests.saved(player).putInt(KEY + soort.name().toLowerCase(java.util.Locale.ROOT), nieuw);
        boolean goud = (nieuw & ~oud & ((1 << PUUR) | (1 << VOL))) != 0;
        player.sendOverlayMessage(Component.translatable(goud ? "gui.guhs.meterkaart.nieuw_goud" : "gui.guhs.meterkaart.nieuw",
                gevuld(kaart(player)), VAKJES).withStyle(goud ? ChatFormatting.GOLD : ChatFormatting.LIGHT_PURPLE));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, goud ? 1.5f : 1.2f);
        if ((nieuw & TIENDEN) == TIENDEN) {
            GuhAdvancements.grant(player, "blub_meterkaart_rij");
        }
        if ((nieuw & ((1 << PUUR) | (1 << VOL))) != 0) {
            GuhAdvancements.grant(player, "blub_meterkaart_goud");
        }
        sync(player);
        return true;
    }

    public static void sync(ServerPlayer player) {
        int[] kaart = kaart(player);
        List<Integer> rijen = new ArrayList<>();
        for (int rij : kaart) {
            rijen.add(rij);
        }
        ModNetworking.sendTo(player, new Kaart(rijen));
    }

    /** The local player's map on the client (one int per soort, in Soort order). */
    public static final class Client {
        public static volatile int[] kaart = new int[BlubTrekking.Soort.values().length];

        private Client() {
        }
    }

    /** Server to client: the map of this player. */
    public record Kaart(List<Integer> rijen) implements CustomPacketPayload {
        public static final Type<Kaart> TYPE = new Type<>(Guhs.id("blub_meterkaart"));
        public static final StreamCodec<FriendlyByteBuf, Kaart> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), Kaart::rijen, Kaart::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Kaart p, IPayloadContext context) {
            int[] kaart = new int[BlubTrekking.Soort.values().length];
            for (int i = 0; i < kaart.length && i < p.rijen().size(); i++) {
                kaart[i] = p.rijen().get(i);
            }
            Client.kaart = kaart;
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Kaart.TYPE, Kaart.STREAM_CODEC, Kaart::handle);
    }
}
