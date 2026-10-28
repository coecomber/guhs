package nl.juiced.guhs.feature.slaapzorg;

import java.util.List;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;

/**
 * 1.8.0 test build, op commands for slaapzorg (permission level 2; merged into the /guhs tree). All act on the guhs within
 * 16 blocks:
 * <ul>
 *   <li>{@code /guhs slaapzorg moe}: a yawn and a nod-off right now (as in the evening);</li>
 *   <li>{@code /guhs slaapzorg slaap}: sung asleep on the spot (until the morning at night, a nap by day);</li>
 *   <li>{@code /guhs slaapzorg liedje}: the lullaby here, as if a Muziekdoosje played (yawns, then they fall asleep);</li>
 *   <li>{@code /guhs slaapzorg wakker}: everyone awake (with a yawn);</li>
 *   <li>{@code /guhs slaapzorg plekjes}: how many sleeping spots (#guhs:guh_slaapplekken) there are around, and who sleeps;</li>
 *   <li>{@code /guhs slaapzorg spullen}: one of every slaapzorg block.</li>
 * </ul>
 */
public final class SlaapzorgCommando {
    static final double BEREIK = 16;

    private SlaapzorgCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("slaapzorg").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("moe").executes(c -> moe(c.getSource())))
                .then(Commands.literal("slaap").executes(c -> slaap(c.getSource())))
                .then(Commands.literal("liedje").executes(c -> liedje(c.getSource())))
                .then(Commands.literal("wakker").executes(c -> wakker(c.getSource())))
                .then(Commands.literal("plekjes").executes(c -> plekjes(c.getSource())))
                .then(Commands.literal("spullen").executes(c -> spullen(c.getSource())))));
    }

    private static List<GuhEntity> guhs(CommandSourceStack source) {
        return source.getLevel().getEntitiesOfClass(GuhEntity.class, AABB.ofSize(source.getPosition(), BEREIK * 2, BEREIK, BEREIK * 2), GuhEntity::isAlive);
    }

    private static int moe(CommandSourceStack source) {
        int n = 0;
        for (GuhEntity g : guhs(source)) {
            if (g.emotes.start(Emote.GAPEN, false, GuhEmotes.Source.SELF)) {
                g.getPersistentData().putLong(Moeheid.KNIK_TOT, g.level().getGameTime() + Emote.GAPEN.onceTicks + 36);
                n++;
            }
        }
        int aantal = n;
        source.sendSuccess(() -> Component.literal("Gapende guhs: " + aantal), false);
        return n;
    }

    private static int slaap(CommandSourceStack source) {
        int n = 0;
        for (GuhEntity g : guhs(source)) {
            if (Slapen.slaapIn(g)) {
                n++;
            }
        }
        int aantal = n;
        source.sendSuccess(() -> Component.literal("In slaap: " + aantal + " guhs. Sst!"), false);
        return n;
    }

    private static int liedje(CommandSourceStack source) {
        int n = Slaapliedje.zing(source.getLevel(), BlockPos.containing(source.getPosition()), Slapen.LIEDJE_BEREIK, 0.9f);
        source.sendSuccess(() -> Component.literal("Slaapliedje voor " + n + " guhs"), false);
        return n;
    }

    private static int wakker(CommandSourceStack source) {
        int n = 0;
        for (GuhEntity g : guhs(source)) {
            if (Slapen.slaapt(g)) {
                Slapen.wordWakker(g, true);
                n++;
            }
        }
        int aantal = n;
        source.sendSuccess(() -> Component.literal("Wakker gemaakt: " + aantal), false);
        return n;
    }

    private static int plekjes(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos hier = BlockPos.containing(source.getPosition());
        int plekken = 0;
        for (BlockPos p : BlockPos.betweenClosed(hier.offset(-16, -4, -16), hier.offset(16, 4, 16))) {
            if (Slapen.isSlaapplek(level.getBlockState(p))) {
                plekken++;
            }
        }
        long slapers = guhs(source).stream().filter(Slapen::slaapt).count();
        int pl = plekken;
        source.sendSuccess(() -> Component.literal("Slaapplekjes (#guhs:guh_slaapplekken, zonder nestjes): " + pl + ", slapende guhs: " + slapers), false);
        return plekken;
    }

    private static int spullen(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        for (DeferredBlock<? extends Block> b : SlaapzorgFeature.ALLE) {
            Minigames.give(p, new ItemStack(b.get()));
        }
        source.sendSuccess(() -> Component.literal("Slaapzorg-spullen: " + SlaapzorgFeature.ALLE.size()), false);
        return 1;
    }
}
