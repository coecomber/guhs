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

    /**
     * The score list on the cabinet itself (it takes turns with the title screen while nobody plays): "TOP 5" and up to five
     * lines "place name score". A name is cut to what fits; the pixel font shows letters without accents and digits.
     */
    public static void top(Doek d, Sim achtergrond, String kop, java.util.List<String> namen, int[] scores) {
        achtergrond.teken(d);
        d.rect(4, 4, Sim.B - 8, Sim.H - 8, 0xFFFF9AC8);
        d.rect(6, 6, Sim.B - 12, Sim.H - 12, 0xFF3D1F47);
        PixelFont.midden(d, kop, Sim.B / 2, 11, 2, 0xFFFFD84E);
        d.rect(10, 25, Sim.B - 20, 1, 0xFFFF9AC8);
        for (int i = 0; i < namen.size() && i < scores.length && i < 5; i++) {
            int y = 31 + i * 16;
            int kleur = i == 0 ? 0xFFFFD84E : 0xFFFFFFFF;
            String score = Integer.toString(scores[i]);
            int scoreX = Sim.B - 11 - PixelFont.breedte(score, 2);
            PixelFont.teken(d, Integer.toString(i + 1), 11, y, 2, 0xFFFF9AC8);
            int tekens = Math.max(1, (scoreX - 6 - 23) / 8);
            String naam = schoon(namen.get(i));
            PixelFont.teken(d, naam.length() > tekens ? naam.substring(0, tekens) : naam, 23, y, 2, kleur);
            PixelFont.teken(d, score, scoreX, y, 2, kleur);
        }
    }

    /** A name for the pixel font: capitals, accents dropped, anything else it does not know becomes a dot. */
    static String schoon(String naam) {
        String kaal = java.text.Normalizer.normalize(naam, java.text.Normalizer.Form.NFD).toUpperCase(java.util.Locale.ROOT);
        StringBuilder uit = new StringBuilder();
        for (int i = 0; i < kaal.length(); i++) {
            char c = kaal.charAt(i);
            if (Character.getType(c) == Character.NON_SPACING_MARK) {
                continue;
            }
            uit.append(c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == ' ' || c == '-' || c == '!' || c == '?' || c == '+' || c == ':' ? c : '.');
        }
        return uit.toString().trim();
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
