package nl.juiced.guhs.feature.vadsrijk.kleding;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * Het Vadsrijk, T-KLEDING: the Kledingkast (transmog, back slot wings/cape, head accessory, dye, name colour, title), wings with
 * gliding + one movement trick each, and the effects (cheer-up, trail, aura). See {@link Kledingkast}, {@link Zweven},
 * {@link Plofjes}, {@link Bronnen}, {@link Cosmetica}; client: {@code kleding.client.KledingClient}. Dev: {@code /guhs vadsrijk kleding}.
 */
public final class KledingRegistry {
    /** Ticks between source checks per player in the Vadsrijk (spread over the players). */
    public static final int CHECK = 200;
    /** The tailors with the extra "Kledingkast" choice (Zwamzoet, Kandijkom, the zone-3 hub); visiting all three is a secret. */
    public static final List<String> KLEERMAKERS = List.of("kleermaker", "z2_breister", "z3_kleermaker");
    /** Wing unlocks that still owe their title card (they came during a scene). */
    private static final Set<UUID> KAART_LATER = ConcurrentHashMap.newKeySet();

    private KledingRegistry() {
    }

    public static void register(IEventBus modBus) {
        Haken.VERSLAGEN.add(Plofjes::verslagen);
        Schade.NA_KLAP.add(Plofjes::klap);
        Haken.LOOT.add((p, s) -> {
            if (Kledingkast.onthoud(KledingData.deel(p, "looks"), s) > 0) {
                KledingData.bewaard(p);
            }
        });
        Haken.BINNEN.add((p, binnen) -> Zweven.stuurStaat(p));
        nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenApi.MOMENT.add((p, moment) -> {
            if (moment.equals("vahoegnir_opgevrolijkt")) {
                vahoegveertjes(p);
            }
        });
        for (String npc : KLEERMAKERS) {
            nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor.EXTRA.computeIfAbsent(npc, k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                    .add(new nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor.ExtraOptie("vadsrijk.kleding.npc.optie", p -> true,
                            p -> Kledingkast.open(p, npc)));
        }
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            Zweven.tick(e.getServer());
            ServerLevel sl = Vadsrijk.level(e.getServer());
            if (sl == null || sl.getGameTime() % 20 != 13) {
                return;
            }
            for (ServerPlayer p : sl.players()) {
                if (KAART_LATER.contains(p.getUUID()) && !RijkScenes.bezig(p)) {
                    KAART_LATER.remove(p.getUUID());
                    vahoegKaart(p);
                }
                if ((sl.getGameTime() / 20 + p.getId()) % (CHECK / 20) == 0 && !RijkScenes.bezig(p)) {
                    Kledingkast.verzamel(p);
                    Bronnen.check(p);
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, KledingRegistry::naam);
        NeoForge.EVENT_BUS.addListener((LivingFallEvent e) -> Zweven.val(e));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                Kledingkast.login(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                Zweven.stuurStaat(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            Zweven.vergeet(e.getEntity().getUUID());
            KAART_LATER.remove(e.getEntity().getUUID());
        });
        NeoForge.EVENT_BUS.addListener(KledingRegistry::commando);
    }

    public static void payloads(PayloadRegistrar registrar) {
        KledingPayloads.register(registrar);
    }

    /** Vahoegnir turned pink (end of zone 3): the first wings + gliding. The title card waits for the end of the scene. */
    static void vahoegveertjes(ServerPlayer p) {
        if (Kledingkast.ontgrendel(p, Cosmetica.VAHOEGVEERTJES.id(), false)) {
            p.sendSystemMessage(Component.translatable("vadsrijk.kleding.vahoeg.chat").withStyle(ChatFormatting.LIGHT_PURPLE));
            if (RijkScenes.bezig(p)) {
                KAART_LATER.add(p.getUUID());
            } else {
                vahoegKaart(p);
            }
        }
    }

    private static void vahoegKaart(ServerPlayer p) {
        RijkScenes.titel(p, Component.translatable(Cosmetica.VAHOEGVEERTJES.naamKey()).withStyle(ChatFormatting.LIGHT_PURPLE),
                Component.translatable("vadsrijk.kleding.vleugel.onder.eerste"), 100, 0xFF8CC8);
        Tips.toon(p, "vadsrijk.kleding.tip.zweven");
    }

    /** The name colour (before the titles feature appends its title: HIGH priority). Server: the store; client: the synced looks. */
    private static void naam(PlayerEvent.NameFormat e) {
        int kleur = -1;
        if (e.getEntity() instanceof ServerPlayer p) {
            kleur = Kledingkast.naamkleur(p);
        } else if (e.getEntity().level().isClientSide()) {
            kleur = KLIENT_NAAMKLEUR.apply(e.getEntity().getUUID());
        }
        if (kleur >= 0) {
            final int k = kleur;
            e.setDisplayname(e.getDisplayname().copy().withStyle(s -> s.withColor(k)));
        }
    }

    /** Set by the client: the synced name colour of a player (-1 none). */
    public static volatile java.util.function.Function<UUID, Integer> KLIENT_NAAMKLEUR = u -> -1;

    // ---- dev command ------------------------------------------------------------------------------------------------------------

    private static void commando(RegisterCommandsEvent event) {
        // the player command: open your wardrobe anywhere
        event.getDispatcher().register(VadsrijkCommando.wortel(Commands.literal("kledingkast").executes(c -> {
            Kledingkast.open(c.getSource().getPlayerOrException(), null);
            return 1;
        })));
        LiteralArgumentBuilder<CommandSourceStack> k = Commands.literal("kleding").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        k.then(Commands.literal("kast").executes(c -> {
            Kledingkast.open(c.getSource().getPlayerOrException(), null);
            return 1;
        }));
        k.then(Commands.literal("alles").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int n = 0;
            for (Cosmetica.Ding d : Cosmetica.alle()) {
                if (Kledingkast.ontgrendel(p, d.id(), false)) {
                    n++;
                }
            }
            int l = Kledingkast.alleLooks(p);
            final int nn = n;
            Kledingkast.stuurUiterlijk(p);
            Zweven.stuurStaat(p);
            c.getSource().sendSuccess(() -> Component.literal("Kledingkast: " + nn + " cosmetica + " + l + " looks erbij (alles ontgrendeld, ook 'later')"), false);
            return 1;
        }));
        k.then(Commands.literal("geef").then(Commands.argument("id", StringArgumentType.greedyString())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Cosmetica.alle().stream().map(Cosmetica.Ding::id), b)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    String id = StringArgumentType.getString(c, "id");
                    boolean ok = id.equals(Cosmetica.VAHOEGVEERTJES.id()) ? geefVahoeg(p) : Kledingkast.ontgrendel(p, id, true);
                    c.getSource().sendSuccess(() -> Component.literal(ok ? "gegeven: " + id : "bestaat niet of had je al: " + id), false);
                    return 1;
                })));
        k.then(Commands.literal("neem").then(Commands.argument("id", StringArgumentType.greedyString())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Cosmetica.alle().stream().map(Cosmetica.Ding::id), b)).executes(c -> {
                    Kledingkast.vergeet(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "id"));
                    c.getSource().sendSuccess(() -> Component.literal("weggehaald"), false);
                    return 1;
                })));
        k.then(Commands.literal("kies").then(Commands.argument("id", StringArgumentType.greedyString())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Cosmetica.alle().stream().map(Cosmetica.Ding::id), b)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Cosmetica.Ding d = Cosmetica.van(StringArgumentType.getString(c, "id"));
                    boolean ok = d != null && Kledingkast.kies(p, d.soort(), d.id());
                    c.getSource().sendSuccess(() -> Component.literal(ok ? "aangedaan" : "kan niet (niet ontgrendeld?)"), false);
                    return 1;
                })));
        k.then(Commands.literal("wis").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            KledingData.wis(p);
            Kledingkast.stuurUiterlijk(p);
            Zweven.stuurStaat(p);
            p.refreshDisplayName();
            c.getSource().sendSuccess(() -> Component.literal("Kledingkast van dit account gewist"), false);
            return 1;
        }));
        k.then(Commands.literal("check").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            int looks = Kledingkast.verzamel(p);
            int n = Bronnen.check(p);
            c.getSource().sendSuccess(() -> Component.literal("check: " + n + " nieuwe cosmetica, " + looks + " nieuwe looks"), false);
            return 1;
        }));
        k.then(Commands.literal("lijst").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            StringBuilder sb = new StringBuilder("Kledingkast (" + Kledingkast.alleHeeft(p).size() + "/" + Cosmetica.alle().size() + "):");
            for (Cosmetica.Ding d : Cosmetica.alle()) {
                sb.append("\n ").append(Kledingkast.heeft(p, d.id()) ? "[x] " : "[ ] ").append(d.id()).append("  (").append(d.bron()).append(")");
            }
            sb.append("\nkills: ").append(Kledingkast.kills(p)).append(", zweven: ").append(Kledingkast.zweven(p));
            c.getSource().sendSuccess(() -> Component.literal(sb.toString()), false);
            return 1;
        }));
        k.then(Commands.literal("zweef").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Cosmetica.Kracht kr = Zweven.kracht(p);
            c.getSource().sendSuccess(() -> Component.literal("zweven ontgrendeld: " + Kledingkast.zweven(p) + ", vleugels: "
                    + (kr == null ? "geen" : Kledingkast.keuze(p, "rug") + " (" + kr + ")") + ", mag hier: " + Zweven.mag(p) + ", zweeft nu: "
                    + Zweven.zweeft(p)), false);
            return 1;
        }));
        k.then(Commands.literal("plof").then(Commands.argument("effect", StringArgumentType.greedyString())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Cosmetica.van(Cosmetica.Soort.PLOF).stream().map(Cosmetica.Ding::id), b)).executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    if (p.level() instanceof ServerLevel sl) {
                        var kijk = p.getLookAngle();
                        Plofjes.toon(sl, p, StringArgumentType.getString(c, "effect"), p.getX() + kijk.x * 3, p.getY() + 1, p.getZ() + kijk.z * 3);
                    }
                    return 1;
                })));
        k.then(Commands.literal("kills").then(Commands.argument("n", IntegerArgumentType.integer(0, 100000)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            KledingData.kast(p).putInt("kills", IntegerArgumentType.getInteger(c, "n"));
            KledingData.bewaard(p);
            c.getSource().sendSuccess(() -> Component.literal("opgevrolijkt-teller gezet (check volgt bij /guhs vadsrijk kleding check)"), false);
            return 1;
        })));
        k.then(Commands.literal("geheim").then(Commands.argument("id", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("zweefvlucht", "kleermakers"), b)).executes(c -> {
                    boolean ok = Kledingkast.geheim(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "id"));
                    c.getSource().sendSuccess(() -> Component.literal(ok ? "geheim gevonden" : "niets nieuws"), false);
                    return 1;
                })));
        k.then(Commands.literal("vahoegnir").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            boolean ok = geefVahoeg(p);
            c.getSource().sendSuccess(() -> Component.literal(ok ? "Vahoegveertjes + zweven gegeven (zoals na Vahoegnir)" : "had je al"), false);
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(k));
    }

    private static boolean geefVahoeg(ServerPlayer p) {
        boolean al = Kledingkast.zweven(p);
        vahoegveertjes(p);
        return !al;
    }
}
