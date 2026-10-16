package nl.juiced.guhs.feature.einde2.h4vuur;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.NpcRole;
import nl.juiced.guhs.feature.bestaand.Schijn;
import nl.juiced.guhs.feature.einde2.Bewoners;
import nl.juiced.guhs.feature.einde2.audio.E2Audio;
import nl.juiced.guhs.feature.einde2.reizen.E2Reizen;
import nl.juiced.guhs.feature.einde2.reizen.E2Team;
import nl.juiced.guhs.feature.einde2.verhaal.E2Verhaal;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.feature.kleding.KledingUnlocks;
import nl.juiced.guhs.feature.ring.Zicht;
import nl.juiced.guhs.feature.verhaal.Cutscene;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import nl.juiced.guhs.feature.verhaal.Doel;
import nl.juiced.guhs.feature.verhaal.NpcRollen;
import nl.juiced.guhs.feature.verhaal.Praat;
import nl.juiced.guhs.feature.verhaal.Verhaallijn;
import nl.juiced.guhs.feature.verhaal.Verteller;
import nl.juiced.guhs.quest.GuhQuests;

/**
 * einde2 phase B, slice h4_vuur: the story of chapter 4 (line {@value E2Verhaal#VUUR}) and of the finale (line
 * {@value E2Verhaal#FINALE}) in the Vuurnatie-hoofdstad (DESIGN 5, CONTRACT_E2_B 7.5). This class is the direction: the two
 * questlines, what each step needs and where, and what happens when a player gets there. The minigames are classes of their
 * own ({@link Dansfeest}, {@link Drakendans}, {@link Rotsblok}, {@link Luchtvloot}, {@link Duel}), the scripts of the
 * cutscenes are {@link Scenes}.
 * <p>
 * Everything is per player and lives in main's {@link Verhaallijn}s: a step only moves forward in the {@code daarna} of a
 * scene or at the end of a minigame, so a player who logs out in the middle starts that bit again (no step is ever lost,
 * nothing costs anything). Every few ticks {@link #regie} looks at where the player stands and what their step asks for.
 * Unlimited players can do every step at the same spot at the same time; a guest's own step never changes by a host.
 */
public final class E2H4Vuur {
    // --- the steps of chapter 4 (guhvatar_vuur) ---------------------------------------------------------------------------
    public static final int V_AANKOMST = 0, V_SCHOOL = 1, V_DANS = 2, V_EENDJES = 3, V_ZWARTE_ZON = 4, V_GEVANGENIS = 5, V_KAMP = 6,
            V_GOEDMAKEN = 7, V_ROTSBLOK = 8, V_DRAKENDANS = 9, V_TONEEL = 10, V_TENT = 11, V_STAPPEN = 12;
    // --- the steps of the finale (guhvatar_finale) ------------------------------------------------------------------------
    public static final int F_TOP = 0, F_SCHILDPAD = 1, F_VLOOT = 2, F_AGNI_KAI = 3, F_DUEL = 4, F_KRONING = 5, F_AFTITELING = 6,
            F_STAPPEN = 7;
    /** The names of the steps for the dev command ({@code /guhs einde2 h4 ga <naam>}), in playing order. */
    public static final List<String> STAPNAMEN = List.of("aankomst", "school", "dansfeest", "eendjes", "zwarte_zon", "gevangenis", "kamp",
            "goedmaken", "kokend_rotsblok", "drakendans", "toneel", "tent", "top", "leeuwschildpad", "luchtvloot", "agni_kai", "duel",
            "kroning", "aftiteling", "klaar");

    /** Own flags of the lines (besides the cross-slice ones of E2Verhaal). */
    static final String V_TOPH = "toph", V_KATARA = "katara", V_BINNEN = "binnen";
    /** The narrator card of the chapter (with its drawn map). */
    public static final String KAART = "einde2_h4_vuur";
    /** Tag of every entity this slice spawns for one player (a guard, Zuka in the dance, an airship): never kept over a restart. */
    static final String TIJDELIJK = "guhs_einde2_h4_vuur_tijdelijk";
    /** Per-player data that is not a step: the day of the last treat in the capital. */
    static final String TRAKTATIE = "guhs_einde2_h4_vuur_traktatie_dag";
    /** The text prefixes. */
    static final String Q = "quest.guhs.einde2_h4_vuur.", G = "gui.guhs.einde2_h4_vuur.";

    public static Verhaallijn VUUR, FINALE;

    private E2H4Vuur() {
    }

    // =====================================================================================================================
    // register (common, both sides)
    // =====================================================================================================================

    static void register() {
        VUUR = Verhaallijn.maak(E2Verhaal.VUUR, E2Verhaal.GROEP).stappen(V_STAPPEN).na(E2Verhaal.AARDE).icoon("guhs:einde2_bouw_vuur_embleem")
                .doelregel(true).sleutel(E2H4Vuur::sleutelVuur).extraSleutels("7_katara", "8_binnen")
                .beloningen(p -> List.of(Verhaallijn.beloning("guhs:einde2_bouw_vuur_embleem", G + "beloning.vermomming", VUUR.klaar(p)),
                        Verhaallijn.beloning("guhs:kaas_knabbels", G + "beloning.knabbels", VUUR.klaar(p))))
                .doel((p, stap) -> doel())
                .registreer();
        FINALE = Verhaallijn.maak(E2Verhaal.FINALE, E2Verhaal.GROEP).stappen(F_STAPPEN).na(E2Verhaal.VUUR).icoon("guhs:guhvatar_beeld")
                .doelregel(true).sleutel(E2H4Vuur::sleutelFinale).extraSleutels("0_geest")
                .beloningen(p -> List.of(Verhaallijn.beloning("guhs:guhvatar_beeld", FINALE.klaar(p)),
                        Verhaallijn.beloning("minecraft:name_tag", G + "beloning.titel", FINALE.klaar(p)),
                        Verhaallijn.beloning("guhs:einde2_bouw_vuur_portret_mikai", G + "beloning.litteken", FINALE.klaar(p))))
                .doel((p, stap) -> doel())
                .registreer();
        Verteller.registreer(KAART, 5, E2Verhaal.VUUR);
        Scenes.registreer();
        VUUR.opStap((p, oud, nieuw) -> {
            Look.sync(p);
            Zicht.kijkOpnieuw(p);
            if (nieuw >= V_STAPPEN) {
                vuurKlaar(p);
            }
        });
        FINALE.opStap((p, oud, nieuw) -> {
            Zicht.kijkOpnieuw(p);
            if (nieuw >= F_STAPPEN) {
                finaleKlaar(p);
            }
        });
        // the residents of the capital that belong to a moment of the story (CONTRACT_E2_B 4.3)
        Bewoners.BIJ_MAKEN.add(E2H4Vuur::bijMaken);
        // the spots this slice talks for (CONTRACT_E2_B 4.4)
        NpcRollen.zet(GuhNpcEntity.Kind.OOM_MIROH_LOTUS, Plek.STRUCTUUR + "/miroh_tent", MIROH);
        NpcRollen.zet(GuhNpcEntity.Kind.VUURNATIE_BURGER_MIKA, Plek.STRUCTUUR + "/bakker", BAKKER);
        Praat.luister(Goedmaken.TOPH, Goedmaken::antwoordToph);
        Praat.luister(Goedmaken.KATARA, Goedmaken::antwoordKatara);
        Praat.luister(FINALE_START, E2H4Vuur::antwoordMiroh);
        Schijn.registreer(Oplichten::vul);
        NeoForge.EVENT_BUS.addListener(E2H4Vuur::opTick);
        NeoForge.EVENT_BUS.addListener(E2H4Vuur::opKlik);
        NeoForge.EVENT_BUS.addListener(E2H4Vuur::opWeg);
        NeoForge.EVENT_BUS.addListener(E2H4Vuur::opBinnen);
        NeoForge.EVENT_BUS.addListener(E2H4VuurCommando::register);
        NeoForge.EVENT_BUS.addListener(Look::opVolgen);
        NeoForge.EVENT_BUS.addListener(Look::opLogin);
        NeoForge.EVENT_BUS.addListener(Look::opDimensie);
    }

    private static Doel doel() {
        return Doel.structuur(GuheindeFeature.GUHEINDE, Plek.STRUCTUUR, Component.translatable("structure.guhs." + Plek.STRUCTUUR));
    }

    private static String sleutelVuur(ServerPlayer p, int stap) {
        if (stap == V_GOEDMAKEN && VUUR.vlag(p, V_TOPH)) {
            return "7_katara";
        }
        if (stap == V_ROTSBLOK && VUUR.vlag(p, V_BINNEN)) {
            return "8_binnen";
        }
        return String.valueOf(stap);
    }

    private static String sleutelFinale(ServerPlayer p, int stap) {
        return stap == F_TOP && FINALE.vlag(p, E2Verhaal.V_WITTE_LOTUS) ? "0_geest" : String.valueOf(stap);
    }

    /** Is chapter 4 open for this player: chapter 3 done (or, on a branch without chapter 3, simply there)? */
    static boolean vuurOpen(ServerPlayer p) {
        return E2Verhaal.lijn(E2Verhaal.AARDE) == null || E2Verhaal.klaar(p, E2Verhaal.AARDE);
    }

    /** Is the player busy with something of this story (a scene, a talk, a minigame, a moment that is planned)? */
    static boolean bezig(ServerPlayer p) {
        return Cutscenes.bezig(p) || Praat.bezig(p) || Later.gepland(p) || Dansfeest.actief(p) || Drakendans.actief(p) || Rotsblok.actief(p)
                || Luchtvloot.actief(p) || Duel.actief(p);
    }

    /** Is this player in the Vuurnatie disguise (chapter 4, from the arrival until the Dag van de Zwarte Zon)? */
    public static boolean vermomd(ServerPlayer p) {
        int s = VUUR.stap(p);
        return s >= V_SCHOOL && s <= V_ZWARTE_ZON && vuurOpen(p);
    }

    // =====================================================================================================================
    // the direction: every few ticks, what the step of this player asks for where they stand
    // =====================================================================================================================

    private static void opTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || p.isSpectator()) {
            return;
        }
        Later.tick(p);
        Dansfeest.tick(p);
        Drakendans.tick(p);
        Rotsblok.tick(p);
        Luchtvloot.tick(p);
        Duel.tick(p);
        if ((p.tickCount + p.getId()) % 10 == 0) {
            regie(p);
        }
    }

    /** What the player's step asks for, where they stand now. Public for the game tests and the dev command. */
    public static void regie(ServerPlayer p) {
        if (bezig(p) || !p.isAlive()) {
            return;
        }
        if (nl.juiced.guhs.feature.einde2.geestenwereld.Geestenwereld.in(p)) {
            Finale.inGeestenwereld(p);
            return;
        }
        if (Plek.guheinde(p) == null || !Plek.inHoofdstad(p) || !vuurOpen(p)) {
            return;
        }
        if (!VUUR.klaar(p)) {
            hoofdstuk(p, VUUR.stap(p));
        } else if (!FINALE.klaar(p)) {
            Finale.regie(p, FINALE.stap(p));
        }
    }

    private static void hoofdstuk(ServerPlayer p, int stap) {
        switch (stap) {
            case V_AANKOMST -> aankomst(p);
            case V_SCHOOL -> {
                if (Plek.in(p, "school")) {
                    speel(p, Scenes.SCHOOL, "schoolbank_speler", q -> VUUR.verder(q, V_SCHOOL));
                }
            }
            case V_DANS -> {
                if (Plek.in(p, "dansvloer")) {
                    Dansfeest.start(p);
                } else if (Plek.in(p, "dansgrot") && VUUR.eenmalig(p, "dansgrot_hint")) {
                    zeg(p, "dans.uitnodiging");
                }
            }
            case V_EENDJES -> {
                if (Plek.bij(p, "brood", 6)) {
                    BlockPos brood = Plek.blok(p, "brood");
                    if (brood != null) {
                        Schijn.deeltjes(p, ParticleTypes.HAPPY_VILLAGER, Vec3.atCenterOf(brood).add(0, 0.6, 0), 3, 0.3, 0.0);
                    }
                }
            }
            case V_ZWARTE_ZON -> {
                if (Plek.in(p, "invasie_strand")) {
                    zwarteZon(p);
                }
            }
            case V_GEVANGENIS -> gevangenis(p);
            case V_KAMP -> {
                if (Plek.in(p, "kamp")) {
                    speel(p, Scenes.KAMP, "appa_kamp", q -> VUUR.verder(q, V_KAMP));
                }
            }
            case V_GOEDMAKEN -> {
                if (Plek.in(p, "kamp")) {
                    Goedmaken.praat(p);
                }
            }
            case V_ROTSBLOK -> Rotsblok.regie(p);
            case V_DRAKENDANS -> {
                if (Plek.in(p, "drakendans")) {
                    Drakendans.start(p);
                } else if (Plek.in(p, "zonnekrijger_ruine") && Plek.bij(p, "zonnekrijger", 8) && VUUR.eenmalig(p, "zonnekrijger")) {
                    zeg(p, "drakendans.zonnekrijger");
                }
            }
            case V_TONEEL -> {
                if (Plek.bij(p, "publiek_speler", 5)) {
                    speel(p, Scenes.TONEEL, "publiek_speler", q -> VUUR.verder(q, V_TONEEL));
                }
            }
            case V_TENT -> {
                if (Plek.in(p, "lotus_tent")) {
                    E2Audio.moment(p, "tent");
                    speel(p, Scenes.TENT, "verzoening_zuka", q -> {
                        E2Audio.stopMoment(q, 40);
                        VUUR.verder(q, V_TENT);
                    });
                }
            }
            default -> {
            }
        }
    }

    /** Step 0: the chapter's narrator card, then the arrival in disguise. */
    private static void aankomst(ServerPlayer p) {
        if (!Plek.in(p, "aankomst") && !Plek.in(p, "kamp") || E2Reizen.landtNog(p)) {
            return;                                                 // (merge_fix1: first Appa lands on his spot)
        }
        Verteller.toon(p, KAART, q -> speel(q, Scenes.AANKOMST, "aankomst_speler", r -> {
            if (VUUR.verder(r, V_AANKOMST)) {
                E2Team.zetInStructuur(r, Plek.STRUCTUUR);
                zeg(r, "aankomst.vermomd");
            }
        }));
    }

    /** Step 4: the invasion. The scene plays from just inside the crater gate (the whole city within reach of the client). */
    private static void zwarteZon(ServerPlayer p) {
        Vec3 poort = Plek.staan(p, "rustpunt_2");
        ServerLevel g = Plek.guheinde(p);
        if (poort == null || g == null) {
            return;
        }
        Plek.breng(p, g, poort, 180f);
        Later.plan(p, 20, q -> {
            E2Audio.moment(q, "zwarte_zon");
            speel(q, Scenes.ZWARTE_ZON, "rustpunt_2", r -> {
                E2Audio.stopMoment(r, 40);
                if (VUUR.verder(r, V_ZWARTE_ZON)) {
                    Plek.breng(r, "rustpunt_4", 180f);
                    zeg(r, "gevangenis.volg");
                    Look.sync(r);
                }
            });
        });
    }

    /** Step 5: Zuka's visit to his uncle's cell under the palace. The iron door of the bunker lets a player of this step through. */
    private static void gevangenis(ServerPlayer p) {
        if (Plek.bij(p, "miroh_cel", 8)) {
            speel(p, Scenes.GEVANGENIS, "miroh_cel", q -> {
                if (VUUR.verder(q, V_GEVANGENIS)) {
                    Plek.breng(q, "rustpunt_1", 0f);
                    zeg(q, "kamp.vlucht");
                }
            });
        } else if (Plek.bij(p, "bunker_ingang", 3)) {
            Plek.breng(p, "rustpunt_4", 180f);
            zeg(p, "gevangenis.deur");
        }
    }

    // =====================================================================================================================
    // the turtle-ducks: a click on the bread basket by the palace pond (step 3)
    // =====================================================================================================================

    private static void opKlik(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.getLevel().isClientSide() || VUUR.stap(p) != V_EENDJES || bezig(p)) {
            return;
        }
        BlockPos brood = Plek.blok(p, "brood");
        if (brood == null || brood.distManhattan(event.getPos()) > 1) {
            return;
        }
        ServerLevel level = p.level();
        Vec3 vijver = Plek.midden(p, "paleisvijver");
        Vec3 waar = vijver != null ? vijver.add(0, 1.2, 0) : Vec3.atCenterOf(brood);
        Schijn.deeltjes(p, ParticleTypes.HEART, waar, 10, 1.5, 0.0);
        Schijn.deeltjes(p, ParticleTypes.SPLASH, waar, 30, 1.8, 0.1);
        Schijn.geluid(p, SoundEvents.GENERIC_SPLASH, BlockPos.containing(waar), 0.8f, 1.4f);
        level.getServer().execute(() -> {
            if (VUUR.verder(p, V_EENDJES)) {
                zeg(p, "eendjes.gevoerd");
                zeg(p, "eendjes.zwarte_zon");
            }
        });
        event.setCanceled(true);
    }

    // =====================================================================================================================
    // the residents of the capital and the roles
    // =====================================================================================================================

    private static void bijMaken(Entity e, String plek) {
        if (!plek.startsWith(Plek.STRUCTUUR + "/")) {
            return;
        }
        switch (plek.substring(Plek.STRUCTUUR.length() + 1)) {
            // the Vuurheer sits on his throne until the finale is done (after it he is a round, content Mika far away)
            case "mikai" -> Zicht.alleenBij(e, E2Verhaal.FINALE, 0, F_STAPPEN - 1);
            // prince Zuka in his old room: until he joins the team
            case "kamer_zuka" -> Zicht.alleenBij(e, E2Verhaal.VUUR, 0, V_DRAKENDANS);
            // Suki-guh in her cell: until she is free
            case "suki" -> Zicht.alleenBij(e, E2Verhaal.VUUR, 0, V_ROTSBLOK);
            // the White Lotus gathers after the dragons
            case "miroh_tent", "bumi", "pakguh_lotus", "piandao" -> Zicht.alleenBij(e, E2Verhaal.VUUR, V_TONEEL, 99);
            default -> {
            }
        }
    }

    /** The sleutel of the talk with Oom Miroh that starts the finale. */
    static final String FINALE_START = "einde2_h4_vuur_finale";

    /** Oom Miroh in the tent of the White Lotus: the start of the finale, and tea ever after. */
    private static final NpcRole MIROH = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            npc.speel("praat");
            if (VUUR.klaar(p) && FINALE.stap(p) == F_TOP) {
                Praat.scene(p, FINALE_START, List.of(
                        new Praat.Regel(npc, "", Q + "finale.miroh.1"),
                        new Praat.Regel(null, G + "naam.aang", Q + "finale.aang.1"),
                        new Praat.Regel(npc, "", Q + "finale.miroh.2")),
                        new Praat.Optie(1, G + "optie.mediteer"), new Praat.Optie(2, G + "optie.straks"));
            } else if (FINALE.klaar(p)) {
                GuhQuests.say(p, npc, Q + "miroh.thee." + (p.getRandom().nextInt(3)));
            } else {
                GuhQuests.say(p, npc, Q + "miroh.komeet");
            }
        }
    };

    private static void antwoordMiroh(ServerPlayer p, @Nullable Entity spreker, int optie) {
        if (optie == 1 && VUUR.klaar(p) && FINALE.stap(p) == F_TOP) {
            Praat.sluit(p);
            Finale.naarDeTop(p);
        }
    }

    /** The baker of Heet Hapje: after the story every day a little party (one treat a day). */
    private static final NpcRole BAKKER = new NpcRole() {
        @Override
        public void talk(GuhNpcEntity npc, ServerPlayer p) {
            npc.speel("groet");
            if (!E2Verhaal.finaleKlaar(p)) {
                GuhQuests.say(p, npc, Q + "bakker." + (VUUR.stap(p) >= V_ZWARTE_ZON ? "spannend" : "vuurvlokken"));
                return;
            }
            long dag = Math.floorDiv(nl.juiced.guhs.world.GuhTime.dayTime(p.level().getServer().overworld()), 24000L);
            if (GuhQuests.saved(p).getLongOr(TRAKTATIE, -1L) == dag) {
                GuhQuests.say(p, npc, Q + "bakker.morgen");
                return;
            }
            GuhQuests.saved(p).putLong(TRAKTATIE, dag);
            GuhQuests.say(p, npc, Q + "bakker.feest");
            Minigames.give(p, new ItemStack(nl.juiced.guhs.registry.ModItems.KAAS_KNABBELS.get(), 6));
            Minigames.give(p, new ItemStack(Items.FIREWORK_ROCKET, 3));
            p.level().sendParticles(ParticleTypes.FIREWORK, npc.getX(), npc.getY() + 2.2, npc.getZ(), 30, 0.6, 0.6, 0.6, 0.05);
        }
    };

    // =====================================================================================================================
    // the rewards
    // =====================================================================================================================

    private static void vuurKlaar(ServerPlayer p) {
        if (VUUR.eenmalig(p, "beloning")) {
            KledingUnlocks.ontgrendel(p, GuhClothes.VUURNATIE_VERMOMMING);
            Minigames.give(p, new ItemStack(nl.juiced.guhs.registry.ModItems.KAAS_KNABBELS.get(), 24));
            p.sendSystemMessage(Component.translatable(Q + "beloning.vuur").withStyle(ChatFormatting.GOLD));
        }
        p.sendSystemMessage(Component.translatable(Q + "hint.finale").withStyle(ChatFormatting.GOLD));
    }

    private static void finaleKlaar(ServerPlayer p) {
        if (FINALE.eenmalig(p, "beloning")) {
            KledingUnlocks.ontgrendel(p, GuhClothes.ZUKA_LITTEKEN);
            Minigames.give(p, Beeld.item(p));
            Minigames.give(p, new ItemStack(nl.juiced.guhs.registry.ModItems.KAAS_KNABBELS.get(), 64));
            p.sendSystemMessage(Component.translatable(Q + "beloning.finale").withStyle(ChatFormatting.GOLD));
        }
        GidsFeature.grant(p, "guheinde/einde2_h4_vuur_guhvatar");
        p.sendSystemMessage(Component.translatable(Q + "hint.daarna").withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // =====================================================================================================================
    // helpers
    // =====================================================================================================================

    /** Plays a scene at a marker of the capital (no rotation: a leegte_bouw is never turned). False: could not start. */
    static boolean speel(ServerPlayer p, Cutscene s, String anker, @Nullable java.util.function.Consumer<ServerPlayer> daarna) {
        BlockPos a = Plek.blok(p, anker);
        return a != null && Cutscenes.speel(p, s, a, Rotation.NONE, daarna);
    }

    /** A line of the story in the chat (gold: what to do; plain: someone talks). */
    static void zeg(ServerPlayer p, String key, Object... args) {
        p.sendSystemMessage(Component.translatable(Q + key, args).withStyle(ChatFormatting.GOLD));
    }

    /** A short line above the hotbar. */
    static void balk(ServerPlayer p, String key, Object... args) {
        p.sendOverlayMessage(Component.translatable(Q + key, args).withStyle(ChatFormatting.GOLD));
    }

    /** A temporary actor of this slice for one player: never saved over a restart ({@link #opBinnen}). */
    static <T extends Entity> T eigen(T e, ServerPlayer p) {
        e.addTag(TIJDELIJK);
        Zicht.alleenVoor(e, p.getUUID());
        return e;
    }

    /** A temporary GuhNpcEntity of this kind for one player, at a spot. */
    @Nullable
    static GuhNpcEntity npc(ServerPlayer p, GuhNpcEntity.Kind kind, Vec3 plek, float yaw) {
        GuhNpcEntity npc = nl.juiced.guhs.registry.ModEntities.GUH_NPC.get().create(p.level(), net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (npc == null) {
            return null;
        }
        npc.setKind(kind);
        npc.snapTo(plek.x, plek.y, plek.z, yaw, 0);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        eigen(npc, p);
        p.level().addFreshEntity(npc);
        return npc;
    }

    /** A temporary actor that was saved by mistake (a crash in the middle of a minigame): gone when it is loaded again. */
    private static void opBinnen(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.loadedFromDisk() && event.getEntity().entityTags().contains(TIJDELIJK)) {
            event.setCanceled(true);
        }
    }

    private static void opWeg(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            Dansfeest.stop(p, false);
            Drakendans.stop(p, false);
            Rotsblok.stop(p);
            Luchtvloot.stop(p, false);
            Duel.stop(p);
            Lagen.weg(p);
            Later.vergeet(p.getUUID());
        }
    }

    /** (dev, tests) every bit of this slice's own state of a player that is not in the lines. */
    static void wisEigen(ServerPlayer p) {
        Dansfeest.stop(p, false);
        Drakendans.stop(p, false);
        Rotsblok.stop(p);
        Luchtvloot.stop(p, false);
        Duel.stop(p);
        Lagen.weg(p);
        Later.vergeet(p.getUUID());
        GuhQuests.saved(p).remove(TRAKTATIE);
        List<String> scenes = new ArrayList<>();
        for (Cutscene s : Cutscene.alle()) {
            if (s.id().startsWith("einde2_h4_vuur_")) {
                scenes.add(s.id());
            }
        }
        scenes.forEach(id -> Cutscenes.vergeet(p, id));
        Verteller.vergeet(p, KAART);
        Look.sync(p);
    }
}
