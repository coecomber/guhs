package nl.juiced.guhs.feature.theehuis;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.knus.Feesttaak;
import nl.juiced.guhs.feature.knus.GuhHooks;
import nl.juiced.guhs.feature.knus.KnusTags;
import nl.juiced.guhs.feature.knus.KnusVoortgang;
import nl.juiced.guhs.feature.knus.Knusfeest;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.registry.ModBlocks;
import nl.juiced.guhs.registry.ModItems;
import nl.juiced.guhs.registry.ModSounds;

/**
 * Het theekransje in the Knabbelthee-huisje (one per Mevrouw Theelepel at a time; state in memory). Your tamed guhs
 * (up to {@value #MAX_GASTEN}, the ones within {@value #GASTEN_BEREIK} blocks) walk in and sit down on the guh_stoelen
 * around the theetafels. They chat (emotes, hearts, happy guh sounds: a little gezelligheid every time), and now and
 * then one of them wants tea or something sweet (a bubble above its head). Right-click it with a cup of tea
 * ({@link TheeBlocks.Thee}; the special teas count more) or a cake (#guhs:knus/gebak from the bakery counts most: home
 * baked!). {@value #DOEL} gezelligheid within the time: a gezellige tafel - the effect guhs:gezellig for you and your
 * guhs, Knus milestones, and while the feesttaakje THEESERVIES is open Mevrouw Theelepel lends you her feest_theeservies.
 * Nobody ever loses anything: a wish nobody served just floats away.
 */
public final class Theekransje {
    public enum Wens { GEEN, THEE, GEBAK }

    /** Actions from Mevrouw Theelepel's screen. */
    public static final int START = 0, STOP = 1;
    public static final int REACH = 14, GASTEN_BEREIK = 24, MAX_GASTEN = 6;
    public static final int KRANSJE_TICKS = 20 * 180, LOOP_TICKS = 80, DOEL = 100, WENS_TICKS = 20 * 25, GEZELLIG_TICKS = 20 * 60 * 5;
    public static final int PUNT_THEE = 10, PUNT_BIJZONDER = 15, PUNT_GEBAK = 12, PUNT_ZELFGEBAKKEN = 20, PUNT_PRAATJE = 1;
    /** Cups of knabbelthee "van het huis" when you come without tea. */
    public static final int HUISTHEE = 4;
    static final String EERSTE = "guhs_theehuis_eerste";
    private static final Emote[] PRAATJES = {Emote.ZWAAIEN, Emote.VERLEGEN, Emote.KNUFFELEN, Emote.VAHOEG, Emote.ZINGEN, Emote.SMAKKEN};

    /** A guest at the table. */
    static final class Gast {
        final UUID guh;
        final BlockPos stoel;
        final boolean zatAl;
        boolean zit;
        Wens wens = Wens.GEEN;
        int wensTimer, rust;

        Gast(UUID guh, BlockPos stoel, boolean zatAl, int rust) {
            this.guh = guh;
            this.stoel = stoel;
            this.zatAl = zatAl;
            this.rust = rust;
        }
    }

    private static final Map<UUID, Theekransje> KRANSJES = new HashMap<>();
    private static final Map<UUID, Long> GASTHEREN = new ConcurrentHashMap<>();

    public static Theekransje of(GuhNpcEntity npc) {
        return KRANSJES.computeIfAbsent(npc.getUUID(), id -> new Theekransje(npc.getUUID()));
    }

    /** Is this player holding a theekransje right now? (Counts as a game: one at a time.) */
    public static boolean isGastheer(Player player) {
        return GASTHEREN.containsKey(player.getUUID());
    }

    private final UUID theelepel;
    final List<BlockPos> tafels = new ArrayList<>();
    final List<BlockPos> stoelen = new ArrayList<>();
    @Nullable
    private ResourceKey<Level> dim;
    @Nullable
    private Vec3 centre;
    private long lastScan = Long.MIN_VALUE / 2;

    @Nullable
    private UUID gastheer;
    final List<Gast> gasten = new ArrayList<>();
    int timer, ticks, gezelligheid, zelfgebakken;

    private Theekransje(UUID theelepel) {
        this.theelepel = theelepel;
    }

    public boolean isBezig() {
        return gastheer != null;
    }

    public int gezelligheid() {
        return gezelligheid;
    }

    @Nullable
    public UUID gastheer() {
        return gastheer;
    }

    // =================================================================================================================
    // talking
    // =================================================================================================================

    public static void talk(GuhNpcEntity npc, ServerPlayer player) {
        Theekransje k = of(npc);
        GuhAdvancements.grant(player, "theehuis_theelepel");
        boolean mine = player.getUUID().equals(k.gastheer);
        npc.level().playSound(null, npc, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 1f, 1.05f);
        GuhQuests.say(player, npc, mine ? "quest.guhs.theeguh.bezig_jij" : k.isBezig() ? "quest.guhs.theeguh.bezig"
                : "quest.guhs.theeguh.hoi." + npc.getRandom().nextInt(3));
        CompoundTag data = new CompoundTag();
        data.putBoolean("Running", k.isBezig());
        data.putBoolean("Mine", mine);
        ServerPlayer host = k.gastheerIn((ServerLevel) npc.level());
        data.putString("Gastheer", host == null ? "?" : host.getGameProfile().getName());
        data.putInt("Gezelligheid", k.gezelligheid);
        data.putInt("Doel", DOEL);
        data.putInt("Gasten", eigenGuhs((ServerLevel) npc.level(), player, npc.position()).size());
        data.putBoolean("Feest", Knusfeest.open(player, Feesttaak.THEESERVIES));
        data.putInt("Kransjes", KnusVoortgang.teller(player, TheehuisVoortgang.KRANSJES));
        data.putInt("Soorten", KnusVoortgang.ontdekt(player, TheehuisVoortgang.THEESOORTEN).size());
        ModNetworking.sendTo(player, new TheehuisPayloads.Open(npc.getId(), data));
    }

    public static void action(GuhNpcEntity npc, ServerPlayer player, int action) {
        if (npc.getKind() != GuhNpcEntity.Kind.THEEGUH || player.distanceToSqr(npc) > 64) {
            return;
        }
        Theekransje k = of(npc);
        if (action == START) {
            k.start(npc, player);
        } else if (action == STOP && player.getUUID().equals(k.gastheer)) {
            GuhQuests.say(player, npc, "quest.guhs.theeguh.gestopt");
            k.klaar((ServerLevel) npc.level(), false);
        }
    }

    /** The player's own tamed guhs around a spot that could come to the table. */
    static List<GuhEntity> eigenGuhs(ServerLevel level, Player player, Vec3 at) {
        return level.getEntitiesOfClass(GuhEntity.class, new AABB(at, at).inflate(GASTEN_BEREIK),
                g -> g.isAlive() && g.isTame() && g.isOwnedBy(player) && !g.isPassenger() && !g.isVehicle() && !g.isLeashed());
    }

    // =================================================================================================================
    // the kransje
    // =================================================================================================================

    public boolean start(GuhNpcEntity npc, ServerPlayer player) {
        ServerLevel level = (ServerLevel) npc.level();
        if (isBezig()) {
            GuhQuests.say(player, npc, player.getUUID().equals(gastheer) ? "quest.guhs.theeguh.bezig_jij" : "quest.guhs.theeguh.bezig");
            return false;
        }
        if (isGastheer(player) || Minigames.refuse(player, npc, Minigames.THEEHUIS)) {
            return false;
        }
        scan(npc, level);
        if (tafels.isEmpty() || stoelen.isEmpty()) {
            GuhQuests.say(player, npc, "quest.guhs.theeguh.geen_tafel");
            return false;
        }
        List<GuhEntity> guhs = new ArrayList<>(eigenGuhs(level, player, npc.position()));
        if (guhs.isEmpty()) {
            GuhQuests.say(player, npc, "quest.guhs.theeguh.geen_guhs");
            return false;
        }
        guhs.sort(Comparator.comparingDouble(g -> g.distanceToSqr(centre)));
        List<BlockPos> vrij = new ArrayList<>(stoelen);
        gasten.clear();
        int n = Math.min(Math.min(MAX_GASTEN, vrij.size()), guhs.size());
        for (int i = 0; i < n; i++) {
            GuhEntity guh = guhs.get(i);
            vrij.sort(Comparator.comparingDouble(s -> guh.distanceToSqr(Vec3.atCenterOf(s))));
            BlockPos stoel = vrij.remove(0);
            gasten.add(new Gast(guh.getUUID(), stoel, guh.isOrderedToSit(), 60 + i * 70));
            guh.setOrderedToSit(false);
            guh.setInSittingPose(false);
            guh.getNavigation().moveTo(stoel.getX() + 0.5, stoel.getY() + 0.6, stoel.getZ() + 0.5, 1.1);
            GuhHooks.bezig(guh, 60);
        }
        gastheer = player.getUUID();
        timer = KRANSJE_TICKS;
        ticks = 0;
        gezelligheid = 0;
        zelfgebakken = 0;
        GASTHEREN.put(gastheer, level.getGameTime());
        Minigames.startKeeping(player);
        for (BlockPos t : tafels) {
            dek(level, t, true);
        }
        int thee = 0;
        for (ItemStack s : player.getInventory().items) {
            if (s.is(KnusTags.THEE) || s.getItem() instanceof TheeBlocks.Thee) {
                thee += s.getCount();
            }
        }
        GuhQuests.say(player, npc, "quest.guhs.theeguh.start", gasten.size());
        if (thee < HUISTHEE) {
            Minigames.give(player, new ItemStack(TheehuisFeature.thee(TheeBlocks.Soort.KNABBELTHEE), HUISTHEE));
            GuhQuests.say(player, npc, "quest.guhs.theeguh.huisthee", HUISTHEE);
        }
        level.playSound(null, npc, TheehuisFeature.KOPJES_KLINK.get(), SoundSource.NEUTRAL, 1f, 1f);
        sync(level);
        return true;
    }

    public void tick(GuhNpcEntity npc) {
        ServerLevel level = (ServerLevel) npc.level();
        ticks++;
        if (gastheer == null) {
            if (centre == null && level.getGameTime() - lastScan > 100
                    && !level.getEntitiesOfClass(ServerPlayer.class, npc.getBoundingBox().inflate(REACH + 16)).isEmpty()) {
                scan(npc, level);
            }
            return;
        }
        ServerPlayer host = gastheerIn(level);
        if (host == null || !host.isAlive() || host.level() != level || centre == null || !GASTHEREN.containsKey(gastheer)
                || host.position().distanceToSqr(centre) > (REACH + 20) * (REACH + 20)) {
            klaar(level, false);
            return;
        }
        GASTHEREN.put(gastheer, level.getGameTime());
        if (ticks % 20 == 0) {
            Minigames.keep(host);
        }
        boolean veranderd = false;
        boolean iedereenZit = true;
        for (Gast g : List.copyOf(gasten)) {
            if (!(level.getEntity(g.guh) instanceof GuhEntity guh) || !guh.isAlive() || guh.isPassenger()) {
                gasten.remove(g);
                veranderd = true;
                continue;
            }
            GuhHooks.bezig(guh, 40);
            if (!g.zit) {
                iedereenZit = false;
                double d = guh.distanceToSqr(Vec3.atBottomCenterOf(g.stoel));
                if (d < 1.6 || ticks >= LOOP_TICKS) {
                    zet(level, guh, g);
                } else if (ticks % 10 == 0) {
                    guh.getNavigation().moveTo(g.stoel.getX() + 0.5, g.stoel.getY() + 0.6, g.stoel.getZ() + 0.5, 1.1);
                }
                continue;
            }
            if (guh.distanceToSqr(Vec3.atBottomCenterOf(g.stoel).add(0, 0.56, 0)) > 0.8) {
                zet(level, guh, g);      // (pushed off its chair: back on it)
            }
            if (ticks % 20 == 0) {
                veranderd |= wensen(level, host, guh, g);
            }
        }
        if (gasten.isEmpty()) {
            klaar(level, false);
            return;
        }
        if (iedereenZit && ticks % 40 == 0) {
            praatje(level);
        }
        if (ticks % 20 == 0) {
            for (BlockPos t : tafels) {
                level.sendParticles(TheehuisFeature.THEESTOOM.get(), t.getX() + 0.5, t.getY() + 1.2, t.getZ() + 0.5, 1, 0.15, 0.05, 0.15, 0.005);
            }
            int s = (timer + 19) / 20;
            host.displayClientMessage(Component.translatable("gui.guhs.theehuis.bar", gezelligheid, DOEL, s)
                    .withStyle(s <= 20 ? ChatFormatting.GOLD : ChatFormatting.LIGHT_PURPLE), true);
        }
        if (veranderd || ticks % 100 == 0) {
            sync(level);
        }
        if (--timer <= 0) {
            klaar(level, false);
        }
    }

    /** A guest sits down on its chair, facing the table. */
    private void zet(ServerLevel level, GuhEntity guh, Gast g) {
        Vec3 at = Vec3.atBottomCenterOf(g.stoel).add(0, 0.5625, 0);
        float yaw = (float) Math.toDegrees(Math.atan2(-(centre.x - at.x), centre.z - at.z));
        guh.getNavigation().stop();
        guh.moveTo(at.x, at.y, at.z, yaw, 0);
        guh.setYHeadRot(yaw);
        guh.yBodyRot = yaw;
        guh.setDeltaMovement(Vec3.ZERO);
        guh.setOrderedToSit(true);
        guh.setInSittingPose(true);
        if (!g.zit) {
            g.zit = true;
            level.sendParticles(TheehuisFeature.GEZELLIG_HARTJE.get(), at.x, at.y + 0.9, at.z, 2, 0.2, 0.1, 0.2, 0.01);
        }
    }

    /** Wishes come and go (every second). True when a bubble changed. */
    private boolean wensen(ServerLevel level, ServerPlayer host, GuhEntity guh, Gast g) {
        if (g.wens == Wens.GEEN) {
            g.rust -= 20;
            if (g.rust <= 0) {
                g.wens = level.getRandom().nextInt(100) < 55 ? Wens.THEE : Wens.GEBAK;
                g.wensTimer = WENS_TICKS;
                level.playSound(null, guh, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.8f, 1.3f);
                return true;
            }
            return false;
        }
        g.wensTimer -= 20;
        if (g.wensTimer <= 0) {
            g.wens = Wens.GEEN;
            g.rust = 100 + level.getRandom().nextInt(100);
            level.playSound(null, guh, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.6f, 0.75f);
            host.displayClientMessage(Component.translatable("gui.guhs.theehuis.vergeten", guh.getDisplayName()).withStyle(ChatFormatting.GRAY), true);
            return true;
        }
        return false;
    }

    /** One of the guests says something to another: an emote, a happy sound, hearts. A little gezelligheid. */
    private void praatje(ServerLevel level) {
        List<GuhEntity> zitten = new ArrayList<>();
        for (Gast g : gasten) {
            if (g.zit && level.getEntity(g.guh) instanceof GuhEntity guh) {
                zitten.add(guh);
            }
        }
        if (zitten.isEmpty()) {
            return;
        }
        GuhEntity spreker = zitten.get(level.getRandom().nextInt(zitten.size()));
        GuhEntity luisteraar = zitten.size() > 1 ? zitten.stream().filter(g -> g != spreker).toList().get(level.getRandom().nextInt(zitten.size() - 1)) : null;
        if (spreker.emotes.current() == null) {
            spreker.emotes.start(PRAATJES[level.getRandom().nextInt(PRAATJES.length)], false, GuhEmotes.Source.SELF);
        }
        if (luisteraar != null) {
            spreker.getLookControl().setLookAt(luisteraar);
            luisteraar.getLookControl().setLookAt(spreker);
            Vec3 mid = spreker.position().add(luisteraar.position()).scale(0.5);
            level.sendParticles(TheehuisFeature.GEZELLIG_HARTJE.get(), mid.x, mid.y + 1.1, mid.z, 1, 0.1, 0.1, 0.1, 0.01);
        }
        level.playSound(null, spreker, ModSounds.GUH_AMBIENT.get(), SoundSource.NEUTRAL, 0.7f, 0.9f + level.getRandom().nextFloat() * 0.5f);
        gezelligheid += PUNT_PRAATJE;
        ServerPlayer host = gastheerIn(level);
        if (host != null && gezelligheid >= DOEL) {
            klaar(level, true);
        }
    }

    /** A right-click on a guh: pouring tea / serving cake to a guest (the host), a friendly word otherwise. */
    static InteractionResult klikOpGuh(GuhEntity guh, Player player, InteractionHand hand) {
        if (player.level().isClientSide) {
            Integer w = TheehuisPayloads.CLIENT_WENSEN.get(guh.getId());
            return w == null ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }
        if (hand != InteractionHand.MAIN_HAND) {
            Theekransje k = vanGast(guh);
            return k == null ? InteractionResult.PASS : InteractionResult.SUCCESS;
        }
        Theekransje k = vanGast(guh);
        if (k == null || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        ServerLevel level = (ServerLevel) guh.level();
        Gast g = k.gast(guh);
        if (!sp.getUUID().equals(k.gastheer)) {
            ServerPlayer host = k.gastheerIn(level);
            sp.displayClientMessage(Component.translatable("gui.guhs.theehuis.niet_jouw", host == null ? "?" : host.getGameProfile().getName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = player.getItemInHand(hand);
        boolean thee = stack.getItem() instanceof TheeBlocks.Thee || stack.is(KnusTags.THEE);
        boolean gebak = isGebak(stack);
        if (g == null || !g.zit || (!thee && !gebak)) {
            String wil = g == null || !g.zit ? "gui.guhs.theehuis.gaat_zitten" : g.wens == Wens.THEE ? "gui.guhs.theehuis.wil_thee"
                    : g.wens == Wens.GEBAK ? "gui.guhs.theehuis.wil_gebak" : "gui.guhs.theehuis.kletst";
            sp.displayClientMessage(Component.translatable(wil, guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return InteractionResult.SUCCESS;
        }
        if ((thee && g.wens != Wens.THEE) || (gebak && g.wens != Wens.GEBAK)) {
            sp.displayClientMessage(Component.translatable(g.wens == Wens.GEEN ? "gui.guhs.theehuis.heeft_al" : g.wens == Wens.THEE
                    ? "gui.guhs.theehuis.wil_thee" : "gui.guhs.theehuis.wil_gebak", guh.getDisplayName()).withStyle(ChatFormatting.LIGHT_PURPLE), true);
            return InteractionResult.SUCCESS;
        }
        k.serveer(level, sp, guh, g, stack, thee);
        return InteractionResult.SUCCESS;
    }

    /** Something sweet for the table: bakery cakes (#guhs:knus/gebak) and a few ordinary sweets. */
    public static boolean isGebak(ItemStack stack) {
        return stack.is(KnusTags.GEBAK) || stack.is(Items.COOKIE) || stack.is(Items.PUMPKIN_PIE) || stack.is(Items.CAKE) || stack.is(Items.SWEET_BERRIES)
                || stack.is(Items.HONEY_BOTTLE);
    }

    /** Points for a cup of tea (special teas more) or a cake (home baked more). */
    public static int punten(boolean thee, boolean bijzonder) {
        return thee ? (bijzonder ? PUNT_BIJZONDER : PUNT_THEE) : (bijzonder ? PUNT_ZELFGEBAKKEN : PUNT_GEBAK);
    }

    void serveer(ServerLevel level, ServerPlayer host, GuhEntity guh, Gast g, ItemStack stack, boolean thee) {
        boolean bijzonder;
        if (thee) {
            TheeBlocks.Soort soort = stack.getItem() instanceof TheeBlocks.Thee t ? t.soort : TheeBlocks.Soort.KNABBELTHEE;
            bijzonder = soort.bijzonder();
            KnusVoortgang.ontdek(host, TheehuisVoortgang.THEESOORTEN, soort.id());
            KnusVoortgang.hoogste(host, TheehuisVoortgang.SOORTEN, KnusVoortgang.ontdekt(host, TheehuisVoortgang.THEESOORTEN).size());
            KnusVoortgang.tel(host, TheehuisVoortgang.INGESCHONKEN, 1);
            level.playSound(null, guh, TheehuisFeature.INSCHENKEN.get(), SoundSource.NEUTRAL, 1f, 1.1f);
            level.sendParticles(TheehuisFeature.THEESTOOM.get(), guh.getX(), guh.getY() + 1.0, guh.getZ(), 4, 0.15, 0.1, 0.15, 0.01);
        } else {
            bijzonder = stack.is(KnusTags.GEBAK);
            if (bijzonder) {
                zelfgebakken++;
                KnusVoortgang.tel(host, TheehuisVoortgang.ZELFGEBAKKEN, 1);
            }
            level.playSound(null, guh, SoundEvents.GENERIC_EAT, SoundSource.NEUTRAL, 0.8f, 1.3f);
        }
        if (!host.getAbilities().instabuild) {
            stack.shrink(1);
        }
        int erbij = punten(thee, bijzonder);
        gezelligheid += erbij;
        g.wens = Wens.GEEN;
        g.rust = 120 + level.getRandom().nextInt(160);
        level.playSound(null, guh, TheehuisFeature.KOPJES_KLINK.get(), SoundSource.NEUTRAL, 0.7f, 1.0f + level.getRandom().nextFloat() * 0.2f);
        level.playSound(null, guh, ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 0.8f, 1.2f);
        TheeBlocks.hartjes(level, guh.getX(), guh.getY() + 1.0, guh.getZ(), 3);
        guh.emotes.start(thee ? Emote.VAHOEG : Emote.SMAKKEN, false, GuhEmotes.Source.SELF);
        host.displayClientMessage(Component.translatable(bijzonder ? (thee ? "gui.guhs.theehuis.lekker_bijzonder" : "gui.guhs.theehuis.lekker_zelfgebakken")
                : "gui.guhs.theehuis.lekker", guh.getDisplayName(), erbij).withStyle(ChatFormatting.GOLD), true);
        sync(level);
        if (gezelligheid >= DOEL) {
            klaar(level, true);
        }
    }

    /**
     * The end of the kransje. Gezellig: the effect for the host and every guest, the milestones, the theemutsje the first
     * time, and the feest_theeservies while the Burgemeester's feesttaakje THEESERVIES is open.
     */
    void klaar(ServerLevel level, boolean gezellig) {
        ServerPlayer host = gastheerIn(level);
        GuhNpcEntity npc = level.getEntity(theelepel) instanceof GuhNpcEntity n ? n : null;
        for (Gast g : gasten) {
            if (level.getEntity(g.guh) instanceof GuhEntity guh) {
                guh.setOrderedToSit(g.zatAl);
                guh.setInSittingPose(g.zatAl);
                GuhHooks.bezig(guh, 0);
                if (gezellig) {
                    guh.addEffect(new MobEffectInstance(TheehuisFeature.GEZELLIG, GEZELLIG_TICKS));
                    TheeBlocks.hartjes(level, guh.getX(), guh.getY() + 1.0, guh.getZ(), 4);
                }
            }
        }
        if (host != null) {
            if (gezellig) {
                host.addEffect(new MobEffectInstance(TheehuisFeature.GEZELLIG, GEZELLIG_TICKS));
                KnusVoortgang.tel(host, TheehuisVoortgang.KRANSJES, 1);
                GuhAdvancements.grant(host, "theehuis_gezellig");
                TheehuisVoortgang.toon(host, "theehuis_gezellig");
                if (zelfgebakken > 0) {
                    GuhAdvancements.grant(host, "theehuis_zelfgebakken");
                    TheehuisVoortgang.toon(host, "theehuis_zelfgebakken");
                }
                CompoundTag saved = GuhQuests.saved(host);
                if (npc != null) {
                    GuhQuests.say(host, npc, "quest.guhs.theeguh.gezellig");
                }
                if (!saved.getBoolean(EERSTE)) {
                    saved.putBoolean(EERSTE, true);
                    Minigames.give(host, new ItemStack(ModItems.clothingItem(GuhClothes.THEEMUTSJE)));
                    if (npc != null) {
                        GuhQuests.say(host, npc, "quest.guhs.theeguh.theemutsje");
                    }
                }
                if (Knusfeest.open(host, Feesttaak.THEESERVIES)) {
                    // a host who still carries a feest_theeservies (e.g. from an earlier round) gets no second one,
                    // but the task still counts as gemaakt
                    if (GuhQuests.count(host, TheehuisFeature.FEEST_THEESERVIES.get()) == 0) {
                        Minigames.give(host, new ItemStack(TheehuisFeature.FEEST_THEESERVIES.get()));
                    }
                    Knusfeest.gemaakt(host, Feesttaak.THEESERVIES);
                    if (npc != null) {
                        GuhQuests.say(host, npc, "quest.guhs.theeguh.theeservies");
                    }
                }
                level.playSound(null, host.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.5f);
                if (centre != null) {
                    level.sendParticles(TheehuisFeature.GEZELLIG_HARTJE.get(), centre.x, centre.y + 1.4, centre.z, 20, 1.5, 0.5, 1.5, 0.02);
                }
            } else if (npc != null && gezelligheid > 0) {
                GuhQuests.say(host, npc, "quest.guhs.theeguh.bijna", gezelligheid, DOEL);
            }
            GASTHEREN.remove(host.getUUID());
        }
        for (BlockPos t : tafels) {
            dek(level, t, false);
        }
        if (gastheer != null) {
            GASTHEREN.remove(gastheer);
        }
        gastheer = null;
        gasten.clear();
        timer = 0;
        sync(level);
    }

    // =================================================================================================================
    // the room
    // =================================================================================================================

    /** Finds the tea tables around Mevrouw Theelepel and the guh_stoelen next to them. */
    void scan(GuhNpcEntity npc, ServerLevel level) {
        lastScan = level.getGameTime();
        if (centre != null && dim == level.dimension()) {
            return;
        }
        tafels.clear();
        stoelen.clear();
        BlockPos c = npc.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(c.offset(-REACH, -3, -REACH), c.offset(REACH, 6, REACH))) {
            if (level.getBlockState(pos).is(TheehuisFeature.THEETAFEL.get())) {
                tafels.add(pos.immutable());
            }
        }
        if (tafels.isEmpty()) {
            return;
        }
        double sx = 0, sy = 0, sz = 0;
        for (BlockPos t : tafels) {
            sx += t.getX() + 0.5;
            sy += t.getY();
            sz += t.getZ() + 0.5;
            for (BlockPos pos : BlockPos.betweenClosed(t.offset(-2, -1, -2), t.offset(2, 1, 2))) {
                if (level.getBlockState(pos).is(ModBlocks.GUH_STOEL.get()) && !stoelen.contains(pos) && level.getBlockState(pos.above()).isAir()) {
                    stoelen.add(pos.immutable());
                }
            }
        }
        dim = level.dimension();
        centre = new Vec3(sx / tafels.size(), sy / tafels.size(), sz / tafels.size());
        for (BlockPos t : tafels) {
            dek(level, t, false);   // (after a restart mid-kransje)
        }
    }

    private static void dek(ServerLevel level, BlockPos pos, boolean gedekt) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof TheeBlocks.Theetafel && state.getValue(TheeBlocks.Theetafel.GEDEKT) != gedekt) {
            level.setBlock(pos, state.setValue(TheeBlocks.Theetafel.GEDEKT, gedekt), Block.UPDATE_CLIENTS);
        }
    }

    /** Tells the players around which guests want what (the bubbles above their heads). */
    void sync(ServerLevel level) {
        List<Integer> data = new ArrayList<>();
        for (Gast g : gasten) {
            if (level.getEntity(g.guh) instanceof GuhEntity guh) {
                data.add(guh.getId());
                data.add(g.wens.ordinal());
            }
        }
        Vec3 at = centre == null ? Vec3.ZERO : centre;
        TheehuisPayloads.Wensen payload = new TheehuisPayloads.Wensen(theelepelId(level), data);
        for (ServerPlayer p : level.players()) {
            if (p.position().distanceToSqr(at) < 64 * 64) {
                ModNetworking.sendTo(p, payload);
            }
        }
    }

    private int theelepelId(ServerLevel level) {
        return level.getEntity(theelepel) instanceof GuhNpcEntity n ? n.getId() : -1;
    }

    @Nullable
    Gast gast(GuhEntity guh) {
        for (Gast g : gasten) {
            if (g.guh.equals(guh.getUUID())) {
                return g;
            }
        }
        return null;
    }

    @Nullable
    static Theekransje vanGast(GuhEntity guh) {
        for (Theekransje k : KRANSJES.values()) {
            if (k.isBezig() && k.gast(guh) != null) {
                return k;
            }
        }
        return null;
    }

    @Nullable
    private ServerPlayer gastheerIn(ServerLevel level) {
        return gastheer == null ? null : level.getServer().getPlayerList().getPlayer(gastheer);
    }

    @Nullable
    public Vec3 centre() {
        return centre;
    }

    // --- events ----------------------------------------------------------------------------------------------------------

    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || player.tickCount % 20 != 0) {
            return;
        }
        Long seen = GASTHEREN.get(player.getUUID());
        if (seen != null && player.level().getGameTime() - seen > 40) {
            GASTHEREN.remove(player.getUUID());
        }
    }

    public static void onLogout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        GASTHEREN.remove(event.getEntity().getUUID());
    }

    public static void onChangeDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) {
        GASTHEREN.remove(event.getEntity().getUUID());
    }

    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        KRANSJES.clear();
        GASTHEREN.clear();
    }

    /** (Tests) forget a Theelepel's kransje. */
    static void vergeet(GuhNpcEntity npc) {
        Theekransje k = KRANSJES.remove(npc.getUUID());
        if (k != null && k.gastheer != null) {
            GASTHEREN.remove(k.gastheer);
        }
    }

    static void gezelligheidVoorTest(Theekransje k, int waarde) {
        k.gezelligheid = waarde;
    }
}
