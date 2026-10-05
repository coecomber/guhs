package nl.juiced.guhs.feature.guhpixel;

import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * The Guhpixel ranks: a name PREFIX by the total muntjes ever earned ({@link Muntjes#totaal}). Shown in chat, the player
 * list and above the head by {@link Rangen}, only for players who unlocked Guhpixel, and each player can switch it off
 * in the Guhdex tab. Names: gui.guhs.guhpixel.rang.&lt;id&gt; ("[VADS+]").
 */
public enum Rang {
    GUH(0, ChatFormatting.GRAY),
    VADS(250, ChatFormatting.GREEN),
    VADS_PLUS(750, ChatFormatting.AQUA),
    MVG(1250, ChatFormatting.GOLD),
    MVG_PLUS_PLUS(1750, ChatFormatting.LIGHT_PURPLE);

    private final int vanaf;
    private final ChatFormatting kleur;

    Rang(int vanaf, ChatFormatting kleur) {
        this.vanaf = vanaf;
        this.kleur = kleur;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The total muntjes this rank starts at. */
    public int vanaf() {
        return vanaf;
    }

    public ChatFormatting kleur() {
        return kleur;
    }

    /** "[VADS+]", coloured. */
    public MutableComponent naam() {
        return Component.translatable("gui.guhs.guhpixel.rang." + id()).withStyle(kleur);
    }

    /** The next rank (null: the highest). */
    public Rang volgende() {
        return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null;
    }

    public static Rang bij(int totaal) {
        Rang r = GUH;
        for (Rang x : values()) {
            if (totaal >= x.vanaf) {
                r = x;
            }
        }
        return r;
    }

    public static Rang op(int ordinal) {
        return values()[Math.floorMod(ordinal, values().length)];
    }
}
