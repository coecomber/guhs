package nl.juiced.guhs.feature.band;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.entity.GuhEntity;
import nl.juiced.guhs.feature.Minigames;
import nl.juiced.guhs.feature.emotes.Emote;
import nl.juiced.guhs.feature.emotes.GuhEmotes;
import nl.juiced.guhs.feature.gids.GidsFeature;
import nl.juiced.guhs.feature.huisje.Huisje;
import nl.juiced.guhs.feature.huisje.Huisjes;
import nl.juiced.guhs.feature.piep.PiepMaatje;
import nl.juiced.guhs.menu.BankGuhMenu;
import nl.juiced.guhs.menu.GuhWardrobeMenu;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The band's own heart sources and bookkeeping (2.10, fundament): feeding, petting, the menu's "Knuffelen!", time
 * together, travelling together, the moments fundament produces (biome, dimension, riding, minigames, records), keeping
 * {@link GuhVolger} current and the level-ups that happened while the owner was offline.
 */
public final class BandEvents {
    /** Persistent data keys on a guh. */
    static final String KNUFFEL_TOT = "guhs_band_knuffel_tot", GEBOREN = "guhs_band_geboren", BIOOM = "guhs_band_bioom",
            DIM = "guhs_band_dim", REIS = "guhs_band_reis", REIS_X = "guhs_band_reis_x", REIS_Z = "guhs_band_reis_z";
    /** The menu's big cuddle rests this long per guh. */
    public static final int KNUFFEL_RUST = 600;
    /** "Together": the owner is within this many blocks. */
    public static final double SAMEN = 16;
    /** Minigame state per player (polled). */
    private static final Map<UUID, String> SPEELT = new ConcurrentHashMap<>();

    private BandEvents() {
    }

    // =====================================================================================================================
    // called by GuhEntity (the four 2.10 edits) and the menu
    // =====================================================================================================================

    /** Tamed by this player: its record, the moment. */
    public static void getemd(GuhEntity guh, ServerPlayer player) {
        if (!Band.isBandGuh(guh)) {
            return;
        }
        Band.bijwerken(guh);
        GuhVolger.zet(guh, PlekSoort.WERELD, "");
        Band.moment(guh, player, Moment.GETEMD, "");
    }

    /** A tamed baby (GuhEntity.getBreedOffspring): it joins the band once it is in the world. */
    public static void geboren(GuhEntity baby) {
        baby.getPersistentData().putBoolean(GEBOREN, true);
    }

    public static boolean isSnack(ItemStack stack) {
        return !stack.isEmpty() && stack.is(BandFeature.SNACKS);
    }

    /** The owner feeds a snack (not kaas knabbels: those have their own path in GuhEntity). */
    public static void voer(GuhEntity guh, ServerPlayer player, ItemStack stack) {
        ItemStack eaten = stack.copyWithCount(1);
        stack.consume(1, player);
        guh.playSound(ModSounds.GUH_EAT.get(), 1f, guh.getVoicePitch());
        guh.heal(20);
        guh.getLookControl().setLookAt(player);
        guh.emotes.start(Emote.SMAKKEN, false, GuhEmotes.Source.SELF);
        gevoerd(guh, player, eaten);
    }

    /** Something was eaten from the owner's hand: hearts (VOEREN) and the moment. */
    public static void gevoerd(GuhEntity guh, ServerPlayer player, ItemStack eaten) {
        nl.juiced.guhs.feature.verhaal.VariantGedrag gedrag = nl.juiced.guhs.feature.verhaal.VariantGedragen.van(guh);
        if (gedrag != null) {
            gedrag.gegeten(guh, player, eaten);   // 3.0: a story variant's own way of eating (Guhtwo: "x2")
        }
        if (!Band.isBandGuh(guh) || !player.getUUID().equals(guh.getOwnerUUID())) {
            return;
        }
        // (3.0: a story variant may eat "x2": VariantGedrag.voerFactor, the day cap grows along in Band.geefHartjes)
        Band.geefHartjes(guh, player, Reden.VOEREN.standaard() * nl.juiced.guhs.feature.verhaal.VariantGedragen.voerFactor(guh), Reden.VOEREN);
        Band.moment(guh, player, Moment.GEGETEN, BuiltInRegistries.ITEM.getKey(eaten.getItem()).toString());
        nl.juiced.guhs.quest.GuhAdvancements.grant(player, "band_gevoerd");
    }

    /** A tap from the owner: a little pet. */
    public static void aai(GuhEntity guh, ServerPlayer player) {
        if (!Band.isBandGuh(guh) || player.isSecondaryUseActive()) {
            return;
        }
        Band.geefHartjes(guh, player, Reden.AAIEN.standaard(), Reden.AAIEN);
        Band.moment(guh, player, Moment.AANGEAAID, "");
        nl.juiced.guhs.quest.GuhAdvancements.grant(player, "band_geaaid");
    }

    /** The menu's "Knuffelen!": a big cuddle (KNUFFELEN, hearts), then 30 seconds rest for this guh. */
    public static boolean knuffel(GuhEntity guh, ServerPlayer player) {
        if (!Band.isBandGuh(guh) || !player.getUUID().equals(guh.getOwnerUUID())) {
            return false;
        }
        long nu = guh.level().getGameTime();
        CompoundTag data = guh.getPersistentData();
        if (data.getLongOr(KNUFFEL_TOT, 0L) > nu) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.band.knuffel_rust", guh.getDisplayName())
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        guh.getLookControl().setLookAt(player);
        if (!guh.emotes.start(Emote.KNUFFELEN, false, GuhEmotes.Source.OWNER)) {
            player.sendOverlayMessage(Component.translatable("gui.guhs.band.knuffel_nu_niet", guh.getDisplayName())
                    .withStyle(ChatFormatting.GRAY));
            return false;
        }
        data.putLong(KNUFFEL_TOT, nu + KNUFFEL_RUST);
        guh.level().playSound(null, guh.blockPosition(), BandFeature.HARTJES_GELUID.get(), SoundSource.NEUTRAL, 1f, 1f);
        if (guh.level() instanceof ServerLevel level) {
            level.sendParticles(BandFeature.HARTJE.get(), guh.getX(), guh.getY() + guh.getBbHeight() * 0.8, guh.getZ(), 8,
                    guh.getBbWidth() * 0.5, 0.3, guh.getBbWidth() * 0.5, 0.03);
        }
        Band.geefHartjes(guh, player, Reden.KNUFFELEN.standaard(), Reden.KNUFFELEN);
        Band.moment(guh, player, Moment.GEKNUFFELD, "");
        nl.juiced.guhs.quest.GuhAdvancements.grant(player, "band_geknuffeld");
        return true;
    }

    // =====================================================================================================================
    // every band guh (GuhHooks.tick, server)
    // =====================================================================================================================

    static void tick(GuhEntity guh) {
        if (!Band.isBandGuh(guh)) {
            return;
        }
        int t = guh.tickCount + guh.getId();
        if (t % 20 == 0) {
            blij(guh);
            reis(guh);
        }
        if (t % 100 == 0) {
            bijhouden(guh);
        }
        if (t % 1200 == 0) {
            samenTijd(guh);
        }
    }

    /** The happy buff: sparkles while it lasts, the flag off afterwards. */
    private static void blij(GuhEntity guh) {
        boolean blij = Band.isBlij(guh);
        BandVlaggen.zet(guh, BandVlaggen.BLIJ, blij);
        if (blij && !Huisjes.isBinnen(guh) && guh.level() instanceof ServerLevel level && guh.getRandom().nextInt(2) == 0) {
            level.sendParticles(BandFeature.HARTJE.get(), guh.getX(), guh.getY() + guh.getBbHeight() + 0.1, guh.getZ(), 1,
                    guh.getBbWidth() * 0.4, 0.1, guh.getBbWidth() * 0.4, 0.01);
        }
    }

    /** Travelling together: blocks travelled while the owner is near (or riding it); REIS every 64 blocks. */
    private static void reis(GuhEntity guh) {
        CompoundTag data = guh.getPersistentData();
        double x = guh.getX(), z = guh.getZ();
        boolean had = data.contains(REIS_X);
        double dx = had ? x - data.getDoubleOr(REIS_X, 0.0) : 0, dz = had ? z - data.getDoubleOr(REIS_Z, 0.0) : 0;
        data.putDouble(REIS_X, x);
        data.putDouble(REIS_Z, z);
        ServerPlayer owner = Band.eigenaarOnline(guh);
        if (!had || owner == null || owner.level() != guh.level() || Huisjes.isBinnen(guh)
                || (owner.distanceTo(guh) > SAMEN && guh.getControllingPassenger() != owner)) {
            return;
        }
        double d = Math.sqrt(dx * dx + dz * dz);
        if (d < 0.5 || d > 80) {
            return;   // (standing still, or a teleport)
        }
        double acc = data.getDoubleOr(REIS, 0.0) + d;
        int blokken = (int) acc;
        if (blokken > 0) {
            Dagboek.tel(guh, DagboekStat.BLOKKEN_SAMEN, blokken);
        }
        acc -= blokken;
        long totaal = data.getLongOr(REIS + "_totaal", 0L) + blokken;
        data.putLong(REIS + "_totaal", totaal % 64);
        for (long i = 0; i < totaal / 64; i++) {
            Band.moment(guh, owner, Moment.REIS, "64");
        }
        data.putDouble(REIS, acc);
    }

    /** Time together: +1 heart per full minute the owner is within 16 blocks (not while it lives in a huisje). */
    private static void samenTijd(GuhEntity guh) {
        ServerPlayer owner = Band.eigenaarOnline(guh);
        if (owner == null || owner.level() != guh.level() || owner.distanceTo(guh) > SAMEN || Huisjes.isBewoner(guh)) {
            return;
        }
        Band.geefHartjes(guh, owner, Reden.SAMEN_TIJD.standaard(), Reden.SAMEN_TIJD);
        BandData.Rec r = Band.rec(guh);
        long vandaag = Band.dag(guh.level().getServer());
        if (r != null && r.samenDag != vandaag) {
            r.samenDag = vandaag;
            Dagboek.tel(guh, DagboekStat.DAGEN_SAMEN, 1);
        }
    }

    /** Every ~5 seconds: its record (name, looks), where it is, the biome and dimension moments, the zielsguh flag. */
    private static void bijhouden(GuhEntity guh) {
        BandData.Rec r = Band.bijwerken(guh);
        if (r == null) {
            return;
        }
        GuhVolger.zet(guh, plekSoort(guh), plekDetail(guh));
        BandVlaggen.zet(guh, BandVlaggen.ZIELSGUH, r.niveau() == BandNiveau.ZIELSGUH);
        CompoundTag data = guh.getPersistentData();
        String dim = guh.level().dimension().identifier().toString();
        if (!dim.equals(data.getStringOr(DIM, ""))) {
            data.putString(DIM, dim);   // (also the first time we see it: tamed in the Guhmensie counts as arriving there)
            Band.moment(guh, nabijeEigenaar(guh, 32), Moment.DIMENSIE, dim);
        }
        Holder<Biome> biome = guh.level().getBiome(guh.blockPosition());
        String bioom = biome.unwrapKey().map(k -> k.identifier().toString()).orElse("");
        if (!bioom.isEmpty() && !bioom.equals(data.getStringOr(BIOOM, ""))) {
            data.putString(BIOOM, bioom);
            Band.moment(guh, nabijeEigenaar(guh, 32), Moment.PLEK, bioom);
        }
    }

    /** The owner when online, in the same level and within r blocks, else null. */
    @Nullable
    public static ServerPlayer nabijeEigenaar(Entity guh, double r) {
        ServerPlayer owner = Band.eigenaarOnline(guh);
        return owner != null && owner.level() == guh.level() && owner.distanceTo(guh) <= r ? owner : null;
    }

    /** What kind of place a loaded guh or maatje is in right now. */
    public static PlekSoort plekSoort(Entity e) {
        if (Huisjes.isBinnen(e)) {
            return PlekSoort.SLAAPT_IN_HUISJE;
        }
        if (Huisjes.isBewoner(e)) {
            return PlekSoort.HUISJE;
        }
        if (e.isVehicle() && e instanceof Mob m && m.getControllingPassenger() instanceof Player) {
            return PlekSoort.RIJDT_OP;
        }
        if (e.isPassenger()) {
            return PlekSoort.RIJDT_MEE;
        }
        if (e instanceof net.minecraft.world.entity.TamableAnimal a && a.isOrderedToSit()) {
            return PlekSoort.ZIT;
        }
        return PlekSoort.WERELD;
    }

    public static String plekDetail(Entity e) {
        if (Huisjes.isBewoner(e)) {
            Huisje h = Huisjes.thuisVan(e);
            return h == null ? "" : h.naam();
        }
        if (e.isVehicle() && e instanceof Mob m && m.getControllingPassenger() instanceof Player p) {
            return p.getGameProfile().name();
        }
        if (e.getVehicle() != null) {
            return e.getVehicle().getName().getString();
        }
        return "";
    }

    // =====================================================================================================================
    // game events
    // =====================================================================================================================

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        Band.server(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Band.server(null);
        SPEELT.clear();
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity e = event.getEntity();
        if (Band.isBandGuh(e) && e instanceof GuhEntity guh) {
            boolean nieuw = guh.getPersistentData().getBooleanOr(GEBOREN, false);
            Band.bijwerken(guh);
            GuhVolger.zet(guh, plekSoort(guh), plekDetail(guh));
            if (nieuw) {
                guh.getPersistentData().remove(GEBOREN);
                ServerPlayer owner = Band.eigenaarOnline(guh);
                Band.moment(guh, owner, Moment.GETEMD, "");
                Dagboek.wistJeDat(guh, "gui.guhs.wistjedat.band.geboren");
            }
        } else if (e instanceof PiepMaatje && e instanceof net.minecraft.world.entity.TamableAnimal a && a.isTame() && a.getOwnerUUID() != null) {
            GuhVolger.zet(e, plekSoort(e), plekDetail(e));
        }
    }

    /** Maatjes (muisjes, Schilly, Poepschilly) every ~5 seconds: where they are (a turtle inside a guh: IN_GUH). */
    @SubscribeEvent
    public static void onEntityTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (!(e instanceof PiepMaatje) || e.level().isClientSide() || (e.tickCount + e.getId()) % 100 != 0
                || !(e instanceof net.minecraft.world.entity.TamableAnimal a) || !a.isTame() || a.getOwnerUUID() == null) {
            return;
        }
        if (e instanceof nl.juiced.guhs.feature.piep.PoepschillyEntity turtle && turtle.isBinnen()) {
            GuhEntity guh = e.level().getNearestEntity(GuhEntity.class, net.minecraft.world.entity.ai.targeting.TargetingConditions.forNonCombat()
                    .ignoreLineOfSight(), null, e.getX(), e.getY(), e.getZ(), e.getBoundingBox().inflate(2));
            GuhVolger.zet(e, PlekSoort.IN_GUH, guh == null ? "" : guh.getName().getString());
            return;
        }
        GuhVolger.zet(e, plekSoort(e), plekDetail(e));
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        Entity e = event.getEntity();
        if (event.getLevel().isClientSide() || e.getRemovalReason() == Entity.RemovalReason.DISCARDED
                || e.getRemovalReason() == Entity.RemovalReason.KILLED) {
            return;   // (picked up: the item takes over; died: see onDeath)
        }
        if (Band.isBandGuh(e) || (e instanceof PiepMaatje && e instanceof net.minecraft.world.entity.TamableAnimal a && a.isTame())) {
            GuhVolger.zet(e, plekSoort(e), plekDetail(e));
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity e = event.getEntity();
        if (e.level().isClientSide()) {
            return;
        }
        if (Band.isBandGuh(e) && e instanceof GuhEntity guh) {
            Wolkjes.naarDeWolkjes(guh);   // 3.0: "In de wolkjes... njeg" (snapshot, a Herinnering star, the Knuffelhart can help)
        } else if (Band.isBandGuh(e) || e instanceof PiepMaatje) {
            GuhVolger.zet(e, PlekSoort.ONBEKEND, "");
        }
    }

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (event.isMounting() && !event.getLevel().isClientSide() && event.getEntityMounting() instanceof ServerPlayer player
                && Band.isBandGuh(event.getEntityBeingMounted()) && event.getEntityBeingMounted() instanceof GuhEntity guh
                && player.getUUID().equals(guh.getOwnerUUID())) {
            Band.moment(guh, player, Moment.RIT, "");
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            Band.meldAchterstallig(player);
            if (!BandData.get(player.level().getServer()).guhsVan(player.getUUID()).isEmpty()) {
                GidsFeature.grant(player, "lieve_vadsjes/root");
            }
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        SPEELT.remove(event.getEntity().getUUID());
    }

    /** Minigames with your guhs around: MINIGAME_START / MINIGAME_EINDE (polled every 10 ticks). */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || (player.tickCount + player.getId()) % 10 != 0) {
            return;
        }
        String nu = Minigames.playing(player);
        String was = SPEELT.get(player.getUUID());
        if (java.util.Objects.equals(nu, was)) {
            return;
        }
        if (nu == null) {
            SPEELT.remove(player.getUUID());
        } else {
            SPEELT.put(player.getUUID(), nu);
        }
        for (GuhEntity guh : Band.samenGuhs(player, 32)) {
            if (was != null) {
                Band.moment(guh, player, Moment.MINIGAME_EINDE, was);
            }
            if (nu != null) {
                Band.moment(guh, player, Moment.MINIGAME_START, nu);
            }
        }
    }

    /** Scorebord: a new personal best with your guhs around: RECORD. */
    static void ingezonden(ServerPlayer player, String board, int score, boolean lowerIsBetter, boolean nieuwRecord) {
        if (!nieuwRecord) {
            return;
        }
        for (GuhEntity guh : Band.samenGuhs(player, 32)) {
            Band.moment(guh, player, Moment.RECORD, board);
        }
    }

    /** Closing a container: guhs and maatjes (as items) that are in it now. */
    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        AbstractContainerMenu menu = event.getContainer();
        long tijd = player.level().getGameTime();
        for (Slot slot : menu.slots) {
            ItemStack stack = slot.getItem();
            if (slot.container == player.getInventory() || GuhVolger.wie(stack) == null) {
                continue;
            }
            Container c = slot.container;
            if (menu instanceof GuhWardrobeMenu) {
                GuhEntity drager = rugzakVan(player, c);
                GuhVolger.item(stack, PlekSoort.ITEM_RUGZAK, player.level().dimension(), drager != null ? drager.blockPosition() : player.blockPosition(),
                        drager != null ? drager.getName().getString() : "", tijd);
            } else if (c instanceof PlayerEnderChestContainer) {
                GuhVolger.item(stack, PlekSoort.ITEM_KIST, player.level().dimension(), player.blockPosition(), "enderkist", tijd);
            } else {
                BlockPos pos = c instanceof BlockEntity be ? be.getBlockPos() : player.blockPosition();
                String soort = c instanceof BlockEntity be ? be.getBlockState().getBlock().getName().getString() : "";
                GuhVolger.item(stack, PlekSoort.ITEM_KIST, player.level().dimension(), pos, soort, tijd);
            }
        }
        if (menu instanceof BankGuhMenu) {
            bankScan(player);
        }
    }

    @Nullable
    private static GuhEntity rugzakVan(Player player, Container c) {
        for (GuhEntity g : player.level().getEntitiesOfClass(GuhEntity.class, player.getBoundingBox().inflate(12))) {
            if (g.getBackpack() == c) {
                return g;
            }
        }
        return null;
    }

    /** The Bank Guhs around a player: guhs and maatjes stored in them. */
    private static void bankScan(ServerPlayer player) {
        ServerLevel level = player.level();
        BlockPos c = player.blockPosition();
        for (int cx = (c.getX() - 8) >> 4; cx <= (c.getX() + 8) >> 4; cx++) {
            for (int cz = (c.getZ() - 8) >> 4; cz <= (c.getZ() + 8) >> 4; cz++) {
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (be instanceof BankGuhBlockEntity bank && be.getBlockPos().closerThan(c, 9)) {
                        for (var entry : bank.getStorage().snapshot().entries()) {
                            ItemStack stack = entry.item();
                            if (GuhVolger.wie(stack) != null) {
                                GuhVolger.item(stack, PlekSoort.ITEM_BANK, level.dimension(), be.getBlockPos(), "", level.getGameTime());
                            }
                        }
                    }
                }
            }
        }
    }

    /** (tests) forget the polled minigame state. */
    static void vergeet(UUID player) {
        SPEELT.remove(player);
    }

    static AABB rond(Entity e, double r) {
        return e.getBoundingBox().inflate(r);
    }
}
