package nl.juiced.guhs.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import nl.juiced.guhs.registry.ModItems;

/**
 * A pink guh slime: bounces around happily, never attacks, and the little and medium ones drop guh slimeballs.
 * 1.10.10: right-click one with an empty hand (not sneaking: sneak-click stays free for picking one up later) and it gives
 * you a guh slimeball, with hearts and a giggly squish; then it is "uitgeknuffeld" for {@value #KNUFFEL_PAUZE} ticks
 * (saved on the slime as {@value #KNUFFEL_TOT}: the game time it can give one again).
 */
public class GuhSlimeEntity extends Slime {
    public static final int KNUFFEL_PAUZE = 6000;
    static final String KNUFFEL_TOT = "GuhsKnuffelTot";
    /** The game time from which it gives a slimeball again. */
    private long knuffelTot;

    public GuhSlimeEntity(EntityType<? extends Slime> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.targetSelector.removeAllGoals(goal -> true); // peaceful: never goes after anyone
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().isEmpty() || player.isShiftKeyDown()) {
            return super.mobInteract(player, hand);
        }
        if (player instanceof ServerPlayer sp) {
            knuffel(sp);
        }
        return InteractionResult.SUCCESS;
    }

    /** A cuddle: a guh slimeball when it is not uitgeknuffeld (true), else just a happy hop and a hint (false). */
    public boolean knuffel(ServerPlayer p) {
        if (!(level() instanceof ServerLevel level)) {
            return false;
        }
        long nu = level.getGameTime();
        hop();
        if (nu < knuffelTot) {
            level.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2, getZ(), 1, 0.2, 0.1, 0.2, 0);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.SLIME_JUMP_SMALL, SoundSource.NEUTRAL, 0.6f, 1.5f);
            p.sendOverlayMessage(Component.translatable("gui.guhs.guh_slime.uitgeknuffeld").withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        knuffelTot = nu + KNUFFEL_PAUZE;
        geef(p, new ItemStack(ModItems.GUH_SLIMEBALL.get()));
        level.sendParticles(ParticleTypes.HEART, getX(), getY() + getBbHeight() + 0.2, getZ(), 6, 0.4, 0.2, 0.4, 0);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.NEUTRAL, 1.0f, 1.4f);
        level.playSound(null, getX(), getY(), getZ(), nl.juiced.guhs.registry.ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 0.7f, 1.7f);
        p.sendOverlayMessage(Component.translatable("gui.guhs.guh_slime.knuffel").withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /**
     * Into the pockets, but not into the empty hand you cuddle with (else the next slime would need you to empty it first):
     * onto a stack of them, or a free slot other than the selected one; else as usual (in the hand, or on the ground).
     */
    private static void geef(ServerPlayer p, ItemStack stack) {
        var inv = p.getInventory();
        int gekozen = inv.getSelectedSlot();
        for (int i = 0; i < 36 && !stack.isEmpty(); i++) {
            ItemStack s = inv.getItem(i);
            if (i != gekozen && ItemStack.isSameItemSameComponents(s, stack) && s.getCount() < s.getMaxStackSize()) {
                s.grow(1);
                stack.shrink(1);
            }
        }
        for (int i = 0; i < 36 && !stack.isEmpty(); i++) {
            if (i != gekozen && inv.getItem(i).isEmpty()) {
                inv.setItem(i, stack.copy());
                stack.setCount(0);
            }
        }
        nl.juiced.guhs.feature.Minigames.give(p, stack);
    }

    /** Ticks until it can give a slimeball again (0: now). */
    public long knuffelWacht() {
        return Math.max(0, knuffelTot - level().getGameTime());
    }

    private void hop() {
        if (onGround()) {
            setDeltaMovement(getDeltaMovement().add(0, 0.32, 0));
            needsSync = true;
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (knuffelTot > 0) {
            output.putLong(KNUFFEL_TOT, knuffelTot);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        knuffelTot = input.getLongOr(KNUFFEL_TOT, 0L);
    }

    @Override
    protected boolean isDealsDamage() {
        return false;
    }

    @Override
    protected void dealDamage(LivingEntity target) {
    }

    /** 26.1: the peaceful check moved to the entity type (EntityType.Builder#notInPeaceful); guh slimes stay (default). */
    public boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    protected ParticleOptions getParticleType() {
        return new ItemParticleOption(ParticleTypes.ITEM, ModItems.GUH_SLIMEBALL.get());
    }

    public static boolean checkGuhSlimeSpawnRules(EntityType<GuhSlimeEntity> type, LevelAccessor level, EntitySpawnReason spawnType, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).isSolid() && level.getRawBrightness(pos, 0) > 7;
    }
}
