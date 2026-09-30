package nl.juiced.guhs.feature.mewtwo;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.VariantGedragen;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.quest.GuhDex;

import net.minecraft.resources.Identifier;
/**
 * 3.0 (Guhverhalen), slice mewtwo: Het kloon-eiland (DESIGN_30 §3). Resources: tools/features/mewtwo.py (+ mewtwo_bouw.py,
 * mewtwo_tex.py).
 * <ul>
 *   <li>The structure {@code guhs:kloon_eiland}: a rock island with a lab in the Diepe Guhzee (a regio "zee" piece): the koepelhal
 *       with the cracked {@link KloontankBlock kloontank}, the office of Professor Knabbelkloon ({@link Knabbelkloon}, NPC kind
 *       KNABBELKLOON), the tower with the arena on top (the Guhtwo story copy, the {@link MewtwoBlokken.Knabbelschaal grote
 *       knabbelschaal}), the pier. Protected against breaking ({@link MewtwoProtection}).</li>
 *   <li>The questline per player ({@link MewtwoVoortgang}, {@link MewtwoVerhaal}): 6 {@link LabnotitieItem labnotities} ->
 *       4 {@link TankonderdeelItem tankonderdelen} in the tank -> Mieuwguh appears -> the big knabbel meal (a double portion) ->
 *       {@code VerhaalGuhs.geefVrij(MEWTWO)}; a click on the Guhtwo copy then tames your own once.</li>
 *   <li>The Guhtwo variant ({@link MewtwoGedrag}): hovers (ZWEEFT, a purple glow), floats over short gaps when ridden,
 *       knabbel telekinesis (8 blocks), eats x2 (voerFactor 2, the x2 animation, double hearts; no extra healing).</li>
 *   <li>Mieuwguh ({@link MewEntity}, creature page MEW): only around the island after the questline ({@link MewSpawner}),
 *       not tameable.</li>
 *   <li>Outfits (source "mewtwo"): trainerpetje, trainerpakje, Guhtwo-staartje + nekbuisje, Mieuwguh-ballonnetje.</li>
 * </ul>
 */
public final class MewtwoFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    /** The structure guhs:kloon_eiland. */
    public static final ResourceKey<Structure> KLOON_EILAND = ResourceKey.create(Registries.STRUCTURE, Guhs.id("kloon_eiland"));
    /** The KledingBronnen source of the four outfit pieces. */
    public static final String BRON = "mewtwo";
    /** How many lab notes and tank parts there are. */
    public static final int NOTITIES = 6, ONDERDELEN = 4;
    /** The double portion for the big knabbel meal: kaas knabbels and snacks (#guhs:band/snacks). */
    public static final int PORTIE_KNABBELS = 32, PORTIE_SNACKS = 2;

    // --- blocks ------------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<KloontankBlock> KLOONTANK = BLOCKS.registerBlock("mewtwo_kloontank", KloontankBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(-1f, 3600000f).sound(SoundType.GLASS).noOcclusion()
                    .lightLevel(s -> 10).pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<MewtwoBlokken.Tankwand> TANKWAND = BLOCKS.registerBlock("mewtwo_tankwand", MewtwoBlokken.Tankwand::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(-1f, 3600000f).sound(SoundType.GLASS).noOcclusion()
                    .lightLevel(s -> 7).pushReaction(PushReaction.BLOCK).isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false));
    public static final DeferredBlock<MewtwoBlokken.Notitieplek> NOTITIEPLEK = BLOCKS.registerBlock("mewtwo_notitieplek", MewtwoBlokken.Notitieplek::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(-1f, 3600000f).sound(SoundType.WOOL).noOcclusion().noCollision()
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<MewtwoBlokken.Onderdelenkist> ONDERDELENKIST = BLOCKS.registerBlock("mewtwo_onderdelenkist",
            MewtwoBlokken.Onderdelenkist::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(-1f, 3600000f).sound(SoundType.WOOD)
                    .noOcclusion().pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<MewtwoBlokken.Knabbelschaal> KNABBELSCHAAL = BLOCKS.registerBlock("mewtwo_knabbelschaal",
            MewtwoBlokken.Knabbelschaal::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(1.5f, 6f).sound(SoundType.STONE)
                    .noOcclusion());
    public static final DeferredBlock<MewtwoBlokken.Deco> COMPUTER = BLOCKS.registerBlock("mewtwo_computer",
            p -> new MewtwoBlokken.Deco(p, MewtwoBlokken.COMPUTER_VORM), () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE)
                    .strength(1.2f).sound(SoundType.METAL).noOcclusion().lightLevel(s -> 6));
    public static final DeferredBlock<MewtwoBlokken.Deco> REAGEERBUISJES = BLOCKS.registerBlock("mewtwo_reageerbuisjes",
            p -> new MewtwoBlokken.Deco(p, MewtwoBlokken.BUISJES_VORM), () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK)
                    .strength(0.6f).sound(SoundType.GLASS).noOcclusion().lightLevel(s -> 4));
    public static final DeferredBlock<MewtwoBlokken.Papieren> PAPIEREN = BLOCKS.registerBlock("mewtwo_papieren", MewtwoBlokken.Papieren::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.1f).sound(SoundType.WOOL).noOcclusion().noCollision()
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KloontankBlockEntity>> KLOONTANK_BE = BLOCK_ENTITY_TYPES.register(
            "mewtwo_kloontank", () -> new BlockEntityType<>(KloontankBlockEntity::new, KLOONTANK.get()));

    // --- items -------------------------------------------------------------------------------------------------------------
    public static final DeferredItem<LabnotitieItem> LABNOTITIE = ITEMS.registerItem("mewtwo_labnotitie", LabnotitieItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<TankonderdeelItem> TANKONDERDEEL = ITEMS.registerItem("mewtwo_tankonderdeel", TankonderdeelItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    // --- Mieuwguh -----------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<MewEntity>> MEW = ENTITY_TYPES.register("mew",
            () -> EntityType.Builder.of(MewEntity::new, MobCategory.AMBIENT).sized(0.5f, 0.6f).eyeHeight(0.45f)
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("mew"))));
    public static final DeferredItem<SpawnEggItem> MEW_SPAWN_EGG = nl.juiced.guhs.registry.ModItems.spawnEgg(ITEMS, "mew_spawn_egg", MEW);   // (26.1: no tint colours; was FAB2D2/4682DE)

    // --- particles and sounds ----------------------------------------------------------------------------------------------
    /** A purple sparkle (the Guhtwo's glow, telekinesis trails). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLOED = PARTICLES.register("mewtwo_gloed", () -> new SimpleParticleType(false));
    /** A floating "x2" (the Guhtwo eats double). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> X2 = PARTICLES.register("mewtwo_x2", () -> new SimpleParticleType(true));
    /** A pink bubble (the repaired kloontank). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BUBBEL = PARTICLES.register("mewtwo_bubbel", () -> new SimpleParticleType(false));
    public static final DeferredHolder<SoundEvent, SoundEvent> MEW_GIECHEL = sound("mewtwo.mew_giechel");
    public static final DeferredHolder<SoundEvent, SoundEvent> TANK_BORREL = sound("mewtwo.tank_borrel");
    public static final DeferredHolder<SoundEvent, SoundEvent> TANK_KLIK = sound("mewtwo.tank_klik");
    public static final DeferredHolder<SoundEvent, SoundEvent> TANK_HEEL = sound("mewtwo.tank_heel");
    public static final DeferredHolder<SoundEvent, SoundEvent> TELEKINESE = sound("mewtwo.telekinese");
    public static final DeferredHolder<SoundEvent, SoundEvent> X2_SMUL = sound("mewtwo.x2");
    public static final DeferredHolder<SoundEvent, SoundEvent> NOTITIE = sound("mewtwo.notitie");

    static {
        for (DeferredBlock<?> block : List.of(KNABBELSCHAAL, COMPUTER, REAGEERBUISJES, PAPIEREN, KLOONTANK, NOTITIEPLEK, ONDERDELENKIST)) {
            ITEMS.registerItem(block.getId().getPath(), p -> new MewtwoItems.LoreBlock(block.get(), p));
        }
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        ENTITY_TYPES.register(modBus);
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(MEW.get(), MewEntity.createAttributes().build()));
        // the story: the professor, the Guhtwo copy, the questline listeners, the variant's behaviour
        NpcRollen.zet(GuhNpcEntity.Kind.KNABBELKLOON, new Knabbelkloon());
        VerhaalGuhs.opKlik(VerhaalGuh.MEWTWO, MewtwoVerhaal::klikKopie);
        MewtwoVerhaal.luisteraars();
        VariantGedragen.zet(GuhVariant.MEWTWO, new MewtwoGedrag());
        GuhDex.creaturePage(GuhVariant.MEW, MEW, 8.0);
        for (GuhClothes c : List.of(GuhClothes.MEWTWO_TRAINERPETJE, GuhClothes.MEWTWO_TRAINERPAKJE, GuhClothes.MEWTWO_STAARTJE,
                GuhClothes.MEW_BALLONNETJE)) {
            KledingBronnen.bron(c, BRON);
        }
        NeoForge.EVENT_BUS.register(MewtwoEvents.class);
        NeoForge.EVENT_BUS.register(MewtwoProtection.class);
        Protected.add(MewtwoProtection::opEiland);
    }

    public static void payloads(PayloadRegistrar registrar) {
        MewtwoPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(MEW_SPAWN_EGG.get()));
        output.accept(new ItemStack(KNABBELSCHAAL.get()));
        output.accept(new ItemStack(COMPUTER.get()));
        output.accept(new ItemStack(REAGEERBUISJES.get()));
        output.accept(new ItemStack(PAPIEREN.get()));
        for (int n = 1; n <= NOTITIES; n++) {
            output.accept(LabnotitieItem.maak(n));
        }
        for (int n = 1; n <= ONDERDELEN; n++) {
            output.accept(TankonderdeelItem.maak(n));
        }
    }

    private MewtwoFeature() {
    }
}
