package nl.juiced.guhs.feature.guhpixel.guhkade.spel;

/**
 * What a Guhkade game draws on: the cabinet's screen of {@link Sim#B} x {@link Sim#H} game pixels (x to the right, y down).
 * Two implementations, both on the client: the game screen you play in and the little screen on the cabinet block itself.
 * A game only draws coloured rectangles and sprites from the one sprite sheet, so both look exactly the same.
 */
public interface Doek {
    /** A filled rectangle (colour 0xAARRGGBB; alpha is all or nothing). */
    void rect(int x, int y, int b, int h, int argb);

    /** A sprite of the sheet, its top left corner at x, y. */
    void sprite(Sprite s, int x, int y);
}
