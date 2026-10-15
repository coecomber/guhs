package nl.juiced.guhs.feature.einde2.geestwezens;

import javax.annotation.Nullable;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * einde2, slice geestmodellen: a lantaarngeest (DESIGN_EINDE2 5): a floating paper lantern with a cute painted face and a soft
 * glow; {@code Soort} 0..3 = red, amber, pink, blue. It drifts slowly about the spot it was put (at most {@link #DRIJF} blocks
 * away, or its Honk) and its face turns to whoever looks at it; with {@code stil} it hangs still. The bobbing is the model's.
 * <p>
 * hook fase B: the side quest "Verdwaalde lantaarns" (eight of them lost over the zones, brought back to the lantern tree in
 * the Meditatiecirkel) is the story's; it may want them to follow a player, which this class does not do.
 */
public class LantaarngeestEntity extends GeestWezen {
    static final double DRIJF = 2.5, SNEL = 0.03;

    @Nullable
    private Vec3 thuis, doel;
    private int wachtTot;

    public LantaarngeestEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
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
        if (stil() || isNoAi()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        if (doel == null || tickCount >= wachtTot && position().distanceToSqr(doel) < 0.05) {
            double straal = honk() > 0 ? Math.min(honk(), DRIJF * 2) : DRIJF;
            double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * straal;
            doel = thuis.add(Math.cos(a) * r, random.nextDouble() * 1.6 - 0.8, Math.sin(a) * r);
            wachtTot = tickCount + 80 + random.nextInt(80);
        }
        Vec3 stap = doel.subtract(position());
        if (stap.length() > SNEL) {
            stap = stap.normalize().scale(SNEL);
        }
        move(MoverType.SELF, stap);
        setDeltaMovement(Vec3.ZERO);
    }
}
