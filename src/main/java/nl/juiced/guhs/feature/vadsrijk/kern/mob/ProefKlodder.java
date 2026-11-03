package nl.juiced.guhs.feature.vadsrijk.kern.mob;

import javax.annotation.Nullable;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;

/**
 * Kern's placeholder test mob (the real zone-1 Klodder is the wezens slice): a grey, ontvadst slime that hops at you. Also the
 * Grauwe Klodderreus's spat-out "kloddertjes" (the small type). Look: the guh slime model, grey until the plof.
 */
public class ProefKlodder extends VadsrijkMob {
    public ProefKlodder(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public boolean klein() {
        return getType() == KernRegistry.PROEFKLODDERTJE.get();
    }

    /** Render size (vanilla slime size). */
    public int grootte() {
        return klein() ? 1 : 2;
    }

    @Override
    protected double basisVads(int level) {
        return (klein() ? 18 : 40) * (1 + 0.35 * (level - 1));
    }

    @Override
    protected double basisSchade(int level) {
        return (klein() ? 4 : 7) * (1 + 0.25 * (level - 1));
    }

    @Override
    public int xp() {
        return (klein() ? 6 : 14) + 3 * mobLevel();
    }

    @Nullable
    @Override
    public String lootTabel() {
        return klein() ? null : "kern:proefklodder";
    }

    @Override
    public Element standaardElement() {
        return Element.WATER;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide() && !geplofd() && !stil() && onGround() && tickCount % 14 == 0) {
            LivingEntity t = getTarget();
            Vec3 naar = t != null ? t.position().subtract(position()).multiply(1, 0, 1).normalize().scale(0.32) : Vec3.ZERO;
            if (t != null || getRandom().nextInt(4) == 0) {
                if (t == null) {
                    double a = getRandom().nextDouble() * Math.PI * 2;
                    naar = new Vec3(Math.cos(a) * 0.2, 0, Math.sin(a) * 0.2);
                }
                setDeltaMovement(naar.x, 0.42, naar.z);
                if (t != null) {
                    lookAt(t, 30, 30);
                }
            }
        }
    }
}
