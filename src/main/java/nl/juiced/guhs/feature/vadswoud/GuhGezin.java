package nl.juiced.guhs.feature.vadswoud;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.BabyEntitySpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhPersonality;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.registry.ModEntities;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;
import nl.juiced.guhs.world.ModDimensions;

/**
 * Guh families. Everything here works from the outside (events and goals), so GuhEntity itself doesn't change:
 * <ul>
 *   <li>Wild guhs that spawn naturally in the Guhmension (the Guhmension spawner and the vanilla spawner, also while
 *       the world generates) form a family with the guhs that spawn with them: 1-2 parents and 1-3 babies, all the same
 *       variant. A family that got no babies gets one or two after its spawn tick. Bred babies join their parents' family.</li>
 *   <li>Every guh gets two goals: babies walk in a line behind their parent ({@link FollowFamilyLineGoal}, instead of
 *       vanilla's FollowParentGoal) and at night everyone sleeps in a nest nearby ({@link SleepInNestGoal}).</li>
 *   <li>Babies are easier to tame: knabbelbessen tame a wild baby 1 in 2 (grown-ups don't want them), and kaasknabbels
 *       get an extra 1 in 3 chance before the guh's own roll. A tame baby grows up faster on knabbelbessen.</li>
 * </ul>
 * A family member carries the compound "GuhsGezin" in its persistent data: Id (the family), Rol ("ouder"/"baby") and
 * Plek (0 for the parents, 1, 2, 3 for the babies: the order of the line).
 */
public final class GuhGezin {
    public static final String TAG = "GuhsGezin";
    public static final String OUDER = "ouder", BABY = "baby";
    /** A wild baby eats knabbelbessen and comes along 1 in this many times. */
    public static final int BESSEN_TAME_CHANCE = 2;
    /** Kaasknabbels: an extra 1 in this many for a wild baby (on top of the guh's own roll). */
    public static final int BABY_KNABBEL_BONUS = 3;
    /** Spawning guhs this close together (and in the same tick) belong to one family. */
    public static final double FAMILY_RADIUS = 10;

    /** A family that is still getting its members (only during the tick its first guh spawned). */
    static final class Forming {
        final long id;
        final long tick;
        final Vec3 pos;
        final GuhVariant variant;
        final float scale;
        final int parentsWanted, babiesWanted;
        int parents = 1, babies = 0;
        final UUID first;

        Forming(long id, long tick, Vec3 pos, GuhVariant variant, float scale, int parentsWanted, int babiesWanted, UUID first) {
            this.id = id;
            this.tick = tick;
            this.pos = pos;
            this.variant = variant;
            this.scale = scale;
            this.parentsWanted = parentsWanted;
            this.babiesWanted = babiesWanted;
            this.first = first;
        }

        boolean full() {
            return parents >= parentsWanted && babies >= babiesWanted;
        }
    }

    private static final Map<ResourceKey<Level>, List<Forming>> FORMING = new HashMap<>();

    // --- joining the world: goals for everyone, families for wild Guhmension guhs ----------------------------------------

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof GuhEntity guh)) {
            return;
        }
        addGoals(guh);
        if (level.dimension() != ModDimensions.GUHMENSION || guh.isTame() || guh.getPersistentData().contains(TAG)) {
            return;
        }
        EntitySpawnReason type = guh.getSpawnType();
        boolean fresh = type == EntitySpawnReason.NATURAL ? !event.loadedFromDisk() : type == EntitySpawnReason.CHUNK_GENERATION;
        if (fresh) {
            join(level, guh);
        }
    }

    /** The family goals (once per guh): the baby line instead of vanilla's follow-the-parent, and sleeping in nests. */
    public static void addGoals(GuhEntity guh) {
        if (guh.goalSelector.getAvailableGoals().stream().anyMatch(w -> w.getGoal() instanceof SleepInNestGoal)) {
            return;
        }
        guh.goalSelector.removeAllGoals(g -> g instanceof FollowParentGoal);
        guh.goalSelector.addGoal(4, new SleepInNestGoal(guh));
        guh.goalSelector.addGoal(5, new FollowFamilyLineGoal(guh));
    }

    /**
     * A wild guh that just spawned: the start of a new family (a parent), or the next member of the family that is
     * forming right here this tick (another parent, or a baby of the same variant).
     */
    public static void join(ServerLevel level, GuhEntity guh) {
        var random = guh.getRandom();
        join(level, guh, random.nextFloat() < 0.35f ? 2 : 1, 1 + random.nextInt(3));
    }

    /** {@link #join(ServerLevel, GuhEntity)}, with the size of the family it may start (parents, babies). */
    public static void join(ServerLevel level, GuhEntity guh, int parentsWanted, int babiesWanted) {
        if (guh.hasSecretNote() || guh.getVariant() == GuhVariant.BROCOCOLIEF) {
            guh.getPersistentData().put(TAG, new CompoundTag());   // (the guh with the note walks alone)
            return;
        }
        long now = level.getGameTime();
        List<Forming> forming = FORMING.computeIfAbsent(level.dimension(), k -> new ArrayList<>());
        Forming family = null;
        for (int i = forming.size() - 1; i >= 0; i--) {
            Forming f = forming.get(i);
            if (f.tick == now && !f.full() && f.pos.distanceTo(guh.position()) <= FAMILY_RADIUS) {
                family = f;
                break;
            }
        }
        if (family == null) {
            family = new Forming(level.getRandom().nextLong() & Long.MAX_VALUE | 1L, now, guh.position(), guh.getVariant(), guh.getGuhScale(),
                    parentsWanted, babiesWanted, guh.getUUID());
            forming.add(family);
            tag(guh, family.id, OUDER, 0);
            return;
        }
        guh.setVariant(family.variant);
        if (family.parents < family.parentsWanted) {
            family.parents++;
            tag(guh, family.id, OUDER, 0);
        } else {
            family.babies++;
            makeBaby(guh, family.scale);
            tag(guh, family.id, BABY, family.babies);
        }
    }

    /** After the spawn tick: a family without babies gets 1-2 of its own, next to its first parent. */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            closeFamilies(level, level.getGameTime());
        }
    }

    /** Closes the families that were forming before this tick (the test calls this with a later tick). Returns the babies born. */
    public static int closeFamilies(ServerLevel level, long now) {
        List<Forming> forming = FORMING.get(level.dimension());
        if (forming == null || forming.isEmpty()) {
            return 0;
        }
        int born = 0;
        // (take the due families out first: a baby that joins the level may start a new forming family in this list)
        List<Forming> due = new ArrayList<>();
        for (Iterator<Forming> it = forming.iterator(); it.hasNext(); ) {
            Forming f = it.next();
            if (f.tick < now) {
                due.add(f);
                it.remove();
            }
        }
        for (Forming f : due) {
            if (f.babies > 0 || !(level.getEntity(f.first) instanceof GuhEntity parent) || !parent.isAlive() || parent.isTame()) {
                continue;
            }
            int n = Math.min(f.babiesWanted, 1 + parent.getRandom().nextInt(2));
            for (int i = 1; i <= n; i++) {
                if (spawnBaby(level, parent, f.id, i) != null) {
                    born++;
                }
            }
        }
        return born;
    }

    /** A baby for this family, right next to its parent (or null if there's no room). */
    @Nullable
    public static GuhEntity spawnBaby(ServerLevel level, GuhEntity parent, long familyId, int place) {
        GuhEntity baby = ModEntities.GUH.get().create(level, EntitySpawnReason.TRIGGERED);
        if (baby == null) {
            return null;
        }
        double a = parent.getRandom().nextDouble() * Math.PI * 2;
        baby.snapTo(parent.getX() + Math.cos(a) * 0.9, parent.getY(), parent.getZ() + Math.sin(a) * 0.9, parent.getYRot(), 0);
        if (!level.noCollision(baby)) {
            baby.snapTo(parent.getX(), parent.getY(), parent.getZ(), parent.getYRot(), 0);
        }
        net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(baby, level, level.getCurrentDifficultyAt(parent.blockPosition()), EntitySpawnReason.EVENT, null);
        baby.setVariant(parent.getVariant() == GuhVariant.BROCOCOLIEF ? GuhVariant.NORMAL : parent.getVariant());
        makeBaby(baby, parent.getGuhScale());
        if (parent.getRandom().nextInt(3) > 0) {
            baby.setPersonality(parent.getPersonality());
        }
        tag(baby, familyId, BABY, place);
        level.addFreshEntity(baby);
        return baby;
    }

    private static void makeBaby(GuhEntity guh, float parentScale) {
        guh.setBaby(true);
        guh.setGuhScale(parentScale * (0.85f + guh.getRandom().nextFloat() * 0.15f));
        guh.setSecretNote(false);
        for (GuhClothes.Slot slot : GuhClothes.Slot.values()) {
            guh.takeOff(slot);   // (no outfits on the little ones)
        }
    }

    public static void tag(GuhEntity guh, long id, String role, int place) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Id", id);
        tag.putString("Rol", role);
        tag.putInt("Plek", place);
        guh.getPersistentData().put(TAG, tag);
    }

    /** The family id of a guh (0: none). */
    public static long familyOf(GuhEntity guh) {
        return guh.getPersistentData().getCompoundOrEmpty(TAG).getLongOr("Id", 0L);
    }

    public static boolean isParent(GuhEntity guh) {
        return OUDER.equals(guh.getPersistentData().getCompoundOrEmpty(TAG).getStringOr("Rol", ""));
    }

    /** A baby's place in the line (1 walks right behind the parent), 0 when it has none. */
    public static int placeOf(GuhEntity guh) {
        return guh.getPersistentData().getCompoundOrEmpty(TAG).getIntOr("Plek", 0);
    }

    /** A bred baby joins its parents' family (they start one if they have none): at the back of the line. */
    @SubscribeEvent
    public static void onBaby(BabyEntitySpawnEvent event) {
        if (!(event.getChild() instanceof GuhEntity baby) || !(event.getParentA() instanceof GuhEntity a)) {
            return;
        }
        long id = familyOf(a);
        if (id == 0 && event.getParentB() instanceof GuhEntity b) {
            id = familyOf(b);
        }
        if (id == 0) {
            id = a.getRandom().nextLong() & Long.MAX_VALUE | 1L;
        }
        tag(a, id, OUDER, 0);
        if (event.getParentB() instanceof GuhEntity b) {
            tag(b, id, OUDER, 0);
        }
        long family = id;
        int place = 1 + a.level().getEntitiesOfClass(GuhEntity.class, a.getBoundingBox().inflate(16),
                g -> g.isBaby() && familyOf(g) == family).stream().mapToInt(GuhGezin::placeOf).max().orElse(0);
        tag(baby, id, BABY, place);
    }

    // --- babies are easier to tame ---------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void onInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide() || !(event.getTarget() instanceof GuhEntity guh) || !(event.getEntity() instanceof ServerPlayer player)
                || guh.getHiddenBy() != null) {
            return;
        }
        ItemStack stack = event.getItemStack();
        InteractionResult result = feed(guh, player, stack);
        if (result != null) {
            event.setCanceled(true);
            event.setCancellationResult(result);
        }
    }

    /** Knabbelbessen (and kaasknabbels) for a baby; null when the guh's own interaction should go on. */
    @Nullable
    public static InteractionResult feed(GuhEntity guh, ServerPlayer player, ItemStack stack) {
        ServerLevel level = player.level();
        if (stack.is(VadswoudFeature.KNABBELBESSEN.get())) {
            if (!guh.isBaby()) {
                if (!guh.isTame()) {
                    player.sendOverlayMessage(Component.translatable("gui.guhs.vadswoud.alleen_babys").withStyle(ChatFormatting.GOLD));
                }
                return null;
            }
            stack.consume(1, player);
            guh.playSound(ModSounds.GUH_EAT.get(), 1f, guh.getVoicePitch());
            if (guh.isTame()) {
                guh.ageUp(AgeableMob.getSpeedUpSecondsWhenFeeding(-guh.getAge()), true);
                guh.triggerAnim("action", "happy");
                player.sendOverlayMessage(Component.translatable("gui.guhs.vadswoud.baby_bessen").withStyle(ChatFormatting.GOLD));
            } else if (guh.getRandom().nextInt(BESSEN_TAME_CHANCE) == 0 && !net.neoforged.neoforge.event.EventHooks.onAnimalTame(guh, player)) {
                tame(guh, player);
            } else {
                level.sendParticles(ParticleTypes.SMOKE, guh.getX(), guh.getY() + guh.getBbHeight() * 0.7, guh.getZ(), 5, 0.2, 0.2, 0.2, 0.01);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(ModItems.KAAS_KNABBELS.get()) && guh.isBaby() && !guh.isTame() && guh.getRandom().nextInt(BABY_KNABBEL_BONUS) == 0
                && !net.neoforged.neoforge.event.EventHooks.onAnimalTame(guh, player)) {
            stack.consume(1, player);
            guh.playSound(ModSounds.GUH_EAT.get(), 1f, guh.getVoicePitch());
            tame(guh, player);
            return InteractionResult.SUCCESS;
        }
        return null;   // (the guh's own roll: a baby may still get lucky there)
    }

    private static void tame(GuhEntity guh, ServerPlayer player) {
        guh.tame(player);
        guh.getNavigation().stop();
        guh.setTarget(null);
        guh.level().broadcastEntityEvent(guh, (byte) 7);
        guh.playSound(ModSounds.GUH_HAPPY.get(), 1f, guh.getVoicePitch());
        guh.triggerAnim("action", "happy");
        if (!guh.hasPersonality()) {
            guh.setPersonality(GuhPersonality.random(guh.getRandom()));
        }
        player.sendOverlayMessage(Component.translatable("gui.guhs.vadswoud.baby_getemd").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        FORMING.clear();
    }

    /** Is someone's family line (a baby with a family) walking behind a parent near this spot? For the family quest. */
    static void noticeFamily(GuhEntity baby) {
        if (baby.tickCount % 40 == 0 && baby.level() instanceof ServerLevel level) {
            for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, baby.getBoundingBox().inflate(10))) {
                VadsAdvancements.grant(player, "vadswoud_gezin");
            }
        }
    }

    private GuhGezin() {
    }
}
