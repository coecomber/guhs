package nl.juiced.guhs.feature.guhpixel.reisbureau;

import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.guhpixel.Klok;
import nl.juiced.guhs.feature.guhpixel.PxData;

/**
 * Dev commands of the Reisbureau (gamemasters): {@code /guhs px reisbureau ...}
 * {@code stap <0..3>} (the questline) · {@code open} (the trip screen, without a balie: looking only) · {@code aanbod}
 * (today's and tomorrow's trips) · {@code terug} (the running trip is over now) · {@code toon} (the player's record) ·
 * {@code souvenirs alles|wis} (the album) · {@code stempels <n>} · {@code kaart <bestemming>} · {@code spullen} (a balie, a
 * stamp, the three koffertje things) · {@code guh} (a tamed guh next to you) · {@code stuur} (books today's shortest trip, or
 * the proefreisje, for your nearest guh). Skipping time: {@code /guhs px klok <uren>}.
 */
public final class ReisCommando {
    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> r = Commands.literal("reisbureau");
        r.then(Commands.literal("stap").then(Commands.argument("n", IntegerArgumentType.integer(0, Reizen.KLAAR)).executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Reizen.zetStap(p, IntegerArgumentType.getInteger(ctx, "n"));
            return zeg(ctx.getSource(), "stap " + Reizen.stap(p));
        })));
        r.then(Commands.literal("open").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Reizen.open(p, p.blockPosition(), null);
            return 1;
        }));
        r.then(Commands.literal("aanbod").executes(ctx -> {
            long seed = ctx.getSource().getServer().overworld().getSeed();
            StringBuilder sb = new StringBuilder("dag " + Klok.dag() + ":");
            for (long dag = Klok.dag(); dag < Klok.dag() + 4; dag++) {
                sb.append(dag == Klok.dag() ? " vandaag" : " | +" + (dag - Klok.dag()));
                for (Bestemming b : Reizen.aanbod(seed, dag)) {
                    sb.append(' ').append(b.id());
                }
            }
            return zeg(ctx.getSource(), sb.toString());
        }));
        r.then(Commands.literal("terug").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            CompoundTag reis = Reizen.reis(p.level().getServer(), p.getUUID());
            if (reis == null) {
                return zeg(ctx.getSource(), "niemand op reis");
            }
            reis.putLong("Terug", Klok.nu());
            PxData.vuil(p.level().getServer());
            return zeg(ctx.getSource(), "de reis is voorbij: ophalen bij een Reisbalie");
        }));
        r.then(Commands.literal("toon").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            CompoundTag d = Reizen.data(p).copy();
            if (d.contains("Reis")) {
                d.getCompoundOrEmpty("Reis").remove("Opslag");   // (the whole guh: too long to read)
                d.getCompoundOrEmpty("Reis").remove("Looks");
            }
            return zeg(ctx.getSource(), d.toString());
        }));
        r.then(Commands.literal("souvenirs")
                .then(Commands.literal("alles").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    CompoundTag s = PxData.sub(Reizen.data(p), "Souvenirs");
                    for (Souvenirs.Soort soort : Souvenirs.ALLE) {
                        if (soort.souvenir() && s.getIntOr(soort.id(), 0) == 0) {
                            s.putInt(soort.id(), 1);
                        }
                    }
                    PxData.vuil(p.level().getServer());
                    return zeg(ctx.getSource(), "album vol: " + Reizen.souvenirs(Reizen.data(p)));
                }))
                .then(Commands.literal("wis").executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    CompoundTag d = Reizen.data(p);
                    for (String k : List.of("Souvenirs", "Kaarten", "Stempels", "StempelsTotaal", "Koffertjes", "Reizen", "Zeldzaam")) {
                        d.remove(k);
                    }
                    PxData.vuil(p.level().getServer());
                    return zeg(ctx.getSource(), "album en reispas leeg (een lopende reis blijft)");
                })));
        r.then(Commands.literal("stempels").then(Commands.argument("n", IntegerArgumentType.integer(0, Reizen.STEMPELS_VOL - 1)).executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Reizen.data(p).putInt("Stempels", IntegerArgumentType.getInteger(ctx, "n"));
            PxData.vuil(p.level().getServer());
            return zeg(ctx.getSource(), "stempels " + Reizen.data(p).getIntOr("Stempels", 0));
        })));
        r.then(Commands.literal("kaart").then(Commands.argument("bestemming", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(Bestemming.values()).map(Bestemming::id), b))
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    Bestemming b = Bestemming.vanId(StringArgumentType.getString(ctx, "bestemming"));
                    if (b == null) {
                        ctx.getSource().sendFailure(Component.literal("onbekende bestemming"));
                        return 0;
                    }
                    Minigames.give(p, KaartItem.maak(b, Component.literal("Vadsje")));
                    return 1;
                })));
        r.then(Commands.literal("spullen").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            Minigames.give(p, new ItemStack(ReisbureauSlice.BALIE_ITEM.get()));
            Minigames.give(p, new ItemStack(ReisbureauSlice.STEMPEL.get()));
            Minigames.give(p, new ItemStack(nl.juiced.guhs.registry.ModItems.KAAS_KNABBELS.get(), Reisagent.KNABBELS));
            Minigames.give(p, new ItemStack(net.minecraft.world.item.Items.PINK_WOOL, Reisagent.WOL));
            Minigames.give(p, new ItemStack(net.minecraft.world.item.Items.PAPER, Reisagent.PAPIER));
            return 1;
        }));
        r.then(Commands.literal("guh").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            nl.juiced.guhs.entity.GuhEntity guh = nl.juiced.guhs.registry.ModEntities.GUH.get().create(p.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if (guh == null) {
                return 0;
            }
            guh.snapTo(p.getX() + 1.5, p.getY(), p.getZ(), p.getYRot(), 0);
            guh.tame(p);
            guh.setCustomName(Component.literal("Vadsje"));
            p.level().addFreshEntity(guh);
            return zeg(ctx.getSource(), "een tamme guh: Vadsje");
        }));
        r.then(Commands.literal("stuur").executes(ctx -> {
            // today's shortest trip (or the proefreisje) for the nearest own guh, from the nearest balie
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            nl.juiced.guhs.entity.GuhEntity dichtst = null;
            for (nl.juiced.guhs.entity.GuhEntity g : p.level().getEntitiesOfClass(nl.juiced.guhs.entity.GuhEntity.class, p.getBoundingBox().inflate(Reizen.BEREIK),
                    x -> p.getUUID().equals(x.getOwnerUUID()))) {
                dichtst = dichtst == null || g.distanceToSqr(p) < dichtst.distanceToSqr(p) ? g : dichtst;
            }
            if (dichtst == null) {
                ctx.getSource().sendFailure(Component.literal("geen eigen guh in de buurt (/guhs px reisbureau guh)"));
                return 0;
            }
            net.minecraft.core.BlockPos balie = null;
            for (net.minecraft.core.BlockPos pos : net.minecraft.core.BlockPos.betweenClosed(p.blockPosition().offset(-8, -3, -8), p.blockPosition().offset(8, 3, 8))) {
                if (p.level().getBlockState(pos).getBlock() instanceof BalieBlock) {
                    balie = pos.immutable();
                    break;
                }
            }
            Bestemming b = Reizen.stap(p) == Reizen.PROEF ? Bestemming.OM_DE_HOEK : Reizen.aanbod(p.level().getServer()).get(0);
            Reizen.Uitkomst u = Reizen.boek(p, dichtst.getUUID(), b, balie);
            return zeg(ctx.getSource(), "boek " + b.id() + ": " + u);
        }));
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("px")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).then(r)));
    }

    private static int zeg(CommandSourceStack source, String tekst) {
        source.sendSuccess(() -> Component.literal(tekst), false);
        return 1;
    }

    private ReisCommando() {
    }
}
