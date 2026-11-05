package nl.juiced.guhs.feature.vadsrijk.zone4.boot;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.economie.RijdierEntity;

/**
 * T-Z4BOOT registry objects: the Zeilbootje entity ({@code vr_eco_zeilbootje}: an {@code vr_eco_} id like every mount, so the
 * mount system and Buitenstaanders treat it as one) and its icon item {@code vr_item_zeilbootje} ("Bootje in een fles": the
 * mount-screen / HUD icon; right-click = the boat is yours, like a saddle).
 */
public final class BootSpul {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** 2 blocks wide (the hull is longer: the model sticks out front and back), 1.1 high (the sail does not collide). */
    public static final DeferredHolder<EntityType<?>, EntityType<RijdierEntity>> ZEILBOOTJE = ENTITIES.register("vr_eco_zeilbootje",
            () -> EntityType.Builder.<RijdierEntity>of(BootEntity::new, MobCategory.MISC).sized(2.0f, 1.1f).eyeHeight(0.9f)
                    .clientTrackingRange(10).updateInterval(1).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_eco_zeilbootje"))));

    public static final DeferredItem<Fles> ICOON = ITEMS.registerItem("vr_item_zeilbootje", Fles::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    private BootSpul() {
    }

    /** "Bootje in een fles": right-click = your own Zeilbootje (Boot.geef). */
    public static class Fles extends Item {
        public Fles(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer sp) {
                if (Boot.geef(sp)) {
                    if (!sp.isCreative()) {
                        sp.getItemInHand(hand).shrink(1);
                    }
                } else {
                    sp.sendOverlayMessage(Component.translatable("vadsrijk.economie.rijdier.al").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("vadsrijk.z4boot.fles.uitleg").withStyle(ChatFormatting.GRAY));
        }
    }
}
