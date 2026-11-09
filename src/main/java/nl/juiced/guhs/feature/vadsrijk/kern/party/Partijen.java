package nl.juiced.guhs.feature.vadsrijk.kern.party;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * Parties (ronde 11, W-PARTY): up to {@link #MAX} members (players + nep-speler companions together), invite + accept/decline,
 * shared xp nearby (see {@code Spelers#geefXpGedeeld}), no friendly fire (no PvP at all in the dimension), party frames.
 * <ul>
 *   <li><b>Saved</b> (W-PARTY): {@link PartijenData} keeps leader, members and their names across restarts. Logging out does NOT
 *       leave the party any more (the frames show you grey); when the leader logs out, the first online member becomes leader.</li>
 *   <li><b>Leader</b>: may remove a member ({@link #verwijder}) and hand over the lead ({@link #maakLeider}). Everyone may leave.</li>
 *   <li><b>Nep-spelers</b> ({@code feature.vadsrijk.party}): {@link Party#npcs} holds recruited fake players (entity UUIDs). They are
 *       never in {@link Party#leden}, so xp, quest sharing, the raid and every {@code leden(..)} caller only ever see real players.
 *       They count for {@link #MAX}. They are not saved (they leave at a restart).</li>
 * </ul>
 */
public final class Partijen {
    public static final int MAX = 5;

    /** A party: its leader and members (leader included), in join order. */
    public static final class Party {
        public UUID leider;
        public final Set<UUID> leden = new LinkedHashSet<>();
        /** W-PARTY: recruited nep-spelers (entity UUIDs), not saved. */
        public final Set<UUID> npcs = new LinkedHashSet<>();
        /** W-PARTY: the last known name per member (for offline members after a restart). */
        public final Map<UUID, String> namen = new HashMap<>();

        Party(UUID leider) {
            this.leider = leider;
            leden.add(leider);
        }

        /** Players + nep-spelers. */
        public int grootte() {
            return leden.size() + npcs.size();
        }

        public boolean vol() {
            return grootte() >= MAX;
        }
    }

    private static final Map<UUID, Party> VAN = new HashMap<>();
    /** Open invitations: invitee -> inviter. */
    private static final Map<UUID, UUID> UITNODIGINGEN = new HashMap<>();
    /** W-PARTY: called after any change (the party, or null when it ended) - the party tab pushes fresh state. */
    public static final List<Consumer<Party>> VERANDERD = new CopyOnWriteArrayList<>();
    /** W-PARTY: a nep-speler left a party (removed, kicked or the party ended): the companion code sends it away. */
    public static final List<Consumer<UUID>> NPC_WEG = new CopyOnWriteArrayList<>();

    private Partijen() {
    }

    @Nullable
    public static Party party(ServerPlayer p) {
        return VAN.get(p.getUUID());
    }

    @Nullable
    public static Party partyVan(UUID id) {
        return VAN.get(id);
    }

    public static boolean zelfdeParty(ServerPlayer a, ServerPlayer b) {
        Party pa = VAN.get(a.getUUID());
        return pa != null && pa == VAN.get(b.getUUID());
    }

    /** Every party (once each). */
    public static List<Party> alle() {
        return new ArrayList<>(new LinkedHashSet<>(VAN.values()));
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

    /** Who invited this player (null: nobody). */
    @Nullable
    public static UUID uitnodigingVan(UUID id) {
        return UITNODIGINGEN.get(id);
    }

    /** Returns a lang key with the result. */
    public static String nodigUit(ServerPlayer van, ServerPlayer naar) {
        if (van == naar) {
            return "vadsrijk.kern.party.zelf";
        }
        Party p = VAN.get(van.getUUID());
        if (p != null && p.vol()) {
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
        Party party = VAN.get(van);
        if (party != null && party.vol()) {
            return "vadsrijk.kern.party.vol";
        }
        if (party == null) {
            party = new Party(van);
            VAN.put(van, party);
            ServerPlayer v = p.level().getServer().getPlayerList().getPlayer(van);
            if (v != null) {
                party.namen.put(van, v.getGameProfile().name());
            }
        }
        party.leden.add(p.getUUID());
        party.namen.put(p.getUUID(), p.getGameProfile().name());
        VAN.put(p.getUUID(), party);
        veranderd(party);
        return "vadsrijk.kern.party.erbij";
    }

    /** W-PARTY: declines the open invitation. Returns a lang key; {@code van} (out) is not needed: see {@link #uitnodigingVan} first. */
    public static String weiger(ServerPlayer p) {
        UUID van = UITNODIGINGEN.remove(p.getUUID());
        return van == null ? "vadsrijk.kern.party.geen_uitnodiging" : "vadsrijk.kern.party.geweigerd";
    }

    public static void verlaat(UUID id) {
        Party party = VAN.remove(id);
        UITNODIGINGEN.remove(id);
        if (party == null) {
            return;
        }
        party.leden.remove(id);
        if (party.leider.equals(id) && !party.leden.isEmpty()) {
            party.leider = party.leden.iterator().next();
        }
        ruim(party);
    }

    /** W-PARTY: the leader removes a member (player or nep-speler). Returns a lang key. */
    public static String verwijder(ServerPlayer leider, UUID wie) {
        Party party = VAN.get(leider.getUUID());
        if (party == null) {
            return "vadsrijk.kern.party.geen_party";
        }
        if (!party.leider.equals(leider.getUUID())) {
            return "vadsrijk.kern.party.niet_leider";
        }
        if (wie.equals(leider.getUUID())) {
            return "vadsrijk.kern.party.zelf_weg";
        }
        if (party.npcs.contains(wie)) {
            verwijderNpc(wie);
            return "vadsrijk.kern.party.verwijderd";
        }
        if (!party.leden.contains(wie)) {
            return "vadsrijk.kern.party.geen_lid";
        }
        verlaat(wie);
        return "vadsrijk.kern.party.verwijderd";
    }

    /** W-PARTY: the leader hands the lead to another (player) member. Returns a lang key. */
    public static String maakLeider(ServerPlayer leider, UUID wie) {
        Party party = VAN.get(leider.getUUID());
        if (party == null) {
            return "vadsrijk.kern.party.geen_party";
        }
        if (!party.leider.equals(leider.getUUID())) {
            return "vadsrijk.kern.party.niet_leider";
        }
        if (!party.leden.contains(wie) || wie.equals(leider.getUUID())) {
            return "vadsrijk.kern.party.geen_lid";
        }
        party.leider = wie;
        veranderd(party);
        return "vadsrijk.kern.party.nieuwe_leider";
    }

    /**
     * W-PARTY: a player logged out. They stay in the party (saved); an open invitation to them is dropped; a leader hands the lead to
     * the first member that is still online (nobody online: they stay leader).
     */
    public static void uitgelogd(MinecraftServer server, UUID id) {
        UITNODIGINGEN.remove(id);
        Party party = VAN.get(id);
        if (party == null || !party.leider.equals(id)) {
            return;
        }
        for (UUID l : party.leden) {
            if (!l.equals(id) && server.getPlayerList().getPlayer(l) != null) {
                party.leider = l;
                veranderd(party);
                return;
            }
        }
    }

    // ---- nep-spelers (W-PARTY) ------------------------------------------------------------------------------------------------

    /** The player's party, made now (just them, leader) when they have none. */
    public static Party maakParty(ServerPlayer p) {
        Party party = VAN.get(p.getUUID());
        if (party == null) {
            party = new Party(p.getUUID());
            party.namen.put(p.getUUID(), p.getGameProfile().name());
            VAN.put(p.getUUID(), party);
        }
        return party;
    }

    /** Adds a nep-speler to p's party (made when needed). Returns a lang key ("erbij" on success). */
    public static String voegNpcToe(ServerPlayer p, UUID npc) {
        Party party = VAN.get(p.getUUID());
        if (party != null && party.vol()) {
            return "vadsrijk.kern.party.vol";
        }
        party = maakParty(p);
        party.npcs.add(npc);
        veranderd(party);
        return "vadsrijk.kern.party.erbij";
    }

    /** The party this nep-speler is in, or null. */
    @Nullable
    public static Party partyVanNpc(UUID npc) {
        for (Party p : VAN.values()) {
            if (p.npcs.contains(npc)) {
                return p;
            }
        }
        return null;
    }

    /** A nep-speler leaves its party (the NPC_WEG hooks run). */
    public static void verwijderNpc(UUID npc) {
        Party party = partyVanNpc(npc);
        if (party == null) {
            return;
        }
        party.npcs.remove(npc);
        NPC_WEG.forEach(c -> c.accept(npc));
        ruim(party);
    }

    // ---- housekeeping ---------------------------------------------------------------------------------------------------------

    /** A party of one player without nep-spelers (or of nobody) ends; otherwise just "changed". */
    private static void ruim(Party party) {
        if (party.leden.isEmpty() || (party.leden.size() <= 1 && party.npcs.isEmpty())) {
            for (UUID l : party.leden) {
                VAN.remove(l);
            }
            party.leden.clear();
            List<UUID> npcs = new ArrayList<>(party.npcs);
            party.npcs.clear();
            for (UUID n : npcs) {
                NPC_WEG.forEach(c -> c.accept(n));
            }
            veranderd(null);
            return;
        }
        veranderd(party);
    }

    private static void veranderd(@Nullable Party party) {
        PartijenData.bewaar();
        VERANDERD.forEach(c -> c.accept(party));
    }

    public static void wisAlles() {
        VAN.clear();
        UITNODIGINGEN.clear();
    }

    // ---- saving (W-PARTY) -----------------------------------------------------------------------------------------------------

    /** Every party as a tag: {@code partijen: [{leider, leden: [uuid..], namen: {uuid: naam}}]} (nep-spelers are not saved). */
    public static CompoundTag naarTag() {
        CompoundTag t = new CompoundTag();
        ListTag lijst = new ListTag();
        for (Party p : alle()) {
            if (p.leden.size() < 2) {
                continue;   // (one player + nep-spelers: the nep-spelers do not survive a restart, so neither does this party)
            }
            CompoundTag pt = new CompoundTag();
            pt.store("leider", UUIDUtil.CODEC, p.leider);
            ListTag leden = new ListTag();
            CompoundTag namen = new CompoundTag();
            for (UUID id : p.leden) {
                leden.add(UUIDUtil.CODEC.encodeStart(NbtOps.INSTANCE, id).getOrThrow());
                String n = p.namen.get(id);
                if (n != null) {
                    namen.putString(id.toString(), n);
                }
            }
            pt.put("leden", leden);
            pt.put("namen", namen);
            lijst.add(pt);
        }
        t.put("partijen", lijst);
        return t;
    }

    /** Replaces every party with the ones in this tag (invitations are dropped). */
    public static void uitTag(CompoundTag t) {
        wisAlles();
        for (Tag x : t.getListOrEmpty("partijen")) {
            if (!(x instanceof CompoundTag pt)) {
                continue;
            }
            UUID leider = pt.read("leider", UUIDUtil.CODEC).orElse(null);
            List<UUID> leden = new ArrayList<>();
            for (Tag l : pt.getListOrEmpty("leden")) {
                UUIDUtil.CODEC.parse(NbtOps.INSTANCE, l).result().ifPresent(id -> {
                    if (!VAN.containsKey(id) && !leden.contains(id)) {
                        leden.add(id);
                    }
                });
            }
            if (leden.size() < 2) {
                continue;
            }
            if (leider == null || !leden.contains(leider)) {
                leider = leden.get(0);
            }
            Party p = new Party(leider);
            p.leden.clear();   // (keep the saved join order, not leader first)
            for (UUID id : leden) {
                p.leden.add(id);
                VAN.put(id, p);
            }
            CompoundTag namen = pt.getCompoundOrEmpty("namen");
            for (String k : namen.keySet()) {
                try {
                    p.namen.put(UUID.fromString(k), namen.getStringOr(k, "?"));
                } catch (IllegalArgumentException ignored) {
                    // (a broken key: skip)
                }
            }
        }
    }

    /** For the party tab: the members in join order with their last known names. */
    public static Map<UUID, String> namen(Party p) {
        Map<UUID, String> m = new LinkedHashMap<>();
        for (UUID id : p.leden) {
            m.put(id, p.namen.getOrDefault(id, "?"));
        }
        return m;
    }
}
