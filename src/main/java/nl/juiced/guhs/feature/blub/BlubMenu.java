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
 * The Blub-guh's own right-click menu (server side; the screen is client.BlubMenuScreen). Bigger than a piep-maatje's: its
 * Blub-Guh-Meter on top, then name, strikje, volgen / blijven, op je schouder, oppakken, slaapliedje piepen, verstoppertje,
 * four trucjes and hupsen. Every button is a guhs:blub_actie; the owner, close by (the same rules as {@link PiepMenu#mag}).
 */
public final class BlubMenu {
    public enum Actie {
        /** Switch a setting (waarde = PiepInstelling ordinal: RONDVADSEN or VOLGEN). */
        WISSEL,
        /** Rename (tekst; empty = no name). */
        NAAM,
        /** Strikje (waarde: -1 off, 0-15 a DyeColor). */
        STRIK,
        SCHOUDER,
        OPPAKKEN,
        SLAAPLIEDJE,
        VERSTOPPERTJE,
        /** A trick (waarde 0 zitten, 1 rondje, 2 dood spelen, 3 hoge sprong). */
        TRUC,
        HUPSEN;

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
        if (blub.isBezig() && actie != Actie.NAAM && actie != Actie.STRIK) {
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
            case STRIK -> {
                blub.setStrik(waarde);
                blub.emotie(BlubGuhEntity.RECHTOP, 60);
                return true;
            }
            case SCHOUDER -> {
                blub.speciaal(player);
                return true;
            }
            case OPPAKKEN -> {
                return PiepDierItem.pakOp(blub, player);
            }
            case SLAAPLIEDJE -> {
                blub.slaapliedje(player);
                return true;
            }
            case VERSTOPPERTJE -> {
                return blub.verstoppertje(player);
            }
            case TRUC -> {
                blub.truc(player, Math.floorMod(waarde, 4));
                return true;
            }
            case HUPSEN -> {
                blub.hupsen(player);
                return true;
            }
        }
        return false;
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
    }
}
