package nl.juiced.guhs.feature.huisje;

import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandData;
import nl.juiced.guhs.feature.piep.PiepMaatje;
import nl.juiced.guhs.network.ModNetworking;

import net.minecraft.core.UUIDUtil;
/**
 * The huisje screen's messages: server to client "open (or refresh) the screen of this huisje" ({@code guhs:huisje_open},
 * with its residents, their chores and who could move in), client to server a button ({@code guhs:huisje_actie}: rename,
 * move in, move out, a chore on/off). Only the owner (or an operator) within 16 blocks.
 * 1.2.8: the overview dialog asks ({@code guhs:huisje_overzicht_vraag}) and gets {@code guhs:huisje_overzicht}.
 */
public final class HuisjePayloads {
    /** Client: opens/refreshes the screen (set by client.HuisjeClient). */
    public static volatile Consumer<Open> opener = p -> {
    };

    /** Client: the overview of the open screen arrived (set by client.HuisjeClient). */
    public static volatile Consumer<Overzicht> overzichtOntvanger = p -> {
    };

    /** (Append only: sent by ordinal.) 1.3.2 BINNEN: the button "Naar binnen", for the owner and for visitors. */
    public enum Actie { NAAM, TREK_IN, UIT, KLUS, MELDINGEN, BINNEN }

    /** Client: the door fade of going in or out (set by client.HuisjeClient). */
    public static volatile Consumer<BinnenFx> fade = p -> {
    };

    /** 1.3.2, server to client: fade the screen (black and pink) because the player steps through the huisje's door. */
    public record BinnenFx(boolean naarBinnen) implements CustomPacketPayload {
        public static final Type<BinnenFx> TYPE = new Type<>(Guhs.id("huisje_binnen_fx"));
        public static final StreamCodec<FriendlyByteBuf, BinnenFx> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.BOOL, BinnenFx::naarBinnen, BinnenFx::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(BinnenFx p, IPayloadContext context) {
            fade.accept(p);
        }
    }

    public record Open(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(Guhs.id("huisje_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, Open::data, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Open p, IPayloadContext context) {
            opener.accept(p);
        }
    }

    public record Doe(BlockPos pos, int actie, String id, String tekst, boolean aan, int entity) implements CustomPacketPayload {
        public static final Type<Doe> TYPE = new Type<>(Guhs.id("huisje_actie"));
        public static final StreamCodec<FriendlyByteBuf, Doe> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Doe::pos, ByteBufCodecs.VAR_INT, Doe::actie, ByteBufCodecs.stringUtf8(64), Doe::id,
                ByteBufCodecs.stringUtf8(64), Doe::tekst, ByteBufCodecs.BOOL, Doe::aan, ByteBufCodecs.VAR_INT, Doe::entity, Doe::new);

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

    /** 1.2.8, client to server: "what can be done around this huisje?" ({@link HuisjeOverzicht}; the owner or an op only). */
    public record OverzichtVraag(BlockPos pos) implements CustomPacketPayload {
        public static final Type<OverzichtVraag> TYPE = new Type<>(Guhs.id("huisje_overzicht_vraag"));
        public static final StreamCodec<FriendlyByteBuf, OverzichtVraag> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, OverzichtVraag::pos, OverzichtVraag::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(OverzichtVraag p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp) {
                HuisjeOverzicht.handle(sp, p.pos());
            }
        }
    }

    /** 1.2.8, server to client: the overview of one huisje ({@link HuisjeOverzicht#data}). */
    public record Overzicht(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Overzicht> TYPE = new Type<>(Guhs.id("huisje_overzicht"));
        public static final StreamCodec<FriendlyByteBuf, Overzicht> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, Overzicht::data, Overzicht::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Overzicht p, IPayloadContext context) {
            overzichtOntvanger.accept(p);
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(Open.TYPE, Open.STREAM_CODEC, Open::handle);
        registrar.playToServer(Doe.TYPE, Doe.STREAM_CODEC, Doe::handle);
        registrar.playToServer(OverzichtVraag.TYPE, OverzichtVraag.STREAM_CODEC, OverzichtVraag::handle);
        registrar.playToClient(Overzicht.TYPE, Overzicht.STREAM_CODEC, Overzicht::handle);
        registrar.playToClient(BinnenFx.TYPE, BinnenFx.STREAM_CODEC, BinnenFx::handle);
    }

    // =====================================================================================================================

    /** Opens (or refreshes) the screen of this huisje for its owner. */
    public static void open(ServerPlayer player, Huisje h) {
        ModNetworking.sendTo(player, new Open(data(player, h)));
    }

    /** What the screen shows. */
    public static CompoundTag data(ServerPlayer player, Huisje h) {
        ServerLevel level = player.level().getServer().getLevel(h.dim());
        CompoundTag t = new CompoundTag();
        t.putLong("Pos", h.pos().asLong());
        t.putString("Maat", h.maat().id());
        nl.juiced.guhs.taal.Tekst.put(t, "Naam", h.naamTekst());   // (1.2.0: Components, read with Tekst on the client)
        t.putInt("Plekken", h.maat().plekken());
        // 3.0: whose it is, and whether the viewer may change it (timmerguh greys the buttons for the others)
        t.store("Eigenaar", UUIDUtil.CODEC, h.eigenaar());
        t.putString("EigenaarNaam", h.eigenaarNaam());
        t.putBoolean("MagBewerken", Huisjes.magBewerken(player, h));
        t.putBoolean("Meldingen", h.meldingen());
        BandData band = BandData.get(player.level().getServer());
        ListTag bewoners = new ListTag();
        for (UUID id : h.bewoners()) {
            CompoundTag b = new CompoundTag();
            b.putString("Id", id.toString());
            String soort = h.soort(id);
            b.putString("Soort", soort);
            Entity e = level == null ? null : Huisjes.zoekBewoner(level, h, id);
            nl.juiced.guhs.taal.Tekst.put(b, "Naam", e != null ? e.getName() : h.naamVan(id));
            BandData.Rec r = band.vind(h.eigenaar(), id);
            b.putInt("Niveau", "guh".equals(soort) && r != null ? r.niveau().ordinal() : -1);
            b.putInt("Hartjes", r == null ? 0 : r.hartjes);
            if (e instanceof GuhEntity g) {
                b.put("Looks", Band.looks(g));
            } else if (r != null && "guh".equals(soort)) {
                b.put("Looks", r.looks.copy());
            }
            b.putBoolean("Binnen", e != null && Huisjes.isBinnen(e));
            b.putBoolean("Baby", e instanceof Mob m && m.isBaby());
            ListTag klussen = new ListTag();
            for (Klus k : Klusjes.alle()) {
                CompoundTag c = new CompoundTag();
                c.putString("Id", k.id());
                c.putBoolean("Aan", e instanceof Mob m ? h.klusAan(m, k.id()) : h.klusAan(id, k.id()));
                boolean kan = true;
                if (e instanceof Mob m) {
                    try {
                        kan = k.kan(m);
                    } catch (RuntimeException ex) {
                        kan = false;
                    }
                } else {
                    kan = "guh".equals(soort);
                }
                c.putBoolean("Kan", kan);
                klussen.add(c);
            }
            b.put("Klussen", klussen);
            bewoners.add(b);
        }
        t.put("Bewoners", bewoners);
        // who could move in: own band guhs and maatjes within 16 blocks of the player, not living here
        ListTag kandidaten = new ListTag();
        if (level != null && level == player.level()) {
            for (Entity e : level.getEntities((Entity) null, player.getBoundingBox().inflate(16), Huisjes::kanBewoner)) {
                if (!player.getUUID().equals(Band.eigenaar(e)) || h.bewoners().contains(Band.id(e))) {
                    continue;
                }
                CompoundTag c = new CompoundTag();
                c.putInt("Entity", e.getId());
                nl.juiced.guhs.taal.Tekst.put(c, "Naam", e.getName());
                c.putString("Soort", e instanceof PiepMaatje m ? m.soort() : "guh");
                Huisje ander = Huisjes.thuisVan(e);
                nl.juiced.guhs.taal.Tekst.put(c, "Woont", ander == null ? Component.empty() : ander.naamTekst());
                kandidaten.add(c);
            }
        }
        t.put("Kandidaten", kandidaten);
        ListTag klusjes = new ListTag();
        for (Klus k : Klusjes.alle()) {
            CompoundTag c = new CompoundTag();
            c.putString("Id", k.id());
            c.putString("Icoon", BuiltInRegistries.ITEM.getKey(k.icoon().getItem()).toString());
            klusjes.add(c);
        }
        t.put("Klusjes", klusjes);
        t.putBoolean("Bank", level != null && HuisjeOpslag.heeftBankGuh(level, h));
        t.putBoolean("Kist", level != null && HuisjeOpslag.kist(level, h) != null);
        // bbq2: a working Hapluikje in the home base (chore output goes there when no Bank Guh stands in it)
        t.putBoolean("Luikje", level != null && !nl.juiced.guhs.feature.klusjes.Voorraad.luikjes(level, h).isEmpty());
        return t;
    }

    public static void doe(ServerPlayer player, Doe p) {   // (3.0: public for the ownership tests)
        if (!(player.level() instanceof ServerLevel level) || player.distanceToSqr(p.pos().getCenter()) > 24 * 24) {
            return;
        }
        Huisje h = Huisjes.op(player.level().getServer(), level.dimension(), p.pos());
        if (h == null) {
            return;
        }
        Actie actie = Actie.values()[Math.floorMod(p.actie(), Actie.values().length)];
        if (actie == Actie.BINNEN) {   // 1.3.2: everybody who can see the screen may go inside
            Binnen.vraag(player, h);
            return;
        }
        if (!Huisjes.magBewerken(player, h)) {   // 3.0: only the owner (or an op) changes a huisje
            player.sendOverlayMessage(Huisjes.vanWie(h).copy().withStyle(ChatFormatting.GRAY));
            return;
        }
        switch (actie) {
            case NAAM -> {
                if (!Huisjes.hernoem(player.level().getServer(), h, p.tekst())) {
                    player.sendOverlayMessage(Component.translatable("gui.guhs.huisje.naam_bezet").withStyle(ChatFormatting.GRAY));
                }
            }
            case TREK_IN -> {
                Entity e = level.getEntity(p.entity());
                if (e == null || e.distanceTo(player) > 24 || !Huisjes.trekIn(h, e)) {
                    player.sendOverlayMessage(Component.translatable(h.isVol() ? "gui.guhs.huisje.vol" : "gui.guhs.huisje.niet_jouw_guh", h.naamTekst())
                            .withStyle(ChatFormatting.GRAY));
                } else if (e instanceof TamableAnimal t && t.isOrderedToSit()) {
                    t.setOrderedToSit(false);   // (a new resident stands up: it has a home to look after)
                    t.setInSittingPose(false);
                }
            }
            case UIT -> {
                try {
                    Huisjes.trekUit(player.level().getServer(), h, UUID.fromString(p.id()));
                } catch (IllegalArgumentException ignored) {
                    // (not a band id)
                }
            }
            case MELDINGEN -> {
                h.zetMeldingen(p.aan());
                Huisjes.dirty();
            }
            case BINNEN -> {
                // (handled above)
            }
            case KLUS -> {
                try {
                    UUID id = UUID.fromString(p.id());
                    if (h.bewoners().contains(id) && Klusjes.van(p.tekst()) != null) {
                        h.zetKlus(id, p.tekst(), p.aan());
                    }
                } catch (IllegalArgumentException ignored) {
                    // (not a band id)
                }
            }
        }
        open(player, h);
    }

    private HuisjePayloads() {
    }
}
