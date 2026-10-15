package nl.juiced.guhs.feature.bio.kompas;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.feature.bio.Bezocht;
import nl.juiced.guhs.feature.bio.BiomeLijst;
import nl.juiced.guhs.item.SuperkompasItem;

/**
 * biomes3: what the Superkompas tab Biomes shows to a player, as plain data (both sides; the client draws it, the server
 * checks a choice against it, the tests read it): the sections of {@link BiomeLijst} in order, each open, closed or
 * locked, and under an open one its biomes with their "been there" tick.
 * <p>
 * A section is locked until the player has ever been in its dimension ({@link Bezocht}); a section without a dimension
 * never opens, whatever the visited list says, and lists nothing.
 */
public final class BiomesMenu {
    /**
     * The tab itself. It is NOT in {@link SuperkompasItem#CATEGORIES} (that list holds structures only): the screen draws
     * it after the category tabs. Lang gui.guhs.superkompas.biomes (+ .tooltip).
     */
    public static final SuperkompasItem.Category CATEGORIE = new SuperkompasItem.Category("biomes", "guhs:guhbloesem_sapling", Items.CHERRY_SAPLING, List.of());

    /** A row of the tab. */
    public sealed interface Rij permits SectieRij, BiomeRij {
    }

    /** A section's heading: locked (with a padlock), or open/closed with how many of its biomes the player has been in. */
    public record SectieRij(String id, boolean opSlot, boolean open, int bezocht, int totaal) implements Rij {
    }

    /** A biome of an open section. */
    public record BiomeRij(String sectie, String biome, boolean bezocht) implements Rij {
    }

    /** Is this section locked for this player (both sides)? */
    public static boolean opSlot(Player player, BiomeLijst.Sectie sectie) {
        return sectie.teaser() || sectie.dimensie() == null || !Bezocht.dimensie(player, sectie.id());
    }

    /** The section that lists this biome (guhs id without namespace), or null. */
    @Nullable
    public static BiomeLijst.Sectie sectieVan(String biome) {
        for (BiomeLijst.Sectie s : BiomeLijst.secties()) {
            if (BiomeLijst.heeft(s.id(), biome)) {
                return s;
            }
        }
        return null;
    }

    /** May this player point the compass at this biome: it is listed, and its section is not locked. */
    public static boolean magKiezen(Player player, String biome) {
        BiomeLijst.Sectie s = sectieVan(biome);
        return s != null && !opSlot(player, s);
    }

    /** The rows for this player; {@code open} says which unlocked sections are folded open. */
    public static List<Rij> rijen(Player player, Predicate<String> open) {
        List<Rij> uit = new ArrayList<>();
        for (BiomeLijst.Sectie s : BiomeLijst.secties()) {
            if (opSlot(player, s)) {
                uit.add(new SectieRij(s.id(), true, false, 0, 0));
                continue;
            }
            List<String> biomes = BiomeLijst.biomes(s.id());
            int bezocht = (int) biomes.stream().filter(b -> Bezocht.biome(player, b)).count();
            boolean isOpen = open.test(s.id());
            uit.add(new SectieRij(s.id(), false, isOpen, bezocht, biomes.size()));
            if (isOpen) {
                for (String b : biomes) {
                    uit.add(new BiomeRij(s.id(), b, Bezocht.biome(player, b)));
                }
            }
        }
        return uit;
    }

    private BiomesMenu() {
    }
}
