package nl.juiced.guhs.feature.einde2.reizen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.appa.AppaGuhEntity;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.registry.ModEntities;

/**
 * einde2 phase B, slice reizen (DESIGN "Reizen en ligging", "Zuka's boog"): Zuka's ship chases the player's Appa between the
 * first and second, and the second and third chapter. A short dodge part, never a fight:
 * <ol>
 *   <li>the ship comes out of the clouds behind them, Zuka shouts, the team answers; Appa flies himself from here on (the
 *       server steers him, so a shove works for everyone alike);</li>
 *   <li>{@link #SCHOTEN} fireballs, one every {@link #TUSSEN} ticks, aimed where Appa will be: a warning above the hotbar,
 *       and any movement key (left, right, jump = up, sneak = down) makes Appa dive aside. A fireball that hits only shoves
 *       him (smoke, a plof, no damage to anybody);</li>
 *   <li>the engine gives up ("MIJN... EER...!"), the ship sinks away behind them, the player has the reins again.</li>
 * </ol>
 */
final class Jacht {
    /** Ticks between two fireballs, and how long one flies to where Appa will be. */
    static final int TUSSEN = 50, VLUCHT = 28;
    /** A fireball this close (blocks) to Appa's middle hits him. */
    static final double RAAK = 3.2;
    /** A dodge: how hard sideways, up, down; and how long before the next one. */
    static final double OPZIJ = 0.95, OP = 0.6, NEER = 0.5;
    static final int DUIK_WACHT = 14;
    /** When things happen (ticks after the start). */
    static final int KOMT = 1, ZUKA_ROEPT = 50, TEAM = 110, UITLEG = 170, EERSTE_SCHOT = 200;

    private record Vuurbal(Vec3 pos, Vec3 v, int leeftijd, boolean raak) {
    }

    final E2Reizen.Vlucht soort;
    final int schoten;
    private int t;
    private int gevuurd, ontweken, geraakt;
    private int laatsteDuik = -100;
    private boolean vorigeToets;
    private int eindeSinds = -1;
    private final List<Vuurbal> ballen = new ArrayList<>();
    @Nullable
    private UUID schip, zuka;
    private int kant = 1;

    Jacht(E2Reizen.Vlucht soort) {
        this.soort = soort;
        this.schoten = soort == E2Reizen.Vlucht.H2_H3 ? 8 : 6;
    }

    /** The ship appears behind and above them; Appa flies himself from now on. */
    void begin(ServerPlayer p, AppaGuhEntity appa, Reis reis) {
        ServerLevel level = (ServerLevel) appa.level();
        Vec3 voor = vooruit(appa, reis);
        Vec3 opzij = new Vec3(-voor.z, 0, voor.x);
        Vec3 plek = appa.position().subtract(voor.scale(70)).add(opzij.scale(30)).add(0, 26, 0);
        ZukaSchipEntity s = E2ReizenFeature.ZUKA_SCHIP.get().create(level, EntitySpawnReason.TRIGGERED);
        if (s == null) {
            return;
        }
        Set<UUID> wie = new HashSet<>();
        for (ServerPlayer k : reis.kijkers(p)) {
            wie.add(k.getUUID());
        }
        s.zetKijkers(wie);
        s.snapTo(plek.x, plek.y, plek.z, appa.getYRot(), 0);
        s.addTag(Reis.TIJDELIJK);
        s.stuur(plek, appa.position());
        level.addFreshEntity(s);
        schip = s.getUUID();
        GuhNpcEntity z = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        if (z != null) {
            z.setKind(GuhNpcEntity.Kind.ZUKA_BALLING);
            z.snapTo(plek.x, plek.y + 2, plek.z, appa.getYRot(), 0);
            z.setInvulnerable(true);
            z.addTag(Reis.TIJDELIJK);
            Zicht.alleenVoor(z, p.getUUID());
            level.addFreshEntity(z);
            z.startRiding(s, true, false);
            zuka = z.getUUID();
        }
        Appa.vlieg(p, reis.landing, false);
        for (ServerPlayer k : reis.kijkers(p)) {
            E2Audio.moment(k, "appa_achtervolging");
        }
    }

    /** One tick of the chase. False: it is over (the ship has gone). */
    boolean tick(ServerPlayer p, AppaGuhEntity appa, Reis reis) {
        t++;
        ServerLevel level = (ServerLevel) appa.level();
        ZukaSchipEntity s = schip == null ? null : level.getEntity(schip) instanceof ZukaSchipEntity z ? z : null;
        if (s == null) {
            klaar(p, appa, reis);
            return false;
        }
        String vlucht = soort.name().toLowerCase(Locale.ROOT);
        List<ServerPlayer> kijkers = reis.kijkers(p);
        if (t == KOMT) {
            zeg(kijkers, "", "gui.guhs.einde2_reizen.jacht.komt");
            level.playSound(null, s.getX(), s.getY(), s.getZ(), E2ReizenFeature.SCHIP.get(), SoundSource.NEUTRAL, 6f, 0.7f);
        } else if (t == ZUKA_ROEPT) {
            zeg(kijkers, "zuka_balling", "gui.guhs.einde2_reizen.jacht.zuka." + vlucht);
        } else if (t == TEAM) {
            zeg(kijkers, soort == E2Reizen.Vlucht.H2_H3 ? "sokka" : "aang", "gui.guhs.einde2_reizen.jacht.team." + vlucht);
        } else if (t == UITLEG) {
            zeg(kijkers, "", "gui.guhs.einde2_reizen.jacht.uitleg");
        }
        // where the ship wants to be: beside and a little behind and above Appa, on one side and then the other
        Vec3 voor = vooruit(appa, reis);
        Vec3 opzij = new Vec3(-voor.z, 0, voor.x);
        if (gevuurd == schoten / 2 && t % TUSSEN == 0) {
            kant = -1;
        }
        if (eindeSinds >= 0) {
            Vec3 weg = s.position().subtract(voor.scale(1.2)).add(0, -0.45, 0);
            s.stuur(weg, s.position().add(voor));
            if (t - eindeSinds > 110) {
                s.ejectPassengers();
                Entity z = zuka == null ? null : level.getEntity(zuka);
                if (z != null) {
                    z.discard();
                }
                s.discard();
                klaar(p, appa, reis);
                return false;
            }
            return true;
        }
        Vec3 plek = appa.position().add(opzij.scale(22 * kant)).subtract(voor.scale(t < EERSTE_SCHOT ? 14 : 6)).add(0, 6, 0);
        s.stuur(plek, appa.position().add(voor.scale(8)));
        duik(p, appa);
        if (t >= EERSTE_SCHOT && gevuurd < schoten && (t - EERSTE_SCHOT) % TUSSEN == 0) {
            vuur(level, s, appa, kijkers);
        }
        ballen(level, appa, kijkers);
        if (gevuurd >= schoten && ballen.isEmpty() && t - (EERSTE_SCHOT + (schoten - 1) * TUSSEN) > 70) {
            eindeSinds = t;
            s.sputter();
            zeg(kijkers, "zuka_balling", "gui.guhs.einde2_reizen.jacht.eer");
            for (ServerPlayer k : kijkers) {
                E2Audio.stinger(k, "stinger.mijn_eer");
            }
            level.playSound(null, s.getX(), s.getY(), s.getZ(), E2ReizenFeature.SCHIP.get(), SoundSource.NEUTRAL, 6f, 0.45f);
        }
        return true;
    }

    /** The way to the goal, flat. */
    private static Vec3 vooruit(AppaGuhEntity appa, Reis reis) {
        Vec3 d = new Vec3(reis.doel.x - appa.getX(), 0, reis.doel.z - appa.getZ());
        return d.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : d.normalize();
    }

    /** A movement key pressed (not held from before): Appa dives that way. */
    private void duik(ServerPlayer p, AppaGuhEntity appa) {
        Input in = p.getLastClientInput();
        boolean toets = in.left() || in.right() || in.jump() || in.shift();
        boolean nieuw = toets && !vorigeToets;
        vorigeToets = toets;
        if (!nieuw || t - laatsteDuik < DUIK_WACHT || t < UITLEG - 20) {
            return;
        }
        laatsteDuik = t;
        float yaw = appa.getYRot() * Mth.DEG_TO_RAD;
        Vec3 links = new Vec3(1, 0, 0).yRot(-yaw);
        Vec3 impuls = in.left() ? links.scale(OPZIJ) : in.right() ? links.scale(-OPZIJ) : in.jump() ? new Vec3(0, OP, 0) : new Vec3(0, -NEER, 0);
        appa.duw(impuls);
        appa.level().playSound(null, appa.getX(), appa.getY(), appa.getZ(), net.minecraft.sounds.SoundEvents.ENDER_DRAGON_FLAP, SoundSource.NEUTRAL, 0.7f, 1.5f);
    }

    private void vuur(ServerLevel level, ZukaSchipEntity s, AppaGuhEntity appa, List<ServerPlayer> kijkers) {
        gevuurd++;
        Vec3 van = s.kanon();
        Vec3 midden = appa.position().add(0, 1.6, 0);
        Vec3 naar = midden.add(appa.getDeltaMovement().scale(VLUCHT));
        ballen.add(new Vuurbal(van, naar.subtract(van).scale(1.0 / VLUCHT), 0, false));
        level.playSound(null, van.x, van.y, van.z, E2ReizenFeature.VUURBAL.get(), SoundSource.NEUTRAL, 4f, 0.9f + level.getRandom().nextFloat() * 0.2f);
        for (ServerPlayer k : kijkers) {
            k.sendOverlayMessage(Component.translatable("gui.guhs.einde2_reizen.jacht.vuurbal").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
    }

    private void ballen(ServerLevel level, AppaGuhEntity appa, List<ServerPlayer> kijkers) {
        Vec3 midden = appa.position().add(0, 1.6, 0);
        for (ListIterator<Vuurbal> it = ballen.listIterator(); it.hasNext(); ) {
            Vuurbal b = it.next();
            Vec3 pos = b.pos().add(b.v());
            int leeftijd = b.leeftijd() + 1;
            for (ServerPlayer k : kijkers) {
                deeltjes(level, k, ParticleTypes.FLAME, pos, 8, 0.35);
                deeltjes(level, k, ParticleTypes.LARGE_SMOKE, pos.subtract(b.v().scale(1.5)), 1, 0.2);
                if (leeftijd % 3 == 0) {
                    deeltjes(level, k, ParticleTypes.LAVA, pos, 1, 0.2);
                }
            }
            if (pos.distanceTo(midden) < RAAK) {
                it.remove();
                geraakt++;
                appa.duw(b.v().normalize().scale(0.9).add(0, 0.35, 0));
                level.playSound(null, pos.x, pos.y, pos.z, E2ReizenFeature.VUURBAL_PLOF.get(), SoundSource.NEUTRAL, 3f, 1f);
                for (ServerPlayer k : kijkers) {
                    deeltjes(level, k, ParticleTypes.POOF, pos, 20, 1.0);
                    deeltjes(level, k, ParticleTypes.LARGE_SMOKE, pos, 10, 1.2);
                    k.sendOverlayMessage(Component.translatable("gui.guhs.einde2_reizen.jacht.geraakt").withStyle(ChatFormatting.RED));
                }
                continue;
            }
            if (leeftijd > VLUCHT + 14) {
                it.remove();
                ontweken++;
                for (ServerPlayer k : kijkers) {
                    deeltjes(level, k, ParticleTypes.SMOKE, pos, 6, 0.4);
                    k.sendOverlayMessage(Component.translatable("gui.guhs.einde2_reizen.jacht.ontweken").withStyle(ChatFormatting.GREEN));
                }
                continue;
            }
            it.set(new Vuurbal(pos, b.v(), leeftijd, false));
        }
    }

    private static void deeltjes(ServerLevel level, ServerPlayer k, ParticleOptions d, Vec3 pos, int n, double spreiding) {
        level.sendParticles(k, d, true, true, pos.x, pos.y, pos.z, n, spreiding, spreiding, spreiding, 0.01);
    }

    /** Over: the player steers again, the travel tune comes back, and they hear how they did. */
    private void klaar(ServerPlayer p, AppaGuhEntity appa, Reis reis) {
        if (p.getVehicle() == appa && appa.vluchtDoel() != null) {
            Appa.vlieg(p, reis.landing, true);
        }
        for (ServerPlayer k : reis.kijkers(p)) {
            E2Audio.moment(k, "appa");
            if (gevuurd > 0) {
                nl.juiced.guhs.quest.GuhAdvancements.grant(k, "einde2_reizen_jacht");
                zeg(List.of(k), "", "gui.guhs.einde2_reizen.jacht.weg");
                k.sendSystemMessage(Component.translatable("gui.guhs.einde2_reizen.jacht.score", ontweken, gevuurd).withStyle(ChatFormatting.GOLD));
            }
        }
    }

    /** The ship and Zuka go now (the flight stopped, the player left). */
    void stop(@Nullable MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (UUID id : new UUID[]{zuka, schip}) {
                Entity e = id == null ? null : level.getEntity(id);
                if (e != null) {
                    e.ejectPassengers();
                    e.discard();
                }
            }
        }
    }

    private static void zeg(List<ServerPlayer> kijkers, String spreker, String key) {
        for (ServerPlayer k : kijkers) {
            E2Reizen.zeg(k, spreker, key);
        }
    }

    /** (tests) how it went: {fired, dodged, hit}. */
    int[] stand() {
        return new int[]{gevuurd, ontweken, geraakt};
    }
}
