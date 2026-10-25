package nl.juiced.guhs.feature.techklus;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.techmachine.PlantagebakBlockEntity;

/**
 * (1.7.0, ronde 45-47 "Vadskracht in de structuren") The klusjes-guhs of a building: the Plantagebak of a structure (no
 * owner: the structure placed it, like the Guhboerderij's plantage) has no huisje whose residents chop its tree, so the
 * building's own guhs do it. A guh with the tag {@link #TAG} (set in the template) within {@link #BEREIK} blocks of such a bak
 * walks to the bak when its tree stands, swings its little axe for {@link #HAK_TICKS} ticks and the whole tree comes down
 * ({@link PlantagebakBlockEntity#hak}); the harvest goes into the nearest chest within {@link #KIST_BEREIK} blocks of the bak
 * (what does not fit falls on the ground), the saplings stay in the bak, which plants the next tree by itself.
 * <p>
 * The bak asks for a guh itself ({@link #roep}, from its own tick while its tree stands); one guh works one bak at a time,
 * a chore that takes longer than {@link #MAX_TICKS} is given up. Nothing is saved: a chore cut short by a restart is
 * simply asked for again.
 */
public final class GebouwKlus {
    /** The tag of a building's klusjes-guh (entity Tags in the template). */
    public static final String TAG = "guhs_gebouwklus";
    public static final int BEREIK = 12, KIST_BEREIK = 4, HAK_TICKS = 50, MAX_TICKS = 600;
    /** How often (in ticks) a bak with a tree asks again. */
    private static final int ROEP_ELKE = 40;

    private static final class Taak {
        final ServerLevel level;
        final BlockPos kern;
        final UUID guh;
        int ticks, hak = -1;

        Taak(ServerLevel level, BlockPos kern, UUID guh) {
            this.level = level;
            this.kern = kern;
            this.guh = guh;
        }
    }

    /** key: the bak's kern (per level: level id + pos). */
    private static final Map<String, Taak> TAKEN = new ConcurrentHashMap<>();

    static void register() {
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> tik(event.getServer()));
    }

    private static String sleutel(ServerLevel level, BlockPos kern) {
        return level.dimension().identifier() + "@" + kern.asLong();
    }

    /** Is a building's guh chopping (or on its way to) this bak? */
    public static boolean bezig(ServerLevel level, BlockPos kern) {
        return TAKEN.containsKey(sleutel(level, kern));
    }

    /** A building's bak with a tree on it asks for one of the building's guhs (called from the bak's tick). */
    public static void roep(ServerLevel level, PlantagebakBlockEntity bak) {
        if (bak.eigenaar() != null || !bak.heeftBoom() || (level.getGameTime() + bak.getBlockPos().hashCode()) % ROEP_ELKE != 0) {
            return;
        }
        BlockPos kern = bak.getBlockPos();
        String k = sleutel(level, kern);
        if (TAKEN.containsKey(k)) {
            return;
        }
        GuhEntity guh = kies(level, kern);
        if (guh != null) {
            TAKEN.put(k, new Taak(level, kern.immutable(), guh.getUUID()));
        }
    }

    /** The nearest free klusjes-guh of the building. */
    @Nullable
    private static GuhEntity kies(ServerLevel level, BlockPos kern) {
        List<UUID> bezet = new ArrayList<>();
        for (Taak t : TAKEN.values()) {
            bezet.add(t.guh);
        }
        GuhEntity beste = null;
        double afstand = Double.MAX_VALUE;
        for (GuhEntity g : level.getEntitiesOfClass(GuhEntity.class, new AABB(kern).inflate(BEREIK),
                g -> g.isAlive() && g.entityTags().contains(TAG) && !bezet.contains(g.getUUID()))) {
            double d = g.distanceToSqr(Vec3.atCenterOf(kern));
            if (d < afstand) {
                afstand = d;
                beste = g;
            }
        }
        return beste;
    }

    private static void tik(MinecraftServer server) {
        for (Iterator<Map.Entry<String, Taak>> it = TAKEN.entrySet().iterator(); it.hasNext(); ) {
            Taak t = it.next().getValue();
            if (!stap(t)) {
                Entity e = t.level.getEntity(t.guh);
                if (e instanceof GuhEntity g) {
                    g.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                }
                it.remove();
            }
        }
    }

    /** One tick of a chore; false = done (or given up). */
    private static boolean stap(Taak t) {
        ServerLevel level = t.level;
        if (++t.ticks > MAX_TICKS || !level.isLoaded(t.kern) || !(level.getBlockEntity(t.kern) instanceof PlantagebakBlockEntity bak)
                || !bak.heeftBoom() || !(level.getEntity(t.guh) instanceof GuhEntity guh) || !guh.isAlive()) {
            return false;
        }
        BlockPos voor = t.kern.relative(bak.voor());
        Vec3 stam = Vec3.atCenterOf(bak.plantPlek());
        if (t.hak < 0) {
            if (guh.distanceToSqr(Vec3.atBottomCenterOf(voor)) > 2.5 * 2.5) {
                if (t.ticks % 20 == 1) {
                    guh.getNavigation().moveTo(voor.getX() + 0.5, voor.getY(), voor.getZ() + 0.5, 1.0);
                }
                return true;
            }
            guh.getNavigation().stop();
            guh.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
            t.hak = 0;
        }
        guh.getLookControl().setLookAt(stam.x, stam.y, stam.z);
        if (++t.hak % 10 == 0) {
            guh.swing(InteractionHand.MAIN_HAND);
            level.playSound(null, t.kern, SoundEvents.WOOD_HIT, SoundSource.NEUTRAL, 0.8f, 0.9f + guh.getRandom().nextFloat() * 0.3f);
            BlockState hout = level.getBlockState(BlockPos.containing(stam));
            if (!hout.isAir()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, hout), stam.x, stam.y, stam.z, 6, 0.3, 0.3, 0.3, 0.05);
            }
        }
        if (t.hak < HAK_TICKS) {
            return true;
        }
        for (ItemStack buit : bak.hak(guh)) {
            ItemStack rest = berg(level, t.kern, buit);
            if (!rest.isEmpty()) {
                Block.popResource(level, voor, rest);
            }
        }
        return false;
    }

    /** Into the nearest chest (or barrel) by the bak; returns what did not fit. */
    private static ItemStack berg(ServerLevel level, BlockPos kern, ItemStack stack) {
        BlockPos beste = null;
        double afstand = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(kern.offset(-KIST_BEREIK, -1, -KIST_BEREIK), kern.offset(KIST_BEREIK, 2, KIST_BEREIK))) {
            if (level.getBlockEntity(p) instanceof Container && !(level.getBlockEntity(p) instanceof PlantagebakBlockEntity)
                    && (level.getBlockState(p).getBlock() instanceof ChestBlock || level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.BARREL))) {
                double d = p.distSqr(kern);
                if (d < afstand) {
                    afstand = d;
                    beste = p.immutable();
                }
            }
        }
        if (beste == null) {
            return stack;
        }
        Container kist = level.getBlockState(beste).getBlock() instanceof ChestBlock chest
                ? ChestBlock.getContainer(chest, level.getBlockState(beste), level, beste, true)
                : (Container) level.getBlockEntity(beste);
        return kist == null ? stack : HopperBlockEntity.addItem(null, kist, stack, Direction.UP);
    }

    private GebouwKlus() {
    }
}
