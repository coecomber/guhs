package nl.juiced.guhs.feature.bouwhulp.keet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * The tabs of the Bouwkeet menu (design 2.2: Vormen | Daken | Blauwdrukken | Bouwguhs | Materiaal | Niveau), so every
 * slice adds its own without touching the menu. The server half of a tab is a {@link Tab}: it writes what its page shows
 * into a tag and handles its buttons; the client half registers under the same id with
 * {@code client.BouwkeetScreen.tab(id, ...)}. Registering an id again replaces the tab (F8 registers "binnenkort"
 * placeholders for vormen, daken and blauwdrukken; F9 replaces them).
 */
public final class BouwkeetTabs {
    /** The ids of the six tabs of the design (others may be added). */
    public static final String VORMEN = "vormen", DAKEN = "daken", BLAUWDRUKKEN = "blauwdrukken", BOUWGUHS = "bouwguhs", MATERIAAL = "materiaal",
            NIVEAU = "niveau";

    /** The server half of a tab. */
    public interface Tab {
        /** The id: the text key is gui.guhs.bouwkeet.tab.&lt;id&gt; and the client half registers under it. */
        String id();

        /** Where it stands (low first: vormen 10, daken 20, blauwdrukken 30, bouwguhs 40, materiaal 50, niveau 60). */
        int volgorde();

        /** From which level it can be used (below that the tab shows what it will do, greyed). */
        default BouwNiveau vanaf() {
            return BouwNiveau.HANDWERK;
        }

        /** What the page shows (only called while this tab is open). */
        void schrijf(ServerPlayer p, ServerLevel level, BouwkeetBlockEntity keet, CompoundTag uit);

        /**
         * A button of this page (only for who may change the keet, {@link BouwkeetBlock#magBewerken}, and only from
         * {@link #vanaf}). The menu is sent again afterwards.
         */
        default void doe(ServerPlayer p, ServerLevel level, BouwkeetBlockEntity keet, int actie, int waarde, String tekst) {
        }
    }

    private static final Map<String, Tab> TABS = new LinkedHashMap<>();

    /** Adds a tab, or replaces the one with the same id. */
    public static synchronized void registreer(Tab tab) {
        TABS.put(tab.id(), tab);
    }

    public static synchronized List<Tab> lijst() {
        List<Tab> l = new ArrayList<>(TABS.values());
        l.sort(Comparator.comparingInt(Tab::volgorde));
        return l;
    }

    @Nullable
    public static synchronized Tab get(String id) {
        return TABS.get(id);
    }

    private BouwkeetTabs() {
    }
}
