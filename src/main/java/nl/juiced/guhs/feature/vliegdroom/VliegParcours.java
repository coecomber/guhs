package nl.juiced.guhs.feature.vliegdroom;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * 1.8.0 uitbreiding: the wolkjesringen-parcours of the Vliegdroom. On a Rookguh of the stal you fly through the rings in their
 * order; the clock starts at the first ring and stops at the last ({@link #stap}). A ring counts when you pass within its radius
 * of its middle; skipping one does nothing (the next one keeps waiting, its sparkles show you where). Forty-five seconds without
 * a ring and the round is off.
 * <p>
 * The first whole round: the Vlieg-diploma and droomzand. A new personal record: droomzand once a Minecraft day
 * ({@link Band#dag}). Saved per player in {@link GuhQuests#saved}: {@link #BESTE} (ticks), {@link #DIPLOMA}, {@link #DAG}.
 */
public final class VliegParcours {
    public static final String BESTE = "guhs_vlieg_beste", DIPLOMA = "guhs_vlieg_diploma", DAG = "guhs_vlieg_dag";
    /** Ticks without passing a ring before the round is off. */
    public static final int KWIJT = 45 * 20;
    /** How close to a ring's middle counts as through it (part of its radius). */
    static final double BINNEN = 0.95;

    /** A round in progress: the next ring to fly through, when it began, when the last ring was passed. */
    static final class Ronde {
        int volgende;
        long start, laatste;
    }

    /** What one step of the parcours did. */
    public enum Stap { NIETS, BEGIN, RING, KLAAR, KWIJT }

    private static final Map<UUID, Ronde> RONDES = new ConcurrentHashMap<>();

    private VliegParcours() {
    }

    /** One tick of this flyer at this spot (world rings). Pure bookkeeping: what happened, for {@link #tick} to show. */
    public static Stap stap(UUID wie, Vec3 pos, long nu, List<Vec3> midden, List<Double> straal) {
        Ronde r = RONDES.get(wie);
        if (r != null && nu - r.laatste > KWIJT) {
            RONDES.remove(wie);
            return Stap.KWIJT;
        }
        int i = r == null ? 0 : r.volgende;
        if (i >= midden.size() || pos.distanceTo(midden.get(i)) > straal.get(i) * BINNEN) {
            return Stap.NIETS;
        }
        if (r == null) {
            r = new Ronde();
            r.start = nu;
            RONDES.put(wie, r);
        }
        r.laatste = nu;
        r.volgende = i + 1;
        if (r.volgende >= midden.size()) {
            RONDES.remove(wie);
            laatsteTijd.put(wie, nu - r.start);
            return Stap.KLAAR;
        }
        return i == 0 ? Stap.BEGIN : Stap.RING;
    }

    private static final Map<UUID, Long> laatsteTijd = new ConcurrentHashMap<>();

    /** The time of this flyer's last finished round (ticks). */
    public static long tijd(UUID wie) {
        return laatsteTijd.getOrDefault(wie, 0L);
    }

    /** The next ring this flyer must fly through (0 when no round is going). */
    public static int volgende(UUID wie) {
        Ronde r = RONDES.get(wie);
        return r == null ? 0 : r.volgende;
    }

    /** Off: no round (dismounted, left the dream, the test command). */
    public static void stop(UUID wie) {
        RONDES.remove(wie);
    }

    /** One tick for a player on a stal-Rookguh: the step, its message and sound, the sparkles at the next ring. */
    static void tick(ServerPlayer p, ServerLevel level) {
        var plekken = VliegPlekken.get();
        if (plekken.ringen().isEmpty()) {
            return;
        }
        List<Vec3> midden = plekken.ringen().stream().map(r -> VliegPlekken.vlieg(r.midden())).toList();
        List<Double> straal = plekken.ringen().stream().map(VliegPlekken.Ring::straal).toList();
        long nu = level.getServer().getTickCount();
        Stap s = stap(p.getUUID(), p.position().add(0, 0.9, 0), nu, midden, straal);
        int n = midden.size();
        switch (s) {
            case BEGIN, RING -> {
                p.sendOverlayMessage(Component.translatable("gui.guhs.vliegdroom.ring", volgende(p.getUUID()), n,
                        tekstTijd(nu - RONDES.get(p.getUUID()).start)).withStyle(ChatFormatting.AQUA));
                level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.8f,
                        0.8f + 0.6f * volgende(p.getUUID()) / n);
            }
            case KLAAR -> klaar(p, level, tijd(p.getUUID()));
            case KWIJT -> p.sendOverlayMessage(Component.translatable("gui.guhs.vliegdroom.kwijt").withStyle(ChatFormatting.GRAY));
            default -> {
            }
        }
        if (nu % 5 == 0) {                         // the sparkles of the next ring, only for this flyer
            Vec3 m = midden.get(Math.min(volgende(p.getUUID()), n - 1));
            double r = straal.get(Math.min(volgende(p.getUUID()), n - 1));
            for (int k = 0; k < 10; k++) {
                double a = Math.PI * 2 * k / 10 + nu * 0.05;
                level.sendParticles(p, ParticleTypes.END_ROD, true, false, m.x + Math.cos(a) * r * 0.7, m.y + Math.sin(a) * r * 0.7, m.z,
                        1, 0, 0, 0, 0);
            }
        }
    }

    /** The round is done: the time, a record, the diploma the first time, droomzand for a record once a day. */
    public static Uitkomst klaar(ServerPlayer p, ServerLevel level, long ticks) {
        var saved = GuhQuests.saved(p);
        int beste = saved.getIntOr(BESTE, Integer.MAX_VALUE);
        boolean record = ticks < beste;
        Uitkomst uit = Uitkomst.KLAAR;
        if (record) {
            saved.putInt(BESTE, (int) ticks);
        }
        if (!saved.getBooleanOr(DIPLOMA, false)) {
            saved.putBoolean(DIPLOMA, true);
            geef(p, new ItemStack(VliegdroomFeature.DIPLOMA.get()));
            geef(p, droomzand(5));
            p.sendSystemMessage(Component.translatable("gui.guhs.vliegdroom.diploma", tekstTijd(ticks)).withStyle(ChatFormatting.LIGHT_PURPLE));
            uit = Uitkomst.DIPLOMA;
        } else if (record) {
            long dag = Band.dag(level.getServer());
            if (saved.getLongOr(DAG, Long.MIN_VALUE) != dag) {
                saved.putLong(DAG, dag);
                geef(p, droomzand(2));
                p.sendSystemMessage(Component.translatable("gui.guhs.vliegdroom.record_beloning", tekstTijd(ticks)).withStyle(ChatFormatting.GOLD));
                uit = Uitkomst.RECORD_BELONING;
            } else {
                p.sendSystemMessage(Component.translatable("gui.guhs.vliegdroom.record", tekstTijd(ticks)).withStyle(ChatFormatting.GOLD));
                uit = Uitkomst.RECORD;
            }
        } else {
            p.sendSystemMessage(Component.translatable("gui.guhs.vliegdroom.klaar", tekstTijd(ticks), tekstTijd(beste)).withStyle(ChatFormatting.AQUA));
        }
        level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.7f, 1.3f);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1, p.getZ(), 20, 0.8, 0.6, 0.8, 0.05);
        return uit;
    }

    /** What finishing a round gave. */
    public enum Uitkomst { KLAAR, DIPLOMA, RECORD, RECORD_BELONING }

    /** "12,3 s". */
    public static String tekstTijd(long ticks) {
        return String.format(java.util.Locale.ROOT, "%d,%d s", ticks / 20, (ticks % 20) / 2);
    }

    static ItemStack droomzand(int n) {
        Item zand = BuiltInRegistries.ITEM.getOptional(Identifier.parse("guhs:droomzand")).orElse(null);
        return zand == null ? ItemStack.EMPTY : new ItemStack(zand, n);
    }

    private static void geef(ServerPlayer p, ItemStack stack) {
        if (!stack.isEmpty() && !p.getInventory().add(stack)) {
            p.drop(stack, false);
        }
    }
}
