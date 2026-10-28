package nl.juiced.guhs.feature.weer;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadskracht.VadskrachtFeature;

/**
 * 1.8.0 (Guhdromenland), TEST BUILD: the Weer vadser ({@link WeerVadserBlock}): its own weather in a circle of 60 blocks
 * ({@link WeerZones}), zon / regen / onweer always and eight guh-weertjes ({@link Weertje}) unlocked with weerpotjes
 * ({@link WeerPotjeItem}, {@link WeerUnlocks}). What the guh-weertjes do on the server: {@link WeerEffecten}; what you see:
 * client/WeerClient. Test commands: {@link WeerCommando} (/guhs weer ...).
 * <p>
 * Where the weerpotjes come from (oude content first; ontwerp-guhs180-dromen.md "Testbuild-feedback 1"): chest loot through
 * the loot modifiers data/guhs/loot_modifiers/weerpotje_*.json (about 1 in 8 chests; tools/features/weer.py: {@code BRONNEN}),
 * the Knabbelspelen (a won zeskamp: knabbelbuitje), the Guh-Circuit (a gold medal: regenboogweer), a constellation at the
 * telescope (sterrenregen) and the Reisbureau's "Regenboogje in een potje" (used on a Weer vadser; it stays). TODO (the dream
 * dimension does not exist yet): {@link #DROOMBRONNEN}.
 */
public final class WeerFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);

    /** What a Weer vadser asks while it makes a weather (tools/features/weer.py: KRACHT): without a Wolkenantenne. */
    public static final int KRACHT = 20;
    /**
     * 1.8.0 (vadskracht): what it asks per Wolkenantenne level (0 = none, 1..3 = Wolkenantenne I..III); the radius that goes
     * with it: {@link WeerZones#STRALEN} (10 / 20 / 35 / 60). tools/features/vadsbasis.py reads these.
     */
    public static final int[] KRACHTEN = {KRACHT, 30, 45, 70};

    /**
     * TODO 1.8.0 (Guhdromenland): the dream sources of the weerpotjes, still to be hooked in once the dream islands exist
     * (each gives its potje once: {@link WeerUnlocks#potjeEenmalig}).
     */
    public static final Map<Weertje, String> DROOMBRONNEN = Map.of(
            Weertje.PLUISJES, "Slaapliedjesheuvels (het liedje van Snoes)",
            Weertje.BELLEN, "Wolkenbadje (Snoes' eerste bubbelbad)",
            Weertje.STERREN, "Sterrenkussenveld (de vallende ster gevangen)",
            Weertje.KNUSMIST, "Maanmolentje (droomzand gemalen)",
            Weertje.HARTJES, "Herinneringseiland (de Guh-familiedag)",
            Weertje.REGENBOOG, "Snoes-finale",
            Weertje.KNABBEL, "Droomplein-fontein");

    public static final DeferredBlock<WeerVadserBlock> WEER_VADSER = BLOCKS.registerBlock("weer_vadser", WeerVadserBlock::new,
            () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_PINK).strength(3f, 6f));
    public static final DeferredItem<BlockItem> WEER_VADSER_ITEM = ITEMS.registerItem("weer_vadser", p -> new BlockItem(WEER_VADSER.get(), p) {
        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
            tooltip.accept(Component.translatable("block.guhs.weer_vadser.lore").withStyle(ChatFormatting.GRAY));
            tooltip.accept(Component.translatable("block.guhs.weer_vadser.lore.kracht", KRACHT).withStyle(ChatFormatting.DARK_PURPLE));
            tooltip.accept(Component.translatable("block.guhs.weer_vadser.lore.straal", WeerZones.straal(0), WeerZones.straal(3)).withStyle(ChatFormatting.DARK_AQUA));
        }
    }, () -> new Item.Properties().useBlockDescriptionPrefix());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WeerVadserBlockEntity>> WEER_VADSER_BE = BLOCK_ENTITIES.register(
            "weer_vadser", () -> new BlockEntityType<>(WeerVadserBlockEntity::new, WEER_VADSER.get()));

    /** 1.8.0 (vadskracht): the Wolkenantennes I, II and III (guhs:wolkenantenne_1.._3). */
    public static final java.util.List<DeferredBlock<WolkenantenneBlock>> ANTENNES = new java.util.ArrayList<>();
    public static final java.util.List<DeferredItem<BlockItem>> ANTENNE_ITEMS = new java.util.ArrayList<>();

    static {
        for (int i = 1; i <= 3; i++) {
            int niveau = i;
            DeferredBlock<WolkenantenneBlock> blok = BLOCKS.registerBlock("wolkenantenne_" + niveau, p -> new WolkenantenneBlock(niveau, p),
                    () -> BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS).mapColor(MapColor.SNOW).strength(2f, 6f).noOcclusion());
            ANTENNES.add(blok);
            ANTENNE_ITEMS.add(ITEMS.registerItem("wolkenantenne_" + niveau, p -> new BlockItem(blok.get(), p) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
                    tooltip.accept(Component.translatable("block.guhs.wolkenantenne.lore", WeerZones.straal(niveau), KRACHTEN[niveau]).withStyle(ChatFormatting.GRAY));
                    tooltip.accept(Component.translatable("block.guhs.wolkenantenne.lore.hoe").withStyle(ChatFormatting.DARK_AQUA));
                }
            }, () -> new Item.Properties().useBlockDescriptionPrefix()));
        }
    }

    /** The Wolkenantenne level of this block (1..3), or 0. */
    public static int antenneNiveau(net.minecraft.world.level.block.state.BlockState state) {
        return state.getBlock() instanceof WolkenantenneBlock a ? a.niveau : 0;
    }

    /** One weerpotje per guh-weertje (guhs:weerpotje_&lt;id&gt;). */
    public static final Map<Weertje, DeferredItem<WeerPotjeItem>> POTJES = new EnumMap<>(Weertje.class);

    static {
        for (Weertje w : Weertje.values()) {
            if (w.ontgrendelbaar()) {
                POTJES.put(w, ITEMS.registerItem("weerpotje_" + w.id(), p -> new WeerPotjeItem(w, p), () -> new Item.Properties().stacksTo(16)));
            }
        }
    }

    // what the guh-weertjes fall with (client: WeerClient; textures of the existing guh particles)
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PLUISJE = deeltje("weer_pluisje");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ZEEPBEL = deeltje("weer_zeepbel");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HARTJE = deeltje("weer_hartje");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> BLOESEM = deeltje("weer_bloesem");
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STER = deeltje("weer_ster");

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> deeltje(String id) {
        return PARTICLES.register(id, () -> new SimpleParticleType(true));
    }

    public static Item potje(Weertje w) {
        DeferredItem<WeerPotjeItem> p = POTJES.get(w);
        return p == null ? net.minecraft.world.item.Items.AIR : p.get();
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        PARTICLES.register(modBus);
        modBus.addListener((RegisterCapabilitiesEvent event) -> VadskrachtFeature.knoopCapability(event, WEER_VADSER_BE.get()));
        NeoForge.EVENT_BUS.addListener(WeerFeature::tick);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer sp) {
                WeerZones.stuur(sp);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer sp) {
                WeerZones.stuur(sp);
            }
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent event) -> WeerCommando.register(event));
    }

    private static void tick(ServerTickEvent.Post event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            WeerZones.tick(level);
            WeerEffecten.tick(level);
        }
    }

    public static void payloads(PayloadRegistrar registrar) {
        WeerPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(WEER_VADSER_ITEM.get()));
        for (DeferredItem<BlockItem> a : ANTENNE_ITEMS) {
            output.accept(new ItemStack(a.get()));
        }
        for (DeferredItem<WeerPotjeItem> p : POTJES.values()) {
            output.accept(new ItemStack(p.get()));
        }
    }

    private WeerFeature() {
    }
}
