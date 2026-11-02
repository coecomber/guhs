package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.network.ModNetworking;

/**
 * De Erfenis, wereldkaart, the server side: sends the map, handles a visit, and runs the growth clock of the NPC villages
 * (every {@value #GROEI_MS} ms of real time, or every {@value #SNEL_MS} ms with "snel" on; missed time while the server was
 * off is not caught up: the map just goes on).
 */
public final class KaartServer {
    public static final long GROEI_MS = 4 * 60_000L, SNEL_MS = 15_000L;
    private static long volgende;

    private KaartServer() {
    }

    /** Seeds the first NPC villages once per world (30-60, from the world's map seed). */
    static KaartData zorg(MinecraftServer server) {
        KaartData data = KaartData.get(server);
        if (!data.gezaaid) {
            int n = 30 + new Random(data.seed).nextInt(31);
            for (NpcDorp d : KaartLeven.zaai(data, n, spelerCellen(server), System.currentTimeMillis())) {
                KaartFeature.BRON.npcVeranderd(server, d);
            }
        }
        return data;
    }

    static Set<Long> spelerCellen(MinecraftServer server) {
        Set<Long> s = new HashSet<>();
        for (KaartDorp d : KaartFeature.BRON.spelerDorpen(server, System.currentTimeMillis())) {
            s.add(KaartData.sleutel(d.cx(), d.cz()));
        }
        return s;
    }

    /** Every village on the map: the real players' bases and the NPC villages. */
    public static List<KaartDorp> alleDorpen(MinecraftServer server) {
        KaartData data = zorg(server);
        long nu = System.currentTimeMillis();
        List<KaartDorp> l = new ArrayList<>(KaartFeature.BRON.spelerDorpen(server, nu));
        for (NpcDorp d : data.npcs()) {
            boolean nieuw = Math.max(d.gesticht, d.gegroeid) > 0 && nu - Math.max(d.gesticht, d.gegroeid) < KaartLeven.NIEUW_MS;
            l.add(new KaartDorp(d.id, d.naam, d.eigenaar, true, d.cx, d.cz, d.stadhuisLevel(), d.punten(), d.kleur, nieuw));
        }
        return l;
    }

    public static void stuur(ServerPlayer p, boolean open) {
        MinecraftServer server = p.level().getServer();
        UUID eigen = KaartFeature.BRON.eigenDorp(p);
        KaartData data = zorg(server);
        ModNetworking.sendTo(p, new KaartPayloads.Kaart(data.seed, eigen, open, System.currentTimeMillis(), alleDorpen(server),
                new ArrayList<>(data.nieuws())));
    }

    static void actie(ServerPlayer p, int soort, UUID dorp) {
        MinecraftServer server = p.level().getServer();
        KaartData data = zorg(server);
        NpcDorp npc = data.npc(dorp);
        boolean bestaat = npc != null;
        if (!bestaat) {
            for (KaartDorp d : KaartFeature.BRON.spelerDorpen(server, System.currentTimeMillis())) {
                bestaat |= d.id().equals(dorp);
            }
        }
        if (!bestaat) {
            p.sendSystemMessage(Component.translatable("erfenis.kaart.weg"));
            return;
        }
        if (soort == KaartPayloads.Actie.BEZOEK || soort == KaartPayloads.Actie.BOVENAANZICHT) {
            KaartFeature.BRON.bezoek(p, dorp, npc, soort == KaartPayloads.Actie.BOVENAANZICHT);
        }
    }

    /** One growth tick now (also the dev command); returns the villages that changed. */
    public static List<NpcDorp> groeiNu(MinecraftServer server) {
        KaartData data = zorg(server);
        List<NpcDorp> veranderd = KaartLeven.groei(data, spelerCellen(server), System.currentTimeMillis());
        for (NpcDorp d : veranderd) {
            KaartFeature.BRON.npcVeranderd(server, d);
        }
        return veranderd;
    }

    public static List<NpcDorp> nieuw(MinecraftServer server, int n) {
        KaartData data = zorg(server);
        List<NpcDorp> l = KaartLeven.nieuweDorpen(data, n, spelerCellen(server), System.currentTimeMillis());
        for (NpcDorp d : l) {
            KaartFeature.BRON.npcVeranderd(server, d);
        }
        return l;
    }

    static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 11) {
            return;
        }
        KaartData data = KaartData.get(server);
        long nu = System.currentTimeMillis();
        long interval = data.snel ? SNEL_MS : GROEI_MS;
        if (volgende == 0 || volgende > nu + interval) {
            volgende = nu + interval;   // (first tick after a start, or "snel" was just switched on)
            return;
        }
        if (nu >= volgende) {
            volgende = nu + interval;
            groeiNu(server);
        }
    }

    static void herstart() {
        volgende = 0;
    }
}
