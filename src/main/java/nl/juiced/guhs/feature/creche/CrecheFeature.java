package nl.juiced.guhs.feature.creche;

import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
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
 * De Knuffelcreche (2.8, plein slot "creche" of the Knuffeldal town): a giant baby guh with a pacifier, where Juf Knuffel
 * looks after the babyguhtjes. Resources: tools/features/creche.py.
 * <ul>
 *   <li>The calm care round ({@link CrecheGame#startVerzorgen}): three babies wake up; give them a babyflesje, a schone
 *       luier, tuck them in with a knuffeldekentje and sing them a slaapliedje (a little rhythm game, 4 songs: the Knus
 *       collection "slaapliedjes"). While the Burgemeester's feesttaakje FEESTSLINGERS is open the babies knutsel
 *       feestslingers first (paper), and you get them at the end.</li>
 *   <li>The minigame "Babyguhtjes terugbrengen" ({@link CrecheGame#startTerugbrengen}): the babies crawl out of their
 *       guh_wiegjes, pick them up and put them back before they crawl off; points, a highscore (board "creche") and
 *       speenmunten for Juf Knuffel's shop.</li>
 *   <li>At home: a guh_wiegje you rock (baby guhs grow a little), a babyflesje for your own baby guhs, the speelkleed,
 *       the knuffeldekentje, and the baby clothes (babymutsje, rompertje, speenkettinkje).</li>
 * </ul>
 */
public final class CrecheFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    // --- blocks -------------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<WiegjeBlock> GUH_WIEGJE = BLOCKS.registerBlock("guh_wiegje", WiegjeBlock::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.8f).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
    public static final DeferredBlock<CrecheBlocks.Speelkleed> SPEELKLEED = BLOCKS.registerBlock("speelkleed", CrecheBlocks.Speelkleed::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.1f).sound(SoundType.WOOL).ignitedByLava());
    public static final DeferredBlock<CrecheBlocks.Feestslingers> FEESTSLINGERS = BLOCKS.registerBlock("feestslingers", CrecheBlocks.Feestslingers::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.2f).sound(SoundType.WOOL).noCollision().noOcclusion()
                    .pushReaction(PushReaction.DESTROY).ignitedByLava());

    public static final DeferredItem<BlockItem> GUH_WIEGJE_ITEM = ITEMS.registerItem("guh_wiegje",
            p -> new CrecheBlocks.LoreBlock(GUH_WIEGJE.get(), p));
    public static final DeferredItem<BlockItem> SPEELKLEED_ITEM = ITEMS.registerItem("speelkleed", p -> new CrecheBlocks.LoreBlock(SPEELKLEED.get(), p));
    public static final DeferredItem<BlockItem> FEESTSLINGERS_ITEM = ITEMS.registerItem("feestslingers",
            p -> new CrecheBlocks.LoreBlock(FEESTSLINGERS.get(), p), () -> new Item.Properties().rarity(Rarity.UNCOMMON));

    // --- items --------------------------------------------------------------------------------------------------------------
    /** The Knuffelcreche's coin: earned by bringing babies back (and by the care round); spent at Juf Knuffel. */
    public static final DeferredItem<Item> SPEENMUNT = ITEMS.registerItem("speenmunt", CrecheBlocks.Lore::new, () -> new Item.Properties());
    public static final DeferredItem<Item> BABYFLESJE = ITEMS.registerItem("babyflesje", CrecheBlocks.Lore::new, () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> SCHONE_LUIER = ITEMS.registerItem("schone_luier", CrecheBlocks.Lore::new, () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> KNUFFELDEKENTJE = ITEMS.registerItem("knuffeldekentje", CrecheBlocks.Lore::new, () -> new Item.Properties().stacksTo(1));

    // --- the babies ---------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<CrecheBabyguh>> BABYGUH = ENTITY_TYPES.register("creche_babyguh",
            () -> EntityType.Builder.of(CrecheBabyguh::new, MobCategory.MISC).sized(0.45f, 0.4f).eyeHeight(0.3f).clientTrackingRange(8)
                    .updateInterval(1).noSave().build(ResourceKey.create(Registries.ENTITY_TYPE, Identifier.parse("guhs:creche_babyguh"))));

    // --- particles, sounds --------------------------------------------------------------------------------------------------
    /** A little sleepy star rising from a baby that sleeps. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SLAAPSTERRETJE = PARTICLES.register("slaapsterretje",
            () -> new SimpleParticleType(false));
    public static final DeferredHolder<SoundEvent, SoundEvent> SLAAPLIEDJE = sound("creche.slaapliedje");
    public static final DeferredHolder<SoundEvent, SoundEvent> BABYGIECHEL = sound("creche.babygiechel");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    /** Prices in speenmunten. */
    public static final int PRIJS_MUTSJE = 5, PRIJS_ROMPERTJE = 8, PRIJS_KETTINKJE = 4, PRIJS_WIEGJE = 3, PRIJS_DEKENTJE = 3;

    /** Juf Knuffel: talks (her screen), runs the care round and the minigame, keeps the shop. */
    public static final NpcRole JUF_KNUFFEL = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer player) {
            CrecheGame.talk(npc, player);
        }

        @Override
        public void tick(GuhNpcEntity npc) {
            CrecheGame.of(npc).tick(npc);
        }

        @Override
        public MerchantOffers offers(GuhNpcEntity npc) {
            return shop();
        }
    };

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(BABYGUH.get(), CrecheBabyguh.createAttributes().build()));
        Minigames.registerGame(Minigames.CRECHE, CrecheGame::isPlaying);
        NeoForge.EVENT_BUS.addListener(CrecheGame::onDamage);
        NeoForge.EVENT_BUS.addListener(CrecheGame::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(CrecheGame::onLogout);
        NeoForge.EVENT_BUS.addListener(CrecheGame::onChangeDimension);
        NeoForge.EVENT_BUS.addListener(CrecheGame::onServerStopped);
        GuhHooks.klik(CrecheFeature::klikOpGuh);
        CrecheVoortgang.register();
    }

    public static void payloads(PayloadRegistrar registrar) {
        CrechePayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(SPEENMUNT.get()));
        output.accept(new ItemStack(GUH_WIEGJE_ITEM.get()));
        output.accept(new ItemStack(SPEELKLEED_ITEM.get()));
        output.accept(new ItemStack(FEESTSLINGERS_ITEM.get()));
        output.accept(new ItemStack(BABYFLESJE.get()));
        output.accept(new ItemStack(SCHONE_LUIER.get()));
        output.accept(new ItemStack(KNUFFELDEKENTJE.get()));
    }

    @Nullable
    public static NpcRole role() {
        return JUF_KNUFFEL;
    }

    /** Juf Knuffel's shop: the baby clothes (only here), a crib, play mats and the care things for at home. */
    public static MerchantOffers shop() {
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(PRIJS_MUTSJE, new ItemStack(ModItems.clothingItem(GuhClothes.BABYMUTSJE))));
        offers.add(offer(PRIJS_ROMPERTJE, new ItemStack(ModItems.clothingItem(GuhClothes.ROMPERTJE))));
        offers.add(offer(PRIJS_KETTINKJE, new ItemStack(ModItems.clothingItem(GuhClothes.SPEENKETTINKJE))));
        offers.add(offer(PRIJS_WIEGJE, new ItemStack(GUH_WIEGJE_ITEM.get())));
        offers.add(offer(PRIJS_DEKENTJE, new ItemStack(KNUFFELDEKENTJE.get())));
        offers.add(offer(1, new ItemStack(SPEELKLEED_ITEM.get(), 4)));
        offers.add(offer(1, new ItemStack(BABYFLESJE.get(), 3)));
        offers.add(offer(1, new ItemStack(SCHONE_LUIER.get(), 3)));
        return offers;
    }

    private static MerchantOffer offer(int munten, ItemStack result) {
        return new MerchantOffer(new ItemCost(SPEENMUNT.get(), munten), result, Integer.MAX_VALUE, 0, 0);
    }

    /**
     * A babyflesje (or a schone luier) for your own baby guh: it drinks, gets hearts and grows up a bit, like feeding it
     * kaasknabbels (but it's only for babies: a grown guh says njeg).
     */
    static InteractionResult klikOpGuh(GuhEntity guh, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean fles = stack.is(BABYFLESJE.get()), luier = stack.is(SCHONE_LUIER.get());
        if (!fles && !luier) {
            return InteractionResult.PASS;
        }
        if (!guh.isBaby()) {
            if (!player.level().isClientSide()) {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("gui.guhs.creche.alleen_babys"));
            }
            return InteractionResult.SUCCESS;
        }
        if (!player.level().isClientSide()) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            if (fles) {
                guh.ageUp(AgeableMob.getSpeedUpSecondsWhenFeeding(-guh.getAge()), true);
                guh.level().playSound(null, guh, SoundEvents.GENERIC_DRINK, SoundSource.NEUTRAL, 0.7f, 1.6f);
            } else {
                guh.level().playSound(null, guh, SoundEvents.WOOL_PLACE, SoundSource.NEUTRAL, 0.8f, 1.4f);
            }
            guh.level().playSound(null, guh, BABYGIECHEL.get(), SoundSource.NEUTRAL, 0.8f, 1.5f);
            if (guh.level() instanceof ServerLevel server) {
                server.sendParticles(fles ? ParticleTypes.HEART : ParticleTypes.CLOUD, guh.getX(), guh.getY() + 0.6, guh.getZ(), 4, 0.25, 0.2, 0.25, 0.01);
            }
        }
        return InteractionResult.SUCCESS;
    }

    private CrecheFeature() {
    }
}
