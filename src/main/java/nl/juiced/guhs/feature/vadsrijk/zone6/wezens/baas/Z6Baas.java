package nl.juiced.guhs.feature.vadsrijk.zone6.wezens.baas;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Baas;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6Muziek;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6Vfx;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6WezensApi;

/**
 * The base of the zone-6 bosses: zone 3's fight engine ({@link Z3Baas}: every player in the arena is a target, a rotation,
 * fraction-of-max-Vads mechanics capped at 60 %, immune/floor while a mechanic waits, per-tick tasks, enrage, the bouwdoos of the
 * VFX kit) with the zone-6 identity:
 * <ul>
 *   <li>texts under {@code vadsrijk.z6wezens.baas.<id>} (title card + {@code .onder}; phase cards {@code vadsrijk.z6wezens.<id>.fase<n>});</li>
 *   <li>its own music theme id ({@link Z6Muziek}: u-audio2's pipeline; until the theme is rendered a related existing theme plays);</li>
 *   <li>chests from {@link Z6BazenLoot} (named items, trophy, first-kill guarantee, Gouden Veren);</li>
 *   <li>the zone-6 tone: a victim (ontvadst) is CHEERED UP (pink confetti, it stays happy a while); a construct crumbles into
 *       glittering salt ({@link #construct()}); the Mika flees in smoke ({@link #isMika()}).</li>
 * </ul>
 */
public abstract class Z6Baas extends Z3Baas {
    protected Z6Baas(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    /** A construct (falls apart instead of being cheered up). */
    protected boolean construct() {
        return false;
    }

    @Override
    public String titelKey() {
        return "vadsrijk.z6wezens.baas." + baasId();
    }

    @Override
    public void intro(ServerPlayer p, boolean eersteKeer) {
        super.intro(p, eersteKeer);
        Tips.toon(p, "vadsrijk.z6wezens.tip.baas_" + baasId());
    }

    @Override
    public int xp() {
        int l = Math.max(1, Math.min(Levels.MAX - 1, mobLevel()));
        return (int) Math.max(1, Math.round(Levels.xpVoor(l) * 0.35));
    }

    @Override
    protected int enrageTicks() {
        return 20 * 60 * 9;
    }

    @Nullable
    @Override
    public net.minecraft.resources.Identifier muziek() {
        return Z6Muziek.id(baasId());
    }

    @Override
    protected LootTabel.Resultaat kistInhoud(ServerPlayer p, boolean eersteKill) {
        return Z6BazenLoot.kist(baasId(), p, mobLevel(), getRandom());
    }

    /** A chat line by the boss (key under vadsrijk.z6wezens.). */
    public void praat(String key, Object... args) {
        spreek("vadsrijk.z6wezens." + key, args);
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        boolean was = geplofd();
        super.verslagen(laatste);
        if (!was && geplofd() && level() instanceof ServerLevel sl) {
            for (ServerPlayer p : sl.players()) {
                if (bijdragen().spelers().contains(p.getUUID())) {
                    Z6WezensApi.baas(p, baasId());
                    naVerslagen(p);
                }
            }
        }
    }

    /** Per contributor after the defeat (story hooks). */
    protected void naVerslagen(ServerPlayer p) {
    }

    /** Zone 3's controllers + the VFX kit's generic poses (wind-up, hit, flinch, phase, cheer: t-vfx merges them into our files). */
    @Override
    public void registerControllers(com.geckolib.animatable.manager.AnimatableManager.ControllerRegistrar controllers) {
        super.registerControllers(controllers);
        nl.juiced.guhs.feature.vadsrijk.vfx.BaasAnims.controllers(controllers);
    }

    @Override
    protected void opPlof() {
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        if (isMika()) {
            rook(sl, position(), getBbWidth());
            return;
        }
        if (construct()) {
            Z6Vfx.glinster(sl, position().add(0, getBbHeight() / 2, 0), Math.max(2, getBbWidth()));
            Z6Vfx.zoutKorst(sl, position().add(0, getBbHeight() / 2, 0), Math.max(2, getBbWidth()));
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2f, 0.6f);
            return;
        }
        Z6Vfx.feest(sl, position().add(0, getBbHeight() / 2, 0), Math.max(1.5, getBbWidth()));
        Z6Vfx.zoutKorst(sl, position().add(0, getBbHeight() * 0.7, 0), getBbWidth());
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 2f, 1.2f);
        sl.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1f, 1.4f);
    }
}
