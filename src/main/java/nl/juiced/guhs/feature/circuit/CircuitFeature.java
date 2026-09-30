package nl.juiced.guhs.feature.circuit;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.race.RaceBaan;
import nl.juiced.guhs.feature.race.RaceGame;
import nl.juiced.guhs.feature.race.RaceProtection;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
/**
 * Het Guh-Circuit (2.9, De Grote Guhspelen): one big circuit in the Guhvelden and the Kaasvlakte with three race tracks
 * around a Pitpaleis with two grandstands, and Coach Vahoegvroem (CIRCUITGUH) who lends you a race guh on the track and
 * level you choose. The races run on the race engine of the old racebaan (feature/race: RaceGame, RaceBaan) with the
 * circuit's extras (CircuitExtra): Mika-pikkers, rolling kaasknabbels, the Vadslooping, the rainbow trail.
 * <ul>
 *     <li>Regenboogbaan: a floating rainbow road with jumps and rainbow boost rings.</li>
 *     <li>Vadsbaan: slippery kaassaus, the Vadslooping and bouncy stuiterpaddenstoelen.</li>
 *     <li>Kaasbergbaan: up the Knabbelhelling (dodge the rolling kaasknabbels!), over the icy top and down again.</li>
 * </ul>
 * Coin: circuitbeker; the outfit: circuit helmpje, racepak and the chequered vlagcape. Resources: tools/features/circuit.py.
 */
public final class CircuitFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    /** The rainbow road (7 colours), its slab. */
    public static final DeferredBlock<CircuitBlocks.Regenboogweg> REGENBOOGWEG = BLOCKS.registerBlock("circuit_regenboogweg",
            CircuitBlocks.Regenboogweg::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5f, 6f).lightLevel(s -> 7)
                    .sound(SoundType.AMETHYST).isValidSpawn((s, l, p, e) -> false));
    public static final DeferredBlock<RegenboogPlaat> REGENBOOGWEG_PLAAT = BLOCKS.registerBlock("circuit_regenboogweg_plaat",
            RegenboogPlaat::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5f, 6f).lightLevel(s -> 7)
                    .sound(SoundType.AMETHYST).isValidSpawn((s, l, p, e) -> false));
    /** The cheese road of the Vadsbaan and the Kaasberg, its slab. */
    public static final DeferredBlock<Block> KAASWEG = BLOCKS.registerSimpleBlock("circuit_kaasweg",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.5f, 6f).sound(SoundType.MUD_BRICKS));
    public static final DeferredBlock<SlabBlock> KAASWEG_PLAAT = BLOCKS.registerBlock("circuit_kaasweg_plaat", SlabBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(1.5f, 6f).sound(SoundType.MUD_BRICKS));
    /** Slippery kaassaus on the road (a race guh slides: the Vadsbaan's hairpins). */
    public static final DeferredBlock<Block> KAASSAUS = BLOCKS.registerSimpleBlock("circuit_kaassaus",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(1f).friction(0.989f).sound(SoundType.HONEY_BLOCK));
    /** Glittering ice on the top of the Kaasberg (slippery too; it never melts). */
    public static final DeferredBlock<Block> BERGIJS = BLOCKS.registerSimpleBlock("circuit_bergijs",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(1f).friction(0.98f).lightLevel(s -> 4).sound(SoundType.GLASS));
    /** The shimmering skin of a rainbow boost ring (run through it: VAHOEG!). */
    public static final DeferredBlock<CircuitBlocks.Boostring> BOOSTRING = BLOCKS.registerBlock("circuit_boostring", CircuitBlocks.Boostring::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).noCollission().noOcclusion().strength(-1f, 3600000f).noLootTable()
                    .lightLevel(s -> 13).sound(SoundType.AMETHYST).isValidSpawn((s, l, p, e) -> false).pushReaction(PushReaction.BLOCK));
    /** The bouncy guh mushroom cap. */
    public static final DeferredBlock<CircuitBlocks.Stuiterpaddenstoel> STUITERPADDENSTOEL = BLOCKS.registerBlock("circuit_stuiterpaddenstoel",
            CircuitBlocks.Stuiterpaddenstoel::new, () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.8f)
                    .sound(SoundType.SLIME_BLOCK).isValidSpawn((s, l, p, e) -> false));
    /** The invisible markers: Mika-pikker spots, rolling-knabbel spots, the looping's entrance. */
    public static final DeferredBlock<CircuitBlocks.Marker> MIKAPLEK = marker("circuit_mikaplek");
    public static final DeferredBlock<CircuitBlocks.Marker> ROLPLEK = marker("circuit_rolplek");
    public static final DeferredBlock<CircuitBlocks.Marker> LOOPING = marker("circuit_looping");

    /** The circuit's own money: won by racing, spent on the circuit outfit. */
    public static final DeferredItem<Item> CIRCUITBEKER = ITEMS.registerSimpleItem("circuitbeker", () -> new Item.Properties());
    public static final DeferredItem<BlockItem> REGENBOOGWEG_ITEM = ITEMS.registerSimpleBlockItem(REGENBOOGWEG);
    public static final DeferredItem<BlockItem> REGENBOOGWEG_PLAAT_ITEM = ITEMS.registerSimpleBlockItem(REGENBOOGWEG_PLAAT);
    public static final DeferredItem<BlockItem> KAASWEG_ITEM = ITEMS.registerSimpleBlockItem(KAASWEG);
    public static final DeferredItem<BlockItem> KAASWEG_PLAAT_ITEM = ITEMS.registerSimpleBlockItem(KAASWEG_PLAAT);
    public static final DeferredItem<BlockItem> KAASSAUS_ITEM = ITEMS.registerSimpleBlockItem(KAASSAUS);
    public static final DeferredItem<BlockItem> BERGIJS_ITEM = ITEMS.registerSimpleBlockItem(BERGIJS);
    public static final DeferredItem<BlockItem> BOOSTRING_ITEM = ITEMS.registerSimpleBlockItem(BOOSTRING);
    public static final DeferredItem<BlockItem> STUITERPADDENSTOEL_ITEM = ITEMS.registerSimpleBlockItem(STUITERPADDENSTOEL);

    /** The Mika-pikkers and the pushers of the Knabbelhelling (never saved). */
    public static final DeferredHolder<EntityType<?>, EntityType<MikaPikkerEntity>> MIKAPIKKER = ENTITY_TYPES.register("circuit_mikapikker",
            () -> EntityType.Builder.of(MikaPikkerEntity::new, MobCategory.MISC).sized(0.7f, 0.65f).eyeHeight(0.45f).clientTrackingRange(10)
                    .noSave().noSummon().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("circuit_mikapikker"))));
    /** A rolling kaasknabbel (never saved). */
    public static final DeferredHolder<EntityType<?>, EntityType<RolknabbelEntity>> ROLKNABBEL = ENTITY_TYPES.register("circuit_rolknabbel",
            () -> EntityType.Builder.<RolknabbelEntity>of(RolknabbelEntity::new, MobCategory.MISC).sized(1.2f, 1.2f).clientTrackingRange(10)
                    .updateInterval(1).noSave().noSummon().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("circuit_rolknabbel"))));

    private static final NpcRole ROLE = new CircuitRole();

    private static DeferredBlock<CircuitBlocks.Marker> marker(String name) {
        return BLOCKS.registerBlock(name, CircuitBlocks.Marker::new, BlockBehaviour.Properties.of().noCollission().noLootTable()
                .strength(-1f, 3600000f).noOcclusion().isValidSpawn((s, l, p, e) -> false).pushReaction(PushReaction.BLOCK));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(MIKAPIKKER.get(), MikaPikkerEntity.createAttributes().build()));
        CircuitBanen.init();
        CircuitBanen.REGENBOOG.extra(new CircuitExtra(true));
        CircuitBanen.VADS.extra(new CircuitExtra(false));
        CircuitBanen.KAASBERG.extra(new CircuitExtra(false));
        RaceBaan.RACEBAAN.extra(new CircuitExtra(false));        // (the old racebaan's Mika-pikkers on lastig)
        Minigames.registerGame(Minigames.CIRCUIT, p -> RaceGame.isRacingIn(p, Minigames.CIRCUIT));
        RaceProtection.protect(CircuitBanen.STRUCTURE, "gui.guhs.circuit.no_build");
        KledingBronnen.bron(GuhClothes.CIRCUIT_HELMPJE, "circuit", CircuitRole.PRICE_HELMPJE + " circuitbekers");
        KledingBronnen.bron(GuhClothes.CIRCUIT_VLAGCAPE, "circuit", CircuitRole.PRICE_VLAGCAPE + " circuitbekers");
        KledingBronnen.bron(GuhClothes.CIRCUIT_RACEPAK, "circuit", CircuitRole.PRICE_RACEPAK + " circuitbekers");
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> CircuitExtra.forgetAll());
    }

    public static void payloads(PayloadRegistrar registrar) {
        CircuitPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(CIRCUITBEKER.get()));
        output.accept(new ItemStack(REGENBOOGWEG_ITEM.get()));
        output.accept(new ItemStack(REGENBOOGWEG_PLAAT_ITEM.get()));
        output.accept(new ItemStack(KAASWEG_ITEM.get()));
        output.accept(new ItemStack(KAASWEG_PLAAT_ITEM.get()));
        output.accept(new ItemStack(KAASSAUS_ITEM.get()));
        output.accept(new ItemStack(BERGIJS_ITEM.get()));
        output.accept(new ItemStack(STUITERPADDENSTOEL_ITEM.get()));
        output.accept(new ItemStack(BOOSTRING_ITEM.get()));
    }

    /** Coach Vahoegvroem. */
    @Nullable
    public static NpcRole role() {
        return ROLE;
    }

    /** The rainbow road as a slab (with its colour). */
    public static class RegenboogPlaat extends SlabBlock {
        public static final MapCodec<RegenboogPlaat> CODEC = simpleCodec(RegenboogPlaat::new);

        public RegenboogPlaat(Properties properties) {
            super(properties);
            registerDefaultState(defaultBlockState().setValue(CircuitBlocks.KLEUR, 0));
        }

        @Override
        public MapCodec<? extends SlabBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(CircuitBlocks.KLEUR);
        }
    }

    private CircuitFeature() {
    }
}
