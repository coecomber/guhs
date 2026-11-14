package nl.juiced.guhs.feature.vadsrijk.zone3.npc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcApi;
import nl.juiced.guhs.feature.vadsrijk.npc.NpcRollen;
import nl.juiced.guhs.feature.vadsrijk.npc.Npcs;
import nl.juiced.guhs.feature.vadsrijk.npc.Quest;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.npc.Quests;

/**
 * The story moments of zone 3 (all non-click camera scenes, kern RijkScene) and the airship to Honingtak:
 * <ul>
 *   <li>{@link #opening}: z3_hq01, the first flight: along the endless bark wall, through the clouds, the crown in gold, waving
 *       beekeepers; down along the trunk into the roots where two grey-blue eyes open;</li>
 *   <li>{@link #bam}: z3_hq02, a Mika zeppelin sprays grey over the market, Bladrovers drop onto the plaza (guhs hide, nobody hit);</li>
 *   <li>{@link #nest}: z3_hq05, the old nest flashback: the pink dragon lays a glowing egg and wraps his wings around it;</li>
 *   <li>{@link #flashF1}: z3_hq10, after Graaf Piepvel: a grey mist through a knot hole, Mika henchmen drag the Count into it;</li>
 *   <li>{@link #ei}: z3_hq13, the egg in the Nestkamer; {@link #rit}: z3_hq14, the escape up Het Rijpad (the riding spiral) to the old nest;</li>
 *   <li>{@link #finale}: z3_hq14, the splinters go back, the egg hatches into Vahoegje, the beam to Hoogvads, {@code zone3_klaar};</li>
 *   <li>{@link #cliffhanger}: z3_hq15, from the Nestkapel to the far east, then flash F2 (the throne room).</li>
 * </ul>
 * Bindings other slices may add (a binding wins): {@code z3_luchtlijn} (arg "honingtak"), {@code z3_bladrovers} (arg "x,y,z,n"),
 * {@code z3_vahoegnir} (z3-bazen: start the deelbaas fight at the nest for this player/party; without it the story goes straight on).
 */
public final class Z3Verhaal {
    public static final String SLEUTEL = "vr_z3_npc";
    public static final String VLAG_ZONE3 = "zone3_klaar";
    static final String Z = "vadsrijk.z3npc.";
    static final int OPENING_TICKS = 480, VLIEG_TICKS = 160, BAM_TICKS = 200, NEST_TICKS = 260, F1_TICKS = 240, EI_TICKS = 140, RIT_TICKS = 400,
            FINALE_TICKS = 520, CLIFF_TICKS = 120, F2_TICKS = 140;

    private record Loopt(String soort, long begin, String arg) {
    }

    private static final Map<UUID, Loopt> LOOPT = new HashMap<>();
    private static final Map<UUID, Entity> DRAAK = new HashMap<>();

    private Z3Verhaal() {
    }

    // ---- travel ----------------------------------------------------------------------------------------------------------------

    /** The airship line to Honingtak: after the zone-2 story and from level 30. */
    public static boolean magNaarHoningtak(ServerPlayer p) {
        return Questlog.vlag(p, nl.juiced.guhs.feature.vadsrijk.zone2.npc.Z2Verhaal.VLAG_ZONE2) && Spelers.level(p) >= 30;
    }

    public static boolean openingGezien(ServerPlayer p) {
        return Spelers.data(p).gezien.contains("npc:z3_opening");
    }

    /**
     * To Honingtak. z3-wereld's line wins when it is in the tree ({@code Z3Reizen.naarHoningtak}: the gate, the landing at the mast,
     * its opening film on the first flight); otherwise the first time with our opening, later a short flight.
     */
    public static void vlieg(ServerPlayer p) {
        Quest q = Quests.van(Z3Haken.HQ01);
        if (q != null && Questlog.beschikbaar(p, q)) {
            Questlog.neemAan(p, q);
        }
        if (NpcRollen.doe("z3_luchtlijn", p, "honingtak")) {
            return;
        }
        if (Z3Rollen.bestaat("zone3.wereld.Z3Reizen")) {
            Z3Rollen.reflectie(p, "honingtak", "zone3.wereld.Z3Reizen#naarHoningtak");
            return;   // (its gate said no, or it flew: either way never our own flight next to it)
        }
        if (!openingGezien(p)) {
            opening(p);
            return;
        }
        zet(p, Z3NpcPlekken.DOK, 270f);
        Vec3[] k = Z3NpcPlekken.VLUCHT;
        RijkScenes.speel(p, RijkScene.maak("npc:z3_vlucht", VLIEG_TICKS).camera(0, k[0], k[1]).camera(VLIEG_TICKS / 2, k[2], k[3])
                .camera(VLIEG_TICKS, k[4], k[5]).titel(Z + "scene.vlucht.titel", Z + "scene.vlucht.onder", VLIEG_TICKS - 60, 60, 0xFFD24A).bouw());
        geluid(p, SoundEvents.ELYTRA_FLYING, 0.5f, 0.8f);
        loopt(p, "vlucht", "");
    }

    // ---- the opening ----------------------------------------------------------------------------------------------------------

    public static void opening(ServerPlayer p) {
        Spelers.data(p).gezien.add("npc:z3_opening");
        zet(p, Z3NpcPlekken.OPENING_ANKER[0], 90f);   // (under the camera: the chunks of the trunk and the crown load)
        Vec3[] k = Z3NpcPlekken.OPENING;
        int[] t = Z3NpcPlekken.OPENING_T;
        RijkScene.Builder b = RijkScene.maak("npc:z3_opening", OPENING_TICKS);
        for (int i = 0; i < t.length; i++) {
            b.camera(t[i], k[2 * i], k[2 * i + 1]);
        }
        b.camera(OPENING_TICKS, k[k.length - 2].add(0, 1, 6), Z3NpcPlekken.OGEN);
        RijkScenes.speel(p, b.titel(Z + "scene.opening.titel", Z + "scene.opening.onder", 210, 90, 0xFFD24A).bouw());
        geluid(p, SoundEvents.ELYTRA_FLYING, 0.7f, 0.6f);
        loopt(p, "opening", "");
    }

    private static void openingTik(ServerPlayer p, ServerLevel sl, int t) {
        Vec3 cam = camera(Z3NpcPlekken.OPENING, Z3NpcPlekken.OPENING_T, t);
        if (t == 255) {
            zet(p, Z3NpcPlekken.OPENING_ANKER[1], 90f);
        }
        if (t == 330) {
            zet(p, Z3NpcPlekken.OPENING_ANKER[2], 90f);
        }
        if (t < 200) {
            // the bark wall slides by; clouds around the ship
            deeltjes(p, sl, ParticleTypes.CLOUD, cam.add(-4, 2, -4), 16, 10, 4, 0.02);
            if (t == 20) {
                zeg(p, "scene.schors", ChatFormatting.GOLD);
            }
        }
        if (t == 200) {
            geluid(p, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, 1.2f);
            zeg(p, "scene.kruin", ChatFormatting.GOLD);
        }
        if (t >= 200 && t < 340) {
            // the crown in gold: golden dust and blossom
            deeltjes(p, sl, new DustParticleOptions(0xFFD24A, 1.8f), cam.add(0, -6, -10), 20, 16, 6, 0);
            deeltjes(p, sl, ParticleTypes.CHERRY_LEAVES, cam.add(0, 2, -8), 8, 10, 3, 0);
        }
        if (t >= 270 && t < 340 && t % 8 == 0) {
            // the beekeepers wave their veils
            deeltjes(p, sl, ParticleTypes.HEART, Vec3.atCenterOf(Z3NpcPlekken.PLEIN).add(0, 2, 0), 3, 6, 1, 0);
            deeltjes(p, sl, new DustParticleOptions(0xFFFFFF, 1.2f), Vec3.atCenterOf(Z3NpcPlekken.PLEIN).add(0, 2.5, 0), 8, 6, 0.6, 0);
        }
        if (t == 280) {
            zeg(p, "scene.zwaaien", ChatFormatting.LIGHT_PURPLE);
        }
        if (t == 340) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 120, 0, false, false));
            geluid(p, SoundEvents.AMBIENT_CAVE.value(), 1.2f, 0.6f);
        }
        if (t >= 430) {
            // two grey-blue eyes open in the dark (the Schimmelkolos)
            Vec3 o = Z3NpcPlekken.OGEN;
            deeltjes(p, sl, new DustParticleOptions(0x7F9FB8, 1.4f), o.add(-1.2, 0, 0), 3, 0.08, 0.05, 0);
            deeltjes(p, sl, new DustParticleOptions(0x7F9FB8, 1.4f), o.add(1.2, 0, 0), 3, 0.08, 0.05, 0);
            if (t == 440) {
                geluid(p, SoundEvents.WARDEN_HEARTBEAT, 2f, 0.5f);
                zeg(p, "scene.gekraak", ChatFormatting.DARK_AQUA);
            }
        }
    }

    // ---- the small BAM (z3_hq02) ------------------------------------------------------------------------------------------------

    public static void bam(ServerPlayer p) {
        Spelers.data(p).gezien.add("npc:z3_bam");
        Vec3[] k = Z3NpcPlekken.BAM;
        RijkScenes.speel(p, RijkScene.maak("npc:z3_bam", BAM_TICKS).camera(0, k[0], k[1]).camera(BAM_TICKS, k[2], k[3])
                .titel(Z + "scene.bam.titel", Z + "scene.bam.onder", 40, 70, 0x8A90A0).bouw());
        geluid(p, SoundEvents.ENDER_DRAGON_FLAP, 1.0f, 1.6f);
        loopt(p, "bam", "");
    }

    private static void bamTik(ServerPlayer p, ServerLevel sl, int t) {
        Vec3 z = Z3NpcPlekken.ZEPPELIN_VAN.lerp(Z3NpcPlekken.ZEPPELIN_NAAR, Math.min(1, t / 180.0));
        // the cheese zeppelin: a yellow-grey cloud with a propeller hum, spraying grey
        deeltjes(p, sl, new DustParticleOptions(0xE8C860, 3.0f), z, 30, 4, 1.5, 0);
        deeltjes(p, sl, ParticleTypes.LARGE_SMOKE, z.add(0, -2, 0), 6, 3, 1, 0.01);
        if (t > 30 && t % 2 == 0) {
            deeltjes(p, sl, ParticleTypes.WHITE_ASH, z.add(0, -6, 0), 40, 5, 3, 0);
        }
        if (t % 20 == 0) {
            geluid(p, SoundEvents.MINECART_RIDING, 0.6f, 0.5f);
        }
        if (t == 40) {
            zeg(p, "scene.bam.verstop", ChatFormatting.YELLOW);
        }
        if (t == 120) {
            zeg(p, "scene.bam.pamflet", ChatFormatting.GRAY);
            Vec3 c = Vec3.atBottomCenterOf(Z3NpcPlekken.PLEIN);
            if (!NpcRollen.doe("z3_bladrovers", p, (int) c.x + "," + (int) c.y + "," + (int) c.z + ",8")) {
                bladrovers(sl, c, 8);
            }
            geluid(p, SoundEvents.SLIME_SQUISH, 2.0f, 0.4f);
        }
    }

    /** Fallback when z3-wezens did not bind its Bladrovers: zone 1's klodders dropped by the zeppelin (they cheer up as usual). */
    static void bladrovers(ServerLevel sl, Vec3 c, int n) {
        // 1.10.1: every player who accepts hq02 used to add 8 more: now it tops the plaza up to 8 (they no longer pile up)
        int al = sl.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(c, c).inflate(16, 8, 16),
                x -> x.isAlive() && x.entityTags().contains("z3_bladrover")).size();
        for (int i = al; i < n; i++) {
            Mob m = nl.juiced.guhs.feature.vadsrijk.wezens.WezensRegistry.KLODDER.get().create(sl, EntitySpawnReason.EVENT);
            if (m != null) {
                double a = i * Math.PI * 2 / n;
                m.snapTo(c.x + Math.cos(a) * 6, c.y + 1, c.z + Math.sin(a) * 6, (float) Math.toDegrees(a), 0);
                m.addTag("z3_bladrover");
                sl.addFreshEntity(m);
            }
        }
    }

    // ---- the old nest (z3_hq05) ---------------------------------------------------------------------------------------------------

    public static void nest(ServerPlayer p) {
        Spelers.data(p).gezien.add("npc:z3_nest");
        Vec3[] k = Z3NpcPlekken.NEST_SCENE;
        RijkScenes.speel(p, RijkScene.maak("npc:z3_nest", NEST_TICKS).camera(0, k[0], k[1]).camera(80, k[2], k[3]).camera(NEST_TICKS, k[4], k[5])
                .titel(Z + "scene.nest.titel", Z + "scene.nest.onder", 100, 120, 0xFF8AD0).bouw());
        geluid(p, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, 0.7f);
        loopt(p, "nest", "");
    }

    private static void nestTik(ServerPlayer p, ServerLevel sl, int t) {
        Vec3 n = Z3NpcPlekken.NEST;
        if (t < 80) {
            deeltjes(p, sl, ParticleTypes.WHITE_ASH, n.add(0, 3, 0), 8, 10, 2, 0);
            if (t == 20) {
                zeg(p, "scene.nest.leeg", ChatFormatting.GRAY);
            }
            if (t == 76) {
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, false));   // (into the memory)
                geluid(p, SoundEvents.BEACON_ACTIVATE, 1.5f, 1.4f);
            }
            return;
        }
        if (t == 90) {
            Entity d = nl.juiced.guhs.feature.vadsrijk.wezens.draak.DraakVlucht.vlieg(sl, List.of(Z3NpcPlekken.NEST_DRAAK), NEST_TICKS - 100, false);
            if (d != null) {
                DRAAK.put(p.getUUID(), d);
            }
            zeg(p, "scene.nest.vroeger", ChatFormatting.LIGHT_PURPLE);
        }
        Entity d = DRAAK.get(p.getUUID());
        if (d != null && !d.isRemoved()) {
            Vec3 v = d.position();
            deeltjes(p, sl, new DustParticleOptions(0xFF8AD0, 3.0f), v.add(0, 2, 0), 40, 5, 2.5, 0);
            deeltjes(p, sl, ParticleTypes.CHERRY_LEAVES, v, 10, 6, 2, 0);
        }
        if (t >= 170) {
            // the glowing egg, the wings wrapped around it
            deeltjes(p, sl, new DustParticleOptions(0xFFB3E0, 2.0f), n.add(0, 1, 0), 10, 0.6, 0.6, 0);
            deeltjes(p, sl, ParticleTypes.END_ROD, n.add(0, 1.2, 0), 2, 0.4, 0.4, 0.01);
            if (t == 180) {
                geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME, 2f, 0.6f);
                zeg(p, "scene.nest.ei", ChatFormatting.LIGHT_PURPLE);
            }
        }
    }

    // ---- flash F1 (z3_hq10) -----------------------------------------------------------------------------------------------------

    public static void flashF1(ServerPlayer p) {
        Spelers.data(p).gezien.add("npc:z3_flash_f1");
        Vec3[] k = Z3NpcPlekken.FLASH_F1;
        RijkScenes.speel(p, RijkScene.maak("npc:z3_flash_f1", F1_TICKS).camera(0, k[0], k[1]).camera(70, k[2], k[3]).camera(140, k[4], k[5])
                .camera(F1_TICKS, k[4].add(0, 1, 4), k[5]).bouw());
        loopt(p, "f1", "");
    }

    private static void f1Tik(ServerPlayer p, ServerLevel sl, int t) {
        Vec3 graaf = Z3NpcPlekken.PELSZAAL.add(0, 1, 0), gat = Z3NpcPlekken.KWASTGAT;
        if (t == 5) {
            zeg(p, "scene.f1.cape", ChatFormatting.GRAY);
        }
        if (t == 50) {
            geluid(p, SoundEvents.POWDER_SNOW_STEP, 2f, 0.5f);
            zeg(p, "scene.f1.kraak", ChatFormatting.DARK_GRAY);
        }
        if (t >= 70 && t < 150) {
            // (lore correction: the Keizer's reach is figurative) a grey mist bank rolls out of the knot hole over the Count, then back
            double f = t < 110 ? (t - 70) / 40.0 : 1 - (t - 110) / 40.0;
            Vec3 kop = gat.lerp(graaf, f);
            for (double s = 0; s <= f; s += 0.08) {
                deeltjes(p, sl, new DustParticleOptions(0x8A8A9A, 3.2f), gat.lerp(graaf, s), 3, 0.9, 0.6, 0);
            }
            deeltjes(p, sl, new DustParticleOptions(0x5A5A66, 3.8f), kop, 8, 1.4, 0.9, 0);
            if (t == 100) {
                zeg(p, "scene.f1.nee", ChatFormatting.WHITE);
            }
            if (t == 110) {
                geluid(p, SoundEvents.ENDERMAN_TELEPORT, 1.5f, 0.5f);
                zeg(p, "scene.f1.floep", ChatFormatting.GRAY);
            }
        }
        if (t == 170) {
            zeg(p, "scene.f1.afdruk", ChatFormatting.DARK_PURPLE);
        }
        if (t == 200) {
            deeltjes(p, sl, ParticleTypes.HEART, graaf, 4, 0.6, 0.4, 0);
            zeg(p, "scene.f1.piepje", ChatFormatting.LIGHT_PURPLE);
        }
    }

    // ---- the egg (z3_hq13) ------------------------------------------------------------------------------------------------------

    public static void ei(ServerPlayer p) {
        Spelers.data(p).gezien.add("npc:z3_ei");
        Vec3 e = Z3NpcPlekken.EI;
        RijkScenes.speel(p, RijkScene.maak("npc:z3_ei", EI_TICKS).camera(0, e.add(6, 4, 6), e.add(0, 0.6, 0)).camera(EI_TICKS, e.add(2, 1.6, 2),
                e.add(0, 0.6, 0)).titel(Z + "scene.ei.titel", Z + "scene.ei.onder", 40, 90, 0xFF8AD0).bouw());
        zeg(p, "scene.ei.nep", ChatFormatting.LIGHT_PURPLE);   // K-INT: the egg in Baron Bleu's room was a decoy
        loopt(p, "ei", "");
    }

    private static void eiTik(ServerPlayer p, ServerLevel sl, int t) {
        Vec3 e = Z3NpcPlekken.EI.add(0, 0.6, 0);
        deeltjes(p, sl, new DustParticleOptions(0xFF8AD0, 1.6f), e, 6, 0.4, 0.4, 0);
        int slag = t < 80 ? 30 : 12;   // (the little heartbeat goes faster when you pick it up)
        if (t % slag == 0) {
            geluid(p, SoundEvents.WARDEN_HEARTBEAT, 1.2f, 1.8f);
            deeltjes(p, sl, ParticleTypes.HEART, e.add(0, 0.6, 0), 1, 0.2, 0.2, 0);
        }
        if (t == 80) {
            zeg(p, "scene.ei.warm", ChatFormatting.LIGHT_PURPLE);
        }
    }

    // ---- the escape ride (z3_hq14) ------------------------------------------------------------------------------------------------

    public static void rit(ServerPlayer p) {
        Spelers.data(p).gezien.add("npc:z3_rit");
        Vec3[] k = Z3NpcPlekken.RIT;
        RijkScene.Builder b = RijkScene.maak("npc:z3_rit", RIT_TICKS);
        for (int i = 0; i < k.length / 2; i++) {
            b.camera(i * RIT_TICKS / (k.length / 2 - 1), k[2 * i], k[2 * i + 1]);
        }
        RijkScenes.speel(p, b.titel(Z + "scene.rit.titel", Z + "scene.rit.onder", 10, 70, 0xFF8AD0).bouw());
        if (p.level() instanceof ServerLevel sl) {
            Entity d = nl.juiced.guhs.feature.vadsrijk.wezens.draak.DraakVlucht.vlieg(sl, List.of(Z3NpcPlekken.RIT_DRAAK), RIT_TICKS, true);
            if (d != null) {
                DRAAK.put(p.getUUID(), d);
            }
        }
        loopt(p, "rit", "");
    }

    private static void ritTik(ServerPlayer p, ServerLevel sl, int t) {
        Vec3 cam = camera(Z3NpcPlekken.RIT, null, t * 1.0 / RIT_TICKS);
        if (t % 4 == 0) {
            // the tree shakes: bark and twigs fall past the camera
            deeltjes(p, sl, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SPRUCE_LOG.defaultBlockState()), cam.add(0, 8, 0), 20, 6, 4, 0.1);
            deeltjes(p, sl, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.OAK_LEAVES.defaultBlockState()), cam.add(0, 10, 0), 12, 8, 4, 0.1);
        }
        if (t % 40 == 0) {
            geluid(p, SoundEvents.WOOD_BREAK, 1.6f, 0.4f);
        }
        if (t == 30) {
            zeg(p, "scene.rit.schudt", ChatFormatting.GOLD);
        }
        if (t == 200) {
            zeg(p, "scene.rit.zweef", ChatFormatting.AQUA);
            geluid(p, SoundEvents.ELYTRA_FLYING, 1f, 1.2f);
        }
        if (t == 330) {
            zeg(p, "scene.rit.nest", ChatFormatting.LIGHT_PURPLE);
        }
    }

    // ---- the finale (z3_hq14) --------------------------------------------------------------------------------------------------------

    public static void finale(ServerPlayer p) {
        Spelers.data(p).gezien.add("npc:z3_finale");
        Vec3[] k = Z3NpcPlekken.FINALE;
        RijkScenes.speel(p, RijkScene.maak("npc:z3_finale", FINALE_TICKS).camera(0, k[0], k[1]).camera(200, k[2], k[3]).camera(360, k[2], k[3])
                .camera(FINALE_TICKS, k[4], k[5]).titel(Z + "scene.finale.titel", Z + "scene.finale.onder", 130, 100, 0xFF8AD0).bouw());
        if (p.level() instanceof ServerLevel sl) {
            Vec3 n = Z3NpcPlekken.NEST;
            Entity d = nl.juiced.guhs.feature.vadsrijk.wezens.draak.DraakVlucht.vlieg(sl, List.of(n.add(0, 7, -12), n.add(0.01, 7, -12.2)), 400, false);
            if (d != null) {
                DRAAK.put(p.getUUID(), d);
            }
        }
        loopt(p, "finale", "");
    }

    private static void finaleTik(ServerPlayer p, ServerLevel sl, int t) {
        Vec3 n = Z3NpcPlekken.NEST, borst = n.add(0, 6, -9);
        if (t < 90) {
            // the five splinters float together into one pink heart crystal and glide into his chest
            double f = t / 90.0;
            for (int i = 0; i < 5; i++) {
                double a = i * Math.PI * 2 / 5 + t * 0.08;
                Vec3 s = n.add(Math.cos(a) * 3 * (1 - f), 2 + f * 4, Math.sin(a) * 3 * (1 - f) - 9 * f);
                deeltjes(p, sl, new DustParticleOptions(0xFF5FB8, 1.8f), s, 3, 0.05, 0.05, 0);
            }
            if (t == 10) {
                zeg(p, "scene.finale.splinters", ChatFormatting.LIGHT_PURPLE);
                geluid(p, SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 1.0f);
            }
        }
        if (t >= 90 && t < 170) {
            // the black fluff blows off him like dandelion seeds; he is pink again
            deeltjes(p, sl, ParticleTypes.WHITE_ASH, borst.add(0, 2, 0), 40, 6, 3, 0.05);
            deeltjes(p, sl, new DustParticleOptions(0xFF8AD0, 3.0f), borst, 30, 5, 2.5, 0);
            if (t == 100) {
                geluid(p, SoundEvents.ENDER_DRAGON_GROWL, 1.2f, 1.4f);
                zeg(p, "scene.finale.roze", ChatFormatting.LIGHT_PURPLE);
            }
        }
        Entity d = DRAAK.get(p.getUUID());
        if (t >= 100 && d != null && !d.isRemoved()) {
            deeltjes(p, sl, new DustParticleOptions(0xFF8AD0, 3.0f), d.position().add(0, 2, 0), 20, 5, 2.5, 0);
        }
        if (t == 200) {
            zeg(p, "scene.finale.ei", ChatFormatting.LIGHT_PURPLE);
        }
        if (t == 240) {
            // the egg hatches: a tiny pink dragon rolls out and sneezes
            deeltjes(p, sl, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PINK_TERRACOTTA.defaultBlockState()), n.add(0, 1, 0), 60, 0.5, 0.4, 0.2);
            deeltjes(p, sl, ParticleTypes.HEART, n.add(0, 1.5, 0), 10, 0.6, 0.4, 0.05);
            geluid(p, SoundEvents.TURTLE_EGG_HATCH, 2f, 1.2f);
        }
        if (t == 255) {
            geluid(p, SoundEvents.PANDA_SNEEZE, 1.5f, 1.6f);
            deeltjes(p, sl, ParticleTypes.CLOUD, n.add(0, 1.2, 0.5), 6, 0.1, 0.1, 0.05);
            zeg(p, "scene.finale.nies", ChatFormatting.LIGHT_PURPLE);
        }
        if (t == 285) {
            // a dragon that giggles shakes the whole crown
            geluid(p, SoundEvents.ENDER_DRAGON_AMBIENT, 1.5f, 1.8f);
            deeltjes(p, sl, ParticleTypes.CHERRY_LEAVES, n.add(0, 10, 0), 120, 20, 6, 0);
            zeg(p, "scene.finale.giechel", ChatFormatting.LIGHT_PURPLE);
        }
        if (t == 320) {
            vahoegje(p);
            zeg(p, "scene.finale.schouder", ChatFormatting.LIGHT_PURPLE);
        }
        if (t >= 360) {
            // the long pink beam over the horizon; where it ends floats Hoogvads
            Vec3 bron = borst.add(0, 2, 0);
            double f = Math.min(1, (t - 360) / 60.0);
            for (double s = 0; s <= f; s += 0.01) {
                deeltjes(p, sl, new DustParticleOptions(0xFF8AD0, 2.6f), bron.lerp(Z3NpcPlekken.HOOGVADS, s), 1, 0.2, 0.2, 0);
            }
            if (t == 362) {
                geluid(p, SoundEvents.BEACON_POWER_SELECT, 2f, 0.8f);
                zeg(p, "scene.finale.straal", ChatFormatting.GOLD);
            }
        }
        if (t == 470) {
            zeg(p, "scene.finale.weg", ChatFormatting.LIGHT_PURPLE);
            geluid(p, SoundEvents.ENDER_DRAGON_FLAP, 2f, 0.9f);
        }
    }

    /** The baby dragon: the special pet (Z3NpcRegistry registers it; the pets slice shows it). */
    static void vahoegje(ServerPlayer p) {
        nl.juiced.guhs.feature.vadsrijk.huisdieren.Huisdieren.geef(p, Z3NpcRegistry.VAHOEGJE, 0);
    }

    /** After the finale: the bloom wave, the flag, the title card. */
    static void bloei(ServerPlayer p) {
        vahoegje(p);   // (also when the finale was cut short before the hatching)
        Z3NpcApi.zetZone3Klaar(p);
        NpcRollen.doe("z3_terugkleur", p, "");   // (z3-wereld/kern: the recolour wave over the whole tree, when bound)
        RijkScenes.titel(p, Component.translatable(Z + "bloei.titel").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                Component.translatable(Z + "bloei.onder"), 90, 0xFF8AD0);
        geluid(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        NpcApi.meld(p, "z3", "finale");
    }

    // ---- the cliffhanger + flash F2 (z3_hq15) -----------------------------------------------------------------------------------------

    public static void cliffhanger(ServerPlayer p) {
        Spelers.data(p).gezien.add("npc:z3_cliffhanger");
        Vec3[] k = Z3NpcPlekken.CLIFFHANGER;
        RijkScenes.speel(p, RijkScene.maak("npc:z3_cliff", CLIFF_TICKS).camera(0, k[0], k[1]).camera(CLIFF_TICKS, k[2], k[3]).bouw());
        zeg(p, "scene.cliff.oosten", ChatFormatting.GOLD);
        loopt(p, "cliff", "");
    }

    /**
     * Flash F2 for a player who did not see z3-bazen's throne room after Vahoegnir. W-SPEL: it used to film an empty sky ("toont
     * helemaal niks": a set that was never built, under DARKNESS). Now: "ver in het oosten" = a shot of the one grey thing in the
     * pink east, the Sterrendovende Toren of zone 6 (built terrain; the chunks come through SceneLaden), then the same throne room
     * set as z3-bazen's F2 ({@link nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Scenes#troonzaal}, with its title card).
     */
    static void flashF2(ServerPlayer p) {
        Spelers.data(p).gezien.add("npc:z3_flash_f2");
        Vec3[] k = Z3NpcPlekken.F2_OOST;
        RijkScenes.speel(p, RijkScene.maak("npc:z3_flash_f2", F2_TICKS).camera(0, k[0], k[1]).camera(F2_TICKS, k[2], k[3]).bouw());
        zeg(p, "scene.f2.oosten", ChatFormatting.GRAY);
        loopt(p, "f2", "");
    }

    /** W-SPEL: F2 part 2, the throne room (z3-bazen's set + figures + lines + title); "f2zaal" ends zone 3 when it is over. */
    private static void f2Zaal(ServerPlayer p) {
        if (p.level() instanceof ServerLevel sl && Vadsrijk.in(sl)) {
            Spelers.data(p).gezien.add(Z3Koppelingen.BAZEN_F2);
            nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Scenes.troonzaal(sl, p);
            loopt(p, "f2zaal", "");
        } else {
            einde(p, "f2zaal");
        }
    }

    // ---- ticking -------------------------------------------------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null || LOOPT.isEmpty()) {
            return;
        }
        long nu = sl.getGameTime();
        List<Runnable> daarna = new ArrayList<>();
        Iterator<Map.Entry<UUID, Loopt>> it = LOOPT.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Loopt> e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            Loopt l = e.getValue();
            if (p == null || p.level() != sl) {
                it.remove();
                weg(e.getKey());
                continue;
            }
            int t = (int) (nu - l.begin());
            int duur = switch (l.soort()) {
                case "opening" -> OPENING_TICKS;
                case "vlucht" -> VLIEG_TICKS;
                case "bam" -> BAM_TICKS;
                case "nest" -> NEST_TICKS;
                case "f1" -> F1_TICKS;
                case "ei" -> EI_TICKS;
                case "rit" -> RIT_TICKS;
                case "finale" -> FINALE_TICKS;
                case "cliff" -> CLIFF_TICKS;
                case "f2" -> F2_TICKS;
                case "f2zaal" -> nl.juiced.guhs.feature.vadsrijk.zone3.bazen.Z3Scenes.TROON_TICKS + 10;
                default -> 0;
            };
            switch (l.soort()) {
                case "opening" -> openingTik(p, sl, t);
                case "bam" -> bamTik(p, sl, t);
                case "nest" -> nestTik(p, sl, t);
                case "f1" -> f1Tik(p, sl, t);
                case "ei" -> eiTik(p, sl, t);
                case "rit" -> ritTik(p, sl, t);
                case "finale" -> finaleTik(p, sl, t);
                default -> {
                }
            }
            if (t >= duur) {
                it.remove();
                daarna.add(() -> einde(p, l.soort()));
            }
        }
        daarna.forEach(Runnable::run);
    }

    /** What happens when a scene ends (also called at once by {@link #spoel} in tests). */
    static void einde(ServerPlayer p, String soort) {
        switch (soort) {
            case "opening" -> {
                zet(p, Z3NpcPlekken.PLEIN, 90f);
                NpcApi.meld(p, "z3", "aangekomen");
                zeg(p, "scene.aankomst", ChatFormatting.LIGHT_PURPLE);
            }
            case "vlucht" -> NpcApi.meld(p, "z3", "aangekomen");
            case "bam" -> zeg(p, "bam.aanval", ChatFormatting.RED);
            case "nest" -> {
                weg(p.getUUID());
                NpcApi.meld(p, "z3", "nest_herinnering");
            }
            case "f1" -> NpcApi.meld(p, "z3", "flash_f1");
            case "ei" -> {
                Questlog.geef(p, new net.minecraft.world.item.ItemStack(Z3NpcRegistry.item("drakenei")));
                NpcApi.meld(p, "z3", "ei");
            }
            case "rit" -> {
                weg(p.getUUID());
                zet(p, BlockPos.containing(Z3NpcPlekken.NEST.add(6, 0, 6)), 225f);
                NpcApi.meld(p, "z3", "ontsnapt");
                Z3Haken.startVahoegnir(p);
            }
            case "finale" -> {
                weg(p.getUUID());
                bloei(p);
            }
            case "cliff" -> {
                if (Z3Koppelingen.f2Gezien(p)) {   // K-INT: z3-bazen's throne room (after Vahoegnir) was the F2
                    zeg(p, "zone3.klaar", ChatFormatting.LIGHT_PURPLE);
                    NpcApi.meld(p, "z3", "cliffhanger");
                } else {
                    flashF2(p);
                }
            }
            case "f2" -> f2Zaal(p);   // W-SPEL: after the east shot, the throne room
            case "f2zaal" -> {
                zeg(p, "zone3.klaar", ChatFormatting.LIGHT_PURPLE);
                NpcApi.meld(p, "z3", "cliffhanger");
            }
            default -> {
            }
        }
    }

    // ---- helpers ---------------------------------------------------------------------------------------------------------------

    private static void loopt(ServerPlayer p, String soort, String arg) {
        LOOPT.put(p.getUUID(), new Loopt(soort, p.level().getGameTime(), arg));
    }

    private static void weg(UUID id) {
        Entity d = DRAAK.remove(id);
        if (d != null) {
            d.discard();
        }
    }

    /** The camera position at tick t (keyframe times {@code tijd}, or evenly spread over 0..1 when null and t is a fraction). */
    static Vec3 camera(Vec3[] k, int[] tijd, double t) {
        int n = k.length / 2;
        for (int i = 0; i < n - 1; i++) {
            double a = tijd == null ? i / (double) (n - 1) : tijd[i], b = tijd == null ? (i + 1) / (double) (n - 1) : tijd[i + 1];
            if (t <= b) {
                return k[2 * i].lerp(k[2 * i + 2], Math.max(0, (t - a) / (b - a)));
            }
        }
        return k[k.length - 2];
    }

    static void zeg(ServerPlayer p, String key, ChatFormatting kleur) {
        p.sendSystemMessage(Component.translatable(Z + key).withStyle(kleur, ChatFormatting.ITALIC));
    }

    static void zet(ServerPlayer p, BlockPos b, float yaw) {
        if (!(p.level() instanceof ServerLevel sl && Vadsrijk.in(sl))) {
            return;   // (only inside the Vadsrijk: travel always starts at an airship NPC there; tests stay put)
        }
        double y = Npcs.vloer(sl, b);
        p.teleport(new TeleportTransition(sl, new Vec3(b.getX() + 0.5, y, b.getZ() + 0.5), Vec3.ZERO, yaw, 0f, TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
    }

    static void deeltjes(ServerPlayer p, ServerLevel sl, ParticleOptions soort, Vec3 v, int n, double spreid, double hoog, double snel) {
        sl.sendParticles(p, soort, true, true, v.x, v.y, v.z, n, spreid, hoog, spreid, snel);
    }

    static void geluid(ServerPlayer p, SoundEvent s, float volume, float toon) {
        geluid(p, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(s), volume, toon);
    }

    static void geluid(ServerPlayer p, Holder<SoundEvent> s, float volume, float toon) {
        p.connection.send(new ClientboundSoundPacket(s, SoundSource.AMBIENT, p.getX(), p.getY(), p.getZ(), volume, toon, p.getRandom().nextLong()));
    }

    /** (tests/dev) */
    static void vergeet(ServerPlayer p) {
        LOOPT.remove(p.getUUID());
        weg(p.getUUID());
    }

    static boolean bezig(ServerPlayer p) {
        return LOOPT.containsKey(p.getUUID());
    }

    /** (tests) Is this scene running for the player? */
    static boolean speelt(ServerPlayer p, String soort) {
        Loopt l = LOOPT.get(p.getUUID());
        return l != null && l.soort().equals(soort);
    }

    /** (tests) Run a running scene to its end at once (the end of a scene may start the next one: that one too). */
    static void spoel(ServerPlayer p, MinecraftServer server) {
        for (int i = 0; i < 4; i++) {
            Loopt l = LOOPT.remove(p.getUUID());
            if (l == null) {
                return;
            }
            einde(p, l.soort());
        }
    }

    @SuppressWarnings("unused")
    private static Quest q(String id) {
        return Quests.van(id);
    }
}
