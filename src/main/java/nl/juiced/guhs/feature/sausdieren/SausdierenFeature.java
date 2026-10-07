package nl.juiced.guhs.feature.sausdieren;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.barbecuether.BarbecuetherFeature;
import nl.juiced.guhs.feature.barbecuether.Kaasfrituursaus;
import nl.juiced.guhs.feature.spiesburcht.Brouwsel;
import nl.juiced.guhs.feature.spiesburcht.GuhdrankjeItem;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.wereld.Bescherming;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.item.SuperkompasItem;
import nl.juiced.guhs.quest.GuhDex;
import nl.juiced.guhs.registry.ModItems;

/**
 * bbq2 (sausdieren): the two sweet creatures of the frying-sauce sea of the Guhbarbecuether and everything around them.
 * <ul>
 *   <li>{@link SausloperEntity de Sausloper} (the strider): a guh on very long legs that walks over kaasfrituursaus, shivers
 *       on dry land, and is steered with {@link PindasausStokItem pindasaus aan een stok}. Wild ones can be tamed by every
 *       player who finished the questline of the Sausloper-stal ({@link #LIJN}).</li>
 *   <li>The Sausloper-stal (structure {@code guhs:sausloper_stal}, {@link Stal}) with the Verzorger-guh
 *       ({@link VerzorgerRol}): lure a Sausloper, win its trust, ride the test lap ({@link Proefrit}); a saddle and the
 *       stick are the reward. Everything is per player: the stable's own Sauslopers are never anybody's.</li>
 *   <li>{@link SausblubjeEntity het Sausblubje} (the magma cube): a bouncing blob of sauce in three sizes that splits when
 *       you hug or feed it and leaves {@link #BLUBROOM blubroom}; a small one fits in a jar ({@link #SAUSBLUBJE_POTJE}: the
 *       heart of the Blubkacheltje of the tech-bronnen slice).</li>
 *   <li>The Stuiterdrankje ({@link #STUITERDRANKJE}, Brouwsel BLUBROOM): you bounce instead of falling ({@link #STUITER}).</li>
 * </ul>
 * Resources: tools/features/sausdieren.py (the building: sausdieren_bouw.py, the models: sausdieren_modellen.py).
 */
public final class SausdierenFeature {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, Guhs.MODID);
    /** Random salt of this slice (CONTRACT_130 3: 2130NN50L+). */
    public static final long SALT = 21301250L;

    /** What a Sausloper is warm on (the sauce itself, lava, glowing coal, the heated floor of its stall). */
    public static final TagKey<Block> WARM = TagKey.create(Registries.BLOCK, Guhs.id("sausdieren/warm"));
    /** What a Sausloper follows and eats. */
    public static final TagKey<Item> SAUSLOPER_VOER = TagKey.create(Registries.ITEM, Guhs.id("sausdieren/sausloper_voer"));
    /** What a Sausblubje eats. */
    public static final TagKey<Item> BLUBJE_VOER = TagKey.create(Registries.ITEM, Guhs.id("sausdieren/sausblubje_voer"));

    // --- the creatures (fixed ids, CONTRACT_130 7) ---------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<SausblubjeEntity>> SAUSBLUBJE = ENTITY_TYPES.register("sausblubje",
            () -> EntityType.Builder.of(SausblubjeEntity::new, MobCategory.CREATURE).sized(0.56f, 0.5f).eyeHeight(0.3f).fireImmune()
                    .clientTrackingRange(8).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("sausblubje"))));
    public static final DeferredHolder<EntityType<?>, EntityType<SausloperEntity>> SAUSLOPER = ENTITY_TYPES.register("sausloper",
            () -> EntityType.Builder.of(SausloperEntity::new, MobCategory.CREATURE).sized(0.9f, 1.85f).eyeHeight(1.45f)
                    .passengerAttachments(1.78f).fireImmune().clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("sausloper"))));

    // --- items (blubroom and sausblubje_potje are fixed ids) --------------------------------------------------------------------
    /** What a Sausblubje leaves when it splits: for the Stuiterdrankje and the "Saus" tier recipes of Guh-technologie. */
    public static final DeferredItem<Item> BLUBROOM = ITEMS.registerItem("blubroom", LoreItem::new, () -> new Item.Properties());
    /** A small Sausblubje in a jar: use it on a block to let it out; the Blubkacheltje (tech-bronnen) runs on one. */
    public static final DeferredItem<Item> SAUSBLUBJE_POTJE = ITEMS.registerItem("sausblubje_potje", SausblubjePotjeItem::new,
            () -> new Item.Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE));
    /** Pindasaus aan een stok: a Sausloper follows it, and with a saddle on it goes where you look. */
    public static final DeferredItem<PindasausStokItem> PINDASAUS_STOK = ITEMS.registerItem("sausdieren_pindasaus_stok", PindasausStokItem::new,
            () -> new Item.Properties().durability(100));
    /** The Guhdrankje of blubroom. */
    public static final DeferredItem<GuhdrankjeItem> STUITERDRANKJE = ITEMS.registerItem("sausdieren_stuiterdrankje",
            p -> new GuhdrankjeItem(Brouwsel.BLUBROOM, p), () -> new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));
    public static final DeferredItem<SpawnEggItem> SAUSLOPER_SPAWN_EGG = ModItems.spawnEgg(ITEMS, "sausloper_spawn_egg", SAUSLOPER);
    public static final DeferredItem<SpawnEggItem> SAUSBLUBJE_SPAWN_EGG = ModItems.spawnEgg(ITEMS, "sausblubje_spawn_egg", SAUSBLUBJE);

    /** Stuiterblub: a fall makes you bounce instead of hurting (the Stuiterdrankje). */
    public static final DeferredHolder<MobEffect, MobEffect> STUITER = EFFECTS.register("sausdieren_stuiter",
            () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xE8A23C) {
            });

    // --- sounds (vanilla sounds, pitched, in sounds.json) ---------------------------------------------------------------------
    public static final DeferredHolder<SoundEvent, SoundEvent> SAUSLOPER_GELUID = sound("sausdieren.sausloper");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAUSLOPER_BLIJ = sound("sausdieren.sausloper.blij");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAUSLOPER_BIBBER = sound("sausdieren.sausloper.bibber");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAUSLOPER_STAP = sound("sausdieren.sausloper.stap");
    public static final DeferredHolder<SoundEvent, SoundEvent> SAUSLOPER_SMAK = sound("sausdieren.sausloper.smak");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLUBJE_PLOF = sound("sausdieren.sausblubje.plof");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLUBJE_BLUB = sound("sausdieren.sausblubje.blub");
    public static final DeferredHolder<SoundEvent, SoundEvent> BLUBJE_SPLITS = sound("sausdieren.sausblubje.splits");
    public static final DeferredHolder<SoundEvent, SoundEvent> POTJE = sound("sausdieren.potje");
    public static final DeferredHolder<SoundEvent, SoundEvent> POORTJE = sound("sausdieren.poortje");
    public static final DeferredHolder<SoundEvent, SoundEvent> STUITER_GELUID = sound("sausdieren.stuiter");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    // --- the questline of the stable (per player) ---------------------------------------------------------------------------------
    /** How many times a Sausloper of the stable wants to be fed before it trusts you (step 2). */
    public static final int VOER_NODIG = 3;
    /**
     * The Verzorger-guh's questline: 0 talk (you get the stick), 1 lure a Sausloper to him, 2 feed it {@link #VOER_NODIG}
     * pindascheutjes, 3 ride the test lap, 4 come back for the saddle; 5 = done: wild Sauslopers can be tamed.
     */
    public static final Verhaallijn LIJN = Verhaallijn.maak("sausloper", "barbecue").stappen(5).icoon("guhs:sausdieren_pindasaus_stok")
            .nodig((p, stap) -> stap == 2 ? List.of(Verhaallijn.nodig("guhs:pindascheutjes", voer(p), VOER_NODIG)) : List.of())
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:sausdieren_pindasaus_stok", stap(p) >= 1),
                    Verhaallijn.beloning("minecraft:saddle", stap(p) >= 5)))
            .doel((p, stap) -> Doel.structuur(BarbecuetherFeature.BARBECUETHER, Stal.STRUCTUUR, Component.translatable("structure.guhs.sausloper_stal")))
            .registreer();

    private static int stap(ServerPlayer p) {
        return LIJN.stap(p);
    }

    /** How often this player fed a Sausloper of the stable (step 2 of the questline). */
    public static int voer(ServerPlayer p) {
        return Math.min(VOER_NODIG, LIJN.teller(p, "voer"));
    }

    /** May this player tame wild Sauslopers (the questline of the stable is done)? */
    public static boolean magTemmen(ServerPlayer p) {
        return LIJN.klaar(p);
    }

    public static boolean isSaus(net.minecraft.world.level.material.FluidState fluid) {
        return !fluid.isEmpty() && (Kaasfrituursaus.isSauce(fluid.getType()) || fluid.is(net.minecraft.tags.FluidTags.LAVA));
    }

    /** A spawn spot "in the sauce" (what IN_LAVA is for the strider). */
    public static final SpawnPlacementType IN_SAUS = (level, pos, type) -> type != null && level.getWorldBorder().isWithinBounds(pos)
            && isSaus(level.getFluidState(pos));

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        SOUNDS.register(modBus);
        EFFECTS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> {
            event.put(SAUSLOPER.get(), SausloperEntity.createAttributes().build());
            event.put(SAUSBLUBJE.get(), SausblubjeEntity.createAttributes().build());
        });
        modBus.addListener((RegisterSpawnPlacementsEvent event) -> {
            event.register(SAUSLOPER.get(), IN_SAUS, Heightmap.Types.MOTION_BLOCKING, SausloperEntity::magSpawnen,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
            event.register(SAUSBLUBJE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, SausblubjeEntity::magSpawnen,
                    RegisterSpawnPlacementsEvent.Operation.REPLACE);
        });
        GuhDex.creaturePage(GuhVariant.SAUSLOPER, SAUSLOPER, 8);
        GuhDex.creaturePage(GuhVariant.SAUSBLUBJE, SAUSBLUBJE);
        Brouwsel.zetDrankje(Brouwsel.BLUBROOM, STUITERDRANKJE, SausdierenFeature::stuiterEffecten);
        // the stable: its entry in the Superkompas, nobody breaks it, the keeper and the residents also come to copies that miss them
        SuperkompasItem.voegToe("barbecue", Stal.STRUCTUUR);
        Bescherming.registreer(Stal.STRUCTUUR, 2);
        NpcRollen.zet(GuhNpcEntity.Kind.VERZORGERGUH, new VerzorgerRol());
        Bezetting.npc(Stal.VERZORGER, Stal.STRUCTUUR, null, Stal.NPC, GuhNpcEntity.Kind.VERZORGERGUH, null, Stal.NPC_YAW);
        for (int i = 0; i < Stal.BEWONERS.size(); i++) {
            Bezetting.wezen(Stal.BEWONER + i, Stal.STRUCTUUR, null, Stal.BEWONERS.get(i), (level, plek, draai) -> SausloperEntity.bewoner(level, plek), 20);
        }
        NeoForge.EVENT_BUS.register(SausdierenEvents.class);
        NeoForge.EVENT_BUS.addListener(SausdierenFeature::commando);
    }

    /** What a Stuiterdrankje gives: three minutes of bouncing, and a bit of extra jump. */
    public static List<MobEffectInstance> stuiterEffecten() {
        return List.of(new MobEffectInstance(STUITER, 3600, 0), new MobEffectInstance(MobEffects.JUMP_BOOST, 3600, 0));
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<? extends Item> item : List.of(BLUBROOM, SAUSBLUBJE_POTJE, PINDASAUS_STOK, STUITERDRANKJE, SAUSLOPER_SPAWN_EGG, SAUSBLUBJE_SPAWN_EGG)) {
            output.accept(new ItemStack(item.get()));
        }
    }

    /**
     * {@code /guhs sausdieren ...} (ops; for the AutoCheck script and dev checks): {@code proefrit} starts the test lap at the
     * Verzorger-guh nearest to you, {@code blubje <1-3>} puts a Sausblubje of that size in front of you, and in dev runs only
     * {@code stal} places the stable's template (with its sauce) at your feet, and {@code spawnproef speler|tel|weg} puts a
     * stand-in player at the command's spot, counts the wild creatures around it and takes it away again ({@link Spawnproef}).
     */
    private static void commando(RegisterCommandsEvent event) {
        var wortel = Commands.literal("sausdieren").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("proefrit").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    GuhNpcEntity npc = Stal.verzorger(p.level(), p.blockPosition(), 48);
                    if (npc == null) {
                        c.getSource().sendFailure(Component.literal("Geen Verzorger-guh binnen 48 blokken"));
                        return 0;
                    }
                    return Proefrit.start(p, npc) ? 1 : 0;
                }))
                .then(Commands.literal("blubje").then(Commands.argument("grootte", IntegerArgumentType.integer(1, 3)).executes(c -> {
                    CommandSourceStack s = c.getSource();
                    ServerLevel level = s.getLevel();
                    SausblubjeEntity blubje = SAUSBLUBJE.get().create(level, EntitySpawnReason.COMMAND);
                    if (blubje == null) {
                        return 0;
                    }
                    Vec3 voor = s.getPosition().add(Vec3.directionFromRotation(0, s.getRotation().y).scale(2.5));
                    blubje.setGrootte(IntegerArgumentType.getInteger(c, "grootte"));
                    blubje.snapTo(voor.x, voor.y, voor.z, 0, 0);
                    blubje.setPersistenceRequired();
                    level.addFreshEntity(blubje);
                    return 1;
                })));
        if (!FMLEnvironment.isProduction()) {
            wortel.then(Commands.literal("stal").executes(c -> {
                CommandSourceStack s = c.getSource();
                BlockPos hoek = BlockPos.containing(s.getPosition()).offset(-Stal.NPC.getX(), -Stal.G - 1, -Stal.NPC.getZ());
                boolean gelukt = Stal.plaats(s.getLevel(), hoek);
                s.sendSuccess(() -> Component.literal(gelukt ? "De Sausloper-stal staat op " + hoek.toShortString() : "Geen template guhs:sausloper_stal"), false);
                return gelukt ? 1 : 0;
            }));
            // (a headless dev server has no player, and without one the natural spawner never runs: a stand-in to count wild ones)
            wortel.then(Commands.literal("spawnproef")
                    .then(Commands.literal("speler").executes(c -> Spawnproef.speler(c.getSource())))
                    .then(Commands.literal("tel").executes(c -> Spawnproef.tel(c.getSource())))
                    .then(Commands.literal("weg").executes(c -> Spawnproef.weg(c.getSource()))));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(wortel));
    }

    private SausdierenFeature() {
    }
}
