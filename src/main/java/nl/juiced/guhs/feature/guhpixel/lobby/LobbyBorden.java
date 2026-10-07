package nl.juiced.guhs.feature.guhpixel.lobby;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.guhpixel.Grap;
import nl.juiced.guhs.feature.guhpixel.Grappen;
import nl.juiced.guhs.feature.guhpixel.Guhpixel;
import nl.juiced.guhs.feature.guhpixel.LobbyPlek;
import nl.juiced.guhs.feature.guhpixel.Muntjes;
import nl.juiced.guhs.feature.guhpixel.Rang;
import nl.juiced.guhs.feature.guhpixel.Sessie;
import nl.juiced.guhs.feature.guhpixel.Sessies;
import nl.juiced.guhs.quest.Scorebord;

/**
 * The boards and floating labels of the lobby.
 * <ul>
 *   <li>For everybody (text displays in the world, refreshed every 5 seconds): "Spelers online: n (en 47 guhs)" at
 *   BORD_ONLINE, the top 3 of the lobby parkour at its start, "TERUG NAAR HUIS" over the exit, "BINNENKORT, njeg" at the
 *   empty stall.</li>
 *   <li>Per player ({@link Zweeftekst}: only they see it): their own stats at BORD_STATS (rank, muntjes, joke games,
 *   golden knabbels, parkour best) and their own best time at the parkour start.</li>
 * </ul>
 * All positions are given relative to the spawn point, so the game tests can run them in a test room.
 */
public final class LobbyBorden {
    /** The fake crowd on the "Spelers online" board. */
    public static final int GUHS_ONLINE = 47;
    static final String STATS = "stats", PARKOUR = "parkour";
    private static final Set<UUID> METEEN = ConcurrentHashMap.newKeySet();

    /** A spot of the lobby, relative to a spawn point (the real one: {@link LobbyPlek#SPAWN}'s block). */
    static Vec3 plek(BlockPos oorsprong, double x, double y, double z) {
        return new Vec3(oorsprong.getX() + x, oorsprong.getY() + (y - 100), oorsprong.getZ() + z);
    }

    private static Vec3 bij(BlockPos oorsprong, LobbyPlek plek, double dx, double dy, double dz) {
        Vec3 p = plek.pos();
        return plek(oorsprong, p.x + dx, p.y + dy, p.z + dz);
    }

    /** Show this player's own boards again on the next tick (something changed). */
    public static void meteen(ServerPlayer p) {
        METEEN.add(p.getUUID());
    }

    static boolean moetMeteen(ServerPlayer p) {
        return METEEN.remove(p.getUUID());
    }

    // --- for everybody -------------------------------------------------------------------------------------------------------

    /** "Spelers online: 3 (en 47 guhs)" and where they are. */
    public static Component online(MinecraftServer server) {
        int inSpel = 0;
        for (Sessie s : Sessies.alle()) {
            inSpel += s.spelers().size();
        }
        int lobby = 0;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (Guhpixel.echt(p.level()) && Sessies.van(p) == null) {
                lobby++;
            }
        }
        return Component.empty()
                .append(Component.translatable("gui.guhs.lobby.bord.online.kop").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                .append("\n").append(Component.translatable("gui.guhs.lobby.bord.online.spelers", server.getPlayerCount(), GUHS_ONLINE).withStyle(ChatFormatting.WHITE))
                .append("\n").append(Component.translatable("gui.guhs.lobby.bord.online.waar", lobby, inSpel).withStyle(ChatFormatting.GRAY))
                .append("\n").append(Component.translatable("gui.guhs.lobby.bord.online.ping").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** The parkour's heading with the world's top 3. */
    public static Component parkourTop(MinecraftServer server) {
        return Scorebord.tekst(server, Component.translatable("gui.guhs.lobby.bord.parkour.kop"), List.of(LobbyParkour.BORD),
                List.of(Component.translatable("gui.guhs.lobby.bord.parkour.top")), LobbyParkour::tijdTekst);
    }

    /** Keeps the shared labels up to date (text displays in the world). */
    static void gedeeld(ServerLevel level, BlockPos oorsprong) {
        MinecraftServer server = level.getServer();
        Scorebord.show(level, bij(oorsprong, LobbyPlek.BORD_ONLINE, 0, 1.4, 2.0), "lobby_online", online(server));
        Scorebord.show(level, bij(oorsprong, LobbyPlek.PARKOUR_START, 0, 2.9, 0), "lobby_parkour", parkourTop(server));
        Scorebord.show(level, bij(oorsprong, LobbyPlek.UITGANG, 0, 4.6, 1.5), "lobby_uitgang",
                Component.translatable("gui.guhs.lobby.bord.uitgang").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        Scorebord.show(level, bij(oorsprong, LobbyPlek.SPEL_RESERVE, 0, 2.3, 0), "lobby_reserve",
                Component.empty().append(Component.translatable("gui.guhs.lobby.bord.reserve").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD))
                        .append("\n").append(Component.translatable("gui.guhs.lobby.bord.reserve.onder").withStyle(ChatFormatting.GRAY)));
    }

    // --- per player ----------------------------------------------------------------------------------------------------------

    /** This player's own stats board. */
    public static Component stats(ServerPlayer p) {
        Rang rang = Muntjes.rang(p);
        Rang volgende = rang.volgende();
        int klaar = 0;
        List<Grap> grappen = Grappen.alle();
        for (Grap g : grappen) {
            if (Grappen.isKlaar(p, g.id())) {
                klaar++;
            }
        }
        MutableComponent t = Component.empty()
                .append(Component.translatable("gui.guhs.lobby.bord.stats.kop").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append("\n").append(Component.empty().append(rang.naam()).append(" ").append(p.getName().copy().withStyle(ChatFormatting.WHITE)))
                .append("\n").append(Component.translatable("gui.guhs.lobby.bord.stats.muntjes", Muntjes.saldo(p), Muntjes.totaal(p)).withStyle(ChatFormatting.YELLOW))
                .append("\n").append((volgende == null ? Component.translatable("gui.guhs.lobby.bord.stats.rang_top")
                        : Component.translatable("gui.guhs.lobby.bord.stats.rang_volgende", volgende.naam(), Math.max(0, volgende.vanaf() - Muntjes.totaal(p))))
                        .withStyle(ChatFormatting.GRAY));
        if (!grappen.isEmpty()) {
            t.append("\n").append(Component.translatable("gui.guhs.lobby.bord.stats.grappen", klaar, grappen.size()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        t.append("\n").append(Component.translatable("gui.guhs.lobby.bord.stats.knabbels", Knabbels.gevonden(p), Knabbels.AANTAL).withStyle(ChatFormatting.GOLD));
        int best = LobbyParkour.best(p);
        t.append("\n").append((best > 0 ? Component.translatable("gui.guhs.lobby.bord.stats.parkour", LobbyParkour.tijdTekst(best))
                : Component.translatable("gui.guhs.lobby.bord.stats.parkour_geen")).withStyle(ChatFormatting.AQUA));
        return t;
    }

    /** This player's own line at the parkour start. */
    public static Component parkourEigen(ServerPlayer p) {
        int best = LobbyParkour.best(p);
        return (best > 0 ? Component.translatable("gui.guhs.lobby.bord.parkour.eigen", LobbyParkour.tijdTekst(best), LobbyParkour.keren(p))
                : Component.translatable("gui.guhs.lobby.bord.parkour.eigen_geen", LobbyParkour.MUNTJES)).withStyle(ChatFormatting.AQUA);
    }

    /** Where a player's own stats float: in front of the board behind the BORD_STATS anchor. */
    static Vec3 statsPlek(BlockPos oorsprong) {
        return bij(oorsprong, LobbyPlek.BORD_STATS, 0, 1.3, 2.0);
    }

    /** Shows (or updates) this player's own texts. */
    static void persoonlijk(ServerPlayer p, BlockPos oorsprong) {
        Zweeftekst.toon(p, STATS, statsPlek(oorsprong), stats(p));
        Zweeftekst.toon(p, PARKOUR, bij(oorsprong, LobbyPlek.PARKOUR_START, 0, 2.5, 0), parkourEigen(p));
    }

    private LobbyBorden() {
    }
}
