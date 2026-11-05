package nl.juiced.guhs.feature.vadsrijk.puzzels;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Categorie;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Ding;
import nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.Staat;

/**
 * T-PUZZELS: the Verkenningsboek gets a category "Puzzels en geheimpjes" on the page of zone 1, 2 and 3 (counted in the zone
 * percentage). Puzzles show their name; secrets are "???" until found. The hint is the puzzle's own clue line
 * ("Bij het eerste kampvuur van de Klodderweide...").
 */
public final class PuzzelBoek {
    public static final String CAT = "puzzel";

    private PuzzelBoek() {
    }

    static void registreer() {
        for (int zone = 1; zone <= 3; zone++) {
            int z = zone;
            Voltooiing.extraCategorie("zone" + zone, () -> categorie(z));
        }
    }

    /** After (re)loading the data: the pages are built again with the new list. */
    static void herbouw() {
        for (int zone = 1; zone <= 3; zone++) {
            Voltooiing.herbouw("zone" + zone);
        }
    }

    static Categorie categorie(int zone) {
        List<Ding> l = new ArrayList<>();
        for (PuzzelDef d : Puzzels.defs()) {
            if (d.zone() != zone) {
                continue;
            }
            BlockPos plek = d.anker().punt();
            l.add(new Ding("pz_" + d.id(), d.naamKey(), "", "vadsrijk.puzzels.boek.hint", d.key("aanwijzing"), d.stil(),
                    p -> PuzzelSpeler.opgelost(p, d.id()) ? Staat.KLAAR : Staat.NIETS, plek));
        }
        return new Categorie(CAT, "vadsrijk.puzzels.boek.cat", "minecraft:amethyst_shard", l);
    }
}
