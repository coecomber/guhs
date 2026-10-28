package nl.juiced.guhs.feature.fossiel;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhVariant;
import nl.juiced.guhs.feature.huisje.Huisje;

/**
 * 1.8.0: what the four oerguhs do (most of it in a guhhuisje). Called from the shared code with one line each:
 * <ul>
 *   <li>Mammoet-guh: its chores go {@link #MAMMOET_KLUS}x as fast ({@code Band.klusSnelheid}) and a cuddle from its owner
 *       (the menu's "Knuffelen!") gives a little white wool, at most once per {@link #WOL_RUST} ticks ({@code BandEvents.knuffel});</li>
 *   <li>T-Guh: the Waken chore notices threats {@link #TGUH_EXTRA} blocks outside the home base instead of 6 ({@code WakenKlus});</li>
 *   <li>Pteroguh: the chores of its huisje find chests up to {@link #PTERO_MARGE} blocks around the huisje instead of 4
 *       ({@code Voorraad.kisten}: it flies the finds there);</li>
 *   <li>Oer-stokstaartje-guh: stands upright (client: FossielClient) and, every {@link #SPOT_ELKE} ticks with its owner near,
 *       spots the rarest guh (or Blub) within {@link #SPOT_BEREIK} blocks and tells where ({@link #tick}).</li>
 * </ul>
 */
public final class OerGuhs {
    public static final float MAMMOET_KLUS = 2.0f;
    public static final long WOL_RUST = 2400;
    public static final int TGUH_EXTRA = 22;
    public static final int PTERO_MARGE = 24;
    public static final int SPOT_ELKE = 2400, SPOT_BEREIK = 96, SPOT_BAAS = 24;
    private static final String WOL_TOT = "guhs_oer_wol_tot", GESPOT = "guhs_oer_gespot";

    private OerGuhs() {
    }

    public static boolean is(Entity e, GuhVariant v) {
        return e instanceof GuhEntity g && g.getVariant() == v;
    }

    /** {@code Band.klusSnelheid}: the extra factor of this guh's kind. */
    public static float klusFactor(Mob guh) {
        return is(guh, GuhVariant.MAMMOETGUH) ? MAMMOET_KLUS : 1f;
    }

    /** {@code WakenKlus}: how far outside the home base this watcher notices a threat. */
    public static int wakenExtra(Mob wie, int standaard) {
        return is(wie, GuhVariant.TGUH) ? TGUH_EXTRA : standaard;
    }

    /** {@code Voorraad.kisten}: how far around the huisje its chests may stand (a Pteroguh resident flies further). */
    public static int kistMarge(ServerLevel level, Huisje h, int standaard) {
        for (UUID u : h.bewoners()) {
            if (is(level.getEntity(u), GuhVariant.PTEROGUH)) {
                return PTERO_MARGE;
            }
        }
        return standaard;
    }

    /** {@code BandEvents.knuffel}: a cuddled Mammoet-guh sheds a little wool now and then. */
    public static void geknuffeld(GuhEntity guh, ServerPlayer p) {
        if (guh.getVariant() != GuhVariant.MAMMOETGUH) {
            return;
        }
        long nu = guh.level().getGameTime();
        if (guh.getPersistentData().getLongOr(WOL_TOT, 0L) > nu) {
            return;
        }
        guh.getPersistentData().putLong(WOL_TOT, nu + WOL_RUST);
        ItemStack wol = new ItemStack(Items.WHITE_WOOL, 1 + guh.getRandom().nextInt(2));
        if (!p.getInventory().add(wol)) {
            guh.spawnAtLocation((ServerLevel) guh.level(), wol);
        }
        p.sendOverlayMessage(Component.translatable("gui.guhs.fossiel.mammoet_wol", guh.getDisplayName()).withStyle(ChatFormatting.GOLD));
    }

    /** Every guh, every tick (GuhHooks): the Oer-stokstaartje-guh keeps watch. */
    public static void tick(GuhEntity guh) {
        if (guh.getVariant() != GuhVariant.STOKSTAARTJEGUH || !guh.isTame() || (guh.tickCount + guh.getId()) % SPOT_ELKE != 0
                || !(guh.level() instanceof ServerLevel level) || guh.getOwnerUUID() == null) {
            return;
        }
        ServerPlayer baas = level.getServer().getPlayerList().getPlayer(guh.getOwnerUUID());
        if (baas == null || baas.level() != level || baas.distanceToSqr(guh) > SPOT_BAAS * SPOT_BAAS) {
            return;
        }
        Mob vondst = spot(level, guh);
        if (vondst == null) {
            return;
        }
        guh.getPersistentData().putInt(GESPOT, vondst.getId());
        level.sendParticles(ParticleTypes.NOTE, guh.getX(), guh.getY() + guh.getBbHeight() + 0.5, guh.getZ(), 3, 0.2, 0.1, 0.2, 0);
        int dx = (int) Math.round(vondst.getX() - guh.getX()), dz = (int) Math.round(vondst.getZ() - guh.getZ());
        baas.sendSystemMessage(Component.translatable("gui.guhs.fossiel.stokstaartje_ziet", guh.getDisplayName(), vondst.getDisplayName(),
                richting(dx, dz), (int) Math.round(Math.sqrt(dx * dx + dz * dz))).withStyle(ChatFormatting.GOLD));
    }

    /** The rarest wild guh (a variant with a low roll weight, or a special one) or a wild Blub-guh nearby, or null. */
    static Mob spot(ServerLevel level, GuhEntity wachter) {
        List<Mob> kandidaten = level.getEntitiesOfClass(Mob.class, wachter.getBoundingBox().inflate(SPOT_BEREIK), e -> e != wachter && e.isAlive()
                && zeldzaam(e) && e.getId() != wachter.getPersistentData().getIntOr(GESPOT, -1));
        return kandidaten.stream().min(Comparator.comparingInt(OerGuhs::gewicht).thenComparingDouble(e -> e.distanceToSqr(wachter))).orElse(null);
    }

    private static boolean zeldzaam(Mob e) {
        if (e instanceof GuhEntity g) {
            return !g.isTame() && g.getVariant() != GuhVariant.NORMAL && gewicht(g) <= 6;
        }
        return net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getPath().equals("blubguh")
                && e instanceof net.minecraft.world.entity.OwnableEntity o && nl.juiced.guhs.entity.Owners.uuid(o) == null;
    }

    /** How common: the variant's roll weight (0 = never rolled: rarest), a Blub counts as 5. */
    private static int gewicht(Mob e) {
        return e instanceof GuhEntity g ? g.getVariant().weight : 5;
    }

    private static Component richting(int dx, int dz) {
        String k = Math.abs(dx) > Math.abs(dz) * 2 ? (dx > 0 ? "oost" : "west")
                : Math.abs(dz) > Math.abs(dx) * 2 ? (dz > 0 ? "zuid" : "noord")
                : (dz > 0 ? "zuid" : "noord") + (dx > 0 ? "oost" : "west");
        return Component.translatable("gui.guhs.fossiel.richting." + k);
    }
}
