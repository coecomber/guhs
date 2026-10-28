package nl.juiced.guhs.feature.droomspul;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.dromen.SlaapplekBlock;

/**
 * 1.8.0 Guhdromenland, the finishing touches ("droomspul"):
 * <ul>
 *   <li>three more boxes a guh can be tucked into (verhuisdoos, schoenendoos, pizzadoos): {@link SlaapplekBlock}s in the tag
 *       guhs:guh_slaapplekken, so tucking in works exactly as with the doos met knabbels;</li>
 *   <li>the tuck-in items the start tags guhs:droom_start/&lt;eiland&gt; were waiting for: the zeepje (Wolkenbadje), the
 *       sterlampje (Sterrenkussenveld) and the fotootje (Herinneringseiland);</li>
 *   <li>{@link DroomBewoners}: the guhs and Blubs that live on the islands (put there by code, because entities in a
 *       template skip their spawn setup).</li>
 * </ul>
 * Resources and recipes (also for the droombrievenbusjes and the droomwekker): tools/features/droomspul.py.
 */
public final class DroomspulFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** The extra boxes (tools/features/droomspul.py: DOZEN; add at the end, never reorder). */
    public static final List<String> DOZEN = List.of("verhuisdoos", "schoenendoos", "pizzadoos");
    public static final Map<String, DeferredBlock<SlaapplekBlock>> DOOS_BLOKKEN = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<BlockItem>> DOOS_ITEMS = new LinkedHashMap<>();

    static {
        for (String doos : DOZEN) {
            DeferredBlock<SlaapplekBlock> b = BLOCKS.registerBlock(doos, p -> new SlaapplekBlock(p, SlaapplekBlock.DOOS),
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_WOOL).mapColor(MapColor.COLOR_BROWN).strength(0.6f)
                            .sound(SoundType.WOOL).noOcclusion());
            DOOS_BLOKKEN.put(doos, b);
            DOOS_ITEMS.put(doos, ITEMS.registerSimpleBlockItem(b));
        }
    }

    public static final DeferredItem<Item> ZEEPJE = item("zeepje");
    public static final DeferredItem<Item> STERLAMPJE = item("sterlampje");
    public static final DeferredItem<Item> FOTOOTJE = item("fotootje");

    /** A plain little item with its lore line (item.guhs.&lt;naam&gt;.lore). */
    private static DeferredItem<Item> item(String naam) {
        return ITEMS.registerItem(naam, p -> new Item(p) {
            @Override
            public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip,
                                        TooltipFlag flag) {
                tooltip.accept(Component.translatable("item.guhs." + naam + ".lore").withStyle(ChatFormatting.GRAY));
            }
        });
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        DroomBewoners.register();
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<BlockItem> i : DOOS_ITEMS.values()) {
            output.accept(new ItemStack(i.get()));
        }
        output.accept(new ItemStack(ZEEPJE.get()));
        output.accept(new ItemStack(STERLAMPJE.get()));
        output.accept(new ItemStack(FOTOOTJE.get()));
    }

    private DroomspulFeature() {
    }
}
