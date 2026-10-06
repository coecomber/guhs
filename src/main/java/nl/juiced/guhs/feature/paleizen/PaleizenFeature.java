package nl.juiced.guhs.feature.paleizen;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.boerderij.BoerderijDier;
import nl.juiced.guhs.feature.boerderij.BoerderijItems;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModItems;

/**
 * bbq2 (paleizen): three new palaces of the Mika's in the Guhbarbecuether, each with a character and a questline that
 * every player does for themselves (all progress in a {@code Verhaallijn}; what a player changes in the world comes back).
 * Resources: tools/features/paleizen.py (+ paleizen_bouw, paleizen_modellen, paleizen_tekst).
 * <ul>
 *   <li><b>De Mika-woonblokken</b> ({@link OmaQuest}): Mika-oma on the top gallery of the flats. Bring her worstsoep to
 *       three grumpy neighbours ({@link MopperMikaEntity}), find her knitting on the roof garden ({@link BreiwerkBlock}).
 *       Reward: the knitted Mika hat ({@code GuhClothes.PALEIZEN_MIKAMUTS}) and a discount with the Nether-Mika's (every
 *       third barter gives the ingot back).</li>
 *   <li><b>De Mika-stal</b> ({@link StalQuest}): the Stalknecht-guh and the {@link WorstzwijntjeEntity Worstzwijntjes}, a
 *       sweet hoglin parody. Pet three of them calm, fill the voerbak, catch the runaway. Reward: two Worstzwijntjes in a
 *       basket ({@link MandjeItem}): a farm animal like the ones of the Guhboerderij that sniffs up a little something every
 *       day it is content. Guhdex page WORSTZWIJNTJE.</li>
 *   <li><b>Het Mika-brugpaleis</b> ({@link TolQuest}): the Tolwachter-Mika lets nobody through the gate who has not paid
 *       toll or guessed three riddles (a gentle shove back, per player), the bridge misses five rows of planks (lay them:
 *       they fall out again after a minute, for the next player) and the tolbel must be rung. Reward: free passage and the
 *       bridge building set ({@link #BRUGPLANK}, {@link #BRUGLEUNING}, the recipe card {@link #RECEPT_BRUG}).</li>
 * </ul>
 * The three buildings are protected ({@link Bescherming}), in the Superkompas tab "Barbecue", and their inhabitants come
 * back when they are gone (feature/wereld/Bezetting). Nothing here ever hurts a player.
 */
public final class PaleizenFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final List<String> STRUCTUREN = List.of(PaleisPlekken.WOONBLOKKEN, PaleisPlekken.STAL, PaleisPlekken.BRUGPALEIS);

    // --- blocks ------------------------------------------------------------------------------------------------------------
    /** Mika-brugplanken: the deck of the toll bridge (the missing rows are these), and a building block of the reward. */
    public static final DeferredBlock<Block> BRUGPLANK = BLOCKS.registerSimpleBlock("paleizen_brugplank",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_PLANKS).mapColor(MapColor.COLOR_BROWN));
    /** The rope rail: a fence. */
    public static final DeferredBlock<FenceBlock> BRUGLEUNING = BLOCKS.registerBlock("paleizen_brugleuning", FenceBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CRIMSON_FENCE).mapColor(MapColor.SAND).sound(SoundType.WOOL));
    /** Mika-oma's knitting basket on the roof garden: every player takes "the" knitting out of it, the basket stays. */
    public static final DeferredBlock<BreiwerkBlock> BREIWERK = BLOCKS.registerBlock("paleizen_breiwerk", BreiwerkBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(-1.0f, 3600000.0f).noLootTable().noOcclusion()
                    .sound(SoundType.WOOL).pushReaction(PushReaction.BLOCK));

    // --- items -------------------------------------------------------------------------------------------------------------
    /** A bowl of Mika-oma's worstsoep for the grumpy neighbours (you can eat it yourself: she has a whole pan). */
    public static final DeferredItem<BoerderijItems.Lore> OMASOEP = ITEMS.registerItem("paleizen_omasoep", BoerderijItems.Lore::new,
            () -> new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.5f).build()));
    /** The knitting, on its way back to Mika-oma. */
    public static final DeferredItem<BoerderijItems.Lore> BREIWERKJE = ITEMS.registerItem("paleizen_breiwerkje", BoerderijItems.Lore::new,
            () -> new Item.Properties().stacksTo(1));
    /** The Stalknecht-guh's sack of feed for the stable's voerbak. */
    public static final DeferredItem<BoerderijItems.Lore> ZWIJNENVOER = ITEMS.registerItem("paleizen_zwijnenvoer", BoerderijItems.Lore::new,
            () -> new Item.Properties().stacksTo(1));
    /** The runaway in your arms. */
    public static final DeferredItem<BoerderijItems.Lore> GEVANGEN_ZWIJNTJE = ITEMS.registerItem("paleizen_gevangen_zwijntje", BoerderijItems.Lore::new,
            () -> new Item.Properties().stacksTo(1));
    /** A plank of the Tolwachter: one lays a whole row of the bridge. */
    public static final DeferredItem<BoerderijItems.Lore> LOSSE_PLANK = ITEMS.registerItem("paleizen_losse_plank", BoerderijItems.Lore::new,
            () -> new Item.Properties().stacksTo(16));
    /** The reward of the stable: a Worstzwijntje in a basket, for at home. */
    public static final DeferredItem<MandjeItem> MANDJE = ITEMS.registerItem("paleizen_worstzwijntje_mandje", MandjeItem::new,
            () -> new Item.Properties().stacksTo(1));
    /** The recipe card of the bridge building set (it stays in the crafting grid). */
    public static final DeferredItem<ReceptkaartItem> RECEPT_BRUG = ITEMS.registerItem("paleizen_recept_brug", ReceptkaartItem::new,
            () -> new Item.Properties().stacksTo(1));

    static {
        ITEMS.registerSimpleBlockItem(BRUGPLANK);
        ITEMS.registerSimpleBlockItem(BRUGLEUNING);
    }

    // --- the creatures -----------------------------------------------------------------------------------------------------
    /** The fixed id of CONTRACT_130 7: the Worstzwijntje. (Fire immune: it lives beside the frying sauce.) */
    public static final DeferredHolder<EntityType<?>, EntityType<WorstzwijntjeEntity>> WORSTZWIJNTJE = ENTITY_TYPES.register("worstzwijntje",
            () -> EntityType.Builder.of(WorstzwijntjeEntity::new, MobCategory.CREATURE).sized(0.8f, 0.85f).eyeHeight(0.6f).fireImmune()
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("worstzwijntje"))));
    /** A grumpy neighbour of the Mika-woonblokken. */
    public static final DeferredHolder<EntityType<?>, EntityType<MopperMikaEntity>> MOPPER_MIKA = ENTITY_TYPES.register("paleizen_mopper_mika",
            () -> EntityType.Builder.of(MopperMikaEntity::new, MobCategory.MISC).sized(0.9f, 0.8f).eyeHeight(0.55f).fireImmune()
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("paleizen_mopper_mika"))));

    public static final DeferredItem<SpawnEggItem> WORSTZWIJNTJE_SPAWN_EGG = ModItems.spawnEgg(ITEMS, "worstzwijntje_spawn_egg", WORSTZWIJNTJE);

    // --- sounds ------------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> KNOR = sound("paleizen.worstzwijntje_knor");
    public static final DeferredHolder<SoundEvent, SoundEvent> GIL = sound("paleizen.worstzwijntje_gil");
    public static final DeferredHolder<SoundEvent, SoundEvent> MOPPER = sound("paleizen.mopper");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLANK = sound("paleizen.plank");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(WORSTZWIJNTJE.get(), WorstzwijntjeEntity.createAttributes().build());
            event.put(MOPPER_MIKA.get(), MopperMikaEntity.createAttributes().build());
        });
        modBus.addListener((RegisterSpawnPlacementsEvent event) -> event.register(WORSTZWIJNTJE.get(), SpawnPlacementTypes.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (type, level, reason, pos, random) -> BoerderijDier.checkDierSpawnRules(level, reason, pos),
                RegisterSpawnPlacementsEvent.Operation.REPLACE));
        GuhDex.creaturePage(GuhVariant.WORSTZWIJNTJE, WORSTZWIJNTJE);
        KledingBronnen.bron(GuhClothes.PALEIZEN_MIKAMUTS, "paleizen");
        for (String structuur : STRUCTUREN) {
            SuperkompasItem.voegToe("barbecue", structuur);
            // (no rim: a player who builds a way up to a palace may build right up to it)
            Bescherming.registreer(structuur, 0);
        }
        OmaQuest.register();
        StalQuest.register();
        TolQuest.register();
        NeoForge.EVENT_BUS.register(PaleizenEvents.class);
        NeoForge.EVENT_BUS.addListener(PaleizenFeature::commando);
    }

    /**
     * /guhs paleizen bouw &lt;mika_woonblokken|mika_stal|mika_brugpaleis&gt; (operators, dev runs only: it builds): puts that
     * building in front of you as a try-out copy that the questlines, the protection and the inhabitants treat as a real one
     * until the server stops ({@link PaleisProef}). For the AutoCheck script tools/autocheck/bbq2_paleizen.txt and for looking
     * at a building without searching the Barbecuether for it. "/guhs paleizen dump &lt;structuur&gt;" saves the copy you stand
     * at, with 8 blocks of the land around it, as a template file in the server folder (to look at how a palace lies in real
     * terrain: scratch render_dump.py).
     */
    private static void commando(RegisterCommandsEvent event) {
        if (FMLEnvironment.isProduction()) {
            return;
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("paleizen").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("bouw").then(Commands.argument("structuur", StringArgumentType.word()).executes(c -> {
                    String naam = StringArgumentType.getString(c, "structuur");
                    if (!STRUCTUREN.contains(naam)) {
                        c.getSource().sendFailure(Component.literal("Kies uit " + STRUCTUREN));
                        return 0;
                    }
                    var start = PaleisProef.bouw(c.getSource().getLevel(), naam, net.minecraft.core.BlockPos.containing(c.getSource().getPosition()), true);
                    c.getSource().sendSuccess(() -> Component.literal("Proefkopie van guhs:" + naam + ": " + start.getBoundingBox()), false);
                    return 1;
                })))
                .then(Commands.literal("dump").then(Commands.argument("structuur", StringArgumentType.word()).executes(c -> {
                    String naam = StringArgumentType.getString(c, "structuur");
                    var level = c.getSource().getLevel();
                    var start = PaleisPlekken.kopie(level, naam, net.minecraft.core.BlockPos.containing(c.getSource().getPosition()));
                    if (start == null) {
                        c.getSource().sendFailure(Component.literal("Geen kopie van guhs:" + naam + " hier"));
                        return 0;
                    }
                    var box = start.getBoundingBox().inflatedBy(8);
                    var hoek = new net.minecraft.core.BlockPos(box.minX(), Math.max(level.getMinY(), box.minY()), box.minZ());
                    var maat = new net.minecraft.core.BlockPos(box.getXSpan(), Math.min(level.getMaxY(), box.maxY()) - hoek.getY() + 1, box.getZSpan());
                    var template = new net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate();
                    template.fillFromWorld(level, hoek, maat, true, List.of(Blocks.AIR));
                    var file = level.getServer().getServerDirectory().resolve("paleizen_dump_" + naam + ".nbt");
                    try {
                        net.minecraft.nbt.NbtIo.writeCompressed(template.save(new net.minecraft.nbt.CompoundTag()), file);
                    } catch (java.io.IOException e) {
                        c.getSource().sendFailure(Component.literal("dump mislukt: " + e));
                        return 0;
                    }
                    c.getSource().sendSuccess(() -> Component.literal("Kopie " + start.getBoundingBox() + " met rand 8 vanaf " + hoek.toShortString() + " naar " + file), false);
                    return 1;
                })))
                .then(Commands.literal("weg").executes(c -> {
                    nl.juiced.guhs.feature.wereld.Kopieen.testWissen(c.getSource().getLevel());
                    c.getSource().sendSuccess(() -> Component.literal("Proefkopieen vergeten (de blokken blijven staan)"), false);
                    return 1;
                }))));
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(BRUGPLANK.get()));
        output.accept(new ItemStack(BRUGLEUNING.get()));
        output.accept(new ItemStack(RECEPT_BRUG.get()));
        output.accept(new ItemStack(MANDJE.get()));
        output.accept(new ItemStack(WORSTZWIJNTJE_SPAWN_EGG.get()));
    }

    private PaleizenFeature() {
    }
}
