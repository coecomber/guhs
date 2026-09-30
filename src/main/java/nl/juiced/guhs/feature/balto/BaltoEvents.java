package nl.juiced.guhs.feature.balto;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.baltoslee.SleeTocht;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * The game-bus side of the balto slice: the title "Held van Nomguh" in the player list, the sick guh babies' sneezes in the
 * ziekenhuisje (template guhs with {@code guhs_balto_ziek}), the "all four outfits" advancement, and the op commands
 * {@code /guhs balto ...} (stap, moment, wis, nomguh) for tests and scripts.
 */
public final class BaltoEvents {
    /** Template guhs with this persistent flag are the sick babies (they sneeze now and then). */
    public static final String ZIEK = "guhs_balto_ziek";
    static final String OUTFITS = "guhs_balto_outfits";

    // --- the title --------------------------------------------------------------------------------------------------------
    @SubscribeEvent
    public static void onTabName(PlayerEvent.TabListNameFormat event) {
        if (event.getEntity() instanceof ServerPlayer player && BaltoVerhaal.isHeld(player)) {
            Component base = event.getDisplayName() != null ? event.getDisplayName() : player.getName();
            event.setDisplayName(base.copy().append(Component.literal(" ✿ ").withStyle(ChatFormatting.AQUA))
                    .append(Component.translatable("gui.guhs.balto.titel").withStyle(ChatFormatting.AQUA)));
        }
    }

    // --- the sick babies (GuhHooks.tick) ------------------------------------------------------------------------------------
    static void guhTick(GuhEntity guh) {
        if ((guh.tickCount + guh.getId()) % 90 != 0 || guh.level().isClientSide() || !guh.getPersistentData().getBooleanOr(ZIEK, false)) {
            return;
        }
        if (guh.getRandom().nextInt(4) == 0 && guh.level() instanceof ServerLevel level && level.getNearestPlayer(guh, 14) != null) {
            level.playSound(null, guh.blockPosition(), BaltoFeature.HATSJOE.get(), SoundSource.NEUTRAL, 0.35f, 1.5f + guh.getRandom().nextFloat() * 0.3f);
            var look = guh.getLookAngle();
            level.sendParticles(ParticleTypes.CLOUD, guh.getX() + look.x * 0.3, guh.getEyeY() - 0.05, guh.getZ() + look.z * 0.3, 2, 0.03, 0.03, 0.03, 0.01);
        }
    }

    // --- all four outfits -----------------------------------------------------------------------------------------------------
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.tickCount % 100 != 37 || !BaltoVerhaal.isHeld(p)
                || GuhQuests.saved(p).getBooleanOr(OUTFITS, false)) {
            return;
        }
        for (GuhClothes c : BaltoFeature.KLEDING) {
            if (!KledingUnlocks.heeft(p, c)) {
                return;
            }
        }
        GuhQuests.saved(p).putBoolean(OUTFITS, true);
        BaltoVerhaal.grant(p, "balto_outfits");
    }

    // --- /guhs balto ... (ops) ---------------------------------------------------------------------------------------------------
    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("balto").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("stap").then(Commands.argument("speler", EntityArgument.player())
                        .executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "speler");
                            int s = BaltoVerhaal.stap(p);
                            c.getSource().sendSuccess(() -> Component.literal(p.getScoreboardName() + ": stap " + s + " (" + BaltoVerhaal.naam(s) + ")"), false);
                            return s;
                        })
                        .then(Commands.argument("stap", IntegerArgumentType.integer(0, BaltoVerhaal.KLAAR)).executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "speler");
                            int s = IntegerArgumentType.getInteger(c, "stap");
                            BaltoVerhaal.zet(p, s);
                            c.getSource().sendSuccess(() -> Component.literal(p.getScoreboardName() + " -> stap " + s + " (" + BaltoVerhaal.naam(s) + ")"), true);
                            return s;
                        }))))
                .then(Commands.literal("moment").then(Commands.argument("speler", EntityArgument.player())
                        .then(Commands.argument("moment", StringArgumentType.word()).suggests((c, b) -> {
                            for (SleeTocht.Moment m : SleeTocht.Moment.values()) {
                                b.suggest(m.name().toLowerCase(java.util.Locale.ROOT));
                            }
                            return b.buildFuture();
                        }).executes(c -> {
                            ServerPlayer p = EntityArgument.getPlayer(c, "speler");
                            SleeTocht.Moment m = SleeTocht.Moment.valueOf(StringArgumentType.getString(c, "moment").toUpperCase(java.util.Locale.ROOT));
                            BaltoVerhaal.opMoment(p, m);
                            c.getSource().sendSuccess(() -> Component.literal("moment " + m + " for " + p.getScoreboardName()), true);
                            return 1;
                        }))))
                .then(Commands.literal("wis").then(Commands.argument("speler", EntityArgument.player()).executes(c -> {
                    ServerPlayer p = EntityArgument.getPlayer(c, "speler");
                    BaltoVerhaal.wis(p);
                    c.getSource().sendSuccess(() -> Component.literal("balto questline of " + p.getScoreboardName() + " forgotten"), true);
                    return 1;
                })))
                .then(Commands.literal("nomguh").executes(c -> {
                    ServerLevel level = c.getSource().getLevel();
                    BlockPos a = Nomguh.anker(level, BlockPos.containing(c.getSource().getPosition()));
                    c.getSource().sendSuccess(() -> Component.literal(a == null ? "no Nomguh within " + Nomguh.ZOEK
                            : "Nomguh anchor at " + a.getX() + " " + a.getY() + " " + a.getZ()), false);
                    return a == null ? 0 : 1;
                }))));
    }

    private BaltoEvents() {
    }
}
