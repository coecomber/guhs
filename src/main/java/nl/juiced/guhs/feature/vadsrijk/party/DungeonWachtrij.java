package nl.juiced.guhs.feature.vadsrijk.party;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepRol;

/**
 * W-PARTY: what the party code needs from the dungeon finder (w-grot's {@code dungeon.DungeonRij}). Integration sets
 * {@link Wachtrijen#DUNGEON} to an adapter (see NOTITIES_W_PARTY.md, "Koppeling met w-grot"); without it the Party tab only offers
 * the raid and the nep-spelers never fill a queue.
 */
public interface DungeonWachtrij {
    /** A made-up queue member that fills an empty spot (always answers the ready check with yes). */
    record NepLid(UUID id, String naam, NepRol rol) {
    }

    /** The dungeon ids the Party tab offers, in order. */
    List<String> dungeons();

    Component naam(String dungeon);

    /** The leader queues the whole party (the online members in the Vadsrijk, leader first). null = done, else why not. */
    @Nullable
    Component partyInRij(ServerPlayer leider, List<ServerPlayer> leden, String dungeon);

    /** The dungeon whose queue this player is in (null: none). */
    @Nullable
    String rijVan(UUID speler);

    /** Every queue now: dungeon id -> the queued ids in order (real players and made-up members). */
    Map<String, List<UUID>> rijen();

    /** The group size a queue fills up to. */
    int groepMax();

    /** True while this dungeon cannot take a new group (a group is inside or a ready check runs): no filling then. */
    boolean bezet(String dungeon);

    /** Is this queue id a made-up member (no player behind it)? */
    boolean isNep(UUID id);

    /** Adds made-up members to this dungeon's queue (they answer "yes"); the queue then starts its ready check when full. */
    boolean voegNepToe(String dungeon, List<NepLid> nep);

    /** The dungeon this player is inside now (null: none). Companions that came along leave when their player is out. */
    @Nullable
    String dungeonVan(UUID speler);
}
