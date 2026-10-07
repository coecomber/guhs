package nl.juiced.guhs.feature.ringsausuman;

import java.util.Locale;
import java.util.function.Supplier;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;

/**
 * What goes into the Ringenbakker, and the station ({@link VoorraadBlock}) each comes from: dough from the Deegkneder, hot
 * frying sauce from the Sauskraan and "cheese" from the Kaaskast (which only holds an onion: that is the whole joke).
 */
public enum Ingredient implements StringRepresentable {
    DEEG(() -> RingSausumanFeature.RINGDEEG.get()),
    SAUS(() -> RingSausumanFeature.FRITUURSAUS.get()),
    KAAS(() -> RingSausumanFeature.UI.get());

    private final Supplier<Item> item;

    Ingredient(Supplier<Item> item) {
        this.item = item;
    }

    /** The quest item this station hands out. */
    public Item item() {
        return item.get();
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Override
    public String getSerializedName() {
        return id();
    }
}
