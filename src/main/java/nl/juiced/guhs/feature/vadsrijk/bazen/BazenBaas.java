package nl.juiced.guhs.feature.vadsrijk.bazen;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;

/**
 * What the three bazen bosses share on top of kern's {@link VadsrijkBaas}: GeckoLib (controllers "beweeg" idle/walk and a
 * triggerable "actie"), the boss music hook, the first-fight tip, and personal chests with OUR loot ({@link BazenLoot#kist}:
 * named items, trophies, everything gebonden) through kern's {@code kistInhoud} hook (integration 1.10.0; was a beloon hack).
 */
public abstract class BazenBaas extends VadsrijkBaas implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** Where the chests went (for the gametest). */
    public final List<BlockPos> kisten = new ArrayList<>();

    protected BazenBaas(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    /** koning_klodder | opperzwam | mika_spion. */
    public abstract String baasId();

    protected String[] actieAnimaties() {
        return new String[0];
    }

    @Nullable
    @Override
    public String lootTabel() {
        return "bazen:" + baasId();
    }

    @Override
    public String titelKey() {
        return "vadsrijk.bazen." + baasId();
    }

    @Override
    public void intro(ServerPlayer p, boolean eersteKeer) {
        super.intro(p, eersteKeer);
        BazenMuziek.start(p, baasId());
        Tips.toon(p, "vadsrijk.bazen.tip." + baasId());
    }

    @Override
    protected Vec3 kistMidden() {
        return arena != null ? Vec3.atBottomCenterOf(arena.centrum()) : position();
    }

    /** Our chest contents (kern hook, contract 13): BazenLoot's named items, trophies, economie gear, all gebonden. */
    @Override
    protected LootTabel.Resultaat kistInhoud(ServerPlayer p, boolean eersteKill) {
        return BazenLoot.kist(baasId(), p, mobLevel(), getRandom());
    }

    @Override
    protected void kistGeplaatst(ServerPlayer p, BlockPos pos) {
        kisten.add(pos);
    }

    @Override
    protected void beloon(ServerLevel sl, Set<ServerPlayer> helpers) {
        kisten.clear();
        super.beloon(sl, helpers);   // xp, first-kill memory, our chests via kistInhoud, arena notice
        for (UUID id : bijdragen.spelers()) {
            if (sl.getEntity(id) instanceof ServerPlayer p) {
                BazenMuziek.stop(p);
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = RawAnimation.begin().thenLoop("idle"), walk = RawAnimation.begin().thenLoop("walk");
        controllers.add(new AnimationController<>("beweeg", 4, state -> state.setAndContinue(state.isMoving() ? walk : idle)));
        AnimationController<BazenBaas> actie = new AnimationController<>("actie", 2, state -> PlayState.STOP);
        for (String a : actieAnimaties()) {
            actie.triggerableAnim(a, RawAnimation.begin().thenPlay(a));
        }
        controllers.add(actie);
        nl.juiced.guhs.feature.vadsrijk.vfx.BaasAnims.controllers(controllers);   // T-VFX: wind-up/hit/flinch/phase/cheer
    }

    protected void speel(String anim) {
        triggerAnim("actie", anim);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, net.minecraft.world.damagesource.DamageSource source) {
        return false;
    }
}
