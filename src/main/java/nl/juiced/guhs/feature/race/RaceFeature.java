package nl.juiced.guhs.feature.race;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.NpcRole;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
/**
 * Guhrace: the guh racebaan in the Guhmension. The Raceguh lends you a race guh for 3 laps through the checkpoint rings,
 * with VAHOEG launch pads, a countdown, lap times, your record as a ghost, raceprijsjes and the jockey outfit.
 * See {@link RaceGame} (the race), {@link RaceRole} (the Raceguh), {@link RaceTrack} (the track) and tools/features/race.py
 * (the racebaan, textures, texts, advancements and quests).
 */
public final class RaceFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    /** A checkpoint ring block (glowing, can't be broken). */
    public static final DeferredBlock<RaceBlocks.Checkpoint> RACE_CHECKPOINT = BLOCKS.registerBlock("race_checkpoint", RaceBlocks.Checkpoint::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(-1f, 3600000f).noLootTable().lightLevel(s -> 15)
                    .sound(SoundType.AMETHYST).isValidSpawn((s, l, p, e) -> false).pushReaction(PushReaction.BLOCK));
    /** The VAHOEG launch pad. */
    public static final DeferredBlock<RaceBlocks.VahoegPad> RACE_PAD = BLOCKS.registerBlock("race_vahoegpad", RaceBlocks.VahoegPad::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(-1f, 3600000f).noLootTable().lightLevel(s -> 10)
                    .sound(SoundType.AMETHYST).noOcclusion().isValidSpawn((s, l, p, e) -> false).pushReaction(PushReaction.BLOCK));
    /** The invisible start marker. */
    public static final DeferredBlock<RaceBlocks.Start> RACE_START = BLOCKS.registerBlock("race_start", RaceBlocks.Start::new,
            BlockBehaviour.Properties.of().noCollission().noLootTable().strength(-1f, 3600000f).noOcclusion()
                    .isValidSpawn((s, l, p, e) -> false).pushReaction(PushReaction.BLOCK));

    /** The guhrace's own money: won by racing, spent on the jockey outfit. */
    public static final DeferredItem<Item> RACEPRIJSJE = ITEMS.registerSimpleItem("raceprijsje", new Item.Properties());
    public static final DeferredItem<BlockItem> RACE_CHECKPOINT_ITEM = ITEMS.registerSimpleBlockItem(RACE_CHECKPOINT);
    public static final DeferredItem<BlockItem> RACE_PAD_ITEM = ITEMS.registerSimpleBlockItem(RACE_PAD);

    /** The rental race guh (never saved). */
    public static final DeferredHolder<EntityType<?>, EntityType<RaceGuhEntity>> RACE_GUH = ENTITY_TYPES.register("race_guh",
            () -> EntityType.Builder.of(RaceGuhEntity::new, MobCategory.MISC).sized(0.9f, 0.8f).eyeHeight(0.55f)
                    .passengerAttachments(new Vec3(0, 0.6, -0.3)).clientTrackingRange(10).updateInterval(1).noSave().noSummon()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("race_guh"))));
    /** The ghost of your best race (never saved). */
    public static final DeferredHolder<EntityType<?>, EntityType<RaceGhostEntity>> RACE_GHOST = ENTITY_TYPES.register("race_ghost",
            () -> EntityType.Builder.of(RaceGhostEntity::new, MobCategory.MISC).sized(0.9f, 0.8f).eyeHeight(0.55f)
                    .clientTrackingRange(10).updateInterval(1).noSave().noSummon().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("race_ghost"))));

    private static final RaceRole ROLE = new RaceRole();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(RACE_GUH.get(), GuhEntity.createAttributes().build());
            event.put(RACE_GHOST.get(), GuhEntity.createAttributes().build());
        });
        NeoForge.EVENT_BUS.addListener(RaceGame::onDamage);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                RaceGame.playerGone(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                RaceGame.playerGone(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                RaceGame.playerGone(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                RaceGame.checkStale(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            RaceGame.forgetAll();
            RaceProtection.forgetAll();
        });
        RaceProtection.register();
    }

    public static void payloads(PayloadRegistrar registrar) {
        RacePayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(RACEPRIJSJE.get()));
        output.accept(new ItemStack(RACE_CHECKPOINT_ITEM.get()));
        output.accept(new ItemStack(RACE_PAD_ITEM.get()));
    }

    @Nullable
    public static NpcRole role() {
        return ROLE;
    }

    private RaceFeature() {
    }
}
