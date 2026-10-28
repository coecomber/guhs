package nl.juiced.guhs.feature.droomblokken;

import java.util.function.Consumer;
import java.util.function.Function;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * 1.8.0 (Guhdromenland): droomzand and the dream blocks (BOUWPLAN_180.md, D). Resources: tools/features/droomblokken.py.
 * <ul>
 *   <li>{@code guhs:droomzand}: the item; found in the dream (molentje, sterren, Blub-guhs), crafted into dream blocks;</li>
 *   <li>{@code guhs:zeepbelglas}: see-through, shifting soap colours ({@link DroomBlokken.Zeepbelglas});</li>
 *   <li>{@code guhs:droomzandblok}: storage for droomzand, sparkles drift up ({@link DroomBlokken.Droomzandblok});</li>
 *   <li>{@code guhs:slaapliedjesgras}: lavender grass that hums ({@link DroomBlokken.Slaapliedjesgras}; in #minecraft:dirt);</li>
 *   <li>{@code guhs:maanbloemetje}: only open at night, then glows ({@link DroomBlokken.Maanbloemetje}, state {@code open});</li>
 *   <li>{@code guhs:droomzee}: the sea under the islands, falling in calls {@link DroomBlokken.Droomzee#bijVallen}.</li>
 * </ul>
 * The sterrenhemel, maansteen and zeepbel blocks already exist (sterrenplafond, maansteen, zeepbel): reused, not doubled.
 */
public final class DroomblokkenFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredItem<Item> DROOMZAND = ITEMS.registerItem("droomzand", p -> new Item(p) {
        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.guhs.droomzand.lore").withStyle(ChatFormatting.GRAY));
        }
    }, Item.Properties::new);

    public static final DeferredBlock<DroomBlokken.Zeepbelglas> ZEEPBELGLAS = blok("zeepbelglas", DroomBlokken.Zeepbelglas::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.4f));
    public static final DeferredBlock<DroomBlokken.Droomzandblok> DROOMZANDBLOK = blok("droomzandblok", DroomBlokken.Droomzandblok::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(0.6f).sound(SoundType.SAND).lightLevel(s -> 6));
    public static final DeferredBlock<DroomBlokken.Slaapliedjesgras> SLAAPLIEDJESGRAS = blok("slaapliedjesgras", DroomBlokken.Slaapliedjesgras::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.6f).sound(SoundType.WOOL));
    public static final DeferredBlock<DroomBlokken.Maanbloemetje> MAANBLOEMETJE = blok("maanbloemetje", DroomBlokken.Maanbloemetje::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).noCollision().instabreak().sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.DESTROY)
                    .lightLevel(s -> s.getValue(DroomBlokken.Maanbloemetje.OPEN) ? 7 : 0));
    public static final DeferredBlock<DroomBlokken.Droomzee> DROOMZEE = blok("droomzee", DroomBlokken.Droomzee::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).noCollision().noOcclusion().strength(-1f, 3600000f)
                    .noLootTable().lightLevel(s -> 5).isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false)
                    .isValidSpawn((s, l, p, e) -> false).pushReaction(PushReaction.BLOCK));

    /** A block with its item; the item shows the block's lore line (block.guhs.&lt;id&gt;.lore). */
    private static <B extends Block> DeferredBlock<B> blok(String id, Function<BlockBehaviour.Properties, B> maak,
                                                          java.util.function.Supplier<BlockBehaviour.Properties> props) {
        DeferredBlock<B> b = BLOCKS.registerBlock(id, maak, props);
        ITEMS.registerItem(id, p -> new BlockItem(b.get(), p) {
            @Override
            public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
                tooltip.accept(Component.translatable("block.guhs." + id + ".lore").withStyle(ChatFormatting.GRAY));
            }
        }, () -> new Item.Properties().useBlockDescriptionPrefix());
        return b;
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(DROOMZAND.get()));
        for (DeferredBlock<?> b : java.util.List.of(ZEEPBELGLAS, DROOMZANDBLOK, SLAAPLIEDJESGRAS, MAANBLOEMETJE, DROOMZEE)) {
            output.accept(new ItemStack(b.get()));
        }
    }

    private DroomblokkenFeature() {
    }
}
