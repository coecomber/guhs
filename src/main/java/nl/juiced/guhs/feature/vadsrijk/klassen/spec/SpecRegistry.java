package nl.juiced.guhs.feature.vadsrijk.klassen.spec;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.klassen.Advancement;
import nl.juiced.guhs.feature.vadsrijk.klassen.KlasseBalie;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Bomen;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom;

/**
 * U-SPECS: specialisations + the new job-advancement model (ronde 4, V2/V3). Registered LAST in {@code VadsrijkFeature} (the tree
 * re-layout needs every slice's nodes). Commands: {@code /guhs vadsrijk spec} (info) and dev (op 2) {@code spec kies <spec>},
 * {@code spec scherm}, {@code spec wissel [forceer]}, {@code spec stap <0-3>}, {@code spec gevecht}, {@code spec oud <0-4>},
 * {@code spec migreer}.
 */
public final class SpecRegistry {
    private SpecRegistry() {
    }

    public static void register(IEventBus modBus) {
        SpecBoom.herindeel();
        SpecEffecten.init();
        Advancement.CEREMONIE.add(SpecEffecten::ceremonie);
        // the story places of steps 2 and 3 (step 1 = the lv-17 quest of the Grootmeester, before the Zwamtempel)
        Advancement.POORT.put(2, p -> nl.juiced.guhs.feature.vadsrijk.npc.NpcApi.questKlaar(p, "z3_hq08_werf"));
        Advancement.POORT.put(3, p -> nl.juiced.guhs.feature.vadsrijk.npc.NpcApi.questKlaar(p, "z5_hq09_slaaplied"));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> SpecEffecten.tick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> SpecEffecten.vergeet(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener(SpecRegistry::commando);
        Haken.BINNEN.add((p, binnen) -> {
            if (binnen && Specs.migreer(Spelers.data(p))) {
                questsAlGedaan(p);
                Spelers.sync(p);
                p.sendSystemMessage(Component.translatable("vadsrijk.klassen.spec.migratie").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            if (binnen) {
                for (Klasse k : Klassen.alle()) {
                    if (Specs.open(p, k.id())) {
                        p.sendSystemMessage(Component.translatable("vadsrijk.klassen.spec.kies_nog", Component.translatable(k.naamKey()))
                                .withStyle(ChatFormatting.GOLD));
                    }
                }
            }
        });
        Haken.LEVEL_OP.add((p, wat) -> {
            int i = wat.lastIndexOf(':');
            String kl = i < 0 ? "" : wat.substring(0, i);
            if (Klassen.van(kl) == null) {
                return;
            }
            int lvl;
            try {
                lvl = Integer.parseInt(wat.substring(i + 1));
            } catch (NumberFormatException e) {
                return;
            }
            int s = Advancement.stap(p, kl);
            if (s < Advancement.MAX_STAP && lvl == Advancement.nodigLevel(s)) {
                p.sendSystemMessage(Component.translatable("vadsrijk.klassen.adv.klaar" + (s + 1), Component.translatable(Klassen.ofRidder(kl).naamKey()))
                        .withStyle(ChatFormatting.GOLD));
                Tips.toon(p, "vadsrijk.klassen.tip.adv");
            }
        });
        // the class desk in the zone-4 and zone-5 hubs (zone 1-3 have their ordetempel NPCs): an extra dialogue option
        for (String npc : new String[] {"z4_duinwachter", "z5_veerkracht"}) {
            nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor.EXTRA.computeIfAbsent(npc, x -> new java.util.concurrent.CopyOnWriteArrayList<>())
                    .add(new nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor.ExtraOptie("vadsrijk.klassen.spec.optie_balie", p -> true, KlasseBalie::open));
        }
    }

    /** Migration: the new advancement quests of the steps a save already has count as done (no double offer). */
    static void questsAlGedaan(ServerPlayer p) {
        String[] ids = {"spec17_", "z3_adv35_", "z5_adv58_"};
        var q = nl.juiced.guhs.feature.vadsrijk.npc.Questlog.map(p, "q");
        for (Klasse k : Klassen.alle()) {
            int stap = Advancement.stap(p, k.id());
            for (int s = 1; s <= stap && s <= ids.length; s++) {
                net.minecraft.nbt.CompoundTag t = new net.minecraft.nbt.CompoundTag();
                t.putInt("s", nl.juiced.guhs.feature.vadsrijk.npc.Questlog.KLAAR);
                q.put(ids[s - 1] + k.id(), t);
            }
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> s = Commands.literal("spec").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            for (Klasse k : Klassen.alle()) {
                Spec sp = Specs.spec(p, k.id());
                int stap = Advancement.stap(p, k.id());
                c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.klassen.spec.cmd.info", Component.translatable(k.naamKey()),
                        Spelers.level(p, k.id()), sp == null ? Component.translatable("vadsrijk.klassen.spec.geen") : Component.translatable(sp.naamKey()),
                        stap, Component.translatable(Advancement.titelKey(p, k.id(), stap))), false);
            }
            long w = Specs.wisselOver(p, System.currentTimeMillis());
            c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.klassen.spec.cmd.wissel", w > 0 ? (w / 60 + 1) + " min" : "-"), false);
            return 1;
        });
        s.then(dev("kies").then(Commands.argument("spec", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(Spec.values()).map(x -> x.id), b))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    Spec sp = Spec.van(StringArgumentType.getString(c, "spec"));
                    if (sp == null) {
                        return 0;
                    }
                    if (Specs.spec(p, sp.klasse) != null) {
                        Boom.knopen(Spelers.data(p), sp.klasse).remove(Specs.spec(p, sp.klasse).knoop());
                        Specs.snoei(Klassen.van(sp.klasse), Boom.knopen(Spelers.data(p), sp.klasse));
                    }
                    if (Spelers.level(p, sp.klasse) < Advancement.LV1) {
                        Spelers.zetKlasseLevel(p, sp.klasse, Advancement.LV1);
                    }
                    Specs.zetOpen(Spelers.data(p), sp.klasse, true);
                    String nee = Specs.kies(p, sp);
                    c.getSource().sendSuccess(() -> Component.literal(nee == null ? "Spec: " + sp.id : "Niet gelukt: " + nee), false);
                    return nee == null ? 1 : 0;
                })));
        s.then(dev("scherm").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            KlasseBalie.vraagSpec(p, Spelers.klasse(p).id());
            return 1;
        }));
        s.then(dev("wissel").executes(c -> wissel(c.getSource(), false))
                .then(Commands.literal("forceer").executes(c -> wissel(c.getSource(), true))));
        s.then(dev("stap").then(Commands.argument("stap", IntegerArgumentType.integer(0, Advancement.MAX_STAP)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            String k = Spelers.klasse(p).id();
            int n = IntegerArgumentType.getInteger(c, "stap");
            SpelerData d = Spelers.data(p);
            for (int i = 1; i <= 4; i++) {
                Boom.knopen(d, k).remove(Bomen.advKnoop(k, i));
            }
            if (n >= 1 && Specs.spec(p, k) == null) {
                c.getSource().sendFailure(Component.literal("Kies eerst een spec: /guhs vadsrijk spec kies <spec>"));
                Spelers.sync(p);
                return 0;
            }
            if (n >= 1) {
                if (Spelers.level(p, k) < Advancement.levelVan(n)) {
                    Spelers.zetKlasseLevel(p, k, Advancement.levelVan(n));
                }
                Advancement.voltooi(p, k, n);
            } else {
                Spelers.sync(p);
            }
            return 1;
        })));
        s.then(dev("gevecht").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Specs.zetGevecht(p, !Specs.inGevecht(p));
            c.getSource().sendSuccess(() -> Component.literal("In gevecht: " + Specs.inGevecht(p)), false);
            return 1;
        }));
        s.then(dev("oud").then(Commands.argument("stappen", IntegerArgumentType.integer(0, 4)).executes(c -> {
            // simulate an old save: the old lv 10/20/30/50 step nodes, no spec, a few bought nodes, migration version 1
            ServerPlayer p = c.getSource().getPlayerOrException();
            String k = Spelers.klasse(p).id();
            SpelerData d = Spelers.data(p);
            var heeft = Boom.knopen(d, k);
            heeft.clear();
            int n = IntegerArgumentType.getInteger(c, "stappen");
            for (int i = 1; i <= n; i++) {
                heeft.add(Spec.prefix(k) + "_adv" + i);
            }
            Klasse kl = Klassen.van(k);
            for (var b : kl.boom()) {
                if (b.rij() == 0 && b.vereist().isEmpty() && heeft.size() < n + 2) {
                    heeft.add(b.id());
                }
            }
            d.extra("klassen").putInt("spec_v", 1);
            d.extra("klassen").remove("spec_open_" + k);
            Spelers.sync(p);
            c.getSource().sendSuccess(() -> Component.literal("Oude save nagebootst (" + n + " oude stappen). Nu: /guhs vadsrijk spec migreer"), false);
            return 1;
        })));
        s.then(dev("migreer").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            boolean iets = Specs.migreer(Spelers.data(p));
            if (iets) {
                questsAlGedaan(p);
            }
            Spelers.sync(p);
            c.getSource().sendSuccess(() -> Component.literal(iets ? "Gemigreerd." : "Niets te migreren (al versie " + Specs.VERSIE + ")."), false);
            for (Klasse k : Klassen.alle()) {
                if (Specs.open(p, k.id())) {
                    p.sendSystemMessage(Component.translatable("vadsrijk.klassen.spec.kies_nog", Component.translatable(k.naamKey())).withStyle(ChatFormatting.GOLD));
                }
            }
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(s));
    }

    private static int wissel(CommandSourceStack src, boolean forceer) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        String k = Spelers.klasse(p).id();
        if (forceer) {
            Spelers.data(p).extra("klassen").remove("spec_wissel");
            Specs.zetGevecht(p, false);
            Specs.TEST_BALIE.add(p.getUUID());
        }
        String nee = Specs.wissel(p, k, false, System.currentTimeMillis());
        Specs.TEST_BALIE.remove(p.getUUID());
        if (nee != null) {
            src.sendFailure(Component.translatable("vadsrijk.klassen.spec.nee." + nee, (Specs.wisselOver(p, System.currentTimeMillis()) / 60 + 1) + " min"));
            return 0;
        }
        src.sendSuccess(() -> Component.translatable("vadsrijk.klassen.spec.gewisseld", Component.translatable(Specs.spec(p, k).naamKey())), false);
        return 1;
    }
}
