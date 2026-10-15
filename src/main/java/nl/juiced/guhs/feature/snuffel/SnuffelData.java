package nl.juiced.guhs.feature.snuffel;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * Everything Het Snuffeleiland keeps about one player, in the player's own file (it survives death, a restart and a
 * crash together with the inventory): {@code GuhQuests.saved(p)["guhs_snuffel"]}. Keys: {@code Keuze} (the chosen dog and
 * companion), {@code Vorm} (in dog form: the own inventory is in {@link SnuffelKluis}), {@code Thuis} (exactly where the
 * player stood before leaving), {@code Eiland} (the last spot on the island, relative to the island's corner),
 * {@code Bezocht}, {@code Geuren} (learned scents), {@code Gevonden} (scent sources found), {@code Daden} (good deeds),
 * {@code Boom} (0..4), {@code Maatje} (the companion appeared), {@code Examen}, {@code Diploma}.
 */
public final class SnuffelData {
    public static final String SLEUTEL = "guhs_snuffel";

    private SnuffelData() {
    }

    /** The player's island data (made the first time it is asked for). */
    public static CompoundTag van(Player p) {
        CompoundTag saved = GuhQuests.saved(p);
        if (saved.getCompound(SLEUTEL).isEmpty()) {
            saved.put(SLEUTEL, new CompoundTag());
        }
        return saved.getCompoundOrEmpty(SLEUTEL);
    }

    /** Does this player have any island data at all (asking never creates it)? */
    public static boolean heeft(Player p) {
        return GuhQuests.saved(p).getCompound(SLEUTEL).isPresent();
    }

    /** A list of words under this key (a copy). */
    public static List<String> lijst(Player p, String key) {
        List<String> uit = new ArrayList<>();
        if (!heeft(p)) {
            return uit;
        }
        ListTag l = van(p).getListOrEmpty(key);
        for (int i = 0; i < l.size(); i++) {
            String s = l.getStringOr(i, "");
            if (!s.isEmpty()) {
                uit.add(s);
            }
        }
        return uit;
    }

    public static boolean bevat(Player p, String key, String woord) {
        return lijst(p, key).contains(woord);
    }

    /** Adds the word; false when it was there already. */
    public static boolean voegToe(Player p, String key, String woord) {
        if (bevat(p, key, woord)) {
            return false;
        }
        CompoundTag t = van(p);
        ListTag l = t.getListOrEmpty(key).copy();
        l.add(StringTag.valueOf(woord));
        t.put(key, l);
        return true;
    }

    public static boolean haalWeg(Player p, String key, String woord) {
        List<String> oud = lijst(p, key);
        if (!oud.remove(woord)) {
            return false;
        }
        ListTag nieuw = new ListTag();
        for (String s : oud) {
            nieuw.add(StringTag.valueOf(s));
        }
        van(p).put(key, nieuw);
        return true;
    }
}
