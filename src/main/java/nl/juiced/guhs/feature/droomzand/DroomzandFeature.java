package nl.juiced.guhs.feature.droomzand;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * 1.10.10 economie: droomzand comes noticeably faster. Here the zandhoopjes ({@link Zandhoopje}, {@link Zandhoopjes}); the
 * other changes live with their own features: the golden star and the Sterrenreeks (sterrenveld), x3 droomzand from the
 * concert, the Zandguh's bag, the droomkistje, the Vliegdroom and the big star, the cheaper droomwinkel (droomwinkel).
 */
public final class DroomzandFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredBlock<Zandhoopje> ZANDHOOPJE = BLOCKS.registerBlock("droomzandhoopje", Zandhoopje::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(-1f, 3600000f).noLootTable().noOcclusion()
                    .sound(SoundType.SAND).lightLevel(s -> 4));

    static {
        ITEMS.registerItem("droomzandhoopje", p -> new BlockItem(ZANDHOOPJE.get(), p) {
            @Override
            public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
                tooltip.accept(Component.translatable("block.guhs.droomzandhoopje.lore").withStyle(ChatFormatting.GRAY));
            }
        }, () -> new Item.Properties().useBlockDescriptionPrefix());
    }

    private DroomzandFeature() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        Zandhoopjes.register();
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(ZANDHOOPJE.get()));
    }
}
