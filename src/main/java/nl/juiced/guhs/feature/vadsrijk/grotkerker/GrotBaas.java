package nl.juiced.guhs.feature.vadsrijk.grotkerker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.KerkerData.Kamer;
import nl.juiced.guhs.feature.vadsrijk.grotkerker.KerkerData.Kerker;
import nl.juiced.guhs.feature.vadsrijk.kern.Element;
import nl.juiced.guhs.feature.vadsrijk.kern.Levels;
import nl.juiced.guhs.feature.vadsrijk.kern.baas.VadsrijkBaas;
import nl.juiced.guhs.feature.vadsrijk.kern.loot.LootTabel;
import nl.juiced.guhs.feature.vadsrijk.vfx.Aanval;
import nl.juiced.guhs.feature.vadsrijk.vfx.BaasVfx;
import nl.juiced.guhs.feature.vadsrijk.vfx.Deeltje;
import nl.juiced.guhs.feature.vadsrijk.vfx.Vfx;

/**
 * U-GROTKERKER: a cave's end boss. A kern {@link VadsrijkBaas} in "loose" mode (no registered arena: the run manager
 * {@link GrotKerker} spawns, scales and resets it): the boss bar shows for everyone near it, the fight is the boss room
 * ({@link #inGevecht}: only players in the room can hurt it / are chased), HP scales with the players in the cave when the boss
 * woke up (+75 % per extra player, kern's rule), and kern's contribution rule (more than 5 %) gives a personal end chest
 * ({@link KerkerLoot}). Its moves: melee + its own rotation of VFX-kit attacks ({@link GrotBazen.Def#aanvallen}: decal
 * telegraphs, cast bars, the signature mechanic from phase 2), and it calls two cave creatures for help at half health.
 */
public class GrotBaas extends VadsrijkBaas implements GeoEntity {
    static final String[] ANIMS = {"aanval", "spreuk", "spuug", "klap", "hef", "uithaal", "schelp", "speciaal"};
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    /** The player count the run fixed for the HP scaling (0 = count live). */
    int vasteSpelers;
    private long volgendeAanval = -1;
    private final Map<String, Long> klaarOp = new HashMap<>();
    private int leegT, terugT;
    private boolean ingesteldNiveau;

    public GrotBaas(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Nullable
    public GrotBazen.Def def() {
        return GrotBazen.van(getType());
    }

    public String grot() {
        GrotBazen.Def d = def();
        return d == null ? "zwamgrot" : d.grot();
    }

    @Nullable
    Kerker kerker() {
        return Kerkers.van(grot());
    }

    @Nullable
    Kamer kamer() {
        Kerker k = kerker();
        return k == null ? null : k.baas();
    }

    // ---- numbers (a cave boss: about 14 cave creatures of its level, a bit harder hitting) ---------------------------------------

    @Override
    protected double basisVads(int level) {
        return 40 * (1 + 0.3 * (level - 1)) * 14;
    }

    @Override
    protected double basisSchade(int level) {
        return 9 * (1 + 0.22 * (level - 1)) * 1.4;
    }

    @Override
    public int xp() {
        int l = Math.max(1, Math.min(Levels.MAX - 1, mobLevel()));
        Kerker k = kerker();
        return (int) Math.max(150, Math.round(Levels.xpVoor(l) * xpFactor(k == null ? 1 : k.zone())));
    }

    /**
     * 1101-balans: the boss's share of a level (was 0.15), so a solo run (7 pack mobs + elite + boss) gives ~0.8 level of the boss
     * level in every dungeon: zones 1-2 0.50 (Zwamgrot 791 -> ~1,400 xp), zones 3-6 0.63 (their mobs give stap/55, less per kill).
     */
    public static double xpFactor(int zone) {
        return zone <= 2 ? 0.50 : 0.63;
    }

    @Nullable
    @Override
    public String lootTabel() {
        return null;
    }

    @Override
    public Element standaardElement() {
        GrotBazen.Def d = def();
        return d == null ? Element.WATER : d.element();
    }

    @Override
    protected double[] fases() {
        return new double[] {0.5};
    }

    @Override
    public String titelKey() {
        return "vadsrijk.grotkerker.baas." + grot();
    }

    @Nullable
    @Override
    public net.minecraft.resources.Identifier muziek() {
        GrotBazen.Def d = def();
        String t = d == null ? "algemeen" : d.thema();
        if (!nl.juiced.guhs.feature.vadsrijk.geluid.GeluidPlan.BAAS_THEMAS.contains(t)) {
            t = "algemeen";   // (the fallback theme)
        }
        return Guhs.id(nl.juiced.guhs.feature.vadsrijk.geluid.GeluidPlan.baasId(t));
    }

    // ---- the fight area: the boss room ---------------------------------------------------------------------------------------

    @Override
    public int losseStraal() {
        Kamer k = kamer();
        return k == null ? 18 : k.r() + 8;
    }

    @Override
    public Vec3 gevechtsMidden() {
        Kamer k = kamer();
        return k == null ? super.gevechtsMidden() : Vec3.atBottomCenterOf(k.pos());
    }

    @Override
    public double gevechtsStraal() {
        Kamer k = kamer();
        return k == null ? super.gevechtsStraal() : k.r();
    }

    @Override
    public boolean inGevecht(Player p) {
        Kamer k = kamer();
        if (k == null) {
            return super.inGevecht(p);
        }
        return !p.isSpectator() && k.binnen(p.getX(), p.getY(), p.getZ(), 3);
    }

    @Override
    protected Vec3 kistMidden() {
        return gevechtsMidden();
    }

    @Override
    public void schaal(int spelers) {
        super.schaal(vasteSpelers > 0 ? vasteSpelers : spelers);
    }

    // ---- moves ---------------------------------------------------------------------------------------------------------------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || !(level() instanceof ServerLevel sl) || geplofd()) {
            return;
        }
        if (!ingesteldNiveau) {
            ingesteldNiveau = true;
            if (getCustomName() == null) {
                setCustomName(Component.translatable(titelKey()));
            }
        }
        if (tickCount % 10 != 0) {
            return;
        }
        List<ServerPlayer> vechters = vechters();
        long nu = sl.getGameTime();
        // leash: back to the middle of the boss room when it got dragged out
        Vec3 m = gevechtsMidden();
        double dx = getX() - m.x, dz = getZ() - m.z;
        terugT = dx * dx + dz * dz > Math.pow(gevechtsStraal() + 2, 2) && !inSprong() ? terugT + 10 : 0;
        if (terugT >= 40) {
            terugT = 0;
            springNaar(m, 20, 5);
        }
        // an empty boss room for 30 s: the fight resets (full HP, contributions forgotten)
        leegT = vechters.isEmpty() ? leegT + 10 : 0;
        if (leegT >= 600 && vads() < maxVads()) {
            leegT = 0;
            BaasVfx.stop(this, sl);
            heal(maxVads());
            bijdragen.wis();
            Vfx.deeltjes(sl, Deeltje.PLUIS, position().add(0, getBbHeight() * 0.5, 0), 30, 0xFFC0E0, getBbWidth() * 0.6, 0.1);
        }
        // the own rotation of VFX-kit attacks (the gametest server too: the run's tests drive it themselves)
        if (vechters.isEmpty() || inIntro() || BaasVfx.loopt(this)) {
            return;
        }
        if (volgendeAanval < 0) {
            volgendeAanval = nu + 80;
        }
        if (nu < volgendeAanval) {
            return;
        }
        GrotBazen.Def d = def();
        if (d == null) {
            return;
        }
        List<Aanval> klaar = new ArrayList<>();
        for (Aanval a : d.aanvallen()) {
            if (fase() >= minFase(a) && nu >= klaarOp.getOrDefault(a.id(), 0L)) {
                klaar.add(a);
            }
        }
        if (klaar.isEmpty()) {
            volgendeAanval = nu + 40;
            return;
        }
        // the signature move (the phase-2 one) first when it is ready
        Aanval a = klaar.get(klaar.size() - 1);
        if (minFase(a) < 2) {
            a = klaar.get(getRandom().nextInt(klaar.size()));
        }
        int duur = BaasVfx.start(this, sl, a);
        klaarOp.put(a.id(), nu + wacht(a));
        volgendeAanval = nu + duur + 60 + getRandom().nextInt(60);
    }

    private static final java.lang.reflect.Field MIN_FASE = veld("minFase"), WACHT = veld("wacht");

    @Nullable
    private static java.lang.reflect.Field veld(String naam) {
        try {
            java.lang.reflect.Field f = Aanval.class.getDeclaredField(naam);
            f.setAccessible(true);
            return f;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** (Aanval keeps its phase gate + cooldown package-private in vfx: read them here) */
    static int minFase(Aanval a) {
        try {
            return MIN_FASE == null ? 1 : MIN_FASE.getInt(a);
        } catch (ReflectiveOperationException e) {
            return 1;
        }
    }

    static int wacht(Aanval a) {
        try {
            return WACHT == null ? 400 : WACHT.getInt(a);
        } catch (ReflectiveOperationException e) {
            return 400;
        }
    }

    /** Heals a fraction of max Vads with a green swirl (an un-interrupted heal cast). */
    static void heel(VadsrijkBaas b, ServerLevel sl, double fractie) {
        b.heal(b.maxVads() * fractie);
        Vfx.deeltjes(sl, Deeltje.PLUIS, b.position().add(0, b.getBbHeight() * 0.5, 0), 30, 0x8AF0A0, b.getBbWidth() * 0.6, 0.12);
        Vfx.zuil(sl, b.position(), b.getBbHeight() + 4, 0x8AF0A0, 30);
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(b) > 40 * 40) {
                continue;
            }
            p.sendOverlayMessage(Component.translatable("vadsrijk.vfx.geheeld", Component.translatable(b.titelKey())).withStyle(ChatFormatting.RED));
        }
    }

    @Override
    protected void opFase(int fase) {
        if (fase == 2 && level() instanceof ServerLevel sl) {
            GrotKerker.hulp(sl, this);
        }
    }

    @Override
    public void aanval(LivingEntity doel) {
        GrotBazen.Def d = def();
        if (d != null) {
            triggerAnim("actie", d.aanval());
        }
        super.aanval(doel);
    }

    // ---- the end -------------------------------------------------------------------------------------------------------------

    @Override
    protected LootTabel.Resultaat kistInhoud(ServerPlayer p, boolean eersteKill) {
        return KerkerLoot.kist(this, p);
    }

    @Override
    protected void beloon(ServerLevel sl, Set<ServerPlayer> helpers) {
        java.util.Set<java.util.UUID> kist = new java.util.HashSet<>();
        for (java.util.UUID id : bijdragen.spelers()) {
            if (bijdragen.verdientKist(id)) {
                kist.add(id);
            }
        }
        super.beloon(sl, helpers);
        GrotKerker.baasVerslagen(sl, this, helpers, kist);
    }

    @Override
    protected String killSleutel() {
        return "grotbaas:" + grot();
    }

    // ---- GeckoLib ------------------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<GrotBaas>("beweeg", 4, state -> {
            GrotBaas b = state.animatable();
            GrotBazen.Def d = b.def();
            if (b.geplofd()) {
                return state.setAndContinue(RawAnimation.begin().thenLoop("blij"));
            }
            return state.setAndContinue(RawAnimation.begin().thenLoop(state.isMoving() && d != null ? d.loop() : "idle"));
        }));
        AnimationController<GrotBaas> actie = new AnimationController<>("actie", 2, state -> PlayState.STOP);
        for (String a : ANIMS) {
            actie.triggerableAnim(a, RawAnimation.begin().thenPlay(a));
        }
        controllers.add(actie);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
