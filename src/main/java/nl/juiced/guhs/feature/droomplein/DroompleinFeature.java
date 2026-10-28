package nl.juiced.guhs.feature.droomplein;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.dromen.DroomEilanden;
import nl.juiced.guhs.feature.dromen.Wekkerguh;
import nl.juiced.guhs.registry.ModEntities;

/**
 * 1.8.0 Guhdromenland, island agent "plein" (dromen-ontwerp/BOUWPLAN_180.md part E): the Droomplein itself. Its template
 * {@code dromen/eiland_plein} and {@code dromen/eiland/plein.json} come from tools/features/droomplein.py (the approved render
 * r01_droomplein turned into real blocks); what lives on it:
 * <ul>
 *   <li>the Droomposthuisje's brievenbusjes ({@link DroomBrievenbusBlock}): float to an island you visited before
 *       ({@link DroomBezocht} keeps track);</li>
 *   <li>the Droomfontein ({@link DroomFontein}): throw a knabbel in and hear a dream of a guh somebody on the server owns;</li>
 *   <li>the Wekkerguh in its own look (part C's NPC, hooked into {@link Wekkerguh#MAKER});</li>
 *   <li>the reuzenwekker by its stand ({@link #DROOMWEKKER}, decoration).</li>
 * </ul>
 * Snoes himself is placed by the story agent (Snoes' corner: the doos met knabbels and the kattenhuisje are in the template).
 */
public final class DroompleinFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** One brievenbusje per island, and the plein's own (in this order; tools/features/droomplein.py: BUSSEN). */
    public static final String[] BUSSEN = {DroomEilanden.STERREN, DroomEilanden.MOLENTJE, DroomEilanden.BADJE, DroomEilanden.HERINNERING,
            DroomEilanden.LIEDJES, DroomEilanden.PLEIN};
    public static final Map<String, DeferredBlock<DroomBrievenbusBlock>> BRIEVENBUSSEN = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<BlockItem>> BRIEVENBUS_ITEMS = new LinkedHashMap<>();

    static {
        for (String eiland : BUSSEN) {
            var b = BLOCKS.registerBlock("droombrievenbus_" + eiland, p -> new DroomBrievenbusBlock(p, eiland),
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1f).sound(SoundType.WOOD).noOcclusion());
            BRIEVENBUSSEN.put(eiland, b);
            BRIEVENBUS_ITEMS.put(eiland, ITEMS.registerSimpleBlockItem(b));
        }
    }

    /** The reuzenwekker's little alarm clocks on the Wekkerguh's stand (decoration, facing like the doos met knabbels). */
    public static final DeferredBlock<DroomBrievenbusBlock.Deco> DROOMWEKKER = BLOCKS.registerBlock("droomwekker", DroomBrievenbusBlock.Deco::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.RED_TERRACOTTA).strength(0.6f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredItem<BlockItem> DROOMWEKKER_ITEM = ITEMS.registerSimpleBlockItem(DROOMWEKKER);

    private DroompleinFeature() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.addListener(DroomBezocht::tick);
        NeoForge.EVENT_BUS.addListener(DroomFontein::tick);
        // part C's look for the Wekkerguh (its tag, spot, click and keeping stay in feature/dromen)
        Wekkerguh.MAKER = level -> {
            GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.STRUCTURE);
            if (npc != null) {
                npc.setKind(GuhNpcEntity.Kind.WEKKERGUH);
            }
            return npc;
        };
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<BlockItem> i : BRIEVENBUS_ITEMS.values()) {
            output.accept(new ItemStack(i.get()));
        }
        output.accept(new ItemStack(DROOMWEKKER_ITEM.get()));
    }
}
