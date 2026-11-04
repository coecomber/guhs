package nl.juiced.guhs.feature.vadsrijk.kern;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Klasse;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.SkillContext;
import nl.juiced.guhs.feature.vadsrijk.kern.klasse.Vaardigheid;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/** Kern's side of the skills: the key press -> checks (dimension, stun, cooldown, energy/ult charge) -> the skill -> pay. */
public final class Vaardigheden {
    /** Per-player cooldown factors (klassen: skill-tree nodes, buffs...): all multiplied, after Cooldownreductie. 1 = unchanged. */
    public static final java.util.List<java.util.function.BiFunction<ServerPlayer, Vaardigheid, Float>> COOLDOWN_MOD =
            new java.util.concurrent.CopyOnWriteArrayList<>();
    /** Per-player energy cost factors, the same way. */
    public static final java.util.List<java.util.function.BiFunction<ServerPlayer, Vaardigheid, Float>> ENERGIE_MOD =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    private Vaardigheden() {
    }

    public static int cooldown(ServerPlayer p, Vaardigheid v) {
        double cdr = Math.min(50, Spelers.stats(p).get(Stat.COOLDOWNREDUCTIE));
        double t = v.cooldownTicks() * (1 - cdr / 100.0);
        for (var f : COOLDOWN_MOD) {
            Float m = f.apply(p, v);
            if (m != null) {
                t *= Math.max(0, m);
            }
        }
        return (int) Math.round(t);
    }

    /** What this skill costs this player now. */
    public static double energie(ServerPlayer p, Vaardigheid v) {
        double e = v.energie();
        for (var f : ENERGIE_MOD) {
            Float m = f.apply(p, v);
            if (m != null) {
                e *= Math.max(0, m);
            }
        }
        return e;
    }

    public static boolean gebruik(ServerPlayer p, int slot) {
        if (!Vadsrijk.in(p) || p.isSpectator() || RijkScenes.bezig(p) || Statussen.heeft(p, Status.VERDOOFD)) {
            return false;
        }
        // g-klassen: the class is the weapon in your hand. Attacking with another class's weapon switches (its skills start on
        // full cooldown); an unmet weapon requirement or no class weapon at all: no skills.
        if (!Spelers.voorAanval(p)) {
            return false;
        }
        if (Spelers.actieveWapenKlasse(p) == null) {
            nee(p, "vadsrijk.kern.skill.geen_wapen");
            return false;
        }
        Klasse k = Spelers.klasse(p);
        List<Vaardigheid> vs = k.vaardigheden();
        if (slot < 0 || slot >= vs.size()) {
            return false;
        }
        Vaardigheid v = vs.get(slot);
        SpelerData d = Spelers.data(p);
        long nu = p.level().getGameTime();
        if (d.klaarOp.getOrDefault(v.id(), 0L) > nu) {
            nee(p, "vadsrijk.kern.skill.cooldown");
            return false;
        }
        if (v.ult()) {
            if (d.ultLading < 100) {
                nee(p, "vadsrijk.kern.skill.ult_niet_vol");
                return false;
            }
        } else if (Spelers.energie(p) < energie(p, v)) {
            nee(p, "vadsrijk.kern.skill.geen_energie");
            return false;
        }
        SkillContext ctx = new SkillContext(p, Spelers.stats(p), d.level(k.id()), doel(p, 24), Spelers.wapenElement(p), Spelers.wapenSchade(p));
        if (!v.gebruik(ctx)) {
            return false;
        }
        if (v.ult()) {
            d.ultLading = 0;
        } else {
            d.energie = Spelers.energie(p) - energie(p, v);
        }
        d.klaarOp.put(v.id(), nu + cooldown(p, v));
        blokInZicht(p);
        Tips.toon(p, "vadsrijk.kern.tip.skill");
        Spelers.sync(p);
        return true;
    }

    /** A skill aimed at a block (e.g. a training dummy): {@link Haken#BLOK_GERAAKT}. */
    private static void blokInZicht(ServerPlayer p) {
        if (Haken.BLOK_GERAAKT.isEmpty()) {
            return;
        }
        Vec3 oog = p.getEyePosition();
        net.minecraft.world.phys.BlockHitResult r = p.level().clip(new net.minecraft.world.level.ClipContext(oog, oog.add(p.getViewVector(1f).scale(24)),
                net.minecraft.world.level.ClipContext.Block.OUTLINE, net.minecraft.world.level.ClipContext.Fluid.NONE, p));
        if (r.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            Haken.BLOK_GERAAKT.forEach(h -> h.accept(p, r.getBlockPos()));
        }
    }

    private static void nee(ServerPlayer p, String key) {
        p.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        p.level().playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 0.4f, 0.6f);
    }

    /** The living entity under the crosshair within this distance (Vadsrijk mobs only, not plofd), or null. */
    @Nullable
    public static LivingEntity doel(ServerPlayer p, double afstand) {
        Vec3 oog = p.getEyePosition();
        Vec3 kijk = p.getViewVector(1f);
        Vec3 eind = oog.add(kijk.scale(afstand));
        AABB box = p.getBoundingBox().expandTowards(kijk.scale(afstand)).inflate(1.5);
        EntityHitResult r = ProjectileUtil.getEntityHitResult(p.level(), p, oog, eind, box,
                e -> e instanceof VadsrijkMob m && !m.geplofd(), 0.6f);
        return r != null && r.getEntity() instanceof LivingEntity l ? l : null;
    }
}
