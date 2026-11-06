package nl.juiced.guhs.feature.vadsrijk.toetsen.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.feature.vadsrijk.kern.client.KernClient;

/**
 * U-TOETSEN: which Vadsrijk keys exist, in which group, with which short description. The overview reads the REAL registered
 * {@link KeyMapping}s from {@code options.keyMappings} every time (so a rebind, incl. modifiers like Alt+1, shows at once): every
 * mapping in the Vadsrijk category ({@link KernClient#CATEGORIE}), every {@code key.guhs.vr_*} mapping, and every mapping
 * registered here. Unknown Vadsrijk mappings (added on another branch without a line here) still show, in "Overig", with
 * their own name; {@link #registreer} only gives them a nicer group/description.
 * <p>Other slices: {@code VadsrijkToetsen.registreer("key.guhs.vr_iets", "vadsrijk.toetsen.uitleg.iets", Groep.MENUS)}
 * (client side, any time before the pause menu opens). The description key is a lang key; null = the mapping's own name.
 */
public final class VadsrijkToetsen {
    public enum Groep {
        GEVECHT, MENUS, RIJDEN, SOCIAAL, OVERIG;

        public Component naam() {
            return Component.translatable("vadsrijk.toetsen.groep." + name().toLowerCase(java.util.Locale.ROOT));
        }
    }

    private record Info(String beschrijvingKey, Groep groep, int volgorde) {
    }

    /** One line of the overview. {@code conflictMet} = the other mapping on the same key (null = none). */
    public record Toets(KeyMapping mapping, Component beschrijving, Groep groep, Component conflictMet) {
        public boolean los() {
            return mapping.isUnbound();
        }

        public Component toets() {
            return mapping.getTranslatedKeyMessage();
        }
    }

    private static final Map<String, Info> INFO = new LinkedHashMap<>();
    private static final String U = "vadsrijk.toetsen.uitleg.";

    static {
        // Gevecht
        registreer("key.guhs.vr_r", U + "vr_r", Groep.GEVECHT);
        registreer("key.guhs.vr_v", U + "vr_v", Groep.GEVECHT);
        registreer("key.guhs.vr_g", U + "vr_g", Groep.GEVECHT);
        registreer("key.guhs.vr_z", U + "vr_z", Groep.GEVECHT);
        registreer("key.guhs.vr_sprong", U + "vr_sprong", Groep.GEVECHT);
        registreer("key.guhs.vr_vast", U + "vr_vast", Groep.GEVECHT);
        // Menu's
        registreer("key.guhs.vr_kl_boom", U + "vr_kl_boom", Groep.MENUS);
        registreer("key.guhs.vr_questlog", U + "vr_questlog", Groep.MENUS);
        registreer("key.guhs.vr_boek", U + "vr_boek", Groep.MENUS);
        registreer("key.guhs.vr_rijdieren", U + "vr_rijdieren", Groep.MENUS);
        registreer("key.guhs.vr_huisdieren", U + "vr_huisdieren", Groep.MENUS);
        registreer("key.guhs.vr_kledingkast", U + "vr_kledingkast", Groep.MENUS);
        registreer("key.guhs.vr_knapzak", U + "vr_knapzak", Groep.MENUS);
        registreer("key.guhs.vr_ambachten", U + "vr_ambachten", Groep.MENUS);
        // Rijden
        registreer("key.guhs.vr_rijdier", U + "vr_rijdier", Groep.RIJDEN);
        // Sociaal (u-hud's emote wheel; party keys of later branches land here when they register)
        registreer("key.guhs.vr_emotes", U + "vr_emotes", Groep.SOCIAAL);
        // Overig
        registreer("key.guhs.vr_t_drankje1", U + "vr_t_drankje1", Groep.OVERIG);
        registreer("key.guhs.vr_t_drankje2", U + "vr_t_drankje2", Groep.OVERIG);
        registreer("key.guhs.vr_t_drankje3", U + "vr_t_drankje3", Groep.OVERIG);
        registreer("key.guhs.vr_hudbewerker", U + "vr_hudbewerker", Groep.OVERIG);
        registreer("key.guhs.vr_volger", U + "vr_volger", Groep.OVERIG);
        registreer("key.guhs.vr_straal", U + "vr_straal", Groep.OVERIG);
    }

    private VadsrijkToetsen() {
    }

    /** Give a mapping (by its name, e.g. {@code key.guhs.vr_emotes}) a group and a short description (lang key, may be null). */
    public static synchronized void registreer(String mappingNaam, String beschrijvingKey, Groep groep) {
        Info oud = INFO.get(mappingNaam);
        INFO.put(mappingNaam, new Info(beschrijvingKey, groep, oud != null ? oud.volgorde() : INFO.size()));
    }

    public static void registreer(KeyMapping mapping, String beschrijvingKey, Groep groep) {
        registreer(mapping.getName(), beschrijvingKey, groep);
    }

    /** Does this mapping belong in the overview? */
    public static boolean hoortErbij(KeyMapping k) {
        return k.getCategory() == KernClient.CATEGORIE || k.getName().startsWith("key.guhs.vr_") || INFO.containsKey(k.getName());
    }

    /** Every Vadsrijk key, grouped (group order, then registration order, unknown ones by name), with conflicts filled in. */
    public static synchronized List<Toets> alle() {
        Minecraft mc = Minecraft.getInstance();
        KeyMapping[] alles = mc.options.keyMappings;
        List<KeyMapping> eigen = new ArrayList<>();
        for (KeyMapping k : alles) {
            if (hoortErbij(k)) {
                eigen.add(k);
            }
        }
        eigen.sort(Comparator.<KeyMapping>comparingInt(k -> groep(k).ordinal())
                .thenComparingInt(k -> INFO.containsKey(k.getName()) ? INFO.get(k.getName()).volgorde() : Integer.MAX_VALUE)
                .thenComparing(KeyMapping::getName));
        List<Toets> uit = new ArrayList<>();
        for (KeyMapping k : eigen) {
            uit.add(new Toets(k, beschrijving(k), groep(k), conflict(k, alles)));
        }
        return uit;
    }

    private static Groep groep(KeyMapping k) {
        Info i = INFO.get(k.getName());
        return i != null ? i.groep() : Groep.OVERIG;
    }

    private static Component beschrijving(KeyMapping k) {
        Info i = INFO.get(k.getName());
        if (i != null && i.beschrijvingKey() != null && I18n.exists(i.beschrijvingKey())) {
            return Component.translatable(i.beschrijvingKey());
        }
        return Component.translatable(k.getName());
    }

    /**
     * The first other mapping that really fires on the same input: same key AND same modifier, in overlapping contexts. Not counted:
     * Alt+1 vs the plain hotbar 1 (NeoForge's lookup fires only the Alt binding while Alt is held), and the older Guhs keys of the
     * "Guhs" category (maag G, snuffel R/V/Z/B/N...): those belong to other worlds and kern swallows them in the Vadsrijk.
     */
    static Component conflict(KeyMapping k, KeyMapping[] alles) {
        if (k.isUnbound()) {
            return null;
        }
        for (KeyMapping o : alles) {
            if (o == k || o.isUnbound() || o.getCategory() == nl.juiced.guhs.client.GuhKeys.CATEGORY) {
                continue;
            }
            if (!o.getKey().equals(k.getKey()) || o.getKeyModifier() != k.getKeyModifier()) {
                continue;
            }
            if (k.getKeyConflictContext().conflicts(o.getKeyConflictContext()) || o.getKeyConflictContext().conflicts(k.getKeyConflictContext())) {
                return Component.translatable(o.getName());
            }
        }
        return null;
    }
}
