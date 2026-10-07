package nl.juiced.guhs.feature.torenpeper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.spiesburcht.SpiesburchtFeature;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.QuestRol;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * De Peperteler-guh of the Pepertuin (kind PEPERTELERGUH): the questline {@link TorenpeperFeature#PEPERTUIN}, per player.
 * <ol start="0">
 *   <li>Talk: three peperzaadjes (once; and new ones whenever a kweekbak still needs one and you have none).</li>
 *   <li>Grow the three peppers in the three kweekbakken of the kas and pick them ({@link Kweek}, {@link Pepertuin#opGeplukt}).</li>
 *   <li>Brew a first pepper drink: he gives what the Guhbrouwketel in the kas needs (and again, once a day, to whoever has
 *       nothing left of it: {@link #pakketWeer}). Stirring a red or pink pepper of your own into a ketel finishes the step
 *       ({@link Pepertuin#eigenPeper}; that pan is shared, so who fills the bottles does not matter), and so does having a
 *       drink or having drunk one ({@link Pepertuin#gebrouwen}).</li>
 *   <li>Let him taste: more seeds, a bottle of the OTHER drink and the peperslinger for your guh.</li>
 * </ol>
 * Afterwards he explains which pepper grows where, and helps out with two seeds a day when you lost all of yours.
 */
public final class PepertelerRol extends QuestRol {
    private static final String T = "quest.guhs.torenpeper.peperteler.";
    private static final int JA = 1, UITLEG = 2, ZAADJES = 3;
    /** How many seeds the reward holds, and how many he hands out per day afterwards to somebody who has none. */
    public static final int BELONING_ZAADJES = 6, DAG_ZAADJES = 2;
    private static final Verhaallijn LIJN = TorenpeperFeature.PEPERTUIN;

    public PepertelerRol() {
        super(TorenpeperFeature.PEPERTUIN);
    }

    private static Item zaadjes() {
        return TorenpeperFeature.PEPERZAADJES.get();
    }

    /** How many seeds the player still needs at step 1: one for every kind that is neither picked nor planted. */
    static int zaadjesNodig(ServerPlayer p) {
        int n = 0;
        for (PeperSoort s : PeperSoort.values()) {
            if (!Pepertuin.geplukt(p, s) && Kweek.groei(p, s) == 0) {
                n++;
            }
        }
        return n;
    }

    @Override
    protected void praat(GuhNpcEntity npc, ServerPlayer p, int stap) {
        switch (stap) {
            case 0 -> {
                LIJN.begin(p);
                scherm(p, npc, T + "hallo", new Praat.Optie(JA, "gui.guhs.torenpeper.optie.kweken"));
            }
            case 1 -> {
                int nodig = zaadjesNodig(p);
                if (nodig > GuhQuests.count(p, zaadjes())) {
                    // (eaten by a guh, planted outside, lost: he makes up the difference, the kweekbakken can't be done without)
                    geef(p, new ItemStack(zaadjes(), nodig - GuhQuests.count(p, zaadjes())));
                    zeg(p, npc, T + "zaadjes_kwijt");
                } else {
                    zeg(p, npc, T + "kweek_nog", PeperSoort.values().length - Pepertuin.aantalGeplukt(p));
                }
                hint(p, "quest.guhs.torenpeper.hint.kweek");
            }
            case 2 -> {
                Pepertuin.gebrouwen(p);
                if (LIJN.stap(p) == 3) {
                    proef(npc, p);
                } else if (geefEenmalig(p, "pakket", new ItemStack(SpiesburchtFeature.GRILLSPIESPOEDER.get()), new ItemStack(ModItems.KAAS_SAUS_BUCKET.get()),
                        new ItemStack(Items.GLASS_BOTTLE, 3))) {
                    zeg(p, npc, T + "brouw");
                    hint(p, "quest.guhs.torenpeper.hint.brouw");
                } else if (!pakketWeer(npc, p)) {
                    zeg(p, npc, T + "brouw_nog");
                    hint(p, "quest.guhs.torenpeper.hint.brouw");
                }
            }
            case 3 -> proef(npc, p);
            default -> scherm(p, npc, T + "na", new Praat.Optie(UITLEG, "gui.guhs.torenpeper.optie.uitleg"),
                    new Praat.Optie(ZAADJES, "gui.guhs.torenpeper.optie.zaadjes"));
        }
    }

    /**
     * Step 2, the kit was given before and there is no drink yet. The Guhbrouwketel of the kas is one pan for everybody: a
     * player whose powder and sauce went into a brew that somebody else stirred or tapped must not be stuck. So whoever has
     * neither grillspiespoeder nor a bucket of kaassaus left gets both again (and bottles when they have none), at most once
     * a day; and whoever has no red or pink pepper left, no seed and nothing growing in those two kweekbakken gets a seed.
     * True when he gave (and said) something.
     */
    private boolean pakketWeer(GuhNpcEntity npc, ServerPlayer p) {
        boolean gaf = false;
        if (GuhQuests.count(p, SpiesburchtFeature.GRILLSPIESPOEDER.get()) == 0 && GuhQuests.count(p, ModItems.KAAS_SAUS_BUCKET.get()) == 0) {
            int dag = (int) (p.level().getGameTime() / 24000L) + 1;
            if (LIJN.teller(p, "pakket_dag") != dag) {
                LIJN.teller(p, "pakket_dag", dag);
                geef(p, new ItemStack(SpiesburchtFeature.GRILLSPIESPOEDER.get()));
                geef(p, new ItemStack(ModItems.KAAS_SAUS_BUCKET.get()));
                if (GuhQuests.count(p, Items.GLASS_BOTTLE) == 0) {
                    geef(p, new ItemStack(Items.GLASS_BOTTLE, 3));
                }
                zeg(p, npc, T + "brouw_weer");
                gaf = true;
            }
        }
        if (!Pepertuin.heeftBrouwpeper(p) && GuhQuests.count(p, zaadjes()) == 0 && Kweek.groei(p, PeperSoort.ROOD) == 0 && Kweek.groei(p, PeperSoort.ROZE) == 0) {
            geef(p, new ItemStack(zaadjes()));
            zeg(p, npc, T + "peper_op");
            gaf = true;
        }
        if (gaf) {
            hint(p, "quest.guhs.torenpeper.hint.brouw");
        }
        return gaf;
    }

    /** Step 3: he tastes, and the questline is done. */
    private void proef(GuhNpcEntity npc, ServerPlayer p) {
        if (!verder(p, 3)) {
            return;
        }
        // the other drink than the one the player brought (so they have tasted both)
        boolean heeftVuur = GuhQuests.count(p, TorenpeperFeature.PEPERVUURDRANKJE.get()) > 0;
        Item ander = heeftVuur ? TorenpeperFeature.PEPERZOETDRANKJE.get() : TorenpeperFeature.PEPERVUURDRANKJE.get();
        geefEenmalig(p, "beloning", new ItemStack(zaadjes(), BELONING_ZAADJES), new ItemStack(ander),
                new ItemStack(ModItems.clothingItem(GuhClothes.TORENPEPER_PEPERSLINGER)));
        zichtbaar(p, "barbecuether/toren_peper_pepertuin");
        zeg(p, npc, T + "klaar");
        hint(p, "quest.guhs.torenpeper.hint.klaar_tuin");
    }

    @Override
    protected void antwoord(GuhNpcEntity npc, ServerPlayer p, int stap, int optie) {
        if (optie == JA && stap == 0) {
            if (verder(p, 0)) {
                geefEenmalig(p, "zaadjes", new ItemStack(zaadjes(), PeperSoort.values().length));
                zeg(p, npc, T + "kweek");
                hint(p, "quest.guhs.torenpeper.hint.kweek");
            }
        } else if (optie == UITLEG && stap >= 4) {
            zeg(p, npc, T + "uitleg");
        } else if (optie == ZAADJES && stap >= 4) {
            int dag = (int) (p.level().getGameTime() / 24000L) + 1;
            if (GuhQuests.count(p, zaadjes()) > 0) {
                zeg(p, npc, T + "zaadjes_genoeg");
            } else if (LIJN.teller(p, "zaadjes_dag") == dag) {
                zeg(p, npc, T + "zaadjes_morgen");
            } else {
                LIJN.teller(p, "zaadjes_dag", dag);
                geef(p, new ItemStack(zaadjes(), DAG_ZAADJES));
                zeg(p, npc, T + "zaadjes_nieuw");
            }
        }
    }
}
