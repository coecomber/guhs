package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * De Erfenis, wereldkaart: THE SEAM to the core (feature/erfenis: Base, ErfenisData, Erfenis, Plot). The map only talks to
 * real bases through this interface; {@link KaartFeature#BRON} holds the one in use.
 * The one in use is {@link KaartBronErfenis}: players = the core's non-NPC bases, a visit = {@code Erfenis.gaNaar} (+ the
 * read-only overview), and every new/grown {@link NpcDorp} becomes a real NPC base in ITS cell.
 */
public interface KaartBron {
    /** The real players' bases (no NPC villages: those come from {@link KaartData}). */
    List<KaartDorp> spelerDorpen(MinecraftServer server, long nu);

    /** The viewer's own base id (made on first use, so everybody who opens the map is on it), or null (no base possible). */
    @Nullable
    UUID eigenDorp(ServerPlayer p);

    /**
     * Takes the player to this village's plot as a visitor ({@code bovenaanzicht}: and opens its overview read-only).
     * {@code npc} is the NPC village when it is one (else null: a real player's base with id {@code dorp}).
     * False: not possible (the player gets a message).
     */
    boolean bezoek(ServerPlayer p, UUID dorp, @Nullable NpcDorp npc, boolean bovenaanzicht);

    /** An NPC village was founded or grew: make or update the real base behind it. */
    default void npcVeranderd(MinecraftServer server, NpcDorp dorp) {
    }

    /** A (POC) NPC village left the map: remove the base behind it (never a player's base). */
    default void npcWeg(MinecraftServer server, NpcDorp dorp) {
    }

    /** The Hoeve level of a real player's base (1 when unknown). */
    default int hoeveLevel(MinecraftServer server, UUID speler) {
        for (KaartDorp d : spelerDorpen(server, System.currentTimeMillis())) {
            if (d.id().equals(speler)) {
                return d.hoeve();
            }
        }
        return 1;
    }

    /** The level of the player's Marktplein (0: none yet), for the number of order carts. */
    default int marktpleinLevel(MinecraftServer server, UUID speler) {
        return 0;
    }

    /** Every cell that is taken by a real base (new NPC villages stay off these). Default: the players' cells. */
    default Set<Long> bezetteCellen(MinecraftServer server) {
        Set<Long> s = new HashSet<>();
        for (KaartDorp d : spelerDorpen(server, System.currentTimeMillis())) {
            s.add(KaartData.sleutel(d.cx(), d.cz()));
        }
        return s;
    }
}
