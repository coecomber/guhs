package nl.juiced.guhs.feature.bakkerij;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.VerstopBlocks;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.knus.GuhHooks;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
/**
 * De Knabbelbakkerij (2.8, plein slot bakkerij of the Knuffeldal town): a giant kaasknabbel bread with a guh face in
 * the crust and a chimney puffing knabbelwolkjes. Inside Bakker Korstje (NPC BAKKERGUH, {@link BakkerijRole}) runs his
 * order game ({@link BakkerijGame}: customer guhs with order bubbles, dough / shape / topping, oven timing, serve,
 * combos, highscore board {@code bakkerij}), sells things for bakmunten, and bakes the feesttaart for the Grote Knusfeest.
 * Everyone can bake the twelve recipes ({@link Recept}) at a {@link KnabbelovenBlock} from ingredients ({@link Bakken});
 * the receptenboek and milestones are on the Knus tab ({@link BakkerijVoortgang}).
 * Resources: tools/features/bakkerij.py.
 */
public final class BakkerijFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    // --- blocks --------------------------------------------------------------------------------------------------------
    /** The guh oven: bake the twelve recipes from ingredients (and Korstje's game uses the ones in his bakery). */
    public static final DeferredBlock<KnabbelovenBlock> KNABBELOVEN = BLOCKS.registerBlock("knabbeloven", KnabbelovenBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS).mapColor(MapColor.COLOR_PINK).strength(2.0f, 6f).noOcclusion()
                    .lightLevel(s -> s.getValue(KnabbelovenBlock.LIT) ? 11 : 0));
    /** A little chimney pot that puffs knabbelwolkjes (the bakery's chimney; also a deco block). */
    public static final DeferredBlock<SchoorsteenBlock> SCHOORSTEEN = BLOCKS.registerBlock("bakkerij_schoorsteen", SchoorsteenBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS).mapColor(MapColor.COLOR_ORANGE).strength(1.5f, 6f).noOcclusion()
                    .sound(SoundType.STONE));
    /** Invisible markers in the bakery: where customers stand at the counter, and where they come in. */
    public static final DeferredBlock<VerstopBlocks.Marker> KLANTPLEK = marker("bakkerij_klantplek");
    public static final DeferredBlock<VerstopBlocks.Marker> INGANG = marker("bakkerij_ingang");

    private static DeferredBlock<VerstopBlocks.Marker> marker(String name) {
        return BLOCKS.registerBlock(name, VerstopBlocks.Marker::new, () -> BlockBehaviour.Properties.of().noCollision().noLootTable()
                .strength(-1f, 3600000f).noOcclusion().isValidSpawn((s, l, p, e) -> false));
    }

    // --- items ---------------------------------------------------------------------------------------------------------
    /** The coin of the bakery: earned in Korstje's game, spent in his shop (and one play at the grijpmachine). */
    public static final DeferredItem<Item> BAKMUNT = ITEMS.registerSimpleItem("bakmunt", () -> new Item.Properties());
    /** The Grote Knusfeest's cake, in a pink box with a bow (for the Burgemeester, not for eating on the way!). */
    public static final DeferredItem<FeesttaartItem> FEESTTAART = ITEMS.registerItem("feesttaart", FeesttaartItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<BlockItem> KNABBELOVEN_ITEM = ITEMS.registerSimpleBlockItem(KNABBELOVEN);
    public static final DeferredItem<BlockItem> SCHOORSTEEN_ITEM = ITEMS.registerSimpleBlockItem(SCHOORSTEEN);
    /** The twelve pastries (the receptenboek). */
    private static final Map<Recept, DeferredItem<BakjeItem>> BAKJES = new EnumMap<>(Recept.class);

    static {
        for (Recept r : Recept.BOEK) {
            BAKJES.put(r, ITEMS.registerItem(r.id(), p -> new BakjeItem(r, p), () -> eten(r, new Item.Properties())));
        }
    }

    /**
     * What a pastry does for you: food, and a small cute effect (the guh humour is in the lang). 26.1: the effect and the
     * eating speed ({@code fast()} = 0.8 s) live in the CONSUMABLE component next to the food.
     */
    static Item.Properties eten(Recept r, Item.Properties p) {
        return switch (r) {
            case KNABBELBROODJE -> eten(p, 6, 0.7f, false, effect(MobEffects.SPEED, 30, 0));
            case KAASKRAKELING -> eten(p, 5, 0.6f, false, effect(MobEffects.HASTE, 60, 0));
            case VADSVLAAI -> eten(p, 8, 0.8f, false, effect(MobEffects.ABSORPTION, 60, 0));
            case GUHCROISSANT -> eten(p, 5, 0.6f, false, effect(MobEffects.JUMP_BOOST, 40, 0));
            case KNABBELKOEKJE -> eten(p, 3, 0.4f, true, effect(MobEffects.SPEED, 15, 1));
            case KAASBOLLETJE -> eten(p, 6, 0.7f, false, effect(MobEffects.REGENERATION, 8, 0));
            case PLUISMUFFIN -> eten(p, 5, 0.5f, false, effect(MobEffects.SLOW_FALLING, 30, 0));
            case THEETAARTJE -> eten(p, 4, 0.6f, false, effect(MobEffects.LUCK, 120, 0));
            case KNABBELTOMPOUCE -> eten(p, 6, 0.6f, false, effect(MobEffects.RESISTANCE, 30, 0));
            case VADSDONUT -> eten(p, 5, 0.5f, false, effect(MobEffects.JUMP_BOOST, 20, 1));
            case GUHWAFEL -> eten(p, 5, 0.6f, false, effect(MobEffects.WATER_BREATHING, 60, 0));
            case STERRENKOEKJE -> eten(p, 3, 0.4f, true, effect(MobEffects.NIGHT_VISION, 90, 0));
            default -> p.food(new FoodProperties.Builder().nutrition(4).saturationModifier(0.5f).build());
        };
    }

    private static Item.Properties eten(Item.Properties p, int nutrition, float saturation, boolean fast, MobEffectInstance effect) {
        return p.food(new FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation).build(),
                Consumables.defaultFood().consumeSeconds(fast ? 0.8f : 1.6f)
                        .onConsume(new ApplyStatusEffectsConsumeEffect(effect, 1f)).build());
    }

    private static MobEffectInstance effect(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int seconds, int level) {
        return new MobEffectInstance(effect, seconds * 20, level);
    }

    /** The item of one of the twelve recipes (the feesttaart for {@link Recept#FEESTTAART}). */
    public static Item bakje(Recept r) {
        if (r == Recept.ROZE_GUH_KOEK) {
            return nl.juiced.guhs.feature.piep.PiepFeature.ROZE_GUH_KOEK_ITEM.get();   // 2.8.1 Piep
        }
        return r == Recept.FEESTTAART ? FEESTTAART.get() : BAKJES.get(r).get();
    }

    // --- the customers --------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<BakkerijKlant>> KLANT = ENTITY_TYPES.register("bakkerij_klant",
            () -> EntityType.Builder.of(BakkerijKlant::new, MobCategory.MISC).sized(0.9f, 0.9f).clientTrackingRange(8).updateInterval(2)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.parse("guhs:bakkerij_klant"))));

    // --- particles and sounds -------------------------------------------------------------------------------------------
    /** A little cloud shaped like a kaasknabbel: the bakery's chimney puffs them. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> KNABBELWOLKJE = PARTICLES.register("knabbelwolkje",
            () -> new SimpleParticleType(false));
    /** A pinch of flour dust. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> MEELSTOFJE = PARTICLES.register("meelstofje",
            () -> new SimpleParticleType(false));
    public static final DeferredHolder<SoundEvent, SoundEvent> OVEN_DING = sound("bakkerij.oven_ding");
    public static final DeferredHolder<SoundEvent, SoundEvent> BESTELLING = sound("bakkerij.bestelling");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    private static final NpcRole ROLE = new BakkerijRole();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(KLANT.get(), BakkerijKlant.createAttributes().build()));
        Minigames.registerGame(Minigames.BAKKERIJ, BakkerijGame::isPlaying);
        NeoForge.EVENT_BUS.addListener(BakkerijGame::onDamage);
        NeoForge.EVENT_BUS.addListener(BakkerijGame::onDeath);
        NeoForge.EVENT_BUS.addListener(BakkerijGame::onLogout);
        NeoForge.EVENT_BUS.addListener(BakkerijGame::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(BakkerijGame::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(BakkerijGame::onServerStopped);
        NeoForge.EVENT_BUS.addListener(Bakken::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Bakken::onLogout);
        GuhHooks.klik(BakkerijEvents::guhEet);
        BakkerijVoortgang.register();
    }

    public static void payloads(PayloadRegistrar registrar) {
        BakkerijPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KNABBELOVEN.get()));
        output.accept(new ItemStack(SCHOORSTEEN.get()));
        output.accept(new ItemStack(BAKMUNT.get()));
        for (Recept r : Recept.BOEK) {
            output.accept(new ItemStack(bakje(r)));
        }
        output.accept(new ItemStack(FEESTTAART.get()));
    }

    /** Bakker Korstje (BAKKERGUH). */
    @Nullable
    public static NpcRole role() {
        return ROLE;
    }

    static Supplier<ItemStack> stack(Supplier<? extends net.minecraft.world.level.ItemLike> item, int count) {
        return () -> new ItemStack(item.get(), count);
    }

    private BakkerijFeature() {
    }
}
