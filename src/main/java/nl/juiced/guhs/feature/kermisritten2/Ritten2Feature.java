package nl.juiced.guhs.feature.kermisritten2;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
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
 * 1.8.1: the kermis' Botsguhs and Spookguhhuis. The kermis stamp only puts the two anchor blocks down
 * ({@code guhs:kermisrit_botsguhs}: the middle of the floor, facing = the entrance; {@code guhs:kermisrit_spookhuis}: in the
 * paving in front of the mouths, facing = away from the house); everything else comes from those blocks. Resources:
 * tools/features/kermisritten2.py.
 */
public final class Ritten2Feature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    private static BlockBehaviour.Properties anker(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(-1f, 3600000f).sound(SoundType.STONE).noLootTable();
    }

    public static final DeferredBlock<AnkerBlock> BOTSBAAN = BLOCKS.registerBlock("kermisrit_botsguhs",
            p -> new AnkerBlock(p, BotsbaanBlockEntity::new, Ritten2Feature::botsbaanBe), () -> anker(MapColor.COLOR_PURPLE));
    public static final DeferredBlock<AnkerBlock> SPOOKHUIS = BLOCKS.registerBlock("kermisrit_spookhuis",
            p -> new AnkerBlock(p, SpookhuisBlockEntity::new, Ritten2Feature::spookhuisBe), () -> anker(MapColor.COLOR_GRAY));
    public static final DeferredItem<BlockItem> BOTSBAAN_ITEM = ITEMS.registerSimpleBlockItem(BOTSBAAN);
    public static final DeferredItem<BlockItem> SPOOKHUIS_ITEM = ITEMS.registerSimpleBlockItem(SPOOKHUIS);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BotsbaanBlockEntity>> BOTSBAAN_BE = BLOCK_ENTITIES.register("kermisrit_botsguhs",
            () -> new BlockEntityType<>(BotsbaanBlockEntity::new, BOTSBAAN.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpookhuisBlockEntity>> SPOOKHUIS_BE = BLOCK_ENTITIES.register("kermisrit_spookhuis",
            () -> new BlockEntityType<>(SpookhuisBlockEntity::new, SPOOKHUIS.get()));

    private static BlockEntityType<?> botsbaanBe() {
        return BOTSBAAN_BE.get();
    }

    private static BlockEntityType<?> spookhuisBe() {
        return SPOOKHUIS_BE.get();
    }

    private static ResourceKey<EntityType<?>> sleutel(String naam) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id(naam));
    }

    public static final DeferredHolder<EntityType<?>, EntityType<BotsguhEntity>> BOTSGUH = ENTITY_TYPES.register("kermis_botsguh",
            () -> EntityType.Builder.of(BotsguhEntity::new, MobCategory.MISC).sized(1.7f, 0.9f).clientTrackingRange(10).updateInterval(1)
                    .build(sleutel("kermis_botsguh")));
    public static final DeferredHolder<EntityType<?>, EntityType<SpookkarEntity>> SPOOKKAR = ENTITY_TYPES.register("kermis_spookkar",
            () -> EntityType.Builder.of(SpookkarEntity::new, MobCategory.MISC).sized(1.3f, 0.9f).clientTrackingRange(10).updateInterval(1)
                    .build(sleutel("kermis_spookkar")));
    public static final DeferredHolder<EntityType<?>, EntityType<SpookjeEntity>> SPOOKJE = ENTITY_TYPES.register("kermis_spookje",
            () -> EntityType.Builder.of(SpookjeEntity::new, MobCategory.MISC).sized(1f, 1.8f).clientTrackingRange(8).build(sleutel("kermis_spookje")));

    private Ritten2Feature() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        NeoForge.EVENT_BUS.addListener(Ritten2Feature::onMount);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> eruit(event.getEntity()));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> event.getServer().getPlayerList().getPlayers().forEach(Ritten2Feature::eruit));
    }

    /** No jumping off in the dark: sneaking under way does nothing. */
    private static void onMount(EntityMountEvent event) {
        if (event.isDismounting() && !event.getLevel().isClientSide() && event.getEntityBeingMounted() instanceof SpookkarEntity kar
                && kar.opWeg() && !kar.uitstappen && event.getEntityMounting() instanceof ServerPlayer p && p.isAlive() && !p.isRemoved()
                && p.isShiftKeyDown()) {
            event.setCanceled(true);
            p.sendOverlayMessage(Component.translatable("gui.guhs.spookhuis.blijf_zitten").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** Rides are never saved: whoever leaves gets off first, so they don't vanish with their ride. */
    private static void eruit(Player p) {
        if (p.getVehicle() instanceof SpookkarEntity kar) {
            kar.allemaalEruit(null);
        } else if (p.getVehicle() instanceof BotsguhEntity) {
            p.stopRiding();
        }
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(BOTSBAAN_ITEM.get()));
        output.accept(new ItemStack(SPOOKHUIS_ITEM.get()));
    }
}
