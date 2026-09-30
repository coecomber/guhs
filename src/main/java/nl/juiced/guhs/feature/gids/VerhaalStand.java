package nl.juiced.guhs.feature.gids;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;

/**
 * One questline as the Guhdex tab "Verhalen" shows it (the same on both sides; built by {@link VerhalenVoortgang} on the
 * server, sent by {@link VerhalenPayloads}): its status, the step you're at (of how many; the step names are lang keys
 * {@code gui.guhs.verhalen.<id>.stap.<i>}), what to do now, whom to go to and where, what you still need (items with
 * counts) and its rewards (ticked when you have them).
 */
public record VerhaalStand(String id, String icoon, Status status, int stap, int stappen, Component nu, Component waar,
                           List<Nodig> nodig, List<Beloning> beloningen) {
    public VerhaalStand {
        nodig = List.copyOf(nodig);
        beloningen = List.copyOf(beloningen);
    }

    public enum Status {
        NIET_BEGONNEN, BEZIG, KLAAR;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Component naam() {
            return Component.translatable("gui.guhs.verhalen.status." + id());
        }
    }

    /** Something you need for the current step: an item (id, for the icon), its name, how many you have / need. */
    public record Nodig(String item, Component naam, int heb, int nodig) {
        public boolean genoeg() {
            return heb >= nodig;
        }
    }

    /** A reward of the questline: its text and whether you already have it. */
    public record Beloning(String item, Component tekst, boolean binnen) {
    }

    /** The name of the questline (gui.guhs.verhalen.&lt;id&gt;.naam). */
    public Component naam() {
        return Component.translatable("gui.guhs.verhalen." + id + ".naam");
    }

    /** Its little story in one line (gui.guhs.verhalen.&lt;id&gt;.uitleg). */
    public Component uitleg() {
        return Component.translatable("gui.guhs.verhalen." + id + ".uitleg");
    }

    /** The lang key of step i (0-based). */
    public static String stapKey(String id, int i) {
        return "gui.guhs.verhalen." + id + ".stap." + i;
    }

    public Component stapNaam(int i) {
        return Component.translatable(stapKey(id, i));
    }

    public boolean klaar() {
        return status == Status.KLAAR;
    }

    // --- network ---------------------------------------------------------------------------------------------------------

    private static final StreamCodec<RegistryFriendlyByteBuf, Component> TEKST = ComponentSerialization.TRUSTED_STREAM_CODEC;

    public static final StreamCodec<RegistryFriendlyByteBuf, VerhaalStand> STREAM_CODEC = StreamCodec.of((buf, v) -> {
        buf.writeUtf(v.id);
        buf.writeUtf(v.icoon);
        buf.writeVarInt(v.status.ordinal());
        buf.writeVarInt(v.stap);
        buf.writeVarInt(v.stappen);
        TEKST.encode(buf, v.nu);
        TEKST.encode(buf, v.waar);
        buf.writeVarInt(v.nodig.size());
        for (Nodig n : v.nodig) {
            buf.writeUtf(n.item);
            TEKST.encode(buf, n.naam);
            buf.writeVarInt(n.heb);
            buf.writeVarInt(n.nodig);
        }
        buf.writeVarInt(v.beloningen.size());
        for (Beloning b : v.beloningen) {
            buf.writeUtf(b.item);
            TEKST.encode(buf, b.tekst);
            buf.writeBoolean(b.binnen);
        }
    }, buf -> {
        String id = buf.readUtf();
        String icoon = buf.readUtf();
        Status status = Status.values()[Math.floorMod(buf.readVarInt(), Status.values().length)];
        int stap = buf.readVarInt(), stappen = buf.readVarInt();
        Component nu = TEKST.decode(buf), waar = TEKST.decode(buf);
        int n = Math.min(buf.readVarInt(), 64);
        List<Nodig> nodig = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            nodig.add(new Nodig(buf.readUtf(), TEKST.decode(buf), buf.readVarInt(), buf.readVarInt()));
        }
        int b = Math.min(buf.readVarInt(), 64);
        List<Beloning> beloningen = new ArrayList<>();
        for (int i = 0; i < b; i++) {
            beloningen.add(new Beloning(buf.readUtf(), TEKST.decode(buf), buf.readBoolean()));
        }
        return new VerhaalStand(id, icoon, status, stap, stappen, nu, waar, nodig, beloningen);
    });
}
