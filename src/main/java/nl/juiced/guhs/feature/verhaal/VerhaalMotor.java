package nl.juiced.guhs.feature.verhaal;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.quest.GuhDex;

/**
 * bbq2 (verhaal engine): the game-bus side of the engine (registered by {@link VerhaalFeature#register}): the story state
 * goes to a player at login, after a respawn and a change of dimension, and whenever it changed without a step; and the op
 * commands {@code /guhs verhaal ...} for dev checks and AutoCheck scripts:
 * <pre>
 * /guhs verhaal stap &lt;lijn&gt; &lt;speler&gt; [n]     read / set a step (forward only)
 * /guhs verhaal wis &lt;lijn&gt; &lt;speler&gt;          forget a questline
 * /guhs verhaal scene &lt;id&gt; [draai 0-3]       play a cutscene, anchored on the block you stand on
 * /guhs verhaal kaart &lt;id&gt;                   show a narrator card
 * /guhs verhaal herbekijk scene|kaart &lt;id&gt;   what the Guhdex button does
 * /guhs verhaal vergeet scene|kaart &lt;id&gt;     forget that you saw it
 * /guhs verhaal doel                         where your story points now
 * /guhs verhaal demo [open|wis]              (dev) the demo story: {@link VerhaalDemo}
 * </pre>
 */
public final class VerhaalMotor {
    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            GuhDex.onthoudKenner(p);
            VerhaalSync.sync(p);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            VerhaalSync.sync(p);
        }
    }

    @SubscribeEvent
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            VerhaalSync.sync(p);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        VerhaalSync.vergeet(event.getEntity().getUUID());
        Sluiers.vergeet(event.getEntity().getUUID());
        Doelen.vergeet(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer p && (p.tickCount + p.getId()) % VerhaalSync.CHECK_TICKS == 0) {
            VerhaalSync.kijk(p);
        }
    }

    // --- /guhs verhaal ... (ops) -----------------------------------------------------------------------------------------
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        var verhaal = Commands.literal("verhaal").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("stap").then(Commands.argument("lijn", StringArgumentType.word()).suggests((c, b) -> {
                    Verhaallijnen.alle().forEach(l -> b.suggest(l.id()));
                    return b.buildFuture();
                }).then(Commands.argument("speler", EntityArgument.player())
                        .executes(c -> {
                            Verhaallijn l = lijn(c);
                            ServerPlayer p = EntityArgument.getPlayer(c, "speler");
                            int s = l.stap(p);
                            c.getSource().sendSuccess(() -> Component.literal(p.getScoreboardName() + ": " + l.id() + " step " + s + " of " + l.stappen()
                                    + " (" + l.sleutel(p) + ")"), false);
                            return s;
                        })
                        .then(Commands.argument("stap", IntegerArgumentType.integer(0)).executes(c -> {
                            Verhaallijn l = lijn(c);
                            ServerPlayer p = EntityArgument.getPlayer(c, "speler");
                            int s = IntegerArgumentType.getInteger(c, "stap");
                            boolean gezet = l.zet(p, s);
                            c.getSource().sendSuccess(() -> Component.literal(p.getScoreboardName() + ": " + l.id() + (gezet ? " -> step " + l.stap(p)
                                    : " stays at step " + l.stap(p) + " (forward only)")), true);
                            return l.stap(p);
                        })))))
                .then(Commands.literal("wis").then(Commands.argument("lijn", StringArgumentType.word()).then(Commands.argument("speler", EntityArgument.player())
                        .executes(c -> {
                            Verhaallijn l = lijn(c);
                            ServerPlayer p = EntityArgument.getPlayer(c, "speler");
                            l.wis(p);
                            c.getSource().sendSuccess(() -> Component.literal("questline " + l.id() + " of " + p.getScoreboardName() + " forgotten"), true);
                            return 1;
                        }))))
                .then(Commands.literal("scene").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> {
                    Cutscene.alle().forEach(s -> b.suggest(s.id()));
                    return b.buildFuture();
                }).executes(c -> scene(c, 0)).then(Commands.argument("draai", IntegerArgumentType.integer(0, 3))
                        .executes(c -> scene(c, IntegerArgumentType.getInteger(c, "draai"))))))
                .then(Commands.literal("kaart").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> {
                    Verteller.ids().forEach(b::suggest);
                    return b.buildFuture();
                }).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String id = StringArgumentType.getString(c, "id");
                    boolean ok = Verteller.toon(p, id, null);
                    c.getSource().sendSuccess(() -> Component.literal(ok ? "narrator card " + id : "no card " + id + " (or already watching)"), false);
                    return ok ? 1 : 0;
                })))
                .then(Commands.literal("herbekijk").then(Commands.argument("soort", StringArgumentType.word()).then(Commands.argument("id", StringArgumentType.word())
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            String id = StringArgumentType.getString(c, "id");
                            if (VerhaalPayloads.KAART.equals(StringArgumentType.getString(c, "soort"))) {
                                Verteller.herbekijk(p, id);
                            } else {
                                Cutscenes.herbekijk(p, id);
                            }
                            return Cutscenes.bezig(p) ? 1 : 0;
                        }))))
                .then(Commands.literal("vergeet").then(Commands.argument("soort", StringArgumentType.word()).then(Commands.argument("id", StringArgumentType.word())
                        .executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            String id = StringArgumentType.getString(c, "id");
                            if (VerhaalPayloads.KAART.equals(StringArgumentType.getString(c, "soort"))) {
                                Verteller.vergeet(p, id);
                            } else {
                                Cutscenes.vergeet(p, id);
                            }
                            return 1;
                        }))))
                .then(Commands.literal("doel").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Verhaallijn l = Verhaallijnen.gevolgd(p);
                    Doel d = Doelen.van(p);
                    BlockPos wijs = Doelen.wijs(p);
                    c.getSource().sendSuccess(() -> Component.literal("follows " + (l == null ? "nothing" : l.id() + " (step " + l.stap(p) + ")") + ", goal ")
                            .append(d == null ? Component.literal("none") : d.tekst())
                            .append(Component.literal(wijs == null ? ", the compass points nowhere" : ", the compass points to " + wijs.toShortString())), false);
                    return wijs == null ? 0 : 1;
                }));
        if (VerhaalDemo.AAN) {
            verhaal.then(Commands.literal("demo").executes(c -> VerhaalDemo.speel(c.getSource().getPlayerOrException()))
                    .then(Commands.literal("open").executes(c -> VerhaalDemo.open(c.getSource().getPlayerOrException())))
                    .then(Commands.literal("sluier").executes(c -> VerhaalDemo.sluier(c.getSource().getPlayerOrException())))
                    .then(Commands.literal("wis").executes(c -> VerhaalDemo.wis(c.getSource().getPlayerOrException()))));
        }
        event.getDispatcher().register(Commands.literal("guhs").then(verhaal));
    }

    private static Verhaallijn lijn(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        Verhaallijn l = Verhaallijnen.van(StringArgumentType.getString(c, "lijn"));
        if (l == null) {
            throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("no such questline")).create();
        }
        return l;
    }

    private static int scene(CommandContext<CommandSourceStack> c, int draai) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Cutscene s = Cutscene.van(StringArgumentType.getString(c, "id"));
        if (s == null) {
            throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("no such cutscene")).create();
        }
        boolean ok = Cutscenes.speel(p, s, p.blockPosition(), Rotation.values()[draai], null);
        c.getSource().sendSuccess(() -> Component.literal(ok ? "cutscene " + s.id() + " (" + s.duur() + " ticks)" : "already watching something"), false);
        return ok ? 1 : 0;
    }

    private VerhaalMotor() {
    }
}
