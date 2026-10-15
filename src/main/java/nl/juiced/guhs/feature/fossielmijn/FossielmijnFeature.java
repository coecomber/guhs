package nl.juiced.guhs.feature.fossielmijn;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Unit;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.kaasmijn.LoreItem;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.item.SuperkompasItem;

/**
 * bbq2 (fossiel-mijn): the Fossiel-opgraving, the Zoutkristalmijn and zoutkristal, the resource of the "Zout" tier of
 * Guh-technologie.
 * <ul>
 *   <li><b>Zoutkristal</b> ({@link #ZOUTKRISTAL}, the fixed id of CONTRACT_130 7) comes from the vein of the mine (every
 *       player's own stock that grows back: {@link Zoutmijn}), from {@link #ZOUTKRISTALERTS} in chunks generated from now
 *       on, and now and then from the bottenzand.</li>
 *   <li><b>Fossiel-opgraving</b> (structure fossiel_opgraving, the Archeoloog-guh, questline {@link #ARCHEOLOOG}): brush five
 *       bones out of the bottenzand, put the little Tyrannoguhrus Njex together on the stand ({@link Opgraving}).</li>
 *   <li><b>Zoutkristalmijn</b> (structure zoutkristalmijn, the Mijnwerker-guh, questline {@link #MIJNWERKER}): clear the
 *       cart track, find the vein ({@link Zoutmijn}).</li>
 * </ul>
 * Both buildings are protected ({@link Bescherming}) and never change for good: all progress is per player, so any number
 * of players do both questlines, side by side or years apart. The NPCs also come to copies that lost theirs
 * ({@link Bezetting}). Resources: tools/features/fossiel_mijn.py (+ _bouw, _tex, _wiki).
 */
public final class FossielmijnFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    public static final String OPGRAVING = "fossiel_opgraving", MIJN = "zoutkristalmijn";
    /** Where the NPCs sit in their templates (tools/features/fossiel_mijn_bouw.py ARCHEOLOOG / MIJNWERKER; a game test compares). */
    public static final BlockPos ARCHEOLOOG_PLEK = new BlockPos(27, 4, 28), MIJNWERKER_PLEK = new BlockPos(27, 6, 36);
    public static final float ARCHEOLOOG_YAW = 110f, MIJNWERKER_YAW = 90f;
    public static final String ARCHEOLOOG_ID = "fossielmijn_archeoloog", MIJNWERKER_ID = "fossielmijn_mijnwerker";

    // --- zoutkristal ------------------------------------------------------------------------------------------------------
    public static final DeferredItem<Item> ZOUTKRISTAL = ITEMS.registerItem("zoutkristal", p -> new LoreItem(p), () -> new Item.Properties());
    /** Houtskoolsteen with salt crystals in it (the nether quartz ore of the Barbecuether): new chunks only. */
    public static final DeferredBlock<DropExperienceBlock> ZOUTKRISTALERTS = BLOCKS.registerBlock("fossielmijn_zoutkristalerts",
            p -> new DropExperienceBlock(UniformInt.of(1, 3), p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_QUARTZ_ORE).mapColor(MapColor.COLOR_BLACK));
    /** The vein of the mine: it never breaks, see {@link Zoutmijn}. */
    public static final DeferredBlock<FossielmijnBlocks.Zoutader> ZOUTADER = BLOCKS.registerBlock("fossielmijn_zoutader", FossielmijnBlocks.Zoutader::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(3.0f, 1200f).requiresCorrectToolForDrops()
                    .sound(SoundType.AMETHYST).lightLevel(s -> 9));
    public static final DeferredBlock<Block> ZOUTKRISTALBLOK = BLOCKS.registerSimpleBlock("fossielmijn_zoutkristalblok",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.QUARTZ_BLOCK).mapColor(MapColor.SNOW).sound(SoundType.AMETHYST).lightLevel(s -> 10));
    public static final DeferredBlock<FossielmijnBlocks.Kristalletjes> ZOUTKRISTALLETJES = BLOCKS.registerBlock("fossielmijn_zoutkristalletjes",
            FossielmijnBlocks.Kristalletjes::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.4f).noCollision().noOcclusion()
                    .sound(SoundType.AMETHYST_CLUSTER).lightLevel(s -> 7).pushReaction(PushReaction.DESTROY));

    // --- the dig ----------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<FossielmijnBlocks.Bottenzand> BOTTENZAND = BLOCKS.registerBlock("fossielmijn_bottenzand",
            FossielmijnBlocks.Bottenzand::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(-1.0f, 3600000f)
                    .sound(SoundType.SUSPICIOUS_SAND).noOcclusion().pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<FossielmijnBlocks.Skeletrek> SKELETREK = BLOCKS.registerBlock("fossielmijn_skeletrek",
            FossielmijnBlocks.Skeletrek::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(-1.0f, 3600000f)
                    .sound(SoundType.BONE_BLOCK).noOcclusion().pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<FossielmijnBlocks.Beeldje> FOSSIELBEELDJE = BLOCKS.registerBlock("fossielmijn_fossielbeeldje",
            FossielmijnBlocks.Beeldje::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(1.5f).sound(SoundType.BONE_BLOCK)
                    .noOcclusion());
    /** The five bones of the little Tyrannoguhrus Njex (bit i of the stand's mask = bone i; they are found tail first: {@link Opgraving#volgorde}). */
    public static final List<String> BOT_NAMEN = List.of("schedel", "ruggengraat", "ribben", "pootjes", "staart");
    public static final List<DeferredItem<Item>> BOTTEN = BOT_NAMEN.stream()
            .map(naam -> ITEMS.<Item>registerItem("fossielmijn_bot_" + naam, p -> new LoreItem(p), () -> new Item.Properties().stacksTo(1))).toList();
    /** The Guhkwastje: a brush that never wears out (it brushes suspicious sand and gravel like any brush). */
    public static final DeferredItem<Kwastje> KWASTJE = ITEMS.registerItem("fossielmijn_kwastje", Kwastje::new,
            () -> new Item.Properties().stacksTo(1).component(DataComponents.UNBREAKABLE, Unit.INSTANCE).rarity(Rarity.UNCOMMON));

    // --- the mine ---------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<FossielmijnBlocks.Puin> PUIN = BLOCKS.registerBlock("fossielmijn_puin", FossielmijnBlocks.Puin::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.2f, 6f).sound(SoundType.BASALT).noOcclusion()
                    .pushReaction(PushReaction.BLOCK));
    /** The Zoutkristalhouweel: an iron-level pickaxe that lasts twice as long and takes two crystals per hit out of the vein. */
    public static final ToolMaterial HOUWEEL_TIER = new ToolMaterial(BlockTags.INCORRECT_FOR_IRON_TOOL, 500, 6.5f, 2.0f, 14, ItemTags.IRON_TOOL_MATERIALS);
    public static final DeferredItem<Item> ZOUTKRISTALHOUWEEL = ITEMS.registerItem("fossielmijn_zoutkristalhouweel", p -> new LoreItem(p),
            () -> new Item.Properties().pickaxe(HOUWEEL_TIER, 1.0f, -2.8f).repairable(ZOUTKRISTAL.get()).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<BlockItem> ZOUTKRISTALERTS_ITEM = ITEMS.registerSimpleBlockItem(ZOUTKRISTALERTS);
    public static final DeferredItem<BlockItem> ZOUTADER_ITEM = ITEMS.registerSimpleBlockItem(ZOUTADER);
    public static final DeferredItem<BlockItem> ZOUTKRISTALBLOK_ITEM = ITEMS.registerSimpleBlockItem(ZOUTKRISTALBLOK);
    public static final DeferredItem<BlockItem> ZOUTKRISTALLETJES_ITEM = ITEMS.registerSimpleBlockItem(ZOUTKRISTALLETJES);
    public static final DeferredItem<BlockItem> BOTTENZAND_ITEM = ITEMS.registerSimpleBlockItem(BOTTENZAND);
    public static final DeferredItem<BlockItem> SKELETREK_ITEM = ITEMS.registerSimpleBlockItem(SKELETREK);
    public static final DeferredItem<BlockItem> PUIN_ITEM = ITEMS.registerSimpleBlockItem(PUIN);
    public static final DeferredItem<LoreItem.Block> FOSSIELBEELDJE_ITEM = ITEMS.registerItem("fossielmijn_fossielbeeldje",
            p -> new LoreItem.Block(FOSSIELBEELDJE.get(), p), () -> new Item.Properties().rarity(Rarity.UNCOMMON));

    /** The block entity of the two per-player drawn blocks (the bottenzand, the stand). */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FossielmijnBlocks.TekenBlockEntity>> TEKENING = BLOCK_ENTITIES.register(
            "fossielmijn_tekening", () -> new BlockEntityType<>(FossielmijnBlocks.TekenBlockEntity::new, BOTTENZAND.get(), SKELETREK.get()));

    // --- the two questlines (per player; the Guhdex tab Verhalen shows them by themselves) ----------------------------------
    public static final Verhaallijn ARCHEOLOOG = Verhaallijn.maak("archeoloog", "barbecue").stappen(4).icoon("guhs:fossielmijn_bot_schedel")
            .nodig(Opgraving::nodig)
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:fossielmijn_kwastje", FossielmijnFeature.ARCHEOLOOG.stap(p) >= 1),
                    Verhaallijn.beloning("guhs:fossielmijn_fossielbeeldje", FossielmijnFeature.ARCHEOLOOG.klaar(p))))
            .doel((p, stap) -> Doel.structuur(BarbecuetherFeature.BARBECUETHER, OPGRAVING, Component.translatable("structure.guhs." + OPGRAVING)))
            .registreer();
    public static final Verhaallijn MIJNWERKER = Verhaallijn.maak("mijnwerker", "barbecue").stappen(4).icoon("guhs:zoutkristal")
            .nodig(Zoutmijn::nodig)
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:fossielmijn_zoutader", "gui.guhs.fossielmijn.beloning.ader", FossielmijnFeature.MIJNWERKER.stap(p) >= 2),
                    Verhaallijn.beloning("guhs:fossielmijn_zoutkristalhouweel", FossielmijnFeature.MIJNWERKER.klaar(p))))
            .doel((p, stap) -> Doel.structuur(BarbecuetherFeature.BARBECUETHER, MIJN, Component.translatable("structure.guhs." + MIJN)))
            .registreer();

    /** The Guhkwastje: any brush works on bottenzand ({@link Opgraving#opKwast}); this one says what it is for. */
    public static class Kwastje extends BrushItem {
        public Kwastje(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                    Consumer<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        NpcRollen.zet(GuhNpcEntity.Kind.ARCHEOLOOGGUH, new Opgraving.Rol());
        NpcRollen.zet(GuhNpcEntity.Kind.MIJNWERKERGUH, new Zoutmijn.Rol());
        // the same NPC stands in the template: worldgen places it, this only brings it back to a copy that lost it
        Bezetting.npc(ARCHEOLOOG_ID, OPGRAVING, null, ARCHEOLOOG_PLEK, GuhNpcEntity.Kind.ARCHEOLOOGGUH, null, ARCHEOLOOG_YAW);
        Bezetting.npc(MIJNWERKER_ID, MIJN, null, MIJNWERKER_PLEK, GuhNpcEntity.Kind.MIJNWERKERGUH, null, MIJNWERKER_YAW);
        // both buildings stay whole. The dig needs no exception at all (brushing changes nothing); in the mine the rubble
        // may be hacked away (it falls back) and the vein may be "broken" (it never really breaks)
        Bescherming.registreer(OPGRAVING, 2);
        Bescherming.registreer(MIJN, 2);
        Bescherming.uitzondering(MIJN, (p, pos) -> {
            var state = p.level().getBlockState(pos);
            return state.is(PUIN.get()) || state.is(ZOUTADER.get());
        });
        SuperkompasItem.voegToe("barbecue", OPGRAVING);
        SuperkompasItem.voegToe("barbecue", MIJN);
        NeoForge.EVENT_BUS.addListener(Opgraving::opKlik);
        NeoForge.EVENT_BUS.addListener(Opgraving::opKwast);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> sync(event.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> sync(event.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> sync(event.getEntity()));
        NeoForge.EVENT_BUS.addListener(FossielmijnFeature::commando);
    }

    private static void sync(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer p) {
            Opgraving.sync(p);
        }
    }

    public static void payloads(PayloadRegistrar registrar) {
        FossielmijnPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(ZOUTKRISTAL.get()));
        for (var item : List.of(ZOUTKRISTALERTS_ITEM, ZOUTKRISTALBLOK_ITEM, ZOUTKRISTALLETJES_ITEM, ZOUTADER_ITEM, PUIN_ITEM, BOTTENZAND_ITEM,
                SKELETREK_ITEM, FOSSIELBEELDJE_ITEM)) {
            output.accept(new ItemStack(item.get()));
        }
        output.accept(new ItemStack(ZOUTKRISTALHOUWEEL.get()));
        output.accept(new ItemStack(KWASTJE.get()));
        for (DeferredItem<Item> bot : BOTTEN) {
            output.accept(new ItemStack(bot.get()));
        }
    }

    /**
     * /guhs fossielmijn (operators; for AutoCheck scripts and dev checks): "stand" says what you did at the dig and in the
     * mine, "rek &lt;mask&gt;" puts bones on your stand (0..31; the questline's step is not touched), "zout &lt;n&gt;" sets
     * your stock in the vein, "wis" forgets the spots you brushed. (The steps themselves: /guhs verhaal stap archeoloog|mijnwerker.)
     * Only in a dev run: "dump &lt;structuur&gt;" saves the copy you stand at, with the land around it, as a template file in
     * the server folder (to look at how a building lies in real terrain).
     */
    private static void commando(RegisterCommandsEvent event) {
        var basis = Commands.literal("fossielmijn").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("stand").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    c.getSource().sendSuccess(() -> Component.translatable("gui.guhs.fossielmijn.commando.stand", p.getDisplayName(), Opgraving.gevonden(p),
                            Integer.bitCount(Opgraving.geplaatst(p)), MIJNWERKER.teller(p, Zoutmijn.PUIN), Zoutmijn.voorraad(p)), false);
                    return 1;
                }))
                .then(Commands.literal("rek").then(Commands.argument("mask", IntegerArgumentType.integer(0, Opgraving.ALLES)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ARCHEOLOOG.teller(p, Opgraving.GEPLAATST, IntegerArgumentType.getInteger(c, "mask"));
                    Opgraving.sync(p);
                    return gezet(c.getSource());
                })))
                .then(Commands.literal("zout").then(Commands.argument("n", IntegerArgumentType.integer(0, Zoutmijn.VOORRAAD_MAX)).executes(c -> {
                    Zoutmijn.zetVoorraad(c.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(c, "n"));
                    return gezet(c.getSource());
                })))
                .then(Commands.literal("wis").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Opgraving.wis(p);
                    Opgraving.sync(p);
                    return gezet(c.getSource());
                }));
        if (!FMLEnvironment.isProduction()) {
            basis = basis.then(Commands.literal("dump").then(Commands.argument("structuur", StringArgumentType.word())
                    .executes(c -> dump(c.getSource(), StringArgumentType.getString(c, "structuur")))));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(basis));
    }

    private static int gezet(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("gui.guhs.fossielmijn.commando.gezet"), false);
        return 1;
    }

    /** (dev) the copy of this structure around the source, 10 blocks of land around it and 6 under it, as a template file. */
    private static int dump(CommandSourceStack source, String structuur) {
        ServerLevel level = source.getLevel();
        StructureStart start = Bezetting.start(level, structuur, BlockPos.containing(source.getPosition()));
        if (start == null) {
            source.sendFailure(Component.literal("no copy of guhs:" + structuur + " here"));
            return 0;
        }
        BoundingBox box = start.getBoundingBox();
        BlockPos hoek = new BlockPos(box.minX() - 10, Math.max(level.getMinY(), box.minY() - 6), box.minZ() - 10);
        BlockPos maat = new BlockPos(box.getXSpan() + 20, Math.min(level.getMaxY(), box.maxY() + 8) - hoek.getY() + 1, box.getZSpan() + 20);
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, hoek, maat, false, List.of());
        var file = level.getServer().getServerDirectory().resolve("fossielmijn_dump_" + structuur + ".nbt");
        try {
            NbtIo.writeCompressed(template.save(new CompoundTag()), file);
        } catch (java.io.IOException e) {
            source.sendFailure(Component.literal("dump failed: " + e));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("dumped " + box + " (ground of the template at y " + (box.minY()) + "+G) to " + file), false);
        return 1;
    }

    private FossielmijnFeature() {
    }
}
