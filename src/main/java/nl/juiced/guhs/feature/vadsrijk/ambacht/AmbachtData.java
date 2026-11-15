package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;

/**
 * Per-player Ambachten state in {@code SpelerData.extra("ambacht")} (saved with the player; when t-karakter makes the extra tags
 * per character, this follows automatically: nothing here keys on the UUID). Layout:
 * <pre>
 *   xp {ambacht: long}        uitgerust long (s of double xp)   laatst long (epoch ms of the last logout)
 *   buff {id, tot, vads}      gezellig long (ms)                 korven {spot: {sinds, laatst}}
 *   plekken [spot ids found]  vissen [species]  honing [kinds]  gemaakt [recipe ids]   verhaal {ambacht: step}
 *   bestel {...} (Bestelbord) oogsten long (total gathers)
 * </pre>
 * Times are real time ({@link #KLOK}, tests swap it).
 */
public final class AmbachtData {
    public static final String SLICE = "ambacht";
    public static volatile LongSupplier KLOK = System::currentTimeMillis;
    /** Called on every ambacht level-up (player, ambacht, new level). */
    public static final List<LevelOp> LEVEL_OP = new java.util.concurrent.CopyOnWriteArrayList<>();

    @FunctionalInterface
    public interface LevelOp {
        void op(ServerPlayer p, Ambacht a, int level);
    }

    private AmbachtData() {
    }

    /** The data version: 2 = the 4 ambachten of 1.10.4 (no "versie" = the 9 of 1.10.0-1.10.3, migrated once by {@link #migreer}). */
    public static final int VERSIE = 2;

    public static CompoundTag tag(Player p) {
        CompoundTag t = Spelers.data(p).extra(SLICE);
        if (t.getIntOr("versie", 0) < VERSIE) {
            migreer(t);
        }
        return t;
    }

    // ---- 1.10.4: 9 -> 4 (once, on the first read) ------------------------------------------------------------------------------

    /**
     * Migrates one player's slice from the 9 ambachten to the 4 (idempotent; sets {@code versie}):
     * <ul>
     *   <li>xp: per new ambacht the HIGHEST of its old ones (most xp = highest level; that xp total is kept, so on the 3x faster
     *   curve it is a higher level now);</li>
     *   <li>story step: the furthest of the merged chains (capped at what exists);</li>
     *   <li>mastery moments ("meester" {@code old@cap}) under the new id (no second reward);</li>
     *   <li>today's Bestelbord orders are drawn again (their ids named old ambachten); week count, hives, found spots, fish, honey
     *   and made recipes stay as they are.</li>
     * </ul>
     * The chosen title is migrated at login ({@link #migreerTitel}).
     */
    public static void migreer(CompoundTag t) {
        CompoundTag oudXp = t.getCompoundOrEmpty("xp"), oudVerhaal = t.getCompoundOrEmpty("verhaal");
        CompoundTag xp = new CompoundTag(), verhaal = new CompoundTag();
        for (Ambacht a : Ambacht.values()) {
            long beste = oudXp.getLongOr(a.id, 0L);
            int stap = oudVerhaal.getIntOr(a.id, 0);
            for (String o : a.oud) {
                beste = Math.max(beste, oudXp.getLongOr(o, 0L));
                stap = Math.max(stap, oudVerhaal.getIntOr(o, 0));
            }
            if (beste > 0) {
                xp.putLong(a.id, beste);
            }
            if (stap > 0) {
                verhaal.putInt(a.id, Math.min(stap, AmbachtNpcs.VERHAAL_LEVELS.length + HoogvadsGilde.LEVELS.length));
            }
        }
        t.put("xp", xp);
        t.put("verhaal", verhaal);
        ListTag meester = t.getListOrEmpty("meester");
        if (!meester.isEmpty()) {
            ListTag nieuw = new ListTag();
            java.util.Set<String> gezien = new java.util.LinkedHashSet<>();
            for (int i = 0; i < meester.size(); i++) {
                String s = meester.getString(i).orElse("");
                int at = s.indexOf('@');
                Ambacht a = Ambacht.van(at < 0 ? s : s.substring(0, at));
                if (a != null && gezien.add(a.id + (at < 0 ? "" : s.substring(at)))) {
                    nieuw.add(StringTag.valueOf(a.id + (at < 0 ? "" : s.substring(at))));
                }
            }
            t.put("meester", nieuw);
        }
        if (t.contains("bestel")) {
            CompoundTag b = t.getCompoundOrEmpty("bestel").copy();
            b.remove("dag");
            t.put("bestel", b);
        }
        t.putInt("versie", VERSIE);
    }

    /**
     * At login: a chosen ambacht title of the 9 ({@code vr_amb_mijnen_leerling}) becomes the one of its new ambacht
     * ({@code vr_amb_verzamelen_leerling}); the "already announced" list gets the new ids too (no "new title!" for what you had).
     */
    public static void migreerTitel(ServerPlayer p) {
        CompoundTag saved = nl.juiced.guhs.quest.GuhQuests.saved(p);
        String keuze = saved.getStringOr(nl.juiced.guhs.feature.titels.Titels.KEUZE, "");
        String nieuw = titelNaarNieuw(keuze);
        if (!nieuw.equals(keuze)) {
            saved.putString(nl.juiced.guhs.feature.titels.Titels.KEUZE, nieuw);
        }
        String bekend = saved.getStringOr(nl.juiced.guhs.feature.titels.Titels.BEKEND, "");
        if (!bekend.isEmpty()) {
            java.util.Set<String> s = new java.util.LinkedHashSet<>(java.util.Arrays.asList(bekend.split(",")));
            for (String id : List.copyOf(s)) {
                s.add(titelNaarNieuw(id));
            }
            String uit = String.join(",", s);
            if (!uit.equals(bekend)) {
                saved.putString(nl.juiced.guhs.feature.titels.Titels.BEKEND, uit);
            }
        }
    }

    /** {@code vr_amb_<old>_<kind>} -> {@code vr_amb_<new>_<kind>}; anything else unchanged. */
    public static String titelNaarNieuw(String id) {
        if (!id.startsWith("vr_amb_")) {
            return id;
        }
        String rest = id.substring("vr_amb_".length());
        int u = rest.lastIndexOf('_');
        if (u <= 0) {
            return id;
        }
        Ambacht a = Ambacht.van(rest.substring(0, u));
        return a == null ? id : "vr_amb_" + a.id + rest.substring(u);
    }

    public static long nu() {
        return KLOK.getAsLong();
    }

    // ---- xp / levels -------------------------------------------------------------------------------------------------------

    public static long xp(Player p, Ambacht a) {
        return tag(p).getCompoundOrEmpty("xp").getLongOr(a.id, 0L);
    }

    public static int level(Player p, Ambacht a) {
        return AmbachtXp.levelVan(xp(p, a));
    }

    public static void zetXp(Player p, Ambacht a, long xp) {
        CompoundTag t = tag(p);
        CompoundTag x = t.getCompoundOrEmpty("xp").copy();
        x.putLong(a.id, Math.max(0, Math.min(AmbachtXp.totaalVoor(AmbachtXp.CAP), xp)));
        t.put("xp", x);
    }

    /** Dev/tests: set a level exactly. */
    public static void zetLevel(Player p, Ambacht a, int level) {
        zetXp(p, a, AmbachtXp.totaalVoor(Math.max(1, Math.min(AmbachtXp.CAP, level))));
    }

    public static int totaalLevel(Player p) {
        int n = 0;
        for (Ambacht a : Ambacht.values()) {
            n += level(p, a);
        }
        return n;
    }

    /**
     * Ambacht xp from an action: doubled while Uitgerust (uses {@link AmbachtXp#UITGERUST_PER_ACTIE} s), times the food buff's xp
     * factor; plus a little generic xp. Shows "+35 Mijnen-xp" over the hotbar and handles the level-up. Returns the xp given.
     */
    public static long geefXp(ServerPlayer p, Ambacht a, long n, boolean actie) {
        if (n <= 0) {
            return 0;
        }
        boolean rust = false;
        CompoundTag t = tag(p);
        if (actie) {
            long u = t.getLongOr("uitgerust", 0L);
            if (u > 0) {
                rust = true;
                n *= 2;
                t.putLong("uitgerust", Math.max(0, u - AmbachtXp.UITGERUST_PER_ACTIE));
            }
            n = Math.round(n * Eten.xpFactor(p));
        }
        int voor = level(p, a);
        zetXp(p, a, xp(p, a) + n);
        int na = level(p, a);
        if (actie) {
            nl.juiced.guhs.feature.vadsrijk.kern.Spelers.geefXp(p, AmbachtXp.generiek(p), "ambacht", null);
        }
        p.sendOverlayMessage(Component.translatable(rust ? "vadsrijk.ambacht.xp_rust" : "vadsrijk.ambacht.xp", n, Component.translatable(a.naamKey()))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        if (na > voor) {
            p.sendSystemMessage(Component.translatable("vadsrijk.ambacht.level_op", Component.translatable(a.naamKey()), na).withStyle(ChatFormatting.GOLD));
            p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.3f);
            for (LevelOp l : LEVEL_OP) {
                l.op(p, a, na);
            }
        }
        Ranglijst.bij(p);
        return n;
    }

    // ---- rested ------------------------------------------------------------------------------------------------------------

    public static long uitgerust(Player p) {
        return tag(p).getLongOr("uitgerust", 0L);
    }

    public static void zetUitgerust(Player p, long s) {
        tag(p).putLong("uitgerust", Math.max(0, Math.min(AmbachtXp.UITGERUST_MAX, s)));
    }

    /** At login: the offline time since the last logout becomes rest (1 s per {@link AmbachtXp#OFFLINE_FACTOR} s). */
    public static void ingelogd(ServerPlayer p) {
        CompoundTag t = tag(p);
        long laatst = t.getLongOr("laatst", 0L);
        if (laatst > 0) {
            long weg = Math.max(0, (nu() - laatst) / 1000);
            zetUitgerust(p, uitgerust(p) + weg / AmbachtXp.OFFLINE_FACTOR);
        }
        t.putLong("laatst", 0L);
    }

    public static void uitgelogd(ServerPlayer p) {
        tag(p).putLong("laatst", nu());
    }

    // ---- small sets (found spots, fish, honey kinds, recipes made) ------------------------------------------------------------

    public static boolean heeft(Player p, String lijst, String id) {
        ListTag l = tag(p).getListOrEmpty(lijst);
        for (int i = 0; i < l.size(); i++) {
            if (l.getString(i).orElse("").equals(id)) {
                return true;
            }
        }
        return false;
    }

    /** Adds to a set; true when new. */
    public static boolean voegToe(Player p, String lijst, String id) {
        if (heeft(p, lijst, id)) {
            return false;
        }
        CompoundTag t = tag(p);
        ListTag l = t.getListOrEmpty(lijst).copy();
        l.add(StringTag.valueOf(id));
        t.put(lijst, l);
        return true;
    }

    public static List<String> lijst(Player p, String lijst) {
        ListTag l = tag(p).getListOrEmpty(lijst);
        List<String> uit = new ArrayList<>();
        for (int i = 0; i < l.size(); i++) {
            l.getString(i).ifPresent(uit::add);
        }
        return uit;
    }

    // ---- story steps ---------------------------------------------------------------------------------------------------------

    public static int verhaal(Player p, Ambacht a) {
        return tag(p).getCompoundOrEmpty("verhaal").getIntOr(a.id, 0);
    }

    public static void zetVerhaal(Player p, Ambacht a, int stap) {
        CompoundTag t = tag(p);
        CompoundTag v = t.getCompoundOrEmpty("verhaal").copy();
        v.putInt(a.id, stap);
        t.put("verhaal", v);
    }

    /** Dev: forget everything. */
    public static void reset(Player p) {
        CompoundTag t = tag(p);
        for (String k : List.copyOf(t.keySet())) {
            t.remove(k);
        }
        t.putInt("versie", VERSIE);
    }
}
