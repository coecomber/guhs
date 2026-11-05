package nl.juiced.guhs.feature.vadsrijk.zone4.bazen;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Baas;
import nl.juiced.guhs.feature.vadsrijk.zone4.wezens.Z4Vfx;

/**
 * The base of the zone-4 bosses: the whole zone-3 engine ({@link Z3Baas}: every player in the arena is a target, a rotation,
 * fraction-of-max-Vads mechanics capped at 60 %, immune/floor while a mechanic waits, per-tick tasks, enrage, "one fight at a
 * time"), with the zone-4 identity:
 * <ul>
 *   <li>texts under {@code vadsrijk.z4bazen.<id>} (title, .onder, tips {@code vadsrijk.z4bazen.tip.<id>});</li>
 *   <li>chests from {@link Z4BazenLoot} (named Legendaries, trophy, the first-kill guarantee);</li>
 *   <li>the zone-4 tone: an ontvadst boss gets cheered up (pink confetti, it stays a moment, happy) instead of falling apart;
 *       the Mika flees in smoke ({@link #isMika()}).</li>
 * </ul>
 * Zone 4 is a rest zone: the numbers are gentler than zone 3's Hart (enrage 8 min, fewer adds).
 */
public abstract class Z4Baas extends Z3Baas {
    protected Z4Baas(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public String titelKey() {
        return "vadsrijk.z4bazen." + baasId();
    }

    @Override
    public void intro(ServerPlayer p, boolean eersteKeer) {
        super.intro(p, eersteKeer);
        Tips.toon(p, "vadsrijk.z4bazen.tip." + baasId());
    }

    @Override
    protected int enrageTicks() {
        return 20 * 60 * 8;
    }

    @Override
    protected LootTabel.Resultaat kistInhoud(ServerPlayer p, boolean eersteKill) {
        return Z4BazenLoot.kist(baasId(), p, Math.max(50, Math.min(56, mobLevel())), getRandom());
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        boolean was = geplofd();
        super.verslagen(laatste);
        if (!was && geplofd()) {
            Z4BazenApi.verslagen(this);
        }
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
        Z4Vfx.feest(sl, position().add(0, getBbHeight() / 2, 0), Math.max(1.5, getBbWidth()));
        sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 2f, 1.2f);
        sl.playSound(null, blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1f, 1.4f);
    }
}
