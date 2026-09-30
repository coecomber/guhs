package nl.juiced.guhs.feature.knuffelbad;

import java.util.List;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.knus.KnusVoortgang;

/**
 * Het Knuffelbad (2.8): a big bath house with water slides on the guhzee coast. tools/features/knuffelbad.py (+
 * knuffelbad_*.py) makes the resources and the structure.
 * <ul>
 *   <li>Badmeester Bubbel ({@link Badmeester}): records, tips, the shop for eendjesmunten.</li>
 *   <li>The washing ritual ({@link Wasritueel}): guhshampoo, foam, the tub's shower, the guh-föhn: your guh is fluffy
 *       and shiny for a day (GuhHooks.GLANZEND, drawn by the client's GlansLaag).</li>
 *   <li>The three slides ({@link Glijbaan}, {@link GlijPad}, {@link GlijRit}): first person in a {@link ZwembandjeEntity},
 *       spinning funnels, a glowing star tunnel, a big plons; steer to pick up {@link BadeendjeEntity rubber ducks}
 *       ({@link Eendsoort}); a highscore per slide.</li>
 *   <li>The Knus tab ({@link KnuffelbadVoortgang}), the protection of the building ({@link KnuffelbadProtection}).</li>
 * </ul>
 */
public final class KnuffelbadFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    private static BlockBehaviour.Properties tegel(MapColor colour) {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_CONCRETE).mapColor(colour).strength(1.5f, 6f).sound(SoundType.STONE);
    }

    // --- blocks ------------------------------------------------------------------------------------------------------------
    public static final DeferredBlock<KnuffelbadBlocks.Wastobbe> GUH_WASTOBBE = BLOCKS.registerBlock("guh_wastobbe", KnuffelbadBlocks.Wastobbe::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(1.5f).sound(SoundType.WOOD).noOcclusion().ignitedByLava());
    public static final DeferredBlock<KnuffelbadBlocks.GlijbaanStart> GLIJBAAN_START = BLOCKS.registerBlock("glijbaan_start",
            KnuffelbadBlocks.GlijbaanStart::new, BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(-1f, 3600000f).noLootTable()
                    .lightLevel(s -> 10).noOcclusion().sound(SoundType.AMETHYST).isValidSpawn((s, l, p, e) -> false).pushReaction(PushReaction.BLOCK));
    public static final DeferredBlock<KnuffelbadBlocks.Glimtegel> GLIMTEGEL = BLOCKS.registerBlock("glimtegel", KnuffelbadBlocks.Glimtegel::new,
            tegel(MapColor.COLOR_BLUE).lightLevel(s -> s.getValue(KnuffelbadBlocks.FEL) ? 12 : 4).sound(SoundType.AMETHYST)
                    .isValidSpawn((s, l, p, e) -> false));
    public static final DeferredBlock<Block> TRECHTERTEGEL = BLOCKS.registerSimpleBlock("trechtertegel", tegel(MapColor.COLOR_PINK));
    public static final DeferredBlock<KnuffelbadBlocks.Glijgoot> GLIJGOOT = BLOCKS.registerBlock("knuffelbad_glijgoot", KnuffelbadBlocks.Glijgoot::new,
            tegel(MapColor.COLOR_PINK).friction(0.9f).lightLevel(s -> s.getValue(KnuffelbadBlocks.KLEUR) == KnuffelbadBlocks.Kleur.GLIM ? 3 : 0)
                    .isValidSpawn((s, l, p, e) -> false));
    public static final DeferredBlock<KnuffelbadBlocks.Schuim> SCHUIM_BLOK = BLOCKS.registerBlock("knuffelbad_schuim", KnuffelbadBlocks.Schuim::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(0.2f).sound(SoundType.WOOL).noOcclusion()
                    .isValidSpawn((s, l, p, e) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)
                    .pushReaction(PushReaction.DESTROY));
    public static final DeferredBlock<Block> BADTEGEL = BLOCKS.registerSimpleBlock("knuffelbad_badtegel", tegel(MapColor.SNOW));

    // --- items -------------------------------------------------------------------------------------------------------------
    /** The Knuffelbad's money: earned on the slides and by washing, spent at Badmeester Bubbel's. */
    public static final DeferredItem<Item> EENDJESMUNT = ITEMS.registerSimpleItem("eendjesmunt", new Item.Properties());
    public static final DeferredItem<KnuffelbadItems.Guhshampoo> GUHSHAMPOO = ITEMS.registerItem("guhshampoo", KnuffelbadItems.Guhshampoo::new,
            new Item.Properties().durability(16));
    public static final DeferredItem<KnuffelbadItems.GuhFohn> GUH_FOHN = ITEMS.registerItem("guh_fohn", KnuffelbadItems.GuhFohn::new,
            new Item.Properties().stacksTo(1));
    /** A rubber duck to put in your own pond (it floats and squeaks). */
    public static final DeferredItem<BadeendjeItem> BADEENDJE_ITEM = ITEMS.registerItem("knuffelbad_badeendje", BadeendjeItem::new,
            new Item.Properties().stacksTo(16));
    public static final DeferredItem<BlockItem> GUH_WASTOBBE_ITEM = ITEMS.registerSimpleBlockItem(GUH_WASTOBBE);
    public static final DeferredItem<BlockItem> GLIJBAAN_START_ITEM = ITEMS.registerSimpleBlockItem("glijbaan_start", GLIJBAAN_START,
            new Item.Properties().rarity(Rarity.EPIC));
    public static final DeferredItem<BlockItem> GLIMTEGEL_ITEM = ITEMS.registerSimpleBlockItem(GLIMTEGEL);
    public static final DeferredItem<BlockItem> TRECHTERTEGEL_ITEM = ITEMS.registerSimpleBlockItem(TRECHTERTEGEL);
    public static final DeferredItem<BlockItem> GLIJGOOT_ITEM = ITEMS.registerSimpleBlockItem(GLIJGOOT);
    public static final DeferredItem<BlockItem> SCHUIM_ITEM = ITEMS.registerSimpleBlockItem(SCHUIM_BLOK);
    public static final DeferredItem<BlockItem> BADTEGEL_ITEM = ITEMS.registerSimpleBlockItem(BADTEGEL);

    // --- entities ----------------------------------------------------------------------------------------------------------
    public static final DeferredHolder<EntityType<?>, EntityType<ZwembandjeEntity>> ZWEMBANDJE = ENTITY_TYPES.register("zwembandje",
            () -> EntityType.Builder.<ZwembandjeEntity>of(ZwembandjeEntity::new, MobCategory.MISC).sized(1.3f, 0.35f).clientTrackingRange(10)
                    .updateInterval(1).noSave().noSummon().build(Guhs.id("zwembandje").toString()));
    public static final DeferredHolder<EntityType<?>, EntityType<BadeendjeEntity>> BADEENDJE = ENTITY_TYPES.register("badeendje",
            () -> EntityType.Builder.<BadeendjeEntity>of(BadeendjeEntity::new, MobCategory.MISC).sized(0.45f, 0.45f).clientTrackingRange(8)
                    .updateInterval(2).build(Guhs.id("badeendje").toString()));

    // --- particles and sounds ------------------------------------------------------------------------------------------------
    /** A soap bubble: floats up, shimmering, and pops. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ZEEPBELLETJE = PARTICLES.register("zeepbelletje", () -> new SimpleParticleType(false));
    /** A flake of pink foam. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SCHUIMVLOKJE = PARTICLES.register("schuimvlokje", () -> new SimpleParticleType(false));
    /** A sparkle (a shiny guh, the star tunnel, a special duck): glows in the dark. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> GLINSTERING = PARTICLES.register("glinstering", () -> new SimpleParticleType(false));
    /** A splash drop (flies up and falls back, a PLONS makes lots of them). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PLONS = PARTICLES.register("plons", () -> new SimpleParticleType(true));

    public static final DeferredHolder<SoundEvent, SoundEvent> PLONS_GELUID = sound("knuffelbad.plons");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLIJDEN = sound("knuffelbad.glijden");
    public static final DeferredHolder<SoundEvent, SoundEvent> EENDJE_PIEP = sound("knuffelbad.eendje_piep");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCHUIM = sound("knuffelbad.schuim");
    public static final DeferredHolder<SoundEvent, SoundEvent> FOHN = sound("knuffelbad.fohn");
    public static final DeferredHolder<SoundEvent, SoundEvent> FLUIT = sound("knuffelbad.fluit");
    public static final DeferredHolder<SoundEvent, SoundEvent> SPETTER = sound("knuffelbad.spetter");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Guhs.id(name)));
    }

    private static final Badmeester BADMEESTER = new Badmeester();

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        ENTITY_TYPES.register(modBus);
        PARTICLES.register(modBus);
        SOUNDS.register(modBus);
        // one game at a time: riding a slide or busy washing
        Minigames.registerGame(Minigames.KNUFFELBAD, p -> GlijRit.rijdt(p) || Wasritueel.bezig(p));
        NeoForge.EVENT_BUS.addListener((LivingIncomingDamageEvent event) -> GlijRit.opSchade(event));
        NeoForge.EVENT_BUS.addListener((EntityMountEvent event) -> GlijRit.opAfstappen(event));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                GlijRit.spelerWeg(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                GlijRit.spelerWeg(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                GlijRit.spelerWeg(player);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerTickEvent.Post event) -> {
            if (event.getEntity() instanceof ServerPlayer player) {
                GlijRit.controleer(player);
                // arriving in the Knuffelbad counts as finding it (every 2 s, only until it is found)
                if (player.tickCount % 40 == 0 && !player.isSpectator()
                        && KnusVoortgang.teller(player, KnuffelbadVoortgang.BEZOCHT) < 1
                        && KnuffelbadProtection.beschermd(player.level(), player.blockPosition())) {
                    KnuffelbadVoortgang.gevonden(player);
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent event) -> {
            GlijRit.vergeetAlles();
            Wasritueel.vergeetAlles();
            KnuffelbadProtection.vergeetAlles();
        });
        KnuffelbadProtection.register();
        Wasritueel.register();
        KnuffelbadVoortgang.register();
    }

    public static void payloads(PayloadRegistrar registrar) {
        KnuffelbadPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        for (var item : List.of(EENDJESMUNT, GUHSHAMPOO, GUH_FOHN, BADEENDJE_ITEM, GUH_WASTOBBE_ITEM, GLIJBAAN_START_ITEM, GLIMTEGEL_ITEM, TRECHTERTEGEL_ITEM,
                GLIJGOOT_ITEM, SCHUIM_ITEM, BADTEGEL_ITEM)) {
            output.accept(new ItemStack(item.get()));
        }
    }

    /** The role of BADMEESTERGUH: Badmeester Bubbel. */
    @Nullable
    public static NpcRole role() {
        return BADMEESTER;
    }

    private KnuffelbadFeature() {
    }
}
