package nl.juiced.guhs.feature.vadsrijk.ambacht;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.vadsrijk.ambacht.PlekWereld.Spot;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;
import nl.juiced.guhs.feature.vadsrijk.economie.Waarde;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor;

/**
 * Het Vadsrijk "Ambachten" (night round T, slice t-ambacht; VOORSTEL_PROFESSIONS + decisions A1-A3): 9 professions with their own
 * levels ({@link AmbachtData}, {@link AmbachtXp}), per-player gathering spots in zones 1-3 ({@link Verzamelen}, {@link PlekWereld},
 * {@link AmbachtPlekken}), hives ({@link Imkeren}), food buffs ({@link Eten}), brewing ({@link Brouwsels}), Maatwerk/Meesterwerk
 * ({@link Maatwerk}), the recipes and workshops ({@link Recepten}, {@link Werkplaats}, {@link AmbachtNpcs}), the Bestelbord, the
 * rankings + website export ({@link Ranglijst}) and the Verkenningsboek page + titles ({@link AmbachtBoek}).
 * Commands (op 2): {@code /guhs vadsrijk ambacht ...} (see SPIEK_T_AMBACHT.md).
 */
public final class AmbachtRegistry {
    private AmbachtRegistry() {
    }

    public static void register(IEventBus modBus) {
        // (class init order: every item/component is added to the deferred registers before they are hooked to the bus)
        Materialen.ALLE.size();
        Gemaakt.VADSIGHEID.getId();
        Eten.GERECHTEN.size();
        Brouwsels.DRANKEN.size();
        Maatwerk.SOORTEN.size();
        Materialen.ITEMS.register(modBus);
        Materialen.COMPONENTS.register(modBus);
        Maatwerk.basisTypes();
        AmbachtBoek.registreer();
        Spelers.EXTRA_STATS.add(Eten::stats);
        Spelers.XP_FACTOR.add(Eten::gevechtXp);
        nl.juiced.guhs.feature.vadsrijk.kern.Schade.VOOR_KLAP.add(Maatwerk::voorKlap);
        Haken.VERSLAGEN.add(AmbachtRegistry::kernDrop);
        QuestMotor.KLIK_VOOR.add(AmbachtNpcs::klik);
        Praat.luister(AmbachtNpcs.SLEUTEL, AmbachtNpcs::antwoord);
        Waarde.EXTRA.add(AmbachtRegistry::waarde);
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> Ranglijst.koppel(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            Verzamelen.tick(e.getServer());
            AmbachtNpcs.tick(e.getServer());
            Ranglijst.tick(e.getServer());
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                AmbachtData.ingelogd(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                AmbachtData.uitgelogd(p);
                Ranglijst.bij(p);
                Verzamelen.vergeet(p.getUUID());
                AmbachtSchermen.vergeet(p.getUUID());
                AmbachtNpcs.vergeetSpeler(p.getUUID());
                if (p.level().getServer() != null) {
                    Ranglijst.exporteer(p.level().getServer());
                }
            }
        });
        NeoForge.EVENT_BUS.addListener(AmbachtRegistry::commando);
    }

    public static void payloads(PayloadRegistrar registrar) {
        AmbachtPayloads.register(registrar);
    }

    /** Boss cores (Meesterwerk): every boss 15 % per helper, by its level's zone; elites 3 % a kernsplinter. */
    static void kernDrop(Haken.Verslagen v) {
        boolean baas = v.mob() instanceof VadsrijkBaas;
        if (!baas && !v.mob().elite()) {
            return;
        }
        int lv = v.mob().mobLevel();
        String kern = lv <= 20 ? "zwamkern" : lv <= 35 ? "splinterkern" : "wortelkern";
        for (ServerPlayer p : v.helpers()) {
            ItemStack s = baas ? (p.getRandom().nextDouble() < 0.15 ? new ItemStack(Materialen.KERN.get(kern).get()) : ItemStack.EMPTY)
                    : (p.getRandom().nextDouble() < 0.03 ? new ItemStack(Materialen.KERNSPLINTER.get()) : ItemStack.EMPTY);
            if (!s.isEmpty()) {
                LootBundel.laatVallen(p, v.mob().position().add(0, 0.5, 0), new LootTabel.Resultaat(List.of(s), 0), baas ? 3 : 2);
            }
        }
    }

    /** Shop value of one (economie Waarde.EXTRA): materials by tier and quality, dishes/potions by tier. */
    static long waarde(ItemStack s) {
        Materialen.Materiaal m = Materialen.van(s);
        if (m != null) {
            long[] t = {60, 3, 8, 15, 25};
            return Math.round(t[Math.min(4, m.tier())] * (1 + 0.5 * Materialen.kwaliteit(s)));
        }
        if (s.getItem() instanceof Eten.EtenItem e) {
            return 8L * e.gerecht.tier() * e.gerecht.tier();
        }
        if (s.getItem() instanceof Brouwsels.DrankItem d) {
            return 10L * d.drank.tier() * d.drank.tier();
        }
        return 0;
    }

    // ---- commands --------------------------------------------------------------------------------------------------------------

    private static ServerPlayer speler(CommandContext<CommandSourceStack> c) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return c.getSource().getPlayerOrException();
    }

    private static void zeg(CommandContext<CommandSourceStack> c, String tekst) {
        c.getSource().sendSuccess(() -> Component.literal(tekst), false);
    }

    private static List<String> ambachten() {
        List<String> l = new ArrayList<>();
        for (Ambacht a : Ambacht.values()) {
            l.add(a.id);
        }
        return l;
    }

    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> k = Commands.literal("ambacht").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        k.then(Commands.literal("boek").executes(c -> {
            AmbachtSchermen.boek(speler(c), "overzicht", null);
            return 1;
        }).then(Commands.argument("tab", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(AmbachtSchermen.TABS, b)).executes(c -> {
            AmbachtSchermen.boek(speler(c), StringArgumentType.getString(c, "tab"), null);
            return 1;
        })));
        k.then(Commands.literal("werk").then(Commands.argument("ambacht", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(List.of("smeden", "naaien", "koken", "brouwen"), b)).executes(c -> {
                    Ambacht a = Ambacht.van(StringArgumentType.getString(c, "ambacht"));
                    if (a != null) {
                        AmbachtSchermen.werkplaats(speler(c), a, null);
                    }
                    return 1;
                })));
        k.then(Commands.literal("level").then(Commands.argument("ambacht", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(lijstMetAlle(), b))
                .then(Commands.argument("n", IntegerArgumentType.integer(1, 100)).executes(c -> {
                    ServerPlayer p = speler(c);
                    String w = StringArgumentType.getString(c, "ambacht");
                    int n = IntegerArgumentType.getInteger(c, "n");
                    for (Ambacht a : Ambacht.values()) {
                        if (w.equals("alle") || a.id.equals(w)) {
                            AmbachtData.zetLevel(p, a, n);
                        }
                    }
                    Ranglijst.bij(p);
                    zeg(c, "Ambacht " + w + " -> lv " + n);
                    return 1;
                }))));
        k.then(Commands.literal("xp").then(Commands.argument("ambacht", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(ambachten(), b))
                .then(Commands.argument("n", IntegerArgumentType.integer(1, 10_000_000)).executes(c -> {
                    Ambacht a = Ambacht.van(StringArgumentType.getString(c, "ambacht"));
                    if (a != null) {
                        AmbachtData.geefXp(speler(c), a, IntegerArgumentType.getInteger(c, "n"), false);
                    }
                    return 1;
                }))));
        k.then(Commands.literal("geef").then(Commands.literal("alles").executes(c -> {
            ServerPlayer p = speler(c);
            for (Materialen.Materiaal m : Materialen.ALLE.values()) {
                MateriaalTas.geef(p, Materialen.stack(m.id(), 1, m.zeldzaam() ? 4 : 16));
            }
            for (String kern : Materialen.KERNEN) {
                EcoUtil.geefOfBewaar(p, new ItemStack(Materialen.KERN.get(kern).get(), 2));
            }
            return 1;
        })).then(Commands.argument("materiaal", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(materiaalIds(), b)).executes(c -> {
            MateriaalTas.geef(speler(c), Materialen.stack(StringArgumentType.getString(c, "materiaal"), 0, 16));
            return 1;
        }).then(Commands.argument("kwaliteit", IntegerArgumentType.integer(0, 2)).then(Commands.argument("n", IntegerArgumentType.integer(1, 999)).executes(c -> {
            MateriaalTas.geef(speler(c), Materialen.stack(StringArgumentType.getString(c, "materiaal"), IntegerArgumentType.getInteger(c, "kwaliteit"),
                    IntegerArgumentType.getInteger(c, "n")));
            return 1;
        })))));
        k.then(Commands.literal("plekken").executes(c -> {
            ServerPlayer p = speler(c);
            List<Spot> l = Verzamelen.inBuurt(p, 96);
            zeg(c, l.size() + " plekken in de buurt (96):");
            int n = 0;
            for (Spot s : l) {
                BlockPos pos = Verzamelen.positie(p, s);
                if (n++ < 20) {
                    zeg(c, " " + s.id() + " " + pos.toShortString() + (Verzamelen.beschikbaar(p, s) ? "" : " (leeg)") + (Verzamelen.zeldzaam(p, s) ? " *glinster*" : ""));
                }
            }
            return 1;
        }));
        k.then(Commands.literal("tp").then(Commands.argument("groep", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(AmbachtPlekken.GROEPEN.stream().map(AmbachtPlekken.Groep::id), b)).executes(c -> {
                    ServerPlayer p = speler(c);
                    AmbachtPlekken.Groep g = AmbachtPlekken.groep(StringArgumentType.getString(c, "groep"));
                    BlockPos a = g == null ? null : g.anker();
                    if (a != null && p.level() instanceof ServerLevel sl) {
                        List<Spot> l = PlekWereld.get(sl.getServer()).bekend(g);
                        BlockPos naar = l != null && !l.isEmpty() ? Verzamelen.positie(p, l.get(0)) : a;
                        p.teleportTo(sl, naar.getX() + 0.5, naar.getY() + 1, naar.getZ() + 0.5, java.util.Set.of(), p.getYRot(), p.getXRot(), false);
                    }
                    return 1;
                })));
        k.then(Commands.literal("zeldzaam").executes(c -> {
            ServerPlayer p = speler(c);
            Spot s = dichtste(p);
            if (s != null) {
                Verzamelen.maakZeldzaam(p, s);
                Verzamelen.sync(p, true);
                zeg(c, s.id() + " glinstert nu");
            }
            return 1;
        }));
        k.then(Commands.literal("vul").executes(c -> {
            ServerPlayer p = speler(c);
            for (Spot s : Verzamelen.inBuurt(p, 128)) {
                Verzamelen.staat(p, s).klaar = 0;
                if (s.groep().ambacht() == Ambacht.IMKEREN && Imkeren.eigen(p, s)) {
                    Imkeren.vul(p, s);
                }
            }
            Verzamelen.sync(p, true);
            return 1;
        }));
        k.then(Commands.literal("oogst").executes(c -> {
            ServerPlayer p = speler(c);
            Spot s = dichtste(p);
            zeg(c, s == null ? "geen plek" : s.id() + ": " + Verzamelen.oogst(p, s.id(), 2, false));
            return 1;
        }));
        k.then(Commands.literal("scan").executes(c -> {
            PlekWereld.get(c.getSource().getServer()).vergeet();
            AmbachtNpcs.vergeet();
            zeg(c, "Plekken en werkplaats-guhs worden opnieuw gezocht als je in de buurt komt.");
            return 1;
        }));
        k.then(Commands.literal("eten").then(Commands.argument("id", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Eten.GERECHTEN.keySet(), b)).executes(c -> {
                    Eten.Gerecht g = Eten.GERECHTEN.get(StringArgumentType.getString(c, "id"));
                    if (g != null) {
                        Eten.eet(speler(c), g, 100);
                    }
                    return 1;
                })));
        k.then(Commands.literal("uitgerust").then(Commands.argument("seconden", IntegerArgumentType.integer(0, 7200)).executes(c -> {
            AmbachtData.zetUitgerust(speler(c), IntegerArgumentType.getInteger(c, "seconden"));
            return 1;
        })));
        k.then(Commands.literal("bestel").executes(c -> {
            AmbachtSchermen.boek(speler(c), "bestel", null);
            return 1;
        }).then(Commands.literal("nieuw").executes(c -> {
            AmbachtData.tag(speler(c)).remove("bestel");
            AmbachtSchermen.boek(speler(c), "bestel", null);
            return 1;
        })).then(Commands.literal("klaar").then(Commands.argument("slot", IntegerArgumentType.integer(0, 2)).executes(c -> {
            ServerPlayer p = speler(c);
            int s = IntegerArgumentType.getInteger(c, "slot");
            var t = Bestelbord.vandaag(p).copy();
            int[] v = Bestelbord.getallen(t, "voortgang");
            v[s] = 999;
            t.putIntArray("voortgang", v);
            AmbachtData.tag(p).put("bestel", t);
            Bestelbord.Bestelling b = Bestelbord.POOL.get(Bestelbord.ids(p).get(s));
            if (b != null && b.soort().equals("breng")) {
                String w = b.wat();
                ItemStack st = w.startsWith("eten:") ? new ItemStack(Eten.ITEM.get(w.substring(5)).get(), b.aantal())
                        : w.startsWith("drank:") ? new ItemStack(Brouwsels.ITEM.get(w.substring(6)).get(), b.aantal()) : Materialen.stack(w, 0, b.aantal());
                EcoUtil.geefOfBewaar(p, st);
            }
            return 1;
        }))).then(Commands.literal("week").executes(c -> {
            ServerPlayer p = speler(c);
            var t = Bestelbord.vandaag(p).copy();
            t.putInt("weekN", Bestelbord.WEEKDOEL - 1);
            AmbachtData.tag(p).put("bestel", t);
            zeg(c, "Nog 1 bestelling tot de weekkist.");
            return 1;
        })));
        k.then(Commands.literal("verhaal").then(Commands.argument("ambacht", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(ambachten(), b))
                .then(Commands.argument("stap", IntegerArgumentType.integer(0, AmbachtNpcs.VERHAAL_LEVELS.length)).executes(c -> {
                    Ambacht a = Ambacht.van(StringArgumentType.getString(c, "ambacht"));
                    if (a != null) {
                        AmbachtData.zetVerhaal(speler(c), a, IntegerArgumentType.getInteger(c, "stap"));
                    }
                    return 1;
                }))));
        k.then(Commands.literal("npc").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(AmbachtNpcs.ids(), b))
                .executes(c -> {
                    AmbachtPlekken.Werkplaats w = AmbachtPlekken.werkplaats(StringArgumentType.getString(c, "id"));
                    if (w != null) {
                        AmbachtNpcs.praat(speler(c), null, w);
                    }
                    return 1;
                })));
        k.then(Commands.literal("npcs").executes(c -> {
            for (String id : AmbachtNpcs.ids()) {
                BlockPos p = AmbachtNpcs.waar(id);
                zeg(c, " " + id + ": " + (p == null ? "(nog niet geplaatst: kom in de buurt van de hub)" : p.toShortString()));
            }
            return 1;
        }));
        k.then(Commands.literal("tpnpc").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(AmbachtNpcs.ids(), b))
                .executes(c -> {
                    ServerPlayer p = speler(c);
                    String id = StringArgumentType.getString(c, "id");
                    BlockPos pos = AmbachtNpcs.waar(id);
                    AmbachtPlekken.Werkplaats w = AmbachtPlekken.werkplaats(id);
                    if (pos == null && w != null) {
                        pos = w.anker();
                    }
                    if (pos != null && p.level() instanceof ServerLevel sl) {
                        p.teleportTo(sl, pos.getX() + 0.5, pos.getY(), pos.getZ() + 2.5, java.util.Set.of(), 180, 0, false);
                    }
                    return 1;
                })));
        k.then(Commands.literal("maatwerk").then(Commands.argument("soort", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Maatwerk.SOORTEN.keySet(), b))
                .then(Commands.argument("tier", IntegerArgumentType.integer(1, 3)).executes(c -> {
                    ServerPlayer p = speler(c);
                    Maatwerk.Soort s = Maatwerk.SOORTEN.get(StringArgumentType.getString(c, "soort"));
                    if (s != null) {
                        boolean m = s.slot().sieraad();
                        EcoUtil.geefOfBewaar(p, Maatwerk.maak(s, IntegerArgumentType.getInteger(c, "tier"), List.of(Stat.KRITIEK, Stat.KRACHT, Stat.VADS), 100,
                                p.getGameProfile().name(), m ? Maatwerk.Eigenschap.BAZEN : null, p.getRandom()));
                    }
                    return 1;
                }))));
        k.then(Commands.literal("kist").executes(c -> {
            EcoUtil.geefOfBewaar(speler(c), new ItemStack(Materialen.AMBACHTSKIST.get()));
            return 1;
        }));
        k.then(Commands.literal("export").executes(c -> {
            Path map = Ranglijst.exporteer(c.getSource().getServer());
            Path recepten = map.getParent().resolve("ambachten_recepten.json");
            try {
                Files.createDirectories(map.getParent());
                Files.writeString(recepten, receptenJson(), StandardCharsets.UTF_8);
            } catch (java.io.IOException e) {
                zeg(c, "Kon recepten niet schrijven: " + e);
            }
            zeg(c, "Ranglijsten -> " + map.toAbsolutePath() + ", recepten -> " + recepten.toAbsolutePath());
            return 1;
        }));
        k.then(Commands.literal("reset").executes(c -> {
            ServerPlayer p = speler(c);
            AmbachtData.reset(p);
            Verzamelen.vergeet(p.getUUID());
            zeg(c, "Ambachten van " + p.getGameProfile().name() + " gewist.");
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(k));
    }

    private static List<String> lijstMetAlle() {
        List<String> l = ambachten();
        l.add("alle");
        return l;
    }

    private static List<String> materiaalIds() {
        List<String> l = new ArrayList<>(Materialen.ALLE.keySet());
        l.addAll(Materialen.KERNEN);
        l.add("kernsplinter");
        return l;
    }

    @javax.annotation.Nullable
    static Spot dichtste(ServerPlayer p) {
        Spot beste = null;
        double bd = Double.MAX_VALUE;
        for (Spot s : Verzamelen.inBuurt(p, 96)) {
            double d = Verzamelen.positie(p, s).distSqr(p.blockPosition());
            if (d < bd) {
                bd = d;
                beste = s;
            }
        }
        return beste;
    }

    /** The recipes as JSON (website, VOORSTEL_WEBSITE §5.3: "ambachtsrecepten"). */
    static String receptenJson() {
        JsonArray l = new JsonArray();
        for (Recepten.Recept r : Recepten.ALLE.values()) {
            JsonObject o = new JsonObject();
            o.addProperty("id", r.id());
            o.addProperty("ambacht", r.ambacht().id);
            o.addProperty("level", r.level());
            o.addProperty("tier", r.tier());
            o.addProperty("uit", r.uit());
            o.addProperty("aantal", r.aantal());
            JsonArray in = new JsonArray();
            for (Recepten.Ingredient i : r.in()) {
                JsonObject x = new JsonObject();
                JsonArray opties = new JsonArray();
                i.opties().forEach(opties::add);
                x.add("opties", opties);
                x.addProperty("aantal", i.aantal());
                in.add(x);
            }
            o.add("in", in);
            l.add(o);
        }
        return new GsonBuilder().setPrettyPrinting().create().toJson(l);
    }
}
