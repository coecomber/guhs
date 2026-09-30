package nl.juiced.guhs.feature.bakkerij;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;
import net.minecraft.world.entity.EquipmentSlot;
/**
 * A pastry of the Knabbelbakkerij (one of the twelve {@link Recept#BOEK} recipes): food with a cute little effect, and
 * a burst of hearts and knabbelwolkjes when you eat it.
 * <p>
 * During Bakker Korstje's order game the pastries you bake are "for a customer" (custom data {@value #SPEL}): they
 * can't be eaten, only served, and they're gone as soon as the game is over (outside a game they vanish from any
 * inventory they tick in, and dropped they disappear).
 */
public class BakjeItem extends Item {
    /** Custom data: a game pastry (boolean) and its quality (the ordinal of {@link Recept.Kwaliteit}). */
    public static final String SPEL = "BakkerijSpel", KWALITEIT = "Kwaliteit";

    public final Recept recept;

    public BakjeItem(Recept recept, Properties properties) {
        super(properties);
        this.recept = recept;
    }

    /** A pastry for a customer in the order game. */
    public static ItemStack voorKlant(Recept recept, Recept.Kwaliteit kwaliteit) {
        ItemStack stack = new ItemStack(BakkerijFeature.bakje(recept));
        CompoundTag tag = new CompoundTag();
        tag.putBoolean(SPEL, true);
        tag.putInt(KWALITEIT, kwaliteit.ordinal());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }

    public static boolean isSpel(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBooleanOr(SPEL, false);
    }

    @Nullable
    public static Recept.Kwaliteit kwaliteit(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null || !data.copyTag().contains(KWALITEIT) ? null : Recept.Kwaliteit.byIndex(data.copyTag().getIntOr(KWALITEIT, 0));
    }

    @Nullable
    public static Recept recept(ItemStack stack) {
        if (stack.getItem() instanceof BakjeItem bakje) {
            return bakje.recept;
        }
        return stack.is(BakkerijFeature.FEESTTAART.get()) ? Recept.FEESTTAART : null;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isSpel(stack)) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("gui.guhs.bakkerij.voor_klant").withStyle(ChatFormatting.GOLD));
            }
            return InteractionResult.FAIL;
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity eater) {
        if (level instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HEART, eater.getX(), eater.getY() + eater.getBbHeight() + 0.3, eater.getZ(), 4, 0.35, 0.15, 0.35, 0);
            server.sendParticles(BakkerijFeature.KNABBELWOLKJE.get(), eater.getX(), eater.getY() + eater.getBbHeight() * 0.8, eater.getZ(),
                    6, 0.3, 0.2, 0.3, 0.01);
            server.playSound(null, eater.blockPosition(), nl.juiced.guhs.registry.ModSounds.GUH_HAPPY.get(), SoundSource.PLAYERS, 0.5f, 1.4f);
            if (eater instanceof ServerPlayer p) {
                p.sendOverlayMessage(Component.translatable("item.guhs." + recept.id() + ".njam").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        return super.finishUsingItem(stack, level, eater);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @org.jspecify.annotations.Nullable EquipmentSlot equipSlot) {
        if (!level.isClientSide() && isSpel(stack) && (!(entity instanceof ServerPlayer player) || !BakkerijGame.isPlaying(player))) {
            stack.setCount(0);
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide() && isSpel(stack)) {
            entity.discard();
            return true;
        }
        return false;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return kwaliteit(stack) == Recept.Kwaliteit.PERFECT || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        if (isSpel(stack)) {
            Recept.Kwaliteit k = kwaliteit(stack);
            tooltip.accept(Component.translatable("gui.guhs.bakkerij.voor_klant.tooltip").withStyle(ChatFormatting.GOLD));
            if (k != null) {
                tooltip.accept(k.naam().copy().withStyle(k == Recept.Kwaliteit.PERFECT ? ChatFormatting.YELLOW : ChatFormatting.GRAY));
            }
        } else {
            tooltip.accept(Component.translatable("item.guhs." + recept.id() + ".lore").withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.translatable("gui.guhs.bakkerij.recept.regel", recept.deeg.naam(), recept.vorm.naam(), recept.topping.naam())
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
