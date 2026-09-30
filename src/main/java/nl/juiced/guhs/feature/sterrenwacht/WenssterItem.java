package nl.juiced.guhs.feature.sterrenwacht;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.registry.ModItems;

/**
 * A wensster: what a guh constellation gives you. Hold it up (right-click) and make a wish: the star flies off with a
 * sparkle and you get a little present ({@link #wens}). Or pay Professor Sterretje with it.
 */
public class WenssterItem extends Item {
    public WenssterItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.guhs.wensster.lore").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            wens(serverPlayer, level.getRandom());
            stack.consume(1, player);
            player.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
    }

    /**
     * A wish: one of a few little presents (every count one more than it would be: the "+1 per reward" rule). Returns
     * the lang key of what came true.
     */
    public static String wens(ServerPlayer player, RandomSource random) {
        int roll = random.nextInt(100);
        String what;
        if (roll < 34) {
            Minigames.give(player, new ItemStack(ModItems.KAAS_KNABBELS.get(), 4 + random.nextInt(5) + 1));
            what = "knabbels";
        } else if (roll < 54) {
            Minigames.give(player, new ItemStack(ModItems.GEFRITUURDE_KAASKNABBELS.get(), 1 + random.nextInt(2) + 1));
            what = "gefrituurd";
        } else if (roll < 68) {
            Minigames.give(player, new ItemStack(ModItems.GUH_BALLON.get(), 1 + 1));
            what = "ballon";
        } else if (roll < 80) {
            Minigames.give(player, new ItemStack(SterrenwachtFeature.STERRENLANTAARN_ITEM.get(), 1 + 1));
            Sterrenkijken.lantaarnGekregen(player);
            what = "lantaarn";
        } else if (roll < 96) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 20 * 60 * 5, 0));
            player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 20 * 60, 1));
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 60 * 3, 0));
            what = "geluk";
        } else {
            Minigames.give(player, new ItemStack(ModItems.VAHOEGE_VADS_INGOT.get(), 1 + 1));
            what = "vahoeg";
        }
        player.sendSystemMessage(Component.translatable("gui.guhs.sterrenwacht.wens." + what).withStyle(ChatFormatting.LIGHT_PURPLE));
        ServerLevel level = player.level();
        for (int i = 0; i < 12; i++) {       // the star flies up, sparkling
            level.sendParticles(SterrenwachtFeature.WENSSTER_DEELTJE.get(), player.getX() + player.getLookAngle().x * (0.5 + i * 0.3),
                    player.getEyeY() + i * 0.35, player.getZ() + player.getLookAngle().z * (0.5 + i * 0.3), 2, 0.05, 0.05, 0.05, 0.01);
        }
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getEyeY() + 4, player.getZ(), 10, 0.4, 0.4, 0.4, 0.05);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.4f);
        level.playSound(null, player.blockPosition(), SterrenwachtFeature.STER_KLIK.get(), SoundSource.PLAYERS, 1f, 1.6f);
        KnusVoortgang.tel(player, Sterrenkijken.WENSEN, 1);
        GuhAdvancements.grant(player, "sterrenwacht_wens");
        return "gui.guhs.sterrenwacht.wens." + what;
    }
}
