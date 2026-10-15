package nl.juiced.guhs.feature.einde2.h3aarde;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.network.ModNetworking;

/**
 * The games of Pai Sho that are not on a table: the story game with Oom Miroh (step {@link E2H3Aarde#PAI_SHO}), one per
 * player, in memory. You begin ('a'), Miroh answers after a moment of thought, and he plays "for the gezelligheid" (now
 * and then just a nice square). Win, lose or draw, afterwards he talks about hope ({@link H3Rollen#paiShoKlaar}) and gives
 * you the Pai Sho table; the step moves on. A table's game is {@link PaiShoTafelBlockEntity}; the clicks of both come here.
 */
public final class PaiShoSpel {
    /** How long Miroh thinks (ticks), and how often he plays for fun. */
    public static final int DENK = 16;
    public static final double MIROH_KANS = 0.3;

    private record Spel(String bord, long denkTot, long klaarOp) {
    }

    private static final Map<UUID, Spel> MIROH = new ConcurrentHashMap<>();
    private static final Random R = new Random();

    private PaiShoSpel() {
    }

    /** Starts (or shows again) the game with Miroh. */
    static void metMiroh(ServerPlayer p) {
        Spel s = MIROH.computeIfAbsent(p.getUUID(), k -> new Spel(PaiSho.NIEUW, 0, 0));
        stuur(p, s);
        E2Audio.stinger(p, "deuntje.pai_sho");
    }

    private static void stuur(ServerPlayer p, Spel s) {
        char w = PaiSho.winnaar(s.bord());
        String uitslag = w == PaiSho.A ? "gui.guhs.einde2_h3_aarde.pai_sho.gewonnen" : w == PaiSho.B ? "gui.guhs.einde2_h3_aarde.pai_sho.miroh_wint"
                : PaiSho.vol(s.bord()) ? "gui.guhs.einde2_h3_aarde.pai_sho.gelijk" : "";
        ModNetworking.sendTo(p, new H3Payloads.Stand(H3Payloads.MIROH, BlockPos.ZERO, s.bord(), p.getName().getString(),
                "#entity.guhs.guh_npc.oom_miroh", 1, PaiSho.klaar(s.bord()) ? H3Payloads.KLAAR : H3Payloads.BEZIG, uitslag));
    }

    /** A click from a screen: a tile, close, or a new game. */
    static void zet(ServerPlayer p, String spel, BlockPos tafel, int cel) {
        if (H3Payloads.TAFEL.equals(spel)) {
            if (p.level().getBlockEntity(tafel) instanceof PaiShoTafelBlockEntity be && tafel.closerToCenterThan(p.position(), 8)) {
                be.klik(p, cel);
            }
            return;
        }
        Spel s = MIROH.get(p.getUUID());
        if (s == null || cel < 0 || s.denkTot() > 0) {
            return;
        }
        String na = PaiSho.zet(s.bord(), cel, PaiSho.A);
        if (na == null) {
            return;
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.DECORATED_POT_INSERT, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.3f);
        long nu = p.level().getGameTime();
        Spel nieuw = PaiSho.klaar(na) ? new Spel(na, 0, nu + 50) : new Spel(na, nu + DENK, 0);
        MIROH.put(p.getUUID(), nieuw);
        stuur(p, nieuw);
    }

    /** Miroh's moves after his moment of thought; the end of a game. */
    static void opTick(ServerTickEvent.Post event) {
        if (MIROH.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, Spel> e : MIROH.entrySet()) {
            ServerPlayer p = event.getServer().getPlayerList().getPlayer(e.getKey());
            if (p == null) {
                MIROH.remove(e.getKey());
                continue;
            }
            Spel s = e.getValue();
            long nu = p.level().getGameTime();
            if (s.denkTot() > 0 && nu >= s.denkTot()) {
                int c = PaiSho.guhZet(s.bord(), PaiSho.B, R, MIROH_KANS);
                String na = c < 0 ? s.bord() : PaiSho.zet(s.bord(), c, PaiSho.B);
                if (na == null) {
                    na = s.bord();
                }
                Spel nieuw = new Spel(na, 0, PaiSho.klaar(na) ? nu + 50 : 0);
                MIROH.put(e.getKey(), nieuw);
                p.level().playSound(null, p.blockPosition(), SoundEvents.DECORATED_POT_INSERT, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.0f);
                stuur(p, nieuw);
            } else if (s.klaarOp() > 0 && nu >= s.klaarOp()) {
                MIROH.remove(e.getKey());
                H3Rollen.paiShoKlaar(p, PaiSho.winnaar(s.bord()));
            }
        }
    }

    /** (dev, tests) forgets this player's game with Miroh. */
    static void vergeet(ServerPlayer p) {
        MIROH.remove(p.getUUID());
    }

    /** The prize for the story game: Miroh's own table goes with you. */
    static void prijs(ServerPlayer p) {
        Minigames.give(p, new ItemStack(E2H3AardeFeature.PAI_SHO_TAFEL_ITEM.get()));
    }

    /** Miroh behind his counter in this copy (for a word in the chat), or null. */
    static Entity miroh(ServerPlayer p) {
        E2H3Aarde.Stad stad = E2H3Aarde.Stad.van(p.level());
        return stad == null ? null : H3Thee.vind(stad, GuhNpcEntity.Kind.OOM_MIROH, "miroh");
    }
}
