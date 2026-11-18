package nl.juiced.guhs.feature.guhwinter.tekenen;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.juiced.guhs.feature.guhwinter.Guhwinter;

/**
 * The Kerstalbum: every player who logs in from 26 Dec up to and including 2 Jan gets ONE (once; also when they were already
 * online when 26 Dec began). Opening it shows a book of all kerstboom drawings with their makers; each page has
 * "Ophangen-versie pakken": that drawing as a hangable 2x2 item, once per drawing per player (kept server side in
 * {@link TekeningenData}). The album itself is permanent.
 */
public final class Kerstalbum {
    private Kerstalbum() {
    }

    /** The album item: right-click to browse. */
    public static class AlbumItem extends Item {
        public AlbumItem(Properties properties) {
            super(properties);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer sp) {
                open(sp, 0);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                    java.util.function.Consumer<net.minecraft.network.chat.Component> builder, net.minecraft.world.item.TooltipFlag flag) {
            builder.accept(net.minecraft.network.chat.Component.translatable("item.guhs.guhwinter_kerstalbum.lore")
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    /** Gives the album when it's album time and this player never got one. */
    public static void geefAlsNodig(ServerPlayer speler) {
        MinecraftServer server = speler.level().getServer();
        TekeningenData data = TekeningenData.get(server);
        if (!Guhwinter.albumOpen(server) || data.albumGekregen(speler.getUUID())) {
            return;
        }
        geef(speler);
    }

    /** Gives an album and marks it (dev: also outside the album days). */
    public static void geef(ServerPlayer speler) {
        TekeningenData.get(speler.level().getServer()).markeerAlbum(speler.getUUID());
        Tekeningen.geef(speler, new ItemStack(TekenenFeature.KERSTALBUM.get()));
        speler.sendSystemMessage(Tekeningen.melding("guhwinter.tekenen.album.gekregen"));
        speler.level().playSound(null, speler.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.4f);
    }

    /** Kern's moment {@code album} (26 Dec, also on catch-up): everyone online gets theirs; the others at their login. */
    static void opAlbumMoment(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            geefAlsNodig(p);
        }
    }

    public static void open(ServerPlayer speler, int pagina) {
        MinecraftServer server = speler.level().getServer();
        TekeningenData data = TekeningenData.get(server);
        List<TekenPayloads.AlbumRij> rijen = new ArrayList<>();
        for (Tekening t : data.kerstboomTekeningen()) {
            rijen.add(new TekenPayloads.AlbumRij(t.id(), t.makerNaam(), data.gepakt(speler.getUUID(), t.id())));
        }
        PacketDistributor.sendToPlayer(speler, new TekenPayloads.AlbumOpen(rijen, Math.max(0, Math.min(pagina, rijen.size() - 1))));
    }

    static void pak(ServerPlayer speler, UUID id, int pagina) {
        MinecraftServer server = speler.level().getServer();
        TekeningenData data = TekeningenData.get(server);
        Tekening t = data.get(id);
        if (t == null || !data.isKerstboom(id) || !heeftAlbum(speler)) {
            return;
        }
        if (data.pak(speler.getUUID(), id)) {
            Tekeningen.geef(speler, Tekeningen.maakItem(id, t.makerNaam()));
            speler.sendOverlayMessage(Tekeningen.melding("guhwinter.tekenen.album.gepakt", t.makerNaam()));
            speler.level().playSound(null, speler.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.7f, 1.2f);
        }
        open(speler, pagina);
    }

    private static boolean heeftAlbum(ServerPlayer speler) {
        return speler.getInventory().contains(s -> s.is(TekenenFeature.KERSTALBUM.get()));
    }
}
