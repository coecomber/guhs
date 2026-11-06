package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.Map;

import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;

/**
 * T-INT: Hoogvads is the professions capital. t-z5verhaal's Ambachtsgilde teachers (z5_leermeester_*) open t-ambacht's
 * workshop of their craft from their talk menu ("Werkplaats"), and the Gildemeester opens the Ambachtenboek ("Receptenboek").
 */
public final class HoogvadsGilde {
    private static final Map<String, Ambacht> LEERMEESTERS = Map.of(
            "z5_leermeester_koken", Ambacht.KOKEN, "z5_leermeester_brouwen", Ambacht.BROUWEN, "z5_leermeester_smeden", Ambacht.SMEDEN,
            "z5_leermeester_naaien", Ambacht.NAAIEN);

    private HoogvadsGilde() {
    }

    static void koppel() {
        // T-INT: t-z4verhaal's Lampionhaven workshop NPC (role "werkplaats", a carpenter with a tool belt) = the smithy
        nl.juiced.guhs.feature.vadsrijk.npc.NpcRollen.koppel("werkplaats", p -> AmbachtSchermen.werkplaats(p, Ambacht.SMEDEN, "z4_werkplaats"));
        LEERMEESTERS.forEach((npc, a) -> optie(npc, new QuestMotor.ExtraOptie("vadsrijk.ambacht.optie.werk", p -> true,
                p -> AmbachtSchermen.werkplaats(p, a, npc))));
        optie("z5_gildemeester", new QuestMotor.ExtraOptie("vadsrijk.ambacht.optie.boek", p -> true,
                p -> AmbachtSchermen.boek(p, "overzicht", null)));
        // (imkeren is a gathering craft: the imker opens the book on beekeeping instead of a workbench)
        optie("z5_leermeester_imkeren", new QuestMotor.ExtraOptie("vadsrijk.ambacht.optie.boek", p -> true,
                p -> AmbachtSchermen.boek(p, "recepten", Ambacht.IMKEREN)));
    }

    private static void optie(String npc, QuestMotor.ExtraOptie o) {
        QuestMotor.EXTRA.computeIfAbsent(npc, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(o);
    }
}
