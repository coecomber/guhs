package nl.juiced.guhs.feature.vadsrijk.luchtschip;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldReizen;

/**
 * T-LUCHTSCHIP: the scheduled airships (W-3) + the view system (own view distance of the dimension, the silhouettes on the
 * client). Hooked in from {@code VadsrijkFeature} (one marked line each in register / payloads).
 * <p>
 * Dev: {@code /guhs vadsrijk luchtschip lijnen | vertrek <lijn> [s] | rij <lijn> [heen|terug] | haven <id> | land | schimmen
 * aan|uit|altijd | zicht [n] | reset}.
 */
public final class LuchtschipRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Guhs.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<LuchtschipEntity>> SCHIP = ENTITY_TYPES.register("vr_luchtschip",
            () -> EntityType.Builder.<LuchtschipEntity>of(LuchtschipEntity::new, MobCategory.MISC).sized(7.0f, 2.2f).noSummon().noSave()
                    .fireImmune().clientTrackingRange(20).updateInterval(20)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_luchtschip"))));
    public static final DeferredHolder<EntityType<?>, EntityType<VertrekbordEntity>> BORD = ENTITY_TYPES.register("vr_luchtschip_bord",
            () -> EntityType.Builder.<VertrekbordEntity>of(VertrekbordEntity::new, MobCategory.MISC).sized(0.2f, 0.2f).noSummon().noSave()
                    .fireImmune().clientTrackingRange(4).updateInterval(20)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, Guhs.id("vr_luchtschip_bord"))));

    private LuchtschipRegistry() {
    }

    public static void register(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            Luchtschepen.tick(e.getServer());
            if (e.getServer().getTickCount() % 20 == 0) {
                Zicht.pas(e.getServer());
            }
        });
        NeoForge.EVENT_BUS.addListener(Luchtschepen::opMount);
        NeoForge.EVENT_BUS.addListener((LivingIncomingDamageEvent e) -> Luchtschepen.opSchade(e));
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent e) -> Luchtschepen.opDood(e));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> Luchtschepen.opUitloggen(e));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            Luchtschepen.opInloggen(e);
            if (e.getEntity() instanceof ServerPlayer p) {
                Zicht.binnen(p);
                Tijd.stuur(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                Zicht.binnen(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                Zicht.binnen(p);
            }
        });
        NeoForge.EVENT_BUS.addListener(LuchtschipRegistry::commandos);
    }

    public static void payloads(PayloadRegistrar r) {
        r.playToClient(Tijd.Schuif.TYPE, Tijd.Schuif.STREAM_CODEC, (p, c) -> c.enqueueWork(() -> Tijd.schuifOntvanger.accept(p.tag())));
        r.playToClient(Tijd.Schimmen.TYPE, Tijd.Schimmen.STREAM_CODEC, (p, c) -> c.enqueueWork(() -> Tijd.schimmenOntvanger.accept(p.stand())));
    }

    // ------------------------------------------------------------------------------------------------ dev commands

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> lijnArg() {
        return Commands.argument("lijn", StringArgumentType.word()).suggests((c, b) -> {
            Lijnen.ALLE.forEach(l -> b.suggest(l.letter()));
            Lijnen.ALLE.forEach(l -> b.suggest(l.id()));
            return b.buildFuture();
        });
    }

    private static Lijnen.Lijn lijn(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c) {
        Lijnen.Lijn l = Lijnen.lijn(StringArgumentType.getString(c, "lijn"));
        if (l == null) {
            c.getSource().sendFailure(Component.literal("Onbekende lijn (0, A, B, C, D)"));
        }
        return l;
    }

    private static ServerLevel rijk(ServerPlayer p) {
        if (!Vadsrijk.in(p)) {
            VadsrijkCommando.naarBinnen(p, false);
        }
        return Vadsrijk.level(p.level().getServer());
    }

    private static void naarPier(ServerPlayer p, Lijnen.Haven h) {
        ServerLevel sl = rijk(p);
        if (sl == null) {
            return;
        }
        Vec3 v = WereldReizen.veiligeLanding(sl, h.plek());
        p.teleport(new TeleportTransition(sl, v, Vec3.ZERO, h.richting(), 0f, TeleportTransition.DO_NOTHING));
    }

    private static void commandos(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> w = Commands.literal("luchtschip");
        w.then(dev("lijnen").executes(c -> {
            ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
            if (sl == null) {
                return 0;
            }
            Luchtschepen.status(sl).forEach(m -> c.getSource().sendSuccess(() -> m, false));
            return 1;
        }));
        w.then(dev("vertrek").then(lijnArg().executes(c -> vertrek(c, 5)).then(Commands.argument("seconden", IntegerArgumentType.integer(1, 170))
                .executes(c -> vertrek(c, IntegerArgumentType.getInteger(c, "seconden"))))));
        w.then(dev("rij").then(lijnArg().executes(c -> rij(c, true)).then(Commands.literal("heen").executes(c -> rij(c, true)))
                .then(Commands.literal("terug").executes(c -> rij(c, false)))));
        w.then(dev("haven").then(Commands.argument("id", StringArgumentType.word()).suggests((c, b) -> {
            Lijnen.havens().forEach(h -> b.suggest(h.id()));
            return b.buildFuture();
        }).executes(c -> {
            Lijnen.Haven h = Lijnen.haven(StringArgumentType.getString(c, "id"));
            if (h == null) {
                c.getSource().sendFailure(Component.literal("Onbekende haven"));
                return 0;
            }
            naarPier(c.getSource().getPlayerOrException(), h);
            return 1;
        })));
        w.then(dev("land").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            if (p.getVehicle() instanceof LuchtschipEntity e && e.lijn() != null && e.stand() != null && p.level() instanceof ServerLevel sl) {
                Luchtschepen.land(sl, e, e.stand().naar(e.lijn()));
                return 1;
            }
            c.getSource().sendFailure(Component.literal("Je zit niet in een luchtschip"));
            return 0;
        }));
        w.then(dev("schimmen").then(Commands.literal("aan").executes(c -> schimmen(c, 1))).then(Commands.literal("uit").executes(c -> schimmen(c, 0)))
                .then(Commands.literal("altijd").executes(c -> schimmen(c, 2))));
        w.then(dev("zicht").executes(c -> {
            var server = c.getSource().getServer();
            c.getSource().sendSuccess(() -> Component.literal("Vadsrijk-zichtafstand: " + Zicht.effectief(server) + " chunks (ingesteld "
                    + Zicht.ingesteld() + ", server " + server.getPlayerList().getViewDistance() + "; config/" + Zicht.BESTAND + ")"), false);
            return 1;
        }).then(Commands.argument("chunks", IntegerArgumentType.integer(0, 32)).executes(c -> {
            Zicht.zet(c.getSource().getServer(), IntegerArgumentType.getInteger(c, "chunks"));
            c.getSource().sendSuccess(() -> Component.literal("Vadsrijk-zichtafstand nu " + Zicht.effectief(c.getSource().getServer()) + " chunks"), true);
            return 1;
        })));
        w.then(dev("reset").executes(c -> {
            ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
            if (sl != null) {
                Tijd.wis(sl);
                Dienstregeling.vergeet();
            }
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(w));
    }

    private static int vertrek(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, int seconden) {
        Lijnen.Lijn l = lijn(c);
        ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
        if (l == null || sl == null) {
            return 0;
        }
        Tijd.vertrekOver(sl, l, seconden * 20);
        c.getSource().sendSuccess(() -> Component.literal("Lijn " + l.letter() + " vertrekt over " + seconden + " s (beide havens)"), false);
        return 1;
    }

    /** Go to the departure pier of the line, the ship leaves in 8 s, and you are on board (gate ignored). */
    private static int rij(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, boolean heen) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Lijnen.Lijn l = lijn(c);
        if (l == null) {
            return 0;
        }
        Lijnen.Haven van = heen ? l.a() : l.b();
        naarPier(p, van);
        ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
        if (sl == null) {
            return 0;
        }
        Tijd.vertrekOver(sl, l, 160);
        int k = Dienstregeling.schipAan(l, van, Tijd.nu(sl, l));
        if (k < 0) {
            return 0;
        }
        LuchtschipEntity e = Luchtschepen.schipNu(sl, l, k);
        if (e == null) {
            return 0;
        }
        e.volg();
        return Luchtschepen.instappen(p, e, true) ? 1 : 0;
    }

    private static int schimmen(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c, int stand) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PacketDistributor.sendToPlayer(c.getSource().getPlayerOrException(), new Tijd.Schimmen(stand));
        return 1;
    }
}
