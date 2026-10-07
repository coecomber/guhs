package nl.juiced.guhs.feature.einde2.sponsguh;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;

/**
 * einde2, slice sponsguh_wezens: Dennis-Mika, the hired tough Mika in black leather. His flag {@code OpMotor} (NBT, synced):
 * he sits on his motorbike (the bike is part of his model) and is faster on it.
 * <p>
 * hook fase B: the chase of the second part of the Sponsguh story puts him on the bike with {@link #setOpMotor}; like every
 * Mika he only ever shoves.
 */
public class DennisMikaEntity extends SponsguhWezen {
    public DennisMikaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public boolean isOpMotor() {
        return vlag();
    }

    public void setOpMotor(boolean opMotor) {
        zetVlag(opMotor);
    }

    @Override
    public float getSpeed() {
        return isOpMotor() ? super.getSpeed() * 1.8f : super.getSpeed();
    }

    @Override
    protected String beweging(boolean beweegt) {
        String houding = houding();
        if (isOpMotor() && (beweegt || houding.equals("rij"))) {
            return "rij";
        }
        return houding.equals("rij") ? "idle" : super.beweging(beweegt);
    }
}
