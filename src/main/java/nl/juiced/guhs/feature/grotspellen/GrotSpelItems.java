package nl.juiced.guhs.feature.grotspellen;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The tools the cave-game hosts lend you for one game (item tag guhs:loaned: never into storage): they can't be dropped,
 * and they're gone as soon as you stop playing.
 * <ul>
 *   <li>{@link Schepje}: Kapitein Schepsnuit's soft shovel: right-click (or left-click) a block of the treasure field to dig it;</li>
 *   <li>{@link Netje}: Juf Glimmie's net: swing it (right-click) at the glimwormpjes right in front of you;</li>
 *   <li>{@link Potje}: the jar on your back: it glows brighter the more glimworms are in it (custom model data 0-4,
 *       drawn on your back by the client).</li>
 * </ul>
 */
public final class GrotSpelItems {

    /** A borrowed tool. */
    public static class Leen extends Item {
        private final String naam;

        public Leen(Properties properties, String naam) {
            super(properties);
            this.naam = naam;
        }

        /** Only players in a cave game keep it. */
        @Override
        public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @org.jspecify.annotations.Nullable EquipmentSlot slot) {
            if (!(entity instanceof Player player) || !GrotSpel.speelt(player)) {
                stack.setCount(0);
            }
        }

        @Override
        public boolean onDroppedByPlayer(ItemStack item, Player player) {
            return false;
        }

        @Override
        public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
            if (!entity.level().isClientSide()) {
                entity.discard();
            }
            return true;
        }

        @Override
        public boolean canFitInsideContainerItems() {
            return false;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.guhs." + naam + ".lore").withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("item.guhs.grotspel.leen").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** The soft shovel of Schatgraven. */
    public static class Schepje extends Leen {
        public Schepje(Properties properties) {
            super(properties, "schatschepje");
        }

        @Override
        public InteractionResult useOn(UseOnContext context) {
            if (context.getPlayer() instanceof ServerPlayer p && !context.getLevel().isClientSide()) {
                Schatgraven.graaf(p, context.getClickedPos());
            }
            return context.getLevel().getBlockState(context.getClickedPos()).is(GrotSpellenFeature.SCHATGROND.get())
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
    }

    /** The glimworm net. */
    public static class Netje extends Leen {
        public Netje(Properties properties) {
            super(properties, "glimnetje");
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (player instanceof ServerPlayer p && !level.isClientSide()) {
                Glimwormvangen.zwaai(p);
                p.getCooldowns().addCooldown(player.getItemInHand(hand), Glimwormvangen.ZWAAI_PAUZE);
            }
            player.swing(hand);
            return InteractionResult.SUCCESS;
        }
    }

    /** The glimworm jar on your back. */
    public static class Potje extends Leen {
        public Potje(Properties properties) {
            super(properties, "glimpotje_rug");
        }
    }

    /** How bright the jar is (0-4). */
    public static int stand(ItemStack stack) {
        CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return data == null || data.floats().isEmpty() ? 0 : Math.round(data.floats().get(0));
    }

    public static void zetStand(ItemStack stack, int stand) {
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(java.util.List.of((float) stand), java.util.List.of(), java.util.List.of(),
                java.util.List.of()));
    }

    private GrotSpelItems() {
    }
}
