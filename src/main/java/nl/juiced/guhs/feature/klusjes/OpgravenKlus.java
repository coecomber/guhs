package nl.juiced.guhs.feature.klusjes;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.band.Dagboek;
import nl.juiced.guhs.feature.band.DagboekStat;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.KlusTaak;
import nl.juiced.guhs.registry.ModItems;

/**
 * Kaasknabbels opgraven: the resident trots to a patch of grass, dirt or sand in the home base, digs with its little paws
 * (crumbs flying, scritch scritch) and finds kaasknabbels (loot table guhs:gameplay/klusjes_opgraven), sometimes something
 * rare (#guhs:klusjes/zeldzaam: a guhkristal, a schelpje, gefrituurde knabbels...). The ground stays as it was. Guhs and
 * pieppiepmuisjes (they love digging). Counts KNABBELS_OPGEGRAVEN in the dagboek.
 */
public class OpgravenKlus extends BasisKlus {
    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE, Guhs.id("gameplay/klusjes_opgraven"));
    public static final int GRAAF_TICKS = 50;

    OpgravenKlus() {
        super("opgraven", () -> new ItemStack(Items.WOODEN_SHOVEL), 1200);
    }

    @Override
    public boolean kan(Mob bewoner) {
        return isGuh(bewoner) || isMuisje(bewoner);
    }

    @Nullable
    @Override
    public KlusTaak zoek(ServerLevel level, Huisje huisje, Mob bewoner) {
        BlockPos plek = KlusGebied.kies(level, huisje, KlusGebied.Soort.GRAAF, bewoner, true,
                p -> KlusGebied.graafbaar(level, p, level.getBlockState(p)));
        return plek == null ? null : new Taak(level, huisje, bewoner, plek);
    }

    /** What digging here brings up (the loot table, rolled with the chores' salt). */
    public static List<ItemStack> vondst(ServerLevel level, Mob wie, BlockPos plek) {
        LootTable table = level.getServer().reloadableRegistries().getLootTable(LOOT);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(plek))
                .withOptionalParameter(LootContextParams.THIS_ENTITY, wie).create(LootContextParamSets.CHEST);
        long seed = KlusjesFeature.SALT ^ wie.getUUID().getLeastSignificantBits() ^ (level.getGameTime() * 31L) ^ plek.asLong();
        return table.getRandomItems(params, seed);
    }

    private class Taak extends StappenTaak {
        private final BlockPos plek;

        Taak(ServerLevel level, Huisje huisje, Mob mob, BlockPos plek) {
            super(level, huisje, mob, OpgravenKlus.this);
            this.plek = plek;
        }

        @Override
        protected void begin() {
            claim(plek, 600);
            erbij(loop(plek.above(), 1.2));
            BlockState grond = level.getBlockState(plek);
            Vec3 gat = new Vec3(plek.getX() + 0.5, plek.getY() + 1.02, plek.getZ() + 0.5);
            erbij(doe(() -> toon(new ItemStack(Items.WOODEN_SHOVEL))));
            erbij(werk(GRAAF_TICKS, gat, t -> {
                if (t % 6 == 0) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, grond), gat.x, gat.y, gat.z, 6, 0.2, 0.05, 0.2, 0.08);
                    level.playSound(null, plek, KlusjesFeature.GRAAF.get(), SoundSource.NEUTRAL, 0.5f, 0.9f + mob.getRandom().nextFloat() * 0.3f);
                }
            }));
            erbij(doe(this::gevonden));
        }

        private void gevonden() {
            int knabbels = 0;
            ItemStack zeldzaam = ItemStack.EMPTY;
            for (ItemStack s : vondst(level, mob, plek)) {
                if (s.is(ModItems.KAAS_KNABBELS.get())) {
                    knabbels += s.getCount();
                }
                if (s.is(KlusjesFeature.ZELDZAAM) && zeldzaam.isEmpty()) {
                    zeldzaam = s.copy();
                }
                pak(s);
            }
            if (knabbels > 0) {
                Dagboek.tel(mob, DagboekStat.KNABBELS_OPGEGRAVEN, knabbels);
            }
            aantal = knabbels;
            sprankel(new Vec3(plek.getX() + 0.5, plek.getY() + 1.3, plek.getZ() + 0.5), zeldzaam.isEmpty() ? 3 : 12);
            if (!zeldzaam.isEmpty()) {
                KlusBeloning.zeldzaam(mob, huisje, zeldzaam);
                if (mob instanceof GuhEntity g) {
                    g.emotes.start(Emote.VAHOEG, false, GuhEmotes.Source.SELF);
                }
            }
            gelukt();
        }
    }
}
