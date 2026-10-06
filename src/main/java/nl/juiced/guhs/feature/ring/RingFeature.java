package nl.juiced.guhs.feature.ring;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
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
import nl.juiced.guhs.feature.barbecuether.GrillPortalBlock;
import nl.juiced.guhs.feature.kleding.KledingBronnen;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.verhaal.Halte;
import nl.juiced.guhs.feature.verhaal.Reiskaart;
import nl.juiced.guhs.feature.verhaal.Reiskaarten;
import nl.juiced.guhs.feature.verhaal.VerhaalGuh;
import nl.juiced.guhs.feature.verhaal.VerhaalGuhs;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.registry.ModItems;

/**
 * bbq2 (ring-kern): the core of "In de ban van de Knabbelring" (DESIGN_130 4). Resources: tools/features/ring.py (+ ring_*.py).
 * The seven chapter slices (ring-h1 .. ring-h6, ring-sausuman) build on what is here; their manual is
 * guhs_work130/reports/slice_ring-kern.md, their entry point is {@link Ring}.
 * <ul>
 *   <li>The ring ({@link KnabbelringItem}, {@link Ring}, {@link RingEvents}): its craving, wearing it (invisible to Mika's,
 *       seen by the Eye, hunted by the Nine: {@link Negen}, {@link KnekelRuiterEntity}), its weight near the mountain.</li>
 *   <li>Sam-guh ({@link Sam}): everybody's own companion; Guhdalf and the rest of the cast ({@link Cast}); Smikagol
 *       ({@link Smikagol}, {@link SmikagolEntity}): the guide, later a buddy who fishes.</li>
 *   <li>The three gifts ({@link Gaven}), the rest points ({@link Rustpunt}; structure guhs:ring_rustpunt) and the rule "back
 *       to your last rest point" ({@link Ring#terugNaarRustpunt}).</li>
 *   <li>The portal lock: the grill portal Guhmensie -> Barbecuether only works for a player who finished ALL of chapter 1
 *       (everybody, also who was there before); the way back is never blocked.</li>
 *   <li>The travel map "knabbelring", the title "Ringdrager", the four outfits and the end rewards ({@link RingBeloning}),
 *       the daily party hook, per-player visibility of story entities ({@link Zicht}), and one hidden, protected box per
 *       story structure ({@link Ring#sluier}; CONTRACT_130 13.9).</li>
 * </ul>
 * The fixed ids of CONTRACT_130 7 keep their field names: KNABBELRING, LICHTFLESJE, ELFENMANTELTJE, ELFENTOUW,
 * ELFENTOUW_HAAK(_ITEM), SMIKAGOL, KNEKEL_RUITER.
 */
public final class RingFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    // --- the ring and the gifts (fixed ids) ---------------------------------------------------------------------------------
    public static final DeferredItem<KnabbelringItem> KNABBELRING = ITEMS.registerItem("knabbelring", KnabbelringItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    public static final DeferredItem<Gaven.Lichtflesje> LICHTFLESJE = ITEMS.registerItem("lichtflesje", Gaven.Lichtflesje::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
    public static final DeferredItem<Gaven.Manteltje> ELFENMANTELTJE = ITEMS.registerItem("elfenmanteltje", Gaven.Manteltje::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
    public static final DeferredItem<Gaven.Touw> ELFENTOUW = ITEMS.registerItem("elfentouw", Gaven.Touw::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE).fireResistant());
    public static final DeferredBlock<Gaven.Haak> ELFENTOUW_HAAK = BLOCKS.registerBlock("elfentouw_haak", Gaven.Haak::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.5f).sound(SoundType.CHAIN).noCollision().noOcclusion()
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredItem<BlockItem> ELFENTOUW_HAAK_ITEM = ITEMS.registerSimpleBlockItem(ELFENTOUW_HAAK);

    // --- the rest of the trip -----------------------------------------------------------------------------------------------
    /** The rest fire of a rest point (it never burns anybody). */
    public static final DeferredBlock<Rustpunt.Vuur> RUSTVUUR = BLOCKS.registerBlock("ring_rustvuur", Rustpunt.Vuur::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.PODZOL).strength(2f).sound(SoundType.WOOD).noOcclusion().lightLevel(s -> 13)
                    .pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<BlockItem> RUSTVUUR_ITEM = ITEMS.registerSimpleBlockItem(RUSTVUUR);
    /** What Sam-guh cooks at a rest point. */
    public static final DeferredItem<Item> STOOFPOTJE = ITEMS.registerItem("ring_stoofpotje", Item::new,
            () -> new Item.Properties().stacksTo(16).food(new FoodProperties.Builder().nutrition(8).saturationModifier(0.8f).build()));
    /** The treat of the daily party in the Knabbelgouw. */
    public static final DeferredItem<Item> FEESTKNABBEL = ITEMS.registerItem("ring_feestknabbel", Item::new,
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(5).saturationModifier(0.6f).alwaysEdible().build()));
    /** Calls your own Smikagol (the buddy). */
    public static final DeferredItem<Smikagol.Botje> VISSENBOTJE = ITEMS.registerItem("ring_vissenbotje", Smikagol.Botje::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    // --- the creatures (fixed ids) ---------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<SmikagolEntity>> SMIKAGOL = ENTITY_TYPES.register("smikagol",
            () -> EntityType.Builder.of(SmikagolEntity::new, MobCategory.MISC).sized(0.7f, 0.75f).eyeHeight(0.5f).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("smikagol"))));
    public static final DeferredHolder<EntityType<?>, EntityType<KnekelRuiterEntity>> KNEKEL_RUITER = ENTITY_TYPES.register("knekel_ruiter",
            () -> EntityType.Builder.of(KnekelRuiterEntity::new, MobCategory.MISC).sized(1.1f, 2.7f).eyeHeight(2.3f).clientTrackingRange(10).fireImmune()
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("knekel_ruiter"))));
    public static final DeferredItem<SpawnEggItem> SMIKAGOL_SPAWN_EGG = ModItems.spawnEgg(ITEMS, "smikagol_spawn_egg", SMIKAGOL);
    public static final DeferredItem<SpawnEggItem> KNEKEL_RUITER_SPAWN_EGG = ModItems.spawnEgg(ITEMS, "knekel_ruiter_spawn_egg", KNEKEL_RUITER);

    /** The rest point structure (a small camp, all over the Barbecuether) and the title of the story. */
    public static final String RUSTPUNT = "ring_rustpunt", TITEL = "ringdrager";
    /** The lang key of the message at the locked portal (DESIGN_130 4, "PORTAL LOCK"). */
    public static final String PORTAAL_DICHT = "quest.guhs.ring.portaal_dicht";

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(SMIKAGOL.get(), SmikagolEntity.createAttributes().build());
            event.put(KNEKEL_RUITER.get(), KnekelRuiterEntity.createAttributes().build());
        });
        NeoForge.EVENT_BUS.register(RingEvents.class);
        NeoForge.EVENT_BUS.addListener(RingCommands::register);

        // the portal lock: Guhmensie -> Barbecuether only after ALL of chapter 1, for everybody (the hook is only ever
        // asked on the way there; the way back is never blocked). Other entities than players are let through.
        GrillPortalBlock.SLOTEN.add((level, entity) -> entity instanceof ServerPlayer p && !p.isSpectator() && !Ring.lijn(1).klaar(p)
                ? Component.translatable(PORTAAL_DICHT) : null);

        // the travel map of the Guhdex (the picture and the names: tools/features/ring.py reiskaart)
        Reiskaarten.registreer(new Reiskaart(Ring.REISKAART, Ring.GROEP, List.of(
                new Halte("ring_h1", 1, 28, 118, Ring.STRUCTUREN.get(0)), new Halte("ring_h2", 2, 70, 84, Ring.STRUCTUREN.get(1)),
                new Halte("ring_h3", 3, 104, 56, Ring.STRUCTUREN.get(2)), new Halte("ring_h4", 4, 140, 92, Ring.STRUCTUREN.get(3)),
                new Halte("ring_h5", 5, 184, 66, Ring.STRUCTUREN.get(4)), new Halte("ring_h6", 6, 224, 46, Ring.STRUCTUREN.get(5)),
                new Halte("ring_sausuman", 7, 196, 124, Ring.SAUSUMAN))));

        // the story as a whole: its end gives the rewards; every step lets the cast of that step appear for the player
        Verhaallijn einde = Ring.lijn(6);
        einde.opStap((p, oud, nieuw) -> {
            if (nieuw >= einde.stappen()) {
                RingBeloning.geef(p);
            }
        });
        for (int n = 1; n <= Ring.SAUSUMAN_NR; n++) {
            Ring.lijn(n).opStap((p, oud, nieuw) -> Zicht.kijkOpnieuw(p));
        }
        Verhaallijn eerste = Ring.lijn(1);
        eerste.opStap((p, oud, nieuw) -> {
            if (nieuw >= eerste.stappen() && oud < eerste.stappen()) {
                p.sendSystemMessage(Component.translatable("quest.guhs.ring.portaal_open").withStyle(ChatFormatting.GOLD));
            }
        });
        Titels.registreer(new Titels.Titel(TITEL, "gui.guhs.titels.naam." + TITEL, ChatFormatting.GOLD, "guhs:knabbelring", Ring::klaar));
        for (GuhClothes c : RingBeloning.KLEDING) {
            KledingBronnen.bron(c, RingBeloning.BRON);
        }

        // the characters
        Cast.registreer();
        VerhaalGuhs.opKlik(VerhaalGuh.SAM_GUH, Sam::klik);
        GuhHooks.doelen((guh, goals) -> {
            goals.addGoal(0, new Sam.Draag(guh));
            goals.addGoal(1, new Sam.Volg(guh));
        });
        GuhHooks.tick(RingEvents::guhTick);

        // the rest points: little camps all over the Barbecuether, in the Superkompas, whole for everybody
        SuperkompasItem.voegToe("barbecue", RUSTPUNT);
        Bescherming.registreer(RUSTPUNT, 1);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(KNABBELRING.get()));
        output.accept(new ItemStack(LICHTFLESJE.get()));
        output.accept(new ItemStack(ELFENMANTELTJE.get()));
        output.accept(new ItemStack(ELFENTOUW.get()));
        output.accept(new ItemStack(ELFENTOUW_HAAK_ITEM.get()));
        output.accept(new ItemStack(RUSTVUUR_ITEM.get()));
        output.accept(new ItemStack(STOOFPOTJE.get()));
        output.accept(new ItemStack(FEESTKNABBEL.get()));
        output.accept(new ItemStack(VISSENBOTJE.get()));
        output.accept(new ItemStack(SMIKAGOL_SPAWN_EGG.get()));
        output.accept(new ItemStack(KNEKEL_RUITER_SPAWN_EGG.get()));
    }

    private RingFeature() {
    }
}
