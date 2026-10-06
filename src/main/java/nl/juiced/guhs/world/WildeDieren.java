package nl.juiced.guhs.world;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Bucketable;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import nl.juiced.guhs.entity.MikaEntity;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.kaasmoeras.KaasmoerasFeature;
import nl.juiced.guhs.feature.landdiertjes.LanddiertjesFeature;
import nl.juiced.guhs.feature.piep.PiepFeature;
import nl.juiced.guhs.feature.sausdieren.SausdierenFeature;
import nl.juiced.guhs.feature.spiesburcht.SpiesburchtFeature;
import nl.juiced.guhs.feature.vogels.VogelsFeature;
import nl.juiced.guhs.feature.vogels.Vogeltje;
import nl.juiced.guhs.feature.waterdiertjes.WaterdiertjesFeature;
import nl.juiced.guhs.registry.ModEntities;

/**
 * 1.1.2: the wild animals, critters and Mikas of Guhs (not the guhs themselves: see GuhEntity#isKomEnGaGuh and the
 * GuhmensionSpawner) never pile up in the world.
 * <ul>
 *   <li>A wild one that a spawner brings while you play (the vanilla natural spawner, a mob spawner block, the birds'
 *   top-up around players) is a <b>come-and-go</b> animal ({@link #KOM_EN_GA}, an entity tag): it is never written to
 *   disk (gone when its chunk unloads, see EntitySaveMixin) and it despawns when every player is far away, like a vanilla
 *   monster or bat. The spawners bring new ones around you.</li>
 *   <li>The animals the world is made with (chunk generation) stay, just like vanilla cows: that number only depends on
 *   how much world there is.</li>
 *   <li>Everything you keep stays exactly as before: tame, named, leashed, riding/carried, from a bucket, bred, placed by a
 *   structure or on purpose (persistence required), Big Mika and every boss.</li>
 *   <li>A tidy-up every 30 s ({@link #tidyUp}) is the safety net: when a level holds more wild ones of a kind than
 *   {@link #max} (e.g. from a world of 1.1.1 or older, where they were still saved), the ones far from every player go,
 *   farthest first.</li>
 * </ul>
 */
public final class WildeDieren {
    /** The entity tag of a come-and-go animal (set when it spawns; only means something while the animal is wild). */
    public static final String KOM_EN_GA = "guhs_kom_en_ga";
    public static final int OPRUIM_TIJD = 20 * 30;
    /** A wild one this close to a player is never tidied away (you'd see it vanish). */
    public static final double VEILIG = 64;
    /** At most this many wild ones of a kind per level (plus {@link #MAX_PER_SPELER} per player there) before the tidy-up. */
    public static final int MAX_BASIS = 200;
    public static final int MAX_PER_SPELER = 100;

    private static Set<EntityType<?>> soorten;

    /** The kinds this is about: every wild Guhs animal, critter and Mika (not the guhs, not the farm animals). */
    public static Set<EntityType<?>> soorten() {
        if (soorten == null) {
            soorten = Set.of(
                    VogelsFeature.PLUISVINKJE.get(), VogelsFeature.KAASMEESJE.get(), VogelsFeature.GUH_UILTJE.get(), VogelsFeature.ZEEMEEUWTJE.get(),
                    WaterdiertjesFeature.GUHXOLOTL.get(), WaterdiertjesFeature.GUH_EENDJE.get(), WaterdiertjesFeature.KNABBELVLINDERTJE.get(),
                    WaterdiertjesFeature.GLIMGUHTJE.get(), WaterdiertjesFeature.LIEVEHEERSBEESTJE.get(),
                    LanddiertjesFeature.PLUISEGELTJE.get(), LanddiertjesFeature.GUH_KONIJNTJE.get(), LanddiertjesFeature.PLUISEEKHOORNTJE.get(),
                    LanddiertjesFeature.SHUCKLE.get(),
                    ModEntities.GUH_BEE.get(), ModEntities.GUH_SLIME.get(), ModEntities.GUH_VIS.get(),
                    ModEntities.MIKA.get(), ModEntities.NETHER_MIKA.get(), GuheindeFeature.MIKA_LARFJE.get(),
                    KaasmoerasFeature.KAASMOT.get(), KaasmoerasFeature.KIKKERGUH.get(), KaasmoerasFeature.MOERASHEKS_MIKA.get(),
                    SpiesburchtFeature.ROOKGUH.get(), SpiesburchtFeature.VONK_MIKA.get(), SpiesburchtFeature.KNEKEL_MIKA.get(),
                    PiepFeature.POEPSCHILLY.get(), PiepFeature.SCHILLY.get(),
                    SausdierenFeature.SAUSLOPER.get(), SausdierenFeature.SAUSBLUBJE.get());
        }
        return soorten;
    }

    /** Nobody's and not kept on purpose: not tame, not named, not persistent, not leashed or riding, not from a bucket, no boss. */
    public static boolean isWild(Mob mob) {
        // (a Mika always carries its own name "Mika"; a name tag also makes a mob persistent, so a renamed Mika still stays)
        if (mob.isPersistenceRequired() || mob.requiresCustomPersistence() || mob.hasCustomName() && !(mob instanceof MikaEntity) || mob.isVehicle()) {
            return false;
        }
        if (mob instanceof TamableAnimal t && (t.isTame() || t.getOwnerReference() != null)) {
            return false;
        }
        if (mob instanceof Bucketable b && b.fromBucket()) {
            return false;
        }
        return !(mob instanceof MikaEntity m && m.isBoss());
    }

    /** A come-and-go animal: brought by a spawner while playing and still wild. Never saved, despawns when far. */
    public static boolean isKomEnGa(Mob mob) {
        return mob.entityTags().contains(KOM_EN_GA) && isWild(mob);
    }

    /** Makes this (new) animal a come-and-go one. */
    public static void markeer(Mob mob) {
        mob.addTag(KOM_EN_GA);
    }

    /** Is this spawn one that comes and goes (the natural spawner, a mob spawner block)? Chunk generation is not. */
    public static boolean komtEnGaat(EntitySpawnReason reason) {
        return reason == EntitySpawnReason.NATURAL || EntitySpawnReason.isSpawner(reason);
    }

    @SubscribeEvent
    public static void onFinalizeSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (komtEnGaat(event.getSpawnType()) && soorten().contains(mob.getType()) && !mob.isPersistenceRequired()) {
            markeer(mob);
        }
    }

    /**
     * A come-and-go animal despawns like a vanilla monster: at once when every player is more than its category's despawn
     * distance away (128 blocks, fish 64), now and then when it has been idle a while beyond 32 blocks. (Many of these
     * animals say {@code removeWhenFarAway false}, which is right for the ones the world is made with.)
     */
    @SubscribeEvent
    public static void onDespawnCheck(MobDespawnEvent event) {
        Mob mob = event.getEntity();
        if (event.getResult() != MobDespawnEvent.Result.DEFAULT || !isKomEnGa(mob)) {
            return;
        }
        Entity player = mob.level().getNearestPlayer(mob, -1.0);
        if (player == null) {
            return;
        }
        double d = player.distanceToSqr(mob);
        int dichtbij = mob.getType().getCategory().getNoDespawnDistance();
        if (magWeg(mob, d) || mob.getNoActionTime() > 600 && mob.getRandom().nextInt(800) == 0 && d > (double) dichtbij * dichtbij) {
            event.setResult(MobDespawnEvent.Result.ALLOW);
        }
    }

    /** Does this animal despawn at once, with the nearest player this far away (squared)? Only a come-and-go one does. */
    public static boolean magWeg(Mob mob, double afstandSqr) {
        int ver = mob.getType().getCategory().getDespawnDistance();
        return isKomEnGa(mob) && afstandSqr > (double) ver * ver;
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getGameTime() % OPRUIM_TIJD == 11) {
            tidyUp(level);
        }
    }

    /**
     * May the tidy-up take this one? A come-and-go animal, or a wild one that can't be yours in any way and would despawn
     * anyway: a bird (1.1.1 and older saved every top-up flock) or a mob that says removeWhenFarAway (the Mikas, the
     * flutter critters). Wild land and water animals from the world itself (chunk generation) are never taken.
     */
    public static boolean opruimbaar(Mob mob) {
        if (!soorten().contains(mob.getType()) || !isWild(mob)) {
            return false;
        }
        return mob.entityTags().contains(KOM_EN_GA) || mob instanceof Vogeltje || mob.removeWhenFarAway(Double.MAX_VALUE);
    }

    public static int max(ServerLevel level) {
        return MAX_BASIS + MAX_PER_SPELER * (int) level.players().stream().filter(p -> !p.isSpectator()).count();
    }

    /** The safety net (see the class comment). Returns how many went. */
    public static int tidyUp(ServerLevel level) {
        Map<EntityType<?>, List<Mob>> perSoort = new HashMap<>();
        for (Entity e : level.getAllEntities()) {
            if (e instanceof Mob mob && !mob.isRemoved() && opruimbaar(mob)) {
                perSoort.computeIfAbsent(mob.getType(), k -> new ArrayList<>()).add(mob);
            }
        }
        int max = max(level);
        int weg = 0;
        for (List<Mob> lijst : perSoort.values()) {
            weg += opruimen(lijst, level.players(), max);
        }
        return weg;
    }

    /**
     * Of these wild ones (one kind), all above {@code max} go that are more than {@link #VEILIG} blocks from every one of
     * these players, farthest first. Returns how many went.
     */
    public static int opruimen(List<? extends Mob> lijst, List<? extends Player> players, int max) {
        if (lijst.size() <= max) {
            return 0;
        }
        record Afstand(Mob mob, double d) {
        }
        List<Afstand> ver = new ArrayList<>();
        for (Mob mob : lijst) {
            double d = Double.MAX_VALUE;
            for (Player p : players) {
                if (!p.isSpectator()) {
                    d = Math.min(d, p.distanceToSqr(mob));
                }
            }
            if (d > VEILIG * VEILIG) {
                ver.add(new Afstand(mob, d));
            }
        }
        ver.sort(Comparator.comparingDouble(Afstand::d).reversed());
        int n = Math.min(lijst.size() - max, ver.size());
        for (int i = 0; i < n; i++) {
            ver.get(i).mob().discard();
        }
        return n;
    }

    private WildeDieren() {
    }
}
