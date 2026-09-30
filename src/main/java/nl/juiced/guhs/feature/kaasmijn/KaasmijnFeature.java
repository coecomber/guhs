package nl.juiced.guhs.feature.kaasmijn;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * De kaasmijn: a very rare structure in the Guhmension (tools/features/kaasmijn.py builds it). A giant guh head with a
 * hard hat on the surface, and under it a cheese mine on two levels: tunnels with a powered rail loop (the Kaasexpress),
 * caverns full of cheese veins, a kaassaus lake and a treasure room with the kaaskluis.
 * <ul>
 *   <li>The Mijnguh ({@link Mijnguh}) lends you a pickaxe ({@link LeenhouweelItem}; it goes back when you leave the mine)
 *       and sells the miner outfit, only there.</li>
 *   <li>Cheese veins ({@link KaasaderBlock}) drop kaasbrokken and goudkaas; mined out they slowly grow back.</li>
 *   <li>The mine itself can't be broken or built in ({@link KaasmijnProtection}); only the veins can be mined.</li>
 * </ul>
 */
public final class KaasmijnFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    // --- the cheese veins and what's left of them ---------------------------------------------------------------------
    public static final DeferredBlock<KaasaderBlock> KAASADER = BLOCKS.registerBlock("kaasader",
            p -> new KaasaderBlock(UniformInt.of(0, 2), () -> KaasmijnFeature.UITGEMIJNDE_KAASADER.get(), p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).mapColor(MapColor.GOLD));
    public static final DeferredBlock<KaasaderBlock> DIEPE_KAASADER = BLOCKS.registerBlock("diepe_kaasader",
            p -> new KaasaderBlock(UniformInt.of(1, 3), () -> KaasmijnFeature.UITGEMIJNDE_DIEPE_KAASADER.get(), p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).mapColor(MapColor.GOLD));
    public static final DeferredBlock<KaasaderBlock> GOUDEN_KAASADER = BLOCKS.registerBlock("gouden_kaasader",
            p -> new KaasaderBlock(UniformInt.of(3, 7), () -> KaasmijnFeature.UITGEMIJNDE_GOUDEN_KAASADER.get(), p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).mapColor(MapColor.GOLD).lightLevel(s -> 4));
    public static final DeferredBlock<KaasaderBlock.MinedOut> UITGEMIJNDE_KAASADER = BLOCKS.registerBlock("uitgemijnde_kaasader",
            p -> new KaasaderBlock.MinedOut(() -> KaasmijnFeature.KAASADER.get(), 1, p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.STONE).randomTicks().noLootTable());
    public static final DeferredBlock<KaasaderBlock.MinedOut> UITGEMIJNDE_DIEPE_KAASADER = BLOCKS.registerBlock("uitgemijnde_diepe_kaasader",
            p -> new KaasaderBlock.MinedOut(() -> KaasmijnFeature.DIEPE_KAASADER.get(), 1, p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).randomTicks().noLootTable());
    /** Gold cheese is slow: it takes about three times as long to grow back. */
    public static final DeferredBlock<KaasaderBlock.MinedOut> UITGEMIJNDE_GOUDEN_KAASADER = BLOCKS.registerBlock("uitgemijnde_gouden_kaasader",
            p -> new KaasaderBlock.MinedOut(() -> KaasmijnFeature.GOUDEN_KAASADER.get(), 3, p),
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE).randomTicks().noLootTable());

    // --- the vault in the treasure room, the cart dispensers of the Kaasexpress -----------------------------------------
    public static final DeferredBlock<KaaskluisBlock> KAASKLUIS = BLOCKS.registerBlock("kaaskluis", KaaskluisBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.GOLD).strength(5f, 1200f).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final DeferredBlock<KarretjesautomaatBlock> KARRETJESAUTOMAAT = BLOCKS.registerBlock("karretjesautomaat",
            KarretjesautomaatBlock::new, () -> BlockBehaviour.Properties.ofFullCopy(Blocks.SPRUCE_PLANKS));

    // --- items ------------------------------------------------------------------------------------------------------------
    public static final DeferredItem<Item> KAASBROK = ITEMS.registerSimpleItem("kaasbrok",
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.4f).build()));
    public static final DeferredItem<LoreItem> GOUDKAAS = ITEMS.registerItem("goudkaas", LoreItem::new, () -> new Item.Properties().rarity(Rarity.UNCOMMON)
            .food(new FoodProperties.Builder().nutrition(6).saturationModifier(1.2f).alwaysEdible().build(),
                    net.minecraft.world.item.component.Consumables.defaultFood()
                    .onConsume(new net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1), 1f))
                    .onConsume(new net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 0), 1f)).build()));
    public static final DeferredItem<LeenhouweelItem> LEENHOUWEEL = ITEMS.registerItem("leenhouweel",
            LeenhouweelItem::new,
            () -> new Item.Properties().pickaxe(net.minecraft.world.item.ToolMaterial.IRON, 1.0f, -2.8f)
                    .component(DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<KaashouweelItem> KAASHOUWEEL = ITEMS.registerItem("kaashouweel",
            KaashouweelItem::new,
            () -> new Item.Properties().pickaxe(KaashouweelItem.TIER, 1.0f, -2.8f).repairable(KAASBROK.get()).rarity(Rarity.RARE));

    static {
        for (DeferredBlock<?> block : java.util.List.of(KAASADER, DIEPE_KAASADER, GOUDEN_KAASADER, UITGEMIJNDE_KAASADER,
                UITGEMIJNDE_DIEPE_KAASADER, UITGEMIJNDE_GOUDEN_KAASADER)) {
            ITEMS.registerSimpleBlockItem(block);
        }
        ITEMS.registerItem("kaaskluis", p -> new LoreItem.Block(KAASKLUIS.get(), p));
        ITEMS.registerItem("karretjesautomaat", p -> new LoreItem.Block(KARRETJESAUTOMAAT.get(), p));
    }

    private static final Mijnguh MIJNGUH = new Mijnguh();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.addListener(KaasmijnProtection::onBreak);
        nl.juiced.guhs.feature.Protected.add(KaasmijnProtection::inMine);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event) -> KaasmijnProtection.TEST_AREAS.clear());
        NeoForge.EVENT_BUS.addListener(KaasmijnProtection::onPlace);
        NeoForge.EVENT_BUS.addListener(KaasmijnProtection::onUseBlock);
        NeoForge.EVENT_BUS.addListener(KaasmijnProtection::onUseItem);
        NeoForge.EVENT_BUS.addListener(KaasmijnProtection::onExplosion);
        NeoForge.EVENT_BUS.addListener(KaasmijnProtection::onInteractEntity);
        NeoForge.EVENT_BUS.addListener(KaasmijnProtection::onInteractEntityAt);
        NeoForge.EVENT_BUS.addListener(KaasmijnProtection::onMobGriefing);
        NeoForge.EVENT_BUS.addListener(KaasmijnFeature::onPlayerTick);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        // (not the loaner pickaxe: it only exists inside a mine)
        for (var item : java.util.List.of(KAASBROK, GOUDKAAS, KAASHOUWEEL)) {
            output.accept(new ItemStack(item.get()));
        }
        for (var block : java.util.List.of(KAASADER, DIEPE_KAASADER, GOUDEN_KAASADER, KAASKLUIS, KARRETJESAUTOMAAT)) {
            output.accept(new ItemStack(block.get()));
        }
    }

    @Nullable
    public static NpcRole role() {
        return MIJNGUH;
    }

    // --- the Kaasexpress: ride half a minute through the mine -----------------------------------------------------------

    public static final String RIDE_KEY = "guhs_kaasmijn_rit";
    public static final int RIDE_SECONDS = 30;

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0 && player.getVehicle() instanceof AbstractMinecart
                && KaasmijnProtection.inMine(player.level(), player.blockPosition())) {
            rideSecond(player);
        }
    }

    /** Another second in a cart in the mine (the ride counter survives dying and logging out). */
    public static void rideSecond(ServerPlayer player) {
        var saved = GuhQuests.saved(player);
        int seconds = saved.getIntOr(RIDE_KEY, 0);
        if (seconds >= RIDE_SECONDS) {
            return;
        }
        saved.putInt(RIDE_KEY, ++seconds);
        player.sendOverlayMessage(Component.translatable("quest.guhs.kaasmijn.ride", seconds, RIDE_SECONDS).withStyle(ChatFormatting.GOLD));
        if (seconds >= RIDE_SECONDS) {
            GuhAdvancements.grant(player, "kaasmijn_rondrit");
            player.sendSystemMessage(Component.translatable("quest.guhs.kaasmijn.ride_done").withStyle(ChatFormatting.GOLD));
        }
    }

    private KaasmijnFeature() {
    }
}
