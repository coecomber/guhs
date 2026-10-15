package nl.juiced.guhs.feature.techklus;

import java.util.List;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.bank.HapluikjeBlockEntity;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.huisje.KlusStand;
import nl.juiced.guhs.feature.klusjes.KlusGebied;
import nl.juiced.guhs.feature.klusjes.Voorraad;
import nl.juiced.guhs.feature.techmachine.PlantagebakBlockEntity;

/**
 * Op commands of the chore slice (for the AutoCheck script and for looking why a resident does nothing; plain texts, these
 * are tools and not part of the game):
 * <ul>
 *   <li>{@code /guhs techklus stand}: the nearest Guhhuisje within 24 blocks: what its machines were shown and whether
 *       something lies ready in them, its Plantagebakken, its Hapluikjes, and what the two chores answer the overview;</li>
 *   <li>{@code /guhs techklus vergeet <x y z>}: the machine there forgets what it was shown.</li>
 * </ul>
 */
final class TechklusCommando {
    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("guhs").then(Commands.literal("techklus")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("stand").executes(c -> stand(c.getSource())))
                .then(Commands.literal("vergeet").then(Commands.argument("plek", BlockPosArgument.blockPos())
                        .executes(c -> vergeet(c.getSource(), BlockPosArgument.getLoadedBlockPos(c, "plek")))))));
    }

    private static int stand(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        BlockPos hier = BlockPos.containing(source.getPosition());
        Huisje huisje = null;
        for (Huisje h : Huisjes.rond(level, hier, 24)) {
            if (huisje == null || h.pos().distSqr(hier) < huisje.pos().distSqr(hier)) {
                huisje = h;
            }
        }
        if (huisje == null) {
            source.sendFailure(Component.literal("Geen Guhhuisje binnen 24 blokken"));
            return 0;
        }
        Huisje h = huisje;
        KlusGebied.vergeet();
        say(source, "Guhhuisje op " + h.pos().toShortString() + ": spullen gaan naar " + Voorraad.afleverPlek(level, h).toShortString());
        List<BlockPos> machines = Klusmachines.rond(level, h);
        for (BlockPos p : machines) {
            StringBuilder wil = new StringBuilder();
            for (Klusmachines.Wens w : Klusmachines.wensen(level, p)) {
                wil.append(wil.isEmpty() ? "" : ", ").append(w.voorbeeld().getItem());
            }
            say(source, "  machine " + level.getBlockState(p).getBlock().getDescriptionId() + " op " + p.toShortString() + ": wil ["
                    + wil + "], bij te vullen " + Klusmachines.teVullen(level, h, p).size() + ", ligt klaar: " + Klusmachines.heeftUitvoer(level, p));
        }
        List<PlantagebakBlockEntity> bakken = PlantageKlus.bakken(level, h);
        for (PlantagebakBlockEntity bak : bakken) {
            say(source, "  plantagebak op " + bak.getBlockPos().toShortString() + ": " + bak.stand() + ", zaailingen " + bak.voorraad().getCount()
                    + ", vadskracht " + bak.heeftKracht());
        }
        List<HapluikjeBlockEntity> luikjes = Voorraad.luikjes(level, h);
        for (HapluikjeBlockEntity luikje : luikjes) {
            say(source, "  hapluikje op " + luikje.getBlockPos().toShortString() + " -> bank " + luikje.adres());
        }
        say(source, "  klusje machines: " + tekst(TechklusFeature.MACHINE_KLUS.stand(level, h)) + ", klusje plantage: "
                + tekst(TechklusFeature.PLANTAGE_KLUS.stand(level, h)));
        return 1 + machines.size() + bakken.size() + luikjes.size();
    }

    private static String tekst(KlusStand stand) {
        return stand.staat() + " " + stand.reden() + " " + stand.aantal();
    }

    private static int vergeet(CommandSourceStack source, BlockPos pos) {
        Klusmachines.vergeet(source.getLevel(), pos);
        say(source, "De machine op " + pos.toShortString() + " weet niet meer wat erin hoort");
        return 1;
    }

    private static void say(CommandSourceStack source, String tekst) {
        source.sendSuccess(() -> Component.literal(tekst), false);
    }
}
