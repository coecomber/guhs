package nl.juiced.guhs.feature.bouwhulp.gereedschap;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import nl.juiced.guhs.feature.bouwhulp.blauwdruk.Blauwdruk;
import nl.juiced.guhs.feature.bouwhulp.keet.BouwNiveau;
import nl.juiced.guhs.feature.bouwhulp.kern.Ontwerp;

/**
 * The vormstaf's capture mode on the server: the first corner, the frame (two corners or automatic), the frame screen
 * (move each side, a name, save). A frame may be as big as the staf's level allows for blueprints (16, 32 or 64).
 */
public final class VastlegDienst {
    private record Stand(Vastleggen.Doos doos, BouwNiveau niveau) {
    }

    private static final Map<UUID, BlockPos> HOEK = new ConcurrentHashMap<>();
    private static final Map<UUID, Stand> DOOS = new ConcurrentHashMap<>();

    static void klik(ServerPlayer p, BouwNiveau n, BlockPos pos, boolean sluip) {
        UUID u = p.getUUID();
        if (sluip) {
            HOEK.remove(u);
            DOOS.put(u, new Stand(Vastleggen.auto(p.level(), pos, n.maxBlauwdruk), n));
            open(p);
            return;
        }
        BlockPos eerste = HOEK.remove(u);
        if (eerste == null) {
            HOEK.put(u, pos.immutable());
            p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.vastleg.hoek1", pos.getX(), pos.getY(), pos.getZ()));
            p.level().playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.5f, 1.2f);
            return;
        }
        DOOS.put(u, new Stand(Vastleggen.Doos.van(eerste, pos), n));
        open(p);
    }

    /** The frame screen's buttons: 1 move a side (waarde = side * 2 + grow), 2 save (tekst = name), 3 cancel. */
    public static void doe(ServerPlayer p, int actie, int waarde, String tekst) {
        UUID u = p.getUUID();
        Stand s = DOOS.get(u);
        if (s == null) {
            return;
        }
        switch (actie) {
            case 1 -> {
                Vastleggen.Doos d = s.doos().schuif(waarde / 2, waarde % 2 == 1 ? 1 : -1);
                if (d.maat() <= s.niveau().maxBlauwdruk) {
                    DOOS.put(u, new Stand(d, s.niveau()));
                }
                open(p);
            }
            case 2 -> {
                if (s.doos().maat() > s.niveau().maxBlauwdruk) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.vastleg.te_groot", s.niveau().maxBlauwdruk));
                    return;
                }
                Ontwerp o = Vastleggen.vang(p.level(), s.doos());
                if (o.isLeeg()) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.vastleg.leeg"));
                    return;
                }
                String naam = tekst.isBlank() ? Component.translatable("gui.guhs.bouwhulp.vastleg.naamloos").getString() : tekst.strip();
                Blauwdruk b = Blauwdruk.nieuw(naam.length() > 32 ? naam.substring(0, 32) : naam, u, p.getGameProfile().name(), o);
                if (!BouwSpelers.van(p.level().getServer()).bewaar(u, b)) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.bouwhulp.vastleg.vol", BouwSpelers.MAX_EIGEN));
                    return;
                }
                DOOS.remove(u);
                p.level().playSound(null, p.blockPosition(), nl.juiced.guhs.feature.bouwhulp.BouwGeluid.PAPIER.get(), SoundSource.PLAYERS, 0.8f, 1f);   // F12
                p.sendSystemMessage(Component.translatable("gui.guhs.bouwhulp.vastleg.bewaard", naam, o.aantal()));
                GereedschapPayloads.sluitVastleg(p);
            }
            default -> {
                DOOS.remove(u);
                GereedschapPayloads.sluitVastleg(p);
            }
        }
    }

    private static void open(ServerPlayer p) {
        Stand s = DOOS.get(p.getUUID());
        if (s == null) {
            return;
        }
        CompoundTag t = new CompoundTag();
        t.putLong("Min", s.doos().min().asLong());
        t.putLong("Max", s.doos().max().asLong());
        t.putInt("MaxMaat", s.niveau().maxBlauwdruk);
        t.putInt("Eigen", BouwSpelers.van(p.level().getServer()).eigen(p.getUUID()).size());
        t.putInt("MaxEigen", BouwSpelers.MAX_EIGEN);
        GereedschapPayloads.stuur(p, "vastleg", t);
    }

    /** (tests) the frame a player has now. */
    public static Vastleggen.Doos doos(UUID u) {
        Stand s = DOOS.get(u);
        return s == null ? null : s.doos();
    }

    public static void vergeet(UUID u) {
        HOEK.remove(u);
        DOOS.remove(u);
    }

    private VastlegDienst() {
    }
}
