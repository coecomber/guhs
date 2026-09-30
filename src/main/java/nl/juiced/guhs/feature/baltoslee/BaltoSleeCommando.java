package nl.juiced.guhs.feature.baltoslee;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.balto.Nomguh;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Op commands of the sled (testing, the autocheck; Brigadier merges them into the /guhs tree):
 * <ul>
 *   <li>{@code /guhs baltoslee vrij [spelers]}: they may race Steele-Mika (as if their Nomguh story were done);</li>
 *   <li>{@code /guhs baltoslee tocht [anker]} / {@code sprint <niveau> [anker]}: a ride over the Nomguh route (the Nomguh near
 *   you, or the route file anchored at this block);</li>
 *   <li>{@code /guhs baltoslee proef [tocht|makkelijk|medium|lastig]}: a test ride over a made-up route in front of you (120
 *   blocks, following the ground; with rest points, an ice bridge, an avalanche and the dieptepunt) - no blocks are placed;</li>
 *   <li>{@code /guhs baltoslee verder} / {@code helder}: what balto does after the berghut / the wolf howl; {@code stop}: ends your ride;</li>
 *   <li>{@code /guhs baltoslee steele <anker>}: a Steele-Mika with the sledesprint at your feet (the route anchored there);</li>
 *   <li>{@code /guhs baltoslee toon}: your own sneeuwslee in front of you.</li>
 * </ul>
 */
public final class BaltoSleeCommando {
    private BaltoSleeCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        var niveau = Commands.argument("niveau", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("makkelijk", "medium", "lastig"), b));
        var proef = Commands.argument("soort", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("tocht", "makkelijk", "medium", "lastig"), b));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("baltoslee").requires(s -> s.hasPermission(2))
                .then(Commands.literal("vrij").executes(c -> vrij(c.getSource(), List.of(c.getSource().getPlayerOrException())))
                        .then(Commands.argument("spelers", EntityArgument.players()).executes(c -> vrij(c.getSource(), new ArrayList<>(EntityArgument.getPlayers(c, "spelers"))))))
                .then(Commands.literal("tocht").executes(c -> tocht(c.getSource(), null))
                        .then(Commands.argument("anker", BlockPosArgument.blockPos()).executes(c -> tocht(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "anker")))))
                .then(Commands.literal("sprint").then(niveau.executes(c -> sprint(c.getSource(), StringArgumentType.getString(c, "niveau"), null))
                        .then(Commands.argument("anker", BlockPosArgument.blockPos())
                                .executes(c -> sprint(c.getSource(), StringArgumentType.getString(c, "niveau"), BlockPosArgument.getLoadedBlockPos(c, "anker"))))))
                .then(Commands.literal("proef").executes(c -> proef(c.getSource(), "tocht"))
                        .then(proef.executes(c -> proef(c.getSource(), StringArgumentType.getString(c, "soort")))))
                .then(Commands.literal("verder").executes(c -> {
                    SleeTocht.verder(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("stop").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    SleeRit rit = SleeRit.van(p);
                    if (rit != null) {
                        rit.einde(p.serverLevel(), SleeRit.Einde.GESTOPT);
                    }
                    return rit != null ? 1 : 0;
                }))
                .then(Commands.literal("helder").executes(c -> {
                    SleeTocht.stormKlaartOp(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("steele").then(Commands.argument("anker", BlockPosArgument.blockPos())
                        .executes(c -> steele(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "anker")))))
                .then(Commands.literal("toon").executes(c -> toon(c.getSource())))));
    }

    private static int vrij(CommandSourceStack source, List<ServerPlayer> spelers) {
        for (ServerPlayer p : spelers) {
            SleeRit.data(p).putBoolean("Vrij", true);
        }
        source.sendSuccess(() -> Component.literal("De sledesprint is vrij voor " + spelers.size() + " speler(s). Njeg!"), true);
        return spelers.size();
    }

    private static NomguhRoute route(CommandSourceStack source, ServerPlayer p, BlockPos anker) {
        BlockPos a = anker != null ? anker : Nomguh.anker(p.serverLevel(), p.blockPosition());
        if (a == null) {
            source.sendFailure(Component.literal("Njeg: geen Nomguh in de buurt. Geef een anker op, of probeer /guhs baltoslee proef."));
            return null;
        }
        return NomguhRoute.laad().in(a);
    }

    private static int tocht(CommandSourceStack source, BlockPos anker) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        NomguhRoute r = route(source, p, anker);
        if (r == null) {
            return 0;
        }
        return SleeTocht.startMet(p, r, null) ? 1 : 0;
    }

    private static int sprint(CommandSourceStack source, String niveau, BlockPos anker) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        NomguhRoute r = route(source, p, anker);
        if (r == null) {
            return 0;
        }
        return SleeRit.start(p, r, SleeRit.Modus.SPRINT, Niveau.byId(niveau), null, null) != null ? 1 : 0;
    }

    private static int proef(CommandSourceStack source, String soort) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        NomguhRoute r = proefRoute(p.blockPosition(), p.getYRot(), 30);
        SleeRit rit = soort.equals("tocht") ? (SleeTocht.startMet(p, r, null) ? SleeRit.van(p) : null)
                : SleeRit.start(p, r, SleeRit.Modus.SPRINT, Niveau.byId(soort), null, null);
        if (rit == null) {
            source.sendFailure(Component.literal("Njeg: je bent al met iets anders bezig."));
            return 0;
        }
        return 1;
    }

    /**
     * A made-up route of n points (every 4 blocks) from start in direction yaw, gently winding: rest points at 7 and 19, an
     * ice bridge 11-13, an avalanche 15-17 from the left, the dieptepunt at 22.
     */
    public static NomguhRoute proefRoute(BlockPos start, float yaw, int n) {
        Vec3 dir = Vec3.directionFromRotation(0, yaw);
        Vec3 side = new Vec3(-dir.z, 0, dir.x);
        List<Vec3> punten = new ArrayList<>();
        List<Double> breedte = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            Vec3 p = Vec3.atLowerCornerOf(start).add(dir.scale(3 + i * 4.0)).add(side.scale(Math.sin(i * 0.45) * 5));
            punten.add(new Vec3(Math.floor(p.x), start.getY(), Math.floor(p.z)));
            breedte.add(i >= 11 && i <= 13 ? 1.3 : 2.6);
        }
        List<Vec3> terug = new ArrayList<>(punten);
        java.util.Collections.reverse(terug);
        BlockPos eind = BlockPos.containing(punten.get(n - 1));
        return new NomguhRoute(start, List.copyOf(punten), List.copyOf(terug), List.copyOf(breedte), start, start.offset(2, 0, 2), eind,
                List.of(7, 19), List.of(new int[]{11, 13}), List.of(new NomguhRoute.Lawine(15, 17, true)), 22, n * 4.0,
                Map.of("makkelijk", 2400, "medium", 1800, "lastig", 1400));
    }

    private static int steele(CommandSourceStack source, BlockPos anker) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        ServerLevel level = p.serverLevel();
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level);
        if (npc == null) {
            return 0;
        }
        npc.setKind(GuhNpcEntity.Kind.STEELE_MIKA);
        npc.roleData.putString(nl.juiced.guhs.feature.verhaal.NpcRollen.PLEK, SteeleSprint.PLEK);
        npc.roleData.putLong(SteeleSprint.ANKER, anker.asLong());
        npc.moveTo(p.getX(), p.getY(), p.getZ(), p.getYRot() + 180, 0);
        npc.setPersistenceRequired();
        level.addFreshEntity(npc);
        source.sendSuccess(() -> Component.literal("Steele-Mika staat klaar voor de sledesprint. Njeh-heh!"), true);
        return 1;
    }

    private static int toon(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        SneeuwsleeEntity s = BaltoSleeFeature.SNEEUWSLEE_ENTITY.get().create(p.serverLevel());
        if (s == null) {
            return 0;
        }
        Vec3 at = p.position().add(Vec3.directionFromRotation(0, p.getYRot()).scale(3));
        s.moveTo(at.x, p.getY(), at.z, p.getYRot() + 90, 0);
        s.zetEigenaar(p.getUUID());
        p.serverLevel().addFreshEntity(s);
        return 1;
    }
}
