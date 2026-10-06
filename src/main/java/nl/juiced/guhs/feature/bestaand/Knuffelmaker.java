package nl.juiced.guhs.feature.bestaand;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (bestaand): the captive Knuffelmaker-guh in his naaihoek on the first floor of the Mika-grillpaleis, and his
 * questline "De gestolen knuffels" ({@link BestaandFeature#KNUFFELMAKER}). One NPC for everybody, a talk per player:
 * <ol start="0">
 *   <li>he whispers about his stolen plush guhs (and why he does not simply walk out of his open cage);</li>
 *   <li>the three cages ({@link Kooien}: with vads or by sneaking); he counts along, and counts a cage that somebody broke
 *       in this copy as freed;</li>
 *   <li>the Mikas pulled the seams loose: he wants {@link BestaandFeature#DRAAD} thread, then the reward, once per player:
 *       the knuffelpatroon and one of each plush.</li>
 * </ol>
 * Afterwards he chats (he stays: "de Mika's hebben ook een knuffel nodig"), and gives the patroon again when it was lost.
 */
final class Knuffelmaker extends QuestRol {
    private static final String Q = "quest.guhs.bestaand.knuffelmaker.";
    private static final int HELP = 1, WAAROM = 2;

    Knuffelmaker() {
        super(BestaandFeature.KNUFFELMAKER);
    }

    @Override
    protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
        switch (stap) {
            case 0 -> {
                BestaandFeature.KNUFFELMAKER.begin(p);
                scherm(p, npc, Q + "hallo", new Praat.Optie(HELP, Q + "optie.help"), new Praat.Optie(WAAROM, Q + "optie.waarom"));
            }
            case BestaandFeature.KNUFFELMAKER_KOOIEN -> kooien(npc, p);
            case BestaandFeature.KNUFFELMAKER_DRAAD -> draad(npc, p);
            default -> {
                if (geefAlsKwijt(p, BestaandFeature.RECEPT_KNUFFEL.get())) {
                    zeg(p, npc, Q + "patroon_kwijt");
                } else {
                    zeg(p, npc, Q + "bedankt" + p.getRandom().nextInt(3));
                }
            }
        }
    }

    @Override
    protected void antwoord(GuhNpcEntity npc, ServerPlayer p, int stap, int optie) {
        if (stap == 0 && optie == WAAROM) {
            scherm(p, npc, Q + "waarom", new Praat.Optie(HELP, Q + "optie.help"));
        } else if (stap == 0 && optie == HELP && verder(p, 0)) {
            zeg(p, npc, Q + "start");
            hint(p, Q + "hint.kooien");
        }
    }

    /** At the cages step: what is still to do. */
    private void kooien(GuhNpcEntity npc, ServerPlayer p) {
        if (meetellen(npc, p)) {
            zeg(p, npc, Q + "kooi_weg");
            Schijn.ververs(p);
        }
        if (Kooien.klaar(p)) {
            draad(npc, p);
            return;
        }
        zeg(p, npc, Q + "kooien_nog", Kooien.AANTAL - Kooien.aantal(p), Kooien.AANTAL);
        hint(p, Q + "hint.kooien");
    }

    /** At the thread step: enough thread = done, with the reward. */
    private void draad(GuhNpcEntity npc, ServerPlayer p) {
        int heeft = GuhQuests.count(p, Items.STRING);
        if (heeft < BestaandFeature.DRAAD) {
            if (heeft == 0) {
                zeg(p, npc, Q + "draad", BestaandFeature.DRAAD);
            } else {
                zeg(p, npc, Q + "draad_tekort", heeft, BestaandFeature.DRAAD);
            }
            hint(p, Q + "hint.draad");
        } else if (lever(p, npc, BestaandFeature.KNUFFELMAKER_DRAAD, Items.STRING, BestaandFeature.DRAAD, Q + "draad_tekort")) {
            zeg(p, npc, Q + "klaar");
            geefEenmalig(p, "beloning", new ItemStack(BestaandFeature.RECEPT_KNUFFEL.get()), new ItemStack(BestaandFeature.KNUFFELGUH_ITEM.get()),
                    new ItemStack(BestaandFeature.KNUFFELMIKA_ITEM.get()), new ItemStack(BestaandFeature.KNUFFELROOKGUH_ITEM.get()));
            zichtbaar(p, "barbecuether/bestaand_knuffelmaker");
        }
    }

    /**
     * A cage of this copy whose plush is gone (somebody broke the cage open for real) can't be opened: it counts as freed
     * for this player. True when one was counted now.
     */
    private static boolean meetellen(GuhNpcEntity npc, ServerPlayer p) {
        ServerLevel level = p.level();
        StructureStart start = Bezetting.start(level, BestaandFeature.GRILLPALEIS, npc.blockPosition());
        if (start == null) {
            return false;
        }
        boolean geteld = false;
        for (int i = 0; i < Kooien.AANTAL; i++) {
            if (!Kooien.vrij(p, i) && !Kooien.knuffelZitEr(level, start, i)) {
                BestaandFeature.KNUFFELMAKER.vlag(p, "kooi_" + i, true);
                geteld = true;
            }
        }
        return geteld;
    }
}
