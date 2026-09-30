package nl.juiced.guhs.feature.kamperen;

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
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.knus.Dagdeel;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * Opa Guh (OPA_GUH): an old grey-pink guh with a white moustache, little round glasses and a nightcap, at the
 * kampeerplekjes and on his bench by the Knuffeldal town's campfire. In the evening and at night he tells a story
 * ({@link Verhalen}: one per night); in the daytime he dozes and gives a tip about camping. Sneak + talk: his little
 * shop (for kaasknabbels: a slaapzak, the slaapmutsje, the pyjama). He keeps his campfire burning.
 */
public final class OpaGuh implements NpcRole {
    public static final OpaGuh INSTANCE = new OpaGuh();
    public static final int TIPS = 5;
    public static final int PRIJS_SLAAPZAK = 8, PRIJS_MUTSJE = 10, PRIJS_PYJAMA = 14;
    private static final String GESPROKEN = "guhs_kamperen_gesproken";

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 0.7f);
        var saved = GuhQuests.saved(player);
        int keer = saved.getInt(GESPROKEN);
        if (player.isSecondaryUseActive() && keer > 0) {
            GuhQuests.say(player, npc, "quest.guhs.kamperen.winkel");
            npc.openShop(player);
            return;
        }
        saved.putInt(GESPROKEN, keer + 1);
        if (keer == 0) {
            GuhQuests.say(player, npc, "quest.guhs.kamperen.hallo");
            KnusVoortgang.hoogste(player, Verhalen.GEVONDEN, 1);
            GuhAdvancements.grant(player, "kamperen_opa");
        }
        praat(npc, player, isAvond(npc));
    }

    /** Is it evening or night here (story time)? */
    public static boolean isAvond(GuhNpcEntity npc) {
        if (npc.level().dimensionType().hasFixedTime()) {
            return false;
        }
        Dagdeel d = Dagdeel.huidig(npc.level());
        return d == Dagdeel.AVOND || d == Dagdeel.NACHT;
    }

    /** What he says (and does): a story at night, a tip in the daytime. Public for the tests (avond = story time). */
    public static void praat(GuhNpcEntity npc, ServerPlayer player, boolean avond) {
        if (Verhalen.luistert(player)) {
            GuhQuests.say(player, npc, "quest.guhs.kamperen.sst");
        } else if (!avond) {
            int keer = GuhQuests.saved(player).getInt(GESPROKEN);
            GuhQuests.say(player, npc, "quest.guhs.kamperen.tip" + (keer % TIPS));
        } else if (Verhalen.vannachtGehoord(player)) {
            GuhQuests.say(player, npc, "quest.guhs.kamperen.morgen");      // one story per night, also when he's telling one
        } else if (Verhalen.vertelt(npc)) {
            Verhalen.begin(npc, player, Verhalen.verhaal(npc));
        } else if (npc.level() instanceof ServerLevel level && KampvuurPyjama.kampvuurBij(level, npc.blockPosition(), 8) == null
                && !steekAan(level, npc.blockPosition())) {
            GuhQuests.say(player, npc, "quest.guhs.kamperen.geen_vuur");
        } else {
            Verhalen.begin(npc, player, Verhalen.volgende(player));
        }
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        Verhalen.tick(npc);
        if ((npc.tickCount + npc.getId()) % 200 == 0 && npc.level() instanceof ServerLevel level && isAvond(npc)) {
            steekAan(level, npc.blockPosition());
        }
    }

    /** Lights an unlit campfire near him (he always keeps his fire going at night); true if there is one burning now. */
    static boolean steekAan(ServerLevel level, BlockPos at) {
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-6, -2, -6), at.offset(6, 2, 6))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof CampfireBlock && s.hasProperty(CampfireBlock.LIT) && !s.getValue(CampfireBlock.LIT)
                    && !s.getValue(CampfireBlock.WATERLOGGED)) {
                level.setBlock(p, s.setValue(CampfireBlock.LIT, true), 3);
                level.playSound(null, p, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.8f, 1f);
                level.sendParticles(ParticleTypes.FLAME, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.01);
                return true;
            }
        }
        return false;
    }

    /** For kaasknabbels: a guh_slaapzak, the slaapmutsje and the pyjama. Never sold out. */
    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(PRIJS_SLAAPZAK, new ItemStack(KamperenFeature.SLAAPZAK_ITEM.get())));
        offers.add(offer(PRIJS_MUTSJE, new ItemStack(ModItems.clothingItem(GuhClothes.SLAAPMUTSJE))));
        offers.add(offer(PRIJS_PYJAMA, new ItemStack(ModItems.clothingItem(GuhClothes.PYJAMA_PAKJE))));
        return offers;
    }

    private static MerchantOffer offer(int price, ItemStack result) {
        return new MerchantOffer(new ItemCost(ModItems.KAAS_KNABBELS.get(), price), result, Integer.MAX_VALUE, 0, 0);
    }

    private OpaGuh() {
    }
}
