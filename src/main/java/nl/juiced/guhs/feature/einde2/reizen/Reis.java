package nl.juiced.guhs.feature.einde2.reizen;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.appa.AppaGuhEntity;
import nl.juiced.guhs.feature.einde2.appa.Momo;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.registry.ModEntities;

/**
 * einde2 phase B, slice reizen: one flight on a player's Appa ({@link E2Reizen#vlieg}). It seats the team of that moment on
 * Appa's team seats, lets them talk in the chat at their moments ({@link #REGELS}: per flight its own lines; a free flight
 * picks a line now and then), runs Zuka's chase on the flights after the first and second chapter ({@link Jacht}), and
 * notices the landing (Appa's own, or the player arriving another way). Nothing of it is saved but the goal of a story flight
 * ({@link E2Reizen#REIS_KEY}); the actors it makes carry {@link #TIJDELIJK} and are thrown away when a chunk brings them back.
 */
final class Reis {
    /** The tag of every actor of this slice that must not outlive its moment (the team on Appa, Zuka, the campfire copies). */
    static final String TIJDELIJK = "guhs_einde2_reizen_tijdelijk";
    /** Ticks between two lines of the chat, at least. */
    static final int PAUZE = 70;
    /** Blocks from the goal: the line of the end of a flight. */
    static final double EIND = 170;

    enum Wanneer { TIJD, NA_JACHT, EIND }

    /** A line of the chat: when (seconds after the start, after the chase, or near the goal), who says it, which one. */
    record Regel(Wanneer wanneer, int sec, String spreker, int n) {
    }

    private static Regel t(int sec, String spreker, int n) {
        return new Regel(Wanneer.TIJD, sec, spreker, n);
    }

    private static Regel na(int sec, String spreker, int n) {
        return new Regel(Wanneer.NA_JACHT, sec, spreker, n);
    }

    private static Regel eind(String spreker, int n) {
        return new Regel(Wanneer.EIND, 0, spreker, n);
    }

    /**
     * The lines of each flight (lang {@code gui.guhs.einde2_reizen.chat.<vlucht>.<n>}, texts in einde2_reizen.py). A line of a
     * member who is not aboard is skipped; "" = the narrator ("momo" lines are the narrator too: only said with a Momo).
     */
    static final java.util.Map<E2Reizen.Vlucht, List<Regel>> REGELS = java.util.Map.of(
            E2Reizen.Vlucht.START, List.of(t(4, "aang", 1), t(16, "aang", 2), t(30, "momo", 3), t(48, "aang", 4), t(70, "aang", 5),
                    t(95, "aang", 6), eind("aang", 7)),
            E2Reizen.Vlucht.H1_H2, List.of(t(4, "aang", 1), t(18, "aang", 2), na(4, "aang", 3), na(16, "aang", 4), na(32, "momo", 5),
                    eind("aang", 6)),
            E2Reizen.Vlucht.H2_H3, List.of(t(4, "sokka", 1), t(16, "katara", 2), t(22, "aang", 3), t(28, "katara", 4), na(4, "sokka", 5),
                    na(16, "momo", 6), na(26, "sokka", 7), na(40, "katara", 8), na(50, "aang", 9), eind("sokka", 10)),
            E2Reizen.Vlucht.H3_H4, List.of(t(4, "toph", 1), t(10, "sokka", 2), t(24, "katara", 3), t(32, "toph", 4), t(48, "aang", 5),
                    t(62, "sokka", 6), t(70, "momo", 7), t(86, "toph", 8), t(100, "katara", 9), t(110, "", 10), eind("katara", 11)),
            E2Reizen.Vlucht.VRIJ, List.of());
    /** The pool of a free flight: (who, n) of {@code gui.guhs.einde2_reizen.chat.vrij.<n>}. */
    static final List<Regel> VRIJ = List.of(t(0, "aang", 1), t(0, "aang", 2), t(0, "aang", 3), t(0, "katara", 4), t(0, "katara", 5),
            t(0, "sokka", 6), t(0, "sokka", 7), t(0, "sokka", 8), t(0, "toph", 9), t(0, "toph", 10), t(0, "zuka", 11), t(0, "zuka", 12),
            t(0, "momo", 13), t(0, "momo", 14), t(0, "", 15), t(0, "", 16));

    final UUID speler;
    final String structuur;
    final E2Reizen.Vlucht soort;
    final Vec3 doel;
    /** einde2 fix-reizen: where Appa lands (his spot, the rider's spot, the area that counts as arrived). */
    final Appa.Landing landing;
    @Nullable
    Consumer<ServerPlayer> daarna;
    Vec3 start = Vec3.ZERO;
    double totaal;
    int tik;
    boolean geland;
    private boolean wachtNu;
    /** Who sits on the team seats (entity UUIDs) and who that is. */
    private final List<UUID> team = new ArrayList<>();
    private final List<E2Team.Lid> aanBoord = new ArrayList<>();
    private final boolean[] gezegd;
    private int laatsteRegel = -PAUZE;
    private int naJacht = -1;
    private int volgendeVrij = 400;
    private int herinner;
    /** merge_fix1: the first reminder of a waiting story flight after this many ticks, then every {@link #HERINNER_DAARNA}. */
    static final int HERINNER_EERST = 20 * 60, HERINNER_DAARNA = 20 * 60 * 5;
    @Nullable
    Jacht jacht;
    private boolean jachtGehad;

    Reis(ServerPlayer p, String structuur, E2Reizen.Vlucht soort, Appa.Landing landing, @Nullable Consumer<ServerPlayer> daarna) {
        this.speler = p.getUUID();
        this.structuur = structuur;
        this.soort = soort;
        this.doel = landing.appa();
        this.landing = landing;
        this.daarna = daarna;
        this.gezegd = new boolean[REGELS.getOrDefault(soort, List.of()).size()];
    }

    /**
     * The team on Appa's back for this flight: who travels with the player NOW according to the story
     * ({@link E2Team#opAppa}), the same whichever flight it is and however the player got to this step (playtest 1: the
     * first flight showed nobody, a dev jump to a later step showed Aang-guh).
     */
    static List<E2Team.Lid> ledenVoor(ServerPlayer p, E2Reizen.Vlucht soort) {
        return E2Team.opAppa(p);
    }

    void begin(AppaGuhEntity appa) {
        ServerPlayer p = appa.baas();
        if (p == null) {
            return;
        }
        start = p.position();
        totaal = Math.max(1, horizontaal(start, doel));
        for (E2Team.Lid l : ledenVoor(p, soort)) {
            zetOp(p, appa, l);
        }
        for (ServerPlayer k : kijkers(p)) {
            E2Audio.moment(k, "appa");
        }
    }

    /** One team member onto a team seat of Appa (made fresh; false: no seat, or it could not be made). */
    private boolean zetOp(ServerPlayer p, AppaGuhEntity appa, E2Team.Lid l) {
        ServerLevel level = (ServerLevel) appa.level();
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.TRIGGERED);
        if (npc == null) {
            return false;
        }
        npc.setKind(l.kind(p));
        npc.snapTo(appa.getX(), appa.getY() + 2.5, appa.getZ(), appa.getYRot(), 0);
        npc.setInvulnerable(true);
        npc.addTag(TIJDELIJK);
        level.addFreshEntity(npc);
        if (npc.isRemoved() || !appa.zetTeam(npc)) {
            npc.discard();
            LOGGER.warn("[reizen] {} could not sit on Appa's back (removed: {})", l.id(), npc.getRemovalReason());
            return false;
        }
        team.add(npc.getUUID());
        if (!aanBoord.contains(l)) {
            aanBoord.add(l);
        }
        return true;
    }

    /**
     * Once a second while flying (einde2 fix-reizen): every member who belongs on Appa's back sits there. One that is gone
     * (removed by something else, got off) gets a fresh copy; this is logged, so a cause can be found.
     */
    private void teamWacht(ServerPlayer p, AppaGuhEntity appa) {
        if (tik % 20 != 7 || !(appa.level() instanceof ServerLevel level)) {
            return;
        }
        for (E2Team.Lid l : ledenVoor(p, soort)) {
            boolean zit = false;
            for (UUID id : team) {
                Entity e = level.getEntity(id);
                if (e instanceof GuhNpcEntity npc && e.isAlive() && e.getVehicle() == appa && npc.getKind() == l.kind(p)) {
                    zit = true;
                    break;
                }
            }
            if (!zit) {
                LOGGER.warn("[reizen] {} was missing from Appa's back on the flight to {}: seated again", l.id(), structuur);
                team.removeIf(id -> {
                    Entity e = level.getEntity(id);
                    return e == null || !e.isAlive() || e.getVehicle() != appa;
                });
                zetOp(p, appa, l);
            }
        }
    }

    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();

    /** The player and whoever rides in their Appa's basket. */
    List<ServerPlayer> kijkers(ServerPlayer p) {
        List<ServerPlayer> uit = new ArrayList<>();
        uit.add(p);
        AppaGuhEntity appa = Appa.geladen(p);
        if (appa != null) {
            for (Entity e : appa.gasten()) {
                if (e instanceof ServerPlayer g && !uit.contains(g)) {
                    uit.add(g);
                }
            }
        }
        return uit;
    }

    /** Is the player within this many blocks (sideways) of the goal? */
    boolean dichtbij(Player p, double blokken) {
        return horizontaal(p.position(), doel) <= blokken && Math.abs(p.getY() - doel.y) < 40;
    }

    static double horizontaal(Vec3 a, Vec3 b) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Is this flight waiting (the player got off on the way)? */
    boolean wacht() {
        return wachtNu;
    }

    void tick(ServerPlayer p) {
        tik++;
        if (geland) {
            E2Reizen.klaar(p, this);
            return;
        }
        AppaGuhEntity appa = Appa.geladen(p);
        boolean rijdt = appa != null && p.getVehicle() == appa;
        boolean vliegt = rijdt && appa.vluchtDoel() != null;
        if (!vliegt && !rijdt && appa != null && appa.vluchtDoel() != null) {
            return;                                               // (the rider was just taken off: Appa decides this tick)
        }
        if (!vliegt) {
            if (dichtbij(p, 24) && (!rijdt || !appa.isLucht())) {
                E2Reizen.klaar(p, this);                          // (there: Appa landed, or the player came another way)
                return;
            }
            if (!wachtNu) {
                wachtNu = true;
                herinner = HERINNER_EERST;
                if (jacht != null) {
                    jacht.stop(p.level().getServer());
                    jacht = null;
                    jachtGehad = true;
                    naJacht = tik;
                }
            }
            // merge_fix1 (fix-kern's finding: it kept coming in the overworld after a death): only in the Guheinde, rarely,
            // and it names the ways back (Appa's menu: the star or "Verder met het verhaal"; the kaasbol)
            if (herinner-- <= 0 && soort != E2Reizen.Vlucht.VRIJ && GuheindeFeature.isGuheinde(p.level())) {
                herinner = HERINNER_DAARNA;
                p.sendSystemMessage(Component.translatable("gui.guhs.einde2_reizen.wacht", Component.translatable("structure.guhs." + structuur))
                        .withStyle(ChatFormatting.GOLD));
            }
            if (!rijdt && soort == E2Reizen.Vlucht.VRIJ && tik > 20 * 30) {
                E2Reizen.REIZEN.remove(p.getUUID(), this);       // (a free flight that was left: forget it)
                ruimOp(p.level().getServer());
            }
            return;
        }
        wachtNu = false;
        teamWacht(p, appa);
        if (jacht != null) {
            if (!jacht.tick(p, appa, this)) {
                jacht = null;
                jachtGehad = true;
                naJacht = tik;
            }
        } else if (!jachtGehad && magJacht(appa)) {
            jacht = new Jacht(soort);
            jacht.begin(p, appa, this);
        }
        chat(p, appa);
    }

    private boolean magJacht(AppaGuhEntity appa) {
        if (soort != E2Reizen.Vlucht.H1_H2 && soort != E2Reizen.Vlucht.H2_H3 || tik < 20 * 30) {
            return false;
        }
        double rest = horizontaal(appa.position(), doel);
        return rest > 280 && (totaal - rest) >= totaal * 0.2 && GuheindeFeature.isGuheinde(appa.level());
    }

    private void chat(ServerPlayer p, AppaGuhEntity appa) {
        if (tik - laatsteRegel < PAUZE || jacht != null) {
            return;
        }
        if (soort == E2Reizen.Vlucht.VRIJ) {
            if (tik >= volgendeVrij && !aanBoord.isEmpty()) {
                volgendeVrij = tik + 20 * (28 + p.getRandom().nextInt(20));
                List<Regel> kan = new ArrayList<>();
                for (Regel r : VRIJ) {
                    if (magZeggen(p, r.spreker())) {
                        kan.add(r);
                    }
                }
                if (!kan.isEmpty()) {
                    Regel r = kan.get(p.getRandom().nextInt(kan.size()));
                    zeg(p, r);
                }
            }
            return;
        }
        List<Regel> regels = REGELS.getOrDefault(soort, List.of());
        double rest = horizontaal(appa.position(), doel);
        for (int i = 0; i < regels.size(); i++) {
            if (gezegd[i]) {
                continue;
            }
            Regel r = regels.get(i);
            boolean nu = switch (r.wanneer()) {
                case TIJD -> tik >= r.sec() * 20 && rest > EIND;
                case NA_JACHT -> naJacht >= 0 && tik - naJacht >= r.sec() * 20 && rest > EIND;
                case EIND -> rest <= EIND;
            };
            if (!nu) {
                if (r.wanneer() == Wanneer.TIJD && rest <= EIND) {
                    gezegd[i] = true;                             // (too late for it: we are there)
                }
                continue;
            }
            gezegd[i] = true;
            if (magZeggen(p, r.spreker())) {
                zeg(p, r);
                return;
            }
        }
    }

    /** Is the speaker of a line aboard (a member on a team seat, Momo on the player's shoulder; the narrator always)? */
    private boolean magZeggen(ServerPlayer p, String spreker) {
        if (spreker.isEmpty()) {
            return true;
        }
        if (spreker.equals("momo")) {
            return Momo.heeft(p);
        }
        E2Team.Lid lid = E2Team.Lid.van(spreker);
        return lid != null && aanBoord.contains(lid);
    }

    private void zeg(ServerPlayer p, Regel r) {
        laatsteRegel = tik;
        String key = "gui.guhs.einde2_reizen.chat." + soort.name().toLowerCase(Locale.ROOT) + "." + r.n();
        String spreker = r.spreker().equals("momo") ? "" : r.spreker();
        for (ServerPlayer k : kijkers(p)) {
            E2Reizen.zeg(k, spreker, key);
        }
    }

    /** Takes the actors of this flight away (the team off Appa, Zuka's ship). */
    void ruimOp(@Nullable MinecraftServer server) {
        if (jacht != null) {
            jacht.stop(server);
            jacht = null;
        }
        if (server == null) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            for (UUID id : team) {
                Entity e = level.getEntity(id);
                if (e != null) {
                    e.stopRiding();
                    e.discard();
                }
            }
        }
        team.clear();
        aanBoord.clear();
    }

    /** (tests, dev) the members aboard. */
    List<E2Team.Lid> aanBoord() {
        return List.copyOf(aanBoord);
    }
}
