package nl.juiced.guhs.feature.vadsrijk.kern.party;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * Parties (ronde 11): up to {@link #MAX} players, invite + accept, shared xp nearby (see {@code Spelers#geefXpGedeeld}), no
 * friendly fire (no PvP at all in the dimension), a party HUD with everybody's Vads. Not saved (a restart breaks parties up).
 */
public final class Partijen {
    public static final int MAX = 5;

    /** A party: its leader and members (leader included), in join order. */
    public static final class Party {
        public UUID leider;
        public final Set<UUID> leden = new LinkedHashSet<>();

        Party(UUID leider) {
            this.leider = leider;
            leden.add(leider);
        }
    }

    private static final Map<UUID, Party> VAN = new HashMap<>();
    /** Open invitations: invitee -> inviter. */
    private static final Map<UUID, UUID> UITNODIGINGEN = new HashMap<>();

    private Partijen() {
    }

    @Nullable
    public static Party party(ServerPlayer p) {
        return VAN.get(p.getUUID());
    }

    public static boolean zelfdeParty(ServerPlayer a, ServerPlayer b) {
        Party pa = VAN.get(a.getUUID());
        return pa != null && pa == VAN.get(b.getUUID());
    }

    /** The online party members (incl. p) within this distance in the dimension; just p without a party. */
    public static List<ServerPlayer> ledenInBuurt(ServerPlayer p, double afstand) {
        List<ServerPlayer> uit = new ArrayList<>();
        Party party = VAN.get(p.getUUID());
        if (party == null) {
            uit.add(p);
            return uit;
        }
        for (UUID id : party.leden) {
            ServerPlayer l = p.level().getServer().getPlayerList().getPlayer(id);
            if (l != null && (l == p || (l.level() == p.level() && Vadsrijk.in(l) && l.distanceTo(p) <= afstand))) {
                uit.add(l);
            }
        }
        return uit;
    }

    /** All online members (for the HUD). */
    public static List<ServerPlayer> leden(ServerPlayer p) {
        Party party = VAN.get(p.getUUID());
        List<ServerPlayer> uit = new ArrayList<>();
        if (party == null) {
            return uit;
        }
        for (UUID id : party.leden) {
            ServerPlayer l = p.level().getServer().getPlayerList().getPlayer(id);
            if (l != null) {
                uit.add(l);
            }
        }
        return uit;
    }

    /** Returns a lang key with the result. */
    public static String nodigUit(ServerPlayer van, ServerPlayer naar) {
        if (van == naar) {
            return "vadsrijk.kern.party.zelf";
        }
        Party p = VAN.get(van.getUUID());
        if (p != null && p.leden.size() >= MAX) {
            return "vadsrijk.kern.party.vol";
        }
        if (VAN.containsKey(naar.getUUID())) {
            return "vadsrijk.kern.party.al_in_party";
        }
        UITNODIGINGEN.put(naar.getUUID(), van.getUUID());
        return "vadsrijk.kern.party.uitgenodigd";
    }

    public static String accepteer(ServerPlayer p) {
        UUID van = UITNODIGINGEN.remove(p.getUUID());
        if (van == null) {
            return "vadsrijk.kern.party.geen_uitnodiging";
        }
        if (VAN.containsKey(p.getUUID())) {
            return "vadsrijk.kern.party.al_in_party";
        }
        Party party = VAN.computeIfAbsent(van, Party::new);
        if (party.leden.size() >= MAX) {
            return "vadsrijk.kern.party.vol";
        }
        party.leden.add(p.getUUID());
        VAN.put(p.getUUID(), party);
        return "vadsrijk.kern.party.erbij";
    }

    public static void verlaat(UUID id) {
        Party party = VAN.remove(id);
        UITNODIGINGEN.remove(id);
        if (party == null) {
            return;
        }
        party.leden.remove(id);
        if (party.leden.size() <= 1) {
            for (UUID l : party.leden) {
                VAN.remove(l);
            }
            party.leden.clear();
        } else if (party.leider.equals(id)) {
            party.leider = party.leden.iterator().next();
        }
    }

    public static void wisAlles() {
        VAN.clear();
        UITNODIGINGEN.clear();
    }
}
