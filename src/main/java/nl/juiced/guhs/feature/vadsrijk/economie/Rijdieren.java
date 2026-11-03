package nl.juiced.guhs.feature.vadsrijk.economie;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;

/**
 * Mounts with stats (design 9: "rijdieren met stats: eigen snelheid en sprong, en ze kunnen in level stijgen"). Kept in
 * {@code SpelerData.extra("economie")}: {@code rijdieren} = {soort id: {level, xp}}, {@code actief} = the chosen one.
 * <ul>
 *   <li>API for the npc slice: {@link #geefEerste} (the Hoedhupper quest at lv ~8: the mount + a rijfluitje), {@link #geef};</li>
 *   <li>the rijfluitje calls the chosen mount and you sit on it at once ({@link #fluit}); again: it hops home;</li>
 *   <li>it poofs away when you get hit, when you get off, outside the Vadsrijk, and after a restart (never saved in the world);</li>
 *   <li>riding gives the mount xp: one per block ({@link #geefXp}); level 1..{@link RijdierSoort#MAX_LEVEL}.</li>
 * </ul>
 */
public final class Rijdieren {
    /** Server: the mount each player has out right now (entity id). */
    private static final Map<UUID, UUID> UIT = new ConcurrentHashMap<>();

    private Rijdieren() {
    }

    private static CompoundTag tag(ServerPlayer p) {
        CompoundTag e = Spelers.data(p).extra("economie");
        if (!e.contains("rijdieren")) {
            e.put("rijdieren", new CompoundTag());
        }
        return e.getCompoundOrEmpty("rijdieren");
    }

    public static boolean heeft(ServerPlayer p, RijdierSoort s) {
        return tag(p).contains(s.id());
    }

    public static int level(ServerPlayer p, RijdierSoort s) {
        return tag(p).getCompoundOrEmpty(s.id()).getIntOr("level", 1);
    }

    public static long xp(ServerPlayer p, RijdierSoort s) {
        return tag(p).getCompoundOrEmpty(s.id()).getLongOr("xp", 0);
    }

    @Nullable
    public static RijdierSoort actief(ServerPlayer p) {
        RijdierSoort s = RijdierSoort.van(Spelers.data(p).extra("economie").getStringOr("actief", ""));
        if (s != null && heeft(p, s)) {
            return s;
        }
        for (RijdierSoort x : RijdierSoort.values()) {
            if (heeft(p, x)) {
                return x;
            }
        }
        return null;
    }

    public static void kies(ServerPlayer p, RijdierSoort s) {
        if (heeft(p, s)) {
            Spelers.data(p).extra("economie").putString("actief", s.id());
        }
    }

    /** API (npc slice, the lv ~8 quest): the Hoedhupper + a rijfluitje. False when the player already had it. */
    public static boolean geefEerste(ServerPlayer p) {
        boolean nieuw = geef(p, RijdierSoort.HOEDHUPPER);
        if (EcoUtil.tel(p.getInventory(), EcoSpul.RIJFLUITJE.get()) == 0) {
            EcoUtil.geefOfBewaar(p, new ItemStack(EcoSpul.RIJFLUITJE.get()));
        }
        if (nieuw) {
            Tips.toon(p, "vadsrijk.economie.tip.rijdier");
        }
        return nieuw;
    }

    /** Unlocks a mount (level 1). False when already owned. */
    public static boolean geef(ServerPlayer p, RijdierSoort s) {
        CompoundTag t = tag(p);
        if (t.contains(s.id())) {
            return false;
        }
        CompoundTag m = new CompoundTag();
        m.putInt("level", 1);
        m.putLong("xp", 0);
        t.put(s.id(), m);
        if (actief(p) == s || Spelers.data(p).extra("economie").getStringOr("actief", "").isEmpty()) {
            kies(p, s);
        }
        EcoUtil.geluid(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.3f);
        EcoUtil.chat(p, "vadsrijk.economie.rijdier.nieuw", Component.translatable(s.naamKey()).withStyle(ChatFormatting.GOLD));
        return true;
    }

    /** Riding xp; levels up (message + sparkles; title Rijdiermeester at the max). Returns the new level. */
    public static int geefXp(ServerPlayer p, RijdierSoort s, long n) {
        if (!heeft(p, s) || n <= 0) {
            return heeft(p, s) ? level(p, s) : 0;
        }
        CompoundTag m = tag(p).getCompoundOrEmpty(s.id());
        int level = m.getIntOr("level", 1);
        long xp = m.getLongOr("xp", 0) + n;
        boolean op = false;
        while (level < RijdierSoort.MAX_LEVEL && xp >= RijdierSoort.xpNodig(level)) {
            xp -= RijdierSoort.xpNodig(level);
            level++;
            op = true;
        }
        if (level >= RijdierSoort.MAX_LEVEL) {
            xp = 0;
        }
        m.putInt("level", level);
        m.putLong("xp", xp);
        tag(p).put(s.id(), m);
        if (op) {
            EcoUtil.geluid(p, SoundEvents.PLAYER_LEVELUP, 0.6f, 1.5f);
            EcoUtil.zeg(p, "vadsrijk.economie.rijdier.level", Component.translatable(s.naamKey()), level);
            if (p.getVehicle() instanceof RijdierEntity r) {
                r.zetLevel(level);
                p.level().sendParticles(ParticleTypes.HAPPY_VILLAGER, r.getX(), r.getY() + 1, r.getZ(), 20, 0.6, 0.5, 0.6, 0.1);
            }
            if (level >= RijdierSoort.MAX_LEVEL) {
                Cosmetica.geefTitel(p, Cosmetica.RIJDIERMEESTER);
            }
        }
        return level;
    }

    // ---- calling ---------------------------------------------------------------------------------------------------------------

    @Nullable
    static RijdierEntity uit(ServerPlayer p) {
        UUID id = UIT.get(p.getUUID());
        if (id == null) {
            return null;
        }
        Entity e = p.level().getEntity(id);
        return e instanceof RijdierEntity r && r.isAlive() ? r : null;
    }

    /** The rijfluitje. */
    public static void fluit(ServerPlayer p) {
        if (!Vadsrijk.in(p)) {
            EcoUtil.zeg(p, "vadsrijk.economie.rijdier.alleen_rijk");
            return;
        }
        RijdierEntity oud = uit(p);
        if (oud != null) {
            poef(oud);
            return;
        }
        RijdierSoort s = actief(p);
        if (s == null) {
            EcoUtil.zeg(p, "vadsrijk.economie.rijdier.geen");
            return;
        }
        roep(p, s);
    }

    /** Summons the mount and seats the player on it. */
    @Nullable
    public static RijdierEntity roep(ServerPlayer p, RijdierSoort s) {
        ServerLevel sl = p.level();
        RijdierEntity r = EcoSpul.type(s).create(sl, EntitySpawnReason.MOB_SUMMONED);
        if (r == null) {
            return null;
        }
        r.snapTo(p.getX(), p.getY(), p.getZ(), p.getYRot(), 0);
        r.zetEigenaar(p.getUUID(), level(p, s));
        sl.addFreshEntity(r);
        UIT.put(p.getUUID(), r.getUUID());
        p.startRiding(r);
        sl.sendParticles(ParticleTypes.CHERRY_LEAVES, r.getX(), r.getY() + 0.8, r.getZ(), 30, 0.6, 0.6, 0.6, 0.05);
        EcoUtil.geluid(p, SoundEvents.NOTE_BLOCK_FLUTE.value(), 0.9f, 1.6f);
        Tips.toon(p, "vadsrijk.economie.tip.rijden");
        return r;
    }

    /** The mount hops away with a pink poof. */
    public static void poef(RijdierEntity r) {
        if (r.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.CLOUD, r.getX(), r.getY() + 0.6, r.getZ(), 16, 0.5, 0.4, 0.5, 0.03);
            sl.sendParticles(ParticleTypes.CHERRY_LEAVES, r.getX(), r.getY() + 0.8, r.getZ(), 16, 0.5, 0.4, 0.5, 0.03);
            sl.playSound(null, r.blockPosition(), SoundEvents.SLIME_JUMP_SMALL, net.minecraft.sounds.SoundSource.NEUTRAL, 0.8f, 1.5f);
        }
        if (r.eigenaar() != null) {
            UIT.remove(r.eigenaar(), r.getUUID());
        }
        r.ejectPassengers();
        r.discard();
    }

    /** The rider got hit: the mount runs off. */
    static void geraakt(ServerPlayer p) {
        if (p.getVehicle() instanceof RijdierEntity r) {
            poef(r);
            EcoUtil.zeg(p, "vadsrijk.economie.rijdier.geschrokken");
        }
    }

    static void vergeet(ServerPlayer p) {
        RijdierEntity r = uit(p);
        if (r != null) {
            poef(r);
        }
        UIT.remove(p.getUUID());
    }
}
