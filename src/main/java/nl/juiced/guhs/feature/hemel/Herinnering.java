package nl.juiced.guhs.feature.hemel;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.entity.GuhEntity;

import net.minecraft.core.UUIDUtil;
/**
 * 3.0: the glowing star "Herinnering aan &lt;naam&gt;" that a tamed guh leaves behind when it goes to the wolkjes
 * (CONTRACT_30 §4.6; dropped by nl.juiced.guhs.feature.band.Wolkjes). Custom data {@value #TAG} = {Band: UUID, Naam, Variant}.
 * <ul>
 *   <li>it twinkles (an animated icon, the enchantment glint, little stars around you while you hold it);</li>
 *   <li>its tooltip says who it is (the variant) and what the Knuffelhart can do;</li>
 *   <li>right-click: you hug the memory (hearts, a twinkle, a sweet line);</li>
 *   <li>brought to the Knuffelhart ({@link KnuffelhartBlock}): that very guh comes back, the star goes with it.</li>
 * </ul>
 */
public class Herinnering extends Item {
    public static final String TAG = "guhs_herinnering";

    public Herinnering(Properties properties) {
        super(properties);
    }

    /** The star of this guh ("Herinnering aan Wolkje"). */
    public static ItemStack maak(GuhEntity guh) {
        ItemStack stack = new ItemStack(HemelFeature.HERINNERING.get());
        CompoundTag data = new CompoundTag();
        CompoundTag h = new CompoundTag();
        h.store("Band", UUIDUtil.CODEC, nl.juiced.guhs.feature.band.Band.id(guh));
        h.putString("Naam", guh.getName().getString());
        h.putString("Variant", guh.getVariant().id());
        data.put(TAG, h);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("item.guhs.herinnering.van", guh.getName().getString())
                .withStyle(s -> s.withItalic(false).withColor(ChatFormatting.LIGHT_PURPLE)));
        return stack;
    }

    /** The data of a star (empty when it has none). */
    public static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getCompoundOrEmpty(TAG);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        CompoundTag d = data(stack);
        if (!d.getStringOr("Variant", "").isEmpty()) {
            tooltip.add(Component.translatable("item.guhs.herinnering.variant", Component.translatable("entity.guhs.guh." + d.getStringOr("Variant", "")))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        tooltip.add(Component.translatable("item.guhs.herinnering.lore").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.guhs.herinnering.naar_het_hart").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("item.guhs.herinnering.ook_zonder").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        tooltip.add(Component.translatable("item.guhs.herinnering.knuffel").withStyle(ChatFormatting.DARK_GRAY));
    }

    /** Hugging the memory: hearts, a twinkle and a sweet line (a little cooldown so it stays special). */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            String naam = data(stack).getStringOr("Naam", "");
            player.sendOverlayMessage(naam.isEmpty() ? Component.translatable("gui.guhs.hemel.ster.leeg").withStyle(ChatFormatting.LIGHT_PURPLE)
                    : Component.translatable("gui.guhs.hemel.ster.knuffel", naam).withStyle(ChatFormatting.LIGHT_PURPLE));
            server.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.4, player.getZ(), 5, 0.4, 0.3, 0.4, 0.02);
            server.sendParticles(HemelFeature.STERRETJE.get(), player.getX(), player.getY() + 1.2, player.getZ(), 14, 0.5, 0.5, 0.5, 0.03);
            server.playSound(null, player, HemelFeature.STER.get(), SoundSource.PLAYERS, 0.8f, 1f);
        }
        player.getCooldowns().addCooldown(this, 40);
        return InteractionResult.SUCCESS.heldItemTransformedTo(stack);
    }

    /** (client) a few little stars around you while you hold it. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide() && selected && level.getRandom().nextInt(10) == 0) {
            level.addParticle(HemelFeature.STERRETJE.get(), entity.getX() + (level.getRandom().nextDouble() - 0.5) * 1.2,
                    entity.getY() + 0.6 + level.getRandom().nextDouble() * 1.2, entity.getZ() + (level.getRandom().nextDouble() - 0.5) * 1.2, 0, 0.02, 0);
        }
    }
}
