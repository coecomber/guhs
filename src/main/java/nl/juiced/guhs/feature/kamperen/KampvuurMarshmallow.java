package nl.juiced.guhs.feature.kamperen;

import java.util.Comparator;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * Marshmallows at the campfire (the consumer of the tag {@code #guhs:knus/marshmallow}, filled by the wereldleven
 * slice): in the evening or at night, right-click a burning campfire with a marshmallow knabbel and you roast it over
 * the fire and eat it, warm and sticky. Sparks and crumbs, a bite of food, the guhs nearby smell it and come to sit by
 * the fire, and Opa Guh (if he's there) says something about it. Counted in the Knus tab (kamperen.marshmallows).
 * No kaasknabbels as a reward: a marshmallow knabbel is made from a kaasknabbel, so that would be a knabbel machine.
 */
public final class KampvuurMarshmallow {
    /** Knus counter: marshmallows roasted. */
    public static final String GEROOSTERD = "kamperen.marshmallows";
    /** Ticks before you can roast the next one. */
    public static final int WACHT = 30;
    /** Guhs this close come and sit by the fire; Opa Guh this close says something. */
    public static final double GUH_AFSTAND = 8, OPA_AFSTAND = 10;
    public static final int OPA_ZINNEN = 3;

    private KampvuurMarshmallow() {
    }

    /** Is this a marshmallow (the item tag guhs:knus/marshmallow)? */
    public static boolean isMarshmallow(ItemStack stack) {
        return !stack.isEmpty() && stack.is(KnusTags.MARSHMALLOW);
    }

    /** Marshmallow time: the evening and the night, in a world with a day and a night. */
    public static boolean avond(Level level) {
        if (level.dimensionType().hasFixedTime()) {
            return false;
        }
        Dagdeel d = Dagdeel.huidig(level);
        return d == Dagdeel.AVOND || d == Dagdeel.NACHT;
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!isMarshmallow(stack)) {
            return;
        }
        Level level = event.getLevel();
        BlockState state = level.getBlockState(event.getPos());
        if (!state.is(BlockTags.CAMPFIRES) || !CampfireBlock.isLitCampfire(state)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer player) || player.getCooldowns().isOnCooldown(stack)) {
            return;
        }
        if (!avond(level)) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.kamperen.marshmallow_overdag").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        rooster(player, event.getPos(), stack);
    }

    /**
     * Roasts one marshmallow of this stack over the campfire at {@code vuur} and eats it (public for the tests, which
     * use a stand-in item: the tag is filled by another slice). Returns the player's total of roasted marshmallows.
     */
    public static int rooster(ServerPlayer player, BlockPos vuur, ItemStack stack) {
        ServerLevel level = player.level();
        ItemStack een = stack.copyWithCount(1);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.getCooldowns().addCooldown(een, WACHT);
        player.getFoodData().eat(3, 0.6f);
        double x = vuur.getX() + 0.5, y = vuur.getY() + 1.0, z = vuur.getZ() + 0.5;
        level.sendParticles(ParticleTypes.SMALL_FLAME, x, y, z, 6, 0.15, 0.1, 0.15, 0.01);
        level.sendParticles(KamperenFeature.KAMPVUURVONKJE.get(), x, y + 0.2, z, 10, 0.25, 0.2, 0.25, 0.02);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, een.getItem()), player.getX(), player.getEyeY() - 0.15, player.getZ(),
                6, 0.12, 0.08, 0.12, 0.04);
        level.sendParticles(ParticleTypes.HEART, player.getX(), player.getEyeY() + 0.6, player.getZ(), 1, 0.1, 0.1, 0.1, 0);
        level.playSound(null, vuur, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1f, 1.3f);
        level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT.value(), SoundSource.PLAYERS, 0.8f, 1.2f);
        player.sendOverlayMessage(Component.translatable("gui.guhs.kamperen.marshmallow").withStyle(ChatFormatting.GOLD));
        int totaal = KnusVoortgang.tel(player, GEROOSTERD, 1);
        GuhAdvancements.grant(player, "kamperen_marshmallow");
        // the guhs nearby smell it: they come and sit by the fire, very happy
        for (GuhEntity guh : level.getEntitiesOfClass(GuhEntity.class, new AABB(vuur).inflate(GUH_AFSTAND),
                g -> !g.isOrderedToSit() && !g.isPassenger() && !g.isNoAi())) {
            LuisterGoal.luister(guh, vuur, 100);
            level.sendParticles(ParticleTypes.HEART, guh.getX(), guh.getY() + guh.getBbHeight() + 0.3, guh.getZ(), 1, 0.1, 0.1, 0.1, 0);
        }
        GuhNpcEntity opa = opa(level, vuur);
        if (opa != null) {
            GuhQuests.say(player, opa, Verhalen.vertelt(opa) ? "quest.guhs.kamperen.marshmallow_sst"
                    : "quest.guhs.kamperen.marshmallow" + level.getRandom().nextInt(OPA_ZINNEN));
        }
        return totaal;
    }

    /** The nearest Opa Guh by this campfire. */
    @Nullable
    static GuhNpcEntity opa(ServerLevel level, BlockPos vuur) {
        return level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(vuur).inflate(OPA_AFSTAND), n -> n.getKind() == GuhNpcEntity.Kind.OPA_GUH)
                .stream().min(Comparator.comparingDouble(n -> n.distanceToSqr(vuur.getCenter()))).orElse(null);
    }
}
