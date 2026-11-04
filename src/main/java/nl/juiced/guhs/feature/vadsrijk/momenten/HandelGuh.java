package nl.juiced.guhs.feature.vadsrijk.momenten;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;

/**
 * Kruimel, the trader guh of the caravan (vr_mom_handelaar): the npc slice's {@link RijkGuh} (the real guh model and clothes,
 * passive, never hurt, never saved) sitting on the cart's bench. It only looks around; a click opens the caravan shop.
 */
public class HandelGuh extends RijkGuh {
    public HandelGuh(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    /** Dress it (server side, right after creating it). */
    void kleed() {
        setVariant(nl.juiced.guhs.entity.GuhVariant.MINT);
        wear(GuhClothes.STRAW_HAT);
        wear(GuhClothes.VR_ITEM_VADSRIJKSJAAL);
        setCustomName(Component.translatable("vadsrijk.momenten.karavaan.handelaar"));
        setCustomNameVisible(true);
        setOrderedToSit(true);
        setInSittingPose(true);
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
            Karavaan.klik(sp);
        }
        return InteractionResult.SUCCESS;
    }
}
