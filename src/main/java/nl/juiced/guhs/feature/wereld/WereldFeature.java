package nl.juiced.guhs.feature.wereld;

import java.util.function.Consumer;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Protected;
import nl.juiced.guhs.world.NieuwTerrein;

/**
 * bbq2 (F3 wereld): the world helpers of this update. Nothing here is a thing of its own in the game: it is what the
 * building, story and machine slices build on.
 * <ul>
 *   <li>{@link Bezetting}: quest NPCs and small props also at the copies of a structure that were generated long ago;</li>
 *   <li>{@link Bescherming}: the new quest buildings can't be broken, and "may a machine change this block";</li>
 *   <li>{@link QuestRol}: the role of a quest NPC, a different talk per player ({@link Stappen} = the questline);</li>
 *   <li>{@link Herstel}: what a player changes for a quest comes back by itself;</li>
 *   <li>{@link Kopieen}: the copies of a structure around a spot and where their template blocks are;</li>
 *   <li>(package world) {@code GegarandeerdPlacement} "alleen_nieuw" / "rond", {@code NieuwTerrein}, /guhs bouwcheck gegarandeerd.</li>
 * </ul>
 * tools/features/wereld.py makes the resources (the texts, the tag guhs:wereld/natuurlijk, the test templates) and has the
 * python helpers for new structure sets (wereld.bbq_structuur).
 */
public final class WereldFeature {
    public static void register(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(NieuwTerrein::opSave);
        NeoForge.EVENT_BUS.addListener(Bezetting::opSpelerTick);
        NeoForge.EVENT_BUS.addListener(Bezetting::opJoin);
        NeoForge.EVENT_BUS.addListener(Bezetting::opTem);
        NeoForge.EVENT_BUS.addListener(Herstel::opLevelTick);
        NeoForge.EVENT_BUS.addListener(Bescherming::opBreek);
        NeoForge.EVENT_BUS.addListener(Bescherming::opPlaats);
        NeoForge.EVENT_BUS.addListener(Bescherming::opGebruik);
        NeoForge.EVENT_BUS.addListener(Bescherming::opExplosie);
        NeoForge.EVENT_BUS.addListener(Bescherming::opMobGriefing);
        NeoForge.EVENT_BUS.addListener(Bescherming::opZuiger);
        Protected.add(Bescherming::beschermd);   // (fire and fluids from outside)
        NeoForge.EVENT_BUS.addListener(WereldFeature::commando);
    }

    public static void payloads(PayloadRegistrar registrar) {
    }

    public static void creative(Consumer<ItemStack> output) {
    }

    /**
     * /guhs wereld (operators, for AutoCheck scripts and dev checks): "bezetting" looks at the copies around you now (as if
     * you had been standing there for a while: what is missing comes at once), "beschermd" says whether the block you stand
     * on is in a protected building, "herstel" how many blocks wait to be put back in this dimension, and
     * "kopie &lt;structuur&gt;" says which copy of that structure you are at and what the template coordinates of your spot are
     * (the numbers for Bezetting.npc / blokken: stand where the NPC must come in a real copy).
     * Only in a dev run: "proef &lt;structuur&gt;" tries it out: until the server stops a Wachter-guh belongs at your spot of
     * every copy of that structure, and the structure is protected ("proef weg" undoes it).
     */
    private static void commando(RegisterCommandsEvent event) {
        if (!FMLEnvironment.isProduction()) {
            event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("wereld").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(Commands.literal("proef").then(Commands.argument("structuur", StringArgumentType.word()).executes(c -> proef(c.getSource(),
                            StringArgumentType.getString(c, "structuur")))))));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("wereld").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("bezetting").executes(c -> {
                    CommandSourceStack s = c.getSource();
                    BlockPos pos = BlockPos.containing(s.getPosition());
                    Bezetting.controleer(s.getLevel(), pos);
                    Bezetting.bevestigAlles(s.getLevel());
                    int n = Bezetting.controleer(s.getLevel(), pos);
                    s.sendSuccess(() -> Component.literal("Bezetting: " + n + " erbij rond " + pos.toShortString()), false);
                    return n;
                }))
                .then(Commands.literal("beschermd").executes(c -> {
                    CommandSourceStack s = c.getSource();
                    BlockPos pos = BlockPos.containing(s.getPosition()).below();
                    String naam = Bescherming.structuurBij(s.getLevel(), pos);
                    s.sendSuccess(() -> Component.literal("Beschermd " + pos.toShortString() + ": " + (naam == null ? "nee" : "ja (" + naam + ")")), false);
                    return naam == null ? 0 : 1;
                }))
                .then(Commands.literal("kopie").then(Commands.argument("structuur", StringArgumentType.word()).executes(c -> {
                    CommandSourceStack s = c.getSource();
                    String naam = StringArgumentType.getString(c, "structuur");
                    BlockPos pos = BlockPos.containing(s.getPosition());
                    StructureStart start = Bezetting.start(s.getLevel(), naam, pos);
                    if (start == null) {
                        s.sendFailure(Component.literal("Geen kopie van guhs:" + naam + " binnen " + Bezetting.BEREIK + " blokken van " + pos.toShortString()));
                        return 0;
                    }
                    String stuk = Kopieen.stukBij(start, pos);
                    BlockPos lokaal = Kopieen.lokaal(start, stuk, pos), eerste = Kopieen.lokaal(start, null, pos);
                    s.sendSuccess(() -> Component.literal("Kopie guhs:" + naam + " start " + start.getChunkPos() + ", " + start.getPieces().size() + " stukken, gedraaid "
                            + Kopieen.draai(start, stuk) + "; " + pos.toShortString() + " = stuk " + stuk + " lokaal " + (lokaal == null ? "-" : lokaal.toShortString())
                            + " (startstuk / hele burcht: " + (eerste == null ? "-" : eerste.toShortString()) + "); terug: "
                            + (eerste == null ? "-" : String.valueOf(Kopieen.wereld(start, null, eerste)))), false);
                    return 1;
                })))
                .then(Commands.literal("herstel").executes(c -> {
                    ServerLevel level = c.getSource().getLevel();
                    int n = Herstel.aantal(level);
                    c.getSource().sendSuccess(() -> Component.literal("Herstel: " + n + " wachten in " + level.dimension().identifier()), false);
                    return n;
                }))));
    }

    private static final String PROEF = "wereld_proef";
    private static String proefStructuur;

    /** (dev runs) a try-out registration at the template spot the command is given at; "weg" removes it. */
    private static int proef(CommandSourceStack s, String naam) {
        Bezetting.vergeet(PROEF);
        if (proefStructuur != null) {
            Bescherming.vergeet(proefStructuur);
            proefStructuur = null;
        }
        if (naam.equals("weg")) {
            s.sendSuccess(() -> Component.literal("Proef: weg"), false);
            return 1;
        }
        BlockPos pos = BlockPos.containing(s.getPosition());
        StructureStart start = Bezetting.start(s.getLevel(), naam, pos);
        if (start == null) {
            s.sendFailure(Component.literal("Geen kopie van guhs:" + naam + " binnen " + Bezetting.BEREIK + " blokken van " + pos.toShortString()));
            return 0;
        }
        String stuk = Kopieen.stukBij(start, pos);
        BlockPos lokaal = Kopieen.lokaal(start, stuk, pos);
        if (lokaal == null) {
            s.sendFailure(Component.literal("Deze plek ligt niet in een stuk van de kopie"));
            return 0;
        }
        Bezetting.npc(PROEF, naam, stuk, lokaal, GuhNpcEntity.Kind.WACHTERGUH, null);
        Bescherming.registreer(naam, 0);
        proefStructuur = naam;
        s.sendSuccess(() -> Component.literal("Proef: Bezetting.npc(\"" + PROEF + "\", \"" + naam + "\", " + (stuk == null ? "null" : "\"" + stuk + "\"")
                + ", new BlockPos(" + lokaal.getX() + ", " + lokaal.getY() + ", " + lokaal.getZ() + "), ...) en Bescherming.registreer(\"" + naam
                + "\", 0), tot de server stopt"), false);
        return 1;
    }

    private WereldFeature() {
    }
}
