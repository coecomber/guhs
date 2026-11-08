package nl.juiced.guhs.feature.vadsrijk.geluid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;

/**
 * T-GELUID (Guhs 1.10, roadmap §5.2 + §5.3, decision R3 "muziek ja, zacht"): soft area music with long silences, a theme per
 * boss, stingers, and a richer ambience. This class registers the sound events, the packet, the server hooks that send the
 * stingers, and the dev commands. The music itself is made by tools/vadsrijk/geluid (MIDI -> SoundFont -> OGG); the players and
 * the sliders live in {@code geluid.client}.
 * <p>
 * Stingers from the server (to the player it is about): a quest handed in ({@code NpcApi.QUEST_KLAAR}), a level up
 * ({@code Haken.LEVEL_OP}), a boss cheered up ({@code Haken.VERSLAGEN} with a {@link VadsrijkBaas}: everyone who helped) and the
 * first visit of an area ({@code Haken.GEBIED_BETREDEN}, before the Verkenningsboek marks it). The secret stinger comes from the
 * client (the "schat" title cards).
 */
public final class GeluidRegistry {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, Guhs.MODID);
    public static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> ALLE = new LinkedHashMap<>();
    /** Areas that are too big / too general to count as a discovery. */
    private static final List<String> GEEN_ONTDEKKING = List.of("ring", "z2_ring", "z3_ring", "proeftuin");
    /** One level-up stinger per player per tick (a generic + class level-up arrive together). */
    private static final Map<UUID, Long> LAATSTE_LEVEL = new HashMap<>();
    /** The stingers sent per player this session (gametests read it; cleared on logout). */
    static final Map<UUID, List<String>> GESTUURD = new java.util.concurrent.ConcurrentHashMap<>();

    static {
        for (String t : GeluidPlan.GEBIED_THEMAS) {
            geluid(GeluidPlan.gebiedId(t));
        }
        for (String t : GeluidPlan.BAAS_THEMAS) {
            geluid(GeluidPlan.baasId(t));
        }
        for (String t : GeluidPlan.STINGERS) {
            geluid(GeluidPlan.stingerId(t));
        }
        for (String b : GeluidPlan.BEDDEN) {
            geluid(GeluidPlan.bedId(b));
            geluid(GeluidPlan.sfeerId(b));
        }
        // U-AUDIO2: the last-phase loops, the fanfares, the NPC voices and the world point sounds
        for (String t : GeluidPlan.BAAS_THEMAS) {
            geluid(GeluidPlan.faseId(t));
            geluid(GeluidPlan.fanfareId(t));
        }
        for (String st : StemPlan.SOORTEN) {
            geluid(StemPlan.id(st));
        }
        for (String w : WereldBron.SOORTEN) {
            geluid(WereldBron.id(w));
        }
    }

    private GeluidRegistry() {
    }

    private static void geluid(String id) {
        ALLE.put(id, SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(Guhs.id(id))));
    }

    public static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
        NpcApiHaak.koppel();
        Haken.LEVEL_OP.add((p, wat) -> {
            long nu = p.level().getGameTime();
            Long oud = LAATSTE_LEVEL.put(p.getUUID(), nu);
            if (oud == null || nu - oud > 20) {
                stinger(p, "level_op");
            }
        });
        Haken.VERSLAGEN.add(v -> {
            if (v.mob() instanceof VadsrijkBaas b) {
                String thema = themaVan(b);   // U-AUDIO2: the boss's own fanfare (the client falls back to the shared stinger)
                v.helpers().forEach(p -> fanfare(p, thema));
            }
        });
        // in FRONT of the Verkenningsboek's own listener, so "not marked yet" still means "first time here"
        Haken.GEBIED_BETREDEN.add(0, (p, gebied) -> {
            if (!GEEN_ONTDEKKING.contains(gebied) && !nl.juiced.guhs.feature.vadsrijk.voltooiing.Voltooiing.gemarkeerd(p, gebied)) {
                stinger(p, "z6_moederoase".equals(gebied) ? "moederoase" : "ontdekking");   // U-Z6MUZIEK: the oasis' own stinger
            }
        });
        NeoForge.EVENT_BUS.addListener(GeluidRegistry::commandos);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e) -> {
            LAATSTE_LEVEL.remove(e.getEntity().getUUID());
            GESTUURD.remove(e.getEntity().getUUID());
        });
    }

    public static void payloads(PayloadRegistrar registrar) {
        registrar.playToClient(GeluidPayloads.Speel.TYPE, GeluidPayloads.Speel.STREAM_CODEC, (s, c) -> GeluidPayloads.speel.accept(s));
    }

    /**
     * Play a stinger for this player (the client applies its own Stingers slider). U-Z6MUZIEK: zone 6 calls
     * {@code stinger(p, "baken")} when a star beacon is lit; "moederoase" plays by itself on the first visit of z6_moederoase.
     */
    public static void stinger(ServerPlayer p, String soort) {
        List<String> l = GESTUURD.computeIfAbsent(p.getUUID(), u -> new java.util.concurrent.CopyOnWriteArrayList<>());
        if (l.size() < 64) {
            l.add(soort);
        }
        PacketDistributor.sendToPlayer(p, new GeluidPayloads.Speel("stinger", soort));
    }

    // ------------------------------------------------------------------------------------------------ U-AUDIO2

    /** The boss theme of this boss: its own muziek() id when that is one of ours, else by its entity type. */
    public static String themaVan(VadsrijkBaas b) {
        net.minecraft.resources.Identifier m = b.muziek();
        String t = m == null ? null : GeluidPlan.themaVanId(m.toString());
        if (t != null) {
            return t;
        }
        return GeluidPlan.baasThema(BuiltInRegistries.ENTITY_TYPE.getKey(b.getType()).getPath());
    }

    /** A boss beaten: its own victory fanfare for this helper. */
    public static void fanfare(ServerPlayer p, String thema) {
        noteer(p, "fanfare:" + thema);
        PacketDistributor.sendToPlayer(p, new GeluidPayloads.Speel("fanfare", thema));
    }

    /**
     * (hook in VadsrijkBaas, U-AUDIO2) A boss reached phase {@code fase} of {@code max}: everyone within 64 blocks hears the
     * last-phase layer fade in (the client only reacts when that boss's theme is the one playing).
     */
    public static void fase(VadsrijkBaas b, int fase, int max) {
        if (!(b.level() instanceof net.minecraft.server.level.ServerLevel sl)) {
            return;
        }
        String naam = b.getId() + ":" + fase + ":" + max;
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(b) <= 64 * 64) {
                noteer(p, "fase:" + fase + "/" + max);
                PacketDistributor.sendToPlayer(p, new GeluidPayloads.Speel("fase", naam));
            }
        }
    }

    private static void noteer(ServerPlayer p, String wat) {
        List<String> l = GESTUURD.computeIfAbsent(p.getUUID(), u -> new java.util.concurrent.CopyOnWriteArrayList<>());
        if (l.size() < 64) {
            l.add(wat);
        }
    }

    /** The quest hook lives in the npc slice: a tiny holder so this class compiles without touching it. */
    private static final class NpcApiHaak {
        static void koppel() {
            nl.juiced.guhs.feature.vadsrijk.npc.NpcApi.QUEST_KLAAR.add((p, quest) -> stinger(p, "quest_klaar"));
        }
    }

    // ------------------------------------------------------------------------------------------------ dev commands

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    private static void commandos(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> g = dev("geluid").executes(c -> lijst(c.getSource()));
        g.then(Commands.literal("lijst").executes(c -> lijst(c.getSource())));
        g.then(Commands.literal("stop").executes(c -> stuur(c.getSource(), "stop", "")));
        g.then(Commands.literal("nu").executes(c -> stuur(c.getSource(), "nu", "")));
        g.then(Commands.literal("info").executes(c -> stuur(c.getSource(), "info", "")));
        g.then(soort("gebied", GeluidPlan.GEBIED_THEMAS));
        g.then(soort("baas", GeluidPlan.BAAS_THEMAS));
        g.then(soort("stinger", GeluidPlan.STINGERS));
        g.then(soort("bed", GeluidPlan.BEDDEN));
        // U-AUDIO2: a boss theme with its last phase fading in after 12 s, the fanfares, the NPC voices, the world sounds
        g.then(soort("fase", GeluidPlan.BAAS_THEMAS));
        g.then(soort("fanfare", GeluidPlan.BAAS_THEMAS));
        g.then(soort("stem", StemPlan.SOORTEN));
        g.then(soort("wereld", WereldBron.SOORTEN));
        g.then(Commands.literal("wereldinfo").executes(c -> stuur(c.getSource(), "wereldinfo", "")));
        g.then(soort("stad", stadDeuntjes()));   // U-Z6MUZIEK: every town tune (rozenkloof_1, saffraanpoort_2, ...)
        event.getDispatcher().register(VadsrijkCommando.wortel(g));
    }

    /** U-Z6MUZIEK: the town tunes by name ("zwamzoet_1", ..., "saffraanpoort_2"). */
    static List<String> stadDeuntjes() {
        List<String> l = new ArrayList<>();
        for (String stad : nl.juiced.guhs.feature.vadsrijk.stadsmuziek.StadsMuziek.STEDEN) {
            for (int i = 1; i <= nl.juiced.guhs.feature.vadsrijk.stadsmuziek.StadsMuziek.muziekVan(stad).size(); i++) {
                l.add(stad + "_" + i);
            }
        }
        return l;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> soort(String soort, List<String> namen) {
        return Commands.literal(soort).then(Commands.argument("naam", StringArgumentType.word())
                .suggests((c, s) -> SharedSuggestionProvider.suggest(namen, s))
                .executes(c -> {
                    String n = StringArgumentType.getString(c, "naam");
                    if (!namen.contains(n)) {
                        c.getSource().sendFailure(Component.literal("Onbekend: " + n + " (kies uit: " + String.join(", ", namen) + ")"));
                        return 0;
                    }
                    return stuur(c.getSource(), soort, n);
                }));
    }

    private static int stuur(CommandSourceStack src, String soort, String naam) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        PacketDistributor.sendToPlayer(p, new GeluidPayloads.Speel(soort, naam));
        if (!soort.equals("info") && !soort.equals("wereldinfo")) {
            src.sendSuccess(() -> Component.literal("Geluid: " + soort + (naam.isEmpty() ? "" : " " + naam)).withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }
        return 1;
    }

    /** A clickable list of every track (click = play it now). */
    private static int lijst(CommandSourceStack src) {
        List<Component> regels = new ArrayList<>();
        regels.add(Component.literal("Vadsrijk-geluid (klik om te luisteren):").withStyle(ChatFormatting.GOLD));
        regels.add(regel("gebied", GeluidPlan.GEBIED_THEMAS));
        regels.add(regel("baas", GeluidPlan.BAAS_THEMAS));
        regels.add(regel("stinger", GeluidPlan.STINGERS));
        regels.add(regel("bed", GeluidPlan.BEDDEN));
        regels.add(regel("fase", GeluidPlan.BAAS_THEMAS));
        regels.add(regel("fanfare", GeluidPlan.BAAS_THEMAS));
        regels.add(regel("stem", StemPlan.SOORTEN));
        regels.add(regel("wereld", WereldBron.SOORTEN));
        regels.add(regel("stad", stadDeuntjes()));
        regels.add(Component.literal("[stop] ").withStyle(st -> st.withColor(ChatFormatting.RED)
                        .withClickEvent(new ClickEvent.RunCommand("/guhs vadsrijk geluid stop")))
                .append(Component.literal("[nu: gebiedsmuziek zonder stilte] ").withStyle(st -> st.withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent.RunCommand("/guhs vadsrijk geluid nu"))))
                .append(Component.literal("[info]").withStyle(st -> st.withColor(ChatFormatting.YELLOW)
                        .withClickEvent(new ClickEvent.RunCommand("/guhs vadsrijk geluid info")))));
        regels.forEach(r -> src.sendSuccess(() -> r, false));
        return 1;
    }

    private static Component regel(String soort, List<String> namen) {
        MutableComponent r = Component.literal(soort + ": ").withStyle(ChatFormatting.GOLD);
        for (String n : namen) {
            r.append(Component.literal("[" + n + "] ").withStyle(st -> st.withColor(ChatFormatting.AQUA)
                    .withClickEvent(new ClickEvent.RunCommand("/guhs vadsrijk geluid " + soort + " " + n))));
        }
        return r;
    }
}
