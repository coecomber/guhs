package nl.juiced.guhs.feature.klusjes;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.feature.vissen.VissenFeature;
import nl.juiced.guhs.registry.ModItems;

/**
 * Vissen: when there is a pond in the home base (at least {@link #MIN_WATER} water sources), the resident sits down at
 * the edge, dips its paws (or, for Schilly and Poepschilly, its whole little shell) in the water, waits for a bite (bubbles,
 * a plop now and then) and fishes up guhvissen and schelpjes (loot table guhs:gameplay/klusjes_vissen), very rarely a
 * Gouden Guhvis. Guhs, Schilly and Poepschilly.
 */
public class VissenKlus extends BasisKlus {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Guhs.id("gameplay/klusjes_vissen"));
    public static final int MIN_WATER = 3;
    public static final int VIS_TICKS = 90;

    VissenKlus() {
        super("vissen", () -> new ItemStack(ModItems.GUH_VIS.get()), 600);
    }

    @Override
    public boolean kan(Mob bewoner) {
        return isGuh(bewoner) || isSchildpad(bewoner);
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        if (KlusGebied.van(level, huisje, KlusGebied.Soort.WATER).size() < MIN_WATER) {
            return null;
        }
        BlockPos water = KlusGebied.kies(level, huisje, KlusGebied.Soort.WATER, bewoner, true,
                p -> KlusGebied.viswater(level, p, level.getBlockState(p)) && oever(level, p) != null);
        if (water == null) {
            return null;
        }
        return new Taak(level, huisje, bewoner, water, oever(level, water));
    }

    /** A spot on the bank next to this water to sit on (air with something solid below), or null. */
    @Nullable
    static BlockPos oever(ServerLevel level, BlockPos water) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos p = water.above().relative(d);
            if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
                    && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP)) {
                return p;
            }
        }
        return null;
    }

    public static List<ItemStack> vangst(ServerLevel level, Mob wie, BlockPos water) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(LOOT);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(water))
                .withOptionalParameter(LootContextParams.THIS_ENTITY, wie).create(LootContextParamSets.CHEST);
        long seed = KlusjesFeature.SALT * 7L ^ wie.getUUID().getMostSignificantBits() ^ (level.getGameTime() * 131L) ^ water.asLong();
        return table.getRandomItems(params, seed);
    }

    private class Taak extends StappenTaak {
        private final BlockPos water;
        private final BlockPos oever;

        Taak(ServerLevel level, Huisje huisje, Mob mob, BlockPos water, BlockPos oever) {
            super(level, huisje, mob, VissenKlus.this);
            this.water = water;
            this.oever = oever;
        }

        @Override
        protected void begin() {
            claim(water, 800);
            erbij(loop(oever, 1.1));
            erbij(doe(() -> toon(new ItemStack(VissenFeature.GUHVIS_HENGEL.get()))));
            Vec3 w = new Vec3(water.getX() + 0.5, water.getY() + 0.9, water.getZ() + 0.5);
            int duur = VIS_TICKS + mob.getRandom().nextInt(60);
            erbij(werk(duur, w, t -> {
                if (t % 12 == 0) {
                    level.sendParticles(ParticleTypes.BUBBLE_POP, w.x, w.y + 0.1, w.z, 3, 0.25, 0.02, 0.25, 0.01);
                }
                if (t % 40 == 20) {
                    level.sendParticles(ParticleTypes.SPLASH, w.x, w.y + 0.1, w.z, 6, 0.2, 0.02, 0.2, 0.05);
                    level.playSound(null, water, KlusjesFeature.PLONS.get(), SoundSource.NEUTRAL, 0.35f, 1.3f);
                }
            }));
            erbij(doe(this::beet));
        }

        private void beet() {
            level.sendParticles(ParticleTypes.SPLASH, water.getX() + 0.5, water.getY() + 1.0, water.getZ() + 0.5, 20, 0.3, 0.05, 0.3, 0.1);
            level.playSound(null, water, SoundEvents.FISHING_BOBBER_RETRIEVE, SoundSource.NEUTRAL, 0.7f, 1.2f);
            ItemStack zeldzaam = ItemStack.EMPTY;
            for (ItemStack s : vangst(level, mob, water)) {
                if (s.is(KlusjesFeature.ZELDZAAM) && zeldzaam.isEmpty()) {
                    zeldzaam = s.copy();
                }
                aantal += s.getCount();
                pak(s);
            }
            if (!zeldzaam.isEmpty()) {
                KlusBeloning.zeldzaam(mob, huisje, zeldzaam);
                sprankel(mob.position().add(0, mob.getBbHeight() + 0.3, 0), 12);
                if (mob instanceof GuhEntity g) {
                    g.emotes.start(Emote.VAHOEG, false, GuhEmotes.Source.SELF);
                }
            }
            gelukt();
        }
    }
}
