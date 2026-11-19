package nl.juiced.guhs.feature.guhwinter.lootjes;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.function.Consumer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.guhwinter.Guhwinter;
import nl.juiced.guhs.feature.guhwinter.GuhwinterCommando;
import nl.juiced.guhs.feature.guhwinter.GuhwinterZone;
import nl.juiced.guhs.feature.guhwinter.plein.GuhwinterPlein;
import nl.juiced.guhs.feature.guhwinter.tekenen.Doel;

/**
 * Guhwinter 1.10.10, slice LOOTJES (package {@code feature.guhwinter.lootjes}; ids {@code guhwinter_*}, lang
 * {@code guhwinter.lootjes.*}, generator tools/features/guhwinter_lootjes.py, gametests {@code LootjesGameTests}).
 * The flow is in {@link Lootjes}; building in {@link Bouwen} / {@link Bouwkist} / {@link BouwRegels}; the gift in
 * {@link Inhoud}, {@link SurpriseItem}, {@link SurpriseBlok}, {@link SurprisePlaatsen} (client: lootjes.client).
 * <p>Blocks: {@code guhwinter_grote_boek} (Het Grote Boek), {@code guhwinter_bouwbord} (a plot's name sign: a spruce
 * standing sign of our own), {@code guhwinter_surprise} + {@code guhwinter_surprise_deel} (a placed gift). Items:
 * {@code guhwinter_surprise} ("Surprise van ..."), {@code guhwinter_zak} (Zak van Sinterklaas).
 */
public final class LootjesFeature {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Guhs.MODID);
    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Guhs.MODID);

    private static BlockBehaviour.Properties vast(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(-1.0f, 3600000.0f).sound(SoundType.WOOD).pushReaction(PushReaction.BLOCK);
    }

    private static BlockBehaviour.Properties surprise() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PINK).strength(-1.0f, 3600000.0f).sound(SoundType.WOOD)
                .noOcclusion().dynamicShape().forceSolidOn().noLootTable().pushReaction(PushReaction.BLOCK)
                .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)
                .lightLevel(s -> s.getValue(SurpriseBlok.LICHT));
    }

    public static final DeferredBlock<GroteBoekBlok> GROTE_BOEK = BLOCKS.registerBlock("guhwinter_grote_boek", GroteBoekBlok::new,
            () -> vast(MapColor.WOOD).noOcclusion());
    public static final DeferredBlock<StandingSignBlock> BOUWBORD = BLOCKS.registerBlock("guhwinter_bouwbord",
            p -> new StandingSignBlock(WoodType.SPRUCE, p), () -> vast(MapColor.PODZOL).forceSolidOn().noCollision());
    public static final DeferredBlock<SurpriseBlok.Kern> SURPRISE = BLOCKS.registerBlock("guhwinter_surprise", SurpriseBlok.Kern::new, LootjesFeature::surprise);
    public static final DeferredBlock<SurpriseBlok.Deel> SURPRISE_DEEL = BLOCKS.registerBlock("guhwinter_surprise_deel", SurpriseBlok.Deel::new,
            LootjesFeature::surprise);

    public static final DeferredItem<BlockItem> GROTE_BOEK_ITEM = ITEMS.registerSimpleBlockItem(GROTE_BOEK);
    public static final DeferredItem<BlockItem> BOUWBORD_ITEM = ITEMS.registerSimpleBlockItem(BOUWBORD);
    public static final DeferredItem<SurpriseItem> SURPRISE_ITEM = ITEMS.registerItem("guhwinter_surprise", SurpriseItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
    public static final DeferredItem<ZakItem> ZAK = ITEMS.registerItem("guhwinter_zak", ZakItem::new,
            () -> new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SurpriseBlokEntity>> SURPRISE_BE = BLOCK_ENTITIES.register("guhwinter_surprise",
            () -> new BlockEntityType<>(SurpriseBlokEntity::kern, SURPRISE.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SurpriseBlokEntity>> SURPRISE_DEEL_BE = BLOCK_ENTITIES.register("guhwinter_surprise_deel",
            () -> new BlockEntityType<>(SurpriseBlokEntity::deel, SURPRISE_DEEL.get()));

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SurpriseData>> SURPRISE_DATA =
            COMPONENTS.registerComponentType("guhwinter_surprise", b -> b.persistent(SurpriseData.CODEC).networkSynchronized(SurpriseData.STREAM_CODEC));

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    /** The three moments of this slice (kern remembers them per world: never rename). */
    static final String TREKKING = "trekking:lootjes", BOUWEN_DICHT = "bouwen_dicht:lootjes", PAKJESAVOND = "pakjesavond:lootjes";

    private LootjesFeature() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        COMPONENTS.register(modBus);
        modBus.addListener((BlockEntityTypeAddBlocksEvent e) -> e.modify(BlockEntityType.SIGN, BOUWBORD.get()));

        Guhwinter.opMoment(TREKKING, s -> LOGGER.info("Guhwinter lootjes: {}", Lootjes.trek(s)));
        Guhwinter.opMoment(BOUWEN_DICHT, s -> LOGGER.info("Guhwinter lootjes: {}", Lootjes.vang(s)));
        Guhwinter.opMoment(PAKJESAVOND, s -> LOGGER.info("Guhwinter lootjes: {}", Lootjes.bezorg(s)));

        GuhwinterZone.uitzondering(Bouwen::uitzondering);
        GuhwinterPlein.voorStempel((level, welke) -> "lootjes: " + Bouwen.iedereenTerug(level.getServer()) + " bouwers terug");
        GuhwinterPlein.naStempel((level, welke) -> welke.equals(GuhwinterPlein.WINTER) ? LootjesPlaatsing.plaats(level) : null);
        Doel.LOOTJE.magOpslaan((speler, bestaande) -> Lootjes.magTekenen(speler.level().getServer(), speler.getUUID()));
        Doel.LOOTJE.opOpslaan(Lootjes::tekeningOpgeslagen);

        var bus = NeoForge.EVENT_BUS;
        bus.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                Lootjes.opLogin(p);
            }
        });
        bus.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            Bouwen.opUitloggen(e);
            Lootjes.uitgelogd(e.getEntity().getUUID());
        });
        bus.addListener((ServerStoppingEvent e) -> Bouwen.iedereenTerug(e.getServer()));
        bus.addListener((ServerStoppedEvent e) -> {
            Lootjes.vergeetAlles();
            Bouwen.vergeet();
            Bouwvelden.vergeet();
        });
        bus.addListener(Bouwen::opReis);
        bus.addListener(Bouwen::opDood);
        bus.addListener(Bouwen::opModus);
        bus.addListener(Bouwen::opGooien);
        bus.addListener(Bouwen::opEntiteit);
        bus.addListener(Bouwen::opPlaats);
        bus.addListener(Bouwen::opKlikBlok);
        bus.addListener(Bouwen::opKlikItem);
        bus.addListener(Bouwen::opEntiteitKlik);
        bus.addListener(Bouwen::opEntiteitKlikPrecies);
        bus.addListener(Bouwen::opSlaan);
        bus.addListener(EventPriority.HIGH, SurprisePlaatsen::opKlik);
        bus.addListener(EventPriority.HIGH, LootjesFeature::opBordKlik);
        bus.addListener(SurprisePlaatsen::opBreek);
        nl.juiced.guhs.world.TikWacht.post("GuhwinterLootjes", e -> {
            MinecraftServer s = e.getServer();
            Bouwen.tick(s);
            Lootjes.tick(s);
            if (s.getTickCount() % 200 == 37 && Guhwinter.bezig(s)) {
                ServerLevel level = s.getLevel(GuhwinterZone.DIMENSIE);
                if (level != null) {
                    LootjesPlaatsing.controleer(level);
                }
            }
        });
        GuhwinterCommando.voegToe(ctx -> commandos());
    }

    public static void payloads(PayloadRegistrar registrar) {
        LootjesPayloads.register(registrar);
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(GROTE_BOEK_ITEM.get()));
        output.accept(new ItemStack(BOUWBORD_ITEM.get()));
        output.accept(new ItemStack(SURPRISE_ITEM.get()));
        output.accept(new ItemStack(ZAK.get()));
    }

    /** A plot's sign: its maker opens their drawing (building time); anyone else hears whose plot it is. */
    static void opBordKlik(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().getBlockState(event.getPos()).is(BOUWBORD.get())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Bouwvelden.Veld v = Bouwvelden.vanBord(event.getPos());
        if (v == null) {
            return;
        }
        MinecraftServer server = p.level().getServer();
        LootjesData d = LootjesData.get(server);
        UUID eigenaar = d.eigenaarVanVeld(v.i());
        if (p.getUUID().equals(eigenaar) && Guhwinter.bouwenOpen(server)) {
            Lootjes.teken(p);
            return;
        }
        p.sendOverlayMessage((eigenaar == null ? Component.translatable(Lootjes.T + "bord.info_vrij", v.i() + 1)
                : Component.translatable(Lootjes.T + "bord.info", v.i() + 1, d.naam(eigenaar))).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // =====================================================================================================================
    // /guhs winter lootjes ...
    // =====================================================================================================================

    private static int zeg(CommandSourceStack bron, String tekst) {
        bron.sendSuccess(() -> Component.literal("[winter lootjes] " + tekst).withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    private interface SpelerActie {
        void doe(ServerPlayer p);
    }

    private static int speler(CommandContext<CommandSourceStack> c, SpelerActie a) throws CommandSyntaxException {
        a.doe(c.getSource().getPlayerOrException());
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> commandos() {
        LiteralArgumentBuilder<CommandSourceStack> l = Commands.literal("lootjes");
        // everyone (the chat buttons of the book)
        l.then(Commands.literal("boek").executes(c -> speler(c, Lootjes::openBoek)));
        l.then(Commands.literal("inschrijven").executes(c -> speler(c, Lootjes::inschrijven)));
        l.then(Commands.literal("uitschrijven").executes(c -> speler(c, Lootjes::uitschrijven)));
        l.then(Commands.literal("teken").executes(c -> speler(c, Lootjes::teken)));
        // dev (op 2)
        l.then(GuhwinterCommando.dev("lijst").executes(c -> lijst(c.getSource())));
        l.then(GuhwinterCommando.dev("nep").then(Commands.argument("naam", StringArgumentType.word()).executes(c -> {
            String naam = StringArgumentType.getString(c, "naam");
            LootjesData d = LootjesData.get(c.getSource().getServer());
            if (d.ingeschreven.size() >= Lootjes.MAX) {
                return zeg(c.getSource(), "het boek is vol (" + Lootjes.MAX + ")");
            }
            UUID id = UUID.nameUUIDFromBytes(("guhwinter_nep:" + naam).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if (d.isIngeschreven(id)) {
                return zeg(c.getSource(), naam + " staat er al in");
            }
            d.ingeschreven.add(new LootjesData.Deelnemer(id, naam, true));
            d.setDirty();
            return zeg(c.getSource(), "nep-naam " + naam + " ingeschreven (" + d.ingeschreven.size() + "/" + Lootjes.MAX + ")");
        })));
        l.then(GuhwinterCommando.dev("erbij").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            LootjesData d = LootjesData.get(c.getSource().getServer());
            if (!d.isIngeschreven(p.getUUID()) && d.ingeschreven.size() < Lootjes.MAX) {
                d.ingeschreven.add(new LootjesData.Deelnemer(p.getUUID(), p.getGameProfile().name(), false));
                d.setDirty();
            }
            return zeg(c.getSource(), "ingeschreven (ook buiten de tijd): " + d.ingeschreven.size() + "/" + Lootjes.MAX);
        }));
        l.then(GuhwinterCommando.dev("trek").executes(c -> zeg(c.getSource(), Lootjes.trek(c.getSource().getServer()))));
        l.then(GuhwinterCommando.dev("vang").executes(c -> zeg(c.getSource(), Lootjes.vang(c.getSource().getServer()))));
        l.then(GuhwinterCommando.dev("bezorg").executes(c -> zeg(c.getSource(), Lootjes.bezorg(c.getSource().getServer()))));
        l.then(GuhwinterCommando.dev("reset").executes(c -> zeg(c.getSource(), reset(c.getSource().getServer()))));
        l.then(GuhwinterCommando.dev("plaats").executes(c -> {
            ServerLevel level = c.getSource().getServer().getLevel(GuhwinterZone.DIMENSIE);
            return zeg(c.getSource(), level == null ? "geen Guhmension" : LootjesPlaatsing.plaats(level));
        }));
        l.then(GuhwinterCommando.dev("surprise").then(Commands.argument("veld", IntegerArgumentType.integer(1, Bouwvelden.AANTAL)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            ItemStack s = Lootjes.testSurprise(c.getSource().getServer(), IntegerArgumentType.getInteger(c, "veld") - 1, p.getGameProfile().name());
            if (s == null) {
                return zeg(c.getSource(), "dat bouwplekje is er niet (ankers?)");
            }
            p.getInventory().add(s);
            return zeg(c.getSource(), "Surprise van bouwplekje " + IntegerArgumentType.getInteger(c, "veld") + " in je tas");
        })));
        l.then(GuhwinterCommando.dev("zak").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            ItemStack z = Lootjes.maakZak(c.getSource().getServer(), p.getUUID());
            if (z == null) {
                return zeg(c.getSource(), "niemand heeft jouw lootje (trek eerst, met jezelf erin)");
            }
            p.getInventory().add(z);
            return zeg(c.getSource(), "een kopie van jouw Zak van Sinterklaas in je tas");
        }));
        l.then(GuhwinterCommando.dev("veld").then(Commands.argument("veld", IntegerArgumentType.integer(1, Bouwvelden.AANTAL)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Bouwvelden.Veld v = Bouwvelden.veld(IntegerArgumentType.getInteger(c, "veld") - 1);
            ServerLevel level = c.getSource().getServer().getLevel(GuhwinterZone.DIMENSIE);
            if (v == null || level == null) {
                return zeg(c.getSource(), "dat bouwplekje is er niet (ankers?)");
            }
            p.teleportTo(level, v.uitgang().x, v.uitgang().y, v.uitgang().z, java.util.Set.of(), v.uitgangYaw(), 0, true);
            return 1;
        })));
        return l;
    }

    private static int lijst(CommandSourceStack bron) {
        MinecraftServer server = bron.getServer();
        LootjesData d = LootjesData.get(server);
        zeg(bron, "fase " + Guhwinter.fase(server) + ", inschrijven " + (Guhwinter.inschrijvenOpen(server) ? "open" : "dicht") + ", bouwen "
                + (Guhwinter.bouwenOpen(server) ? "open" : "dicht") + "; getrokken " + d.getrokken + (d.mislukt ? " (mislukt)" : "")
                + ", gevangen " + d.gevangen + ", bezorgd " + d.bezorgd);
        zeg(bron, "ingeschreven " + d.ingeschreven.size() + "/" + Lootjes.MAX + ":");
        for (LootjesData.Deelnemer x : d.ingeschreven) {
            UUID o = d.lootjeVan(x.id());
            int v = d.veldVan(x.id());
            String regel = "  " + x.naam() + (x.nep() ? " (nep)" : "") + (o == null ? "" : " -> " + d.naam(o) + ", bouwplekje " + (v + 1))
                    + (d.tekening.containsKey(x.id()) ? ", tekening" : "")
                    + (d.cadeau.containsKey(x.id()) ? ", cadeau " + Inhoud.aantal(d.cadeau.get(x.id())) + " blokjes" : "")
                    + (d.teBezorgen.contains(x.id()) ? ", zak WACHT" : "") + (d.klaar.contains(x.id()) ? ", klaar" : "")
                    + ", status " + Lootjes.status(server, x.id());
            zeg(bron, regel);
        }
        long bouwers = server.getPlayerList().getPlayers().stream().filter(Bouwkist::bouwt).count();
        zeg(bron, "nu aan het bouwen: " + bouwers + "; reservekopieën van tassen: " + d.kist.size());
        return 1;
    }

    /** Dev: everything back to empty; the moments still to come may fire again (passed ones stay done). */
    private static String reset(MinecraftServer server) {
        int terug = Bouwen.iedereenTerug(server);
        LootjesData.get(server).wis();
        ZonedDateTime nu = Guhwinter.nu(server);
        int vergeten = 0;
        for (String id : new String[]{TREKKING, BOUWEN_DICHT, PAKJESAVOND}) {
            ZonedDateTime t = Guhwinter.MOMENTEN.get(id.substring(0, id.indexOf(':')));
            if (t != null && t.isAfter(nu) && Guhwinter.vergeetMoment(server, id)) {
                vergeten++;
            }
        }
        ServerLevel level = server.getLevel(GuhwinterZone.DIMENSIE);
        if (level != null) {
            LootjesPlaatsing.schrijfBorden(level);
        }
        return "gewist; " + terug + " bouwers terug; " + vergeten + " momenten kunnen weer vuren"
                + " (zet eerst /guhs winter datum terug om een gepasseerd moment opnieuw te laten vuren)";
    }
}
