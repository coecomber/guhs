package nl.juiced.guhs.feature.vadsrijk.npc;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * /guhs vadsrijk npc ... (dev, op 2): {@code log} (your quests in chat), {@code quest start|klaar|vergeet <id>},
 * {@code reset} (forget all quests, secrets and story moments), {@code opening}, {@code bam}, {@code vlieg stad|kamp},
 * {@code cliffhanger}, {@code tp <npc|poi|knuffel|aanwijzing|schat id>}, {@code npcs} (put every NPC back).
 */
public final class NpcCommando {
    private NpcCommando() {
    }

    static void register(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> npc = Commands.literal("npc").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        npc.then(Commands.literal("log").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (Quest q : Quests.alle().values()) {
                int s = Questlog.status(p, q.id());
                if (s != 0) {
                    StringBuilder b = new StringBuilder();
                    for (int i = 0; i < q.doelen().size(); i++) {
                        b.append(" ").append(q.doelen().get(i).type().name().toLowerCase()).append("=").append(Questlog.waarde(p, q, i)).append("/")
                                .append(q.doelen().get(i).n());
                    }
                    c.getSource().sendSuccess(() -> Component.literal((s == Questlog.KLAAR ? "[af] " : "[..] ") + q.id() + b), false);
                }
            }
            c.getSource().sendSuccess(() -> Component.literal("zone1_klaar=" + NpcApi.zone1Klaar(p) + " quests=" + Quests.alle().size()), false);
            return 1;
        }));
        // (fix round 2, g-werver) Werver Wimpel right here (any dimension), or forget that you talked to him
        npc.then(Commands.literal("werver").executes(c -> Werver.zetHier(c.getSource().getPlayerOrException()) == null ? 0 : 1)
                .then(Commands.literal("vergeet").executes(c -> {
                    Werver.gesproken(c.getSource().getPlayerOrException(), false);
                    return 1;
                })));
        npc.then(Commands.literal("quest")
                .then(Commands.literal("start").then(questArg().executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Quest q = Quests.van(StringArgumentType.getString(c, "id"));
                    if (q == null) {
                        return 0;
                    }
                    Questlog.vergeet(p, q.id());
                    Questlog.neemAan(p, q);
                    return 1;
                })))
                .then(Commands.literal("klaar").then(questArg().executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Quest q = Quests.van(StringArgumentType.getString(c, "id"));
                    if (q == null) {
                        return 0;
                    }
                    if (!Questlog.actief(p, q.id())) {
                        Questlog.neemAan(p, q);
                    }
                    int[] v = new int[q.doelen().size()];
                    for (int i = 0; i < v.length; i++) {
                        v[i] = q.doelen().get(i).n();
                    }
                    Questlog.zetV(p, q, v);
                    c.getSource().sendSuccess(() -> Component.literal("Alle doelen van " + q.id() + " klaar: lever in bij " + q.inleveren()), false);
                    NpcMarkers.vies(p);
                    return 1;
                })))
                .then(Commands.literal("af").then(questArg().executes(c -> {
                    // dev: hand in at once (rewards, no dialogue); "start:" actions run, story actions (bam, cliffhanger) do not
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Quest q = Quests.van(StringArgumentType.getString(c, "id"));
                    if (q == null) {
                        return 0;
                    }
                    if (!Questlog.klaar(p, q.id())) {
                        Questlog.voltooi(p, q);
                        if (q.actieKlaar().startsWith("start:")) {
                            QuestMotor.doeActie(p, q.actieKlaar(), null);
                        }
                    }
                    NpcVolg.meldVolgende(p, q);
                    return 1;
                })))
                .then(Commands.literal("vergeet").then(questArg().executes(c -> {
                    Questlog.vergeet(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "id"));
                    return 1;
                }))));
        npc.then(Commands.literal("reset").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Questlog.wisAlles(p);
            Spelers.data(p).gezien.removeIf(s -> s.startsWith("npc:"));
            Verhaal.vergeet(p);
            QuestMotor.vergeet(p);
            c.getSource().sendSuccess(() -> Component.literal("Questlog, geheimen en verhaalmomenten vergeten."), false);
            return 1;
        }));
        npc.then(Commands.literal("opening").executes(c -> {
            Verhaal.opening(c.getSource().getPlayerOrException());
            return 1;
        }));
        npc.then(Commands.literal("bam").executes(c -> {
            Verhaal.bam(c.getSource().getPlayerOrException());
            return 1;
        }));
        npc.then(Commands.literal("cliffhanger").executes(c -> {
            Verhaal.cliffhanger(c.getSource().getPlayerOrException());
            return 1;
        }));
        npc.then(Commands.literal("vlieg").then(Commands.literal("stad").executes(c -> {
            Verhaal.vlieg(c.getSource().getPlayerOrException(), "zwamzoet");
            return 1;
        })).then(Commands.literal("kamp").executes(c -> {
            Verhaal.vlieg(c.getSource().getPlayerOrException(), "knuffelkamp");
            return 1;
        })));
        npc.then(Commands.literal("questlog").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            NpcPayloads.stuur(p, new NpcPayloads.Log(NpcLog.bouw(p), true));
            return 1;
        }));
        npc.then(Commands.literal("rij").executes(c -> {
            // dev/AutoCheck: every NPC in rows of 8 north of you, looking at you (not tracked: they vanish with the chunk)
            ServerPlayer p = c.getSource().getPlayerOrException();
            int i = 0;
            for (NpcDef d : Npcs.alle()) {
                if (d.id().startsWith("kampwacht_") && !d.id().equals("kampwacht_weide_1")) {
                    continue;
                }
                RijkGuh g = NpcRegistry.GUH.get().create(p.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (g == null) {
                    continue;
                }
                double x = p.getX() + (i % 8 - 3.5) * 2, z = p.getZ() - 4 - (i / 8) * 2.5;
                g.snapTo(x, p.getY(), z, 0, 0);
                g.setYHeadRot(0);
                g.setYBodyRot(0);
                g.stelIn(d);
                g.thuis = BlockPos.containing(x, p.getY(), z);
                p.level().addFreshEntity(g);
                i++;
            }
            int n = i;
            c.getSource().sendSuccess(() -> Component.literal(n + " NPC's op een rij."), false);
            return n;
        }));
        // (q-dialoog, dev/AutoCheck) the portrait cases in one scene: you, a zone-1 and a zone-3 NPC that are not here (stand-ins
        // from their definitions), and the narrator
        npc.then(Commands.literal("portrettest").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            nl.juiced.guhs.feature.verhaal.Praat.scene(p, QuestMotor.SLEUTEL + "_portrettest", java.util.List.of(
                    new nl.juiced.guhs.feature.verhaal.Praat.Regel(p, "vadsrijk.npc.jij", "vadsrijk.npc.praat.oma_truus.1"),
                    new nl.juiced.guhs.feature.verhaal.Praat.Regel(null, "vadsrijk.npc.naam.oma_truus", "vadsrijk.npc.praat.oma_truus.2"),
                    new nl.juiced.guhs.feature.verhaal.Praat.Regel(null, "vadsrijk.npc.naam.z3_imker_1", "vadsrijk.npc.praat.z3_imker_1.1"),
                    new nl.juiced.guhs.feature.verhaal.Praat.Regel(null, "vadsrijk.npc.verteller", "vadsrijk.npc.praat.oma_truus.3")));
            return 1;
        }));
        // (fix round 2, dev/AutoCheck) talk to an NPC as if you right-clicked its live guh (it must be loaded near you)
        npc.then(Commands.literal("praat").then(Commands.argument("id", StringArgumentType.word()).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
            RijkGuh g = sl == null ? null : Npcs.levend(sl, StringArgumentType.getString(c, "id"));
            if (g == null) {
                c.getSource().sendFailure(Component.literal("Die NPC is hier niet (geladen)."));
                return 0;
            }
            QuestMotor.klik(p, g);
            return 1;
        })));
        npc.then(Commands.literal("npcs").executes(c -> {
            ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
            int n = sl == null ? 0 : Npcs.wisAlles(sl);
            c.getSource().sendSuccess(() -> Component.literal(n + " NPC's weg; ze komen zo terug."), false);
            return n;
        }));
        npc.then(Commands.literal("tp").then(Commands.argument("plek", StringArgumentType.word()).suggests((c, b) -> {
            java.util.List<String> l = new java.util.ArrayList<>();
            l.addAll(NpcPlekken.NPC.keySet());
            l.addAll(NpcPlekken.KAMPWACHT.keySet());
            l.addAll(nl.juiced.guhs.feature.vadsrijk.zone2.npc.Z2NpcPlekken.NPC.keySet());   // (K-INT)
            l.addAll(nl.juiced.guhs.feature.vadsrijk.zone3.npc.Z3NpcPlekken.NPC.keySet());
            NpcPlekken.POI.keySet().forEach(k -> l.add("poi." + k));
            NpcPlekken.KNUFFEL.keySet().forEach(k -> l.add("knuffel." + k));
            NpcPlekken.AANWIJZING.keySet().forEach(k -> l.add("aanwijzing." + k));
            NpcPlekken.SCHAT.keySet().forEach(k -> l.add("schat." + k));
            return SharedSuggestionProvider.suggest(l, b);
        }).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            String s = StringArgumentType.getString(c, "plek");
            BlockPos b = plek(s);
            ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
            if (b == null || sl == null) {
                c.getSource().sendFailure(Component.literal("Onbekende plek: " + s));
                return 0;
            }
            p.teleport(new TeleportTransition(sl, new Vec3(b.getX() + 2.5, Npcs.vloer(sl, b.offset(2, 0, 0)), b.getZ() + 0.5), Vec3.ZERO, p.getYRot(), p.getXRot(),
                    TeleportTransition.DO_NOTHING));
            return 1;
        })));
        event.getDispatcher().register(VadsrijkCommando.wortel(npc));
    }

    static BlockPos plek(String s) {
        if (s.startsWith("poi.")) {
            return NpcPlekken.POI.get(s.substring(4));
        }
        if (s.startsWith("knuffel.")) {
            return NpcPlekken.KNUFFEL.get(s.substring(8));
        }
        if (s.startsWith("aanwijzing.")) {
            return NpcPlekken.AANWIJZING.get(s.substring(11));
        }
        if (s.startsWith("schat.")) {
            return NpcPlekken.SCHAT.get(s.substring(6));
        }
        if (NpcPlekken.NPC.containsKey(s)) {
            return NpcPlekken.NPC.get(s);
        }
        // (K-INT) the NPCs of zone 2 and zone 3 too
        BlockPos z = nl.juiced.guhs.feature.vadsrijk.zone2.npc.Z2NpcPlekken.NPC.get(s);
        if (z == null) {
            z = nl.juiced.guhs.feature.vadsrijk.zone3.npc.Z3NpcPlekken.NPC.get(s);
        }
        return z != null ? z : NpcPlekken.KAMPWACHT.get(s);
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> questArg() {
        return Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(Quests.alle().keySet(), b));
    }
}
