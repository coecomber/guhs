package nl.juiced.guhs.feature.techquest;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.kaasmijn.LoreItem;
import nl.juiced.guhs.feature.techbron.AangebrandeMika;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.item.SuperkompasItem;

/**
 * bbq2 (tech-quests): where Guh-technologie is taught and where it ends.
 * <ul>
 *   <li><b>The Oude Guhrad-centrale</b> (structure oude_guhrad_centrale in the Guhbarbecuether, protected): five old Guhraden
 *       that still run, a practice hall with five broken setups ({@link Centrale}), the workshop of the
 *       <b>Uitvinder-guh</b> ({@link UitvinderRol}).</li>
 *   <li>Questline {@link #TECHNIEK} (per player): mend the five setups. Reward: the Bodemloos Knabbelmaagje (the Bank Guh's
 *       upgrade, bank's item) and the three recipe cards ({@link #RECEPT_SAUS}, {@link #RECEPT_MACHINES},
 *       {@link #RECEPT_BEZORG}) without which the machines of the "Saus" tier cannot be crafted. That is the gate of tier 3
 *       (tier 2 = zoutkristal in the recipes; tier 4 = a gloeister and the flag of the Aangebrande Mika).</li>
 *   <li>Questline {@link #KNABBELMACHINE} (per player, after the first and after the Aangebrande Mika): De Grote
 *       Knabbelmachine ({@link Knabbelmachine}), five stages of big deliveries; then one {@link #PERFECTE_KNABBEL} a day,
 *       the statuette {@link #KNABBELMACHINE_BEELDJE} and the title Knabbelmachinist.</li>
 * </ul>
 * Resources: tools/features/tech_quests.py (+ _bouw: the building, _modellen: the machine, _ftb: the whole FTB chapter
 * Guh-technologie, _wiki).
 */
public final class TechquestFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    public static final String UITVINDER_ID = "techquest_uitvinder";
    public static final float UITVINDER_YAW = -90f;
    public static final String KNABBELMACHINIST = "knabbelmachinist";

    // --- the fixed ids of CONTRACT_130 7 -----------------------------------------------------------------------------------
    /** The kern of the machine (its bowl): part of the building, never broken or dropped. It glows a little: the machine is drawn in its light. */
    public static final DeferredBlock<TechquestBlocks.Knabbelmachine> GROTE_KNABBELMACHINE = BLOCKS.registerBlock("grote_knabbelmachine",
            TechquestBlocks.Knabbelmachine::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(-1.0f, 3600000f)
                    .sound(SoundType.COPPER).noOcclusion().pushReaction(PushReaction.BLOCK).lightLevel(s -> 13));
    public static final DeferredItem<BlockItem> GROTE_KNABBELMACHINE_ITEM = ITEMS.registerItem("grote_knabbelmachine",
            p -> new LoreItem.Block(GROTE_KNABBELMACHINE.get(), p), () -> new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<Item> PERFECTE_KNABBEL = ITEMS.registerItem("perfecte_knabbel", p -> new LoreItem(p),
            () -> new Item.Properties().rarity(Rarity.RARE).food(new FoodProperties.Builder().nutrition(10).saturationModifier(1.0f).alwaysEdible().build(),
                    Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0), 1.0f))
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SPEED, 1200, 0), 1.0f)).build()));
    public static final DeferredBlock<TechquestBlocks.Beeldje> KNABBELMACHINE_BEELDJE = BLOCKS.registerBlock("knabbelmachine_beeldje",
            TechquestBlocks.Beeldje::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1.5f).sound(SoundType.COPPER)
                    .noOcclusion());
    public static final DeferredItem<LoreItem.Block> KNABBELMACHINE_BEELDJE_ITEM = ITEMS.registerItem("knabbelmachine_beeldje",
            p -> new LoreItem.Block(KNABBELMACHINE_BEELDJE.get(), p), () -> new Item.Properties().rarity(Rarity.UNCOMMON));

    // --- the rest ----------------------------------------------------------------------------------------------------------
    /** Never placed: its block states are the models of the machine's parts (client.KnabbelmachineRenderer). */
    public static final DeferredBlock<TechquestBlocks.Deel> KNABBELMACHINE_DEEL = BLOCKS.registerBlock("techquest_knabbelmachine_deel",
            TechquestBlocks.Deel::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(-1.0f, 3600000f).noOcclusion().noLootTable());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TechquestBlocks.Kern>> KNABBELMACHINE_BE = BLOCK_ENTITIES.register(
            "techquest_knabbelmachine", () -> new BlockEntityType<>(TechquestBlocks.Kern::new, GROTE_KNABBELMACHINE.get()));
    /** The three recipe cards: the reward of the practice hall, the key of the "Saus" tier. */
    public static final DeferredItem<TechquestBlocks.Receptkaart> RECEPT_SAUS = kaart("techquest_recept_saus");
    public static final DeferredItem<TechquestBlocks.Receptkaart> RECEPT_MACHINES = kaart("techquest_recept_machines");
    public static final DeferredItem<TechquestBlocks.Receptkaart> RECEPT_BEZORG = kaart("techquest_recept_bezorg");
    /** What rolls through the tubes of the practice hall: cardboard knabbels, and crumpled paper for the filter to refuse. */
    public static final DeferredItem<Item> OEFENKNABBEL = ITEMS.registerItem("techquest_oefenknabbel", p -> new LoreItem(p), () -> new Item.Properties());
    public static final DeferredItem<Item> OEFENROMMEL = ITEMS.registerItem("techquest_oefenrommel", p -> new LoreItem(p), () -> new Item.Properties());

    private static DeferredItem<TechquestBlocks.Receptkaart> kaart(String id) {
        return ITEMS.registerItem(id, TechquestBlocks.Receptkaart::new, () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    // --- the two questlines (per player; the Guhdex tab Verhalen shows them by themselves) ------------------------------------
    public static final Verhaallijn TECHNIEK = Verhaallijn.maak("techniek", "techniek").stappen(8).icoon("guhs:guh_wire")
            .nodig(UitvinderRol::nodig)
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:bank_upgrade", TechquestFeature.TECHNIEK.klaar(p)),
                    Verhaallijn.beloning("guhs:techquest_recept_saus", TechquestFeature.TECHNIEK.klaar(p)),
                    Verhaallijn.beloning("guhs:techquest_recept_machines", TechquestFeature.TECHNIEK.klaar(p)),
                    Verhaallijn.beloning("guhs:techquest_recept_bezorg", TechquestFeature.TECHNIEK.klaar(p))))
            .doel((p, stap) -> Doel.structuur(BarbecuetherFeature.BARBECUETHER, Centrale.STRUCTUUR, Component.translatable("structure.guhs." + Centrale.STRUCTUUR)))
            .registreer();
    public static final Verhaallijn KNABBELMACHINE = Verhaallijn.maak("knabbelmachine", "techniek").stappen(Knabbelmachine.FASE_KLAAR)
            .icoon("guhs:perfecte_knabbel").na("techniek")
            .nodig(Knabbelmachine::nodig)
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:perfecte_knabbel", "gui.guhs.techquest.beloning.knabbel", TechquestFeature.KNABBELMACHINE.klaar(p)),
                    Verhaallijn.beloning("guhs:knabbelmachine_beeldje", TechquestFeature.KNABBELMACHINE.klaar(p))))
            .doel((p, stap) -> Doel.structuur(BarbecuetherFeature.BARBECUETHER, Centrale.STRUCTUUR, Component.translatable("structure.guhs." + Centrale.STRUCTUUR)))
            .registreer();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        NpcRollen.zet(GuhNpcEntity.Kind.UITVINDERGUH, new UitvinderRol());
        // the same NPC stands in the template: worldgen places it, this only brings it back to a copy that lost it
        Bezetting.npc(UITVINDER_ID, Centrale.STRUCTUUR, null, Centrale.UITVINDER[0], GuhNpcEntity.Kind.UITVINDERGUH, null, UITVINDER_YAW);
        // the building stays whole; only the practice hall's own wires and the piece laid in a gap may be taken out
        Bescherming.registreer(Centrale.STRUCTUUR, 2);
        Bescherming.uitzondering(Centrale.STRUCTUUR, Centrale::magBreken);
        SuperkompasItem.voegToe("barbecue", Centrale.STRUCTUUR);
        Titels.registreer(new Titels.Titel(KNABBELMACHINIST, "gui.guhs.titels.naam." + KNABBELMACHINIST, ChatFormatting.GOLD, "guhs:knabbelmachine_beeldje",
                p -> TechquestFeature.KNABBELMACHINE.klaar(p)));
        KNABBELMACHINE.opStap((p, oud, nieuw) -> Knabbelmachine.sync(p));
        NeoForge.EVENT_BUS.addListener(TechquestFeature::opTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, (PlayerInteractEvent.RightClickBlock event) -> Centrale.opKlik(event));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> sync(event.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> sync(event.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> sync(event.getEntity()));
        NeoForge.EVENT_BUS.addListener(TechquestFeature::commando);
    }

    private static void opTick(ServerTickEvent.Post event) {
        Centrale.opTick(event);
        // a new day began while somebody stood at their machine: the knabbel appears in the bowl
        if (event.getServer().getTickCount() % 100 == 0) {
            for (ServerPlayer p : event.getServer().getPlayerList().getPlayers()) {
                if (KNABBELMACHINE.klaar(p)) {
                    Knabbelmachine.syncAlsAnders(p);
                }
            }
        }
    }

    private static void sync(net.minecraft.world.entity.player.Player player) {
        if (player instanceof ServerPlayer p) {
            Knabbelmachine.sync(p);
        }
    }

    public static void payloads(PayloadRegistrar registrar) {
        TechquestPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(GROTE_KNABBELMACHINE_ITEM.get()));
        output.accept(new ItemStack(PERFECTE_KNABBEL.get()));
        output.accept(new ItemStack(KNABBELMACHINE_BEELDJE_ITEM.get()));
        output.accept(new ItemStack(RECEPT_SAUS.get()));
        output.accept(new ItemStack(RECEPT_MACHINES.get()));
        output.accept(new ItemStack(RECEPT_BEZORG.get()));
        output.accept(new ItemStack(OEFENKNABBEL.get()));
        output.accept(new ItemStack(OEFENROMMEL.get()));
    }

    /**
     * /guhs techquest (operators; for AutoCheck scripts and dev checks, literal texts): "stand" says where you are in both
     * questlines, "fase &lt;0..6&gt;" sets the stage of YOUR Grote Knabbelmachine (the questline is started over up to there),
     * "dag" forgets that you took today's knabbel, "opstelling" says which setups of the copy you stand in work.
     * (The steps themselves: /guhs verhaal stap techniek|knabbelmachine.) Only in a dev run: "proef [0..3]" puts the
     * building down with your feet on the floor of its door (turned 0..3 quarter turns) and furnishes it, "proef weg"
     * forgets those copies, "dump" saves the generated copy you stand at as a template file.
     */
    private static void commando(RegisterCommandsEvent event) {
        var basis = Commands.literal("techquest").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("stand").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    c.getSource().sendSuccess(() -> Component.literal("techquest: techniek " + TECHNIEK.stap(p) + "/" + TECHNIEK.stappen() + ", knabbelmachine "
                            + KNABBELMACHINE.stap(p) + "/" + KNABBELMACHINE.stappen() + ", Aangebrande Mika " + AangebrandeMika.verslagen(p) + ", knabbel klaar "
                            + Knabbelmachine.knabbelKlaar(p)), false);
                    return 1;
                }))
                .then(Commands.literal("fase").then(Commands.argument("n", IntegerArgumentType.integer(0, Knabbelmachine.FASE_KLAAR)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    KNABBELMACHINE.wis(p);
                    KNABBELMACHINE.zet(p, IntegerArgumentType.getInteger(c, "n"));
                    Knabbelmachine.sync(p);
                    c.getSource().sendSuccess(() -> Component.literal("techquest: fase " + Knabbelmachine.fase(p)), false);
                    return 1;
                })))
                .then(Commands.literal("dag").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    nl.juiced.guhs.quest.GuhQuests.saved(p).remove(Knabbelmachine.DAG);
                    Knabbelmachine.sync(p);
                    c.getSource().sendSuccess(() -> Component.literal("techquest: knabbel klaar " + Knabbelmachine.knabbelKlaar(p)), false);
                    return 1;
                }))
                .then(Commands.literal("opstelling").executes(c -> {
                    Centrale.Kopie k = Centrale.bij(c.getSource().getLevel(), BlockPos.containing(c.getSource().getPosition()));
                    if (k == null) {
                        c.getSource().sendFailure(Component.literal("techquest: no Oude Guhrad-centrale here"));
                        return 0;
                    }
                    int werkt = 0;
                    StringBuilder tekst = new StringBuilder("techquest:");
                    for (Centrale.Opstelling o : Centrale.Opstelling.values()) {
                        boolean ok = Centrale.werkt(k, o);
                        werkt += ok ? 1 : 0;
                        tekst.append(' ').append(o.id()).append('=').append(ok ? "werkt" : "kapot");
                    }
                    c.getSource().sendSuccess(() -> Component.literal(tekst.toString()), false);
                    return werkt;
                }));
        if (!FMLEnvironment.isProduction()) {
            basis = basis.then(Commands.literal("proef").executes(c -> proef(c.getSource(), 0))
                            .then(Commands.argument("draai", IntegerArgumentType.integer(0, 3)).executes(c -> proef(c.getSource(), IntegerArgumentType.getInteger(c, "draai"))))
                            .then(Commands.literal("weg").executes(c -> {
                                PROEVEN.forEach(Centrale::proefUit);
                                PROEVEN.clear();
                                return 1;
                            })))
                    .then(Commands.literal("dump").executes(c -> dump(c.getSource())));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(basis));
    }

    private static final List<Centrale.Kopie> PROEVEN = new java.util.concurrent.CopyOnWriteArrayList<>();

    /** (dev) the building with the player's feet on the floor just inside its door, turned, furnished, ticking. */
    private static int proef(CommandSourceStack source, int kwart) {
        ServerLevel level = source.getLevel();
        Optional<StructureTemplate> template = level.getStructureManager().get(Guhs.id(Centrale.STRUCTUUR));
        if (template.isEmpty()) {
            source.sendFailure(Component.literal("techquest: no template"));
            return 0;
        }
        Rotation draai = Rotation.values()[kwart];
        BlockPos deur = new BlockPos(23, 4, 30);
        BlockPos hoek = BlockPos.containing(source.getPosition()).subtract(StructureTemplate.transform(deur, net.minecraft.world.level.block.Mirror.NONE, draai, BlockPos.ZERO));
        template.get().placeInWorld(level, hoek, hoek, new StructurePlaceSettings().setRotation(draai), level.getRandom(), 2);
        Centrale.Kopie k = Centrale.proef(level, hoek, draai, true);
        Centrale.proefAan(k);
        PROEVEN.add(k);
        Centrale.richtIn(k);
        source.sendSuccess(() -> Component.literal("techquest: proef at " + hoek.toShortString() + " turned " + draai), false);
        return 1;
    }

    /** (dev) the generated copy around the source, 10 blocks of land around it and 6 under it, as a template file. */
    private static int dump(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        StructureStart start = Bezetting.start(level, Centrale.STRUCTUUR, BlockPos.containing(source.getPosition()));
        if (start == null) {
            source.sendFailure(Component.literal("no copy of guhs:" + Centrale.STRUCTUUR + " here"));
            return 0;
        }
        BoundingBox box = start.getBoundingBox();
        BlockPos hoek = new BlockPos(box.minX() - 10, Math.max(level.getMinY(), box.minY() - 6), box.minZ() - 10);
        BlockPos maat = new BlockPos(box.getXSpan() + 20, Math.min(level.getMaxY(), box.maxY() + 8) - hoek.getY() + 1, box.getZSpan() + 20);
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, hoek, maat, false, List.of());
        var file = level.getServer().getServerDirectory().resolve("techquest_dump.nbt");
        try {
            NbtIo.writeCompressed(template.save(new CompoundTag()), file);
        } catch (java.io.IOException e) {
            source.sendFailure(Component.literal("dump failed: " + e));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("dumped " + box + " to " + file), false);
        return 1;
    }

    private TechquestFeature() {
    }
}
