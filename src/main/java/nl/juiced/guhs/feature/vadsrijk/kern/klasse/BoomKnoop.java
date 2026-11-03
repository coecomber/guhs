package nl.juiced.guhs.feature.vadsrijk.kern.klasse;

import java.util.List;

import javax.annotation.Nullable;

import nl.juiced.guhs.feature.vadsrijk.kern.Stats;

/**
 * A skill tree node: costs {@code kosten} skill points, needs all of {@code vereist}, sits at (rij, kolom) in the tree screen,
 * gives a stat bonus and/or turns on a skill variant flag ({@code variant}, read by the skill with
 * {@code Spelers.heeftKnoop(p, id)}).
 */
public record BoomKnoop(String id, String klasse, int kosten, List<String> vereist, int rij, int kolom, Stats bonus, @Nullable String variant) {
    public String naamKey() {
        return "vadsrijk.klassen.knoop." + id;
    }
}
