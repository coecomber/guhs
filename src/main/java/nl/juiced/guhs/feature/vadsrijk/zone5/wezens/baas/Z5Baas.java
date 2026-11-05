package nl.juiced.guhs.feature.vadsrijk.zone5.wezens.baas;

import javax.annotation.Nullable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Baas;
import nl.juiced.guhs.feature.vadsrijk.zone5.wezens.Z5WezensApi;

/**
 * The base of the zone-5 bosses: zone 3's fight engine ({@link Z3Baas}: every player in the arena is a target, mechanic hits as a
 * fraction of max Vads capped at 60 %, immune thresholds, tasks, enrage, one fight at a time) with zone-5 texts
 * ({@code vadsrijk.z5wezens.baas.<id>}), zone-5 chests ({@link Z5BazenLoot}) and the hook {@link Z5WezensApi#BAAS}.
 */
public abstract class Z5Baas extends Z3Baas {
    protected Z5Baas(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Override
    public String titelKey() {
        return "vadsrijk.z5wezens.baas." + baasId();
    }

    @Override
    public int xp() {
        int l = Math.max(1, Math.min(Levels.MAX - 1, mobLevel()));
        return (int) Math.max(1, Math.round(Levels.xpVoor(l) * 0.35));
    }

    @Override
    protected LootTabel.Resultaat kistInhoud(ServerPlayer p, boolean eersteKill) {
        return Z5BazenLoot.kist(baasId(), p, mobLevel(), getRandom());
    }

    /** A chat line by the boss (key under vadsrijk.z5wezens.). */
    public void praat(String key, Object... args) {
        spreek("vadsrijk.z5wezens." + key, args);
    }

    @Override
    public void verslagen(@Nullable LivingEntity laatste) {
        boolean was = geplofd();
        super.verslagen(laatste);
        if (!was && geplofd() && level() instanceof ServerLevel sl) {
            for (ServerPlayer p : sl.players()) {
                if (bijdragen().spelers().contains(p.getUUID())) {
                    Z5WezensApi.baas(p, baasId());
                    naVerslagen(p);
                }
            }
        }
    }

    /** Per contributor after the defeat (story hooks). */
    protected void naVerslagen(ServerPlayer p) {
    }
}
