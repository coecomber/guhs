package nl.juiced.guhs.feature.vadsrijk.party;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;
import nl.juiced.guhs.feature.vadsrijk.zone6.raid.Z6Raid;

/**
 * W-PARTY: group queueing from the Party tab. The party leader puts the whole party (the online members in the Vadsrijk) in the
 * queue of a dungeon ({@link #DUNGEON}, w-grot's finder through an adapter) or of the raid ({@link Z6Raid#partyInRij}).
 */
public final class Wachtrijen {
    /** Set by integration (an adapter around w-grot's DungeonRij); null = no dungeon finder yet. */
    @Nullable
    public static DungeonWachtrij DUNGEON;
    /** Gametests: no Vadsrijk check for the members. */
    public static boolean TEST;

    private Wachtrijen() {
    }

    /** The leader + the online members in the Vadsrijk (leader first); an empty list when p is no leader. */
    public static List<ServerPlayer> groep(ServerPlayer p) {
        List<ServerPlayer> uit = new ArrayList<>();
        Partijen.Party party = Partijen.party(p);
        if (party == null) {
            uit.add(p);
            return uit;
        }
        if (!party.leider.equals(p.getUUID())) {
            return uit;
        }
        uit.add(p);
        for (ServerPlayer l : Partijen.leden(p)) {
            if (l != p && (TEST || Vadsrijk.in(l))) {
                uit.add(l);
            }
        }
        return uit;
    }

    /** The leader queues the party for a dungeon. Returns null when queued, else the reason. */
    @Nullable
    public static Component dungeon(ServerPlayer p, String dungeon) {
        if (DUNGEON == null) {
            return Component.translatable("vadsrijk.party.rij.geen_dungeons");
        }
        List<ServerPlayer> wie = groep(p);
        if (wie.isEmpty()) {
            return Component.translatable("vadsrijk.kern.party.niet_leider");
        }
        return DUNGEON.partyInRij(p, wie, dungeon);
    }

    /** The leader queues the party for the raid. Returns null when queued, else the reason. */
    @Nullable
    public static Component raid(ServerPlayer p) {
        List<ServerPlayer> wie = groep(p);
        if (wie.isEmpty()) {
            return Component.translatable("vadsrijk.kern.party.niet_leider");
        }
        String r = Z6Raid.partyInRij(wie);
        return r == null ? null : Component.translatable(r);
    }
}
