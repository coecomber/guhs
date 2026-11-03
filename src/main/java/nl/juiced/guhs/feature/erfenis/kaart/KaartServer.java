package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.network.ModNetworking;

/**
 * De Erfenis, wereldkaart, the server side: sends the map, handles a visit / orders / a helping hand, keeps every player's
 * NPC villages by the 1.9 rule ({@link KaartLeven#zorgBuren}, checked every few seconds, so a new player gets their village
 * at once), lets the villages follow their player and grow (every {@value #GROEI_MS} ms of real time, or every
 * {@value #SNEL_MS} ms with "snel" on), and moves the order carts every second.
 */
public final class KaartServer {
    public static final long GROEI_MS = 4 * 60_000L, SNEL_MS = 15_000L;
    private static long volgende;
    private static int spelersGezien = -1;

    private KaartServer() {
    }

    /** The real players in arrival order, as the village rule sees them. */
    static List<KaartLeven.Speler> spelers(MinecraftServer server) {
        List<KaartLeven.Speler> l = new ArrayList<>();
        for (KaartDorp d : KaartFeature.BRON.spelerDorpen(server, System.currentTimeMillis())) {
            l.add(new KaartLeven.Speler(d.id(), d.naam(), d.cx(), d.cz(), d.hoeve()));
        }
        return l;
    }

    /** Makes sure the map follows the rule (every player has their villages; a POC map is migrated once). */
    static KaartData zorg(MinecraftServer server) {
        KaartData data = KaartData.get(server);
        List<KaartLeven.Speler> spelers = spelers(server);
        boolean nodig = data.versie < KaartData.VERSIE || spelers.size() != spelersGezien;
        if (nodig) {
            KaartLeven.Uitkomst u = KaartLeven.zorgBuren(data, spelers, spelerCellen(server), System.currentTimeMillis());
            for (NpcDorp d : u.weg()) {
                KaartFeature.BRON.npcWeg(server, d);
            }
            for (NpcDorp d : u.veranderd()) {
                KaartFeature.BRON.npcVeranderd(server, d);
            }
            spelersGezien = spelers.size();
        }
        return data;
    }

    /** The cells taken by real bases (players, and every NPC base of the core). */
    static Set<Long> spelerCellen(MinecraftServer server) {
        return new HashSet<>(KaartFeature.BRON.bezetteCellen(server));
    }

    /** Every village on the map: the real players' bases and the NPC villages. */
    public static List<KaartDorp> alleDorpen(MinecraftServer server) {
        KaartData data = zorg(server);
        long nu = System.currentTimeMillis();
        List<KaartDorp> l = new ArrayList<>(KaartFeature.BRON.spelerDorpen(server, nu));
        for (NpcDorp d : data.npcs()) {
            boolean nieuw = Math.max(d.gesticht, d.gegroeid) > 0 && nu - Math.max(d.gesticht, d.gegroeid) < KaartLeven.NIEUW_MS;
            l.add(new KaartDorp(d.id, d.naam, d.eigenaar, true, d.cx, d.cz, d.hoeveLevel(), d.punten(), d.kleur, nieuw));
        }
        return l;
    }

    public static void stuur(ServerPlayer p, boolean open) {
        MinecraftServer server = p.level().getServer();
        // 1.9 afwerking: the world map only after buying the makelaar's letter (creative always), like the overview
        if (!nl.juiced.guhs.feature.erfenis.Erfenis.magErfenis(p)) {
            if (open) {
                p.sendSystemMessage(Component.translatable("erfenis.cmd.nog_geen_erfenis"), true);
            }
            return;
        }
        UUID eigen = KaartFeature.BRON.eigenDorp(p);
        List<KaartDorp> dorpen = alleDorpen(server);
        KaartData data = KaartData.get(server);
        ModNetworking.sendTo(p, new KaartPayloads.Kaart(data.seed, eigen, open, System.currentTimeMillis(), dorpen,
                new ArrayList<>(data.nieuws()), Bestellingen.kaartExtra(server, p.getUUID())));
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
        switch (soort) {
            case KaartPayloads.Actie.BEZOEK, KaartPayloads.Actie.BOVENAANZICHT ->
                    KaartFeature.BRON.bezoek(p, dorp, npc, soort == KaartPayloads.Actie.BOVENAANZICHT);
            case KaartPayloads.Actie.BESTELLINGEN -> Bestellingen.open(p);
            case KaartPayloads.Actie.HULP_BOUW -> {
                Handje.help(p, dorp, Handje.BOUW);
                stuur(p, false);
            }
            case KaartPayloads.Actie.HULP_BESTELLING -> {
                Handje.help(p, dorp, Handje.BESTELLING);
                stuur(p, false);
            }
            default -> {
            }
        }
    }

    /** One growth tick now (also the dev command): villages follow their player, then a bit of cosy growth. */
    public static List<NpcDorp> groeiNu(MinecraftServer server) {
        KaartData data = zorg(server);
        long nu = System.currentTimeMillis();
        List<NpcDorp> veranderd = new ArrayList<>(KaartLeven.volg(data, spelers(server), nu));
        for (NpcDorp d : KaartLeven.groei(data, nu)) {
            if (!veranderd.contains(d)) {
                veranderd.add(d);
            }
        }
        for (NpcDorp d : veranderd) {
            KaartFeature.BRON.npcVeranderd(server, d);
        }
        return veranderd;
    }

    static void tick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int t = server.getTickCount();
        if (t % 20 != 11) {
            return;
        }
        Bestellingen.tick(server);
        if (t % 100 == 11) {
            zorg(server);   // (a new player gets their village within a few seconds)
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
        spelersGezien = -1;
    }
}
