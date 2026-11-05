package nl.juiced.guhs.feature.vadsrijk.zone3.npc;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcHaken;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRollen;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.wereld.Later;
import nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3BazenApi;
import nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3Reizen;
import nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3WereldPlekken;
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * K-INT (zone-3 integration): the glue between the zone-3 slices now that they are in one tree.
 * <ul>
 *   <li>z3-bazen's story moments ({@link Z3BazenApi#MOMENT}) become the npc quest events ({@code NpcApi.meld}).</li>
 *   <li>Flash F1, the Vahoegnir ending and flash F2 are z3-bazen's scenes ({@code Z3Scenes}); the npc steps wait for them and only play
 *       their own version as a fallback for a player who was not there (see {@link #f1Gezien}, {@link #f2Gezien}).</li>
 *   <li>The bindings z3-npc asks for: {@code z3_vahoegnir} (bazen's fight), {@code z3_imker} (the secret ends in a short lore scene in
 *       the Mika-imker's raatzaal: the boss itself is not built), {@code z3_ring_aan}, {@code z3_ketel_uit}, {@code z3_terugkleur}
 *       (small per-player particle effects).</li>
 *   <li>The opening: z3-wereld's film ({@code Z3Reizen.opening}) is THE zone-3 opening; {@code EERSTE_AANKOMST} stays null.</li>
 * </ul>
 */
public final class Z3Koppelingen {
    /** Bazen's per-player "seen" keys (Z3Scenes). */
    static final String BAZEN_F1 = "z3:f1_arm", BAZEN_F2 = "z3:f2_troonzaal";
    static final String IMKER_OPEN = "z3_imker_open";
    /** Players whose Vahoegnir ending runs (bazen): the npc finale waits until the throne room is done (or this deadline). */
    private static final Map<UUID, Long> WACHT_OP_TROON = new ConcurrentHashMap<>();

    private Z3Koppelingen() {
    }

    private static boolean momentGekoppeld;

    /** At server start (after Z3Rollen.koppel): the bindings win over the reflection fallbacks. */
    static void registreer() {
        Z3Reizen.EERSTE_AANKOMST = null;   // (z3-wereld's opening film; z3-npc's Z3Verhaal.opening is only its own fallback)
        if (!momentGekoppeld) {
            momentGekoppeld = true;
            Z3BazenApi.MOMENT.add(Z3Koppelingen::moment);
        }
        Z3BazenApi.HEEFT_SPLINTERS = p -> p.getAbilities().instabuild || Questlog.actief(p, Z3Haken.HQ14) || Questlog.klaar(p, Z3Haken.HQ14);
        Z3BazenApi.HART_VERHAAL = p -> p.getAbilities().instabuild || Questlog.actief(p, Z3Haken.HQ13) || Questlog.klaar(p, Z3Haken.HQ13);   // M-BAZEN
        NpcRollen.koppel("z3_vahoegnir", p -> Z3BazenApi.startVahoegnir(p, false));
        NpcRollen.koppel("z3_imker", Z3Koppelingen::imkerOpen);
        NpcRollen.koppelMet("z3_ring_aan", Z3Koppelingen::ringAan);
        NpcRollen.koppelMet("z3_ketel_uit", (p, id) -> ketelUit(p));
        NpcRollen.koppel("z3_terugkleur", Z3Koppelingen::terugkleur);
    }

    // ---- bazen moments -> quest events -----------------------------------------------------------------------------------------

    static void moment(ServerPlayer p, String id) {
        if (id.startsWith("baas:")) {
            String baas = id.substring(5);
            switch (baas) {
                case "vahoegnir" -> {
                    // (comes through "vahoegnir_opgevrolijkt")
                }
                case "nestwachter" -> NpcApi.meld(p, "z3_nestwachter", "opgevrolijkt");
                default -> NpcApi.meld(p, "z3_" + baas, "verslagen");
            }
            return;
        }
        switch (id) {
            case "piepvel_weg" -> {
                Spelers.data(p).gezien.add("npc:z3_flash_f1");
                NpcApi.meld(p, "z3", "flash_f1");
            }
            case "nestwachter_gered" -> NpcApi.meld(p, "z3_nestwachter", "opgevrolijkt");
            case "vahoegnir_opgevrolijkt" -> {
                Spelers.data(p).gezien.add("npc:z3_finale");   // (bazen's ending is the finale: never the npc one next to it)
                WACHT_OP_TROON.put(p.getUUID(), p.level().getGameTime() + 20 * 90);
                NpcApi.meld(p, "z3_vahoegnir", "opgevrolijkt");
            }
            case "troonzaal_klaar" -> {
                WACHT_OP_TROON.remove(p.getUUID());
                Spelers.data(p).gezien.add("npc:z3_flash_f2");
                if (Questlog.actief(p, Z3Haken.HQ14)) {
                    Z3Verhaal.bloei(p);
                }
            }
            default -> {
            }
        }
    }

    /** Did this player see bazen's arm flash (F1)? Then the npc's own F1 is not played. */
    static boolean f1Gezien(ServerPlayer p) {
        return Spelers.data(p).gezien.contains(BAZEN_F1);
    }

    /** Did this player see bazen's throne room (F2)? Then the hq15 cliffhanger skips the npc's own F2. */
    static boolean f2Gezien(ServerPlayer p) {
        return Spelers.data(p).gezien.contains(BAZEN_F2);
    }

    /** hq14: the bloom waits while bazen's ending + throne room run (or up to 90 s). */
    static boolean wachtOpTroon(ServerPlayer p) {
        Long t = WACHT_OP_TROON.get(p.getUUID());
        if (t == null) {
            return false;
        }
        if (p.level().getGameTime() > t) {
            WACHT_OP_TROON.remove(p.getUUID());
            return false;
        }
        return true;
    }

    // ---- the Mika-imker secret (lore scene; the boss itself is not built) -----------------------------------------------------

    static void imkerOpen(ServerPlayer p) {
        Questlog.zetVlag(p, IMKER_OPEN, true);
        p.sendSystemMessage(Component.translatable("vadsrijk.z3npc.imker.open").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }

    /** Called from Z3Haken's tick: in the raatzaal under the Raattros with all 5 clues: the scene, and the quest step is done. */
    static void imkerTik(ServerPlayer p) {
        if (!Questlog.vlag(p, IMKER_OPEN) || !Questlog.actief(p, "z3_geheim_imker") || !Z3Haken.eenmaligNog(p, "imker_scene")) {
            return;
        }
        BlockPos zaal = Z3WereldPlekken.plek("mika_imker_raatzaal");
        if (zaal == null || !NpcHaken.bij(p, zaal, 7, 5)) {
            return;
        }
        Z3Haken.eenmalig(p, "imker_scene");
        String Z = "vadsrijk.z3npc.imker.";
        String mika = Z + "naam", verteller = "vadsrijk.npc.verteller";
        Praat.scene(p, Z3Verhaal.SLEUTEL, List.of(new Praat.Regel(null, verteller, Z + "scene.0"), new Praat.Regel(null, mika, Z + "scene.1"),
                new Praat.Regel(null, mika, Z + "scene.2"), new Praat.Regel(null, verteller, Z + "scene.3"), new Praat.Regel(null, mika, Z + "scene.4"),
                new Praat.Regel(null, verteller, Z + "scene.5")));
        if (p.level() instanceof ServerLevel sl) {
            Vec3 c = Vec3.atCenterOf(zaal);
            sl.sendParticles(p, ParticleTypes.LARGE_SMOKE, true, true, c.x, c.y + 1, c.z, 60, 3, 1.5, 3, 0.02);
            sl.sendParticles(p, new DustParticleOptions(0xFFC23A, 1.6f), true, true, c.x, c.y + 1.5, c.z, 80, 4, 2, 4, 0);
            sl.playSound(null, zaal, SoundEvents.BEEHIVE_EXIT, SoundSource.PLAYERS, 1.5f, 0.8f);
        }
        Later.na(20 * 14, () -> {
            if (!p.isRemoved()) {
                NpcApi.meld(p, "z3_imker", "verslagen");
            }
        });
    }

    // ---- small per-player effects ---------------------------------------------------------------------------------------------

    /** hq10: the lantern ring n lights up: a ring of little flames around the trunk at the ring's height, for this player. */
    static void ringAan(ServerPlayer p, String arg) {
        BlockPos r = Z3NpcPlekken.poi("z3_ring_" + arg);
        if (r == null || !(p.level() instanceof ServerLevel sl)) {
            return;
        }
        double cx = Z3WereldPlekken.STAM_X + 0.5, cz = Z3WereldPlekken.STAM_Z + 0.5;
        double straal = Math.max(8, Math.hypot(r.getX() + 0.5 - cx, r.getZ() + 0.5 - cz));
        for (int k = 0; k < 3; k++) {
            Later.na(k * 15, () -> {
                for (int i = 0; i < 64; i++) {
                    double a = i * Math.PI * 2 / 64;
                    sl.sendParticles(p, ParticleTypes.FLAME, true, false, cx + Math.cos(a) * straal, r.getY() + 1.2, cz + Math.sin(a) * straal, 1, 0.1, 0.1, 0.1, 0);
                    sl.sendParticles(p, new DustParticleOptions(0xFFD24A, 1.4f), true, false, cx + Math.cos(a) * straal, r.getY() + 1.6, cz + Math.sin(a) * straal,
                            1, 0.1, 0.1, 0.1, 0);
                }
            });
        }
    }

    /** hq11: a kettle goes out: the mould smoke turns into blossom for a moment (the npc slice already puffs smoke). */
    static void ketelUit(ServerPlayer p) {
        if (p.level() instanceof ServerLevel sl) {
            Later.na(20, () -> sl.sendParticles(p, ParticleTypes.CHERRY_LEAVES, true, false, p.getX(), p.getY() + 3, p.getZ(), 60, 6, 2, 6, 0.02));
        }
    }

    /** The finale: a pink recolour wave rolls out over the tree from the nest (for this player, ~6 s). */
    static void terugkleur(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return;
        }
        double cx = Z3WereldPlekken.STAM_X + 0.5, cz = Z3WereldPlekken.STAM_Z + 0.5;
        double y = p.getY();
        for (int s = 0; s < 12; s++) {
            int stap = s;
            Later.na(stap * 10, () -> {
                double straal = 10 + stap * 14;
                int n = 24 + stap * 8;
                for (int i = 0; i < n; i++) {
                    double a = i * Math.PI * 2 / n;
                    sl.sendParticles(p, ParticleTypes.CHERRY_LEAVES, true, false, cx + Math.cos(a) * straal, y - stap * 6, cz + Math.sin(a) * straal, 2, 1.5, 1,
                            1.5, 0);
                    sl.sendParticles(p, new DustParticleOptions(0xFF8AD0, 2.4f), true, false, cx + Math.cos(a) * straal, y - stap * 6 + 1, cz + Math.sin(a) * straal,
                            1, 1, 0.5, 1, 0);
                }
            });
        }
    }
}
