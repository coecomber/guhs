package nl.juiced.guhs.feature.guhwinter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import nl.juiced.guhs.feature.guhwinter.kern.KernRegistry;
import nl.juiced.guhs.feature.guhwinter.kern.Momenten;

/**
 * 1.10.10 "Guhwinter": the facade of the event (kern). Everything time-related goes through here, so the dev override
 * ({@code /guhs winter datum <yyyy-MM-dd HH:mm> | echt}) moves the whole event at once.
 * <ul>
 *   <li>{@link #nu}, {@link #fase}: the time in Europe/Amsterdam (real, or the override) and the {@link GuhwinterFase};</li>
 *   <li>the moments ({@link #START} ... {@link #EINDE}) and helpers for the lootjes, kerstboom and album windows;</li>
 *   <li>{@link #opMoment}: one-shot persistent listeners (also fire after downtime);</li>
 *   <li>{@link #bijEinde}: what happens at AFGELOPEN (plein registers its empty frame there);</li>
 *   <li>the pepernoot currency ({@link #PEPERNOOT}, {@link #geefPepernoten}, {@link #telPepernoten}, {@link #neemPepernoten});</li>
 *   <li>{@link #hoeKomIkDaar}: the clickable chat hint to the Winterplein.</li>
 * </ul>
 * The plaza and its protection: {@link GuhwinterZone}; saved state: {@link GuhwinterData}; commands: {@link GuhwinterCommando}.
 */
public final class Guhwinter {
    public static final ZoneId ZONE = ZoneId.of("Europe/Amsterdam");

    /** Before this the phase is VOOR (which counts as Sint). The planned release. */
    public static final ZonedDateTime START = op(2026, 11, 24, 0, 0);
    /** Lootjes: signing up is open until 1 Dec 23:59 (so: before this). */
    public static final ZonedDateTime INSCHRIJVEN_TOT = op(2026, 12, 2, 0, 0);
    /** Lootjes: the Sint draws at 2 Dec 00:00; the building plots open. */
    public static final ZonedDateTime TREKKING = op(2026, 12, 2, 0, 0);
    /** Lootjes: building and drawing until 5 Dec 18:00 (before this). */
    public static final ZonedDateTime BOUWEN_TOT = op(2026, 12, 5, 18, 0);
    /** Pakjesavond: 5 Dec 19:00 (phase PAKJESAVOND, the gifts are unwrapped). */
    public static final ZonedDateTime PAKJESAVOND = op(2026, 12, 5, 19, 0);
    /** Kerst: 6 Dec 00:00. */
    public static final ZonedDateTime KERST = op(2026, 12, 6, 0, 0);
    /** Kerstboom drawings can be handed in until 25 Dec 23:59 (before this). */
    public static final ZonedDateTime KERSTBOOM_TOT = op(2026, 12, 26, 0, 0);
    /** Oud & Nieuw: 26 Dec 00:00, also the Kerstalbum (26 Dec - 2 Jan). */
    public static final ZonedDateTime OUDJAAR = op(2026, 12, 26, 0, 0);
    public static final ZonedDateTime ALBUM_VAN = OUDJAAR;
    /** The end: 3 Jan 2027 00:00 (AFGELOPEN; the album closes, things melt, the plaza becomes the empty frame). */
    public static final ZonedDateTime EINDE = op(2027, 1, 3, 0, 0);
    public static final ZonedDateTime ALBUM_TOT = EINDE;

    /**
     * The named moments for {@link #opMoment(String, Consumer)}, in time order: an id {@code "<moment>"} or
     * {@code "<moment>:<who>"} fires at that moment.
     */
    public static final Map<String, ZonedDateTime> MOMENTEN;

    static {
        Map<String, ZonedDateTime> m = new LinkedHashMap<>();
        m.put("start", START);
        m.put("inschrijven_dicht", INSCHRIJVEN_TOT);
        m.put("trekking", TREKKING);
        m.put("bouwen_dicht", BOUWEN_TOT);
        m.put("pakjesavond", PAKJESAVOND);
        m.put("kerst", KERST);
        m.put("kerstboom_dicht", KERSTBOOM_TOT);
        m.put("album", ALBUM_VAN);
        m.put("oudjaar", OUDJAAR);
        m.put("afgelopen", EINDE);
        MOMENTEN = Collections.unmodifiableMap(m);
    }

    /** The currency: edible, stays in your pockets after the event. */
    public static final DeferredItem<Item> PEPERNOOT = KernRegistry.PEPERNOOT;

    /** {@code yyyy-MM-dd HH:mm}, the way the dev command writes and reads a date. */
    public static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Momenten MOMENT_LUISTERAARS = new Momenten();
    private static final List<Consumer<ServerLevel>> EINDE_HAKEN = new CopyOnWriteArrayList<>();

    private Guhwinter() {
    }

    private static ZonedDateTime op(int jaar, int maand, int dag, int uur, int minuut) {
        return ZonedDateTime.of(jaar, maand, dag, uur, minuut, 0, 0, ZONE);
    }

    // --- time ------------------------------------------------------------------------------------------------------------

    /** Now in ms since the epoch: the real clock plus the dev override. */
    public static long nuMs(MinecraftServer server) {
        Long v = GuhwinterData.get(server).verschuiving;
        return System.currentTimeMillis() + (v == null ? 0 : v);
    }

    /** Now in Europe/Amsterdam, honouring the dev override. */
    public static ZonedDateTime nu(MinecraftServer server) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(nuMs(server)), ZONE);
    }

    /** The phase now (honouring the dev override). */
    public static GuhwinterFase fase(MinecraftServer server) {
        return GuhwinterFase.op(nu(server));
    }

    /** Is the event on (VOOR, SINT, PAKJESAVOND, KERST, OUDJAAR)? */
    public static boolean bezig(MinecraftServer server) {
        return fase(server).bezig();
    }

    /** Lootjes: may you still sign up (before 2 Dec 00:00)? */
    public static boolean inschrijvenOpen(MinecraftServer server) {
        return inschrijvenOpen(nu(server));
    }

    public static boolean inschrijvenOpen(ZonedDateTime t) {
        return t.isBefore(INSCHRIJVEN_TOT);
    }

    /** Lootjes: are the building plots open (2 Dec 00:00 until 5 Dec 18:00)? */
    public static boolean bouwenOpen(MinecraftServer server) {
        return bouwenOpen(nu(server));
    }

    public static boolean bouwenOpen(ZonedDateTime t) {
        return !t.isBefore(TREKKING) && t.isBefore(BOUWEN_TOT);
    }

    /** Can a kerstboom drawing be handed in or changed (until 25 Dec 23:59)? */
    public static boolean kerstboomOpen(MinecraftServer server) {
        return kerstboomOpen(nu(server));
    }

    public static boolean kerstboomOpen(ZonedDateTime t) {
        return t.isBefore(KERSTBOOM_TOT);
    }

    /** Is the Kerstalbum there (26 Dec until 2 Jan 23:59)? */
    public static boolean albumOpen(MinecraftServer server) {
        return albumOpen(nu(server));
    }

    public static boolean albumOpen(ZonedDateTime t) {
        return !t.isBefore(ALBUM_VAN) && t.isBefore(ALBUM_TOT);
    }

    // --- the dev override ------------------------------------------------------------------------------------------------

    /** Dev: from now on the event clock reads {@code datum} (Europe/Amsterdam) and keeps running from there (saved). */
    public static void zetDatum(MinecraftServer server, LocalDateTime datum) {
        GuhwinterData d = GuhwinterData.get(server);
        d.verschuiving = datum.atZone(ZONE).toInstant().toEpochMilli() - System.currentTimeMillis();
        d.setDirty();
    }

    /** Dev: back to the real date. */
    public static void zetEcht(MinecraftServer server) {
        GuhwinterData d = GuhwinterData.get(server);
        d.verschuiving = null;
        d.setDirty();
    }

    /** Is the dev override on? */
    public static boolean nepDatum(MinecraftServer server) {
        return GuhwinterData.get(server).verschuiving != null;
    }

    /** {@code yyyy-MM-dd HH:mm} or {@code yyyy-MM-dd} (00:00); null when it isn't a date. */
    @Nullable
    public static LocalDateTime leesDatum(String tekst) {
        String s = tekst.strip().replace('T', ' ');
        try {
            return LocalDateTime.parse(s.length() <= 10 ? s + " 00:00" : s, DATUM);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    // --- moments ---------------------------------------------------------------------------------------------------------

    /**
     * Calls {@code actie} once, persistently, when the named moment has passed: {@code id} is {@code "<moment>"} or
     * {@code "<moment>:<who>"} with {@code <moment>} one of {@link #MOMENTEN} (e.g. {@code "pakjesavond:lootjes"}). The id
     * is what the world remembers, so keep it unique and never change it. When the server was down at that moment it
     * fires at the next start, in time order. Register from your Feature.register (mod construction).
     */
    public static void opMoment(String id, Consumer<MinecraftServer> actie) {
        String moment = id.indexOf(':') < 0 ? id : id.substring(0, id.indexOf(':'));
        ZonedDateTime t = MOMENTEN.get(moment);
        if (t == null) {
            throw new IllegalArgumentException("Guhwinter: unknown moment '" + moment + "' (one of " + MOMENTEN.keySet() + ")");
        }
        opMoment(id, t, actie);
    }

    /** Like {@link #opMoment(String, Consumer)} at a time of your own. */
    public static void opMoment(String id, ZonedDateTime wanneer, Consumer<MinecraftServer> actie) {
        MOMENT_LUISTERAARS.voegToe(id, wanneer, actie);
    }

    /** (kern: every second and at server start) fires what is due. Returns the ids that fired. */
    public static List<String> vuurMomenten(MinecraftServer server) {
        GuhwinterData d = GuhwinterData.get(server);
        return MOMENT_LUISTERAARS.vuur(server, nuMs(server), d.gevuurd, d::setDirty);
    }

    /** Every registered listener, by time (for /guhs winter status). */
    public static List<Momenten.Luisteraar> momenten() {
        return MOMENT_LUISTERAARS.alle();
    }

    /** Dev: forget that a listener fired (it fires again as soon as its time has passed). False if it hadn't fired. */
    public static boolean vergeetMoment(MinecraftServer server, String id) {
        GuhwinterData d = GuhwinterData.get(server);
        boolean was = d.gevuurd.remove(id);
        d.setDirty();
        return was;
    }

    /**
     * At AFGELOPEN (3 Jan, also when that passed while the server was down) kern calls these with the Guhmension, after
     * its own end work. Plein registers {@code GuhwinterPlein::plaatsRaam} here.
     */
    public static void bijEinde(Consumer<ServerLevel> haak) {
        EINDE_HAKEN.add(haak);
    }

    /** (kern) the end hooks. */
    public static List<Consumer<ServerLevel>> eindeHaken() {
        return List.copyOf(EINDE_HAKEN);
    }

    // --- pepernoten ------------------------------------------------------------------------------------------------------

    /** Gives {@code n} pepernoten (what doesn't fit drops at the player's feet). */
    public static void geefPepernoten(ServerPlayer player, int n) {
        int max = PEPERNOOT.get().getDefaultMaxStackSize();
        while (n > 0) {
            int k = Math.min(n, max);
            ItemStack s = new ItemStack(PEPERNOOT.get(), k);
            if (!player.getInventory().add(s)) {
                player.drop(s, false);
            }
            n -= k;
        }
    }

    /** How many pepernoten this player carries. */
    public static int telPepernoten(ServerPlayer player) {
        int n = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(PEPERNOOT.get())) {
                n += s.getCount();
            }
        }
        return n;
    }

    /** Takes {@code n} pepernoten when the player has that many (true), else takes nothing (false). */
    public static boolean neemPepernoten(ServerPlayer player, int n) {
        if (n <= 0) {
            return true;
        }
        if (telPepernoten(player) < n) {
            return false;
        }
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && n > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(PEPERNOOT.get())) {
                int k = Math.min(n, s.getCount());
                s.shrink(k);
                n -= k;
            }
        }
        inv.setChanged();
        return true;
    }

    // --- chat ------------------------------------------------------------------------------------------------------------

    /** "[Hoe kom ik daar?]": click = {@code /guhs winter waar} (puts the Winterplein in your Reisguh list and shows the way). */
    public static MutableComponent hoeKomIkDaar() {
        return Component.translatable("guhwinter.kern.klik").withStyle(s -> s.withColor(ChatFormatting.AQUA).withUnderlined(true)
                .withClickEvent(new ClickEvent.RunCommand("/guhs winter waar"))
                .withHoverEvent(new HoverEvent.ShowText(Component.translatable("guhwinter.kern.klik.hover"))));
    }
}
