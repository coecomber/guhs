package nl.juiced.guhs.feature.vadsrijk.wereld.sfeer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.Slijmwezen;

/**
 * Het Vadsrijk, "sfeer" (fix round 1, ronde 14): fog and light per area, the dragon pass, the skyline light wave, the spore
 * storm, pink showers with a rainbow and the aurora above the Zwamtempel. Nearly all of it is client side
 * ({@code sfeer.client}); the server runs the dragon pass, the happy slimes in the shower, the Opperzwam flag and the dev commands
 * {@code /guhs vadsrijk sfeer ...}.
 */
public final class SfeerRegistry {
    /** {@code SpelerData.gezien} key of the first Opperzwam kill (kern {@code VadsrijkBaas#killSleutel}). */
    public static final String OPPERZWAM_KILL = "baaskill:guhs:vr_baas_opperzwam";
    private static final Map<UUID, Boolean> VLAG = new HashMap<>();
    /** A forced shower (dev) lasts until this game time (server: the slimes hop). */
    private static long buiTot;

    private SfeerRegistry() {
    }

    public static void register(IEventBus modBus) {
        Schade.NA_KLAP.add(Drakenpas::klap);
        nl.juiced.guhs.world.TikWacht.post("SfeerRegistry:49", e -> {
            ServerLevel sl = Vadsrijk.level(e.getServer());
            if (sl == null) {
                return;
            }
            Drakenpas.tick(sl);
            long nu = sl.getGameTime();
            if (nu % 40 == 0) {
                for (ServerPlayer p : sl.players()) {
                    vlag(p, false);
                }
            }
            if (nu % 10 == 0 && (SfeerKlok.bui(sl) > 0.2f || nu < buiTot)) {
                blijeSlijmpjes(sl);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                vlag(p, true);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                vlag(p, true);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            VLAG.remove(e.getEntity().getUUID());
            Drakenpas.vergeet(e.getEntity().getUUID());
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            VLAG.clear();
            Drakenpas.wis();
            buiTot = 0;
        });
        NeoForge.EVENT_BUS.addListener(SfeerRegistry::commandos);
    }

    public static void payloads(PayloadRegistrar registrar) {
        registrar.playToClient(SfeerPayloads.Vlaggen.TYPE, SfeerPayloads.Vlaggen.STREAM_CODEC, (v, c) -> SfeerPayloads.vlaggen.accept(v));
        registrar.playToClient(SfeerPayloads.Forceer.TYPE, SfeerPayloads.Forceer.STREAM_CODEC, (v, c) -> SfeerPayloads.forceer.accept(v));
        registrar.playToClient(SfeerPayloads.DraakPas.TYPE, SfeerPayloads.DraakPas.STREAM_CODEC, (v, c) -> SfeerPayloads.draakPas.accept(v));
    }

    /** Send the Opperzwam flag when it changed (or always). */
    static void vlag(ServerPlayer p, boolean altijd) {
        if (!Vadsrijk.in(p) && !altijd) {
            return;
        }
        boolean k = Spelers.data(p).gezien.contains(OPPERZWAM_KILL);
        Boolean oud = VLAG.put(p.getUUID(), k);
        if (altijd || oud == null || oud != k) {
            PacketDistributor.sendToPlayer(p, new SfeerPayloads.Vlaggen(k));
        }
    }

    /** The pink shower: slimes near players on the Klodderweide hop happily (only the ones not fighting). */
    private static void blijeSlijmpjes(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) {
            Gebieden.Gebied g = Gebieden.op(p.getX(), p.getY(), p.getZ());
            if (g == null || !g.id().equals("klodderweide")) {
                continue;
            }
            for (Slijmwezen s : sl.getEntitiesOfClass(Slijmwezen.class, p.getBoundingBox().inflate(40, 16, 40),
                    s -> s.getTarget() == null && s.onGround())) {
                if (s.getRandom().nextInt(3) == 0) {
                    s.setDeltaMovement(s.getDeltaMovement().x * 0.3, 0.5 + s.getRandom().nextDouble() * 0.15, s.getDeltaMovement().z * 0.3);
                    sl.sendParticles(ParticleTypes.SPLASH, s.getX(), s.getY() + 0.1, s.getZ(), 6, 0.4, 0.05, 0.4, 0.1);
                    if (s.getRandom().nextInt(3) == 0) {
                        sl.sendParticles(ParticleTypes.HEART, s.getX(), s.getY() + s.getBbHeight() + 0.3, s.getZ(), 1, 0.1, 0.1, 0.1, 0);
                        sl.playSound(null, s.getX(), s.getY(), s.getZ(), SoundEvents.SLIME_JUMP_SMALL, SoundSource.HOSTILE, 0.4f, 1.6f);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ dev commands

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    private static int forceer(CommandSourceStack src, String wat) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        PacketDistributor.sendToPlayer(p, new SfeerPayloads.Forceer(wat));
        if (wat.equals("bui")) {
            buiTot = p.level().getGameTime() + SfeerKlok.BUI_DUUR;
        } else if (wat.equals("stop")) {
            buiTot = 0;
        }
        src.sendSuccess(() -> Component.literal("Sfeer: " + wat), false);
        return 1;
    }

    private static void commandos(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> s = Commands.literal("sfeer");
        s.then(dev("draak").executes(c -> draak(c.getSource(), false))
                .then(Commands.literal("stad").executes(c -> draak(c.getSource(), true)))
                .then(Commands.literal("hier").executes(c -> draak(c.getSource(), false))));
        for (String w : new String[]{"golf", "storm", "bui", "noorderlicht", "stop"}) {
            s.then(dev(w).executes(c -> forceer(c.getSource(), w)));
        }
        s.then(dev("zuil").then(Commands.literal("aan").executes(c -> zuil(c.getSource(), true)))
                .then(Commands.literal("uit").executes(c -> zuil(c.getSource(), false))));
        s.then(dev("status").executes(c -> {
            ServerLevel sl = Vadsrijk.level(c.getSource().getServer());
            if (sl == null) {
                return 0;
            }
            long over = Math.max(0, Drakenpas.volgende() - sl.getGameTime()) / 20;
            c.getSource().sendSuccess(() -> Component.literal(String.format("Sfeer: draak over %d s, golf %d, storm %.2f, bui %.2f (start %d), boog %.2f, nacht %.2f",
                    over, SfeerKlok.golfFase(sl), SfeerKlok.storm(sl), SfeerKlok.bui(sl), SfeerKlok.buiStart(sl), SfeerKlok.regenboog(sl),
                    SfeerKlok.nacht(sl))), false);
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(s));
    }

    private static int draak(CommandSourceStack src, boolean stad) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel sl = Vadsrijk.level(src.getServer());
        if (sl == null || !Vadsrijk.in(p)) {
            src.sendFailure(Component.literal("Alleen in het Vadsrijk"));
            return 0;
        }
        boolean ok = Drakenpas.nu(sl, p, stad);
        src.sendSuccess(() -> Component.literal(ok ? "Vahoegnir vliegt over!" : "Geen draak (kon niet)"), false);
        return ok ? 1 : 0;
    }

    private static int zuil(CommandSourceStack src, boolean aan) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (aan) {
            Spelers.data(p).gezien.add(OPPERZWAM_KILL);
        } else {
            Spelers.data(p).gezien.remove(OPPERZWAM_KILL);
        }
        Spelers.opgeslagen(p);
        vlag(p, true);
        return 1;
    }
}
