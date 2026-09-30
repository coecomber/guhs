package nl.juiced.guhs.feature.barbecuether;

import java.util.Optional;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The Grillguh, a guh chef at the big barbecue pit (barbecueput). A short parody questline, per player
 * ({@link #STEP_KEY} in the player's saved guh data):
 * <ol>
 *   <li>find him: "NJEG, mijn barbecue! De Mika's hebben mijn Aanmaakblokjes gejat!" ({@link #MET})</li>
 *   <li>make or gather grillkool and mend the grillkool frame of his pit ({@link #FRAME})</li>
 *   <li>win the Aanmaakblokjes back at a Mika-kamp: while the quest runs every Mika there drops one ({@link #BLOKJES})</li>
 *   <li>light the pit with an Aanmaakblokje: "VAHOEG, mijn barbecue brandt weer!" ({@link #DONE})</li>
 * </ol>
 * After that: his secret recipe (the Aanmaakblokje becomes craftable), and his barbecue shop.
 */
public final class Grillguh implements NpcRole {
    public static final String STEP_KEY = "guhs_grill_stap";
    public static final int MET = 1, FRAME = 2, BLOKJES = 3, DONE = 4;
    public static final int TIPS = 5;
    /** How far around him his pit (and its grillkool frame) can be. */
    public static final int PIT_RADIUS = 16;

    public static int step(ServerPlayer player) {
        return GuhQuests.saved(player).getIntOr(STEP_KEY, 0);
    }

    public static void setStep(ServerPlayer player, int step) {
        GuhQuests.saved(player).putInt(STEP_KEY, step);
    }

    /** While the quest runs (met him, not finished yet), Mikas in a Mika-kamp drop Aanmaakblokjes. */
    public static boolean questActive(ServerPlayer player) {
        int s = step(player);
        return s >= MET && s < DONE;
    }

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 0.75f);
        int step = step(player);
        if (step == 0) {
            setStep(player, MET);
            GuhAdvancements.grant(player, "grill_gevonden");
            GuhQuests.say(player, npc, "quest.guhs.grillguh.hallo1");
            GuhQuests.say(player, npc, "quest.guhs.grillguh.hallo2");
            return;
        }
        if (step >= DONE) {
            if (GuhQuests.count(player, BarbecuetherFeature.GRILLGUH_RECEPT.get()) == 0) {
                GuhQuests.say(player, npc, "quest.guhs.grillguh.recept_weer");   // lost it? here's another copy
                GuhQuests.give(player, BarbecuetherFeature.GRILLGUH_RECEPT.get());
            } else {
                GuhQuests.say(player, npc, "quest.guhs.grillguh.tip" + npc.getRandom().nextInt(TIPS));
            }
            npc.openShop(player);
            return;
        }
        if (litNear(npc.level(), npc.blockPosition())) {
            complete(player, npc);
            return;
        }
        if (step == MET) {
            if (frameNear(npc.level(), npc.blockPosition())) {
                setStep(player, FRAME);
                GuhAdvancements.grant(player, "grill_frame");
                GuhQuests.say(player, npc, "quest.guhs.grillguh.frame_ok");
                step = FRAME;
            } else {
                GuhQuests.say(player, npc, "quest.guhs.grillguh.frame_hint");
                return;
            }
        }
        if (step == FRAME) {
            if (GuhQuests.count(player, BarbecuetherFeature.AANMAAKBLOKJE.get()) > 0) {
                setStep(player, BLOKJES);
                GuhAdvancements.grant(player, "grill_blokjes");
                GuhQuests.say(player, npc, "quest.guhs.grillguh.blokjes_terug");
            } else {
                GuhQuests.say(player, npc, "quest.guhs.grillguh.blokjes_hint");
            }
            return;
        }
        // BLOKJES: only the lighting left (a mended frame that got broken again: mend it first)
        GuhQuests.say(player, npc, frameNear(npc.level(), npc.blockPosition()) ? "quest.guhs.grillguh.aansteken" : "quest.guhs.grillguh.frame_hint");
    }

    /** The pit burns again: the quest is done (also when the player lights it before telling him). */
    public static void complete(ServerPlayer player, GuhNpcEntity npc) {
        if (step(player) >= DONE) {
            return;
        }
        setStep(player, DONE);
        for (String adv : new String[]{"grill_gevonden", "grill_frame", "grill_blokjes", "grill_aangestoken"}) {
            GuhAdvancements.grant(player, adv);
        }
        GuhQuests.say(player, npc, "quest.guhs.grillguh.klaar1");
        GuhQuests.say(player, npc, "quest.guhs.grillguh.klaar2");
        GuhQuests.give(player, BarbecuetherFeature.GRILLGUH_RECEPT.get());
        // (2.9: the koksmuts is only sold in his shop now; for finishing the quest you get a big bag of grilled knabbels)
        nl.juiced.guhs.feature.Minigames.give(player, new net.minecraft.world.item.ItemStack(ModItems.GEFRITUURDE_KAASKNABBELS.get(), 16));
        npc.level().playSound(null, npc, SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 1f, 1.2f);
        if (npc.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.FLAME, npc.getX(), npc.getY() + 1.2, npc.getZ(), 30, 0.6, 0.6, 0.6, 0.02);
        }
    }

    /** A player lit a barbecue portal here: a Grillguh nearby whose quest this player is doing is very happy. */
    public static void portalLit(ServerPlayer player, BlockPos pos) {
        if (step(player) < MET || step(player) >= DONE) {
            return;
        }
        player.level().getEntitiesOfClass(GuhNpcEntity.class, new AABB(pos).inflate(PIT_RADIUS + 4),
                n -> n.getKind() == BarbecuetherFeature.GRILLGUH_KIND).stream().findFirst().ifPresent(npc -> complete(player, npc));
    }

    /** Is there a whole grillkool frame (lit or not) around here? */
    public static boolean frameNear(Level level, BlockPos center) {
        return findFrame(level, center).isPresent();
    }

    /** The inside (bottom block) of a whole grillkool frame near here. */
    public static Optional<BlockPos> findFrame(Level level, BlockPos center) {
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-PIT_RADIUS, -8, -PIT_RADIUS), center.offset(PIT_RADIUS, 8, PIT_RADIUS))) {
            if (GrillPortalShape.isFrame(level.getBlockState(p))) {
                BlockPos above = p.above();
                if (GrillPortalShape.findAnyShape(level, above).isPresent()) {
                    return Optional.of(above.immutable());
                }
            }
        }
        return Optional.empty();
    }

    /** Does a barbecue portal burn around here? */
    public static boolean litNear(Level level, BlockPos center) {
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-PIT_RADIUS, -8, -PIT_RADIUS), center.offset(PIT_RADIUS, 8, PIT_RADIUS))) {
            if (level.getBlockState(p).is(BarbecuetherFeature.BARBECUETHER_PORTAAL.get())) {
                return true;
            }
        }
        return false;
    }

    /** Now and then a puff of barbecue smoke rises above his head. */
    @Override
    public void tick(GuhNpcEntity npc) {
        if (npc.tickCount % 60 == 0 && npc.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, npc.getX(), npc.getY() + 1.9, npc.getZ(), 1, 0.1, 0.05, 0.1, 0.005);
        }
    }

    /** His barbecue shop, for kaasknabbels (it opens after the questline). */
    @Nullable
    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        var knabbel = ModItems.KAAS_KNABBELS.get();
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(new ItemCost(knabbel, 12), null, new ItemStack(BarbecuetherFeature.AANMAAKBLOKJE.get())));
        offers.add(offer(new ItemCost(knabbel, 16), null, new ItemStack(BarbecuetherFeature.GRILLKOOL_ITEM.get(), 2)));
        offers.add(offer(new ItemCost(knabbel, 6), null, new ItemStack(BarbecuetherFeature.GEGRILDE_KAASKNABBELSATE.get(), 2)));
        offers.add(offer(new ItemCost(knabbel, 8), null, new ItemStack(BarbecuetherFeature.GUHBRAADWORST.get(), 2)));
        offers.add(offer(new ItemCost(knabbel, 10), null, new ItemStack(BarbecuetherFeature.GLOEIKOOLGRUIS.get(), 6)));
        offers.add(offer(new ItemCost(knabbel, 16), null, new ItemStack(ModItems.clothingItem(GuhClothes.GRILL_HALSDOEK))));
        offers.add(offer(new ItemCost(knabbel, 24), null, new ItemStack(ModItems.clothingItem(GuhClothes.GRILL_KOKSMUTS))));
        offers.add(offer(new ItemCost(knabbel, 32), null, new ItemStack(ModItems.clothingItem(GuhClothes.GRILL_SCHORT))));
        return offers;
    }

    private static MerchantOffer offer(ItemCost a, @Nullable ItemCost b, ItemStack result) {
        return new MerchantOffer(a, Optional.ofNullable(b), result, Integer.MAX_VALUE, 0, 0);
    }
}
