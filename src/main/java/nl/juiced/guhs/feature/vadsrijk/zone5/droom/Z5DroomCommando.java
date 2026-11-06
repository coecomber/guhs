package nl.juiced.guhs.feature.vadsrijk.zone5.droom;

import java.util.Set;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.zone5.verhaal.Z5Finale;

/**
 * Dev command {@code /guhs vadsrijk droom ...} (op level 2):
 * <pre>
 *   start                 into the dream + the fight (as the replay: no story needed)
 *   verhaal               Z5Finale.start for you (only does something during z5_hq08, after the spy)
 *   tp | wakker           onto the cloud (no fight) | wake up at the middle blowhole
 *   bouw                  (re)build the cloud floor (only into air)
 *   fase &lt;2|3&gt;            the boss to 66 % / 31 % (just above the threshold: the next hit starts it)
 *   wiegelied | ranken | omkeer | neurie   trigger a mechanic now (neurie = the hum meter full)
 *   win                   hum meter full + cheered up (the plof, chests, story, wake-up timer)
 *   reset                 the boss away (the floor turns white again)
 *   loot [level]          a chest roll straight into your bags
 *   info                  where the cloud is, boss state
 * </pre>
 */
public final class Z5DroomCommando {
    private Z5DroomCommando() {
    }

    private static int zeg(CommandContext<CommandSourceStack> c, String s) {
        c.getSource().sendSuccess(() -> Component.literal(s), false);
        return 1;
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(VadsrijkCommando.wortel(Commands.literal("droom").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("start").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    return Z5Droom.start(p, true) ? zeg(c, "De droom begint. (Alleen in Het Vadsrijk.)") : zeg(c, "Kan niet: ben je in Het Vadsrijk?");
                }))
                .then(Commands.literal("verhaal").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Z5Finale.Fase f = Z5Finale.volgende(p);
                    Z5Finale.start(p);
                    return zeg(c, "Z5Finale.volgende = " + f + (f == null ? " (niet bij z5_hq08 na het spuitgat: niets te doen)" : ""));
                }))
                .then(Commands.literal("tp").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (!(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(sl)) {
                        return zeg(c, "Ga eerst naar Het Vadsrijk.");
                    }
                    Z5Droom.bouw(sl, Z5Droom.PLEK, false);
                    p.teleportTo(sl, Z5Droom.AANKOMST.getX() + 0.5, Z5Droom.AANKOMST.getY() + 0.2, Z5Droom.AANKOMST.getZ() + 0.5, Set.of(), 180, 0, true);
                    return zeg(c, "Op de Droomwolk (" + Z5Droom.PLEK.toShortString() + ").");
                }))
                .then(Commands.literal("wakker").executes(c -> {
                    Z5Droom.wakker(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("bouw").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Z5Droom.vergeetBouw();
                    return zeg(c, Z5Droom.bouw(p.level(), Z5Droom.PLEK, true) + " wolkblokken geplaatst (alleen in lucht).");
                }))
                .then(Commands.literal("fase").then(Commands.argument("fase", IntegerArgumentType.integer(2, 3)).executes(c -> {
                    GrauweDroom b = baas(c);
                    int f = IntegerArgumentType.getInteger(c, "fase");
                    b.heal(b.maxVads() * (f == 2 ? 0.66 : 0.31) - b.vads());
                    return zeg(c, "Grauwe Droom op " + (f == 2 ? 66 : 31) + "%: de volgende klap start fase " + f + ".");
                })))
                .then(Commands.literal("wiegelied").executes(c -> {
                    baas(c).wiegelied((ServerLevel) baas(c).level());
                    return zeg(c, "Het achterstevoren slaapliedje: kijk welke noten oplichten en klik ze daarna in die volgorde.");
                }))
                .then(Commands.literal("ranken").executes(c -> {
                    baas(c).ranken((ServerLevel) baas(c).level(), 3);
                    return zeg(c, "Drie grauwe ranken: sla ze stuk.");
                }))
                .then(Commands.literal("omkeer").executes(c -> {
                    baas(c).omkeer((ServerLevel) baas(c).level());
                    return zeg(c, "De droom draait om: sta op de roze wolkjes.");
                }))
                .then(Commands.literal("neurie").executes(c -> {
                    baas(c).neurieKlaar((ServerLevel) baas(c).level());
                    return zeg(c, "Neurie-meter vol: de droom wordt lila (+25% schade).");
                }))
                .then(Commands.literal("win").executes(c -> {
                    GrauweDroom b = baas(c);
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    b.neurieKlaar((ServerLevel) b.level());
                    b.krijgSchade(b.maxVads() * 10, p);
                    return zeg(c, b.geplofd() ? "Opgevrolijkt!" : "Lukte niet (sta je in de droom, niet in een scene?)");
                }))
                .then(Commands.literal("reset").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    GrauweDroom b = Z5Droom.baas(p.level());
                    if (b != null) {
                        b.discard();
                    }
                    return zeg(c, b == null ? "Geen Grauwe Droom." : "Grauwe Droom weg.");
                }))
                .then(Commands.literal("loot").executes(c -> loot(c, 60))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 70)).executes(c -> loot(c, IntegerArgumentType.getInteger(c, "level")))))
                .then(Commands.literal("info").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    GrauweDroom b = Z5Droom.baas(p.level());
                    String s = "Droomwolk " + Z5Droom.PLEK.toShortString() + " (r " + Z5Droom.STRAAL + "), wakker worden bij het spuitgat. Al opgevrolijkt: "
                            + Z5Droom.alOpgevrolijkt(p) + ". Verhaal-fase: " + Z5Finale.volgende(p) + ".";
                    if (b != null) {
                        s += "\nGrauwe Droom: " + (int) b.vads() + "/" + (int) b.maxVads() + ", fase " + b.fase() + ", neurie " + b.neurie() + "/" + GrauweDroom.NEURIE_NODIG
                                + (b.neurieKlaar() ? " (klaar)" : "") + ", ranken " + b.rankenLevend() + ", nacht " + b.nacht() + ", getroost " + b.getroost()
                                + ", slaapliedjes goed/mis " + b.wiegeliedGoed + "/" + b.wiegeliedMis;
                    }
                    return zeg(c, s);
                }))));
    }

    private static GrauweDroom baas(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        GrauweDroom b = Z5Droom.baas(p.level());
        if (b == null) {
            throw new com.mojang.brigadier.exceptions.SimpleCommandExceptionType(Component.literal("Geen Grauwe Droom: eerst /guhs vadsrijk droom start")).create();
        }
        return b;
    }

    private static int loot(CommandContext<CommandSourceStack> c, int level) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        LootTabel.Resultaat r = DroomLoot.kist(p, level, p.getRandom());
        for (ItemStack s : r.items()) {
            if (!p.getInventory().add(s)) {
                p.drop(s, false);
            }
        }
        return zeg(c, r.items().size() + " items (+" + r.munten() + " munten niet uitbetaald).");
    }
}
