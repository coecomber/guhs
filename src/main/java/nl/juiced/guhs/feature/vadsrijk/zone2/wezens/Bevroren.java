package nl.juiced.guhs.feature.vadsrijk.zone2.wezens;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.kern.Getallen;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Stat;
import nl.juiced.guhs.feature.vadsrijk.kern.Stats;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.Status;
import nl.juiced.guhs.feature.vadsrijk.kern.Statussen;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.loot.Z2Sets;

/**
 * The status "Bevroren" (ronde 15: a status, NOT an element). Integration: ONE implementation = kern {@code Status.BEVROREN} with
 * its effects in {@code zone2.systemen.Bevroren} (slowed, heavy hits +30 %, the chocomelk immunity, kern HUD line). This class is
 * the wezens facade: it keeps the wezens call sites, adds the Kristalset (2) halving, the first-time tip, the vanilla frost
 * overlay on players and the snowflakes.
 */
public final class Bevroren {
    /** Extra damage of a heavy hit on a frozen target (systemen's value). */
    public static final double EXTRA = nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Bevroren.ZWAAR_EXTRA;
    private static final Map<LivingEntity, Boolean> GEZIEN = new WeakHashMap<>();

    private Bevroren() {
    }

    /** Freeze for this many ticks. Kristalset (2) halves it. */
    public static void geef(LivingEntity e, int ticks, @Nullable LivingEntity bron) {
        if (e.level().isClientSide() || !e.isAlive()) {
            return;
        }
        if (e instanceof Player p && Z2Sets.stukken(p, Z2Sets.KRISTAL) >= 2) {
            ticks /= 2;
        }
        nl.juiced.guhs.feature.vadsrijk.zone2.systemen.Bevroren.geef(e, ticks, bron);
        if (heeft(e)) {
            GEZIEN.put(e, true);
            if (e instanceof ServerPlayer sp) {
                Tips.toon(sp, "vadsrijk.z2wezens.tip.bevroren");
            }
        }
    }

    public static boolean heeft(LivingEntity e) {
        return Statussen.heeft(e, Status.BEVROREN);
    }

    public static int ticks(LivingEntity e) {
        return Statussen.ticks(e, Status.BEVROREN);
    }

    public static void wis(LivingEntity e) {
        Statussen.wis(e, Status.BEVROREN);
        GEZIEN.remove(e);
    }

    /** (kept for the call sites) The slow-down is systemen's EXTRA_STATS hook: nothing extra here. */
    public static Stats stats(ServerPlayer p) {
        return new Stats();
    }

    /** (kept for the call sites) Heavy hits are systemen's VOOR_KLAP hook: unchanged here. */
    public static double klap(Schade.Klap k, double n) {
        return n;
    }

    /** Every server tick: the looks (frost overlay below the vanilla freeze-damage mark, snowflakes). */
    public static void tick() {
        Iterator<Map.Entry<LivingEntity, Boolean>> it = GEZIEN.entrySet().iterator();
        while (it.hasNext()) {
            LivingEntity e = it.next().getKey();
            int t = e == null ? 0 : ticks(e);
            if (e == null || e.isRemoved() || !e.isAlive() || t <= 0) {
                it.remove();
                continue;
            }
            if (e instanceof Player p) {
                p.setTicksFrozen(Math.max(p.getTicksFrozen(), Math.min(110, 60 + t)));
            }
            if (t % 10 == 0 && e.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 3, e.getBbWidth() * 0.5, 0.4,
                        e.getBbWidth() * 0.5, 0.0);
            }
        }
    }
}
