package nl.juiced.guhs.feature.guhwinter.spellen;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Minigames;

/**
 * Het schaatsrondje on the frozen pond of the winter plaza: right-click the little start sign, count down 3-2-1, then skate
 * through the poortjes in order (plein's anchors {@code vijver_route_0..n}; a ring round the pond while those don't exist)
 * and back through the first one: that is the finish. You get your time, a place on the board by the pond, and pepernoten
 * for your first lap ({@value #EERSTE_LAP}) and for every new personal best after that ({@value #NIEUW_BESTE}).
 * <p>
 * Skates: the elftocht-style winter skates ({@link WinterSchaatsenItem}: faster on ice); who has none borrows a pair for
 * the lap (they go back when it's over). The next poortje sparkles for you only (particles sent to you alone).
 */
public final class Schaatsrondje {
    public static final String MINIGAME = "guhwinter_schaats";
    public static final String VLAG_EERSTE = "schaats_eerste";
    public static final int EERSTE_LAP = 15, NIEUW_BESTE = 5;
    static final int AFTEL = 60, MAX_TICKS = 20 * 60 * 3;
    /** How close (horizontally) to a poortje counts as through it, and how far from the pond you may wander. */
    static final double POORT = 2.6, WEG = 34;

    private static final class Rondje {
        final List<Vec3> poorten;
        final Vec3 midden;
        int volgende = 1;       // gate 0 is the start (and the finish at the end)
        int ticks = -AFTEL;
        boolean geleend;

        Rondje(List<Vec3> poorten, Vec3 midden) {
            this.poorten = poorten;
            this.midden = midden;
        }
    }

    private static final Map<UUID, Rondje> RONDJES = new ConcurrentHashMap<>();

    private Schaatsrondje() {
    }

    static void register() {
        Minigames.registerGame(MINIGAME, p -> RONDJES.containsKey(p.getUUID()));
    }

    public static boolean bezig(ServerPlayer p) {
        return RONDJES.containsKey(p.getUUID());
    }

    /** The route of the lap: the anchors (or the ring round the pond). */
    public static List<Vec3> route(MinecraftServer server) {
        return SpelBrug.ankers(server, "vijver_route").stream().map(SpelBrug.Anker::pos).toList();
    }

    public static void start(ServerPlayer p, BlockPos bord) {
        MinecraftServer server = p.level().getServer();
        if (!SpelBrug.open(server)) {
            p.sendSystemMessage(Component.translatable("gui.guhs.guhwinter_spellen.voorbij").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (bezig(p)) {
            stop(p, "gui.guhs.guhwinter_spellen.schaats.gestopt");
            return;
        }
        if (Minigames.busyElsewhere(p, MINIGAME)) {
            p.sendSystemMessage(Component.translatable("gui.guhs.guhwinter_spellen.bezig").withStyle(ChatFormatting.GRAY));
            return;
        }
        List<Vec3> poorten = route(server);
        if (poorten.size() < 2) {
            return;
        }
        SpelBrug.Anker m = SpelBrug.anker(server, "vijver_midden");
        Vec3 midden = m != null ? m.pos() : poorten.stream().reduce(Vec3.ZERO, Vec3::add).scale(1.0 / poorten.size());
        Rondje r = new Rondje(poorten, midden);
        if (!WinterSchaatsenItem.heeft(p)) {
            ItemStack leen = WinterSchaatsenItem.leen();
            if (!p.getInventory().add(leen)) {
                p.sendSystemMessage(Component.translatable("gui.guhs.guhwinter_spellen.schaats.vol").withStyle(ChatFormatting.GRAY));
                return;
            }
            r.geleend = true;
            p.sendSystemMessage(Component.translatable("gui.guhs.guhwinter_spellen.schaats.leen").withStyle(ChatFormatting.AQUA));
        }
        RONDJES.put(p.getUUID(), r);
        Minigames.startKeeping(p);
        p.sendSystemMessage(Component.translatable("gui.guhs.guhwinter_spellen.schaats.uitleg", poorten.size()).withStyle(ChatFormatting.AQUA));
    }

    /** Stops a lap without a time (left, logged out, the sign again); the borrowed skates go back. */
    public static void stop(ServerPlayer p, String reden) {
        Rondje r = RONDJES.remove(p.getUUID());
        if (r == null) {
            return;
        }
        Minigames.forget(p);
        if (r.geleend) {
            WinterSchaatsenItem.neemLeenTerug(p);
        }
        if (reden != null) {
            p.sendSystemMessage(Component.translatable(reden).withStyle(ChatFormatting.GRAY));
        }
    }

    static void vergeetAlles() {
        RONDJES.clear();
    }

    private static void titel(ServerPlayer p, Component kop, Component onder) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(0, 20, 6));
        p.connection.send(new ClientboundSetSubtitleTextPacket(onder));
        p.connection.send(new ClientboundSetTitleTextPacket(kop));
    }

    /** Every server tick, for every player on a lap. */
    static void tick(MinecraftServer server) {
        for (var e : RONDJES.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p == null) {
                RONDJES.remove(e.getKey());
                continue;
            }
            Rondje r = e.getValue();
            Minigames.keep(p);
            r.ticks++;
            if (r.ticks < 0) {
                if (r.ticks % 20 == 0) {
                    int tel = -r.ticks / 20;
                    titel(p, Component.literal(Integer.toString(tel)).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), Component.empty());
                    p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8f, 1.0f);
                }
                continue;
            }
            if (r.ticks == 0) {
                titel(p, Component.translatable("gui.guhs.guhwinter_spellen.schaats.go").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), Component.empty());
                p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1f, 2.0f);
            }
            if (p.position().distanceTo(r.midden) > WEG || p.level() != SpelBrug.plein(server) && SpelBrug.heeftAnkers(server)) {
                stop(p, "gui.guhs.guhwinter_spellen.schaats.te_ver");
                continue;
            }
            if (r.ticks > MAX_TICKS) {
                stop(p, "gui.guhs.guhwinter_spellen.schaats.te_lang");
                continue;
            }
            int doel = r.volgende % r.poorten.size();
            Vec3 poort = r.poorten.get(doel);
            if (r.ticks % 4 == 0) {
                vonk(p, poort, true);
                vonk(p, r.poorten.get((doel + 1) % r.poorten.size()), false);
            }
            double dx = p.getX() - poort.x, dz = p.getZ() - poort.z;
            if (dx * dx + dz * dz <= POORT * POORT && Math.abs(p.getY() - poort.y) < 4) {
                if (r.volgende >= r.poorten.size()) {
                    finish(p, r);
                    continue;
                }
                r.volgende++;
                p.level().playSound(null, p.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.6f, 1.2f + 0.05f * r.volgende);
            }
            p.sendOverlayMessage(Component.translatable("gui.guhs.guhwinter_spellen.schaats.stand", Math.min(r.volgende, r.poorten.size()), r.poorten.size(),
                    HighscorebordBlock.tijd(r.ticks)).withStyle(ChatFormatting.AQUA));
        }
    }

    /** A little arch of sparkles at a poortje, seen by this player only (the next one bright, the one after faint). */
    private static void vonk(ServerPlayer p, Vec3 poort, boolean helder) {
        ServerLevel level = p.level();
        Vec3 naar = p.position().subtract(poort).multiply(1, 0, 1);
        Vec3 dwars = naar.lengthSqr() < 1e-4 ? new Vec3(1, 0, 0) : new Vec3(-naar.z, 0, naar.x).normalize();
        int n = helder ? 9 : 5;
        for (int i = 0; i <= n; i++) {
            double t = Math.PI * i / n;
            Vec3 at = poort.add(dwars.scale(Math.cos(t) * 1.6)).add(0, 0.2 + Math.sin(t) * 2.2, 0);
            if (helder) {
                level.sendParticles(p, ParticleTypes.END_ROD, true, false, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            } else {
                level.sendParticles(p, new DustParticleOptions(0xA0D8FF, 0.8f), true, false, at.x, at.y, at.z, 1, 0, 0, 0, 0);
            }
        }
    }

    private static void finish(ServerPlayer p, Rondje r) {
        int ticks = r.ticks;
        stop(p, null);
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.3f);
        titel(p, Component.translatable("gui.guhs.guhwinter_spellen.schaats.finish").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.literal(HighscorebordBlock.tijd(ticks)).withStyle(ChatFormatting.AQUA));
        verwerk(p, ticks);
    }

    /** A finished lap (also the dev command and the test): the board, your best, the pepernoten. Returns the pepernoten given. */
    public static int verwerk(ServerPlayer p, int ticks) {
        SpellenData data = SpellenData.get(p.level().getServer());
        SpellenData.Inzending in = data.zet(p, SpellenData.SCHAATS, ticks);
        int noten = 0;
        if (!data.vlag(p.getUUID(), VLAG_EERSTE)) {
            data.zetVlag(p.getUUID(), VLAG_EERSTE, true);
            noten += EERSTE_LAP;
        } else if (in.nieuwBeste()) {
            noten += NIEUW_BESTE;
        }
        p.sendSystemMessage(Component.translatable(in.nieuwBeste() ? "gui.guhs.guhwinter_spellen.schaats.nieuw_beste"
                : "gui.guhs.guhwinter_spellen.schaats.tijd", HighscorebordBlock.tijd(ticks)).withStyle(ChatFormatting.AQUA));
        if (noten > 0 && SpelBrug.geef(p, noten)) {
            p.sendSystemMessage(Component.translatable("gui.guhs.guhwinter_spellen.schaats.noten", noten).withStyle(ChatFormatting.YELLOW));
        }
        if (in.plaats() > 0) {
            p.sendSystemMessage(Component.translatable("gui.guhs.guhwinter_spellen.race.plaats", in.plaats()).withStyle(ChatFormatting.GOLD));
        }
        HighscorebordBlock.verversAlles(p.level().getServer());
        return noten;
    }

    /** (Unused helper for tests) does the inventory hold borrowed skates? */
    static boolean heeftLeen(Inventory inv) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (WinterSchaatsenItem.isLeen(inv.getItem(i))) {
                return true;
            }
        }
        return false;
    }
}
