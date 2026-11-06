package nl.juiced.guhs.feature.vadsrijk.hud;

import javax.annotation.Nullable;

/**
 * U-HUD: the emotes. {@code loop} = keeps going until you move (dans, zit, slaap); otherwise {@code ticks} long.
 * {@code teken} = the glyph on the emote wheel. The pose itself is client side ({@code hud.client.EmotePose}).
 */
public enum Emote {
    ZWAAI("zwaai", false, 50, "✋"),
    KNUFFEL("knuffel", false, 60, "❤"),
    DANS("dans", true, 0, "♪"),
    JUICH("juich", false, 45, "★"),
    ZIT("zit", true, 0, "▁"),
    SLAAP("slaap", true, 0, "z"),
    LACH("lach", false, 50, "☺"),
    WIJS("wijs", false, 40, "➜"),
    HARTJE("hartje", false, 50, "♥");

    public final String id;
    public final boolean loop;
    public final int ticks;
    public final String teken;

    Emote(String id, boolean loop, int ticks, String teken) {
        this.id = id;
        this.loop = loop;
        this.ticks = ticks;
        this.teken = teken;
    }

    public String naamKey() {
        return "vadsrijk.uhud.emote." + id;
    }

    @Nullable
    public static Emote van(String id) {
        for (Emote e : values()) {
            if (e.id.equals(id)) {
                return e;
            }
        }
        return null;
    }
}
