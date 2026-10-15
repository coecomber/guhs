package nl.juiced.guhs.feature.einde2.geestwezens;

import javax.annotation.Nullable;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * einde2, slice geestmodellen: a libelkonijnguh (DESIGN_EINDE2 5): a tiny curious spirit, a bunny with the guh face and four
 * dragonfly wings. It flits about the spot it was put (within its Honk, else {@link #RONDJE} blocks); a player within
 * {@link #NIEUWSGIERIG} blocks is followed at a little distance, and whoever comes too close ({@link #TE_DICHTBIJ}), or just now
 * and then, makes it dart off ({@code schiet_weg}) and come back later. It flies around blocks, never through them.
 * <p>
 * hook fase B: the cosmetic pet of the side quest "Verdwaalde lantaarns" (a libelkonijnguhtje that travels with you) is the
 * story's; it can reuse this entity type and model.
 */
public class LibelkonijnguhEntity extends GeestWezen {
    static final double NIEUWSGIERIG = 10.0, TE_DICHTBIJ = 1.3, RONDJE = 6.0;
    /** Blocks a tick: flitting, and darting away. */
    static final double SNEL = 0.16, WEG = 0.55;

    @Nullable
    private Vec3 thuis, doel;
    private int nieuwDoelOver, wegTot, rustTot;
    private boolean vliegt;

    public LibelkonijnguhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** Is it darting off right now (server)? */
    public boolean schietWeg() {
        return wegTot > tickCount;
    }

    @Nullable
    private Player nieuwsgierigNaar() {
        Player p = level().getNearestPlayer(this, NIEUWSGIERIG);
        return p == null || p.isSpectator() ? null : p;
    }

    private void kiesDoel(@Nullable Player speler) {
        if (speler != null && rustTot <= tickCount) {
            double a = random.nextDouble() * Math.PI * 2;
            doel = speler.getEyePosition().add(Math.cos(a) * 2.2, random.nextDouble() * 0.8 - 0.2, Math.sin(a) * 2.2);
        } else {
            double straal = honk() > 0 ? honk() : RONDJE;
            double a = random.nextDouble() * Math.PI * 2, r = random.nextDouble() * straal;
            doel = thuis.add(Math.cos(a) * r, random.nextDouble() * 2.0, Math.sin(a) * r);
        }
        nieuwDoelOver = 30 + random.nextInt(40);
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
        if (stil() || isNoAi()) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }
        Player speler = nieuwsgierigNaar();
        if (!schietWeg() && speler != null && (distanceTo(speler) < TE_DICHTBIJ || random.nextInt(400) == 0)) {
            Vec3 weg = position().subtract(speler.position()).multiply(1, 0, 1);
            weg = weg.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : weg.normalize();
            doel = position().add(weg.scale(7.0)).add(0, 1.5, 0);
            wegTot = tickCount + wezen.duur("schiet_weg");
            rustTot = tickCount + 100 + random.nextInt(100);       // (it keeps its distance a while)
            nieuwDoelOver = 30;
            zetAnimatie("schiet_weg");
        }
        if (doel == null || --nieuwDoelOver <= 0 || position().distanceToSqr(doel) < 0.3) {
            if (!schietWeg()) {
                kiesDoel(speler);
            }
        }
        Vec3 stap = doel.subtract(position());
        double snel = schietWeg() ? WEG : SNEL;
        if (stap.length() > snel) {
            stap = stap.normalize().scale(snel);
        }
        stap = stap.add(0, Math.sin(tickCount * 0.3) * 0.02, 0);   // (a little flutter)
        move(MoverType.SELF, stap);
        setDeltaMovement(Vec3.ZERO);
        if (speler != null && !schietWeg() && rustTot <= tickCount) {
            kijkLangs(speler.position().subtract(position()));       // (it keeps an eye on you)
        } else {
            kijkLangs(stap);
        }
        vliegt = stap.lengthSqr() > 1e-4;
    }

    @Override
    protected boolean isBezig() {
        return vliegt || !stil() && !isNoAi();
    }
}
