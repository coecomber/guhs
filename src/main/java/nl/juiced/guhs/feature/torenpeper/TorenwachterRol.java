package nl.juiced.guhs.feature.torenpeper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.techbezorg.TechbezorgFeature;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * De Torenwachter-guh of the Rookguh-vuurtoren (kind TORENWACHTERGUH): the questline {@link TorenpeperFeature#VUURTOREN}, per
 * player.
 * <ol start="0">
 *   <li>Talk: the lamp is out and the Rookguhs can't find home.</li>
 *   <li>Bring {@link Vuurtoren#GRUIS_NODIG} gloeikoolgruis: he presses a lampkooltje from it and lends his seinlantaarn.</li>
 *   <li>Climb the tower and light the lamp with the lampkooltje ({@link Vuurtoren#klikLamp}); he gives a new one when it got
 *       lost.</li>
 *   <li>Guide {@link Vuurtoren#ROOKGUHS} lost Rookguhs to the light with the seinlantaarn (a new lantern when it got lost).</li>
 *   <li>Come back: the Bezorgguhtje-fluitje (the item of the tech-bezorg slice) and the keeper's coat for your guh.</li>
 * </ol>
 * Afterwards he chats, and once a day gives a new whistle to somebody who lost theirs (it has no recipe).
 */
public final class TorenwachterRol extends QuestRol {
    private static final String T = "quest.guhs.torenpeper.torenwachter.";
    private static final int JA = 1, ROOKGUHS = 2, FLUITJE = 3;
    private static final Verhaallijn LIJN = TorenpeperFeature.VUURTOREN;

    public TorenwachterRol() {
        super(TorenpeperFeature.VUURTOREN);
    }

    @Override
    protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
        switch (stap) {
            case 0 -> {
                LIJN.begin(p);
                scherm(p, npc, T + "hallo", new Praat.Optie(JA, "gui.guhs.torenpeper.optie.helpen"));
            }
            case 1 -> {
                if (lever(p, npc, 1, BarbecuetherFeature.GLOEIKOOLGRUIS.get(), Vuurtoren.GRUIS_NODIG, T + "gruis_tekort")) {
                    geef(p, new ItemStack(TorenpeperFeature.LAMPKOOLTJE.get()));
                    geefEenmalig(p, "lantaarn", new ItemStack(TorenpeperFeature.SEINLANTAARN.get()));
                    zeg(p, npc, T + "kooltje");
                    hint(p, "quest.guhs.torenpeper.hint.lamp");
                } else {
                    hint(p, "quest.guhs.torenpeper.hint.gruis");
                }
            }
            case 2 -> {
                zeg(p, npc, geefAlsKwijt(p, TorenpeperFeature.LAMPKOOLTJE.get()) ? T + "kooltje_kwijt" : T + "lamp");
                hint(p, "quest.guhs.torenpeper.hint.lamp");
            }
            case 3 -> {
                if (geefAlsKwijt(p, TorenpeperFeature.SEINLANTAARN.get())) {
                    zeg(p, npc, T + "lantaarn_kwijt");
                } else {
                    zeg(p, npc, T + "zoek", Vuurtoren.ROOKGUHS - Vuurtoren.thuis(p));
                }
                hint(p, "quest.guhs.torenpeper.hint.rookguhs");
            }
            case 4 -> {
                if (verder(p, 4)) {
                    geefEenmalig(p, "beloning", new ItemStack(TechbezorgFeature.BEZORGGUHTJE_FLUITJE.get()),
                            new ItemStack(ModItems.clothingItem(GuhClothes.TORENPEPER_WACHTERSJAS)));
                    zichtbaar(p, "barbecuether/toren_peper_vuurtoren");
                    zeg(p, npc, T + "klaar");
                    hint(p, "quest.guhs.torenpeper.hint.klaar_toren");
                }
            }
            default -> scherm(p, npc, T + "na", new Praat.Optie(ROOKGUHS, "gui.guhs.torenpeper.optie.rookguhs"),
                    new Praat.Optie(FLUITJE, "gui.guhs.torenpeper.optie.fluitje"));
        }
    }

    @Override
    protected void antwoord(GuhNpcEntity npc, ServerPlayer p, int stap, int optie) {
        if (optie == JA && stap == 0) {
            if (verder(p, 0)) {
                zeg(p, npc, T + "gruis", Vuurtoren.GRUIS_NODIG);
                hint(p, "quest.guhs.torenpeper.hint.gruis");
            }
        } else if (optie == ROOKGUHS && stap >= 5) {
            zeg(p, npc, T + "rookguhs");
        } else if (optie == FLUITJE && stap >= 5) {
            int dag = (int) (p.level().getGameTime() / 24000L) + 1;
            if (GuhQuests.count(p, TechbezorgFeature.BEZORGGUHTJE_FLUITJE.get()) > 0) {
                zeg(p, npc, T + "fluitje_heb_je");
            } else if (LIJN.teller(p, "fluitje_dag") == dag) {
                zeg(p, npc, T + "fluitje_morgen");
            } else {
                LIJN.teller(p, "fluitje_dag", dag);
                geef(p, new ItemStack(TechbezorgFeature.BEZORGGUHTJE_FLUITJE.get()));
                zeg(p, npc, T + "fluitje_nieuw");
            }
        }
    }
}
