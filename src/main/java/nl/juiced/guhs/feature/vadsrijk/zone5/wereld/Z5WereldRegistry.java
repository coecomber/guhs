package nl.juiced.guhs.feature.vadsrijk.zone5.wereld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.VadsrijkCommando;

/**
 * Het Vadsrijk zone 5 "Hoogvads", slice T-Z5WERELD: the map (tiles from tools/vadsrijk/zone5 via {@code maak_kaart.py --zone 5},
 * placed by kern's KaartFeature like the other zones: the capital on the back of Moeder Wolkwal, above the east end of zone 4)
 * and its living parts:
 * <ul>
 *   <li>{@link Z5Blokken}: the Hoogvads building blocks (marble, golden cornices, cathedral glass, whale skin, the banner);</li>
 *   <li>areas: the districts ({@link Z5WereldPlekken#WIJKEN}) go into kern {@link Gebieden} with a y-range (zone 4 lies below
 *       the whale in the same x/z), levels 56-60, all town (safe); the biome "Het Vadsrijk - Hoogvads ..." per district
 *       ({@link #biome});</li>
 *   <li>the Wolkvangnet: whoever falls off the whale (a waterfall, a pier end, over a wall) is caught by a soft cloud and set back
 *       on the last safe spot on the whale ({@link #vangnet});</li>
 *   <li>{@link Z5Bewoners}: the townsfolk of the concept renders as passive guh props near players.</li>
 * </ul>
 * Called from {@code wereld.WereldRegistry} (one marked line).
 */
public final class Z5WereldRegistry {
    /** Below this y a player who was on the whale is caught (the belly is at ~134, zone 4's highest point ~110). */
    public static final int VANG_Y = 128;
    /** How long (ticks) after leaving solid ground on the whale a fall still counts as "fell off Hoogvads". */
    public static final int VANG_TIJD = 20 * 40;

    private record Veilig(BlockPos pos, long tijd) {
    }

    private static final Map<UUID, Veilig> LAATST_VEILIG = new HashMap<>();

    private Z5WereldRegistry() {
    }

    public static void register(IEventBus modBus) {
        Z5Blokken.register(modBus);
        nl.juiced.guhs.feature.vadsrijk.zone5.wolkwal.WolkwalRegistry.register();   // T-WOLKWAL: knight model, moving whale, panoramas
        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent e) -> gebieden());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> {
            ServerLevel sl = Vadsrijk.level(e.getServer());
            if (sl == null) {
                return;
            }
            long t = sl.getGameTime();
            for (ServerPlayer p : sl.players()) {
                vangnet(sl, p, t);
            }
            if (t % 20 == 0) {
                Z5Bewoners.tick(sl);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> LAATST_VEILIG.remove(e.getEntity().getUUID()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            LAATST_VEILIG.clear();
            Z5Bewoners.wis();
        });
        NeoForge.EVENT_BUS.addListener(Z5WereldRegistry::commandos);
    }

    // ------------------------------------------------------------------------------------------------ areas + biomes

    /** The districts as kern areas (with a y-range from MIN_Y up), the first district first. */
    static void gebieden() {
        java.util.List<Z5WereldPlekken.Wijk> w = Z5WereldPlekken.WIJKEN;
        for (int i = w.size() - 1; i >= 0; i--) {
            Z5WereldPlekken.Wijk k = w.get(i);
            Gebieden.voegVooraan(gebied(k));
        }
    }

    static Gebieden.Gebied gebied(Z5WereldPlekken.Wijk k) {
        boolean walvis = k.id().equals("z5_moeder_wolkwal");
        return new Gebieden.Gebied(k.id(), k.x1(), k.z1(), k.x2(), k.z2(), Z5WereldPlekken.MIN_LEVEL, Z5WereldPlekken.MAX_LEVEL, !walvis)
                .metY(Z5WereldPlekken.MIN_Y, 330);
    }

    /** The zone-5 biome id at x/y/z ("hoogvads_markt", "moeder_wolkwal", ...), or null outside zone 5 (VadsrijkBiomen asks first). */
    @Nullable
    public static String biome(int x, int y, int z) {
        Z5WereldPlekken.Wijk w = Z5WereldPlekken.wijk(x + 0.5, y, z + 0.5);
        return w == null ? null : w.biome();
    }

    // ------------------------------------------------------------------------------------------------ the Wolkvangnet

    /** Pure rule (tested): should a player at y who last stood safely on the whale at tick 'sinds' be caught now? */
    public static boolean vangen(double x, double y, double z, double vy, long nu, @Nullable Long sinds) {
        if (sinds == null || nu - sinds > VANG_TIJD) {
            return false;
        }
        return y < VANG_Y && vy < -0.2 && x >= Z5WereldPlekken.MIN_X - 40 && x <= Z5WereldPlekken.MAX_X + 40
                && z >= Z5WereldPlekken.MIN_Z - 40 && z <= Z5WereldPlekken.MAX_Z + 40;
    }

    static void vangnet(ServerLevel sl, ServerPlayer p, long t) {
        if (p.isSpectator() || p.isPassenger() || p.getAbilities().flying || p.isFallFlying()) {
            return;
        }
        UUID id = p.getUUID();
        if (p.onGround() && Z5WereldPlekken.inZone5(p.getX(), p.getY(), p.getZ()) && p.getY() > 150) {
            if (t % 10 == 0) {
                LAATST_VEILIG.put(id, new Veilig(p.blockPosition(), t));
            }
            return;
        }
        Veilig v = LAATST_VEILIG.get(id);
        if (v == null || !vangen(p.getX(), p.getY(), p.getZ(), p.getDeltaMovement().y, t, v.tijd())) {
            return;
        }
        BlockPos terug = dichtstbijzijnde(v.pos());
        sl.sendParticles(ParticleTypes.CLOUD, p.getX(), p.getY(), p.getZ(), 40, 1.2, 0.6, 1.2, 0.02);
        p.teleport(new TeleportTransition(sl, Vec3.atBottomCenterOf(terug), Vec3.ZERO, p.getYRot(), 0f, TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        p.setDeltaMovement(Vec3.ZERO);
        p.hurtMarked = true;
        sl.sendParticles(ParticleTypes.CLOUD, terug.getX() + 0.5, terug.getY() + 0.5, terug.getZ() + 0.5, 30, 0.8, 0.4, 0.8, 0.02);
        sl.playSound(null, terug, SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1f, 0.7f);
        p.sendOverlayMessage(Component.translatable("vadsrijk.z5wereld.vangnet").withStyle(ChatFormatting.LIGHT_PURPLE));
        LAATST_VEILIG.put(id, new Veilig(terug, t));
    }

    /** Where the cloud sets you down: your last safe spot, unless one of the town squares is close by (always solid ground). */
    static BlockPos dichtstbijzijnde(BlockPos laatst) {
        BlockPos best = laatst;
        double bd = 24 * 24;
        for (BlockPos b : Z5WereldPlekken.VANGNET) {
            double d = b.distSqr(laatst);
            if (d < bd) {
                bd = d;
                best = b;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------------------------------------ dev commands

    private static LiteralArgumentBuilder<CommandSourceStack> dev(String naam) {
        return Commands.literal(naam).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));
    }

    static int naar(CommandSourceStack src, @Nullable BlockPos b) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel sl = Vadsrijk.level(p.level().getServer());
        if (b == null || sl == null) {
            src.sendFailure(Component.literal("Onbekende plek (zie POI_Z5.md)"));
            return 0;
        }
        if (!Vadsrijk.in(p)) {
            VadsrijkCommando.naarBinnen(p, false);
        }
        p.teleport(new TeleportTransition(sl, Vec3.atBottomCenterOf(b), Vec3.ZERO, p.getYRot(), 0f, TeleportTransition.DO_NOTHING));
        return 1;
    }

    private static void commandos(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> w = Commands.literal("z5wereld");
        w.then(dev("plek").then(Commands.argument("id", StringArgumentType.string()).suggests((c, b) -> {
            Z5WereldPlekken.ALLE.keySet().forEach(b::suggest);
            return b.buildFuture();
        }).executes(c -> naar(c.getSource(), Z5WereldPlekken.plek(StringArgumentType.getString(c, "id"))))));
        w.then(dev("huis").then(Commands.argument("id", StringArgumentType.string()).suggests((c, b) -> {
            Z5WereldPlekken.HUIZEN.forEach(h -> b.suggest(h.id()));
            return b.buildFuture();
        }).executes(c -> {
            String id = StringArgumentType.getString(c, "id");
            for (Z5WereldPlekken.Huis h : Z5WereldPlekken.HUIZEN) {
                if (h.id().equals(id)) {
                    return naar(c.getSource(), h.binnen());
                }
            }
            return naar(c.getSource(), null);
        })));
        w.then(dev("wijk").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            Z5WereldPlekken.Wijk k = Z5WereldPlekken.wijk(p.getX(), p.getY(), p.getZ());
            Gebieden.Gebied g = Gebieden.op(p.getX(), p.getY(), p.getZ());
            c.getSource().sendSuccess(() -> Component.literal("wijk " + (k == null ? "-" : k.id() + " (biome vadsrijk_" + k.biome() + ")")
                    + ", kern-gebied " + (g == null ? "-" : g.id())), false);
            return 1;
        }));
        w.then(dev("vangnet").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            BlockPos b = Z5WereldPlekken.plek("haven_z5_z4_vertrek");
            if (b == null || naar(c.getSource(), b) == 0) {
                return 0;
            }
            // stand at the open pier end, then step off: the cloud should catch you
            LAATST_VEILIG.put(p.getUUID(), new Veilig(b, p.level().getGameTime()));
            p.teleport(new TeleportTransition((ServerLevel) p.level(), new Vec3(b.getX() + 0.5, b.getY() + 3, b.getZ() + 14.5), Vec3.ZERO,
                    p.getYRot(), 0f, TeleportTransition.DO_NOTHING));
            c.getSource().sendSuccess(() -> Component.literal("Je valt van de pier: een wolkje hoort je op te vangen."), false);
            return 1;
        }));
        w.then(dev("bewoners").then(Commands.literal("aan").executes(c -> {
            Z5Bewoners.aan = true;
            return 1;
        })).then(Commands.literal("uit").executes(c -> {
            Z5Bewoners.aan = false;
            Z5Bewoners.wis();
            return 1;
        })).executes(c -> {
            c.getSource().sendSuccess(() -> Component.literal("bewoners: " + Z5Bewoners.aantal() + " in de wereld, " + Z5WereldPlekken.BEWONERS.size()
                    + " plekken, " + (Z5Bewoners.aan ? "aan" : "uit")), false);
            return 1;
        }));
        event.getDispatcher().register(VadsrijkCommando.wortel(w));
    }
}
