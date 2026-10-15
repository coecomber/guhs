package nl.juiced.guhs.feature.einde2.geestwezens;

import java.util.function.BiPredicate;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * einde2 phase B, slice geest_modellen: Wan-guh, the hooded figure (DESIGN_EINDE2 5 "Geheimzinnige gestalte = Wan-guh, de
 * eerste"): a guh in a dark robe with a glowing hem, the hood pulled over his head (his ears through it), two glowing eyes in
 * its shadow, a staff with a pale light. He stands far away in different zones; whoever comes within {@link #DICHTBIJ}
 * blocks sees him fade ({@code verdwijn}, then hidden), and he is back on his spot once nobody has been within {@link #TERUG}
 * blocks for {@link #WACHT} ticks. With {@code NoAI} (an actor of a scene), or with the hood lowered ({@code ontkap}: the
 * reveal on the 8th island), he never vanishes; {@link #MAG_VERDWIJNEN} can say no too.
 */
public class WanGuhEntity extends GeestWezen {
    public static final double DICHTBIJ = 20.0, TERUG = 40.0;
    public static final int WACHT = 200;
    /** Asked before he vanishes for a player (hook fase B; default yes). */
    public static BiPredicate<WanGuhEntity, Player> MAG_VERDWIJNEN = (wan, speler) -> true;

    private int verdwijntOp = -1;
    private int rustig;

    public WanGuhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** Fades now (the animation, then hidden until nobody is near). */
    public void verdwijn() {
        if (verdwijntOp < 0 && !isVerscholen()) {
            zetAnimatie("verdwijn");
            verdwijntOp = tickCount + GeestWezens.WAN_GUH.duur("verdwijn");
        }
    }

    /** Is he gone right now (or fading)? */
    public boolean isVerdwenen() {
        return isVerscholen() || verdwijntOp >= 0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        if (verdwijntOp >= 0 && tickCount >= verdwijntOp) {
            verdwijntOp = -1;
            zetVerscholen(true);
            rustig = 0;
            if (level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.END_ROD, getX(), getY() + 1.4, getZ(), 6, 0.3, 0.6, 0.3, 0.01);
            }
            return;
        }
        if (isVerscholen()) {
            rustig = level().getNearestPlayer(this, TERUG) == null ? rustig + 1 : 0;
            if (rustig >= WACHT) {
                zetVerscholen(false);                                 // (back on his spot, far away again)
            }
            return;
        }
        if (isNoAi() || houding().equals("ontkap") || verdwijntOp >= 0 || onzichtbaar()) {
            return;
        }
        Player p = level().getNearestPlayer(this, DICHTBIJ);
        if (p != null && !p.isSpectator() && MAG_VERDWIJNEN.test(this, p)) {
            verdwijn();
        }
    }
}
