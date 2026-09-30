package nl.juiced.guhs.feature.guhkamer;

import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandData;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.network.ModNetworking;

/**
 * The Guhbel screen's messages: server to client "open (or refresh) the Guhbel screen" ({@code guhs:guhkamer_open}: your
 * room's size, its guests, your guhs nearby), client to server a button ({@code guhs:guhkamer_actie}: send a guh to the
 * room, call a guest back).
 */
public final class GuhkamerPayloads {
    /** Client: opens/refreshes the screen (set by client.GuhkamerClient). */
    public static volatile Consumer<Open> opener = p -> {
    };

    public enum Actie { STUUR, ROEP }

    /** Your own guhs within this many blocks can be sent. */
    public static final double BIJ = 24;

    public record Open(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("guhkamer_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open p, IPayloadContext context) {
            opener.accept(p);
        }
    }

    public record Doe(int actie, String id) implements CustomPacketPayload {
        public static final Type<Doe> TYPE = new Type<>(Guhs.id("guhkamer_actie"));
        public static final StreamCodec<FriendlyByteBuf, Doe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Doe::actie, ByteBufCodecs.stringUtf8(64), Doe::id, Doe::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Doe p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                doe(sp, p);
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Doe.TYPE, Doe.STREAM_CODEC, Doe::handle);
    }

    public static void open(ServerPlayer player) {
        ModNetworking.sendTo(player, new Open(data(player)));
    }

    /** What the Guhbel screen shows. */
    public static CompoundTag data(ServerPlayer player) {
        CompoundTag t = new CompoundTag();
        UUID eigenaar = player.getUUID();
        int z = Guhkamer.zielsguhs(player.server, eigenaar);
        t.putBoolean("Maag", Guhkamer.heeftMaag(player.server, eigenaar));
        t.putInt("Zielsguhs", z);
        t.putInt("Breedte", Guhkamer.breedte(z));
        t.putInt("Plekken", Guhkamer.plekken(z));
        BandData band = BandData.get(player.server);
        ListTag gasten = new ListTag();
        GuhkamerData.Kamer k = GuhkamerData.get(player.server).vind(eigenaar);
        if (k != null) {
            for (GuhkamerData.Gast g : k.gasten.values()) {
                CompoundTag c = new CompoundTag();
                c.putString("Id", g.id.toString());
                c.putString("Naam", g.naam);
                c.put("Looks", g.looks.copy());
                BandData.Rec r = band.vind(eigenaar, g.id);
                c.putInt("Niveau", r == null ? 0 : r.niveau().ordinal());
                Huisje h = Huisjes.vanBewoner(player.server, eigenaar, g.id);
                c.putString("Woont", h == null ? "" : h.naam());
                gasten.add(c);
            }
        }
        t.put("Gasten", gasten);
        ListTag bij = new ListTag();
        for (GuhEntity g : Band.samenGuhs(player, BIJ)) {
            if (Guhkamer.isGast(g)) {
                continue;
            }
            CompoundTag c = new CompoundTag();
            c.putString("Id", g.getUUID().toString());
            c.putString("Naam", g.getName().getString());
            c.put("Looks", Band.looks(g));
            c.putInt("Niveau", Band.niveau(g).ordinal());
            Huisje h = Huisjes.thuisVan(g);
            c.putString("Woont", h == null ? "" : h.naam());
            bij.add(c);
        }
        t.put("Bij", bij);
        return t;
    }

    static void doe(ServerPlayer player, Doe p) {
        UUID id;
        try {
            id = UUID.fromString(p.id());
        } catch (IllegalArgumentException e) {
            return;
        }
        Actie actie = p.actie() >= 0 && p.actie() < Actie.values().length ? Actie.values()[p.actie()] : null;
        if (actie == Actie.STUUR) {
            Entity e = player.serverLevel().getEntity(id);
            if (e instanceof GuhEntity g && g.distanceTo(player) <= BIJ + 8) {
                meld(player, g, Guhkamer.stuur(player, g));
            }
        } else if (actie == Actie.ROEP) {
            Guhkamer.roep(player, id);
        }
        open(player);
    }

    /** The actionbar message after sending a guh (Guhbel or the Guh menu). */
    static void meld(ServerPlayer player, GuhEntity g, Guhkamer.Uitkomst u) {
        String key = switch (u) {
            case OK -> "gui.guhs.guhkamer.gestuurd";
            case VOL -> "gui.guhs.guhkamer.vol";
            case GEEN_MAAG -> "gui.guhs.guhkamer.geen_maag";
            case NIET_JOUW -> "gui.guhs.guhkamer.niet_jouw";
        };
        player.displayClientMessage(Component.translatable(key, g.getName()).withStyle(u == Guhkamer.Uitkomst.OK
                ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY), true);
    }

    /** The Guh menu's "Logeren in de Guhkamer" (2.10.1): like sending it with the Guhbel (own tamed guh, room not full). */
    public static Guhkamer.Uitkomst menuLogeren(ServerPlayer player, GuhEntity guh) {
        if (Guhkamer.isGast(guh)) {
            return Guhkamer.Uitkomst.OK;   // (already staying there)
        }
        Guhkamer.Uitkomst u = Guhkamer.stuur(player, guh);
        meld(player, guh, u);
        if (u == Guhkamer.Uitkomst.OK) {
            player.level().playSound(null, player.blockPosition(), GuhkamerFeature.BEL.get(), net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.1f);
        }
        return u;
    }

    /** The Guh menu's "Uit de logeerkamer" (2.10.1): a guest comes to you (and follows you out), like the Guhbel's call. */
    @javax.annotation.Nullable
    public static Entity menuUit(ServerPlayer player, GuhEntity guh) {
        if (!Guhkamer.isGast(guh) || !player.getUUID().equals(guh.getOwnerUUID())) {
            return null;
        }
        Entity e = Guhkamer.roep(player, guh.getUUID());
        if (e == null && !guh.isRemoved()) {
            Guhkamer.markeer(guh, false);   // (not on the room's list any more: just your guh again)
            e = guh;
        }
        if (e instanceof GuhEntity g) {
            g.setOrderedToSit(false);
        }
        return e;
    }

    private GuhkamerPayloads() {
    }
}
