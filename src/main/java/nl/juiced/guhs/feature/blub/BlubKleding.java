package nl.juiced.guhs.feature.blub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;

/**
 * 1.8.0: what a Blub-guh can wear (its Kledingkast, like the guh's wardrobe). Per {@link Plek} (a place on the Blub) one
 * {@link Stuk} at a time; a Stuk is drawn by the bone of its plek, tinted with its colour. For now only strikjes (one per dye
 * colour, all free); more places and pieces are added here (a plek needs a bone in tools/features/blub_modellen.py with the
 * same name, white in the texture), the screen and saving follow by themselves.
 */
public final class BlubKleding {
    /** A place on the Blub. Its id is its bone name and its save key (append only). */
    public enum Plek {
        STRIK;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Component naam() {
            return Component.translatable("gui.guhs.blubguh.kast.plek." + id());
        }
    }

    /** One piece: its id (saved), its place and the colour it is drawn in (0xRRGGBB). */
    public record Stuk(String id, Plek plek, int kleur, Component naam) {
    }

    private static final Map<String, Stuk> ALLES = new LinkedHashMap<>();

    static {
        for (DyeColor kleur : DyeColor.values()) {
            voegToe(new Stuk("strik_" + kleur.getName(), Plek.STRIK, kleur.getTextureDiffuseColor() & 0xFFFFFF,
                    Component.translatable("gui.guhs.blubguh.kast.strik", Component.translatable("color.minecraft." + kleur.getName()))));
        }
    }

    private BlubKleding() {
    }

    /** Adds a piece (for later pieces; ids must stay the same once players wear them). */
    public static void voegToe(Stuk stuk) {
        ALLES.put(stuk.id(), stuk);
    }

    @Nullable
    public static Stuk stuk(String id) {
        return ALLES.get(id);
    }

    /** Every piece of this place, in order. */
    public static List<Stuk> van(Plek plek) {
        List<Stuk> out = new ArrayList<>();
        for (Stuk s : ALLES.values()) {
            if (s.plek() == plek) {
                out.add(s);
            }
        }
        return Collections.unmodifiableList(out);
    }

    /** May this Blub wear this piece? (For now: everything is free.) */
    public static boolean mag(BlubGuhEntity blub, Stuk stuk) {
        return true;
    }
}
