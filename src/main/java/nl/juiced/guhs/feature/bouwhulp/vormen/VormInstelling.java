package nl.juiced.guhs.feature.bouwhulp.vormen;

import net.minecraft.nbt.CompoundTag;

/**
 * What the vormstaf (and the Bouwkeet's Vormen and Daken tabs) are set to: the shape or roof, its size, hollow or filled,
 * the wall thickness and the two materials (design 2.4: wand, and rand/vloer for hollow shapes). Read from the player's
 * settings tag (keys as the option keys of {@code Gereedschap.VORMSTAF}).
 */
public record VormInstelling(Vormen.Vorm vorm, Daken.Dak dak, int b, int h, int d, boolean hol, int dikte, Mix wand, Mix rand, String tekst,
                             int overstek, boolean gevels) {
    public static VormInstelling van(CompoundTag t) {
        return new VormInstelling(Vormen.Vorm.van(t.getIntOr("vorm", 0)), Daken.Dak.van(t.getIntOr("dak", 0)), t.getIntOr("b", 9),
                t.getIntOr("h", 9), t.getIntOr("d", 9), t.getIntOr("hol", 1) != 0, t.getIntOr("dikte", 1),
                Mix.laad(t.getListOrEmpty("wand")), Mix.laad(t.getListOrEmpty("rand")), t.getStringOr("tekst", "GUH"),
                t.getIntOr("overstek", 1), t.getIntOr("gevels", 1) != 0);
    }

    /** The same with this size (the levels' limit). */
    public VormInstelling begrensd(int max) {
        return new VormInstelling(vorm, dak, klem(b, max), klem(h, max), klem(d, max), hol, Math.max(1, Math.min(4, dikte)), wand, rand, tekst,
                Math.max(0, Math.min(2, overstek)), gevels);
    }

    private static int klem(int v, int max) {
        return Math.max(1, Math.min(max, v));
    }
}
