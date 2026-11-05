package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.ambacht.Recepten.Recept;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Z3Data;

/**
 * The Bestelbord (A2: "dagelijks 3 bestellingen + een weekkist"): every day (06:00 Europe/Amsterdam, the same day service as the
 * Premiebord: {@link Z3Data#dag}) 3 orders from the town's guhs, picked from what YOUR levels can do, with one free swap a day.
 * Three kinds: {@code breng} (hand in N of a material / dish / potion, any quality), {@code maak} (make N with a recipe of an
 * ambacht), {@code verzamel} (gather N of an ambacht). Reward: ambacht xp ({@link #XP_DEEL} of a level), Vadsmunten; after
 * {@link #WEEKDOEL} orders in a week the Ambachtskist (kernsplinters, a rare material, munten).
 * State: {@code AmbachtData "bestel"}.
 */
public final class Bestelbord {
    public static final int PER_DAG = 3, WEEKDOEL = 12;
    public static final double XP_DEEL = 0.08;
    /** The guhs who order things (their names are the npc slice's lang keys). */
    static final List<String> GEVERS = List.of("bakker_bram", "oma_truus", "kleine_kiki", "dichter_daan", "postguh_pim", "tuinier_tilly", "muzikant_mo",
            "professor_pluis", "ridder_rammel", "kapster_fohnie", "schilder_sien", "koopman_kees");

    /** One possible order: kind, ambacht, minimum level, what (material/item id, recipe id or ""), how many. */
    public record Bestelling(String id, String soort, Ambacht ambacht, int level, String wat, int aantal) {
    }

    public static final Map<String, Bestelling> POOL = new LinkedHashMap<>();

    static {
        for (Materialen.Materiaal m : Materialen.ALLE.values()) {
            if (m.tier() > 0) {
                b("breng_" + m.id(), "breng", m.ambacht(), m.ambacht() == Ambacht.IMKEREN ? 1 : AmbachtXp.tierLevel(m.tier()), m.id(), m.ambacht() == Ambacht.IMKEREN ? 4 : 10);
            }
        }
        for (Recept r : Recepten.ALLE.values()) {
            if (r.soort().equals("eten") || r.soort().equals("drank")) {
                b("breng_" + r.id(), "breng", r.ambacht(), r.level(), r.soort() + ":" + r.doel(), 4);
            }
        }
        for (Ambacht a : Ambacht.values()) {
            if (a.verzamel) {
                for (int t = 1; t <= 3; t++) {
                    if (a != Ambacht.IMKEREN || t == 1) {
                        b("verzamel_" + a.id + "_" + t, "verzamel", a, AmbachtXp.tierLevel(t), "", a == Ambacht.IMKEREN ? 6 : 12 + 4 * t);
                    }
                }
            } else {
                for (int t = 1; t <= 3; t++) {
                    b("maak_" + a.id + "_" + t, "maak", a, AmbachtXp.tierLevel(t), "", 3);
                }
            }
        }
    }

    private Bestelbord() {
    }

    private static void b(String id, String soort, Ambacht a, int level, String wat, int n) {
        POOL.put(id, new Bestelling(id, soort, a, level, wat, n));
    }

    // ---- the day -------------------------------------------------------------------------------------------------------------

    /** Today's state (a new day: 3 new orders). */
    public static CompoundTag vandaag(ServerPlayer p) {
        CompoundTag t = AmbachtData.tag(p);
        CompoundTag b = t.getCompoundOrEmpty("bestel");
        String dag = Z3Data.dag(), week = Z3Data.week();
        if (!week.equals(b.getStringOr("week", ""))) {
            b = b.copy();
            b.putString("week", week);
            b.putInt("weekN", 0);
            b.putBoolean("weekKist", false);
            t.put("bestel", b);
        }
        if (!dag.equals(b.getStringOr("dag", ""))) {
            b = b.copy();
            b.putString("dag", dag);
            ListTag ids = new ListTag();
            RandomSource r = RandomSource.create(dag.hashCode() * 31L + p.getUUID().getLeastSignificantBits());
            List<Bestelling> kan = mogelijk(p);
            List<String> gekozen = new ArrayList<>();
            for (int i = 0; i < 40 && gekozen.size() < PER_DAG && !kan.isEmpty(); i++) {
                Bestelling x = kan.get(r.nextInt(kan.size()));
                if (!gekozen.contains(x.id())) {
                    gekozen.add(x.id());
                }
            }
            for (String g : gekozen) {
                ids.add(StringTag.valueOf(g));
            }
            b.put("ids", ids);
            b.put("voortgang", new IntArrayTag(new int[PER_DAG]));
            b.put("geclaimd", new IntArrayTag(new int[PER_DAG]));
            b.putBoolean("geruild", false);
            b.putString("gevers", gevers(r));
            t.put("bestel", b);
        }
        return t.getCompoundOrEmpty("bestel");
    }

    private static String gevers(RandomSource r) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < PER_DAG; i++) {
            s.append(i == 0 ? "" : ",").append(GEVERS.get(r.nextInt(GEVERS.size())));
        }
        return s.toString();
    }

    /** The orders this player can do: their level in that ambacht is high enough (never imkeren before you have a hive). */
    static List<Bestelling> mogelijk(ServerPlayer p) {
        List<Bestelling> l = new ArrayList<>();
        for (Bestelling b : POOL.values()) {
            int lv = AmbachtData.level(p, b.ambacht());
            boolean imker = b.ambacht() == Ambacht.IMKEREN && AmbachtData.tag(p).getCompoundOrEmpty("korven").isEmpty();
            if (lv >= b.level() && lv <= b.level() + 25 && !imker) {
                l.add(b);
            }
        }
        return l;
    }

    public static List<String> ids(ServerPlayer p) {
        ListTag l = vandaag(p).getListOrEmpty("ids");
        List<String> uit = new ArrayList<>();
        for (int i = 0; i < l.size(); i++) {
            l.getString(i).ifPresent(uit::add);
        }
        return uit;
    }

    static int[] getallen(CompoundTag t, String k) {
        int[] a = t.getIntArray(k).orElse(new int[PER_DAG]);
        return a.length >= PER_DAG ? a : java.util.Arrays.copyOf(a, PER_DAG);
    }

    public static int voortgang(ServerPlayer p, int slot) {
        return getallen(vandaag(p), "voortgang")[slot];
    }

    public static boolean geclaimd(ServerPlayer p, int slot) {
        return getallen(vandaag(p), "geclaimd")[slot] != 0;
    }

    public static String gever(ServerPlayer p, int slot) {
        String[] g = vandaag(p).getStringOr("gevers", "").split(",");
        return slot < g.length && !g[slot].isEmpty() ? g[slot] : GEVERS.get(0);
    }

    private static void tel(ServerPlayer p, java.util.function.Predicate<Bestelling> past, int n) {
        CompoundTag t = vandaag(p).copy();
        int[] v = getallen(t, "voortgang");
        List<String> ids = ids(p);
        boolean iets = false;
        for (int i = 0; i < ids.size(); i++) {
            Bestelling b = POOL.get(ids.get(i));
            if (b != null && past.test(b) && v[i] < b.aantal()) {
                v[i] = Math.min(b.aantal(), v[i] + n);
                iets = true;
            }
        }
        if (iets) {
            t.putIntArray("voortgang", v);
            AmbachtData.tag(p).put("bestel", t);
        }
    }

    /** Hook from gathering. */
    static void verzameld(ServerPlayer p, Ambacht a, int n) {
        tel(p, b -> b.soort().equals("verzamel") && b.ambacht() == a, n);
    }

    /** Hook from making. */
    static void gemaakt(ServerPlayer p, Recept r, int n) {
        tel(p, b -> b.soort().equals("maak") && b.ambacht() == r.ambacht() && AmbachtXp.tierVoor(b.level()) <= r.tier(), 1);
    }

    /** The items of a "breng" order. */
    static java.util.function.Predicate<ItemStack> past(Bestelling b) {
        String w = b.wat();
        if (w.startsWith("eten:")) {
            return s -> s.getItem() instanceof Eten.EtenItem e && e.gerecht.id().equals(w.substring(5));
        }
        if (w.startsWith("drank:")) {
            return s -> s.getItem() instanceof Brouwsels.DrankItem d && d.drank.id().equals(w.substring(6));
        }
        return s -> {
            Materialen.Materiaal m = Materialen.van(s);
            return m != null && m.id().equals(w);
        };
    }

    public static boolean klaar(ServerPlayer p, int slot) {
        List<String> ids = ids(p);
        if (slot < 0 || slot >= ids.size()) {
            return false;
        }
        Bestelling b = POOL.get(ids.get(slot));
        if (b == null) {
            return false;
        }
        return b.soort().equals("breng") ? MateriaalTas.tel(p, past(b)) >= b.aantal() : voortgang(p, slot) >= b.aantal();
    }

    /** Hands in an order. Returns "" or a message key. */
    public static String claim(ServerPlayer p, int slot) {
        List<String> ids = ids(p);
        if (slot < 0 || slot >= ids.size() || geclaimd(p, slot)) {
            return "vadsrijk.ambacht.bestel.niks";
        }
        Bestelling b = POOL.get(ids.get(slot));
        if (b == null || !klaar(p, slot)) {
            return "vadsrijk.ambacht.bestel.nog_niet";
        }
        if (b.soort().equals("breng")) {
            MateriaalTas.neem(p, past(b), b.aantal());
        }
        CompoundTag t = vandaag(p).copy();
        int[] c = getallen(t, "geclaimd");
        c[slot] = 1;
        t.putIntArray("geclaimd", c);
        int weekN = t.getIntOr("weekN", 0) + 1;
        t.putInt("weekN", weekN);
        boolean kist = weekN >= WEEKDOEL && !t.getBooleanOr("weekKist", false);
        if (kist) {
            t.putBoolean("weekKist", true);
        }
        AmbachtData.tag(p).put("bestel", t);
        int lv = AmbachtData.level(p, b.ambacht());
        AmbachtData.geefXp(p, b.ambacht(), Math.round(AmbachtXp.xpVoor(lv) * XP_DEEL), false);
        int tier = AmbachtXp.tierVoor(b.level());
        Spelers.geefMunten(p, 60L * tier * tier);
        if (kist) {
            EcoUtil.geefOfBewaar(p, new ItemStack(Materialen.AMBACHTSKIST.get()));
            p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.bestel.weekkist").withStyle(ChatFormatting.GOLD));
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 0.5f, 1.6f);
        return "vadsrijk.ambacht.bestel.geclaimd";
    }

    /** The one free swap a day: a new order in this slot. */
    public static String ruil(ServerPlayer p, int slot) {
        CompoundTag t = vandaag(p).copy();
        List<String> ids = ids(p);
        if (t.getBooleanOr("geruild", false) || slot < 0 || slot >= ids.size() || geclaimd(p, slot)) {
            return "vadsrijk.ambacht.bestel.geen_ruil";
        }
        List<Bestelling> kan = mogelijk(p);
        kan.removeIf(b -> ids.contains(b.id()));
        if (kan.isEmpty()) {
            return "vadsrijk.ambacht.bestel.geen_ruil";
        }
        Bestelling n = kan.get(p.getRandom().nextInt(kan.size()));
        ListTag l = new ListTag();
        for (int i = 0; i < ids.size(); i++) {
            l.add(StringTag.valueOf(i == slot ? n.id() : ids.get(i)));
        }
        t.put("ids", l);
        int[] v = getallen(t, "voortgang");
        v[slot] = 0;
        t.putIntArray("voortgang", v);
        t.putBoolean("geruild", true);
        AmbachtData.tag(p).put("bestel", t);
        return "vadsrijk.ambacht.bestel.geruild";
    }

    /** The Ambachtskist: 2-3 kernsplinters, a rare material, munten. */
    public static void openKist(ServerPlayer p) {
        RandomSource r = p.getRandom();
        EcoUtil.geefOfBewaar(p, new ItemStack(Materialen.KERNSPLINTER.get(), 2 + r.nextInt(2)));
        List<Materialen.Materiaal> zeld = new ArrayList<>();
        for (Materialen.Materiaal m : Materialen.ALLE.values()) {
            if (m.zeldzaam()) {
                zeld.add(m);
            }
        }
        Materialen.Materiaal m = zeld.get(r.nextInt(zeld.size()));
        MateriaalTas.geef(p, Materialen.stack(m.id(), 1, 1));
        Spelers.geefMunten(p, 500);
        p.level().playSound(null, p.blockPosition(), SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 0.7f, 1.2f);
        p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.bestel.kist_open").withStyle(ChatFormatting.GOLD));
    }

    /** The text of an order (for the screen). */
    public static Component tekst(ServerPlayer p, int slot) {
        Bestelling b = POOL.get(ids(p).get(slot));
        Component gever = Component.translatable("vadsrijk.npc.naam." + gever(p, slot));
        if (b == null) {
            return Component.literal("?");
        }
        return switch (b.soort()) {
            case "breng" -> Component.translatable("vadsrijk.ambacht.bestel.breng", gever, b.aantal(), naam(b.wat()));
            case "maak" -> Component.translatable("vadsrijk.ambacht.bestel.maak", gever, b.aantal(), Component.translatable(b.ambacht().naamKey()));
            default -> Component.translatable("vadsrijk.ambacht.bestel.verzamel", gever, b.aantal(), Component.translatable(b.ambacht().naamKey()));
        };
    }

    /** The order text for the screen: {lang key, giver name key, thing name key} (+ the count in the data). */
    public static String[] tekstDelen(ServerPlayer p, int slot) {
        Bestelling b = POOL.get(ids(p).get(slot));
        String gever = "vadsrijk.npc.naam." + gever(p, slot);
        if (b == null) {
            return new String[] {"vadsrijk.ambacht.bestel.niks", gever, ""};
        }
        return switch (b.soort()) {
            case "breng" -> new String[] {"vadsrijk.ambacht.bestel.breng", gever, naamKey(b.wat())};
            case "maak" -> new String[] {"vadsrijk.ambacht.bestel.maak", gever, b.ambacht().naamKey()};
            default -> new String[] {"vadsrijk.ambacht.bestel.verzamel", gever, b.ambacht().naamKey()};
        };
    }

    static String naamKey(String wat) {
        if (wat.startsWith("eten:")) {
            return "item.guhs.vr_amb_eten_" + wat.substring(5);
        }
        if (wat.startsWith("drank:")) {
            return "item.guhs.vr_amb_drank_" + wat.substring(6);
        }
        return "item.guhs.vr_amb_" + wat;
    }

    static Component naam(String wat) {
        if (wat.startsWith("eten:")) {
            return Component.translatable("item.guhs.vr_amb_eten_" + wat.substring(5));
        }
        if (wat.startsWith("drank:")) {
            return Component.translatable("item.guhs.vr_amb_drank_" + wat.substring(6));
        }
        return Component.translatable("item.guhs.vr_amb_" + wat);
    }
}
