package nl.juiced.guhs.feature.tuintjes;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.boerderij.BoerderijItems;
import nl.juiced.guhs.feature.knus.Feesttaak;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.knus.KnusSignalen;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.Knusfeest;

/**
 * De guhtuintjes (2.8, slice boerderij): pots and raised beds with a guh face where you grow the three guh plants
 * (tools/features/tuintjes.py makes the resources).
 * <ul>
 *   <li>Blocks {@link GuhBloempotBlock guh_bloempot} and {@link GuhMoestuinbakBlock guh_moestuinbak} ({@link TuinBlock}):
 *       plant {@link TuinPlant seeds}, they grow in three steps, harvest when ripe: knabbelgraan (the bakery's dough),
 *       theekruid and guhbloemetjes (the tea house).</li>
 *   <li>The {@link GuhGieterItem guh_gieter}: watered plants grow much faster. Tamed guhs water the plants around them
 *       ({@link GuhGietGoal}, a GuhHooks goal) and guhs singing nearby make them grow ({@link KnusSignalen#opZang}).</li>
 *   <li>Five guhbloemetjes make a feestboeket: the FEESTBLOEMEN task of the Grote Knusfeest ({@link #feestboeketGemaakt}).</li>
 *   <li>Knus tab section "tuintjes" with the tuinboek ({@link TuintjesVoortgang}).</li>
 * </ul>
 */
public final class TuintjesFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    // --- blocks ------------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<GuhBloempotBlock> GUH_BLOEMPOT = BLOCKS.registerBlock("guh_bloempot", GuhBloempotBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.8f).sound(SoundType.DECORATED_POT).noOcclusion().randomTicks());
    public static final DeferredBlock<GuhMoestuinbakBlock> GUH_MOESTUINBAK = BLOCKS.registerBlock("guh_moestuinbak", GuhMoestuinbakBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD).noOcclusion().randomTicks().ignitedByLava());

    static {
        ITEMS.registerItem("guh_bloempot", p -> new BoerderijItems.LoreBlock(GUH_BLOEMPOT.get(), p));
        ITEMS.registerItem("guh_moestuinbak", p -> new BoerderijItems.LoreBlock(GUH_MOESTUINBAK.get(), p));
    }

    // --- items -------------------------------------------------------------------------------------------------------------
    public static final DeferredItem<GuhGieterItem> GUH_GIETER = ITEMS.registerItem("guh_gieter", GuhGieterItem::new, new Item.Properties());
    public static final DeferredItem<BoerderijItems.Lore> KNABBELZAADJES = ITEMS.registerItem("knabbelzaadjes", BoerderijItems.Lore::new, new Item.Properties());
    public static final DeferredItem<BoerderijItems.Lore> THEEKRUIDZAADJES = ITEMS.registerItem("theekruidzaadjes", BoerderijItems.Lore::new, new Item.Properties());
    public static final DeferredItem<BoerderijItems.Lore> GUHBLOEMZAADJES = ITEMS.registerItem("guhbloemzaadjes", BoerderijItems.Lore::new, new Item.Properties());
    public static final DeferredItem<BoerderijItems.Lore> KNABBELGRAAN = ITEMS.registerItem("knabbelgraan", BoerderijItems.Lore::new, new Item.Properties());
    public static final DeferredItem<BoerderijItems.Lore> THEEKRUID = ITEMS.registerItem("theekruid", BoerderijItems.Lore::new, new Item.Properties());
    public static final DeferredItem<BoerderijItems.Lore> GUHBLOEMETJE = ITEMS.registerItem("guhbloemetje", BoerderijItems.Lore::new, new Item.Properties());
    public static final DeferredItem<BoerderijItems.Lore> FEESTBOEKET = ITEMS.registerItem("feestboeket", BoerderijItems.Lore::new,
            new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));

    // --- particles and sounds --------------------------------------------------------------------------------------------
    /** A drop from the gieter (or from a guh that waters). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GIETERDRUPPEL = PARTICLES.register("gieterdruppel",
            () -> new SimpleParticleType(false));
    /** A green-gold sparkle: a plant grows a step. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GROEISPRANKEL = PARTICLES.register("groeisprankel",
            () -> new SimpleParticleType(false));
    public static final DeferredHolder<SoundEvent, SoundEvent> GIETER_GELUID = SOUNDS.register("tuintjes.gieter",
            () -> SoundEvent.createVariableRangeEvent(Guhs.id("tuintjes.gieter")));
    public static final DeferredHolder<SoundEvent, SoundEvent> OOGST_GELUID = SOUNDS.register("tuintjes.oogst",
            () -> SoundEvent.createVariableRangeEvent(Guhs.id("tuintjes.oogst")));

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        TuintjesVoortgang.register();
        GuhHooks.doelen((guh, goals) -> goals.addGoal(5, new GuhGietGoal(guh)));
        KnusSignalen.opZang(TuintjesFeature::zang);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.ItemCraftedEvent event) -> {
            if (event.getCrafting().is(FEESTBOEKET.get()) && event.getEntity() instanceof ServerPlayer player) {
                feestboeketGemaakt(player);
            }
        });
    }

    /** Guhs sing here: the plants around may grow a step; the nearest player (16 blocks) gets it on the Knus tab. */
    static void zang(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos, int radius) {
        zang(level, pos, radius, TuinBlock.KANS_ZANG);
    }

    /** {@link #zang(net.minecraft.server.level.ServerLevel, net.minecraft.core.BlockPos, int)} with its own chance (gametests). */
    static void zang(net.minecraft.server.level.ServerLevel level, net.minecraft.core.BlockPos pos, int radius, float kans) {
        int grew = TuinBlock.zang(level, pos, radius, kans);
        if (grew > 0) {
            var player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16, false);
            if (player instanceof ServerPlayer sp) {
                TuintjesVoortgang.zang(sp, grew);
            }
        }
    }

    /** A feestboeket was made (crafted): the FEESTBLOEMEN task of the Knusfeest, when the Burgemeester asked for it. */
    public static void feestboeketGemaakt(ServerPlayer player) {
        KnusVoortgang.tel(player, TuintjesVoortgang.FEESTBOEKET, 1);
        if (Knusfeest.open(player, Feesttaak.FEESTBLOEMEN)) {
            Knusfeest.gemaakt(player, Feesttaak.FEESTBLOEMEN);
            player.sendSystemMessage(Component.translatable("gui.guhs.tuintjes.feestboeket_klaar").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (var item : List.of(GUH_GIETER, KNABBELZAADJES, THEEKRUIDZAADJES, GUHBLOEMZAADJES, KNABBELGRAAN, THEEKRUID, GUHBLOEMETJE, FEESTBOEKET)) {
            output.accept(new ItemStack(item.get()));
        }
        output.accept(new ItemStack(GUH_BLOEMPOT.get()));
        output.accept(new ItemStack(GUH_MOESTUINBAK.get()));
    }

    private TuintjesFeature() {
    }
}
