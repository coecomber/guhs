package nl.juiced.guhs.feature.kleding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * Favourite outfits (2.9): {@value #AANTAL} per player, saved in {@code GuhQuests.saved(player)["guhs_kleding_favorieten"]}
 * (a list of strings, one per favourite: the clothes ids of the wardrobe slots in GuhClothes.Slot.kleding() order,
 * comma-separated, empty = nothing; an empty string = no favourite yet). Synced with guhs:kleding_favorieten.
 */
public final class KledingFavorieten {
    public static final String KEY = "guhs_kleding_favorieten";
    public static final int AANTAL = 5;

    /** The favourites as strings (always {@value #AANTAL}). */
    public static List<String> alle(ServerPlayer player) {
        ListTag list = GuhQuests.saved(player).getListOrEmpty(KEY);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < AANTAL; i++) {
            out.add(i < list.size() ? list.getStringOr(i, "") : "");
        }
        return out;
    }

    /** Saves favourite {@code index} (an outfit per slot, null = nothing; all null = forget it). */
    public static void bewaar(ServerPlayer player, int index, List<GuhClothes> outfit) {
        if (index < 0 || index >= AANTAL) {
            return;
        }
        List<String> alle = alle(player);
        alle.set(index, codeer(outfit));
        ListTag list = new ListTag();
        alle.forEach(s -> list.add(StringTag.valueOf(s)));
        GuhQuests.saved(player).put(KEY, list);
        sync(player);
        if (!alle.get(index).isEmpty()) {
            KledingOntgrendel.grant(player, "kleding_favoriet");
        }
    }

    public static void sync(ServerPlayer player) {
        ModNetworking.sendTo(player, new KledingPayloads.Favorieten(alle(player)));
    }

    /** An outfit as a string ("" when it has nothing at all). */
    public static String codeer(List<GuhClothes> outfit) {
        if (outfit.stream().allMatch(java.util.Objects::isNull)) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < GuhClothes.Slot.kleding().size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            GuhClothes c = i < outfit.size() ? outfit.get(i) : null;
            if (c != null) {
                sb.append(c.id());
            }
        }
        return sb.toString();
    }

    /** A saved string back to an outfit (one entry per wardrobe slot; unknown or misplaced ids become null). */
    public static List<GuhClothes> decodeer(String s) {
        List<GuhClothes.Slot> slots = GuhClothes.Slot.kleding();
        GuhClothes[] out = new GuhClothes[slots.size()];
        if (s != null && !s.isEmpty()) {
            String[] ids = s.split(",", -1);
            for (int i = 0; i < ids.length && i < out.length; i++) {
                GuhClothes c = GuhClothes.byId(ids[i]);
                out[i] = c != null && c.slot == slots.get(i) ? c : null;
            }
        }
        return Arrays.asList(out);
    }

    /** The client's copy of its own favourites. */
    public static final class Client {
        private static List<String> favorieten = new ArrayList<>(java.util.Collections.nCopies(AANTAL, ""));

        public static void set(List<String> list) {
            List<String> out = new ArrayList<>(list);
            while (out.size() < AANTAL) {
                out.add("");
            }
            favorieten = out;
        }

        @Nullable
        public static List<GuhClothes> get(int index) {
            String s = index >= 0 && index < favorieten.size() ? favorieten.get(index) : "";
            return s.isEmpty() ? null : decodeer(s);
        }

        private Client() {
        }
    }

    private KledingFavorieten() {
    }
}
