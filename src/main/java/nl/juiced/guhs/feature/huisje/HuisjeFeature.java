package nl.juiced.guhs.feature.huisje;

import java.util.function.Consumer;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.piep.PiepMaatje;

import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
/**
 * Het Guhhuisje (2.10 "Lieve vadsjes van elkaar", fundament): a little house shaped like a guh HEAD in three sizes
 * (klein 3, medium 5, groot 8 residents), the home base of your guhs, muisjes, Schilly and Poepschilly. Residents sleep
 * inside at night, come out yawning in the morning and do chores (klusjes) and play with toys (speelgoed) around it
 * ({@link HuisjeGoal}); their output goes to a chest next to it or a Bank Guh ({@link HuisjeOpslag}). The owner's screen
 * lists the residents with their chores and shows the chore area as a blue dome. tools/features/huisje.py makes the
 * resources (the guh-head models, textures, recipes, sounds, texts).
 */
public final class HuisjeFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<PoiType> POI_TYPES = DeferredRegister.create(Registries.POINT_OF_INTEREST_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    private static BlockBehaviour.Properties props() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5f, 6f).sound(SoundType.WOOL).noOcclusion()
                .pushReaction(PushReaction.BLOCK).ignitedByLava();
    }

    public static final DeferredBlock<HuisjeBlock> KLEIN = BLOCKS.registerBlock("guhhuisje_klein", p -> new HuisjeBlock(HuisjeMaat.KLEIN, p), () -> props());
    public static final DeferredBlock<HuisjeBlock> MEDIUM = BLOCKS.registerBlock("guhhuisje_medium", p -> new HuisjeBlock(HuisjeMaat.MEDIUM, p), () -> props());
    public static final DeferredBlock<HuisjeBlock> GROOT = BLOCKS.registerBlock("guhhuisje_groot", p -> new HuisjeBlock(HuisjeMaat.GROOT, p), () -> props());
    /** The invisible parts of every huisje (no item, no drops: breaking it breaks the huisje). */
    public static final DeferredBlock<HuisjeDeelBlock> DEEL = BLOCKS.registerBlock("guhhuisje_deel", HuisjeDeelBlock::new,
            () -> props().noLootTable());

    static {
        ITEMS.registerItem("guhhuisje_klein", p -> new HuisjeItem(KLEIN.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
        ITEMS.registerItem("guhhuisje_medium", p -> new HuisjeItem(MEDIUM.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
        ITEMS.registerItem("guhhuisje_groot", p -> new HuisjeItem(GROOT.get(), p), () -> new Item.Properties().rarity(Rarity.UNCOMMON).useBlockDescriptionPrefix());
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HuisjeBlockEntity>> HUISJE_BE = BLOCK_ENTITIES.register("guhhuisje",
            () -> new BlockEntityType<>(HuisjeBlockEntity::new, KLEIN.get(), MEDIUM.get(), GROOT.get()));

    public static final DeferredHolder<PoiType, PoiType> POI = POI_TYPES.register("guhhuisje", () -> new PoiType(ImmutableSet.<net.minecraft.world.level.block.state.BlockState>builder()
            .addAll(KLEIN.get().getStateDefinition().getPossibleStates()).addAll(MEDIUM.get().getStateDefinition().getPossibleStates())
            .addAll(GROOT.get().getStateDefinition().getPossibleStates()).build(), 0, 1));

    public static final DeferredHolder<SoundEvent, SoundEvent> DEUR_GELUID = SOUNDS.register("huisje.deur",
            () -> SoundEvent.createVariableRangeEvent(Guhs.id("huisje.deur")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SNURK_GELUID = SOUNDS.register("huisje.snurk",
            () -> SoundEvent.createVariableRangeEvent(Guhs.id("huisje.snurk")));

    // --- 1.3.2 "Huisje betreden" (Binnen): the doorbell, the two blocks of the rooms, the sleeping stand-in ------------------
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredHolder<SoundEvent, SoundEvent> BEL_GELUID = SOUNDS.register("huisje.bel",
            () -> SoundEvent.createVariableRangeEvent(Guhs.id("huisje.bel")));
    /** The little guh bed of the rooms (no item: it only stands in the room templates). */
    public static final DeferredBlock<nl.juiced.guhs.block.GuhFurnitureBlock> BEDJE = BLOCKS.registerBlock("huisje_bedje",
            p -> new nl.juiced.guhs.block.GuhFurnitureBlock(p, -1, new double[] {1, 0, 0, 15, 7, 16}),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(-1f, 3600000f).sound(SoundType.WOOL).noOcclusion()
                    .noLootTable().pushReaction(PushReaction.BLOCK));
    /** The little window with a painted view (no item either). */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> RAAM = BLOCKS.registerSimpleBlock("huisje_raam",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_BLUE).strength(-1f, 3600000f).sound(SoundType.GLASS)
                    .noLootTable().lightLevel(s -> 11).pushReaction(PushReaction.BLOCK));
    public static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<BinnenGuh>> SLAPER =
            ENTITY_TYPES.register("huisje_slaper", () -> net.minecraft.world.entity.EntityType.Builder.of(BinnenGuh::new, net.minecraft.world.entity.MobCategory.MISC)
                    .sized(0.9f, 0.8f).eyeHeight(0.55f).clientTrackingRange(10).noSave().noSummon()
                    .build(net.minecraft.resources.ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("huisje_slaper"))));

    public static HuisjeBlock blok(HuisjeMaat maat) {
        return switch (maat) {
            case KLEIN -> KLEIN.get();
            case MEDIUM -> MEDIUM.get();
            case GROOT -> GROOT.get();
        };
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        POI_TYPES.register(modBus);
        SOUNDS.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) ->
                event.put(SLAPER.get(), nl.juiced.guhs.entity.GuhEntity.createAttributes().build()));
        Binnen.register();
        NeoForge.EVENT_BUS.addListener(BinnenCommando::register);
        GuhHooks.doelen((guh, goals) -> goals.addGoal(3, new HuisjeGoal(guh)));
        GuhHooks.tick(guh -> {
            if (Huisjes.isBinnen(guh)) {
                Huisjes.binnenTick(guh);
            }
            // 1.3.2: hearts it was given in its room (tucked in, petted) while it was not loaded
            if (!guh.level().isClientSide() && (guh.tickCount + guh.getId()) % 60 == 0 && !(guh instanceof BinnenGuh)) {
                BinnenInrichting.teGoed(guh);
            }
        });
        // maatjes (muisjes, Schilly, Poepschilly) get the home-base goal when they join the level
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent event) -> {
            if (!event.getLevel().isClientSide() && event.getEntity() instanceof PiepMaatje && event.getEntity() instanceof PathfinderMob mob) {
                HuisjeGoal.zorgVoor(mob);
                if (Huisjes.isBewoner(mob)) {
                    Huisje h = Huisjes.thuisVan(mob);
                    if (h != null) {
                        Vec3 m = h.midden();
                        mob.setHomeTo(net.minecraft.core.BlockPos.containing(m), Huisjes.BEREIK);
                    }
                }
            }
        });
        // 3.0: only the owner (or an op) breaks a huisje; the owner's name stays up to date
        NeoForge.EVENT_BUS.addListener((BreakBlockEvent event) -> {
            if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel sl && (event.getState().getBlock() instanceof HuisjeBlock
                    || event.getState().getBlock() instanceof HuisjeDeelBlock)) {
                net.minecraft.core.BlockPos c = event.getState().getBlock() instanceof HuisjeDeelBlock ? HuisjeDeelBlock.controller(event.getState(), event.getPos())
                        : event.getPos();
                Huisje h = Huisjes.van(sl, c);
                if (h != null && !Huisjes.magBewerken(event.getPlayer(), h)) {
                    event.setCanceled(true);
                    event.getPlayer().sendOverlayMessage(Huisjes.vanWie(h).copy().withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer sp) {
                Huisjes.naamBijwerken(sp);
            }
        });
        NeoForge.EVENT_BUS.addListener((EntityTickEvent.Pre event) -> {
            if (event.getEntity() instanceof PiepMaatje && !event.getEntity().level().isClientSide() && Huisjes.isBinnen(event.getEntity())) {
                Huisjes.binnenTick(event.getEntity());
            }
        });
    }

    public static void payloads(PayloadRegistrar registrar) {
        HuisjePayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KLEIN.get()));
        output.accept(new ItemStack(MEDIUM.get()));
        output.accept(new ItemStack(GROOT.get()));
    }

    private HuisjeFeature() {
    }

    /** A huisje item: its lore lines come from the block (1.0.0: Block#appendHoverText, gone in 26.1). */
    static final class HuisjeItem extends BlockItem {
        HuisjeItem(HuisjeBlock block, Item.Properties properties) {
            super(block, properties);
        }

        @Override
        public void appendHoverText(net.minecraft.world.item.ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                    java.util.function.Consumer<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
            if (getBlock() instanceof HuisjeBlock blok) {
                blok.appendHoverText(stack, context, display, tooltip, flag);
            }
        }
    }
}
