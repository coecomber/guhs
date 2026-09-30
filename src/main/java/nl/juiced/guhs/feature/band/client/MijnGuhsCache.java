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

    public record Vriend(String naam, boolean bestie) {
    }

    public record Eerste(String id, long dag) {
    }

    public record Wist(Component tekst, long dag) {
    }

    /** One of your guhs as the tab shows it. */
    public record Guh(UUID id, String naam, CompoundTag looks, int hartjes, int niveau, int volgende, List<Fav> fav, List<Vriend> vrienden,
                      String huisje, List<Component> klussen, Component plek, Map<String, Long> stats, List<Eerste> eerste, List<Wist> wist,
                      long sinds, boolean dood) {
    }

    private static List<Guh> guhs = List.of();
    private static long dag;
    @Nullable
    private static UUID focus;
    private static int versie;

    private MijnGuhsCache() {
    }

    public static List<Guh> guhs() {
        return guhs;
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
        ListTag guhList = data.getList("Guhs", Tag.TAG_COMPOUND);
        for (int i = 0; i < guhList.size(); i++) {
            CompoundTag t = guhList.getCompound(i);
            try {
                list.add(lees(t, reg));
            } catch (RuntimeException e) {
                com.mojang.logging.LogUtils.getLogger().warn("Mijn guhs: skipped a guh", e);
            }
        }
        guhs = List.copyOf(list);
        dag = data.getLong("Dag");
        if (data.contains("Focus")) {
            try {
                focus = UUID.fromString(data.getString("Focus"));
            } catch (IllegalArgumentException ignored) {
                focus = null;
            }
        }
        versie++;
        if (Minecraft.getInstance().screen instanceof GuhDexScreen screen) {
            if (focus != null) {
                screen.naarDagboek();   // 2.10.1: the menu's "Dagboekje": straight to that guh's page, whatever tab was open
            } else if (GuhDexScreen.tab() == GuhDexScreen.Tab.MIJN_GUHS) {
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
        ListTag f = t.getList("Fav", Tag.TAG_COMPOUND);
        for (int i = 0; i < f.size(); i++) {
            String naam = f.getCompound(i).getString("Naam");
            fav.add(new Fav(f.getCompound(i).getString("Soort"), naam.isEmpty() ? null : tekst(naam, reg)));
        }
        List<Vriend> vrienden = new ArrayList<>();
        ListTag v = t.getList("Vrienden", Tag.TAG_COMPOUND);
        for (int i = 0; i < v.size(); i++) {
            vrienden.add(new Vriend(v.getCompound(i).getString("Naam"), v.getCompound(i).getBoolean("Bestie")));
        }
        List<Component> klussen = new ArrayList<>();
        ListTag k = t.getList("Klussen", Tag.TAG_STRING);
        for (int i = 0; i < k.size(); i++) {
            klussen.add(tekst(k.getString(i), reg));
        }
        Map<String, Long> stats = new LinkedHashMap<>();
        CompoundTag st = t.getCompound("Stats");
        for (String key : st.getAllKeys()) {
            stats.put(key, st.getLong(key));
        }
        List<Eerste> eerste = new ArrayList<>();
        ListTag e = t.getList("Eerste", Tag.TAG_COMPOUND);
        for (int i = 0; i < e.size(); i++) {
            eerste.add(new Eerste(e.getCompound(i).getString("Id"), e.getCompound(i).getLong("Dag")));
        }
        List<Wist> wist = new ArrayList<>();
        ListTag w = t.getList("Wist", Tag.TAG_COMPOUND);
        for (int i = 0; i < w.size(); i++) {
            wist.add(new Wist(tekst(w.getCompound(i).getString("Tekst"), reg), w.getCompound(i).getLong("Dag")));
        }
        return new Guh(UUID.fromString(t.getString("Id")), t.getString("Naam"), t.getCompound("Looks"), t.getInt("Hartjes"), t.getInt("Niveau"),
                t.getInt("Volgende"), fav, vrienden, t.getString("Huisje"), klussen, tekst(t.getString("Plek"), reg), stats, eerste, wist,
                t.getLong("Sinds"), t.getBoolean("Dood"));
    }

    private static Component tekst(String json, HolderLookup.Provider reg) {
        try {
            Component c = Component.Serializer.fromJson(json, reg);
            return c == null ? Component.empty() : c;
        } catch (RuntimeException e) {
            return Component.literal(json);
        }
    }
}
