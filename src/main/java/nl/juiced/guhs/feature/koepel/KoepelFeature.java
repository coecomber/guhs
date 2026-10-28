package nl.juiced.guhs.feature.koepel;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.feature.weer.WeerZones;

/**
 * 1.8.0 (Dromen en vadskracht): the endgame machines of De Koepel, the roof of the Guhtechniek-beurs
 * (ontwerp-guhs180-vadskracht.md, V2-V6): the {@link MegaOvenBlockEntity Mega-oven}, the {@link WeerstationBlockEntity
 * Knabbel-weerstation}, the {@link SterrenkoepelBlockEntity Sterrenkoepel} (the Vadsigheids-monument), the
 * {@link DraaimolenBlockEntity Draaimolen} and the {@link WolkenvangerBlockEntity Wolkenvanger}. One block class
 * ({@link KoepelBlock}); every number in {@link KoepelGetallen}; resources and texts: tools/features/koepel.py.
 */
public final class KoepelFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    public static final Map<KoepelBlock.Soort, DeferredBlock<KoepelBlock>> BLOKKEN = new EnumMap<>(KoepelBlock.Soort.class);
    public static final Map<KoepelBlock.Soort, DeferredItem<BlockItem>> ITEMS_PER_SOORT = new EnumMap<>(KoepelBlock.Soort.class);

    static {
        for (KoepelBlock.Soort s : KoepelBlock.Soort.values()) {
            DeferredBlock<KoepelBlock> blok = BLOCKS.registerBlock(s.id, p -> new KoepelBlock(s, p),
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_PINK).strength(3f, 6f).noOcclusion());
            BLOKKEN.put(s, blok);
            ITEMS_PER_SOORT.put(s, ITEMS.registerItem(s.id, p -> new BlockItem(blok.get(), p) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
                    tooltip.accept(Component.translatable("block.guhs." + s.id + ".lore").withStyle(ChatFormatting.GRAY));
                    tooltip.accept(Component.translatable("block.guhs.koepel.lore.kracht", vraag(s)).withStyle(ChatFormatting.DARK_PURPLE));
                }
            }, () -> new Item.Properties().useBlockDescriptionPrefix()));
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MegaOvenBlockEntity>> MEGA_OVEN_BE = BLOCK_ENTITIES.register(
            "mega_oven", () -> new BlockEntityType<>(MegaOvenBlockEntity::new, BLOKKEN.get(KoepelBlock.Soort.MEGA_OVEN).get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WeerstationBlockEntity>> WEERSTATION_BE = BLOCK_ENTITIES.register(
            "knabbel_weerstation", () -> new BlockEntityType<>(WeerstationBlockEntity::new, BLOKKEN.get(KoepelBlock.Soort.WEERSTATION).get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SterrenkoepelBlockEntity>> STERRENKOEPEL_BE = BLOCK_ENTITIES.register(
            "sterrenkoepel", () -> new BlockEntityType<>(SterrenkoepelBlockEntity::new, BLOKKEN.get(KoepelBlock.Soort.STERRENKOEPEL).get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DraaimolenBlockEntity>> DRAAIMOLEN_BE = BLOCK_ENTITIES.register(
            "draaimolen", () -> new BlockEntityType<>(DraaimolenBlockEntity::new, BLOKKEN.get(KoepelBlock.Soort.DRAAIMOLEN).get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WolkenvangerBlockEntity>> WOLKENVANGER_BE = BLOCK_ENTITIES.register(
            "wolkenvanger", () -> new BlockEntityType<>(WolkenvangerBlockEntity::new, BLOKKEN.get(KoepelBlock.Soort.WOLKENVANGER).get()));

    public static KoepelBlock blok(KoepelBlock.Soort s) {
        return BLOKKEN.get(s).get();
    }

    /** What a machine asks (for the tooltip). */
    public static int vraag(KoepelBlock.Soort s) {
        return switch (s) {
            case MEGA_OVEN -> KoepelGetallen.MEGA_OVEN;
            case WEERSTATION -> KoepelGetallen.WEERSTATION;
            case STERRENKOEPEL -> KoepelGetallen.STERRENKOEPEL;
            case DRAAIMOLEN -> KoepelGetallen.DRAAIMOLEN;
            case WOLKENVANGER -> KoepelGetallen.WOLKENVANGER;
        };
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener((RegisterCapabilitiesEvent event) -> {
            VadskrachtFeature.machineCapabilities(event, MEGA_OVEN_BE.get());
            VadskrachtFeature.machineCapabilities(event, WEERSTATION_BE.get());
            VadskrachtFeature.machineCapabilities(event, STERRENKOEPEL_BE.get());
            VadskrachtFeature.machineCapabilities(event, DRAAIMOLEN_BE.get());
            VadskrachtFeature.machineCapabilities(event, WOLKENVANGER_BE.get());
        });
        // the Knabbel-weerstation widens the Weer vadsers near it (the biggest radius wins)
        var eerder = WeerZones.STRAAL_VOOR;
        WeerZones.STRAAL_VOOR = (level, pos) -> Math.max(eerder.applyAsInt(level, pos), WeerstationBlockEntity.straalVoor(level, pos));
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<BlockItem> item : ITEMS_PER_SOORT.values()) {
            output.accept(new ItemStack(item.get()));
        }
    }

    private KoepelFeature() {
    }
}
