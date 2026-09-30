package nl.juiced.guhs.feature.beroepen;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.beroepen.BeroepenVoortgang.Beroep;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * Bob de Guhbouwer (BOUWVAKKERGUH), by his half-built house in a guh village (layout_c). "Kunnen wij het maken? JA, WIJ
 * KUNNEN HET!" The job (once per player):
 * <ol>
 *   <li>Talk to him: his roof isn't finished. Bring {@link #PLANKEN} planks (any wood) for the dakbalken and
 *   {@link #LUNCH} kaasknabbels for his lunch - a bouwvakker who doesn't eat never gets vahoeg (step 1).</li>
 *   <li>Bring them: he takes them and hands you the dakpannen (loaned, one per ghost tile) (step 2).</li>
 *   <li>Climb the ladder, walk up the roof steps and lay a dakpan on every see-through ghost tile ({@link DakpanItem}).
 *   The last one: the flag goes up (de vlag in top!) and the job is done: the builder's helmet and the safety vest.</li>
 * </ol>
 * When someone new starts while the roof is finished, the Mika's turn out to have "borrowed" all the tiles in the night,
 * giggling: the ghost tiles are back. roleData: Plekken (the roof spots), Vlag (the flag's spot).
 */
public final class Bouw implements NpcRole {
    public static final Bouw ROLE = new Bouw();
    public static final Beroep BEROEP = Beroep.BOUW;
    public static final int PLANKEN = 16, LUNCH = 8, BEREIK = 14;

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 0.85f);
        if (BeroepenVoortgang.klaar(player, BEROEP)) {
            GuhQuests.say(player, npc, "quest.guhs.beroepen.bouw.bedankt" + player.getRandom().nextInt(3));
            return;
        }
        List<BlockPos> plekken = plekken(npc);
        if (plekken.isEmpty()) {
            GuhQuests.say(player, npc, "quest.guhs.beroepen.bouw.niks");
            return;
        }
        int stap = BeroepenVoortgang.stap(player, BEROEP);
        if (stap == 0) {
            if (open(npc) == 0) {
                sloop(npc);
                GuhQuests.say(player, npc, "quest.guhs.beroepen.bouw.gepikt");
            }
            BeroepenVoortgang.zet(player, BEROEP, 1);
            GuhQuests.say(player, npc, "quest.guhs.beroepen.bouw.start", PLANKEN, LUNCH);
        } else if (stap == 1) {
            int planken = planken(player), knabbels = GuhQuests.count(player, ModItems.KAAS_KNABBELS.get());
            if (planken >= PLANKEN && knabbels >= LUNCH) {
                neemPlanken(player, PLANKEN);
                GuhQuests.take(player, ModItems.KAAS_KNABBELS.get(), LUNCH);
                BeroepenVoortgang.zet(player, BEROEP, 2);
                if (open(npc) == 0) {
                    sloop(npc);
                }
                geefPannen(player, open(npc));
                npc.level().playSound(null, npc.blockPosition(), BeroepenFeature.HAMER.get(), SoundSource.NEUTRAL, 1f, 1f);
                GuhQuests.say(player, npc, "quest.guhs.beroepen.bouw.materiaal", open(npc));
            } else {
                GuhQuests.say(player, npc, "quest.guhs.beroepen.bouw.nodig", Math.min(planken, PLANKEN), PLANKEN, Math.min(knabbels, LUNCH), LUNCH);
            }
        } else {
            int open = open(npc);
            if (open == 0) {
                vlag(npc);
                GuhQuests.say(player, npc, "quest.guhs.beroepen.bouw.af");
                BeroepenVoortgang.rondAf(player, BEROEP, npc);
            } else {
                int heb = GuhQuests.count(player, BeroepenFeature.DAKPAN_ITEM.get());
                if (heb < open) {
                    geefPannen(player, open - heb);
                }
                GuhQuests.say(player, npc, "quest.guhs.beroepen.bouw.nog", open);
            }
        }
    }

    static int planken(ServerPlayer player) {
        int n = 0;
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
            if (s.is(ItemTags.PLANKS)) {
                n += s.getCount();
            }
        }
        return n;
    }

    static void neemPlanken(ServerPlayer player, int n) {
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
            if (n > 0 && s.is(ItemTags.PLANKS)) {
                int take = Math.min(n, s.getCount());
                s.shrink(take);
                n -= take;
            }
        }
    }

    static void geefPannen(ServerPlayer player, int n) {
        if (n > 0) {
            Minigames.give(player, new ItemStack(BeroepenFeature.DAKPAN_ITEM.get(), n));
        }
    }

    /** The roof spots round Bob (ghost tiles or laid tiles; found once, remembered). */
    public static List<BlockPos> plekken(GuhNpcEntity npc) {
        if (!npc.roleData.contains("Plekken")) {
            List<BlockPos> ps = BeroepenHulp.zoek((ServerLevel) npc.level(), npc.blockPosition(), BEREIK, 4, 14,
                    s -> s.is(BeroepenFeature.DAKPLEK.get()) || s.is(BeroepenFeature.DAKPAN.get()));
            if (ps.isEmpty()) {
                return ps;
            }
            npc.roleData.putLongArray("Plekken", BeroepenHulp.longs(ps));
        }
        return BeroepenHulp.posities(npc.roleData, "Plekken");
    }

    /** How many ghost tiles are still open. */
    public static int open(GuhNpcEntity npc) {
        int n = 0;
        for (BlockPos p : plekken(npc)) {
            if (!npc.level().getBlockState(p).is(BeroepenFeature.DAKPAN.get())) {
                n++;
            }
        }
        return n;
    }

    /** The Mika's "borrowed" the tiles: every spot is a ghost tile again, the flag comes down. */
    public static void sloop(GuhNpcEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        for (BlockPos p : plekken(npc)) {
            level.setBlock(p, BeroepenFeature.DAKPLEK.get().defaultBlockState(), 3);
        }
        if (npc.roleData.contains("Vlag")) {
            BlockPos v = BlockPos.of(npc.roleData.getLongOr("Vlag", 0L));
            if (level.getBlockState(v).getBlock() instanceof BannerBlock) {
                level.setBlock(v, Blocks.AIR.defaultBlockState(), 3);
            }
            if (level.getBlockState(v.below()).is(Blocks.SPRUCE_FENCE)) {
                level.setBlock(v.below(), Blocks.AIR.defaultBlockState(), 3);
            }
            npc.roleData.remove("Vlag");
        }
    }

    /** De vlag in top: an orange flag on a pole on the middle of the roof, and a little party. */
    public static void vlag(GuhNpcEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        List<BlockPos> ps = plekken(npc);
        if (ps.isEmpty()) {
            return;
        }
        double x = 0, z = 0;
        int y = Integer.MIN_VALUE;
        for (BlockPos p : ps) {
            x += p.getX();
            z += p.getZ();
            y = Math.max(y, p.getY());
        }
        BlockPos paal = BlockPos.containing(x / ps.size(), y + 1, z / ps.size());
        if (level.getBlockState(paal).isAir()) {
            level.setBlock(paal, Blocks.SPRUCE_FENCE.defaultBlockState(), 3);
            BlockState banier = Blocks.ORANGE_BANNER.defaultBlockState().setValue(BannerBlock.ROTATION, Math.floorMod(Math.round(npc.getYRot() / 22.5f), 16));
            if (level.getBlockState(paal.above()).isAir()) {
                level.setBlock(paal.above(), banier, 3);
                npc.roleData.putLong("Vlag", paal.above().asLong());
            }
        }
        level.sendParticles(ParticleTypes.FIREWORK, paal.getX() + 0.5, paal.getY() + 2, paal.getZ() + 0.5, 40, 0.8, 0.8, 0.8, 0.15);
        level.playSound(null, paal, SoundEvents.FIREWORK_ROCKET_TWINKLE, SoundSource.NEUTRAL, 1f, 1f);
    }

    /**
     * A dakpan is laid on a ghost tile near Bob: the last one puts the flag up and finishes the job for everyone round
     * about who's on it (step 2).
     */
    public static void gelegd(ServerLevel level, BlockPos pos, ServerPlayer player) {
        for (GuhNpcEntity npc : level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(pos).inflate(BEREIK + 4),
                n -> n.getKind() == GuhNpcEntity.Kind.BOUWVAKKERGUH)) {
            if (!plekken(npc).contains(pos)) {
                continue;
            }
            int open = open(npc);
            if (open > 0) {
                player.sendOverlayMessage(Component.translatable("gui.guhs.beroepen.bouw.nog", open).withStyle(ChatFormatting.LIGHT_PURPLE));
                return;
            }
            vlag(npc);
            for (Player p : level.players()) {
                if (p instanceof ServerPlayer sp && sp.distanceTo(npc) < 32 && BeroepenVoortgang.stap(sp, BEROEP) == 2) {
                    GuhQuests.say(sp, npc, "quest.guhs.beroepen.bouw.af");
                    BeroepenVoortgang.rondAf(sp, BEROEP, npc);
                    neemPannen(sp);
                }
            }
            return;
        }
    }

    /** Leftover tiles go back to Bob. */
    static void neemPannen(ServerPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(BeroepenFeature.DAKPAN_ITEM.get())) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    /** (Tests) the flag's spot, or null. */
    @Nullable
    public static BlockPos vlagPlek(GuhNpcEntity npc) {
        return npc.roleData.contains("Vlag") ? BlockPos.of(npc.roleData.getLongOr("Vlag", 0L)) : null;
    }
}
