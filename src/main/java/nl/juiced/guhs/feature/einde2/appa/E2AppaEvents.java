package nl.juiced.guhs.feature.einde2.appa;

import java.util.Collection;
import java.util.List;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.feature.piep.Schouder;

/**
 * einde2, slice appa: its game events. You don't get off a flying Appa-guh; the Momo-guh on a shoulder (his tick, who sees
 * him, sneak + empty hand on a block = he stays there at home); what the death of a player drops is not for Momo for 5
 * seconds; and the op commands {@code /guhs einde2 appa ...} (testing, the autocheck, and the only way to an Appa or a Momo
 * until the story gives them).
 */
public final class E2AppaEvents {
    private E2AppaEvents() {
    }

    // --- the Appa-guh -------------------------------------------------------------------------------------------------------------

    /** Sneaking does not get you off him in the air (there, sneak is "down"): land first. */
    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (event.isDismounting() && !event.getLevel().isClientSide() && event.getEntityBeingMounted() instanceof AppaGuhEntity appa
                && appa.isLucht() && !appa.uitstappen && !appa.isRemoved() && event.getEntityMounting() instanceof ServerPlayer player
                && player.isAlive() && !player.isRemoved() && player.isShiftKeyDown()) {
            event.setCanceled(true);
            if (!appa.isEigenaar(player) || appa.vliegtZelf()) {
                player.sendOverlayMessage(Component.translatable("gui.guhs.einde2_appa.blijf_zitten").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Appa.leeg();
        Momo.leeg();
    }

    // --- the Momo-guh -------------------------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % Momo.STAP == 0) {
            Momo.spelerTick(player);
        }
    }

    /** What the death of a player drops carries whose it was and when ({@link Momo#magRapen}). */
    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            for (ItemEntity item : event.getDrops()) {
                item.getPersistentData().putString(Momo.DOOD_VAN, player.getUUID().toString());
                item.getPersistentData().putLong(Momo.DOOD_TIJD, player.level().getGameTime());
            }
        }
    }

    /**
     * Sneak + an empty hand on a block: the Momo-guh on your shoulder hops down there and stays at home. A piep maatje on
     * the other shoulder goes first (PiepEvents: the same click), so this one waits until that shoulder is empty.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (player.isSecondaryUseActive() && player.getMainHandItem().isEmpty() && Momo.opSchouder(player) && !Schouder.heeft(player)) {
            BlockPos at = event.getPos().relative(event.getFace() == null ? Direction.UP : event.getFace());
            Momo.zetThuis(player, new Vec3(at.getX() + 0.5, at.getY(), at.getZ() + 0.5));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target && event.getEntity() instanceof ServerPlayer watcher && Momo.opSchouder(target)) {
            nl.juiced.guhs.network.ModNetworking.sendTo(watcher, E2AppaPayloads.bericht(target));
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            E2AppaPayloads.sync(player);
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            E2AppaPayloads.sync(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            E2AppaPayloads.sync(player);
        }
    }

    // --- /guhs einde2 appa ... (op) -------------------------------------------------------------------------------------------------

    /**
     * <ul>
     *   <li>{@code geef [spelers]} / {@code momo [spelers]}: gives an Appa-guh / a Momo-guh (what the story does later);</li>
     *   <li>{@code roep}: your Appa comes; {@code weg <true|false>}: he is kept away or not;</li>
     *   <li>{@code vlieg <plek> [stuur]}: he flies you there (with {@code stuur}: you may steer, he points the way);
     *       {@code stop}: breaks that off;</li>
     *   <li>{@code thuis <true|false>}: your Momo sits at home or comes along; {@code jat}: he borrows a knabbel now.</li>
     * </ul>
     */
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        var appa = Commands.literal("appa")
                .then(Commands.literal("geef").executes(c -> geef(c, List.of(c.getSource().getPlayerOrException()), false))
                        .then(Commands.argument("spelers", EntityArgument.players()).executes(c -> geef(c, EntityArgument.getPlayers(c, "spelers"), false))))
                .then(Commands.literal("momo").executes(c -> geef(c, List.of(c.getSource().getPlayerOrException()), true))
                        .then(Commands.argument("spelers", EntityArgument.players()).executes(c -> geef(c, EntityArgument.getPlayers(c, "spelers"), true))))
                .then(Commands.literal("roep").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    return antwoord(c, Appa.roep(p) != null, "geroepen", p);
                }))
                .then(Commands.literal("weg").then(Commands.argument("weg", BoolArgumentType.bool()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    boolean weg = BoolArgumentType.getBool(c, "weg");
                    Appa.houdWeg(p, weg);
                    return antwoord(c, Appa.heeft(p), weg ? "weg" : "niet_weg", p);
                })))
                .then(Commands.literal("vlieg").then(Commands.argument("plek", Vec3Argument.vec3())
                        .executes(c -> vlieg(c, false))
                        .then(Commands.literal("stuur").executes(c -> vlieg(c, true)))))
                .then(Commands.literal("stop").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    boolean vloog = Appa.vliegt(p);
                    Appa.stop(p);
                    return antwoord(c, vloog, "gestopt", p);
                }))
                .then(Commands.literal("thuis").then(Commands.argument("thuis", BoolArgumentType.bool()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    boolean thuis = BoolArgumentType.getBool(c, "thuis");
                    Momo.zetThuis(p, thuis);
                    return antwoord(c, Momo.heeft(p), thuis ? "thuis" : "mee", p);
                })))
                .then(Commands.literal("jat").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    return antwoord(c, Momo.jat(p), "jat", p);
                }));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("einde2")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(appa)));
    }

    private static int geef(CommandContext<CommandSourceStack> c, Collection<ServerPlayer> spelers, boolean momo) {
        for (ServerPlayer p : spelers) {
            if (momo) {
                Momo.geef(p);
            } else {
                Appa.geef(p);
            }
            c.getSource().sendSuccess(() -> Component.translatable("gui.guhs.einde2_appa.commando." + (momo ? "momo" : "gegeven"), p.getDisplayName()), true);
        }
        return spelers.size();
    }

    private static int vlieg(CommandContext<CommandSourceStack> c, boolean stuur) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Vec3 doel = Vec3Argument.getVec3(c, "plek");
        return antwoord(c, Appa.vlieg(p, doel, stuur), "vlieg", p);
    }

    /** "gui.guhs.einde2_appa.commando.&lt;wat&gt;" when it worked, "...commando.mislukt" when not. */
    private static int antwoord(CommandContext<CommandSourceStack> c, boolean gelukt, String wat, ServerPlayer p) {
        if (!gelukt) {
            c.getSource().sendFailure(Component.translatable("gui.guhs.einde2_appa.commando.mislukt", p.getDisplayName()));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.translatable("gui.guhs.einde2_appa.commando." + wat, p.getDisplayName()), false);
        return 1;
    }
}
