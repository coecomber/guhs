package nl.juiced.guhs.feature.vadsrijk.zone3.systemen;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;

/**
 * The Jachtbord next to the Premiebord (ZONE3_PLAN §8.4; the user: named targets only via a contract, never at random). Each day
 * (06:00 reset) 2 of the 8 targets are on the board, the same for everyone. Take a contract ({@link #neem}) and the target
 * appears at its lair for you and your party (z3-wezens {@code Z3Contracten.spawnElite}); defeat it ({@link #verslagen}) for
 * Jachtpenningen, xp, coins and a 3 % chance on a Gouden Veer. No lockout: a contract can be taken again.
 * <p>The spawn goes through {@link #SPAWN}. When z3-wezens' {@code zone3.wezens.Z3Contracten} is on the classpath it is wired
 * by itself ({@link #koppel}: spawnElite + its VERSLAGEN / VERTROKKEN hooks); otherwise the board says the target hides today.
 */
public final class Contracten {
    public static final int MIN_LEVEL = 45, PER_DAG = 2, PENNINGEN = 25;
    public static final double VEERKANS = 0.03;
    /** The 8 targets (z3-wezens Z3Contracten.DOELWITTEN ids). */
    public static final List<String> DOELWITTEN = List.of("ouwe_rotkeiler", "moeder_wesp", "de_harsreus", "kapitein_kaaskop", "grootvader_rups",
            "de_stille_ziener", "slangenkoningin_sapsie", "bramzwijn_de_brede");
    /** (player, target id) -> spawned? Set by {@link #koppel} or the integrator. */
    public static volatile BiFunction<ServerPlayer, String, Boolean> SPAWN = (p, id) -> false;

    private static final Logger LOG = LogUtils.getLogger();

    private Contracten() {
    }

    /** Today's 2 targets (the same for everyone). */
    public static List<String> vandaag() {
        RandomSource r = RandomSource.create(Z3Data.dag().hashCode() * 7919L + 13);
        List<String> pool = new ArrayList<>(DOELWITTEN);
        List<String> l = new ArrayList<>();
        while (l.size() < PER_DAG) {
            l.add(pool.remove(r.nextInt(pool.size())));
        }
        return l;
    }

    public static String actief(ServerPlayer p) {
        return Z3Data.tag(p).getStringOr("contract", "");
    }

    public static String naamKey(String id) {
        return "vadsrijk.z3.systemen.contract." + id;
    }

    /** Take today's contract on a target: it appears at its lair. Returns a lang key. */
    public static String neem(ServerPlayer p, String id) {
        if (Spelers.level(p) < MIN_LEVEL) {
            return "vadsrijk.z3.systemen.contract.te_laag";
        }
        if (!vandaag().contains(id)) {
            return "vadsrijk.z3.systemen.contract.niet_vandaag";
        }
        if (!Boolean.TRUE.equals(SPAWN.apply(p, id))) {
            return "vadsrijk.z3.systemen.contract.verstopt";
        }
        Z3Data.tag(p).putString("contract", id);
        p.level().playSound(null, p.blockPosition(), SoundEvents.VILLAGER_WORK_CARTOGRAPHER, SoundSource.PLAYERS, 0.8f, 1f);
        return "vadsrijk.z3.systemen.contract.genomen";
    }

    /** A target was defeated (for every contract player who helped). */
    public static void verslagen(ServerPlayer p, String id) {
        CompoundTag t = Z3Data.tag(p);
        if (!id.equals(t.getStringOr("contract", ""))) {
            return;
        }
        t.putString("contract", "");
        int lv = Spelers.level(p);
        Spelers.geefXp(p, Z3Xp.contract(lv), "contract");
        Spelers.geefMunten(p, 300 + 10L * lv);
        Z3Data.geefPenningen(p, PENNINGEN);
        p.sendSystemMessage(Component.translatable("vadsrijk.z3.systemen.contract.klaar", Component.translatable(naamKey(id)), PENNINGEN)
                .withStyle(ChatFormatting.GOLD));
        if (p.getRandom().nextDouble() < VEERKANS) {
            GoudenVeren.geef(p, 1);
        }
        Spelers.sync(p);
    }

    /** A target left without being defeated: the contract is free again. */
    public static void vertrokken(String id, List<UUID> spelers, MinecraftServer server) {
        for (UUID u : spelers) {
            ServerPlayer p = server.getPlayerList().getPlayer(u);
            if (p != null && id.equals(actief(p))) {
                Z3Data.tag(p).putString("contract", "");
                p.sendSystemMessage(Component.translatable("vadsrijk.z3.systemen.contract.weg", Component.translatable(naamKey(id))).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    /** Wires z3-wezens' Z3Contracten when it is there (no compile dependency between the slices). */
    @SuppressWarnings("unchecked")
    static void koppel(java.util.function.Supplier<MinecraftServer> server) {
        try {
            Class<?> c = Class.forName("nl.juiced.guhs.feature.vadsrijk.zone3.wezens.Z3Contracten");
            Method spawn = c.getMethod("spawnElite", ServerPlayer.class, String.class);
            SPAWN = (p, id) -> {
                try {
                    return spawn.invoke(null, p, id) != null;
                } catch (ReflectiveOperationException e) {
                    LOG.warn("z3-systemen: Z3Contracten.spawnElite failed", e);
                    return false;
                }
            };
            Field v = c.getField("VERSLAGEN");
            ((List<BiConsumer<ServerPlayer, String>>) v.get(null)).add(Contracten::verslagen);
            Field w = c.getField("VERTROKKEN");
            ((List<BiConsumer<String, List<UUID>>>) w.get(null)).add((id, l) -> {
                MinecraftServer s = server.get();
                if (s != null) {
                    vertrokken(id, l, s);
                }
            });
            LOG.info("z3-systemen: hunting contracts wired to z3-wezens Z3Contracten");
        } catch (ClassNotFoundException e) {
            // z3-wezens not merged yet: the board says the targets hide
        } catch (ReflectiveOperationException | ClassCastException e) {
            LOG.warn("z3-systemen: could not wire Z3Contracten", e);
        }
    }
}
