package nl.juiced.guhs.feature.vadsrijk.zone2.npc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

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
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
import nl.juiced.guhs.feature.verhaal.Praat;

/**
 * The story moments of zone 2 (non-click camera scenes: kern RijkScene; the dragon's question is the only click, a quest
 * dialogue) and the travel of zone 2:
 * <ul>
 *   <li>{@link #opening}: the first flight to Kandijkom: snowstorm, ice floes on the hull, lights out, a Galmwolf howls on a crystal
 *       needle that bursts, the ship slips into the warm geode; hot chocolate;</li>
 *   <li>{@link #draak}: z2_hq09, Vahoegnir wounded and resting in a ravine: you pull out a grey crystal splinter, he looks at you
 *       and flies off (no fight);</li>
 *   <li>{@link #flashback}: z2_hq11, the first shard of his pink heart crystal: long ago, the dragon pink and protective over the realm;</li>
 *   <li>{@link #cliffhanger}: z2_hq12, the giant tree on the far EASTERN horizon (zone 3); flag {@code zone2_klaar};</li>
 *   <li>{@link #vlieg} (De Haven &lt;-&gt; Kandijkom) and {@link #gondel} (Kandijkom &lt;-&gt; the top of the Galmhellingen).</li>
 * </ul>
 * Other slices may take parts over through NpcRollen bindings (a binding always wins; reflection on the z2-wereld classes next):
 * {@code z2_luchtlijn} (arg "kandijkom"/"haven"), {@code z2_gondelbaan} (arg "op"/"af"), {@code z2_galmwolf_opening} (arg "x,y,z"),
 * {@code z2_draak_rust} / {@code z2_draak_roze} (args "x,y,z,ticks").
 */
public final class Z2Verhaal {
    public static final String SLEUTEL = "vr_z2_npc";
    static final int OPENING_TICKS = 400, DRAAK1_TICKS = 160, DRAAK2_TICKS = 180, FLASH_TICKS = 260, EINDE_TICKS = 280, VLIEG_TICKS = 160,
            GONDEL_TICKS = 140;
    static final int SPLINTER = 1;
    public static final String VLAG_ZONE2 = "zone2_klaar";

    private record Loopt(String soort, long begin, String arg) {
    }

    private static final Map<UUID, Loopt> LOOPT = new HashMap<>();
    /** The dragon entities of a running scene, per player (discarded when the scene ends). */
    private static final Map<UUID, Entity> DRAAK = new HashMap<>();

    private Z2Verhaal() {
    }

    static void init() {
        Praat.luister(SLEUTEL, Z2Verhaal::antwoord);
    }

    // ---- the opening: the first flight to Kandijkom -----------------------------------------------------------------------------

    public static boolean openingGezien(ServerPlayer p) {
        return Spelers.data(p).gezien.contains("npc:z2_opening");
    }

    public static void opening(ServerPlayer p) {
        Spelers.data(p).gezien.add("npc:z2_opening");
        // the player waits at the dock (so the chunks around the camera load); the camera flies the storm
        zet(p, Z2NpcPlekken.DOK_AANKOMST, 180f);
        Vec3[] k = Z2NpcPlekken.OPENING;
        RijkScene s = RijkScene.maak("npc:z2_opening", OPENING_TICKS)
                .camera(0, k[0], k[1]).camera(100, k[2], k[3]).camera(180, k[4], k[5]).camera(250, k[6], k[7]).camera(310, k[8], k[9])
                .camera(OPENING_TICKS, k[10], k[11])
                .titel("vadsrijk.z2npc.scene.opening.titel", "vadsrijk.z2npc.scene.opening.onder", 320, 80, 0xFFB3D1).bouw();
        RijkScenes.speel(p, s);
        geluid(p, SoundEvents.ELYTRA_FLYING, 0.7f, 0.6f);
        LOOPT.put(p.getUUID(), new Loopt("opening", p.level().getGameTime(), ""));
    }

    private static void openingTik(ServerPlayer p, ServerLevel sl, int t) {
        double f = Math.min(1, t / 300.0);
        Vec3 schip = Z2NpcPlekken.SCHIP_VAN.lerp(Z2NpcPlekken.SCHIP_NAAR, f);
        Vec3 cam = camera(Z2NpcPlekken.OPENING, t);
        if (t < 290) {
            // the snowstorm around the camera: thick snow and grey grit driven sideways
            deeltjes(p, sl, ParticleTypes.SNOWFLAKE, cam.add(6, 0, -10), 70, 14, 7, 0.25);
            deeltjes(p, sl, ParticleTypes.WHITE_ASH, cam.add(4, 0, -8), 50, 12, 6, 0.1);
            if (t % 20 == 0) {
                geluid(p, SoundEvents.WEATHER_RAIN, 0.6f, 0.5f);
            }
        }
        if (t == 50 || t == 75 || t == 95) {
            // ice floes hit the hull: a crack of ice right in front of the camera
            Vec3 klap = cam.add(cam.vectorTo(schip).normalize().scale(4));
            deeltjes(p, sl, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), klap, 60, 1.2, 1.0, 0.2);
            geluid(p, SoundEvents.GLASS_BREAK, 1.2f, t == 75 ? 0.6f : 0.8f);
            geluid(p, SoundEvents.ANVIL_LAND, 0.35f, 0.5f);
            if (t == 50) {
                p.sendSystemMessage(Component.translatable("vadsrijk.z2npc.scene.storm").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
            }
        }
        if (t == 120) {
            // the lights go out
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false));
            p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false));
            geluid(p, SoundEvents.BEACON_DEACTIVATE, 1.2f, 0.7f);
            p.sendSystemMessage(Component.translatable("vadsrijk.z2npc.scene.licht_uit").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        Vec3 naald = Z2NpcPlekken.OPENING_NAALD;
        if (t == 175) {
            if (!NpcRollen.doe("z2_galmwolf_opening", p, (int) naald.x + "," + (int) naald.y + "," + (int) naald.z)) {
                LOOPT.put(p.getUUID(), new Loopt("opening", LOOPT.get(p.getUUID()).begin(), "eigen_wolf"));
            }
            p.sendSystemMessage(Component.translatable("vadsrijk.z2npc.scene.wolf").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.ITALIC));
        }
        if (t >= 175 && t < 245 && "eigen_wolf".equals(LOOPT.get(p.getUUID()).arg())) {
            // our own wolf: a grey crystal silhouette of dust on the needle top, the eyes glowing
            for (int i = 0; i < 6; i++) {
                double a = p.getRandom().nextDouble() * Math.PI * 2, r = p.getRandom().nextDouble() * 1.4;
                deeltjes(p, sl, new DustParticleOptions(0x8A90A0, 2.2f), naald.add(Math.cos(a) * r * 1.6, 2.4 + p.getRandom().nextDouble() * 1.6,
                        Math.sin(a) * r), 1, 0.05, 0.05, 0);
            }
            deeltjes(p, sl, new DustParticleOptions(0x7FE8FF, 1.0f), naald.add(1.0, 3.6, 0), 2, 0.05, 0.05, 0);
        }
        if (t == 200 || t == 222) {
            // the howl: a deep crystal howl that rolls over the storm
            geluid(p, Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace("entity.wolf.howl"))), 3f, t == 200 ? 0.6f : 0.5f);
            geluid(p, SoundEvents.WARDEN_ROAR, 0.5f, 1.6f);
            deeltjes(p, sl, ParticleTypes.SONIC_BOOM, naald.add(0, 3, 0), 1, 0, 0, 0);
        }
        if (t == 245) {
            // the needle bursts
            deeltjes(p, sl, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState()), naald, 200, 2.5, 6, 0.4);
            deeltjes(p, sl, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CALCITE.defaultBlockState()), naald, 120, 2.5, 6, 0.4);
            deeltjes(p, sl, ParticleTypes.EXPLOSION, naald.add(0, 2, 0), 4, 1.5, 3, 0);
            geluid(p, SoundEvents.AMETHYST_CLUSTER_BREAK, 3f, 0.5f);
            geluid(p, SoundEvents.GLASS_BREAK, 2f, 0.4f);
            p.sendSystemMessage(Component.translatable("vadsrijk.z2npc.scene.naald").withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
        }
        if (t >= 290) {
            // the warm geode: pink light, the storm stays outside
            deeltjes(p, sl, new DustParticleOptions(0xFF9AD5, 1.6f), cam.add(0, -4, -20), 12, 12, 6, 0);
            deeltjes(p, sl, ParticleTypes.CHERRY_LEAVES, cam.add(0, 2, -12), 6, 8, 3, 0);
            if (t == 300) {
                geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME, 1.5f, 0.8f);
                geluid(p, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2f, 1.0f);
            }
        }
    }

    /** The camera position of a keyframe list at tick t (linear, as an approximation of the client's path; for effects). */
    private static Vec3 camera(Vec3[] k, int t) {
        int[] tijd = {0, 100, 180, 250, 310, OPENING_TICKS};
        for (int i = 0; i < tijd.length - 1; i++) {
            if (t <= tijd[i + 1]) {
                return k[2 * i].lerp(k[2 * i + 2], (t - tijd[i]) / (double) (tijd[i + 1] - tijd[i]));
            }
        }
        return k[k.length - 2];
    }

    // ---- travel --------------------------------------------------------------------------------------------------------------------

    /** The Haven pilot's line to Kandijkom: after zone 1 and from level 20. */
    public static boolean magNaarKandijkom(ServerPlayer p) {
        return NpcApi.zone1Klaar(p) && Spelers.level(p) >= 20;
    }

    /** From the Haven pilot's dialogue (the extra choice): start z2_hq01 if needed, then fly. */
    static void naarKandijkom(ServerPlayer p) {
        if (Spelers.level(p) < 20) {
            p.sendSystemMessage(Component.translatable("vadsrijk.z2npc.luchtschip.te_laag", Spelers.level(p)).withStyle(ChatFormatting.AQUA));
            return;
        }
        Quest q = Quests.van("z2_hq01_splinterpieken");
        if (q != null && Questlog.beschikbaar(p, q)) {
            Questlog.neemAan(p, q);
        }
        vlieg(p, "kandijkom");
    }

    /** De Haven -> Kandijkom ("kandijkom", the first time with the opening) or back ("haven"). */
    public static void vlieg(ServerPlayer p, String naar) {
        boolean heen = naar.equals("kandijkom");
        // (integration) ONE opening: z2-wereld's (Z2Reizen plays it on the first flight, on the real map with its landing fix).
        // This slice's own opening only runs when no airship binding is there (it is: NpcRollen.koppelAlles binds z2_luchtlijn).
        if (heen && !openingGezien(p) && !NpcRollen.gekoppeld("z2_luchtlijn")) {
            opening(p);
            return;
        }
        if (heen) {
            Spelers.data(p).gezien.add("npc:z2_opening");
        }
        if (NpcRollen.doe("z2_luchtlijn", p, naar) || Z2Rollen.reflectie(p, naar, "zone2.wereld.Z2Reizen#vlieg", "zone2.wereld.Luchtlijn#vlieg")) {
            return;
        }
        Vec3[] k = Z2NpcPlekken.VLUCHT_KANDIJKOM;
        RijkScene.Builder b = RijkScene.maak("npc:z2_vlucht_" + naar, VLIEG_TICKS);
        if (heen) {
            zet(p, Z2NpcPlekken.DOK_AANKOMST, 180f);
            b.camera(0, k[0], k[1]).camera(VLIEG_TICKS / 2, k[2], k[3]).camera(VLIEG_TICKS, k[4], k[5]);
        } else {
            // (the way back: the camera rises out of the geode and over the cloud sea, then the haven)
            b.camera(0, k[4], k[5]).camera(VLIEG_TICKS / 2, k[2], k[1]).camera(VLIEG_TICKS, k[0], k[1].add(0, 40, 400));
        }
        b.titel("vadsrijk.z2npc.scene.vlucht." + naar + ".titel", "vadsrijk.z2npc.scene.vlucht." + naar + ".onder", VLIEG_TICKS - 60, 60, 0xFFD24A);
        RijkScenes.speel(p, b.bouw());
        geluid(p, SoundEvents.ELYTRA_FLYING, 0.5f, 0.8f);
        LOOPT.put(p.getUUID(), new Loopt("vlucht", p.level().getGameTime(), naar));
    }

    /** The gondola: up from Kandijkom to the top of the Galmhellingen, or back down. */
    public static void gondel(ServerPlayer p, boolean omhoog) {
        if (NpcRollen.doe("z2_gondelbaan", p, omhoog ? "op" : "af") || Z2Rollen.reflectie(p, omhoog ? "op" : "af", "zone2.wereld.Z2Reizen#gondel",
                "zone2.wereld.Gondel#rij")) {
            return;
        }
        Vec3[] k = Z2NpcPlekken.GONDEL;
        RijkScene.Builder b = RijkScene.maak("npc:z2_gondel", GONDEL_TICKS);
        if (omhoog) {
            b.camera(0, k[0], k[1]).camera(GONDEL_TICKS / 2, k[2], k[3]).camera(GONDEL_TICKS, k[4], k[5])
                    .titel("vadsrijk.z2npc.scene.gondel.titel", "vadsrijk.z2npc.scene.gondel.onder", GONDEL_TICKS - 50, 50, 0xBFE8FF);
            p.sendSystemMessage(Component.translatable("vadsrijk.z2npc.gondel.omhoog").withStyle(ChatFormatting.AQUA));
        } else {
            b.camera(0, k[4], k[0]).camera(GONDEL_TICKS / 2, k[2], k[0]).camera(GONDEL_TICKS, k[0], Vec3.atCenterOf(Z2NpcPlekken.PLEIN));
        }
        RijkScenes.speel(p, b.bouw());
        geluid(p, SoundEvents.CHAIN_PLACE, 1f, 0.6f);
        LOOPT.put(p.getUUID(), new Loopt("gondel", p.level().getGameTime(), omhoog ? "op" : "af"));
    }

    // ---- the dragon in the ravine (z2_hq09) -----------------------------------------------------------------------------------------

    /** Is the player at the ravine with the dragon step open? (Z2Haken starts the scene) */
    static boolean draakKlaarVoorScene(ServerPlayer p) {
        Quest q = Quests.van("z2_hq09_draak");
        return q != null && Questlog.actief(p, q.id()) && Questlog.doelKlaar(p, q, 0) && !Questlog.doelKlaar(p, q, 1) && !LOOPT.containsKey(p.getUUID());
    }

    /** Part 1: down into the ravine, the wounded dragon resting; then the (click) question. */
    public static void draak(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return;
        }
        Vec3[] k = Z2NpcPlekken.DRAAK_1;
        RijkScenes.speel(p, RijkScene.maak("npc:z2_draak_1", DRAAK1_TICKS).camera(0, k[0], k[1]).camera(DRAAK1_TICKS, k[2], k[3])
                .titel("vadsrijk.z2npc.scene.draak.titel", "vadsrijk.z2npc.scene.draak.onder", 50, 80, 0x9A86B0).bouw());
        Vec3 r = Z2NpcPlekken.DRAAK_RUST;
        if (!NpcRollen.doe("z2_draak_rust", p, (int) r.x + "," + (int) r.y + "," + (int) r.z + "," + (DRAAK1_TICKS + 600))) {
            Entity d = rustendeDraak(sl, r, DRAAK1_TICKS + 600);
            if (d != null) {
                DRAAK.put(p.getUUID(), d);
            }
        }
        geluid(p, SoundEvents.ENDER_DRAGON_GROWL, 0.6f, 0.4f);
        Spelers.data(p).gezien.add("npc:z2_draak_1");
        LOOPT.put(p.getUUID(), new Loopt("draak1", p.level().getGameTime(), ""));
    }

    /** Back at the ravine after closing the question: he is still there, ask again (no second scene). */
    static void draakOpnieuw(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return;
        }
        Entity d = DRAAK.get(p.getUUID());
        if ((d == null || d.isRemoved()) && !NpcRollen.doe("z2_draak_rust", p, (int) Z2NpcPlekken.DRAAK_RUST.x + "," + (int) Z2NpcPlekken.DRAAK_RUST.y + ","
                + (int) Z2NpcPlekken.DRAAK_RUST.z + ",600")) {
            Entity n = rustendeDraak(sl, Z2NpcPlekken.DRAAK_RUST, 600);
            if (n != null) {
                DRAAK.put(p.getUUID(), n);
            }
        }
        draakVraag(p);
    }

    /** wezens' Vahoegnir "resting": a flight over (almost) no distance for this long (he hovers in the ravine, breathing). */
    @Nullable
    static Entity rustendeDraak(ServerLevel sl, Vec3 plek, int ticks) {
        return nl.juiced.guhs.feature.vadsrijk.wezens.draak.DraakVlucht.vlieg(sl, List.of(plek, plek.add(0.01, 0, -0.3)), ticks, false);
    }

    static void draakVraag(ServerPlayer p) {
        List<Praat.Regel> r = List.of(new Praat.Regel(null, "vadsrijk.npc.verteller", "vadsrijk.z2npc.draak.splinter.0"),
                new Praat.Regel(null, "vadsrijk.npc.verteller", "vadsrijk.z2npc.draak.splinter.1"),
                new Praat.Regel(null, "vadsrijk.npc.verteller", "vadsrijk.z2npc.draak.splinter.2"),
                new Praat.Regel(null, "vadsrijk.npc.verteller", "vadsrijk.z2npc.draak.splinter.vraag"));
        Praat.scene(p, SLEUTEL, r, new Praat.Optie(SPLINTER, "vadsrijk.z2npc.draak.splinter.optie"));
    }

    private static void antwoord(ServerPlayer p, @Nullable Entity spreker, int optie) {
        if (optie == SPLINTER) {
            Praat.sluit(p);
            draakHelp(p);
        }
        // (closed without choosing: Z2Haken asks again when you are still at the ravine, the dragon waits)
    }

    /** Part 2: the splinter comes out, he looks at you, and flies off. Counts the quest step. */
    static void draakHelp(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return;
        }
        Vec3[] k = Z2NpcPlekken.DRAAK_2;
        RijkScenes.speel(p, RijkScene.maak("npc:z2_draak_2", DRAAK2_TICKS).camera(0, k[0], k[1]).camera(80, k[2], k[3]).camera(DRAAK2_TICKS, k[4], k[5])
                .bouw());
        NpcApi.meld(p, "z2", "draak_geholpen");
        Spelers.data(p).gezien.add("npc:z2_draak");
        LOOPT.put(p.getUUID(), new Loopt("draak2", p.level().getGameTime(), ""));
        Vec3 r = Z2NpcPlekken.DRAAK_RUST;
        deeltjes(p, sl, new BlockParticleOption(ParticleTypes.BLOCK, Blocks.TUFF.defaultBlockState()), r.add(2, 2, 0), 80, 1, 1, 0.3);
        geluid(p, SoundEvents.AMETHYST_CLUSTER_BREAK, 2f, 0.6f);
    }

    private static void draakTik(ServerPlayer p, ServerLevel sl, String soort, int t) {
        Vec3 r = Z2NpcPlekken.DRAAK_RUST;
        if (soort.equals("draak1")) {
            // the grey splinter in his wing, grey snow around him, a heavy breath now and then
            deeltjes(p, sl, new DustParticleOptions(0x6A6478, 1.8f), r.add(3, 3, 1), 4, 0.3, 0.6, 0);
            deeltjes(p, sl, ParticleTypes.WHITE_ASH, r.add(0, 6, 0), 10, 8, 3, 0);
            if (t % 50 == 10) {
                geluid(p, SoundEvents.ENDER_DRAGON_AMBIENT, 0.5f, 0.45f);
            }
        } else {
            if (t < 50) {
                // the splinter is out: pink warmth wells up where it was
                deeltjes(p, sl, new DustParticleOptions(0xFF8AD0, 1.5f), r.add(3, 3, 1), 8, 0.6, 0.6, 0.02);
                deeltjes(p, sl, ParticleTypes.HEART, r.add(0, 5, 0), 1, 1.5, 1, 0);
            }
            if (t == 20) {
                geluid(p, SoundEvents.ENDER_DRAGON_GROWL, 0.5f, 1.3f);   // (not a roar: a soft, high sigh)
                p.sendSystemMessage(Component.translatable("vadsrijk.z2npc.draak.weg").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
            }
            if (t == 70) {
                Entity oud = DRAAK.remove(p.getUUID());
                if (oud != null) {
                    oud.discard();
                }
                if (!NpcRollen.doe("z2_draak_weg", p, "")) {
                    Entity d = nl.juiced.guhs.feature.vadsrijk.wezens.draak.DraakVlucht.vlieg(sl, List.of(Z2NpcPlekken.DRAAK_WEG), 160, false);
                    if (d != null) {
                        DRAAK.put(p.getUUID(), d);
                    }
                }
                geluid(p, SoundEvents.ENDER_DRAGON_FLAP, 2f, 0.7f);
            }
        }
    }

    // ---- the flashback (z2_hq11) and the cliffhanger (z2_hq12) ----------------------------------------------------------------

    public static void flashback(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return;
        }
        Vec3[] k = Z2NpcPlekken.FLASHBACK;
        RijkScenes.speel(p, RijkScene.maak("npc:z2_flashback", FLASH_TICKS).camera(0, k[0], k[1]).camera(70, k[0].add(0, 0.5, 2), k[1])
                .camera(90, k[2], k[3]).camera(FLASH_TICKS, k[4], k[5])
                .titel("vadsrijk.z2npc.scene.flashback.titel", "vadsrijk.z2npc.scene.flashback.onder", 100, 120, 0xFF8AD0).bouw());
        geluid(p, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.5f, 0.7f);
        LOOPT.put(p.getUUID(), new Loopt("flashback", p.level().getGameTime(), ""));
        Spelers.data(p).gezien.add("npc:z2_flashback");
    }

    private static void flashbackTik(ServerPlayer p, ServerLevel sl, int t) {
        if (t < 85) {
            deeltjes(p, sl, new DustParticleOptions(0xFF8AD0, 1.2f), Z2NpcPlekken.FLASHBACK[1], 6, 0.3, 0.3, 0.01);
            if (t == 70) {
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, false, false));   // (the flash into the memory)
                geluid(p, SoundEvents.BEACON_ACTIVATE, 1.5f, 1.4f);
            }
            return;
        }
        if (t == 90) {
            String pad = String.join(";", java.util.Arrays.stream(Z2NpcPlekken.FLASHBACK_DRAAK).map(v -> (int) v.x + "," + (int) v.y + "," + (int) v.z)
                    .toList());
            if (!NpcRollen.doe("z2_draak_roze", p, pad + "|" + (FLASH_TICKS - 90))) {
                Entity d = nl.juiced.guhs.feature.vadsrijk.wezens.draak.DraakVlucht.vlieg(sl, List.of(Z2NpcPlekken.FLASHBACK_DRAAK), FLASH_TICKS - 90, false);
                if (d != null) {
                    DRAAK.put(p.getUUID(), d);
                }
            }
            geluid(p, SoundEvents.ENDER_DRAGON_FLAP, 1.5f, 1.1f);
        }
        Entity d = DRAAK.get(p.getUUID());
        if (d != null && !d.isRemoved()) {
            // the memory: a pink, protective dragon (the grey model wrapped in pink light, hearts and blossom)
            Vec3 v = d.position();
            deeltjes(p, sl, new DustParticleOptions(0xFF8AD0, 3.0f), v.add(0, 2, 0), 40, 5, 2.5, 0);
            deeltjes(p, sl, ParticleTypes.CHERRY_LEAVES, v.add(0, -3, 0), 12, 8, 2, 0);
            if (t % 6 == 0) {
                deeltjes(p, sl, ParticleTypes.HEART, v.add(0, 3, 0), 3, 4, 2, 0);
            }
        }
        if (t == 150) {
            geluid(p, SoundEvents.ENDER_DRAGON_GROWL, 0.8f, 1.5f);
        }
    }

    public static void cliffhanger(ServerPlayer p) {
        Vec3[] k = Z2NpcPlekken.CLIFFHANGER;
        RijkScenes.speel(p, RijkScene.maak("npc:z2_cliffhanger", EINDE_TICKS).camera(0, k[0], k[1]).camera(120, k[2], k[3]).camera(EINDE_TICKS, k[4], k[5])
                .titel("vadsrijk.z2npc.scene.einde.titel", "vadsrijk.z2npc.scene.einde.onder", 150, 110, 0x9AD08A).bouw());
        NpcRollen.doe("draak", p, "ver");
        geluid(p, SoundEvents.ENDER_DRAGON_GROWL, 0.6f, 0.6f);
        Z2NpcApi.zetZone2Klaar(p);
        p.sendSystemMessage(Component.translatable("vadsrijk.z2npc.zone2.klaar").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // ---- ticking -----------------------------------------------------------------------------------------------------------------

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
                Entity d = DRAAK.remove(e.getKey());
                if (d != null) {
                    d.discard();
                }
                continue;
            }
            int t = (int) (nu - l.begin());
            switch (l.soort()) {
                case "opening" -> {
                    openingTik(p, sl, t);
                    if (t >= OPENING_TICKS) {
                        it.remove();
                        daarna.add(() -> p.sendSystemMessage(Component.translatable("vadsrijk.z2npc.scene.aankomst").withStyle(ChatFormatting.LIGHT_PURPLE)));
                    }
                }
                case "vlucht" -> {
                    if (t >= VLIEG_TICKS - 6) {
                        it.remove();
                        if (!l.arg().equals("kandijkom")) {
                            daarna.add(() -> zet(p, nl.juiced.guhs.feature.vadsrijk.npc.NpcPlekken.HAVEN_AANKOMST, 180f));
                        }
                    }
                }
                case "gondel" -> {
                    if (t == 4) {
                        zet(p, l.arg().equals("op") ? Z2NpcPlekken.GONDEL_BOVEN : Z2NpcPlekken.GONDEL_BENEDEN, 90f);
                    }
                    if (t >= GONDEL_TICKS) {
                        it.remove();
                    }
                }
                case "draak1" -> {
                    draakTik(p, sl, "draak1", t);
                    if (t >= DRAAK1_TICKS) {
                        it.remove();
                        daarna.add(() -> draakVraag(p));
                    }
                }
                case "draak2" -> {
                    draakTik(p, sl, "draak2", t);
                    if (t >= DRAAK2_TICKS) {
                        it.remove();
                    }
                }
                case "flashback" -> {
                    flashbackTik(p, sl, t);
                    if (t >= FLASH_TICKS) {
                        it.remove();
                        daarna.add(() -> {
                            Entity d = DRAAK.remove(p.getUUID());
                            if (d != null) {
                                d.discard();
                            }
                            Quest q = Quests.van("z2_hq12_hartsplinter");
                            if (q != null && Questlog.status(p, q.id()) == 0) {
                                Questlog.neemAan(p, q);
                            }
                        });
                    }
                }
                default -> it.remove();
            }
        }
        daarna.forEach(Runnable::run);
    }

    // ---- helpers ------------------------------------------------------------------------------------------------------------------

    static void zet(ServerPlayer p, BlockPos b, float yaw) {
        ServerLevel sl = p.level() instanceof ServerLevel s && Vadsrijk.in(s) ? s : Vadsrijk.level(p.level().getServer());
        if (sl == null) {
            return;
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
        Entity d = DRAAK.remove(p.getUUID());
        if (d != null) {
            d.discard();
        }
    }

    static boolean bezig(ServerPlayer p) {
        return LOOPT.containsKey(p.getUUID());
    }
}
