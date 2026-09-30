package nl.juiced.guhs.feature.hemel;

import java.util.function.Consumer;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.feature.band.Wolkjes;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.verhaal.NpcRollen;

/**
 * 3.0 (Guhverhalen), slice hemel: Het Hemelkapelletje en het Knuffelhart (DESIGN_30 §4).
 * <ul>
 *   <li>the structure {@code guhs:hemelkapelletje}: a little cloud chapel on a floating islet above a cloud plaza with two
 *       wolkenliften (a bit rarer than the other story buildings; protected by {@link HemelProtection});</li>
 *   <li>the {@link KnuffelhartBlock Knuffelhart}: a glowing pink heart under a glass dome, only in the chapel (not craftable,
 *       not minable, no item). It sleeps until the player did the wolkenhoeder's questline; then it beats (for that player)
 *       and brings their tamed guhs back from the wolkjes: free and unlimited ({@link Hemel#terug});</li>
 *   <li>the wolkenhoeder (NPC WOLKENHOEDER, {@link Wolkenhoeder}): asks a guhkristal, a gouden kaasknabbel and a pluisveertje
 *       ({@link HemelQuest}), and gives the gouden aureooltje and the wolkenvleugeltjes (clothes, source "hemel");</li>
 *   <li>the revive screen (payloads {@link HemelPayloads}): all your dead tamed guhs (name, variant, hearts level, clothes);</li>
 *   <li>the {@link Herinnering} star a tamed guh leaves when it goes to the wolkjes (glowing, a tooltip, a hug, and at the
 *       Knuffelhart it brings exactly that guh back);</li>
 *   <li>after coming back a guh sparkles for a minute (KnusVlag GLANS, {@link HemelEvents#glans}).</li>
 * </ul>
 * Resources: tools/features/hemel.py (+ hemel_bouw.py, hemel_npc.py, hemel_geluid.py).
 */
public final class HemelFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    /** The chapel (structure id). */
    public static final ResourceKey<Structure> KAPELLETJE = ResourceKey.create(Registries.STRUCTURE, Guhs.id("hemelkapelletje"));

    /**
     * The glowing star "Herinnering aan &lt;naam&gt;" a tamed guh leaves when it goes to the wolkjes (band.Wolkjes drops it via
     * {@link Herinnering#maak}).
     */
    public static final DeferredItem<Item> HERINNERING = ITEMS.registerItem("herinnering", Herinnering::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant());

    /** The Knuffelhart: unbreakable (like bedrock), explosion proof, can't be pushed, glows, no loot, no item. */
    public static final DeferredBlock<KnuffelhartBlock> KNUFFELHART = BLOCKS.registerBlock("knuffelhart", KnuffelhartBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(-1f, 3600000f).noLootTable().noOcclusion()
                    .lightLevel(s -> 13).sound(SoundType.AMETHYST).pushReaction(PushReaction.BLOCK).isValidSpawn((s, l, p, e) -> false)
                    .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KnuffelhartBlockEntity>> KNUFFELHART_BE = BLOCK_ENTITY_TYPES.register(
            "knuffelhart", () -> BlockEntityType.Builder.of(KnuffelhartBlockEntity::new, KNUFFELHART.get()).build(null));

    /** A tiny twinkly star (the heart's sparkle, a guh's glans after coming back). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STERRETJE = PARTICLES.register("hemel_sterretje",
            () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> MUZIEK = sound("hemel.muziek");
    public static final DeferredHolder<SoundEvent, SoundEvent> HARTKLOP = sound("hemel.hartklop");
    public static final DeferredHolder<SoundEvent, SoundEvent> TERUG = sound("hemel.terug");
    public static final DeferredHolder<SoundEvent, SoundEvent> STER = sound("hemel.ster");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    /** The clothing source of the two pieces (KledingBronnen.BEKEND, lang gui.guhs.kledingbron.hemel). */
    public static final String BRON = "hemel";

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        KledingBronnen.bron(GuhClothes.HEMEL_AUREOOLTJE, BRON);
        KledingBronnen.bron(GuhClothes.HEMEL_WOLKENVLEUGELTJES, BRON);
        NpcRollen.zet(GuhNpcEntity.Kind.WOLKENHOEDER, new Wolkenhoeder());
        Wolkenhoeder.luisteraars();
        NeoForge.EVENT_BUS.register(HemelProtection.class);
        NeoForge.EVENT_BUS.register(HemelEvents.class);
        NeoForge.EVENT_BUS.register(HemelCommando.class);
        Protected.add(HemelProtection::protectedAt);
        GuhHooks.tick(HemelEvents::glans);
        Wolkjes.opDood(HemelEvents::naarDeWolkjes);
    }

    public static void payloads(PayloadRegistrar registrar) {
        HemelPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        // the Herinnering only comes from a guh (it would be an empty star); the Knuffelhart has no item
    }

    private HemelFeature() {
    }
}
