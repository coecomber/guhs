package nl.juiced.guhs.feature.guhpixel.among.model;

/**
 * One statement in a meeting: who says it, what kind, about whom ({@code wie}, -1 = nobody) and where ({@code zone},
 * -1 = nowhere). {@code variant} only picks one of the wordings. NPCs build these from their memory; a player picks them
 * ready-made; everybody weighs them the same way ({@link Vergadering#weeg}).
 */
public record Uitspraak(int spreker, Soort soort, int wie, int zone, int variant, int tick) {
    public enum Soort {
        /** "Ik vond [wie] in [zone]" (the reporter). */
        GEVONDEN,
        /** "Ik drukte op de knop." */
        KNOP,
        /** "Ik was in [zone]". */
        WAAR_IK,
        /** "Ik zag [wie] in [zone]". */
        GEZIEN,
        /** "Ik zag [wie] vlak bij de slaper in [zone]". */
        BIJ_SLAPER,
        /** "Ik zag [wie] uit het luik komen!" */
        LUIK,
        /** "Ik zag [wie] iemand in slaap duwen!" */
        DUW,
        /** "Ik verdenk [wie]". */
        VERDENK,
        /** "Ik sta in voor [wie], die was bij mij". */
        STA_IN,
        /** "Dat klopt niet: [wie] was niet in [zone]". */
        KLOPT_NIET,
        /** "Ik heb niks gezien, njeg". */
        NIKS,
        /** "[wie] is sus, njeg" (the colour joke; weighs almost nothing). */
        SUS;

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }

        public static Soort op(int ordinal) {
            return values()[Math.floorMod(ordinal, values().length)];
        }

        /** What a player may pick. */
        public boolean kiesbaar() {
            return this == WAAR_IK || this == GEZIEN || this == BIJ_SLAPER || this == LUIK || this == DUW || this == VERDENK || this == STA_IN
                    || this == NIKS;
        }

        public boolean overIemand() {
            return this != WAAR_IK && this != NIKS && this != KNOP;
        }

        public boolean metZone() {
            return this == WAAR_IK || this == GEZIEN || this == BIJ_SLAPER || this == GEVONDEN || this == KLOPT_NIET;
        }
    }
}
