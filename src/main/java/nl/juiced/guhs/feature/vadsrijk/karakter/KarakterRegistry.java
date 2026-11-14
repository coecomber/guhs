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
        // T-INT wiring with the other night slices:
        // t-opslag: the knapzak is per character (its own attachment, keyed by slot; bank + materiaaltas stay account-wide),
        // close every storage screen before a switch
        // U-KARAKTER: the stable per-character key (survives the fixed-slot re-ordering; old knapzakken keep "1".."3")
        nl.juiced.guhs.feature.vadsrijk.opslag.Opslag.KARAKTER = Karakter::opslagSleutel;
        Karakter.VOOR_WISSEL.add(nl.juiced.guhs.feature.vadsrijk.opslag.Opslag::sluit);
        // t-rijdier's patterns/tack wardrobe + t-z4boot's boat (looks, level) are account-wide like the mounts
        Karakter.accountBreed("economie", "garderobe");
        Karakter.accountBreed("boot");
        // (t-kleding's KledingData is SavedData keyed by UUID: account-wide and never swapped)
        // entering / leaving
        Haken.BINNEN.add((p, binnen) -> {
            if (Karakters.bezig(p)) {
                return;   // (our own switch: not a real entry)
            }
            CompoundTag lijst = Karakter.lijst(p);   // (migration of an old save: slot 0; a v1 list: re-ordered by mode)
            Badges.ververs(p);
            if (!binnen) {
                WELKOM.remove(p.getUUID());
                return;
            }
            Karakters.binnengekomen(p);
            if (Karakter.gepensioneerd(p)) {   // (U-KARAKTER: still standing as a retired knight, e.g. logged out right after)
                nl.juiced.guhs.feature.vadsrijk.wereld.Later.na(40, () -> Karakters.naRust(p));
            } else if (!lijst.getBooleanOr("welkom", false)) {
                WELKOM.put(p.getUUID(), p.level().getGameTime());   // the welcome screen, once the intro is over
            } else if (aantal(p) > 1) {
                KarakterScherm.open(p, true);
            }
        });
        // U-KARAKTER: findable without commands: sneak + right-click any campfire = the character screen (before kern's
        // checkpoint click, which then doesn't run), and a one-time hint the first time you light / rest at one
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH, (net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock e) -> {
            if (e.getEntity() instanceof ServerPlayer p && p.isShiftKeyDown() && Vadsrijk.in(p)
                    && e.getLevel().getBlockState(e.getPos()).getBlock() instanceof net.minecraft.world.level.block.CampfireBlock) {
                e.setCanceled(true);
                e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
                KarakterScherm.open(p, false);
            }
        });
        Haken.KAMPVUUR.add((p, pos) -> nl.juiced.guhs.feature.vadsrijk.kern.Tips.toon(p, "vadsrijk.karakter.tip.kampvuur"));
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
        nl.juiced.guhs.world.TikWacht.post("KarakterRegistry:101", e -> {
            if (e.getServer().getTickCount() % 100 == 37) {
                RustendeRidders.tick(e.getServer());
            }
            if (e.getServer().getTickCount() % 10 == 3) {
                welkomTick(e.getServer());
            }
        });
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> npcOpties());
        NeoForge.EVENT_BUS.addListener(KarakterRegistry::commando);
    }

    public static void payloads(PayloadRegistrar registrar) {
        KarakterPayloads.register(registrar);
    }

    /** U-KARAKTER: players waiting for the first-entry welcome screen (uuid -> game time they came in). */
    static final java.util.Map<java.util.UUID, Long> WELKOM = new java.util.concurrent.ConcurrentHashMap<>();
    /** Ticks of quiet (no scene) after the intro before the welcome screen opens. */
    static final int WELKOM_RUST = 40;
    private static final java.util.Map<java.util.UUID, Long> RUSTIG_SINDS = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * U-KARAKTER, the first-creation flow: the first time in the Vadsrijk (per account), after the opening scene, the character
     * screen opens by itself with a welcome text ("choose how you want to play"), plus a chat line on where to find it again.
     * While waiting: no scene running for {@link #WELKOM_RUST} ticks, and the opening was seen (or 15 s passed without one, e.g.
     * entering elsewhere than the camp).
     */
    static void welkomTick(net.minecraft.server.MinecraftServer server) {
        for (var it = WELKOM.entrySet().iterator(); it.hasNext(); ) {
            var en = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(en.getKey());
            if (p == null || !Vadsrijk.in(p)) {
                it.remove();
                RUSTIG_SINDS.remove(en.getKey());
                continue;
            }
            long nu = p.level().getGameTime();
            if (nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes.bezig(p) || Karakters.bezig(p)) {
                RUSTIG_SINDS.remove(p.getUUID());
                continue;
            }
            long rustig = RUSTIG_SINDS.computeIfAbsent(p.getUUID(), k -> nu);
            boolean opening = Spelers.data(p).gezien.contains("npc:opening");
            if (nu - rustig < WELKOM_RUST || (!opening && nu - en.getValue() < 300)) {
                continue;
            }
            it.remove();
            RUSTIG_SINDS.remove(p.getUUID());
            welkom(p);
        }
    }

    /** Opens the welcome screen now (also {@code /guhs vadsrijk karakter welkom}). */
    static void welkom(ServerPlayer p) {
        Karakter.lijst(p).putBoolean("welkom", true);
        Karakters.binnengekomen(p);   // (the arrival window: choosing + playing is allowed right here)
        KarakterScherm.open(p, true, true);
        p.sendSystemMessage(Component.translatable("vadsrijk.karakter.welkom.chat").withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
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

    /**
     * T-INT: places where fainting is "mild" in Eénvads (practice, not a real fight): sparring in Hoogvads' Proefarena. Zone 5's
     * Wolkvangnet catches you BEFORE you faint, so it never counts. Other slices may add area ids or predicates.
     */
    public static final java.util.Set<String> ZACHT = java.util.concurrent.ConcurrentHashMap.newKeySet();
    public static final java.util.List<java.util.function.Predicate<ServerPlayer>> ZACHT_ALS = new java.util.concurrent.CopyOnWriteArrayList<>();

    static {
        ZACHT.add("z5_proefarena");
    }

    /** Eénvads: fainting outside a town = retiring (never in creative/spectator). */
    static void flauw(ServerPlayer p) {
        if (p.isCreative() || p.isSpectator() || Karakter.modus(p) != Modus.EENVADS) {
            return;
        }
        if (nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes.bezig(p)) {
            return;   // (T-INT: a scripted scene never retires you)
        }
        Gebieden.Gebied g = Gebieden.op(p.getX(), p.getY(), p.getZ());
        if (g != null && (g.stad() || ZACHT.contains(g.id()))) {
            return;
        }
        for (java.util.function.Predicate<ServerPlayer> z : ZACHT_ALS) {
            if (z.test(p)) {
                return;
            }
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
                        .add(new nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor.ExtraOptie(d.id().equals("rekruteerder") ? "vadsrijk.karakter.optie.rekruteerder"
                                : "vadsrijk.karakter.optie", p -> true,
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

    private static java.util.concurrent.CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> slotSuggesties(
            com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, com.mojang.brigadier.suggestion.SuggestionsBuilder b) {
        return SharedSuggestionProvider.suggest(java.util.Arrays.stream(Modus.values()).map(m -> m.id), b);
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
        // U-KARAKTER: slots have fixed modes: give the slot as "normaal" / "knuffel" / "eenvads" (or 0 / 1 / 2)
        k.then(dev("maak").then(Commands.argument("slot", StringArgumentType.word()).suggests(KarakterRegistry::slotSuggesties)
                .then(Commands.argument("naam", StringArgumentType.greedyString()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    int slot = Modus.slotVan(StringArgumentType.getString(c, "slot"));
                    return slot >= 0 && Karakters.maak(p, slot, StringArgumentType.getString(c, "naam")) ? 1 : 0;
                }))));
        k.then(dev("wissel").then(Commands.argument("slot", StringArgumentType.word()).suggests(KarakterRegistry::slotSuggesties).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int slot = Modus.slotVan(StringArgumentType.getString(c, "slot"));
            return slot >= 0 && Karakters.wissel(p, slot, true) ? 1 : 0;
        })));
        k.then(dev("naam").then(Commands.argument("slot", StringArgumentType.word()).suggests(KarakterRegistry::slotSuggesties)
                .then(Commands.argument("naam", StringArgumentType.greedyString()).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    int slot = Modus.slotVan(StringArgumentType.getString(c, "slot"));
                    return slot >= 0 && Karakters.hernoem(p, slot, StringArgumentType.getString(c, "naam")) ? 1 : 0;
                }))));
        k.then(dev("archief").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            var archief = Karakter.lijst(p).getListOrEmpty("archief");
            zeg(c.getSource(), "Archief: " + archief.size() + " ridder(s)");
            for (int i = 0; i < archief.size(); i++) {
                CompoundTag a = archief.getCompoundOrEmpty(i);
                zeg(c.getSource(), " " + i + ": " + a.getStringOr("naam", "?") + " [" + a.getStringOr("modus", "?") + "] level " + a.getIntOr("glevel", 1)
                        + " - " + a.getStringOr("archief_reden", "?") + (a.contains("data") ? " (met data)" : " (alleen snapshot)"));
            }
            return 1;
        }));
        k.then(dev("welkom").executes(c -> {
            welkom(c.getSource().getPlayerOrException());
            return 1;
        }).then(Commands.literal("vergeet").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Karakter.lijst(p).remove("welkom");
            zeg(c.getSource(), "Welkomstscherm vergeten: komt weer na de volgende binnenkomst (na het filmpje)");
            return 1;
        })));
        k.then(dev("v1-test").executes(c -> {
            for (String r : KarakterTest.herschik()) {
                zeg(c.getSource(), r);
            }
            return 1;
        }));
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
                    zeg(c.getSource(), "Rustende Ridders-zaal gebouwd op " + RustendeRidders.ANKER.toShortString()
                            + " (alleen nodig in een wereld van voor ronde U: de zaal zit nu in de kaart)");
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
                    // U-KARAKTER: inside the hall, in the aisle by the door, looking at the statues along the back wall
                    p.teleport(new TeleportTransition(sl, new Vec3(a.getX() + 0.5, a.getY() + 1, a.getZ() + RustendeRidders.HALF_Z - 1.5), Vec3.ZERO, 180f, 0f,
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
