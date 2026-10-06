package nl.juiced.guhs.feature.guhpixel.among;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.guhpixel.Grappen;
import nl.juiced.guhs.feature.guhpixel.Guhpixel;
import nl.juiced.guhs.feature.guhpixel.LobbyPlek;
import nl.juiced.guhs.feature.guhpixel.Sessie;
import nl.juiced.guhs.feature.guhpixel.Sessies;
import nl.juiced.guhs.feature.guhpixel.Toegang;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.taal.Tekst;

/**
 * The queue at the Kapitein-guh. Whoever talks to him joins the group that is waiting there; the first one is the leader
 * and picks the difficulty (Normaal: 9 participants and 1 Mika; Lastig: 10 and 2 Mikas, more muntjes). Everybody presses
 * "klaar"; when EVERY real player in the queue is ready the round starts in its own ship (alone = at once) and the queue
 * is free for the next group, so any number of groups play at the same time. Empty places are filled with guh NPCs.
 * <p>
 * A player leaves the queue with the button, by walking away from the Kapitein, by leaving the lobby or by logging out.
 * Until the parody round exists as a joke game (Grappen id "among") the queue is open to everybody who unlocked Guhpixel;
 * once it is registered it must be finished first.
 */
public final class AmongWachtrij {
    public static final int MAX_NORMAAL = 9, MAX_LASTIG = 10;
    private static final double MAX_AFSTAND = 14.0;

    private static final List<UUID> RIJ = new ArrayList<>();
    private static final Set<UUID> KLAAR = new HashSet<>();
    private static boolean lastig;
    /** How a full queue becomes a round (tests replace it: the game test server has no guhpixel dimension). */
    public static volatile BiFunction<List<ServerPlayer>, Boolean, Sessie> starter = AmongWachtrij::start;

    private static Sessie start(List<ServerPlayer> spelers, boolean lastig) {
        CompoundTag opties = new CompoundTag();
        opties.putBoolean("Lastig", lastig);
        return Sessies.start(AmongSlice.SPEL, spelers, opties);
    }

    /** May this player play the real game already? */
    public static boolean magSpelen(ServerPlayer p) {
        return Toegang.heeft(p) && (Grappen.van("among") == null || Grappen.isKlaar(p, "among"));
    }

    public static boolean inRij(ServerPlayer p) {
        return RIJ.contains(p.getUUID());
    }

    public static int grootte() {
        return RIJ.size();
    }

    public static boolean lastig() {
        return lastig;
    }

    @Nullable
    public static UUID leider() {
        return RIJ.isEmpty() ? null : RIJ.get(0);
    }

    /** The player joins the queue (or gets the screen again). False when refused (the reason was shown). */
    public static boolean erbij(ServerPlayer p) {
        if (!magSpelen(p)) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.among.wachtrij.eerst_oefenen").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        if (Sessies.van(p) != null) {
            return false;
        }
        if (!RIJ.contains(p.getUUID())) {
            if (RIJ.size() >= (lastig ? MAX_LASTIG : MAX_NORMAAL)) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.among.wachtrij.vol").withStyle(ChatFormatting.LIGHT_PURPLE));
                return false;
            }
            if (RIJ.isEmpty()) {
                lastig = false;
            }
            RIJ.add(p.getUUID());
        }
        stuur(p.level().getServer(), p);
        return true;
    }

    public static void weg(ServerPlayer p) {
        if (RIJ.remove(p.getUUID())) {
            KLAAR.remove(p.getUUID());
            ModNetworking.sendTo(p, new AmongPayloads.Scherm(AmongPayloads.WACHTRIJ, dicht()));
            stuur(p.level().getServer(), null);
            misschienStart(p.level().getServer());
        }
    }

    private static CompoundTag dicht() {
        CompoundTag t = new CompoundTag();
        t.putBoolean("Dicht", true);
        return t;
    }

    /** A button of the queue screen. */
    public static void actie(ServerPlayer p, int actie) {
        if (!RIJ.contains(p.getUUID())) {
            return;
        }
        MinecraftServer server = p.level().getServer();
        switch (actie) {
            case AmongPayloads.WACHTRIJ_KLAAR -> {
                if (!KLAAR.remove(p.getUUID())) {
                    KLAAR.add(p.getUUID());
                }
                stuur(server, null);
                misschienStart(server);
            }
            case AmongPayloads.WACHTRIJ_NIVEAU -> {
                if (p.getUUID().equals(leider())) {
                    lastig = !lastig || RIJ.size() > MAX_NORMAAL;
                    KLAAR.clear();      // the rules changed: everybody says "klaar" again
                    stuur(server, null);
                }
            }
            case AmongPayloads.WACHTRIJ_WEG -> weg(p);
            default -> {
            }
        }
    }

    private static List<ServerPlayer> spelers(MinecraftServer server) {
        List<ServerPlayer> uit = new ArrayList<>();
        for (UUID id : RIJ) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                uit.add(p);
            }
        }
        return uit;
    }

    private static void misschienStart(MinecraftServer server) {
        if (RIJ.isEmpty() || !KLAAR.containsAll(RIJ)) {
            return;
        }
        List<ServerPlayer> spelers = spelers(server);
        if (spelers.size() != RIJ.size()) {
            return;
        }
        boolean moeilijk = lastig;
        Sessie s = starter.apply(spelers, moeilijk);
        if (s == null) {
            KLAAR.clear();
            stuur(server, null);
            return;
        }
        for (ServerPlayer p : spelers) {
            ModNetworking.sendTo(p, new AmongPayloads.Scherm(AmongPayloads.WACHTRIJ, dicht()));
        }
        RIJ.clear();
        KLAAR.clear();
        lastig = false;
    }

    /** The state of the queue as this player sees it. */
    public static CompoundTag stand(MinecraftServer server, ServerPlayer voor, boolean open) {
        CompoundTag t = new CompoundTag();
        t.putBoolean("Open", open);
        t.putBoolean("Lastig", lastig);
        t.putBoolean("Leider", voor.getUUID().equals(leider()));
        t.putBoolean("Klaar", KLAAR.contains(voor.getUUID()));
        t.putInt("Deelnemers", lastig ? MAX_LASTIG : MAX_NORMAAL);
        t.putInt("Mikas", lastig ? 2 : 1);
        ListTag lijst = new ListTag();
        for (ServerPlayer p : spelers(server)) {
            CompoundTag k = new CompoundTag();
            Tekst.put(k, "Naam", p.getName());
            k.putBoolean("Klaar", KLAAR.contains(p.getUUID()));
            k.putBoolean("Leider", p.getUUID().equals(leider()));
            lijst.add(k);
        }
        t.put("Spelers", lijst);
        return t;
    }

    /** Sends the state to everybody in the queue; the screen opens for {@code open}. */
    private static void stuur(MinecraftServer server, @Nullable ServerPlayer open) {
        for (ServerPlayer p : spelers(server)) {
            ModNetworking.sendTo(p, new AmongPayloads.Scherm(AmongPayloads.WACHTRIJ, stand(server, p, p == open)));
        }
    }

    /** Every second: whoever walked away, left the lobby, logged out or is in a game is out of the queue. */
    static void tick(MinecraftServer server) {
        if (RIJ.isEmpty() || server.getTickCount() % 20 != 0) {
            return;
        }
        boolean veranderd = false;
        for (UUID id : new ArrayList<>(RIJ)) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            boolean blijft = p != null && p.isAlive() && Sessies.van(p) == null && Guhpixel.in(p)
                    && (!Guhpixel.echt(p.level()) || p.distanceToSqr(LobbyPlek.SPEL_AMONG.pos()) <= MAX_AFSTAND * MAX_AFSTAND);
            if (!blijft) {
                RIJ.remove(id);
                KLAAR.remove(id);
                veranderd = true;
                if (p != null) {
                    ModNetworking.sendTo(p, new AmongPayloads.Scherm(AmongPayloads.WACHTRIJ, dicht()));
                    p.sendOverlayMessage(Component.translatable("gui.guhs.among.wachtrij.eruit").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
        }
        if (veranderd) {
            stuur(server, null);
            misschienStart(server);
        }
    }

    /** (Tests, a server stop) forgets the queue. */
    public static void leeg() {
        RIJ.clear();
        KLAAR.clear();
        lastig = false;
    }

    private AmongWachtrij() {
    }
}
