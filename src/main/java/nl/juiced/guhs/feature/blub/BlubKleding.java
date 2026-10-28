package nl.juiced.guhs.feature.blub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;

/**
 * 1.8.0: what a Blub-guh can wear (its Kledingkast, like the guh's wardrobe). Per {@link Plek} (a place on the Blub) one
 * {@link Stuk} at a time. A strikje is drawn by the bone "strik", tinted with its dye colour (all free). The Blub-kleding
 * (1.8.0 extension) are drawn by their own bone {@code kl_<id>} in their own colours (painted in every Blub texture by
 * tools/features/blub_modellen.py) and have to be unlocked first, per player, with their item guhs:blubkleding_<id>
 * ({@link BlubKledingUnlocks}). A new piece: a bone kl_<id> in blub_modellen.py (kleding_bones), its texts and icon in
 * tools/features/blub_kleding.py and one line here; the screen, saving and drawing follow by themselves.
 */
public final class BlubKleding {
    /** A place on the Blub. Its id is its save key (append only). */
    public enum Plek {
        STRIK, HOOFD, NEK, LIJF, OGEN;

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        public Component naam() {
            return Component.translatable("gui.guhs.blubguh.kast.plek." + id());
        }
    }

    /**
     * One piece: its id (saved), its place, its colour (0xRRGGBB: the tint of a strikje, the swatch of the others), its
     * name and whether it is a tinted strikje ({@code getint}) or a Blub-kleding piece drawn by its own bone.
     */
    public record Stuk(String id, Plek plek, int kleur, Component naam, boolean getint) {
        /** The bone that draws it. */
        public String bone() {
            return getint ? "strik" : "kl_" + id;
        }

        /** Does it have to be unlocked first? (every piece but the strikjes) */
        public boolean opSlot() {
            return !getint;
        }
    }

    private static final Map<String, Stuk> ALLES = new LinkedHashMap<>();
    /** The Blub-kleding pieces (ids as in BOUWPLAN_180_UITBREIDING and tools/features/blub_kleding.py). */
    public static final List<String> KLEDING = List.of("slaapmutsje", "sjaaltje", "sterrenkroontje", "bubbelhelm", "pyjamaatje",
            "slaapmaskertje", "bloemenkransje", "brilletje");
    /** The see-through ones (their own translucent pass). */
    public static final Set<String> DOORZICHTIG = Set.of("bubbelhelm");

    static {
        for (DyeColor kleur : DyeColor.values()) {
            voegToe(new Stuk("strik_" + kleur.getName(), Plek.STRIK, kleur.getTextureDiffuseColor() & 0xFFFFFF,
                    Component.translatable("gui.guhs.blubguh.kast.strik", Component.translatable("color.minecraft." + kleur.getName())), true));
        }
        kleding("slaapmutsje", Plek.HOOFD, 0x96BAEE);
        kleding("sjaaltje", Plek.NEK, 0xEE708C);
        kleding("sterrenkroontje", Plek.HOOFD, 0xF0C450);
        kleding("bubbelhelm", Plek.HOOFD, 0xC4E6FC);
        kleding("pyjamaatje", Plek.LIJF, 0xB0CEF6);
        kleding("slaapmaskertje", Plek.OGEN, 0xBCA0E2);
        kleding("bloemenkransje", Plek.HOOFD, 0xFAF6DE);
        kleding("brilletje", Plek.OGEN, 0x9C7046);
    }

    private static void kleding(String id, Plek plek, int kleur) {
        voegToe(new Stuk(id, plek, kleur, Component.translatable("gui.guhs.blubguh.kast.kleding." + id), false));
    }

    private BlubKleding() {
    }

    /** Adds a piece (ids must stay the same once players wear them). */
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

    /** May this player put this piece on a Blub? (strikjes always; the rest once unlocked; creative too) */
    public static boolean mag(Player player, Stuk stuk) {
        return !stuk.opSlot() || player.isCreative() || BlubKledingUnlocks.heeft(player, stuk.id());
    }

    /** (Client) the pieces this player has unlocked, as the server last sent them (with the Blub's menu). */
    public static final class Client {
        public static final Set<String> ONTGRENDELD = new HashSet<>();

        private Client() {
        }

        public static boolean heeft(Stuk stuk) {
            return !stuk.opSlot() || ONTGRENDELD.contains(stuk.id());
        }
    }
}
