package nl.juiced.guhs.entity;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

/**
 * The Hungry Guh from the guh picnics: sits up and never moves. Right-click it and it asks (in chat) for
 * {@link #KNABBELS_WANTED} gefrituurde kaasknabbels. Right-click with that many in your hand and it happily
 * disappears, leaving you a Bank Guh.
 */
public class QuestGuhEntity extends PathfinderMob implements GeoEntity {
    public static final int KNABBELS_WANTED = 10;
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.guh_sitting.idle");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    public QuestGuhEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // always show "Hungry Guh" above its head
        this.setCustomName(Component.translatable("entity.guhs.quest_guh"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10.0f));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = player.getItemInHand(hand);
        Component name = Component.literal("<").append(this.getDisplayName()).append("> ").withStyle(ChatFormatting.LIGHT_PURPLE);
        if (stack.is(ModItems.GEFRITUURDE_KAASKNABBELS.get()) && stack.getCount() >= KNABBELS_WANTED) {
            stack.consume(KNABBELS_WANTED, player);
            player.getInventory().placeItemBackInInventory(new ItemStack(ModItems.BANK_GUH.get()));
            player.sendSystemMessage(name.copy().append(Component.translatable("entity.guhs.quest_guh.thanks").withStyle(ChatFormatting.WHITE)));
            if (this.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.HEART, getX(), getY() + 1.5, getZ(), 10, 0.5, 0.5, 0.5, 0.1);
                server.sendParticles(ParticleTypes.POOF, getX(), getY() + 0.8, getZ(), 25, 0.4, 0.6, 0.4, 0.05);
            }
            this.level().playSound(null, this, ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1f, 1.1f);
            this.discard();
        } else {
            int holding = stack.is(ModItems.GEFRITUURDE_KAASKNABBELS.get()) ? stack.getCount() : 0;
            player.sendSystemMessage(name.copy().append(Component.translatable("entity.guhs.quest_guh.request", KNABBELS_WANTED, holding)
                    .withStyle(ChatFormatting.WHITE)));
            this.level().playSound(null, this, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.1f);
        }
        return InteractionResult.SUCCESS;
    }

    // --- a quest giver: can't be hurt, pushed or leashed, never despawns ---

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // --- GeckoLib ---

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 0, state -> state.setAndContinue(IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
