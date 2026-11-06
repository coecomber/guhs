package nl.juiced.guhs.feature.vadsrijk.huizen;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.TriState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorStandItem;
import net.minecraft.world.item.BoatItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.HangingEntityItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MinecartItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.BlockSnapshot;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import nl.juiced.guhs.feature.vadsrijk.huizen.HuizenData.Deur;
import nl.juiced.guhs.feature.vadsrijk.huizen.HuizenData.Huis;
import nl.juiced.guhs.feature.vadsrijk.huizen.WijkPlan.Kavel;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;

/**
 * U-HUIZEN: the rules inside Het Knuffelkwartier (server events). Creative players are never stopped.
 * <ul>
 *   <li>Placing/breaking: see {@link Rechten}; outside your own (or allowed friend's) fence nothing changes. Placed blocks are
 *   remembered per house (only those can be broken again; everything goes back at the mailbox when the house changes).</li>
 *   <li>Doors: a door, trapdoor or gate of a house you may not enter stays shut; anyone found inside a house they may not be in is
 *   walked out to the gate.</li>
 *   <li>Nothing hurts, no hunger; out of the ward (or falling) = back on the square; the pink gate brings you back to the town.</li>
 *   <li>The clicks: the "te koop" sign, the mailbox, the notice board ({@link Menus}).</li>
 * </ul>
 */
public final class Regels {
    /** Per player: game tick of arrival (the gate waits a moment) and the plot shown last. */
    private static final Map<UUID, Long> AANGEKOMEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> LAATSTE_KAVEL = new ConcurrentHashMap<>();

    private Regels() {
    }

    static void aangekomen(ServerPlayer p) {
        AANGEKOMEN.put(p.getUUID(), (long) p.level().getServer().getTickCount());
        LAATSTE_KAVEL.remove(p.getUUID());
    }

    static void vergeet(UUID id) {
        AANGEKOMEN.remove(id);
        LAATSTE_KAVEL.remove(id);
    }

    private static boolean vrijgesteld(Player p) {
        return p.isCreative() || p.isSpectator();
    }

    /** The first second: kern's inventory swap is not done yet (the Vadsrijk bag is still in your hands). */
    static boolean nogNietGewisseld(Player p) {
        return Spelers.data(p).binnen;
    }

    @Nullable
    static Huis huisOp(MinecraftServer server, BlockPos pos) {
        Kavel k = WijkPlan.kavelOp(pos);
        return k == null ? null : Kwartier.data(server).huis(k.sleutel());
    }

    // ---- placing / breaking -------------------------------------------------------------------------------------------------

    static void plaats(BlockEvent.EntityPlaceEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !Kwartier.in(p) || vrijgesteld(p)) {
            return;
        }
        String nee = nogNietGewisseld(p) ? "vadsrijk.huizen.nee.even" : null;
        Kavel k = WijkPlan.kavelOp(e.getPos());
        HuizenData d = Kwartier.data(p.level().getServer());
        Huis h = k == null ? null : d.huis(k.sleutel());
        if (nee == null) {
            nee = k == null ? "vadsrijk.huizen.nee.buiten_kavel" : Rechten.plaatsen(h, k, e.getPos(), p.getUUID(), e.getPlacedBlock());
        }
        if (nee == null && e instanceof BlockEvent.EntityMultiPlaceEvent mp) {   // (a door or bed: every part inside)
            for (BlockSnapshot s : mp.getReplacedBlockSnapshots()) {
                if (!k.binnenHek(s.getPos()) || s.getPos().getY() > WijkPlan.VLOER + WijkPlan.HOOGTE) {
                    nee = "vadsrijk.huizen.nee.buiten_kavel";
                }
            }
        }
        if (nee != null) {
            e.setCanceled(true);
            p.sendOverlayMessage(Component.translatable(nee, h == null ? 0 : h.soort.maxDeco).withStyle(ChatFormatting.RED));
            return;
        }
        if (e instanceof BlockEvent.EntityMultiPlaceEvent mp) {
            for (BlockSnapshot s : mp.getReplacedBlockSnapshots()) {
                h.geplaatst.add(s.getPos().asLong());
            }
        } else {
            h.geplaatst.add(e.getPos().asLong());
        }
        d.setDirty();
    }

    static void breek(BreakBlockEvent e) {
        Player p = e.getPlayer();
        if (!Kwartier.in(p) || vrijgesteld(p) || !(p instanceof ServerPlayer sp)) {
            return;
        }
        HuizenData d = Kwartier.data(sp.level().getServer());
        Kavel k = WijkPlan.kavelOp(e.getPos());
        Huis h = k == null ? null : d.huis(k.sleutel());
        String nee = nogNietGewisseld(p) ? "vadsrijk.huizen.nee.even" : Rechten.breken(h, e.getPos(), p.getUUID());
        if (nee != null) {
            e.setCanceled(true);
            sp.sendOverlayMessage(Component.translatable(nee).withStyle(ChatFormatting.RED));
            return;
        }
        // the block goes (with its normal drop); its other half too
        h.geplaatst.remove(e.getPos().asLong());
        BlockState s = e.getState();
        if (s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
            BlockPos ander = s.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? e.getPos().above() : e.getPos().below();
            h.geplaatst.remove(ander.asLong());
        }
        if (s.getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
            h.geplaatst.remove(e.getPos().relative(net.minecraft.world.level.block.BedBlock.getConnectedDirection(s)).asLong());
        }
        d.setDirty();
    }

    static void gereedschap(BlockEvent.BlockToolModificationEvent e) {   // (no stripping/pathing/tilling of the ward)
        Player p = e.getPlayer();
        if (p == null || !Kwartier.in(p) || vrijgesteld(p)) {
            return;
        }
        Huis h = p instanceof ServerPlayer sp ? huisOp(sp.level().getServer(), e.getPos()) : null;
        if (h == null || !h.geplaatst.contains(e.getPos().asLong()) || !Rechten.magInrichten(h, p.getUUID())) {
            e.setCanceled(true);
        }
    }

    // ---- clicks -------------------------------------------------------------------------------------------------------------

    private static boolean verbodenItem(ItemStack s) {
        Item i = s.getItem();
        return i instanceof BucketItem || i instanceof EnderpearlItem || i instanceof SpawnEggItem || i instanceof BoatItem || i instanceof FireChargeItem
                || i instanceof FlintAndSteelItem || i instanceof ArmorStandItem || i instanceof HangingEntityItem || i instanceof MinecartItem
                || i instanceof EndCrystalItem;
    }

    static void klikBlok(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !Kwartier.in(p)) {
            return;
        }
        BlockPos pos = e.getPos();
        MinecraftServer server = p.level().getServer();
        HuizenData d = Kwartier.data(server);
        Kavel k = WijkPlan.kavelOp(pos);
        Huis h = k == null ? null : d.huis(k.sleutel());
        // the special blocks first (also for creative: they are the menus)
        if (k != null && h == null && k.wereld(WijkPlan.POORT_U, WijkPlan.VLOER, 1).equals(pos)) {
            e.setCanceled(true);
            Menus.teKoop(p, k);
            return;
        }
        if (k != null && h != null && k.wereld(WijkPlan.POST_U, WijkPlan.VLOER + 1, WijkPlan.POST_V).equals(pos)) {
            e.setCanceled(true);
            Menus.post(p, k, h);
            return;
        }
        if (k == null && WijkBouw.isBord(pos)) {
            e.setCanceled(true);
            Menus.prikbord(p, WijkPlan.wijkVan(pos.getX()));
            return;
        }
        if (vrijgesteld(p)) {
            return;
        }
        if (nogNietGewisseld(p)) {
            e.setCanceled(true);
            return;
        }
        BlockState s = p.level().getBlockState(pos);
        boolean deur = s.getBlock() instanceof DoorBlock || s.getBlock() instanceof TrapDoorBlock || s.getBlock() instanceof FenceGateBlock;
        if (h != null && deur && !Rechten.magBinnen(h, p.getUUID())) {
            e.setCanceled(true);
            p.sendOverlayMessage(Component.translatable("vadsrijk.huizen.deur.dicht", h.naam).withStyle(ChatFormatting.RED));
            return;
        }
        if (verbodenItem(e.getItemStack())) {
            e.setUseItem(TriState.FALSE);
        }
        boolean magHier = h != null && Rechten.magInrichten(h, p.getUUID()) && k.binnenHek(pos.relative(e.getFace() == null ? net.minecraft.core.Direction.UP : e.getFace()));
        if (!magHier) {
            e.setUseItem(TriState.FALSE);   // (blocks can still be used: sit on a bench, open a door you may open)
            if (e.getItemStack().getItem() instanceof BoneMealItem) {
                e.setCanceled(true);
            }
        }
    }

    static void klikItem(PlayerInteractEvent.RightClickItem e) {
        if (e.getEntity() instanceof ServerPlayer p && Kwartier.in(p) && !vrijgesteld(p) && verbodenItem(e.getItemStack())) {
            e.setCanceled(true);
        }
    }

    static void klikEntiteit(PlayerInteractEvent.EntityInteract e) {
        if (Kwartier.in(e.getEntity()) && e.getTarget().entityTags().contains(Figuranten.TAG)) {
            e.setCanceled(true);
        }
    }

    static void klikEntiteitPrecies(PlayerInteractEvent.EntityInteractSpecific e) {
        if (Kwartier.in(e.getEntity()) && e.getTarget().entityTags().contains(Figuranten.TAG)) {
            e.setCanceled(true);
        }
    }

    static void aanval(AttackEntityEvent e) {
        if (Kwartier.in(e.getEntity()) && (!vrijgesteld(e.getEntity()) || e.getTarget() instanceof Player)) {
            e.setCanceled(true);   // (no fights, no breaking item frames or figures; creative may clean up)
        }
    }

    static void schade(LivingIncomingDamageEvent e) {
        if (Kwartier.in(e.getEntity()) && (e.getEntity() instanceof Player || e.getEntity().entityTags().contains(Figuranten.TAG)
                || e.getEntity().entityTags().contains(Figuranten.NPC_TAG))) {
            e.setCanceled(true);
        }
    }

    static void binnenkomen(EntityJoinLevelEvent e) {
        if (e.loadedFromDisk() && e.getLevel().dimension() == Kwartier.DIM && (e.getEntity().entityTags().contains(Figuranten.TAG) || e.getEntity().entityTags().contains(Figuranten.NPC_TAG))) {
            e.setCanceled(true);   // (figures are never saved: the keeper puts them back while somebody is near)
        }
    }

    // ---- per player, every 10 ticks -------------------------------------------------------------------------------------------

    static void tick(ServerPlayer p, long nu) {
        if (!Kwartier.in(p)) {
            return;
        }
        MinecraftServer server = p.level().getServer();
        HuizenData d = Kwartier.data(server);
        p.getFoodData().setFoodLevel(20);
        int wijk = WijkPlan.wijkVan(p.getX());
        if (!vrijgesteld(p) && (!WijkPlan.binnenWijk(p.getX(), p.getZ()) || p.getY() < WijkPlan.VLOER - 14)) {
            Vec3 a = WijkPlan.aankomst(Math.max(0, Math.min(wijk, d.wijken - 1)));
            p.teleportTo(p.level(), a.x, a.y, a.z, java.util.Set.of(), 0f, 0f, true);
            p.resetFallDistance();
            p.sendOverlayMessage(Component.translatable("vadsrijk.huizen.rand").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        long sinds = nu - AANGEKOMEN.getOrDefault(p.getUUID(), 0L);
        if (WijkPlan.inPoort(p.getX(), p.getY(), p.getZ()) && sinds > 60) {
            AANGEKOMEN.put(p.getUUID(), nu);
            Kwartier.terug(p);
            return;
        }
        Kavel k = WijkPlan.kavelOp(p.getX(), p.getZ());
        Huis h = k == null ? null : d.huis(k.sleutel());
        // the plot sign in the action bar when you walk onto another plot
        int sl = k == null ? -1 : k.sleutel();
        Integer oud = LAATSTE_KAVEL.put(p.getUUID(), sl);
        if (k != null && (oud == null || oud != sl)) {
            Component t = h == null ? Component.translatable("vadsrijk.huizen.kavel.vrij", k.nr() + 1)
                    : h.eigenaar.equals(p.getUUID()) ? Component.translatable("vadsrijk.huizen.kavel.eigen", Component.translatable(h.soort.naamKey()))
                    : Component.translatable("vadsrijk.huizen.kavel.van", h.naam, Component.translatable(h.soort.naamKey()));
            p.sendOverlayMessage(t.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        if (h == null) {
            return;
        }
        if (h.eigenaar.equals(p.getUUID()) && nu % 100 < 10) {
            Figuranten.bijwerkenVan(p, h);
        }
        // locked out: walked to the gate
        if (!vrijgesteld(p) && Ontwerp.inHuis(k, h.soort, p.getX(), p.getY(), p.getZ()) && !Rechten.magBinnen(h, p.getUUID())) {
            Vec3 v = k.voorDeur();
            p.teleportTo(p.level(), v.x, v.y, v.z, java.util.Set.of(), k.yawNaarBinnen() + 180, 0f, true);
            p.sendOverlayMessage(Component.translatable("vadsrijk.huizen.deur.dicht", h.naam).withStyle(ChatFormatting.RED));
        }
    }

    /** Shuts the front door of a house (after the owner locked it). */
    static void deurDicht(ServerLevel l, Kavel k, Huis h) {
        BlockPos onder = k.wereld(WijkPlan.POORT_U, WijkPlan.VLOER, h.soort.v0());
        for (BlockPos p : new BlockPos[] {onder, onder.above()}) {
            BlockState s = l.getBlockState(p);
            if (s.getBlock() instanceof DoorBlock && s.getValue(DoorBlock.OPEN)) {
                l.setBlock(p, s.setValue(DoorBlock.OPEN, false), 10);
            }
        }
        if (h.deur != Deur.OPEN) {
            l.sendParticles(ParticleTypes.CRIT, onder.getX() + 0.5, onder.getY() + 1, onder.getZ() + 0.5, 6, 0.3, 0.4, 0.3, 0);
        }
    }

    // ---- rested xp hooks -------------------------------------------------------------------------------------------------------

    static void uitgelogd(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        vergeet(p.getUUID());
        Menus.vergeet(p.getUUID());
        if (!Kwartier.in(p)) {
            return;
        }
        HuizenData d = Kwartier.data(p.level().getServer());
        Integer sl = d.sleutelVan(p.getUUID());
        if (sl != null) {
            Kavel k = WijkPlan.kavel(sl);
            Huis h = d.huis(sl);
            if (h != null && Ontwerp.inHuis(k, h.soort, p.getX(), p.getY(), p.getZ())) {
                d.thuisUit.put(p.getUUID(), System.currentTimeMillis());
                d.setDirty();
            }
        }
    }

    static void ingelogd(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        HuizenData d = Kwartier.data(p.level().getServer());
        Figuranten.accountTrofeeen(p);   // (this character's boss kills join the account's trophy list)
        Integer eigen = d.sleutelVan(p.getUUID());
        nl.juiced.guhs.feature.vadsrijk.karakter.Karakter.account(p, "huizen").putInt("kavel", eigen == null ? -1 : eigen);   // (mirror for other slices)
        Long t = d.thuisUit.remove(p.getUUID());
        if (t == null) {
            return;
        }
        d.setDirty();
        double uren = (System.currentTimeMillis() - t) / 3_600_000.0;
        long erbij = RustXp.geef(p, uren);
        if (erbij > 0) {
            p.sendSystemMessage(Component.translatable("vadsrijk.huizen.rust.erbij", erbij, RustXp.pot(p)).withStyle(ChatFormatting.AQUA));
        }
    }
}
