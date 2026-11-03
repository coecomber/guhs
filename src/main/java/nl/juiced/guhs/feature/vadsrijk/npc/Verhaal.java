package nl.juiced.guhs.feature.vadsrijk.npc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.KernRegistry;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arena;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.Arenas;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.klodderreus.Klodderreus;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScene;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;

/**
 * The story moments of zone 1 (non-click scenes: kern RijkScene) and their effects per player:
 * <ul>
 *   <li>{@link #opening}: the first arrival by airship at sunrise (the island, then the far skyline of Zwamzoet);</li>
 *   <li>{@link #bam}: "rustig, dan BAM" (ronde 5/9): the dragon flies over (wezens DraakVlucht, or our own shadow + roar), grey
 *       fluff rains down, the camp's flowers wilt (only for you), and the Grauwe Klodderreus lands in the pond (ronde 7);</li>
 *   <li>{@link #vlieg}: the airship between Knuffelkamp and Zwamzoet (wereld WereldReizen, or our own flight scene);</li>
 *   <li>{@link #cliffhanger}: the end of zone 1 (flag {@code zone1_klaar}), {@link #ontmasker}, {@link #advancement}.</li>
 * </ul>
 */
public final class Verhaal {
    public static final String ARENA = "npc:kampvijver";
    static final int BAM_TICKS = 220, VLIEG_TICKS = 200;

    /** A running effect of one player: its kind and the tick it began. */
    private record Loopt(String soort, long begin, String arg) {
    }

    private static final Map<UUID, Loopt> LOOPT = new HashMap<>();
    /** The flowers that wilted for this player (restored after the Klodderreus). */
    private static final Map<UUID, List<BlockPos>> VERWELKT = new HashMap<>();

    private Verhaal() {
    }

    // ---- the opening ----------------------------------------------------------------------------------------------------------

    /** (Haken.BINNEN) the first time in the dimension, at the camp: the opening scene. */
    static void binnen(ServerPlayer p, boolean in) {
        if (!in) {
            LOOPT.remove(p.getUUID());
            return;
        }
        Gebieden.Gebied g = Gebieden.op(p.getX(), p.getZ());
        if (g != null && g.id().equals("knuffelkamp") && !Spelers.data(p).gezien.contains("npc:opening")) {
            LOOPT.put(p.getUUID(), new Loopt("opening_wacht", p.level().getGameTime(), ""));
        }
    }

    public static void opening(ServerPlayer p) {
        Spelers.data(p).gezien.add("npc:opening");
        Vec3[] k = NpcPlekken.OPENING;
        RijkScene s = RijkScene.maak("npc:opening", 280)
                .camera(0, k[0], k[1]).camera(90, k[2], k[3]).camera(190, k[4], k[5]).camera(280, k[6], k[7])
                .titel("vadsrijk.npc.scene.opening.titel", "vadsrijk.npc.scene.opening.onder", 110, 90, 0xFFB3D1).bouw();
        RijkScenes.speel(p, s);
        geluid(p, SoundEvents.AMETHYST_BLOCK_CHIME, 0.8f, 0.7f);
        LOOPT.put(p.getUUID(), new Loopt("opening", p.level().getGameTime(), ""));
    }

    // ---- the BAM ---------------------------------------------------------------------------------------------------------------

    public static void bam(ServerPlayer p) {
        Spelers.data(p).gezien.add("npc:bam");
        Vec3[] k = NpcPlekken.BAM;
        RijkScene s = RijkScene.maak("npc:bam", BAM_TICKS)
                .camera(0, k[0], k[1]).camera(120, k[2], k[3]).camera(BAM_TICKS, k[4], k[5])
                .titel("vadsrijk.npc.scene.draak.titel", "vadsrijk.npc.scene.draak.onder", 70, 80, 0x9A86B0).bouw();
        RijkScenes.speel(p, s);
        boolean draak = NpcRollen.doe("draak", p, "overvlucht");
        LOOPT.put(p.getUUID(), new Loopt("bam", p.level().getGameTime(), draak ? "wezens" : "eigen"));
    }

    private static void bamTik(ServerPlayer p, ServerLevel sl, int t, boolean eigenDraak) {
        if (eigenDraak && t < 170) {
            // a huge grey shadow sweeping over the camp, crackling with black fluff
            double f = t / 170.0;
            Vec3 d = NpcPlekken.DRAAK_VAN.lerp(NpcPlekken.DRAAK_NAAR, f);
            deeltjes(p, sl, ParticleTypes.LARGE_SMOKE, d, 50, 7, 2.5, 0.02);
            deeltjes(p, sl, ParticleTypes.SQUID_INK, d, 20, 6, 2, 0.02);
            deeltjes(p, sl, ParticleTypes.LARGE_SMOKE, new Vec3(d.x, Vadsrijk.GROND + 1.5, d.z), 25, 9, 0.2, 0.0);
        }
        if (t == 40 || t == 110) {
            geluid(p, SoundEvents.ENDER_DRAGON_GROWL, 2.0f, t == 40 ? 0.7f : 0.55f);
        }
        if (t == 30) {
            geluid(p, SoundEvents.ENDER_DRAGON_FLAP, 2.0f, 0.6f);
        }
        if (t >= 60 && t % 2 == 0) {
            // grey fluff rains down
            deeltjes(p, sl, ParticleTypes.WHITE_ASH, p.position().add(0, 6, 0), 40, 12, 3, 0.0);
            deeltjes(p, sl, ParticleTypes.ASH, p.position().add(0, 6, 0), 30, 12, 3, 0.0);
        }
        if (t == 80) {
            verwelk(p, sl);
        }
        if (t == BAM_TICKS - 10) {
            Quest q = Quests.van("hq05_klodderreus");
            if (q != null && Questlog.status(p, q.id()) == 0) {
                Questlog.neemAan(p, q);
            }
            p.sendSystemMessage(Component.translatable("vadsrijk.npc.bam.aanval").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            geluid(p, SoundEvents.SLIME_SQUISH, 2.0f, 0.4f);
        }
    }

    /** The camp's flowers wilt grey, only for this player (fake block updates; restored by {@link #herstel}). */
    static void verwelk(ServerPlayer p, ServerLevel sl) {
        List<BlockPos> l = new ArrayList<>();
        BlockPos c = NpcPlekken.KAMPPLEIN;
        for (BlockPos b : BlockPos.betweenClosed(c.offset(-40, -6, -60), c.offset(70, 8, 50))) {
            if (sl.hasChunkAt(b) && sl.getBlockState(b).is(BlockTags.SMALL_FLOWERS)) {   // (never load the camp's chunks for this)
                BlockPos v = b.immutable();
                l.add(v);
                p.connection.send(new ClientboundBlockUpdatePacket(v, (l.size() % 3 == 0 ? Blocks.WITHER_ROSE : Blocks.DEAD_BUSH).defaultBlockState()));
            }
        }
        VERWELKT.put(p.getUUID(), l);
    }

    /** After the Klodderreus: the flowers bloom again. */
    static void herstel(ServerPlayer p) {
        List<BlockPos> l = VERWELKT.remove(p.getUUID());
        if (l != null && p.level() instanceof ServerLevel sl) {
            for (BlockPos b : l) {
                p.connection.send(new ClientboundBlockUpdatePacket(sl, b));
            }
            deeltjes(p, sl, ParticleTypes.CHERRY_LEAVES, Vec3.atCenterOf(NpcPlekken.KAMPPLEIN).add(0, 3, 0), 120, 18, 3, 0.02);
        }
    }

    /** The Klodderreus arena at the pond (registered on server start unless the bazen slice took the BAM over: "bam_baas"). */
    static void registreerArena() {
        if (Arenas.van(ARENA) != null || NpcRollen.gekoppeld("bam_baas")) {
            return;
        }
        BlockPos c = NpcPlekken.KAMPVIJVER;
        // (the respawn board lives deep under the island: a board "back in 0:00" would spoil the calm camp)
        Arenas.registreer(new Arena(ARENA, c, NpcPlekken.KAMPVIJVER_STRAAL, NpcPlekken.KAMPVIJVER_DEUR, 20, c.below(58), "vadsrijk.kern.baas.klodderreus"),
                sl -> {
                    if (!bamBezig(sl)) {
                        return null;
                    }
                    Klodderreus k = KernRegistry.KLODDERREUS.get().create(sl, EntitySpawnReason.SPAWNER);
                    if (k != null) {
                        k.stelIn(3, Element.WATER, false, Set.of());
                    }
                    return k;
                });
    }

    /** Is someone near the pond who is in the middle of the BAM quest? */
    static boolean bamBezig(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) {
            if (Questlog.actief(p, "hq05_klodderreus") && p.distanceToSqr(Vec3.atCenterOf(NpcPlekken.KAMPVIJVER)) < 80 * 80) {
                return true;
            }
        }
        return false;
    }

    /**
     * bazen's MikaSpion: right-clicking "Akim de Kaasguh" unmasks him once you found the 5 clues (its MAG_ONTMASKEREN,
     * reflection: this tree may not hold it). The test also reports the unmasking to the quest.
     */
    static void koppelSpion() {
        try {
            Class<?> c = Class.forName("nl.juiced.guhs.feature.vadsrijk.bazen.mika.MikaSpion");
            java.lang.reflect.Field f = c.getField("MAG_ONTMASKEREN");
            f.set(null, (java.util.function.Predicate<ServerPlayer>) Verhaal::magOntmaskeren);
        } catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
            // (no bazen slice in this tree: our own disguised Akim does it, see QuestMotor)
        }
        // wereld: the airship to the haven only after the camp story (the BAM and the Klodderreus); back to the camp always
        NpcRollen.zetVeld("nl.juiced.guhs.feature.vadsrijk.wereld.WereldReizen#MAG_VLIEGEN",
                (java.util.function.BiPredicate<ServerPlayer, Object>) (p, naar) -> !String.valueOf(naar).equals("HAVEN")
                        || Questlog.klaar(p, "hq05_klodderreus") || Questlog.actief(p, "hq06_zwamzoet"));
    }

    static boolean magOntmaskeren(ServerPlayer p) {
        Quest q = Quests.van("geheim_spion");
        if (q == null || !Questlog.actief(p, q.id()) || !Questlog.doelKlaar(p, q, 0) || Questlog.doelKlaar(p, q, 1)) {
            return false;
        }
        Questlog.zetVlag(p, "spion_ontmaskerd", true);
        NpcApi.meld(p, "spion", "ontmaskerd");
        return true;
    }

    // ---- the airship ----------------------------------------------------------------------------------------------------------

    public static void vlieg(ServerPlayer p, String naar) {
        if (NpcRollen.doe("luchtschip", p, naar)) {
            return;
        }
        boolean stad = naar.equals("zwamzoet");
        Vec3[] k = stad ? NpcPlekken.VLUCHT_NAAR_STAD : NpcPlekken.VLUCHT_NAAR_KAMP;
        RijkScene.Builder b = RijkScene.maak("npc:vlucht_" + naar, VLIEG_TICKS);
        for (int i = 0; i < k.length / 2; i++) {
            b.camera(i * VLIEG_TICKS / (k.length / 2 - 1), k[2 * i], k[2 * i + 1]);
        }
        b.titel(stad ? "vadsrijk.npc.scene.stad.titel" : "vadsrijk.npc.scene.kamp.titel", stad ? "vadsrijk.npc.scene.stad.onder" : "vadsrijk.npc.scene.kamp.onder",
                VLIEG_TICKS - 70, 60, 0xFFD24A);
        RijkScenes.speel(p, b.bouw());
        geluid(p, SoundEvents.ELYTRA_FLYING, 0.5f, 0.8f);
        LOOPT.put(p.getUUID(), new Loopt("vlucht", p.level().getGameTime(), naar));
    }

    private static void land(ServerPlayer p, String naar) {
        if (!(p.level() instanceof ServerLevel sl)) {
            return;
        }
        BlockPos a = naar.equals("zwamzoet") ? NpcPlekken.HAVEN_AANKOMST : NpcPlekken.KAMP_AANKOMST_TERUG;
        double y = Npcs.vloer(sl, a);
        p.teleport(new TeleportTransition(sl, new Vec3(a.getX() + 0.5, y, a.getZ() + 0.5), Vec3.ZERO, naar.equals("zwamzoet") ? 180f : 0f, 0f,
                TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
    }

    // ---- the end of zone 1, the spy, the advancements ---------------------------------------------------------------------------

    public static void cliffhanger(ServerPlayer p) {
        Vec3[] k = NpcPlekken.CLIFFHANGER;
        RijkScene s = RijkScene.maak("npc:cliffhanger", 260).camera(0, k[0], k[1]).camera(120, k[2], k[3]).camera(260, k[4], k[5])
                .titel("vadsrijk.npc.scene.einde.titel", "vadsrijk.npc.scene.einde.onder", 150, 100, 0x9A86B0).bouw();
        RijkScenes.speel(p, s);
        NpcRollen.doe("draak", p, "ver");
        geluid(p, SoundEvents.ENDER_DRAGON_GROWL, 0.8f, 0.5f);
        NpcApi.zetZone1Klaar(p);
    }

    /**
     * Unmask the spy. With the bazen slice: its MikaSpion "Akim de Kaasguh" (reveal scene + rooftop fight; the capture counts
     * through Haken.VERSLAGEN, a boss in Zwamzoet). Without it (fallback): a title card, and the fight's objective counts too.
     */
    public static void ontmasker(ServerPlayer p) {
        Questlog.zetVlag(p, "spion_ontmaskerd", true);
        NpcApi.meld(p, "spion", "ontmaskerd");
        if (NpcRollen.doe("mika_spion", p)) {
            return;
        }
        NpcApi.meld(p, "spion", "gevangen");
        RijkScenes.titel(p, Component.translatable("vadsrijk.npc.spion.titel").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD),
                Component.translatable("vadsrijk.npc.spion.onder"), 80, 0x7A3CA0);
        geluid(p, nl.juiced.guhs.registry.ModSounds.MIKA_AMBIENT.get(), 1.2f, 0.8f);
        p.sendSystemMessage(Component.translatable("vadsrijk.npc.spion.later").withStyle(ChatFormatting.GRAY));
    }

    public static void advancement(ServerPlayer p, String klasse, int tier) {
        Questlog.data(p).putInt("adv_" + klasse, Math.max(tier, Questlog.data(p).getIntOr("adv_" + klasse, 0)));
        if (NpcRollen.doe("advancement", p, klasse + ":" + tier)) {
            return;
        }
        RijkScenes.titel(p, Component.translatable("vadsrijk.npc.adv." + klasse + "." + tier).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                Component.translatable("vadsrijk.npc.adv.onder"), 80, 0xFFD24A);
        if (p.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, p.getX(), p.getY() + 1, p.getZ(), 80, 0.6, 1.2, 0.6, 0.3);
        }
        geluid(p, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    // ---- ticking ----------------------------------------------------------------------------------------------------------------

    static void tick(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null || LOOPT.isEmpty()) {
            return;
        }
        long nu = sl.getGameTime();
        Iterator<Map.Entry<UUID, Loopt>> it = LOOPT.entrySet().iterator();
        List<Runnable> daarna = new ArrayList<>();
        while (it.hasNext()) {
            Map.Entry<UUID, Loopt> e = it.next();
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            Loopt l = e.getValue();
            if (p == null || p.level() != sl) {
                it.remove();
                continue;
            }
            int t = (int) (nu - l.begin());
            switch (l.soort()) {
                case "opening_wacht" -> {
                    if (t >= 20) {
                        it.remove();
                        daarna.add(() -> opening(p));
                    }
                }
                case "opening" -> {
                    if (t >= 280) {
                        it.remove();
                        p.sendSystemMessage(Component.translatable("vadsrijk.npc.opening.welkom").withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                }
                case "bam" -> {
                    bamTik(p, sl, t, l.arg().equals("eigen"));
                    if (t >= BAM_TICKS) {
                        it.remove();
                    }
                }
                case "vlucht" -> {
                    if (t >= VLIEG_TICKS - 8) {
                        it.remove();
                        daarna.add(() -> land(p, l.arg()));
                    }
                }
                default -> it.remove();
            }
        }
        daarna.forEach(Runnable::run);
    }

    // ---- helpers ------------------------------------------------------------------------------------------------------------------

    static void deeltjes(ServerPlayer p, ServerLevel sl, ParticleOptions soort, Vec3 v, int n, double spreid, double hoog, double snel) {
        sl.sendParticles(p, soort, true, true, v.x, v.y, v.z, n, spreid, hoog, spreid, snel);
    }

    static void geluid(ServerPlayer p, SoundEvent s, float volume, float toon) {
        p.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(s), SoundSource.AMBIENT, p.getX(), p.getY(), p.getZ(), volume,
                toon, p.getRandom().nextLong()));
    }

    static void geluid(ServerPlayer p, net.minecraft.core.Holder<SoundEvent> s, float volume, float toon) {
        p.connection.send(new ClientboundSoundPacket(s, SoundSource.AMBIENT, p.getX(), p.getY(), p.getZ(), volume, toon, p.getRandom().nextLong()));
    }

    /** (tests/dev) */
    static void vergeet(ServerPlayer p) {
        LOOPT.remove(p.getUUID());
        VERWELKT.remove(p.getUUID());
    }
}
