package nl.juiced.guhs.feature.bouwhulp.keet;

import javax.annotation.Nullable;

/**
 * The five levels of a Bouwkeet (design 2.3): each is unlocked with an upgrade from a tech tier of the main questline
 * ({@link BouwkeetUpgradeItem}), and from then on the keet draws its VK while it is switched on. Without vadskracht a keet
 * works as level 0 (handwerk) until the power is back.
 *
 * <table>
 * <tr><th>level</th><th>VK</th><th>shapes</th><th>blueprints</th><th>adds</th></tr>
 * <tr><td>0 Handwerk</td><td>0</td><td>16</td><td>16</td><td>vormstaf, kwasten, roofs, own blueprints, the library</td></tr>
 * <tr><td>1 Guhrad-keet</td><td>10</td><td>24</td><td>16</td><td>bouwguhs, draadloos materiaal</td></tr>
 * <tr><td>2 Zoutkeet</td><td>30</td><td>24</td><td>32</td><td>natuurvormen, egaliseerder, bloktekst</td></tr>
 * <tr><td>3 Sauskeet</td><td>80</td><td>24</td><td>32</td><td>verfmachine, tunnelboor, publishing in the library</td></tr>
 * <tr><td>4 Gloeikeet</td><td>200</td><td>64</td><td>64</td><td>Pof!</td></tr>
 * </table>
 * Test round 5: how far from the keet it builds ({@link #bereik}, square, horizontally): 64, 96, 128, 160, 192.
 */
public enum BouwNiveau {
    HANDWERK(0, 16, 16, 64),
    GUHRAD(10, 24, 16, 96),
    ZOUT(30, 24, 32, 128),
    SAUS(80, 24, 32, 160),
    GLOEI(200, 64, 64, 192);

    /** VK per second while the keet is switched on. */
    public final int vk;
    /** The longest side of a shape (vormen ≤ n). */
    public final int maxVorm;
    /** The longest side of a blueprint (≤ n³). */
    public final int maxBlauwdruk;
    /** How far from the keet (horizontally, in each of x and z) its project may stand; the rest stays a ghost. */
    public final int bereik;

    BouwNiveau(int vk, int maxVorm, int maxBlauwdruk, int bereik) {
        this.vk = vk;
        this.maxVorm = maxVorm;
        this.maxBlauwdruk = maxBlauwdruk;
        this.bereik = bereik;
    }

    public int nummer() {
        return ordinal();
    }

    public static BouwNiveau van(int n) {
        return values()[Math.max(0, Math.min(values().length - 1, n))];
    }

    /** What this keet can do NOW: its level while it has vadskracht (and is switched on), else {@link #HANDWERK}. */
    public static BouwNiveau van(@Nullable BouwkeetBlockEntity keet) {
        return keet == null ? HANDWERK : keet.werkNiveau();
    }

    /** The next level, or null at the top. */
    @Nullable
    public BouwNiveau volgende() {
        return this == GLOEI ? null : values()[ordinal() + 1];
    }

    public boolean bouwguhs() {
        return this.compareTo(GUHRAD) >= 0;
    }

    public boolean draadloos() {
        return this.compareTo(GUHRAD) >= 0;
    }

    public boolean natuurvormen() {
        return this.compareTo(ZOUT) >= 0;
    }

    public boolean egaliseerder() {
        return this.compareTo(ZOUT) >= 0;
    }

    public boolean bloktekst() {
        return this.compareTo(ZOUT) >= 0;
    }

    public boolean verfmachine() {
        return this.compareTo(SAUS) >= 0;
    }

    public boolean tunnelboor() {
        return this.compareTo(SAUS) >= 0;
    }

    public boolean publiceren() {
        return this.compareTo(SAUS) >= 0;
    }

    public boolean pof() {
        return this == GLOEI;
    }

    /** The text key of its name (gui.guhs.bouwkeet.niveau.N). */
    public String sleutel() {
        return "gui.guhs.bouwkeet.niveau." + ordinal();
    }
}
