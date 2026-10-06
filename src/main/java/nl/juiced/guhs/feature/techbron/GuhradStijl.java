package nl.juiced.guhs.feature.techbron;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import nl.juiced.guhs.entity.GuhVariant;

/**
 * How a guh does its rounds in a Guhrad (bbq2, tech-bronnen: the refinement of the Guhrad "per variant"). How MUCH a
 * variant gives is data ({@code data/guhs/vadskracht/guhrad.json}, {@link nl.juiced.guhs.feature.vadskracht.GuhradKracht});
 * this is how it LOOKS and what the hover readout says about it: the Baltoguh runs harder, Guhtwo does not run at all (it
 * floats and lets the wheel turn with its thoughts), the 626-guh counts for two, Sam-guh carries the whole wheel along and
 * Guhshi flutter-kicks. Both sides: the wheel's block entity shows the line, its renderer and its animation use
 * {@link #tempo} and {@link #zweeft}.
 */
public enum GuhradStijl {
    GEWOON(1.0f, false),
    /** The Baltoguh: a sled dog at heart. */
    HARD(1.7f, false),
    /** Guhtwo: floats in the wheel, the wheel turns by itself. */
    ZWEEFT(1.3f, true),
    /** The 626-guh: four arms and two legs. */
    DUBBEL(2.0f, false),
    /** Sam-guh: steady, with a full backpack. */
    SJOUWT(1.15f, false),
    /** Guhshi: flutter-kicks. */
    FLADDERT(1.5f, false);

    /** How fast the wheel turns with this guh in it (1 = an ordinary guh). */
    public final float tempo;
    /** The guh floats above the wheel's floor and does not move its legs. */
    public final boolean zweeft;

    GuhradStijl(float tempo, boolean zweeft) {
        this.tempo = tempo;
        this.zweeft = zweeft;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The style of this variant id ("baltoguh"...). */
    public static GuhradStijl van(String variant) {
        return switch (variant.toLowerCase(Locale.ROOT)) {
            case "baltoguh" -> HARD;
            case "mewtwo" -> ZWEEFT;
            case "stitch626" -> DUBBEL;
            case "sam_guh" -> SJOUWT;
            case "guhshi" -> FLADDERT;
            default -> GEWOON;
        };
    }

    /** The style of the guh with this saved data (a picked-up guh, as it sits in the wheel). */
    public static GuhradStijl van(@Nullable CompoundTag guh) {
        return guh == null ? GEWOON : van(guh.getStringOr("Variant", GuhVariant.NORMAL.id()));
    }

    /** The line of the hover readout about this style ({@code gui.guhs.techbron.guhrad.<id>}), or null for an ordinary guh. */
    @Nullable
    public Component regel() {
        return this == GEWOON ? null : Component.translatable("gui.guhs.techbron.guhrad." + id()).withStyle(ChatFormatting.AQUA);
    }
}
