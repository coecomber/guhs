package nl.juiced.guhs.feature.guhpixel.guhkade.spel;

/**
 * What a cabinet's screen shows around a game: the title screen (while nobody plays) and the "game over" card. The few
 * words come from the caller (the client's language), in capitals: the pixel font knows A-Z, digits and - ! ? . : +
 */
public final class Schermen {
    /** The title screen over a fresh game as its backdrop: the game's name, the best score here, and a blinking "START". */
    public static void titel(Doek d, Sim achtergrond, String naam, String top, int topScore, String start, boolean knipper) {
        achtergrond.teken(d);
        d.rect(0, 30, Sim.B, 34, 0xFF3D1F47);
        d.rect(0, 30, Sim.B, 2, 0xFFFF9AC8);
        d.rect(0, 62, Sim.B, 2, 0xFFFF9AC8);
        int schaal = PixelFont.breedte(naam, 3) <= Sim.B - 8 ? 3 : 2;
        PixelFont.midden(d, naam, Sim.B / 2, schaal == 3 ? 39 : 42, schaal, 0xFFFFD84E);
        if (topScore > 0) {
            PixelFont.midden(d, top + " " + topScore, Sim.B / 2, 72, 2, 0xFFFFFFFF);
        }
        if (knipper && !start.isEmpty()) {
            PixelFont.midden(d, start, Sim.B / 2, 92, 2, 0xFFFF9AC8);
        }
    }

    /** Over the last picture of a game: a card with a word ("NJEG!", or "VAHOEG!" for a new best) and the score. */
    public static void af(Doek d, String woord, int score, boolean record) {
        d.rect(24, 38, Sim.B - 48, 44, 0xFFFF9AC8);
        d.rect(26, 40, Sim.B - 52, 40, 0xFF3D1F47);
        PixelFont.midden(d, woord, Sim.B / 2, 46, 2, record ? 0xFFFFD84E : 0xFFFFFFFF);
        PixelFont.midden(d, Integer.toString(score), Sim.B / 2, 62, 3, 0xFFFFFFFF);
    }

    private Schermen() {
    }
}
