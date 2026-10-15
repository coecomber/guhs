package nl.juiced.guhs.feature.einde2.geestwezens;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * einde2 phase B, slice geest_modellen: a geestreus (DESIGN_EINDE2 5 "Geestentij + reuzen"): a giant spirit silhouette that
 * drifts in the far distance, deep blue and see-through with faint glowing eyes. Soort 0 a sitting guh as big as a hill (its
 * ears!), 1 a sky serpent with fins, 2 an antlered deer spirit, 3 a robed figure with four arms (one bone per form, the
 * renderer shows one). You never come close: within {@link #NABIJ} blocks it fades away, and it is back once nobody has been
 * within {@link #TERUG} blocks for {@link #WACHT} ticks. It drifts slowly on a wide circle round where it was put, through
 * everything (no collision, nothing to click). Nobody explains it.
 * <p>
 * hook fase B: geest_spel shows them only during the geestentij ({@link #zetWeg}, the one-shot {@code verschijn}).
 */
public class GeestreusEntity extends GeestWezen {
    public static final double NABIJ = 64.0, TERUG = 96.0, RONDJE = 24.0, SNEL = 0.0015;
    public static final int WACHT = 100;

    @Nullable
    private Vec3 thuis;
    private int rustig;

    public GeestreusEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    @Override
    protected boolean kijktNaarSpelers() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return false;
    }

    @Override
    public void tick() {
        this.noPhysics = true;
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (thuis == null) {
            thuis = position();
        }
        if (isVerscholen()) {
            rustig = level().getNearestPlayer(this, TERUG) == null ? rustig + 1 : 0;
            if (rustig >= WACHT) {
                zetVerscholen(false);
                zetAnimatie("verschijn");
            }
        } else if (level().getNearestPlayer(this, NABIJ) != null) {
            zetVerscholen(true);
            rustig = 0;
        }
        if (stil() || isNoAi()) {
            return;
        }
        double a = level().getGameTime() * SNEL + getId() * 1.7;
        Vec3 doel = thuis.add(Math.cos(a) * RONDJE, Math.sin(a * 3) * 2.0, Math.sin(a) * RONDJE);
        Vec3 stap = doel.subtract(position());
        setPos(position().add(stap.scale(0.05)));
        if (stap.horizontalDistanceSqr() > 1e-4) {
            kijkLangs(new Vec3(-Math.sin(a), 0, Math.cos(a)));
        }
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
