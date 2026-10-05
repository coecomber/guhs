package nl.juiced.guhs.feature.huisje;

/**
 * 1.2.8: what a chore could do around one huisje right now, for the overview in the huisje screen ({@link HuisjeOverzicht}):
 * can it be done ({@link Staat}), why (the text gui.guhs.huisje.overzicht.klus.&lt;klus&gt;.&lt;reden&gt;, with {@link #aantal} as
 * its %s) and how many things there are to do. Every {@link Klus} answers it itself ({@link Klus#stand}), next to its
 * {@code zoek}, from the same checks.
 */
public record KlusStand(Staat staat, String reden, int aantal) {
    public enum Staat {
        /** There is something to do right now. */
        JA,
        /** Everything it needs is there, only nothing to do at this moment (nothing ripe, not evening yet...). */
        STRAKS,
        /** Something it needs is missing. */
        NEE
    }

    /** A chore that doesn't tell (shown as possible, with its own tip as the explanation). */
    public static final KlusStand ONBEKEND = new KlusStand(Staat.JA, "", 0);

    public static KlusStand ja(String reden, int aantal) {
        return new KlusStand(Staat.JA, reden, aantal);
    }

    public static KlusStand straks(String reden, int aantal) {
        return new KlusStand(Staat.STRAKS, reden, aantal);
    }

    public static KlusStand nee(String reden, int aantal) {
        return new KlusStand(Staat.NEE, reden, aantal);
    }
}
