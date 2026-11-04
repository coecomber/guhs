package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * The "!" above a quest giver with something for you (ronde 10) and the "?" above whoever you should go back to. Per player
 * (every player sees their own), sent every second for the NPC guhs within 48 blocks when it changed.
 */
public final class NpcMarkers {
    public static final byte GEEN = 0, ZIJ = 1, HOOFD = 2, INLEVEREN = 3, PRATEN = 4;
    private static final Map<UUID, Integer> LAATST = new HashMap<>();
    private static final Set<UUID> VIES = new HashSet<>();

    private NpcMarkers() {
    }

    /** Something changed for this player: send again on the next round. */
    public static void vies(ServerPlayer p) {
        VIES.add(p.getUUID());
    }

    /** The marker this NPC shows for this player. */
    public static byte marker(ServerPlayer p, String npc) {
        byte m = GEEN;
        for (Quest q : Questlog.actieve(p)) {
            if (q.inleveren().equals(npc) && Questlog.allesKlaar(p, q)) {
                return INLEVEREN;
            }
            for (int i = 0; i < q.doelen().size(); i++) {
                Quest.Doel d = q.doelen().get(i);
                if ((d.type() == Quest.Doel.Type.PRAAT || d.type() == Quest.Doel.Type.ESCORTE) && d.doel().equals(npc) && !Questlog.doelKlaar(p, q, i)) {
                    m = PRATEN;
                }
            }
        }
        if (m != GEEN) {
            return m;
        }
        for (Quest q : Quests.alle().values()) {
            if (q.gever().equals(npc) && (!q.auto() || q.hoofd()) && Questlog.beschikbaar(p, q)) {
                return q.hoofd() ? HOOFD : ZIJ;
            }
        }
        return GEEN;
    }

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 13) {
            return;
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        for (ServerPlayer p : sl.players()) {
            List<Integer> ids = new ArrayList<>();
            List<Byte> soort = new ArrayList<>();
            for (RijkGuh g : sl.getEntitiesOfClass(RijkGuh.class, p.getBoundingBox().inflate(48))) {
                byte m = marker(p, g.npcId());
                if (m != GEEN) {
                    ids.add(g.getId());
                    soort.add(m);
                }
            }
            int[] a = ids.stream().mapToInt(Integer::intValue).toArray();
            byte[] b = new byte[soort.size()];
            for (int i = 0; i < b.length; i++) {
                b[i] = soort.get(i);
            }
            int hash = 31 * Arrays.hashCode(a) + Arrays.hashCode(b);
            Integer oud = LAATST.get(p.getUUID());
            if (oud == null || oud != hash || VIES.remove(p.getUUID())) {
                LAATST.put(p.getUUID(), hash);
                NpcPayloads.stuur(p, new NpcPayloads.Markers(a, b));
            }
        }
    }

    static void vergeet(UUID id) {
        LAATST.remove(id);
        VIES.remove(id);
    }
}
