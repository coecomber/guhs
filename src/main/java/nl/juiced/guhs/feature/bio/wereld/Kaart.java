package nl.juiced.guhs.feature.bio.wereld;

/**
 * biomes3 wereld: what the terrain model ({@link BioModel}) says about the 16 x 16 columns of one chunk. A pure function
 * of the world's noises and the position, so the terrain (density function), the biome (region function), the water and
 * clouds (feature) and the structure spots ({@link BioPlekken}) all read the same thing, before any block exists.
 * <p>
 * Per column (index {@code (x & 15) | (z & 15) << 4}):
 * <ul>
 *   <li>{@link #soort}: which of our biomes the column belongs to ({@link #BUITEN}: none);</li>
 *   <li>{@link #meng}: 0 = the Guhmensie's own terrain, 1 = ours, between: the rim where the two blend;</li>
 *   <li>{@link #hoogte}: the y of our top solid block (ground, river bed, lake floor, island);</li>
 *   <li>{@link #water}: the y of the top water block, or {@link #GEEN};</li>
 *   <li>{@link #terras}: the Klaterdal terrace (0 = the valley floor at the lake, 3 = the rim), -1 elsewhere;</li>
 *   <li>{@link #vlag}: bits {@link #RIVIER}, {@link #LIP}, {@link #VAL}, {@link #EILAND}, {@link #GROOT}, and the
 *       polish bits {@link #MEER_STEEN}, {@link #MEER_STRAND}, {@link #DAL_VORM}, {@link #DAL_TREDE};</li>
 *   <li>{@link #spans}: floating solids of the Wolkenweide as pairs (bottom y, top y), or null.</li>
 * </ul>
 */
public final class Kaart {
    public static final int GEEN = Integer.MIN_VALUE;
    public static final byte BUITEN = 0, DAL = 1, MEER = 2, WEIDE = 3;
    /** The column is river (it holds river water, unless it is a {@link #LIP}). */
    public static final byte RIVIER = 1;
    /** A dry rock lip in the river: the column would have leaked onto lower ground. */
    public static final byte LIP = 2;
    /** River water that falls onto lower water next to it (the top of a cascade or waterfall). */
    public static final byte VAL = 4;
    /** Land (or beach) of a lake island. */
    public static final byte EILAND = 8;
    /** Land of a LARGE lake island. */
    public static final byte GROOT = 16;
    // biomes3 merge: the bits the polish of the lake and of the valley added each in their own file were the same two
    // numbers (32, 64). Lake and valley columns lie side by side and several readers look at a neighbour's flags without
    // asking whose column it is (DalPlanten "against rock", MeerLeven on the valley-floor strip of biome Bloesemmeertje,
    // BioPlekken's meer_oever in the valley floor), so every bit has ONE meaning now; that needs more than a byte.
    /** Lake: a boulder or stepping stone (stone at or above the water). {@link MeerTerrein#STEEN}. */
    public static final short MEER_STEEN = 32;
    /** Lake: sand (the shore's edge, an island's rim or beach). {@link MeerTerrein#STRAND}. */
    public static final short MEER_STRAND = 64;
    /** Klaterdal: a sculpted column (cascade, plunge pool, boulder, rounded face, natural step). {@link DalTerrein#VORM}. */
    public static final short DAL_VORM = 128;
    /** Klaterdal: a natural step beside a cascade. {@link DalTerrein#TREDE}. */
    public static final short DAL_TREDE = 256;

    /** A chunk none of our biomes touches. */
    static Kaart leeg(int cx, int cz) {
        Kaart k = new Kaart(true);
        k.cx = cx;
        k.cz = cz;
        return k;
    }

    public final boolean leeg;
    public final byte[] soort;
    public final float[] meng;
    public final int[] hoogte;
    public final int[] water;
    public final byte[] terras;
    public final short[] vlag;
    public final int[][] spans;
    int cx, cz;
    /** biomes3 merge: working this map out took a terrain pass (also when it came out empty): worth sharing between threads. */
    boolean duur;

    private Kaart(boolean leeg) {
        this.leeg = leeg;
        soort = null;
        meng = null;
        hoogte = null;
        water = null;
        terras = null;
        vlag = null;
        spans = null;
    }

    Kaart(int cx, int cz) {
        this.cx = cx;
        this.cz = cz;
        leeg = false;
        soort = new byte[256];
        meng = new float[256];
        hoogte = new int[256];
        water = new int[256];
        terras = new byte[256];
        vlag = new short[256];
        spans = new int[256][];
        java.util.Arrays.fill(water, GEEN);
        java.util.Arrays.fill(terras, (byte) -1);
    }

    static int index(int x, int z) {
        return (x & 15) | (z & 15) << 4;
    }

    /** Adds a floating solid (bottom..top, both included) to a column. */
    void span(int i, int onder, int boven) {
        int[] oud = spans[i];
        if (oud == null) {
            spans[i] = new int[]{onder, boven};
        } else {
            int[] nieuw = java.util.Arrays.copyOf(oud, oud.length + 2);
            nieuw[oud.length] = onder;
            nieuw[oud.length + 1] = boven;
            spans[i] = nieuw;
        }
    }

    /** Is (this column, y) inside a floating solid? */
    public boolean inSpan(int i, int y) {
        int[] sp = spans[i];
        if (sp != null) {
            for (int s = 0; s < sp.length; s += 2) {
                if (y >= sp[s] && y <= sp[s + 1]) {
                    return true;
                }
            }
        }
        return false;
    }
}
