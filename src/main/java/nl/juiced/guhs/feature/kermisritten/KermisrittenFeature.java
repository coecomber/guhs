package nl.juiced.guhs.feature.kermisritten;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;

/**
 * 1.8.1: the turning rides of the Guhkermis: Knabbelbooster, Zweefmolen, Reuzenrad and Guhtwister. One controller block per
 * ride ({@link KermisritBlock}, placed by the kermis stamp), one seat entity ({@link KermisritStoel}), all geometry in
 * {@link KermisritSoort}. Resources and texts: tools/features/kermisritten.py.
 */
public final class KermisrittenFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final Map<KermisritSoort, DeferredBlock<KermisritBlock>> BLOKKEN = new EnumMap<>(KermisritSoort.class);
    public static final Map<KermisritSoort, DeferredItem<BlockItem>> BLOK_ITEMS = new EnumMap<>(KermisritSoort.class);

    static {
        for (KermisritSoort s : KermisritSoort.values()) {
            DeferredBlock<KermisritBlock> blok = BLOCKS.registerBlock(s.blokId(), p -> new KermisritBlock(s, p),
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.COLOR_PINK).strength(-1f, 3600000f).noLootTable());
            BLOKKEN.put(s, blok);
            BLOK_ITEMS.put(s, ITEMS.registerItem(s.blokId(), p -> new BlockItem(blok.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix()));
        }
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<KermisritBlockEntity>> RIT_BE = BLOCK_ENTITIES.register("kermisrit",
            () -> new BlockEntityType<>(KermisritBlockEntity::new, BLOKKEN.values().stream().map(b -> (Block) b.get()).toArray(Block[]::new)));

    public static final DeferredHolder<EntityType<?>, EntityType<KermisritStoel>> STOEL = ENTITIES.register("kermisrit_stoel",
            () -> EntityType.Builder.<KermisritStoel>of(KermisritStoel::new, MobCategory.MISC).sized(1f, 1f).noSave().noSummon()
                    .clientTrackingRange(10).updateInterval(20).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("kermisrit_stoel"))));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ENTITIES.register(modBus);
        NeoForge.EVENT_BUS.addListener(KermisrittenFeature::onMount);
        NeoForge.EVENT_BUS.addListener(KermisrittenFeature::onLogout);
        NeoForge.EVENT_BUS.addListener(KermisrittenFeature::onStopping);
    }

    public static KermisritBlock blok(KermisritSoort soort) {
        return BLOKKEN.get(soort).get();
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<BlockItem> item : BLOK_ITEMS.values()) {
            output.accept(new ItemStack(item.get()));
        }
    }

    /** No sneaking out halfway a ride. */
    private static void onMount(EntityMountEvent event) {
        if (event.isDismounting() && !event.getLevel().isClientSide() && event.getEntityBeingMounted() instanceof KermisritStoel stoel
                && !stoel.uitstappen && event.getEntityMounting() instanceof ServerPlayer speler && speler.isAlive() && !speler.isRemoved()
                && speler.isShiftKeyDown()) {
            KermisritBlockEntity rit = stoel.rit();
            if (rit != null && rit.rijdt()) {
                event.setCanceled(true);
                speler.sendOverlayMessage(Component.translatable("gui.guhs.kermisrit.blijf_zitten").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    /** Seats are never saved, so whoever leaves is first put down on the ground. */
    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer speler) {
            zetUit(speler);
        }
    }

    private static void onStopping(ServerStoppingEvent event) {
        for (ServerPlayer speler : event.getServer().getPlayerList().getPlayers()) {
            zetUit(speler);
        }
    }

    private static void zetUit(ServerPlayer speler) {
        if (speler.getVehicle() instanceof KermisritStoel stoel) {
            KermisritBlockEntity rit = stoel.rit();
            if (rit != null) {
                rit.laatUit(speler);
            } else {
                speler.stopRiding();
            }
        }
    }

    private KermisrittenFeature() {
    }
}
