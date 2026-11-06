package nl.juiced.guhs.feature.vadsrijk.ruiters;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;

/**
 * U-RUITERS: a riding guh (vr_ruiter_guh): the npc slice's {@link RijkGuh} (real guh model + clothes, passive, never hurt, never
 * saved), sitting on its {@link RuiterPluim}, or standing next to one at the Pluimstal (the groomer). It only looks around (and
 * waves at you now and then, like every Vadsrijk guh); a click gives a cute line ({@link Ruiters#klik}).
 */
public class RuiterGuh extends RijkGuh {
    /** The manager's key (route#k), for the click line. Server side. */
    String sleutel = "";

    public RuiterGuh(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 8f));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !this.level().isClientSide() && player instanceof ServerPlayer sp) {
            getLookControl().setLookAt(sp);
            Ruiters.klik(sp, this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void checkDespawn() {
    }
}
