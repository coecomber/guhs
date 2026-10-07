package nl.juiced.guhs.feature.einde2.vetbootje;

import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.Guhs;

/**
 * einde2, slice vetbootje: the scheepswerf (menu guhs:einde2_vetbootje_werf): no slots, only what the boat looks like now
 * (colour, sail, name), for the screen ({@code client.WerfScreen}) to start from. The screen answers with {@link Kies};
 * {@link Vetbootje#openWerf} opens it (the werfboekje now, the shipyard of the Leegtehaven in phase B).
 * <p>
 * Also here: the balloon rider's burner message ({@link Brander}).
 */
public class WerfMenu extends AbstractContainerMenu {
    public final int kleur, zeil;
    public final String naam;

    public WerfMenu(int id, int kleur, int zeil, String naam) {
        super(E2VetbootjeFeature.WERF_MENU.get(), id);
        this.kleur = kleur;
        this.zeil = zeil;
        this.naam = naam;
    }

    /** The client's copy, from what {@link Vetbootje#openWerf} wrote. */
    public static WerfMenu client(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new WerfMenu(id, buf.readVarInt(), buf.readVarInt(), buf.readUtf(64));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToServer(Kies.TYPE, Kies.STREAM_CODEC, Kies::handle);
        registrar.playToServer(Brander.TYPE, Brander.STREAM_CODEC, Brander::handle);
    }

    /** A typed name made safe: no control or colour codes, single spaces, at most {@link Vetbootje#NAAM_MAX} long. */
    public static String schoon(String naam) {
        StringBuilder out = new StringBuilder();
        for (char c : naam.toCharArray()) {
            if (c >= ' ' && c != '§' && c != 127) {
                out.append(c);
            }
        }
        String s = out.toString().trim().replaceAll("\\s+", " ");
        return s.length() > Vetbootje.NAAM_MAX ? s.substring(0, Vetbootje.NAAM_MAX).trim() : s;
    }

    /**
     * The player chose at the werf: checked and put on their boat (an empty name = the default name again). False when
     * they have no boat or sent nonsense.
     */
    public static boolean kies(ServerPlayer player, int kleur, int zeil, String naam) {
        if (!Vetbootje.heeft(player) || kleur < 0 || kleur > 15 || zeil < 0 || zeil >= Zeil.values().length) {
            return false;
        }
        String schoon = schoon(naam);
        Vetbootje.stel(player, DyeColor.byId(kleur), Zeil.values()[zeil], schoon.isEmpty() ? null : Component.literal(schoon));
        player.level().playSound(null, player.blockPosition(), E2VetbootjeFeature.VERF.get(), SoundSource.PLAYERS, 0.9f, 1f);
        player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_vetbootje.werf.klaar").withStyle(ChatFormatting.GOLD));
        return true;
    }

    /** Client -> server: the werf screen's "Verf het!" (colour 0..15 = DyeColor id, sail = Zeil ordinal, the typed name). */
    public record Kies(int kleur, int zeil, String naam) implements CustomPacketPayload {
        public static final Type<Kies> TYPE = new Type<>(Guhs.id("einde2_vetbootje_werf_kies"));
        public static final StreamCodec<ByteBuf, Kies> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Kies::kleur,
                ByteBufCodecs.VAR_INT, Kies::zeil, ByteBufCodecs.stringUtf8(64), Kies::naam, Kies::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Kies p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof WerfMenu) {
                kies(player, p.kleur(), p.zeil(), p.naam());
                player.closeContainer();
            }
        }
    }

    /** Client -> server: the rider of this balloon turned the burner on or off. */
    public record Brander(int ballon, boolean aan) implements CustomPacketPayload {
        public static final Type<Brander> TYPE = new Type<>(Guhs.id("einde2_vetbootje_brander"));
        public static final StreamCodec<ByteBuf, Brander> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Brander::ballon,
                ByteBufCodecs.BOOL, Brander::aan, Brander::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Brander p, IPayloadContext context) {
            if (context.player() instanceof ServerPlayer player && player.getVehicle() instanceof HeteluchtballonEntity ballon
                    && ballon.getId() == p.ballon() && ballon.getControllingPassenger() == player) {
                ballon.zetBrander(p.aan());
            }
        }
    }
}
