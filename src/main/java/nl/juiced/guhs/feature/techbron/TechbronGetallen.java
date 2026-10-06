package nl.juiced.guhs.feature.techbron;

/**
 * The tunable numbers of the vadskracht sources (bbq2, tech-bronnen) that are not in
 * {@link nl.juiced.guhs.feature.vadskracht.VadsGetallen} (what a source gives lives there): how far guhs come from, how long a
 * knabbel lasts, the bonus of a rare disc. tools/features/tech_bronnen.py reads them for the texts ({@code getal}).
 */
public final class TechbronGetallen {
    /** Tamed guhs within this many blocks of a Knuffelgenerator or a Disco-dynamo come to it by themselves. */
    public static final int BEREIK = 8;
    /** A guh that follows its owner only joins while the owner is within this many blocks of the source. */
    public static final int EIGENAAR_BEREIK = 12;
    /** A guh that cuddled on the cushion stays "blij" this long afterwards (ticks): long enough to put it in a Guhrad. */
    public static final int BLIJ_NA_KNUFFEL = 20 * 60;
    /** A rare disc (item tag guhs:techbron/zeldzame_plaat) makes every dancer give this much more. */
    public static final int DISCO_BONUS = 1;
    /** One knabbel keeps the Sausblubje of a Blubkacheltje warm for this many seconds. */
    public static final int BLUB_SECONDEN = 600;
    /** How many knabbels fit in a Blubkacheltje's bakje. */
    public static final int BLUB_VOER_MAX = 16;
    /** The flag "Aangebrande Mika verslagen" goes to every player within this many blocks of the Mika when it dies. */
    public static final int MIKA_BEREIK = 32;
    /** A source's guhs and face are looked at once per this many ticks. */
    public static final int KIJK = 20;

    private TechbronGetallen() {
    }
}
