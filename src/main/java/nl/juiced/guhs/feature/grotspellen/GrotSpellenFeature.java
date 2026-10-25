package nl.juiced.guhs.feature.grotspellen;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.GuhFurnitureBlock;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.spelen.SpelGroepen;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.registry.ModItems;

/**
 * 1.7.0 "Onder de Wol": three cave minigames (design: ontwerp-guhs170-mijnen.md, rondes 41-43), each in its own hall under
 * a little entrance hut with a Wolkenliftje down:
 * <ul>
 *   <li>{@link Schatgraven} with Kapitein Schepsnuit and Snuffie (coin: schatmuntjes; piratenhoedje + oogklepje);</li>
 *   <li>{@link Glimwormvangen} with Juf Glimmie (glimmertjes; glimworm-antennetjes + vlindernetje);</li>
 *   <li>{@link Lichtjespuzzel} with Meester Knipperlicht (lampjesmunten; the glowing paddenstoelhoedje).</li>
 * </ul>
 * Every coin also works in the grijpmachine (tag guhs:knus/grijptickets). Each host has a shop ({@link GrotSpelRol}) with
 * clothes, building blocks and deco. Resources and the halls: tools/features/grotspellen.py.
 */
public final class GrotSpellenFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final List<ResourceKey<Structure>> STRUCTUREN = List.of(
            ResourceKey.create(Registries.STRUCTURE, Guhs.id(GrotSpel.Soort.SCHATGRAVEN.structuur)),
            ResourceKey.create(Registries.STRUCTURE, Guhs.id(GrotSpel.Soort.GLIMWORMVANGEN.structuur)),
            ResourceKey.create(Registries.STRUCTURE, Guhs.id(GrotSpel.Soort.LICHTJESPUZZEL.structuur)));

    private static BlockBehaviour.Properties spelOnly(Block like) {
        return BlockBehaviour.Properties.ofFullCopy(like).strength(-1f, 3600000f).noLootTable();
    }

    private static BlockBehaviour.Properties deco(MapColor kleur, SoundType geluid) {
        return BlockBehaviour.Properties.of().mapColor(kleur).sound(geluid).strength(0.6f).noOcclusion();
    }

    // --- the game blocks (only in the halls, no item) ---------------------------------------------------------------
    public static final DeferredBlock<GrotSpellenBlocks.Schatgrond> SCHATGROND = BLOCKS.registerBlock("grotspel_schatgrond",
            GrotSpellenBlocks.Schatgrond::new, () -> spelOnly(Blocks.PINK_WOOL).noOcclusion());
    public static final DeferredBlock<GrotSpellenBlocks.Puzzellamp> PUZZELLAMP = BLOCKS.registerBlock("grotspel_puzzellamp",
            GrotSpellenBlocks.Puzzellamp::new, () -> spelOnly(Blocks.SHROOMLIGHT).lightLevel(s -> s.getValue(GrotSpellenBlocks.AAN) ? 13 : 1));

    // --- building blocks (the shops: 1 coin for 4) ------------------------------------------------------------------
    public static final DeferredBlock<Block> SCHATZANDSTEEN = BLOCKS.registerSimpleBlock("schatzandsteen",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SANDSTONE).mapColor(MapColor.COLOR_PINK));
    public static final DeferredBlock<Block> SCHATKISTPLANKEN = BLOCKS.registerSimpleBlock("schatkistplanken",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_PLANKS));
    public static final DeferredBlock<Block> NACHTVILT = BLOCKS.registerSimpleBlock("nachtvilt",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BLUE_WOOL));
    public static final DeferredBlock<Block> GLIMMOS = BLOCKS.registerSimpleBlock("glimmos",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_BLOCK).lightLevel(s -> 7));
    public static final DeferredBlock<Block> PADDENSTOELSTEEN = BLOCKS.registerSimpleBlock("paddenstoelsteen",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.RED_MUSHROOM_BLOCK).strength(1.2f));
    public static final DeferredBlock<Block> STEELTJESSTEEN = BLOCKS.registerSimpleBlock("steeltjessteen",
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.MUSHROOM_STEM).strength(1.2f));

    // --- deco -------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<GuhFurnitureBlock> SCHATKISTJE = BLOCKS.registerBlock("schatkistje",
            p -> new GuhFurnitureBlock(p, -1, new double[]{1, 0, 3, 15, 12, 13}), () -> deco(MapColor.COLOR_PINK, SoundType.WOOD));
    public static final DeferredBlock<GuhFurnitureBlock> GOUDEN_KAASJE = BLOCKS.registerBlock("gouden_kaasje",
            p -> new GuhFurnitureBlock(p, -1, new double[]{3, 0, 3, 13, 7, 13}), () -> deco(MapColor.GOLD, SoundType.METAL).lightLevel(s -> 4));
    /** A jar of glimwormpjes: the hall's lamps (their light follows your catch) and the shop's deco (always bright). */
    public static final DeferredBlock<GrotSpellenBlocks.Glimpot> GLIMPOT = BLOCKS.registerBlock("glimpotje", GrotSpellenBlocks.Glimpot::new,
            () -> deco(MapColor.COLOR_LIGHT_GREEN, SoundType.GLASS).lightLevel(s -> GrotSpellenBlocks.LICHT_NIVEAU[s.getValue(GrotSpellenBlocks.LICHT)]));
    public static final DeferredBlock<GrotSpellenBlocks.Glimplant> GLIMPLANT = BLOCKS.registerBlock("glimplant", GrotSpellenBlocks.Glimplant::new,
            () -> deco(MapColor.COLOR_CYAN, SoundType.AZALEA).noCollision().instabreak().lightLevel(s -> 5));
    public static final DeferredBlock<GrotSpellenBlocks.Klein> PADDENSTOELLAMPJE = BLOCKS.registerBlock("paddenstoellampje",
            p -> new GrotSpellenBlocks.Klein(p, new double[]{4, 0, 4, 12, 11, 12}), () -> deco(MapColor.COLOR_RED, SoundType.WOOL).lightLevel(s -> 13));
    public static final DeferredBlock<GuhFurnitureBlock> PADDENSTOELKRUKJE = BLOCKS.registerBlock("paddenstoelkrukje",
            p -> new GuhFurnitureBlock(p, 0.5, new double[]{2, 0, 2, 14, 9, 14}), () -> deco(MapColor.COLOR_RED, SoundType.WOOL));
    public static final DeferredBlock<GrotSpellenBlocks.Lampjestegel> LAMPJESTEGEL = BLOCKS.registerBlock("lampjestegel",
            GrotSpellenBlocks.Lampjestegel::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SHROOMLIGHT)
                    .lightLevel(s -> s.getValue(GrotSpellenBlocks.AAN) ? 12 : 0));

    // --- items --------------------------------------------------------------------------------------------------------
    public static final DeferredItem<Item> SCHATMUNTJE = ITEMS.registerSimpleItem("schatmuntje", () -> new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> GLIMMERTJE = ITEMS.registerSimpleItem("glimmertje", () -> new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> LAMPJESMUNT = ITEMS.registerSimpleItem("lampjesmunt", () -> new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<GrotSpelItems.Schepje> SCHATSCHEPJE = ITEMS.registerItem("schatschepje", GrotSpelItems.Schepje::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<GrotSpelItems.Netje> GLIMNETJE = ITEMS.registerItem("glimnetje", GrotSpelItems.Netje::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<GrotSpelItems.Potje> GLIMPOTJE_RUG = ITEMS.registerItem("glimpotje_rug", GrotSpelItems.Potje::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    public static final DeferredItem<BlockItem> SCHATZANDSTEEN_ITEM = ITEMS.registerSimpleBlockItem(SCHATZANDSTEEN);
    public static final DeferredItem<BlockItem> SCHATKISTPLANKEN_ITEM = ITEMS.registerSimpleBlockItem(SCHATKISTPLANKEN);
    public static final DeferredItem<BlockItem> NACHTVILT_ITEM = ITEMS.registerSimpleBlockItem(NACHTVILT);
    public static final DeferredItem<BlockItem> GLIMMOS_ITEM = ITEMS.registerSimpleBlockItem(GLIMMOS);
    public static final DeferredItem<BlockItem> PADDENSTOELSTEEN_ITEM = ITEMS.registerSimpleBlockItem(PADDENSTOELSTEEN);
    public static final DeferredItem<BlockItem> STEELTJESSTEEN_ITEM = ITEMS.registerSimpleBlockItem(STEELTJESSTEEN);
    public static final DeferredItem<BlockItem> SCHATKISTJE_ITEM = ITEMS.registerSimpleBlockItem(SCHATKISTJE);
    public static final DeferredItem<BlockItem> GOUDEN_KAASJE_ITEM = ITEMS.registerSimpleBlockItem(GOUDEN_KAASJE);
    public static final DeferredItem<BlockItem> GLIMPOTJE_ITEM = ITEMS.registerSimpleBlockItem(GLIMPOT);
    public static final DeferredItem<BlockItem> GLIMPLANT_ITEM = ITEMS.registerSimpleBlockItem(GLIMPLANT);
    public static final DeferredItem<BlockItem> PADDENSTOELLAMPJE_ITEM = ITEMS.registerSimpleBlockItem(PADDENSTOELLAMPJE);
    public static final DeferredItem<BlockItem> PADDENSTOELKRUKJE_ITEM = ITEMS.registerSimpleBlockItem(PADDENSTOELKRUKJE);
    public static final DeferredItem<BlockItem> LAMPJESTEGEL_ITEM = ITEMS.registerSimpleBlockItem(LAMPJESTEGEL);

    public static final DeferredHolder<EntityType<?>, EntityType<VangGlimwormEntity>> VANGGLIMWORM = ENTITIES.register("vangglimworm",
            () -> EntityType.Builder.<VangGlimwormEntity>of(VangGlimwormEntity::new, MobCategory.MISC).sized(0.4f, 0.4f)
                    .clientTrackingRange(8).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vangglimworm"))));

    /** The clothes of each game (for the outfit check, the Guhdex groups). */
    public static final List<GuhClothes> KLEDING = List.of(GuhClothes.SCHAT_PIRATENHOEDJE, GuhClothes.SCHAT_OOGKLEPJE, GuhClothes.GLIM_ANTENNETJES,
            GuhClothes.GLIM_VLINDERNETJE, GuhClothes.LICHTJES_PADDENSTOELHOEDJE);

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> e.put(VANGGLIMWORM.get(), VangGlimwormEntity.createAttributes().build()));
        for (GrotSpel.Soort soort : GrotSpel.Soort.values()) {
            NpcRollen.zet(soort.npc(), new GrotSpelRol(soort));
            Minigames.registerGame(soort.id, p -> GrotSpel.speelt(p, soort));
        }
        NpcRollen.zet(GuhNpcEntity.Kind.SNUFFELHULPJE, GrotSpelRol.SNUFFIE);
        NeoForge.EVENT_BUS.register(GrotSpellenEvents.class);
        Protected.add(GrotSpellenEvents::inZaal);
        bron(GuhClothes.SCHAT_PIRATENHOEDJE, GrotSpel.Soort.SCHATGRAVEN, GrotSpelRol.PRIJS_PIRATENHOEDJE);
        bron(GuhClothes.SCHAT_OOGKLEPJE, GrotSpel.Soort.SCHATGRAVEN, GrotSpelRol.PRIJS_OOGKLEPJE);
        bron(GuhClothes.GLIM_ANTENNETJES, GrotSpel.Soort.GLIMWORMVANGEN, GrotSpelRol.PRIJS_ANTENNETJES);
        bron(GuhClothes.GLIM_VLINDERNETJE, GrotSpel.Soort.GLIMWORMVANGEN, GrotSpelRol.PRIJS_VLINDERNETJE);
        bron(GuhClothes.LICHTJES_PADDENSTOELHOEDJE, GrotSpel.Soort.LICHTJESPUZZEL, GrotSpelRol.PRIJS_PADDENSTOELHOEDJE);
        // the Minigames tab of the Guhdex and the Superkompas (Minigames tab, after the subheadings like the Wisselgrot and the
        // Kegelgrot; the compass finds the nearest hall): visited when you stand in the hall or talk to the host
        for (GrotSpel.Soort soort : GrotSpel.Soort.values()) {
            nl.juiced.guhs.item.SuperkompasItem.voegToe("minigames", soort.structuur);
        }
        groep(GrotSpel.Soort.SCHATGRAVEN, "schatmuntje", Items.GOLD_NUGGET);
        groep(GrotSpel.Soort.GLIMWORMVANGEN, "glimmertje", Items.GLOW_BERRIES);
        groep(GrotSpel.Soort.LICHTJESPUZZEL, "lampjesmunt", Items.SHROOMLIGHT);
    }

    private static void bron(GuhClothes c, GrotSpel.Soort soort, int prijs) {
        KledingBronnen.bron(c, soort.id, KledingBronnen.prijs("gui.guhs.kleding.prijs.munt." + munten(soort), prijs));
    }

    /** The lang word of a game's coins (gui.guhs.kleding.prijs.munt.&lt;x&gt;: "%s schatmuntjes"). */
    static String munten(GrotSpel.Soort soort) {
        return switch (soort) {
            case SCHATGRAVEN -> "schatmuntjes";
            case GLIMWORMVANGEN -> "glimmertjes";
            case LICHTJESPUZZEL -> "lampjesmunten";
        };
    }

    private static void groep(GrotSpel.Soort soort, String munt, Item standIn) {
        SpelGroepen.groep(new SpelGroepen.Groep(soort.id, SpelGroepen.Tijdperk.GROTE_GUHSPELEN, SpelGroepen.icoon(munt, standIn), soort.structuur, soort.npc(),
                soort.borden()));
    }

    public static void payloads(net.neoforged.neoforge.network.registration.PayloadRegistrar registrar) {
        GrotSpellenPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<?> item : List.of(SCHATMUNTJE, GLIMMERTJE, LAMPJESMUNT, SCHATZANDSTEEN_ITEM, SCHATKISTPLANKEN_ITEM, NACHTVILT_ITEM,
                GLIMMOS_ITEM, PADDENSTOELSTEEN_ITEM, STEELTJESSTEEN_ITEM, SCHATKISTJE_ITEM, GOUDEN_KAASJE_ITEM, GLIMPOTJE_ITEM, GLIMPLANT_ITEM,
                PADDENSTOELLAMPJE_ITEM, PADDENSTOELKRUKJE_ITEM, LAMPJESTEGEL_ITEM)) {
            output.accept(new ItemStack(item.get()));
        }
    }

    /** Is this one of the tools the hosts lend you? */
    public static boolean geleend(ItemStack stack) {
        return stack.is(SCHATSCHEPJE.get()) || stack.is(GLIMNETJE.get()) || stack.is(GLIMPOTJE_RUG.get());
    }

    /** The present after your very first game: a piece of the game's deco. */
    static ItemStack cadeautje(GrotSpel.Soort soort) {
        return switch (soort) {
            case SCHATGRAVEN -> new ItemStack(GOUDEN_KAASJE_ITEM.get());
            case GLIMWORMVANGEN -> new ItemStack(GLIMPOTJE_ITEM.get());
            case LICHTJESPUZZEL -> new ItemStack(PADDENSTOELLAMPJE_ITEM.get());
        };
    }

    /** A game's whole outfit (unlocked, or all in your pockets): the hidden advancement quest/&lt;game&gt;_kleding (for FTB). */
    public static void checkKleding(ServerPlayer player) {
        for (GrotSpel.Soort soort : GrotSpel.Soort.values()) {
            boolean alles = true;
            for (GuhClothes c : KLEDING) {
                if (soort.id.equals(KledingBronnen.bron(c))) {
                    alles &= KledingUnlocks.heeft(player, c) || player.getInventory().countItem(ModItems.clothingItem(c)) > 0;
                }
            }
            if (alles) {
                advancement(player, soort.id + "_kleding");
            }
        }
    }

    /** Grants the hidden advancement quest/&lt;name&gt; (all its criteria), when it exists. */
    public static void advancement(ServerPlayer player, String name) {
        var holder = player.level().getServer().getAdvancements().get(Guhs.id("quest/" + name));
        if (holder == null) {
            return;
        }
        var progress = player.getAdvancements().getOrStartProgress(holder);
        if (!progress.isDone()) {
            List<String> remaining = new java.util.ArrayList<>();
            progress.getRemainingCriteria().forEach(remaining::add);
            for (String criterion : remaining) {
                player.getAdvancements().award(holder, criterion);
            }
        }
    }

    private GrotSpellenFeature() {
    }
}
