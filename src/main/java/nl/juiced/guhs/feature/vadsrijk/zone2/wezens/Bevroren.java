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
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.loot.Z2Sets;

/**
 * The status "Bevroren" (ronde 15: a status, NOT an element). Kern's {@code Status} enum has no such entry, so this is the slice's
 * adapter (NOTITIES_Z2_WEZENS: kern can lift it into Status/Statussen + the HUD icon):
 * <ul>
 *   <li>slowed: players -30 % Loopsnelheid (kern's EXTRA_STATS hook), mobs -30 % movement speed (an attribute modifier);</li>
 *   <li>a HEAVY hit (one hit worth at least {@link #ZWAAR} of the target's max Vads) does +{@link #EXTRA} damage and breaks the ice
 *       (the status ends with a crack: "IJs gebroken!");</li>
 *   <li>looks: the vanilla frost overlay on players (ticksFrozen kept under the "fully frozen" mark, so no vanilla freeze damage),
 *       snowflakes around the body.</li>
 * </ul>
 * Not saved (short, like kern's statuses); cleared on fainting.
 */
public final class Bevroren {
    /** A heavy hit: at least this part of the target's max Vads in one hit. */
    public static final double ZWAAR = 0.12;
    /** Extra damage of a heavy hit on a frozen target. */
    public static final double EXTRA = 0.30;
    /** Walking speed while frozen (%). */
    public static final double TRAAG = -30;
    private static final net.minecraft.resources.Identifier MOD = Guhs.id("vr_z2_bevroren");
    private static final Map<LivingEntity, Integer> ACTIEF = new WeakHashMap<>();

    private Bevroren() {
    }

    /** Freeze for this many ticks (the longer wins). Kristalset (2) halves it. */
    public static void geef(LivingEntity e, int ticks, @Nullable LivingEntity bron) {
        if (e.level().isClientSide() || !e.isAlive()) {
            return;
        }
        if (e instanceof Player p && Z2Sets.stukken(p, Z2Sets.KRISTAL) >= 2) {
            ticks /= 2;
        }
        Integer al = ACTIEF.get(e);
        if (al == null) {
            if (e.level() instanceof ServerLevel sl) {
                sl.playSound(null, e.blockPosition(), SoundEvents.POWDER_SNOW_STEP, SoundSource.HOSTILE, 1f, 0.7f);
                sl.sendParticles(ParticleTypes.SNOWFLAKE, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 14, 0.35, 0.5, 0.35, 0.02);
            }
            if (e instanceof ServerPlayer sp) {
                Tips.toon(sp, "vadsrijk.z2wezens.tip.bevroren");
            }
            traag(e, true);
        }
        ACTIEF.put(e, Math.max(al == null ? 0 : al, ticks));
    }

    public static boolean heeft(LivingEntity e) {
        return ACTIEF.containsKey(e);
    }

    public static int ticks(LivingEntity e) {
        return ACTIEF.getOrDefault(e, 0);
    }

    public static void wis(LivingEntity e) {
        if (ACTIEF.remove(e) != null) {
            traag(e, false);
        }
    }

    private static void traag(LivingEntity e, boolean aan) {
        if (e instanceof Player) {
            return;   // players: through Spelers.EXTRA_STATS (stats())
        }
        AttributeInstance a = e.getAttribute(Attributes.MOVEMENT_SPEED);
        if (a == null) {
            return;
        }
        a.removeModifier(MOD);
        if (aan) {
            a.addTransientModifier(new AttributeModifier(MOD, TRAAG / 100.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    /** The stat part for kern (Spelers.EXTRA_STATS). */
    public static Stats stats(ServerPlayer p) {
        Stats s = new Stats();
        if (heeft(p)) {
            s.plus(Stat.LOOPSNELHEID, TRAAG);
        }
        return s;
    }

    /** Schade.VOOR_KLAP: heavy hits on a frozen target hit harder and break the ice. */
    public static double klap(Schade.Klap k, double n) {
        LivingEntity doel = k.doel();
        if (n <= 0 || !heeft(doel) || !(doel.level() instanceof ServerLevel sl)) {
            return n;
        }
        double max = doel instanceof Player p ? Spelers.maxVads(p) : doel instanceof VadsrijkMob m ? m.maxVads() : doel.getMaxHealth();
        if (max <= 0 || n < max * ZWAAR) {
            return n;
        }
        wis(doel);
        sl.playSound(null, doel.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1f, 0.8f);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), doel.getX(), doel.getY() + doel.getBbHeight() * 0.5,
                doel.getZ(), 30, 0.4, 0.5, 0.4, 0.15);
        Getallen.tekst(sl, doel.getX(), doel.getY() + doel.getBbHeight() + 0.5, doel.getZ(), Component.translatable("vadsrijk.z2wezens.ijs_gebroken"), 0.9f);
        if (doel instanceof Player p && Z2Sets.stukken(p, Z2Sets.KRISTAL) >= 2) {
            return n;   // Kristalhuid: the ice breaks, but no extra damage
        }
        return n * (1 + EXTRA);
    }

    /** Every server tick. */
    public static void tick() {
        Iterator<Map.Entry<LivingEntity, Integer>> it = ACTIEF.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<LivingEntity, Integer> en = it.next();
            LivingEntity e = en.getKey();
            int t = en.getValue() - 1;
            if (e == null || e.isRemoved() || !e.isAlive() || t <= 0 || (e instanceof VadsrijkMob m && m.geplofd())) {
                if (e != null) {
                    traag(e, false);
                }
                it.remove();
                continue;
            }
            en.setValue(t);
            if (e instanceof Player p) {
                // the frost overlay: below the "fully frozen" mark (140), so vanilla never deals freeze damage
                p.setTicksFrozen(Math.max(p.getTicksFrozen(), Math.min(110, 60 + t)));
            }
            if (t % 10 == 0 && e.level() instanceof ServerLevel sl) {
                sl.sendParticles(ParticleTypes.SNOWFLAKE, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 3, e.getBbWidth() * 0.5, 0.4,
                        e.getBbWidth() * 0.5, 0.0);
            }
        }
    }
}
