package nl.juiced.guhs.feature.vissen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.registry.ModItems;

/**
 * The Visguh at the guhvis pond: explains and runs the Guhvis-wedstrijd ({@link VisWedstrijd}) and has a stall with the
 * angler outfit (only sold here) for visbonnen. She stays on her spot at the judge's desk (even when a fishing line
 * yanks at her), and now and then a fish jumps in her pond.
 */
public class VisguhRole implements NpcRole {
    /** Prices in visbonnen. */
    public static final int PRICE_HOED = 6, PRICE_VEST = 10, PRICE_HAAK = 8;

    @Override
    public void talk(GuhNpcEntity npc, ServerPlayer player) {
        VisWedstrijd.talk(npc, player);
    }

    @Override
    public void tick(GuhNpcEntity npc) {
        stayHome(npc);
        VisWedstrijd.tick(npc);
        if (npc.tickCount % 50 == 0) {
            jumpingFish(npc);
        }
    }

    @Override
    public MerchantOffers offers(GuhNpcEntity npc) {
        MerchantOffers offers = new MerchantOffers();
        offers.add(clothes(PRICE_HOED, GuhClothes.VISSERSHOEDJE));
        offers.add(clothes(PRICE_HAAK, GuhClothes.VIS_AAN_DE_HAAK));
        offers.add(clothes(PRICE_VEST, GuhClothes.VISVEST));
        offers.add(new MerchantOffer(new ItemCost(VissenFeature.VISBON.get(), 1), new ItemStack(ModItems.GEBAKKEN_GUH_VIS.get(), 3),
                Integer.MAX_VALUE, 0, 0));
        return offers;
    }

    private static MerchantOffer clothes(int price, GuhClothes piece) {
        return new MerchantOffer(new ItemCost(VissenFeature.VISBON.get(), price), new ItemStack(ModItems.clothingItem(piece)),
                Integer.MAX_VALUE, 0, 0);
    }

    /** Remembers where she was put, and goes right back there (fishing hooks pull at her, the water pushes). */
    private static void stayHome(GuhNpcEntity npc) {
        var data = npc.roleData;
        if (!data.contains("HomeX")) {
            data.putDouble("HomeX", npc.getX());
            data.putDouble("HomeY", npc.getY());
            data.putDouble("HomeZ", npc.getZ());
            return;
        }
        Vec3 home = new Vec3(data.getDouble("HomeX"), data.getDouble("HomeY"), data.getDouble("HomeZ"));
        if (npc.position().distanceToSqr(home) > 0.04) {
            npc.moveTo(home.x, home.y, home.z, npc.getYRot(), npc.getXRot());
            npc.setDeltaMovement(Vec3.ZERO);
        }
    }

    /** A little splash somewhere on the pond: there's fish here! */
    private static void jumpingFish(GuhNpcEntity npc) {
        ServerLevel world = (ServerLevel) npc.level();
        var random = world.getRandom();
        double angle = random.nextDouble() * Math.PI * 2, dist = 6 + random.nextDouble() * 28;
        BlockPos top = BlockPos.containing(npc.getX() + Math.cos(angle) * dist, npc.getY() - 1, npc.getZ() + Math.sin(angle) * dist);
        for (int dy = 0; dy < 4; dy++) {
            BlockPos p = top.below(dy);
            if (world.getFluidState(p).is(Fluids.WATER) && world.isEmptyBlock(p.above())) {
                world.sendParticles(ParticleTypes.SPLASH, p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5, 12, 0.3, 0.05, 0.3, 0.1);
                world.sendParticles(ParticleTypes.FISHING, p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5, 4, 0.2, 0, 0.2, 0.02);
                return;
            }
        }
    }
}
