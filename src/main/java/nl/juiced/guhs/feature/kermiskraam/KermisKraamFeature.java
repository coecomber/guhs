package nl.juiced.guhs.feature.kermiskraam;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * 1.8.1: the stalls of the bigger guh kermis (kermis-ontwerp/BOUWPLAN_181.md): four food stalls (Kaas Knabbels) and three game
 * stalls (kermisbonnen). Each stall is one block, {@code guhs:kermiskraam_<id>}, put down by the kermis stamp; the block looks
 * after its own stall guh. Resources: tools/features/kermiskraam.py.
 */
public final class KermisKraamFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    public static final Map<Kraam, DeferredBlock<KermisKraamBlock>> KRAMEN = new EnumMap<>(Kraam.class);
    public static final Map<Kraam, DeferredItem<BlockItem>> KRAAM_ITEMS = new EnumMap<>(Kraam.class);

    static {
        for (Kraam kraam : Kraam.values()) {
            DeferredBlock<KermisKraamBlock> blok = BLOCKS.registerBlock(kraam.blok(), p -> new KermisKraamBlock(kraam, p),
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor(MapColor.COLOR_PINK).strength(2f).sound(SoundType.WOOD).noOcclusion());
            KRAMEN.put(kraam, blok);
            KRAAM_ITEMS.put(kraam, ITEMS.registerItem(kraam.blok(), p -> new BlockItem(blok.get(), p) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
                    tooltip.accept(Component.translatable("block.guhs." + kraam.blok() + ".lore").withStyle(ChatFormatting.GRAY));
                }
            }, () -> new Item.Properties().useBlockDescriptionPrefix()));
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KermisKraamBlockEntity>> KRAAM_BE = BLOCK_ENTITY_TYPES.register("kermiskraam",
            () -> new BlockEntityType<>(KermisKraamBlockEntity::new, KRAMEN.values().stream().map(DeferredBlock::get).toArray(Block[]::new)));

    public static final DeferredItem<KraamEten> CHURROS = eten("churros", 5, 0.6f, false);
    public static final DeferredItem<KraamEten> CHURROS_GUHTELLA = eten("churros_guhtella", 6, 0.7f, false);
    public static final DeferredItem<KraamEten> SUIKERSPIN = eten("suikerspin", 3, 0.3f, true);
    public static final DeferredItem<KraamEten> POFFERTJES = eten("poffertjes", 5, 0.6f, false);
    public static final DeferredItem<KraamEten> OLIEBOLLEN = eten("oliebollen", 6, 0.7f, false);

    private KermisKraamFeature() {
    }

    private static DeferredItem<KraamEten> eten(String id, int voeding, float verzadiging, boolean altijd) {
        return ITEMS.registerItem(id, KraamEten::new, () -> {
            FoodProperties.Builder food = new FoodProperties.Builder().nutrition(voeding).saturationModifier(verzadiging);
            return new Item.Properties().food((altijd ? food.alwaysEdible() : food).build());
        });
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        NeoForge.EVENT_BUS.addListener(KermisKraam::onPraat);
    }

    public static void payloads(PayloadRegistrar registrar) {
        KraamPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        KRAAM_ITEMS.values().forEach(i -> output.accept(new ItemStack(i.get())));
        for (DeferredItem<KraamEten> i : List.of(CHURROS, CHURROS_GUHTELLA, SUIKERSPIN, POFFERTJES, OLIEBOLLEN)) {
            output.accept(new ItemStack(i.get()));
        }
    }
}
