package nl.juiced.guhs.feature.einde2.geestwezens;

import java.util.function.BiPredicate;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * einde2, slice geestmodellen: a schaduwgeest (DESIGN_EINDE2 5, the dark edges of the Gefluisterwoud): a tall, smoky, dark
 * shape with two white eyes. It drifts about its Honk and, the one thing it does, SHOVES: a player in survival or adventure
 * mode who comes within {@link #BEREIK} blocks is pushed away ({@code duw}), at most once every {@link #RUST} ticks. A shove
 * is a push and a little hop, never damage (the standing rule: only Mika's and dark spirits shove, nobody is ever hurt).
 * <p>
 * hook fase B: {@link #MAG_DUWEN} may refuse a shove (a cutscene, a player who is a guest, a spot where it should just stand).
 */
public class SchaduwgeestEntity extends GeestWezen {
    static final double BEREIK = 2.2, KRACHT = 0.9, HOP = 0.25;
    static final int RUST = 40;
    /** Asked before every shove (the story may say no); default: always. */
    public static BiPredicate<SchaduwgeestEntity, Player> MAG_DUWEN = (geest, speler) -> true;

    private long volgendeDuw;

    public SchaduwgeestEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    /** The player it would shove now, or null. */
    @Nullable
    public Player doelwit() {
        if (onzichtbaar()) {
            return null;                                            // (a hidden spirit never shoves)
        }
        Player p = level().getNearestPlayer(this, BEREIK);
        if (p == null || p.isSpectator() || p.getAbilities().instabuild || !p.isAlive()) {
            return null;
        }
        return MAG_DUWEN.test(this, p) ? p : null;
    }

    /** Pushes this player away from it (a push and a little hop; no damage). */
    public void duw(Player speler) {
        zetAnimatie("duw");
        getLookControl().setLookAt(speler);
        double dx = getX() - speler.getX(), dz = getZ() - speler.getZ();
        if (dx * dx + dz * dz < 1e-4) {                               // (right on top of it: along the way it looks)
            dx = Math.sin(getYRot() * Math.PI / 180.0);
            dz = -Math.cos(getYRot() * Math.PI / 180.0);
        }
        speler.knockback(KRACHT, dx, dz);
        speler.setDeltaMovement(speler.getDeltaMovement().add(0, HOP, 0));
        speler.hurtMarked = true;                                   // (make sure the player gets the push)
        volgendeDuw = level().getGameTime() + RUST;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || isNoAi() || level().getGameTime() < volgendeDuw) {
            return;
        }
        Player speler = doelwit();
        if (speler instanceof ServerPlayer) {
            duw(speler);
        }
    }
}
