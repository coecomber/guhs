package nl.juiced.guhs.feature.verhaal;

import javax.annotation.Nullable;

import nl.juiced.guhs.entity.GuhVariant;

/**
 * 3.0 (Guhverhalen): the story guhs. Each is tameable ONCE per player, after its questline ({@link VerhaalGuhs}); the owner
 * slice (pkg) runs the questline and decides when to call {@link VerhaalGuhs#tem}. Append only.
 */
public enum VerhaalGuh {
    BALTOGUH(GuhVariant.BALTOGUH, "balto"),
    MEWTWO(GuhVariant.MEWTWO, "mewtwo"),
    STITCH626(GuhVariant.STITCH626, "guhwaii"),
    // bbq2: Sam-guh (after the Knabbelring) and Guhshi (after the duel in Super Guhrio)
    SAM_GUH(GuhVariant.SAM_GUH, "ring"),
    GUHSHI(GuhVariant.GUHSHI, "guhrio");

    private final GuhVariant variant;
    private final String pkg;

    VerhaalGuh(GuhVariant variant, String pkg) {
        this.variant = variant;
        this.pkg = pkg;
    }

    public GuhVariant variant() {
        return variant;
    }

    /** The owner slice's package (advancement verhalen/&lt;pkg&gt;_getemd). */
    public String pkg() {
        return pkg;
    }

    /** = the variant id (baltoguh, mewtwo, stitch626). */
    public String id() {
        return variant.id();
    }

    @Nullable
    public static VerhaalGuh van(GuhVariant v) {
        for (VerhaalGuh g : values()) {
            if (g.variant == v) {
                return g;
            }
        }
        return null;
    }

    @Nullable
    public static VerhaalGuh byId(String id) {
        for (VerhaalGuh g : values()) {
            if (g.id().equals(id)) {
                return g;
            }
        }
        return null;
    }
}
