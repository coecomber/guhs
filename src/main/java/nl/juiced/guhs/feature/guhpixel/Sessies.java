package nl.juiced.guhs.feature.guhpixel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.logging.LogUtils;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Minigames;
import org.slf4j.Logger;

/**
 * The running games ({@link Sessie}) and their kinds ({@link SpelSoort}). Every session has its own arena cell; any number
 * run at once. All sessions together are the one game id {@link #SPEL_ID} for {@link Minigames} (one game at a time).
 * <p>
 * Leaving (any way: done, /lobby, logout, death, another dimension, the server stops) gives the own inventory back at
 * once, before the player is saved ({@link Kluis}).
 */
public final class Sessies {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String SPEL_ID = "guhpixel";
    private static final Map<String, SpelSoort> SOORTEN = new LinkedHashMap<>();
    private static final Map<UUID, Sessie> ACTIEF = new LinkedHashMap<>();
    private static final Map<UUID, Sessie> VAN_SPELER = new java.util.HashMap<>();

    public static void registreer(SpelSoort soort) {
        SOORTEN.put(soort.id(), soort);
        Arenas.registreer(soort.arena());
    }

    @Nullable
    public static SpelSoort soort(String id) {
        return SOORTEN.get(id);
    }

    public static Collection<SpelSoort> soorten() {
        return List.copyOf(SOORTEN.values());
    }

    @Nullable
    public static Sessie van(ServerPlayer p) {
        return VAN_SPELER.get(p.getUUID());
    }

    public static Collection<Sessie> alle() {
        return List.copyOf(ACTIEF.values());
    }

    /** How many real players are in a game of this kind right now. */
    public static int aantalSpelers(String soortId) {
        int n = 0;
        for (Sessie s : ACTIEF.values()) {
            if (s.soort().id().equals(soortId)) {
                n += s.spelers().size();
            }
        }
        return n;
    }

    /** Why these players cannot start this game now (null: they can). */
    @Nullable
    public static Component weigering(SpelSoort soort, List<ServerPlayer> spelers) {
        if (spelers.size() < soort.minSpelers() || spelers.size() > soort.maxSpelers()) {
            return Component.translatable("gui.guhs.guhpixel.spel.aantal", soort.minSpelers(), soort.maxSpelers());
        }
        for (ServerPlayer p : spelers) {
            if (!Toegang.heeft(p)) {
                return Component.translatable("gui.guhs.guhpixel.spel.op_slot", p.getDisplayName());
            }
            if (van(p) != null || Minigames.playing(p) != null || Kluis.heeft(p)) {
                return Component.translatable("gui.guhs.guhpixel.spel.bezig", p.getDisplayName());
            }
            if (!p.isAlive() || p.isSpectator() || !Guhpixel.in(p)) {
                return Component.translatable("gui.guhs.guhpixel.spel.niet_hier", p.getDisplayName());
            }
        }
        return null;
    }

    /** Starts a game in a grid arena. Null = refused; the reason was shown to the players. */
    @Nullable
    public static Sessie start(SpelSoort soort, List<ServerPlayer> spelers, CompoundTag opties) {
        if (spelers.isEmpty()) {
            return null;
        }
        Component nee = weigering(soort, spelers);
        ServerLevel level = Guhpixel.level(spelers.get(0).level().getServer());
        if (nee == null && level == null) {
            nee = Component.translatable("gui.guhs.guhpixel.binnen.storing");
        }
        Arena arena = nee == null ? Arenas.neem(level, soort.arena()) : null;
        if (nee == null && arena == null) {
            nee = Component.translatable("gui.guhs.guhpixel.binnen.storing");
        }
        if (nee != null) {
            for (ServerPlayer p : spelers) {
                p.sendOverlayMessage(nee.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            return null;
        }
        return begin(soort, arena, spelers, opties);
    }

    /** (Tests) starts a game in an arena stamped at a given spot of any level. Null = refused. */
    @Nullable
    public static Sessie startOp(ServerLevel level, BlockPos oorsprong, SpelSoort soort, List<ServerPlayer> spelers, CompoundTag opties) {
        if (spelers.isEmpty() || weigering(soort, spelers) != null) {
            return null;
        }
        Arena arena = Arenas.neemOp(level, oorsprong, soort.arena());
        return arena == null ? null : begin(soort, arena, spelers, opties);
    }

    private static Sessie begin(SpelSoort soort, Arena arena, List<ServerPlayer> spelers, CompoundTag opties) {
        Sessie s = soort.maker().apply(new SessieStart(UUID.randomUUID(), soort, arena, List.copyOf(spelers), opties == null ? new CompoundTag() : opties));
        ACTIEF.put(s.id(), s);
        Vec3 start = arena.start();
        for (ServerPlayer p : spelers) {
            VAN_SPELER.put(p.getUUID(), s);
            p.stopRiding();
            p.ejectPassengers();
            Kluis.bewaar(p);
            p.teleportTo(arena.level(), start.x, start.y, start.z, Set.of(), arena.soort().startYaw(), 0f, true);
            p.resetFallDistance();
            s.uitrusting(p);
            p.inventoryMenu.broadcastChanges();
        }
        try {
            s.begin();
        } catch (RuntimeException e) {
            LOGGER.error("Guhpixel: game {} failed in begin()", soort.id(), e);
            stop(s, Vertrek.GESTOPT);
        }
        return s;
    }

    /** This player leaves their game (nothing happens when they are in none). */
    public static void verlaat(ServerPlayer p, Vertrek reden) {
        Sessie s = VAN_SPELER.remove(p.getUUID());
        if (s == null) {
            return;
        }
        uit(s, p, reden);
        if (s.leeg() && !s.gestopt) {
            stop(s, Vertrek.GESTOPT);
        }
    }

    private static void uit(Sessie s, ServerPlayer p, Vertrek reden) {
        s.verwijder(p);
        VAN_SPELER.remove(p.getUUID());
        Kluis.herstel(p);
        if (reden.naarLobby() && p.isAlive()) {
            ServerLevel lobby = Guhpixel.level(p.level().getServer());
            if (lobby != null && Guhpixel.echt(s.level())) {
                Vec3 voor = s.soort().plek().voor();
                p.stopRiding();
                p.teleportTo(lobby, voor.x, voor.y, voor.z, Set.of(), s.soort().plek().voorYaw(), 0f, true);
                p.resetFallDistance();
            }
        }
        try {
            s.spelerWeg(p, reden);
        } catch (RuntimeException e) {
            LOGGER.error("Guhpixel: game {} failed in spelerWeg()", s.soort().id(), e);
        }
        GuhpixelPayloads.hud(p);
    }

    /** Everybody out (with this reason), einde(), the arena goes back. */
    static void stop(Sessie s, Vertrek reden) {
        if (s.gestopt) {
            return;
        }
        s.gestopt = true;
        for (ServerPlayer p : s.spelers()) {
            uit(s, p, reden);
        }
        try {
            s.einde();
        } catch (RuntimeException e) {
            LOGGER.error("Guhpixel: game {} failed in einde()", s.soort().id(), e);
        }
        ACTIEF.remove(s.id());
        Arenas.geefTerug(s.arena());
    }

    /** Every server tick. */
    static void tick(MinecraftServer server) {
        if (ACTIEF.isEmpty()) {
            return;
        }
        for (Sessie s : new ArrayList<>(ACTIEF.values())) {
            if (s.gestopt) {
                continue;
            }
            // a player who is gone without an event (removed, another level): out
            for (ServerPlayer p : s.spelers()) {
                if (p.isRemoved() || p.hasDisconnected()) {
                    verlaat(p, Vertrek.UITGELOGD);
                } else if (p.level() != s.level()) {
                    verlaat(p, Vertrek.DIMENSIE);
                }
            }
            if (s.gestopt) {
                continue;
            }
            if (s.leeg()) {
                stop(s, Vertrek.GESTOPT);
                continue;
            }
            s.ticks++;
            try {
                s.tick();
            } catch (RuntimeException e) {
                LOGGER.error("Guhpixel: game {} failed in tick(), stopping it", s.soort().id(), e);
                stop(s, Vertrek.GESTOPT);
            }
        }
    }

    /** The server stops (or a test tidies up): every game ends, every inventory goes back. */
    static void stopAlles(Vertrek reden) {
        for (Sessie s : new ArrayList<>(ACTIEF.values())) {
            stop(s, reden);
        }
        ACTIEF.clear();
        VAN_SPELER.clear();
    }

    /**
     * A player logs in: a snapshot without a running game (a crash, a stop in the middle of a game) is restored and the
     * player goes to the lobby spawn. True when something was restored.
     */
    static boolean opLogin(ServerPlayer p) {
        if (van(p) != null || !Kluis.heeft(p) || p.isDeadOrDying()) {
            return false;   // (dead: the safe opens at the respawn, see Regels.onLogin)
        }
        Kluis.herstel(p);
        ServerLevel lobby = Guhpixel.level(p.level().getServer());
        if (lobby != null && Guhpixel.echt(p.level()) && Lobby.zorg(lobby)) {
            Vec3 spawn = LobbyPlek.SPAWN.pos();
            p.teleportTo(lobby, spawn.x, spawn.y, spawn.z, Set.of(), LobbyPlek.SPAWN.yaw(), 0f, true);
        }
        p.sendSystemMessage(Component.translatable("gui.guhs.guhpixel.kluis.terug").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    private Sessies() {
    }
}
