package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.Locale;

/**
 * Het Vadsrijk: status effects (ronde 13). Debuffs BRANDEND (fire DoT), GESPOREN (poison DoT + less healing), PLAKKERIG (slowed),
 * VERDOOFD (stunned); team buffs VAHOEG (+damage), KNUFFELSCHILD (absorbs), SNELPOOTJES (faster); DUIZELIG after fainting (-25 % stats).
 * z2-systemen: BEVROREN (zone 2, a status, not an element): slowed, and heavy hits (skills, slow weapons) do extra damage; the
 * effects live in {@code zone2.systemen.Bevroren} (hooks), kern only knows the id.
 * The ticking is {@link Statussen}.
 */
public enum Status {
    BRANDEND(false), GESPOREN(false), PLAKKERIG(false), VERDOOFD(false),
    VAHOEG(true), KNUFFELSCHILD(true), SNELPOOTJES(true),
    DUIZELIG(false),
    BEVROREN(false);   // z2-systemen

    public final boolean buff;

    Status(boolean buff) {
        this.buff = buff;
    }

    public String key() {
        return "vadsrijk.kern.status." + name().toLowerCase(Locale.ROOT);
    }
}
