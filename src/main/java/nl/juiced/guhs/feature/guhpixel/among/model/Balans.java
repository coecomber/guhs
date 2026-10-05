package nl.juiced.guhs.feature.guhpixel.among.model;

/**
 * Every tunable number of a round, in ticks and blocks. Two presets: {@link #normaal} (9 participants, 1 Mika) and
 * {@link #lastig} (10 participants, 2 Mikas). Tuned with {@link Simulatie} so that NPC-only rounds on Normaal are won
 * by the Mika about half of the time.
 */
public final class Balans {
    public boolean lastig;
    public int deelnemers = 9;
    public int mikas = 1;
    public int takenPerGuh = 7;

    /** Walking speed of a guh NPC, blocks per tick. */
    public double npcSnelheid = 0.13;
    public double zicht = 10.0;
    public double zichtDonker = 2.5;
    /** Through a door into the next zone a participant sees this far. */
    public double zichtDeur = 6.0;

    public int duwAfkoel = 700;
    public int duwAfkoelStart = 400;
    /** The cooldown every Mika gets after a meeting (the second Mika a bit more: they do not push at the same moment). */
    public int duwAfkoelVergadering = 800;
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
    /** Ticks a real player needs for one task step of the placeholder panel (the server checks 80% of it). */
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

    public static Balans normaal() {
        return new Balans();
    }

    public static Balans lastig() {
        Balans b = new Balans();
        b.lastig = true;
        b.deelnemers = 10;
        b.mikas = 2;
        b.duwAfkoel = 1000;
        b.duwAfkoelVergadering = 1200;
        b.duwAfkoelSamen = 1.0;
        b.mikaStemtMee = 2;
        b.beschuldigKans = 0.35;
        return b;
    }

    public static Balans van(boolean lastig) {
        return lastig ? lastig() : normaal();
    }
}
