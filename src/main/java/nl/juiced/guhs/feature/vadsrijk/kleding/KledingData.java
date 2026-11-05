package nl.juiced.guhs.feature.vadsrijk.kleding;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import nl.juiced.guhs.storage.GuhSavedData;

/**
 * T-KLEDING: the ACCOUNT-wide wardrobe store (R1: cosmetics are per account, not per character slot). One CompoundTag per player
 * UUID, in the overworld's SavedData {@code guhs:vr_kleding} (NOT in {@code SpelerData}, which t-karakter makes per slot).
 * <p>Everything goes through {@link #kast(ServerPlayer)}: when t-karakter publishes its account-store API, replace
 * {@link #BRON} with an adapter to it (one line; the tag layout below stays):
 * <pre>
 *   heeft      {"vleugel:kristal": 1b, ...}   unlocked cosmetics (Cosmetica ids)
 *   keuze      {"rug": "vleugel:kristal", "hoofd": "...", "verf": "verf:roze", "plof": "...", "spoor": "...", "aura": "...",
 *               "naamkleur": "...", "tm.head|tm.chest|tm.legs|tm.feet": "&lt;item id&gt;" | "verborgen"}
 *   looks      {"head|guhs:vr_mob_zwamhoed": "guhs:vr_mob_zwamhoed", ...}   transmog looks (slot|equipment asset -> an item that has it)
 *   kills      int    cheered-up mobs (account-wide counter for kill-count rewards)
 *   bezocht    {"kleermaker": 1b, ...}   tailor NPCs visited (a secret)
 * </pre>
 */
public final class KledingData extends SavedData {
    public static final SavedDataType<KledingData> TYPE = GuhSavedData.tagType("vr_kleding", KledingData::new, KledingData::load, KledingData::save);

    /** Where an account's wardrobe tag comes from (t-karakter can swap this for its account store). */
    public interface Bron {
        CompoundTag kast(ServerPlayer p);

        void bewaard(ServerPlayer p);
    }

    /** Default: this SavedData, keyed by the player's UUID (= the account). */
    public static volatile Bron BRON = new Bron() {
        @Override
        public CompoundTag kast(ServerPlayer p) {
            MinecraftServer s = p.level().getServer();
            if (s == null) {
                return TEST.computeIfAbsent(p.getUUID(), k -> new CompoundTag());
            }
            return van(s).kasten.computeIfAbsent(p.getUUID(), k -> new CompoundTag());
        }

        @Override
        public void bewaard(ServerPlayer p) {
            MinecraftServer s = p.level().getServer();
            if (s != null) {
                van(s).setDirty();
            }
        }
    };

    private static final Map<UUID, CompoundTag> TEST = new ConcurrentHashMap<>();
    private final Map<UUID, CompoundTag> kasten = new ConcurrentHashMap<>();

    public static KledingData van(MinecraftServer s) {
        return s.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static CompoundTag kast(ServerPlayer p) {
        return BRON.kast(p);
    }

    public static void bewaard(ServerPlayer p) {
        BRON.bewaard(p);
    }

    /** A sub-tag of the wardrobe (made when missing). */
    public static CompoundTag deel(ServerPlayer p, String naam) {
        CompoundTag k = kast(p);
        if (!(k.get(naam) instanceof CompoundTag t)) {
            CompoundTag n = new CompoundTag();
            k.put(naam, n);
            return n;
        }
        return t;
    }

    /** Dev/tests: forget this account's wardrobe. */
    public static void wis(ServerPlayer p) {
        CompoundTag k = kast(p);
        for (String s : java.util.List.copyOf(k.keySet())) {
            k.remove(s);
        }
        bewaard(p);
    }

    private static KledingData load(CompoundTag tag) {
        KledingData d = new KledingData();
        for (String k : tag.keySet()) {
            try {
                UUID u = UUID.fromString(k);
                tag.getCompound(k).ifPresent(t -> d.kasten.put(u, t.copy()));
            } catch (IllegalArgumentException e) {
                // (not a uuid: ignore)
            }
        }
        return d;
    }

    private CompoundTag save() {
        CompoundTag t = new CompoundTag();
        kasten.forEach((u, k) -> t.put(u.toString(), k.copy()));
        return t;
    }
}
