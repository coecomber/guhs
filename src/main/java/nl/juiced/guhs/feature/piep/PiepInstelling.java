package nl.juiced.guhs.feature.piep;

import java.util.List;
import java.util.Locale;

/**
 * The settings in the little menu of a piep-maatje ({@link PiepMaatje}): the pieppiepmuisje, Poepschilly and Schilly.
 * <ul>
 *   <li>{@link #RONDVADSEN}: off = it stays put (it sits: {@code TamableAnimal.isOrderedToSit}, saved by vanilla as "Sitting",
 *       synced as the sitting pose). All three.</li>
 *   <li>The others are bits of a synced int of things that are switched OFF ("PiepUit", 0 = everything on, so old saves and
 *       fresh creatures behave as before): {@link #VOLGEN} (follows its owner), {@link #PIEPJES} (the muisje's ambient peeps),
 *       {@link #VERSTOPPEN} (the muisje starts verstoppertje by itself), {@link #ZWEMMEN} (the turtles swim around by
 *       themselves), {@link #BESTIES} (Schilly waddles to guhs by itself for a bestie moment... or a bit of beef).</li>
 * </ul>
 */
public enum PiepInstelling {
    RONDVADSEN(-1),
    VOLGEN(0),
    PIEPJES(1),
    VERSTOPPEN(2),
    ZWEMMEN(3),
    BESTIES(4);

    /** Its bit in the "off" flags (-1: RONDVADSEN, which is the sitting state). */
    public final int bit;

    PiepInstelling(int bit) {
        this.bit = bit;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Lang key of the button ("gui.guhs.piep.menu.&lt;id&gt;", with %s = aan/uit) and its tooltip (+ ".tooltip"). */
    public String key() {
        return "gui.guhs.piep.menu." + id();
    }

    public static PiepInstelling byIndex(int i) {
        PiepInstelling[] all = values();
        return all[Math.floorMod(i, all.length)];
    }

    /** The settings of the muisje, Poepschilly and Schilly (in menu order). */
    public static final List<PiepInstelling> MUISJE = List.of(RONDVADSEN, VOLGEN, PIEPJES, VERSTOPPEN);
    public static final List<PiepInstelling> POEPSCHILLY = List.of(RONDVADSEN, VOLGEN, ZWEMMEN);
    public static final List<PiepInstelling> SCHILLY = List.of(RONDVADSEN, VOLGEN, ZWEMMEN, BESTIES);
}
