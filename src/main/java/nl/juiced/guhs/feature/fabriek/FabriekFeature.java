package nl.juiced.guhs.feature.fabriek;

import java.util.function.Consumer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bio.dieren.DierenSlice;
import nl.juiced.guhs.feature.bio.dieren.WolkenschaapjeEntity;
import nl.juiced.guhs.feature.huisje.Klusjes;
import nl.juiced.guhs.feature.klusjes.BloemetjesKlus;
import nl.juiced.guhs.feature.klusjes.ScherenKlus;
import nl.juiced.guhs.feature.piep.PiepDierItem;
import nl.juiced.guhs.feature.techmachine.TechBlockEntity;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;
import nl.juiced.guhs.item.GuhClothingItem;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * 1.8.0 (vadskracht): de Fabriekshal, wool and building blocks on vadskracht.
 * <ul>
 *   <li>{@link #PLUIS}: the raw stuff of wool (the Knuffelrecycler makes it; the Wolkenschaapje's wolkenpluis spins too:
 *       the item tag {@code guhs:spinbaar}).</li>
 *   <li>{@link #SPINMACHINE}: pluis into wool; {@link #VERFBAD}: wool (and every block the game dyes) + dye into colour.</li>
 *   <li>{@link #SCHEERSTAL}: shears the kept Wolkenschaapjes around it; {@link #KNUFFELRECYCLER}: old knuffels, clothes,
 *       wool and cushions into pluis.</li>
 *   <li>{@link #BOUWBLOKKEN_FABRIEK}: the smart stonecutter that also paints, linked to a bank or a chest.</li>
 *   <li>{@link #KLEURMOLEN} (tier 2): flowers into dye with a bonus; {@link #REGENBOOGPERS} (tier 3): pluis + kaassaus into
 *       any dye.</li>
 *   <li>The Wolkenschaapje is a maatje now (tame, pick up, breed: {@link #WOLKENSCHAAPJE_ITEM}); chores Scheren and
 *       Bloemetjes plukken.</li>
 * </ul>
 * Resources: tools/features/fabriek.py.
 */
public final class FabriekFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    /** What the Spinmachine spins (pluis, wolkenpluis). */
    public static final TagKey<Item> SPINBAAR = TagKey.create(Registries.ITEM, Guhs.id("spinbaar"));
    /** What the Knuffelrecycler shreds besides wool and carpets (the knuffels, cushions: tools/features/fabriek.py). */
    public static final TagKey<Item> RECYCLEBAAR = TagKey.create(Registries.ITEM, Guhs.id("recyclebaar"));

    private static BlockBehaviour.Properties machine(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(2.5f, 6f).sound(SoundType.METAL).requiresCorrectToolForDrops()
                .pushReaction(PushReaction.BLOCK).noOcclusion();
    }

    private static DeferredBlock<FabriekBlock> blok(String id, FabriekBlock.Soort soort, MapColor kleur) {
        return BLOCKS.registerBlock(id, p -> new FabriekBlock(p, soort), () -> machine(kleur));
    }

    // --- the blocks ---
    public static final DeferredBlock<FabriekBlock> SPINMACHINE = blok("spinmachine", FabriekBlock.Soort.SPINMACHINE, MapColor.WOOL);
    public static final DeferredBlock<FabriekBlock> VERFBAD = blok("verfbad", FabriekBlock.Soort.VERFBAD, MapColor.COLOR_LIGHT_BLUE);
    public static final DeferredBlock<FabriekBlock> KNUFFELRECYCLER = blok("knuffelrecycler", FabriekBlock.Soort.KNUFFELRECYCLER, MapColor.COLOR_PINK);
    public static final DeferredBlock<FabriekBlock> SCHEERSTAL = blok("scheerstal", FabriekBlock.Soort.SCHEERSTAL, MapColor.WOOD);
    public static final DeferredBlock<FabriekBlock> BOUWBLOKKEN_FABRIEK = blok("bouwblokken_fabriek", FabriekBlock.Soort.BOUWBLOKKEN_FABRIEK,
            MapColor.COLOR_ORANGE);
    public static final DeferredBlock<FabriekBlock> KLEURMOLEN = blok("kleurmolen", FabriekBlock.Soort.KLEURMOLEN, MapColor.COLOR_YELLOW);
    public static final DeferredBlock<FabriekBlock> REGENBOOGPERS = blok("regenboogpers", FabriekBlock.Soort.REGENBOOGPERS, MapColor.COLOR_PURPLE);

    public static final DeferredItem<BlockItem> SPINMACHINE_ITEM = ITEMS.registerSimpleBlockItem(SPINMACHINE);
    public static final DeferredItem<BlockItem> VERFBAD_ITEM = ITEMS.registerSimpleBlockItem(VERFBAD);
    public static final DeferredItem<BlockItem> KNUFFELRECYCLER_ITEM = ITEMS.registerSimpleBlockItem(KNUFFELRECYCLER);
    public static final DeferredItem<BlockItem> SCHEERSTAL_ITEM = ITEMS.registerSimpleBlockItem(SCHEERSTAL);
    public static final DeferredItem<BlockItem> BOUWBLOKKEN_FABRIEK_ITEM = ITEMS.registerSimpleBlockItem(BOUWBLOKKEN_FABRIEK);
    public static final DeferredItem<BlockItem> KLEURMOLEN_ITEM = ITEMS.registerSimpleBlockItem(KLEURMOLEN);
    public static final DeferredItem<BlockItem> REGENBOOGPERS_ITEM = ITEMS.registerSimpleBlockItem(REGENBOOGPERS);

    // --- the items ---
    public static final DeferredItem<Item> PLUIS = ITEMS.registerSimpleItem("pluis");
    /** A picked-up Wolkenschaapje (the maatje in the item). */
    public static final DeferredItem<PiepDierItem> WOLKENSCHAAPJE_ITEM = ITEMS.registerItem("wolkenschaapje_item",
            p -> new PiepDierItem(() -> DierenSlice.WOLKENSCHAAPJE.get(), p), () -> new Item.Properties().stacksTo(1));

    // --- block entities ---
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpinmachineBlockEntity>> SPINMACHINE_BE = BLOCK_ENTITIES.register(
            "spinmachine", () -> new BlockEntityType<>(SpinmachineBlockEntity::new, SPINMACHINE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VerfbadBlockEntity>> VERFBAD_BE = BLOCK_ENTITIES.register(
            "verfbad", () -> new BlockEntityType<>(VerfbadBlockEntity::new, VERFBAD.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KnuffelrecyclerBlockEntity>> KNUFFELRECYCLER_BE = BLOCK_ENTITIES.register(
            "knuffelrecycler", () -> new BlockEntityType<>(KnuffelrecyclerBlockEntity::new, KNUFFELRECYCLER.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ScheerstalBlockEntity>> SCHEERSTAL_BE = BLOCK_ENTITIES.register(
            "scheerstal", () -> new BlockEntityType<>(ScheerstalBlockEntity::new, SCHEERSTAL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BouwblokkenFabriekBlockEntity>> BOUWBLOKKEN_FABRIEK_BE = BLOCK_ENTITIES.register(
            "bouwblokken_fabriek", () -> new BlockEntityType<>(BouwblokkenFabriekBlockEntity::new, BOUWBLOKKEN_FABRIEK.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KleurmolenBlockEntity>> KLEURMOLEN_BE = BLOCK_ENTITIES.register(
            "kleurmolen", () -> new BlockEntityType<>(KleurmolenBlockEntity::new, KLEURMOLEN.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RegenboogpersBlockEntity>> REGENBOOGPERS_BE = BLOCK_ENTITIES.register(
            "regenboogpers", () -> new BlockEntityType<>(RegenboogpersBlockEntity::new, REGENBOOGPERS.get()));

    private FabriekFeature() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(FabriekFeature::capabilities);
        WolkenschaapjeEntity.OPPAK_ITEM = WOLKENSCHAAPJE_ITEM;
        Klusjes.registreer(new ScherenKlus());
        Klusjes.registreer(new BloemetjesKlus());
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock event) -> Koppelen.klik(event));
        NeoForge.EVENT_BUS.addListener((AddServerReloadListenersEvent event) -> {
            Bouwwegen.vergeet();
            Verven.vergeet();
            Kleuren.vergeet();
        });
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        VadskrachtFeature.machineCapabilities(event, SPINMACHINE_BE.get());
        VadskrachtFeature.machineCapabilities(event, VERFBAD_BE.get());
        VadskrachtFeature.machineCapabilities(event, KNUFFELRECYCLER_BE.get());
        VadskrachtFeature.machineCapabilities(event, SCHEERSTAL_BE.get());
        VadskrachtFeature.machineCapabilities(event, BOUWBLOKKEN_FABRIEK_BE.get());
        VadskrachtFeature.machineCapabilities(event, KLEURMOLEN_BE.get());
        VadskrachtFeature.machineCapabilities(event, REGENBOOGPERS_BE.get());
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(PLUIS.get()));
        output.accept(new ItemStack(SPINMACHINE_ITEM.get()));
        output.accept(new ItemStack(VERFBAD_ITEM.get()));
        output.accept(new ItemStack(SCHEERSTAL_ITEM.get()));
        output.accept(new ItemStack(KNUFFELRECYCLER_ITEM.get()));
        output.accept(new ItemStack(BOUWBLOKKEN_FABRIEK_ITEM.get()));
        output.accept(new ItemStack(KLEURMOLEN_ITEM.get()));
        output.accept(new ItemStack(REGENBOOGPERS_ITEM.get()));
    }

    // --- little helpers ------------------------------------------------------------------------------------------------

    /** Does the Spinmachine (and the Regenboogpers) take this? */
    public static boolean spinbaar(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(PLUIS.get()) || stack.is(SPINBAAR));
    }

    /** Is this a dye? */
    public static boolean isVerf(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(Tags.Items.DYES) || Kleuren.kleur(stack) != null);
    }

    /** How much pluis the Knuffelrecycler makes of one of these (0: it does not take it). */
    public static int pluisVoor(ItemStack stack) {
        if (stack.isEmpty() || stack.is(PLUIS.get()) || stack.is(SPINBAAR)) {
            return 0;
        }
        if (stack.is(ItemTags.WOOL)) {
            return FabriekGetallen.PLUIS_WOL;
        }
        if (stack.is(ItemTags.WOOL_CARPETS)) {
            return FabriekGetallen.PLUIS_TAPIJT;
        }
        if (stack.getItem() instanceof GuhClothingItem) {
            return FabriekGetallen.PLUIS_KLEDING;
        }
        String pad = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (pad.endsWith("kussen") || pad.endsWith("kussentje")) {
            return FabriekGetallen.PLUIS_KUSSEN;
        }
        if (stack.is(RECYCLEBAAR) || pad.startsWith("knuffel_")) {
            return FabriekGetallen.PLUIS_KNUFFEL;
        }
        return 0;
    }

    /** The machine did its thing: its online owner gets guhs:quest/&lt;name&gt; (for the FTB quests). */
    public static void beloon(TechBlockEntity machine, String name) {
        if (machine.getLevel() != null && machine.getLevel().getServer() != null && machine.eigenaar() != null) {
            ServerPlayer speler = machine.getLevel().getServer().getPlayerList().getPlayer(machine.eigenaar());
            if (speler != null) {
                GuhAdvancements.grant(speler, name);
            }
        }
    }
}
