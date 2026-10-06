package nl.juiced.guhs.feature.guhpixel.among.model;

/** Something that happened in a round; the session turns these into sounds, texts, entities and blocks. */
public record Gebeurtenis(Soort soort, int a, int b, int c) {
    public enum Soort {
        /** a pushed b asleep (in zone c). */
        DUW,
        /** a reported the sleeper b. */
        GEMELD,
        /** a pressed the emergency button. */
        KNOP,
        /** A meeting starts (a = who called it, b = the sleeper or -1). */
        VERGADERING,
        /** A new statement (a = index in the list of the meeting). */
        UITSPRAAK,
        /** a voted. */
        STEM,
        /** The voting starts. */
        STEMMEN,
        /** The result: a = voted out (-1 nobody), b = 1 when that was a Mika, c = 1 on a tie. */
        UITSLAG,
        /** The meeting is over, everybody walks again. */
        VERDER,
        /** A sabotage starts: a = Sabotage ordinal, b = room (doors), c = who. */
        SABOTAGE,
        /** The sabotage a ended (b = 1: fixed by c; 0: ran out or cancelled). */
        SABOTAGE_KLAAR,
        /** One alarm panel (a = 0/1) was fixed by b. */
        ALARM_PANEEL,
        /** a finished a task step (b = 1: the whole task). */
        TAAK,
        /** a went through the vents from vent b to vent c. */
        LUIK,
        /** a is out without a push: voted out (b = 1) or left the game (b = 2). */
        UIT,
        /** The round is over: a = winner (Rol ordinal), b = Einde ordinal. */
        EINDE
    }
}
