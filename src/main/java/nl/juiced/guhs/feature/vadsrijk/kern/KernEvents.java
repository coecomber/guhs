package nl.juiced.guhs.feature.vadsrijk.kern;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.SweepAttackEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Telegraaf;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootBundel;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Spawners;
import nl.juiced.guhs.feature.vadsrijk.kern.party.Partijen;
import nl.juiced.guhs.feature.vadsrijk.klassen.ridder.Knuffelridder;

/**
 * Kern's game events: the inventory reconciler, the per-tick systems (statuses, telegraphs, numbers, beams, arenas, spawners,
 * skills), the per-player upkeep in the dimension (HUD, energy, vanilla health/hunger frozen, areas), and the rules
 * (vanilla damage rerouted, no dying, no building, no ender chest, no vanilla sweep).
 */
final class KernEvents {
    private KernEvents() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(KernEvents::tick);
        NeoForge.EVENT_BUS.addListener(VadsrijkCommando::register);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                verzoen(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                verzoen(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p) {
                verzoen(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            java.util.UUID id = e.getEntity().getUUID();
            Partijen.verlaat(id);
            AANVALSSNELHEID.remove(id);
            Schade.VOLGENDE_KLAP.remove(id);
            Knuffelsprong.vergeet(id);
        });
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent e) -> Partijen.wisAlles());
        NeoForge.EVENT_BUS.addListener(KernEvents::schade);
        NeoForge.EVENT_BUS.addListener((LivingDeathEvent e) -> {
            if (e.getEntity() instanceof ServerPlayer p && Vadsrijk.in(p) && !p.isCreative()) {
                e.setCanceled(true);
                p.setHealth(p.getMaxHealth());
                Flauwvallen.flauw(p);
            }
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.block.BreakBlockEvent e) -> {
            if (Vadsrijk.in(e.getPlayer()) && !e.getPlayer().isCreative()) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((BlockEvent.EntityPlaceEvent e) -> {
            if (e.getEntity() instanceof Player p && Vadsrijk.in(p) && !p.isCreative()) {
                e.setCanceled(true);
            }
        });
        NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> {
            if (!Vadsrijk.in(e.getEntity())) {
                return;
            }
            var b = e.getLevel().getBlockState(e.getPos()).getBlock();
            if (b instanceof EnderChestBlock || b instanceof ShulkerBoxBlock) {   // no smuggling between the worlds
                e.setCanceled(true);
                return;
            }
            Kampvuren.opKlik(e);
        });
        NeoForge.EVENT_BUS.addListener((SweepAttackEvent e) -> {
            if (Vadsrijk.in(e.getEntity())) {
                e.setSweeping(false);
            }
        });
        NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> {
            if (e.loadedFromDisk() && e.getEntity().entityTags().contains(Getallen.TAG)) {
                e.setCanceled(true);   // (a number/hologram left behind by a crash or an unloaded chunk)
            }
        });
        Schade.NA_KLAP.add(u -> {
            if (u.klap().doel() instanceof ServerPlayer p && u.klap().bron() instanceof nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas b) {
                b.bijdragen().tank(p.getUUID(), u.schade());
            }
        });
        Schade.NA_HEAL.add((bron, doel, n) -> {
            if (bron instanceof ServerPlayer p && p.level() instanceof ServerLevel sl) {
                var a = Arenas.op(p.getX(), p.getY(), p.getZ());
                var b = a == null ? null : Arenas.baas(sl, a.id());
                if (b != null) {
                    b.bijdragen().heal(p.getUUID(), n);
                }
                // loose bosses (no arena): healers within their radius count too
                for (var lb : sl.getEntitiesOfClass(nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas.class, p.getBoundingBox().inflate(48),
                        x -> x.arena() == null && x.losseStraal() > 0 && !x.geplofd() && x.distanceTo(p) <= x.losseStraal())) {
                    lb.bijdragen().heal(p.getUUID(), n);
                }
            }
        });
        Flauwvallen.CHECKPOINT_KIEZER.add(p -> {
            var a = Arenas.op(p.getX(), p.getY(), p.getZ());
            return a == null ? null : a.deur();
        });
    }

    /** The live inventory must match the dimension the player is in. */
    static void verzoen(ServerPlayer p) {
        SpelerData d = Spelers.data(p);
        boolean in = Vadsrijk.in(p);
        if (in && !d.binnen) {
            Inventaris.naarRijk(p);
            Haken.BINNEN.forEach(h -> h.accept(p, true));
            snelheid(p);
            Spelers.sync(p);
            Sieraden.stuur(p);
            Tips.toon(p, "vadsrijk.kern.tip.welkom");
        } else if (!in && d.binnen) {
            Inventaris.naarThuis(p);
            Statussen.wisAlles(p);
            snelheidUit(p);
            Haken.BINNEN.forEach(h -> h.accept(p, false));
            KernPayloads.stuurHud(p);   // (binnen = false: the client hides the HUD)
            Sieraden.stuur(p);
        }
    }

    private static void schade(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !Vadsrijk.in(p)) {
            return;
        }
        var s = e.getSource();
        e.setCanceled(true);
        if (p.isCreative() || p.isSpectator()) {
            return;
        }
        if (s.is(DamageTypes.FELL_OUT_OF_WORLD) || s.is(DamageTypes.GENERIC_KILL)) {
            Flauwvallen.flauw(p);
            return;
        }
        if (s.is(DamageTypes.FALL)) {
            Knuffelsprong.valSchade(p, e.getAmount());   // (none during/just after a Knuffelsprong)
        } else if (s.is(DamageTypes.LAVA) || s.is(DamageTypes.IN_FIRE) || s.is(DamageTypes.ON_FIRE) || s.is(DamageTypes.DROWN)) {
            if (p.invulnerableTime <= 0) {
                Schade.doe(new Schade.Klap(null, p, Spelers.maxVads(p) * 0.05, Element.GEEN, Schade.Soort.WAAR, false, "omgeving"));
                p.invulnerableTime = 10;
            }
        }
    }

    private static void tick(ServerTickEvent.Post e) {
        MinecraftServer server = e.getServer();
        Statussen.tick();
        Telegraaf.tick();
        Getallen.tick();
        Knuffelridder.tick(server);
        long t = server.getTickCount();
        if (t % 5 == 0) {
            LootBundel.tick();
            Arenas.tick(server);
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (t % 20 == 0 && sl != null) {
            Spawners.tick(sl);
        }
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (t % 20 == 0) {
                verzoen(p);
            }
            if (!Vadsrijk.in(p)) {
                continue;
            }
            if (p.getHealth() < p.getMaxHealth()) {
                p.setHealth(p.getMaxHealth());
            }
            p.getFoodData().setFoodLevel(20);
            p.getFoodData().setSaturation(0);   // (no hunger here: also keeps AppleSkin's saturation rings off the HUD)
            p.setAirSupply(p.getMaxAirSupply());
            aanvalTempo(p);
            Knuffelsprong.tick(p);
            if (t % 20 == 0) {
                SpelerData d = Spelers.data(p);
                d.energie = Math.min(Spelers.maxEnergie(p), Spelers.energie(p) + Spelers.klasse(p).energieRegen(Spelers.klasseLevel(p)));
                double max = Spelers.maxVads(p);
                d.vads = Math.min(max, Spelers.vads(p) + max * 0.01);
                gebied(p, d);
                snelheid(p);
            }
            if (t % 5 == 0) {
                KernPayloads.stuurHud(p);
            }
            if (t % 10 == 0 && Partijen.party(p) != null) {
                KernPayloads.stuurParty(p);
            }
        }
    }

    private static final net.minecraft.resources.Identifier LOOP_ID = nl.juiced.guhs.Guhs.id("vr_loopsnelheid"),
            AANVAL_ID = nl.juiced.guhs.Guhs.id("vr_aanvalssnelheid");

    /** Outside the dimension: our speed modifiers off again. */
    static void snelheidUit(ServerPlayer p) {
        var loop = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (loop != null) {
            loop.removeModifier(LOOP_ID);
        }
        var aanval = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
        if (aanval != null) {
            aanval.removeModifier(AANVAL_ID);
        }
    }

    /** Aanvalssnelheid% per player (refreshed every second with the other stats; the weapon is checked every tick). */
    private static final java.util.Map<java.util.UUID, Double> AANVALSSNELHEID = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Attack speed is a weapon stat (kern.Aanvalstempo): the vanilla attack speed attribute = 1 / (the held weapon's interval
     * / (1 + Aanvalssnelheid%)), so the vanilla attack cooldown and its crosshair indicator pace every basic attack (melee,
     * ranged and magic). Every tick (a weapon swap shows at once); only in the dimension.
     */
    static void aanvalTempo(ServerPlayer p) {
        var aanval = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
        if (aanval == null) {
            return;
        }
        double doel = 1.0 / Aanvalstempo.seconden(p, AANVALSSNELHEID.getOrDefault(p.getUUID(), 0.0));
        var oud = aanval.getModifier(AANVAL_ID);
        double zonder = aanval.getValue() - (oud == null ? 0 : oud.amount());
        if (oud != null && Math.abs(zonder + oud.amount() - doel) < 1e-4) {
            return;
        }
        aanval.removeModifier(AANVAL_ID);
        aanval.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(AANVAL_ID, doel - zonder,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
    }

    /** Loopsnelheid -> the movement attribute (and the Aanvalssnelheid cache for {@link #aanvalTempo}). Only in the dimension. */
    private static void snelheid(ServerPlayer p) {
        AANVALSSNELHEID.put(p.getUUID(), Spelers.stats(p).get(Stat.AANVALSSNELHEID));
        aanvalTempo(p);
        var attr = p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        if (attr == null) {
            return;
        }
        var id = LOOP_ID;
        double pct = Math.max(-60, Math.min(60, Spelers.stats(p).get(Stat.LOOPSNELHEID))) / 100.0;
        attr.removeModifier(id);
        if (Math.abs(pct) > 0.001) {
            attr.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(id, pct,
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void gebied(ServerPlayer p, SpelerData d) {
        Gebieden.Gebied g = Gebieden.op(p.getX(), p.getZ());
        String id = g == null ? "" : g.id();
        if (!id.equals(d.gebied)) {
            d.gebied = id;
            if (g != null) {
                Component onder = g.stad() ? Component.translatable("vadsrijk.kern.gebied.veilig")
                        : Component.translatable("vadsrijk.kern.gebied.levels", g.minLevel(), g.maxLevel());
                nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes.titel(p, Component.translatable(g.naamKey()).withStyle(ChatFormatting.WHITE), onder, 40,
                        0xFFC2E8);
                Haken.GEBIED_BETREDEN.forEach(h -> h.accept(p, g.id()));
            }
        }
    }
}
