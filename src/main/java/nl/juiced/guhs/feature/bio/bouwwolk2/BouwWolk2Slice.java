package nl.juiced.guhs.feature.bio.bouwwolk2;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.guhpixel.blok.DecoBlock;
import nl.juiced.guhs.feature.guhpixel.blok.LoreBlockItem;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.item.SuperkompasItem;

/**
 * biomes3 slice "bouw-wolk2": three findable wonders of the Wolkenweide (Superkompas tab "wonderen").
 * <ul>
 *   <li><b>Regenboogbrug</b>: a rainbow you walk over, from a floating island to a bank of cloud. At its end the pot of
 *       kaasknabbels ({@link PotBlock}: a handful a day per player and a few rainbow blocks). The rainbow is a building
 *       material: you may break it, and it grows back ({@link Bewakers#hergroei}).</li>
 *   <li><b>Wolkenkasteeltje</b>: a half-ruined castle of cloud with the sleeping giant guh ({@link ReuzenguhEntity}) and his
 *       hoard ({@link SchatBlock}: one gouden knabbelkruimel a day per player). Sneak past him; run, jump or break blocks
 *       in his hall and he stirs, then sneezes you gently out of the castle. The geometry: {@link Kasteel}.</li>
 *   <li><b>Bliksemsmidse</b>: a dark thundercloud ({@link OnweerswolkBlock}: small harmless flashes and a soft rumble, on
 *       the client only) with the smid-guh ({@link Smidguh}), who trades wolkenpluis for cloud furniture.</li>
 * </ul>
 * {@link Bewakers} keeps the giant and the smid in their place (one each, put back when missing), grows the rainbow back
 * and grants the "found" proofs. Resources: tools/features/bio_bouw_wolk2.py (templates: bio_bouw_wolk2_bouw.py).
 */
public final class BouwWolk2Slice {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);

    public static final ResourceKey<Structure> REGENBOOGBRUG = ResourceKey.create(Registries.STRUCTURE, Guhs.id("regenboogbrug"));
    public static final ResourceKey<Structure> WOLKENKASTEELTJE = ResourceKey.create(Registries.STRUCTURE, Guhs.id("wolkenkasteeltje"));
    public static final ResourceKey<Structure> BLIKSEMSMIDSE = ResourceKey.create(Registries.STRUCTURE, Guhs.id("bliksemsmidse"));
    /** The three structures, as the Superkompas lists them. */
    public static final List<String> STRUCTUREN = List.of("regenboogbrug", "wolkenkasteeltje", "bliksemsmidse");

    // --- the giant ----------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<ReuzenguhEntity>> REUZENGUH = ENTITY_TYPES.register("reuzenguh",
            () -> EntityType.Builder.of(ReuzenguhEntity::new, MobCategory.MISC).sized(4.6f, 4.2f).eyeHeight(2.4f).clientTrackingRange(10)
                    .fireImmune().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("reuzenguh"))));

    // --- sounds (tools/features/bio_bouw_wolk2_geluid.py) and particles ----------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> SNURK = geluid("reuzenguh.snurk");
    public static final DeferredHolder<SoundEvent, SoundEvent> GROM = geluid("reuzenguh.grom");
    public static final DeferredHolder<SoundEvent, SoundEvent> NIES = geluid("reuzenguh.nies");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROMMEL = geluid("bliksemsmidse.rommel");
    public static final DeferredHolder<SoundEvent, SoundEvent> HAMER = geluid("smidguh.hamer");
    /** A "z" floating up from the sleeping giant. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ZZZ = PARTICLES.register("reuzenguh_zzz", () -> new SimpleParticleType(false));
    /** A small soft flash on the thundercloud. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLITS = PARTICLES.register("bliksemsmidse_flits", () -> new SimpleParticleType(false));

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    // --- the thundercloud --------------------------------------------------------------------------------------------------
    private static BlockBehaviour.Properties onweer() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.3f).sound(SoundType.WOOL);
    }

    public static final DeferredBlock<OnweerswolkBlock> ONWEERSWOLK = BLOCKS.registerBlock("bliksemsmidse_wolk", OnweerswolkBlock::new, () -> onweer());
    public static final DeferredBlock<SlabBlock> ONWEERSWOLK_PLAAT = BLOCKS.registerBlock("bliksemsmidse_wolk_plaat", SlabBlock::new, () -> onweer());
    public static final DeferredBlock<StairBlock> ONWEERSWOLK_TRAP = BLOCKS.registerBlock("bliksemsmidse_wolk_trap",
            p -> new StairBlock(ONWEERSWOLK.get().defaultBlockState(), p), () -> onweer());

    // --- the smid-guh's own furniture (shapes facing north: tools/features/bio_bouw_wolk2.py VORMEN) ---------------------------
    private static VoxelShape vorm(double[]... dozen) {
        VoxelShape s = Shapes.empty();
        for (double[] d : dozen) {
            s = Shapes.or(s, Block.box(d[0], d[1], d[2], d[3], d[4], d[5]));
        }
        return s;
    }

    private static BlockBehaviour.Properties meubel() {
        return DecoBlock.props().mapColor(MapColor.SNOW).sound(SoundType.WOOL);
    }

    public static final DeferredBlock<DecoBlock> WOLKENTAFEL = BLOCKS.registerBlock("bliksemsmidse_wolkentafel",
            p -> new DecoBlock(p, vorm(new double[]{1, 11, 1, 15, 14, 15}, new double[]{5, 0, 5, 11, 11, 11})), () -> meubel());
    public static final DeferredBlock<DecoBlock> WOLKENPLANK = BLOCKS.registerBlock("bliksemsmidse_wolkenplank",
            p -> new DecoBlock(p, vorm(new double[]{0, 3, 9, 16, 6, 16}, new double[]{0, 11, 9, 16, 14, 16})), () -> meubel());
    public static final DeferredBlock<OnweerswolkjeBlock> ONWEERSWOLKJE = BLOCKS.registerBlock("bliksemsmidse_onweerswolkje",
            p -> new OnweerswolkjeBlock(p, vorm(new double[]{3, 8, 3, 13, 14, 13})), () -> meubel().mapColor(MapColor.COLOR_GRAY).noCollision());

    // --- the pot, the hoard and the crumb -----------------------------------------------------------------------------------
    /** Part of their place: nobody breaks them in survival. */
    private static BlockBehaviour.Properties vast() {
        return BlockBehaviour.Properties.of().strength(-1f, 3600000f).noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK);
    }

    public static final DeferredBlock<PotBlock> POT = BLOCKS.registerBlock("regenboogbrug_pot",
            p -> new PotBlock(p, vorm(new double[]{2, 0, 2, 14, 12, 14})), () -> vast().mapColor(MapColor.COLOR_BLACK).sound(SoundType.METAL));
    public static final DeferredBlock<SchatBlock> SCHAT = BLOCKS.registerBlock("wolkenkasteeltje_schat",
            p -> new SchatBlock(p, vorm(new double[]{0, 0, 0, 16, 9, 16})), () -> vast().mapColor(MapColor.GOLD).sound(SoundType.METAL));
    public static final DeferredBlock<DecoBlock> KRUIMEL = BLOCKS.registerBlock("wolkenkasteeltje_kruimel",
            p -> new DecoBlock(p, vorm(new double[]{5, 0, 5, 11, 5, 11})), () -> DecoBlock.props().mapColor(MapColor.GOLD).sound(SoundType.METAL));

    // --- items --------------------------------------------------------------------------------------------------------------
    public static final DeferredItem<BlockItem> ONWEERSWOLK_ITEM = ITEMS.registerSimpleBlockItem(ONWEERSWOLK);
    public static final DeferredItem<BlockItem> ONWEERSWOLK_PLAAT_ITEM = ITEMS.registerSimpleBlockItem(ONWEERSWOLK_PLAAT);
    public static final DeferredItem<BlockItem> ONWEERSWOLK_TRAP_ITEM = ITEMS.registerSimpleBlockItem(ONWEERSWOLK_TRAP);
    public static final DeferredItem<BlockItem> WOLKENTAFEL_ITEM = ITEMS.registerSimpleBlockItem(WOLKENTAFEL);
    public static final DeferredItem<BlockItem> WOLKENPLANK_ITEM = ITEMS.registerSimpleBlockItem(WOLKENPLANK);
    public static final DeferredItem<LoreBlockItem> ONWEERSWOLKJE_ITEM = ITEMS.registerItem("bliksemsmidse_onweerswolkje",
            p -> new LoreBlockItem(ONWEERSWOLKJE.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
    public static final DeferredItem<LoreBlockItem> POT_ITEM = ITEMS.registerItem("regenboogbrug_pot",
            p -> new LoreBlockItem(POT.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
    public static final DeferredItem<LoreBlockItem> SCHAT_ITEM = ITEMS.registerItem("wolkenkasteeltje_schat",
            p -> new LoreBlockItem(SCHAT.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
    /**
     * The gouden knabbelkruimel: a trophy you can put down, and a rare snack (it makes you fall like a cloud for a while).
     */
    public static final DeferredItem<LoreBlockItem> KRUIMEL_ITEM = ITEMS.registerItem("wolkenkasteeltje_kruimel",
            p -> new LoreBlockItem(KRUIMEL.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix().rarity(Rarity.RARE)
                    .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8f).alwaysEdible().build(),
                            Consumables.defaultFood().onConsume(new ApplyStatusEffectsConsumeEffect(
                                    new MobEffectInstance(MobEffects.SLOW_FALLING, 20 * 45, 0))).build()));

    /** Every block of this slice (the tests walk it). */
    public static List<DeferredBlock<? extends Block>> blokken() {
        return List.of(ONWEERSWOLK, ONWEERSWOLK_PLAAT, ONWEERSWOLK_TRAP, WOLKENTAFEL, WOLKENPLANK, ONWEERSWOLKJE, POT, SCHAT, KRUIMEL);
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        PARTICLES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(REUZENGUH.get(), ReuzenguhEntity.createAttributes().build()));
        NpcRollen.zet(GuhNpcEntity.Kind.SMIDGUH, Smidguh.ROL);
        for (String s : STRUCTUREN) {
            SuperkompasItem.voegToe("wonderen", s);
        }
        NeoForge.EVENT_BUS.register(Bewakers.class);
        NeoForge.EVENT_BUS.addListener(ReuzenguhEntity::onBreek);
        NeoForge.EVENT_BUS.addListener(Commando::registreer);
        Inspectie.registreer();
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<? extends Item> item : List.of(ONWEERSWOLK_ITEM, ONWEERSWOLK_PLAAT_ITEM, ONWEERSWOLK_TRAP_ITEM, WOLKENTAFEL_ITEM, WOLKENPLANK_ITEM,
                ONWEERSWOLKJE_ITEM, KRUIMEL_ITEM, POT_ITEM, SCHAT_ITEM)) {
            output.accept(new ItemStack(item.get()));
        }
    }

    private BouwWolk2Slice() {
    }
}
