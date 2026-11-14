package nl.juiced.guhs.feature.vadsrijk.zone3.bazen;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.IsoFields;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;

/**
 * The Vahoeg mode of the two dungeons (ZONE3_PLAN 8.5, made LIGHT by the user: no lockouts, no leaderboards, no time board).
 * Put a Vahoegzegel on the Vahoeg altar at the dungeon door: for 30 minutes (or until the last boss falls) every boss of that
 * dungeon that spawns, or still stands untouched, is in its Vahoeg form (lv 52, x1.5 HP, x1.25 damage, one extra mechanic each,
 * and the week seal). Vahoeg bosses drop Gouden Veren (25 = the gold Pluimguh, z3-systemen's GoudenVeren), the last boss of a
 * run has 2 % for the gold Pluimguh itself. Everything else (Normal) stays exactly as it is.
 */
public final class VahoegModus {
    /** The week seal: changes every Monday (ISO week). */
    public enum Zegel {
        DONKER, HAASTIG, SPLITSEND, TAAI;

        public String key() {
            return "vadsrijk.z3bazen.zegel." + name().toLowerCase(Locale.ROOT);
        }
    }

    public static final int DUUR = 20 * 60 * 30;
    /** Gametests: force a seal (null = by the week). */
    @Nullable
    public static Zegel testZegel;
    private static final Map<String, Long> ACTIEF = new HashMap<>();
    /** 1.10.1: per dungeon the seal holder + his party at the start: only for them the bosses wear the Vahoeg form. */
    private static final Map<String, java.util.Set<java.util.UUID>> GROEP = new HashMap<>();

    private VahoegModus() {
    }

    public static Zegel zegel() {
        if (testZegel != null) {
            return testZegel;
        }
        int week = LocalDate.now(ZoneId.of("Europe/Amsterdam")).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return Zegel.values()[Math.floorMod(week, Zegel.values().length)];
    }

    public static boolean actief(ServerLevel sl, String kerker) {
        Long tot = ACTIEF.get(kerker);
        return tot != null && sl.getGameTime() < tot;
    }

    /** The arenas of a dungeon. */
    public static String[] arenas(String kerker) {
        return "werf".equals(kerker) ? new String[] {Z3BazenRegistry.ARENA_TALEGGIO, Z3BazenRegistry.ARENA_BROKKEL, Z3BazenRegistry.ARENA_ROQUEFORT}
                : new String[] {Z3BazenRegistry.ARENA_WURGER, Z3BazenRegistry.ARENA_WACHTER, Z3BazenRegistry.ARENA_BLEU};
    }

    /** Starts the mode (the altar). Bosses that stand untouched switch at once; the others when they respawn. */
    public static void start(ServerLevel sl, String kerker, @Nullable ServerPlayer wie, Vec3 altaar) {
        ACTIEF.put(kerker, sl.getGameTime() + DUUR);
        java.util.Set<java.util.UUID> groep = new java.util.HashSet<>();
        if (wie != null) {
            groep.add(wie.getUUID());
            nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen.leden(wie).forEach(q -> groep.add(q.getUUID()));
        }
        GROEP.put(kerker, groep);
        for (String id : arenas(kerker)) {
            VadsrijkBaas b = Arenas.baas(sl, id);
            if (b instanceof Z3Baas z && !z.vahoeg() && z.vads() >= z.maxVads() - 0.5 && z.strijders().isEmpty()) {
                z.zetVahoeg(true);
            }
        }
        sl.sendParticles(new DustParticleOptions(0xFF5FC8, 2.4f), altaar.x, altaar.y + 1.2, altaar.z, 80, 0.8, 1.2, 0.8, 0);
        sl.sendParticles(ParticleTypes.END_ROD, altaar.x, altaar.y + 1.5, altaar.z, 40, 0.4, 1.5, 0.4, 0.05);
        sl.playSound(null, altaar.x, altaar.y, altaar.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.5f, 1.2f);
        Component z = Component.translatable(zegel().key()).withStyle(ChatFormatting.GOLD);
        for (ServerPlayer p : sl.players()) {
            if (p.position().distanceTo(altaar) < 64) {
                p.sendSystemMessage(Component.translatable("vadsrijk.z3bazen.vahoeg.aan." + kerker, z).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
    }

    /**
     * 1.10.1: the mode was global: one level-50 seal made every boss of the dungeon lv 52 for everyone for 30 minutes, also for
     * story players (z3_hq08 / z3_hq13, lv 38-50). Now, every second, an untouched boss (full Vads, nobody fighting) wears the
     * Vahoeg form only while someone of the seal holder's group stands in its arena; with only others inside it is normal.
     * A fight in progress never changes.
     */
    public static void kijk(net.minecraft.server.MinecraftServer server) {
        ServerLevel sl = nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk.level(server);
        if (sl == null || sl.getGameTime() % 20 != 3 || ACTIEF.isEmpty()) {
            return;
        }
        for (String kerker : new java.util.ArrayList<>(ACTIEF.keySet())) {
            if (!actief(sl, kerker)) {
                continue;
            }
            java.util.Set<java.util.UUID> groep = GROEP.getOrDefault(kerker, java.util.Set.of());
            for (String id : arenas(kerker)) {
                VadsrijkBaas b = Arenas.baas(sl, id);
                if (!(b instanceof Z3Baas z) || z.vads() < z.maxVads() - 0.5 || !z.strijders().isEmpty()) {
                    continue;
                }
                var a = Arenas.van(id);
                if (a == null) {
                    continue;
                }
                var binnen = Arenas.spelersBinnen(sl, a);
                if (binnen.isEmpty()) {
                    continue;
                }
                z.zetVahoeg(binnen.stream().anyMatch(p -> groep.contains(p.getUUID())));
            }
        }
    }

    /** 1.10.1 tests: is this player in the Vahoeg group of this dungeon? */
    public static boolean inGroep(String kerker, ServerPlayer p) {
        return GROEP.getOrDefault(kerker, java.util.Set.of()).contains(p.getUUID());
    }

    /** The last boss of a Vahoeg run fell: the mode ends (a fresh seal starts the next run). */
    public static void runKlaar(ServerLevel sl, String kerker) {
        ACTIEF.remove(kerker);
        GROEP.remove(kerker);
    }

    /** A boss of this dungeon was just made by its arena: Vahoeg when the mode is on. */
    public static <T extends Z3Baas> T pas(ServerLevel sl, T b) {
        if (b != null && b.kerker() != null && actief(sl, b.kerker())) {
            b.zetVahoeg(true);
        }
        return b;
    }

    /** The altar: with a Vahoegzegel in hand, start; else explain. */
    public static boolean altaar(ServerLevel sl, ServerPlayer p, String kerker, Vec3 plek) {
        if (!nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Vahoeg.heeftZegel(p)) {
            long rest = actief(sl, kerker) ? (ACTIEF.get(kerker) - sl.getGameTime()) / 20 : 0;
            p.sendSystemMessage(Component.translatable(rest > 0 ? "vadsrijk.z3bazen.vahoeg.loopt" : "vadsrijk.z3bazen.vahoeg.uitleg",
                    Component.translatable(zegel().key()).withStyle(ChatFormatting.GOLD), rest / 60).withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        if (!p.isCreative() && !nl.juiced.guhs.feature.vadsrijk.zone3.systemen.Vahoeg.neemZegel(p)) {
            return false;   // (under lv 50: z3-systemen says why)
        }
        start(sl, kerker, p, plek);
        return true;
    }
}
