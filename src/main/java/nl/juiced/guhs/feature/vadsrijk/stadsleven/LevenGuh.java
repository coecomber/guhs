package nl.juiced.guhs.feature.vadsrijk.stadsleven;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.vadsrijk.npc.RijkGuh;

/**
 * U-LEVEN-STAD: a town guh (vr_sl_guh): the npc slice's {@link RijkGuh} (real guh model + clothes, passive, never hurt, never
 * saved) without any AI of its own. {@link StadsLeven} moves it kinematically (walking its pet, playing tag, sliding, swinging)
 * and gives it a pose: sitting ({@code setInSittingPose}), an emote held as long as the group wants ({@link #houd}: the ukulele
 * of a street musician, eating on a terrace) and a speech bubble ({@link #zeg}: the custom name, shown for a few seconds).
 * Children are the same guh at {@link #KIND} scale.
 */
public class LevenGuh extends RijkGuh {
    public static final float KIND = 0.55f;
    /** The emote this guh keeps doing (null = none); re-applied every tick because the plain emote motor only runs for wild guhs. */
    @Nullable
    private Emote houd;
    private int bubbelTot;
    /** The group that owns it (for the click). Server side. */
    @Nullable
    Groep groep;

    public LevenGuh(EntityType<? extends TamableAnimal> type, Level level) {
        super(type, level);
    }

    @Override
    protected void registerGoals() {
    }

    /** Child size or grown-up. */
    public void maat(float m) {
        if (getAttribute(Attributes.SCALE) != null) {
            getAttribute(Attributes.SCALE).setBaseValue(m);
        }
    }

    public void zit(boolean zit) {
        setOrderedToSit(zit);
        setInSittingPose(zit);
    }

    /** Keep doing this emote (looping) until {@code houd(null)}. */
    public void houd(@Nullable Emote e) {
        this.houd = e;
        setEmoteData(e == null ? 0 : (e.ordinal() + 1) | GuhEmotes.LOOP);
    }

    /** A short speech bubble above its head (the custom name), for {@code ticks}. */
    public void zeg(Component tekst, int ticks) {
        setCustomName(tekst);
        setCustomNameVisible(true);
        bubbelTot = tickCount + ticks;
    }

    public boolean praat() {
        return bubbelTot > tickCount;
    }

    /** Server: walk/move to v this tick (kinematic), facing where it goes. */
    public void stap(Vec3 v) {
        Vec3 d = v.subtract(position());
        if (d.horizontalDistanceSqr() > 1e-4) {
            float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
            kijk(yaw);
        }
        setPos(v.x, v.y, v.z);
        setDeltaMovement(Vec3.ZERO);
    }

    public void kijk(float yaw) {
        setYRot(yaw);
        setYHeadRot(yaw);
        setYBodyRot(yaw);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        int wil = houd == null ? 0 : (houd.ordinal() + 1) | GuhEmotes.LOOP;
        if (getEmoteData() != wil) {
            setEmoteData(wil);
        }
        if (bubbelTot != 0 && tickCount >= bubbelTot) {
            bubbelTot = 0;
            setCustomNameVisible(false);
            setCustomName(null);
        }
    }

    @Override
    public boolean shouldShowName() {
        return hasCustomName() && isCustomNameVisible();
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !this.level().isClientSide() && player instanceof ServerPlayer sp) {
            StadsLeven.klik(sp, this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean broadcastToPlayer(ServerPlayer player) {
        return StadsLeven.ziet(player);
    }

    @Override
    public void checkDespawn() {
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }
}
