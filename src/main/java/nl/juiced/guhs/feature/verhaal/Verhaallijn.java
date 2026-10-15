package nl.juiced.guhs.feature.verhaal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.gids.VerhaalStand;
import nl.juiced.guhs.feature.gids.VerhalenVoortgang;
import nl.juiced.guhs.feature.wereld.Stappen;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (verhaal engine, CONTRACT_130 §6.2.1): a questline with per-player steps. Everything a player did lives in
 * {@code GuhQuests.saved(p)} ({@code guhs_<id>_stap}, {@code _v_<naam>} flags, {@code _t_<naam>} counters,
 * {@code _e_<naam>} once-only marks), so any number of players can do the same questline at the same place. A step is
 * only ever set forward; every step passed grants the hidden advancement {@code guhs:quest/<id>_stap_<i>} (the FTB task
 * of that step) and the client gets the new state ({@link VerhaalSync}).
 * <p>
 * A registered line shows up in the Guhdex tab Verhalen by itself (under the heading of its {@code groep}), on the travel
 * map of its group ({@link Reiskaarten}), and, with {@link Builder#doelregel}, as the objective line on the screen.
 * Register from your Feature.register (common code, both sides):
 * <pre>
 * public static final Verhaallijn LIJN = Verhaallijn.maak("wachter", "barbecue").stappen(4).icoon("guhs:grillspies")
 *         .nodig((p, stap) -&gt; stap == 1 ? List.of(Verhaallijn.nodig("guhs:grillkool", GuhQuests.count(p, KOOL.get()), 4)) : List.of())
 *         .beloningen(p -&gt; List.of(Verhaallijn.beloning("guhs:wachter_lantaarn", LIJN.klaar(p))))
 *         .doel((p, stap) -&gt; Doel.structuur(ModDimensions.BARBECUETHER, "spiesburcht", Component.translatable("structure.guhs.spiesburcht")))
 *         .registreer();
 * </pre>
 * Texts: tools/features/verhaal_motor.py {@code verhaallijn(h, ...)} ({@code gui.guhs.verhalen.<id>.*}).
 */
public final class Verhaallijn implements Stappen {
    /** Told when a player's step changed (always forward). */
    @FunctionalInterface
    public interface StapLuisteraar {
        void stap(ServerPlayer p, int oud, int nieuw);
    }

    private final String id, groep, icoon;
    private final int stappen;
    private final boolean doelregel;
    private final BiFunction<ServerPlayer, Integer, String> sleutel;
    private final List<String> extraSleutels;
    private final BiFunction<ServerPlayer, Integer, List<VerhaalStand.Nodig>> nodig;
    private final Function<ServerPlayer, List<VerhaalStand.Beloning>> beloningen;
    private final BiFunction<ServerPlayer, Integer, Doel> doel;
    @Nullable
    private final String na;
    private final List<StapLuisteraar> luisteraars = new CopyOnWriteArrayList<>();

    private Verhaallijn(Builder b) {
        this.id = b.id;
        this.groep = b.groep;
        this.icoon = b.icoon;
        this.stappen = b.stappen;
        this.doelregel = b.doelregel != null ? b.doelregel : "knabbelring".equals(b.groep);
        this.sleutel = b.sleutel;
        this.extraSleutels = List.copyOf(b.extraSleutels);
        this.nodig = b.nodig;
        this.beloningen = b.beloningen;
        this.doel = b.doel;
        this.na = b.na;
    }

    /** A new questline; groep = the heading of the Verhalen tab ("barbecue", "techniek", "knabbelring", "guhrio"). */
    public static Builder maak(String id, String groep) {
        return new Builder(id, groep);
    }

    public static final class Builder {
        private final String id, groep;
        private int stappen = 1;
        private String icoon = "minecraft:writable_book";
        @Nullable
        private Boolean doelregel;
        private BiFunction<ServerPlayer, Integer, String> sleutel = (p, stap) -> String.valueOf(stap);
        private final List<String> extraSleutels = new ArrayList<>();
        private BiFunction<ServerPlayer, Integer, List<VerhaalStand.Nodig>> nodig = (p, stap) -> List.of();
        private Function<ServerPlayer, List<VerhaalStand.Beloning>> beloningen = p -> List.of();
        private BiFunction<ServerPlayer, Integer, Doel> doel = (p, stap) -> null;
        @Nullable
        private String na;

        private Builder(String id, String groep) {
            this.id = id;
            this.groep = groep;
        }

        /** Steps 0..n-1; stap == n means done. */
        public Builder stappen(int n) {
            this.stappen = Math.max(1, n);
            return this;
        }

        /** The item that draws the line in the Guhdex (an item id). */
        public Builder icoon(String itemId) {
            this.icoon = itemId;
            return this;
        }

        /** Show the on-screen objective line while busy (default: only the group "knabbelring"). */
        public Builder doelregel(boolean aan) {
            this.doelregel = aan;
            return this;
        }

        /** The text variant of a step ("3_bewoner"); default the step number. Name every variant with {@link #extraSleutels}. */
        public Builder sleutel(BiFunction<ServerPlayer, Integer, String> s) {
            this.sleutel = s;
            return this;
        }

        /** Every variant sleutel besides "0".."n-1" and "klaar" (the lang check wants them all). */
        public Builder extraSleutels(String... s) {
            this.extraSleutels.addAll(List.of(s));
            return this;
        }

        /** What the player needs for a step (items with counts), shown in the Guhdex. */
        public Builder nodig(BiFunction<ServerPlayer, Integer, List<VerhaalStand.Nodig>> n) {
            this.nodig = n;
            return this;
        }

        /** The rewards of the line, each ticked when the player has it. */
        public Builder beloningen(Function<ServerPlayer, List<VerhaalStand.Beloning>> b) {
            this.beloningen = b;
            return this;
        }

        /** Where to go for a step ({@link Doelen}: the Superkompas, a companion); may return null. */
        public Builder doel(BiFunction<ServerPlayer, Integer, Doel> d) {
            this.doel = d;
            return this;
        }

        /** Only counts as "next" once that line is done (the order of the travel map; until then its halte shows "???"). */
        public Builder na(String lijnId) {
            this.na = lijnId;
            return this;
        }

        /** Registers the line (from your Feature.register; common code, both sides). */
        public Verhaallijn registreer() {
            Verhaallijn l = new Verhaallijn(this);
            Verhaallijnen.voegToe(l);
            return l;
        }
    }

    // =====================================================================================================================
    // what it is
    // =====================================================================================================================

    public String id() {
        return id;
    }

    public String groep() {
        return groep;
    }

    public int stappen() {
        return stappen;
    }

    public String icoon() {
        return icoon;
    }

    public boolean doelregel() {
        return doelregel;
    }

    /** The line that has to be done first (null: none). */
    @Nullable
    public String na() {
        return na;
    }

    /** Every sleutel besides the step numbers and "klaar". */
    public List<String> extraSleutels() {
        return extraSleutels;
    }

    private String key(String wat) {
        return "guhs_" + id + "_" + wat;
    }

    // =====================================================================================================================
    // a player's progress (server)
    // =====================================================================================================================

    @Override
    public int stap(ServerPlayer p) {
        return Math.max(0, Math.min(stappen, GuhQuests.saved(p).getIntOr(key("stap"), 0)));
    }

    public boolean klaar(ServerPlayer p) {
        return stap(p) >= stappen;
    }

    /** Has the player started (a step done, or {@link #begin} called)? */
    public boolean begonnen(ServerPlayer p) {
        return stap(p) > 0 || GuhQuests.saved(p).getBooleanOr(key("begonnen"), false);
    }

    /** Marks the line as started without a step done (the first talk with its NPC); true the first time. */
    public boolean begin(ServerPlayer p) {
        if (begonnen(p)) {
            return false;
        }
        GuhQuests.saved(p).putBoolean(key("begonnen"), true);
        raak(p);
        VerhaalSync.sync(p);
        return true;
    }

    /** Is this line open for the player (its {@link #na} line done, or none)? */
    public boolean aanDeBeurt(ServerPlayer p) {
        Verhaallijn voor = na == null ? null : Verhaallijnen.van(na);
        return voor == null || voor.klaar(p);
    }

    /**
     * Sets the step, forward only: saves, grants {@code quest/<id>_stap_<i>} for every step passed, syncs and tells the
     * listeners. True = changed.
     */
    public boolean zet(ServerPlayer p, int stap) {
        int oud = stap(p), nieuw = Math.min(stappen, stap);
        if (nieuw <= oud) {
            return false;
        }
        GuhQuests.saved(p).putInt(key("stap"), nieuw);
        for (int i = oud + 1; i <= nieuw; i++) {
            GuhAdvancements.grant(p, id + "_stap_" + i);
        }
        raak(p);
        VerhaalSync.sync(p);
        for (StapLuisteraar l : luisteraars) {
            l.stap(p, oud, nieuw);
        }
        return true;
    }

    /** {@code zet(vanStap + 1)}, only when the player is exactly at vanStap. */
    @Override
    public boolean verder(ServerPlayer p, int vanStap) {
        return stap(p) == vanStap && zet(p, vanStap + 1);
    }

    public void opStap(StapLuisteraar l) {
        luisteraars.add(l);
    }

    public boolean vlag(ServerPlayer p, String naam) {
        return GuhQuests.saved(p).getBooleanOr(key("v_" + naam), false);
    }

    public void vlag(ServerPlayer p, String naam, boolean aan) {
        if (aan) {
            GuhQuests.saved(p).putBoolean(key("v_" + naam), true);
        } else {
            GuhQuests.saved(p).remove(key("v_" + naam));
        }
    }

    public int teller(ServerPlayer p, String naam) {
        return GuhQuests.saved(p).getIntOr(key("t_" + naam), 0);
    }

    public void teller(ServerPlayer p, String naam, int n) {
        GuhQuests.saved(p).putInt(key("t_" + naam), n);
    }

    /** True the first time only (once-per-player rewards). */
    @Override
    public boolean eenmalig(ServerPlayer p, String naam) {
        CompoundTag saved = GuhQuests.saved(p);
        if (saved.getBooleanOr(key("e_" + naam), false)) {
            return false;
        }
        saved.putBoolean(key("e_" + naam), true);
        return true;
    }

    /** (dev command, tests) forgets everything this player did in the line. */
    public void wis(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        String prefix = "guhs_" + id + "_";
        for (String k : List.copyOf(saved.keySet())) {
            if (k.startsWith(prefix)) {
                saved.remove(k);
            }
        }
        VerhaalSync.sync(p);
    }

    /** The line was touched just now (for "the most recently advanced line"). */
    private void raak(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        long n = saved.getLongOr(VerhaalSync.TELLER, 0L) + 1;
        saved.putLong(VerhaalSync.TELLER, n);
        saved.putLong(key("laatst"), n);
    }

    /** When the line was last touched (a per-player counter, 0 = never). */
    long laatst(ServerPlayer p) {
        return GuhQuests.saved(p).getLongOr(key("laatst"), 0L);
    }

    /** The text variant of where the player is now ("2", "3_bewoner", "klaar"). */
    public String sleutel(ServerPlayer p) {
        int stap = stap(p);
        String s = sleutel.apply(p, stap);
        if (s == null) {
            s = String.valueOf(stap);
        }
        return stap >= stappen && !s.startsWith("klaar") ? "klaar" : s;
    }

    /** Where to go now (null: nowhere in particular, or done). */
    @Nullable
    public Doel doel(ServerPlayer p) {
        return klaar(p) ? null : doel.apply(p, stap(p));
    }

    /** The line as the Guhdex tab Verhalen shows it for this player. */
    public VerhaalStand stand(ServerPlayer p) {
        int stap = stap(p);
        List<VerhaalStand.Nodig> n = stap >= stappen ? List.of() : nodig.apply(p, stap);
        List<VerhaalStand.Beloning> b = beloningen.apply(p);
        return VerhalenVoortgang.stand(id, icoon, stap, begonnen(p), sleutel(p), n == null ? List.of() : n, b == null ? List.of() : b);
    }

    // =====================================================================================================================
    // client
    // =====================================================================================================================

    /** Client: the synced step of the local player in this line (-1: unknown). */
    public static int stapClient(String id) {
        return VerhaalSync.Client.stap(id);
    }

    // =====================================================================================================================
    // helpers for nodig / beloningen
    // =====================================================================================================================

    /** "You need n of this item, you have heb" (the item's own name). */
    public static VerhaalStand.Nodig nodig(String itemId, int heb, int n) {
        return new VerhaalStand.Nodig(itemId, itemNaam(itemId), Math.min(heb, n), n);
    }

    /** The same with your own text (a lang key). */
    public static VerhaalStand.Nodig nodig(String itemId, String tekstKey, int heb, int n) {
        return new VerhaalStand.Nodig(itemId, Component.translatable(tekstKey), Math.min(heb, n), n);
    }

    /** A reward that is an item (its own name), ticked when binnen. */
    public static VerhaalStand.Beloning beloning(String itemId, boolean binnen) {
        return new VerhaalStand.Beloning(itemId, itemNaam(itemId), binnen);
    }

    /** A reward with your own text (a lang key), drawn with that item. */
    public static VerhaalStand.Beloning beloning(String itemId, String tekstKey, boolean binnen) {
        return new VerhaalStand.Beloning(itemId, Component.translatable(tekstKey), binnen);
    }

    private static Component itemNaam(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        return Component.translatable(id == null ? itemId : BuiltInRegistries.ITEM.getValue(id).getDescriptionId());
    }

    @Override
    public String toString() {
        return "Verhaallijn[" + id + "]";
    }
}
