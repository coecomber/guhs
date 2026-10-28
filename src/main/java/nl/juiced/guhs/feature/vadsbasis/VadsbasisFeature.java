package nl.juiced.guhs.feature.vadsbasis;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadskracht.VadsGetallen;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;

/**
 * 1.8.0 (vadskracht, "vadsbasis"): the Knabbelketel ({@link KnabbelketelBlock}), an 80 VK source that eats kaasknabbels.
 * The other parts of this slice live where they belong: the Guhraden's afnemende opbrengst in {@code BronSoort}/{@code VadsNet},
 * the Wolkenantennes in {@code weer}, the Knuffelsteen-generator, Sausbuis and Saustank in {@code techsaus}.
 * Resources: tools/features/vadsbasis.py.
 */
public final class VadsbasisFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    public static final DeferredBlock<KnabbelketelBlock> KNABBELKETEL = BLOCKS.registerBlock("knabbelketel", KnabbelketelBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(3.0f, 6f).sound(SoundType.METAL).noOcclusion()
                    .lightLevel(KnabbelketelBlock::licht).pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> KNABBELKETEL_ITEM = ITEMS.registerItem("knabbelketel", p -> new BlockItem(KNABBELKETEL.get(), p) {
        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("block.guhs.knabbelketel.lore").withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("block.guhs.knabbelketel.lore.kracht", VadsGetallen.KNABBELKETEL, VadsGetallen.KETEL_KNABBELS_PER_UUR)
                    .withStyle(ChatFormatting.DARK_PURPLE));
        }
    }, () -> new Item.Properties().useBlockDescriptionPrefix());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KnabbelketelBlockEntity>> KNABBELKETEL_BE = BLOCK_ENTITIES.register(
            "knabbelketel", () -> new BlockEntityType<>(KnabbelketelBlockEntity::new, KNABBELKETEL.get()));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(VadsbasisFeature::capabilities);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KNABBELKETEL_ITEM.get()));
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        VadskrachtFeature.knoopCapability(event, KNABBELKETEL_BE.get());
        event.registerBlockEntity(Capabilities.Item.BLOCK, KNABBELKETEL_BE.get(), KnabbelketelBlockEntity::handler);
    }

    private VadsbasisFeature() {
    }
}
