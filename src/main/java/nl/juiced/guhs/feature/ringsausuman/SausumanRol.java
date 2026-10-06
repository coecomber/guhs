package nl.juiced.guhs.feature.ringsausuman;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.ring.Ring;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.QuestRol;

/**
 * Sausuman van de Vele Sauzen (kind SAUSUMAN), the wizard-Mika of the black tower: the questline
 * {@link RingSausumanFeature#LIJN}, per player. He also wants a bite of the Knabbelring.
 * <ol start="0">
 *   <li>Talk: he asks for a bite. Whatever the player answers, he does not get one, so he will bake a ring of his own.</li>
 *   <li>Fetch the three ingredients from the stations upstairs ({@link Bakkerij#klikVoorraad}); he says which are missing.</li>
 *   <li>Pull the lever of the Ringenbakker ({@link Bakkerij#klikBakker}): the baking scene. It bakes an onion ring.</li>
 *   <li>He sulks in his Mokhoek. Offer him a bite of the onion ring (or own up to having eaten it): the Pannantir and a few
 *       onion rings, and he goes on sulking anyway.</li>
 * </ol>
 * Afterwards he explains, grudgingly, why none of his machines work (the nod to Guh-technologie: five machines on one Mika-rad
 * is too heavy, and a Guhrad wants a guh) and that the lever bakes one onion ring a day. A player whose story has not reached
 * the tower (a creative visitor) is sent away.
 */
public final class SausumanRol extends QuestRol {
    private static final String T = "quest.guhs.ringsausuman.sausuman.";
    private static final int NEE = 1, DELEN = 2, HAPJE = 3, WEG = 4, MACHINES = 5, NOG_EEN = 6;
    /** What he hands over at the end, besides the Pannantir. */
    public static final int UIENRINGEN = 4;
    private static final Verhaallijn LIJN = RingSausumanFeature.LIJN;

    public SausumanRol() {
        super(RingSausumanFeature.LIJN);
    }

    @Override
    protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
        if (!LIJN.aanDeBeurt(p)) {
            zeg(p, npc, T + "te_vroeg");
            return;
        }
        switch (stap) {
            case 0 -> {
                LIJN.begin(p);
                // (with the ring in the story: he smells it; after the story: he hears it has been eaten without him)
                scherm(p, npc, T + (Ring.klaar(p) ? "hallo_op" : "hallo"), new Praat.Optie(NEE, "gui.guhs.ringsausuman.optie.nee"),
                        new Praat.Optie(DELEN, "gui.guhs.ringsausuman.optie.delen"));
            }
            case 1 -> {
                zeg(p, npc, T + "ingredienten", Ingredient.values().length - Bakkerij.aantal(p));
                hint(p, "quest.guhs.ringsausuman.hint.ingredienten");
            }
            case 2 -> {
                zeg(p, npc, T + "hendel");
                hint(p, "quest.guhs.ringsausuman.hint.hendel");
            }
            case 3 -> scherm(p, npc, T + "mok", new Praat.Optie(HAPJE, "gui.guhs.ringsausuman.optie.hapje"),
                    new Praat.Optie(WEG, "gui.guhs.ringsausuman.optie.weg"));
            default -> scherm(p, npc, T + "na", new Praat.Optie(MACHINES, "gui.guhs.ringsausuman.optie.machines"),
                    new Praat.Optie(NOG_EEN, "gui.guhs.ringsausuman.optie.nog_een"));
        }
    }

    @Override
    protected void antwoord(GuhNpcEntity npc, ServerPlayer p, int stap, int optie) {
        if (!LIJN.aanDeBeurt(p)) {
            return;
        }
        if (stap == 0 && (optie == NEE || optie == DELEN)) {
            if (verder(p, 0)) {
                zeg(p, npc, T + (optie == NEE ? "zelf_nee" : "zelf_delen"));
                zeg(p, npc, T + "opdracht");
                hint(p, "quest.guhs.ringsausuman.hint.ingredienten");
            }
        } else if (stap == 3 && optie == HAPJE) {
            // a bite of the onion ring; whoever ate it already owns up, and he sulks twice as hard
            boolean hapje = neem(p, RingSausumanFeature.UIENRING.get(), 1);
            if (verder(p, 3)) {
                zeg(p, npc, T + (hapje ? "hapje" : "opgegeten"));
                geefEenmalig(p, "beloning", new ItemStack(RingSausumanFeature.PANNANTIR_ITEM.get()),
                        new ItemStack(RingSausumanFeature.UIENRING.get(), UIENRINGEN));
                Ring.behaald(p, "ring_sausuman_mok");
                npc.level().playSound(null, npc.blockPosition(), RingSausumanFeature.MOK.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
                hint(p, "quest.guhs.ringsausuman.hint.klaar");
            }
        } else if (stap == 3 && optie == WEG) {
            zeg(p, npc, T + "weg");
        } else if (stap >= LIJN.stappen() && optie == MACHINES) {
            zeg(p, npc, T + "te_zwaar", MikaradBlock.VERMOGEN);
        } else if (stap >= LIJN.stappen() && optie == NOG_EEN) {
            zeg(p, npc, T + (LIJN.teller(p, Bakkerij.RING_DAG) == Bakkerij.dag(p) ? "nog_een_morgen" : "nog_een"));
        }
    }
}
