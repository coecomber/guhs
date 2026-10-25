package nl.juiced.guhs.feature.techbeurs;

import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;

/**
 * 1.7.0 (ronde 45-47, "Vadskracht in de structuren"): the Vadslamp ({@link VadslampBlock}: a guh lantern on 1 vadskracht
 * that lights a much bigger area than a normal lamp) and the Guhtechniek-beurs. Resources: tools/features/vadslamp.py.
 */
public final class TechbeursFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    /** {@code guhs:vadslamp}: the Vadslamp (1 VK, light 15 + its light blocks). */
    public static final DeferredBlock<VadslampBlock> VADSLAMP = BLOCKS.registerBlock("vadslamp", VadslampBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5f, 6f).sound(SoundType.LANTERN)
                    .noOcclusion().lightLevel(VadslampBlock::licht).pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> VADSLAMP_ITEM = item("vadslamp", VADSLAMP);
    /** {@code guhs:vadslamp_licht}: the invisible light around a burning Vadslamp (no item). */
    public static final DeferredBlock<VadslampLicht> VADSLAMP_LICHT = BLOCKS.registerBlock("vadslamp_licht", VadslampLicht::new,
            () -> BlockBehaviour.Properties.of().replaceable().noCollision().noOcclusion().noLootTable().instabreak()
                    .lightLevel(s -> VadslampLicht.LICHT).randomTicks().pushReaction(PushReaction.DESTROY));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VadslampBlockEntity>> VADSLAMP_BE = BLOCK_ENTITIES.register(
            "vadslamp", () -> new BlockEntityType<>(VadslampBlockEntity::new, VADSLAMP.get()));

    /** A block item with the line {@code block.guhs.<id>.lore} under its name. */
    private static DeferredItem<BlockItem> item(String id, Supplier<? extends Block> block) {
        return ITEMS.registerItem(id, p -> new BlockItem(block.get(), p) {
            @Override
            public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
                tooltip.accept(Component.translatable("block.guhs." + id + ".lore").withStyle(ChatFormatting.GRAY));
            }
        }, () -> new Item.Properties());
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(TechbeursFeature::capabilities);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        VadskrachtFeature.knoopCapability(event, VADSLAMP_BE.get());
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(VADSLAMP_ITEM.get()));
    }

    private TechbeursFeature() {
    }
}
