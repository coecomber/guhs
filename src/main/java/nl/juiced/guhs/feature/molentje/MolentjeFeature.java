package nl.juiced.guhs.feature.molentje;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.droomwezens.DroomNpc;

/**
 * 1.8.0 Guhdromenland, island "molentje" (dromen-ontwerp/BOUWPLAN_180.md part E; ontwerp round 10): Het Maanmolentje, where
 * the Zandguh grinds dreams into droomzand. The island itself is a template (tools/features/molentje.py, stamped by part A's
 * DroomEilanden); this feature adds its activity ({@link Molen}: Blubs on the blowing cushions, the cog puzzle, the turning
 * sails, the Zandguh's bag of droomzand and Snoes' piece) and its three blocks ({@link MolenBlokken}).
 * Test commands: {@link MolentjeCommando} (/guhs molentje ...).
 */
public final class MolentjeFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    private static <B extends Block> DeferredBlock<B> blok(String id, Function<BlockBehaviour.Properties, B> maak, Supplier<BlockBehaviour.Properties> props) {
        DeferredBlock<B> b = BLOCKS.registerBlock(id, maak, props);
        ITEMS.registerItem(id, p -> new BlockItem(b.get(), p) {
            @Override
            public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
                tooltip.accept(Component.translatable("block.guhs." + id + ".lore").withStyle(ChatFormatting.GRAY));
            }
        }, () -> new Item.Properties().useBlockDescriptionPrefix());
        return b;
    }

    public static final DeferredBlock<MolenBlokken.Tandwiel> TANDWIEL = blok("molen_tandwiel", MolenBlokken.Tandwiel::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(1.5f).sound(SoundType.COPPER).noOcclusion());
    public static final DeferredBlock<MolenBlokken.Kussentje> KUSSENTJE = blok("blaaskussentje", MolenBlokken.Kussentje::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.5f).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredBlock<MolenBlokken.Wieken> WIEKEN = blok("molenwieken", MolenBlokken.Wieken::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5f).sound(SoundType.WOOD).noOcclusion());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MolenBlokken.WiekenBlockEntity>> WIEKEN_BE = BLOCK_ENTITIES.register(
            "molenwieken", () -> new BlockEntityType<>(MolenBlokken.WiekenBlockEntity::new, WIEKEN.get()));

    public static final List<DeferredBlock<? extends Block>> ALLE = List.of(TANDWIEL, KUSSENTJE, WIEKEN);

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        NeoForge.EVENT_BUS.addListener(Molen::tick);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> MolentjeCommando.register(event));
        // (integration) the Zandguh talk is chained in SnoesVerhaalFeature (molen first, then the story hint)
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredBlock<? extends Block> b : ALLE) {
            output.accept(new ItemStack(b.get()));
        }
    }

    private MolentjeFeature() {
    }
}
