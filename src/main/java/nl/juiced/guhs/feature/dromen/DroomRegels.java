package nl.juiced.guhs.feature.dromen;

import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The rules of the dream (ontwerp ronde 9):
 * <ul>
 *   <li>Look around and gather, but no building or breaking: the islands stay pretty for everyone on the server (creative
 *       players may, for building and testing). No buckets either.</li>
 *   <li>Floating: between the islands you drift (soft slow falling and a little higher jump, without particles), taken
 *       off again when you leave.</li>
 *   <li>Whoever sinks below {@link DroomEilanden#BUBBEL_Y} is caught by a Blub bubble and set back on the island they last
 *       stood on.</li>
 *   <li>Dying in a dream is waking up: nothing lost ({@link DroomReis#wakker}).</li>
 * </ul>
 */
public final class DroomRegels {
    private static final String TAG = "guhs_droom_zweeft";

    private DroomRegels() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(DroomRegels::onBreak);
        NeoForge.EVENT_BUS.addListener(DroomRegels::onPlace);
        NeoForge.EVENT_BUS.addListener(DroomRegels::onItem);
        NeoForge.EVENT_BUS.addListener(DroomRegels::onDood);
        NeoForge.EVENT_BUS.addListener(DroomRegels::onTick);
        NeoForge.EVENT_BUS.addListener(DroomRegels::onServerTick);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer sp && !DroomReis.inDroom(sp)) {
                weg(sp);
            }
        });
    }

    /** May this player change blocks here? (Everywhere outside the dream; in it only in creative.) */
    public static boolean magBouwen(Player p) {
        return !DroomReis.inDroom(p) || p.getAbilities().instabuild;
    }

    /** 1.8.0 (droomwolkje): may this player change this block in the dream anyway (their own droomwolkje)? */
    public static java.util.function.BiPredicate<Player, BlockPos> MAG_HIER = (p, pos) -> false;
    /** 1.8.0 (droomwolkje): the spot a dreamer stands on that is no island (e.g. "wolkje:<uuid>"), or null. */
    public static java.util.function.Function<ServerPlayer, String> PLEK = p -> null;
    /** 1.8.0 (droomwolkje): the Blub bubble for a spot of {@link #PLEK}; true when it put the player back itself. */
    public static java.util.function.BiPredicate<ServerPlayer, String> TERUG = (p, plek) -> false;

    /** May this player change this block? ({@link #magBouwen(Player)}, or a spot of their own in the dream.) */
    public static boolean magBouwen(Player p, BlockPos pos) {
        return magBouwen(p) || MAG_HIER.test(p, pos);
    }

    private static void nee(Player p) {
        if (p instanceof ServerPlayer sp) {
            sp.sendOverlayMessage(Component.translatable("gui.guhs.dromen.regels.niet_bouwen").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    private static void onBreak(BreakBlockEvent event) {
        if (!magBouwen(event.getPlayer(), event.getPos())) {
            event.setCanceled(true);
            nee(event.getPlayer());
        }
    }

    private static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof Player p && !magBouwen(p, event.getPos())) {
            event.setCanceled(true);
            nee(p);
        }
    }

    private static void onItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getItemStack().getItem() instanceof BucketItem && !magBouwen(event.getEntity(), event.getEntity().blockPosition())) {
            event.setCanceled(true);
            nee(event.getEntity());
        }
    }

    private static void onDood(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && DroomReis.inDroom(p)) {
            event.setCanceled(true);
            p.setHealth(p.getMaxHealth());
            p.clearFire();
            DroomReis.wakker(p);
        }
    }

    private static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || (p.tickCount % 10) != 0) {
            return;
        }
        if (!DroomReis.inDroom(p)) {
            if (p.getPersistentData().getBooleanOr(TAG, false)) {
                weg(p);
            }
            return;
        }
        p.getPersistentData().putBoolean(TAG, true);
        if (!p.getAbilities().flying) {
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, true, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 40, 1, true, false, false));
        }
        if (p.getY() < DroomEilanden.BUBBEL_Y && !p.isSpectator()) {
            bubbel(p);
        } else if (p.onGround()) {
            String plek = PLEK.apply(p);
            DroomReis.data(p).putString("Laatste", plek != null ? plek : DroomEilanden.dichtstbij(p.position()));
            if (plek == null) {
                steiger(p);
            }
        }
    }

    /** The Blub bubble: back on the island this player last stood on. */
    static void bubbel(ServerPlayer p) {
        ServerLevel level = (ServerLevel) p.level();
        String eiland = DroomReis.data(p).getStringOr("Laatste", DroomEilanden.PLEIN);
        if (!DroomEilanden.IDS.contains(eiland) && TERUG.test(p, eiland)) {
            return;
        }
        if (!DroomEilanden.IDS.contains(eiland)) {
            eiland = DroomEilanden.PLEIN;
        }
        level.sendParticles(ParticleTypes.BUBBLE, p.getX(), p.getY() + 1, p.getZ(), 40, 0.6, 0.8, 0.6, 0.1);
        level.playSound(null, p.blockPosition(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.PLAYERS, 1f, 1.4f);
        DroomEilanden.zorg(level, eiland);
        DroomReis.zetOp(p, level, eiland, DroomEilanden.opzet(eiland).aankomst());
        p.sendOverlayMessage(Component.translatable("gui.guhs.dromen.regels.bubbel").withStyle(ChatFormatting.AQUA));
    }

    /** Standing on a steiger: float to where it leads (islands: the plein; the plein: its island). */
    static void steiger(ServerPlayer p) {
        if (DroomReis.sindsAankomst(p) < 60) {
            return;
        }
        String hier = DroomEilanden.dichtstbij(p.position());
        DroomEilanden.Opzet o = DroomEilanden.opzet(hier);
        if (o.steiger() != null && dichtbij(p, hier, o.steiger())) {
            DroomReis.zweef(p, DroomEilanden.PLEIN);
            return;
        }
        for (Map.Entry<String, Vec3> e : o.steigers().entrySet()) {
            if (dichtbij(p, hier, e.getValue())) {
                DroomReis.zweef(p, e.getKey());
                return;
            }
        }
    }

    private static boolean dichtbij(ServerPlayer p, String eiland, Vec3 rel) {
        Vec3 w = DroomEilanden.wereld(eiland, rel);
        double dx = p.getX() - w.x, dz = p.getZ() - w.z;
        return dx * dx + dz * dz < 1.3 * 1.3 && Math.abs(p.getY() - w.y) < 2;
    }

    /** Leaving the dream: the floating effects go (only the ones the dream gave). */
    static void weg(ServerPlayer p) {
        if (p.getPersistentData().getBooleanOr(TAG, false)) {
            p.getPersistentData().remove(TAG);
            MobEffectInstance v = p.getEffect(MobEffects.SLOW_FALLING), s = p.getEffect(MobEffects.JUMP_BOOST);
            if (v != null && v.isAmbient()) {
                p.removeEffect(MobEffects.SLOW_FALLING);
            }
            if (s != null && s.isAmbient()) {
                p.removeEffect(MobEffects.JUMP_BOOST);
            }
        }
    }

    // =====================================================================================================================
    // the creatures of the islands stay on their island
    // =====================================================================================================================

    /** Below this height a creature that tumbled off an island floats back onto it (the islands span about Y-40 .. Y+40). */
    public static final int VAL_Y = DroomEilanden.Y - 30;

    /** Every two seconds while somebody dreams: Blubs, guhs and other creatures that tumbled off an island come back. */
    private static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 40 != 3) {
            return;
        }
        ServerLevel level = server.getLevel(DromenFeature.DIM);
        if (level == null || level.players().isEmpty()) {
            return;
        }
        for (String eiland : DroomEilanden.IDS) {
            if (DroomEilanden.gebouwd(level, eiland) >= 0) {
                terugOpHetEiland(level, eiland);
            }
        }
    }

    /** A creature (no player) under this island, between the sea and {@link #VAL_Y}, floats back to the island's arrival spot. */
    public static int terugOpHetEiland(ServerLevel level, String eiland) {
        BlockPos m = DroomEilanden.midden(eiland);
        double r = DroomEilanden.MAX_MAAT.getX() / 2.0 + 8;
        AABB onder = new AABB(m.getX() - r, level.getMinY(), m.getZ() - r, m.getX() + r, VAL_Y, m.getZ() + r);
        Vec3 terug = DroomEilanden.wereld(eiland, DroomEilanden.opzet(eiland).aankomst().plek());
        int n = 0;
        for (Entity e : level.getEntities((Entity) null, onder, e -> e instanceof LivingEntity && !(e instanceof Player))) {
            e.teleportTo(terug.x, terug.y, terug.z);
            e.setDeltaMovement(Vec3.ZERO);
            e.fallDistance = 0;
            level.sendParticles(ParticleTypes.BUBBLE_POP, terug.x, terug.y + 0.5, terug.z, 8, 0.3, 0.3, 0.3, 0.02);
            n++;
        }
        return n;
    }
}
