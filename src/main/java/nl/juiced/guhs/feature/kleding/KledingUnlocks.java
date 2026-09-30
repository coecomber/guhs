package nl.juiced.guhs.feature.kleding;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * The clothes a player has unlocked (2.9): once unlocked, a piece is available for all their tamed guhs. Saved per player
 * in {@code GuhQuests.saved(player)["guhs_kleding"]} (a list of clothes ids; survives death), synced to the client with
 * the payload {@code guhs:kleding_data} ({@link #sync}: on login, on opening the Guhdex and on every change).
 * <p>
 * Phase 1 is only the data and the sync; the consume-to-unlock item, its message and sound, and the wardrobe are the
 * kleding slice's. Hair (GuhClothes.Slot.HAAR) is never an unlock.
 */
public final class KledingUnlocks {
    /** The saved list in {@link GuhQuests#saved}. */
    public static final String KEY = "guhs_kleding";

    /** Has this player unlocked the piece? Server: the saved data; client: the synced cache ({@link Client}). */
    public static boolean heeft(Player player, GuhClothes c) {
        if (player.level().isClientSide()) {
            return Client.UNLOCKS.contains(c);
        }
        return ids(player).contains(c.id());
    }

    /** Unlocks a piece (server); true when it is new (then it is saved and synced). Message and sound: the kleding slice. */
    public static boolean ontgrendel(ServerPlayer player, GuhClothes c) {
        if (ids(player).contains(c.id())) {
            return false;
        }
        ListTag list = GuhQuests.saved(player).getList(KEY, Tag.TAG_STRING);
        list.add(StringTag.valueOf(c.id()));
        GuhQuests.saved(player).put(KEY, list);
        sync(player);
        return true;
    }

    /** Every piece this player has unlocked (server: saved; client: the synced cache). */
    public static Set<GuhClothes> alle(Player player) {
        if (player.level().isClientSide()) {
            return Client.alle();
        }
        Set<GuhClothes> out = EnumSet.noneOf(GuhClothes.class);
        for (String id : ids(player)) {
            GuhClothes c = GuhClothes.byId(id);
            if (c != null) {
                out.add(c);
            }
        }
        return out;
    }

    /**
     * (Beauty show) this player's unlocked pieces for the dressing screen, next to the loaner wardrobe: only the given
     * slots, and per slot only as many as still fit in the last row of that slot ({@code perRij} icons per row; the
     * dressing screen has no room for more rows), the ones that suit the show's themes best first. {@code alTelt}: own
     * pieces that are already in (what your own guh wears).
     */
    public static List<GuhClothes> eigenStukken(Player player, List<GuhClothes.Slot> slots, int perRij, java.util.Collection<GuhClothes> alTelt) {
        List<GuhClothes> out = new ArrayList<>();
        for (GuhClothes.Slot slot : slots) {
            int leen = (int) nl.juiced.guhs.feature.beauty.ShowTheme.loaners().stream().filter(c -> c.slot == slot).count();
            int extra = (int) alTelt.stream().filter(c -> c.slot == slot && !nl.juiced.guhs.feature.beauty.ShowTheme.isLoaner(c)).count();
            int plekken = Math.max(1, (leen + extra + perRij - 1) / perRij) * perRij;
            int ruimte = plekken - leen - extra;
            alle(player).stream()
                    .filter(c -> c.slot == slot && !alTelt.contains(c) && !nl.juiced.guhs.feature.beauty.ShowTheme.isLoaner(c))
                    .sorted(java.util.Comparator.comparingInt((GuhClothes c) -> -java.util.Arrays.stream(
                            nl.juiced.guhs.feature.beauty.ShowTheme.values()).mapToInt(t -> t.fit(c)).sum()).thenComparing(Enum::ordinal))
                    .limit(Math.max(0, ruimte))
                    .forEach(out::add);
        }
        return out;
    }

    /** (Tests) adds an unlock to any player's saved data, without messages or syncing. */
    public static void voegToe(Player player, GuhClothes c) {
        if (!ids(player).contains(c.id())) {
            ListTag list = GuhQuests.saved(player).getList(KEY, Tag.TAG_STRING);
            list.add(StringTag.valueOf(c.id()));
            GuhQuests.saved(player).put(KEY, list);
        }
    }

    /** (Tests / admin) forgets every unlock of this player. */
    public static void wis(ServerPlayer player) {
        GuhQuests.saved(player).remove(KEY);
        sync(player);
    }

    private static List<String> ids(Player player) {
        ListTag list = GuhQuests.saved(player).getList(KEY, Tag.TAG_STRING);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            out.add(list.getString(i));
        }
        return out;
    }

    /** Sends the player's unlocks to their client (payload guhs:kleding_data, clothes ids). */
    public static void sync(ServerPlayer player) {
        ModNetworking.sendTo(player, new KledingPayloads.KledingData(ids(player)));
    }

    /** The client's copy of its own unlocks. */
    public static final class Client {
        static final Set<GuhClothes> UNLOCKS = ConcurrentHashMap.newKeySet();

        public static void set(List<String> ids) {
            UNLOCKS.clear();
            for (String id : ids) {
                GuhClothes c = GuhClothes.byId(id);
                if (c != null) {
                    UNLOCKS.add(c);
                }
            }
        }

        public static boolean heeft(GuhClothes c) {
            return UNLOCKS.contains(c);
        }

        public static Set<GuhClothes> alle() {
            Set<GuhClothes> out = EnumSet.noneOf(GuhClothes.class);
            out.addAll(UNLOCKS);
            return out;
        }

        private Client() {
        }
    }

    private KledingUnlocks() {
    }
}
