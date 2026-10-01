package nl.juiced.guhs.feature.weerder;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;

/**
 * De Wilde-guhweerder (1.2.0): a little guh sign ("hier woont al een vadsje") you put in your house or base. No WILD guhs
 * spawn in the area around it: not the natural/biome spawns (overworld, Guhmension, any dimension), not the Guhmension's
 * top-ups ({@link nl.juiced.guhs.world.GuhmensionSpawner}), not the wild Zeemeerguhs. Still lief: the wild guhs see the sign
 * and know a vadsje already lives here, so they go cuddle somewhere else. Your own (tamed) guhs, babies, story/NPC guhs and
 * guhs from a spawn egg are welcome, and guhs that are already there stay.
 * <p>
 * Right-click: a small screen ({@code client.WeerderScreen}) with the area radius ({@link #STRALEN}) and the toggle "laat de
 * area zien" (the same blue dome as the Guhhuisje's klus-area). Like a Guhhuisje only the one who placed it (or an op)
 * changes it or breaks it. Where the weerders are: {@link WeerderIndex} (a chunk index per dimension, so the spawn check is
 * cheap). tools/features/weerder.py makes the resources (model, textures, recipe, loot, texts, FTB quest).
 */
public final class WeerderFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);

    /** The radius steps of the area (blocks around the sign); a new weerder gets {@link #STANDAARD}. */
    public static final List<Integer> STRALEN = List.of(8, 16, 24, 32, 48);
    public static final int STANDAARD = 16;

    public static final DeferredBlock<WildeGuhweerderBlock> WEERDER = BLOCKS.registerBlock("wilde_guhweerder", WildeGuhweerderBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.0f, 6f).sound(SoundType.WOOD).noOcclusion()
                    .ignitedByLava());
    public static final DeferredItem<BlockItem> WEERDER_ITEM = ITEMS.registerItem("wilde_guhweerder",
            p -> new BlockItem(WEERDER.get(), p) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
                    tooltip.accept(Component.translatable("block.guhs.wilde_guhweerder.lore").withStyle(ChatFormatting.GRAY));
                    tooltip.accept(Component.translatable("block.guhs.wilde_guhweerder.lore.lief").withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }, () -> new Item.Properties());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WeerderBlockEntity>> WEERDER_BE = BLOCK_ENTITIES.register("wilde_guhweerder",
            () -> new BlockEntityType<>(WeerderBlockEntity::new, WEERDER.get()));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        // natural and chunk-generation spawns (every dimension): a wild guh inside a weerder's area may not appear
        NeoForge.EVENT_BUS.addListener((MobSpawnEvent.PositionCheck event) -> {
            if (event.getEntity() instanceof GuhEntity && isWildeSpawn(event.getSpawnType())
                    && magNiet(event.getLevel().getLevel(), event.getEntity())) {
                event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
            }
        });
        // like a Guhhuisje: only the one who placed it (or an op) breaks it
        NeoForge.EVENT_BUS.addListener((BreakBlockEvent event) -> {
            if (event.getState().getBlock() instanceof WildeGuhweerderBlock && event.getLevel().getBlockEntity(event.getPos()) instanceof WeerderBlockEntity be
                    && !be.magBewerken(event.getPlayer())) {
                event.setCanceled(true);
                event.getPlayer().sendOverlayMessage(be.vanWie().copy().withStyle(ChatFormatting.GRAY));
            }
        });
    }

    /** The spawn reasons of wild guhs: natural (biome) spawns and the ones made with a new chunk. */
    public static boolean isWildeSpawn(EntitySpawnReason reason) {
        return reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION;
    }

    /** May a WILD guh appear at this spot? False inside the area of a Wilde-guhweerder (all spawn paths of wild guhs ask this). */
    public static boolean wildeGuhMag(ServerLevel level, BlockPos pos) {
        return !WeerderIndex.beschermd(level, pos);
    }

    private static boolean magNiet(ServerLevel level, Entity guh) {
        return !(guh instanceof GuhEntity g && g.isTame()) && !wildeGuhMag(level, guh.blockPosition());
    }

    public static void payloads(PayloadRegistrar registrar) {
        WeerderPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(WEERDER_ITEM.get()));
    }

    private WeerderFeature() {
    }
}
