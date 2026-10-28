package nl.juiced.guhs.feature.blub;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.piep.PiepDierItem;
import nl.juiced.guhs.feature.piep.PiepInstelling;
import nl.juiced.guhs.feature.piep.PiepMenu;
import nl.juiced.guhs.network.ModNetworking;

/**
 * The Blub-guh's own right-click menu (server side; the screens are client.BlubMenuScreen, BlubTrucScreen and
 * BlubKledingkastScreen): its Blub-Guh-Meter on top, then name, volgen / blijven, Kledingkast, Trucjes (every trick and
 * emote: {@link BlubTruc}), op je schouder, oppakken. Every button is a guhs:blub_actie; the owner, close by (the same
 * rules as {@link PiepMenu#mag}).
 */
public final class BlubMenu {
    public enum Actie {
        /** Switch a setting (waarde = PiepInstelling ordinal: RONDVADSEN or VOLGEN). */
        WISSEL,
        /** Rename (tekst; empty = no name). */
        NAAM,
        SCHOUDER,
        OPPAKKEN,
        /** A trick or emote of its Trucjes screen (waarde = BlubTruc ordinal). */
        TRUC,
        /** Its Kledingkast (waarde = BlubKleding.Plek ordinal, tekst = the piece id; empty = take it off). */
        KLEED,
        /** 1.8.0: "Knuffel laten maken": a Blub-knuffel of this Blub, for one wool. */
        KNUFFEL;

        public static Actie byIndex(int i) {
            Actie[] all = values();
            return all[Math.floorMod(i, all.length)];
        }
    }

    /** Client: opens the screen (set by client.BlubClient; a dedicated server never gets the payload). */
    public static volatile java.util.function.Consumer<MenuOpen> menuOpener = m -> {
    };

    private BlubMenu() {
    }

    public static void open(ServerPlayer player, BlubGuhEntity blub) {
        if (blub.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HEART, blub.getX(), blub.getY() + blub.getBbHeight() + 0.15, blub.getZ(), 1, 0.15, 0.05, 0.15, 0);
        }
        ModNetworking.sendTo(player, new MenuOpen(blub.getId(), blub.hupsRecord()));
    }

    /** A button (after the checks). Returns whether something happened. */
    public static boolean doe(ServerPlayer player, BlubGuhEntity blub, Actie actie, int waarde, String tekst) {
        if (!PiepMenu.mag(player, blub)) {
            return false;
        }
        if (blub.isBezig() && actie != Actie.NAAM && actie != Actie.KLEED) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.piep.even_bezig", blub.getDisplayName()).withStyle(ChatFormatting.GRAY));
            return false;
        }
        switch (actie) {
            case WISSEL -> {
                PiepInstelling instelling = PiepInstelling.byIndex(waarde);
                if (!blub.instellingen().contains(instelling)) {
                    return false;
                }
                boolean nu = !blub.aan(instelling);
                blub.zet(instelling, nu);
                player.sendOverlayMessage(Component.translatable(instelling.key() + (nu ? ".aan" : ".uit"), blub.getDisplayName())
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                return true;
            }
            case NAAM -> {
                PiepMenu.hernoem(blub, tekst);
                return true;
            }
            case SCHOUDER -> {
                blub.speciaal(player);
                return true;
            }
            case OPPAKKEN -> {
                return PiepDierItem.pakOp(blub, player);
            }
            case TRUC -> {
                BlubTruc.byIndex(waarde).doe(blub, player);
                return true;
            }
            case KNUFFEL -> {
                return knuffel(player, blub);
            }
            case KLEED -> {
                BlubKleding.Plek plek = BlubKleding.Plek.values()[Math.floorMod(waarde, BlubKleding.Plek.values().length)];
                boolean uit = tekst == null || tekst.isEmpty();
                BlubKleding.Stuk stuk = uit ? null : BlubKleding.stuk(tekst);
                if (!uit && (stuk == null || stuk.plek() != plek || !BlubKleding.mag(blub, stuk))) {
                    return false;
                }
                blub.kleed(plek, stuk);
                return true;
            }
        }
        return false;
    }

    /** The price of a Blub-knuffel: one wool, any colour (free in creative). */
    public static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> KNUFFEL_PRIJS = net.minecraft.tags.ItemTags.WOOL;

    /** "Knuffel laten maken": takes one wool and gives a Blub-knuffel of this Blub (its soort, meter, tint, strikje, name). */
    static boolean knuffel(ServerPlayer player, BlubGuhEntity blub) {
        if (!player.getAbilities().instabuild) {
            net.minecraft.world.item.ItemStack wol = net.minecraft.world.item.ItemStack.EMPTY;
            for (net.minecraft.world.item.ItemStack st : player.getInventory().getNonEquipmentItems()) {
                if (st.is(KNUFFEL_PRIJS)) {
                    wol = st;
                    break;
                }
            }
            if (wol.isEmpty()) {
                player.sendOverlayMessage(Component.translatable("gui.guhs.blubguh.knuffel.geen_wol").withStyle(ChatFormatting.GRAY));
                return false;
            }
            wol.shrink(1);
        }
        net.minecraft.world.item.ItemStack knuffel = new net.minecraft.world.item.ItemStack(BlubFeature.KNUFFEL_ITEM.get());
        nl.juiced.guhs.feature.blub.knuffel.BlubKnuffelBlockEntity.schrijf(knuffel, blub.knuffelData());
        if (!player.getInventory().add(knuffel)) {
            player.drop(knuffel, false);
        }
        BlubGeluid.SQUISH.speel(blub);
        player.sendOverlayMessage(Component.translatable("gui.guhs.blubguh.knuffel.gemaakt", blub.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    // --- payloads ----------------------------------------------------------------------------------------------------------------

    /** Server to client: open the menu of this Blub-guh (with its hupsen record). */
    public record MenuOpen(int blub, int record) implements CustomPacketPayload {
        public static final Type<MenuOpen> TYPE = new Type<>(Guhs.id("blub_menu"));
        public static final StreamCodec<FriendlyByteBuf, MenuOpen> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, MenuOpen::blub, ByteBufCodecs.VAR_INT, MenuOpen::record, MenuOpen::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(MenuOpen p, IPayloadContext context) {
            menuOpener.accept(p);
        }
    }

    /** Client to server: a button ({@link Actie}; waarde = setting / colour / trick, tekst = the new name). */
    public record MenuActie(int blub, int actie, int waarde, String tekst) implements CustomPacketPayload {
        public static final Type<MenuActie> TYPE = new Type<>(Guhs.id("blub_actie"));
        public static final StreamCodec<FriendlyByteBuf, MenuActie> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, MenuActie::blub, ByteBufCodecs.VAR_INT, MenuActie::actie, ByteBufCodecs.VAR_INT, MenuActie::waarde,
                ByteBufCodecs.stringUtf8(PiepMenu.MAX_NAAM), MenuActie::tekst, MenuActie::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(MenuActie p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer sp && sp.level().getEntity(p.blub()) instanceof BlubGuhEntity blub) {
                doe(sp, blub, Actie.byIndex(p.actie()), p.waarde(), p.tekst());
            }
        }
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playToClient(MenuOpen.TYPE, MenuOpen.STREAM_CODEC, MenuOpen::handle);
        registrar.playToServer(MenuActie.TYPE, MenuActie.STREAM_CODEC, MenuActie::handle);
        BlubSnoes.register(registrar);
    }
}
