package nl.juiced.guhs.feature.bestaand;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * bbq2 (bestaand): the Wachter-guh in his wachthokje next to the statue of the Spiesburcht, and his questline "De wacht
 * bij de Spiesburcht" ({@link BestaandFeature#WACHTER}). One NPC for everybody, a talk per player:
 * <ol start="0">
 *   <li>he tells about the bridge fires that the Vonk-Mikas blew out and lends his aansteekspies;</li>
 *   <li>the four fires ({@link Vuren}); he counts along, lends a new spies when it was lost, and counts a fire bowl that is
 *       gone from this copy (somebody built over its spots) as lit;</li>
 *   <li>back with him: he asks to weed his tuintje;</li>
 *   <li>the six tufts of Mikakruid ({@link Tuintje});</li>
 *   <li>the reward, once per player: the lantaarnrecept, two Zielige lantaarntjes and the wachterspak. He takes his spies
 *       back.</li>
 * </ol>
 * Afterwards he chats, and gives the recipe card again when it was lost.
 */
final class Wachter extends QuestRol {
    private static final String Q = "quest.guhs.bestaand.wachter.";
    private static final int HELP = 1, WAT = 2, WIEDEN = 3, DANK = 4;

    Wachter() {
        super(BestaandFeature.WACHTER);
    }

    @Override
    protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
        switch (stap) {
            case 0 -> {
                BestaandFeature.WACHTER.begin(p);
                scherm(p, npc, Q + "hallo", new Praat.Optie(HELP, Q + "optie.help"), new Praat.Optie(WAT, Q + "optie.wat"));
            }
            case BestaandFeature.WACHTER_VUREN -> vuren(npc, p);
            case BestaandFeature.WACHTER_MELDEN -> scherm(p, npc, Q + "vuren_klaar", new Praat.Optie(WIEDEN, Q + "optie.wieden"));
            case BestaandFeature.WACHTER_TUIN -> {
                zeg(p, npc, Q + "tuin_nog", Tuintje.AANTAL - Tuintje.aantal(p));
                hint(p, Q + "hint.tuin");
            }
            case BestaandFeature.WACHTER_BELONING -> scherm(p, npc, Q + "klaar", new Praat.Optie(DANK, Q + "optie.dank"));
            default -> {
                if (geefAlsKwijt(p, BestaandFeature.RECEPT_LANTAARN.get())) {
                    zeg(p, npc, Q + "recept_kwijt");
                } else {
                    zeg(p, npc, Q + "bedankt" + p.getRandom().nextInt(3));
                }
            }
        }
    }

    @Override
    protected void antwoord(GuhNpcEntity npc, ServerPlayer p, int stap, int optie) {
        if (stap == 0 && optie == WAT) {
            scherm(p, npc, Q + "wat", new Praat.Optie(HELP, Q + "optie.help"));
        } else if (stap == 0 && optie == HELP && verder(p, 0)) {
            geefAlsKwijt(p, BestaandFeature.AANSTEEKSPIES.get());
            zeg(p, npc, Q + "start");
            hint(p, Q + "hint.vuren");
        } else if (stap == BestaandFeature.WACHTER_MELDEN && optie == WIEDEN && verder(p, BestaandFeature.WACHTER_MELDEN)) {
            zeg(p, npc, Q + "tuin_start", Tuintje.AANTAL);
            hint(p, Q + "hint.tuin");
            Schijn.ververs(p);   // (the weeds are there at once)
        } else if (stap == BestaandFeature.WACHTER_BELONING && optie == DANK && verder(p, BestaandFeature.WACHTER_BELONING)) {
            GuhQuests.take(p, BestaandFeature.AANSTEEKSPIES.get(), GuhQuests.count(p, BestaandFeature.AANSTEEKSPIES.get()));
            geefEenmalig(p, "beloning", new ItemStack(BestaandFeature.RECEPT_LANTAARN.get()), new ItemStack(BestaandFeature.ZIELIG_LANTAARNTJE_ITEM.get(), 2),
                    new ItemStack(ModItems.clothingItem(GuhClothes.BESTAAND_WACHTERSHELM)), new ItemStack(ModItems.clothingItem(GuhClothes.BESTAAND_WACHTERSMANTEL)));
            zichtbaar(p, "barbecuether/bestaand_wachter");
        }
    }

    /** At the fires step: what is still to do. */
    private void vuren(GuhNpcEntity npc, ServerPlayer p) {
        if (meetellen(npc, p)) {
            zeg(p, npc, Q + "brug_dicht");
        }
        if (Vuren.klaar(p)) {
            praat(npc, p, BestaandFeature.WACHTER_MELDEN);
            return;
        }
        if (geefAlsKwijt(p, BestaandFeature.AANSTEEKSPIES.get())) {
            zeg(p, npc, Q + "spies_kwijt");
        }
        zeg(p, npc, Q + "vuren_nog", Vuren.AANTAL - Vuren.aantal(p), Vuren.AANTAL);
        hint(p, Q + "hint.vuren");
    }

    /**
     * A bridge of this copy whose fire bowl could not be placed (every spot was built over), or is gone, can't be lit: it
     * counts as lit for this player. True when one was counted now.
     */
    private static boolean meetellen(GuhNpcEntity npc, ServerPlayer p) {
        ServerLevel level = p.level();
        StructureStart start = Bezetting.start(level, BestaandFeature.SPIESBURCHT, npc.blockPosition());
        if (start == null) {
            return false;
        }
        Bezetting.Geplaatst data = Bezetting.Geplaatst.get(level);
        boolean geteld = false;
        for (int nr = 0; nr < Vuren.AANTAL; nr++) {
            String id = BestaandFeature.BRUGVUUR_ID + nr;
            if (Vuren.brandt(p, nr) || !data.gehad(id, start)) {
                continue;   // (not handled yet: its chunks were never loaded with a player near, the bowl will come)
            }
            Optional<BlockPos> sokkel = data.plek(id, start);
            boolean weg = sokkel.isEmpty() || level.isLoaded(sokkel.get()) && !level.getBlockState(sokkel.get().above(Vuren.KORF_HOOGTE)).is(BestaandFeature.VUURKORF.get());
            if (weg) {
                BestaandFeature.WACHTER.vlag(p, "vuur_" + nr, true);
                geteld = true;
            }
        }
        return geteld;
    }
}
