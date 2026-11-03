package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.Locale;

/**
 * Het Vadsrijk: status effects (ronde 13). Debuffs BRANDEND (fire DoT), GESPOREN (poison DoT + less healing), PLAKKERIG (slowed),
 * VERDOOFD (stunned); team buffs VAHOEG (+damage), KNUFFELSCHILD (absorbs), SNELPOOTJES (faster); DUIZELIG after fainting (-25 % stats).
 * The ticking is {@link Statussen}.
 */
public enum Status {
    BRANDEND(false), GESPOREN(false), PLAKKERIG(false), VERDOOFD(false),
    VAHOEG(true), KNUFFELSCHILD(true), SNELPOOTJES(true),
    DUIZELIG(false);

    public final boolean buff;

    Status(boolean buff) {
        this.buff = buff;
    }

    public String key() {
        return "vadsrijk.kern.status." + name().toLowerCase(Locale.ROOT);
    }
}
