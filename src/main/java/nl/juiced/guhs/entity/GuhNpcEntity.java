package nl.juiced.guhs.entity;

import java.util.Locale;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import nl.juiced.guhs.quest.GuhQuests;
import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import nl.juiced.guhs.feature.verhaal.Cutscenes;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.UUIDUtil;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
/**
 * The sitting guh characters of the quests (same model as the Hungry Guh): Moeder Vadsig (huge, in her shrine), the
 * Tandarts-guh (in the mouth), the Maagenzym-guh (in every stomach) and the Slee-guh (in the sled hut).
 * They never move, can't be hurt, and talk to you when you right-click them (see {@link GuhQuests}).
 * The Kermis-guh (at the guh kermis) trades instead: the kermis outfit for kermisbonnen. Verstopguhtje (on the roof of
 * the verstopguh house) runs hide-and-seek games ({@link nl.juiced.guhs.quest.VerstopGame}) and sells the detective outfit.
 */
public class GuhNpcEntity extends PathfinderMob implements GeoEntity, net.minecraft.world.item.trading.Merchant {
    public enum Kind {
        MOEDER_VADSIG(2.6f),
        TANDARTS(1.0f),
        MAAGENZYM(0.85f),
        SLEE_GUH(1.0f),
        KERMIS_GUH(1.0f),
        VERSTOPGUHTJE(1.0f),
        TIPGUH(0.8f),
        POORTWACHTER(1.3f),
        REISGUH(1.0f),
        // --- 2.4: the characters of the minigames and the rare structures (their roles: nl.juiced.guhs.feature.*) ---
        SHOWGUH(1.0f),
        RACEGUH(1.0f),
        MEPGUH(1.0f),
        DJGUH(1.0f),
        GOLFGUH(1.0f),
        SMULGUH(1.1f),
        VISGUH(1.0f),
        MIJNGUH(1.0f),
        BIBLIOTHECARIS(1.0f),
        // --- 2.5 ---
        ZEEMEERGUH(1.0f),
        // --- 2.7 ---
        BOSWACHTERGUH(1.0f),
        KNABBELPLUKKER(1.0f),
        GRILLGUH(1.0f),
        // --- 2.8 (Knuffeldal; the roles: KnuffeldalFeature and the phase-2 features, see Features.role) ---
        BURGEMEESTERGUH(1.1f),
        BAKKERGUH(1.0f),
        JUF_KNUFFEL(1.0f),
        THEEGUH(1.0f),
        KAPPERGUH(1.0f),
        BOERINNEGUH(1.0f),
        STERRENKIJKERGUH(1.0f),
        BALLONGUH(1.0f),
        OPA_GUH(0.95f),
        BADMEESTERGUH(1.0f),
        /** Cocotje, in her little house in the Knuffeldal town (knuffeldal feature): "WEET JIJ WAAR ZE ZIJN??????" */
        COCOTJE(0.9f),
        // --- 2.9 (De Grote Guhspelen; the roles: the 2.9 features, see Features.role) ---
        SJOELGUH(1.0f),
        DOOLHOFGUH(1.0f),
        KATAPULTGUH(1.0f),
        SPELLEIDERGUH(1.0f),
        SCHAATSMEESTERGUH(1.0f),
        STEMPELGUH(1.0f),
        CIRCUITGUH(1.0f),
        BRANDWEERGUH(1.0f),
        POLITIEGUH(1.0f),
        APOTHEKERGUH(1.0f),
        BOUWVAKKERGUH(1.0f),
        // --- 3.0 (Guhverhalen; the roles: nl.juiced.guhs.feature.verhaal.NpcRollen, set by the owner slices) ---
        TIMMERGUH(1.0f), BORIS(0.9f), STEELE_MIKA(1.0f), MUK(1.3f), LUK(1.3f), ROSY(0.55f), WITTE_WOLFGUH(1.25f),
        KNABBELKLOON(1.0f), WOLKENHOEDER(1.05f), LILO_GUH(0.8f), NANI_GUH(1.0f), TIKIGUH(1.0f),
        // --- bbq2 (CONTRACT_130 5.2; the roles: NpcRollen, set by the owner slices; no Guhdex pages) ---
        // <bbq2>
        UITVINDERGUH(1.0f), WACHTERGUH(1.0f), KNUFFELMAKERGUH(1.0f), MIKA_OMA(0.9f), STALKNECHTGUH(1.0f), TOLWACHTER_MIKA(1.0f),
        ARCHEOLOOGGUH(1.0f), MIJNWERKERGUH(1.0f), VERZORGERGUH(1.0f), KAMPBAASGUH(1.0f), HOUTHAKKERGUH(1.0f),
        MARKTMEESTER_MIKA(1.0f), TORENWACHTERGUH(1.0f), PEPERTELERGUH(1.0f), GUHDALF(1.15f), SMIKAGOL(0.85f), ARAGUH(1.0f),
        LEGUHLAS(1.0f), GIMGUH(0.85f), BOROMIKA(1.0f), MERRIE(0.8f), PIPPGUH(0.8f), GUHROND(1.0f), GUHLADRIEL(1.0f), SAUSUMAN(1.2f),
        PADGUH(0.8f), PERZIKGUH(1.0f),
        // </bbq2>
        // <timmerguh>
        // </timmerguh>
        // einde2, cast_team (CONTRACT_E2 11): Team Guhvatar and their friends; a look of a character is a kind of its own (<BASIS>_<LOOK>)
        // <einde2_cast_team>
        AANG_GUH(0.9f), KATARA_GUH(1.0f), SOKKA_GUH(1.0f), TOPH_GUH(0.8f),
        // (CONTRACT_E2 11.9: Zuka in six looks)
        ZUKA(1.0f), ZUKA_PRINS(1.0f), ZUKA_VUURHEER(1.05f), ZUKA_BALLING(1.0f), ZUKA_BLAUWE_GEEST(1.0f), ZUKA_TEAM(1.0f),
        OOM_MIROH(1.0f), OOM_MIROH_GEVANGENE(1.0f), OOM_MIROH_LOTUS(1.0f),
        SUKI_GUH(1.0f), SUKI_GUH_GEVANGENE(1.0f), YUE_GUH(0.95f), YUE_GUH_MAAN(0.95f),
        VEERGUH(1.0f), SCHEEPSBOUWERGUH(1.05f), KROEGBAASGUH(1.1f), KAPITEINGUH(0.95f), RINGENRACEGUH(1.0f),
        // </einde2_cast_team>
        // <balto>
        // </balto>
        // einde2, cast_volk (CONTRACT_E2 11): the masters, rulers and townsfolk of the Guhvatar story
        // <einde2_cast_volk>
        GUHATSO(1.0f), PAKGUH(1.0f), BUMI_GUH(1.0f), AARDEKONING_GUH(1.0f), ARNOOK_GUH(1.1f), PIANDAO_GUH(1.0f), WERKTUIGGUH(1.0f),
        ROKU_GUH(1.15f), ROKU_GUH_BEELD(1.15f), KYOSHI_GUH(1.25f), KYOSHI_GUH_BEELD(1.25f),
        JOO_DEE_GUH(1.0f), KOOLMAN(1.0f), LOKETGUH(1.0f), KEI_GUH(1.3f), AARDROMMEL_OMROEPER(0.9f), FANGUH(0.9f), LUCHTNON_GUH(1.0f), POSTGUH(1.0f),
        PROFESSOR_ZEI_GUH(1.0f), KYOSHI_DORPSOUDSTE(0.9f), KYOSHI_KRIJGSTERGUH(1.0f),
        WATERSTAM_GUH(1.0f), WATERSTAM_WACHTER(1.1f), BA_SING_GUH_BURGER(1.0f), OMASHGUH_BURGER(1.0f), KYOSHI_DORPSGUH(1.0f), GEVANGENE_GUH(1.0f),
        ZONNEKRIJGER_GUH(1.05f), VUURWIJZE_MIKA(1.0f), VUURWIJZE_SHYU(1.0f), VUURNATIE_BURGER_MIKA(1.0f), VUURNATIE_JUF(1.0f), VUURNATIE_LEERLING(0.7f),
        TONEELSPELER_GUH(1.0f), TONEELSPELER_GUH_KATARA(1.0f), TONEELSPELER_GUH_SOKKA(1.0f), TONEELSPELER_GUH_TOPH(1.3f), TONEELSPELER_GUH_ZUKA(1.0f),
        // </einde2_cast_volk>
        // <mewtwo>
        // </mewtwo>
        // einde2, cast_schurk (CONTRACT_E2 11): the Mika's of the Guhvatar story (they only shove); its creatures are entity types of their own
        // <einde2_cast_schurk>
        VUURHEER_MIKAI(1.6f), VUURHEER_MIKAI_KOMEET(1.6f), VUURHEER_MIKAI_VADSIG(1.25f), AZULA_MIKA(1.0f), TY_LEE_MIKA(0.9f), ZHAO_MIKA(1.05f),
        LONG_FENG_MIKA(1.05f), DAI_LI_MIKA(1.0f), VUURNATIE_MIKA(1.0f), GEVANGENIS_DIRECTEUR_MIKA(1.1f), MIKA_KOK(1.0f), VESTINGKAPITEIN_MIKA(1.15f),
        // </einde2_cast_schurk>
        // einde2, geestmodellen: the Theegeest of the Geestenwereld, and the Bestelguh of tech tier 5 (its model; phase C gives it its work)
        // <einde2_geest>
        THEEGEEST_GUH(1.0f), BESTELGUH(1.0f),
        // (skelet B, CONTRACT_E2_B 7.7: the echoes of the White Lotus masters, Guru Pathik and earlier Guhvatars; geest_modellen gives them their looks)
        JEONG_JEONG_GUH_ECHO(1.0f), OOM_MIROH_ECHO(1.0f), BUMI_GUH_ECHO(1.0f), PAKGUH_ECHO(1.0f), PIANDAO_GUH_ECHO(1.0f),
        LOTUS_STICHTER_ECHO(1.05f), LOTUS_LID_ECHO(1.0f), GURU_PATHIK_GUH(0.9f), KURUK_GUH(1.1f), YANGCHEN_GUH(1.0f), SZETO_GUH(1.0f),
        // </einde2_geest>
        // einde2 phase B, h1_lucht (CONTRACT_E2_B 1.2): the kinds of chapter 1 (only between its own two markers, after the lines there)
        // <einde2_h1_lucht>
        // </einde2_h1_lucht>
        // einde2 phase B, h2_water: the kinds of chapter 2
        // <einde2_h2_water>
        // </einde2_h2_water>
        // einde2 phase B, h3_aarde: the kinds of chapter 3
        // <einde2_h3_aarde>
        // </einde2_h3_aarde>
        // einde2 phase B, h4_vuur: the kinds of chapter 4 and the finale (the Sintelstrand theatre included)
        // <einde2_h4_vuur>
        // </einde2_h4_vuur>
        // einde2 phase B, zij: the kinds of the side places
        // <einde2_zij>
        // </einde2_zij>
        // einde2 phase C, trap5_machines / trap5_ballon: the kinds of tech tier 5
        // <einde2_trap5>
        // </einde2_trap5>
        // einde2 Sponsguh, sp_cast: the kinds of the Sponsguh story (the Mikas of De Ruige Kroeg ...); the cast itself are SponsguhWezens
        // <einde2_sp_cast>
        // (skelet C, CONTRACT_E2_C 7.2: the Mikas of De Ruige Kroeg; sp_cast gives them their looks)
        KROEG_BARMAN_MIKA(1.0f), KROEG_UITSMIJTER_MIKA(1.25f), STOERE_MIKA(1.1f), SLAPENDE_MIKA(1.05f),
        STOERE_MIKA_2(1.1f),                 // (sp_cast: a second stoere Mika: a denim vest, a cap, an eye patch)
        // </einde2_sp_cast>
        // <hemel>
        // </hemel>
        // <guhwaii>
        // </guhwaii>
        // <guhwaiispellen>
        // </guhwaiispellen>
        // guhpixel: one block per slice; a slice adds its kinds ONLY between its own two markers (names N_..., one per line, each ending in a comma)
        // <px_lobby>
        LOBBY_WELKOMSTGUH(1.0f),
        LOBBY_VERKOPER_GUH(1.0f),
        LOBBY_CHATGUH(0.8f),
        INTERNETCAFE_BEHEERDER(1.0f),
        INTERNETCAFE_SLAPER(0.85f),
        // </px_lobby>
        // <px_grap1>
        /** Guhpixel: the Skyblok-guh (a deadly serious pro with a headset) at the lobby anchor SPEL_SKYBLOK. */
        SKYBLOK_GUH(1.0f),
        /** Guhpixel: the Bedwars-guh (armour made of pillows) at the lobby anchor SPEL_BEDWARS. */
        BEDWARS_GUH(1.0f),
        /** Guhpixel: the Vadsnite-guh (a parachute backpack) at the lobby anchor SPEL_VADSNITE. */
        VADSNITE_GUH(1.0f),
        // </px_grap1>
        // <px_grap2>
        GUHMON_GYMLEIDER(1.05f),
        BZG_PRESENTATRICE(1.0f),
        BZG_BOER(1.1f),
        // </px_grap2>
        // <px_among>
        /** Among Guhs: the Kapitein-guh of De Vadsvaarder (the queue of the real game). */
        AMONG_KAPITEIN(1.0f),
        /** Among Guhs: the Logboek-guh beside the Kapitein: your personal numbers. */
        AMONG_LOGBOEKGUH(1.0f),
        // </px_among>
        // <px_guhkade>
        // </px_guhkade>
        // <px_kantoor>
        // </px_kantoor>
        // <px_bioscoop>
        // </px_bioscoop>
        // <px_reisbureau>
        REISBUREAU_AGENT(1.0f),
        // </px_reisbureau>
        // <px_parkour>
        // </px_parkour>
        // biomes3: one block per slice; a slice adds its kinds only between its own two lines
        // <bio_bouw_dal>
        WEEB_EVIVADS(1.12f),
        WEEB_NIELSVADS(1.2f),
        DAL_THEEGUH(0.95f),
        // </bio_bouw_dal>
        // <bio_bouw_meer>
        /** biomes3: the visser-guh on the jetty of a botenhuisje (koivoer, the roeibootje, the lessons of the lake). */
        BOTENHUISJE_VISSERGUH(1.0f),
        /** biomes3: the hanami guhs of a picknickeilandje: a plain guh and a bloesemguh, awake and asleep. */
        HANAMI_GUH(0.85f),
        HANAMI_GUH_SLAAPT(0.85f),
        HANAMI_BLOESEMGUH(0.9f),
        HANAMI_BLOESEMGUH_SLAAPT(0.9f),
        // </bio_bouw_meer>
        // <bio_bouw_wolk1>
        STERRENWACHT_RUINE_STERRENKIJKER(1.0f),
        BALLONVAARDERGUH(1.0f),
        // </bio_bouw_wolk1>
        // <bio_bouw_wolk2>
        SMIDGUH(1.0f),
        // </bio_bouw_wolk2>
        // <park>
        /** 1.4.2: the parkwachter-guh in his hut in Het Knabbelpark, and the kooi-verzorger-guh at the hertenkooi (feature/park). */
        PARKWACHTERGUH(1.0f),
        KOOIVERZORGERGUH(0.95f),
        // </park>
        // <park-a>
        /** 1.4.2: de kraampjesguh at the picknick kraampje of Het Knabbelpark: picnic treats and playground pieces for vads (feature/park/plek). */
        KRAAMGUH(1.0f),
        // </park-a>
        // <park-b>
        /** 1.4.2 park-plekjes b (feature/park/plek): Oma-guh feeding the Duifguhtjes, the sulking Mika on his bench, the guhkindjes at the poppenkast. */
        OMAGUH(1.0f),
        MOKMIKA(0.9f),
        GUHKINDJE(0.6f),
        // </park-b>
        // <safari-lobby>
        /** 1.6.0 Op Safari! (feature/safari/lobby): the lobby's guhs in ranger/work uniform and the eight zone experts. */
        SAFARI_HOOFDRANGER(1.05f),
        SAFARI_FOTOGRAAF(1.0f),
        SAFARI_VERZORGER(1.0f),
        SAFARI_VERKOPER(1.0f),
        SAFARI_LODGEBEHEERDER(1.0f),
        SAFARI_EXPERT_SAVANNE(1.0f),
        SAFARI_EXPERT_DELTA(1.0f),
        SAFARI_EXPERT_REGENWOUD(1.0f),
        SAFARI_EXPERT_BAMBOE(1.0f),
        SAFARI_EXPERT_IJSKUST(1.0f),
        SAFARI_EXPERT_OERTIJD(1.0f),
        SAFARI_EXPERT_EUROPA(1.0f),
        SAFARI_EXPERT_AUSTRALIE(1.0f),
        /** 1.6.0 round 4 (L4): the ninth expert, of the Kriebelhuis (lab coat, magnifying glass). */
        SAFARI_EXPERT_KRIEBELHUIS(1.0f),
        /** 1.6.0 test round 3 (J3): the ijsverkoper of the ijskraam and the storyteller at the evening campfire. */
        SAFARI_IJSVERKOPER(1.0f),
        SAFARI_VERTELLER(1.0f),
        /** 1.6.0 round 4 (L2): the Kriebelhuis' ticket guh and its shop guh. */
        SAFARI_KRIEBELHUIS_BALIE(1.0f),
        SAFARI_KRIEBELHUIS_WINKEL(1.0f),
        /** 1.6.0 (G1): the Aussie guhs of Australia: in the hammock, at the outback post office, at the camp. */
        SAFARI_AUSSIE_HANGMAT(1.0f),
        SAFARI_AUSSIE_POST(1.0f),
        SAFARI_AUSSIE_KAMP(1.0f),
        /** 1.6.0 F1b: Chauffeur Toeter of the Guh-safaribus at the busstations in the Guhmension. */
        SAFARI_CHAUFFEUR(1.0f),
        // </safari-lobby>
        // <beverbeek>
        /** 1.6.0 deel 2 (F10a): the ranger-guh at the ranger post in the Beverbeek (feature/beverbeek). */
        BEVERBEEK_RANGER(1.0f),
        // </beverbeek>
        // 1.7.0 Onder de Wol: the hosts of the three cave minigames and Snuffie the snuffelguh (feature/grotspellen)
        // <minigames1>
        SCHATGRAAFGUH(1.0f),
        GLIMWORMGUH(1.0f),
        LAMPJESGUH(1.0f),
        SNUFFELHULPJE(0.55f),
        // </minigames1>
        // <minigames2>
        /** 1.7.0 Onder de Wol (feature/mijnspellen): Conducteur Piepwiel of the Wisselgrot and De Guhde of the Kegelgrot. */
        WISSELGUH(1.0f),
        KEGELGUH(1.05f),
        // </minigames2>
        // <droomwezens>
        /** 1.8.0 Guhdromenland (feature/droomwezens): Snoes the zwerfguh (and after his story: blij), the Zandguh, the Wekkerguh,
         * the Rommelguh, Oma-guh with her knitting and a kussengevecht-guh. Their talk: DroomNpc.praat. */
        SNOES(0.95f),
        SNOES_BLIJ(0.95f),
        ZANDGUH(1.0f),
        WEKKERGUH(1.0f),
        ROMMELGUH(1.05f),
        DROOM_OMAGUH(1.0f),
        KUSSENGEVECHT_GUH(0.95f),
        // </droomwezens>
        // <zingendeweide>
        /** 1.8.0 De Zingende Weide (feature/zingendeweide): Badmeester Bubbels of the bath hut at the Bubbelsap-poeltjes. */
        BUBBELBADGUH(1.0f),
        // </zingendeweide>
        // <guhpixelspel>
        /** 1.8.0 Guhpixel Gezelschapshoek (feature/guhpixel/gezel): the Kussenbaas (Kussengevecht), the Spelletjesguh (Wie is
         * het) and the Rijtjesguh (Guh op een rij), on the lobby anchors SPEL_KUSSEN, SPEL_WIEISHET and SPEL_OPEENRIJ. */
        KUSSENBAAS_GUH(1.0f),
        WIEISHET_GUH(1.0f),
        OPEENRIJ_GUH(1.0f),
        // </guhpixelspel>
        ;

        public final float scale;

        Kind(float scale) {
            this.scale = scale;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    private static final EntityDataAccessor<Integer> DATA_KIND = SynchedEntityData.defineId(GuhNpcEntity.class, EntityDataSerializers.INT);
    /** einde2 (CONTRACT_E2 11.7 M3): the named animation that plays now ("" = idle) and a counter that restarts the same one. */
    private static final EntityDataAccessor<String> DATA_SPEEL = SynchedEntityData.defineId(GuhNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> DATA_SPEEL_N = SynchedEntityData.defineId(GuhNpcEntity.class, EntityDataSerializers.INT);
    private static final String ANIMATIE = "animation.guh_sitting.";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop(ANIMATIE + "idle");
    /**
     * einde2 (CONTRACT_E2 11.7 M3): per kind, how long each named animation of its file lasts in ticks (0 = a pose that lasts
     * until {@link #speel speel("")}; below 0 = the file does not hold it). Filled for the einde2 cast from its three tables
     * (feature/einde2/CastTabellen); a kind without an entry plays nothing on demand.
     */
    public static final java.util.Map<Kind, java.util.function.ToIntFunction<String>> ANIMATIE_DUUR = new java.util.concurrent.ConcurrentHashMap<>();
    /** Server: the tick count at which the one-shot that plays now is over (-1: none, or a lasting pose). */
    private int speelTot = -1;
    /** Client: the last {@link #DATA_SPEEL_N} the controller started. */
    private int speelGezien;
    /** Client: the cutscene animation ({@code Cutscenes.animatie}) the controller saw last ("" = none). */
    private String sceneGezien = "";
    /** Client: the age of that animation the controller saw last (it goes back when the same name starts again). */
    private int sceneTicks;
    /** Client: a new scene animation began; the controller starts it from its first frame. */
    private boolean sceneOpnieuw;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    /** The Maagenzym-guh belongs to one stomach. */
    @Nullable
    private UUID maagOwner;
    /** Saved state for the character's feature role (see nl.juiced.guhs.feature.NpcRole). */
    public net.minecraft.nbt.CompoundTag roleData = new net.minecraft.nbt.CompoundTag();

    /** A Reisguh's own name (the waypoint's name), empty = just "Reisguh". 1.2.0: a Component (a place name of a template
     *  or an automatic name is translatable, a player's own name a literal; quest/Reisguh.vanOud). */
    private Component reisName = Component.empty();

    public Component getReisName() {
        return reisName;
    }

    public void setReisName(Component name) {
        this.reisName = name;
        if (getKind() == Kind.REISGUH && !nl.juiced.guhs.taal.Tekst.empty(name)) {
            this.setCustomName(name.copy());
        }
    }

    /** Verstopguhtje's game of verstopguh. */
    public final nl.juiced.guhs.quest.VerstopGame verstop = new nl.juiced.guhs.quest.VerstopGame();

    public GuhNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 100.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KIND, Kind.TANDARTS.ordinal());
        builder.define(DATA_SPEEL, "");
        builder.define(DATA_SPEEL_N, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 10.0f));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    public Kind getKind() {
        int i = this.entityData.get(DATA_KIND);
        return i >= 0 && i < Kind.values().length ? Kind.values()[i] : Kind.TANDARTS;
    }

    public void setKind(Kind kind) {
        this.entityData.set(DATA_KIND, kind.ordinal());
        this.getAttribute(Attributes.SCALE).setBaseValue(kind.scale);
        this.refreshDimensions();
        this.setCustomName(Component.translatable("entity.guhs.guh_npc." + kind.id()));
    }

    /**
     * einde2 (CONTRACT_E2 11.7 M3): plays {@code animation.guh_sitting.<naam>} of the kind's animation file once on every
     * client (synced) and goes back to idle; a pose ({@code (houding)}, 0 ticks in the table) lasts until {@code speel("")}.
     * A name the kind's file does not hold does nothing. Server side only. hook fase B: the cutscenes and roles call this.
     */
    public void speel(String naam) {
        if (this.level().isClientSide()) {
            return;
        }
        if (naam == null || naam.isEmpty()) {
            this.entityData.set(DATA_SPEEL, "");
            speelTot = -1;
            return;
        }
        int duur = duur(naam);
        if (duur < 0) {
            return;
        }
        this.entityData.set(DATA_SPEEL, naam);
        this.entityData.set(DATA_SPEEL_N, this.entityData.get(DATA_SPEEL_N) + 1);
        speelTot = duur == 0 ? -1 : this.tickCount + duur;
    }

    /** The named animation that plays now ("" = idle). */
    public String speelt() {
        return this.entityData.get(DATA_SPEEL);
    }

    /** How long this kind's animation lasts in ticks: 0 = a lasting pose, below 0 = its file does not hold it. */
    public int duur(String naam) {
        java.util.function.ToIntFunction<String> tabel = ANIMATIE_DUUR.get(getKind());
        return tabel == null ? -1 : tabel.applyAsInt(naam);
    }

    @Nullable
    public UUID getMaagOwner() {
        return maagOwner;
    }

    public void setMaagOwner(@Nullable UUID owner) {
        this.maagOwner = owner;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (!this.level().isClientSide() && player instanceof ServerPlayer serverPlayer) {
            nl.juiced.guhs.feature.NpcRole role = nl.juiced.guhs.feature.Features.role(getKind());
            if (role != null) {
                role.talk(this, serverPlayer);
            } else if (getKind() == Kind.REISGUH && player.isSecondaryUseActive()) {
                nl.juiced.guhs.quest.Reisguh.pickUp(this, serverPlayer);
            } else if (getKind() == Kind.REISGUH) {
                nl.juiced.guhs.quest.Reisguh.talk(this, serverPlayer);
            } else if (getKind() == Kind.POORTWACHTER) {
                nl.juiced.guhs.quest.KasteelPoort.talk(this, serverPlayer);
            } else if (getKind() == Kind.TIPGUH) {
                nl.juiced.guhs.quest.VerstopGame.askTip(this, serverPlayer);
            } else if (getKind() == Kind.VERSTOPGUHTJE) {
                nl.juiced.guhs.quest.VerstopGame.talk(this, serverPlayer);
            } else if (getKind() == Kind.KERMIS_GUH) {
                GuhQuests.say(serverPlayer, this, "quest.guhs.kermis.hello");
                openShop(player);
            } else {
                GuhQuests.talkTo(this, serverPlayer);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("Kind", getKind().id());
        if (maagOwner != null) {
            tag.store("MaagOwner", UUIDUtil.CODEC, maagOwner);
        }
        if (getKind() == Kind.VERSTOPGUHTJE) {
            tag.store("Verstop", CompoundTag.CODEC, verstop.save());
        }
        if (!nl.juiced.guhs.taal.Tekst.empty(reisName)) {
            nl.juiced.guhs.taal.Tekst.put(tag, "ReisName", reisName);
        }
        if (!roleData.isEmpty()) {
            tag.store("RoleData", CompoundTag.CODEC, roleData);
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput tag) {
        super.readAdditionalSaveData(tag);
        for (Kind kind : Kind.values()) {
            if (kind.id().equals(tag.getStringOr("Kind", ""))) {
                setKind(kind);
            }
        }
        maagOwner = tag.read("MaagOwner", UUIDUtil.CODEC).isPresent() ? tag.read("MaagOwner", UUIDUtil.CODEC).orElseThrow() : null;
        verstop.load(tag.read("Verstop", CompoundTag.CODEC).orElseGet(CompoundTag::new));
        setReisName(nl.juiced.guhs.quest.Reisguh.vanOud(nl.juiced.guhs.taal.Tekst.get(tag, "ReisName")));
        roleData = tag.read("RoleData", CompoundTag.CODEC).orElseGet(CompoundTag::new);
        if (getKind() == Kind.POORTWACHTER && !roleData.contains("GateYaw")) {
            roleData.putFloat("GateYaw", getYRot()); // (the Rotation from a structure is already loaded here)
        }
    }

    // --- a quest character: can't be hurt, pushed or leashed, never despawns ---

    @Override
    public boolean isInvulnerableTo(net.minecraft.server.level.ServerLevel serverLevel, DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    // --- the Kermis-guh's stall -------------------------------------------------------------------------------------

    @Nullable
    private Player tradingPlayer;
    @Nullable
    private net.minecraft.world.item.trading.MerchantOffers offers;

    /** The kermis outfit, for kermisbonnen (one per lap of the coaster); never sold out. */
    @Override
    public net.minecraft.world.item.trading.MerchantOffers getOffers() {
        nl.juiced.guhs.feature.NpcRole role = nl.juiced.guhs.feature.Features.role(getKind());
        if (offers == null && role != null) {
            offers = role.offers(this);
        }
        if (offers == null && getKind() == Kind.VERSTOPGUHTJE) {
            offers = new net.minecraft.world.item.trading.MerchantOffers();
            offers.add(offer(nl.juiced.guhs.registry.ModItems.VERSTOPGUHTICKET.get(), 2, GuhClothes.DETECTIVE_VERGROOTGLAS));
            offers.add(offer(nl.juiced.guhs.registry.ModItems.VERSTOPGUHTICKET.get(), 3, GuhClothes.DETECTIVE_PET));
            offers.add(offer(nl.juiced.guhs.registry.ModItems.VERSTOPGUHTICKET.get(), 5, GuhClothes.DETECTIVE_JAS));
        }
        if (offers == null) {
            offers = new net.minecraft.world.item.trading.MerchantOffers();
            offers.add(offer(2, GuhClothes.KERMIS_STRIK));
            offers.add(offer(4, GuhClothes.KERMIS_HOED));
            offers.add(offer(6, GuhClothes.KERMIS_JASJE));
            offers.add(new net.minecraft.world.item.trading.MerchantOffer(
                    new net.minecraft.world.item.trading.ItemCost(nl.juiced.guhs.registry.ModItems.KERMISBON.get(), 1),
                    new net.minecraft.world.item.ItemStack(nl.juiced.guhs.registry.ModItems.GUH_BALLON.get(), 2), Integer.MAX_VALUE, 0, 0));
        }
        return offers;
    }

    private static net.minecraft.world.item.trading.MerchantOffer offer(net.minecraft.world.item.Item currency, int price, GuhClothes clothes) {
        return new net.minecraft.world.item.trading.MerchantOffer(new net.minecraft.world.item.trading.ItemCost(currency, price),
                new net.minecraft.world.item.ItemStack(nl.juiced.guhs.registry.ModItems.clothingItem(clothes)), Integer.MAX_VALUE, 0, 0);
    }

    /**
     * Opens the shop for this player. 1.2.7: any number of players can shop with the same character at once (it used to
     * serve one customer at a time, with no time limit: one player with the screen open kept the shop shut for everybody
     * else). Every trading screen has its own counter (the payment and result slots are the menu's); the character only
     * has to know who its {@link #customers} are.
     */
    public void openShop(Player player) {
        if (this.level().isClientSide()) {
            return;
        }
        customers.add(player);
        this.tradingPlayer = player;                                // (the customer who came last)
        openTradingScreen(player, getDisplayName(), 1);
    }

    /** Everybody who has this character's shop open right now (pruned every tick). */
    private final java.util.Set<Player> customers = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());

    /** Is this player shopping here right now? */
    public boolean isCustomer(Player player) {
        return customers.contains(player);
    }

    private static net.minecraft.world.item.trading.MerchantOffer offer(int bonnen, GuhClothes clothes) {
        return new net.minecraft.world.item.trading.MerchantOffer(
                new net.minecraft.world.item.trading.ItemCost(nl.juiced.guhs.registry.ModItems.KERMISBON.get(), bonnen),
                new net.minecraft.world.item.ItemStack(nl.juiced.guhs.registry.ModItems.clothingItem(clothes)), Integer.MAX_VALUE, 0, 0);
    }

    /** (Vanilla calls this with null when a trading screen closes: that customer is pruned in {@link #tick}.) */
    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
        if (player != null) {
            customers.add(player);
        }
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return tradingPlayer;
    }

    @Override
    public void overrideOffers(net.minecraft.world.item.trading.MerchantOffers offers) {
        this.offers = offers;
    }

    @Override
    public void notifyTrade(net.minecraft.world.item.trading.MerchantOffer offer) {
        offer.increaseUses();
        this.playSound(nl.juiced.guhs.registry.ModSounds.GUH_AMBIENT.get(), 1f, 1.3f);
    }

    @Override
    public void notifyTradeUpdated(net.minecraft.world.item.ItemStack stack) {
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public net.minecraft.sounds.SoundEvent getNotifyTradeSound() {
        return nl.juiced.guhs.registry.ModSounds.GUH_AMBIENT.get();
    }

    /** 26.1: Merchant#stillValid is abstract now; 1.21.1's MerchantMenu checked just the trading player. */
    @Override
    public boolean stillValid(Player player) {
        return customers.contains(player) && this.isAlive() && player.level() == this.level() && player.distanceToSqr(this) <= 64;
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide();
    }

    @Override
    public void tick() {
        if (getKind() == Kind.POORTWACHTER && !roleData.contains("GateYaw")) {
            // which way the gate faces: as placed, before the guard turns its head to look at people
            roleData.putFloat("GateYaw", getYRot());
        }
        super.tick();
        if (!this.level().isClientSide() && speelTot >= 0 && this.tickCount >= speelTot) {
            speelTot = -1;                                           // (the one-shot is over: back to idle)
            this.entityData.set(DATA_SPEEL, "");
        }
        if (!this.level().isClientSide() && getKind() == Kind.VERSTOPGUHTJE) {
            verstop.tick(this);
        }
        if (!this.level().isClientSide() && getKind() == Kind.REISGUH) {
            nl.juiced.guhs.quest.Reisguh.tick(this);
        }
        if (!this.level().isClientSide()) {
            nl.juiced.guhs.feature.NpcRole role = nl.juiced.guhs.feature.Features.role(getKind());
            if (role != null) {
                role.tick(this);
            }
        }
        if (!this.level().isClientSide() && getKind() == Kind.POORTWACHTER) {
            nl.juiced.guhs.quest.KasteelPoort.tick(this);
        }
        if (!this.level().isClientSide() && getKind() == Kind.KERMIS_GUH) {
            nl.juiced.guhs.quest.Kermis.tick(this);   // 1.2.7: the station's finish line and sleds
        }
        if (!customers.isEmpty()) {
            customers.removeIf(p -> !p.isAlive() || p.isRemoved() || p.level() != this.level()
                    || !(p.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu) || p.distanceToSqr(this) > 64);
        }
        if (tradingPlayer != null && !customers.contains(tradingPlayer)) {
            tradingPlayer = customers.isEmpty() ? null : customers.iterator().next();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("main", 0, state -> {
            // a client-side cutscene actor never gets the synced speel(): a scene's Builder.animatie goes first
            String scene = sceneAnimatie();
            String naam = !scene.isEmpty() && duur(scene) >= 0 ? scene : speelt();
            if (sceneBegonnen() && !scene.isEmpty()) {
                state.controller().reset();                         // (a new Builder.animatie: from its start)
            }
            if (naam.isEmpty()) {
                return state.setAndContinue(IDLE);
            }
            int n = this.entityData.get(DATA_SPEEL_N);
            if (n != speelGezien) {
                speelGezien = n;
                state.controller().reset();                         // (the same name again: from the start)
            }
            return state.setAndContinue(duur(naam) == 0 ? RawAnimation.begin().thenPlayAndHold(ANIMATIE + naam)
                    : RawAnimation.begin().thenPlay(ANIMATIE + naam).thenLoop(ANIMATIE + "idle"));
        }));
    }

    /**
     * Client (einde2 integratie_b): the scene animation this actor plays ({@code Cutscenes.animatie}), held while the scene
     * waits for a click. {@code Cutscenes.animatie} answers "" while the scene stands still (1.4.4, every actor rests), and
     * read as it is, the actors dropped to idle at every line's end and every click started each actor's last one-shot again
     * from its first frame (a cheer, a bow or a jump played once per click). A GeckoLib animation cannot be frozen, so here it simply runs its
     * course during the wait (a one-shot ends in idle, a pose holds) and is never restarted by a click; only a new
     * {@code Builder.animatie} (another name, or the same name again: its age goes back) starts it from the top.
     */
    public String sceneAnimatie() {
        if (Cutscenes.gepauzeerd()) {
            return sceneGezien;
        }
        String scene = Cutscenes.animatie(this);
        int ticks = Cutscenes.animatieTicks(this);
        if (!scene.equals(sceneGezien) || (!scene.isEmpty() && ticks < sceneTicks)) {
            sceneGezien = scene;
            sceneOpnieuw = true;
        }
        sceneTicks = ticks;
        return scene;
    }

    /** Client: did a new scene animation begin since the last ask (once; the controller then starts it from its top)? */
    public boolean sceneBegonnen() {
        boolean b = sceneOpnieuw;
        sceneOpnieuw = false;
        return b;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
