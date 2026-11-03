package nl.juiced.guhs.feature.vadsrijk.kern.loot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Tips;

/**
 * Loot in the world: personal drops (only the owner can pick them up) that pop out of a defeated mob, with a light beam in the
 * rarity colour (rarer = taller; Guhddelijk: pink glitter + its own sound). The coins go straight into the owner's purse.
 */
public final class LootBundel {
    private static final List<ItemEntity> BUIT = new ArrayList<>();

    private LootBundel() {
    }

    /** Drops this result for {@code p} at {@code pos} (items pop up and out a little). */
    public static void laatVallen(ServerPlayer p, Vec3 pos, LootTabel.Resultaat r, int extraHoog) {
        if (r.munten() > 0) {
            Spelers.geefMunten(p, r.munten());
        }
        if (!(p.level() instanceof ServerLevel sl)) {
            return;
        }
        for (ItemStack s : r.items()) {
            ItemEntity e = new ItemEntity(sl, pos.x, pos.y, pos.z, s);
            e.setDeltaMovement((sl.getRandom().nextDouble() - 0.5) * 0.25, 0.35 + 0.05 * extraHoog, (sl.getRandom().nextDouble() - 0.5) * 0.25);
            e.setTarget(p.getUUID());
            e.setPickUpDelay(15);
            e.setUnlimitedLifetime();
            sl.addFreshEntity(e);
            BUIT.add(e);
            Uitrusting u = Rollen.van(s);
            if (u != null && u.zeldzaamheid() == Zeldzaamheid.GUHDDELIJK) {
                sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1f, 1.6f);
                sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1f, 1.2f);
            } else if (u != null && u.zeldzaamheid().ordinal() >= Zeldzaamheid.EPISCH.ordinal()) {
                sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1f, 1.2f);
            }
            Haken.LOOT.forEach(h -> h.accept(p, s));
            Tips.toon(p, "vadsrijk.kern.tip.loot");
        }
    }

    /** Every 5 ticks: the beams. */
    public static void tick() {
        Iterator<ItemEntity> it = BUIT.iterator();
        while (it.hasNext()) {
            ItemEntity e = it.next();
            if (e.isRemoved() || !(e.level() instanceof ServerLevel sl)) {
                it.remove();
                continue;
            }
            Uitrusting u = Rollen.van(e.getItem());
            if (u == null) {
                continue;
            }
            Zeldzaamheid z = u.zeldzaamheid();
            int hoogte = 2 + z.ordinal() * 2;
            DustParticleOptions stof = new DustParticleOptions(z.rgb, 1.4f);
            for (int i = 0; i < hoogte; i++) {
                sl.sendParticles(stof, e.getX(), e.getY() + 0.3 + i * 0.5, e.getZ(), 1, 0.03, 0.1, 0.03, 0);
            }
            if (z == Zeldzaamheid.GUHDDELIJK) {
                sl.sendParticles(ParticleTypes.END_ROD, e.getX(), e.getY() + 1, e.getZ(), 2, 0.4, 1.2, 0.4, 0.01);
                sl.sendParticles(ParticleTypes.CHERRY_LEAVES, e.getX(), e.getY() + 2, e.getZ(), 1, 0.5, 0.5, 0.5, 0.01);
            }
        }
    }

    public static Component naam(ItemStack s) {
        return s.getHoverName();
    }
}
