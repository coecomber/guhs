package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * De Erfenis, wereldkaart: THE SEAM to the core (feature/erfenis: Base, ErfenisData, Erfenis, Plot). The map only talks to
 * real bases through this interface; {@link KaartFeature#BRON} holds the one in use.
 * <ul>
 *   <li>Stand-alone (this branch): {@link KaartBronStandaard}: players get a cell of their own on the map, a visit only works
 *   once a dimension {@code guhs:erfenis} exists.</li>
 *   <li>MERGE: a {@code KaartBronErfenis} that reads {@code ErfenisData.alleBases()} (players = {@code !b.npc}), teleports with
 *   {@code Erfenis.gaNaar(p, base)}, opens the read-only overview, and turns every new/grown {@link NpcDorp} into a real base
 *   ({@code Base.npc(id, naam, cx, cz, List<Base.Plan>, bouwspul, nu)} in ITS cell). See the class comment of KaartFeature.</li>
 * </ul>
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

    /** An NPC village was founded or grew: make or update the real base behind it (stand-alone: nothing). */
    default void npcVeranderd(MinecraftServer server, NpcDorp dorp) {
    }
}
