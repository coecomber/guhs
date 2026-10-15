package nl.juiced.guhs.feature.einde2.reizen;

import java.util.List;
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
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.einde2.Einde2Ids;
import nl.juiced.guhs.feature.einde2.Plekken;
import nl.juiced.guhs.feature.einde2.appa.Appa;
import nl.juiced.guhs.feature.einde2.zwevers.E2ZweversFeature;
import nl.juiced.guhs.feature.einde2.zwevers.LeegtewalvisguhEntity;
import nl.juiced.guhs.feature.einde2.zwevers.Leegtewalvissen;
import nl.juiced.guhs.feature.einde2.zwevers.Walviszicht;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 phase B (DESIGN §3 "Leegtewalvisguh", CONTRACT_E2_B 7.10), slice reizen: the old captain's questline in harbour
 * pub De Vadsige Walvis (Leegtehaven). Only who believes sees the whale; the whole pub laughs at the captain.
 * <ol start="0">
 *   <li>listen to his story and believe him: from now on the whale can be seen ({@link Walviszicht}); he names a spot in
 *       the void ({@link #WACHT}: about {@value #WACHT_AFSTAND} blocks out from his harbour, away from the middle);</li>
 *   <li>wait there, above the void, for {@value #WACHT_TICKS} ticks: scene {@code walvis} (first the song, then the whale
 *       rises out of the void), and a real one swims by for you;</li>
 *   <li>bring proof: a leegteparel (feed him a knabbelkoorvrucht) to the captain: scene {@code walvis_kroeg} (the pub
 *       believes), and his old horn;</li>
 *   <li>feed whales {@value #VOER} times: the last one you fed becomes your own ({@link Walvis}).</li>
 * </ol>
 * The kroegbaas mocks the captain until the proof, and is ashamed after it. Free of any story gate (the harbour pub is for
 * everyone). Texts: einde2_reizen.py ({@code gui.guhs.verhalen.leegtewalvis.*}, {@code gui.guhs.einde2_reizen.kapitein.*}).
 */
public final class Leegtewalvis {
    public static final String ID = "leegtewalvis";
    public static final int STAPPEN = 4;
    /** Step 1: how far out the waiting spot is, how close counts, how long you wait. */
    public static final int WACHT_AFSTAND = 220, WACHT_BEREIK = 96, WACHT_TICKS = 200;
    /** Step 3: feed this many times. */
    public static final int VOER = 8;
    /** The player data: the waiting spot (a BlockPos as a long). */
    public static final String WACHT = "guhs_einde2_reizen_wachtplek";
    /** The Praat key of the captain's story. */
    static final String PRAAT = "einde2_reizen_kapitein";
    /** The spots of the pub (Leegtehaven). */
    static final String HAVEN = Einde2Ids.LEEGTEHAVEN, KAPITEIN = "leegtehaven/kapitein", KROEGBAAS = "leegtehaven/kroegbaas";

    public static final Verhaallijn LIJN = Verhaallijn.maak(ID, "guheinde").stappen(STAPPEN).icoon("guhs:leegteparel").doelregel(true)
            .nodig((p, stap) -> switch (stap) {
                case 2 -> List.of(Verhaallijn.nodig("guhs:leegteparel", GuhQuests.count(p, E2ZweversFeature.LEEGTEPAREL.get()), 1));
                case 3 -> List.of(Verhaallijn.nodig("guhs:knabbelkoorvrucht", "gui.guhs.einde2_reizen.walvis.gevoerd", lijn().teller(p, "voer"), VOER));
                default -> List.of();
            })
            .beloningen(p -> List.of(Verhaallijn.beloning("guhs:einde2_reizen_walvishoorn", GuhQuests.count(p, E2ReizenFeature.WALVISHOORN.get()) > 0
                            || lijn().stap(p) >= 3),
                    Verhaallijn.beloning("guhs:leegteparel", "gui.guhs.einde2_reizen.walvis.eigen", Walvis.heeft(p))))
            .doel((p, stap) -> switch (stap) {
                case 0, 2 -> Doel.structuur(GuheindeFeature.GUHEINDE, HAVEN, Component.translatable("gui.guhs.einde2_reizen.doel.kroeg"));
                case 1 -> wachtplek(p) == null ? null
                        : Doel.plek(GuheindeFeature.GUHEINDE, wachtplek(p), Component.translatable("gui.guhs.einde2_reizen.doel.wachten"));
                default -> null;
            })
            .registreer();

    /** The waiting time of the players at their spot (step 1). */
    private static final Map<UUID, Integer> GEWACHT = new ConcurrentHashMap<>();

    public static final Cutscene WALVIS = walvisScene();
    public static final Cutscene KROEG = kroegScene();

    private Leegtewalvis() {
    }

    static Verhaallijn lijn() {
        return LIJN;
    }

    static void register() {
        NpcRollen.zet(GuhNpcEntity.Kind.KAPITEINGUH, KAPITEIN, new Kapitein());
        NpcRollen.zet(GuhNpcEntity.Kind.KROEGBAASGUH, KROEGBAAS, new Kroegbaas());
        Praat.luister(PRAAT, (p, spreker, optie) -> {
            if (optie == 1) {
                neemAan(p, spreker);
            } else if (optie == 2 && spreker != null) {
                GuhQuests.say(p, spreker, "gui.guhs.einde2_reizen.kapitein.ongelovig");
            }
        });
        LeegtewalvisguhEntity.GEVOERD.add(Leegtewalvis::gevoerd);
        // the first sighting is the questline's own moment: no whale comes by itself before it
        Leegtewalvissen.MAG_KOMEN = p -> !LIJN.begonnen(p) || LIJN.stap(p) >= 2;
    }

    // =====================================================================================================================
    // the steps
    // =====================================================================================================================

    /** Step 0 -> 1: the player believes the captain. */
    static void neemAan(ServerPlayer p, @Nullable Entity kapitein) {
        if (LIJN.stap(p) != 0) {
            return;
        }
        LIJN.begin(p);
        Walviszicht.zet(p, true);
        Vec3 van = kapitein != null ? kapitein.position() : p.position();
        Vec3 uit = new Vec3(van.x, 0, van.z);
        uit = uit.lengthSqr() < 1 ? new Vec3(1, 0, 0) : uit.normalize();
        BlockPos plek = BlockPos.containing(van.x + uit.x * WACHT_AFSTAND, 112, van.z + uit.z * WACHT_AFSTAND);
        GuhQuests.saved(p).putLong(WACHT, plek.asLong());
        LIJN.zet(p, 1);
        if (kapitein != null) {
            GuhQuests.say(p, kapitein, "gui.guhs.einde2_reizen.kapitein.geloof", plek.getX(), plek.getZ());
        }
        GuhQuests.hint(p, "gui.guhs.verhalen.leegtewalvis.kort.1");
    }

    @Nullable
    static BlockPos wachtplek(ServerPlayer p) {
        long l = GuhQuests.saved(p).getLongOr(WACHT, Long.MIN_VALUE);
        return l == Long.MIN_VALUE ? null : BlockPos.of(l);
    }

    /** Is this player out above the void here: nothing below them at all (an empty column of the Guheinde)? */
    static boolean bovenLeegte(ServerLevel level, Vec3 pos) {
        int x = Mth.floor(pos.x), z = Mth.floor(pos.z);
        return level.hasChunkAt(BlockPos.containing(pos)) && level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) <= level.getMinY() + 1;
    }

    /** Every 10 ticks, for each player at step 1 in the Guheinde: are they waiting at the spot? */
    static void tick(MinecraftServer server) {
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (LIJN.stap(p) != 1 || !(p.level() instanceof ServerLevel level) || !GuheindeFeature.isGuheinde(level) || Cutscenes.bezig(p)) {
                GEWACHT.remove(p.getUUID());
                continue;
            }
            BlockPos plek = wachtplek(p);
            if (plek == null) {
                continue;
            }
            double dx = p.getX() - plek.getX() - 0.5, dz = p.getZ() - plek.getZ() - 0.5;
            boolean daar = dx * dx + dz * dz <= WACHT_BEREIK * WACHT_BEREIK && bovenLeegte(level, p.position());
            if (!daar) {
                if (GEWACHT.remove(p.getUUID()) != null) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_reizen.wacht_weg").withStyle(ChatFormatting.GRAY));
                }
                continue;
            }
            int t = GEWACHT.merge(p.getUUID(), 10, Integer::sum);
            if (t < WACHT_TICKS) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_reizen.wachten." + Math.min(4, t * 5 / WACHT_TICKS))
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC));
                continue;
            }
            GEWACHT.remove(p.getUUID());
            zie(p);
        }
    }

    /** The whale rises (the scene), then a real one swims by under the player: step 2. */
    static void zie(ServerPlayer p) {
        if (Appa.vliegt(p)) {
            Appa.stop(p);
        }
        BlockPos anker = p.blockPosition();
        boolean speelt = Cutscenes.speel(p, WALVIS, anker, Rotation.NONE, speler -> {
            stuurWalvis(speler);
            LIJN.verder(speler, 1);
            GuhQuests.hint(speler, "gui.guhs.verhalen.leegtewalvis.kort.2");
        });
        if (!speelt) {
            GEWACHT.put(p.getUUID(), WACHT_TICKS - 40);
        }
    }

    /** A real whale for the player that just saw one: it passes right beside them, its back at about their feet. */
    @Nullable
    static LeegtewalvisguhEntity stuurWalvis(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) {
            return null;
        }
        double y = Mth.clamp(p.getY() - LeegtewalvisguhEntity.RUG - 1, LeegtewalvisguhEntity.MIN_Y, LeegtewalvisguhEntity.MAX_Y);
        Vec3 voor = LeegtewalvisguhEntity.vooruit(p.getYRot());
        for (int kant : new int[]{1, -1}) {
            Vec3 opzij = new Vec3(-voor.z, 0, voor.x).scale(kant * 22);
            Vec3 plek = new Vec3(p.getX(), y, p.getZ()).add(opzij).subtract(voor.scale(40));
            LeegtewalvisguhEntity w = Leegtewalvissen.op(level, plek, p.getYRot());
            if (w != null) {
                w.zing();
                return w;
            }
        }
        return null;
    }

    /** Step 2 -> 3: the proof on the captain's lectern, the pub believes; the horn. */
    static void bewijs(ServerPlayer p, GuhNpcEntity kapitein) {
        if (LIJN.stap(p) != 2) {
            return;
        }
        if (GuhQuests.count(p, E2ZweversFeature.LEEGTEPAREL.get()) <= 0) {
            GuhQuests.say(p, kapitein, "gui.guhs.einde2_reizen.kapitein.bewijs");
            return;
        }
        BlockPos anker = lessenaar((ServerLevel) p.level(), kapitein);
        // the pearl goes onto the lectern only once the scene was watched (a scene broken off by leaving costs nothing)
        Cutscenes.speel(p, KROEG, anker, Rotation.NONE, speler -> {
            if (LIJN.stap(speler) == 2 && GuhQuests.count(speler, E2ZweversFeature.LEEGTEPAREL.get()) > 0) {
                GuhQuests.take(speler, E2ZweversFeature.LEEGTEPAREL.get(), 1);
                LIJN.verder(speler, 2);
                Minigames.give(speler, new ItemStack(E2ReizenFeature.WALVISHOORN.get()));
                speler.sendSystemMessage(Component.translatable("gui.guhs.einde2_reizen.kapitein.hoorn").withStyle(ChatFormatting.GOLD));
                GuhQuests.hint(speler, "gui.guhs.verhalen.leegtewalvis.kort.3");
            }
        });
    }

    /** Where the proof goes: the lectern of the captain's corner of this harbour (or beside the captain). */
    static BlockPos lessenaar(ServerLevel level, Entity kapitein) {
        StructureStart start = Plekken.bij(level, HAVEN, kapitein.blockPosition(), 32);
        Plekken.Plek plek = Plekken.plek(HAVEN, "walvis_bewijs");
        if (start != null && plek != null) {
            BlockPos b = Plekken.wereld(start, plek);
            if (b != null) {
                return b;
            }
        }
        return kapitein.blockPosition().offset(-3, 0, -2);
    }

    /** A whale was fed by a player (LeegtewalvisguhEntity.GEVOERD): at step 3 it counts, the last one becomes theirs. */
    static void gevoerd(LeegtewalvisguhEntity w, ServerPlayer p) {
        if (LIJN.stap(p) != 3) {
            return;
        }
        if (w.eigenaar() != null && !w.eigenaar().equals(p.getUUID())) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.einde2_reizen.walvis.van_ander").withStyle(ChatFormatting.LIGHT_PURPLE));
            return;
        }
        int n = LIJN.teller(p, "voer") + 1;
        LIJN.teller(p, "voer", n);
        if (n < VOER) {
            p.sendSystemMessage(Component.translatable("gui.guhs.einde2_reizen.walvis.vertrouwen", n, VOER).withStyle(ChatFormatting.LIGHT_PURPLE));
            nl.juiced.guhs.feature.verhaal.VerhaalSync.sync(p);
            return;
        }
        if (Walvis.maakEigen(p, w)) {
            LIJN.zet(p, STAPPEN);
            ServerLevel level = (ServerLevel) w.level();
            level.sendParticles(p, ParticleTypes.HEART, true, false, w.getX(), w.rugY() + 2, w.getZ(), 30, 6, 2, 6, 0);
            level.playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 0.8f);
            p.sendSystemMessage(Component.translatable("gui.guhs.einde2_reizen.walvis.getemd").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            p.sendSystemMessage(Component.translatable("gui.guhs.einde2_reizen.walvis.uitleg").withStyle(ChatFormatting.AQUA));
        }
    }

    // =====================================================================================================================
    // the captain and the kroegbaas
    // =====================================================================================================================

    /** The old captain-guh in his corner of the pub. */
    static final class Kapitein implements NpcRole {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            npc.getLookControl().setLookAt(p);
            switch (LIJN.stap(p)) {
                case 0 -> Praat.scene(p, PRAAT, List.of(
                                new Praat.Regel(npc, "", "gui.guhs.einde2_reizen.kapitein.verhaal.1"),
                                new Praat.Regel(npc, "", "gui.guhs.einde2_reizen.kapitein.verhaal.2"),
                                new Praat.Regel(npc, "", "gui.guhs.einde2_reizen.kapitein.verhaal.3"),
                                new Praat.Regel(npc, "", "gui.guhs.einde2_reizen.kapitein.verhaal.4"),
                                new Praat.Regel(npc, "", "gui.guhs.einde2_reizen.kapitein.verhaal.5")),
                        new Praat.Optie(1, "gui.guhs.einde2_reizen.kapitein.ja"), new Praat.Optie(2, "gui.guhs.einde2_reizen.kapitein.nee"));
                case 1 -> {
                    BlockPos plek = wachtplek(p);
                    if (plek == null) {
                        neemAanOpnieuw(p, npc);
                    } else {
                        GuhQuests.say(p, npc, "gui.guhs.einde2_reizen.kapitein.wacht", plek.getX(), plek.getZ());
                    }
                }
                case 2 -> bewijs(p, npc);
                case 3 -> GuhQuests.say(p, npc, "gui.guhs.einde2_reizen.kapitein.temmen", LIJN.teller(p, "voer"), VOER);
                default -> GuhQuests.say(p, npc, "gui.guhs.einde2_reizen.kapitein.klaar." + (1 + npc.getRandom().nextInt(3)));
            }
        }

        /** (a waiting spot lost by a dev command) he names it again. */
        private static void neemAanOpnieuw(ServerPlayer p, GuhNpcEntity npc) {
            Vec3 van = npc.position();
            Vec3 uit = new Vec3(van.x, 0, van.z);
            uit = uit.lengthSqr() < 1 ? new Vec3(1, 0, 0) : uit.normalize();
            BlockPos plek = BlockPos.containing(van.x + uit.x * WACHT_AFSTAND, 112, van.z + uit.z * WACHT_AFSTAND);
            GuhQuests.saved(p).putLong(WACHT, plek.asLong());
            GuhQuests.say(p, npc, "gui.guhs.einde2_reizen.kapitein.wacht", plek.getX(), plek.getZ());
        }
    }

    /** The kroegbaas behind the tap: mocks the captain (kindly) until the proof, and is ashamed after it. */
    static final class Kroegbaas implements NpcRole {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            npc.getLookControl().setLookAt(p);
            int stap = LIJN.stap(p);
            String wat = stap < 2 ? "spot" : stap == 2 ? "twijfel" : "spijt";
            GuhQuests.say(p, npc, "gui.guhs.einde2_reizen.kroegbaas." + wat + "." + (1 + npc.getRandom().nextInt(2)));
        }
    }

    // =====================================================================================================================
    // the two scenes
    // =====================================================================================================================

    /**
     * {@code einde2_reizen_walvis} (34 s, CONTRACT_E2_B 5: 30-45): in the void, at the waiting spot (anchor = the player's
     * feet). Silence; then the song, low and everywhere; then the whale rises out of the void beneath the player, looks at
     * them with its big guh eyes, sings, and swims by under them.
     */
    private static Cutscene walvisScene() {
        var zang = E2ZweversFeature.WALVIS_ZANG;
        return Cutscene.maak("einde2_reizen_walvis").duur(680).bij(ID)
                .acteur("walvis", E2ZweversFeature.LEEGTEWALVISGUH, new Vec3(0.5, -80, 36), 180)
                .camera(0, new Vec3(9, 2.5, -9), new Vec3(0.5, 1, 0.5)).camera(100, new Vec3(7, 1.5, -6), new Vec3(0.5, 0, 0.5))
                .geluid(10, zang, 2f, 0.5f)
                .zeg(20, "", "stil", 60)
                .geluid(60, zang, 3f, 0.6f)
                .zeg(70, "", "lied", 60)
                .geluid(95, zang, 4f, 0.75f)
                .deeltjes(95, ParticleTypes.NOTE, new Vec3(0.5, -12, 6), 14, 6)
                .cameraKnip(120, new Vec3(14, -30, -8), new Vec3(0.5, -6, 10))
                .loop("walvis", 110, 300, new Vec3(0.5, -17, 8))
                .geluid(150, zang, 6f, 0.67f)
                .schud(150, 0.6f, 70)
                .deeltjes(170, ParticleTypes.NOTE, new Vec3(0.5, -6, 0), 20, 8)
                .zeg(180, "", "omhoog", 60)
                .camera(230, new Vec3(26, 3, 2), new Vec3(0.5, -6, 4))
                .animatie("walvis", 250, "zing")
                .geluid(250, zang, 8f, 0.5f)
                .deeltjes(260, ParticleTypes.NOTE, new Vec3(0.5, -4, -10), 24, 5)
                .zeg(270, "", "oogjes", 70)
                .loop("walvis", 310, 640, new Vec3(0.5, -17, -70))
                .cameraKnip(330, new Vec3(-12, 4, -6), new Vec3(0.5, -6, -6))
                .geluid(380, zang, 7f, 0.75f)
                .zeg(400, "", "gelijk", 70)
                .camera(500, new Vec3(0.5, 22, 24), new Vec3(0.5, -10, -28))
                .geluid(520, zang, 6f, 0.6f)
                .zeg(580, "", "gelooft", 70)
                .camera(680, new Vec3(0.5, 30, 34), new Vec3(0.5, -14, -50))
                .cameraOntwijkt()
                .registreer();
    }

    /**
     * {@code einde2_reizen_walvis_kroeg} (20 s, CONTRACT_E2_B 5: 15-25): in De Vadsige Walvis (anchor = the lectern of the
     * captain's corner, {@code leegtehaven/walvis_bewijs}). The pub laughs; the pearl on the lectern; silence; the kroegbaas
     * says sorry; the whole pub raises its glass to the great singing one.
     */
    private static Cutscene kroegScene() {
        return Cutscene.maak("einde2_reizen_walvis_kroeg").duur(400).bij(ID).verbergEcht(16)
                .speler(new Vec3(0.5, 0, 2.2), 180)
                .npc("kapitein", GuhNpcEntity.Kind.KAPITEINGUH, new Vec3(2.6, 0, 1.6), 100)
                .npc("kroegbaas", GuhNpcEntity.Kind.KROEGBAASGUH, new Vec3(-3.5, 0, 9.5), 180)
                .npc("veerguh", GuhNpcEntity.Kind.VEERGUH, new Vec3(-5.5, 0, 5.0), -60)
                .npc("bouwer", GuhNpcEntity.Kind.SCHEEPSBOUWERGUH, new Vec3(-6.5, 0, 7.5), -80)
                .npc("racer", GuhNpcEntity.Kind.RINGENRACEGUH, new Vec3(-4.0, 0, 7.0), -120)
                .camera(0, new Vec3(-7, 3, 11), new Vec3(0.5, 1, 2)).camera(100, new Vec3(-5.5, 2.6, 8), new Vec3(0.5, 1, 1.5))
                .zeg(10, "kroegbaas", "spot", 60)
                .animatie("veerguh", 40, "zwaai")
                .zeg(40, "veerguh", "lach", 60)
                .animatie("speler", 75, "buk")
                .animatie("speler", 95, "sta")
                .geluid(80, () -> SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f, 0.7f)
                .deeltjes(80, ParticleTypes.END_ROD, new Vec3(0.5, 1.2, 0.5), 16, 0.3)
                .deeltjes(90, ParticleTypes.WITCH, new Vec3(0.5, 1.4, 0.5), 10, 0.3)
                .zeg(90, "", "parel", 70)
                .cameraKnip(120, new Vec3(1.9, 1.6, 3.0), new Vec3(0.5, 1.0, 0.5))
                .kijk("bouwer", 120, new Vec3(0.5, 1, 0.5)).kijk("racer", 120, new Vec3(0.5, 1, 0.5)).kijk("veerguh", 120, new Vec3(0.5, 1, 0.5))
                .zeg(130, "bouwer", "fluister", 60)
                .loop("kroegbaas", 150, 215, new Vec3(-1.5, 0, 3.5))
                .zeg(160, "kroegbaas", "stil", 50)
                .cameraKnip(220, new Vec3(-1.6, 1.8, 4.2), new Vec3(2.6, 1.4, 1.6))
                .kijk("kroegbaas", 220, new Vec3(2.6, 1.2, 1.6))
                .zeg(230, "kapitein", "zestig", 80)
                .zeg(270, "kroegbaas", "sorry", 70)
                .camera(300, new Vec3(-7, 3, 11), new Vec3(0, 1, 3))
                .animatie("veerguh", 310, "zwaai").animatie("bouwer", 310, "zwaai").animatie("racer", 310, "zwaai").animatie("kroegbaas", 312, "zwaai")
                .geluid(310, E2ReizenFeature.PROOST, 1.5f, 1.0f)
                .zeg(320, "veerguh", "proost", 50)
                .kijk("kapitein", 350, new Vec3(0.5, 1.6, 2.2))
                .zeg(355, "kapitein", "jij", 60)
                .registreer();
    }

    static void leeg() {
        GEWACHT.clear();
    }
}
