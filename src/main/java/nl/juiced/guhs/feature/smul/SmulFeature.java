package nl.juiced.guhs.feature.smul;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.VerstopBlocks;
import nl.juiced.guhs.feature.NpcRole;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
/**
 * Het Vadsig eetfestijn: a big guh food festival in the Guhmension. In its arena the Smulguh lets you catch falling
 * food for a minute with a big borrowed bowl (see {@link SmulGame}); you earn smulmunten for the smul outfit in her shop.
 * Resources: tools/features/smul.py.
 */
public final class SmulFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    // --- the invisible markers in the arena: where you start, two opposite corners, and the ends of the food chutes ---
    public static final DeferredBlock<VerstopBlocks.Marker> SMUL_START = marker("smul_start");
    public static final DeferredBlock<VerstopBlocks.Marker> SMUL_HOEK = marker("smul_hoek");
    public static final DeferredBlock<VerstopBlocks.Marker> SMUL_TRECHTER = marker("smul_trechter");

    /** The currency of the eetfestijn: earned by playing, spent at the Smulguh's shop. */
    public static final DeferredItem<Item> SMULMUNT = ITEMS.registerSimpleItem("smulmunt", () -> new Item.Properties());
    /** The big bowl you catch with: only ever borrowed during a game (see {@link SmulSchaalItem}). */
    public static final DeferredItem<SmulSchaalItem> SMULSCHAAL = ITEMS.registerItem("smulschaal", SmulSchaalItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    /** The golden kaasknabbel: a bonus in the game, and a (shop) treat that makes you quick. */
    public static final DeferredItem<Item> GOUDEN_SMULKNABBEL = ITEMS.registerSimpleItem("gouden_smulknabbel", new Item.Properties()
            .rarity(Rarity.RARE).food(new FoodProperties.Builder().nutrition(6).saturationModifier(1.2f).alwaysEdible()
                    .effect(() -> new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SPEED, 20 * 30, 1), 1f)
                    .effect(() -> new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.ABSORPTION, 20 * 60, 0), 1f)
                    .build()));

    public static final DeferredHolder<EntityType<?>, EntityType<SmulHapje>> HAPJE = ENTITY_TYPES.register("smul_hapje",
            () -> EntityType.Builder.<SmulHapje>of(SmulHapje::new, MobCategory.MISC).sized(0.7f, 0.7f).clientTrackingRange(6)
                    .updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("smul_hapje"))));

    private static DeferredBlock<VerstopBlocks.Marker> marker(String name) {
        return BLOCKS.registerBlock(name, VerstopBlocks.Marker::new, BlockBehaviour.Properties.of().noCollission().noLootTable()
                .strength(-1f, 3600000f).noOcclusion().isValidSpawn((s, l, p, e) -> false));
    }

    private static final NpcRole ROLE = new SmulRole();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        NeoForge.EVENT_BUS.addListener(SmulGame::onDamage);
        NeoForge.EVENT_BUS.addListener(SmulGame::onDeath);
        NeoForge.EVENT_BUS.addListener(SmulGame::onLogout);
        NeoForge.EVENT_BUS.addListener(SmulGame::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(SmulGame::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(SmulGame::onServerStopped);
        NeoForge.EVENT_BUS.addListener(SmulGame::onContainerClose);
        NeoForge.EVENT_BUS.addListener(SmulGame::onInteractEntity);
        NeoForge.EVENT_BUS.addListener(SmulGame::onInteractEntityAt);
        NeoForge.EVENT_BUS.addListener(SmulProtection::onBreak);
        nl.juiced.guhs.feature.Protected.add(SmulProtection::protectedAt);
        NeoForge.EVENT_BUS.addListener(SmulProtection::onPlace);
        NeoForge.EVENT_BUS.addListener(SmulProtection::onUseBlock);
        NeoForge.EVENT_BUS.addListener(SmulProtection::onUseItem);
        NeoForge.EVENT_BUS.addListener(SmulProtection::onExplosion);
        NeoForge.EVENT_BUS.addListener(SmulProtection::onMobGriefing);
    }

    public static void payloads(PayloadRegistrar registrar) {
        registrar.playToClient(SmulPayloads.Open.TYPE, SmulPayloads.Open.STREAM_CODEC, SmulPayloads.Open::handle);
        registrar.playToServer(SmulPayloads.Action.TYPE, SmulPayloads.Action.STREAM_CODEC, SmulPayloads.Action::handle);
    }

    /** The coin and the golden treat (the bowl is only lent out, so it's not in the tab). */
    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(SMULMUNT.get()));
        output.accept(new ItemStack(GOUDEN_SMULKNABBEL.get()));
    }

    @Nullable
    public static NpcRole role() {
        return ROLE;
    }

    private SmulFeature() {
    }
}
