package nl.juiced.guhs.feature.vadsrijk.gilden;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import nl.juiced.guhs.feature.vadsrijk.ambacht.Ambacht;
import nl.juiced.guhs.feature.vadsrijk.ambacht.AmbachtData;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;

/**
 * U-GILDEN: bond xp, bond levels 1-10, the convenience perks and the weekly tasks (FEEDBACK_RONDE4 V1: "guild-xp uit wat leden
 * toch al doen, guild-levels met gemaksperks, wekelijkse opdrachten; GEEN grote stats").
 * <ul>
 *   <li>Sources (hooked in {@link BondRegistry}): a mob cheered up (+1, elite +3), a boss (+40 + a trophy), a quest done (+15), a
 *   level-up (+10), an ambacht level-up (+10) and ambacht xp (1 bond xp per {@link #AMBACHT_PER} ambacht xp, polled).
 *   Per member at most {@link #DAG_CAP} bond xp a day (the week tasks themselves are not capped).</li>
 *   <li>Perks: {@link #bankTabs}, {@link #reisKorting} (the luxury travel-map flight), {@link #tpMinuten} (castle teleport
 *   cooldown) and Bondsrust ({@link #rustFactor}: offline time fills a pool that gives +50 % fighting xp until empty).</li>
 *   <li>Week tasks: 3 a week (reset Monday 06:00 Europe/Amsterdam), picked per bond from {@link #TAKEN}, targets scale with the
 *   member count; each done: +{@link #TAAK_XP} bond xp and {@link #TAAK_KAS} brons in the kas.</li>
 * </ul>
 */
public final class BondXp {
    /** Total bond xp needed for level 1..10. */
    public static final long[] DREMPELS = {0, 1000, 3000, 6000, 10000, 16000, 24000, 35000, 50000, 70000};
    public static final int MAX = DREMPELS.length;
    public static final long DAG_CAP = 600, AMBACHT_PER = 50, TAAK_XP = 400, TAAK_KAS = 1000, WEEK_BONUS = 500;
    public static volatile LongSupplier KLOK = System::currentTimeMillis;
    private static final ZoneId AMS = ZoneId.of("Europe/Amsterdam");

    /** A week task kind: id, base target, extra per member above 3, cap. */
    public record Taak(String id, long basis, long perLid, long cap) {
        public long doel(int leden) {
            return Math.min(cap, basis + perLid * Math.max(0, leden - 3));
        }
    }

    public static final List<Taak> TAKEN = List.of(
            new Taak("opgevrolijkt", 300, 60, 1500),
            new Taak("bazen", 5, 1, 20),
            new Taak("quests", 15, 3, 60),
            new Taak("ambacht", 5000, 1000, 25000),
            new Taak("levels", 10, 2, 40));

    private BondXp() {
    }

    public static int levelVan(long xp) {
        int l = 1;
        for (int i = 1; i < DREMPELS.length; i++) {
            if (xp >= DREMPELS[i]) {
                l = i + 1;
            }
        }
        return l;
    }

    /** xp still needed for the next level (0 at max). */
    public static long nogNodig(long xp) {
        int l = levelVan(xp);
        return l >= MAX ? 0 : DREMPELS[l] - xp;
    }

    // ---- perks ---------------------------------------------------------------------------------------------------------------

    public static int bankTabs(int level) {
        return 1 + (level >= 3 ? 1 : 0) + (level >= 5 ? 1 : 0) + (level >= 7 ? 1 : 0) + (level >= 9 ? 1 : 0);
    }

    /** Discount on the luxury travel-map flight (0..0.15). */
    public static double reisKorting(int level) {
        return level >= 8 ? 0.15 : level >= 6 ? 0.10 : level >= 4 ? 0.05 : 0;
    }

    /** Minutes between two "to our castle" teleports from a hub. */
    public static int tpMinuten(int level) {
        return level >= 10 ? 5 : level >= 7 ? 10 : level >= 5 ? 15 : 30;
    }

    /** Bondsrust: the pool fills with this share of a level's xp per offline hour (0 = no perk yet), capped by {@link #rustMax}. */
    public static double rustFactor(int level) {
        return level >= 6 ? 0.04 : level >= 2 ? 0.02 : 0;
    }

    public static double rustMax(int level) {
        return level >= 6 ? 1.0 : 0.5;
    }

    /** The perk lines for the panel: lang key + args, and from which level. */
    public record Perk(int level, String key, Object... args) {
    }

    public static List<Perk> perks() {
        return List.of(new Perk(1, "vadsrijk.gilden.perk.basis"), new Perk(2, "vadsrijk.gilden.perk.rust", 50),
                new Perk(3, "vadsrijk.gilden.perk.bank", 2), new Perk(4, "vadsrijk.gilden.perk.reis", 5), new Perk(5, "vadsrijk.gilden.perk.tp", 15),
                new Perk(5, "vadsrijk.gilden.perk.bank", 3), new Perk(6, "vadsrijk.gilden.perk.rust2"), new Perk(6, "vadsrijk.gilden.perk.reis", 10),
                new Perk(7, "vadsrijk.gilden.perk.tp", 10), new Perk(7, "vadsrijk.gilden.perk.bank", 4), new Perk(8, "vadsrijk.gilden.perk.reis", 15),
                new Perk(9, "vadsrijk.gilden.perk.bank", 5), new Perk(10, "vadsrijk.gilden.perk.tp", 5), new Perk(10, "vadsrijk.gilden.perk.goud"));
    }

    // ---- giving xp -----------------------------------------------------------------------------------------------------------

    static long dag(long ms) {
        return Math.floorDiv(ms + 3_600_000L * 18, 86_400_000L);   // (days start at 06:00, close enough to Amsterdam)
    }

    /** Gives bond xp for something this member did (daily cap per member). Returns what was given. */
    public static long geef(MinecraftServer s, Bond b, String sleutel, long n, String bron) {
        Bond.Lid l = b.lid(sleutel);
        if (l == null || n <= 0) {
            return 0;
        }
        long nu = KLOK.getAsLong(), d = dag(nu);
        if (l.dag != d) {
            l.dag = d;
            l.dagXp = 0;
        }
        long mag = Math.max(0, Math.min(n, DAG_CAP - l.dagXp));
        if (mag <= 0) {
            return 0;
        }
        l.dagXp += mag;
        l.bijdrage += mag;
        if (l.rang == Rang.REKRUUT && l.bijdrage >= BondActies.AUTO_LID) {
            l.rang = Rang.LID;
            BondChat.meld(s, b, Component.translatable("vadsrijk.gilden.auto_lid", l.naam).withStyle(ChatFormatting.AQUA));
        }
        bijXp(s, b, mag);
        return mag;
    }

    /** Raw bond xp (week tasks, dev): no cap. Announces a level-up to the online members. */
    public static void bijXp(MinecraftServer s, Bond b, long n) {
        int voor = b.level();
        b.xp = Math.max(0, b.xp + n);
        int na = b.level();
        Bonden.get(s).setDirty();
        if (na > voor) {
            BondChat.meld(s, b, Component.translatable("vadsrijk.gilden.level_op", b.naam, na).withStyle(ChatFormatting.GOLD));
            for (ServerPlayer p : BondChat.online(s, b)) {
                p.level().playSound(null, p.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 0.7f, 1.2f);
            }
            BondTags.ververs(s);
        }
    }

    /** A player did something countable: bond xp + week-task progress. */
    public static void deed(ServerPlayer p, String taak, long aantal, long xp) {
        MinecraftServer s = p.level().getServer();
        if (s == null) {
            return;
        }
        Bond b = Bonden.van(p);
        if (b == null) {
            return;
        }
        Bond.Lid l = b.lid(Bonden.sleutel(p));
        if (l != null) {
            l.gezien = KLOK.getAsLong();
        }
        if (xp > 0) {
            geef(s, b, Bonden.sleutel(p), xp, taak);
        }
        if (taak != null && aantal > 0) {
            voortgang(s, b, taak, aantal);
        }
    }

    // ---- week tasks ----------------------------------------------------------------------------------------------------------

    /** The week number: weeks since the epoch, each from Monday 06:00 Europe/Amsterdam. */
    public static long weekId(long ms) {
        ZonedDateTime z = Instant.ofEpochMilli(ms).atZone(AMS);
        ZonedDateTime start = z.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).truncatedTo(ChronoUnit.DAYS).plusHours(6);
        if (z.isBefore(start)) {
            start = start.minusWeeks(1);
        }
        return Math.floorDiv(start.toEpochSecond() + 4 * 86400L, 7 * 86400L);
    }

    /** Makes sure the bond has this week's 3 tasks (new week: new tasks, progress 0). */
    public static void week(Bond b) {
        long w = weekId(KLOK.getAsLong());
        if (b.week == w && b.taken.size() == 3) {
            return;
        }
        b.week = w;
        b.taken.clear();
        List<Taak> pool = new ArrayList<>(TAKEN);
        long seed = w * 31 + b.id * 7919L;
        for (int i = 0; i < 3; i++) {
            int k = (int) Math.floorMod(seed + i * 104729L, (long) pool.size());
            b.taken.add(pool.remove(k).id());
        }
        for (int i = 0; i < 3; i++) {
            b.voortgang[i] = 0;
            b.taakKlaar[i] = false;
        }
    }

    @Nullable
    public static Taak taak(String id) {
        for (Taak t : TAKEN) {
            if (t.id().equals(id)) {
                return t;
            }
        }
        return null;
    }

    static void voortgang(MinecraftServer s, Bond b, String taak, long n) {
        week(b);
        for (int i = 0; i < b.taken.size(); i++) {
            if (!b.taken.get(i).equals(taak) || b.taakKlaar[i]) {
                continue;
            }
            Taak t = taak(taak);
            long doel = t == null ? Long.MAX_VALUE : t.doel(b.leden.size());
            b.voortgang[i] = Math.min(doel, b.voortgang[i] + n);
            if (b.voortgang[i] >= doel) {
                b.taakKlaar[i] = true;
                b.kas += TAAK_KAS;
                BondChat.meld(s, b, Component.translatable("vadsrijk.gilden.taak.klaar", Component.translatable("vadsrijk.gilden.taak." + taak),
                        TAAK_XP, nl.juiced.guhs.feature.vadsrijk.kern.Spelers.munten(TAAK_KAS)).withStyle(ChatFormatting.GOLD));
                bijXp(s, b, TAAK_XP);
                if (b.taakKlaar[0] && b.taakKlaar[1] && b.taakKlaar[2]) {
                    BondChat.meld(s, b, Component.translatable("vadsrijk.gilden.taak.alle", WEEK_BONUS).withStyle(ChatFormatting.GOLD));
                    bijXp(s, b, WEEK_BONUS);
                }
            }
        }
        Bonden.get(s).setDirty();
    }

    // ---- Bondsrust (rest xp) + ambacht polling ---------------------------------------------------------------------------------

    /** Per character (extra "gilden"): rust pool (xp), last logout (ms). */
    static CompoundTag tag(ServerPlayer p) {
        return Spelers.data(p).extra(BondRegistry.SLICE);
    }

    public static long rust(ServerPlayer p) {
        return tag(p).getLongOr("rust", 0L);
    }

    /** At login (and after a character switch): offline hours fill the pool, if the bond has the perk. */
    static void ingelogd(ServerPlayer p) {
        CompoundTag t = tag(p);
        long uit = t.getLongOr("uit", 0L);
        t.putLong("uit", 0L);
        LAATST.remove(p.getUUID());
        Bond b = Bonden.van(p);
        if (b == null || uit <= 0) {
            return;
        }
        double f = rustFactor(b.level());
        if (f <= 0) {
            return;
        }
        double uren = Math.min(72, (KLOK.getAsLong() - uit) / 3_600_000.0);
        SpelerData d = Spelers.data(p);
        long perLevel = Levels.xpVoorGeneriek(Math.max(1, d.level()));
        long max = Math.round(perLevel * rustMax(b.level()));
        long nieuw = Math.min(max, rust(p) + Math.round(perLevel * f * uren));
        if (nieuw > rust(p)) {
            t.putLong("rust", nieuw);
            Spelers.opgeslagen(p);
            p.sendSystemMessage(Component.translatable("vadsrijk.gilden.rust.vol", nieuw).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    static void uitgelogd(ServerPlayer p) {
        tag(p).putLong("uit", KLOK.getAsLong());
    }

    /** XP_FACTOR: +50 % while the pool has something. */
    static double factor(ServerPlayer p) {
        return rust(p) > 0 ? 1.5 : 1;
    }

    private static final Map<UUID, long[]> LAATST = new ConcurrentHashMap<>();

    /** Every second: what the rest bonus gave is taken from the pool; ambacht xp gained becomes bond xp (accumulated). */
    static void tik(ServerPlayer p) {
        SpelerData d = Spelers.data(p);
        long amb = 0;
        for (Ambacht a : Ambacht.values()) {
            amb += AmbachtData.xp(p, a);
        }
        long[] l = LAATST.get(p.getUUID());
        if (l == null || l[2] != nl.juiced.guhs.feature.vadsrijk.karakter.Karakter.slot(p)) {
            LAATST.put(p.getUUID(), new long[] {d.generiekXp, amb, nl.juiced.guhs.feature.vadsrijk.karakter.Karakter.slot(p), 0});
            return;
        }
        long dg = d.generiekXp - l[0], da = amb - l[1];
        l[0] = d.generiekXp;
        l[1] = amb;
        if (dg > 0 && rust(p) > 0) {
            long bonus = Math.max(1, dg / 3);   // (x1.5: a third of what came in was the bonus)
            tag(p).putLong("rust", Math.max(0, rust(p) - bonus));
        }
        if (da > 0) {
            l[3] += da;
            long xp = l[3] / AMBACHT_PER;
            if (xp > 0) {
                l[3] -= xp * AMBACHT_PER;
            }
            deed(p, "ambacht", da, xp);
        }
    }

    static void vergeet(UUID id) {
        LAATST.remove(id);
    }
}
