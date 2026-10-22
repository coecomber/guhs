package nl.juiced.guhs.feature.safari.verblijf;

import java.util.function.Consumer;

import javax.annotation.Nullable;

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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhpixel.blok.LoreBlockItem;
import nl.juiced.guhs.feature.safari.SafariDier;

/**
 * Op Safari! step 2, slice S5: the verblijfsbord and the animals at home (design ontwerp-safaripark-en-bouwen.md 1.9, rounds
 * 5-8, 21, 27, 36, 38-39; contract SAFARI_CONTRACT.md "Stap 2 API").
 * <ul>
 *   <li>{@link VerblijfBlokken.Bord the verblijfsbord} + {@link VerblijfsbordBlockEntity}: one item per species
 *       ({@link VerblijfsbordItem}), a family of 4 {@link ThuisDier animals at home} (1 male, 1 female, 2 little ones; never
 *       more, never breeding, back to the park when the board goes), its menu ({@link VerblijfPayloads}, client.VerblijfScreen);</li>
 *   <li>happiness ({@link Tevredenheid}), gifts, petting and feeding (its food, or {@code #guhs:safarivoer}: double), the
 *       four toys ({@link VerblijfBlokken.Speeltje}), friends;</li>
 *   <li>the specials: the ostrich (ride it, giant eggs: {@link #REUZEN_EI} → {@link #REUZEN_OMELET}), the stork's deliveries
 *       ({@link Ooievaar}, {@link VerblijfBlokken.Bus the Ooievaarsbus}), the narwhal (and the other sea animals) need water;</li>
 *   <li>for the gift shop (slice S2): {@link VerblijfWinkel} (price, dex + 7-day cooldown, buying).</li>
 * </ul>
 * Data: data/guhs/safari/verblijf.json ({@link VerblijfData}); resources: tools/features/safari_verblijf.py.
 */
public final class VerblijfFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final DeferredBlock<VerblijfBlokken.Bord> BORD = BLOCKS.registerBlock("safari_verblijfsbord", VerblijfBlokken.Bord::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5f).sound(SoundType.WOOD).noOcclusion().pushReaction(PushReaction.BLOCK));
    public static final DeferredItem<VerblijfsbordItem> BORD_ITEM = ITEMS.registerItem("safari_verblijfsbord",
            p -> new VerblijfsbordItem(BORD.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix().stacksTo(16));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VerblijfsbordBlockEntity>> BORD_BE = BLOCK_ENTITIES.register("safari_verblijfsbord",
            () -> new BlockEntityType<>(VerblijfsbordBlockEntity::new, BORD.get()));
    /** Round 4 (L4): the terrarium a terrariumbord becomes at home (a glass tank; the little creatures live inside). No item of
     *  its own: the board item places it, and it drops the board again. */
    public static final DeferredBlock<VerblijfBlokken.Terrarium> TERRARIUM = BLOCKS.registerBlock("safari_terrarium", VerblijfBlokken.Terrarium::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0f).sound(SoundType.GLASS).noOcclusion().pushReaction(PushReaction.BLOCK)
                    .isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VerblijfsbordBlockEntity>> TERRARIUM_BE = BLOCK_ENTITIES.register("safari_terrarium",
            () -> new BlockEntityType<>((pos, state) -> new VerblijfsbordBlockEntity(VerblijfFeature.TERRARIUM_BE.get(), pos, state), TERRARIUM.get()));

    public static final DeferredBlock<VerblijfBlokken.Bus> BUS = BLOCKS.registerBlock("safari_ooievaarsbus", VerblijfBlokken.Bus::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(1.5f).sound(SoundType.WOOD).noOcclusion());
    public static final DeferredItem<BlockItem> BUS_ITEM = ITEMS.registerItem("safari_ooievaarsbus", p -> new LoreBlockItem(BUS.get(), p),
            () -> new Item.Properties().useBlockDescriptionPrefix());
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OoievaarsbusBlockEntity>> BUS_BE = BLOCK_ENTITIES.register("safari_ooievaarsbus",
            () -> new BlockEntityType<>(OoievaarsbusBlockEntity::new, BUS.get()));

    public static final DeferredBlock<VerblijfBlokken.Speeltje> SPEELBAL = speeltje("speelbal", Block.box(3, 0, 3, 13, 9, 13), MapColor.COLOR_PINK, SoundType.WOOL);
    public static final DeferredBlock<VerblijfBlokken.Speeltje> GLIJBAANTJE = speeltje("glijbaantje", Block.box(1, 0, 1, 15, 12, 15), MapColor.COLOR_LIGHT_BLUE, SoundType.WOOD);
    public static final DeferredBlock<VerblijfBlokken.Speeltje> KRABBOOM = speeltje("krabboom", Block.box(2, 0, 2, 14, 16, 14), MapColor.WOOD, SoundType.WOOL);
    public static final DeferredBlock<VerblijfBlokken.Speeltje> SCHOMMELBAND = speeltje("schommelband", Block.box(0, 0, 3, 16, 16, 13), MapColor.COLOR_BLACK, SoundType.WOOD);

    public static final DeferredItem<Item> REUZEN_EI = ITEMS.registerItem("safari_reuzen_ei", nl.juiced.guhs.feature.kaasmijn.LoreItem::new,
            () -> new Item.Properties().stacksTo(16));
    public static final DeferredItem<Item> REUZEN_OMELET = ITEMS.registerItem("safari_reuzen_omelet", nl.juiced.guhs.feature.kaasmijn.LoreItem::new,
            () -> new Item.Properties().food(new FoodProperties.Builder().nutrition(14).saturationModifier(0.9f).build()));

    public static final DeferredHolder<EntityType<?>, EntityType<ThuisDier>> THUISDIER = ENTITY_TYPES.register("safari_thuisdier",
            () -> EntityType.Builder.<ThuisDier>of(ThuisDier::new, MobCategory.CREATURE).sized(0.9f, 1.2f).eyeHeight(1.0f)
                    .clientTrackingRange(10).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("safari_thuisdier"))));

    private static DeferredBlock<VerblijfBlokken.Speeltje> speeltje(String naam, net.minecraft.world.phys.shapes.VoxelShape vorm, MapColor kleur, SoundType geluid) {
        DeferredBlock<VerblijfBlokken.Speeltje> b = BLOCKS.registerBlock("safari_" + naam, p -> new VerblijfBlokken.Speeltje(p, vorm),
                () -> BlockBehaviour.Properties.of().mapColor(kleur).strength(0.8f).sound(geluid).noOcclusion());
        ITEMS.registerItem("safari_" + naam, p -> new LoreBlockItem(b.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
        return b;
    }

    /** The toy block of a toy name in verblijf.json ("bal", "glijbaan", "krabboom", "schommel"), or null. */
    @Nullable
    public static Block speeltje(String naam) {
        return switch (naam) {
            case "bal" -> SPEELBAL.get();
            case "glijbaan" -> GLIJBAANTJE.get();
            case "krabboom" -> KRABBOOM.get();
            case "schommel" -> SCHOMMELBAND.get();
            default -> null;
        };
    }

    /** The toy's display key (gui.guhs.verblijf.speeltje.&lt;id&gt;) for a toy name. */
    public static String speeltjeId(String naam) {
        return switch (naam) {
            case "bal" -> "speelbal";
            case "glijbaan" -> "glijbaantje";
            case "schommel" -> "schommelband";
            default -> naam;
        };
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ENTITY_TYPES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(THUISDIER.get(), SafariDier.createAttributes().build()));
        NeoForge.EVENT_BUS.addListener(VerblijfFeature::breken);
        // test round 5: "Slaapplek aanwijzen" (the next click on a block inside the home)
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH,
                (net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) -> SlaapplekAanwijzen.klik(event));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) ->
                SlaapplekAanwijzen.weg(event.getEntity().getUUID()));
    }

    public static void payloads(PayloadRegistrar registrar) {
        VerblijfPayloads.register(registrar);
    }

    /** One board per species with a board (and the toys, the mailbox, the egg) in the creative tab. */
    public static void creative(Consumer<ItemStack> output) {
        for (String soort : VerblijfData.soorten()) {
            output.accept(VerblijfsbordItem.bord(soort));
        }
        ITEMS.getEntries().stream().filter(i -> i.get() != BORD_ITEM.get()).forEach(i -> output.accept(new ItemStack(i.get())));
    }

    /** Only the owner (or an op / creative player) breaks a verblijfsbord or someone's Ooievaarsbus. */
    private static void breken(BreakBlockEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer p) || p.isCreative()
                || p.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER)) {
            return;
        }
        var be = event.getLevel().getBlockEntity(event.getPos());
        java.util.UUID eigenaar = be instanceof VerblijfsbordBlockEntity b ? b.eigenaar() : be instanceof OoievaarsbusBlockEntity bus ? bus.eigenaar() : null;
        String naam = be instanceof VerblijfsbordBlockEntity b ? b.eigenaarNaam() : be instanceof OoievaarsbusBlockEntity bus ? bus.eigenaarNaam() : "";
        if (eigenaar != null && !eigenaar.equals(p.getUUID())) {
            event.setCanceled(true);
            p.sendOverlayMessage(Component.translatable("gui.guhs.verblijf.niet_jouw", naam).withStyle(ChatFormatting.RED));
        }
    }

    private VerblijfFeature() {
    }
}
