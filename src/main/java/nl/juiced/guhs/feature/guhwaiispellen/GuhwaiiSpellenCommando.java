package nl.juiced.guhs.feature.guhwaiispellen;

import java.util.Comparator;

import javax.annotation.Nullable;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.spelen.Niveau;
import nl.juiced.guhs.feature.verhaal.NpcRollen;

/**
 * Op commands of the surf beach (for testing and the AutoCheck scripts; Brigadier merges them into the /guhs tree):
 * {@code /guhs guhwaiispellen surf <makkelijk|medium|lastig>} starts surfing with the nearest surf Lilo-guh,
 * {@code /guhs guhwaiispellen hula <makkelijk|medium|lastig>} a dance with the nearest hula Lilo-guh,
 * {@code /guhs guhwaiispellen stop} ends your game, {@code /guhs guhwaiispellen munten <n>} gives schelpjesmunten.
 */
public final class GuhwaiiSpellenCommando {
    public static final double BEREIK = 96;

    static void register(RegisterCommandsEvent event) {
        var root = Commands.literal("guhwaiispellen").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        for (Niveau n : Niveau.values()) {
            root.then(Commands.literal("surf").then(Commands.literal(n.id()).executes(ctx -> surf(ctx.getSource(), n))));
            root.then(Commands.literal("hula").then(Commands.literal(n.id()).executes(ctx -> hula(ctx.getSource(), n))));
        }
        root.then(Commands.literal("stop").executes(ctx -> stop(ctx.getSource())));
        root.then(Commands.literal("spot").executes(ctx -> spot(ctx.getSource())));
        root.then(Commands.literal("munten").then(Commands.argument("n", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 999))
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    int n = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "n");
                    p.getInventory().add(new ItemStack(GuhwaiiSpellenBlocks.SCHELPJESMUNT.get(), n));
                    return n;
                })));
        event.getDispatcher().register(Commands.literal("guhs").then(root));
    }

    @Nullable
    private static GuhNpcEntity lilo(ServerPlayer p, String plek) {
        return p.level().getEntitiesOfClass(GuhNpcEntity.class, p.getBoundingBox().inflate(BEREIK),
                        n -> n.getKind() == GuhNpcEntity.Kind.LILO_GUH && plek.equals(n.roleData.getStringOr(NpcRollen.PLEK, "")))
                .stream().min(Comparator.comparingDouble(n -> n.distanceToSqr(p))).orElse(null);
    }

    private static int surf(CommandSourceStack source, Niveau n) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        GuhNpcEntity lilo = lilo(p, "surf");
        if (lilo == null) {
            source.sendFailure(Component.literal("Njeg: geen surf-Lilo-guh in de buurt."));
            return 0;
        }
        if (p.isSpectator()) {
            p.setGameMode(GameType.CREATIVE);
        }
        return SurfSpel.start(lilo, p, n) ? 1 : 0;
    }

    private static int hula(CommandSourceStack source, Niveau n) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        GuhNpcEntity lilo = lilo(p, "hula");
        if (lilo == null) {
            source.sendFailure(Component.literal("Njeg: geen hula-Lilo-guh in de buurt."));
            return 0;
        }
        if (p.isSpectator()) {
            p.setGameMode(GameType.CREATIVE);
        }
        return HulaSpel.of(lilo).start(lilo, p, HulaLiedje.of(n)) ? 1 : 0;
    }

    /** (Worldgen checks, also without a player) the surf spot of the nearest surf Lilo-guh and how much of the waves is over water. */
    private static int spot(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        var pos = source.getPosition();
        GuhNpcEntity lilo = level.getEntitiesOfClass(GuhNpcEntity.class, new net.minecraft.world.phys.AABB(pos, pos).inflate(BEREIK),
                        n -> n.getKind() == GuhNpcEntity.Kind.LILO_GUH && "surf".equals(n.roleData.getStringOr(NpcRollen.PLEK, "")))
                .stream().min(Comparator.comparingDouble(n -> n.distanceToSqr(pos))).orElse(null);
        if (lilo == null) {
            source.sendFailure(Component.literal("Njeg: geen surf-Lilo-guh binnen " + (int) BEREIK + " blokken."));
            return 0;
        }
        Surfplek.Spot spot = Surfplek.zoek(level, lilo.blockPosition());
        if (spot == null) {
            source.sendFailure(Component.literal("surfspot: GEEN open water bij Lilo-guh op " + lilo.blockPosition().toShortString()));
            return 0;
        }
        int water = 0, alles = 0;
        for (double u = SurfGolven.U_EIND; u <= SurfGolven.U_START; u += 2) {
            for (double v = -SurfGolven.HALF; v <= SurfGolven.HALF; v += 2) {
                var w = spot.wereld(u, v, 0);
                var b = net.minecraft.core.BlockPos.containing(w.x, w.y - 0.5, w.z);
                alles++;
                if (level.getFluidState(b).is(net.minecraft.tags.FluidTags.WATER)) {
                    water++;
                }
            }
        }
        int pct = water * 100 / Math.max(1, alles);
        var line = spot.wereld(SurfGolven.U_LINE, 0, 0);
        source.sendSuccess(() -> Component.literal("surfspot: Lilo " + lilo.blockPosition().toShortString() + " origin " + spot.origin().toShortString()
                + " richting " + Math.round(Math.toDegrees(spot.hoek())) + " graden, line-up " + (int) line.x + " " + (int) line.z
                + ", water onder de golven " + pct + "%"), false);
        return pct;
    }

    private static int stop(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = source.getPlayerOrException();
        SurfSpel s = SurfSpel.van(p);
        if (s != null) {
            s.einde((ServerLevel) p.level(), p, true);
            return 1;
        }
        var npcId = HulaSpel.npcVan(p);
        if (npcId != null && ((ServerLevel) p.level()).getEntity(npcId) instanceof GuhNpcEntity npc) {
            HulaSpel.of(npc).klaar(npc, p);
            return 1;
        }
        source.sendFailure(Component.literal("Je surft en danst niet."));
        return 0;
    }

    private GuhwaiiSpellenCommando() {
    }
}
