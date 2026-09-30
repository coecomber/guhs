package nl.juiced.guhs.feature.tuintjes;

import java.util.Locale;

import javax.annotation.Nullable;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** What grows in a guh_bloempot or guh_moestuinbak (block state "plant"): nothing, or one of the three guh plants. */
public enum TuinPlant implements StringRepresentable {
    LEEG, KNABBELPLANTJE, THEEKRUID, GUHBLOEM;

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Its seeds (null for LEEG). */
    @Nullable
    public Item zaadje() {
        return switch (this) {
            case KNABBELPLANTJE -> TuintjesFeature.KNABBELZAADJES.get();
            case THEEKRUID -> TuintjesFeature.THEEKRUIDZAADJES.get();
            case GUHBLOEM -> TuintjesFeature.GUHBLOEMZAADJES.get();
            case LEEG -> null;
        };
    }

    /** What you harvest (null for LEEG). */
    @Nullable
    public Item oogst() {
        return switch (this) {
            case KNABBELPLANTJE -> TuintjesFeature.KNABBELGRAAN.get();
            case THEEKRUID -> TuintjesFeature.THEEKRUID.get();
            case GUHBLOEM -> TuintjesFeature.GUHBLOEMETJE.get();
            case LEEG -> null;
        };
    }

    /** The plant these seeds grow into, or null. */
    @Nullable
    public static TuinPlant vanZaadje(ItemStack stack) {
        for (TuinPlant p : values()) {
            if (p.zaadje() != null && stack.is(p.zaadje())) {
                return p;
            }
        }
        return null;
    }
}
