package nl.juiced.guhs.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Guhs.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> GUHS_TAB = TABS.register("guhs", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.guhs"))
            .icon(() -> ModItems.GUH_SPAWN_EGG.get().getDefaultInstance())
            // every item this mod registers shows up in the tab automatically
            .displayItems((params, output) -> {
                // (the old single-purpose compasses are replaced by the super compass: not in the tab any more)
                ModItems.ITEMS.getEntries().stream().filter(item -> !(item.get() instanceof nl.juiced.guhs.item.GuhCompassItem c && c.isOld()))
                        .forEach(item -> output.accept(item.get()));
                nl.juiced.guhs.feature.Features.creative(output::accept);
                // the guh paintings: a painting item that always hangs that picture
                params.holders().lookup(Registries.PAINTING_VARIANT).ifPresent(paintings -> paintings.listElements()
                        .filter(p -> p.key().identifier().getNamespace().equals(Guhs.MODID))
                        .forEach(p -> {
                            net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.PAINTING);
                            // 26.1: the variant is its own item component (was ENTITY_DATA {id, variant})
                            stack.set(net.minecraft.core.component.DataComponents.PAINTING_VARIANT, p);
                            output.accept(stack);
                        }));
            })
            .build());

    private ModCreativeTabs() {
    }
}
