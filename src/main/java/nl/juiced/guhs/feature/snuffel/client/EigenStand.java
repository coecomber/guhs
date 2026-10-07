package nl.juiced.guhs.feature.snuffel.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.snuffel.GeurSoort;
import nl.juiced.guhs.feature.snuffel.Rang;
import nl.juiced.guhs.feature.snuffel.SnuffelPayloads;

/**
 * Client: the local player's own island state, as the server last sent it ({@code guhs:snuffel_stand}, see
 * {@link nl.juiced.guhs.feature.snuffel.Stand}): the screens, the HUD and the tree read it. The tree's stage is shown with
 * a delay when the server says so (the growth scene pops it halfway).
 */
public final class EigenStand {
    /** A learned scent as the snuffelboekje shows it. */
    public record Geur(String id, GeurSoort soort, String icoon) {
        public Component naam() {
            return Component.translatable("gui.guhs.snuffel.geur." + id);
        }
    }

    private static CompoundTag data = new CompoundTag();
    private static int boomGetoond, boomDoel;
    private static long boomWissel;

    private EigenStand() {
    }

    static void ontvang(SnuffelPayloads.StandBericht p) {
        zet(p.data());
    }

    /** Takes over a state (also the copy that comes with a screen). */
    static void zet(CompoundTag nieuw) {
        data = nieuw;
        int boom = nieuw.getIntOr("Boom", 0);
        int over = nieuw.getIntOr("BoomOver", 0);
        if (boom != boomDoel) {
            // (only a CHANGE sets the moment: a later message with the same stage does not hurry the pop)
            boomDoel = boom;
            boomWissel = SnuffelClient.ticks() + Math.max(0, over);
        }
    }

    static void wis() {
        data = new CompoundTag();
        boomGetoond = boomDoel = 0;
        boomWissel = 0;
    }

    public static CompoundTag data() {
        return data;
    }

    public static boolean hond() {
        return data.getBooleanOr("Hond", false);
    }

    public static String ras() {
        return data.getStringOr("Ras", "shiba");
    }

    public static String kleur() {
        return data.getStringOr("Kleur", "rood");
    }

    public static String naam() {
        return data.getStringOr("Naam", "");
    }

    public static String maatje() {
        return data.getStringOr("Maatje", "b");
    }

    public static Rang rang() {
        return Rang.metNummer(data.getIntOr("Rang", 1));
    }

    public static int aantal() {
        return data.getIntOr("Aantal", 0);
    }

    public static boolean diploma() {
        return data.getBooleanOr("Diploma", false);
    }

    public static boolean heeftMaatje() {
        return data.getBooleanOr("HeeftMaatje", false);
    }

    public static List<Geur> geuren() {
        return geuren(data);
    }

    static List<Geur> geuren(CompoundTag van) {
        List<Geur> uit = new ArrayList<>();
        ListTag l = van.getListOrEmpty("Geuren");
        for (int i = 0; i < l.size(); i++) {
            CompoundTag x = l.getCompoundOrEmpty(i);
            GeurSoort soort = GeurSoort.vanNummer(x.getIntOr("Soort", 0));
            uit.add(new Geur(x.getStringOr("Id", ""), soort == null ? GeurSoort.VREEMD : soort, x.getStringOr("Icoon", "minecraft:bone")));
        }
        return uit;
    }

    public static List<String> daden() {
        return daden(data);
    }

    static List<String> daden(CompoundTag van) {
        List<String> uit = new ArrayList<>();
        ListTag l = van.getListOrEmpty("Daden");
        for (int i = 0; i < l.size(); i++) {
            uit.add(l.getStringOr(i, ""));
        }
        return uit;
    }

    /** The exam that runs: {found, total}, or null. */
    public static int[] examen() {
        CompoundTag e = data.getCompound("Examen").orElse(null);
        return e == null ? null : new int[] {e.getIntOr("Gevonden", 0), e.getIntOr("Totaal", 0)};
    }

    public static String examenId() {
        return data.getCompoundOrEmpty("Examen").getStringOr("Id", "");
    }

    /** The stage of the tree as this client shows it now (0..4). Returns true in {@code gegroeid[0]} when it just went up. */
    static int boom(boolean[] gegroeid) {
        if (boomGetoond != boomDoel && SnuffelClient.ticks() >= boomWissel) {
            gegroeid[0] = boomDoel > boomGetoond;
            boomGetoond = boomDoel;
        }
        return boomGetoond;
    }

    public static int boom() {
        return boomGetoond;
    }
}
