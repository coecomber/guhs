package nl.juiced.guhs.feature.vadsrijk.kern.beleving;

import java.util.Arrays;
import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.BaasKist;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;

/**
 * Dev commands {@code /guhs vadsrijk beleving ...} (op only), to look at every effect without a real fight:
 * {@code getallen} (a row of sample damage numbers), {@code reeks <n>}, {@code klap} (hit-stop on the nearest mob), {@code schud},
 * {@code fase} / {@code overwinning} (the moments on the nearest boss), {@code versla} (really defeat the nearest boss: victory +
 * real chests), {@code kist <zeldzaamheid> [pos]} (a falling chest with one piece of that rarity).
 */
final class BelevingCommando {
    private BelevingCommando() {
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> b = Commands.literal("beleving");
        b.then(dev("getallen").executes(BelevingCommando::getallen));
        b.then(dev("reeks").then(Commands.argument("n", IntegerArgumentType.integer(1, 200)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int n = IntegerArgumentType.getInteger(c, "n");
            Reeks.wis(p.getUUID());
            long nu = Beleving.nu(p);
            int stand = 0;
            for (int i = 0; i < n; i++) {
                stand = Reeks.tel(p.getUUID(), nu);
            }
            CompoundTag d = new CompoundTag();
            d.putInt("n", stand);
            d.putBoolean("mijlpaal", Reeks.mijlpaal(stand));
            d.putInt("xp", (int) Math.round((Reeks.xpFactor(stand) - 1) * 100));
            BelevingPayloads.stuur(p, "reeks", d);
            if (Reeks.mijlpaal(stand)) {
                Beleving.confetti((ServerLevel) p.level(), p.getEyePosition().add(p.getLookAngle().scale(3)), stand >= 25 ? 2 : 1);
            }
            return zeg(c, "Vrolijke reeks x" + stand + " (xp x" + Reeks.xpFactor(stand) + ")");
        })));
        b.then(dev("schud").executes(c -> {
            Beleving.schud(c.getSource().getPlayerOrException(), 1f, 20);
            return zeg(c, "Schud!");
        }));
        b.then(dev("klap").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            VadsrijkMob m = dichtste(p, VadsrijkMob.class, 24);
            if (m == null) {
                return zeg(c, "Geen wezen in de buurt.");
            }
            Beleving.klap((ServerLevel) p.level(), m, p, 4, 1f);
            return zeg(c, "Hit-stop op " + m.getName().getString());
        }));
        b.then(dev("fase").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            VadsrijkBaas baas = dichtste(p, VadsrijkBaas.class, 64);
            if (baas == null) {
                return zeg(c, "Geen baas in de buurt.");
            }
            Beleving.faseMoment(baas, Math.max(2, baas.fase() + 1), true);
            return zeg(c, "Fasemoment " + Math.max(2, baas.fase() + 1));
        }));
        b.then(dev("overwinning").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            VadsrijkBaas baas = dichtste(p, VadsrijkBaas.class, 64);
            if (baas == null) {
                return zeg(c, "Geen baas in de buurt.");
            }
            Beleving.overwinning(baas);
            return zeg(c, "Overwinningsmoment (de baas blijft staan)");
        }));
        b.then(dev("versla").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            VadsrijkBaas baas = dichtste(p, VadsrijkBaas.class, 64);
            if (baas == null) {
                return zeg(c, "Geen baas in de buurt.");
            }
            baas.krijgSchade(baas.vads() + 1, p);
            return zeg(c, "Verslagen: " + baas.getName().getString());
        }));
        b.then(dev("kist").then(Commands.argument("zeldzaamheid", StringArgumentType.word())
                .suggests((c, s) -> SharedSuggestionProvider.suggest(Arrays.stream(Zeldzaamheid.values()).map(z -> z.name().toLowerCase()).toList(), s))
                .executes(c -> kist(c, null))
                .then(Commands.argument("pos", BlockPosArgument.blockPos()).executes(c -> kist(c, BlockPosArgument.getLoadedBlockPos(c, "pos"))))));
        event.getDispatcher().register(VadsrijkCommando.wortel(b));
    }

    private static int kist(CommandContext<CommandSourceStack> c, BlockPos pos) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Zeldzaamheid z = Zeldzaamheid.van(StringArgumentType.getString(c, "zeldzaamheid"));
        BlockPos plek = pos != null ? pos : BlockPos.containing(p.position().add(p.getLookAngle().multiply(3, 0, 3)));
        var stuk = Rollen.stack(Rollen.rol(BasisTypes.van("kussenzwaard"), Math.max(1, Spelers.level(p)), z, p.getRandom()));
        BaasKist.plaats((ServerLevel) p.level(), p, plek, new LootTabel.Resultaat(List.of(stuk), 0));
        return zeg(c, "Een vallende kist (" + z.name().toLowerCase() + ") op " + plek.toShortString());
    }

    /** A row of sample numbers 4 blocks in front of you (they live ~0.9 s). */
    private static int getallen(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerLevel sl = (ServerLevel) p.level();
        Vec3 kijk = p.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 rechts = new Vec3(-kijk.z, 0, kijk.x);
        Vec3 m = p.getEyePosition().add(kijk.scale(5)).add(0, -0.6, 0);
        Getalstijl.Stijl[] st = {
                Getalstijl.van(4, false, Element.GEEN, 1, false, false),
                Getalstijl.van(23, false, Element.VUUR, 1.5, false, false),
                Getalstijl.van(9, false, Element.WATER, 0.7, false, false),
                Getalstijl.van(96, true, Element.ROZE, 1, false, false),
                Getalstijl.van(310, true, Element.NATUUR, 1.5, false, false),
                Getalstijl.van(18, false, Element.VUUR, 1, true, false),
                Getalstijl.van(14, false, Element.GEEN, 1, false, true)};
        for (int i = 0; i < st.length; i++) {
            Vec3 at = m.add(rechts.scale((i - (st.length - 1) / 2.0) * 1.3)).add(0, (i % 2) * 0.5, 0);
            var t = Getalstijl.tekst(st[i]);
            if (i == 5) {
                t.append(Component.literal(" ♨"));
            }
            Getallen.tekst(sl, at.x, at.y, at.z, t, st[i].schaal());
        }
        return 1;
    }

    private static <T extends net.minecraft.world.entity.Entity> T dichtste(ServerPlayer p, Class<T> soort, double r) {
        List<T> l = p.level().getEntitiesOfClass(soort, new AABB(p.position(), p.position()).inflate(r), e -> e.isAlive());
        T best = null;
        for (T e : l) {
            if (best == null || e.distanceToSqr(p) < best.distanceToSqr(p)) {
                best = e;
            }
        }
        return best;
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }
}
