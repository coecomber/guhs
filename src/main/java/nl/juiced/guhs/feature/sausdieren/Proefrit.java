package nl.juiced.guhs.feature.sausdieren;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.verhaal.Duwtje;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * The test lap of the Sausloper-stal: the Verzorger-guh whistles a saddled Sausloper for ONE player (a loaner,
 * {@link SausloperEntity#leen}), who rides it through the four gates of the basin in order ({@link Stal.Baan}); the next
 * gate sparkles for that player only. Any number of players ride at the same time, each on their own Sausloper; nothing in
 * the world changes and nothing is taken. Failing does not exist: there is no clock during the questline, a rider who
 * strays far is put back at the start, and the loaner is simply gone when its player is.
 * <p>
 * The first finished lap is step 3 of the questline; later laps are timed (the best time is kept per player).
 */
public final class Proefrit {
    /** A gate counts when the Sausloper is this close to its middle (blocks, horizontally). */
    public static final double POORT_BEREIK = 2.3;
    /** Further than this from the start: back to the start. */
    public static final double TE_VER = 44;
    /** A timed lap under this many ticks earns the advancement "Sausracer". */
    public static final int SNEL = 20 * 30;

    /** One player's lap (not saved: after a restart you just ask again). */
    static final class Rit {
        final ResourceKey<Level> dim;
        final Stal.Baan baan;
        final UUID loper;
        int poort;
        long begin = -1;
        boolean klaar;

        Rit(ResourceKey<Level> dim, Stal.Baan baan, UUID loper) {
            this.dim = dim;
            this.baan = baan;
            this.loper = loper;
        }
    }

    private static final Map<UUID, Rit> RITTEN = new ConcurrentHashMap<>();

    /** Is this player on a lap that is not finished yet? */
    public static boolean bezig(@Nullable UUID speler) {
        Rit r = speler == null ? null : RITTEN.get(speler);
        return r != null && !r.klaar;
    }

    /** The gate this player has to ride through now (0-based; -1: no lap, or it is finished). */
    public static int poort(ServerPlayer p) {
        Rit r = RITTEN.get(p.getUUID());
        return r == null || r.klaar ? -1 : r.poort;
    }

    /** The loaner of this player's lap (null: none). */
    @Nullable
    public static SausloperEntity loper(ServerPlayer p) {
        Rit r = RITTEN.get(p.getUUID());
        return r != null && p.level().getEntity(r.loper) instanceof SausloperEntity loper && loper.isAlive() ? loper : null;
    }

    /** The lap of this player's ride (null: none). */
    @Nullable
    public static Stal.Baan baan(ServerPlayer p) {
        Rit r = RITTEN.get(p.getUUID());
        return r == null ? null : r.baan;
    }

    /** The Verzorger-guh whistles a Sausloper for this player at the start of his lap. False: it could not be made. */
    public static boolean start(ServerPlayer p, GuhNpcEntity npc) {
        ServerLevel level = p.level();
        stop(p);
        Stal.Baan baan = Stal.baan(level, npc);
        SausloperEntity loper = SausdierenFeature.SAUSLOPER.get().create(level, EntitySpawnReason.EVENT);
        if (loper == null) {
            return false;
        }
        loper.snapTo(baan.start().x, baan.start().y, baan.start().z, baan.startYaw(), 0);
        loper.yBodyRot = loper.yHeadRot = baan.startYaw();
        loper.zetLeen(p.getUUID());
        loper.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        loper.setDropChance(EquipmentSlot.SADDLE, 0f);
        loper.setInvulnerable(true);
        level.addFreshEntity(loper);
        level.sendParticles(ParticleTypes.POOF, baan.start().x, baan.start().y + 1, baan.start().z, 12, 0.3, 0.5, 0.3, 0.02);
        loper.playSound(SausdierenFeature.SAUSLOPER_BLIJ.get(), 1.0f, 1.1f);
        RITTEN.put(p.getUUID(), new Rit(level.dimension(), baan, loper.getUUID()));
        return true;
    }

    /** Ends this player's lap (logout, a new lap): the loaner goes when nobody sits on it. */
    public static void stop(ServerPlayer p) {
        Rit r = RITTEN.remove(p.getUUID());
        if (r == null) {
            return;
        }
        for (ServerLevel level : p.level().getServer().getAllLevels()) {
            Entity e = level.getEntity(r.loper);
            if (e instanceof SausloperEntity loper && !loper.isVehicle()) {
                loper.discard();
            }
        }
    }

    /** (called by the loaner when it leaves) */
    static void loperWeg(@Nullable UUID speler) {
        if (speler != null) {
            RITTEN.remove(speler);
        }
    }

    /** Every server tick of a player with a lap. */
    static void tick(ServerPlayer p) {
        Rit r = RITTEN.get(p.getUUID());
        if (r == null) {
            return;
        }
        ServerLevel level = p.level();
        if (level.dimension() != r.dim || !(level.getEntity(r.loper) instanceof SausloperEntity loper) || !loper.isAlive()) {
            RITTEN.remove(p.getUUID());
            return;
        }
        if (r.klaar) {
            return;
        }
        boolean rijdt = p.getVehicle() == loper;
        if (rijdt && r.begin < 0) {
            r.begin = level.getGameTime();
        }
        Vec3 poort = r.baan.poorten().get(r.poort);
        if (p.tickCount % 6 == 0) {
            // the next gate sparkles, for this player only
            level.sendParticles(p, ParticleTypes.END_ROD, true, true, poort.x, poort.y + 1.6, poort.z, 5, 0.9, 0.9, 0.9, 0.01);
            level.sendParticles(p, ParticleTypes.HAPPY_VILLAGER, true, true, poort.x, poort.y + 0.6, poort.z, 3, 1.0, 0.3, 1.0, 0.0);
        }
        if (p.tickCount % 20 == 0) {
            p.sendOverlayMessage(Component.translatable(rijdt ? "gui.guhs.sausdieren.poortje" : "gui.guhs.sausdieren.stap_op", r.poort + 1, r.baan.poorten().size())
                    .withStyle(ChatFormatting.GOLD));
        }
        if (!rijdt) {
            return;
        }
        double dx = loper.getX() - poort.x, dz = loper.getZ() - poort.z;
        if (dx * dx + dz * dz <= POORT_BEREIK * POORT_BEREIK && Math.abs(loper.getY() - poort.y) < 4) {
            r.poort++;
            level.playSound(null, loper.blockPosition(), SausdierenFeature.POORTJE.get(), SoundSource.PLAYERS, 0.9f, 0.8f + 0.15f * r.poort);
            level.sendParticles(p, ParticleTypes.FIREWORK, true, true, poort.x, poort.y + 2.0, poort.z, 12, 0.6, 0.6, 0.6, 0.08);
            if (r.poort >= r.baan.poorten().size()) {
                klaar(p, r, level.getGameTime() - r.begin);
            }
            return;
        }
        if (loper.distanceToSqr(r.baan.start()) > TE_VER * TE_VER) {
            // strayed far: both back to the start, nothing lost
            Duwtje.terug(p, r.dim, r.baan.start().add(0, 0.6, 0), r.baan.startYaw());
            if (p.getVehicle() != loper) {
                loper.snapTo(r.baan.start().x, r.baan.start().y, r.baan.start().z, r.baan.startYaw(), 0);
            }
            r.poort = 0;
            p.sendOverlayMessage(Component.translatable("gui.guhs.sausdieren.te_ver").withStyle(ChatFormatting.GOLD));
        }
    }

    private static void klaar(ServerPlayer p, Rit r, long ticks) {
        r.klaar = true;
        r.poort = r.baan.poorten().size();
        GuhAdvancements.grant(p, "sausdieren_proefrit");
        String tijd = seconden(ticks);
        if (SausdierenFeature.LIJN.verder(p, 3)) {
            p.sendSystemMessage(Component.translatable("gui.guhs.sausdieren.rondje_klaar", tijd).withStyle(ChatFormatting.LIGHT_PURPLE));
            GuhQuests.hint(p, "quest.guhs.sausdieren.hint.terug");
            return;
        }
        int record = SausdierenFeature.LIJN.teller(p, "record");
        if (record <= 0 || ticks < record) {
            SausdierenFeature.LIJN.teller(p, "record", (int) Math.min(Integer.MAX_VALUE, Math.max(1, ticks)));
            p.sendSystemMessage(Component.translatable("gui.guhs.sausdieren.rondje_record", tijd).withStyle(ChatFormatting.GOLD));
        } else {
            p.sendSystemMessage(Component.translatable("gui.guhs.sausdieren.rondje_tijd", tijd, seconden(record)).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (ticks <= SNEL) {
            GidsFeature.grant(p, "barbecuether/sausdieren_snel");
        }
    }

    private static String seconden(long ticks) {
        return String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0);
    }

    private Proefrit() {
    }
}
