package nl.juiced.guhs.feature.einde2.geestwezens;

import javax.annotation.Nullable;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * einde2 phase B, slice geest_modellen: a mistreiger (DESIGN_EINDE2 5 "Nieuwe wezens"): a small white heron with tiny guh ears
 * that stands on the roofs of the White Lotus. A player who comes within {@link #SCHRIK} blocks (sneaking: within
 * {@link #SCHRIK_SLUIP}) startles it: it flies up ({@code opvlieg}), circles above its spot for a while ({@code vlieg}) and
 * lands back on the very spot it stood (its home: where it was put). Now and then it preens ({@code poets}). It flies
 * through nothing: blocks stop it.
 */
public class MistreigerEntity extends GeestWezen {
    static final double SCHRIK = 5.0, SCHRIK_SLUIP = 2.0, SNEL = 0.22, HOOG = 7.0, RONDJE = 5.0;
    static final int WEG_MIN = 120, WEG_MAX = 220;

    @Nullable
    private Vec3 thuis;
    private int wegTot = -1;
    private boolean vliegt;

    public MistreigerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** Is it up in the air right now (server)? */
    public boolean isOpgevlogen() {
        return wegTot >= 0;
    }

    private boolean schrikt(Player p) {
        double d = distanceTo(p);
        return !p.isSpectator() && (d < SCHRIK_SLUIP || d < SCHRIK && !p.isShiftKeyDown());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (thuis == null) {
            thuis = position();
        }
        vliegt = false;
        if (isNoAi() || onzichtbaar()) {
            return;
        }
        Player p = level().getNearestPlayer(this, SCHRIK);
        if (wegTot < 0) {
            if (p != null && schrikt(p)) {
                wegTot = tickCount + WEG_MIN + random.nextInt(WEG_MAX - WEG_MIN);
                zetAnimatie("opvlieg");
            } else {
                if (random.nextInt(600) == 0) {
                    zetAnimatie("poets");
                }
                setDeltaMovement(Vec3.ZERO);
                return;
            }
        }
        Vec3 doel;
        boolean terug = tickCount >= wegTot && (p == null || !schrikt(p));
        if (terug) {
            doel = thuis;
            if (position().distanceToSqr(thuis) < 0.05) {
                snapTo(thuis.x, thuis.y, thuis.z, getYRot(), 0f);
                wegTot = -1;                                         // (home again, on the very spot)
                return;
            }
        } else {
            double a = tickCount * 0.05 + getId();
            doel = thuis.add(Math.cos(a) * RONDJE, HOOG + Math.sin(a * 0.7), Math.sin(a) * RONDJE);
        }
        Vec3 stap = doel.subtract(position());
        if (stap.length() > SNEL) {
            stap = stap.normalize().scale(SNEL);
        }
        move(MoverType.SELF, stap);
        setDeltaMovement(Vec3.ZERO);
        kijkLangs(stap);
        vliegt = stap.lengthSqr() > 1e-4;
    }

    @Override
    protected boolean isBezig() {
        return vliegt;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        if (thuis != null) {
            tag.putDouble("ThuisX", thuis.x);
            tag.putDouble("ThuisY", thuis.y);
            tag.putDouble("ThuisZ", thuis.z);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        double y = tag.getDoubleOr("ThuisY", Double.NaN);
        thuis = Double.isNaN(y) ? null : new Vec3(tag.getDoubleOr("ThuisX", 0), y, tag.getDoubleOr("ThuisZ", 0));
    }
}
