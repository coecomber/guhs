package nl.juiced.guhs.feature.vadsrijk.ftb;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.balto.BaltoBlocks;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.zone6.raid.Z6RaidHaken;

/**
 * Y-FTB (round Y): the compact FTB chapter "Het Vadsrijk" (tools/features/vadsrijk_y_ftb.py): its tasks ({@link VrFtb}), the claim
 * rule ({@link FtbClaimRegel} + mixin/FtbClaimMixin) and its 14 rewards: little decoration blocks for your own world
 * ({@link TrofeeBlock}), ids {@code guhs:vadsrijk_trofee_*} (NOT "vr_": those count as Vadsrijk items). Dev command
 * {@code /guhs vadsrijk ftb status|check|geef <wat>|vergeet}.
 */
public final class VrFtbRegistry {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Guhs.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Guhs.MODID);
    /** id (without "guhs:") -> block, in creative-tab / quest-book order. */
    public static final Map<String, DeferredBlock<TrofeeBlock>> TROFEEEN = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<BlockItem>> ITEMS_PER_ID = new LinkedHashMap<>();

    static {
        // (the boxes match the models of vadsrijk_y_ftb.py; made square around the middle, so they fit every facing)
        trofee("rekruut", MapColor.COLOR_PINK, SoundType.WOOL, 0, 5, 16);
        trofee("zone1", MapColor.COLOR_PINK, SoundType.WOOL, 12, 3, 12.6);
        trofee("zone2", MapColor.ICE, SoundType.GLASS, 4, 3, 16);
        trofee("zone3", MapColor.COLOR_GREEN, SoundType.WOOD, 13, 2, 16);
        trofee("zone4", MapColor.COLOR_ORANGE, SoundType.WOOL, 13, 5, 15);
        trofee("zone5", MapColor.QUARTZ, SoundType.GLASS, 6, 2, 15.5);
        trofee("zone6", MapColor.SAND, SoundType.STONE, 10, 4, 16);
        trofee("raid", MapColor.GOLD, SoundType.METAL, 4, 2.5, 13.5);
        for (int z = 1; z <= 6; z++) {
            trofee("geheim" + z, MapColor.WOOD, SoundType.WOOD, 0, 3.5, 8);
        }
    }

    private VrFtbRegistry() {
    }

    /** A trophy: light level, its inset from the block's sides (pixels) and its height. */
    private static void trofee(String naam, MapColor kleur, SoundType geluid, int licht, double rand, double hoog) {
        String id = "vadsrijk_trofee_" + naam;
        VoxelShape vorm = Block.box(rand, 0, rand, 16 - rand, Math.min(16, hoog), 16 - rand);
        DeferredBlock<TrofeeBlock> blok = BLOCKS.registerBlock(id, p -> new TrofeeBlock(p, vorm),
                () -> BlockBehaviour.Properties.of().mapColor(kleur).strength(0.5f).sound(geluid).noOcclusion().lightLevel(s -> licht));
        TROFEEEN.put(id, blok);
        ITEMS_PER_ID.put(id, ITEMS.registerItem(id, p -> new BaltoBlocks.LoreBlock(blok.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix()));
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        NpcApi.QUEST_KLAAR.add(VrFtb::questKlaar);
        Z6RaidHaken.OPGEVROLIJKT.add(VrFtb::raidOpgevrolijkt);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                VrFtb.controleer(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % VrFtb.INTERVAL == 37) {
                for (ServerPlayer p : List.copyOf(e.getServer().getPlayerList().getPlayers())) {
                    VrFtb.controleer(p);
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(VrFtbRegistry::commando);
    }

    /** The Guhs creative tab: the 14 trophies. */
    public static void creative(Consumer<ItemStack> output) {
        for (DeferredItem<BlockItem> i : ITEMS_PER_ID.values()) {
            output.accept(new ItemStack(i.get()));
        }
    }

    // ---- dev command ------------------------------------------------------------------------------------------------------------

    static void commando(RegisterCommandsEvent event) {
        event.getDispatcher().register(VadsrijkCommando.wortel(Commands.literal("ftb").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("status").executes(VrFtbRegistry::status))
                .then(Commands.literal("check").executes(c -> {
                    int n = VrFtb.controleer(c.getSource().getPlayerOrException());
                    c.getSource().sendSuccess(() -> Component.literal("FTB Vadsrijk: " + n + " nieuwe taak/taken afgevinkt."), false);
                    return 1;
                }))
                .then(Commands.literal("geef").then(Commands.argument("wat", StringArgumentType.word()).suggests((c, b) -> {
                    for (String a : VrFtb.alle()) {
                        b.suggest(a.substring("vr_ftb_".length()));
                    }
                    return b.buildFuture();
                }).executes(c -> {
                    String a = "vr_ftb_" + StringArgumentType.getString(c, "wat");
                    if (!VrFtb.alle().contains(a)) {
                        c.getSource().sendFailure(Component.literal("Onbekend: " + a));
                        return 0;
                    }
                    VrFtb.geef(c.getSource().getPlayerOrException(), a);
                    c.getSource().sendSuccess(() -> Component.literal("Afgevinkt: " + a + " (de FTB-quest vinkt zichzelf nu af)."), false);
                    return 1;
                })))
                .then(Commands.literal("vergeet").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    for (String a : VrFtb.alle()) {
                        VrFtb.neemTerug(p, a);
                    }
                    c.getSource().sendSuccess(() -> Component.literal("Alle vr_ftb-advancements teruggenomen (FTB-voortgang zelf: /ftbquests change_progress)."), false);
                    return 1;
                }))
                .then(Commands.literal("trofeeen").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    for (DeferredItem<BlockItem> i : ITEMS_PER_ID.values()) {
                        p.getInventory().placeItemBackInInventory(new ItemStack(i.get()));
                    }
                    return 1;
                }))
                .then(Commands.literal("zone").then(Commands.argument("n", IntegerArgumentType.integer(1, 6)).executes(c -> {
                    int z = IntegerArgumentType.getInteger(c, "n");
                    String eind = VrFtb.eindQuest(z);
                    c.getSource().sendSuccess(() -> Component.literal("Zone " + z + ": laatste hoofdquest = " + eind
                            + " (/guhs vadsrijk npc quest af " + eind + ")"), false);
                    return 1;
                })))));
    }

    static int status(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        List<String> regels = new ArrayList<>();
        regels.add("FTB Vadsrijk (" + p.getName().getString() + "), claimen hier: " + (FtbClaimRegel.magClaimen(p) ? "ja" : "NEE (Vadsrijk)"));
        regels.add(" rekruut (" + VrFtb.REKRUUT_QUEST + "): " + (VrFtb.heeft(p, "vr_ftb_rekruut") ? "af" : VrFtb.rekruut(p) ? "klaar, nog niet afgevinkt" : "nog niet"));
        for (int z = 1; z <= 6; z++) {
            int[] g = VrFtb.geheimen(p, z);
            regels.add(" zone " + z + " (" + VrFtb.eindQuest(z) + "): " + (VrFtb.heeft(p, "vr_ftb_zone" + z) ? "af" : "nog niet")
                    + " · geheimpjes " + g[0] + "/" + g[1] + (VrFtb.heeft(p, "vr_ftb_geheimen" + z) ? " af" : "")
                    + (z >= 2 ? " · poort " + (VrFtb.heeft(p, "vr_ftb_poort" + z) ? "open" : "dicht") : ""));
        }
        regels.add(" raid: " + (VrFtb.heeft(p, "vr_ftb_raid") ? "af" : VrFtb.raidKlaar(p) ? "klaar, nog niet afgevinkt" : "nog niet"));
        for (String r : regels) {
            c.getSource().sendSuccess(() -> Component.literal(r), false);
        }
        return 1;
    }
}
