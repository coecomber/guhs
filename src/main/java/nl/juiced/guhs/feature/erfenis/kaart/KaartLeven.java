package nl.juiced.guhs.feature.erfenis.kaart;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

/**
 * De Erfenis, wereldkaart: the life of the NPC villages. Pure logic on {@link KaartData} (no world, no server), so it can be
 * tested on its own.
 * <ul>
 *   <li>{@link #zaai}: the first villages, in clusters ("eilandjes") around the middle of the map, where the real players
 *   live (the players' cells come from the core's spiral around (0, 0); the inner {@value #KERN} cells each way are kept free
 *   for them).</li>
 *   <li>{@link #groei}: one growth tick: a few villages build, upgrade or level up, now and then a new village is founded,
 *   and the news feed says so.</li>
 * </ul>
 * All randomness comes from the data's seed (+ a counter), so the same world grows the same map.
 */
public final class KaartLeven {
    /** Cells with |cx| and |cz| at most this stay free for real players (the first 25 cells of the core's spiral). */
    public static final int KERN = 2;
    /** No more NPC villages than this (the map stays readable). */
    public static final int MAX_NPC = 160;
    /** How long a village counts as "nieuw!" on the map. */
    public static final long NIEUW_MS = 10 * 60_000L;
    static final int GEZELLIG = 10;

    private KaartLeven() {
    }

    /** The first NPC villages ({@code n}, in 3+ clusters), once per world. {@code bezet}: cells already taken (real players). */
    public static List<NpcDorp> zaai(KaartData data, int n, Set<Long> bezet, long nu) {
        List<NpcDorp> nieuw = new ArrayList<>();
        Random rng = new Random(data.seed);
        int eilanden = Math.max(3, n / 8);
        List<int[]> centra = new ArrayList<>();
        for (int i = 0, poging = 0; i < eilanden && poging < 500; poging++) {
            double hoek = rng.nextDouble() * Math.PI * 2, afstand = 5 + rng.nextDouble() * (3 + n / 5.0);
            int[] c = {(int) Math.round(Math.cos(hoek) * afstand), (int) Math.round(Math.sin(hoek) * afstand)};
            boolean ver = true;
            for (int[] o : centra) {
                if (Math.abs(o[0] - c[0]) + Math.abs(o[1] - c[1]) < 6) {
                    ver = false;
                    break;
                }
            }
            if (ver) {
                centra.add(c);
                i++;
            }
        }
        for (int i = 0; i < n; i++) {
            // every islet gets two, the rest goes anywhere (so some islets are big and busy, others small and quiet)
            int[] c = centra.get(i < 2 * centra.size() ? i % centra.size() : rng.nextInt(centra.size()));
            NpcDorp d = sticht(data, rng, c[0], c[1], 1.4, bezet, nu, true);
            if (d != null) {
                nieuw.add(d);
            }
        }
        data.gezaaid = true;
        data.setDirty();
        return nieuw;
    }

    /**
     * Founds one village near (ox, oz) (a Gaussian spread of {@code spreiding} cells) on a free cell. Its stadhuis level: at
     * the start a mix of 1..5, later always 1 (a new village starts small). Null: no free cell found.
     */
    @Nullable
    static NpcDorp sticht(KaartData data, Random rng, int ox, int oz, double spreiding, Set<Long> bezet, long nu, boolean start) {
        Set<Long> npc = data.npcCellen();
        for (int poging = 0; poging < 60; poging++) {
            double s = spreiding * (1 + poging / 20.0);
            int cx = ox + (int) Math.round(rng.nextGaussian() * s), cz = oz + (int) Math.round(rng.nextGaussian() * s);
            long k = KaartData.sleutel(cx, cz);
            if ((Math.abs(cx) <= KERN && Math.abs(cz) <= KERN) || npc.contains(k) || bezet.contains(k)) {
                continue;
            }
            UUID id = new UUID(rng.nextLong(), rng.nextLong());
            Random eigen = new Random(id.getMostSignificantBits() ^ data.seed);
            String eigenaar = NpcNamen.eigenaar(eigen);
            String naam = uniekeNaam(data, eigen, eigenaar);
            NpcDorp d = new NpcDorp(id, naam, eigenaar, NpcNamen.kleur(eigen), cx, cz);
            int level = start ? startLevel(eigen) : 1;
            d.plan.addAll(NpcBouwer.maak(level, eigen));
            d.gesticht = start ? 0 : nu;
            data.voegToe(d);
            return d;
        }
        return null;
    }

    private static String uniekeNaam(KaartData data, Random r, String eigenaar) {
        String naam = NpcNamen.dorp(r, eigenaar);
        for (int i = 0; i < 20 && bestaat(data, naam); i++) {
            naam = NpcNamen.dorp(r, eigenaar);
        }
        return bestaat(data, naam) ? naam + " " + (data.npcs.size() + 1) : naam;
    }

    private static boolean bestaat(KaartData data, String naam) {
        for (NpcDorp d : data.npcs.values()) {
            if (d.naam.equalsIgnoreCase(naam)) {
                return true;
            }
        }
        return false;
    }

    /** 1: 30%, 2: 30%, 3: 20%, 4: 13%, 5: 7%. */
    private static int startLevel(Random r) {
        int k = r.nextInt(100);
        return k < 30 ? 1 : k < 60 ? 2 : k < 80 ? 3 : k < 93 ? 4 : 5;
    }

    /** {@code n} extra villages (dev command): each next to a random existing one, or on a new islet now and then. */
    public static List<NpcDorp> nieuweDorpen(KaartData data, int n, Set<Long> bezet, long nu) {
        Random rng = new Random(data.seed * 31 + data.groeiTeller * 7919L + data.npcs.size());
        List<NpcDorp> l = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            NpcDorp d = stichtErgens(data, rng, bezet, nu);
            if (d != null) {
                l.add(d);
                meldGesticht(data, d, nu);
            }
        }
        return l;
    }

    @Nullable
    private static NpcDorp stichtErgens(KaartData data, Random rng, Set<Long> bezet, long nu) {
        List<NpcDorp> alle = new ArrayList<>(data.npcs.values());
        if (alle.isEmpty() || rng.nextInt(5) == 0) {   // a new islet, a bit further out
            double hoek = rng.nextDouble() * Math.PI * 2, afstand = 6 + rng.nextDouble() * (6 + Math.sqrt(alle.size()) * 2);
            return sticht(data, rng, (int) Math.round(Math.cos(hoek) * afstand), (int) Math.round(Math.sin(hoek) * afstand), 0.8, bezet, nu, false);
        }
        NpcDorp buur = alle.get(rng.nextInt(alle.size()));
        return sticht(data, rng, buur.cx, buur.cz, 1.3, bezet, nu, false);
    }

    /**
     * One growth tick: 1-3 villages grow a step, sometimes (1 in 8) a new village is founded, sometimes a cosy news line
     * without a change. Returns the villages that changed or are new (the core stamps those as real bases).
     */
    public static List<NpcDorp> groei(KaartData data, Set<Long> bezet, long nu) {
        Random rng = new Random(data.seed * 1_000_003L + data.groeiTeller * 0x9E3779B97F4A7C15L);
        data.groeiTeller++;
        data.setDirty();
        List<NpcDorp> veranderd = new ArrayList<>();
        List<NpcDorp> alle = new ArrayList<>(data.npcs.values());
        if (!alle.isEmpty()) {
            int n = 1 + rng.nextInt(3);
            for (int i = 0; i < n; i++) {
                NpcDorp d = alle.get(rng.nextInt(alle.size()));
                NpcBouwer.Groei g = NpcBouwer.groei(d.plan, rng);
                if (g.soort() == NpcBouwer.Groei.Soort.NIKS) {
                    continue;
                }
                d.gegroeid = nu;
                if (!veranderd.contains(d)) {
                    veranderd.add(d);
                }
                meldGroei(data, d, g, nu);
            }
            if (rng.nextInt(4) == 0) {
                NpcDorp d = alle.get(rng.nextInt(alle.size()));
                data.meld(new KaartNieuws("erfenis.kaart.nieuws.gezellig." + rng.nextInt(GEZELLIG), List.of(d.eigenaar, d.naam), nu));
            }
        }
        if (data.npcs.size() < MAX_NPC && (alle.isEmpty() || rng.nextInt(8) == 0)) {
            NpcDorp d = stichtErgens(data, rng, bezet, nu);
            if (d != null) {
                veranderd.add(d);
                meldGesticht(data, d, nu);
            }
        }
        return veranderd;
    }

    private static void meldGesticht(KaartData data, NpcDorp d, long nu) {
        data.meld(new KaartNieuws("erfenis.kaart.nieuws.gesticht", List.of(d.eigenaar, d.naam), nu));
    }

    private static void meldGroei(KaartData data, NpcDorp d, NpcBouwer.Groei g, long nu) {
        String lvl = Integer.toString(g.level());
        switch (g.soort()) {
            case STADHUIS -> data.meld(new KaartNieuws("erfenis.kaart.nieuws.stadhuis", List.of(d.naam, lvl), nu));
            case NIEUW -> data.meld(g.type() == KaartGebouwType.MUUR
                    ? new KaartNieuws("erfenis.kaart.nieuws.nieuw.muur", List.of(d.eigenaar, Integer.toString(g.aantal()), d.naam), nu)
                    : new KaartNieuws("erfenis.kaart.nieuws.nieuw." + g.type().id, List.of(d.eigenaar, d.naam), nu));
            case VERBETER -> data.meld(g.type() == KaartGebouwType.MUUR
                    ? new KaartNieuws("erfenis.kaart.nieuws.verbeter.muur", List.of(d.eigenaar, d.naam, lvl), nu)
                    : new KaartNieuws("erfenis.kaart.nieuws.verbeter", List.of(d.eigenaar, "#erfenis.kaart.gebouw." + g.type().id + ".de", d.naam, lvl), nu));
            default -> {
            }
        }
    }
}
