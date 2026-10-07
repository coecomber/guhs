package nl.juiced.guhs.feature.einde2.sponsguh;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/**
 * einde2, slice sponsguh_wezens: koning Neptuguh, the huge merman guh king with the trident. His flag {@code Kroon} (NBT,
 * synced; on unless it says otherwise): without his crown a shiny bald head shows.
 * <p>
 * hook fase B: Plankmika steals the crown ({@link #setKroon}), and {@code zetAnimatie("bevries")} is the swing of the
 * trident that freezes Meneer Krabguh.
 */
public class NeptuguhEntity extends SponsguhWezen {
    public NeptuguhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        zetVlag(true);                                                       // (he wears his crown until somebody takes it)
    }

    public boolean heeftKroon() {
        return vlag();
    }

    public void setKroon(boolean kroon) {
        zetVlag(kroon);
    }

    @Override
    protected boolean standaardVlag() {
        return true;
    }
}
