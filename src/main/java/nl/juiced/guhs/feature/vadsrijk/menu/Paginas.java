package nl.juiced.guhs.feature.vadsrijk.menu;

/**
 * 1.10.27: the page maths of a paged grid in a Vadsrijk screen (the Huisdieren tab: 4 x 4 pets per page). Pure, side-neutral, so
 * a gametest can check it. Pages count from 0 (the label shows page + 1).
 */
public final class Paginas {
    private Paginas() {
    }

    /** How many pages {@code aantal} things take at {@code perPagina} each (always at least 1, also for an empty list). */
    public static int aantal(int aantal, int perPagina) {
        int per = Math.max(1, perPagina);
        return Math.max(1, (Math.max(0, aantal) + per - 1) / per);
    }

    /** The page the thing at {@code index} is on (0 for a negative index: "nothing chosen" opens on the first page). */
    public static int van(int index, int perPagina) {
        return Math.max(0, index) / Math.max(1, perPagina);
    }

    /** {@code pagina} kept inside the pages there are (after the list changed, or after a step past the first / last page). */
    public static int klem(int pagina, int aantal, int perPagina) {
        return Math.max(0, Math.min(pagina, aantal(aantal, perPagina) - 1));
    }

    /** The index of the first thing on this page. */
    public static int eerste(int pagina, int perPagina) {
        return Math.max(0, pagina) * Math.max(1, perPagina);
    }

    /** The index after the last thing on this page (the last page may be shorter). */
    public static int eind(int pagina, int aantal, int perPagina) {
        return Math.min(Math.max(0, aantal), eerste(pagina, perPagina) + Math.max(1, perPagina));
    }
}
