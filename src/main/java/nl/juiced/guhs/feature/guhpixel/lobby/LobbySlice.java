package nl.juiced.guhs.feature.guhpixel.lobby;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.guhpixel.GidsBlad;
import nl.juiced.guhs.feature.guhpixel.Guhpixel;
import nl.juiced.guhs.feature.guhpixel.GuhpixelFeature;
import nl.juiced.guhs.feature.guhpixel.Lobby;
import nl.juiced.guhs.feature.guhpixel.LobbyNpcs;
import nl.juiced.guhs.feature.guhpixel.LobbyPlek;
import nl.juiced.guhs.feature.guhpixel.Muntjes;
import nl.juiced.guhs.feature.guhpixel.PxData;
import nl.juiced.guhs.feature.guhpixel.PxZelftest;
import nl.juiced.guhs.feature.guhpixel.Rang;
import nl.juiced.guhs.feature.guhpixel.Sessies;
import nl.juiced.guhs.feature.guhpixel.Toegang;
import nl.juiced.guhs.feature.guhpixel.blok.DecoBlock;
import nl.juiced.guhs.feature.guhpixel.blok.LoreBlockItem;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.quest.GuhAdvancements;

/**
 * Guhpixel slice "lobby" (DESIGN_PX section 1): everything that lives on the lobby island and the way to it.
 * <ul>
 *   <li>The lobby template itself, the Guh-internetcafé "De Trage Verbinding" and every text: tools/features/guhpixel_lobby*.py.</li>
 *   <li>{@link Welkomstguh}: the greeter, gives the Netwerkkabeltje (and the recipe of the Guhpixel-poort).</li>
 *   <li>{@link LobbyRollen}: the Verkoper-guh (opens the shop), the Beheerder-guh and the sleepers of the café.</li>
 *   <li>{@link Chatguhs}: the lobby guhs and their chat jokes.</li>
 *   <li>{@link LobbyBorden} / {@link Zweeftekst}: "Spelers online", the personal stats board, the labels.</li>
 *   <li>{@link LobbyParkour}: the parkour over the roofs with a personal best.</li>
 *   <li>{@link Knabbels} / {@link GoudenKnabbelBlock}: the ten hidden golden knabbels, per player.</li>
 *   <li>{@link LobbyGids}: the lobby's part of the Guhdex tab; {@link LobbyCommando}: {@code /guhs px lobby ...}.</li>
 * </ul>
 * Per-player data: {@code PxData.deel(p, "lobby")} (parkour best, greeted) and the kern's once-keys {@code lobby:*}.
 */
public final class LobbySlice {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);

    /** The spawn point: every spot of the lobby is given relative to it (game tests use their own). */
    static final BlockPos OORSPRONG = LobbyPlek.SPAWN.blok();

    private static BlockBehaviour.Properties vast(MapColor kleur) {
        return BlockBehaviour.Properties.of().mapColor(kleur).strength(-1.0f, 3600000.0f).noLootTable().pushReaction(PushReaction.BLOCK);
    }

    public static final DeferredBlock<GoudenKnabbelBlock> GOUDEN_KNABBEL = BLOCKS.registerBlock("lobby_gouden_knabbel", GoudenKnabbelBlock::new,
            () -> vast(MapColor.GOLD).noOcclusion().lightLevel(s -> 9).sound(SoundType.AMETHYST));
    public static final DeferredBlock<Block> PARKOUR_START = BLOCKS.registerBlock("lobby_parkour_start", Block::new,
            () -> vast(MapColor.COLOR_LIGHT_GREEN).lightLevel(s -> 10).sound(SoundType.WOOL));
    public static final DeferredBlock<Block> PARKOUR_TUSSENPUNT = BLOCKS.registerBlock("lobby_parkour_tussenpunt", Block::new,
            () -> vast(MapColor.COLOR_YELLOW).lightLevel(s -> 8).sound(SoundType.WOOL));
    public static final DeferredBlock<Block> PARKOUR_FINISH = BLOCKS.registerBlock("lobby_parkour_finish", Block::new,
            () -> vast(MapColor.SNOW).lightLevel(s -> 10).sound(SoundType.WOOL));
    /** The old beige computer of the Guh-internetcafé (a decoration; breaking one in the café gives it to you). */
    public static final DeferredBlock<DecoBlock> COMPUTER = BLOCKS.registerBlock("internetcafe_computer", p -> new DecoBlock(p, Block.box(2, 0, 1, 14, 13, 15)),
            () -> DecoBlock.props().lightLevel(s -> 5));
    public static final DeferredItem<LoreBlockItem> COMPUTER_ITEM = ITEMS.registerItem("internetcafe_computer", p -> new LoreBlockItem(COMPUTER.get(), p),
            () -> new Item.Properties().useBlockDescriptionPrefix());

    public static final DeferredHolder<SoundEvent, SoundEvent> KNABBEL_GELUID = geluid("lobby.knabbel");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARKOUR_START_GELUID = geluid("lobby.parkour_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> PARKOUR_FINISH_GELUID = geluid("lobby.parkour_finish");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHAT_GELUID = geluid("lobby.chat");

    private static DeferredHolder<SoundEvent, SoundEvent> geluid(String naam) {
        return SOUNDS.register(naam, () -> SoundEvent.createVariableRangeEvent(Guhs.id(naam)));
    }

    /** This slice's own tag for a player. */
    static CompoundTag data(ServerPlayer p) {
        return PxData.deel(p, "lobby");
    }

    /** The highest rank. (1.3.1: no title hangs on it any more.) */
    public static boolean isMvgPlusPlus(ServerPlayer p) {
        return Toegang.heeft(p) && Muntjes.rang(p) == Rang.MVG_PLUS_PLUS;
    }

    /** Grants quest/lobby_rang_mvg once the player is [MVG] or higher. */
    static void controleerRang(ServerPlayer p) {
        if (Toegang.heeft(p) && Muntjes.rang(p).ordinal() >= Rang.MVG.ordinal()) {
            GuhAdvancements.grant(p, "lobby_rang_mvg");
        }
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        SOUNDS.register(modBus);
        NpcRollen.zet(GuhNpcEntity.Kind.LOBBY_WELKOMSTGUH, Welkomstguh.ROL);
        NpcRollen.zet(GuhNpcEntity.Kind.LOBBY_VERKOPER_GUH, LobbyRollen.VERKOPER);
        NpcRollen.zet(GuhNpcEntity.Kind.LOBBY_CHATGUH, Chatguhs.ROL);
        NpcRollen.zet(GuhNpcEntity.Kind.INTERNETCAFE_BEHEERDER, LobbyRollen.BEHEERDER);
        NpcRollen.zet(GuhNpcEntity.Kind.INTERNETCAFE_SLAPER, LobbyRollen.SLAPER);
        LobbyNpcs.registreer(LobbyPlek.WELKOM, GuhNpcEntity.Kind.LOBBY_WELKOMSTGUH,
                Component.translatable("gui.guhs.lobby.welkom.kop").withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD),
                server -> Component.translatable("gui.guhs.lobby.welkom.onder").withStyle(ChatFormatting.GRAY));
        LobbyNpcs.registreer(LobbyPlek.WINKEL, GuhNpcEntity.Kind.LOBBY_VERKOPER_GUH,
                Component.translatable("gui.guhs.lobby.verkoper.kop").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                server -> Component.translatable("gui.guhs.lobby.verkoper.onder." + (server.getTickCount() / 200) % 4).withStyle(ChatFormatting.GRAY));
        GidsBlad.registreer(new LobbyGids());
        NeoForge.EVENT_BUS.addListener(LobbySlice::opServerTick);
        NeoForge.EVENT_BUS.addListener(LobbySlice::opSpelerTick);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> weg(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem e) -> {
            if (e.getEntity() instanceof ServerPlayer p && LobbyParkour.geenItems(p)) {
                e.setCanceled(true);
                e.setCancellationResult(net.minecraft.world.InteractionResult.FAIL);
            }
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.EntityTeleportEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && !e.isCanceled()) {
                LobbyParkour.geteleporteerd(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            weg(e.getEntity());
            if (e.getFrom() == Guhpixel.DIM) {
                // back where they came from: when that is INSIDE the café's screen (they walked in there), /lobby must not
                // send them straight to the lobby again. Like a nether portal: no new trip until they step out of it.
                e.getEntity().setPortalCooldown();
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> weg(e.getEntity()));
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> LobbyKaart.vergeet());
        NeoForge.EVENT_BUS.addListener(LobbyCommando::register);
        PxZelftest.registreer("lobby", LobbySlice::zelftest);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
        output.accept(new ItemStack(COMPUTER_ITEM.get()));
    }

    // --- ticking -------------------------------------------------------------------------------------------------------------

    /** Are the chunk and its entities at this spot loaded (so nothing is made twice)? */
    static boolean geladen(ServerLevel level, Vec3 pos) {
        BlockPos blok = BlockPos.containing(pos);
        ChunkPos chunk = ChunkPos.containing(blok);
        return level.hasChunk(chunk.x(), chunk.z()) && level.areEntitiesLoaded(ChunkPos.pack(blok));
    }

    private static void opServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        ServerLevel level = Guhpixel.level(server);
        if (level == null || level.players().isEmpty() || Lobby.herbouwBezig() || Lobby.gebouwd(server) == 0) {
            return;
        }
        if (server.getTickCount() % 100 == 7 && geladen(level, LobbyPlek.SPAWN.pos()) && geladen(level, LobbyPlek.WINKEL.pos())
                && geladen(level, LobbyPlek.PARKOUR_START.pos()) && geladen(level, LobbyPlek.UITGANG.pos()) && geladen(level, LobbyPlek.SPEL_RESERVE.pos())) {
            Chatguhs.zorg(level, OORSPRONG);
            LobbyBorden.gedeeld(level, OORSPRONG);
        }
        Chatguhs.tick(level, OORSPRONG);
    }

    private static void opSpelerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        if (Guhpixel.inLobby(p) && Sessies.van(p) == null) {
            LobbyParkour.tick(p);
            if (LobbyBorden.moetMeteen(p) || (p.tickCount + p.getId()) % 40 == 0 || (p.tickCount % 10 == 0 && !Zweeftekst.ziet(p))) {
                LobbyBorden.persoonlijk(p, OORSPRONG);
            }
        } else {
            LobbyParkour.vergeet(p);
            if (Zweeftekst.ziet(p)) {
                Zweeftekst.weg(p);
            }
        }
        if ((p.tickCount + p.getId()) % 100 == 0) {
            controleerRang(p);
        }
    }

    /** The player's client forgot the lobby (logged out, another dimension, respawned). */
    private static void weg(net.minecraft.world.entity.player.Player speler) {
        if (speler instanceof ServerPlayer p) {
            LobbyParkour.vergeet(p);
            Zweeftekst.vergeet(p);
        }
    }

    // --- the dev-server self test -----------------------------------------------------------------------------------------------

    private static void zelftest(MinecraftServer server, ServerLevel level, PxZelftest.Melder meld) {
        LobbyKaart.Kaart kaart = LobbyKaart.van(server);
        meld.check(kaart.knabbels().size() == Knabbels.AANTAL && kaart.chatguhs().size() == Chatguhs.NAMEN && kaart.tussen().size() == LobbyParkour.TUSSENPUNTEN,
                "lobby_kaart.json: " + kaart.knabbels().size() + " knabbels, " + kaart.chatguhs().size() + " chat guhs, " + kaart.tussen().size() + " checkpoints");
        if (!Lobby.zorg(level)) {
            meld.fout("the lobby does not stand");
            return;
        }
        if (Lobby.gebouwd(server) < Lobby.versie(server)) {
            meld.ok("the lobby here is an older version (" + Lobby.gebouwd(server) + " of " + Lobby.versie(server) + "): it is rebuilt at the next start; block checks skipped");
            return;
        }
        int goed = 0;
        for (int i = 0; i < kaart.knabbels().size(); i++) {
            BlockPos pos = kaart.knabbels().get(i);
            level.getChunk(pos);
            BlockState s = level.getBlockState(pos);
            if (s.is(GOUDEN_KNABBEL.get()) && s.getValue(GoudenKnabbelBlock.NUMMER) == i) {
                goed++;
            } else {
                meld.fout("golden knabbel " + i + " is not at " + pos.toShortString() + " (" + s.getBlock() + ")");
            }
        }
        meld.check(goed == Knabbels.AANTAL, goed + " of " + Knabbels.AANTAL + " golden knabbels stand where the map says");
        level.getChunk(kaart.start());
        level.getChunk(kaart.finish());
        meld.check(level.getBlockState(kaart.start()).is(PARKOUR_START.get()) && kaart.start().equals(LobbyPlek.PARKOUR_START.blok().below()),
                "the parkour start block lies under the PARKOUR_START anchor");
        meld.check(level.getBlockState(kaart.finish()).is(PARKOUR_FINISH.get()), "the parkour finish block at " + kaart.finish().toShortString());
        int tussen = 0;
        for (BlockPos pos : kaart.tussen()) {
            level.getChunk(pos);
            if (level.getBlockState(pos).is(PARKOUR_TUSSENPUNT.get())) {
                tussen++;
            }
        }
        meld.check(tussen == LobbyParkour.TUSSENPUNTEN, tussen + " parkour checkpoints");
        BlockPos winkel = LobbyPlek.WINKEL.blok();
        level.getChunk(winkel);
        meld.check(!level.getBlockState(new BlockPos(-44, 103, 0)).isAir() && !level.getBlockState(new BlockPos(-25, 106, 6)).isAir()
                && level.getBlockState(new BlockPos(-25, 102, 6)).isAir(), "the shop stands at x -44..-25 with its doorway in front of the Verkoper-guh");
        level.getChunk(new BlockPos(0, 100, 30));
        meld.check(level.getBlockState(new BlockPos(-2, 103, 30)).is(GuhpixelFeature.PORTAAL.get()), "the door home is 5 wide and 4 high");
        boolean alles = true;
        for (LobbyKaart.Plek plek : kaart.chatguhs()) {
            level.getChunk(plek.pos());
            alles &= geladen(level, Vec3.atBottomCenterOf(plek.pos()));
        }
        int zitten = Chatguhs.zorg(level, OORSPRONG);
        if (alles) {
            meld.check(zitten == kaart.chatguhs().size(), "lobby guhs sitting: " + zitten + " of " + kaart.chatguhs().size());
        } else {
            meld.ok("lobby guhs sitting: " + zitten + " of " + kaart.chatguhs().size() + " (the entities of the lobby are not all loaded without a player)");
        }
        meld.check(server.getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(Registries.RECIPE, Guhs.id("guhpixel_poort"))).isPresent(),
                "the recipe guhs:guhpixel_poort exists (the greeter teaches it)");
        var sets = server.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        meld.check(sets.containsKey(Guhs.id("internetcafe")) && sets.containsKey(Guhs.id("internetcafe_gegarandeerd")),
                "structure sets internetcafe and internetcafe_gegarandeerd are loaded");
    }

    private LobbySlice() {
    }
}
