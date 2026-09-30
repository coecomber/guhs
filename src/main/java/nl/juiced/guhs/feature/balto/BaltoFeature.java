package nl.juiced.guhs.feature.balto;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
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
import nl.juiced.guhs.feature.baltoslee.SleeTocht;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.VariantGedragen;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;

/**
 * 3.0 (Guhverhalen), slice balto: Baltoguh en Nomguh (DESIGN_30 §2). Resources: tools/features/balto.py (+ balto_*.py).
 * <ul>
 *   <li>The Sneeuwguhtoendra's content: the sneeuwguhspar (a spruce-like guh tree with a sleepy face), snow drifts, snowy
 *       boulders ({@link BaltoWorldgen}), its blizzard look (client {@code Sneeuwstorm}).</li>
 *   <li>{@link Nomguh}: the snowy guh town (one per tundra, structure {@code guhs:nomguh}): houses with snow roofs, the
 *       ziekenhuisje with the sick guh babies and Rosy, the sled-dog stable, Baltoguh and Boris' old boat on the frozen bay,
 *       Muk &amp; Luk's igloo, and the marked trek route (storm valley, ice bridge, avalanche slope, rest points) up to the
 *       berghut with the medicine chest. Protected like the other guh buildings ({@link NomguhProtection}).</li>
 *   <li>The characters ({@link BaltoRollen}): Boris (a real goose), Steele-Mika (the boastful sled champion), Muk and Luk
 *       (two polar-bear guhs), Rosy (the sick baby guh) and the white wolf-guh (only at the dieptepunt).</li>
 *   <li>The questline ({@link BaltoVerhaal}): the scenes, the medicine trek (balto-slee's {@link SleeTocht}), the wolf moment
 *       and the quote, the rewards: Baltoguh (once per player), the own sneeuwslee, the Baltoguh-beeldje, the title "Held van
 *       Nomguh" and four outfits (source {@code nomguh}).</li>
 *   <li>The Baltoguh variant ({@link BaltoGedrag}): fast in the snow, and sniffs the way to your last huisje or spawn.</li>
 * </ul>
 */
public final class BaltoFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    /** The structure guhs:nomguh (one per Sneeuwguhtoendra, see tools/features/balto_bouw.py). */
    public static final ResourceKey<Structure> NOMGUH = ResourceKey.create(Registries.STRUCTURE, Guhs.id("nomguh"));
    /** The KledingBronnen source of the four outfits. */
    public static final String BRON = "nomguh";
    /** The random salt of the slice (CONTRACT_30 §2). */
    public static final long SALT = 20300701L;

    // --- blocks ---------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<BaltoBlocks.Beeldje> BALTOGUH_BEELDJE = BLOCKS.registerBlock("baltoguh_beeldje", BaltoBlocks.Beeldje::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_BLOCK).mapColor(MapColor.COLOR_GRAY).strength(2f, 6f).noOcclusion()
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<BaltoBlocks.Routepaal> ROUTEPAAL = BLOCKS.registerBlock("nomguh_routepaal", BaltoBlocks.Routepaal::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(1f).sound(SoundType.WOOD).noOcclusion().lightLevel(s -> 10));
    public static final DeferredBlock<Block> SNEEUWSPOOR = BLOCKS.registerSimpleBlock("nomguh_sneeuwspoor",
            BlockBehaviour.Properties.ofFullCopy(Blocks.SNOW_BLOCK).mapColor(MapColor.SNOW).strength(0.3f));
    public static final DeferredBlock<Block> SNEEUWDAK = BLOCKS.registerSimpleBlock("nomguh_sneeuwdak",
            BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_PLANKS).mapColor(MapColor.SNOW).sound(SoundType.SNOW).strength(1.5f));
    public static final DeferredBlock<StairBlock> SNEEUWDAK_TRAP = BLOCKS.registerBlock("nomguh_sneeuwdak_trap",
            p -> new StairBlock(SNEEUWDAK.get().defaultBlockState(), p), BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_STAIRS)
                    .mapColor(MapColor.SNOW).sound(SoundType.SNOW).strength(1.5f));
    public static final DeferredBlock<SlabBlock> SNEEUWDAK_PLAAT = BLOCKS.registerBlock("nomguh_sneeuwdak_plaat", SlabBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_SLAB).mapColor(MapColor.SNOW).sound(SoundType.SNOW).strength(1.5f));
    public static final DeferredBlock<BaltoBlocks.Medicijnkist> MEDICIJNKIST = BLOCKS.registerBlock("nomguh_medicijnkist", BaltoBlocks.Medicijnkist::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_PLANKS).strength(1f).noOcclusion());
    public static final DeferredBlock<RotatedPillarBlock> SPAR_STAM = BLOCKS.registerBlock("sneeuwguhspar_stam", RotatedPillarBlock::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LOG).mapColor(MapColor.PODZOL));
    public static final DeferredBlock<BaltoBlocks.SparGezicht> SPAR_GEZICHT = BLOCKS.registerBlock("sneeuwguhspar_gezicht", BaltoBlocks.SparGezicht::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LOG).mapColor(MapColor.PODZOL));
    public static final DeferredBlock<BaltoBlocks.SparNaalden> SPAR_NAALDEN = BLOCKS.registerBlock("sneeuwguhspar_naalden", BaltoBlocks.SparNaalden::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_LEAVES).mapColor(MapColor.COLOR_CYAN));
    public static final DeferredBlock<BaltoBlocks.SparZaailing> SPAR_ZAAILING = BLOCKS.registerBlock("sneeuwguhspar_zaailing", BaltoBlocks.SparZaailing::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_SAPLING));

    // --- items ------------------------------------------------------------------------------------------------------------
    public static final DeferredItem<BaltoBlocks.LoreBlock> BALTOGUH_BEELDJE_ITEM = ITEMS.registerItem("baltoguh_beeldje",
            p -> new BaltoBlocks.LoreBlock(BALTOGUH_BEELDJE.get(), p), new Item.Properties().rarity(Rarity.RARE));
    public static final DeferredItem<BaltoBlocks.LoreBlock> MEDICIJNKIST_ITEM = ITEMS.registerItem("nomguh_medicijnkist",
            p -> new BaltoBlocks.LoreBlock(MEDICIJNKIST.get(), p), new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    static {
        for (DeferredBlock<?> b : List.of(SNEEUWSPOOR, SNEEUWDAK, SNEEUWDAK_TRAP, SNEEUWDAK_PLAAT, SPAR_STAM, SPAR_GEZICHT, SPAR_NAALDEN)) {
            ITEMS.registerSimpleBlockItem(b);
        }
        for (DeferredBlock<?> b : List.of(ROUTEPAAL, SPAR_ZAAILING)) {
            ITEMS.registerItem(b.getId().getPath(), p -> new BaltoBlocks.LoreBlock(b.get(), p));
        }
    }

    // --- worldgen -----------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<Feature<?>, BaltoWorldgen.Sneeuwguhspar> SPAR_FEATURE = FEATURES.register("balto_sneeuwguhspar",
            () -> new BaltoWorldgen.Sneeuwguhspar(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, BaltoWorldgen.Sneeuwduin> DUIN_FEATURE = FEATURES.register("balto_sneeuwduin",
            () -> new BaltoWorldgen.Sneeuwduin(NoneFeatureConfiguration.CODEC));
    public static final DeferredHolder<Feature<?>, BaltoWorldgen.Sneeuwkei> KEI_FEATURE = FEATURES.register("balto_sneeuwkei",
            () -> new BaltoWorldgen.Sneeuwkei(NoneFeatureConfiguration.CODEC));

    // --- particles and sounds -------------------------------------------------------------------------------------------------
    /** The Baltoguh's scent trail: little pink-white paw prints that light up one after another towards home. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SNUFFEL = PARTICLES.register("balto_snuffel", () -> new SimpleParticleType(true));
    /** A soft white-blue sparkle (the white wolf-guh, the route lamps, the beeldje). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WOLFGLANS = PARTICLES.register("balto_wolfglans", () -> new SimpleParticleType(false));
    public static final DeferredHolder<SoundEvent, SoundEvent> HUIL = sound("balto.huil");
    public static final DeferredHolder<SoundEvent, SoundEvent> GAK = sound("balto.gak");
    public static final DeferredHolder<SoundEvent, SoundEvent> BELLETJES = sound("balto.belletjes");
    public static final DeferredHolder<SoundEvent, SoundEvent> HATSJOE = sound("balto.hatsjoe");
    public static final DeferredHolder<SoundEvent, SoundEvent> SNUIF = sound("balto.snuffel");
    /** A gust of the snow storm (client Sneeuwstorm). */
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND = sound("balto.wind");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    /** The four outfits of the questline (their one source: "nomguh"). */
    public static final List<GuhClothes> KLEDING = List.of(GuhClothes.BALTO_SJAALTJE, GuhClothes.BALTO_WOLFSOORTJES, GuhClothes.BALTO_SNEEUWMUTS,
            GuhClothes.BALTO_WANTJES);

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        FEATURES.register(modBus);
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener(BaltoFeature::setup);
        NeoForge.EVENT_BUS.register(BaltoEvents.class);
        NeoForge.EVENT_BUS.register(NomguhProtection.class);
        Protected.add(NomguhProtection::protectedAt);
        for (GuhClothes c : KLEDING) {
            KledingBronnen.bron(c, BRON);
        }
        // the characters (Steele-Mika's plek "sledesprint" is balto-slee's race; every other Steele-Mika boasts here)
        NpcRollen.zet(GuhNpcEntity.Kind.BORIS, BaltoRollen.BORIS);
        NpcRollen.zet(GuhNpcEntity.Kind.STEELE_MIKA, BaltoRollen.STEELE);
        NpcRollen.zet(GuhNpcEntity.Kind.MUK, BaltoRollen.MUK);
        NpcRollen.zet(GuhNpcEntity.Kind.LUK, BaltoRollen.LUK);
        NpcRollen.zet(GuhNpcEntity.Kind.ROSY, BaltoRollen.ROSY);
        NpcRollen.zet(GuhNpcEntity.Kind.WITTE_WOLFGUH, BaltoRollen.WITTE_WOLF);
        // the story: Baltoguh's copy, the scenes, the medicine trek
        VerhaalGuhs.opKlik(VerhaalGuh.BALTOGUH, BaltoVerhaal::klikBalto);
        VariantGedragen.zet(GuhVariant.BALTOGUH, new BaltoGedrag());
        SleeTocht.luister(BaltoVerhaal::opMoment);
        BaltoVerhaal.luisteraars();
        nl.juiced.guhs.feature.knus.GuhHooks.tick(BaltoEvents::guhTick);
    }

    private static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            FireBlock fire = (FireBlock) Blocks.FIRE;
            fire.setFlammable(SPAR_STAM.get(), 5, 5);
            fire.setFlammable(SPAR_GEZICHT.get(), 5, 5);
            fire.setFlammable(SPAR_NAALDEN.get(), 30, 60);
        });
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(BALTOGUH_BEELDJE_ITEM.get()));
        output.accept(new ItemStack(MEDICIJNKIST_ITEM.get()));
        for (DeferredBlock<?> b : List.of(ROUTEPAAL, SNEEUWSPOOR, SNEEUWDAK, SNEEUWDAK_TRAP, SNEEUWDAK_PLAAT, SPAR_STAM, SPAR_GEZICHT, SPAR_NAALDEN,
                SPAR_ZAAILING)) {
            output.accept(new ItemStack(b.get()));
        }
    }

    private BaltoFeature() {
    }
}
