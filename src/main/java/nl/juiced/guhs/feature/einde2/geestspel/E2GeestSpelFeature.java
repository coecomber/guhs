package nl.juiced.guhs.feature.einde2.geestspel;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.geestwezens.GeestWezen;

/**
 * einde2 phase B/C, slice geest_spel: the play of the Geestenwereld (CONTRACT_E2_B 7.9): zone access and guests
 * ({@link GeestToegang}), the White Lotus and its five trials ({@link WitteLotus}, {@link Proeven}), the lost lanterns
 * ({@link Lantaarns}), Hei Bai-guh's acorn ({@link HeiBai}), Koh's riddles ({@link KohRaadsels}), the chakra path and the
 * Guhvatar glow ({@link Chakras}), fast travel, mirror ponds and the meditation cushion ({@link Snelreis}), the rare
 * sightings, the moon night and the spirit tide ({@link Verschijningen}), footprints, the Geestenklok, the swamp vision,
 * Wan-guh and the eighth island ({@link Mysterie}), the per-player overlays ({@link GeestLagen}) and the five scenes
 * ({@link GeestScenes}). Lines: {@link GeestLijnen}. Dev: {@link GeestSpelCommando}. Resources:
 * tools/features/einde2_geest_spel.py (+ _tekst, _wiki).
 */
public final class E2GeestSpelFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    /** An item with a grey line under its name ({@code <item>.lore}). */
    public static class Lore extends Item {
        public Lore(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }
    }

    /** The six Pai Sho tiles (keepsakes of the trials; never used up). */
    public static final Map<String, DeferredItem<Item>> TEGELS = new LinkedHashMap<>();

    static {
        for (String m : GeestLijnen.TEGELS) {
            TEGELS.put(m, ITEMS.registerItem("pai_sho_tegel_" + m, Lore::new,
                    () -> new Item.Properties().stacksTo(1).rarity(m.equals("witte_lotus") ? Rarity.EPIC : Rarity.RARE)));
        }
    }

    public static final DeferredItem<Item> EIKEL = ITEMS.registerItem("einde2_geest_spel_knabbelkoor_eikel", Lore::new, () -> new Item.Properties());
    public static final DeferredItem<Item> KOH_MASKER = ITEMS.registerItem("einde2_geest_spel_koh_masker", Lore::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE)
                    .component(net.minecraft.core.component.DataComponents.EQUIPPABLE, Equippable.builder(EquipmentSlot.HEAD)
                            .setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).setDamageOnHurt(false).build()));
    public static final DeferredItem<Item> VEER = ITEMS.registerItem("einde2_geest_spel_veer", Lore::new, () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<Item> GOUDEN_ROL = ITEMS.registerItem("einde2_geest_spel_gouden_rol", Lore::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<Item> KOH_SCHUB = ITEMS.registerItem("einde2_geest_spel_koh_schub", Lore::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<Item> MAANDRUPPEL = ITEMS.registerItem("einde2_geest_spel_maandruppel", Lore::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<Item> EERSTE_KNABBEL = ITEMS.registerItem("einde2_geest_spel_eerste_knabbel", Lore::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

    /** The meditation cushion: after the story, a click (and a moment's stillness) takes your soul into the Geestenwereld. */
    public static class Mediteerkussen extends HorizontalDirectionalBlock {
        public static final MapCodec<Mediteerkussen> CODEC = simpleCodec(Mediteerkussen::new);
        private static final VoxelShape VORM = Block.box(1, 0, 1, 15, 5, 15);

        public Mediteerkussen(BlockBehaviour.Properties properties) {
            super(properties);
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
            return CODEC;
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            builder.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext context) {
            return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        }

        @Override
        protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
            return VORM;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!level.isClientSide() && player instanceof ServerPlayer p) {
                Snelreis.zitOpKussen(p);
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static final DeferredBlock<Mediteerkussen> MEDITEERKUSSEN = BLOCKS.registerBlock("einde2_geest_spel_mediteerkussen", Mediteerkussen::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.6f).sound(SoundType.WOOL).noOcclusion());
    public static final DeferredItem<BlockItem> MEDITEERKUSSEN_ITEM = ITEMS.registerItem("einde2_geest_spel_mediteerkussen",
            p -> new nl.juiced.guhs.feature.balto.BaltoBlocks.LoreBlock(MEDITEERKUSSEN.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());

    /** The tile item of a master ("bumi" .. "miroh", "witte_lotus"). */
    public static Item tegel(String meester) {
        return TEGELS.get(meester).get();
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        GeestLijnen.register();
        GeestScenes.register();
        WitteLotus.register();
        Proeven.register();
        HeiBai.register();
        KohRaadsels.register();
        Chakras.register();
        Snelreis.register();
        GeestHints.registreer();
        GeestWezen.BIJ_KLIK.add(GeestSpel::opWezenKlik);
        Schijn.registreer(GeestToegang::lantaarnsUit);
        Schijn.registreer(GeestLagen::vul);
        NeoForge.EVENT_BUS.addListener(GeestToegang::opTick);
        NeoForge.EVENT_BUS.addListener(GeestToegang::opDimensie);
        NeoForge.EVENT_BUS.addListener(GeestToegang::opWeg);
        NeoForge.EVENT_BUS.addListener(GeestSpel::opTick);
        NeoForge.EVENT_BUS.addListener(GeestSpel::opDimensie);
        NeoForge.EVENT_BUS.addListener(GeestSpel::opUitloggen);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH, GeestSpel::opBlokKlik);
        NeoForge.EVENT_BUS.addListener(GeestSpel::opEntityKlik);
        NeoForge.EVENT_BUS.addListener(GeestLagen::opTick);
        NeoForge.EVENT_BUS.addListener(Snelreis::kussenTick);
        NeoForge.EVENT_BUS.addListener(Verschijningen::levelTick);
        NeoForge.EVENT_BUS.addListener(GeestActeurs::opLaden);
        NeoForge.EVENT_BUS.addListener(GeestSpelCommando::register);
    }

    public static void payloads(PayloadRegistrar registrar) {
        GeestSpelPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<Item> t : TEGELS.values()) {
            output.accept(new ItemStack(t.get()));
        }
        for (DeferredItem<? extends Item> i : List.of(EIKEL, KOH_MASKER, VEER, GOUDEN_ROL, KOH_SCHUB, MAANDRUPPEL, EERSTE_KNABBEL, MEDITEERKUSSEN_ITEM)) {
            output.accept(new ItemStack(i.get()));
        }
    }

    private E2GeestSpelFeature() {
    }
}
