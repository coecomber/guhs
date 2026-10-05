package nl.juiced.guhs.feature.guheinde;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModEntities;

/**
 * 1.2.8: the Enderguh belongs to the endgame. It used to be born on the Guhpieken of the Guhmensie (1 in 30 guhs there),
 * which gave you a flying mount far too early.
 * <ul>
 *   <li>Wild Enderguhs only live in the Guheinde, and only around players who beat Opper-Mika themselves
 *   ({@link #heeftVerslagen}): a gentle top-up ({@link #aanvullen}), a low cap, slow, never during the fight. They come
 *   and go ({@link #WILD}: never saved, gone when everybody is far away, see GuhEntity#removeWhenFarAway).</li>
 *   <li>Only a player who beat Opper-Mika can tame a wild one ({@link #temSlot}).</li>
 *   <li>Only a rider who beat Opper-Mika can fly on an Enderguh (also a Vahoege Enderguh, also one from before 1.2.8):
 *   for everybody else it walks like any big guh ({@link #vliegSlot}; GuhEntity keeps the lock in synced data, because
 *   the rider's own client steers).</li>
 * </ul>
 */
public final class Enderguhs {
    /** The entity tag of a wild Enderguh of the top-up: it comes and goes. */
    public static final String WILD = "guhs_guheinde_enderguh";
    /** The top-up looks every this many ticks, per qualified player; 1 in {@link #KANS} looks brings one. */
    public static final int TIJD = 600, KANS = 3;
    /** No new one when this many wild Enderguhs are within 64 / 128 blocks of the player. */
    public static final int VOL = 2, VOL_WIJD = 3;

    /** Did this player beat Opper-Mika (at least once, themselves)? */
    public static boolean heeftVerslagen(@Nullable Player player) {
        return player != null && GuhQuests.saved(player).getIntOr(GuheindeGevecht.WINS, 0) > 0;
    }

    /** The message for a rider who can't fly on an Enderguh yet (once per time they get on). */
    public static void vliegSlot(Player rider) {
        rider.sendSystemMessage(Component.translatable("gui.guhs.guheinde.enderguh.vlieg_slot").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    /** The message for a player who can't tame a wild Enderguh yet. */
    public static void temSlot(Player player) {
        player.sendSystemMessage(Component.translatable("gui.guhs.guheinde.enderguh.tem_slot").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !GuheindeFeature.isGuheinde(level) || level.getGameTime() % TIJD != 130
                || !level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.SPAWN_MOBS)) {
            return;
        }
        GuheindeGevecht gevecht = GuheindeGevecht.of(level);
        if (gevecht != null && bezig(gevecht)) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (level.getRandom().nextInt(KANS) == 0) {
                aanvullen(level, player, level.getRandom());
            }
        }
    }

    /** Is Opper-Mika there (or on his way)? Then no Enderguhs come. */
    public static boolean bezig(GuheindeGevecht gevecht) {
        return gevecht.fightActive || gevecht.bossId != null || gevecht.respawnTicks >= 0;
    }

    /**
     * One wild Enderguh flies in 24 to 48 blocks from this player, when the player beat Opper-Mika and there are few
     * around. Returns the Enderguh, or null.
     */
    @Nullable
    public static GuhEntity aanvullen(ServerLevel level, ServerPlayer player, RandomSource random) {
        if (player.isSpectator() || !heeftVerslagen(player)
                || level.getEntitiesOfClass(GuhEntity.class, player.getBoundingBox().inflate(64), Enderguhs::isWild).size() >= VOL
                || level.getEntitiesOfClass(GuhEntity.class, player.getBoundingBox().inflate(128), Enderguhs::isWild).size() >= VOL_WIJD) {
            return null;
        }
        double a = random.nextDouble() * Math.PI * 2, d = 24 + random.nextInt(25);
        int x = (int) Math.floor(player.getX() + Math.cos(a) * d), z = (int) Math.floor(player.getZ() + Math.sin(a) * d);
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) {
            return null;
        }
        BlockPos pos = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
        return op(level, pos, random);
    }

    /** A wild come-and-go Enderguh on this spot, when there is ground under it (not the void) and room. Or null. */
    @Nullable
    public static GuhEntity op(ServerLevel level, BlockPos pos, RandomSource random) {
        if (pos.getY() <= level.getMinY() || level.getBlockState(pos.below()).isAir()) {
            return null;
        }
        GuhEntity guh = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (guh == null) {
            return null;
        }
        guh.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360f, 0);
        guh.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.NATURAL, null);
        guh.setVariant(GuhVariant.ENDER);
        guh.setGuhScale(Math.max(guh.getGuhScale(), GuhEntity.RIDEABLE_SCALE + 0.15f + random.nextFloat() * 0.4f));   // big enough to ride
        guh.addTag(WILD);
        if (!level.noCollision(guh)) {
            guh.discard();
            return null;
        }
        level.addFreshEntity(guh);
        return guh;
    }

    /** A wild Enderguh (either kind). */
    public static boolean isWild(GuhEntity guh) {
        return guh.isEnder() && !guh.isTame() && guh.isAlive();
    }

    private Enderguhs() {
    }
}
