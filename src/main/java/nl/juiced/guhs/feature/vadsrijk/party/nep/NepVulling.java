package nl.juiced.guhs.feature.vadsrijk.party.nep;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;
import nl.juiced.guhs.feature.vadsrijk.party.DungeonWachtrij;
import nl.juiced.guhs.feature.vadsrijk.party.Wachtrijen;

/**
 * W-PARTY: nep-spelers fill empty spots in a DUNGEON queue (never the raid). When a queue has real players but empty spots and no
 * new real player joined for {@link #VUL_TICKS} (1 minute), made-up members fill it up ({@link DungeonWachtrij#voegNepToe}; roles that
 * are missing first: tank, heler, then schade). Never when the server switched them off or when ANY real player in that queue has
 * "Nep-spelers: uit". Recruited companions of the queued players count as taken spots (they walk in with their player).
 * <p>When the group goes in, the dungeon finder calls {@link #binnen}: the fillers appear as {@link NepSpeler} companions next to
 * the players (one player each, round robin; their hits count for that player, they never take loot). They leave ("gg") when their
 * player has been out of the dungeon for 10 s.
 */
public final class NepVulling {
    /** How long a queue waits for real players before nep-spelers fill it. */
    public static int VUL_TICKS = 20 * 60;

    /** Per dungeon: the real players in the queue (as last seen) and since when that set is unchanged. */
    private static final Map<String, Set<UUID>> ECHT = new HashMap<>();
    private static final Map<String, Long> SINDS = new HashMap<>();
    /** The made-up members handed to the queue (id -> who they are), until they go in or the queue drops them. */
    static final Map<UUID, DungeonWachtrij.NepLid> WACHTEND = new LinkedHashMap<>();

    private NepVulling() {
    }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 == 0) {
            tick(server, server.overworld().getGameTime(), false);
        }
    }

    /**
     * One check of every queue at time {@code nu}; {@code nu} is the overworld game time (gametests pass their own). {@code nu}
     * forced: fill now, no waiting (the dev command). Returns how many made-up members were added.
     */
    public static int tick(MinecraftServer server, long nu, boolean nuVullen) {
        DungeonWachtrij rij = Wachtrijen.DUNGEON;
        if (rij == null) {
            return 0;
        }
        int erbij = 0;
        Map<String, List<UUID>> rijen = rij.rijen();
        ECHT.keySet().retainAll(rijen.keySet());
        SINDS.keySet().retainAll(rijen.keySet());
        for (var e : rijen.entrySet()) {
            String d = e.getKey();
            Set<UUID> echt = new HashSet<>();
            for (UUID id : e.getValue()) {
                if (!rij.isNep(id)) {
                    echt.add(id);
                }
            }
            if (echt.isEmpty()) {
                ECHT.remove(d);
                SINDS.remove(d);
                continue;
            }
            if (!echt.equals(ECHT.get(d))) {
                ECHT.put(d, echt);
                SINDS.put(d, nu);
            }
            if (!nuVullen && nu - SINDS.getOrDefault(d, nu) < VUL_TICKS) {
                continue;
            }
            erbij += vul(server, rij, d, e.getValue(), echt);
        }
        WACHTEND.keySet().removeIf(id -> rijen.values().stream().noneMatch(l -> l.contains(id)));
        return erbij;
    }

    /** Fills one queue now when allowed. */
    static int vul(MinecraftServer server, DungeonWachtrij rij, String d, List<UUID> ids, Set<UUID> echt) {
        if (rij.bezet(d) || !mag(echt)) {
            return 0;
        }
        int npcs = 0;
        for (UUID id : echt) {
            Partijen.Party p = Partijen.partyVan(id);
            if (p != null) {
                npcs += p.npcs.size();
            }
        }
        int open = rij.groepMax() - ids.size() - npcs;
        if (open <= 0) {
            return 0;
        }
        // which roles are there already (the real players' classes + made-up members in the queue)?
        Set<NepRol> er = new HashSet<>();
        for (UUID id : ids) {
            DungeonWachtrij.NepLid l = WACHTEND.get(id);
            if (l != null) {
                er.add(l.rol());
            }
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                String k = Spelers.data(p).klasse;
                for (NepRol r : NepRol.values()) {
                    if (r.klasse.equals(k)) {
                        er.add(r);
                    }
                }
            }
        }
        RandomSource r = server.overworld().getRandom();
        Set<String> bezet = new HashSet<>();
        for (NepSpeler n : NepSpelers.alle()) {
            bezet.add(n.naam());
        }
        for (DungeonWachtrij.NepLid l : WACHTEND.values()) {
            bezet.add(l.naam());
        }
        List<DungeonWachtrij.NepLid> nieuw = new ArrayList<>();
        for (int i = 0; i < open; i++) {
            NepRol rol = !er.contains(NepRol.TANK) ? NepRol.TANK : !er.contains(NepRol.HELER) ? NepRol.HELER : NepRol.SCHADE;
            er.add(rol);
            String naam = NepNamen.naam(r, bezet);
            bezet.add(naam);
            nieuw.add(new DungeonWachtrij.NepLid(UUID.randomUUID(), naam, rol));
        }
        if (!rij.voegNepToe(d, nieuw)) {
            return 0;
        }
        for (DungeonWachtrij.NepLid l : nieuw) {
            WACHTEND.put(l.id(), l);
        }
        for (UUID id : echt) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                p.sendSystemMessage(Component.translatable("vadsrijk.party.nep.gevuld", nieuw.size(), rij.naam(d)).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        return nieuw.size();
    }

    /** Server on, and every real player in the queue has them on. */
    public static boolean mag(Set<UUID> echt) {
        if (!NepConfig.aan()) {
            return false;
        }
        for (UUID id : echt) {
            if (!NepSpelers.magZien(id)) {
                return false;
            }
        }
        return true;
    }

    /** Is this queue id one of our made-up members? (for the adapter's naming) */
    @Nullable
    public static DungeonWachtrij.NepLid lid(UUID id) {
        return WACHTEND.get(id);
    }

    /**
     * HOOK for the dungeon finder: the group went in. {@code nep} = the made-up queue members that came with it (ids from
     * {@link #lid}; others are ignored). Each becomes a companion next to one of the players. Returns the spawned nep-spelers.
     */
    public static List<NepSpeler> binnen(String dungeon, List<ServerPlayer> wie, List<UUID> nep) {
        List<NepSpeler> uit = new ArrayList<>();
        if (wie.isEmpty()) {
            return uit;
        }
        int i = 0;
        for (UUID id : nep) {
            DungeonWachtrij.NepLid l = WACHTEND.remove(id);
            if (l == null || !NepConfig.aan()) {
                continue;
            }
            ServerPlayer p = wie.get(i++ % wie.size());
            if (!(p.level() instanceof ServerLevel sl)) {
                continue;
            }
            Vec3 pos = p.position().add(sl.getRandom().nextDouble() * 2 - 1, 0, sl.getRandom().nextDouble() * 2 - 1);
            NepSpeler n = NepSpelers.spawn(sl, pos, l.rol(), Spelers.level(p), l.naam(), l.id());
            n.eigenaar = p.getUUID();
            n.dungeon = dungeon;
            for (ServerPlayer w : wie) {
                n.groep.add(w.getUUID());
            }
            NepSpelers.zeg(n, NepNamen.zeg("vullen", sl.getRandom()));
            uit.add(n);
        }
        return uit;
    }

    public static void wisAlles() {
        ECHT.clear();
        SINDS.clear();
        WACHTEND.clear();
    }
}
