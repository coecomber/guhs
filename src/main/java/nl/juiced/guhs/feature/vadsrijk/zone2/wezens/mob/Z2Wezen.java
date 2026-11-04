package nl.juiced.guhs.feature.vadsrijk.zone2.wezens.mob;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabellen;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.wezens.mob.RijkWezen;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.Verslagen;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.Z2WezensPlekken;
import nl.juiced.guhs.feature.vadsrijk.zone2.wezens.loot.Z2Loot;

/**
 * The base of every zone-2 creature ("De Splinterpieken", lv 20-30): real monsters, no pink plof. On top of the zone-1
 * {@link RijkWezen} (GeckoLib anims, elites, split copies, tips):
 * <ul>
 *   <li>{@link #aard()}: KRISTAL shatters into grey shards that melt away, MIKA flees in a smoke puff, BEEST sinks into snow dust
 *       ({@link Verslagen}); the body vanishes at once (no hops, no hearts);</li>
 *   <li>{@link #versteend()}: grey crystal = kern's "ontvadst" flag, so Roze hits it x1.5 (no new element);</li>
 *   <li>{@link #verborgen()}: hidden (under the ice, burrowed in the snow, invisible): no nameplate, not hittable, not pushable;</li>
 *   <li>loot: its table "z2wezens:&lt;naam&gt;" (+ "_elite") and sometimes a raw crystal for sockets ({@link Z2Loot#kristal});</li>
 *   <li>safe spots ({@link Z2WezensPlekken#VEILIG}): a spawned creature that wanders into one forgets its target and walks back.</li>
 * </ul>
 */
public abstract class Z2Wezen extends RijkWezen {
    public enum Aard {
        KRISTAL, MIKA, BEEST
    }

    /** Held poses used by several creatures (RijkWezen.HOUDING). */
    public static final int VERBORGEN = 9, TELEGRAAF = 8;

    protected Z2Wezen(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    /** The name used for loot tables, tips and textures. */
    public abstract String naam();

    public abstract Aard aard();

    /** Grey crystal ("versteend"): Roze does x1.5. */
    public boolean versteend() {
        return false;
    }

    /** Hidden right now (no nameplate, not hittable). Default: the VERBORGEN pose. */
    public boolean verborgen() {
        return houding() == VERBORGEN;
    }

    static double groei(double basis, int level, double perLevel) {
        return basis * (1 + perLevel * (level - 1));
    }

    /** The zone-1 curve (HP base x (1 + 0.3/level)); zone 2 adds no other factor, the levels do the work. */
    protected static double vads(double basis, int level) {
        return groei(basis, level, 0.3);
    }

    protected static double schade(double basis, int level) {
        return groei(basis, level, 0.22);
    }

    @Nullable
    @Override
    public String lootTabel() {
        return "z2wezens:" + naam();
    }

    @Override
    protected String tip() {
        return naam();
    }

    @Override
    protected SoundEvent piepGeluid() {
        return aard() == Aard.KRISTAL ? SoundEvents.AMETHYST_BLOCK_HIT : aard() == Aard.MIKA ? SoundEvents.OCELOT_HURT : SoundEvents.POLAR_BEAR_WARNING;
    }

    @Override
    public void stelIn(int level, @Nullable Element element, boolean elite, Set<Affix> affixen) {
        super.stelIn(level, element, elite, affixen);
        zetOntvadst(versteend());
    }

    protected Vec3 voor() {
        double a = Math.toRadians(getYRot());
        return new Vec3(-Math.sin(a), 0, Math.cos(a));
    }

    @Override
    public boolean isPickable() {
        return !verborgen() && !geplofd() && super.isPickable();
    }

    @Override
    public boolean isPushable() {
        return !verborgen() && super.isPushable();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !(level() instanceof ServerLevel sl)) {
            return;
        }
        if (geplofd()) {
            // no happy hops: the body is gone (shards / smoke / dust were the defeat), kern discards later; we go sooner
            setDeltaMovement(0, Math.min(0, getDeltaMovement().y), 0);
            if (plofTicks() >= 4) {
                discard();
            }
            return;
        }
        if (tickCount % 20 == 5 && !spawner.isEmpty() && !spawner.startsWith("z2wezens:burcht")
                && Z2WezensPlekken.veilig(getX(), getZ(), 0)) {
            // a safe spot (campfire, town, NPC): forget the fight and go home
            setTarget(null);
            Spawners.Spawner s = Spawners.van(spawner);
            if (s != null) {
                getNavigation().moveTo(s.centrum().getX() + 0.5, getY(), s.centrum().getZ() + 0.5, 1.2);
            } else if (tickCount > 40) {
                discard();
            }
        }
        if (tickCount % 20 == 0 && getTarget() instanceof ServerPlayer p && distanceTo(p) < 12 && versteend()) {
            Tips.toon(p, "vadsrijk.z2wezens.tip.versteend");
        }
    }

    // ---- defeat --------------------------------------------------------------------------------------------------------

    @Override
    protected void beloon(ServerLevel sl, Set<ServerPlayer> helpers) {
        String id = lootTabel();
        LootTabel t = id == null || klein() ? null : elite() && LootTabellen.van(id + "_elite") != null ? LootTabellen.van(id + "_elite") : LootTabellen.van(id);
        int xp = (int) Math.round(xp() * (elite() ? 3 : 1) * (klein() ? 0.4 : 1));
        for (ServerPlayer p : helpers) {
            Spelers.geefXpGedeeld(p, xp, "mob");
            Spelers.data(p).opgevrolijkt++;
            if (t == null) {
                continue;
            }
            LootTabel.Resultaat r = t.rol(mobLevel(), getRandom());
            List<ItemStack> items = new ArrayList<>(r.items());
            ItemStack k = Z2Loot.kristal(this, getRandom());
            if (!k.isEmpty()) {
                items.add(k);
            }
            LootBundel.laatVallen(p, position().add(0, 0.5, 0), new LootTabel.Resultaat(items, r.munten()), elite() ? 1 : 0);
        }
    }

    /** What a Mika shouts when it flees (a key under vadsrijk.z2wezens.), or null. */
    @Nullable
    protected String vluchtRoep() {
        return null;
    }

    @Override
    protected void opPlof() {
        // (no super: kern's and RijkWezen's plof are pink and happy; zone 2 is not)
        if (!(level() instanceof ServerLevel sl)) {
            return;
        }
        float b = getBbWidth(), h = getBbHeight();
        switch (aard()) {
            case KRISTAL -> Verslagen.scherven(sl, position(), b, h);
            case MIKA -> Verslagen.rookpluim(sl, position(), h, Z2WezensPlekken.MIKA_FORT,
                    vluchtRoep() == null ? null : Component.translatable("vadsrijk.z2wezens." + vluchtRoep() + "." + getRandom().nextInt(3)));
            case BEEST -> Verslagen.stof(sl, position(), b, h);
        }
        setInvisible(true);
        if (affixen.contains(Affix.SPLITSEND) && !klein()) {
            for (int i = 0; i < 2; i++) {
                splits(sl, getType(), Math.max(1, mobLevel() - 1), true, i);
            }
        }
    }

    // ---- helpers for the creatures ---------------------------------------------------------------------------------------

    /** The usual ground goals: melee at this speed, stroll, look; alarm = a hit alerts the same kind nearby. */
    protected void standaardDoelen(double snelheid, boolean alarm) {
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        this.goalSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, snelheid, true));
        this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new net.minecraft.world.entity.ai.goal.LookAtPlayerGoal(this, net.minecraft.world.entity.player.Player.class, 10f));
        this.goalSelector.addGoal(7, new net.minecraft.world.entity.ai.goal.RandomLookAroundGoal(this));
        var h = new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this);
        this.targetSelector.addGoal(1, alarm ? h.setAlertOthers() : h);
        this.targetSelector.addGoal(2, new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,
                net.minecraft.world.entity.player.Player.class, true));
    }

    /** A hit on every player within r of a point (factor on the normal hit). */
    protected int klapRond(Vec3 midden, double r, double factor, Schade.Soort soort, String skill) {
        int n = 0;
        for (var p : spelersRond(midden, r)) {
            klap(p, factor, soort, skill);
            n++;
        }
        return n;
    }

    /** The ground height under a point (first floor from 6 above down 30), or the point's own y. */
    protected double grond(Vec3 v) {
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int x = net.minecraft.util.Mth.floor(v.x), z = net.minecraft.util.Mth.floor(v.z);
        for (int y = net.minecraft.util.Mth.floor(v.y) + 6; y > v.y - 30; y--) {
            m.set(x, y - 1, z);
            if (!level().getBlockState(m).getCollisionShape(level(), m).isEmpty()) {
                return y;
            }
        }
        return v.y;
    }

    /** Is this living thing a valid target still (alive, not creative)? */
    protected static boolean geldig(@Nullable LivingEntity l) {
        return l != null && l.isAlive() && !(l instanceof net.minecraft.world.entity.player.Player p && (p.isCreative() || p.isSpectator()));
    }
}
