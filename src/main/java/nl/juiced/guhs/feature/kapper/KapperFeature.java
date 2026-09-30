package nl.juiced.guhs.feature.kapper;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.block.GuhFurnitureBlock;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.registry.ModItems;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
/**
 * De Pluiskapper "Knip &amp; Vads" (2.8, plein slot kapper of the Knuffeldal town): Kapper Krulletje (KAPPERGUH), the
 * hairstyles (GuhClothes.Slot.HAAR: {@link Kapsel}, permanent) and the hair dyes ({@link Haarverf}), the kappersshow
 * minigame ({@link KappersShow}: give each customer the hairstyle on its picture, in time; krulmunten, highscore
 * "kapper"), the Knus collection "kapsels" ({@link KapperVoortgang}) and the Knusfeest task FEESTKAPSELS (a feest round
 * of the show gives the feestkapselset). Your own tamed guhs get hair with the kapsel items and dyes ({@link KapperHaar}).
 * Resources: tools/features/kapper.py (+ kapper_salon.py, kapper_tex.py).
 */
public final class KapperFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    // --- blocks ------------------------------------------------------------------------------------------------------------
    /** The kappersstoel: a pink barber's chair on a chrome foot (you can sit on it; the show's customers sit here). */
    public static final DeferredBlock<GuhFurnitureBlock> KAPPERSSTOEL = BLOCKS.registerBlock("kappersstoel",
            p -> new GuhFurnitureBlock(p, 0.5, new double[]{6, 0, 6, 10, 4, 10}, new double[]{3, 0, 3, 13, 1, 13}, new double[]{2, 4, 2, 14, 8, 14},
                    new double[]{2, 8, 11, 14, 19, 14}, new double[]{1, 8, 3, 3, 11, 11}, new double[]{13, 8, 3, 15, 11, 11}),
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.0f).sound(SoundType.METAL).noOcclusion());
    /** The haarwasbak: a pink basin with a golden tap (right-click: foam!). */
    public static final DeferredBlock<HaarwasbakBlock> HAARWASBAK = BLOCKS.registerBlock("haarwasbak", HaarwasbakBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(1.0f).sound(SoundType.STONE).noOcclusion());
    public static final DeferredItem<BlockItem> KAPPERSSTOEL_ITEM = ITEMS.registerSimpleBlockItem(KAPPERSSTOEL);
    public static final DeferredItem<BlockItem> HAARWASBAK_ITEM = ITEMS.registerSimpleBlockItem(HAARWASBAK);

    // --- items -------------------------------------------------------------------------------------------------------------
    /** The kapper's coin: earned in the kappersshow, spent at Kapper Krulletje (and one go at the grijpmachine). */
    public static final DeferredItem<Item> KRULMUNT = ITEMS.registerSimpleItem("krulmunt", new Item.Properties());
    /** The Knusfeest task FEESTKAPSELS: a box with clips, ribbons and glitter spray for the party hairdos. */
    public static final DeferredItem<Item> FEESTKAPSELSET = ITEMS.registerItem("feestkapselset", FeestkapselsetItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
    /** The kappersschaar (lent: tag guhs:loaned). */
    public static final DeferredItem<KappersschaarItem> KAPPERSSCHAAR = ITEMS.registerItem("kappersschaar", KappersschaarItem::new,
            new Item.Properties().stacksTo(1));
    /** haarverf_roze ... haarverf_regenboog. */
    public static final Map<Haarverf, DeferredItem<HaarverfItem>> HAARVERF = new EnumMap<>(Haarverf.class);

    static {
        for (Haarverf verf : Haarverf.values()) {
            HAARVERF.put(verf, ITEMS.registerItem(verf.id(), p -> new HaarverfItem(verf, p),
                    new Item.Properties().stacksTo(16).rarity(verf == Haarverf.REGENBOOG ? Rarity.RARE : Rarity.COMMON)));
        }
    }

    // --- the customer ----------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<KapperKlantEntity>> KAPPER_KLANT = ENTITY_TYPES.register("kapper_klant",
            () -> EntityType.Builder.of(KapperKlantEntity::new, MobCategory.MISC).sized(0.9f, 0.8f).eyeHeight(0.55f)
                    .clientTrackingRange(10).noSave().noSummon().build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("kapper_klant"))));

    // --- particles and sounds ----------------------------------------------------------------------------------------------
    /** A little tuft of cut hair, twirling down. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> HAARPLUKJE = PARTICLES.register("haarplukje", () -> new SimpleParticleType(false));
    /** A glittery curl (dye, a perfect hairdo). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> KRULGLITTER = PARTICLES.register("krulglitter", () -> new SimpleParticleType(false));
    public static final DeferredHolder<SoundEvent, SoundEvent> KNIP = sound("kapper.knip");
    public static final DeferredHolder<SoundEvent, SoundEvent> FOHN = sound("kapper.fohn");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    /** Kapper Krulletje: talks (the screen), runs the kappersshow and keeps the shop. */
    public static final NpcRole KRULLETJE = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer player) {
            KappersShow.talk(npc, player);
        }

        @Override
        public void tick(GuhNpcEntity npc) {
            KappersShow.of(npc).tick(npc);
        }

        @Override
        public MerchantOffers offers(GuhNpcEntity npc) {
            return shop();
        }
    };

    /** Prices in krulmunten. */
    public static final int PRIJS_KAPSEL = 6, PRIJS_VERF = 3, PRIJS_REGENBOOG = 8, PRIJS_CAPE = 10, PRIJS_MEUBEL = 4;

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(KAPPER_KLANT.get(), GuhEntity.createAttributes().build()));
        Minigames.registerGame(Minigames.KAPPER, KappersShow::isPlaying);
        GuhHooks.klik(KapperHaar::klik);
        GuhHooks.tick(KapperHaar::tick);
        KapperVoortgang.register();
        NeoForge.EVENT_BUS.addListener(KappersShow::onDamage);
        NeoForge.EVENT_BUS.addListener(KappersShow::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(KappersShow::onLogout);
        NeoForge.EVENT_BUS.addListener(KappersShow::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(KappersShow::onServerStopped);
    }

    public static void payloads(PayloadRegistrar registrar) {
        KapperPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KRULMUNT.get()));
        output.accept(new ItemStack(KAPPERSSCHAAR.get()));
        for (Kapsel k : Kapsel.values()) {
            output.accept(new ItemStack(ModItems.clothingItem(k.kleding)));
        }
        HAARVERF.values().forEach(v -> output.accept(new ItemStack(v.get())));
        output.accept(new ItemStack(ModItems.clothingItem(GuhClothes.KAPPERSCAPE)));
        output.accept(new ItemStack(KAPPERSSTOEL_ITEM.get()));
        output.accept(new ItemStack(HAARWASBAK_ITEM.get()));
        output.accept(new ItemStack(FEESTKAPSELSET.get()));
    }

    /** The role of KAPPERGUH: Kapper Krulletje. */
    @Nullable
    public static NpcRole role() {
        return KRULLETJE;
    }

    /** Krulletje's shop: the eight hairstyles, the eight dyes, the kapperscape and salon furniture, for krulmunten. */
    public static MerchantOffers shop() {
        MerchantOffers offers = new MerchantOffers();
        for (Kapsel k : Kapsel.values()) {
            offers.add(offer(PRIJS_KAPSEL, new ItemStack(ModItems.clothingItem(k.kleding))));
        }
        for (Haarverf v : Haarverf.values()) {
            offers.add(offer(v == Haarverf.REGENBOOG ? PRIJS_REGENBOOG : PRIJS_VERF, new ItemStack(HAARVERF.get(v).get())));
        }
        offers.add(offer(PRIJS_CAPE, new ItemStack(ModItems.clothingItem(GuhClothes.KAPPERSCAPE))));
        offers.add(offer(PRIJS_MEUBEL, new ItemStack(KAPPERSSTOEL_ITEM.get())));
        offers.add(offer(PRIJS_MEUBEL, new ItemStack(HAARWASBAK_ITEM.get())));
        return offers;
    }

    private static MerchantOffer offer(int munten, ItemStack result) {
        return new MerchantOffer(new ItemCost(KRULMUNT.get(), munten), result, Integer.MAX_VALUE, 0, 0);
    }

    private KapperFeature() {
    }
}
