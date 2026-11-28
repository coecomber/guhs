package nl.juiced.guhs.feature.vadsrijk.klassen.boom;

import nl.juiced.guhs.feature.vadsrijk.menu.MenuIndeling;
import nl.juiced.guhs.feature.vadsrijk.menu.MenuIndeling.Kader;

/**
 * The geometry of the skill tree panel (side-neutral, so gametests can check it). The Skillboom tab uses EXACTLY the frame of every
 * other Vadsrijk-menu tab ({@link #kader}; it used to ask for a taller frame, which defeated the scale-up and made the frame jump
 * when you switched to it). The panel fills that frame; the tree column on the left is a clipped view that scrolls when the rows
 * (and, with {@code badgesOnder}, the advancement badges below them) are taller than the view.
 * <p>left/top/w/h = the dark tree panel in logical screen coordinates. "Content" y = 0 at the top of the scrolled content; a
 * content y is drawn at {@code vy0() + y - scroll}.
 */
public record BoomIndeling(int left, int top, int w, int h, int rijen, int dy, boolean badgesOnder) {
    /** A node is KNOOP x KNOOP; the 4 columns are DX apart. */
    public static final int KNOOP = 26, DX = 44;
    /** Above the panel inside the frame: the title band and the class tabs. */
    public static final int BOVEN = MenuIndeling.BAND + 24;
    /** The header (title, level, points) and the sticky column names above the scrolled view. */
    public static final int KOP = 34, KOLOMKOP = 12;
    /** The x (relative to left) of the line between the tree column and the right side. */
    public static final int SCHEIDING = 220;
    /** Row distance: as roomy as fits without scrolling, never tighter than MIN_DY (the lines between rows need the room). */
    public static final int MIN_DY = 34, MAX_DY = 38;
    /** Advancement badges: BADGE_W x BADGE_H, below the tree two on a line. */
    public static final int BADGE_W = 90, BADGE_H = 22, BADGES = 3;
    private static final int RAND = 3;

    /** The frame of the Skillboom tab: the common frame, nothing of its own. */
    public static Kader kader(int sw, int sh, int guiScale) {
        return MenuIndeling.kader(sw, sh, 0, 0, true, guiScale);
    }

    public static BoomIndeling van(Kader k, int rijen, boolean badgesOnder) {
        int h = k.h() - BOVEN - 8;
        int kijk = h - 6 - (KOP + KOLOMKOP);
        int extra = badgesOnder ? badgesH() : 0;
        int dy = MIN_DY;
        for (int d = MAX_DY; d > MIN_DY; d--) {
            if (2 * RAND + (rijen - 1) * d + KNOOP + extra <= kijk) {
                dy = d;
                break;
            }
        }
        return new BoomIndeling(k.left() + 8, k.top() + BOVEN, k.w() - 16, h, rijen, dy, badgesOnder);
    }

    private static int badgesH() {
        return 4 + 12 + ((BADGES + 1) / 2) * (BADGE_H + 4);
    }

    // ---- the clipped view (screen coordinates) --------------------------------------------------------------------------------

    public int vx0() {
        return left + 2;
    }

    public int vx1() {
        return left + SCHEIDING - 1;
    }

    public int vy0() {
        return top + KOP + KOLOMKOP;
    }

    public int vy1() {
        return top + h - 6;
    }

    public int kijkH() {
        return vy1() - vy0();
    }

    public boolean inKijk(double mx, double my) {
        return mx >= vx0() && mx < vx1() && my >= vy0() && my < vy1();
    }

    // ---- the scrolled content -----------------------------------------------------------------------------------------------------

    /** The bottom of the tree rows (content y). */
    public int boomOnder() {
        return 2 * RAND + (rijen - 1) * dy + KNOOP;
    }

    public int inhoudH() {
        return boomOnder() + (badgesOnder ? badgesH() : 0);
    }

    public int maxScroll() {
        return Math.max(0, inhoudH() - kijkH());
    }

    public int klem(double scroll) {
        return (int) Math.round(Math.max(0, Math.min(maxScroll(), scroll)));
    }

    /** Screen x of a node's left edge. */
    public int knoopX(int kolom) {
        return left + 20 + kolom * DX;
    }

    /** Content y of a node's top edge. */
    public int knoopY(int rij) {
        return RAND + rij * dy;
    }

    /** The scroll that puts this row in the middle of the view (clamped). */
    public int scrollVoor(int rij) {
        return klem(knoopY(rij) + KNOOP / 2.0 - kijkH() / 2.0);
    }

    /** Content y of the "Job-advancement" heading below the tree (only with {@code badgesOnder}). */
    public int badgeKopY() {
        return boomOnder() + 4;
    }

    /** Screen x of badge i (0-based) below the tree. */
    public int badgeX(int i) {
        return left + 12 + (i % 2) * (BADGE_W + 4);
    }

    /** Content y of badge i (0-based) below the tree. */
    public int badgeY(int i) {
        return badgeKopY() + 12 + (i / 2) * (BADGE_H + 4);
    }

    // ---- the right side -----------------------------------------------------------------------------------------------------------

    public int rx() {
        return left + SCHEIDING + 6;
    }

    public int rw() {
        return w - SCHEIDING - 16;
    }

    /** A low panel (GUI scale 4, small windows): the right side is laid out tighter. */
    public boolean krap() {
        return h < 230;
    }
}
