package nl.juiced.guhs.feature.baltoslee;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.GuhFurnitureBlock;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.verhaal.NpcRollen;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
/**
 * 3.0 (Guhverhalen), slice baltoslee: De sneeuwslee (DESIGN_30 §2).
 * <ul>
 *   <li>The medicine ride ({@link SleeTocht}, started by balto's questline) and the sledesprint against Steele-Mika
 *   ({@link SteeleSprint}, plek "sledesprint"): a steered sled ({@link SleeEntity}, {@link SleeRit}, {@link SleeRijden}) over the
 *   Nomguh route ({@link NomguhRoute}, {@link RitRoute}, {@link SleeBaan}) through a storm with gusts, ice bridges, avalanches
 *   and vuurkorf rest points.</li>
 *   <li>Your own sneeuwslee ({@link SneeuwsleeEntity}, {@link SneeuwsleeItem}) with four guh-sledehondjes, for the snowy
 *   biomes.</li>
 *   <li>The sledebelletje (the race's coin) and Steele-Mika's winter deco shop; the op command {@code /guhs baltoslee}.</li>
 * </ul>
 * Resources: tools/features/balto_slee.py (+ balto_slee_*.py; the sounds: balto_slee_geluid.py).
 */
public final class BaltoSleeFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);

    // --- items -----------------------------------------------------------------------------------------------------------
    /** Your own snow sled (balto gives it as the quest reward): put it down on snow and ride. */
    public static final DeferredItem<Item> SNEEUWSLEE = ITEMS.register("sneeuwslee",
            () -> new SneeuwsleeItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));
    /** The coin of the sledesprint: a little golden sled bell with a red ribbon. */
    public static final DeferredItem<Item> SLEDEBELLETJE = ITEMS.registerSimpleItem("sledebelletje", () -> new Item.Properties().rarity(Rarity.UNCOMMON));

    // --- Steele-Mika's winter deco (for sledebelletjes) -----------------------------------------------------------------------
    private static BlockBehaviour.Properties deco(MapColor kleur, SoundType geluid) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(0.8f).sound(geluid).noOcclusion();
    }

    public static final DeferredBlock<GuhFurnitureBlock> SNEEUWGUH = BLOCKS.registerBlock("baltoslee_sneeuwguh",
            p -> new GuhFurnitureBlock(p, -1, new double[]{2.5, 0, 2.5, 13.5, 9, 13.5}, new double[]{4, 9, 4, 12, 16, 12}),
            () -> deco(MapColor.SNOW, SoundType.SNOW));
    public static final DeferredBlock<GuhFurnitureBlock> MINISLEE = BLOCKS.registerBlock("baltoslee_minislee",
            p -> new GuhFurnitureBlock(p, 0.32, new double[]{1, 0, 0, 15, 6, 16}), () -> deco(MapColor.WOOD, SoundType.WOOD));
    public static final DeferredBlock<Sledebellen> SLEDEBELLEN = BLOCKS.registerBlock("baltoslee_sledebellen",
            p -> new Sledebellen(p, new double[]{1, 0, 6, 15, 15, 10}), () -> deco(MapColor.GOLD, SoundType.WOOD));
    public static final DeferredBlock<GuhFurnitureBlock> BEKER = BLOCKS.registerBlock("baltoslee_beker",
            p -> new GuhFurnitureBlock(p, -1, new double[]{4, 0, 4, 12, 13, 12}), () -> deco(MapColor.GOLD, SoundType.METAL).lightLevel(s -> 4));
    public static final DeferredBlock<GuhFurnitureBlock> HONDENMAND = BLOCKS.registerBlock("baltoslee_hondenmand",
            p -> new GuhFurnitureBlock(p, -1, new double[]{1, 0, 1, 15, 6, 15}), () -> deco(MapColor.COLOR_RED, SoundType.WOOL));
    public static final DeferredBlock<GuhFurnitureBlock> LANTAARNPAAL = BLOCKS.registerBlock("baltoslee_lantaarnpaal",
            p -> new GuhFurnitureBlock(p, -1, new double[]{6.5, 0, 6.5, 9.5, 11, 9.5}, new double[]{4.5, 11, 4.5, 11.5, 16, 11.5}),
            () -> deco(MapColor.WOOD, SoundType.LANTERN).lightLevel(s -> 14));

    public static final DeferredItem<BlockItem> SNEEUWGUH_ITEM = ITEMS.registerSimpleBlockItem(SNEEUWGUH);
    public static final DeferredItem<BlockItem> MINISLEE_ITEM = ITEMS.registerSimpleBlockItem(MINISLEE);
    public static final DeferredItem<BlockItem> SLEDEBELLEN_ITEM = ITEMS.registerSimpleBlockItem(SLEDEBELLEN);
    public static final DeferredItem<BlockItem> BEKER_ITEM = ITEMS.registerSimpleBlockItem("baltoslee_beker", BEKER, () -> new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<BlockItem> HONDENMAND_ITEM = ITEMS.registerSimpleBlockItem(HONDENMAND);
    public static final DeferredItem<BlockItem> LANTAARNPAAL_ITEM = ITEMS.registerSimpleBlockItem(LANTAARNPAAL);

    // --- entities ------------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<SleeEntity>> SLEE = ENTITY_TYPES.register("baltoslee_slee",
            () -> EntityType.Builder.<SleeEntity>of(SleeEntity::new, MobCategory.MISC).sized(1.2f, 0.9f).clientTrackingRange(10)
                    .updateInterval(1).noSave().noSummon().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("baltoslee_slee"))));
    public static final DeferredHolder<EntityType<?>, EntityType<SneeuwsleeEntity>> SNEEUWSLEE_ENTITY = ENTITY_TYPES.register("sneeuwslee",
            () -> EntityType.Builder.<SneeuwsleeEntity>of(SneeuwsleeEntity::new, MobCategory.MISC).sized(1.2f, 0.8f).clientTrackingRange(10)
                    .updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("sneeuwslee"))));
    /** Only drawn in front of the sleds (never in the world). */
    public static final DeferredHolder<EntityType<?>, EntityType<SledehondjeEntity>> SLEDEHONDJE = ENTITY_TYPES.register("baltoslee_sledehondje",
            () -> EntityType.Builder.<SledehondjeEntity>of(SledehondjeEntity::new, MobCategory.MISC).sized(0.5f, 0.6f).clientTrackingRange(4)
                    .noSave().noSummon().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("baltoslee_sledehondje"))));

    // --- sounds, particles ------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> GLIJDEN = sound("baltoslee.glijden");
    public static final DeferredHolder<SoundEvent, SoundEvent> BELLEN = sound("baltoslee.bellen");
    public static final DeferredHolder<SoundEvent, SoundEvent> WOEF = sound("baltoslee.woef");
    public static final DeferredHolder<SoundEvent, SoundEvent> WINDVLAAG = sound("baltoslee.windvlaag");
    public static final DeferredHolder<SoundEvent, SoundEvent> LAWINE = sound("baltoslee.lawine");
    public static final DeferredHolder<SoundEvent, SoundEvent> PLOF = sound("baltoslee.plof");
    public static final DeferredHolder<SoundEvent, SoundEvent> IJS = sound("baltoslee.ijs");
    public static final DeferredHolder<SoundEvent, SoundEvent> VUURKORF = sound("baltoslee.vuurkorf");
    public static final DeferredHolder<SoundEvent, SoundEvent> FANFARE = sound("baltoslee.fanfare");
    /** Baltoguh's nose: little glowing sparkles along the middle of the track, ahead of the sled (you see them in the storm). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SNUFFEL = PARTICLES.register("baltoslee_snuffel", () -> new SimpleParticleType(true));

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    /** The bell arch: ring-a-ling when you tap it. */
    public static class Sledebellen extends GuhFurnitureBlock {
        public Sledebellen(Properties properties, double[]... boxes) {
            super(properties, -1, boxes);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            level.playSound(player, pos, BELLEN.get(), SoundSource.BLOCKS, 0.8f, 0.9f + level.getRandom().nextFloat() * 0.3f);
            return InteractionResult.SUCCESS;
        }
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        PARTICLES.register(modBus);
        Minigames.registerGame(SleeRit.GAME_TOCHT, p -> SleeRit.rijdt(p, SleeRit.Modus.TOCHT));
        Minigames.registerGame(SleeRit.GAME_SPRINT, p -> SleeRit.rijdt(p, SleeRit.Modus.SPRINT));
        NpcRollen.zet(GuhNpcEntity.Kind.STEELE_MIKA, SteeleSprint.PLEK, new SteeleSprint());
        NeoForge.EVENT_BUS.addListener((LivingIncomingDamageEvent event) -> SleeRit.opSchade(event));
        NeoForge.EVENT_BUS.addListener((EntityMountEvent event) -> SleeRit.opAfstappen(event));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                SleeRit.spelerWeg(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                SleeRit.spelerWeg(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                SleeRit.spelerWeg(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                SleeRit.controleer(player);
            }
        });
        // a Baltoguh copy hidden by a ride that never ended (the server stopped): it comes back
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            if (!event.getLevel().isClientSide() && event.getEntity() instanceof GuhEntity guh && guh.getPersistentData().getBooleanOr(SleeRit.VERSTOPT, false)
                    && !SleeRit.trektNog(guh.getUUID())) {
                SleeRit.verstop(guh, false);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> SleeRit.vergeetAlles());
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> BaltoSleeCommando.register(event));
    }

    public static void payloads(PayloadRegistrar registrar) {
        BaltoSleePayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        for (var item : List.of(SNEEUWSLEE, SLEDEBELLETJE, SLEDEBELLEN_ITEM, LANTAARNPAAL_ITEM, SNEEUWGUH_ITEM, HONDENMAND_ITEM, MINISLEE_ITEM, BEKER_ITEM)) {
            output.accept(new ItemStack(item.get()));
        }
    }

    private BaltoSleeFeature() {
    }
}
