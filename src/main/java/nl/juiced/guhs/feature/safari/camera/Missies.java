package nl.juiced.guhs.feature.safari.camera;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.safari.ParkData;
import nl.juiced.guhs.feature.safari.kern.SafariSpeler;

/**
 * The three photo missions of a real day (design 1.7, S1): the same three for everyone on that day, picked from a pool by
 * the day number ({@code Klok.dag()}, sent to the client as {@code DagNr} in the safari state), so server and client agree
 * without sending them. A mission is done once a day (daily counter {@code missie<i>}) and pays {@link #MUNTEN}.
 */
public final class Missies {
    public static final int AANTAL = 3, MUNTEN = 40;

    /** The kinds of mission; {@code soort}/{@code zone} are filled for the ones that name one. */
    public enum Soort {
        SLAPEND_3, BABY, TWEE_SOORTEN, FAMILIE, ETEND, WATER, VERSTOPPER, ZONE, DIER_2, DRIE_STER
    }

    public record Missie(Soort soort, String dier, String zone) {
        public boolean klopt(Foto f) {
            return switch (soort) {
                case SLAPEND_3 -> f.slaapt() && f.sterren() == 3;
                case BABY -> f.baby();
                case TWEE_SOORTEN -> f.soorten() >= 2;
                case FAMILIE -> f.zelfde() >= 3;
                case ETEND -> "eet".equals(f.actie());
                case WATER -> f.water();
                case VERSTOPPER -> {
                    ParkData.Soort s = ParkData.get().soort(f.soort());
                    yield s != null && s.heeft("verstopper");
                }
                case ZONE -> zone.equals(f.zone());
                case DIER_2 -> dier.equals(f.soort()) && f.sterren() >= 2;
                case DRIE_STER -> f.sterren() == 3;
            };
        }

        public Component tekst() {
            String key = "gui.guhs.safari.missie." + soort.name().toLowerCase(java.util.Locale.ROOT);
            return switch (soort) {
                case ZONE -> Component.translatable(key, Component.translatable("biome.guhs.safari_" + zone));
                case DIER_2 -> Component.translatable(key, Component.translatable("entity.guhs.safari." + dier));
                default -> Component.translatable(key);
            };
        }
    }

    /** The missions of this day (always three different kinds). */
    public static List<Missie> van(long dag) {
        Random r = new Random(dag * 7919L + 31L);
        List<Soort> pool = new ArrayList<>(List.of(Soort.values()));
        List<Missie> uit = new ArrayList<>();
        List<String> zones = new ArrayList<>(ParkData.get().zones);
        zones.remove("entree");
        List<String> dieren = ParkData.get().soorten.keySet().stream().filter(SafariSpeler::telt).sorted().toList();
        while (uit.size() < AANTAL && !pool.isEmpty()) {
            Soort s = pool.remove(r.nextInt(pool.size()));
            String zone = zones.isEmpty() ? "savanne" : zones.get(r.nextInt(zones.size()));
            String dier = dieren.isEmpty() ? "leeuw" : dieren.get(r.nextInt(dieren.size()));
            uit.add(new Missie(s, dier, zone));
        }
        return uit;
    }

    private Missies() {
    }
}
