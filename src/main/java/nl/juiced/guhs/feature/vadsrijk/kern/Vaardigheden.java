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
    private Vaardigheden() {
    }

    public static int cooldown(ServerPlayer p, Vaardigheid v) {
        double cdr = Math.min(50, Spelers.stats(p).get(Stat.COOLDOWNREDUCTIE));
        return (int) Math.round(v.cooldownTicks() * (1 - cdr / 100.0));
    }

    public static boolean gebruik(ServerPlayer p, int slot) {
        if (!Vadsrijk.in(p) || p.isSpectator() || RijkScenes.bezig(p) || Statussen.heeft(p, Status.VERDOOFD)) {
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
        } else if (Spelers.energie(p) < v.energie()) {
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
            d.energie = Spelers.energie(p) - v.energie();
        }
        d.klaarOp.put(v.id(), nu + cooldown(p, v));
        Tips.toon(p, "vadsrijk.kern.tip.skill");
        Spelers.sync(p);
        return true;
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
