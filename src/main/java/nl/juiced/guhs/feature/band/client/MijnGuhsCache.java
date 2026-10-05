package nl.juiced.guhs.feature.band.client;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.client.screen.GuhDexScreen;

/**
 * Client cache of the Guhdex tab "Mijn guhs" (from {@code guhs:band_mijn_guhs}): every band guh of the player with all
 * its dagboekje data, and which guh's page should open (the menu's "Dagboekje").
 */
public final class MijnGuhsCache {
    public record Fav(String soort, @Nullable Component naam) {
    }

    public record Vriend(Component naam, boolean bestie) {
    }

    public record Eerste(String id, long dag) {
    }

    public record Wist(Component tekst, long dag) {
    }

    /** One of your guhs as the tab shows it. */
    public record Guh(UUID id, Component naam, CompoundTag looks, int hartjes, int niveau, int volgende, List<Fav> fav, List<Vriend> vrienden,
                      Component huisje, List<Component> klussen, Component plek, Map<String, Long> stats, List<Eerste> eerste, List<Wist> wist,
                      long sinds, boolean dood, String plekSoort) {
    }

    /** 1.2.10: one of your other tamed critters (the tab "Mijn andere vadsjes"); naam is empty when it has no name of its own. */
    public record Vadsje(UUID id, Component naam, String soort, Component plek, String plekSoort) {
    }

    private static List<Guh> guhs = List.of();
    private static List<Vadsje> vadsjes = List.of();
    private static long dag;
    @Nullable
    private static UUID focus;
    private static int versie;

    private MijnGuhsCache() {
    }

    public static List<Guh> guhs() {
        return guhs;
    }

    /** Your other tamed critters, sorted by kind. */
    public static List<Vadsje> vadsjes() {
        return vadsjes;
    }

    /** (AutoCheck) fills the other critters without a server. */
    public static void zetVadsjes(List<Vadsje> voorbeeld) {
        vadsjes = List.copyOf(voorbeeld);
        versie++;
    }

    /** Today's Minecraft day (for "dag N" in the dagboekje). */
    public static long dag() {
        return dag;
    }

    /** Changes every time new data arrives (the tab rebuilds). */
    public static int versie() {
        return versie;
    }

    /** Is a guh's page waiting to be opened (the menu's "Dagboekje")? */
    public static boolean heeftFocus() {
        return focus != null;
    }

    /** The guh whose page should open (once), or null. */
    @Nullable
    public static UUID pakFocus() {
        UUID f = focus;
        focus = null;
        return f;
    }

    @Nullable
    public static Guh van(UUID id) {
        for (Guh g : guhs) {
            if (g.id().equals(id)) {
                return g;
            }
        }
        return null;
    }

    static void zet(CompoundTag data) {
        HolderLookup.Provider reg = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.registryAccess() : RegistryAccess.EMPTY;
        List<Guh> list = new ArrayList<>();
        ListTag guhList = data.getListOrEmpty("Guhs");
        for (int i = 0; i < guhList.size(); i++) {
            CompoundTag t = guhList.getCompoundOrEmpty(i);
            try {
                list.add(lees(t, reg));
            } catch (RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().warn("Mijn guhs: skipped a guh", e);
            }
        }
        guhs = List.copyOf(list);
        List<Vadsje> andere = new ArrayList<>();
        ListTag vadsjesList = data.getListOrEmpty("Vadsjes");
        for (int i = 0; i < vadsjesList.size(); i++) {
            CompoundTag t = vadsjesList.getCompoundOrEmpty(i);
            try {
                andere.add(new Vadsje(UUID.fromString(t.getStringOr("Id", "")), nl.juiced.guhs.taal.Tekst.get(t, "Naam"), t.getStringOr("Soort", ""),
                        tekst(t.getStringOr("Plek", ""), reg), t.getStringOr("PlekSoort", "")));
            } catch (RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().warn("Mijn andere vadsjes: skipped one", e);
            }
        }
        vadsjes = List.copyOf(andere);
        dag = data.getLongOr("Dag", 0L);
        if (data.contains("Focus")) {
            try {
                focus = UUID.fromString(data.getStringOr("Focus", ""));
            } catch (IllegalArgumentException ignored) {
                focus = null;
            }
        }
        versie++;
        if (Minecraft.getInstance().screen instanceof GuhDexScreen screen) {
            if (focus != null) {
                screen.naarDagboek();   // 2.10.1: the menu's "Dagboekje": straight to that guh's page, whatever tab was open
            } else if (GuhDexScreen.tab() == GuhDexScreen.Tab.MIJN_GUHS || GuhDexScreen.tab() == GuhDexScreen.Tab.ANDERE_VADSJES) {
                screen.mijnGuhsVernieuwd();
            }
        }
    }

    /** (AutoCheck) fills the cache without a server. */
    public static void zetVoorbeeld(List<Guh> voorbeeld, long vandaag, @Nullable UUID open) {
        guhs = List.copyOf(voorbeeld);
        dag = vandaag;
        focus = open;
        versie++;
    }

    private static Guh lees(CompoundTag t, HolderLookup.Provider reg) {
        List<Fav> fav = new ArrayList<>();
        ListTag f = t.getListOrEmpty("Fav");
        for (int i = 0; i < f.size(); i++) {
            String naam = f.getCompoundOrEmpty(i).getStringOr("Naam", "");
            fav.add(new Fav(f.getCompoundOrEmpty(i).getStringOr("Soort", ""), naam.isEmpty() ? null : tekst(naam, reg)));
        }
        List<Vriend> vrienden = new ArrayList<>();
        ListTag v = t.getListOrEmpty("Vrienden");
        for (int i = 0; i < v.size(); i++) {
            vrienden.add(new Vriend(nl.juiced.guhs.taal.Tekst.get(v.getCompoundOrEmpty(i), "Naam"), v.getCompoundOrEmpty(i).getBooleanOr("Bestie", false)));
        }
        List<Component> klussen = new ArrayList<>();
        ListTag k = t.getListOrEmpty("Klussen");
        for (int i = 0; i < k.size(); i++) {
            klussen.add(tekst(k.getStringOr(i, ""), reg));
        }
        Map<String, Long> stats = new LinkedHashMap<>();
        CompoundTag st = t.getCompoundOrEmpty("Stats");
        for (String key : st.keySet()) {
            stats.put(key, st.getLongOr(key, 0L));
        }
        List<Eerste> eerste = new ArrayList<>();
        ListTag e = t.getListOrEmpty("Eerste");
        for (int i = 0; i < e.size(); i++) {
            eerste.add(new Eerste(e.getCompoundOrEmpty(i).getStringOr("Id", ""), e.getCompoundOrEmpty(i).getLongOr("Dag", 0L)));
        }
        List<Wist> wist = new ArrayList<>();
        ListTag w = t.getListOrEmpty("Wist");
        for (int i = 0; i < w.size(); i++) {
            wist.add(new Wist(tekst(w.getCompoundOrEmpty(i).getStringOr("Tekst", ""), reg), w.getCompoundOrEmpty(i).getLongOr("Dag", 0L)));
        }
        return new Guh(UUID.fromString(t.getStringOr("Id", "")), nl.juiced.guhs.taal.Tekst.get(t, "Naam"), t.getCompoundOrEmpty("Looks"), t.getIntOr("Hartjes", 0), t.getIntOr("Niveau", 0),
                t.getIntOr("Volgende", 0), fav, vrienden, nl.juiced.guhs.taal.Tekst.get(t, "Huisje"), klussen, tekst(t.getStringOr("Plek", ""), reg), stats, eerste, wist,
                t.getLongOr("Sinds", 0L), t.getBooleanOr("Dood", false), t.getStringOr("PlekSoort", ""));
    }

    private static Component tekst(String json, HolderLookup.Provider reg) {
        try {
            if (json.isEmpty()) {
                return Component.empty();
            }
            // 1.1.0 (MC 26.1): Component.Serializer is gone; the component codec reads the same JSON
            com.google.gson.JsonElement el = com.google.gson.JsonParser.parseString(json);
            if (el.isJsonNull()) {
                return Component.empty();
            }
            return net.minecraft.network.chat.ComponentSerialization.CODEC
                    .parse(reg.createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE), el).getOrThrow();
        } catch (RuntimeException e) {
            return Component.literal(json);
        }
    }
}
