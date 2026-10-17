package nl.juiced.guhs.feature.einde2.spzij;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.einde2.spverhaal.SpVerhaal;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;

/**
 * einde2 "Sponsguh", slice sp_zij (CONTRACT_E2_C 2.1, 7.10): the three side lines of Bikinibodem, group
 * {@value SpVerhaal#GROEP} ("Sponsguh Kaasbroek" in the Guhdex tab Verhalen), every step per player in main's
 * {@link Verhaallijn}. They open when the player works in town (part 1 past the job interview, {@link SpZij#open}) and
 * are begun by themselves then, so they show in the Guhdex. None has a {@code na}.
 * <ul>
 *   <li>{@value SpVerhaal#SANDY} (4): Sandy-guh's tree dome: the visit (scene {@code sandy}), karate on the mat, three
 *       lost rocket parts round town, the rocket (scene {@code sandy_raket}); reward the luchthelm;</li>
 *   <li>{@value SpVerhaal#GUHLANDER} (4): De Vliegende Guhlander: wait at the mooring (scene {@code guhlander}), up the
 *       bubble stream to the deck (scene {@code hakker}), three ghost lanterns, back to the captain; reward the spooklantaarn;</li>
 *   <li>{@value SpVerhaal#ZEEMEERMAN} (4): Zeemeerman and Mosselguh: the visit (scene {@code helden}), the lost belt in the
 *       invisible boatmobile, the belt back, the "heroic" lunch errand in the invisible boatmobile; reward the heldencape.</li>
 * </ul>
 * Step numbers are fixed (FTB tasks and saves name them). Texts: tools/features/einde2_sp_zij.py {@code LIJNEN}.
 */
public final class SpZijLijnen {
    public static final String SANDY = SpVerhaal.SANDY, GUHLANDER = SpVerhaal.GUHLANDER, ZEEMEERMAN = SpVerhaal.ZEEMEERMAN;
    public static final List<String> ALLE = SpVerhaal.ZIJLIJNEN;

    /** {@value SpVerhaal#SANDY}: 4 steps, done at {@value #S_KLAAR}. */
    public static final int S_BEZOEK = 0, S_KARATE = 1, S_ONDERDELEN = 2, S_RAKET = 3, S_KLAAR = 4;
    /** {@value SpVerhaal#GUHLANDER}: 4 steps, done at {@value #G_KLAAR}. */
    public static final int G_WACHT = 0, G_DEK = 1, G_LANTAARNS = 2, G_TERUG = 3, G_KLAAR = 4;
    /** {@value SpVerhaal#ZEEMEERMAN}: 4 steps, done at {@value #Z_KLAAR}; step 3 has the variant {@code 3_b} (the lunch is fetched). */
    public static final int Z_BEZOEK = 0, Z_RIEM = 1, Z_RIEM_TERUG = 2, Z_LUNCH = 3, Z_KLAAR = 4;

    /** The flag of the zeemeerman line once the lunch is in the boatmobile (the variant 3_b). */
    public static final String LUNCH = "lunch";

    private static Doel naar() {
        return Doel.structuur(GuheindeFeature.GUHEINDE, SpZij.BB, Component.translatable("structure.guhs." + SpZij.BB));
    }

    public static final Verhaallijn SANDY_LIJN = Verhaallijn.maak(SANDY, SpVerhaal.GROEP).stappen(S_KLAAR).icoon("guhs:sponsguh_luchthelm")
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:sponsguh_luchthelm", klaar(p, SANDY))))
            .doel((p, s) -> naar())
            .registreer();

    public static final Verhaallijn GUHLANDER_LIJN = Verhaallijn.maak(GUHLANDER, SpVerhaal.GROEP).stappen(G_KLAAR).icoon("guhs:spooklantaarn")
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:spooklantaarn", SpZij.P + "beloning.spooklantaarn", klaar(p, GUHLANDER))))
            .doel((p, s) -> naar())
            .registreer();

    public static final Verhaallijn ZEEMEERMAN_LIJN = Verhaallijn.maak(ZEEMEERMAN, SpVerhaal.GROEP).stappen(Z_KLAAR).icoon("guhs:sponsguh_heldencape")
            .sleutel((p, s) -> s == Z_LUNCH && lijn(ZEEMEERMAN).vlag(p, LUNCH) ? s + "_b" : String.valueOf(s))
            .extraSleutels(Z_LUNCH + "_b")
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:sponsguh_heldencape", klaar(p, ZEEMEERMAN))))
            .doel((p, s) -> naar())
            .registreer();

    private SpZijLijnen() {
    }

    private static boolean klaar(ServerPlayer p, String lijn) {
        return lijn(lijn).klaar(p);
    }

    /** The line of this id (one of {@link #ALLE}). */
    public static Verhaallijn lijn(String id) {
        return switch (id) {
            case SANDY -> SANDY_LIJN;
            case GUHLANDER -> GUHLANDER_LIJN;
            case ZEEMEERMAN -> ZEEMEERMAN_LIJN;
            default -> throw new IllegalArgumentException("not a side line of sp_zij: " + id);
        };
    }

    /** Every line object (forces the class to load: they register themselves). */
    public static List<Verhaallijn> lijnen() {
        return List.of(SANDY_LIJN, GUHLANDER_LIJN, ZEEMEERMAN_LIJN);
    }

    /** The side lines open for this player: begin them, so they show in the Guhdex (once per line; true when one began now). */
    static boolean beginWatOpenIs(ServerPlayer p) {
        if (!SpZij.open(p)) {
            return false;
        }
        boolean nieuw = false;
        for (Verhaallijn l : lijnen()) {
            nieuw |= l.begin(p);
        }
        return nieuw;
    }
}
