package nl.juiced.guhs.feature.vadsrijk.wereld;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.registry.ModCreativeTabs;

/**
 * Het Vadsrijk, slice "wereld" (CONTRACT_VADSRIJK 1 + 10): the hand-built map (tiles from tools/vadsrijk/maak_kaart.py, placed by
 * kern.wereld.KaartFeature), and the moving/living parts of it:
 * <ul>
 *   <li>{@link Portalen}: the sleeping portal of the ruined order temple in the Guhmension (worldgen structure
 *       {@code guhs:vr_w_portaaltempel}), the way back, and the craftable portal (Vadsrijk-poortsteen + Vadsrijk-vonk);</li>
 *   <li>{@link WereldDeuren}: the temple doors as moving blocks with a ceremony scene (hook {@code WereldDeuren.open(id, p)});</li>
 *   <li>{@link WereldReizen}: the airship line Knuffelkamp - De Haven ({@code WereldReizen.vlieg(p, van, naar)}, the ship's bell);</li>
 *   <li>{@link Uitkijken}: the panorama scene on the rock needles;</li>
 *   <li>{@link Littekens}: the grey dragon scars that heal per player (flag {@code extra("npc").zone1_klaar});</li>
 *   <li>client: the sky of zone 1, ambient sounds and particles, the scar tint ({@code wereld.client}).</li>
 * </ul>
 * All coordinates: {@link WereldPlekken} (generated, = POI_VADSRIJK.md).
 */
public final class WereldRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);

    public static final DeferredBlock<PortaalBlok> PORTAAL = BLOCKS.registerBlock("vr_w_portaal", PortaalBlok::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).noCollision().noLootTable().noOcclusion().strength(-1f, 3600000f)
                    .sound(SoundType.GLASS).lightLevel(s -> s.getValue(PortaalBlok.WAKKER) ? 11 : 4).pushReaction(PushReaction.BLOCK)
                    .isValidSpawn((s, l, p, e) -> false));
    public static final DeferredBlock<Block> POORTSTEEN = BLOCKS.registerSimpleBlock("vr_w_poortsteen",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(2.5f, 6f).sound(SoundType.STONE).lightLevel(s -> 3));
    public static final DeferredBlock<Block> LITTEKEN_GRAS = BLOCKS.registerSimpleBlock("vr_w_litteken_gras",
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(0.6f).sound(SoundType.GRASS));
    public static final DeferredBlock<PlantBlok> LITTEKEN_PLANT = BLOCKS.registerBlock("vr_w_litteken_plant", PlantBlok::new,
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).noCollision().instabreak().sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ).replaceable().pushReaction(PushReaction.DESTROY));

    public static final DeferredItem<BlockItem> POORTSTEEN_ITEM = ITEMS.registerSimpleBlockItem(POORTSTEEN);
    public static final DeferredItem<BlockItem> LITTEKEN_GRAS_ITEM = ITEMS.registerSimpleBlockItem(LITTEKEN_GRAS);
    public static final DeferredItem<BlockItem> LITTEKEN_PLANT_ITEM = ITEMS.registerSimpleBlockItem(LITTEKEN_PLANT);
    public static final DeferredItem<VonkItem> VONK = ITEMS.registerItem("vr_w_vonk", VonkItem::new, () -> new Item.Properties().stacksTo(16));

    private static long laatsteOpruim;

    private WereldRegistry() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener((BuildCreativeModeTabContentsEvent e) -> {
            if (e.getTabKey() == ModCreativeTabs.GUHS_TAB.getKey()) {
                e.accept(new ItemStack(POORTSTEEN_ITEM.get()));
                e.accept(new ItemStack(VONK.get()));
                e.accept(new ItemStack(LITTEKEN_GRAS_ITEM.get()));
                e.accept(new ItemStack(LITTEKEN_PLANT_ITEM.get()));
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            Later.tick(e.getServer());
            WereldDeuren.tick(e.getServer());
            ServerLevel sl = Vadsrijk.level(e.getServer());
            if (sl == null) {
                return;
            }
            Uitkijken.tick(sl);
            if (sl.getGameTime() % 40 == 0) {
                for (ServerPlayer p : sl.players()) {
                    Littekens.controleer(p, false);
                    Uitkijken.markeerBinnen(p);
                }
            }
            if (sl.getGameTime() - laatsteOpruim > 20 * 60) {
                laatsteOpruim = sl.getGameTime();
                nl.juiced.guhs.feature.vadsrijk.kern.wereld.KaartFeature.ruimOp(sl);   // (kern since the integration)
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> {
            if (!(e.getEntity() instanceof ServerPlayer p) || !Vadsrijk.in(p) || e.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
                return;
            }
            if (WereldDeuren.klik(p, e.getPos()) || WereldReizen.klik(p, e.getPos())) {
                e.setCanceled(true);
                e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                Littekens.controleer(p, true);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                Littekens.controleer(p, true);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            Littekens.vergeet(e.getEntity().getUUID());
            WereldReizen.vergeet(e.getEntity().getUUID());
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            Later.wis();
            WereldDeuren.wis();
        });
        NeoForge.EVENT_BUS.addListener(WereldRegistry::commandos);
    }

    public static void payloads(PayloadRegistrar registrar) {
        registrar.playToClient(Littekens.Vlag.TYPE, Littekens.Vlag.STREAM_CODEC, (v, c) -> Littekens.ontvanger.accept(v.klaar()));
        registrar.playToClient(WereldReizen.Info.TYPE, WereldReizen.Info.STREAM_CODEC, (v, c) -> WereldReizen.ontvanger.accept(v.overslaanTicks()));
        registrar.playToServer(WereldReizen.Overslaan.TYPE, WereldReizen.Overslaan.STREAM_CODEC, WereldReizen.Overslaan::handle);
    }

    // ------------------------------------------------------------------------------------------------ dev commands

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    private static void commandos(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> w = Commands.literal("wereld");
        w.then(dev("plek").then(Commands.argument("id", StringArgumentType.string()).suggests((c, b) -> {
            WereldPlekken.ALLE.keySet().forEach(b::suggest);
            return b.buildFuture();
        }).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            BlockPos b = WereldPlekken.ALLE.get(StringArgumentType.getString(c, "id"));
            ServerLevel sl = Vadsrijk.level(p.level().getServer());
            if (b == null || sl == null) {
                c.getSource().sendFailure(Component.literal("Onbekende plek (zie POI_VADSRIJK.md)"));
                return 0;
            }
            if (!Vadsrijk.in(p)) {
                VadsrijkCommando.naarBinnen(p, false);
            }
            p.teleport(new TeleportTransition(sl, Vec3.atBottomCenterOf(b), Vec3.ZERO, p.getYRot(), 0f, TeleportTransition.DO_NOTHING));
            return 1;
        })));
        w.then(dev("deur").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> {
            WereldPlekken.DEUREN.forEach(d -> b.suggest(d.id()));
            return b.buildFuture();
        }).then(Commands.literal("open").executes(c -> {
            return WereldDeuren.open(StringArgumentType.getString(c, "id"), c.getSource().getPlayerOrException()) ? 1 : 0;
        })).then(Commands.literal("dicht").executes(c -> {
            ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
            if (sl != null) {
                WereldDeuren.sluit(sl, StringArgumentType.getString(c, "id"), false);
            }
            return 1;
        })).then(Commands.literal("vergeet").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            String id = StringArgumentType.getString(c, "id");
            nl.juiced.guhs.feature.vadsrijk.kern.Spelers.data(p).gezien.remove("vr_w_deur_" + id);
            nl.juiced.guhs.feature.vadsrijk.kern.Spelers.data(p).extra("wereld").remove("deur_" + id);
            return 1;
        }))));
        w.then(dev("vlieg").then(Commands.literal("kamp").executes(c -> {
            WereldReizen.vlieg(c.getSource().getPlayerOrException(), WereldReizen.Dok.HAVEN, WereldReizen.Dok.KAMP, true);
            return 1;
        })).then(Commands.literal("haven").executes(c -> {
            WereldReizen.vlieg(c.getSource().getPlayerOrException(), WereldReizen.Dok.KAMP, WereldReizen.Dok.HAVEN, true);
            return 1;
        })));
        w.then(dev("littekens").then(Commands.literal("heel").executes(c -> {
            Littekens.zetKlaar(c.getSource().getPlayerOrException(), true);
            return 1;
        })).then(Commands.literal("grijs").executes(c -> {
            Littekens.zetKlaar(c.getSource().getPlayerOrException(), false);
            return 1;
        })));
        w.then(dev("uitkijk").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> {
            WereldPlekken.UITKIJKEN.forEach(u -> b.suggest(u.id()));
            return b.buildFuture();
        }).executes(c -> {
            String id = StringArgumentType.getString(c, "id");
            for (WereldPlekken.Uitkijk u : WereldPlekken.UITKIJKEN) {
                if (u.id().equals(id)) {
                    Uitkijken.panorama(c.getSource().getPlayerOrException(), u);
                    return 1;
                }
            }
            return 0;
        })));
        w.then(dev("vergeetportaal").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            nl.juiced.guhs.feature.vadsrijk.kern.Spelers.data(p).gezien.remove(Portalen.ONTWAAKT);
            nl.juiced.guhs.feature.vadsrijk.kern.Spelers.data(p).gezien.remove(Portalen.BINNEN_GEWEEST);
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(w));
    }
}
