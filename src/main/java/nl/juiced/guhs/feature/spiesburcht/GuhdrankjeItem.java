package nl.juiced.guhs.feature.spiesburcht;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.quest.GuhAdvancements;

/** A Guhdrankje from the Guhbrouwketel: drink it like a potion; the bottle stays. */
public class GuhdrankjeItem extends Item {
    public final Brouwsel brouwsel;

    public GuhdrankjeItem(Brouwsel brouwsel, Properties properties) {
        super(properties);
        this.brouwsel = brouwsel;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            for (MobEffectInstance effect : brouwsel.effects()) {
                entity.addEffect(new MobEffectInstance(effect));
            }
            level.playSound(null, entity.blockPosition(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 1f, 1.2f);
            if (entity instanceof ServerPlayer player) {
                player.awardStat(Stats.ITEM_USED.get(this));
                GuhAdvancements.grant(player, "guhdrankje_gedronken");
                if (brouwsel == Brouwsel.VAHOEGHEID) {
                    player.displayClientMessage(Component.translatable("quest.guhs.spiesburcht.drank_vahoeg").withStyle(ChatFormatting.LIGHT_PURPLE), true);
                }
            }
        }
        if (entity instanceof Player player) {
            return player.getAbilities().instabuild ? stack : ItemUtils.createFilledResult(stack, player, new ItemStack(Items.GLASS_BOTTLE));
        }
        stack.shrink(1);
        return stack.isEmpty() ? new ItemStack(Items.GLASS_BOTTLE) : stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (MobEffectInstance effect : brouwsel.effects()) {
            if (effect.getDuration() < 20) {
                tooltip.add(Component.translatable("item.guhs.guhdrankje.verzadiging").withStyle(ChatFormatting.BLUE));
                continue;
            }
            Component name = Component.translatable(effect.getDescriptionId());
            if (effect.getAmplifier() > 0) {
                name = Component.translatable("potion.withAmplifier", name, Component.translatable("potion.potency." + effect.getAmplifier()));
            }
            tooltip.add(Component.translatable("potion.withDuration", name, MobEffectUtil.formatDuration(effect, 1.0f, context.tickRate()))
                    .withStyle(ChatFormatting.BLUE));
        }
        tooltip.add(Component.translatable("item.guhs.guhdrankje." + brouwsel.id() + ".lore").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
    }
}
