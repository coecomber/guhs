package nl.juiced.guhs.feature.guhpixel;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** What a {@link GidsSectie} is built from; every text is a (translatable) Component. */
public interface Bouwer {
    /** A heading. */
    void kop(Component c);

    /** A line of text (wrapped). */
    void regel(Component c);

    /** "label ........ value". */
    void stat(Component label, Component waarde);

    /** A progress bar "label 3/10". */
    void voortgang(Component label, int heb, int totaal);

    /** A whole joke game: its name, its steps with ticks, how often it was played. */
    void grap(String grapId);

    /** An album cell (souvenirs, films, badges): an icon with a name and a tooltip, greyed out until it is earned. */
    void plaatje(ItemStack icoon, Component naam, @Nullable Component tip, boolean behaald);
}
