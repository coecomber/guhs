package nl.juiced.guhs.feature.mewtwo;

/**
 * The client's copy of its own player's questline (from {@link MewtwoPayloads.Stand}; plain static fields, so the blocks'
 * animateTick and the tank's renderer can read it without client classes): which notes and parts you still need, how far
 * your kloontank is repaired.
 */
public final class MewtwoStand {
    private static volatile int stap, notities, onderdelen, ingebouwd;

    public static void zet(int stap, int notities, int onderdelen, int ingebouwd) {
        MewtwoStand.stap = stap;
        MewtwoStand.notities = notities;
        MewtwoStand.onderdelen = onderdelen;
        MewtwoStand.ingebouwd = ingebouwd;
    }

    public static int stap() {
        return stap;
    }

    public static boolean heeftNotitie(int n) {
        return (notities & (1 << n)) != 0;
    }

    /** Is this crate's part still needed (the professor asks and you haven't got it)? */
    public static boolean zoektOnderdeel(int n) {
        return stap == MewtwoVoortgang.ONDERDELEN && (onderdelen & (1 << n)) == 0;
    }

    public static boolean isIngebouwd(int n) {
        return stap >= MewtwoVoortgang.MAALTIJD || (ingebouwd & (1 << n)) != 0;
    }

    /** Your tank: repaired (bubbling pink) or still cracked. */
    public static boolean tankHeel() {
        return stap >= MewtwoVoortgang.MAALTIJD;
    }

    private MewtwoStand() {
    }
}
