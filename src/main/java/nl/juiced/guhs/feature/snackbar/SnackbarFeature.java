package nl.juiced.guhs.feature.snackbar;

import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.item.GuhCompassItem;
import nl.juiced.guhs.item.SuperkompasItem;

/**
 * 1.8.1: De Guhsnackbar, a rare building in the Guhmension shaped like a giant cone of fries (structure {@code guhs:snackbar}).
 * The {@link SnackbarGuh} behind the counter sells snacks and teaches the recipes of {@code feature.frituur.SnackRecepten}.
 * The Snackbarkompas (sold by the vads temmer) points to it, and so does the Superkompas. The building can't be broken
 * ({@link SnackbarBescherming}). Resources: tools/features/snackbar.py, the building: tools/features/snackbar_bouw.py.
 * New chunks only: the server world is never reset.
 */
public final class SnackbarFeature {
    public static final ResourceKey<Structure> STRUCTUUR = ResourceKey.create(Registries.STRUCTURE, Guhs.id("snackbar"));
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    /** One tile of a wide sign (the menu, the name on the cone); the template lays them out. */
    public static final DeferredBlock<SnackbarBordBlock> BORD = BLOCKS.registerBlock("snackbar_bord", SnackbarBordBlock::new,
            () -> BlockBehaviour.Properties.of().strength(1.0f).sound(SoundType.WOOD).noOcclusion().noCollision().lightLevel(s -> 9));
    public static final DeferredBlock<SnackbarParasolBlock> PARASOL = BLOCKS.registerBlock("snackbar_parasol", SnackbarParasolBlock::new,
            () -> BlockBehaviour.Properties.of().strength(0.8f).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredItem<BlockItem> PARASOL_ITEM = ITEMS.registerSimpleBlockItem(PARASOL);
    public static final DeferredItem<GuhCompassItem> KOMPAS = ITEMS.registerItem("snackbarkompas",
            p -> new GuhCompassItem(STRUCTUUR, p), () -> new Item.Properties().stacksTo(1));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        NpcRollen.zet(GuhNpcEntity.Kind.SNACKBARGUH, SnackbarGuh.ROL);
        NeoForge.EVENT_BUS.register(SnackbarBescherming.class);
        Protected.add(SnackbarBescherming::protectedAt);
        SuperkompasItem.voegToe("knus", "snackbar");
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KOMPAS.get()));
        output.accept(new ItemStack(PARASOL_ITEM.get()));
    }

    private SnackbarFeature() {
    }
}
