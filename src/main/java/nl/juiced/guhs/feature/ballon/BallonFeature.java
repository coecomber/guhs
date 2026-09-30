package nl.juiced.guhs.feature.ballon;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.sterrenwacht.Buiten;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
/**
 * Het Ballonfestival (2.8, slice "buiten"): a festival field full of guh-shaped hot-air balloons on the Guhvelden and
 * the Roze pluisjes (the loose structure ballonfestival, tools/features/ballon.py). Kapitein Wolkje
 * ({@link BallonRole}) takes you on a fixed, guided round flight in his {@link LuchtballonEntity} (no steering: just
 * look around), past two of eight viewpoints ({@link BallonRoute}, {@link BallonVlucht}); after landing you get their
 * stamps on your ballonstempelkaart (the Knus tab) and ballonmunten for his little shop (the ballonpet, the
 * ballonbril and mini luchtballonnen).
 */
public final class BallonFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    public static final Buiten.Bescherming BESCHERMING = new Buiten.Bescherming("ballonfestival", "gui.guhs.ballon.beschermd");

    /** The launch platform: the balloon waits on it, and lands on it again. */
    public static final DeferredBlock<BallonBlocks.Ballonsteiger> BALLONSTEIGER = BLOCKS.registerBlock("ballonsteiger", BallonBlocks.Ballonsteiger::new,
            BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_PLANKS).mapColor(MapColor.WOOD));
    /** A little guh balloon to put anywhere (it glows a little: its tiny burner). */
    public static final DeferredBlock<BallonBlocks.MiniLuchtballon> MINI_LUCHTBALLON = BLOCKS.registerBlock("mini_luchtballon",
            BallonBlocks.MiniLuchtballon::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.4f).sound(SoundType.WOOL)
                    .noOcclusion().lightLevel(s -> 8));

    public static final DeferredItem<Buiten.LoreBlockItem> BALLONSTEIGER_ITEM = ITEMS.registerItem("ballonsteiger",
            p -> new Buiten.LoreBlockItem(BALLONSTEIGER.get(), p), new Item.Properties());
    public static final DeferredItem<Buiten.LoreBlockItem> MINI_LUCHTBALLON_ITEM = ITEMS.registerItem("mini_luchtballon",
            p -> new Buiten.LoreBlockItem(MINI_LUCHTBALLON.get(), p), new Item.Properties());
    /** The festival's coin: for every flight (and every new stamp), spent in Kapitein Wolkje's shop. */
    public static final DeferredItem<Buiten.LoreItem> BALLONMUNT = ITEMS.registerItem("ballonmunt", Buiten.LoreItem::new,
            new Item.Properties().rarity(Rarity.UNCOMMON));

    public static final DeferredHolder<EntityType<?>, EntityType<LuchtballonEntity>> LUCHTBALLON = ENTITIES.register("guh_luchtballon",
            () -> EntityType.Builder.<LuchtballonEntity>of(LuchtballonEntity::new, MobCategory.MISC).sized(1.8f, 1.2f)
                    .clientTrackingRange(16).updateInterval(3).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("guh_luchtballon"))));

    /** A little white cloud puff (around a flying balloon, the Wolkenpoort). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BALLONWOLKJE = PARTICLES.register("ballonwolkje",
            () -> new SimpleParticleType(false));

    public static final DeferredHolder<SoundEvent, SoundEvent> BRANDER = sound("ballon.brander");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIND = sound("ballon.wind");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        BESCHERMING.register();
        Minigames.registerGame(Minigames.BALLON, BallonFeature::vliegt);
        NeoForge.EVENT_BUS.register(BallonEvents.class);
        NeoForge.EVENT_BUS.addListener(BallonCommando::register);
        BallonVoortgang.register();
    }

    /** Is this player on a balloon flight? */
    public static boolean vliegt(ServerPlayer player) {
        return player.getVehicle() instanceof LuchtballonEntity ballon && ballon.vliegt();
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(BALLONSTEIGER_ITEM.get()));
        output.accept(new ItemStack(MINI_LUCHTBALLON_ITEM.get()));
        output.accept(new ItemStack(BALLONMUNT.get()));
    }

    /** Kapitein Wolkje (BALLONGUH). */
    @Nullable
    public static NpcRole role() {
        return BallonRole.INSTANCE;
    }

    private BallonFeature() {
    }
}
