package nl.juiced.guhs.feature.vadsrijk.karakter;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;

/**
 * T-KARAKTER: the character key of Het Vadsrijk (VOORSTEL_ROADMAP §7, decisions R1). A player (account) has up to
 * {@link #SLOTS} characters; exactly one is ACTIVE.
 * <p><b>How it works (read this before you build a store):</b>
 * <ul>
 *   <li>The kern attachment {@link SpelerData} ({@code Spelers.data(p)}) always holds the ACTIVE character. Every field and every
 *   {@code extra(slice)} compound in it is PER CHARACTER by default: switching saves the whole thing into the old slot and loads
 *   the new slot's copy. So existing code needs no change, and a store you add to {@link SpelerData#extra} is per character
 *   automatically (one place, no "forgotten store" leaks).</li>
 *   <li>ACCOUNT-WIDE data stays put on a switch: use {@link #account(Player, String)} (= {@code extra("account_<slice>")}), or
 *   register an existing slice/key with {@link #accountBreed(String)} / {@link #accountBreed(String, String)}.</li>
 *   <li>Stores OUTSIDE the attachment (SavedData, files) keyed per player: key them with {@link #sleutel(Player)}
 *   ({@link Sleutel#tekst()} = "uuid/slot") when they are per character, with the plain UUID when they are account-wide.</li>
 *   <li>Existing saves migrate by themselves: what is in the attachment now simply IS slot 0 ({@link #lijst} creates the list).</li>
 * </ul>
 * Account-wide by decision: cosmetics (titles, post, wardrobe, wings, effects), the mount + pet collection INCLUDING their levels
 * (kept simple: a mount is "yours", whichever knight rides it), the bank and the materiaaltas, and the tips you already saw.
 * Per character: levels, class levels, skill trees, the Vadsrijk inventory + worn gear + jewellery, quests, story scenes,
 * exploration book, zone flags/doors, checkpoint, Vadsmunten, everything else.
 */
public final class Karakter {
    /** Character slots per account. */
    public static final int SLOTS = 3;
    /** Prefix of account-wide {@code extra} slices: {@code extra("account_bank")} never switches. */
    public static final String ACCOUNT = "account_";
    /** The account's character list (itself account-wide). */
    static final String LIJST = ACCOUNT + "karakter";
    /** SpelerData fields that belong to the account (the normal-world stash, the way home, found items, seen tips). */
    static final List<String> SCHIL = List.of("binnen", "opgeborgen", "opgeborgen_slot", "terug", "gevonden", "tips");

    /** The key of one character: player UUID + slot (0..{@link #SLOTS}-1). Slot 0 = the migrated, original character. */
    public record Sleutel(UUID speler, int slot) {
        /** "uuid/slot": use this as the map key of a per-character store outside the attachment. */
        public String tekst() {
            return speler + "/" + slot;
        }

        @Nullable
        public static Sleutel van(String s) {
            int i = s.lastIndexOf('/');
            try {
                return i < 0 ? new Sleutel(UUID.fromString(s), 0) : new Sleutel(UUID.fromString(s.substring(0, i)), Integer.parseInt(s.substring(i + 1)));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /** A switch that happened: {@code van} -> {@code naar}; {@code nieuw} = the target was just created (first time played). */
    public record Wissel(int van, int naar, boolean nieuw) {
    }

    /** Before a switch (the old character is still live): close your screens, send your entities home. */
    public static final List<Consumer<ServerPlayer>> VOOR_WISSEL = new CopyOnWriteArrayList<>();
    /** After a switch (the new character is live, inventory swapped, teleported): resync what your slice shows. */
    public static final List<BiConsumer<ServerPlayer, Wissel>> GEWISSELD = new CopyOnWriteArrayList<>();

    private static final Set<String> ACCOUNT_SLICES = ConcurrentHashMap.newKeySet();
    private static final Map<String, Set<String>> ACCOUNT_SLEUTELS = new ConcurrentHashMap<>();

    static {
        // the existing account-wide stores (decision R1: cosmetics + mounts + pets are account-wide)
        accountBreed("huisdieren");                       // G-PETS: owned pets + the chosen one
        for (String k : List.of("rijdieren", "actief", "starter", "titels", "post")) {
            accountBreed("economie", k);                  // mounts (collection + levels + colours), titles, the cosmetics post
        }
    }

    private Karakter() {
    }

    // ---- the API for other slices --------------------------------------------------------------------------------------------

    /** The active character's key. */
    public static Sleutel sleutel(Player p) {
        return new Sleutel(p.getUUID(), slot(p));
    }

    /** The active slot (0 when the list doesn't exist yet: an old save is slot 0). */
    public static int slot(Player p) {
        CompoundTag l = Spelers.data(p).extra.get(LIJST);
        return l == null ? 0 : l.getIntOr("actief", 0);
    }

    /** An ACCOUNT-WIDE compound for your slice (saved in player.dat, never switched). */
    public static CompoundTag account(Player p, String slice) {
        return Spelers.data(p).extra(ACCOUNT + slice);
    }

    /** A PER-CHARACTER compound for your slice (the same as {@code Spelers.data(p).extra(slice)}). */
    public static CompoundTag karakter(Player p, String slice) {
        return Spelers.data(p).extra(slice);
    }

    /** Marks an existing {@code extra} slice as account-wide as a whole (call from your registry class, at mod init). */
    public static void accountBreed(String slice) {
        ACCOUNT_SLICES.add(slice);
    }

    /** Marks one key inside an existing {@code extra} slice as account-wide (the rest of the slice stays per character). */
    public static void accountBreed(String slice, String sleutel) {
        ACCOUNT_SLEUTELS.computeIfAbsent(slice, k -> ConcurrentHashMap.newKeySet()).add(sleutel);
    }

    public static boolean isAccountBreed(String slice) {
        return slice.startsWith(ACCOUNT) || ACCOUNT_SLICES.contains(slice);
    }

    /** The active character's game mode. */
    public static Modus modus(Player p) {
        return Modus.van(info(p, slot(p)).getStringOr("modus", Modus.NORMAAL.id));
    }

    /** The active character's name. */
    public static String naam(Player p) {
        return info(p, slot(p)).getStringOr("naam", p.getGameProfile().name());
    }

    /** Retired Eénvads knight (a badge, plays as Normaal)? */
    public static boolean gepensioneerd(Player p) {
        return info(p, slot(p)).getBooleanOr("pensioen", false);
    }

    // ---- the list ---------------------------------------------------------------------------------------------------------------

    /**
     * The account's character list ({@code extra("account_karakter")}): {@code actief} + {@code s0..s2} = {naam, modus, begin (mode at
     * creation), pensioen, gemaakt; for the inactive ones also data (their saved SpelerData part), plek, level, glevel, klasse}.
     * Creates it (= the migration: the current data becomes slot 0) when missing.
     */
    public static CompoundTag lijst(Player p) {
        SpelerData d = Spelers.data(p);
        CompoundTag l = d.extra.get(LIJST);
        if (l == null) {
            l = migreer(p.getGameProfile().name());
            d.extra.put(LIJST, l);
        }
        return l;
    }

    /** A fresh list with the existing data as slot 0 (Normaal, named after the player). */
    static CompoundTag migreer(String spelernaam) {
        CompoundTag l = new CompoundTag();
        l.putInt("actief", 0);
        l.putInt("versie", 1);
        CompoundTag s = new CompoundTag();
        s.putString("naam", spelernaam);
        s.putString("modus", Modus.NORMAAL.id);
        s.putString("begin", Modus.NORMAAL.id);
        s.putLong("gemaakt", System.currentTimeMillis());
        s.putBoolean("gemigreerd", true);
        l.put("s0", s);
        return l;
    }

    /** One slot's info (empty compound = free slot). Live object: changes stick. */
    public static CompoundTag info(Player p, int slot) {
        CompoundTag l = lijst(p);
        String k = "s" + slot;
        if (!l.contains(k)) {
            return new CompoundTag();
        }
        return l.getCompoundOrEmpty(k);
    }

    public static boolean bestaat(Player p, int slot) {
        return slot >= 0 && slot < SLOTS && lijst(p).contains("s" + slot);
    }

    // ---- splitting the attachment into account + character --------------------------------------------------------------------

    static CompoundTag schrijf(ServerPlayer p, SpelerData d) {
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, p.registryAccess());
        d.serialize(out);
        return out.buildResult();
    }

    static void lees(ServerPlayer p, SpelerData d, CompoundTag t) {
        d.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, p.registryAccess(), t));
        d.klaarOp.clear();
        d.sieradenCache.clear();
        d.gebied = null;
    }

    /** Only the per-character part of a full SpelerData tag. */
    static CompoundTag alleenKarakter(CompoundTag vol) {
        CompoundTag t = vol.copy();
        for (String k : SCHIL) {
            t.remove(k);
        }
        CompoundTag extra = t.getCompoundOrEmpty("extra").copy();
        for (String slice : List.copyOf(extra.keySet())) {
            if (isAccountBreed(slice)) {
                extra.remove(slice);
                continue;
            }
            Set<String> keys = ACCOUNT_SLEUTELS.get(slice);
            if (keys != null) {
                CompoundTag s = extra.getCompoundOrEmpty(slice).copy();
                keys.forEach(s::remove);
                extra.put(slice, s);
            }
        }
        t.put("extra", extra);
        return t;
    }

    /** A character part + the account part of {@code vol} = a full SpelerData tag. */
    static CompoundTag metAccount(CompoundTag karakter, CompoundTag vol) {
        CompoundTag t = karakter.copy();
        for (String k : SCHIL) {
            t.remove(k);
            if (vol.contains(k)) {
                t.put(k, vol.get(k).copy());
            }
        }
        CompoundTag extra = t.getCompoundOrEmpty("extra").copy();
        CompoundTag volExtra = vol.getCompoundOrEmpty("extra");
        for (String slice : List.copyOf(extra.keySet())) {   // (a stale account copy inside a character part never wins)
            if (isAccountBreed(slice)) {
                extra.remove(slice);
            }
        }
        for (String slice : volExtra.keySet()) {
            if (isAccountBreed(slice)) {
                extra.put(slice, volExtra.getCompoundOrEmpty(slice).copy());
                continue;
            }
            Set<String> keys = ACCOUNT_SLEUTELS.get(slice);
            if (keys != null) {
                CompoundTag van = volExtra.getCompoundOrEmpty(slice);
                CompoundTag s = extra.getCompoundOrEmpty(slice).copy();
                for (String k : keys) {
                    s.remove(k);
                    if (van.contains(k)) {
                        s.put(k, van.get(k).copy());
                    }
                }
                extra.put(slice, s);
            }
        }
        t.put("extra", extra);
        return t;
    }
}
