package nl.juiced.guhs.feature.fossiel;

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
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;

/**
 * 1.8.0 (vadskracht, ontwerp-guhs180-vadskracht.md V3-V5): de Fossiel-broedmachine ({@link BroedmachineBlock}) and the four
 * oerguhs it hatches from a fossil ({@link OerSoort}: Mammoet-guh, T-Guh, Pteroguh, Oer-stokstaartje-guh; what they do:
 * {@link OerGuhs}). The fossils ({@code guhs:fossiel_<id>}) are rare finds in the old fossil places: chest loot through
 * data/guhs/loot_modifiers/fossiel_*.json (the Fossiel-opgraving and -mijn of the Barbecuether, the reuzenguh-fossiel, the
 * verlaten guhmijn and the grotschilderingen of Onder de Wol) and now and then the opgraving's daily find
 * ({@code Opgraving.dagvondst}). Resources: tools/features/fossiel.py.
 */
public final class FossielFeature {
    /** VK while brooding (nominal: whenever it is switched on). */
    public static final int KRACHT = 20;
    /** Knabbels eaten when an egg starts. */
    public static final int KNABBELS = 16;
    /** How long an egg broods: a whole in-game day. */
    public static final int BROED_TICKS = 24000;

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    public static final DeferredBlock<BroedmachineBlock> BROEDMACHINE = BLOCKS.registerBlock("fossiel_broedmachine", BroedmachineBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_ORANGE).strength(3f, 6f).noOcclusion());
    public static final DeferredItem<BlockItem> BROEDMACHINE_ITEM = ITEMS.registerItem("fossiel_broedmachine", p -> new BlockItem(BROEDMACHINE.get(), p) {
        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("block.guhs.fossiel_broedmachine.lore").withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("block.guhs.fossiel_broedmachine.lore.kracht", KRACHT, KNABBELS).withStyle(ChatFormatting.DARK_PURPLE));
        }
    }, () -> new Item.Properties().useBlockDescriptionPrefix());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BroedmachineBlockEntity>> BROEDMACHINE_BE = BLOCK_ENTITIES.register(
            "fossiel_broedmachine", () -> new BlockEntityType<>(BroedmachineBlockEntity::new, BROEDMACHINE.get()));

    public static final Map<OerSoort, DeferredItem<Item>> FOSSIELEN = new EnumMap<>(OerSoort.class);

    static {
        for (OerSoort s : OerSoort.values()) {
            FOSSIELEN.put(s, ITEMS.registerItem(s.fossiel(), p -> new Item(p) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
                    tooltip.accept(Component.translatable("item.guhs." + s.fossiel() + ".lore").withStyle(ChatFormatting.GRAY));
                }
            }, () -> new Item.Properties().stacksTo(16)));
        }
    }

    private FossielFeature() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener((RegisterCapabilitiesEvent event) -> VadskrachtFeature.machineCapabilities(event, BROEDMACHINE_BE.get()));
        GuhHooks.tick(OerGuhs::tick);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(BROEDMACHINE_ITEM.get()));
        for (OerSoort s : OerSoort.values()) {
            output.accept(new ItemStack(FOSSIELEN.get(s).get()));
        }
    }
}
