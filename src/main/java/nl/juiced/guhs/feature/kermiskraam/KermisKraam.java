package nl.juiced.guhs.feature.kermiskraam;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.droomwezens.DroomNpc;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModItems;

/**
 * The stall guhs: Kermis-guhs (same look, same character) with the stall's id in their roleData. A click on the guh or on the
 * counter does the same: a food stall opens its shop, a game stall starts a round ({@link KraamSpel}).
 */
public final class KermisKraam {
    /** roleData: the id of the stall ({@link Kraam#id}) and where its counter is. */
    static final String TAG = "guhs_kermiskraam", BLOK = "guhs_kermiskraam_blok";
    private static final String T = "gui.guhs.kermiskraam.";

    private KermisKraam() {
    }

    public static void klik(ServerPlayer p, Kraam kraam, @Nullable GuhNpcEntity guh, BlockPos blok) {
        if (kraam.isSpel()) {
            KraamSpel.start(p, kraam, blok);
        } else if (guh != null) {
            GuhQuests.say(p, guh, T + kraam.id + ".hallo");
            guh.openShop(p);
        }
    }

    /** The stall guh of this counter (spawned when missing, extra ones are removed). Null: this is no counter. */
    @Nullable
    public static GuhNpcEntity zorgVoorGuh(ServerLevel level, BlockPos blok) {
        BlockState state = level.getBlockState(blok);
        if (!(state.getBlock() instanceof KermisKraamBlock kraamBlok)) {
            return null;
        }
        long pos = blok.asLong();
        List<GuhNpcEntity> al = level.getEntitiesOfClass(GuhNpcEntity.class, new AABB(blok).inflate(6),
                n -> n.isAlive() && n.roleData.contains(BLOK) && n.roleData.getLongOr(BLOK, 0L) == pos);
        if (!al.isEmpty()) {
            for (int i = 1; i < al.size(); i++) {
                al.get(i).discard();
            }
            return al.get(0);
        }
        Direction kijk = state.getValue(HorizontalDirectionalBlock.FACING);
        GuhNpcEntity guh = DroomNpc.spawn(level, Vec3.atBottomCenterOf(blok.relative(kijk.getOpposite())), kijk.toYRot(), GuhNpcEntity.Kind.KERMIS_GUH);
        guh.roleData.putString(TAG, kraamBlok.kraam.id);
        guh.roleData.putLong(BLOK, pos);
        guh.setCustomName(Component.translatable("entity.guhs.kermiskraam." + kraamBlok.kraam.id));
        return guh;
    }

    /** What a stall guh sells; null for every other NPC (GuhNpcEntity.getOffers). */
    @Nullable
    public static MerchantOffers offers(GuhNpcEntity npc) {
        Kraam kraam = Kraam.van(npc.roleData.getStringOr(TAG, ""));
        if (kraam == null) {
            return null;
        }
        MerchantOffers offers = new MerchantOffers();
        for (Kraam.Waar waar : kraam.waren) {
            Item item = BuiltInRegistries.ITEM.getValue(Guhs.id(waar.item()));
            offers.add(new MerchantOffer(new ItemCost(ModItems.KAAS_KNABBELS.get(), waar.prijs()), new ItemStack(item), Integer.MAX_VALUE, 0, 0));
        }
        return offers;
    }

    /** A click on a stall guh: same as on its counter. A guh whose counter is gone goes home. */
    static void onPraat(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof GuhNpcEntity npc) || !(event.getEntity() instanceof ServerPlayer p)
                || event.getHand() != InteractionHand.MAIN_HAND || !npc.roleData.contains(TAG)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        BlockPos blok = BlockPos.of(npc.roleData.getLongOr(BLOK, 0L));
        if (p.level().getBlockState(blok).getBlock() instanceof KermisKraamBlock b) {
            klik(p, b.kraam, npc, blok);
        } else {
            npc.discard();
        }
    }
}
