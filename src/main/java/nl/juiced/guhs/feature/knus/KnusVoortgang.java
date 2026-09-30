package nl.juiced.guhs.feature.knus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * The progress on the Knus tab of the Guhdex (2.8): per section ("onderdeel", one per feature) milestones with a
 * progress bar and a reward to claim, and collection pages (receptenboek, knuffelkast, seizoensplakboek...).
 * <p>
 * <b>Registration</b> is static and runs on both sides: call {@link #mijlpaal} and {@link #verzameling} from your
 * Feature.register (the sections themselves are pre-registered by {@link Knus}, in the order of the Knus tab).
 * <ul>
 *   <li>A milestone counts a <i>counter</i> ({@code <pkg>.<naam>}) up to its goal: {@link #tel} (add) or
 *       {@link #hoogste} (keep the best, for records). When it is reached the player gets a toast and (if given) the
 *       hidden advancement {@code guhs:quest/<questAdvancement>}; the reward is claimed in the Knus tab and given with
 *       {@link Minigames#give} (an empty stack: nothing to claim, only the tick).</li>
 *   <li>A collection is a fixed list of entry ids; {@link #ontdek} fills one in (a toast, once).</li>
 * </ul>
 * Lang keys: {@code gui.guhs.knus.onderdeel.<onderdeel>}, {@code gui.guhs.knus.mijlpaal.<id>},
 * {@code gui.guhs.knus.verzameling.<id>}, entries {@code gui.guhs.knus.<verzameling>.<item>} and (optional) the text
 * shown with an entry {@code gui.guhs.knus.<verzameling>.<item>.info}.
 * <p>
 * <b>Data</b> per player lives in {@link GuhQuests#saved} (survives dying), compound {@value #KEY}; the client gets it
 * with the payload {@code guhs:knus_data} (on opening the Guhdex and after every change), claims go back with
 * {@code guhs:knus_claim}.
 */
public final class KnusVoortgang {
    public static final String KEY = "guhs_knus";

    /** A section of the Knus tab. */
    public record Onderdeel(String id, Supplier<ItemStack> icoon) {
        public Component naam() {
            return Component.translatable("gui.guhs.knus.onderdeel." + id);
        }
    }

    /** A milestone: counter {@code teller} reaches {@code doel}. */
    public record Mijlpaal(String onderdeel, String id, String teller, int doel, Supplier<ItemStack> beloning, @Nullable String questAdvancement) {
        public Component naam() {
            return Component.translatable("gui.guhs.knus.mijlpaal." + id);
        }
    }

    /** A collection page with a fixed list of entries. */
    public record Verzameling(String onderdeel, String id, List<String> items, Function<String, ItemStack> icoon) {
        public Component naam() {
            return Component.translatable("gui.guhs.knus.verzameling." + id);
        }

        public Component item(String item) {
            return Component.translatable("gui.guhs.knus." + id + "." + item);
        }

        /** The lang key of an entry's text (may be missing: then there is no text). */
        public String infoKey(String item) {
            return "gui.guhs.knus." + id + "." + item + ".info";
        }
    }

    private static final Map<String, Onderdeel> ONDERDELEN = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<String, Mijlpaal> MIJLPALEN = Collections.synchronizedMap(new LinkedHashMap<>());
    private static final Map<String, Verzameling> VERZAMELINGEN = Collections.synchronizedMap(new LinkedHashMap<>());

    private KnusVoortgang() {
    }

    // =================================================================================================================
    // registration
    // =================================================================================================================

    /** A section (phase 1 registers all of them, in tab order; see {@link Knus#ONDERDELEN}). Registering again only swaps the icon. */
    public static void onderdeel(String id, Supplier<ItemStack> icoon) {
        ONDERDELEN.put(id, new Onderdeel(id, icoon));
    }

    public static void mijlpaal(String onderdeel, String id, String teller, int doel, Supplier<ItemStack> beloning) {
        mijlpaal(onderdeel, id, teller, doel, beloning, null);
    }

    /**
     * A milestone in a section: counter {@code teller} reaches {@code doel}. {@code questAdvancement} (without "quest/"),
     * if not null, is granted with {@link GuhAdvancements#grant} when it is reached.
     */
    public static void mijlpaal(String onderdeel, String id, String teller, int doel, Supplier<ItemStack> beloning,
                                @Nullable String questAdvancement) {
        if (!ONDERDELEN.containsKey(onderdeel)) {
            throw new IllegalArgumentException("unknown Knus section " + onderdeel + " (milestone " + id + ")");
        }
        if (doel <= 0) {
            throw new IllegalArgumentException("milestone " + id + ": goal must be > 0");
        }
        MIJLPALEN.put(id, new Mijlpaal(onderdeel, id, teller, doel, beloning, questAdvancement));
    }

    /** A collection page in a section, with its entries (in page order) and an icon per entry. */
    public static void verzameling(String onderdeel, String id, List<String> items, Function<String, ItemStack> icoon) {
        if (!ONDERDELEN.containsKey(onderdeel)) {
            throw new IllegalArgumentException("unknown Knus section " + onderdeel + " (collection " + id + ")");
        }
        VERZAMELINGEN.put(id, new Verzameling(onderdeel, id, List.copyOf(items), icoon));
    }

    public static List<Onderdeel> onderdelen() {
        synchronized (ONDERDELEN) {
            return List.copyOf(ONDERDELEN.values());
        }
    }

    public static List<Mijlpaal> mijlpalen(String onderdeel) {
        synchronized (MIJLPALEN) {
            return MIJLPALEN.values().stream().filter(m -> m.onderdeel().equals(onderdeel)).toList();
        }
    }

    public static List<Verzameling> verzamelingen(String onderdeel) {
        synchronized (VERZAMELINGEN) {
            return VERZAMELINGEN.values().stream().filter(v -> v.onderdeel().equals(onderdeel)).toList();
        }
    }

    public static List<Mijlpaal> alleMijlpalen() {
        synchronized (MIJLPALEN) {
            return List.copyOf(MIJLPALEN.values());
        }
    }

    public static List<Verzameling> alleVerzamelingen() {
        synchronized (VERZAMELINGEN) {
            return List.copyOf(VERZAMELINGEN.values());
        }
    }

    @Nullable
    public static Mijlpaal mijlpaal(String id) {
        return MIJLPALEN.get(id);
    }

    @Nullable
    public static Verzameling verzameling(String id) {
        return VERZAMELINGEN.get(id);
    }

    // =================================================================================================================
    // progress (server)
    // =================================================================================================================

    /** The player's Knus data (a live compound in their saved data). */
    static CompoundTag data(ServerPlayer player) {
        CompoundTag saved = GuhQuests.saved(player);
        if (!saved.contains(KEY)) {
            saved.put(KEY, new CompoundTag());
        }
        return saved.getCompoundOrEmpty(KEY);
    }

    private static CompoundTag compound(CompoundTag parent, String key) {
        if (!parent.contains(key)) {
            parent.put(key, new CompoundTag());
        }
        return parent.getCompoundOrEmpty(key);
    }

    private static ListTag strings(CompoundTag parent, String key) {
        if (!parent.contains(key)) {
            parent.put(key, new ListTag());
        }
        return parent.getListOrEmpty(key);
    }

    /** Adds to a counter (never below 0); returns the new total. Reached milestones get a toast; the client is updated. */
    public static int tel(ServerPlayer player, String teller, int erbij) {
        CompoundTag tellers = compound(data(player), "Tellers");
        int was = tellers.getIntOr(teller, 0);
        int now = (int) Math.max(0, Math.min(Integer.MAX_VALUE, (long) was + erbij));
        tellers.putInt(teller, now);
        afterChange(player, teller, was, now);
        return now;
    }

    /** Keeps the highest value (records, e.g. a best score); returns the counter afterwards. */
    public static int hoogste(ServerPlayer player, String teller, int waarde) {
        CompoundTag tellers = compound(data(player), "Tellers");
        int was = tellers.getIntOr(teller, 0);
        if (waarde <= was) {
            return was;
        }
        tellers.putInt(teller, waarde);
        afterChange(player, teller, was, waarde);
        return waarde;
    }

    public static int teller(ServerPlayer player, String teller) {
        return compound(data(player), "Tellers").getIntOr(teller, 0);
    }

    /** Fills in a collection entry; true when it is new (a toast, and the client is updated). */
    public static boolean ontdek(ServerPlayer player, String verzameling, String item) {
        Verzameling v = verzameling(verzameling);
        if (v == null || !v.items().contains(item)) {
            org.slf4j.LoggerFactory.getLogger("guhs").warn("Knus: unknown collection entry {}/{}", verzameling, item);
            return false;
        }
        ListTag list = strings(compound(data(player), "Verzamelingen"), verzameling);
        for (Tag t : list) {
            if (t.asString().orElse("").equals(item)) {
                return false;
            }
        }
        list.add(StringTag.valueOf(item));
        long found = list.size();
        melden(player, "verzameling", verzameling, item);
        player.sendOverlayMessage(Component.translatable("gui.guhs.knus.nieuw_item", v.item(item), v.naam(), found, v.items().size())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1f, 1.3f);
        sync(player);
        return true;
    }

    public static boolean heeft(ServerPlayer player, String verzameling, String item) {
        return ontdekt(player, verzameling).contains(item);
    }

    public static Set<String> ontdekt(ServerPlayer player, String verzameling) {
        Set<String> out = new LinkedHashSet<>();
        CompoundTag all = data(player).getCompoundOrEmpty("Verzamelingen");
        for (Tag t : all.getListOrEmpty(verzameling)) {
            out.add(t.asString().orElse(""));
        }
        return out;
    }

    /** Has the player reached this milestone? */
    public static boolean bereikt(ServerPlayer player, Mijlpaal m) {
        return teller(player, m.teller()) >= m.doel();
    }

    public static boolean geclaimd(ServerPlayer player, String mijlpaal) {
        for (Tag t : data(player).getListOrEmpty("Geclaimd")) {
            if (t.asString().orElse("").equals(mijlpaal)) {
                return true;
            }
        }
        return false;
    }

    /** Claims a reached milestone's reward (from the Knus tab); false if it can't (not reached, already claimed). */
    public static boolean claim(ServerPlayer player, String id) {
        Mijlpaal m = mijlpaal(id);
        if (m == null || !bereikt(player, m) || geclaimd(player, id)) {
            return false;
        }
        strings(data(player), "Geclaimd").add(StringTag.valueOf(id));
        ItemStack reward = m.beloning().get();
        if (reward != null && !reward.isEmpty()) {
            Minigames.give(player, reward.copy());
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.3f);
        sync(player);
        return true;
    }

    private static void afterChange(ServerPlayer player, String teller, int was, int now) {
        List<Mijlpaal> reached = new ArrayList<>();
        synchronized (MIJLPALEN) {
            for (Mijlpaal m : MIJLPALEN.values()) {
                if (m.teller().equals(teller) && was < m.doel() && now >= m.doel()) {
                    reached.add(m);
                }
            }
        }
        for (Mijlpaal m : reached) {
            if (m.questAdvancement() != null) {
                GuhAdvancements.grant(player, m.questAdvancement());
            }
            melden(player, "mijlpaal", m.id(), "");
            player.sendSystemMessage(Component.translatable("gui.guhs.knus.mijlpaal_bereikt", m.naam()).withStyle(ChatFormatting.LIGHT_PURPLE));
            player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.5f);
        }
        sync(player);
    }

    /** A toast for the client (sent along with the next sync). */
    private static void melden(ServerPlayer player, String soort, String id, String item) {
        CompoundTag m = new CompoundTag();
        m.putString("Soort", soort);
        m.putString("Id", id);
        m.putString("Item", item);
        PENDING.computeIfAbsent(player.getUUID(), u -> new ArrayList<>()).add(m);
    }

    private static final Map<java.util.UUID, List<CompoundTag>> PENDING = new java.util.concurrent.ConcurrentHashMap<>();

    /** Sends the player's Knus data (and pending toasts) to their client. */
    public static void sync(ServerPlayer player) {
        ListTag toasts = new ListTag();
        List<CompoundTag> pending = PENDING.remove(player.getUUID());
        if (pending != null) {
            toasts.addAll(pending);
        }
        CompoundTag extra = new CompoundTag();
        extra.put("Toasts", toasts);
        nl.juiced.guhs.network.ModNetworking.sendTo(player, new KnusPayloads.KnusData(data(player).copy(), extra));
    }

    // =================================================================================================================
    // client
    // =================================================================================================================

    /** What the client knows (from guhs:knus_data): read by the Guhdex's Knus tab. */
    public static final class Client {
        private static CompoundTag data = new CompoundTag();

        static void set(CompoundTag newData) {
            data = newData;
        }

        public static int teller(String teller) {
            return data.getCompoundOrEmpty("Tellers").getIntOr(teller, 0);
        }

        public static boolean bereikt(Mijlpaal m) {
            return teller(m.teller()) >= m.doel();
        }

        public static boolean geclaimd(String mijlpaal) {
            for (Tag t : data.getListOrEmpty("Geclaimd")) {
                if (t.asString().orElse("").equals(mijlpaal)) {
                    return true;
                }
            }
            return false;
        }

        public static Set<String> ontdekt(String verzameling) {
            Set<String> out = new LinkedHashSet<>();
            for (Tag t : data.getCompoundOrEmpty("Verzamelingen").getListOrEmpty(verzameling)) {
                out.add(t.asString().orElse(""));
            }
            return out;
        }

        /** Done / total of a section: reached milestones plus found entries. */
        public static int[] voortgang(String onderdeel) {
            int done = 0, total = 0;
            for (Mijlpaal m : mijlpalen(onderdeel)) {
                total++;
                if (bereikt(m)) {
                    done++;
                }
            }
            for (Verzameling v : verzamelingen(onderdeel)) {
                total += v.items().size();
                Set<String> found = ontdekt(v.id());
                done += (int) v.items().stream().filter(found::contains).count();
            }
            return new int[]{done, total};
        }

        private Client() {
        }
    }
}
