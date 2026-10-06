package nl.juiced.guhs.feature.campingmarkt;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.kaasmijn.LoreItem;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.item.SuperkompasItem;

/**
 * bbq2 (camping-markt): the Grillcamping and the Nether-Mika-ruilmarkt, two buildings of the Guhbarbecuether.
 * <ul>
 *   <li><b>Grillcamping</b> (structure grillcamping; the Kampbaas-guh and the Houthakker-guh; questline {@link #CAMPING}):
 *       a real guh village of tents around a big camp fire. Pitch your own tent, split fire wood, light the fire for the
 *       camp fire party and roast marshmallows golden brown ({@link Kamperen}, {@link Kampvuur}, {@link RoosterstokItem}).
 *       Reward: the recipe card of the Plantagebak ({@link #RECEPT_PLANTAGEBAK}) and the camping outfit.</li>
 *   <li><b>Nether-Mika-ruilmarkt</b> (structure mika_ruilmarkt; the Marktmeester-Mika; questline {@link #RUILMARKT}): learn
 *       haggling and unmask the fake vads with the scales of the Waag ({@link Ruilmarkt}). Reward: the scales as a deco
 *       block and better barter rates with every Nether-Mika; the stall holders ({@link KraamMikaEntity}) barter on the spot.</li>
 * </ul>
 * Both buildings are protected ({@link Bescherming}) and never change for good: all progress is per player, what a player
 * changes (a tent, the fire, a split log) is put back, so any number of players do both questlines, side by side or years
 * apart. The characters, the residents and the stall holders also come to copies that lost theirs ({@link Bezetting}).
 * The tent canvas ({@link #TENTDOEK}: a block, stairs and a slab in five colours) is what the tents and the awnings are made
 * of, and a building block for players. Resources: tools/features/camping_markt.py (+ _bouw, _tex, _modellen, _wiki).
 */
public final class CampingmarktFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final String CAMPING_STRUCTUUR = "grillcamping", MARKT_STRUCTUUR = "mika_ruilmarkt";
    /** Where the characters sit in their templates (tools/features/camping_markt_bouw.py; a game test compares). */
    public static final BlockPos KAMPBAAS_PLEK = new BlockPos(22, 5, 9), HOUTHAKKER_PLEK = new BlockPos(33, 5, 21), MARKTMEESTER_PLEK = new BlockPos(22, 5, 28);
    public static final float KAMPBAAS_YAW = 0f, HOUTHAKKER_YAW = 90f, MARKTMEESTER_YAW = 0f;
    public static final String KAMPBAAS_ID = "campingmarkt_kampbaas", HOUTHAKKER_ID = "campingmarkt_houthakker", MARKTMEESTER_ID = "campingmarkt_marktmeester";
    /** The stall holders of the market: where they stand in the template and how they look (KRAAM_MIKAS of the builder). */
    public static final List<BlockPos> KRAAM_PLEKKEN = List.of(new BlockPos(22, 4, 7), new BlockPos(8, 4, 22), new BlockPos(30, 4, 33));
    public static final float[] KRAAM_YAW = {0f, -90f, 180f};
    public static final String KRAAM_ID = "campingmarkt_kraam_";

    // --- tent canvas: a block, stairs and a slab per colour ---------------------------------------------------------------
    public static final List<String> KLEUREN = List.of("rood", "geel", "groen", "blauw", "creme");

    /** The three shapes of tent canvas in one colour, and their items. */
    public record Doek(DeferredBlock<Block> blok, DeferredBlock<StairBlock> trap, DeferredBlock<SlabBlock> plaat,
                       DeferredItem<BlockItem> blokItem, DeferredItem<BlockItem> trapItem, DeferredItem<BlockItem> plaatItem) {
    }

    public static final Map<String, Doek> TENTDOEK = tentdoek();

    private static Map<String, Doek> tentdoek() {
        Map<String, Doek> out = new LinkedHashMap<>();
        MapColor[] kaart = {MapColor.COLOR_RED, MapColor.COLOR_YELLOW, MapColor.COLOR_GREEN, MapColor.COLOR_LIGHT_BLUE, MapColor.SAND};
        for (int i = 0; i < KLEUREN.size(); i++) {
            String naam = "campingmarkt_tentdoek_" + KLEUREN.get(i);
            MapColor kleur = kaart[i];
            // (canvas never smothers: a tent that is pitched around you just stands around you)
            DeferredBlock<Block> blok = BLOCKS.registerSimpleBlock(naam, () -> doek(kleur));
            DeferredBlock<StairBlock> trap = BLOCKS.registerBlock(naam + "_trap", p -> new StairBlock(blok.get().defaultBlockState(), p), () -> doek(kleur));
            DeferredBlock<SlabBlock> plaat = BLOCKS.registerBlock(naam + "_plaat", SlabBlock::new, () -> doek(kleur));
            out.put(KLEUREN.get(i), new Doek(blok, trap, plaat, ITEMS.registerSimpleBlockItem(blok), ITEMS.registerSimpleBlockItem(trap),
                    ITEMS.registerSimpleBlockItem(plaat)));
        }
        return out;
    }

    private static BlockBehaviour.Properties doek(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(0.6f).sound(SoundType.WOOL).ignitedByLava()
                .isSuffocating((state, level, pos) -> false).isViewBlocking((state, level, pos) -> false);
    }

    // --- the camping ------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<CampingmarktBlocks.Kampeerplek> KAMPEERPLEK = BLOCKS.registerBlock("campingmarkt_kampeerplek",
            CampingmarktBlocks.Kampeerplek::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(-1.0f, 3600000f).sound(SoundType.WOOD)
                    .noOcclusion().pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<CampingmarktBlocks.Haring> HARING = BLOCKS.registerBlock("campingmarkt_haring", CampingmarktBlocks.Haring::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(0.3f).sound(SoundType.METAL).noOcclusion().noCollision()
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<CampingmarktBlocks.Hakblok> HAKBLOK = BLOCKS.registerBlock("campingmarkt_hakblok", CampingmarktBlocks.Hakblok::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredBlock<CampingmarktBlocks.KampvuurBlock> KAMPVUUR = BLOCKS.registerBlock("campingmarkt_kampvuur",
            CampingmarktBlocks.KampvuurBlock::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).strength(2.0f).sound(SoundType.WOOD)
                    .noOcclusion().lightLevel(s -> s.getValue(CampingmarktBlocks.KampvuurBlock.BRANDT) ? 15 : 0));
    /** The Kampbaas-guh's tent bag (step 1) and the Houthakker-guh's bundle of fire wood (step 3): quest items, never stored. */
    public static final DeferredItem<Item> TENTZAK = ITEMS.registerItem("campingmarkt_tentzak", p -> new LoreItem(p), () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> BRANDHOUT = ITEMS.registerItem("campingmarkt_brandhout", p -> new LoreItem(p), () -> new Item.Properties().stacksTo(1));
    public static final DeferredItem<RoosterstokItem> ROOSTERSTOK = ITEMS.registerItem("campingmarkt_roosterstok", RoosterstokItem::new,
            () -> new Item.Properties().stacksTo(1));
    /** The recipe card of the Plantagebak (CONTRACT_130 10a D5): the Plantagebak's recipe needs it and it stays in the grid. */
    public static final DeferredItem<ReceptkaartItem> RECEPT_PLANTAGEBAK = ITEMS.registerItem("campingmarkt_recept_plantagebak", ReceptkaartItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    // --- the market -------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<CampingmarktBlocks.Weegschaal> WEEGSCHAAL = BLOCKS.registerBlock("campingmarkt_weegschaal",
            CampingmarktBlocks.Weegschaal::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(1.5f).sound(SoundType.METAL).noOcclusion());
    public static final DeferredBlock<CampingmarktBlocks.Vadsstapel> VADSSTAPEL = BLOCKS.registerBlock("campingmarkt_vadsstapel",
            CampingmarktBlocks.Vadsstapel::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1.0f, 3600000f)
                    .sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK));
    /** The Marktmeester's stamp (step 2): a quest item, taken back at the end. */
    public static final DeferredItem<Item> KEURSTEMPEL = ITEMS.registerItem("campingmarkt_keurstempel", p -> new LoreItem(p), () -> new Item.Properties().stacksTo(1));
    public static final DeferredHolder<EntityType<?>, EntityType<KraamMikaEntity>> KRAAM_MIKA = ENTITY_TYPES.register("campingmarkt_kraam_mika",
            () -> EntityType.Builder.of(KraamMikaEntity::new, MobCategory.MISC).sized(0.9f, 0.8f).eyeHeight(0.55f).fireImmune()
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("campingmarkt_kraam_mika"))));

    public static final DeferredItem<BlockItem> KAMPEERPLEK_ITEM = ITEMS.registerSimpleBlockItem(KAMPEERPLEK);
    public static final DeferredItem<BlockItem> HARING_ITEM = ITEMS.registerSimpleBlockItem(HARING);
    public static final DeferredItem<BlockItem> HAKBLOK_ITEM = ITEMS.registerSimpleBlockItem(HAKBLOK);
    public static final DeferredItem<LoreItem.Block> KAMPVUUR_ITEM = ITEMS.registerItem("campingmarkt_kampvuur", p -> new LoreItem.Block(KAMPVUUR.get(), p),
            () -> new Item.Properties());
    public static final DeferredItem<LoreItem.Block> WEEGSCHAAL_ITEM = ITEMS.registerItem("campingmarkt_weegschaal", p -> new LoreItem.Block(WEEGSCHAAL.get(), p),
            () -> new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> VADSSTAPEL_ITEM = ITEMS.registerSimpleBlockItem(VADSSTAPEL);

    // --- the two questlines (per player; the Guhdex tab Verhalen shows them by themselves) ----------------------------------
    public static final Verhaallijn CAMPING = Verhaallijn.maak("camping", "barbecue").stappen(5).icoon("guhs:campingmarkt_roosterstok")
            .nodig(Kamperen::nodig)
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:campingmarkt_roosterstok", CampingmarktFeature.CAMPING.stap(p) >= 4),
                    Verhaallijn.beloning("guhs:campingmarkt_recept_plantagebak", CampingmarktFeature.CAMPING.klaar(p)),
                    Verhaallijn.beloning("guhs:campingmarkt_hoedje", "gui.guhs.campingmarkt.beloning.pakje", CampingmarktFeature.CAMPING.klaar(p))))
            .doel((p, stap) -> Doel.structuur(BarbecuetherFeature.BARBECUETHER, CAMPING_STRUCTUUR, Component.translatable("structure.guhs." + CAMPING_STRUCTUUR)))
            .registreer();
    public static final Verhaallijn RUILMARKT = Verhaallijn.maak("ruilmarkt", "barbecue").stappen(4).icoon("guhs:campingmarkt_weegschaal")
            .nodig(Ruilmarkt::nodig)
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:campingmarkt_weegschaal", CampingmarktFeature.RUILMARKT.klaar(p)),
                    Verhaallijn.beloning("guhs:vahoege_vads_ingot", "gui.guhs.campingmarkt.beloning.extraatje", CampingmarktFeature.RUILMARKT.klaar(p))))
            .doel((p, stap) -> Doel.structuur(BarbecuetherFeature.BARBECUETHER, MARKT_STRUCTUUR, Component.translatable("structure.guhs." + MARKT_STRUCTUUR)))
            .registreer();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(KRAAM_MIKA.get(), KraamMikaEntity.createAttributes().build()));
        NpcRollen.zet(GuhNpcEntity.Kind.KAMPBAASGUH, new Kamperen.KampbaasRol());
        NpcRollen.zet(GuhNpcEntity.Kind.HOUTHAKKERGUH, new Kamperen.HouthakkerRol());
        NpcRollen.zet(GuhNpcEntity.Kind.MARKTMEESTER_MIKA, new Ruilmarkt.MarktmeesterRol());
        // the same characters, residents and stall holders stand in the templates: worldgen places them, this only brings
        // them back to a copy that lost one
        Bezetting.npc(KAMPBAAS_ID, CAMPING_STRUCTUUR, null, KAMPBAAS_PLEK, GuhNpcEntity.Kind.KAMPBAASGUH, null, KAMPBAAS_YAW);
        Bezetting.npc(HOUTHAKKER_ID, CAMPING_STRUCTUUR, null, HOUTHAKKER_PLEK, GuhNpcEntity.Kind.HOUTHAKKERGUH, null, HOUTHAKKER_YAW);
        Bezetting.npc(MARKTMEESTER_ID, MARKT_STRUCTUUR, null, MARKTMEESTER_PLEK, GuhNpcEntity.Kind.MARKTMEESTER_MIKA, null, MARKTMEESTER_YAW);
        for (int i = 0; i < Kamperen.KAMPEERDERS.size(); i++) {
            int nr = i;
            Kamperen.Kampeerder k = Kamperen.KAMPEERDERS.get(i);
            Bezetting.wezen(Kamperen.KAMPEERDER_ID + i, CAMPING_STRUCTUUR, null, k.plek(), (level, plek, draai) -> Kamperen.kampeerder(level, plek, draai, nr),
                    k.zit() ? Bezetting.ZOEK_NPC : Kamperen.THUIS_TERUG + 6);
        }
        for (int i = 0; i < KRAAM_PLEKKEN.size(); i++) {
            int nr = i;
            Bezetting.wezen(KRAAM_ID + i, MARKT_STRUCTUUR, null, KRAAM_PLEKKEN.get(i), (level, plek, draai) -> {
                KraamMikaEntity mika = KRAAM_MIKA.get().create(level, net.minecraft.world.entity.EntitySpawnReason.STRUCTURE);
                if (mika != null) {
                    mika.setKraam(nr);
                    mika.setYRot(KRAAM_YAW[nr]);
                    float y = mika.rotate(draai);
                    mika.snapTo(plek.x, plek.y, plek.z, y, 0f);
                    mika.setYBodyRot(y);
                    mika.setYHeadRot(y);
                }
                return mika;
            }, 6);
        }
        Bescherming.registreer(CAMPING_STRUCTUUR, 2);
        Bescherming.registreer(MARKT_STRUCTUUR, 2);
        SuperkompasItem.voegToe("barbecue", CAMPING_STRUCTUUR);
        SuperkompasItem.voegToe("barbecue", MARKT_STRUCTUUR);
        for (GuhClothes c : List.of(GuhClothes.CAMPINGMARKT_HOEDJE, GuhClothes.CAMPINGMARKT_HALSDOEK, GuhClothes.CAMPINGMARKT_RUGZAK)) {
            KledingBronnen.bron(c, "camping_markt");
        }
        GuhHooks.tick(Kamperen::bewonerTick);
        GuhHooks.klik(Kamperen::bewonerKlik);
        NeoForge.EVENT_BUS.register(CampingmarktEvents.class);
        NeoForge.EVENT_BUS.addListener(CampingmarktFeature::commando);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (Doek d : TENTDOEK.values()) {
            output.accept(new ItemStack(d.blokItem().get()));
            output.accept(new ItemStack(d.trapItem().get()));
            output.accept(new ItemStack(d.plaatItem().get()));
        }
        for (var item : List.of(KAMPVUUR_ITEM, HAKBLOK_ITEM, KAMPEERPLEK_ITEM, HARING_ITEM, WEEGSCHAAL_ITEM, VADSSTAPEL_ITEM)) {
            output.accept(new ItemStack(item.get()));
        }
        for (var item : List.of(ROOSTERSTOK, RECEPT_PLANTAGEBAK, TENTZAK, BRANDHOUT, KEURSTEMPEL)) {
            output.accept(new ItemStack(item.get()));
        }
    }

    /**
     * /guhs campingmarkt (operators; for AutoCheck scripts and dev checks): "stand" says what you did at the camping and on
     * the market, "nep &lt;1-5&gt;" picks your fake stack, "vuur" lights the big camp fire near you, "wis" forgets a haggle.
     * (The steps themselves: /guhs verhaal stap camping|ruilmarkt.) Only in a dev run: "bouw camping|markt|tent" places a
     * template around you (to look at a building without the Guhbarbecuether).
     */
    private static void commando(RegisterCommandsEvent event) {
        var basis = Commands.literal("campingmarkt").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("stand").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    c.getSource().sendSuccess(() -> Component.translatable("gui.guhs.campingmarkt.commando.stand", p.getDisplayName(), CAMPING.stap(p),
                            Kamperen.haringen(p), CAMPING.teller(p, Kamperen.HOUT), CAMPING.teller(p, Kamperen.GEROOSTERD), RUILMARKT.stap(p),
                            RUILMARKT.teller(p, Ruilmarkt.NEP), RUILMARKT.teller(p, Ruilmarkt.WEGINGEN)), false);
                    return 1;
                }))
                .then(Commands.literal("nep").then(Commands.argument("nummer", IntegerArgumentType.integer(1, Ruilmarkt.STAPELS)).executes(c -> {
                    RUILMARKT.teller(c.getSource().getPlayerOrException(), Ruilmarkt.NEP, IntegerArgumentType.getInteger(c, "nummer"));
                    return gezet(c.getSource());
                })))
                .then(Commands.literal("vuur").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    for (BlockPos q : BlockPos.betweenClosed(p.blockPosition().offset(-12, -4, -12), p.blockPosition().offset(12, 4, 12))) {
                        if (p.level().getBlockState(q).getBlock() instanceof CampingmarktBlocks.KampvuurBlock) {
                            Kampvuur.steekAan(p.level(), q.immutable(), true);
                            return gezet(c.getSource());
                        }
                    }
                    return 0;
                }))
                .then(Commands.literal("wis").executes(c -> {
                    Ruilmarkt.vergeet(c.getSource().getPlayerOrException().getUUID());
                    return gezet(c.getSource());
                }));
        if (!FMLEnvironment.isProduction()) {
            basis = basis.then(Commands.literal("bouw")
                    .then(Commands.literal("camping").executes(c -> bouw(c.getSource(), CAMPING_STRUCTUUR)))
                    .then(Commands.literal("markt").executes(c -> bouw(c.getSource(), MARKT_STRUCTUUR)))
                    .then(Commands.literal("tent").executes(c -> {
                        ServerPlayer p = c.getSource().getPlayerOrException();
                        BlockPos bord = p.blockPosition().relative(p.getDirection(), 2);
                        p.level().setBlock(bord, KAMPEERPLEK.get().defaultBlockState().setValue(CampingmarktBlocks.Kampeerplek.FACING,
                                p.getDirection().getOpposite()), Block.UPDATE_ALL);
                        return Kamperen.plaatsTent(p.level(), bord) ? 1 : 0;
                    })));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(basis));
    }

    private static int gezet(CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("gui.guhs.campingmarkt.commando.gezet"), false);
        return 1;
    }

    /** (dev) places a building's template with its ground layer at the feet of the source: corner = you - (22, 4, 22). */
    private static int bouw(CommandSourceStack source, String structuur) {
        ServerLevel level = source.getLevel();
        var template = level.getStructureManager().get(Guhs.id(structuur));
        if (template.isEmpty()) {
            source.sendFailure(Component.literal("no template guhs:" + structuur));
            return 0;
        }
        BlockPos hoek = BlockPos.containing(source.getPosition()).offset(-22, -4, -22);
        var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
        template.get().placeInWorld(level, hoek, hoek, settings, level.getRandom(), Block.UPDATE_ALL);
        source.sendSuccess(() -> Component.literal("placed guhs:" + structuur + " at " + hoek.toShortString() + " (facing " + Direction.SOUTH + " = its gate)"), false);
        return 1;
    }

    private CampingmarktFeature() {
    }
}
