package nl.juiced.guhs.feature.vadsrijk.ambacht;

import static nl.juiced.guhs.entity.GuhClothes.MONOCLE;
import static nl.juiced.guhs.entity.GuhClothes.TIMMER_GEREEDSCHAPSRIEM;
import static nl.juiced.guhs.entity.GuhClothes.TIMMER_HELMPJE;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcDef;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldPlekken;

/**
 * 1.10.4: everything that lives on the Ambachtseiland (tools/vadsrijk/kamp/ambachtseiland.py; POIs {@code ambacht_*} in
 * WereldPlekken, area {@code ambachtseiland}), in one place so the parallel slice that turns gathering spots into real blocks never
 * meets it (only the public {@link AmbachtPlekken#groep}/{@link AmbachtPlekken#korfGroep}/{@link AmbachtPlekken#werkplaats} API):
 * <ul>
 *   <li>Meester Knutsel, the quest guh of the islet lesson (npc slice NPC on POI {@code ambacht_knutsel});</li>
 *   <li>a master per hut, exactly at its {@code .voor} POI, looking at the square ({@code ambacht_plein}): smid (Smederij), kok (Keuken),
 *   vis (Viswinkel), verzamel (Verzamelhuisje);</li>
 *   <li>the gathering groups, all T1 with a fast respawn (20-30 s): Kiezelgroeve (ore), Knuffelbosje (wood), Bloemenwei (herbs),
 *   Bijenhofje (hives: the T1 {@code klaverhoning}), Visvijver + Vissteiger (fish); per-player spots, enough for a few players at once;</li>
 *   <li>the islet's info boards ({@code guhs:infobord} without a species): a helpful text by the nearest place ({@link #bordTekst});</li>
 *   <li>and the hub masters of Verzamelen and Vissen in zone 2 (Kandijkom) and zone 3 (Honingtak: Imkerguh Zoemie), like Zwamzoet's.</li>
 * </ul>
 * Names and lines: tools/features/vadsrijk_ambacht_verhaal.py.
 */
public final class EilandRegistratie {
    private EilandRegistratie() {
    }

    public static final String KNUTSEL = "meester_knutsel";
    /** The centre of the islet's square (the hut masters look at it). */
    public static final String PLEIN = "ambacht_plein";
    /** The islet's own T1 honey (the Bijenhofje): an alternative ingredient in T1 Koken (pudding, energy potion). */
    public static final String HONING = "klaverhoning";
    /** Islet respawn (ONTWERP_1104 "Getallen": 20-30 s; elsewhere 60-120 s). */
    public static final int LEEG_MIN = 20, LEEG_MAX = 30;
    /** group id -> its POI (the islet's ".midden" spots). */
    public static final Map<String, String> GROEPEN = Map.of("eiland_groeve", "ambacht_groeve.midden", "eiland_bosje", "ambacht_bosje.midden",
            "eiland_bloemenwei", "ambacht_bloemenwei.midden", "eiland_bijenhofje", "ambacht_bijenhofje.midden", "eiland_visvijver",
            "ambacht_visvijver.midden", "eiland_vissteiger", "ambacht_vissteiger.midden");
    /** The hut masters: npc id -> its POI. */
    public static final Map<String, String> MEESTERS = Map.of("amb_smid_eiland", "ambacht_smederij.voor", "amb_kok_eiland", "ambacht_keuken.voor",
            "amb_vis_eiland", "ambacht_viswinkel.voor", "amb_verzamel_eiland", "ambacht_verzamelhuisje.voor");

    private static boolean klaar;

    static synchronized void registreer() {
        if (klaar) {
            return;
        }
        klaar = true;
        // Meester Knutsel (quest giver; the npc slice's keeper puts him in the world)
        BlockPos k = WereldPlekken.plek("ambacht_knutsel");
        if (k != null) {
            Npcs.voegToe(new NpcDef(KNUTSEL, k, 180, GuhVariant.GOLDEN, List.of(TIMMER_HELMPJE, MONOCLE, TIMMER_GEREEDSCHAPSRIEM), NpcDef.Gedrag.STAAN,
                    null, 1.1f));
        }
        // the hut masters
        AmbachtPlekken.werkplaats("amb_smid_eiland", 1, MEESTERS.get("amb_smid_eiland"), Ambacht.SMEDEN, true);
        AmbachtPlekken.werkplaats("amb_kok_eiland", 1, MEESTERS.get("amb_kok_eiland"), Ambacht.KOKEN, true);
        AmbachtPlekken.werkplaats("amb_vis_eiland", 1, MEESTERS.get("amb_vis_eiland"), Ambacht.VISSEN, true);
        AmbachtPlekken.werkplaats("amb_verzamel_eiland", 1, MEESTERS.get("amb_verzamel_eiland"), Ambacht.VERZAMELEN, true);
        for (String m : MEESTERS.keySet()) {
            AmbachtPlekken.kijk(m, PLEIN);   // (they face the square, not the wall of their hut)
        }
        // the gathering groups (spots in the terrain round each POI, found the soort's default way)
        AmbachtPlekken.groep("eiland_groeve", 1, GROEPEN.get("eiland_groeve"), NodeSoort.ERTS, 1, 10, 16, LEEG_MIN, LEEG_MAX);
        AmbachtPlekken.groep("eiland_bosje", 1, GROEPEN.get("eiland_bosje"), NodeSoort.HOUT, 1, 10, 16, LEEG_MIN, LEEG_MAX);
        AmbachtPlekken.groep("eiland_bloemenwei", 1, GROEPEN.get("eiland_bloemenwei"), NodeSoort.KRUID, 1, 10, 14, LEEG_MIN, LEEG_MAX);
        AmbachtPlekken.korfGroep("eiland_bijenhofje", 1, GROEPEN.get("eiland_bijenhofje"), 1, HONING, 4, 12);
        AmbachtPlekken.groep("eiland_visvijver", 1, GROEPEN.get("eiland_visvijver"), NodeSoort.VIS, 1, 6, 10, LEEG_MIN, LEEG_MAX);
        AmbachtPlekken.groep("eiland_vissteiger", 1, GROEPEN.get("eiland_vissteiger"), NodeSoort.VIS, 1, 5, 10, LEEG_MIN, LEEG_MAX);
        // zone 2 and 3: a Verzamelen and a Vissen master on the hub square, like Zwamzoet's (Madelief, Dobbertje)
        AmbachtPlekken.werkplaats("amb_verzamel_z2", 2, "plein", Ambacht.VERZAMELEN, false);
        AmbachtPlekken.werkplaats("amb_vis_z2", 2, "plein", Ambacht.VISSEN, false);
        AmbachtPlekken.werkplaats("amb_verzamel_z3", 3, "markt", Ambacht.VERZAMELEN, false);   // (Imkerguh Zoemie)
        AmbachtPlekken.werkplaats("amb_vis_z3", 3, "markt", Ambacht.VISSEN, false);
        // the info boards
        nl.juiced.guhs.feature.safari.kriebel.InfobordBlockEntity.LEEG_TEKST.add(EilandRegistratie::bordTekst);
    }

    // ---- the info boards -------------------------------------------------------------------------------------------------------------

    /** Board POIs -> text id ({@code vadsrijk.ambacht.eiland.bord.<id>}); a board further than {@link #BORD_BIJ} from all = a signpost. */
    static final Map<String, String> BORD_PLEKKEN = Map.of("ambacht_brug_eiland", "welkom", "ambacht_knutsel", "knutsel",
            "ambacht_smederij.deur", "smederij", "ambacht_keuken.deur", "keuken", "ambacht_viswinkel.deur", "viswinkel",
            "ambacht_verzamelhuisje.deur", "verzamelhuisje");
    static final int BORD_BIJ = 9;

    /**
     * An info board without a species on the islet: {name key, text key} by the nearest place (the welcome arch, Knutsel's notice
     * board, a hut's board, else a signpost); null elsewhere (the safari's own "waiting for an animal" stays).
     */
    @Nullable
    public static String[] bordTekst(ServerPlayer p, BlockPos pos) {
        Gebieden.Gebied g = Gebieden.op(pos.getX(), pos.getY(), pos.getZ());
        if (g == null || !g.id().equals("ambachtseiland")) {
            return null;
        }
        return new String[] {"vadsrijk.ambacht.eiland.bord.naam", "vadsrijk.ambacht.eiland.bord." + bordId(pos)};
    }

    /** The board text id for a spot on the islet. */
    public static String bordId(BlockPos pos) {
        String beste = "wegwijzer";
        double bd = BORD_BIJ * BORD_BIJ;
        for (Map.Entry<String, String> e : BORD_PLEKKEN.entrySet()) {
            BlockPos b = WereldPlekken.plek(e.getKey());
            if (b == null) {
                continue;
            }
            double dx = b.getX() - pos.getX(), dz = b.getZ() - pos.getZ();
            double d = dx * dx + dz * dz;
            if (d < bd) {
                bd = d;
                beste = e.getValue();
            }
        }
        return beste;
    }
}
