package nl.juiced.guhs.feature.klusjes;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Band;
import nl.juiced.guhs.feature.band.BandVlaggen;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.HuisjeOpslag;
import nl.juiced.guhs.feature.huisje.Klus;
import nl.juiced.guhs.feature.huisje.KlusTaak;

/**
 * A chore as a little queue of steps ({@link Stap}): walk somewhere, work a while (looking at it, particles, sounds), do
 * the thing. What it collects ({@link #buit}) is carried (shown over its head) and brought to the Bank Guh / the chest
 * at the end ({@link HuisjeOpslag#lever}); if the chore is cut short (night, moved out...) the spoils are still delivered
 * straight away, nothing is ever lost. When the chore really did its job ({@link #gelukt()}) the resident is rewarded
 * once, at the end ({@link KlusBeloning#klaar}). While it runs the guh has the flag KLUSJE (the little carry icon).
 */
public abstract class StappenTaak implements KlusTaak {
    /** Marks the display copy in a working guh's hand (only for the look, never a real item). */
    public static final String TOON = "guhs_klusjes_toon";

    public interface Stap {
        /** One tick: true = this step is done. */
        boolean tick();
    }

    protected final ServerLevel level;
    protected final Huisje huisje;
    protected final Mob mob;
    protected final Klus klus;
    protected final List<ItemStack> buit = new ArrayList<>();
    private final Deque<Stap> stappen = new ArrayDeque<>();
    private final List<BlockPos> claims = new ArrayList<>();
    private boolean begonnen, gelukt, afgebroken, gebracht;
    /** Extra words for the reward (e.g. how many knabbels were dug up). */
    protected int aantal;

    protected StappenTaak(ServerLevel level, Huisje huisje, Mob mob, Klus klus) {
        this.level = level;
        this.huisje = huisje;
        this.mob = mob;
        this.klus = klus;
    }

    /** Puts the first steps in the queue (called on the first tick). */
    protected abstract void begin();

    protected final void erbij(Stap s) {
        stappen.addLast(s);
    }

    protected final void eerst(Stap s) {
        stappen.addFirst(s);
    }

    /** The chore did its job (rewarded at the end). */
    protected final void gelukt() {
        gelukt = true;
    }

    protected final boolean isGelukt() {
        return gelukt;
    }

    /** Stops the chore now (spoils are still delivered). */
    protected final void afbreken() {
        afgebroken = true;
    }

    /** Claims a target so no other resident takes it. */
    protected final void claim(BlockPos p, int ticks) {
        KlusGebied.claim(level, p, ticks);
        claims.add(p);
    }

    protected float snel() {
        return Band.klusSnelheid(mob);
    }

    @Override
    public boolean tick() {
        BandVlaggen.zet(mob, BandVlaggen.KLUSJE, true);
        if (!begonnen) {
            begonnen = true;
            begin();
        }
        if (!buit.isEmpty() && mob.tickCount % 20 == 0 && !isToon(mob.getMainHandItem())) {
            toon(buit.get(0));
        }
        if (!mob.isAlive() || afgebroken) {
            return false;
        }
        Stap s = stappen.peekFirst();
        if (s == null) {
            if (!buit.isEmpty() && !gebracht) {
                gebracht = true;
                erbij(breng());
                return true;
            }
            return false;
        }
        if (s.tick()) {
            stappen.remove(s);
        }
        return !afgebroken;
    }

    @Override
    public void stop() {
        for (BlockPos p : claims) {
            KlusGebied.vrij(level, p);
        }
        if (!buit.isEmpty()) {
            lever();                                      // (cut short: straight into the chest, nothing lost)
        }
        mob.getNavigation().stop();
        toon(ItemStack.EMPTY);
        BandVlaggen.zet(mob, BandVlaggen.KLUSJE, false);
        if (gelukt) {
            KlusBeloning.klaar(level, huisje, mob, klus, aantal);
        }
    }

    @Override
    public int maxTicks() {
        return 1600;
    }

    // =====================================================================================================================
    // steps
    // =====================================================================================================================

    /** Walks to a block until within {@code bereik} (horizontally) and 2.5 up/down; gives up after a while. */
    protected Stap loop(BlockPos doel, double bereik) {
        return new Stap() {
            int t;

            @Override
            public boolean tick() {
                t++;
                if (dichtbij(Vec3.atBottomCenterOf(doel), bereik)) {
                    mob.getNavigation().stop();
                    return true;
                }
                if (t == 1 || t % 20 == 0 || mob.getNavigation().isDone()) {
                    mob.getNavigation().moveTo(doel.getX() + 0.5, doel.getY(), doel.getZ() + 0.5, 1.0 * snel());
                }
                if (t > 400) {
                    if (dichtbij(Vec3.atBottomCenterOf(doel), bereik + 1.5)) {
                        return true;
                    }
                    afbreken();
                }
                return false;
            }
        };
    }

    /** Walks to an entity (it may move) until within {@code bereik}. */
    protected Stap loopNaar(Supplier<Entity> wie, double bereik) {
        return new Stap() {
            int t;

            @Override
            public boolean tick() {
                t++;
                Entity e = wie.get();
                if (e == null || !e.isAlive() || e.level() != mob.level()) {
                    afbreken();
                    return true;
                }
                if (mob.distanceToSqr(e) <= bereik * bereik) {
                    mob.getNavigation().stop();
                    return true;
                }
                if (t == 1 || t % 10 == 0) {
                    mob.getNavigation().moveTo(e, 1.15 * snel());
                }
                if (t > 400) {
                    afbreken();
                }
                return false;
            }
        };
    }

    /** Works on something for a while (faster when blij): looks at it, calls {@code elkeTick} with the tick number. */
    protected Stap werk(int ticks, Vec3 kijk, @Nullable IntConsumer elkeTick) {
        int duur = Math.max(4, Math.round(ticks / snel()));
        return new Stap() {
            int t;

            @Override
            public boolean tick() {
                mob.getNavigation().stop();
                mob.getLookControl().setLookAt(kijk.x, kijk.y, kijk.z);
                if (elkeTick != null) {
                    elkeTick.accept(t);
                }
                return ++t >= duur;
            }
        };
    }

    /** Does something once. */
    protected Stap doe(Runnable r) {
        return () -> {
            r.run();
            return true;
        };
    }

    /** Brings the spoils to the Bank Guh / the chest / the door. */
    private Stap breng() {
        BlockPos plek = Voorraad.brengPlek(level, huisje);
        Stap heen = loop(plek, 2.3);
        return new Stap() {
            boolean daar;

            @Override
            public boolean tick() {
                if (!daar) {
                    daar = heen.tick();
                    if (afgebroken) {
                        afgebroken = false;          // (couldn't get there: it delivers from where it stands)
                        daar = true;
                    }
                    return false;
                }
                mob.getLookControl().setLookAt(plek.getX() + 0.5, plek.getY() + 0.5, plek.getZ() + 0.5);
                lever();
                return true;
            }
        };
    }

    private void lever() {
        boolean bank = HuisjeOpslag.heeftBankGuh(level, huisje);
        boolean iets = false;
        for (ItemStack s : buit) {
            if (!s.isEmpty()) {
                HuisjeOpslag.lever(level, huisje, s);
                iets = true;
            }
        }
        buit.clear();
        toon(ItemStack.EMPTY);
        if (iets) {
            BlockPos p = Voorraad.brengPlek(level, huisje);
            level.sendParticles(KlusjesFeature.STERRETJE.get(), p.getX() + 0.5, p.getY() + 1.1, p.getZ() + 0.5, 5, 0.3, 0.2, 0.3, 0.0);
            level.playSound(null, p, SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.5f, 1.4f);
            if (bank) {
                KlusBeloning.bankGesorteerd(mob);
            }
        }
    }

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    protected boolean dichtbij(Vec3 doel, double bereik) {
        double dx = doel.x - mob.getX(), dz = doel.z - mob.getZ();
        return dx * dx + dz * dz <= bereik * bereik && Math.abs(doel.y - mob.getY()) <= 2.5;
    }

    /** Adds to the spoils (merged) and shows it over its head. */
    protected void pak(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        for (ItemStack b : buit) {
            if (ItemStack.isSameItemSameComponents(b, stack) && b.getCount() < b.getMaxStackSize()) {
                int n = Math.min(stack.getCount(), b.getMaxStackSize() - b.getCount());
                b.grow(n);
                stack = stack.copyWithCount(stack.getCount() - n);
                if (stack.isEmpty()) {
                    break;
                }
            }
        }
        if (!stack.isEmpty()) {
            buit.add(stack.copy());
        }
        toon(buit.get(0));
    }

    /** Picks up the items lying within r of a spot (what a harvest just dropped). */
    protected int raapOp(Vec3 bij, double r) {
        int n = 0;
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(bij, bij).inflate(r),
                i -> i.isAlive() && !i.getItem().isEmpty())) {
            ItemStack s = item.getItem().copy();
            n += s.getCount();
            pak(s);
            item.discard();
        }
        if (n > 0) {
            level.playSound(null, mob.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.4f, 1.6f);
        }
        return n;
    }

    /** The little icon over its head (a display copy in its hand; guhs only). */
    protected void toon(ItemStack stack) {
        if (!(mob instanceof GuhEntity)) {
            return;
        }
        if (stack.isEmpty()) {
            if (isToon(mob.getMainHandItem())) {
                mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
            return;
        }
        ItemStack copy = stack.copyWithCount(1);
        CompoundTag tag = copy.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putBoolean(TOON, true);
        copy.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        if (mob.getMainHandItem().isEmpty() || isToon(mob.getMainHandItem())) {
            mob.setDropChance(EquipmentSlot.MAINHAND, 0f);
            mob.setItemSlot(EquipmentSlot.MAINHAND, copy);
        }
    }

    public static boolean isToon(ItemStack s) {
        return !s.isEmpty() && s.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).contains(TOON);
    }

    /** A guh that was saved in the middle of a chore: no flag, no display item left over. */
    public static void opruimenNaLaden(GuhEntity guh) {
        if (BandVlaggen.heeft(guh, BandVlaggen.KLUSJE) && !nl.juiced.guhs.feature.knus.GuhHooks.isBezig(guh)) {
            BandVlaggen.zet(guh, BandVlaggen.KLUSJE, false);
        }
        if (!BandVlaggen.heeft(guh, BandVlaggen.KLUSJE) && isToon(guh.getMainHandItem())) {
            guh.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    /** Happy little particles at a spot. */
    protected void sprankel(Vec3 p, int n) {
        level.sendParticles(KlusjesFeature.STERRETJE.get(), p.x, p.y, p.z, n, 0.3, 0.25, 0.3, 0.0);
    }

    protected void hartjes(Entity e, int n) {
        level.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + e.getBbHeight() + 0.2, e.getZ(), n, 0.3, 0.2, 0.3, 0.0);
    }
}
