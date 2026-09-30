package nl.juiced.guhs.feature.ballon;

import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.phys.AABB;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

import net.minecraft.world.entity.EntitySpawnReason;
/**
 * Kapitein Wolkje (BALLONGUH), at his kiosk by the ballonsteiger of the Ballonfestival. Talk to him and you're off: he
 * steps into the balloon with you and flies the next round (the first one with a viewpoint you haven't stamped yet).
 * Sneak + talk: his shop, for ballonmunten. While he's up in the air his kiosk is empty.
 */
public final class BallonRole implements NpcRole {
    public static final BallonRole INSTANCE = new BallonRole();
    /** How far from him his balloon may wait. */
    public static final double BALLON_BEREIK = 20;
    public static final int PRIJS_PET = 5, PRIJS_BRIL = 4, PRIJS_MINI = 2, PRIJS_STEIGER = 1;
    private static final String GESPROKEN = "guhs_ballon_gesproken";

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.1f);
        if (npc.isInvisible()) {
            return;
        }
        var saved = GuhQuests.saved(player);
        int keer = saved.getIntOr(GESPROKEN, 0);
        if (player.isSecondaryUseActive() && keer > 0) {
            GuhQuests.say(player, npc, "quest.guhs.ballon.winkel");
            npc.openShop(player);
            return;
        }
        saved.putInt(GESPROKEN, keer + 1);
        if (keer == 0) {
            GuhQuests.say(player, npc, "quest.guhs.ballon.hallo");
            KnusVoortgang.hoogste(player, BallonVlucht.GEVONDEN, 1);
            GuhAdvancements.grant(player, "ballon_wolkje");
        }
        if (Minigames.refuse(player, npc, Minigames.BALLON)) {
            return;
        }
        LuchtballonEntity ballon = ballon(npc);
        if (ballon == null) {
            GuhQuests.say(player, npc, "quest.guhs.ballon.geen_ballon");
            return;
        }
        if (ballon.vliegt()) {
            GuhQuests.say(player, npc, "quest.guhs.ballon.al_in_de_lucht");
            return;
        }
        BallonRoute route = BallonVlucht.volgendeRoute(player);
        GuhQuests.say(player, npc, "quest.guhs.ballon.instappen", net.minecraft.network.chat.Component.translatable("gui.guhs.ballon.route." + route.id()));
        ballon.stijgOp(player, route, npc);
    }

    /** His balloon: the nearest one within reach (the one on his steiger); if there's none, he gets one to the steiger. */
    @Nullable
    public static LuchtballonEntity ballon(GuhNpcEntity npc) {
        List<LuchtballonEntity> list = npc.level().getEntitiesOfClass(LuchtballonEntity.class, npc.getBoundingBox().inflate(BALLON_BEREIK),
                b -> !b.isRemoved() && !b.isDeco());
        LuchtballonEntity best = list.stream().min(Comparator.comparingDouble(b -> b.thuis().distanceToSqr(npc.position()))).orElse(null);
        if (best != null || !(npc.level() instanceof ServerLevel level)) {
            return best;
        }
        BlockPos steiger = null;
        BlockPos at = npc.blockPosition();
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-12, -4, -12), at.offset(12, 4, 12))) {
            if (level.getBlockState(p).is(BallonFeature.BALLONSTEIGER.get()) && level.getBlockState(p.above()).isAir() && p.distSqr(at) < bestD) {
                bestD = p.distSqr(at);
                steiger = p.immutable();
            }
        }
        if (steiger == null) {
            return null;
        }
        LuchtballonEntity nieuw = BallonFeature.LUCHTBALLON.get().create(level, EntitySpawnReason.TRIGGERED);
        if (nieuw == null) {
            return null;
        }
        nieuw.snapTo(steiger.getX() + 0.5, steiger.getY() + 1, steiger.getZ() + 0.5, 0, 0);
        nieuw.setThuis(steiger.above(), 0);
        nieuw.setKleur(level.getRandom().nextInt(LuchtballonEntity.KLEUREN));
        level.addFreshEntity(nieuw);
        return nieuw;
    }

    /** Back at his kiosk when his balloon isn't flying (e.g. after a restart halfway a flight). */
    @Override
    public void tick(GuhNpcEntity npc) {
        if (npc.isInvisible() && (npc.tickCount + npc.getId()) % 40 == 0) {
            boolean weg = !npc.level().getEntitiesOfClass(LuchtballonEntity.class, new AABB(npc.blockPosition()).inflate(160),
                    b -> b.vliegt() && npc.getUUID().equals(b.kapitein())).isEmpty();
            if (!weg) {
                npc.setInvisible(false);
            }
        }
    }

    /** For ballonmunten: the ballonpet, the ballonbril, mini luchtballonnen and ballonsteigers. Never sold out. */
    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(offer(PRIJS_MINI, new ItemStack(BallonFeature.MINI_LUCHTBALLON_ITEM.get())));
        offers.add(offer(PRIJS_BRIL, new ItemStack(ModItems.clothingItem(GuhClothes.BALLONBRIL))));
        offers.add(offer(PRIJS_PET, new ItemStack(ModItems.clothingItem(GuhClothes.BALLONPET))));
        offers.add(offer(PRIJS_STEIGER, new ItemStack(BallonFeature.BALLONSTEIGER_ITEM.get(), 4)));
        return offers;
    }

    private static MerchantOffer offer(int price, ItemStack result) {
        return new MerchantOffer(new ItemCost(BallonFeature.BALLONMUNT.get(), price), result, Integer.MAX_VALUE, 0, 0);
    }

    private BallonRole() {
    }
}
