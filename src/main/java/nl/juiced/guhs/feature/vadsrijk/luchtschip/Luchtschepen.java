package nl.juiced.guhs.feature.vadsrijk.luchtschip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.BiConsumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.scene.RijkScenes;
import nl.juiced.guhs.feature.vadsrijk.npc.Questlog;
import nl.juiced.guhs.feature.vadsrijk.wereld.Later;
import nl.juiced.guhs.feature.vadsrijk.wereld.WereldReizen;

/**
 * T-LUCHTSCHIP: the server side of the scheduled airships.
 * <ul>
 *   <li>{@link #tick}: every line's two ships exist while someone is near their place in the timetable (and go when nobody is);
 *       the departure boards (three {@link VertrekbordEntity} lines above each pier) count down; near a pier and on board the
 *       action bar tells you the line, the destination and the time;</li>
 *   <li>{@link #instappen}: right-click a docked ship (gate per line: {@link #SLOT}); sneak at the pier to get off again;</li>
 *   <li>departure ({@link #faseWissel}): the passengers are locked in (no sneaking off, no damage) until the landing, where they are put
 *       on the destination pier ({@link WereldReizen#veiligeLanding});</li>
 *   <li>edge cases: logout in flight = you land at the destination at once; a server stop/crash in flight = the destination is kept
 *       in the player's persistent data ({@link #TAG_NAAR}) and you land there when you log in; death = the flight is forgotten
 *       (kern's respawn); creative/spectator may leave whenever (dev); thrown off by anything else (a teleport, the ship removed)
 *       = slow falling, or the destination when you are still at the ship.</li>
 *   <li>the luxury travel map ({@link #luxe}): the fast travel-map flight costs {@link Lijnen#LUXE_PRIJS} bronze; the first one
 *       to each place is on the captain.</li>
 * </ul>
 */
public final class Luchtschepen {
    /** Persistent player data: the harbour id where this player is flying to (cleared at the landing). */
    public static final String TAG_NAAR = "guhs_vr_luchtschip_naar";
    /** Ships exist within this horizontal distance of a player; removed beyond {@link #WEG}. */
    static final double DICHTBIJ = 320, WEG = 420;
    /** Boards exist within this distance of a player; the action-bar hint within {@link #BORD_HINT}. */
    static final double BORD_DICHTBIJ = 56, BORD_HINT = 9;

    /**
     * Per line: may this player go there? (player, heen = a-&gt;b) -&gt; null (yes) or the lang key of why not (argument: the player's
     * level). The way back (b-&gt;a) is always open by default. Other slices may replace an entry.
     */
    public static final Map<String, BiFunction<ServerPlayer, Boolean, String>> SLOT = new HashMap<>();
    /** Called when a passenger lands (player, harbour). Default: the first-arrival bookkeeping of zone 2/3. */
    public static BiConsumer<ServerPlayer, Lijnen.Haven> AANKOMST = Luchtschepen::eersteAankomst;

    private static final Map<String, UUID> SCHEPEN = new HashMap<>();
    /** Every ship entity in any level (also the gametests' lines), checked every tick: one that is not ticked is moved anyway. */
    private static final java.util.Set<LuchtschipEntity> LEVEND = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    private static final Map<String, List<UUID>> BORDEN = new HashMap<>();

    static {
        SLOT.put("kamp_z1", (p, heen) -> WereldReizen.MAG_VLIEGEN.test(p, heen ? WereldReizen.Dok.HAVEN : WereldReizen.Dok.KAMP)
                ? null : "vadsrijk.luchtschip.slot.kamp");
        SLOT.put("z1_z2", (p, heen) -> !heen ? null : nl.juiced.guhs.feature.vadsrijk.npc.QuestMotor.reisSlot(p, "kandijkom"));
        SLOT.put("z2_z3", (p, heen) -> !heen ? null : nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3Reizen.slot(p));
        SLOT.put("z3_z4", (p, heen) -> !heen || Questlog.vlag(p, "zone3_klaar") ? null : "vadsrijk.luchtschip.slot.z4");
        SLOT.put("z4_z5", (p, heen) -> !heen || Questlog.vlag(p, Lijnen.VLAG_Z4_FINALE) ? null : "vadsrijk.luchtschip.slot.z5");
    }

    private Luchtschepen() {
    }

    // ------------------------------------------------------------------------------------------------ the world: ships + boards

    static void tick(MinecraftServer server) {
        for (LuchtschipEntity e : List.copyOf(LEVEND)) {
            if (e.isRemoved()) {
                LEVEND.remove(e);
            } else if (e.level() instanceof ServerLevel el && !el.isPositionEntityTicking(e.blockPosition())) {
                e.volgExtern();
            }
        }
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        long t = sl.getGameTime();
        if (t % 10 == 0) {
            schepen(sl);
        }
        if (t % 20 == 0) {
            borden(sl);
            meldingen(sl);
        }
    }

    /** Spawn / move / remove the ships. */
    static void schepen(ServerLevel sl) {
        List<ServerPlayer> spelers = sl.players();
        for (Lijnen.Lijn l : Lijnen.ALLE) {
            for (int k = 0; k < 2; k++) {
                String sleutel = l.id() + ":" + k;
                Dienstregeling.Stand s = Dienstregeling.stand(l, k, Tijd.nu(sl, l));
                LuchtschipEntity e = schip(sl, sleutel);
                boolean nodig = l.test() || !l.actief() ? e != null : dichtbij(spelers, s.pos(), DICHTBIJ);
                if (e == null && nodig && nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.klaar(sl, BlockPos.containing(s.pos()))) {   // U-DUBBEL (was hasChunkAt)
                    e = LuchtschipRegistry.SCHIP.get().create(sl, EntitySpawnReason.EVENT);
                    if (e != null) {
                        e.zet(l.id(), k);
                        nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.merk(e, "luchtschip:" + sleutel);   // U-DUBBEL
                        e.snapTo(s.pos().x, s.pos().y, s.pos().z, s.yaw(), 0f);
                        sl.addFreshEntity(e);
                        SCHEPEN.put(sleutel, e.getUUID());
                        LEVEND.add(e);
                    }
                } else if (e != null) {
                    if (!l.test() && e.getPassengers().isEmpty() && !dichtbij(spelers, s.pos(), WEG)) {
                        e.discard();
                        SCHEPEN.remove(sleutel);
                    }
                }
            }
        }
    }

    @Nullable
    static LuchtschipEntity schip(ServerLevel sl, String sleutel) {
        UUID id = SCHEPEN.get(sleutel);
        return id != null && sl.getEntity(id) instanceof LuchtschipEntity e && !e.isRemoved() ? e : null;
    }

    /** (dev/tests) the entity of ship k of a line, spawning it when it is not there. */
    @Nullable
    public static LuchtschipEntity schipNu(ServerLevel sl, Lijnen.Lijn l, int k) {
        String sleutel = l.id() + ":" + k;
        LuchtschipEntity e = schip(sl, sleutel);
        if (e == null) {
            Dienstregeling.Stand s = Dienstregeling.stand(l, k, Tijd.nu(sl, l));
            sl.getChunk(BlockPos.containing(s.pos()));
            e = LuchtschipRegistry.SCHIP.get().create(sl, EntitySpawnReason.EVENT);
            if (e != null) {
                e.zet(l.id(), k);
                nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.merk(e, "luchtschip:" + sleutel);   // U-DUBBEL
                e.snapTo(s.pos().x, s.pos().y, s.pos().z, s.yaw(), 0f);
                sl.addFreshEntity(e);
                SCHEPEN.put(sleutel, e.getUUID());
                LEVEND.add(e);
            }
        }
        return e;
    }

    /** (tests) forget and remove the ships of a line. */
    public static void ruimOp(ServerLevel sl, Lijnen.Lijn l) {
        for (int k = 0; k < 2; k++) {
            LuchtschipEntity e = schip(sl, l.id() + ":" + k);
            if (e != null) {
                e.uitstappen = true;
                e.ejectPassengers();
                e.discard();
            }
            SCHEPEN.remove(l.id() + ":" + k);
        }
    }

    private static boolean dichtbij(List<ServerPlayer> spelers, Vec3 pos, double r) {
        for (ServerPlayer p : spelers) {
            double dx = p.getX() - pos.x, dz = p.getZ() - pos.z;
            if (dx * dx + dz * dz < r * r) {
                return true;
            }
        }
        return false;
    }

    /** The departure boards: three text lines above each pier while someone is near. */
    static void borden(ServerLevel sl) {
        List<ServerPlayer> spelers = sl.players();
        for (Lijnen.Haven h : Lijnen.havens()) {
            Lijnen.Lijn l = Lijnen.lijnVan(h);
            if (l == null) {
                continue;
            }
            Vec3 voet = bordPlek(h);
            List<UUID> ids = BORDEN.get(h.id());
            boolean nodig = dichtbij(spelers, voet, BORD_DICHTBIJ);
            if (!nodig) {
                if (ids != null && !dichtbij(spelers, voet, BORD_DICHTBIJ + 16)) {
                    for (UUID id : ids) {
                        Entity e = sl.getEntity(id);
                        if (e != null) {
                            e.discard();
                        }
                    }
                    BORDEN.remove(h.id());
                }
                continue;
            }
            if (!nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.klaar(sl, BlockPos.containing(voet))) {   // U-DUBBEL (was hasChunkAt: boards spawned while the old ones were hidden)
                continue;
            }
            List<Component> regels = bordTekst(sl, l, h);
            List<UUID> nieuw = new ArrayList<>();
            for (int i = 0; i < regels.size(); i++) {
                Entity e = ids != null && i < ids.size() ? sl.getEntity(ids.get(i)) : null;
                if (!(e instanceof VertrekbordEntity) || e.isRemoved()) {
                    e = LuchtschipRegistry.BORD.get().create(sl, EntitySpawnReason.EVENT);
                    if (e == null) {
                        continue;
                    }
                    e.snapTo(voet.x, voet.y + 0.32 * (regels.size() - 1 - i), voet.z, 0f, 0f);
                    nl.juiced.guhs.feature.vadsrijk.dubbel.Dubbel.merk(e, "luchtschip_bord:" + h.id() + ":" + i);   // U-DUBBEL
                    sl.addFreshEntity(e);
                }
                e.setCustomName(regels.get(i));
                nieuw.add(e.getUUID());
            }
            BORDEN.put(h.id(), nieuw);
        }
    }

    /** Where the board stands: above the pier spot, a little to the land side. */
    static Vec3 bordPlek(Lijnen.Haven h) {
        BlockPos b = Lijnen.zoek(h.id() + "_bord");   // t-wereld's board POI, when built
        if (b != null) {
            return new Vec3(b.getX() + 0.5, b.getY() + 1.2, b.getZ() + 0.5);
        }
        BlockPos p = h.plek();
        return new Vec3(p.getX() + 0.5, p.getY() + 2.1, p.getZ() + 0.5).subtract(h.uit().scale(1.5));
    }

    /** The board's three lines: "Lijn A -> Kandijkom-dok", "Vertrek over 1:23", how to board. */
    public static List<Component> bordTekst(ServerLevel sl, Lijnen.Lijn l, Lijnen.Haven h) {
        Lijnen.Haven naar = l.ander(h);
        int over = Dienstregeling.totVertrek(l, (long) Tijd.nu(sl, l));
        boolean ligt = Dienstregeling.schipAan(l, h, Tijd.nu(sl, l)) >= 0;
        List<Component> r = new ArrayList<>();
        r.add(Component.translatable("vadsrijk.luchtschip.bord.lijn", l.letter(), Component.translatable(naar.naam()))
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        r.add(over <= 40 && ligt ? Component.translatable("vadsrijk.luchtschip.bord.nu").withStyle(ChatFormatting.GREEN)
                : Component.translatable("vadsrijk.luchtschip.bord.over", klok(over)).withStyle(ChatFormatting.WHITE));
        r.add(Component.translatable(ligt ? "vadsrijk.luchtschip.bord.instappen" : "vadsrijk.luchtschip.bord.onderweg",
                klok(Dienstregeling.duur(l))).withStyle(ChatFormatting.GRAY));
        return r;
    }

    /** m:ss */
    public static String klok(int ticks) {
        int s = Math.max(0, (ticks + 19) / 20);
        return s / 60 + ":" + (s % 60 < 10 ? "0" : "") + s % 60;
    }

    /** The action bar: near a pier (the next departure) and on board (departure / landing countdown). */
    static void meldingen(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) {
            if (RijkScenes.bezig(p)) {
                continue;
            }
            if (p.getVehicle() instanceof LuchtschipEntity e) {
                Lijnen.Lijn l = e.lijn();
                Dienstregeling.Stand s = e.stand();
                if (l == null || s == null) {
                    continue;
                }
                Lijnen.Haven naar = s.naar(l);
                p.sendOverlayMessage(s.fase().vliegt()
                        ? Component.translatable("vadsrijk.luchtschip.aan_boord.vlucht", Component.translatable(naar.naam()), klok((int) s.rest()))
                                .withStyle(ChatFormatting.AQUA)
                        : Component.translatable("vadsrijk.luchtschip.aan_boord.wacht", Component.translatable(naar.naam()), klok((int) s.rest()))
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                continue;
            }
            for (Lijnen.Haven h : Lijnen.havens()) {
                BlockPos b = h.plek();
                if (p.distanceToSqr(b.getX() + 0.5, b.getY(), b.getZ() + 0.5) < BORD_HINT * BORD_HINT) {
                    Lijnen.Lijn l = Lijnen.lijnVan(h);
                    if (l != null) {
                        int over = Dienstregeling.totVertrek(l, (long) Tijd.nu(sl, l));
                        p.sendOverlayMessage(Component.translatable("vadsrijk.luchtschip.pier", l.letter(), Component.translatable(l.ander(h).naam()),
                                klok(over)).withStyle(ChatFormatting.GOLD));
                    }
                    break;
                }
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ boarding

    /** Right-click on a ship: get on board when it lies at a pier and the line is open for you. */
    public static boolean instappen(ServerPlayer p, LuchtschipEntity e) {
        return instappen(p, e, false);
    }

    /** {@code dev}: ignore the line's gate. */
    public static boolean instappen(ServerPlayer p, LuchtschipEntity e, boolean dev) {
        Lijnen.Lijn l = e.lijn();
        Dienstregeling.Stand s = e.stand();
        if (l == null || s == null || p.isSpectator()) {
            return false;
        }
        if (p.getVehicle() == e) {
            p.sendOverlayMessage(Component.translatable(s.fase().vliegt() ? "vadsrijk.luchtschip.al_aan_boord.vlucht" : "vadsrijk.luchtschip.al_aan_boord",
                    klok((int) s.rest())).withStyle(ChatFormatting.LIGHT_PURPLE));
            return false;
        }
        if (s.fase().vliegt()) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.luchtschip.vliegt_al").withStyle(ChatFormatting.GOLD));
            return false;
        }
        Lijnen.Haven van = s.van(l), naar = s.naar(l);
        boolean heen = van == l.a();
        String slot = dev || p.getAbilities().instabuild || l.test() ? null : SLOT.getOrDefault(l.id(), (q, h) -> null).apply(p, heen);
        if (slot != null) {
            p.sendOverlayMessage(Component.translatable(slot, Spelers.level(p)).withStyle(ChatFormatting.GOLD));
            return false;
        }
        if (e.getPassengers().size() >= LuchtschipEntity.PLEKKEN.length) {
            p.sendOverlayMessage(Component.translatable("vadsrijk.luchtschip.vol").withStyle(ChatFormatting.GOLD));
            return false;
        }
        if (p.isPassenger()) {
            p.stopRiding();
        }
        if (!p.startRiding(e, true, true)) {
            return false;
        }
        p.addTag(LuchtschipEntity.TAG_VRIJ);   // t-wereld Barrieres: riders are exempt too
        p.level().playSound(null, e.blockPosition(), SoundEvents.WOOD_STEP, SoundSource.PLAYERS, 1f, 0.9f);
        p.sendSystemMessage(Component.translatable("vadsrijk.luchtschip.welkom", Component.translatable(naar.naam()), klok((int) s.rest()))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    /** The ship changes phase: departure (lock the passengers in) or arrival (everybody off on the pier). */
    static void faseWissel(LuchtschipEntity e, Lijnen.Lijn l, Dienstregeling.Fase oud, Dienstregeling.Fase nieuw) {
        if (!(e.level() instanceof ServerLevel sl)) {
            return;
        }
        if (!oud.vliegt() && nieuw.vliegt()) {
            Lijnen.Haven naar = nieuw == Dienstregeling.Fase.VLIEGT_AB ? l.b() : l.a();
            sl.playSound(null, e.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 2f, 0.7f);
            for (Entity q : e.getPassengers()) {
                if (q instanceof ServerPlayer p) {
                    p.getPersistentData().putString(TAG_NAAR, naar.id());
                    p.sendSystemMessage(Component.translatable("vadsrijk.luchtschip.vertrek", Component.translatable(naar.naam()),
                            klok(Dienstregeling.duur(l))).withStyle(ChatFormatting.AQUA));
                }
            }
        } else if (oud.vliegt() && !nieuw.vliegt()) {
            Lijnen.Haven naar = nieuw == Dienstregeling.Fase.LIGT_B ? l.b() : l.a();
            sl.playSound(null, e.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 2f, 1.1f);
            land(sl, e, naar);
        }
    }

    /** Everybody off at this harbour. */
    static void land(ServerLevel sl, LuchtschipEntity e, Lijnen.Haven naar) {
        List<Entity> weg = List.copyOf(e.getPassengers());
        e.uitstappen = true;
        try {
            e.ejectPassengers();
        } finally {
            e.uitstappen = false;
        }
        for (Entity q : weg) {
            if (q instanceof ServerPlayer p) {
                zetNeer(sl, p, naar, true);
            } else {
                Vec3 v = WereldReizen.veiligeLanding(sl, naar.plek());
                q.teleportTo(v.x, v.y, v.z);
            }
        }
    }

    /** Put a player on the pier of this harbour (aankomst: the landing title + first-arrival bookkeeping). */
    static void zetNeer(ServerLevel sl, ServerPlayer p, Lijnen.Haven h, boolean aankomst) {
        Vec3 v = WereldReizen.veiligeLanding(sl, h.plek());
        float yaw = h.richting() + 180f;   // look at the land
        p.teleport(new TeleportTransition(sl, v, Vec3.ZERO, yaw, 0f, TeleportTransition.DO_NOTHING));
        p.resetFallDistance();
        p.setDeltaMovement(Vec3.ZERO);
        p.getPersistentData().remove(TAG_NAAR);
        if (aankomst) {
            RijkScenes.titel(p, Component.translatable(h.naam()), Component.translatable("vadsrijk.luchtschip.aangekomen"), 70, 0x9AD5FF);
            AANKOMST.accept(p, h);
        }
    }

    /** Default arrival bookkeeping: zone 2's first story quest is taken (as the captain's flight did), zone 3 counts as reached. */
    static void eersteAankomst(ServerPlayer p, Lijnen.Haven h) {
        if (h.zone().equals("z2")) {
            nl.juiced.guhs.feature.vadsrijk.npc.Quest q = nl.juiced.guhs.feature.vadsrijk.npc.Quests.van("z2_hq01_splinterpieken");
            if (q != null && Questlog.beschikbaar(p, q)) {
                Questlog.neemAan(p, q);
            }
        } else if (h.zone().equals("z3")) {
            if (Spelers.data(p).gezien.add(nl.juiced.guhs.feature.vadsrijk.zone3.wereld.Z3Reizen.GEZIEN_EERSTE)) {
                Spelers.opgeslagen(p);
            }
        }
    }

    /** (from the entity) a passenger left the ship some way. */
    static void uitgestapt(LuchtschipEntity e, Entity q) {
        if (!(q instanceof ServerPlayer p) || e.uitstappen || !(e.level() instanceof ServerLevel sl)) {
            return;
        }
        Lijnen.Lijn l = e.lijn();
        Dienstregeling.Stand s = e.stand();
        if (l == null || s == null || !p.isAlive() || p.isRemoved()) {
            if (p.isRemoved() || !p.isAlive()) {
                p.getPersistentData().remove(TAG_NAAR);
            }
            return;
        }
        if (!s.fase().vliegt()) {
            Later.na(1, () -> {
                if (!p.isRemoved() && p.level() == sl && p.distanceToSqr(e) < 64) {
                    zetNeer(sl, p, s.van(l), false);   // off at the pier: back onto the pier
                }
            });
            return;
        }
        // in flight and still at the ship (it was removed, ...): land at the destination; teleported away: just float down
        Lijnen.Haven naar = s.naar(l);
        Later.na(1, () -> {
            if (p.isRemoved() || p.level() != sl) {
                return;
            }
            if (p.position().distanceToSqr(e.position()) < 100 && !p.getAbilities().instabuild && !p.isSpectator()) {
                zetNeer(sl, p, naar, true);
            } else {
                p.getPersistentData().remove(TAG_NAAR);
                if (!p.getAbilities().flying) {
                    p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, false));
                }
            }
        });
    }

    // ------------------------------------------------------------------------------------------------ events

    /** No sneaking off in flight ("Blijf lekker aan boord"); creative / spectator may. */
    static void opMount(EntityMountEvent event) {
        if (event.isDismounting() && !event.getLevel().isClientSide() && event.getEntityBeingMounted() instanceof LuchtschipEntity e
                && !e.uitstappen && !e.isRemoved() && e.vliegt() && event.getEntityMounting() instanceof ServerPlayer p && p.isAlive()
                && !p.isRemoved() && p.isShiftKeyDown() && !p.getAbilities().instabuild && !p.isSpectator()) {
            event.setCanceled(true);
            p.sendOverlayMessage(Component.translatable("vadsrijk.luchtschip.blijf").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** On board nothing hurts you (only /kill and the void get through). */
    static void opSchade(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && p.getVehicle() instanceof LuchtschipEntity
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    static void opDood(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            p.getPersistentData().remove(TAG_NAAR);
        }
    }

    /** Logout on board: land at once (in flight: the destination; at the pier: that pier). */
    static void opUitloggen(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && p.getVehicle() instanceof LuchtschipEntity e && p.level() instanceof ServerLevel sl) {
            Lijnen.Lijn l = e.lijn();
            Dienstregeling.Stand s = e.stand();
            e.uitstappen = true;
            try {
                p.stopRiding();
            } finally {
                e.uitstappen = false;
            }
            if (l != null && s != null) {
                Lijnen.Haven h = s.fase().vliegt() ? s.naar(l) : s.van(l);
                Vec3 v = WereldReizen.veiligeLanding(sl, h.plek());
                p.snapTo(v.x, v.y, v.z, h.richting() + 180f, 0f);
                if (s.fase().vliegt()) {
                    AANKOMST.accept(p, h);
                }
            }
            p.getPersistentData().remove(TAG_NAAR);
        }
    }

    /** Login with a flight still open (the server stopped mid-flight): land at its destination. */
    static void opInloggen(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) {
            return;
        }
        String naar = p.getPersistentData().getStringOr(TAG_NAAR, "");
        if (naar.isEmpty()) {
            return;
        }
        p.getPersistentData().remove(TAG_NAAR);
        Lijnen.Haven h = Lijnen.haven(naar);
        if (h == null || !(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(sl) || p.isPassenger()) {
            return;
        }
        Later.na(5, () -> {
            if (!p.isRemoved() && p.level() == sl) {
                zetNeer(sl, p, h, true);
                p.sendSystemMessage(Component.translatable("vadsrijk.luchtschip.alsnog_geland", Component.translatable(h.naam()))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        });
    }

    // ------------------------------------------------------------------------------------------------ the luxury travel map

    /**
     * Before a travel-map / captain flight: true when it may fly (creative, the first time to this place, or paid
     * {@link Lijnen#LUXE_PRIJS}); false (with a message) when the player cannot pay. {@code test}: always free.
     */
    public static boolean luxe(ServerPlayer p, String naar, boolean test) {
        if (test || p.getAbilities().instabuild) {
            return true;
        }
        String vlag = "vr_ls_luxe_" + naar;
        if (Spelers.data(p).gezien.add(vlag)) {
            Spelers.opgeslagen(p);
            p.sendSystemMessage(Component.translatable("vadsrijk.luchtschip.luxe.gratis").withStyle(ChatFormatting.LIGHT_PURPLE));
            return true;
        }
        if (!Spelers.neemMunten(p, Lijnen.LUXE_PRIJS)) {
            p.sendSystemMessage(Component.translatable("vadsrijk.luchtschip.luxe.te_duur", Lijnen.LUXE_PRIJS).withStyle(ChatFormatting.GOLD));
            return false;
        }
        p.sendSystemMessage(Component.translatable("vadsrijk.luchtschip.luxe.betaald", Lijnen.LUXE_PRIJS).withStyle(ChatFormatting.LIGHT_PURPLE));
        return true;
    }

    // ------------------------------------------------------------------------------------------------ dev

    /** A short status line per line (dev). */
    static List<MutableComponent> status(ServerLevel sl) {
        List<MutableComponent> r = new ArrayList<>();
        for (Lijnen.Lijn l : Lijnen.ALLE) {
            double t = Tijd.nu(sl, l);
            StringBuilder b = new StringBuilder();
            b.append(l.letter()).append(" ").append(l.id()).append(": ").append(Math.round(Dienstregeling.pad(l, true).lengte())).append(" bl, ")
                    .append(klok(Dienstregeling.duur(l))).append(" vlucht, vertrek over ").append(klok(Dienstregeling.totVertrek(l, (long) t)));
            for (int k = 0; k < 2; k++) {
                Dienstregeling.Stand s = Dienstregeling.stand(l, k, t);
                b.append(" | schip ").append(k).append(" ").append(s.fase()).append(" @").append(BlockPos.containing(s.pos()).toShortString())
                        .append(schip(sl, l.id() + ":" + k) != null ? " (er)" : "");
            }
            r.add(Component.literal(b.toString()));
        }
        return r;
    }
}
