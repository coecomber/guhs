package nl.juiced.guhs.feature.beroepen;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import nl.juiced.guhs.block.VerstopBlocks;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.kleding.KledingBronnen;

/**
 * De beroepen (2.9, slice beroepen): four guh characters who each have one job for you, once. tools/features/beroepen.py
 * makes the resources (the Beroepenstraat piece of the Knuffeldal town, Bob's bouwplaats in guh village layout_c, the
 * models, textures, sounds, texts).
 * <ul>
 *   <li>{@link Brandweer} Brandweercommandant Blusguh (BRANDWEERGUH), Brandweerkazerne: the marshmallow campfires on the
 *       oefenterrein flare up; put them out with the loaned {@link BrandslangItem guh-brandslang}, then help a guhtje out
 *       of the tall tree. Reward: the fire helmet and the fire jacket.</li>
 *   <li>{@link Politie} Inspecteur Vahoegsma (POLITIEGUH), Politiebureautje: "De Knabbeldief-zaak". Vadsige paw prints
 *       ({@link PootafdrukBlock}) lead from the empty knabbelkluis to where the {@link KnabbeldiefMikaEntity} hides with the
 *       knabbel stock ({@link KnabbelbuitBlock}); it runs off giggling (it never does anything else). Reward: the police cap
 *       and the uniform.</li>
 *   <li>{@link Apotheek} Dokter Snotneus-guh (APOTHEKERGUH), Apotheekje: Snotje is snotterig. Pick three snotkruidjes in
 *       the kruidentuin ({@link SnotkruidBlock}), mix them with kaasmelk in the {@link MengketelBlock} into a
 *       kaasmelkdrankje, and give it to Snotje. Reward: the doctor's coat and the stethoscope.</li>
 *   <li>{@link Bouw} Bob de Guhbouwer (BOUWVAKKERGUH), the half-built house in a guh village: bring planks and his lunch
 *       (kaasknabbels), then lay the dakpannen on the ghost tiles of his roof ({@link DakpanItem}). Reward: the builder's
 *       helmet and the safety vest.</li>
 * </ul>
 * Every job is one-time per player ({@link BeroepenVoortgang}); afterwards the character only thanks you. The quests are
 * no game: they don't lock you out of the minigames (registered as "never playing").
 */
public final class BeroepenFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    // --- blocks --------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<MarshmallowvuurBlock> MARSHMALLOWVUUR = BLOCKS.registerBlock("beroepen_marshmallowvuur", MarshmallowvuurBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(2.0f).sound(SoundType.STONE).noOcclusion()
                    .lightLevel(s -> s.getValue(MarshmallowvuurBlock.VUUR) * 5));
    public static final DeferredBlock<VerstopBlocks.Marker> GUHTJEPLEK = marker("beroepen_guhtjeplek");
    public static final DeferredBlock<VerstopBlocks.Marker> KLUISPLEK = marker("beroepen_kluisplek");
    public static final DeferredBlock<VerstopBlocks.Marker> VERSTOPPLEK = marker("beroepen_verstopplek");
    public static final DeferredBlock<PootafdrukBlock> POOTAFDRUK = BLOCKS.registerBlock("beroepen_pootafdruk", PootafdrukBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).noCollission().instabreak().noLootTable().sound(SoundType.WOOL)
                    .pushReaction(PushReaction.DESTROY).replaceable());
    public static final DeferredBlock<KnabbelbuitBlock> KNABBELBUIT = BLOCKS.registerBlock("beroepen_knabbelbuit", KnabbelbuitBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN).strength(0.8f).sound(SoundType.WOOL).noOcclusion().noLootTable());
    public static final DeferredBlock<SnotkruidBlock> SNOTKRUID = BLOCKS.registerBlock("beroepen_snotkruid", SnotkruidBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollission().instabreak().randomTicks().sound(SoundType.CROP)
                    .pushReaction(PushReaction.DESTROY).noLootTable());
    public static final DeferredBlock<MengketelBlock> MENGKETEL = BLOCKS.registerBlock("beroepen_mengketel", MengketelBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(2.0f).sound(SoundType.COPPER).noOcclusion());
    /** A see-through tile on Bob's roof where a dakpan still has to go (can't be broken in survival). */
    public static final DeferredBlock<DakplekBlock> DAKPLEK = BLOCKS.registerBlock("beroepen_dakplek", DakplekBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(-1.0f, 3600000.0f).noLootTable().noOcclusion()
                    .sound(SoundType.WOOD).isValidSpawn((s, l, p, e) -> false).isRedstoneConductor((s, l, p) -> false)
                    .isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false));
    /** A dakpan laid on Bob's roof (stays put: only Bob takes them off, when the Mika's have "borrowed" them). */
    public static final DeferredBlock<net.minecraft.world.level.block.Block> DAKPAN = BLOCKS.registerSimpleBlock("beroepen_dakpan",
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE).strength(-1.0f, 3600000.0f).noLootTable().sound(SoundType.DECORATED_POT));

    private static DeferredBlock<VerstopBlocks.Marker> marker(String name) {
        return BLOCKS.registerBlock(name, VerstopBlocks.Marker::new, BlockBehaviour.Properties.of().noCollission().noLootTable()
                .strength(-1.0f, 3600000.0f).noOcclusion().replaceable().pushReaction(PushReaction.BLOCK));
    }

    // --- items ---------------------------------------------------------------------------------------------------------
    public static final DeferredItem<BrandslangItem> GUH_BRANDSLANG = ITEMS.registerItem("guh_brandslang", BrandslangItem::new,
            new Item.Properties().stacksTo(1));
    public static final DeferredItem<BeroepenItems.Drankje> KAASMELKDRANKJE = ITEMS.registerItem("kaasmelkdrankje", BeroepenItems.Drankje::new,
            new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE));
    public static final DeferredItem<BeroepenItems.Snotkruidje> SNOTKRUIDJE = ITEMS.registerItem("snotkruidje", BeroepenItems.Snotkruidje::new,
            new Item.Properties());
    public static final DeferredItem<DakpanItem> DAKPAN_ITEM = ITEMS.registerItem("beroepen_dakpan", p -> new DakpanItem(DAKPAN.get(), p),
            new Item.Properties());
    public static final DeferredItem<BlockItem> MARSHMALLOWVUUR_ITEM = ITEMS.registerItem("beroepen_marshmallowvuur",
            p -> new BeroepenItems.LoreBlock(MARSHMALLOWVUUR.get(), p), new Item.Properties());
    public static final DeferredItem<BlockItem> MENGKETEL_ITEM = ITEMS.registerItem("beroepen_mengketel",
            p -> new BeroepenItems.LoreBlock(MENGKETEL.get(), p), new Item.Properties());
    public static final DeferredItem<BlockItem> KNABBELBUIT_ITEM = ITEMS.registerItem("beroepen_knabbelbuit",
            p -> new BeroepenItems.LoreBlock(KNABBELBUIT.get(), p), new Item.Properties());

    // --- the Knabbeldief -----------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<KnabbeldiefMikaEntity>> KNABBELDIEF_MIKA = ENTITY_TYPES.register("knabbeldief_mika",
            () -> EntityType.Builder.of(KnabbeldiefMikaEntity::new, MobCategory.MISC).sized(0.6f, 0.55f).eyeHeight(0.4f)
                    .clientTrackingRange(10).build(Guhs.id("knabbeldief_mika").toString()));

    // --- sounds --------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> SIRENE = sound("beroepen.sirene");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPUITEN = sound("beroepen.spuiten");
    public static final DeferredHolder<SoundEvent, SoundEvent> SISSEN = sound("beroepen.sissen");
    public static final DeferredHolder<SoundEvent, SoundEvent> HATSJOE = sound("beroepen.hatsjoe");
    public static final DeferredHolder<SoundEvent, SoundEvent> HAMER = sound("beroepen.hamer");
    public static final DeferredHolder<SoundEvent, SoundEvent> BUBBEL = sound("beroepen.bubbel");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(KNABBELDIEF_MIKA.get(), KnabbeldiefMikaEntity.createAttributes().build()));
        // the jobs are no game: they never keep you from a minigame and don't protect you (see the class comment)
        Minigames.registerGame(Minigames.BEROEPEN, p -> false);
        NeoForge.EVENT_BUS.register(BeroepenEvents.class);
        for (BeroepenVoortgang.Beroep beroep : BeroepenVoortgang.Beroep.values()) {
            for (GuhClothes c : beroep.kleding()) {
                KledingBronnen.bron(c, beroep.bron());
            }
        }
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (var item : List.of(KAASMELKDRANKJE, SNOTKRUIDJE, MARSHMALLOWVUUR_ITEM, MENGKETEL_ITEM, KNABBELBUIT_ITEM)) {
            output.accept(new ItemStack(item.get()));
        }
    }

    /** The role of a beroepen character: Blusguh, Vahoegsma, Snotneus-guh or Bob. */
    @Nullable
    public static NpcRole role(GuhNpcEntity.Kind kind) {
        return switch (kind) {
            case BRANDWEERGUH -> Brandweer.ROLE;
            case POLITIEGUH -> Politie.ROLE;
            case APOTHEKERGUH -> Apotheek.ROLE;
            case BOUWVAKKERGUH -> Bouw.ROLE;
            default -> null;
        };
    }

    private BeroepenFeature() {
    }
}
