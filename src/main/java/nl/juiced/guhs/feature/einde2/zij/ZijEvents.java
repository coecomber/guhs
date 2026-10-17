package nl.juiced.guhs.feature.einde2.zij;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.castschurk.CastWezen;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.ring.Zicht;

/**
 * einde2 zij: the events of the slice, in one place: the player tick (powers every tick, the side places at their own
 * pace), the clicks on blocks (the schminktafel, the planetarium, the harbour board), the Ender-Mika traders, soft
 * landings, the poem after the Guheinde, a logout, and a small "later" for timed things (no thread, server ticks).
 */
public final class ZijEvents {
    private record Taak(long op, Runnable wat) {
    }

    private static final List<Taak> TAKEN = new ArrayList<>();
    private static long nu;

    private ZijEvents() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(ZijEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ZijEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppingEvent e) -> ZijKrachten.allesTerug(e.getServer()));
        NeoForge.EVENT_BUS.addListener(ZijEvents::onLogout);
        NeoForge.EVENT_BUS.addListener(ZijEvents::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(Leegtehaven::opKlik);
        NeoForge.EVENT_BUS.addListener(ZijEvents::onFall);
        NeoForge.EVENT_BUS.addListener(ZijScenes::opDimensie);
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, ZijActeurs::onJoin);
    }

    /** Runs this on the server thread after so many server ticks (lost when the server stops: only for small finishing touches). */
    public static void later(int ticks, Runnable wat) {
        synchronized (TAKEN) {
            TAKEN.add(new Taak(nu + Math.max(1, ticks), wat));
        }
    }

    static void onServerTick(ServerTickEvent.Post event) {
        nu++;
        List<Runnable> klaar = new ArrayList<>();
        synchronized (TAKEN) {
            for (Iterator<Taak> it = TAKEN.iterator(); it.hasNext(); ) {
                Taak t = it.next();
                if (t.op() <= nu) {
                    klaar.add(t.wat());
                    it.remove();
                }
            }
        }
        for (Runnable r : klaar) {
            try {
                r.run();
            } catch (RuntimeException ex) {
                com.mojang.logging.LogUtils.getLogger().warn("Guhs einde2 zij: a timed task failed", ex);
            }
        }
        ZijKrachten.serverTick();
        MinecraftServer server = event.getServer();
        ServerLevel guheinde = server.getLevel(GuheindeFeature.GUHEINDE);
        if (guheinde != null) {
            Luchttempel.serverTick(guheinde);
        }
    }

    static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.isSpectator()) {
            return;
        }
        ZijKrachten.tick(p);
        Leeuwschildpad.vang(p);
        int tel = p.tickCount + p.getId();
        if (tel % 10 == 0) {
            KrachtPayloads.stuur(p, false);
        }
        if (p.level().dimension() != GuheindeFeature.GUHEINDE) {
            return;
        }
        Ringenrace.tick(p);
        Vuurtempel.tick(p);
        KyoshiDorp.tick(p);
        if (tel % 5 == 0) {
            Omashguh.tick(p);
        }
        if (tel % 10 == 0) {
            Luchttempel.tick(p);
            VetbootjeOpdracht.tick(p);
            Leeuwschildpad.tick(p);
        }
        if (tel % 40 == 0) {
            MikaKok.houdHavenkok(p);
        }
        if (tel % 100 == 0) {
            Ringenrace.bord(p);
            zicht(p);
        }
    }

    /** Residents made before this slice listened ({@code Bewoners.BIJ_MAKEN}) still get their visibility. */
    private static void zicht(ServerPlayer p) {
        for (Entity e : p.level().getEntities((Entity) null, p.getBoundingBox().inflate(64),
                e -> (e instanceof GuhNpcEntity || e instanceof CastWezen) && !e.entityTags().contains(Zicht.MERK))) {
            String plek = Zij.plek(e);
            if (!plek.isEmpty()) {
                Luchttempel.zicht(e, plek);
                Vuurtempel.zicht(e, plek);
                MikaKok.zicht(e, plek);
            }
        }
    }

    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }
        if (p.level().dimension() != GuheindeFeature.GUHEINDE) {
            return;
        }
        if (KyoshiDorp.klikBlok(p, event.getPos()) || Bibliotheek.klikBlok(p, event.getPos()) || VetbootjeOpdracht.klikBlok(p, event.getPos())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && ZijKrachten.zachteLanding(p)) {
            event.setDamageMultiplier(0f);
        }
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            onLogoutVergeet(p);
        }
    }

    /** Forgets everything of this player that is only kept while they play (logout, the dev command wis). */
    static void onLogoutVergeet(ServerPlayer p) {
        UUID id = p.getUUID();
        Krachten.vergeet(id);
        ZijKrachten.vergeet(id);
        KrachtPayloads.vergeet(id);
        ZijPlek.vergeet(id);
        Omashguh.vergeet(id);
        Vuurtempel.vergeet(id);
        KyoshiDorp.vergeet(id);
        Leeuwschildpad.vergeet(id);
        VetbootjeOpdracht.vergeet(id);
        Ringenrace.vergeet(id);
        if (p.level().getServer() != null) {
            ServerLevel guheinde = p.level().getServer().getLevel(GuheindeFeature.GUHEINDE);
            if (guheinde != null) {
                Luchttempel.ruim(guheinde, id);
            }
        }
    }
}
