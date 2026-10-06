package nl.juiced.guhs.feature.torenpeper;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.spiesburcht.Brouwsel;
import nl.juiced.guhs.feature.spiesburcht.RookguhEntity;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (toren-peper): two buildings of the Guhbarbecuether, each with its guh and a questline per player.
 * <ul>
 *   <li><b>De Rookguh-vuurtoren</b> (structure {@code guhs:rookguh_vuurtoren}, {@link Vuurtoren}) with the Torenwachter-guh
 *       ({@link TorenwachterRol}, questline {@link #VUURTOREN}): bring him gloeikoolgruis, light the lamp at the top
 *       ({@link VuurtorenlampBlock}) and guide three lost Rookguhs ({@link VerdwaaldeRookguhEntity}) to its light with the
 *       seinlantaarn. Reward: the Bezorgguhtje-fluitje of the tech-bezorg slice and the keeper's coat.</li>
 *   <li><b>De Pepertuin met kas</b> (structure {@code guhs:pepertuin}, {@link Pepertuin}) with the Peperteler-guh
 *       ({@link PepertelerRol}, questline {@link #PEPERTUIN}): grow the three peppers in the kweekbakken of the kas
 *       ({@link KweekbakBlock}: every player has an own plant in them, {@link Kweek}), brew a first pepper drink and let him
 *       taste. Reward: peperzaadjes, the other drink and a string of peppers for your guh.</li>
 *   <li>The crop itself: {@link PeperplantBlock} (the nether wart of the Barbecuether): one plant, three peppers
 *       ({@link PeperSoort}) depending on the ground it stands on, quick under glass. Two of them brew the new Guhdrankjes
 *       {@link #PEPERVUURDRANKJE} and {@link #PEPERZOETDRANKJE} (Brouwsel PEPERVUUR / PEPERZOET).</li>
 * </ul>
 * Nothing in either building changes for good: the lamp burns while somebody who lit it is near, the lost Rookguhs are a
 * player's own and never saved, the plants in the kweekbakken only exist per player. Resources: tools/features/toren_peper.py
 * (the buildings: toren_peper_bouw.py).
 */
public final class TorenpeperFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Guhs.MODID);
    /** Random salt of this slice (CONTRACT_130 3: 2130NN50L+). */
    public static final long SALT = 21301450L;

    /** The ground a pepper plant grows on. */
    public static final TagKey<Block> PEPERGROND = TagKey.create(Registries.BLOCK, Guhs.id("torenpeper/pepergrond"));
    /** Hot ground: the plant on it ripens into Vahoegpepers. */
    public static final TagKey<Block> HETE_GROND = TagKey.create(Registries.BLOCK, Guhs.id("torenpeper/hete_grond"));
    /** Sweet ground: the plant on it ripens into Snoeppepers. */
    public static final TagKey<Block> ZOETE_GROND = TagKey.create(Registries.BLOCK, Guhs.id("torenpeper/zoete_grond"));

    // --- blocks --------------------------------------------------------------------------------------------------------------
    /** The lamp of the lighthouse: it burns while a player who lit it is around. */
    public static final DeferredBlock<VuurtorenlampBlock> VUURTORENLAMP = BLOCKS.registerBlock("torenpeper_vuurtorenlamp", VuurtorenlampBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(3.0f).sound(SoundType.LANTERN).noOcclusion()
                    .lightLevel(s -> s.getValue(VuurtorenlampBlock.LIT) ? 15 : 0).isRedstoneConductor((s, l, p) -> false));
    /** The pepper plant (the nether wart of the Barbecuether). */
    public static final DeferredBlock<PeperplantBlock> PEPERPLANT = BLOCKS.registerBlock("torenpeper_peperplant", PeperplantBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_WART).mapColor(MapColor.PLANT).pushReaction(PushReaction.DESTROY));
    /** A kweekbak of the kas: everybody grows an own plant in it. */
    public static final DeferredBlock<KweekbakBlock> KWEEKBAK = BLOCKS.registerBlock("torenpeper_kweekbak", KweekbakBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD).noOcclusion());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VuurtorenlampBlock.Lamp>> VUURTORENLAMP_BE =
            BLOCK_ENTITY_TYPES.register("torenpeper_vuurtorenlamp", () -> new BlockEntityType<>(VuurtorenlampBlock.Lamp::new, VUURTORENLAMP.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KweekbakBlock.Bak>> KWEEKBAK_BE =
            BLOCK_ENTITY_TYPES.register("torenpeper_kweekbak", () -> new BlockEntityType<>(KweekbakBlock.Bak::new, KWEEKBAK.get()));

    // --- items -----------------------------------------------------------------------------------------------------------------
    public static final DeferredItem<BlockItem> VUURTORENLAMP_ITEM = ITEMS.registerSimpleBlockItem(VUURTORENLAMP);
    public static final DeferredItem<BlockItem> KWEEKBAK_ITEM = ITEMS.registerSimpleBlockItem(KWEEKBAK);
    /** Pepper seeds: plant them on as-aarde (green), gloeikool (red) or pindasaus-nylium (pink). */
    public static final DeferredItem<BlockItem> PEPERZAADJES = ITEMS.registerItem("torenpeper_peperzaadjes",
            p -> new PeperItems.Zaadjes(PEPERPLANT.get(), p), () -> new Item.Properties().useItemDescriptionPrefix());
    /** The mild green one. */
    public static final DeferredItem<PeperItems.Peper> NJEGPEPER = ITEMS.registerItem("torenpeper_njegpeper", p -> new PeperItems.Peper(PeperSoort.GROEN, p),
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).build()));
    /** The hot red one: eating it raw makes you run. */
    public static final DeferredItem<PeperItems.Peper> VAHOEGPEPER = ITEMS.registerItem("torenpeper_vahoegpeper", p -> new PeperItems.Peper(PeperSoort.ROOD, p),
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(2).saturationModifier(0.3f).alwaysEdible().build()));
    /** The sweet pink one. */
    public static final DeferredItem<PeperItems.Peper> SNOEPPEPER = ITEMS.registerItem("torenpeper_snoeppeper", p -> new PeperItems.Peper(PeperSoort.ROZE, p),
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.5f).build()));
    /** The Guhdrankje of the Vahoegpeper: you work like fire. */
    public static final DeferredItem<PeperItems.Drankje> PEPERVUURDRANKJE = ITEMS.registerItem("torenpeper_pepervuurdrankje",
            p -> new PeperItems.Drankje(Brouwsel.PEPERVUUR, p), () -> new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    /** The Guhdrankje of the Snoeppeper: warm and sweet inside. */
    public static final DeferredItem<PeperItems.Drankje> PEPERZOETDRANKJE = ITEMS.registerItem("torenpeper_peperzoetdrankje",
            p -> new PeperItems.Drankje(Brouwsel.PEPERZOET, p), () -> new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    /** The keeper's signal lantern: lost Rookguhs follow whoever holds it. */
    public static final DeferredItem<PeperItems.Lore> SEINLANTAARN = ITEMS.registerItem("torenpeper_seinlantaarn", PeperItems.Lore::new,
            () -> new Item.Properties().stacksTo(1));
    /** The glowing coal the lamp is lit with (a quest item: the keeper gives it, the lamp takes it). */
    public static final DeferredItem<PeperItems.Lore> LAMPKOOLTJE = ITEMS.registerItem("torenpeper_lampkooltje", PeperItems.Lore::new,
            () -> new Item.Properties().stacksTo(1).fireResistant());

    // --- the lost Rookguh ----------------------------------------------------------------------------------------------------------
    /** A lost Rookguh of one player's questline: never saved, never spawned by the world. */
    public static final DeferredHolder<EntityType<?>, EntityType<VerdwaaldeRookguhEntity>> VERDWAALDE_ROOKGUH = ENTITY_TYPES.register(
            "torenpeper_verdwaalde_rookguh", () -> EntityType.Builder.<VerdwaaldeRookguhEntity>of(VerdwaaldeRookguhEntity::new, MobCategory.MISC)
                    .sized(1.4f, 1.4f).eyeHeight(1.0f).fireImmune().noSave().clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("torenpeper_verdwaalde_rookguh"))));

    /** Peperadem (the Pepervuurdrankje): little flames with every breath, and you are never cold. */
    public static final DeferredHolder<MobEffect, MobEffect> PEPERADEM = EFFECTS.register("torenpeper_peperadem", () -> new PeperItems.Peperadem(
            MobEffectCategory.BENEFICIAL, 0xD8322A));

    // --- sounds (vanilla sounds, pitched, in sounds.json) -------------------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> LAMP_AAN = sound("torenpeper.lamp_aan");
    public static final DeferredHolder<SoundEvent, SoundEvent> MISTHOORN = sound("torenpeper.misthoorn");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLUK = sound("torenpeper.pluk");
    public static final DeferredHolder<SoundEvent, SoundEvent> HEET = sound("torenpeper.heet");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    // --- the two questlines (per player) ------------------------------------------------------------------------------------------
    /**
     * The Torenwachter-guh's questline: 0 talk, 1 bring {@link Vuurtoren#GRUIS_NODIG} gloeikoolgruis (he gives the lampkooltje and
     * the seinlantaarn), 2 light the lamp at the top, 3 guide {@link Vuurtoren#ROOKGUHS} lost Rookguhs to the light, 4 come back
     * for the whistle and the coat; 5 = done.
     */
    public static final Verhaallijn VUURTOREN = Verhaallijn.maak("vuurtoren", "barbecue").stappen(5).icoon("guhs:torenpeper_seinlantaarn")
            .nodig((p, stap) -> stap == 1 ? List.of(Verhaallijn.nodig("guhs:gloeikoolgruis", GuhQuests.count(p, BarbecuetherFeature.GLOEIKOOLGRUIS.get()),
                    Vuurtoren.GRUIS_NODIG))
                    : stap == 3 ? List.of(Verhaallijn.nodig("guhs:rookguh_spawn_egg", "gui.guhs.torenpeper.nodig.rookguhs", Vuurtoren.thuis(p), Vuurtoren.ROOKGUHS))
                    : List.of())
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:torenpeper_seinlantaarn", vuurtorenStap(p) >= 2),
                    Verhaallijn.beloning("guhs:bezorgguhtje_fluitje", vuurtorenStap(p) >= 5),
                    Verhaallijn.beloning("guhs:torenpeper_wachtersjas", vuurtorenStap(p) >= 5)))
            .doel((p, stap) -> Doel.structuur(BarbecuetherFeature.BARBECUETHER, Vuurtoren.STRUCTUUR, Component.translatable("structure.guhs.rookguh_vuurtoren")))
            .registreer();
    /**
     * The Peperteler-guh's questline: 0 talk (three peperzaadjes), 1 grow and pick the three peppers in the kweekbakken, 2 brew a
     * first pepper drink, 3 let him taste; 4 = done.
     */
    public static final Verhaallijn PEPERTUIN = Verhaallijn.maak("pepertuin", "barbecue").stappen(4).icoon("guhs:torenpeper_vahoegpeper")
            .nodig((p, stap) -> stap == 1 ? List.of(
                    Verhaallijn.nodig("guhs:torenpeper_njegpeper", Pepertuin.geplukt(p, PeperSoort.GROEN) ? 1 : 0, 1),
                    Verhaallijn.nodig("guhs:torenpeper_vahoegpeper", Pepertuin.geplukt(p, PeperSoort.ROOD) ? 1 : 0, 1),
                    Verhaallijn.nodig("guhs:torenpeper_snoeppeper", Pepertuin.geplukt(p, PeperSoort.ROZE) ? 1 : 0, 1))
                    : stap == 2 ? List.of(Verhaallijn.nodig("guhs:torenpeper_pepervuurdrankje", "gui.guhs.torenpeper.nodig.peperdrankje",
                    Pepertuin.heeftDrankje(p) ? 1 : 0, 1))
                    : List.of())
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:torenpeper_peperzaadjes", pepertuinStap(p) >= 1),
                    Verhaallijn.beloning("guhs:torenpeper_peperzoetdrankje", "gui.guhs.torenpeper.beloning.drankje", pepertuinStap(p) >= 4),
                    Verhaallijn.beloning("guhs:torenpeper_peperslinger", pepertuinStap(p) >= 4)))
            .doel((p, stap) -> Doel.structuur(BarbecuetherFeature.BARBECUETHER, Pepertuin.STRUCTUUR, Component.translatable("structure.guhs.pepertuin")))
            .registreer();

    private static int vuurtorenStap(ServerPlayer p) {
        return VUURTOREN.stap(p);
    }

    private static int pepertuinStap(ServerPlayer p) {
        return PEPERTUIN.stap(p);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        EFFECTS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(VERDWAALDE_ROOKGUH.get(), RookguhEntity.createAttributes().build()));
        Brouwsel.zetDrankje(Brouwsel.PEPERVUUR, PEPERVUURDRANKJE, TorenpeperFeature::pepervuurEffecten);
        Brouwsel.zetDrankje(Brouwsel.PEPERZOET, PEPERZOETDRANKJE, TorenpeperFeature::peperzoetEffecten);
        KledingBronnen.bron(GuhClothes.TORENPEPER_WACHTERSJAS, "toren_peper");
        KledingBronnen.bron(GuhClothes.TORENPEPER_PEPERSLINGER, "toren_peper");
        // the two buildings: in the Superkompas, nobody breaks them, their guhs also come to copies that miss them
        for (String structuur : List.of(Vuurtoren.STRUCTUUR, Pepertuin.STRUCTUUR)) {
            SuperkompasItem.voegToe("barbecue", structuur);
            Bescherming.registreer(structuur, 2);
        }
        NpcRollen.zet(GuhNpcEntity.Kind.TORENWACHTERGUH, new TorenwachterRol());
        NpcRollen.zet(GuhNpcEntity.Kind.PEPERTELERGUH, new PepertelerRol());
        Bezetting.npc(Vuurtoren.TORENWACHTER, Vuurtoren.STRUCTUUR, null, Vuurtoren.NPC, GuhNpcEntity.Kind.TORENWACHTERGUH, null, Vuurtoren.NPC_YAW);
        Bezetting.npc(Pepertuin.PEPERTELER, Pepertuin.STRUCTUUR, null, Pepertuin.NPC, GuhNpcEntity.Kind.PEPERTELERGUH, null, Pepertuin.NPC_YAW);
        NeoForge.EVENT_BUS.register(TorenpeperEvents.class);
        NeoForge.EVENT_BUS.addListener(TorenpeperFeature::commando);
    }

    /** What a Pepervuurdrankje gives: three minutes of haste, with flames in your breath. */
    public static List<MobEffectInstance> pepervuurEffecten() {
        return List.of(new MobEffectInstance(MobEffects.HASTE, 3600, 1), new MobEffectInstance(PEPERADEM, 3600, 0));
    }

    /** What a Peperzoetdrankje gives: a warm, sweet feeling (a little healing and two extra hearts for a while). */
    public static List<MobEffectInstance> peperzoetEffecten() {
        return List.of(new MobEffectInstance(MobEffects.REGENERATION, 600, 0), new MobEffectInstance(MobEffects.ABSORPTION, 2400, 0));
    }

    public static void payloads(PayloadRegistrar registrar) {
        Kweek.payloads(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<? extends Item> item : List.of(PEPERZAADJES, NJEGPEPER, VAHOEGPEPER, SNOEPPEPER, PEPERVUURDRANKJE, PEPERZOETDRANKJE, SEINLANTAARN,
                VUURTORENLAMP_ITEM, KWEEKBAK_ITEM)) {
            output.accept(new ItemStack(item.get()));
        }
    }

    /**
     * {@code /guhs torenpeper ...} (ops; for the AutoCheck script and dev checks): {@code stand} says where the caller is in both
     * questlines, {@code rookguh} lets a lost Rookguh of the caller appear at the nearest lamp, {@code kweek <0-4>} gives the
     * caller's plants in the three kweekbakken that stage (0 empty, 4 ripe), and in dev runs only {@code toren} / {@code tuin}
     * place a building's template around you (your feet on its guh's spot) and {@code dump <structuur>} saves the generated
     * copy at the source's position, with the land around it, as a template file in the server folder (to look at how a
     * building lies in real terrain).
     */
    private static void commando(RegisterCommandsEvent event) {
        var wortel = Commands.literal("torenpeper").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("stand").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    c.getSource().sendSuccess(() -> Component.literal("vuurtoren: stap " + VUURTOREN.stap(p) + ", Rookguhs thuis " + Vuurtoren.thuis(p)
                            + "; pepertuin: stap " + PEPERTUIN.stap(p) + ", geplukt " + Pepertuin.aantalGeplukt(p) + "/3"), false);
                    return 1;
                }))
                .then(Commands.literal("rookguh").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    BlockPos lamp = Vuurtoren.lampBij(p.level(), p.blockPosition(), Vuurtoren.BEREIK);
                    if (lamp == null) {
                        c.getSource().sendFailure(Component.literal("Geen vuurtorenlamp binnen " + Vuurtoren.BEREIK + " blokken"));
                        return 0;
                    }
                    return Vuurtoren.laatVerdwalen(p, lamp) != null ? 1 : 0;
                }))
                .then(Commands.literal("kweek").then(Commands.argument("groei", IntegerArgumentType.integer(0, Kweek.RIJP)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    return Kweek.zetGroei(p, IntegerArgumentType.getInteger(c, "groei"));
                })));
        if (!FMLEnvironment.isProduction()) {
            wortel.then(Commands.literal("toren").executes(c -> plaats(c.getSource(), Vuurtoren.STRUCTUUR, Vuurtoren.NPC)));
            wortel.then(Commands.literal("tuin").executes(c -> plaats(c.getSource(), Pepertuin.STRUCTUUR, Pepertuin.NPC)));
            wortel.then(Commands.literal("dump").then(Commands.argument("structuur", StringArgumentType.word())
                    .executes(c -> dump(c.getSource(), StringArgumentType.getString(c, "structuur")))));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(wortel));
    }

    /** (dev) the template with its NPC spot under the caller. */
    private static int plaats(CommandSourceStack s, String structuur, BlockPos npc) {
        BlockPos hoek = BlockPos.containing(s.getPosition()).subtract(npc);
        boolean gelukt = Vuurtoren.plaats(s.getLevel(), structuur, hoek);
        s.sendSuccess(() -> Component.literal(gelukt ? "guhs:" + structuur + " staat op " + hoek.toShortString() : "Geen template guhs:" + structuur), false);
        return gelukt ? 1 : 0;
    }

    /** (dev) the copy of this structure around the source, 10 blocks of land around it and 4 under it, as a template file. */
    private static int dump(CommandSourceStack source, String structuur) {
        ServerLevel level = source.getLevel();
        StructureStart start = Bezetting.start(level, structuur, BlockPos.containing(source.getPosition()));
        if (start == null) {
            source.sendFailure(Component.literal("no copy of guhs:" + structuur + " here"));
            return 0;
        }
        BoundingBox box = start.getBoundingBox();
        BlockPos hoek = new BlockPos(box.minX() - 10, Math.max(level.getMinY(), box.minY() - 4), box.minZ() - 10);
        BlockPos maat = new BlockPos(box.getXSpan() + 20, Math.min(level.getMaxY(), box.maxY() + 6) - hoek.getY() + 1, box.getZSpan() + 20);
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, hoek, maat, false, List.of());
        var file = level.getServer().getServerDirectory().resolve("torenpeper_dump_" + structuur + ".nbt");
        try {
            NbtIo.writeCompressed(template.save(new CompoundTag()), file);
        } catch (java.io.IOException e) {
            source.sendFailure(Component.literal("dump failed: " + e));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("dumped " + box + " from corner " + hoek.toShortString() + " to " + file), false);
        return 1;
    }

    private TorenpeperFeature() {
    }
}
