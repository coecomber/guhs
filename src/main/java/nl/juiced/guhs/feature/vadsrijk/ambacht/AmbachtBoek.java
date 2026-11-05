package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Categorie;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Ding;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Staat;

/**
 * The Ambachten in the Verkenningsboek (VOORSTEL §4: "per zone een Ambachtspagina ... telt niet mee in het zonepercentage, maar krijgt
 * een eigen Ambachtsboek %"): its own page {@code zone_ambachten} (sorted after the zones) with the spots found per zone, the fish,
 * the honey kinds and the recipes made; 100 % = the title "Ambachtskenner" + munten. Also the ambacht titles (A2: titles go home):
 * Leerling at lv 25, Gezel at lv 50 per ambacht (shown outside the dimension by the titles feature).
 */
public final class AmbachtBoek {
    public static final String T = "vadsrijk.ambacht.boek.";
    public static final Voltooiing.ZoneBoek BOEK = new Voltooiing.ZoneBoek("zone_ambachten", T + "titel", "vr_ambachtskenner", 20000, AmbachtBoek::bouw);
    public static final int LEERLING = 25, GEZEL = 50;

    private AmbachtBoek() {
    }

    static void registreer() {
        Voltooiing.registreer(BOEK);
        for (Ambacht a : Ambacht.values()) {
            titel(a, "leerling", LEERLING);
            titel(a, "gezel", GEZEL);
        }
    }

    public static String titelId(Ambacht a, String soort) {
        return "vr_amb_" + a.id + "_" + soort;
    }

    private static void titel(Ambacht a, String soort, int level) {
        String id = titelId(a, soort);
        if (Titels.van(id) == null) {
            Titels.registreer(new Titels.Titel(id, "gui.guhs.titels.naam." + id, level >= GEZEL ? ChatFormatting.GOLD : ChatFormatting.YELLOW, a.icoon,
                    p -> AmbachtData.level(p, a) >= level));
        }
    }

    static List<Categorie> bouw() {
        List<Categorie> l = new ArrayList<>();
        for (int zone = 1; zone <= 3; zone++) {
            List<Ding> d = new ArrayList<>();
            for (AmbachtPlekken.Groep g : AmbachtPlekken.GROEPEN) {
                if (g.zone() == zone) {
                    String id = g.id();
                    d.add(new Ding(id, "vadsrijk.ambacht.groep." + id, "", T + "hint.plek", g.ambacht().naamKey(), id.contains("druipmeer") || id.contains("ijsgrot"),
                            p -> AmbachtData.heeft(p, "plekken", id) ? Staat.KLAAR : Staat.NIETS, null));
                }
            }
            l.add(new Categorie("plekken_" + zone, T + "cat.plekken." + zone, zone == 1 ? "minecraft:iron_pickaxe" : zone == 2 ? "minecraft:amethyst_shard"
                    : "minecraft:honeycomb", d));
        }
        List<Ding> vissen = new ArrayList<>(), honing = new ArrayList<>(), recepten = new ArrayList<>();
        for (Materialen.Materiaal m : Materialen.ALLE.values()) {
            if (m.ambacht() == Ambacht.VISSEN && m.tier() > 0) {
                String id = m.id();
                vissen.add(new Ding(id, m.naamKey(), "", T + "hint.vis." + (m.wanneer().isEmpty() ? "altijd" : m.wanneer()), "vadsrijk.ambacht.tier." + m.tier(),
                        false, p -> AmbachtData.heeft(p, "vissen", id) ? Staat.KLAAR : Staat.NIETS, null));
            }
        }
        for (String h : Recepten.HONING) {
            honing.add(new Ding(h, "item.guhs.vr_amb_" + h, "", T + "hint.honing", "", false,
                    p -> AmbachtData.heeft(p, "honing", h) ? Staat.KLAAR : Staat.NIETS, null));
        }
        for (Recepten.Recept r : Recepten.ALLE.values()) {
            String id = r.id();
            recepten.add(new Ding(id, r.naamKey(), "", T + "hint.recept", r.ambacht().naamKey(), false,
                    p -> AmbachtData.heeft(p, "gemaakt", id) ? Staat.KLAAR : AmbachtData.level(p, r.ambacht()) >= r.level() ? Staat.BEZIG : Staat.NIETS, null));
        }
        l.add(new Categorie("vissen", T + "cat.vissen", "minecraft:cod", vissen));
        l.add(new Categorie("honing", T + "cat.honing", "minecraft:honey_bottle", honing));
        l.add(new Categorie("recepten", T + "cat.recepten", "minecraft:writable_book", recepten));
        return l;
    }

    @SuppressWarnings("unused")
    private static boolean binnen(ServerPlayer p) {
        return true;
    }
}
