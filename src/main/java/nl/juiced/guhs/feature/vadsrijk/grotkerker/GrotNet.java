package nl.juiced.guhs.feature.vadsrijk.grotkerker;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * W-GROT network: server -> client {@link Scherm}(meta, texts) = the dungeon window (the door's interface, or the ready check),
 * client -> server {@link Actie}(wat, grot) = a button. The server builds every text (translatable, so the client's language
 * shows), the client only lays it out ({@code client/GrotScherm}):
 * <ul>
 *   <li>{@code meta.soort}: "deur" or "klaar"; {@code meta.open}: open the window (else only update one that is open);
 *       {@code meta.sluit}: close it;</li>
 *   <li>{@code meta.lijnen}: [{t: text index, k: colour, v: bold, h: heading}] below the title (texts[0]) and subtitle (texts[1]);</li>
 *   <li>{@code meta.knoppen}: [{id, t, aan, tip (-1 none)}] in rows of two;</li>
 *   <li>{@code meta.tijd}/{@code meta.duur}: ticks left of the ready check (a bar).</li>
 * </ul>
 */
public final class GrotNet {
    public record Actie(String wat, String grot) implements CustomPacketPayload {
        public static final Type<Actie> TYPE = new Type<>(Guhs.id("vr_grotdeur_actie"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Actie> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, Actie::wat,
                ByteBufCodecs.STRING_UTF8, Actie::grot, Actie::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Scherm(CompoundTag meta, List<Component> teksten) implements CustomPacketPayload {
        public static final Type<Scherm> TYPE = new Type<>(Guhs.id("vr_grotdeur_scherm"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Scherm> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Scherm::meta,
                ComponentSerialization.TRUSTED_STREAM_CODEC.apply(ByteBufCodecs.list()), Scherm::teksten, Scherm::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Set by the client. */
    public static Consumer<Scherm> ontvanger = s -> {
    };
    /** Gametests: the last window sent per player name (meta + texts). */
    public static final java.util.Map<java.util.UUID, Scherm> LAATSTE = new java.util.concurrent.ConcurrentHashMap<>();

    private GrotNet() {
    }

    public static void payloads(PayloadRegistrar r) {
        r.playToServer(Actie.TYPE, Actie.STREAM_CODEC, (m, c) -> {
            if (c.player() instanceof ServerPlayer sp) {
                c.enqueueWork(() -> GrotRij.actie(sp, m.wat(), m.grot()));
            }
        });
        r.playToClient(Scherm.TYPE, Scherm.STREAM_CODEC, (m, c) -> ontvanger.accept(m));
    }

    static void stuur(ServerPlayer p, Scherm s) {
        LAATSTE.put(p.getUUID(), s);
        if (p.connection != null && !(p instanceof net.neoforged.neoforge.common.util.FakePlayer)) {
            try {
                PacketDistributor.sendToPlayer(p, s);
            } catch (RuntimeException e) {
                // (a gametest mock player has no real connection)
            }
        }
    }

    /** A small builder for a window. */
    static final class Bouw {
        final CompoundTag meta = new CompoundTag();
        final List<Component> teksten = new ArrayList<>();
        final ListTag lijnen = new ListTag(), knoppen = new ListTag();

        Bouw(String soort, String grot, boolean open, Component titel, Component onder) {
            meta.putString("soort", soort);
            meta.putString("grot", grot);
            meta.putBoolean("open", open);
            teksten.add(titel);
            teksten.add(onder);
        }

        int tekst(Component c) {
            teksten.add(c);
            return teksten.size() - 1;
        }

        Bouw lijn(Component c, int kleur) {
            return lijn(c, kleur, false, false);
        }

        Bouw kop(Component c) {
            return lijn(c, 0xFFC0287C, true, true);
        }

        Bouw lijn(Component c, int kleur, boolean vet, boolean kop) {
            CompoundTag t = new CompoundTag();
            t.putInt("t", tekst(c));
            t.putInt("k", kleur);
            t.putBoolean("v", vet);
            t.putBoolean("h", kop);
            lijnen.add(t);
            return this;
        }

        Bouw knop(String id, Component c, boolean aan, Component tip) {
            CompoundTag t = new CompoundTag();
            t.putString("id", id);
            t.putInt("t", tekst(c));
            t.putBoolean("aan", aan);
            t.putInt("tip", tip == null ? -1 : tekst(tip));
            knoppen.add(t);
            return this;
        }

        Scherm klaar() {
            meta.put("lijnen", lijnen);
            meta.put("knoppen", knoppen);
            return new Scherm(meta, List.copyOf(teksten));
        }
    }

    static Scherm sluit(String soort) {
        CompoundTag m = new CompoundTag();
        m.putString("soort", soort);
        m.putBoolean("sluit", true);
        return new Scherm(m, List.of());
    }
}
