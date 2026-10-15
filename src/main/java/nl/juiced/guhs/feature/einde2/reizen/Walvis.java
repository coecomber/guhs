package nl.juiced.guhs.feature.einde2.reizen;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.einde2.biomes.E2BiomesFeature;
import nl.juiced.guhs.feature.einde2.zwevers.E2ZweversFeature;
import nl.juiced.guhs.feature.einde2.zwevers.LeegtewalvisguhEntity;
import nl.juiced.guhs.feature.guheinde.GuheindeFeature;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.storage.Nbt;
import nl.juiced.guhs.world.GuhTime;

/**
 * einde2 phase B (DESIGN §3 "Leegtewalvisguh", CONTRACT_E2_B 7.10), slice reizen: a player's OWN Leegtewalvisguh, tamed at
 * the end of the captain's questline ({@link Leegtewalvis}). One per player, never lost (as Appa): what makes a whale yours
 * lives in your data ({@value #KEY}: Heeft, Gen, Delen, Vlag, the two chests, the day the garden was picked); the entity
 * carries a copy and an older copy steps aside ({@link #meld}).
 * <ul>
 *   <li>the horn ({@link WalvishoornItem}): it comes to you (from wherever it hangs: a whale far away or in an unloaded part
 *       of the void is made again near you and flies in); sneak + horn: the furniture screen;</li>
 *   <li>stand on its back with the horn in your hand: it goes where you look, slowly (look up or down: it climbs or sinks);
 *       let go of the horn and it hangs still;</li>
 *   <li>its back, furnished from a fixed set ({@link #DELEN}, as the vetbootje is customised: parts of its model, no real
 *       blocks on a moving creature): a little house, two chests that really keep items, a bed (a nap = a little rest), a
 *       garden (two knabbelkoorvruchten a day), lanterns, a flag in one of {@link #VLAGGEN} colours.</li>
 * </ul>
 * Who believes sees it (the flag of {@code Walviszicht}); only its owner opens its chests, steers it and picks its garden.
 */
public final class Walvis {
    /** The player's own whale. */
    public static final String KEY = "guhs_einde2_reizen_walvis";
    /** The parts of the back, in bit order (= the model's bones {@code inr_<deel>}, einde2_zwevers_modellen.py). */
    public static final List<String> DELEN = List.of("huisje", "kisten", "bed", "tuin", "lantaarns", "vlag");
    public static final int VLAGGEN = 6;
    public static final int KIST_VAKKEN = 27;
    /** Where the parts you can click stand on the back, in model pixels (= blocks: x left, y up from its feet, z towards its tail). */
    static final AABB KIST_1 = new AABB(2.4, 12, -8.1, 3.6, 13.0, -6.9), KIST_2 = new AABB(3.7, 12, -8.1, 4.9, 13.0, -6.9),
            BED = new AABB(-4.6, 12, -4.1, -3.4, 12.8, -1.9), TUIN = new AABB(-6.1, 12, 6.3, -2.9, 13.2, 8.7),
            HUISJE = new AABB(-2.6, 12, 1.9, 2.6, 18.2, 7.1);
    /** How far (blocks) a click on the back reaches. */
    static final double REIK = 6.0;
    /** A called whale that is further away than this (or not loaded) is made again near the player. */
    static final double OPNIEUW = 320;
    /** A nap on the bed: at most once in this many ticks. */
    static final int DUTJE = 20 * 120;

    private static final Map<UUID, LeegtewalvisguhEntity> GELADEN = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> GESLAPEN = new ConcurrentHashMap<>();

    private Walvis() {
    }

    static void register() {
        LeegtewalvisguhEntity.MELD = Walvis::meld;
        LeegtewalvisguhEntity.STUURT = Walvis::stuurt;
        LeegtewalvisguhEntity.RUG_KLIK = Walvis::rugKlik;
    }

    // --- the saved state ---------------------------------------------------------------------------------------------------------

    private static CompoundTag staat(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        if (!saved.contains(KEY)) {
            saved.put(KEY, new CompoundTag());
        }
        return saved.getCompoundOrEmpty(KEY);
    }

    public static boolean heeft(ServerPlayer p) {
        return GuhQuests.saved(p).getCompoundOrEmpty(KEY).getBooleanOr("Heeft", false);
    }

    static int gen(ServerPlayer p) {
        return GuhQuests.saved(p).getCompoundOrEmpty(KEY).getIntOr("Gen", 0);
    }

    public static int delen(ServerPlayer p) {
        return GuhQuests.saved(p).getCompoundOrEmpty(KEY).getIntOr("Delen", 0);
    }

    public static int vlag(ServerPlayer p) {
        return Math.floorMod(GuhQuests.saved(p).getCompoundOrEmpty(KEY).getIntOr("Vlag", 0), VLAGGEN);
    }

    // --- the loaded whale --------------------------------------------------------------------------------------------------------

    /** Every own whale, about once a second: an older copy, or one whose owner has none any more, goes; the current one wears the owner's furniture. */
    static void meld(LeegtewalvisguhEntity w) {
        ServerPlayer baas = w.baas();
        if (baas == null) {
            return;                                               // (offline: we can't know; nothing of theirs can be taken out of it)
        }
        if (!heeft(baas) || w.gen() != gen(baas)) {
            w.discard();
            return;
        }
        LeegtewalvisguhEntity oud = GELADEN.put(baas.getUUID(), w);
        if (oud != null && oud != w && !oud.isRemoved()) {
            oud.discard();
        }
        w.zetInrichting(delen(baas), vlag(baas));
    }

    /** This player's own whale when it is loaded, or null. */
    @Nullable
    public static LeegtewalvisguhEntity geladen(ServerPlayer p) {
        LeegtewalvisguhEntity w = GELADEN.get(p.getUUID());
        if (w == null || w.isRemoved() || !w.isAlive() || !p.getUUID().equals(w.eigenaar()) || w.gen() != gen(p)) {
            return null;
        }
        return w;
    }

    /** This whale becomes this player's own (the end of the questline). False: it is somebody else's already. */
    static boolean maakEigen(ServerPlayer p, LeegtewalvisguhEntity w) {
        if (w.eigenaar() != null && !w.eigenaar().equals(p.getUUID())) {
            return false;
        }
        LeegtewalvisguhEntity oud = geladen(p);
        if (oud != null && oud != w) {
            oud.discard();
        }
        CompoundTag staat = staat(p);
        staat.putBoolean("Heeft", true);
        staat.putInt("Gen", gen(p) + 1);
        if (!staat.contains("Delen")) {
            staat.putInt("Delen", 1 << DELEN.indexOf("vlag"));
        }
        w.zetEigenaar(p.getUUID(), gen(p));
        w.zetInrichting(delen(p), vlag(p));
        w.roep(null);
        GELADEN.put(p.getUUID(), w);
        return true;
    }

    /**
     * The horn: the own whale comes to a spot in the void beside the player, at a height where its back is about at the
     * player's feet. Far away or not loaded: it is made again a little way off and flies in. False: no whale,
     * or not in the Guheinde.
     */
    static boolean roep(ServerPlayer p) {
        if (!heeft(p) || !(p.level() instanceof ServerLevel level) || !GuheindeFeature.isGuheinde(level)) {
            return false;
        }
        Vec3 doel = roepPlek(level, p);
        LeegtewalvisguhEntity w = geladen(p);
        if (w != null && (w.level() != level || w.distanceToSqr(p) > OPNIEUW * OPNIEUW)) {
            w.discard();
            w = null;
        }
        if (w == null) {
            // made again a little way off, where the world is awake (it flies in), else right at the spot
            Vec3 achter = doel.add(LeegtewalvisguhEntity.vooruit(p.getYRot()).scale(-64));
            if (!level.isPositionEntityTicking(net.minecraft.core.BlockPos.containing(achter))) {
                achter = doel;
            }
            w = E2ZweversFeature.LEEGTEWALVISGUH.get().create(level, EntitySpawnReason.TRIGGERED);
            if (w == null) {
                return false;
            }
            float koers = p.getYRot();
            w.snapTo(achter.x, Mth.clamp(doel.y, LeegtewalvisguhEntity.MIN_Y, LeegtewalvisguhEntity.MAX_Y), achter.z, koers, 0);
            w.setYBodyRot(koers);
            w.setYHeadRot(koers);
            CompoundTag staat = staat(p);
            staat.putInt("Gen", gen(p) + 1);
            w.zetEigenaar(p.getUUID(), gen(p));
            w.zetInrichting(delen(p), vlag(p));
            level.addFreshEntity(w);
            GELADEN.put(p.getUUID(), w);
        }
        w.roep(doel);
        return true;
    }

    /** A free spot in the void beside the player (the whale's middle), its back at about the player's feet. */
    static Vec3 roepPlek(ServerLevel level, ServerPlayer p) {
        double y = Mth.clamp(p.getY() - LeegtewalvisguhEntity.RUG - 0.5, LeegtewalvisguhEntity.MIN_Y, LeegtewalvisguhEntity.MAX_Y);
        Vec3 voor = LeegtewalvisguhEntity.vooruit(p.getYRot());
        Vec3 opzij = new Vec3(-voor.z, 0, voor.x);
        for (int afstand : new int[]{26, 40, 56, 72}) {
            for (Vec3 richting : new Vec3[]{voor, opzij, opzij.scale(-1), voor.scale(-1)}) {
                Vec3 plek = new Vec3(p.getX() + richting.x * afstand, y, p.getZ() + richting.z * afstand);
                if (vrij(level, plek)) {
                    return plek;
                }
            }
        }
        return new Vec3(p.getX() + voor.x * 40, y, p.getZ() + voor.z * 40);
    }

    /** Nothing as high as its belly under its middle or its sides (the void, or islands far below). */
    static boolean vrij(ServerLevel level, Vec3 plek) {
        for (int[] k : new int[][]{{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            int x = Mth.floor(plek.x + k[0] * LeegtewalvisguhEntity.HALF_BREED), z = Mth.floor(plek.z + k[1] * LeegtewalvisguhEntity.HALF_BREED);
            if (!level.hasChunkAt(new net.minecraft.core.BlockPos(x, (int) plek.y, z))
                    || level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z) > plek.y - LeegtewalvisguhEntity.ONDER) {
                return false;
            }
        }
        return true;
    }

    /** Does its owner steer it: standing on its back with the horn in a hand, not sneaking? */
    static boolean stuurt(LeegtewalvisguhEntity w, ServerPlayer p) {
        return p.getUUID().equals(w.eigenaar()) && !p.isShiftKeyDown() && w.staatOp(p)
                && (p.getMainHandItem().is(E2ReizenFeature.WALVISHOORN.get()) || p.getOffhandItem().is(E2ReizenFeature.WALVISHOORN.get()));
    }

    // --- the furniture -----------------------------------------------------------------------------------------------------------

    public static boolean heeftDeel(int delen, String deel) {
        int i = DELEN.indexOf(deel);
        return i >= 0 && (delen & (1 << i)) != 0;
    }

    /** Puts a part on its back or takes it off (the screen); the chests keep their items either way. */
    static void zetDeel(ServerPlayer p, String deel, boolean aan) {
        int i = DELEN.indexOf(deel);
        if (i < 0 || !heeft(p)) {
            return;
        }
        int delen = aan ? delen(p) | (1 << i) : delen(p) & ~(1 << i);
        staat(p).putInt("Delen", delen);
        if (aan) {
            GuhAdvancements.grant(p, "einde2_reizen_ingericht");
        }
        LeegtewalvisguhEntity w = geladen(p);
        if (w != null) {
            w.zetInrichting(delen, vlag(p));
        }
    }

    static void zetVlag(ServerPlayer p, int kleur) {
        if (!heeft(p)) {
            return;
        }
        staat(p).putInt("Vlag", Math.floorMod(kleur, VLAGGEN));
        LeegtewalvisguhEntity w = geladen(p);
        if (w != null) {
            w.zetInrichting(delen(p), vlag(p));
        }
    }

    /** Opens the furniture screen. */
    static void openInrichting(ServerPlayer p) {
        CompoundTag data = new CompoundTag();
        data.putInt("Delen", delen(p));
        data.putInt("Vlag", vlag(p));
        ModNetworking.sendTo(p, new E2ReizenPayloads.Inrichting(data));
    }

    /** A click on an own whale's back: the chests (owner), the bed (anybody who sees it), the garden (owner), the house. */
    static InteractionResult rugKlik(LeegtewalvisguhEntity w, Player speler) {
        if (!(speler instanceof ServerPlayer p) || w.eigenaar() == null) {
            return InteractionResult.PASS;
        }
        String deel = geraakt(w, p);
        if (deel == null) {
            return InteractionResult.PASS;
        }
        boolean baas = p.getUUID().equals(w.eigenaar());
        ServerPlayer eigenaar = w.baas();
        switch (deel) {
            case "kist_1", "kist_2" -> {
                if (!baas || eigenaar == null) {
                    zeg(p, "gui.guhs.einde2_reizen.walvis.kist_niet");
                } else {
                    openKist(p, w, deel.equals("kist_1") ? "Kist1" : "Kist2");
                }
            }
            case "bed" -> dutje(p);
            case "tuin" -> {
                if (!baas) {
                    zeg(p, "gui.guhs.einde2_reizen.walvis.tuin_niet");
                } else {
                    oogst(p);
                }
            }
            default -> zeg(p, "gui.guhs.einde2_reizen.walvis.huisje");
        }
        return InteractionResult.SUCCESS;
    }

    /** Which clickable part the player looks at on this whale's back (null: none within reach). */
    @Nullable
    static String geraakt(LeegtewalvisguhEntity w, Player p) {
        int delen = w.inrichting();
        Vec3 oog = naarModel(w, p.getEyePosition());
        Vec3 eind = naarModel(w, p.getEyePosition().add(p.getViewVector(1f).scale(REIK)));
        String beste = null;
        double besteAfstand = Double.MAX_VALUE;
        Object[][] doelen = {{"kist_1", KIST_1, "kisten"}, {"kist_2", KIST_2, "kisten"}, {"bed", BED, "bed"}, {"tuin", TUIN, "tuin"},
                {"huisje", HUISJE, "huisje"}};
        for (Object[] d : doelen) {
            if (!heeftDeel(delen, (String) d[2])) {
                continue;
            }
            Optional<Vec3> raak = ((AABB) d[1]).inflate(0.3).clip(oog, eind);
            if (raak.isPresent() && raak.get().distanceTo(oog) < besteAfstand) {
                besteAfstand = raak.get().distanceTo(oog);
                beste = (String) d[0];
            }
        }
        return beste;
    }

    /** A spot in the world in the whale's model coordinates (x to its left, y up from its feet, z towards its tail). */
    static Vec3 naarModel(LeegtewalvisguhEntity w, Vec3 wereld) {
        Vec3 l = wereld.subtract(w.position()).yRot(w.getYRot() * Mth.DEG_TO_RAD);
        return new Vec3(l.x, l.y, -l.z);
    }

    /** A spot of the model (x left, y up, z towards the tail) in the world. */
    static Vec3 naarWereld(LeegtewalvisguhEntity w, Vec3 model) {
        return w.position().add(new Vec3(model.x, model.y, -model.z).yRot(-w.getYRot() * Mth.DEG_TO_RAD));
    }

    private static void openKist(ServerPlayer p, LeegtewalvisguhEntity w, String welke) {
        SimpleContainer kist = new SimpleContainer(KIST_VAKKEN) {
            @Override
            public void setChanged() {
                super.setChanged();
                schrijf(p, welke, this);
            }

            @Override
            public boolean stillValid(Player player) {
                return !w.isRemoved() && player.distanceToSqr(w.getX(), w.rugY(), w.getZ()) < 30 * 30;
            }
        };
        lees(p, welke, kist);
        p.openMenu(new SimpleMenuProvider((id, inventory, speler) -> ChestMenu.threeRows(id, inventory, kist),
                Component.translatable("gui.guhs.einde2_reizen.walvis.kist", welke.endsWith("1") ? 1 : 2)));
        p.level().playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 0.6f, 1.0f);
    }

    static void lees(ServerPlayer p, String welke, SimpleContainer kist) {
        kist.clearContent();
        CompoundTag t = staat(p).getCompoundOrEmpty(welke);
        ContainerHelper.loadAllItems(Nbt.input(p.registryAccess(), t), kist.getItems());
    }

    static void schrijf(ServerPlayer p, String welke, SimpleContainer kist) {
        staat(p).put(welke, Nbt.write(p.registryAccess(), out -> ContainerHelper.saveAllItems(out, kist.getItems())));
    }

    /** (tests) what lies in one of the chests. */
    static List<ItemStack> kist(ServerPlayer p, String welke) {
        SimpleContainer kist = new SimpleContainer(KIST_VAKKEN);
        lees(p, welke, kist);
        return kist.getItems().stream().map(ItemStack::copy).toList();
    }

    private static void dutje(ServerPlayer p) {
        long nu = p.level().getGameTime();
        Long laatst = GESLAPEN.get(p.getUUID());
        if (laatst != null && nu - laatst < DUTJE) {
            zeg(p, "gui.guhs.einde2_reizen.walvis.wakker");
            return;
        }
        GESLAPEN.put(p.getUUID(), nu);
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 12, 0));
        p.addEffect(new MobEffectInstance(MobEffects.SATURATION, 20 * 2, 0));
        zeg(p, "gui.guhs.einde2_reizen.walvis.dutje");
    }

    private static void oogst(ServerPlayer p) {
        long dag = GuhTime.day((ServerLevel) p.level());
        CompoundTag staat = staat(p);
        if (staat.getLongOr("TuinDag", Long.MIN_VALUE) == dag) {
            zeg(p, "gui.guhs.einde2_reizen.walvis.tuin_morgen");
            return;
        }
        staat.putLong("TuinDag", dag);
        Minigames.give(p, new ItemStack(E2BiomesFeature.KNABBELKOORVRUCHT.get(), 2));
        zeg(p, "gui.guhs.einde2_reizen.walvis.tuin");
    }

    private static void zeg(ServerPlayer p, String key) {
        p.sendOverlayMessage(Component.translatable(key).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    static void vergeet(ServerPlayer p) {
        GELADEN.remove(p.getUUID());
        GESLAPEN.remove(p.getUUID());
    }

    static void leeg() {
        GELADEN.clear();
        GESLAPEN.clear();
    }

    /** Hand helper for the horn. */
    static boolean inHand(ServerPlayer p, InteractionHand hand) {
        return p.getItemInHand(hand).is(E2ReizenFeature.WALVISHOORN.get());
    }
}
