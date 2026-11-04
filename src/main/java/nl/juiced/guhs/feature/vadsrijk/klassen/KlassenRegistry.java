package nl.juiced.guhs.feature.vadsrijk.klassen;

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
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.SpelerData;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klassen;
import nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom;
import nl.juiced.guhs.feature.vadsrijk.klassen.jager.Pluisjager;
import nl.juiced.guhs.feature.vadsrijk.klassen.magier.Vadsmagier;
import nl.juiced.guhs.feature.vadsrijk.klassen.schot.Schoten;

/**
 * Het Vadsrijk, slice "klassen" (CONTRACT_VADSRIJK 1/7): the Pluisjager and the Vadsmagier, the 3 skill trees (incl. the
 * Knuffelridder's, through {@link RidderMetBoom}), the class weapons ({@link Wapens}), respec + class switch at the ordetempel
 * ({@link KlasseBalie}), job advancement at lv 10/20 ({@link Advancement}), the ranged basic attack ({@link Basisaanval}).
 * Commands: {@code /guhs vadsrijk klasse ...} (see {@link #commando}).
 */
public final class KlassenRegistry {
    private KlassenRegistry() {
    }

    /**
     * Integration 1.10.0 (contract 13): the tree nodes that change a skill's cooldown/energy go through kern's
     * {@code Vaardigheden.COOLDOWN_MOD/ENERGIE_MOD} (before: patched right after kern paid), and the HUD shows the advancement title.
     */
    private static void kernHaken() {
        nl.juiced.guhs.feature.vadsrijk.kern.Vaardigheden.COOLDOWN_MOD.add((p, v) -> {
            int min = switch (v.id()) {
                case "rolsalto" -> nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom.heeft(p, "j_plakbom") ? 40 : 0;
                case "kussenklap" -> nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom.heeft(p, "r_kussenstorm") ? 30 : 0;
                case "stormsprong" -> nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom.heeft(p, "r_aanloop") ? 60 : 0;
                default -> 0;
            };
            return min <= 0 || v.cooldownTicks() <= 0 ? null : (float) Math.max(0, v.cooldownTicks() - min) / v.cooldownTicks();
        });
        nl.juiced.guhs.feature.vadsrijk.kern.Vaardigheden.ENERGIE_MOD.add((p, v) -> {
            if (v.id().equals("kussenklap") && v.energie() > 0 && nl.juiced.guhs.feature.vadsrijk.klassen.boom.Boom.heeft(p, "r_kussenstorm")) {
                return (float) (Math.max(0, v.energie() - 8) / v.energie());
            }
            return null;
        });
        Spelers.TITEL_BRON.add(p -> Spelers.klasse(p) == null ? null : Advancement.titelKey(p));
    }

    public static void register(IEventBus modBus) {
        Wapens.ITEMS.register(modBus);
        Wapens.registreer();
        RidderMetBoom.init();
        Pluisjager.init();
        Vadsmagier.init();
        kernHaken();
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            Later.tick();
            Schoten.tick();
            Basisaanval.tick(e.getServer());
            RidderMetBoom.tick(e.getServer());
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            Basisaanval.vergeet(e.getEntity().getUUID());
            KlasseBalie.vergeet(e.getEntity().getUUID());
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> {
            Schoten.wisAlles();
            Later.wis();
        });
        NeoForge.EVENT_BUS.addListener(KlassenRegistry::commando);
        Haken.LEVEL_OP.add((p, wat) -> {
            int lvl = Spelers.level(p);
            if (lvl >= 2) {
                Tips.toon(p, "vadsrijk.klassen.tip.punt");
            }
            if (Advancement.kanDoen(p) && (lvl == 10 || lvl == 20)) {
                p.sendSystemMessage(Component.translatable("vadsrijk.klassen.adv.klaar", Component.translatable(
                        Advancement.titelKey(Spelers.klasse(p).id(), Advancement.stap(p, Spelers.klasse(p).id()) + 1))).withStyle(ChatFormatting.GOLD));
                Tips.toon(p, "vadsrijk.klassen.tip.adv");
            }
        });
        Haken.BINNEN.add((p, binnen) -> {
            if (binnen && !KlasseBalie.gekozen(p)) {
                Tips.toon(p, "vadsrijk.klassen.tip.welkom");
            }
        });
    }

    public static void payloads(PayloadRegistrar registrar) {
        KlassenPayloads.register(registrar);
    }

    /**
     * {@code /guhs vadsrijk klasse}: your class, level, title and points. Dev (op 2): {@code kies} (the first-class screen
     * again), {@code balie} (the ordetempel desk right here), {@code boom}, {@code wissel <klasse>}, {@code respecvrij}
     * (cooldown off), {@code adv} (next advancement now, needs the level), {@code reset} (tree, advancements, choice and
     * starter weapons of all classes forgotten).
     */
    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> k = Commands.literal("klasse").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Klasse kl = Spelers.klasse(p);
            c.getSource().sendSuccess(() -> Component.translatable("vadsrijk.klassen.cmd.info", Component.translatable(Advancement.titelKey(p)),
                    Spelers.level(p), Spelers.skillpunten(p, kl.id()), String.join(", ", Boom.knopen(Spelers.data(p), kl.id()))), false);
            return 1;
        });
        k.then(dev("kies").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Spelers.data(p).extra("klassen").putBoolean("gekozen", false);
            KlasseBalie.kiesEerste(p);
            return 1;
        }));
        k.then(dev("balie").executes(c -> {
            KlasseBalie.open(c.getSource().getPlayerOrException());
            return 1;
        }));
        k.then(dev("boom").executes(c -> {
            KlasseBalie.boom(c.getSource().getPlayerOrException());
            return 1;
        }));
        k.then(dev("wissel").then(Commands.argument("klasse", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(Klassen.alle().stream().map(Klasse::id), b))
                .executes(c -> KlasseBalie.wissel(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "klasse")) ? 1 : 0)));
        k.then(dev("respecvrij").executes(c -> {
            Spelers.data(c.getSource().getPlayerOrException()).respecKlaar = 0;
            c.getSource().sendSuccess(() -> Component.literal("Respec-afkoeltijd weg."), false);
            return 1;
        }));
        k.then(dev("adv").executes(c -> Advancement.doe(c.getSource().getPlayerOrException()) ? 1 : 0));
        k.then(dev("punten").then(Commands.argument("level", IntegerArgumentType.integer(1, 35)).executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Spelers.zetLevel(p, IntegerArgumentType.getInteger(c, "level"));
            KlasseBalie.boom(p);
            return 1;
        })));
        // (dev/autocheck: fire a skill or a basic shot without keys, a fresh class weapon in the hand)
        k.then(dev("skill").then(Commands.argument("slot", IntegerArgumentType.integer(0, 3))
                .executes(c -> nl.juiced.guhs.feature.vadsrijk.kern.Vaardigheden.gebruik(c.getSource().getPlayerOrException(),
                        IntegerArgumentType.getInteger(c, "slot")) ? 1 : 0)));
        k.then(dev("schiet").executes(c -> {
            Basisaanval.verzoek(c.getSource().getPlayerOrException());
            return 1;
        }));
        k.then(dev("wapen").then(Commands.argument("type", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes.alle().stream()
                        .filter(t -> t.slot() == nl.juiced.guhs.feature.vadsrijk.kern.loot.Slot.WAPEN).map(t -> t.id()), b))
                .executes(c -> {
                    ServerPlayer p = c.getSource().getPlayerOrException();
                    var bt = nl.juiced.guhs.feature.vadsrijk.kern.loot.BasisTypes.van(StringArgumentType.getString(c, "type"));
                    if (bt == null) {
                        return 0;
                    }
                    var u = nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen.rol(bt, Math.max(1, Spelers.level(p)),
                            nl.juiced.guhs.feature.vadsrijk.kern.loot.Zeldzaamheid.ZELDZAAM, p.getRandom());
                    p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, nl.juiced.guhs.feature.vadsrijk.kern.loot.Rollen.stack(u));
                    return 1;
                })));
        k.then(dev("reset").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            SpelerData d = Spelers.data(p);
            d.boom.clear();
            d.respecKlaar = 0;
            d.extra.remove("klassen");
            Spelers.sync(p);
            c.getSource().sendSuccess(() -> Component.literal("Klassen vergeten: bomen, advancements, eerste keuze en startwapens."), false);
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(k));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }
}
