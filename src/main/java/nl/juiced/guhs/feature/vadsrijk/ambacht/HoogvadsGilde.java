package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;

/**
 * T-INT: Hoogvads is the professions capital. t-z5verhaal's Ambachtsgilde teachers (z5_leermeester_*) open t-ambacht's
 * workshop of their craft from their talk menu ("Werkplaats"), and the Gildemeester opens the Ambachtenboek ("Receptenboek").
 * <p>U-AMBACHT2: the Gilde is where the professions grow on: the T4/T5 recipes are made here ({@link Werkplaats#magHier}), and
 * the story goes on here after the five steps of the hub teachers: two steps per craft ({@link #LEVELS}: 40 and 48 now on the
 * 1.10.4 tier scale; 54 and 60 = zone 6 volgt). 1.10.4 (9 -> 4): the Koken and Smeden teachers tell their craft's story, the
 * Gildemeester tells Verzamelen's and Vissen's; the Brouwen and Naaien teachers open the Koken / Smeden workshop, the imker the
 * Verzamelen book. Choice "Vertel eens..." on the story guhs.
 */
public final class HoogvadsGilde {
    /** The teachers whose workbench opens (1.10.4: brouwen -> Koken, naaien -> Smeden). */
    private static final Map<String, Ambacht> LEERMEESTERS = Map.of(
            "z5_leermeester_koken", Ambacht.KOKEN, "z5_leermeester_brouwen", Ambacht.KOKEN, "z5_leermeester_smeden", Ambacht.SMEDEN,
            "z5_leermeester_naaien", Ambacht.SMEDEN);
    /** The teachers that tell their craft's Gilde story (one per making craft). */
    private static final Map<String, Ambacht> VERTELLERS = Map.of("z5_leermeester_koken", Ambacht.KOKEN, "z5_leermeester_smeden", Ambacht.SMEDEN);
    /**
     * The Gilde story steps (step 5, 6, ...): the ambacht level each needs (1.10.4: the old 55/60/65/70 on the new tier scale).
     * The last two = zone 6 volgt ({@link #stappen}).
     */
    public static final int[] LEVELS = {40, 48, 54, 60};
    static final List<Ambacht> VERZAMELAARS = List.of(Ambacht.VERZAMELEN, Ambacht.VISSEN);

    private HoogvadsGilde() {
    }

    /**
     * Lampionhaven's makers (zone 4: T1-T4, {@link Werkplaats#magHier}): Werkmeester Krul's workshop = the smithy, Kok Garnaal's kitchen in
     * visrestaurant De Blije Bot = the kitchen (1.10.22: before, zone 4 had no place to cook T4).
     */
    public static final Map<String, Ambacht> LAMPIONHAVEN = Map.of("z4_werkplaats", Ambacht.SMEDEN, "z4_kok", Ambacht.KOKEN);

    static void koppel() {
        // T-INT: t-z4verhaal's Lampionhaven workshop NPC (role "werkplaats", a carpenter with a tool belt) = the smithy
        nl.juiced.guhs.feature.vadsrijk.npc.NpcRollen.koppel("werkplaats", p -> AmbachtSchermen.werkplaats(p, Ambacht.SMEDEN, "z4_werkplaats"));
        // 1.10.22: Kok Garnaal (role "z4_vissoep": his soup stays) also opens his kitchen = the Koken workshop
        optie("z4_kok", new QuestMotor.ExtraOptie("vadsrijk.ambacht.optie.keuken", p -> true, p -> AmbachtSchermen.werkplaats(p, Ambacht.KOKEN, "z4_kok")));
        LEERMEESTERS.forEach((npc, a) -> optie(npc, new QuestMotor.ExtraOptie("vadsrijk.ambacht.optie.werk", p -> true,
                p -> AmbachtSchermen.werkplaats(p, a, npc))));
        optie("z5_gildemeester", new QuestMotor.ExtraOptie("vadsrijk.ambacht.optie.boek", p -> true,
                p -> AmbachtSchermen.boek(p, "overzicht", null)));
        // (imkeren is a gathering craft: the imker opens the book on beekeeping instead of a workbench)
        optie("z5_leermeester_imkeren", new QuestMotor.ExtraOptie("vadsrijk.ambacht.optie.boek", p -> true,
                p -> AmbachtSchermen.boek(p, "recepten", Ambacht.VERZAMELEN)));
        // U-AMBACHT2: the Gilde story steps
        VERTELLERS.forEach((npc, a) -> optie(npc, new QuestMotor.ExtraOptie("vadsrijk.ambacht.optie.verhaal", p -> true,
                p -> verhaal(p, npc, List.of(a)))));
        optie("z5_gildemeester", new QuestMotor.ExtraOptie("vadsrijk.ambacht.optie.verhaal", p -> true,
                p -> verhaal(p, "z5_gildemeester", VERZAMELAARS)));
    }

    private static void optie(String npc, QuestMotor.ExtraOptie o) {
        QuestMotor.EXTRA.computeIfAbsent(npc, k -> new java.util.concurrent.CopyOnWriteArrayList<>()).add(o);
    }

    /**
     * How many Gilde steps exist now: 2 (zones 4 + 5). 1.10.4: T6 is on, but the steps at 54/60 have no texts yet: they stay off
     * until they are written ({@link #MET_ZONE6}).
     */
    public static int stappen() {
        return MET_ZONE6 ? LEVELS.length : 2;
    }

    /** The Gilde steps 54/60 (zone 6) are written and switched on. */
    static final boolean MET_ZONE6 = false;

    /** All story steps of an ambacht now (the hub teachers' 5 + the Gilde's). */
    public static int totaal() {
        return AmbachtNpcs.VERHAAL_LEVELS.length + stappen();
    }

    /** The Gilde guh standing next to the player (for the portrait), or null (then the pages show its name). */
    @Nullable
    static Entity spreker(ServerPlayer p, String npc) {
        List<RijkGuh> l = p.level().getEntitiesOfClass(RijkGuh.class, p.getBoundingBox().inflate(12), g -> npc.equals(g.npcId()));
        return l.isEmpty() ? null : l.get(0);
    }

    /**
     * Tells the next Gilde step of the first of these ambachten that has one ready; otherwise says what is needed (first the hub
     * teacher's five steps, or a level). Returns the ambacht told about (null: nothing told). Public for the dev command/tests.
     */
    @Nullable
    public static Ambacht verhaal(ServerPlayer p, String npc, List<Ambacht> ambachten) {
        Entity spreker = spreker(p, npc);
        String naam = spreker == null ? "vadsrijk.npc.naam." + npc : "";
        int basis = AmbachtNpcs.VERHAAL_LEVELS.length;
        List<Praat.Regel> regels = new ArrayList<>();
        Ambacht eerst = null, wacht = null;
        int wachtLevel = Integer.MAX_VALUE;
        for (Ambacht a : ambachten) {
            int stap = AmbachtData.verhaal(p, a);
            if (stap < basis) {
                eerst = eerst == null ? a : eerst;
                continue;
            }
            int i = stap - basis;
            if (i >= stappen()) {
                continue;
            }
            if (AmbachtData.level(p, a) < LEVELS[i]) {
                if (LEVELS[i] < wachtLevel) {
                    wachtLevel = LEVELS[i];
                    wacht = a;
                }
                continue;
            }
            String k = "vadsrijk.ambacht.gilde." + a.id + "." + i + ".";
            for (int r = 0; r < 3; r++) {
                regels.add(new Praat.Regel(r == 1 ? p : spreker, r == 1 ? "vadsrijk.npc.jij" : naam, k + r));
            }
            Praat.scene(p, AmbachtNpcs.SLEUTEL, regels);
            AmbachtNpcs.beloon(p, a, stap);
            return a;
        }
        if (wacht != null) {
            regels.add(new Praat.Regel(spreker, naam, "vadsrijk.ambacht.verhaal.wacht", wachtLevel, Component.translatable(wacht.naamKey())));
        } else if (eerst != null) {
            regels.add(new Praat.Regel(spreker, naam, "vadsrijk.ambacht.gilde.eerst", Component.translatable(eerst.naamKey())));
        } else {
            regels.add(new Praat.Regel(spreker, naam, "vadsrijk.ambacht.gilde.uit"));
        }
        Praat.scene(p, AmbachtNpcs.SLEUTEL, regels);
        return null;
    }
}
