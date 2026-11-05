package nl.juiced.guhs.feature.vadsrijk.karakter;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.vadsrijk.kern.Flauwvallen;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * T-KARAKTER (Guhs 1.10.0): character slots + game modes. Hooks into kern (xp factor, damage modifier, fainting, entering),
 * the NPC extra choices (the Grootmeester in Zwamzoet's ordetempel, the Rekruteerder in the Knuffelkamp, every kampwacht at a
 * campfire), the name events, and {@code /guhs vadsrijk karakter}. See {@link Karakter} for the data API.
 */
public final class KarakterRegistry {
    private KarakterRegistry() {
    }

    public static void register(IEventBus modBus) {
        // game modes
        Spelers.XP_FACTOR.add(p -> Karakter.modus(p).xpFactor);
        Schade.VOOR_KLAP.add((k, n) -> k.doel() instanceof ServerPlayer p && Vadsrijk.in(p) ? n * Karakter.modus(p).schadeFactor : n);
        Haken.FLAUW.add(KarakterRegistry::flauw);
        // entering / leaving
        Haken.BINNEN.add((p, binnen) -> {
            if (Karakters.bezig(p)) {
                return;   // (our own switch: not a real entry)
            }
            Karakter.lijst(p);   // (migration of an old save: slot 0)
            Badges.ververs(p);
            if (binnen) {
                Karakters.binnengekomen(p);
                if (aantal(p) > 1) {
                    KarakterScherm.open(p, true);
                } else {
                    nl.juiced.guhs.feature.vadsrijk.kern.Tips.toon(p, "vadsrijk.karakter.tip.slots");
                }
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                Karakter.lijst(p);
                Badges.stuur(p);
                Badges.ververs(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            Karakters.vergeet(e.getEntity().getUUID());
            Badges.vergeet(e.getEntity().level().getServer(), e.getEntity().getUUID());
        });
        NeoForge.EVENT_BUS.addListener(Badges::naam);
        NeoForge.EVENT_BUS.addListener(Badges::tabNaam);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            if (e.getServer().getTickCount() % 100 == 37) {
                RustendeRidders.tick(e.getServer());
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> npcOpties());
        NeoForge.EVENT_BUS.addListener(KarakterRegistry::commando);
    }

    public static void payloads(PayloadRegistrar registrar) {
        KarakterPayloads.register(registrar);
    }

    static int aantal(ServerPlayer p) {
        int n = 0;
        for (int i = 0; i < Karakter.SLOTS; i++) {
            if (Karakter.bestaat(p, i)) {
                n++;
            }
        }
        return n;
    }

    /** Eénvads: fainting outside a town = retiring (never in creative/spectator). */
    static void flauw(ServerPlayer p) {
        if (p.isCreative() || p.isSpectator() || Karakter.modus(p) != Modus.EENVADS) {
            return;
        }
        Gebieden.Gebied g = Gebieden.op(p.getX(), p.getY(), p.getZ());
        if (g != null && g.stad()) {
            return;
        }
        Karakters.pensioen(p, "flauw");
    }

    /** "Ander karakter" in the dialogue of the Grootmeester, the Rekruteerder and every kampwacht (the place rule still applies). */
    private static boolean optiesGezet;

    static void npcOpties() {
        if (optiesGezet) {
            return;
        }
        optiesGezet = true;
        for (var d : nl.juiced.guhs.feature.vadsrijk.npc.Npcs.alle()) {
            if (d.id().equals("grootmeester") || d.id().equals("rekruteerder") || "kampwacht".equals(d.rol())) {
                nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor.EXTRA.computeIfAbsent(d.id(), k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                        .add(new nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor.ExtraOptie("vadsrijk.karakter.optie", p -> true,
                                p -> KarakterScherm.open(p, false)));
            }
        }
    }

    // ---- /guhs vadsrijk karakter ---------------------------------------------------------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    private static void zeg(CommandSourceStack s, String tekst) {
        s.sendSuccess(() -> Component.literal(tekst), false);
    }

    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> k = Commands.literal("karakter").executes(c -> {
            KarakterScherm.open(c.getSource().getPlayerOrException(), false);
            return 1;
        });
        k.then(dev("lijst").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            CompoundTag l = Karakter.lijst(p);
            zeg(c.getSource(), "Actief slot " + Karakter.slot(p) + " (sleutel " + Karakter.sleutel(p).tekst() + ")");
            for (int i = 0; i < Karakter.SLOTS; i++) {
                if (!Karakter.bestaat(p, i)) {
                    zeg(c.getSource(), " " + i + ": (leeg)");
                    continue;
                }
                CompoundTag s = Karakter.info(p, i);
                boolean act = i == Karakter.slot(p);
                SpelerData d = Spelers.data(p);
                zeg(c.getSource(), " " + i + ": " + s.getStringOr("naam", "?") + " [" + s.getStringOr("modus", "?") + (s.getBooleanOr("pensioen", false)
                        ? ", met pensioen" : "") + "] level " + (act ? d.level() : s.getIntOr("glevel", 1)) + ", munten "
                        + Spelers.munten(act ? d.munten : s.getLongOr("munten", 0)) + (act ? "  <- actief" : "")
                        + (s.getBooleanOr("nieuw", false) ? " (nog nooit gespeeld)" : ""));
            }
            zeg(c.getSource(), "versie " + l.getIntOr("versie", 0));
            return 1;
        }));
        k.then(dev("maak").then(Commands.argument("slot", IntegerArgumentType.integer(0, Karakter.SLOTS - 1))
                .then(Commands.argument("modus", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(
                        java.util.Arrays.stream(Modus.values()).map(m -> m.id), b))
                        .then(Commands.argument("naam", StringArgumentType.greedyString()).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            return Karakters.maak(p, IntegerArgumentType.getInteger(c, "slot"), StringArgumentType.getString(c, "naam"),
                                    Modus.van(StringArgumentType.getString(c, "modus"))) ? 1 : 0;
                        })))));
        k.then(dev("wissel").then(Commands.argument("slot", IntegerArgumentType.integer(0, Karakter.SLOTS - 1)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            return Karakters.wissel(p, IntegerArgumentType.getInteger(c, "slot"), true) ? 1 : 0;
        })));
        k.then(dev("naam").then(Commands.argument("slot", IntegerArgumentType.integer(0, Karakter.SLOTS - 1))
                .then(Commands.argument("naam", StringArgumentType.greedyString()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    return Karakters.hernoem(p, IntegerArgumentType.getInteger(c, "slot"), StringArgumentType.getString(c, "naam")) ? 1 : 0;
                }))));
        k.then(dev("modus").then(Commands.argument("slot", IntegerArgumentType.integer(0, Karakter.SLOTS - 1))
                .then(Commands.argument("modus", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(
                        java.util.Arrays.stream(Modus.values()).map(m -> m.id), b)).executes(c -> {
                            ServerPlayer p = c.getSource().getPlayerOrException();
                            return Karakters.zetModus(p, IntegerArgumentType.getInteger(c, "slot"), Modus.van(StringArgumentType.getString(c, "modus")),
                                    true) ? 1 : 0;
                        }))));
        k.then(dev("sterf-test").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            if (Karakter.modus(p) != Modus.EENVADS) {
                c.getSource().sendFailure(Component.literal("Dit karakter is geen Eénvads (modus " + Karakter.modus(p).id + ")"));
                return 0;
            }
            Karakters.pensioen(p, "sterf-test");
            return 1;
        }).then(Commands.literal("echt").executes(c -> {
            Flauwvallen.flauw(c.getSource().getPlayerOrException());   // (the real path: retires only outside a town)
            return 1;
        })));
        k.then(dev("migratie-test").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (String r : KarakterTest.migratie(p)) {
                zeg(c.getSource(), r);
            }
            return 1;
        }));
        k.then(dev("scherm").executes(c -> {
            KarakterScherm.open(c.getSource().getPlayerOrException(), true);
            return 1;
        }));
        k.then(dev("zaal")
                .then(Commands.literal("bouw").executes(c -> {
                    ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
                    if (sl == null) {
                        return 0;
                    }
                    RustendeRidders.bouwAnker(sl);
                    zeg(c.getSource(), "Rustende Ridders-zaal gebouwd op " + RustendeRidders.ANKER.toShortString());
                    return 1;
                }).then(Commands.literal("hier").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (!(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(p)) {
                        c.getSource().sendFailure(Component.literal("alleen in het Vadsrijk"));
                        return 0;
                    }
                    RustendeRidders.bouwHier(sl, p.blockPosition());
                    zeg(c.getSource(), "Rustende Ridders-zaal hier gebouwd (" + p.blockPosition().below().toShortString() + ")");
                    return 1;
                })))
                .then(Commands.literal("tp").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
                    if (sl == null) {
                        return 0;
                    }
                    var a = RustendeRidders.get(c.getSource().getServer()).plek();
                    p.teleport(new TeleportTransition(sl, new Vec3(a.getX() + 0.5, a.getY() + 1, a.getZ() + RustendeRidders.HALF_Z + 3.5), Vec3.ZERO, 180f, 0f,
                            java.util.Set.<Relative>of(), TeleportTransition.DO_NOTHING));
                    return 1;
                }))
                .then(Commands.literal("test").executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    int n = RustendeRidders.get(c.getSource().getServer()).ridders().size() + 1;
                    RustendeRidders.voegToe(c.getSource().getServer(), new RustendeRidders.Ridder(p.getUUID(), 9, "Testridder " + n,
                            p.getGameProfile().name(), 10 + n, Spelers.data(p).klasse, 10 + n, "zwamzoet", "test", System.currentTimeMillis()));
                    zeg(c.getSource(), "Testridder " + n + " toegevoegd aan de zaal");
                    return 1;
                }))
                .then(Commands.literal("leeg").executes(c -> {
                    RustendeRidders d = RustendeRidders.get(c.getSource().getServer());
                    d.ridders.clear();
                    d.vies = true;
                    d.setDirty();
                    ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
                    if (sl != null) {
                        d.beelden(sl);
                    }
                    zeg(c.getSource(), "zaal leeggemaakt");
                    return 1;
                }))
                .then(Commands.literal("lijst").executes(c -> {
                    for (var r : RustendeRidders.get(c.getSource().getServer()).ridders()) {
                        zeg(c.getSource(), r.naam() + " (" + r.spelerNaam() + ") level " + r.level() + " " + r.klasse() + " in " + r.gebied() + " - " + r.waarom());
                    }
                    return 1;
                })));
        event.getDispatcher().register(VadsrijkCommando.wortel(k));
    }
}
