package nl.juiced.guhs.feature.guhpixel.parkour;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhpixel.GidsBlad;
import nl.juiced.guhs.feature.guhpixel.PxZelftest;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.speelgoed.SpeelgoedFeature;

/**
 * Guhpixel slice "parkour": the Guh-parkour (DESIGN_PX section 6; it has nothing to do with the guhpixel dimension).
 * <ul>
 *   <li>the <b>Startpaaltje</b> ({@link StartpaalBlock}) and the <b>Finishpaaltje</b> ({@link FinishpaalBlock});</li>
 *   <li>six obstacles ({@link Obstakel}, {@link ObstakelBlock}): Guh-horde, Springplankje, Kruiptunnel, Slalompaaltjes,
 *       Evenwichtsbalk, Knabbeltafeltje; the "wip" of a route is the existing Guh-wip, and the glijbaantje, the schommel and
 *       the pluizige tunnel are pieces too;</li>
 *   <li>the <b>Scorebord</b> ({@link ScorebordBlock}): laps and best lap time per guh;</li>
 *   <li>laying out a route by clicking ({@link Uitzetten}, {@link Routes}), the post screen ({@link ParkourPayloads},
 *       client.ParkourScherm) and the runner ({@link RouteGoal}, {@link RouteStukken}) for up to four of your guhs.</li>
 * </ul>
 * tools/features/guhpixel_parkour.py makes the models, textures, recipes, loot, sounds, texts, the test rooms and the quests.
 */
public final class ParkourSlice {
    /** The namespace: ids guhparkour_*, the GuhKiezer claim, lang gui.guhs.guhparkour.*. */
    public static final String NS = "guhparkour";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    private static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.8f, 3f).sound(SoundType.WOOD).noOcclusion()
                .pushReaction(PushReaction.BLOCK).ignitedByLava();
    }

    public static final DeferredBlock<StartpaalBlock> STARTPAALTJE = BLOCKS.registerBlock("guhparkour_startpaaltje", StartpaalBlock::new, () -> props());
    public static final DeferredBlock<FinishpaalBlock> FINISHPAALTJE = BLOCKS.registerBlock("guhparkour_finishpaaltje", FinishpaalBlock::new, () -> props());
    public static final DeferredBlock<ScorebordBlock> SCOREBORD = BLOCKS.registerBlock("guhparkour_scorebord", ScorebordBlock::new, () -> props());
    /** The six obstacles by kind (ids guhparkour_&lt;kind&gt;). */
    public static final Map<Obstakel, DeferredBlock<ObstakelBlock>> OBSTAKELS = new EnumMap<>(Obstakel.class);

    static {
        for (Obstakel o : Obstakel.values()) {
            OBSTAKELS.put(o, BLOCKS.registerBlock(NS + "_" + o.id(),
                    p -> o == Obstakel.KRUIPTUNNEL ? new ObstakelBlock.Kruiptunnel(p) : new ObstakelBlock(p, o),
                    () -> o == Obstakel.KRUIPTUNNEL ? props().sound(SoundType.WOOL) : props()));
        }
    }

    public static final DeferredItem<StartpaalBlock.Artikel> STARTPAALTJE_ITEM = ITEMS.registerItem("guhparkour_startpaaltje",
            p -> new StartpaalBlock.Artikel(STARTPAALTJE.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix().stacksTo(1));

    static {
        ITEMS.registerItem("guhparkour_finishpaaltje", p -> new SpeelgoedFeature.SpeelgoedBlockItem(FINISHPAALTJE.get(), p),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        ITEMS.registerItem("guhparkour_scorebord", p -> new SpeelgoedFeature.SpeelgoedBlockItem(SCOREBORD.get(), p),
                () -> new Item.Properties().useBlockDescriptionPrefix());
        for (Obstakel o : Obstakel.values()) {
            ITEMS.registerItem(NS + "_" + o.id(), p -> new SpeelgoedFeature.SpeelgoedBlockItem(OBSTAKELS.get(o).get(), p),
                    () -> new Item.Properties().useBlockDescriptionPrefix());
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StartpaalBlockEntity>> STARTPAAL_BE = BLOCK_ENTITIES.register(
            "guhparkour_startpaaltje", () -> new BlockEntityType<>(StartpaalBlockEntity::new, STARTPAALTJE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ScorebordBlockEntity>> SCOREBORD_BE = BLOCK_ENTITIES.register(
            "guhparkour_scorebord", () -> new BlockEntityType<>(ScorebordBlockEntity::new, SCOREBORD.get()));

    public static final DeferredHolder<SoundEvent, SoundEvent> KLIK = geluid("guhparkour.klik");
    public static final DeferredHolder<SoundEvent, SoundEvent> START = geluid("guhparkour.start");
    public static final DeferredHolder<SoundEvent, SoundEvent> FINISH = geluid("guhparkour.finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> RECORD = geluid("guhparkour.record");
    public static final DeferredHolder<SoundEvent, SoundEvent> HUP = geluid("guhparkour.hup");
    public static final DeferredHolder<SoundEvent, SoundEvent> BOING = geluid("guhparkour.boing");
    public static final DeferredHolder<SoundEvent, SoundEvent> OEPS = geluid("guhparkour.oeps");
    public static final DeferredHolder<SoundEvent, SoundEvent> SMAK = geluid("guhparkour.smak");

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(Guhs.id(id)));
    }

    /** Every block of the slice, in creative-tab order. */
    public static List<Block> blokken() {
        List<Block> uit = new java.util.ArrayList<>();
        uit.add(STARTPAALTJE.get());
        uit.add(FINISHPAALTJE.get());
        for (Obstakel o : Obstakel.values()) {
            uit.add(OBSTAKELS.get(o).get());
        }
        uit.add(SCOREBORD.get());
        return uit;
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        SOUNDS.register(modBus);
        GuhHooks.doelen((guh, goals) -> goals.addGoal(3, new RouteGoal(guh)));
        GuhHooks.tick(RouteStukken::land);
        NeoForge.EVENT_BUS.register(Uitzetten.class);
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> ParkourCommando.register(event));
        GidsBlad.registreer(ParkourStats.SECTIE);
        PxZelftest.registreer("parkour", (server, level, meld) -> {
            for (Block b : blokken()) {
                Identifier id = BuiltInRegistries.BLOCK.getKey(b);
                meld.check(b.asItem() != net.minecraft.world.item.Items.AIR, "block and item " + id);
                ResourceKey<Recipe<?>> recept = ResourceKey.create(Registries.RECIPE, id);
                meld.check(server.getRecipeManager().byKey(recept).isPresent(), "recipe " + id);
            }
            for (String adv : List.of("guhparkour_rondje", "guhparkour_groot")) {
                meld.check(server.getAdvancements().get(Guhs.id("quest/" + adv)) != null, "advancement quest/" + adv);
            }
        });
    }

    public static void payloads(PayloadRegistrar registrar) {
        ParkourPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        for (Block b : blokken()) {
            output.accept(new ItemStack(b));
        }
    }

    private ParkourSlice() {
    }
}
