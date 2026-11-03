package nl.juiced.guhs.feature.vadsrijk.kern.klasse;

/**
 * A skill. Kern does the key (slot 0..3 = R, V, G, Z), the energy, the cooldown (minus COOLDOWNREDUCTIE), the ult charge
 * (slot 3 needs a full charge instead of energy) and the HUD; the skill only does its effect and returns whether it fired
 * (false = nothing happened, nothing is paid).
 */
public interface Vaardigheid {
    String id();

    int slot();

    double energie();

    int cooldownTicks();

    default boolean ult() {
        return slot() == 3;
    }

    boolean gebruik(SkillContext ctx);

    default String naamKey() {
        return "vadsrijk.klassen.skill." + id();
    }

    /** Icon texture (16x16 or 32x32) for the HUD, or null for a letter. */
    default net.minecraft.resources.Identifier icoon() {
        return null;
    }
}
