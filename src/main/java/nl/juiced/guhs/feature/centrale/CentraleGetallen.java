package nl.juiced.guhs.feature.centrale;

/**
 * The tunable numbers of the Mega-knuffelcentrale (1.8.0, vadskracht theme). tools/features/centrale.py reads them for the
 * texts ({@code getal}).
 */
public final class CentraleGetallen {
    /** How many guhs fit on the giant cushion. */
    public static final int MAX_GUHS = 16;
    /** VK per guh that cuddles on it, with full care: 16 x 22 = 352 VK. Not under the Guhrad's diminishing returns. */
    public static final int PER_GUH = 22;
    /** What is left (in percent) without food: the guhs are too hungry to cuddle properly. */
    public static final int HONGER_PROCENT = 25;
    /** What is left (in percent) with food but without fresh blankets. */
    public static final int GEEN_DEKENS_PROCENT = 75;
    /** Every guh that cuddles eats one portion per this many seconds (16 guhs: 480 portions an hour). */
    public static final int VOER_SECONDEN = 120;
    /** Portions per food item: a kaasknabbel 1, a roze broodje 4, a block of kaasknabbels 9. */
    public static final int PORTIE_KNABBEL = 1, PORTIE_BROODJE = 4, PORTIE_BLOK = 9;
    /** How many items fit in the food and the blanket compartments. */
    public static final int BAKJE_MAX = 64;
    /** Fresh blankets: one wool per in-game day keeps them fresh. */
    public static final int DEKENS_TICKS = 24_000;

    private CentraleGetallen() {
    }
}
