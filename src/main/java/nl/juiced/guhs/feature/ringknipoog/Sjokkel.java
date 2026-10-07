package nl.juiced.guhs.feature.ringknipoog;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.feature.landdiertjes.LanddiertjesFeature;
import nl.juiced.guhs.feature.landdiertjes.ShuckleEntity;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.ringh3.Mijn;
import nl.juiced.guhs.feature.ringh3.RingH3Feature;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.wereld.Bezetting;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * bbq2 (ring-knipogen), wink 6, the first half: Sjokkel sets out over the bridge of Knabbelmoria. No camera: he is a
 * figure in the background of chapter 3, a real Sjokkel ({@link ShuckleEntity}, nothing about him changed) on the west
 * bridge head of every copy of the mine, shuffling towards the bridge along the edge of the platform, a few centimetres a
 * second, and pulling into his shell (as a Sjokkel does) when somebody thunders past. Only players who are at the chase
 * and the bridge (step 5 of ring_h3) ever see him (ring-kern's Zicht); the bridge scene hides every real entity of the
 * hall for its viewer, so he is not in it. Three chapters later he walks into the feast ({@link Knipogen#SJOKKEL}).
 * <p>
 * He is a registered inhabitant of the mine ({@link Bezetting}: a copy that lost him gets him back, he can't be hurt and
 * never despawns), stands still by himself (no AI) and is moved from here: {@link #seconde} of a player who can see him
 * nudges him along his line, and tells that player once, in the action bar, who that is.
 */
public final class Sjokkel {
    /** The Bezetting id of the Sjokkel of a mine, and his entity tag. */
    public static final String ID = "ringknipoog_sjokkel", TAG = "guhs_ringknipoog_sjokkel";
    /** Player saved data: this player was told who is crossing the bridge there. */
    public static final String GEZIEN = "guhs_ringknipoog_sjokkel_gezien";
    /** Template coordinates of the mine: the block he starts in, on the south edge of the west bridge head. */
    public static final BlockPos BEGIN = new BlockPos(61, 12, 16);
    /** His line (template coordinates): from the back of the platform to the head of the bridge; how far he gets in a second. */
    public static final Vec3 VAN = new Vec3(61.6, 12.0, 16.5), NAAR = new Vec3(64.4, 12.0, 16.5);
    public static final double STAP = 0.07;
    /** Nobody looked for this many ticks: he starts his journey again. */
    public static final int OPNIEUW = 200;
    /** The step of ring_h3 he is there for (the great hall, the chase, the bridge). */
    public static final int STAP_HAL = 5;
    /** How near a player has to come to be told who that is. */
    public static final double ZIE_AFSTAND = 14.0;

    private Sjokkel() {
    }

    /** (RingKnipoogFeature.register) one Sjokkel at every copy of the mine. */
    static void registreer() {
        Bezetting.wezen(ID, Mijn.STRUCTUUR, null, BEGIN, Sjokkel::maak, 8);
    }

    /** The Sjokkel of a copy, as he sets out (not in the world yet). */
    @Nullable
    static Entity maak(ServerLevel level, Vec3 plek, Rotation draai) {
        ShuckleEntity s = LanddiertjesFeature.SHUCKLE.get().create(level, EntitySpawnReason.STRUCTURE);
        if (s == null) {
            return null;
        }
        float yaw = Cutscene.wereldYaw(draai, 270f);          // (he looks east: at the bridge)
        s.snapTo(plek.x, plek.y, plek.z, yaw, 0f);
        s.setYBodyRot(yaw);
        s.setYHeadRot(yaw);
        s.setNoAi(true);
        s.setInvulnerable(true);
        s.setPersistenceRequired();
        s.addTag(TAG);
        Zicht.alleenBij(s, RingH3Feature.LIJN.id(), STAP_HAL, STAP_HAL);
        return s;
    }

    public static boolean isSjokkel(Entity e) {
        return e instanceof ShuckleEntity && e.entityTags().contains(TAG);
    }

    /** The Sjokkel of this copy of the mine (null: not there right now). */
    @Nullable
    static ShuckleEntity van(Mijn m) {
        Vec3 a = m.wereld(VAN), b = m.wereld(NAAR);
        List<ShuckleEntity> gevonden = m.level().getEntitiesOfClass(ShuckleEntity.class, new AABB(a, b).inflate(8.0), Sjokkel::isSjokkel);
        return gevonden.isEmpty() ? null : gevonden.get(0);
    }

    /**
     * (once a second, a player) a player who can see him (step 5 of chapter 3, at a mine): he shuffles on a little, and
     * starts again at the back of the platform when he reaches the bridge (for the next player it is the start of his
     * journey all the same). The first time the player is near: who that is.
     */
    static void seconde(ServerPlayer p) {
        if (RingH3Feature.LIJN.stap(p) != STAP_HAL || !RingH3Feature.LIJN.aanDeBeurt(p)) {
            return;
        }
        Mijn m = Mijn.van(p);
        ShuckleEntity s = m == null ? null : van(m);
        if (s == null) {
            return;
        }
        Vec3 van = m.wereld(VAN), naar = m.wereld(NAAR);
        if (!s.isInSchelp()) {
            schuifel(s, van, naar, m.yaw(270f));
        }
        if (!GuhQuests.saved(p).getBooleanOr(GEZIEN, false) && s.distanceToSqr(p) <= ZIE_AFSTAND * ZIE_AFSTAND) {
            GuhQuests.saved(p).putBoolean(GEZIEN, true);
            p.sendOverlayMessage(Component.translatable("quest.guhs.ringknipoog.sjokkel.brug").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /**
     * One shuffle along his line (at most once a game tick, however many players look). At the head of the bridge he waits;
     * when nobody has looked for a while he is back at the start of his journey, for whoever comes next.
     */
    static void schuifel(ShuckleEntity s, Vec3 van, Vec3 naar, float yaw) {
        long nu = s.level().getGameTime(), vorige = s.getPersistentData().getLongOr(TAG, Long.MIN_VALUE);
        if (vorige == nu) {
            return;
        }
        s.getPersistentData().putLong(TAG, nu);
        Vec3 lijn = naar.subtract(van);
        double lang = lijn.length();
        // how far along his line he is now (somebody may have shoved him): his spot projected on it
        double langs = lang < 1e-6 ? 0 : s.position().subtract(van).dot(lijn) / lang;
        langs = vorige == Long.MIN_VALUE || nu - vorige > OPNIEUW || nu < vorige ? 0 : Math.max(0, Math.min(lang, langs + STAP));
        Vec3 plek = lang < 1e-6 ? van : van.add(lijn.scale(langs / lang));
        s.snapTo(plek.x, plek.y, plek.z, yaw, 0f);
        s.setYBodyRot(yaw);
        s.setYHeadRot(yaw);
    }

    /** Nobody feeds, tames or picks up a Sjokkel who is on his way: a click only says so. */
    static void opKlik(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!isSjokkel(event.getTarget())) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getEntity() instanceof ServerPlayer p && event.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND) {
            p.sendOverlayMessage(Component.translatable("quest.guhs.ringknipoog.sjokkel.klik").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    static void opKlik2(PlayerInteractEvent.EntityInteract event) {
        if (isSjokkel(event.getTarget())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
