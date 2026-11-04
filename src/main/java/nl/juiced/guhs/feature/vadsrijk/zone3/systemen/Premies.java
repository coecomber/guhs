package nl.juiced.guhs.feature.vadsrijk.zone3.systemen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Kristallen;

/**
 * The Premiebord in the Jagershut of Honingtak (ZONE3_PLAN §8.3, ronde 18: LIGHT repeatables, no lockouts, no leaderboard).
 * <ul>
 *   <li>Every day 3 bounties for you from a pool of ~23 (seeded per player per day, so the same all day); reset at 06:00
 *   Europe/Amsterdam ({@link Z3Data#dag}); open from generic lv {@link #MIN_LEVEL}.</li>
 *   <li>1 free swap a day ({@link #ruil}): an unclaimed bounty for another one from the pool.</li>
 *   <li>Reward per bounty ({@link #claim}): xp (5 % of your level, {@link Z3Xp#premie}), Vadsmunten and 10 Jachtpenningen (bosses 15).</li>
 *   <li>Optional week goal: after {@link #WEEKDOEL} bounties in a week (from Monday 06:00) a Premiekist (Episch/Legendarisch zone-3
 *   weapon + 1 Gouden Veer + a crystal).</li>
 *   <li>The penningwinkel ({@link #WINKEL}): Vahoegzegels, crystals, Herrolknabbels... Others may add wares ({@link #voegWaarToe}).</li>
 * </ul>
 * Goals: {@code dood:<area|area|*>} (normal foes defeated in a kern area, by height), {@code elite:<...>}, {@code baas:<area>},
 * {@code rit} (blocks ridden in zone 3), {@code wind} (wind columns used), {@code event:<naam>} (another slice calls
 * {@link #meld}; such bounties are only offered once that slice called {@link #activeer}).
 */
public final class Premies {
    public static final int MIN_LEVEL = 40, PER_DAG = 3, WEEKDOEL = 15, PENNINGEN = 10, PENNINGEN_BAAS = 15;

    public record Premie(String id, String doel, int aantal, int minLevel) {
        public String naamKey() {
            return "vadsrijk.z3.systemen.premie." + id;
        }

        public boolean event() {
            return doel.startsWith("event:");
        }

        public boolean baas() {
            return doel.startsWith("baas:");
        }
    }

    public static final Map<String, Premie> POOL = new LinkedHashMap<>();
    /** Event goals some slice has switched on (only then are those bounties offered). */
    private static final Set<String> ACTIEF = ConcurrentHashMap.newKeySet();

    static {
        p("jacht_kruinzee", "dood:z3_kruinzee", 20, 30);
        p("jacht_takken", "dood:z3_takkenbanen", 20, 34);
        p("jacht_schors", "dood:z3_schorsstad|z3_stamspiraal", 20, 40);
        p("jacht_velden", "dood:z3_wortelvelden", 25, 43);
        p("jacht_duister", "dood:z3_wortelduister", 20, 46);
        p("jacht_overal", "dood:*", 50, 40);
        p("elite_kruin", "elite:z3_kruinzee|z3_takkenbanen", 2, 34);
        p("elite_schors", "elite:z3_schorsstad|z3_stamspiraal", 2, 40);
        p("elite_velden", "elite:z3_wortelvelden", 2, 43);
        p("elite_duister", "elite:z3_wortelduister", 2, 46);
        p("elite_drie", "elite:*", 3, 40);
        p("baas_werf", "baas:z3_zeppelinwerf", 1, 38);
        p("baas_kruin", "baas:z3_kruinzee", 1, 38);
        p("baas_schors", "baas:z3_schorsstad|z3_stamspiraal", 1, 42);
        p("baas_hart", "baas:z3_hart", 1, 49);
        p("rit_kort", "rit", 600, 30);
        p("rit_lang", "rit", 1500, 40);
        p("wind", "wind", 3, 30);
        p("ketels", "event:ketel", 2, 43);
        p("sporen", "event:sporen", 10, 43);
        p("honingpot", "event:honingpot", 1, 40);
        p("muisjes", "event:muisje", 3, 42);
        p("lantaarns", "event:lantaarn", 4, 46);
    }

    private static void p(String id, String doel, int n, int lv) {
        POOL.put(id, new Premie(id, doel, n, lv));
    }

    private Premies() {
    }

    // ---- API for other slices -----------------------------------------------------------------------------------------------

    /** Switch on an event goal (e.g. z3-wezens: "ketel" once the mould kettles exist). */
    public static void activeer(String event) {
        ACTIEF.add(event);
    }

    /** Progress on an event goal ("ketel", "sporen", "honingpot", "muisje", "lantaarn"...). */
    public static void meld(ServerPlayer p, String event, int n) {
        voortgang(p, d -> d.equals("event:" + event), n);
    }

    // ---- the day -------------------------------------------------------------------------------------------------------------

    /** The bounties offered to this player at this level (events only when active). */
    static List<Premie> keuze(int level) {
        List<Premie> l = new ArrayList<>();
        for (Premie x : POOL.values()) {
            if (x.minLevel() <= Math.max(level, MIN_LEVEL) && (!x.event() || ACTIEF.contains(x.doel().substring(6)))) {
                l.add(x);
            }
        }
        return l;
    }

    /** Today's state of this player (rolls a new day after the 06:00 reset). */
    public static CompoundTag vandaag(ServerPlayer p) {
        CompoundTag t = Z3Data.tag(p);
        String dag = Z3Data.dag();
        if (!dag.equals(t.getStringOr("premieDag", ""))) {
            RandomSource r = RandomSource.create(p.getUUID().getLeastSignificantBits() ^ dag.hashCode() * 31L);
            List<Premie> k = keuze(Spelers.level(p));
            List<String> ids = new ArrayList<>();
            while (ids.size() < PER_DAG && !k.isEmpty()) {
                ids.add(k.remove(r.nextInt(k.size())).id());
            }
            t.putString("premieDag", dag);
            t.putString("premies", String.join(",", ids));
            t.putString("voortgang", "0,0,0");
            t.putString("geclaimd", "0,0,0");
            t.putBoolean("geruild", false);
        }
        String week = Z3Data.week();
        if (!week.equals(t.getStringOr("premieWeek", ""))) {
            t.putString("premieWeek", week);
            t.putInt("weekN", 0);
            t.putBoolean("weekKist", false);
        }
        return t;
    }

    public static List<String> ids(ServerPlayer p) {
        String s = vandaag(p).getStringOr("premies", "");
        return s.isEmpty() ? List.of() : List.of(s.split(","));
    }

    static int[] getallen(CompoundTag t, String k) {
        int[] a = new int[PER_DAG];
        String[] s = t.getStringOr(k, "0,0,0").split(",");
        for (int i = 0; i < PER_DAG && i < s.length; i++) {
            try {
                a[i] = Integer.parseInt(s[i].trim());
            } catch (NumberFormatException e) {
                a[i] = 0;
            }
        }
        return a;
    }

    static void zet(CompoundTag t, String k, int[] a) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            b.append(i == 0 ? "" : ",").append(a[i]);
        }
        t.putString(k, b.toString());
    }

    public static int voortgang(ServerPlayer p, int slot) {
        return getallen(vandaag(p), "voortgang")[slot];
    }

    public static boolean geclaimd(ServerPlayer p, int slot) {
        return getallen(vandaag(p), "geclaimd")[slot] != 0;
    }

    public static boolean klaar(ServerPlayer p, int slot) {
        List<String> ids = ids(p);
        Premie x = slot < ids.size() ? POOL.get(ids.get(slot)) : null;
        return x != null && voortgang(p, slot) >= x.aantal();
    }

    /** Adds progress to every unfinished bounty of today whose goal matches. */
    static void voortgang(ServerPlayer p, java.util.function.Predicate<String> doel, int n) {
        if (Spelers.level(p) < MIN_LEVEL || n <= 0) {
            return;
        }
        CompoundTag t = vandaag(p);
        List<String> ids = ids(p);
        int[] v = getallen(t, "voortgang");
        boolean veranderd = false;
        for (int i = 0; i < ids.size(); i++) {
            Premie x = POOL.get(ids.get(i));
            if (x == null || v[i] >= x.aantal() || !doel.test(x.doel())) {
                continue;
            }
            v[i] = Math.min(x.aantal(), v[i] + n);
            veranderd = true;
            if (v[i] >= x.aantal()) {
                p.sendSystemMessage(Component.translatable("vadsrijk.z3.systemen.premie.klaar", Component.translatable(x.naamKey(), x.aantal()))
                        .withStyle(ChatFormatting.GOLD));
                p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
            }
        }
        if (veranderd) {
            zet(t, "voortgang", v);
        }
    }

    /** Does a "dood:/elite:/baas:" goal match this area id? */
    static boolean gebiedPast(String lijst, @Nullable String gebied) {
        if (lijst.equals("*")) {
            return gebied != null && gebied.startsWith("z3_");
        }
        return gebied != null && Arrays.asList(lijst.split("\\|")).contains(gebied);
    }

    /** Kern hook: a defeat counts for every helper (by the area at the foe's position, with height). */
    static void verslagen(Haken.Verslagen v) {
        VadsrijkMob m = v.mob();
        if (!Z3Lagen.inZone3(m.getX(), m.getZ())) {
            return;
        }
        Gebieden.Gebied g = Gebieden.op(m.getX(), m.getY(), m.getZ());
        String gebied = g == null ? null : g.id();
        boolean baas = m instanceof VadsrijkBaas;
        for (ServerPlayer p : v.helpers()) {
            voortgang(p, d -> {
                if (baas) {
                    return d.startsWith("baas:") && gebiedPast(d.substring(5), gebied);
                }
                if (d.startsWith("dood:")) {
                    return gebiedPast(d.substring(5), gebied);
                }
                return m.elite() && d.startsWith("elite:") && gebiedPast(d.substring(6), gebied);
            }, 1);
        }
    }

    /** Ride progress (blocks) and wind columns: from Z3Wind. */
    static void rit(ServerPlayer p, int blokken) {
        voortgang(p, "rit"::equals, blokken);
    }

    static void wind(ServerPlayer p) {
        voortgang(p, "wind"::equals, 1);
    }

    // ---- actions -------------------------------------------------------------------------------------------------------------

    /** Claim a finished bounty: xp, coins, Jachtpenningen, the week goal. Returns a lang key for the board. */
    public static String claim(ServerPlayer p, int slot) {
        if (Spelers.level(p) < MIN_LEVEL) {
            return "vadsrijk.z3.systemen.premie.te_laag";
        }
        CompoundTag t = vandaag(p);
        List<String> ids = ids(p);
        if (slot < 0 || slot >= ids.size()) {
            return "vadsrijk.z3.systemen.premie.weg";
        }
        Premie x = POOL.get(ids.get(slot));
        int[] c = getallen(t, "geclaimd");
        if (x == null || c[slot] != 0) {
            return "vadsrijk.z3.systemen.premie.al";
        }
        if (getallen(t, "voortgang")[slot] < x.aantal()) {
            return "vadsrijk.z3.systemen.premie.nog_niet";
        }
        c[slot] = 1;
        zet(t, "geclaimd", c);
        int lv = Spelers.level(p);
        Spelers.geefXp(p, Z3Xp.premie(lv), "premie");
        Spelers.geefMunten(p, 150 + 8L * lv);
        Z3Data.geefPenningen(p, x.baas() ? PENNINGEN_BAAS : PENNINGEN);
        int week = t.getIntOr("weekN", 0) + 1;
        t.putInt("weekN", week);
        if (week >= WEEKDOEL && !t.getBooleanOr("weekKist", false)) {
            t.putBoolean("weekKist", true);
            EcoUtil.geefOfBewaar(p, new ItemStack(Z3Spul.PREMIEKIST.get()));
            p.sendSystemMessage(Component.translatable("vadsrijk.z3.systemen.premie.weekkist").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.3f);
        Spelers.sync(p);
        return "vadsrijk.z3.systemen.premie.geclaimd";
    }

    /** The free daily swap: replaces an unclaimed bounty with another one (once a day). */
    public static String ruil(ServerPlayer p, int slot) {
        CompoundTag t = vandaag(p);
        List<String> ids = new ArrayList<>(ids(p));
        if (t.getBooleanOr("geruild", false)) {
            return "vadsrijk.z3.systemen.premie.al_geruild";
        }
        if (slot < 0 || slot >= ids.size() || getallen(t, "geclaimd")[slot] != 0) {
            return "vadsrijk.z3.systemen.premie.niet_ruilen";
        }
        List<Premie> k = keuze(Spelers.level(p));
        k.removeIf(x -> ids.contains(x.id()));
        if (k.isEmpty()) {
            return "vadsrijk.z3.systemen.premie.niet_ruilen";
        }
        ids.set(slot, k.get(p.getRandom().nextInt(k.size())).id());
        int[] v = getallen(t, "voortgang");
        v[slot] = 0;
        zet(t, "voortgang", v);
        t.putString("premies", String.join(",", ids));
        t.putBoolean("geruild", true);
        return "vadsrijk.z3.systemen.premie.geruild";
    }

    /** The weekly chest: an Episch (70 %) or Legendarisch zone-3 weapon at your level, 1 Gouden Veer and a geslepen crystal. */
    public static void openKist(ServerPlayer p) {
        RandomSource r = p.getRandom();
        String w = Z3Wapens.ALLE.get(r.nextInt(Z3Wapens.ALLE.size()));
        var bt = BasisTypes.van(w);
        if (bt != null) {
            EcoUtil.geefOfBewaar(p, Rollen.stack(Rollen.rol(bt, Math.max(30, Spelers.level(p)), r.nextDouble() < 0.7 ? Zeldzaamheid.EPISCH : Zeldzaamheid.LEGENDARISCH, r)));
        }
        EcoUtil.geefOfBewaar(p, Kristallen.stack(Z3Kristallen.SOORTEN.get(r.nextInt(Z3Kristallen.SOORTEN.size())), 1, 1));
        GoudenVeren.geef(p, 1);
        p.level().playSound(null, p.blockPosition(), SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 1f, 1.2f);
    }

    // ---- the penningwinkel ---------------------------------------------------------------------------------------------------

    public record Waar(String id, int prijs, Supplier<ItemStack> item) {
        public String naamKey() {
            return "vadsrijk.z3.systemen.winkel." + id;
        }
    }

    public static final List<Waar> WINKEL = new java.util.concurrent.CopyOnWriteArrayList<>();

    static {
        WINKEL.add(new Waar("vahoegzegel", Vahoeg.ZEGEL_PRIJS, () -> new ItemStack(Z3Spul.VAHOEGZEGEL.get())));
        for (String s : Z3Kristallen.SOORTEN) {
            WINKEL.add(new Waar("kristal_" + s, 12, () -> Kristallen.stack(s, 0, 1)));
        }
        WINKEL.add(new Waar("kristal_geslepen", 35, () -> Kristallen.stack(Z3Kristallen.SOORTEN.get((int) (Math.random() * 4)), 1, 1)));
        WINKEL.add(new Waar("herrolknabbel", 15, () -> new ItemStack(nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul.HERROLKNABBEL.get())));
    }

    /** Another slice adds a ware (e.g. z3-wezens: the Premiejagerset pieces, a mount cosmetic). */
    public static void voegWaarToe(Waar w) {
        WINKEL.removeIf(x -> x.id().equals(w.id()));
        WINKEL.add(w);
    }

    public static String koop(ServerPlayer p, String id) {
        for (Waar w : WINKEL) {
            if (w.id().equals(id)) {
                if (!Z3Data.neemPenningen(p, w.prijs())) {
                    return "vadsrijk.z3.systemen.winkel.te_weinig";
                }
                EcoUtil.geefOfBewaar(p, w.item().get());
                p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_YES, SoundSource.PLAYERS, 0.6f, 1.4f);
                return "vadsrijk.z3.systemen.winkel.gekocht";
            }
        }
        return "vadsrijk.z3.systemen.premie.weg";
    }
}
