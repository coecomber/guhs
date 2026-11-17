package nl.juiced.guhs.feature.guhpixel.among.model;

/**
 * Every tunable number of a round, in ticks and blocks. Two presets: {@link #normaal} (9 participants, 1 Mika) and
 * {@link #lastig} (10 participants, 2 Mikas). Tuned with {@link Simulatie} so that NPC-only rounds on Normaal are won
 * by the Mika about half of the time and a round takes ten to fifteen minutes: about 12 minutes on Normaal (the Mika wins
 * 50%), about 12 on Lastig (the Mikas win 66% since 1.10.6: smarter Mikas, a crew with a longer memory, a shorter push cooldown). Ten tasks each and a pillow that needs almost a minute to cool down make
 * the round that long; with fewer tasks or a quicker pillow it is over in nine.
 */
public final class Balans {
    public boolean lastig;
    public int deelnemers = 9;
    public int mikas = 1;
    public int takenPerGuh = 10;

    /** Walking speed of a guh NPC, blocks per tick. */
    public double npcSnelheid = 0.13;
    public double zicht = 10.0;
    public double zichtDonker = 2.5;
    /** Through a door into the next zone a participant sees this far. */
    public double zichtDeur = 6.0;

    public int duwAfkoel = 1100;
    public int duwAfkoelStart = 900;
    /** The cooldown every Mika gets after a meeting (the second Mika a bit more: they do not push at the same moment). */
    public int duwAfkoelVergadering = 1200;
    public double duwBereik = 2.6;
    /** After a push the OTHER Mikas must wait this part of the cooldown too (two Mikas do not push twice as fast). */
    public double duwAfkoelSamen = 0.0;
    public int knopAfkoel = 400;
    public int saboteerAfkoel = 500;
    public int saboteerAfkoelStart = 300;
    public int alarmTijd = 900;
    public int deurTijd = 200;
    public int luikAfkoel = 40;
    /** Ticks a participant needs at a panel to fix a sabotage. */
    public int herstelTijd = 60;
    /**
     * The pace of a real player's task panels, in percent: every mini-game has its own shortest time (Taken), and the server
     * refuses an answer that comes sooner than that time times this (tests make it small).
     */
    public int spelerTaakTijd = 100;

    public int bespreekTijd = 400;
    public int stemTijd = 900;
    public int uitslagTijd = 120;
    public int maxUitspraken = 6;

    // --- crew NPCs ---
    public int treuzelMin = 160;
    public int treuzelMax = 700;
    /** Chance that a crew guh strolls to another spot before its next task. */
    public double zwerfKans = 0.45;
    public double lichtReageerKans = 0.35;
    public double alarmReageerKans = 0.8;
    public double vergeetKans = 0.15;
    public double vergisKans = 0.07;
    /** From this much suspicion a crew guh votes for somebody; between twijfel and stem it votes half of the time. */
    public double stemDrempel = 2.7;
    public double twijfelDrempel = 1.7;
    public double ruis = 0.3;

    // --- the NPC Mika ---
    /** Chance per decision that the Mika walks to where a lonely crew guh really is (it "hears" them). */
    public double sluwheid = 0.7;
    public double gehoorAfstand = 45.0;
    /** Chance that the Mika pushes although somebody could see it. */
    public double roekeloos = 0.03;
    public double luikKans = 0.7;
    public double zelfMeldKans = 0.1;
    public double beschuldigKans = 0.6;
    /** An NPC Mika votes somebody out only when at least this many crew guhs accused that one. */
    public int mikaStemtMee = 1;
    /** Average ticks between sabotage attempts when the sabotage is ready. */
    public int saboteerGemiddeld = 900;

    // --- 1.10.6: Lastig thinks harder ---
    /**
     * The NPC Mikas play smart ({@link MikaBrein}): always away through a vent after a push (out far from the sleeper, at a
     * task panel), crawling through the vents to a lonely crew guh, the lights out right before a push and the Knabbelalarm
     * right after one, the doors shut behind a victim, two Mikas hunting in different parts of the ship and covering for each
     * other now and then, and lies that fit what the crew could have seen. (Tried and dropped: reporting its own push when
     * caught beside the sleeper, and fewer random sabotages: both made the Mikas lose more often.)
     */
    public boolean slimmeMika;
    /**
     * The crew NPCs remember more ({@link Geheugen}): the last sightings of everybody (not only the last one), a logbook of
     * the meetings (who accused whom, who vouched for whom), they learn from every vote (whoever pushed an innocent out, or
     * vouched for a Mika, becomes suspect) and from every sleeper (pushed guhs are innocent: whoever accused them is
     * suspect), a proven liar stays suspect, and what they saw for sure they say again in every meeting.
     */
    public boolean langGeheugen;
    /** After a meeting the suspicion that is not certain shrinks to this part. */
    public double verval = 0.8;

    public static Balans normaal() {
        return new Balans();
    }

    public static Balans lastig() {
        Balans b = new Balans();
        b.lastig = true;
        b.deelnemers = 10;
        b.mikas = 2;
        b.duwAfkoel = 1350;     // (1.10.6: 1550 -> 1350, with the crew's longer memory; NPC-only rounds: the Mikas win ~66%)
        b.duwAfkoelVergadering = 1700;
        b.duwAfkoelSamen = 1.0;
        b.mikaStemtMee = 2;
        b.beschuldigKans = 0.35;
        // 1.10.6: smarter on both sides
        b.slimmeMika = true;
        b.langGeheugen = true;
        b.verval = 0.9;
        b.vergeetKans = 0.15;
        b.vergisKans = 0.08;
        return b;
    }

    public static Balans van(boolean lastig) {
        return lastig ? lastig() : normaal();
    }
}
