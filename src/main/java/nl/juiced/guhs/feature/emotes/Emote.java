package nl.juiced.guhs.feature.emotes;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import com.geckolib.animation.RawAnimation;

/**
 * The guh emotes (seven since 2.5, three more in 2.8: GAPEN, ZINGEN, KNUFFELEN; four in 2.10: HARTJES,
 * KNUFFELDANSJE, BFF_KNUFFEL, VERDRIETJE; new ones always go last). The animations ("animation.guh.emote_&lt;id&gt;") are made by tools/features/emotes.py and all
 * loop: {@link #onceTicks} is how long "nu" (once) lasts, "blijven doen" goes on until it is stopped.
 */
public enum Emote {
    /** Waves a paw (wild guhs wave at players who come close). */
    ZWAAIEN(48),
    /** Wiggles its vads (also by itself near a playing jukebox); music notes. */
    DANSEN(96),
    /** Curls up and snores softly; zzz. */
    SLAPEN(200),
    /** A happy jump with a spin: "VAHOEG!". */
    VAHOEG(22),
    /** Rolls onto its back, wiggles its paws and rolls back. */
    ROLLEN(40),
    /** Munches an imaginary kaasknabbel; crumbs. */
    SMAKKEN(72),
    /** Paws in front of its eyes, peeking; hearts. */
    VERLEGEN(60),
    // --- 2.8 (Knuffeldal) ---
    /** A big yawn and a stretch (paws out, back arched): good morning! */
    GAPEN(60),
    /** Sings, swaying, with music notes: guhs around sing along; the tuintjes grow faster (KnusSignalen.zang). */
    ZINGEN(80),
    /** A big hug (paws around an imaginary friend, a plushie or another guh); hearts. */
    KNUFFELEN(60),
    // --- 2.10 ---
    /** Blows little hearts at you, paw at its snoet (hartjes level 1, "lieve vadsjes van elkaar"). */
    HARTJES(60),
    /** A happy twirl dance, round and round with little hops (hartjes level 2, "mega lieve vadsjes van elkaar"). */
    KNUFFELDANSJE(96),
    /** Stands up and hugs you with both paws, a big heart above you both (hartjes level 3, "zielsguh bff 5evr &lt;3"). */
    BFF_KNUFFEL(80),
    /** Lovingly sad: ears and head down, a little sigh, "ooh njeg..." (at a miss; never gated). */
    VERDRIETJE(60),
    // --- 3.0 ---
    /** Only the 626-guh (guhwaii): strums a little ukelele, "Aloha, njeg!" (see {@link #alleenVoor}). */
    UKELELE(80),
    // --- 1.2.0 ---
    /**
     * Being petted (a short tap from the owner, BandEvents.aai): a squish (flatter and wider, eyes happily shut), then head
     * and a paw up against your hand and a happy wiggle. Not in the picker, never a favourite or a random one, and it doesn't
     * count for "alle emotes" ({@link #kiesbaar}).
     */
    AAIEN(32);

    public final int onceTicks;
    public final RawAnimation animation;

    Emote(int onceTicks) {
        this.onceTicks = onceTicks;
        this.animation = RawAnimation.begin().thenLoop("animation.guh.emote_" + id());
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Can the owner pick it (the emote picker, a favourite)? Everything but the petting moment. */
    public boolean kiesbaar() {
        return this != AAIEN;
    }

    /** The emotes of the picker, in order. */
    public static java.util.List<Emote> kiesbare() {
        return java.util.Arrays.stream(values()).filter(Emote::kiesbaar).toList();
    }

    public Component displayName() {
        return Component.translatable("emote.guhs." + id());
    }

    public Component description() {
        return Component.translatable("emote.guhs." + id() + ".description");
    }

    /**
     * 3.0: the one guh variant that can do this emote (null: every guh). The UKELELE is only for the 626-guh: it is refused
     * for others (EmotePayload, the picker) and doesn't count for "alle emotes" (GuhEmotes.alleNietBand).
     */
    @Nullable
    public static nl.juiced.guhs.entity.GuhVariant alleenVoor(Emote emote) {
        return emote == UKELELE ? nl.juiced.guhs.entity.GuhVariant.STITCH626 : null;
    }

    /** May this guh do this emote (its variant, see {@link #alleenVoor})? */
    public static boolean magVoor(Emote emote, nl.juiced.guhs.entity.GuhEntity guh) {
        var v = alleenVoor(emote);
        return v == null || guh.getVariant() == v;
    }

    @Nullable
    public static Emote byIndex(int i) {
        return i >= 0 && i < values().length ? values()[i] : null;
    }

    @Nullable
    public static Emote byId(String id) {
        for (Emote e : values()) {
            if (e.id().equals(id)) {
                return e;
            }
        }
        return null;
    }
}
