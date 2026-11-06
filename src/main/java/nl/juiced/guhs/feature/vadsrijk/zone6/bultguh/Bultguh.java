package nl.juiced.guhs.feature.vadsrijk.zone6.bultguh;

import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierSoort;
import nl.juiced.guhs.feature.vadsrijk.economie.Rijdieren;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Aankleden;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.As;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.Bron;
import nl.juiced.guhs.feature.vadsrijk.rijdier.Uiterlijk.Variant;

/**
 * DE BULTGUH (U-Z6WEZENS): the camel-guh of Het Zijdeland, a new LAND mount on t-rijdier's species API (one enum line
 * {@code RijdierSoort.BULTGUH}: speed 330 %, +30 % on sand, TWO seats). Sweet: two soft humps with the saddle between, a long
 * neck, guh ears, tassels on the saddle blanket and little bells round the neck; it sways when it walks.
 * <ul>
 *   <li><b>Tandem</b> (user decision): a second player right-clicks the Bultguh while its owner rides it and sits behind; only the
 *       owner steers. Owner off = the Bultguh trots home (the passenger lands softly).</li>
 *   <li><b>Coats</b> (ZONE6_PLAN §9.2): zandroze (the normal one), saffraan, wolkwit, kaneel (the four of the stal) and the raid's
 *       sterrennacht. The stal four: you PICK one free when you get the Bultguh (hq02), the others at the stal for coins.
 *       Sterrennacht: {@code Aankleden.geef(p, "vacht:bultguh:sterrennacht")} from the raid (u-z6raid).</li>
 *   <li>Patterns and tack (account-wide) fit it like every land mount; plus the zone-6 tack <b>Wormelien-zadeldeken</b> (the field
 *       boss Koningin Wormelien, first kill).</li>
 *   <li><b>Unlock</b>: the stal quest of u-z6verhaal (hq02 "De karavaan van Oma Dadel", Stalmeester Hobbel) calls {@link #geef};
 *       until then the mount screen says where it comes from ({@code vadsrijk.rijdier.bron.api.z6_stal}).</li>
 * </ul>
 */
public final class Bultguh {
    /** The coats (vacht ids): the first is the normal texture. */
    public static final List<String> VACHTEN = List.of(Uiterlijk.NATUREL, "saffraan", "wolkwit", "kaneel", "sterrennacht");
    /** The coats you may pick for free when you get it (hq02). */
    public static final List<String> KEUZE = List.of(Uiterlijk.NATUREL, "saffraan", "wolkwit", "kaneel");
    private static boolean klaar;

    private Bultguh() {
    }

    /** From Z6WezensRegistry.register (common): coats, source, the zone-6 tack. */
    public static void registreer() {
        if (klaar) {
            return;
        }
        klaar = true;
        RijdierSoort b = RijdierSoort.BULTGUH;
        Uiterlijk.registreer(new Variant(As.VACHT, Uiterlijk.NATUREL, b, 0xFFF2B4B0, Bron.START));
        Uiterlijk.registreer(new Variant(As.VACHT, "saffraan", b, 0xFFFFC048, Bron.stal(1800)));
        Uiterlijk.registreer(new Variant(As.VACHT, "wolkwit", b, 0xFFF6F4FF, Bron.stal(1800)));
        Uiterlijk.registreer(new Variant(As.VACHT, "kaneel", b, 0xFFC0784A, Bron.stal(1800)));
        Uiterlijk.registreer(new Variant(As.VACHT, "sterrennacht", b, 0xFF3A3478, Bron.api("z6_raid_sterrennacht")));
        Aankleden.SOORT_BRON.put(b, Bron.api("z6_stal"));
    }

    /**
     * API (u-z6verhaal, hq02): gives the Bultguh with the chosen coat (one of {@link #KEUZE}; anything else = the normal one), owned
     * and worn. True when it was new.
     */
    public static boolean geef(ServerPlayer p, String vacht) {
        boolean nieuw = Rijdieren.geef(p, RijdierSoort.BULTGUH);
        String v = KEUZE.contains(vacht) ? vacht : Uiterlijk.NATUREL;
        if (!v.equals(Uiterlijk.NATUREL)) {
            Aankleden.geef(p, "vacht:bultguh:" + v);
        }
        Aankleden.kies(p, RijdierSoort.BULTGUH, As.VACHT, v);
        return nieuw;
    }

    /** API: the Bultguh in its normal coat. */
    public static boolean geef(ServerPlayer p) {
        return geef(p, Uiterlijk.NATUREL);
    }
}
