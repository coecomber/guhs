package nl.juiced.guhs.feature.elftocht;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.knus.GuhHooks;

/**
 * De Elf-Guhjestocht (2.9, the showpiece of De Grote Guhspelen): once in every Guhpolder, on the peak of its noise, a
 * frozen canal loop of ~1300 blocks through eleven villages (structure {@code elfguhjestocht}, {@link ElftochtStructure},
 * placed by {@link ElftochtPlacement}; tools/features/elftocht.py + elftocht_*.py). Start and finish in Guhwarden, at
 * the Bonkevads.
 * <ul>
 *   <li><b>Schaatsmeester Guhglij</b> (SCHAATSMEESTERGUH, {@link SchaatsmeesterRole}) lends guh-schaatsen and a stempelkaart,
 *       fires the start (the tour: {@link ElftochtTocht}), rents skates for free skating, and runs a shop for elfstempels.</li>
 *   <li>Eleven <b>Stempelguhs</b> (STEMPELGUH, {@link StempelRole}; village index from their tag guhs_elftocht_dorp_&lt;n&gt;):
 *       stamp your card in the fixed order 2..11 and finally 1 (Guhwarden = the finish). Split times in the actionbar
 *       (green: faster than your best tour, red: slower); the total time goes on the board {@code elfguhjestocht}.</li>
 *   <li>The skates ({@link SchaatsenItem}, {@link ElftochtSchaatsen}): fast gliding on ice, a skater's sway, the scrape
 *       of the blades; off the ice you just walk.</li>
 *   <li>Atmosphere: cheering audience guhs ({@link ElftochtPubliek}), warm chocovet and snert at the stalls
 *       ({@link KopjesBlock}: a short warm speed boost), lampions and vuurkorven that light up at night
 *       ({@link NachtlichtBlock}), a tamed Pinguh sliding along with you.</li>
 * </ul>
 */
public final class ElftochtFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Guhs.MODID);
    public static final DeferredRegister<StructurePlacementType<?>> PLACEMENT_TYPES = DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, Guhs.MODID);

    public static final ResourceKey<Structure> STRUCTURE = ResourceKey.create(Registries.STRUCTURE, Guhs.id("elfguhjestocht"));
    public static final DeferredHolder<StructureType<?>, StructureType<ElftochtStructure>> STRUCTURE_TYPE =
            STRUCTURE_TYPES.register("elfguhjestocht", () -> () -> ElftochtStructure.CODEC);
    public static final DeferredHolder<StructurePlacementType<?>, StructurePlacementType<ElftochtPlacement>> PIEK_PLACEMENT =
            PLACEMENT_TYPES.register("elftocht_piek", () -> () -> ElftochtPlacement.CODEC);

    /** The blocks you can skate on (ice, packed ice, blue ice, the Guhpolder's polderijs). */
    public static final TagKey<Block> SCHAATSIJS = TagKey.create(Registries.BLOCK, Guhs.id("elftocht/schaatsijs"));

    // --- blocks --------------------------------------------------------------------------------------------------------
    /** The Elf-Guhjeskruisje: the medal of the tour (the first time you finish), a deco block. */
    public static final DeferredBlock<KruisjeBlock> KRUISJE = BLOCKS.registerBlock("elf_guhjeskruisje", KruisjeBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(0.8f).sound(SoundType.METAL).noOcclusion().lightLevel(s -> 4));
    /** A paper lampion that lights up at night (along the canal, in the villages). */
    public static final DeferredBlock<NachtlichtBlock.Lampion> LAMPION = BLOCKS.registerBlock("elftocht_lampion", NachtlichtBlock.Lampion::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN).sound(SoundType.WOOL).mapColor(MapColor.COLOR_ORANGE)
                    .lightLevel(s -> s.getValue(NachtlichtBlock.LIT) ? 15 : 0).randomTicks());
    /** A vuurkorf (fire basket) that burns at night: warm and cosy, it never hurts anyone. */
    public static final DeferredBlock<NachtlichtBlock.Vuurkorf> VUURKORF = BLOCKS.registerBlock("elftocht_vuurkorf", NachtlichtBlock.Vuurkorf::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5f, 6f).sound(SoundType.CHAIN).noOcclusion()
                    .lightLevel(s -> s.getValue(NachtlichtBlock.LIT) ? 15 : 0).randomTicks());
    /** A tray of steaming cups at a koek-en-zopie stall: grab a warme chocovet or a kommetje snert. */
    public static final DeferredBlock<KopjesBlock> KOPJES = BLOCKS.registerBlock("elftocht_kopjes", KopjesBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(0.6f).sound(SoundType.WOOD).noOcclusion());

    // --- items ---------------------------------------------------------------------------------------------------------
    /** The coin of the tour. */
    public static final DeferredItem<Item> ELFSTEMPEL = ITEMS.registerSimpleItem("elfstempel", new Item.Properties());
    /** The loaned skates (tag guhs:loaned). */
    public static final DeferredItem<SchaatsenItem> SCHAATSEN = ITEMS.registerItem("guh_schaatsen", SchaatsenItem::new,
            new Item.Properties().stacksTo(1));
    /** The loaned stamp card of your tour (tag guhs:loaned). */
    public static final DeferredItem<StempelkaartItem> STEMPELKAART = ITEMS.registerItem("stempelkaart", StempelkaartItem::new,
            new Item.Properties().stacksTo(1));
    public static final DeferredItem<WarmDrankjeItem> WARME_CHOCOVET = ITEMS.registerItem("warme_chocovet", WarmDrankjeItem::new,
            new Item.Properties().stacksTo(16).food(drankje(3)));
    public static final DeferredItem<WarmDrankjeItem> SNERT_KOMMETJE = ITEMS.registerItem("snert_kommetje", WarmDrankjeItem::new,
            new Item.Properties().stacksTo(16).food(drankje(5)));
    public static final DeferredItem<BlockItem> KRUISJE_ITEM = ITEMS.registerItem("elf_guhjeskruisje", p -> new BlockItem(KRUISJE.get(), p),
            new Item.Properties().rarity(Rarity.EPIC).stacksTo(16));
    public static final DeferredItem<BlockItem> LAMPION_ITEM = ITEMS.registerSimpleBlockItem(LAMPION);
    public static final DeferredItem<BlockItem> VUURKORF_ITEM = ITEMS.registerSimpleBlockItem(VUURKORF);
    public static final DeferredItem<BlockItem> KOPJES_ITEM = ITEMS.registerSimpleBlockItem(KOPJES);

    /** A warm drink: a little food, and while it warms you (the boost) you skate and walk faster. */
    static FoodProperties drankje(int nutrition) {
        return new FoodProperties.Builder().nutrition(nutrition).saturationModifier(0.5f).alwaysEdible().fast()
                .effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, WarmDrankjeItem.BOOST_TICKS, 1), 1f).build();
    }

    // --- sounds --------------------------------------------------------------------------------------------------------
    /** The hiss of the blades (a seamless loop; the client follows your speed with volume and pitch). */
    public static final DeferredHolder<SoundEvent, SoundEvent> GLIJ = sound("elftocht.glij");
    /** A skate stride: the blade bites into the ice. */
    public static final DeferredHolder<SoundEvent, SoundEvent> KRAS = sound("elftocht.kras");
    /** PLOF: the Stempelguh's stamp. */
    public static final DeferredHolder<SoundEvent, SoundEvent> PLOF = sound("elftocht.plof");
    /** Schaatsmeester Guhglij's whistle (the start). */
    public static final DeferredHolder<SoundEvent, SoundEvent> FLUIT = sound("elftocht.fluit");
    /** The audience cheering "VAHOEG!". */
    public static final DeferredHolder<SoundEvent, SoundEvent> JUICH = sound("elftocht.juich");
    /** The finish: a little fanfare. */
    public static final DeferredHolder<SoundEvent, SoundEvent> FINISH = sound("elftocht.finish");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    private static final NpcRole SCHAATSMEESTER = new SchaatsmeesterRole();
    private static final NpcRole STEMPELGUH = new StempelRole();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        PLACEMENT_TYPES.register(modBus);
        Minigames.registerGame(Minigames.ELFTOCHT, ElftochtTocht::isBezig);
        NeoForge.EVENT_BUS.register(ElftochtEvents.class);
        NeoForge.EVENT_BUS.register(ElftochtProtection.class);
        Protected.add(ElftochtProtection::protectedAt);
        GuhHooks.tick(ElftochtPubliek::tick);
        // the tour's clothes: only from Schaatsmeester Guhglij's shop
        KledingBronnen.bron(GuhClothes.ELFTOCHT_SCHAATSMUTS, "elftocht", SchaatsmeesterRole.PRIJS_MUTS + " elfstempels");
        KledingBronnen.bron(GuhClothes.ELFTOCHT_TRUITJE, "elftocht", SchaatsmeesterRole.PRIJS_TRUITJE + " elfstempels");
        KledingBronnen.bron(GuhClothes.ELFTOCHT_SJAAL, "elftocht", SchaatsmeesterRole.PRIJS_SJAAL + " elfstempels");
        KledingBronnen.bron(GuhClothes.ELFTOCHT_OORWARMERS, "elftocht", SchaatsmeesterRole.PRIJS_OORWARMERS + " elfstempels");
    }

    public static void payloads(PayloadRegistrar registrar) {
        ElftochtPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(ELFSTEMPEL.get()));
        output.accept(new ItemStack(WARME_CHOCOVET.get()));
        output.accept(new ItemStack(SNERT_KOMMETJE.get()));
        output.accept(new ItemStack(KRUISJE_ITEM.get()));
        output.accept(new ItemStack(LAMPION_ITEM.get()));
        output.accept(new ItemStack(VUURKORF_ITEM.get()));
        output.accept(new ItemStack(KOPJES_ITEM.get()));
    }

    /** Schaatsmeester Guhglij. */
    @Nullable
    public static NpcRole schaatsmeester() {
        return SCHAATSMEESTER;
    }

    /** The Stempelguhs of the eleven villages. */
    @Nullable
    public static NpcRole stempelguh() {
        return STEMPELGUH;
    }

    private ElftochtFeature() {
    }
}
