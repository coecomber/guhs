package nl.juiced.guhs.feature.vadsrijk.party;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.hud.PartyFrames;
import nl.juiced.guhs.feature.vadsrijk.hud.UHudPayloads;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.kern.party.PartijenData;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepConfig;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepRol;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepSpeler;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepSpelers;
import nl.juiced.guhs.feature.vadsrijk.party.nep.NepVulling;

/**
 * W-PARTY: the Party tab (saved parties, invites with a toast, leader tools, group queueing) and the nep-spelers. Registers the
 * nep-speler entity, the server config, the payloads, the ticks, the party frames for companions, and the dev command
 * {@code /guhs vadsrijk nepspeler spawn [rol] [aantal] | rekruteer | aan | uit | vul | weg | lijst | mij aan|uit}.
 */
public final class PartyRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);
    public static final DeferredHolder<EntityType<?>, EntityType<NepSpeler>> NEPSPELER = ENTITIES.register("vr_nepspeler",
            () -> EntityType.Builder.<NepSpeler>of(NepSpeler::new, MobCategory.MISC).sized(0.6f, 1.8f).eyeHeight(1.62f)
                    .clientTrackingRange(8).updateInterval(2).build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_nepspeler"))));

    private PartyRegistry() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> e.put(NEPSPELER.get(), NepSpeler.attributen().build()));
        ModList.get().getModContainerById(Guhs.MODID).ifPresent(c -> c.registerConfig(ModConfig.Type.SERVER, NepConfig.SPEC, "guhs-vadsrijk-nepspelers.toml"));
        PartyDienst.init();
        NepSpelers.init();
        PartyFrames.EXTRA.add(PartyRegistry::frames);
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> {
            NepSpelers.wisAlles();
            NepVulling.wisAlles();
            PartijenData.laad(e.getServer());
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> {
            NepSpelers.wisAlles();
            NepVulling.wisAlles();
            PartijenData.vergeet();
        });
        nl.juiced.guhs.world.TikWacht.post("PartyRegistry:79", e -> {
            NepSpelers.tick(e.getServer());
            NepVulling.tick(e.getServer());
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            NepSpelers.uitgelogd(e.getEntity().getUUID());
            PartyDienst.vergeet(e.getEntity().getUUID());
        });
        NeoForge.EVENT_BUS.addListener(PartyRegistry::commando);
        DungeonRijBrug.koppel();   // W-INT: the party tab + nep-spelers on w-grot's dungeon finder
    }

    public static void payloads(PayloadRegistrar registrar) {
        PartyPayloads.register(registrar);
    }

    /** The companions in p's frames: "[NPC] name", their role as the spec line, fainted = grey. */
    static List<UHudPayloads.Frame> frames(ServerPlayer p) {
        List<UHudPayloads.Frame> uit = new ArrayList<>();
        for (NepSpeler n : NepSpelers.metgezellen(p)) {
            if (uit.size() >= 3) {
                break;
            }
            String naam = "[NPC] " + n.naam();
            uit.add(new UHudPayloads.Frame(n.getUUID(), naam.length() > 32 ? naam.substring(0, 32) : naam, n.rol().klasse, n.rol().key(), n.nepLevel(),
                    n.getHealth(), n.getMaxHealth(), List.of(), n.level().dimension().identifier().toString(), (float) n.getX(), (float) n.getY(),
                    (float) n.getZ(), n.flauw() ? UHudPayloads.STAAT_FLAUW : UHudPayloads.STAAT_OK, false));
        }
        return uit;
    }

    // ---- dev command --------------------------------------------------------------------------------------------------------

    private static void commando(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> m = Commands.literal("nepspeler").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
        m.then(Commands.literal("spawn").executes(c -> spawn(c.getSource(), null, 1))
                .then(Commands.argument("rol", StringArgumentType.word()).suggests((c, b) -> SharedSuggestionProvider.suggest(
                                java.util.Arrays.stream(NepRol.values()).map(NepRol::id), b))
                        .executes(c -> spawn(c.getSource(), NepRol.vanId(StringArgumentType.getString(c, "rol")), 1))
                        .then(Commands.argument("aantal", IntegerArgumentType.integer(1, 3))
                                .executes(c -> spawn(c.getSource(), NepRol.vanId(StringArgumentType.getString(c, "rol")), IntegerArgumentType.getInteger(c, "aantal"))))));
        m.then(Commands.literal("rekruteer").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            NepSpeler n = NepSpelers.alle().stream().filter(x -> x.wild() && x.level() == p.level() && x.distanceTo(p) < 24)
                    .min(Comparator.comparingDouble(x -> x.distanceToSqr(p))).orElse(null);
            if (n == null) {
                n = NepSpelers.spawn((ServerLevel) p.level(), p.position().add(1.5, 0, 0), NepRol.HELER, Spelers.level(p), null, null);
            }
            String r = NepSpelers.rekruteer(p, n);
            NepSpeler fn = n;
            c.getSource().sendSuccess(() -> Component.translatable(r, fn.naam()), false);
            return r.endsWith(".mee") ? 1 : 0;
        }));
        m.then(Commands.literal("aan").executes(c -> {
            NepConfig.zet(true);
            c.getSource().sendSuccess(() -> Component.literal("Nep-spelers AAN voor de hele server."), true);
            return 1;
        }));
        m.then(Commands.literal("uit").executes(c -> {
            NepConfig.zet(false);
            NepSpelers.allemaalWeg(true);
            c.getSource().sendSuccess(() -> Component.literal("Nep-spelers UIT voor de hele server (ze zijn allemaal weg)."), true);
            return 1;
        }));
        m.then(Commands.literal("vul").executes(c -> {
            if (Wachtrijen.DUNGEON == null) {
                c.getSource().sendFailure(Component.literal("Geen dungeon-wachtrij gekoppeld (Wachtrijen.DUNGEON is leeg: integratie)."));
                return 0;
            }
            int n = NepVulling.tick(c.getSource().getServer(), c.getSource().getServer().overworld().getGameTime(), true);
            c.getSource().sendSuccess(() -> Component.literal(n + " nep-speler(s) in de wachtrijen gezet (alleen waar het mag)."), false);
            return n;
        }));
        m.then(Commands.literal("weg").executes(c -> {
            int n = NepSpelers.alle().size();
            NepSpelers.allemaalWeg(true);
            c.getSource().sendSuccess(() -> Component.literal(n + " nep-speler(s) weg."), false);
            return n;
        }));
        m.then(Commands.literal("lijst").executes(c -> {
            List<NepSpeler> l = NepSpelers.alle();
            c.getSource().sendSuccess(() -> Component.literal(l.size() + " nep-speler(s); server " + (NepConfig.aan() ? "aan" : "uit")), false);
            for (NepSpeler n : l) {
                String wat = n.wild() ? "wild in " + n.gebied : n.dungeon != null ? "dungeon " + n.dungeon : "party";
                c.getSource().sendSuccess(() -> Component.literal(" - " + n.naam() + " (" + n.rol().id() + ", Lv " + n.nepLevel() + ", " + wat + ") @ "
                        + n.blockPosition().toShortString()), false);
            }
            return l.size();
        }));
        m.then(Commands.literal("mij").then(Commands.literal("aan").executes(c -> {
            NepSpelers.zetStand(c.getSource().getPlayerOrException().getUUID(), true);
            c.getSource().sendSuccess(() -> Component.literal("Jouw nep-spelers: aan (server-kant; de client-instelling stuurt dit ook)."), false);
            return 1;
        })).then(Commands.literal("uit").executes(c -> {
            NepSpelers.zetStand(c.getSource().getPlayerOrException().getUUID(), false);
            c.getSource().sendSuccess(() -> Component.literal("Jouw nep-spelers: uit (server-kant)."), false);
            return 1;
        })));
        event.getDispatcher().register(VadsrijkCommando.wortel(m));
    }

    /** Dev: wild nep-spelers right next to you (your zone, your level), whatever the zone. */
    private static int spawn(CommandSourceStack src, NepRol rol, int aantal) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel sl = (ServerLevel) p.level();
        Gebieden.Gebied g = Gebieden.op(p.blockPosition());
        String gebied = g == null ? "dev" : g.id();
        List<NepSpeler> l = new ArrayList<>();
        for (int i = 0; i < aantal; i++) {
            NepRol r = rol != null ? rol : NepRol.values()[sl.getRandom().nextInt(NepRol.values().length)];
            Vec3 pos = p.position().add(p.getLookAngle().multiply(2.5, 0, 2.5)).add(i * 1.2, 0, 0);
            NepSpeler n = NepSpelers.spawn(sl, pos, r, Spelers.level(p), null, null);
            n.gebied = gebied;
            n.tot = sl.getServer().overworld().getGameTime() + 20 * 60 * 15;
            l.add(n);
        }
        src.sendSuccess(() -> Component.literal(l.size() + " nep-speler(s): " + String.join(", ", l.stream().map(n -> n.naam() + " (" + n.rol().id() + ")").toList())
                + ". Rechtsklik er een voor \"Kom je mee?\"."), false);
        return l.size();
    }
}
