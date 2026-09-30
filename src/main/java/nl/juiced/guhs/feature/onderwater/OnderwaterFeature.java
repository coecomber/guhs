package nl.juiced.guhs.feature.onderwater;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * De Guhbubbel (onderwater): one in the deep middle of every Diepe Guhzee (tools/features/onderwater.py builds it, the
 * sea itself is tools/features/diepzee.py; {@link GuhbubbelStructure} finds the spot): on the sea floor a glass bubble
 * dome shaped like a guh head, and on a little island above it the Duikpost.
 * <ul>
 *   <li>Getting there needs nothing of your own: the Duikpost on its island has a spiral staircase down to the dome, and
 *       a bubble lift brings you back up. The dome is full of air; dive doors lead out onto the reef.</li>
 *   <li>{@link KaaskoraalBlock kaaskoraal} (cheese coral) blows air bubbles: near it you breathe again under water
 *       ({@link #breathe}). {@link ReuzenschelpBlock Reuzenschelpen} (giant shells) now and then hold a pearl, the money of
 *       the Zeemeerguh's shop ({@link ZeemeerguhRole}): the {@link DuikhelmItem duikhelm} (water breathing) and the
 *       duikpakje for your guh (only sold there).</li>
 *   <li>The Zeemeerguh variant (a guh with a fish tail, {@link Zeemeerguh}) turns up now and then in the guh seas: it swims
 *       fast and can be ridden under water.</li>
 *   <li>The bubble can't be broken or built in ({@link OnderwaterProtection}): no leaks, ever.</li>
 * </ul>
 */
public final class OnderwaterFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Guhs.MODID);
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Guhs.MODID);

    /** The Guhbubbel's structure type: one bubble in the deep middle of every Diepe Guhzee ({@link GuhbubbelStructure}). */
    public static final DeferredHolder<StructureType<?>, StructureType<GuhbubbelStructure>> GUHBUBBEL_TYPE =
            STRUCTURE_TYPES.register("guhbubbel", () -> () -> GuhbubbelStructure.CODEC);
    public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Guhs.MODID);
    /** The water of the Diepe Guhzee ({@link DiepzeeWaterFeature}). */
    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>, DiepzeeWaterFeature> DIEPZEE_WATER =
            FEATURES.register("diepzee_water", DiepzeeWaterFeature::new);
    public static final DeferredRegister<net.minecraft.world.level.levelgen.placement.PlacementModifierType<?>> PLACEMENT_MODIFIERS =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, Guhs.MODID);
    /** Placement filter: not in the Guhbubbel ({@link OutsideStructureFilter}). */
    public static final DeferredHolder<net.minecraft.world.level.levelgen.placement.PlacementModifierType<?>,
            net.minecraft.world.level.levelgen.placement.PlacementModifierType<OutsideStructureFilter>> OUTSIDE_STRUCTURE =
            PLACEMENT_MODIFIERS.register("outside_structure", () -> () -> OutsideStructureFilter.CODEC);

    // --- blocks ------------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<KaaskoraalBlock> KAASKORAAL = BLOCKS.registerBlock("kaaskoraal", KaaskoraalBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).noCollission().instabreak().sound(SoundType.WET_GRASS)
                    .lightLevel(s -> 10).pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<net.minecraft.world.level.block.Block> KAASKORAALBLOK = BLOCKS.registerSimpleBlock("kaaskoraalblok",
            BlockBehaviour.Properties.ofFullCopy(Blocks.BRAIN_CORAL_BLOCK).mapColor(MapColor.GOLD).lightLevel(s -> 3));
    public static final DeferredBlock<net.minecraft.world.level.block.Block> PARELMOER = BLOCKS.registerSimpleBlock("parelmoer",
            BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE).mapColor(MapColor.QUARTZ).strength(1.2f, 6f));
    public static final DeferredBlock<net.minecraft.world.level.block.Block> PARELMOER_TEGELS = BLOCKS.registerSimpleBlock("parelmoer_tegels",
            BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE).mapColor(MapColor.QUARTZ).strength(1.2f, 6f));
    public static final DeferredBlock<ReuzenschelpBlock> REUZENSCHELP = BLOCKS.registerBlock("reuzenschelp", ReuzenschelpBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5f, 6f).sound(SoundType.BONE_BLOCK).randomTicks()
                    .noOcclusion().pushReaction(PushReaction.BLOCK));

    // --- items -------------------------------------------------------------------------------------------------------------
    /** The duikhelm's material: a brass helmet (only the helmet exists), repaired with pearls. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> DUIK = ARMOR_MATERIALS.register("duikhelm", () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorItem.Type type : ArmorItem.Type.values()) {
            defense.put(type, type == ArmorItem.Type.HELMET ? 2 : 0);
        }
        return new ArmorMaterial(defense, 12, SoundEvents.ARMOR_EQUIP_TURTLE, () -> Ingredient.of(OnderwaterFeature.PAREL.get()),
                List.of(new ArmorMaterial.Layer(Guhs.id("duikhelm"))), 0f, 0f);
    });
    public static final DeferredItem<Item> PAREL = ITEMS.registerItem("parel", LoreItem::new, new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<DuikhelmItem> DUIKHELM = ITEMS.registerItem("duikhelm", p -> new DuikhelmItem(DUIK, p),
            new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(20)).rarity(Rarity.UNCOMMON));

    static {
        ITEMS.registerItem("kaaskoraal", p -> new LoreItem.Block(KAASKORAAL.get(), p));
        for (DeferredBlock<?> block : List.of(KAASKORAALBLOK, PARELMOER, PARELMOER_TEGELS, REUZENSCHELP)) {
            ITEMS.registerSimpleBlockItem(block);
        }
    }

    private static final ZeemeerguhRole ZEEMEERGUH = new ZeemeerguhRole();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ARMOR_MATERIALS.register(modBus);
        STRUCTURE_TYPES.register(modBus);
        FEATURES.register(modBus);
        PLACEMENT_MODIFIERS.register(modBus);
        OnderwaterProtection.register();
        nl.juiced.guhs.feature.Protected.add(OnderwaterProtection::inBubble);
        NeoForge.EVENT_BUS.addListener(OnderwaterFeature::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(OnderwaterFeature::onMount);
        NeoForge.EVENT_BUS.addListener(Zeemeerguh::onLevelTick);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (var item : List.of(PAREL, DUIKHELM)) {
            output.accept(new ItemStack(item.get()));
        }
        for (var block : List.of(KAASKORAAL, KAASKORAALBLOK, PARELMOER, PARELMOER_TEGELS, REUZENSCHELP)) {
            output.accept(new ItemStack(block.get()));
        }
    }

    @Nullable
    public static NpcRole role() {
        return ZEEMEERGUH;
    }

    // --- kaaskoraal: air bubbles -------------------------------------------------------------------------------------------

    /** How close (blocks from your eyes) a kaaskoraal must be to breathe from it, and how much air one breath gives. */
    public static final int CORAL_RANGE = 2, AIR_PER_BREATH = 90;

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 10 == 0) {
            breathe(player);
        }
    }

    /** Under water with a kaaskoraal close by: a breath of air from its bubbles. Returns whether you got one. */
    public static boolean breathe(ServerPlayer player) {
        if (!underWater(player) || player.getAirSupply() >= player.getMaxAirSupply()) {
            return false;
        }
        BlockPos eye = BlockPos.containing(player.getEyePosition());
        BlockPos coral = null;
        for (BlockPos p : BlockPos.betweenClosed(eye.offset(-CORAL_RANGE, -CORAL_RANGE - 1, -CORAL_RANGE), eye.offset(CORAL_RANGE, CORAL_RANGE, CORAL_RANGE))) {
            if (player.level().getBlockState(p).is(KAASKORAAL.get())) {
                coral = p.immutable();
                break;
            }
        }
        if (coral == null) {
            return false;
        }
        boolean gasping = player.getAirSupply() < player.getMaxAirSupply() / 3;
        player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + AIR_PER_BREATH));
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, coral.getX() + 0.5, coral.getY() + 0.6, coral.getZ() + 0.5, 8, 0.2, 0.3, 0.2, 0.05);
        level.sendParticles(ParticleTypes.BUBBLE, player.getX(), player.getEyeY(), player.getZ(), 6, 0.25, 0.2, 0.25, 0.02);
        level.playSound(null, coral, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.BLOCKS, 0.8f, 1.2f);
        if (gasping) {
            player.displayClientMessage(Component.translatable("quest.guhs.onderwater.air").withStyle(ChatFormatting.AQUA), true);
        }
        GuhAdvancements.grant(player, "onderwater_lucht");
        return true;
    }

    /** Are this one's eyes under water (looked up in the world, so it's also right before the entity's first tick)? */
    public static boolean underWater(net.minecraft.world.entity.Entity entity) {
        BlockPos eye = BlockPos.containing(entity.getEyePosition());
        var fluid = entity.level().getFluidState(eye);
        return entity.isEyeInFluid(FluidTags.WATER)
                || fluid.is(FluidTags.WATER) && entity.getEyeY() < eye.getY() + fluid.getHeight(entity.level(), eye);
    }

    /** Getting on a Zeemeerguh: how to ride it. */
    private static void onMount(EntityMountEvent event) {
        if (event.isMounting() && event.getEntityMounting() instanceof ServerPlayer player && event.getEntityBeingMounted() instanceof GuhEntity guh
                && guh.isZeemeer()) {
            player.displayClientMessage(Component.translatable("quest.guhs.onderwater.ride").withStyle(ChatFormatting.AQUA), true);
        }
    }

    /** An item with one line of lore under its name (lang key: its own key + ".lore"). */
    public static class LoreItem extends Item {
        public LoreItem(Properties properties) {
            super(properties);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
            tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
        }

        /** The same for a block item. */
        public static class Block extends net.minecraft.world.item.BlockItem {
            public Block(net.minecraft.world.level.block.Block block, Properties properties) {
                super(block, properties);
            }

            @Override
            public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                tooltip.add(Component.translatable(getDescriptionId() + ".lore").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    private OnderwaterFeature() {
    }
}
