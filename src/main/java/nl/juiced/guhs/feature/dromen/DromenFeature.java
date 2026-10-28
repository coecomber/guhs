package nl.juiced.guhs.feature.dromen;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * 1.8.0 Guhdromenland, part A (dromen-ontwerp/BOUWPLAN_180.md; design ontwerp-guhs180-dromen.md): the dream dimension
 * {@code guhs:guhdromenland} and the way in and out.
 * <ul>
 *   <li>{@link DroomSlapen}: tuck your guh in (right-click a {@link #SLAAPPLEKKEN} block in the evening; what you hold goes
 *       along and picks the island, {@link DroomStart}), then sleep in a bed close by: you wake up in its dream.</li>
 *   <li>{@link DroomReis}: to the dream and back home (the droomkussen, the Wekkerguh, dying in a dream), and floating from
 *       island to island (the steigers).</li>
 *   <li>{@link DroomEilanden}: the Droomplein and the five islands on fixed spots; each island is a template of its own
 *       island agent, with a placeholder until it exists.</li>
 *   <li>{@link DroomRegels}: no building or breaking, soft low gravity, a Blub bubble out of the droomzee.</li>
 *   <li>{@link DroomVoortgang}: the story API (Snoes' five pieces, the finale, "Snoes geholpen").</li>
 *   <li>{@link Wekkerguh}: the alarm-clock guh on the plein (talk = wake up).</li>
 * </ul>
 * Test commands: {@link DroomCommando} (/guhs droom ...). Resources: tools/features/dromen.py.
 */
public final class DromenFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final ResourceKey<Level> DIM = ResourceKey.create(Registries.DIMENSION, Guhs.id("guhdromenland"));

    /** Where a guh can be tucked in: the doos met knabbels, the kattenhuisjes, the guhbedje and the guhnestje (and more later). */
    public static final TagKey<Block> SLAAPPLEKKEN = TagKey.create(Registries.BLOCK, Guhs.id("guh_slaapplekken"));

    /** The colours of the kattenhuisje (tools/features/dromen.py: KATTENHUISJES; add at the end, never reorder). */
    public static final String[] KATTENHUISJE_KLEUREN = {"roze", "wit", "lavendel", "mint"};

    public static final DeferredBlock<SlaapplekBlock> DOOS_KNABBELS = BLOCKS.registerBlock("doos_knabbels",
            p -> new SlaapplekBlock(p, SlaapplekBlock.DOOS),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_WOOL).mapColor(MapColor.COLOR_BROWN).strength(0.6f).sound(SoundType.WOOL)
                    .noOcclusion());
    public static final Map<String, DeferredBlock<SlaapplekBlock>> KATTENHUISJES = new LinkedHashMap<>();
    /**
     * TEMPORARY (until part D's guhs:droomzee exists): the lavender sea floor far below the islands. The dimension's flat
     * layer uses it (tools/features/dromen.py); {@link DroomRegels} bubbles anyone who sinks this low back up.
     */
    public static final DeferredBlock<Block> DROOMZEE_TIJDELIJK = BLOCKS.registerSimpleBlock("droomzee_tijdelijk",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PURPLE_CONCRETE).mapColor(MapColor.COLOR_PURPLE).lightLevel(s -> 7)
                    .strength(-1f, 3600000f).noLootTable());

    static {
        for (String kleur : KATTENHUISJE_KLEUREN) {
            KATTENHUISJES.put(kleur, BLOCKS.registerBlock("kattenhuisje_" + kleur, p -> new SlaapplekBlock(p, SlaapplekBlock.KATTENHUISJE),
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.PINK_WOOL).strength(0.8f).sound(SoundType.WOOL).noOcclusion()));
        }
    }

    public static final DeferredItem<BlockItem> DOOS_KNABBELS_ITEM = ITEMS.registerSimpleBlockItem(DOOS_KNABBELS);
    public static final Map<String, DeferredItem<BlockItem>> KATTENHUISJE_ITEMS = new LinkedHashMap<>();

    static {
        for (var e : KATTENHUISJES.entrySet()) {
            KATTENHUISJE_ITEMS.put(e.getKey(), ITEMS.registerSimpleBlockItem(e.getValue()));
        }
    }

    /** The droomkussen: right-click in a dream to wake up (you get one when you fall asleep). */
    public static final DeferredItem<Item> DROOMKUSSEN = ITEMS.registerItem("droomkussen", p -> new Item(p) {
        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("item.guhs.droomkussen.lore").withStyle(ChatFormatting.GRAY));
        }

        @Override
        public net.minecraft.world.InteractionResult use(Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
            return DroomReis.kussen(level, player);
        }
    }, () -> new Item.Properties().stacksTo(1));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        DroomSlapen.register();
        DroomRegels.register();
        DroomEilanden.register();
        Wekkerguh.register();
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> DroomCommando.register(event));
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(DOOS_KNABBELS_ITEM.get()));
        for (DeferredItem<BlockItem> i : KATTENHUISJE_ITEMS.values()) {
            output.accept(new ItemStack(i.get()));
        }
        output.accept(new ItemStack(DROOMKUSSEN.get()));
    }

    /** Is this the dream dimension? */
    public static boolean isDroom(Level level) {
        return level != null && level.dimension().equals(DIM);
    }

    private DromenFeature() {
    }
}
