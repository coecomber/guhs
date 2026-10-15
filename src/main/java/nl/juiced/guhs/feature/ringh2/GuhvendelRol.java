package nl.juiced.guhs.feature.ringh2;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.ring.Cast;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * bbq2 (ring-h2): what the characters of Guhvendel say (the NpcRollen plek {@link Guhvendel#PLEK}; texts
 * quest.guhs.ringh2.*: tools/features/ring_h2.py). One role per kind, the same entity for everybody, a different talk per
 * player: it looks at the player's own step of {@link RingH2Feature#LIJN}.
 * <ul>
 *   <li><b>Guhrond</b>: the welcome (step 1 -> 2), who is still to meet, "ring the bell", the question who carries the ring
 *       with its answer buttons (step 4: {@link Guhvendel#meldAan}), afterwards his ordinary lines.</li>
 *   <li><b>Guhdalf</b>: says what to do now; at the end "Op weg!" ({@link Guhvendel#vertrek}).</li>
 *   <li><b>The six</b>: the first talk is the meeting (a flag per player). Araguh first wants to see you sneak (crouch and
 *       click again), Merrie and Pippguh snack one kaasknabbel from your bag (once, when you have one). After the bell
 *       they sit in the council ring and bicker; after the fellowship is formed they are ready to go.</li>
 * </ul>
 * A player whose story is not here (not reached, or done) hears the character's default lines ({@link Cast.Rol}).
 */
public record GuhvendelRol(GuhNpcEntity.Kind kind) implements NpcRole {
    /** The answers of the two talking screens. */
    static final int JA = 1, NOG_NIET = 2;
    private static final String T = "quest.guhs.ringh2.";

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer p) {
        Verhaallijn lijn = RingH2Feature.LIJN;
        if (!lijn.aanDeBeurt(p) || lijn.klaar(p)) {
            new Cast.Rol(kind).talk(npc, p);
            return;
        }
        if (Ring.aanZet(p, lijn, Guhvendel.REIS)) {
            Guhvendel.aankomst(p);   // (whoever talks to somebody of the house has arrived)
        }
        geluid(npc);
        int stap = lijn.stap(p);
        switch (kind) {
            case GUHROND -> guhrond(npc, p, stap);
            case GUHDALF -> guhdalf(npc, p, stap);
            default -> gezel(npc, p, stap);
        }
    }

    @Override
    public void antwoord(GuhNpcEntity npc, ServerPlayer p, int optie) {
        Verhaallijn lijn = RingH2Feature.LIJN;
        if (kind == GuhNpcEntity.Kind.GUHROND && Ring.aanZet(p, lijn, Guhvendel.MELDEN)) {
            if (optie == JA) {
                Guhvendel.meldAan(p, npc);
            } else if (optie == NOG_NIET) {
                zeg(npc, p, "guhrond.nog_niet");
            }
        } else if (kind == GuhNpcEntity.Kind.GUHDALF && Ring.aanZet(p, lijn, Guhvendel.VERTREK)) {
            if (optie == JA) {
                Guhvendel.vertrek(p, npc);
            } else if (optie == NOG_NIET) {
                zeg(npc, p, "guhdalf.nog_niet");
            }
        }
    }

    // --- Guhrond ---------------------------------------------------------------------------------------------------------

    private void guhrond(GuhNpcEntity npc, ServerPlayer p, int stap) {
        Verhaallijn lijn = RingH2Feature.LIJN;
        switch (stap) {
            case Guhvendel.WELKOM -> {
                zeg(npc, p, "guhrond.welkom.0");
                zeg(npc, p, "guhrond.welkom.1");
                if (lijn.verder(p, Guhvendel.WELKOM) && !Guhvendel.kennisKlaar(p)) {
                    GuhQuests.hint(p, T + "guhrond.welkom.hint");
                }
            }
            case Guhvendel.KENNIS -> GuhQuests.say(p, npc, T + "guhrond.nog", Guhvendel.nogTeOntmoeten(p));
            case Guhvendel.RAADSBEL -> zeg(npc, p, "guhrond.bel");
            case Guhvendel.MELDEN -> Praat.open(p, npc, null, T + "guhrond.wie", new Object[0], new Praat.Optie(JA, "gui.guhs.ringh2.optie.ik"),
                    new Praat.Optie(NOG_NIET, "gui.guhs.ringh2.optie.nog_niet"));
            default -> zeg(npc, p, "guhrond.vertrek");
        }
    }

    // --- Guhdalf ---------------------------------------------------------------------------------------------------------

    private void guhdalf(GuhNpcEntity npc, ServerPlayer p, int stap) {
        Ring.behaald(p, "ring_guhdalf");
        switch (stap) {
            case Guhvendel.WELKOM, Guhvendel.KENNIS, Guhvendel.RAADSBEL -> {
                zeg(npc, p, "guhdalf.voor." + p.getRandom().nextInt(2));
                Ring.vertelDoel(p, npc);
            }
            case Guhvendel.MELDEN -> zeg(npc, p, "guhdalf.raad");
            default -> Praat.open(p, npc, null, T + "guhdalf.vertrek", new Object[0], new Praat.Optie(JA, "gui.guhs.ringh2.optie.op_weg"),
                    new Praat.Optie(NOG_NIET, "gui.guhs.ringh2.optie.rondkijken"));
        }
    }

    // --- the six ---------------------------------------------------------------------------------------------------------

    private void gezel(GuhNpcEntity npc, ServerPlayer p, int stap) {
        String wie = kind.id() + ".";
        if (stap >= Guhvendel.VERTREK) {
            zeg(npc, p, wie + "klaar");
            return;
        }
        if (stap == Guhvendel.MELDEN) {
            zeg(npc, p, wie + "raad");
            return;
        }
        if (Guhvendel.heeftOntmoet(p, kind)) {
            zeg(npc, p, wie + (stap == Guhvendel.RAADSBEL ? "bel" : "praat"));
            return;
        }
        Verhaallijn lijn = RingH2Feature.LIJN;
        if (kind == GuhNpcEntity.Kind.ARAGUH) {
            // the ranger's sneaking lesson: crouch, then click him again
            if (!p.isShiftKeyDown()) {
                zeg(npc, p, lijn.vlag(p, Guhvendel.SLUIPLES) ? "araguh.niet_gebukt" : "araguh.les");
                lijn.vlag(p, Guhvendel.SLUIPLES, true);
                return;
            }
        } else if ((kind == GuhNpcEntity.Kind.MERRIE || kind == GuhNpcEntity.Kind.PIPPGUH) && !lijn.vlag(p, Guhvendel.GESNOEPT)) {
            // they snack from your bag: one kaasknabbel, once, and only when there is one
            lijn.vlag(p, Guhvendel.GESNOEPT, true);
            if (GuhQuests.count(p, ModItems.KAAS_KNABBELS.get()) > 0) {
                GuhQuests.take(p, ModItems.KAAS_KNABBELS.get(), 1);
                zeg(npc, p, wie + "snoep");
            } else {
                zeg(npc, p, wie + "lege_tas");
            }
        }
        zeg(npc, p, wie + "kennis");
        Guhvendel.zetOntmoet(p, kind);
    }

    // --- small things ----------------------------------------------------------------------------------------------------

    private void zeg(GuhNpcEntity npc, ServerPlayer p, String key) {
        GuhQuests.say(p, npc, T + key);
    }

    private void geluid(GuhNpcEntity npc) {
        boolean mika = kind == GuhNpcEntity.Kind.BOROMIKA;
        npc.level().playSound(null, npc, mika ? ModSounds.MIKA_AMBIENT.get() : ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f,
                kind == GuhNpcEntity.Kind.GUHDALF ? 0.75f : kind == GuhNpcEntity.Kind.GUHROND ? 0.85f : 1f);
    }
}
