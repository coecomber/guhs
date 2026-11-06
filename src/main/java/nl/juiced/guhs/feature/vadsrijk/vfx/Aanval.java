package nl.juiced.guhs.feature.vadsrijk.vfx;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;

/**
 * One attack from the boss bouwdoos (roadmap 3.1): pure data, built with a small fluent builder, run by {@link Bouwdoos}.
 * <p>
 * The rhythm is always <b>telegraph &rarr; wind-up &rarr; hit</b>: at the start the cast bar shows ({@link #cast}), the boss plays
 * its wind-up ({@link #windup}, held until the hit) and each {@link Stap} lays its decal at {@code na} ticks, which fills up for
 * {@code telegraaf} ticks and then hits exactly the area it showed (+ the hit animation {@link #treffer}, particles, a shockwave,
 * screen shake). Mechanics: tankbuster (anchor DOEL), spread (ELKE_SPELER), stack ({@link Stap#stapel}: the damage is shared by
 * everyone inside), safe spot ({@link Vorm#veilig}), interrupt ({@link #onderbreek}: deal this fraction of its max Vads during the
 * cast, or stun it, and it stops), and whatever a boss adds itself ({@link #bijStart}, {@link #nietOnderbroken}).
 * <p>
 * Example: {@code Aanval.nieuw("kloddergolf").fase(2).wacht(400).cast().windup("vfx_windup").treffer("stamp")
 * .stap(Stap.op(0, 40, Vorm.donut(4, 11), Anker.BAAS).factor(1.4).effect(Effect.SLIJM).schud(0.5f))}.
 * The cast bar name is the lang key {@code vadsrijk.vfx.aanval.<id>}.
 */
public final class Aanval {
    /** Where a step's shape is laid. */
    public enum Anker {
        /** On the boss (cones/strips point at its target). */
        BAAS,
        /** On the boss's target (a tankbuster): one shape. */
        DOEL,
        /** On every fighter (spread). */
        ELKE_SPELER,
        /** {@code aantal} random spots in the fight (rain). */
        WILLEKEURIG,
        /** On the centre of the fight. */
        MIDDEN
    }

    /** The look of a hit: which particles, which colour. */
    public enum Effect {
        PLOF(Deeltje.PLUIS, 0xFF8AD0), SLIJM(Deeltje.SLIJM, 0x9AD86A), SPOREN(Deeltje.SPOOR, 0xC8A0FF), KRISTAL(Deeltje.KRISTAL, 0x9AD8FF),
        VUUR(Deeltje.VONK, 0xFFA040), VEREN(Deeltje.VEER, 0xE8E0D0), KAAS(Deeltje.HONING, 0xFFD040), WORTEL(Deeltje.BLAD, 0x8AB060),
        ROZE(Deeltje.HART, 0xFF7AC8), STER(Deeltje.STER, 0xFFF0A0), SCHADUW(Deeltje.ROOK, 0x7A4A9A);

        public final Deeltje deeltje;
        public final int kleur;

        Effect(Deeltje deeltje, int kleur) {
            this.deeltje = deeltje;
            this.kleur = kleur;
        }
    }

    /** One telegraphed hit of an attack. */
    public static final class Stap {
        final int na, telegraaf;
        final Vorm vorm;
        final Anker anker;
        double draai, factor = 1.0, duw;
        int aantal = 1, kleur = 0xFF2A2A, stTicks;
        @Nullable
        Status status;
        Effect effect = Effect.PLOF;
        float schud = 0.25f;
        boolean stapel;

        private Stap(int na, int telegraaf, Vorm vorm, Anker anker) {
            this.na = na;
            this.telegraaf = telegraaf;
            this.vorm = vorm;
            this.anker = anker;
        }

        /** The decal appears {@code na} ticks after the start and hits {@code telegraaf} ticks later. */
        public static Stap op(int na, int telegraaf, Vorm vorm, Anker anker) {
            return new Stap(na, Math.max(10, telegraaf), vorm, anker);
        }

        /** Turn the shape by this many degrees (cones/strips; for a second cone behind: 180). */
        public Stap draai(double graden) {
            this.draai = graden;
            return this;
        }

        /** x the boss's base hit (per player hit). */
        public Stap factor(double f) {
            this.factor = f;
            return this;
        }

        /** Knock players away from the anchor. */
        public Stap duw(double d) {
            this.duw = d;
            return this;
        }

        /** {@link Anker#WILLEKEURIG}: how many spots. */
        public Stap aantal(int n) {
            this.aantal = n;
            return this;
        }

        public Stap kleur(int k) {
            this.kleur = k;
            return this;
        }

        public Stap status(Status s, int ticks) {
            this.status = s;
            this.stTicks = ticks;
            return this;
        }

        public Stap effect(Effect e) {
            this.effect = e;
            return this;
        }

        public Stap schud(float s) {
            this.schud = s;
            return this;
        }

        /** Stack: the damage is shared by everyone inside (alone = 60 %, two = 50 % each, ...). Yellow by default. */
        public Stap stapel() {
            this.stapel = true;
            this.kleur = 0xFFD62A;
            return this;
        }

        public int na() {
            return na;
        }

        public int telegraaf() {
            return telegraaf;
        }

        public Vorm vorm() {
            return vorm;
        }

        public Anker anker() {
            return anker;
        }
    }

    final String id;
    int minFase = 1, maxFase = 99, wacht = 400, eersteWacht = 160, gewicht = 1;
    boolean cast = true;
    double onderbreek;
    @Nullable
    String windup = "vfx_windup", treffer;
    final List<Stap> stappen = new ArrayList<>();
    @Nullable
    BiConsumer<VadsrijkBaas, ServerLevel> bijStart, nietOnderbroken;

    private Aanval(String id) {
        this.id = id;
    }

    public static Aanval nieuw(String id) {
        return new Aanval(id);
    }

    /** Only from this phase on. */
    public Aanval fase(int f) {
        this.minFase = f;
        return this;
    }

    /** Only up to this phase. */
    public Aanval totFase(int f) {
        this.maxFase = f;
        return this;
    }

    /** Cooldown (ticks) before this attack may come again. */
    public Aanval wacht(int ticks) {
        this.wacht = ticks;
        return this;
    }

    /** Ticks after the fight (or this phase) starts before it may come the first time. */
    public Aanval eerst(int ticks) {
        this.eersteWacht = ticks;
        return this;
    }

    /** Relative chance when several are ready. */
    public Aanval gewicht(int g) {
        this.gewicht = g;
        return this;
    }

    /** No cast bar (a quick attack). */
    public Aanval zonderCast() {
        this.cast = false;
        return this;
    }

    /** Interruptible: deal this fraction of its max Vads during the cast (or stun it) and it stops. */
    public Aanval onderbreek(double fractie) {
        this.onderbreek = fractie;
        return this;
    }

    /** The wind-up animation (GeckoLib; "vfx_..." on our controller, other names on the boss's own "actie"), null = none. */
    public Aanval windup(@Nullable String anim) {
        this.windup = anim;
        return this;
    }

    /** The hit animation at the first hit (e.g. the boss's own "stamp"), null = our "vfx_treffer". */
    public Aanval treffer(@Nullable String anim) {
        this.treffer = anim;
        return this;
    }

    public Aanval stap(Stap s) {
        stappen.add(s);
        return this;
    }

    /** Runs when the attack starts (summon adds, change the arena...). */
    public Aanval bijStart(BiConsumer<VadsrijkBaas, ServerLevel> r) {
        this.bijStart = r;
        return this;
    }

    /** Runs when an interruptible attack was NOT interrupted (heal, adds, a big wave...). */
    public Aanval nietOnderbroken(BiConsumer<VadsrijkBaas, ServerLevel> r) {
        this.nietOnderbroken = r;
        return this;
    }

    public String id() {
        return id;
    }

    public String naamKey() {
        return "vadsrijk.vfx.aanval." + id;
    }

    public List<Stap> stappen() {
        return stappen;
    }

    public int minFase() {
        return minFase;
    }

    public boolean onderbreekbaar() {
        return onderbreek > 0;
    }

    /** Ticks from the start to the last hit. */
    public int duur() {
        int d = 0;
        for (Stap s : stappen) {
            d = Math.max(d, s.na + s.telegraaf);
        }
        return d;
    }
}
