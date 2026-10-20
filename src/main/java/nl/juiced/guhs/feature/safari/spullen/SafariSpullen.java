package nl.juiced.guhs.feature.safari.spullen;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpyglassItem;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.kleding.KledingBronnen;

/**
 * Op Safari! step 2, slice S6: the safari things (resources: tools/features/safari_blokken.py, the paintings
 * tools/features/safari_schilderijen.py, the sounds {@link SafariGeluid}).
 * <ul>
 *   <li>safari building blocks: rieten dak (+ trap, plaat), bamboewand, bamboe-omheining, safari-acaciaplanken, the
 *       dierentuinbordje;</li>
 *   <li>items: safarivoer (tag {@code guhs:safarivoer}: feeding the home animals), the safarihoed (a cosmetic hat for the
 *       player, worn on the head) and the verrekijker (zooms like a spyglass);</li>
 *   <li>guh clothes (the wardrobe source {@code safari}): safarihoedje, rangerpet, kaki vestje, verrekijker;</li>
 *   <li>the posters and lieve schilderijen: painting variants {@code guhs:safari_<naam>} (data only).</li>
 * </ul>
 */
public final class SafariSpullen {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** The KledingBronnen source of the four safari clothes (sold in the giftshop, S2). */
    public static final String BRON = "safari";
    public static final List<GuhClothes> KLEDING = List.of(GuhClothes.SAFARI_HOEDJE, GuhClothes.SAFARI_RANGERPET, GuhClothes.SAFARI_VESTJE,
            GuhClothes.SAFARI_VERREKIJKER);

    // --- blocks ---------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<Block> RIETEN_DAK = BLOCKS.registerSimpleBlock("rieten_dak",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK).mapColor(MapColor.COLOR_YELLOW).sound(SoundType.GRASS).strength(0.6f));
    public static final DeferredBlock<StairBlock> RIETEN_DAK_TRAP = BLOCKS.registerBlock("rieten_dak_trap",
            p -> new StairBlock(RIETEN_DAK.get().defaultBlockState(), p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK).mapColor(MapColor.COLOR_YELLOW).sound(SoundType.GRASS).strength(0.6f));
    public static final DeferredBlock<SlabBlock> RIETEN_DAK_PLAAT = BLOCKS.registerBlock("rieten_dak_plaat", SlabBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.HAY_BLOCK).mapColor(MapColor.COLOR_YELLOW).sound(SoundType.GRASS).strength(0.6f));
    public static final DeferredBlock<Block> BAMBOEWAND = BLOCKS.registerSimpleBlock("bamboewand",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BAMBOO_PLANKS).mapColor(MapColor.PLANT));
    public static final DeferredBlock<DunBlok> BAMBOE_OMHEINING = BLOCKS.registerBlock("bamboe_omheining", p -> new DunBlok(p, 6, 10),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BAMBOO_PLANKS).mapColor(MapColor.PLANT).noOcclusion());
    public static final DeferredBlock<Block> SAFARI_ACACIAPLANKEN = BLOCKS.registerSimpleBlock("safari_acaciaplanken",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.ACACIA_PLANKS));
    public static final DeferredBlock<DunBlok> DIERENTUINBORDJE = BLOCKS.registerBlock("dierentuinbordje", p -> new DunBlok(p, 6, 10),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).strength(1f).noOcclusion());

    static {
        for (DeferredBlock<?> b : List.of(RIETEN_DAK, RIETEN_DAK_TRAP, RIETEN_DAK_PLAAT, BAMBOEWAND, BAMBOE_OMHEINING, SAFARI_ACACIAPLANKEN,
                DIERENTUINBORDJE)) {
            ITEMS.registerSimpleBlockItem(b);
        }
    }

    // --- items ------------------------------------------------------------------------------------------------------------
    /** Feeding the home animals of a verblijfsbord (S5): counts double, compared with their normal food. */
    public static final DeferredItem<Item> SAFARIVOER = ITEMS.registerSimpleItem("safarivoer", () -> new Item.Properties());
    /** The player's cosmetic safari hat: worn on the head (rendered as its item model, like a carved pumpkin). */
    public static final DeferredItem<Item> SAFARIHOED = ITEMS.registerSimpleItem("safarihoed", () -> new Item.Properties().stacksTo(1)
            .component(net.minecraft.core.component.DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD)
                    .setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).setDamageOnHurt(false).build()));
    /** The verrekijker: zooms in like a spyglass (NeoForge: the SPYGLASS_SCOPE ability of SpyglassItem). */
    public static final DeferredItem<SpyglassItem> VERREKIJKER = ITEMS.registerItem("verrekijker", SpyglassItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SafariGeluid.SOUNDS.register(modBus);
        for (GuhClothes c : KLEDING) {
            KledingBronnen.bron(c, BRON);
        }
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredBlock<?> b : List.of(RIETEN_DAK, RIETEN_DAK_TRAP, RIETEN_DAK_PLAAT, BAMBOEWAND, BAMBOE_OMHEINING, SAFARI_ACACIAPLANKEN,
                DIERENTUINBORDJE)) {
            output.accept(new ItemStack(b.get()));
        }
        output.accept(new ItemStack(SAFARIVOER.get()));
        output.accept(new ItemStack(SAFARIHOED.get()));
        output.accept(new ItemStack(VERREKIJKER.get()));
    }

    private SafariSpullen() {
    }
}
