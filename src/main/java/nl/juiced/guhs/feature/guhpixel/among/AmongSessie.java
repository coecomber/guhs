package nl.juiced.guhs.feature.guhpixel.among;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.guhpixel.Arena;
import nl.juiced.guhs.feature.guhpixel.PxGeluid;
import nl.juiced.guhs.feature.guhpixel.Sessie;
import nl.juiced.guhs.feature.guhpixel.SessieStart;
import nl.juiced.guhs.feature.guhpixel.Sessies;
import nl.juiced.guhs.feature.guhpixel.Vertrek;
import nl.juiced.guhs.feature.guhpixel.among.model.Balans;
import nl.juiced.guhs.feature.guhpixel.among.model.Deelnemer;
import nl.juiced.guhs.feature.guhpixel.among.model.Gebeurtenis;
import nl.juiced.guhs.feature.guhpixel.among.model.Ronde;
import nl.juiced.guhs.feature.guhpixel.among.model.Schip;
import nl.juiced.guhs.feature.guhpixel.among.model.TaakStand;
import nl.juiced.guhs.feature.guhpixel.among.model.Uitspraak;
import nl.juiced.guhs.feature.guhpixel.among.model.Vergadering;
import nl.juiced.guhs.network.ModNetworking;
import nl.juiced.guhs.quest.GuhQuests;
import nl.juiced.guhs.taal.Tekst;

/**
 * One round of Among Guhs on its own ship. The rules live in {@code among.model} ({@link Ronde}); this class is the
 * bridge to the world: it tells the round where the real players are and what they do, moves the guh NPCs
 * ({@link AmongGuhEntity}) to where the round says they are, and turns the round's events into sounds, texts, screens,
 * door blocks and sleeping guhs.
 * <p>
 * Options ({@code opties}): {@code Lastig} (10 participants, 2 Mikas), and for tests and the dev command {@code Rol}
 * ("mika" / "crew": the role of the first player) and {@code Seed}.
 * <p>
 * Nobody is hurt and nothing is lost: a player who is pushed asleep or voted out goes on as a droomguh (spectator mode:
 * see-through, floating, through walls) and still does tasks; the own game mode comes back when the player leaves.
 */
public final class AmongSessie extends Sessie {
    /** (Tests) changes the balance of every round that starts while it is set. */
    public static volatile Consumer<Balans> TEST_BALANS;
    private static final int NA_AFLOOP = 200;
    private static final String KAMER = "gui.guhs.among.kamer.";
    /** In the player's own data while it is a droomguh: the game mode it had before. */
    static final String MODUS = "guhs_px_among_modus";

    public final Ronde ronde;
    private final Schip schip;
    public final boolean lastig;
    private final List<UUID> spelerIds = new ArrayList<>();
    private final Map<UUID, Integer> idxVan = new HashMap<>();
    private final ServerPlayer[] spelerVan;
    /** Per participant: the guh NPC, or the sleeping body of a real player. */
    private final AmongGuhEntity[] guhs;
    private final int[] bedVan;
    private int volgendBed;
    private final Map<UUID, GameType> oudeModus = new HashMap<>();
    /** The repair job a player is busy with: participant -> {panel, since}. */
    private final int[] herstelPaneel, herstelSinds;
    private final List<BlockPos> deurBlokken = new ArrayList<>();
    private int eindTeller = -1;
    private boolean afgerekend;

    public AmongSessie(SessieStart start) {
        super(start);
        this.schip = Schip.standaard();
        this.lastig = start.opties().getBooleanOr("Lastig", false);
        Balans balans = Balans.van(lastig);
        Consumer<Balans> test = TEST_BALANS;
        if (test != null) {
            test.accept(balans);
        }
        List<ServerPlayer> spelers = start.spelers();
        String rol = start.opties().getStringOr("Rol", "");
        int[] gedwongen = null;
        if (rol.equals("mika")) {
            gedwongen = lastig ? new int[]{0, Math.max(balans.deelnemers, spelers.size()) - 1} : new int[]{0};
        } else if (rol.equals("crew")) {
            int n = Math.max(balans.deelnemers, spelers.size());
            gedwongen = lastig ? new int[]{n - 1, n - 2} : new int[]{n - 1};
        }
        long seed = start.opties().contains("Seed") ? start.opties().getLongOr("Seed", 0L) : start.id().getLeastSignificantBits();
        this.ronde = new Ronde(schip, balans, seed, spelers.size(), gedwongen);
        int n = ronde.deelnemers.size();
        this.spelerVan = new ServerPlayer[n];
        this.guhs = new AmongGuhEntity[n];
        this.bedVan = new int[n];
        this.herstelPaneel = new int[n];
        this.herstelSinds = new int[n];
        java.util.Arrays.fill(bedVan, -1);
        java.util.Arrays.fill(herstelPaneel, -1);
        for (int i = 0; i < spelers.size(); i++) {
            spelerIds.add(spelers.get(i).getUUID());
            idxVan.put(spelers.get(i).getUUID(), i);
            spelerVan[i] = spelers.get(i);
        }
    }

    // --- who is who -----------------------------------------------------------------------------------------------------

    /** The participant index of a player in this round (-1: none). */
    public int idx(ServerPlayer p) {
        Integer i = idxVan.get(p.getUUID());
        return i == null ? -1 : i;
    }

    @Nullable
    public ServerPlayer speler(int idx) {
        ServerPlayer p = idx >= 0 && idx < spelerVan.length ? spelerVan[idx] : null;
        return p != null && speelt(p) ? p : null;
    }

    @Nullable
    public AmongGuhEntity guh(int idx) {
        return guhs[idx];
    }

    /** The coloured name of a participant: the player's name, or the colour of the guh NPC. */
    public MutableComponent naam(int idx) {
        Deelnemer d = ronde.d(idx);
        ServerPlayer p = spelerVan[idx];
        MutableComponent c = p != null ? Component.empty().append(p.getName()) : Component.translatable("gui.guhs.among.kleur." + d.kleur.id);
        return c.withStyle(s -> s.withColor(d.kleur.rgb));
    }

    public static Component kamer(Schip.Zone zone) {
        return Component.translatable(KAMER + (zone.kamer() ? zone.id() : "gang"));
    }

    private Vec3 wereld(double x, double z, double dy) {
        return arena().wereld(new Vec3(x, schip.voet + dy, z));
    }

    private void aanAllen(Component c) {
        for (ServerPlayer p : spelers()) {
            p.sendSystemMessage(c);
        }
    }

    private void geluid(Vec3 pos, SoundEvent sound, float volume, float pitch) {
        level().playSound(null, pos.x, pos.y, pos.z, sound, SoundSource.NEUTRAL, volume, pitch);
    }

    private void geluidAllen(SoundEvent sound, float pitch) {
        for (ServerPlayer p : spelers()) {
            PxGeluid.speel(p, sound, SoundSource.NEUTRAL, 0.9f, pitch);
        }
    }

    // --- life cycle -------------------------------------------------------------------------------------------------------

    @Override
    protected void uitrusting(ServerPlayer p) {
        int i = idx(p);
        if (i < 0) {
            return;
        }
        Deelnemer d = ronde.d(i);
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack pak = new ItemStack(switch (slot) {
                case HEAD -> Items.LEATHER_HELMET;
                case CHEST -> Items.LEATHER_CHESTPLATE;
                case LEGS -> Items.LEATHER_LEGGINGS;
                default -> Items.LEATHER_BOOTS;
            });
            pak.set(DataComponents.DYED_COLOR, new DyedItemColor(d.kleur.rgb));
            pak.set(DataComponents.CUSTOM_NAME, Component.translatable("gui.guhs.among.pakje", Component.translatable("gui.guhs.among.kleur." + d.kleur.id)));
            p.setItemSlot(slot, pak);
        }
        if (d.mika()) {
            p.getInventory().setItem(0, new ItemStack(AmongSlice.KUSSEN.get()));
            p.getInventory().setItem(1, new ItemStack(AmongSlice.SABOTEERKAART.get()));
        }
        p.getInventory().setItem(8, new ItemStack(AmongSlice.STEMBRIEFJE.get()));
    }

    @Override
    protected void begin() {
        for (Deelnemer d : ronde.deelnemers) {
            if (d.npc) {
                guhs[d.idx] = maakGuh(d, wereld(d.x, d.z, 0), d.yaw, false);
            } else {
                ServerPlayer p = spelerVan[d.idx];
                naarStoel(p, d);
                boolean mika = d.mika();
                PxGeluid.titel(p, Component.translatable(mika ? "gui.guhs.among.rol.mika" : "gui.guhs.among.rol.crew")
                                .withStyle(mika ? ChatFormatting.RED : ChatFormatting.AQUA, ChatFormatting.BOLD),
                        Component.translatable(mika ? "gui.guhs.among.rol.mika.onder" : "gui.guhs.among.rol.crew.onder").withStyle(ChatFormatting.LIGHT_PURPLE), 70);
                p.sendSystemMessage(Component.translatable("gui.guhs.among.begin." + (mika ? "mika" : "crew"), naam(d.idx),
                        ronde.balans.mikas).withStyle(ChatFormatting.LIGHT_PURPLE));
                if (mika && ronde.balans.mikas > 1) {
                    for (Deelnemer o : ronde.deelnemers) {
                        if (o != d && o.mika()) {
                            p.sendSystemMessage(Component.translatable("gui.guhs.among.begin.maat", naam(o.idx)).withStyle(ChatFormatting.RED));
                        }
                    }
                }
            }
        }
        geluidAllen(AmongSlice.GELUID_BEGIN.get(), 1f);
        stuurHud();
    }

    private AmongGuhEntity maakGuh(Deelnemer d, Vec3 pos, float yaw, boolean slaapt) {
        AmongGuhEntity e = AmongSlice.AMONG_GUH.get().create(level(), EntitySpawnReason.TRIGGERED);
        if (e == null) {
            return null;
        }
        e.deelnemer = d.idx;
        e.setKleur(d.kleur);
        e.setSlaapt(slaapt);
        e.setCustomName(naam(d.idx));
        e.snapTo(pos.x, pos.y, pos.z, yaw, 0f);
        e.setYHeadRot(yaw);
        e.setYBodyRot(yaw);
        level().addFreshEntity(e);
        return e;
    }

    private void naarStoel(ServerPlayer p, Deelnemer d) {
        Vec3 pos = wereld(d.x, d.z, 0);
        p.teleportTo(level(), pos.x, pos.y, pos.z, Set.of(), d.yaw, 0f, true);
        p.resetFallDistance();
    }

    @Override
    protected void tick() {
        if (eindTeller >= 0) {
            if (--eindTeller <= 0) {
                stop();
            }
            return;
        }
        BlockPos o = arena().oorsprong();
        boolean vergadering = ronde.fase == Ronde.Fase.VERGADERING;
        AABB doos = arena().doos().inflate(3);
        for (ServerPlayer p : spelers()) {
            int i = idx(p);
            if (i < 0) {
                continue;
            }
            Deelnemer d = ronde.d(i);
            if (!doos.contains(p.position())) {
                Vec3 start = arena().start();
                p.teleportTo(level(), start.x, start.y, start.z, Set.of(), p.getYRot(), p.getXRot(), true);
            }
            if (vergadering && d.wakker) {
                if (p.distanceToSqr(wereld(d.x, d.z, 0)) > 2.25) {
                    naarStoel(p, d);
                }
            } else {
                ronde.zetPositie(i, p.getX() - o.getX(), p.getZ() - o.getZ(), p.getYRot());
            }
            if (!d.wakker && p.getCamera() != p) {
                p.setCamera(p);
            }
            if (ticks() % 20 == 0 && ronde.donker() && d.wakker && !d.mika()) {
                p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
            }
        }
        ronde.tick();
        verwerk();
        if (isGestopt()) {
            return;
        }
        for (Deelnemer d : ronde.deelnemers) {
            AmongGuhEntity e = guhs[d.idx];
            if (e == null || !e.isAlive() || !d.npc || !d.wakker) {
                continue;
            }
            Vec3 pos = wereld(d.x, d.z, 0);
            e.setPos(pos.x, pos.y, pos.z);
            e.setYRot(d.yaw);
            e.setYBodyRot(d.yaw);
            e.setYHeadRot(d.yaw);
            e.setWerkt(d.werkt);
        }
        if (ticks() % 10 == 0) {
            stuurHud();
        }
        if (ronde.sabotage == Ronde.Sabotage.ALARM && ticks() % 30 == 0) {
            geluidAllen(AmongSlice.GELUID_ALARM.get(), 1f);
        }
    }

    @Override
    protected void spelerWeg(ServerPlayer p, Vertrek reden) {
        GameType oud = oudeModus.remove(p.getUUID());
        if (oud != null) {
            p.setGameMode(oud);
        }
        GuhQuests.saved(p).remove(MODUS);
        p.removeEffect(MobEffects.BLINDNESS);
        ModNetworking.sendTo(p, new AmongPayloads.Scherm(AmongPayloads.HUD, new CompoundTag()));
        int i = idx(p);
        if (i >= 0 && ronde.fase != Ronde.Fase.KLAAR) {
            ronde.verlaat(i);
            AmongGuhEntity lichaam = guhs[i];
            if (lichaam != null) {
                lichaam.discard();
                guhs[i] = null;
            }
            if (!isGestopt()) {
                verwerk();
            }
        }
    }

    @Override
    protected void einde() {
        ronde.breekAf();
        deurenOpen();
    }

    @Override
    public boolean magGebruiken(ServerPlayer p, BlockPos pos, net.minecraft.world.level.block.state.BlockState s) {
        return false;       // (the panels, the button and the vents are handled before the block: gebruikBlok)
    }

    // --- the round's events -------------------------------------------------------------------------------------------------

    private void verwerk() {
        while (!ronde.gebeurtenissen.isEmpty()) {
            List<Gebeurtenis> nu = new ArrayList<>(ronde.gebeurtenissen);
            ronde.gebeurtenissen.clear();
            for (Gebeurtenis g : nu) {
                gebeurtenis(g);
            }
        }
    }

    private void gebeurtenis(Gebeurtenis g) {
        switch (g.soort()) {
            case DUW -> {
                Deelnemer b = ronde.d(g.b());
                Vec3 pos = wereld(b.lichaamX, b.lichaamZ, 0);
                geluid(pos, AmongSlice.GELUID_DUW.get(), 0.6f, 1f);
                level().sendParticles(ParticleTypes.POOF, pos.x, pos.y + 0.5, pos.z, 8, 0.3, 0.2, 0.3, 0.01);
                inSlaap(b, pos, b.yaw);
                ServerPlayer slachtoffer = speler(b.idx);
                if (slachtoffer != null) {
                    PxGeluid.titel(slachtoffer, Component.translatable("gui.guhs.among.geduwd").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD),
                            Component.translatable("gui.guhs.among.geduwd.onder", naam(g.a())), 70);
                }
            }
            case GEMELD -> {
                aanAllen(Component.translatable("gui.guhs.among.gemeld", naam(g.a()), naam(g.b())).withStyle(ChatFormatting.YELLOW));
                geluidAllen(AmongSlice.GELUID_VERGADERING.get(), 1f);
            }
            case KNOP -> {
                aanAllen(Component.translatable("gui.guhs.among.knop", naam(g.a())).withStyle(ChatFormatting.YELLOW));
                geluidAllen(AmongSlice.GELUID_VERGADERING.get(), 1.2f);
            }
            case VERGADERING -> {
                deurenOpen();
                for (Deelnemer d : ronde.deelnemers) {
                    ServerPlayer p = speler(d.idx);
                    if (p != null) {
                        p.removeEffect(MobEffects.BLINDNESS);
                        herstelPaneel[d.idx] = -1;
                        if (d.wakker) {
                            naarStoel(p, d);
                        }
                    }
                    if (!d.wakker && !d.weg) {
                        naarBed(d);
                    }
                }
                stuurVergadering(true, false);
            }
            case UITSPRAAK -> {
                Vergadering v = ronde.vergadering != null ? ronde.vergadering : ronde.vorigeVergadering;
                if (v != null && g.a() < v.uitspraken.size()) {
                    Uitspraak u = v.uitspraken.get(g.a());
                    aanAllen(Component.translatable("gui.guhs.among.zegt", naam(u.spreker()), tekst(u)));
                }
                if (ronde.vergadering != null) {
                    stuurVergadering(false, false);
                }
            }
            case STEM -> {
                geluidAllen(AmongSlice.GELUID_STEM.get(), 1f);
                stuurVergadering(false, false);
            }
            case STEMMEN -> {
                aanAllen(Component.translatable("gui.guhs.among.stemmen").withStyle(ChatFormatting.YELLOW));
                stuurVergadering(false, false);
            }
            case UITSLAG -> {
                Component regel;
                if (g.a() < 0) {
                    regel = Component.translatable(g.c() == 1 ? "gui.guhs.among.uitslag.gelijk" : "gui.guhs.among.uitslag.niemand");
                } else {
                    regel = Component.translatable(g.b() == 1 ? "gui.guhs.among.uitslag.mika" : "gui.guhs.among.uitslag.geen_mika", naam(g.a()));
                }
                aanAllen(regel.copy().withStyle(ChatFormatting.GOLD));
                for (ServerPlayer p : spelers()) {
                    PxGeluid.titel(p, Component.translatable("gui.guhs.among.uitslag").withStyle(ChatFormatting.GOLD), regel, 80);
                }
                geluidAllen(AmongSlice.GELUID_WEGGESTEMD.get(), 1f);
                stuurVergadering(false, false);
            }
            case UIT -> {
                Deelnemer d = ronde.d(g.a());
                if (g.b() == 1) {
                    // voted out: launched to the Slaapzaal with a pillow
                    Vec3 van = wereld(d.x, d.z, 0);
                    level().sendParticles(ParticleTypes.CLOUD, van.x, van.y + 0.6, van.z, 14, 0.3, 0.3, 0.3, 0.05);
                    inSlaap(d, van, d.yaw);
                    naarBed(d);
                    ServerPlayer p = speler(d.idx);
                    if (p != null) {
                        p.sendSystemMessage(Component.translatable("gui.guhs.among.weggestemd.jij").withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                }
            }
            case VERDER -> {
                stuurVergadering(false, true);
                stuurHud();
            }
            case SABOTAGE -> {
                Ronde.Sabotage s = Ronde.Sabotage.values()[g.a()];
                geluidAllen(AmongSlice.GELUID_SABOTAGE.get(), s == Ronde.Sabotage.ALARM ? 0.8f : 1.1f);
                if (s == Ronde.Sabotage.DEUREN) {
                    deurenDicht(g.b());
                    aanAllen(Component.translatable("gui.guhs.among.sabotage.deuren", kamer(schip.zone(g.b()))).withStyle(ChatFormatting.RED));
                } else {
                    Component kop = Component.translatable("gui.guhs.among.sabotage." + (s == Ronde.Sabotage.ALARM ? "alarm" : "licht"))
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                    Component onder = Component.translatable("gui.guhs.among.sabotage." + (s == Ronde.Sabotage.ALARM ? "alarm" : "licht") + ".onder");
                    for (ServerPlayer p : spelers()) {
                        PxGeluid.titel(p, kop, onder, 60);
                        p.sendSystemMessage(Component.empty().append(kop).append(" ").append(onder));
                    }
                }
                stuurHud();
            }
            case SABOTAGE_KLAAR -> {
                Ronde.Sabotage s = Ronde.Sabotage.values()[g.a()];
                if (s == Ronde.Sabotage.DEUREN) {
                    deurenOpen();
                } else {
                    for (ServerPlayer p : spelers()) {
                        p.removeEffect(MobEffects.BLINDNESS);
                    }
                    if (g.b() == 1) {
                        aanAllen(Component.translatable("gui.guhs.among.sabotage." + (s == Ronde.Sabotage.ALARM ? "alarm" : "licht") + ".klaar",
                                g.c() >= 0 ? naam(g.c()) : Component.empty()).withStyle(ChatFormatting.GREEN));
                        geluidAllen(AmongSlice.GELUID_TAAK.get(), 0.8f);
                    }
                }
                stuurHud();
            }
            case ALARM_PANEEL -> {
                aanAllen(Component.translatable("gui.guhs.among.sabotage.alarm.half", naam(g.b()),
                        kamer(schip.zone(schip.alarmPanelen.get(g.a()).kamer()))).withStyle(ChatFormatting.YELLOW));
                stuurHud();
            }
            case TAAK -> {
                ServerPlayer p = speler(g.a());
                if (p != null) {
                    PxGeluid.speel(p, AmongSlice.GELUID_TAAK.get(), SoundSource.PLAYERS, 0.8f, g.b() == 1 ? 1.2f : 1f);
                    p.sendOverlayMessage(Component.translatable(g.b() == 1 ? "gui.guhs.among.taak.klaar" : "gui.guhs.among.taak.stap")
                            .withStyle(ChatFormatting.GREEN));
                }
            }
            case LUIK -> {
                Schip.Luik van = schip.luiken.get(g.b()), naar = schip.luiken.get(g.c());
                for (Schip.Luik l : List.of(van, naar)) {
                    Vec3 pos = arena().wereld(new Vec3(l.x() + 0.5, l.y(), l.z() + 0.5));
                    geluid(pos, AmongSlice.GELUID_LUIK.get(), 0.5f, 1f);
                    level().sendParticles(ParticleTypes.SMOKE, pos.x, pos.y + 0.2, pos.z, 6, 0.2, 0.1, 0.2, 0.01);
                }
                ServerPlayer p = speler(g.a());
                Vec3 pos = arena().wereld(new Vec3(naar.x() + 0.5, naar.y() + 0.2, naar.z() + 0.5));
                if (p != null) {
                    p.teleportTo(level(), pos.x, pos.y, pos.z, Set.of(), p.getYRot(), p.getXRot(), true);
                } else if (guhs[g.a()] != null) {
                    guhs[g.a()].snapTo(pos.x, pos.y - 0.2, pos.z, ronde.d(g.a()).yaw, 0f);
                }
            }
            case EINDE -> afloop();
            default -> {
            }
        }
    }

    /** A participant falls asleep: an NPC guh lies down, a real player becomes a droomguh and leaves a sleeping guh behind. */
    private void inSlaap(Deelnemer d, Vec3 pos, float yaw) {
        if (d.npc) {
            AmongGuhEntity e = guhs[d.idx];
            if (e != null) {
                e.setPos(pos.x, pos.y, pos.z);
                e.setSlaapt(true);
                e.setWerkt(false);
            }
            return;
        }
        ServerPlayer p = speler(d.idx);
        if (guhs[d.idx] == null) {
            guhs[d.idx] = maakGuh(d, pos, yaw, true);
        }
        if (p != null && !oudeModus.containsKey(p.getUUID())) {
            oudeModus.put(p.getUUID(), p.gameMode.getGameModeForPlayer());
            // (also in the player's own data: after a crash the login puts the game mode back, see herstelModus)
            GuhQuests.saved(p).putInt(MODUS, p.gameMode.getGameModeForPlayer().getId());
            p.setGameMode(GameType.SPECTATOR);
            p.removeEffect(MobEffects.BLINDNESS);
            p.sendSystemMessage(Component.translatable("gui.guhs.among.droomguh").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    /** A sleeper is carried to its own bed in the Slaapzaal. */
    private void naarBed(Deelnemer d) {
        AmongGuhEntity e = guhs[d.idx];
        if (e == null) {
            return;
        }
        if (bedVan[d.idx] < 0) {
            bedVan[d.idx] = volgendBed++ % schip.bedden.size();
        }
        Schip.Plek bed = schip.bedden.get(bedVan[d.idx]);
        Vec3 pos = wereld(bed.x(), bed.z(), 0.5625);
        e.snapTo(pos.x, pos.y, pos.z, bed.yaw(), 0f);
        e.setYBodyRot(bed.yaw());
        e.setYHeadRot(bed.yaw());
        e.setSlaapt(true);
        level().sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + 0.3, pos.z, 6, 0.3, 0.1, 0.3, 0.01);
    }

    private void deurenDicht(int kamer) {
        deurenOpen();
        Arena a = arena();
        for (Schip.Deur deur : schip.deurenVan(kamer)) {
            for (int x = deur.x0(); x <= deur.x1(); x++) {
                for (int z = deur.z0(); z <= deur.z1(); z++) {
                    for (int y = schip.voet; y < schip.voet + 3; y++) {
                        BlockPos pos = a.wereld(x, y, z);
                        if (level().getBlockState(pos).isAir() && level().getEntitiesOfClass(Player.class, new AABB(pos)).isEmpty()) {
                            level().setBlock(pos, Blocks.PINK_WOOL.defaultBlockState(), 3);
                            deurBlokken.add(pos);
                        }
                    }
                }
            }
        }
    }

    private void deurenOpen() {
        for (BlockPos pos : deurBlokken) {
            if (level().getBlockState(pos).is(Blocks.PINK_WOOL)) {
                level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        deurBlokken.clear();
    }

    /** (Arena cleanup) no door of this ship stays shut for the next game. */
    static void herstelArena(Arena a) {
        Schip schip = Schip.standaard();
        for (Schip.Deur deur : schip.deuren) {
            for (int x = deur.x0(); x <= deur.x1(); x++) {
                for (int z = deur.z0(); z <= deur.z1(); z++) {
                    for (int y = schip.voet; y < schip.voet + 3; y++) {
                        BlockPos pos = a.wereld(x, y, z);
                        if (a.level().getBlockState(pos).is(Blocks.PINK_WOOL)) {
                            a.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }
    }

    /** The round is over: titles, who the Mikas were, numbers and muntjes; a little later everybody goes back to the lobby. */
    private void afloop() {
        if (afgerekend) {
            return;
        }
        afgerekend = true;
        deurenOpen();
        boolean mikaWint = ronde.winnaar == Deelnemer.Rol.MIKA;
        MutableComponent mikas = Component.empty();
        boolean eerste = true;
        for (Deelnemer d : ronde.deelnemers) {
            if (d.mika()) {
                if (!eerste) {
                    mikas.append(", ");
                }
                mikas.append(naam(d.idx));
                eerste = false;
            }
        }
        Component reden = Component.translatable("gui.guhs.among.einde." + (ronde.einde == null ? "verlaten" : ronde.einde.name().toLowerCase(java.util.Locale.ROOT)));
        for (ServerPlayer p : spelers()) {
            int i = idx(p);
            if (i < 0) {
                continue;
            }
            Deelnemer d = ronde.d(i);
            p.removeEffect(MobEffects.BLINDNESS);
            boolean gewonnen = d.mika() == mikaWint;
            PxGeluid.titel(p, Component.translatable(mikaWint ? "gui.guhs.among.einde.mika_wint" : "gui.guhs.among.einde.crew_wint")
                    .withStyle(gewonnen ? ChatFormatting.GREEN : ChatFormatting.RED, ChatFormatting.BOLD), reden, 120);
            p.sendSystemMessage(Component.empty().append(reden).append(" ").append(Component.translatable("gui.guhs.among.einde.mikas", mikas))
                    .withStyle(ChatFormatting.GOLD));
            int betaald = AmongBeloning.rondeKlaar(p, d.mika(), gewonnen, lastig, d.takenKlaar, d.duwen, d.weggestemd);
            p.sendSystemMessage(Component.translatable(gewonnen ? "gui.guhs.among.einde.gewonnen" : "gui.guhs.among.einde.verloren", betaald,
                    AmongBeloning.DAG_MAX - nl.juiced.guhs.feature.guhpixel.Muntjes.vandaag(p, AmongBeloning.POT)).withStyle(ChatFormatting.LIGHT_PURPLE));
            PxGeluid.speel(p, gewonnen ? AmongSlice.GELUID_TAAK.get() : AmongSlice.GELUID_WEGGESTEMD.get(), SoundSource.PLAYERS, 1f, gewonnen ? 1.3f : 0.8f);
        }
        stuurVergadering(false, true);
        stuurHud();
        eindTeller = NA_AFLOOP;
    }

    // --- what a real player does ----------------------------------------------------------------------------------------------

    private void nee(ServerPlayer p, String sleutel, Object... args) {
        p.sendOverlayMessage(Component.translatable("gui.guhs.among.nee." + sleutel, args).withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    private void antwoord(ServerPlayer p, Ronde.Antwoord a, int afkoel) {
        switch (a) {
            case AFKOEL -> nee(p, "afkoel", Math.max(1, (afkoel + 19) / 20));
            case TE_VER -> nee(p, "te_ver");
            case NIET_NU -> nee(p, "niet_nu");
            case MAG_NIET -> nee(p, "mag_niet");
            default -> {
            }
        }
    }

    /**
     * A player right-clicks a block of the ship: a task panel, the emergency button, a vent. True when it was one of those
     * (the click is used up).
     */
    public boolean gebruikBlok(ServerPlayer p, BlockPos pos) {
        int i = idx(p);
        if (i < 0 || eindTeller >= 0) {
            return false;
        }
        BlockPos l = arena().lokaal(pos);
        Deelnemer d = ronde.d(i);
        if (l.getX() == schip.knopX && l.getY() == schip.knopY && l.getZ() == schip.knopZ) {
            Ronde.Antwoord a = ronde.knop(i);
            if (a == Ronde.Antwoord.MAG_NIET) {
                nee(p, d.wakker ? "knop_op" : "droom");
            } else if (a == Ronde.Antwoord.NIET_NU && ronde.sabotage == Ronde.Sabotage.ALARM) {
                nee(p, "knop_alarm");
            } else {
                antwoord(p, a, ronde.knopAfkoel);
            }
            verwerk();
            return true;
        }
        for (Schip.Paneel paneel : schip.panelen) {
            if (l.getX() == paneel.bx() && l.getY() == paneel.by() && l.getZ() == paneel.bz()) {
                paneel(p, d, paneel);
                return true;
            }
        }
        for (Schip.Luik luik : schip.luiken) {
            if (l.getX() == luik.x() && l.getY() == luik.y() && l.getZ() == luik.z()) {
                if (!d.mika() || !d.wakker) {
                    nee(p, "luik");
                } else {
                    stuurKaart(p, luik);
                }
                return true;
            }
        }
        return false;
    }

    private void paneel(ServerPlayer p, Deelnemer d, Schip.Paneel paneel) {
        if (ronde.fase != Ronde.Fase.SPEL) {
            nee(p, "niet_nu");
            return;
        }
        if (!ronde.bijPaneel(d, paneel)) {
            nee(p, "te_ver");
            return;
        }
        CompoundTag data = new CompoundTag();
        data.putInt("Paneel", paneel.idx());
        Tekst.put(data, "Kamer", kamer(schip.zone(paneel.kamer())));
        if (ronde.teHerstellen(paneel.idx())) {
            String soort = paneel.soort() == Schip.PaneelSoort.LICHT ? TaakSoorten.HERSTEL_LICHT : TaakSoorten.HERSTEL_ALARM;
            herstelPaneel[d.idx] = paneel.idx();
            herstelSinds[d.idx] = ronde.tick;
            data.putString("Soort", soort);
            data.putInt("Duur", TaakSoorten.van(soort).duur(ronde.balans));
            data.putBoolean("Herstel", true);
            ModNetworking.sendTo(p, new AmongPayloads.Scherm(AmongPayloads.TAAK, data));
            return;
        }
        TaakStand t = ronde.beginTaak(d.idx, paneel.idx());
        if (t == null) {
            nee(p, paneel.soort() == Schip.PaneelSoort.TAAK ? "geen_taak" : "niets_stuk");
            return;
        }
        herstelPaneel[d.idx] = -1;
        data.putString("Soort", t.taak.soort());
        data.putInt("Duur", TaakSoorten.van(t.taak.soort()).duur(ronde.balans));
        data.putInt("Stap", t.stap + 1);
        data.putInt("Stappen", t.taak.panelen().length);
        ModNetworking.sendTo(p, new AmongPayloads.Scherm(AmongPayloads.TAAK, data));
    }

    /** The client says the panel is finished. Checked here: the right panel, long enough, still standing there, a good result. */
    public boolean taakKlaar(ServerPlayer p, int paneelIdx, CompoundTag resultaat) {
        int i = idx(p);
        if (i < 0 || paneelIdx < 0 || paneelIdx >= schip.panelen.size() || eindTeller >= 0) {
            return false;
        }
        boolean goed;
        if (herstelPaneel[i] == paneelIdx) {
            Schip.Paneel paneel = schip.panelen.get(paneelIdx);
            String soort = paneel.soort() == Schip.PaneelSoort.LICHT ? TaakSoorten.HERSTEL_LICHT : TaakSoorten.HERSTEL_ALARM;
            TaakSoorten.TaakSoort ts = TaakSoorten.van(soort);
            goed = ronde.tick - herstelSinds[i] >= ts.minTicks(ronde.balans) && ts.geldig(resultaat) && ronde.herstel(i, paneelIdx);
            herstelPaneel[i] = -1;
        } else {
            TaakStand t = ronde.taakBij(i, paneelIdx);
            if (t == null) {
                return false;
            }
            TaakSoorten.TaakSoort ts = TaakSoorten.van(t.taak.soort());
            goed = ts.geldig(resultaat) && ronde.taakKlaar(i, paneelIdx, ts.minTicks(ronde.balans));
        }
        if (!goed) {
            nee(p, "taak_mislukt");
        }
        verwerk();
        if (!isGestopt()) {
            stuurHud(p);
        }
        return goed;
    }

    public void taakStop(ServerPlayer p) {
        int i = idx(p);
        if (i >= 0) {
            ronde.d(i).werkPaneel = -1;
            herstelPaneel[i] = -1;
        }
    }

    /** A click on a guh of the round: a sleeper is reported; with the pillow in the hand an awake one is pushed. */
    public void klikGuh(ServerPlayer p, AmongGuhEntity guh) {
        int i = idx(p);
        if (i < 0 || guh.deelnemer < 0 || guh.deelnemer >= ronde.deelnemers.size() || eindTeller >= 0) {
            return;
        }
        Deelnemer doel = ronde.d(guh.deelnemer);
        if (doel.lichaam) {
            Ronde.Antwoord a = ronde.meld(i, doel.idx);
            if (a == Ronde.Antwoord.MAG_NIET) {
                nee(p, "droom");
            } else {
                antwoord(p, a, 0);
            }
            verwerk();
        } else if (doel.wakker && p.getMainHandItem().is(AmongSlice.KUSSEN.get())) {
            duw(p, doel.idx);
        }
    }

    /** The pillow: push this participant asleep, or (doel -1) whoever stands closest in front of the Mika. */
    public boolean duw(ServerPlayer p, int doel) {
        int i = idx(p);
        if (i < 0 || eindTeller >= 0) {
            return false;
        }
        Deelnemer ik = ronde.d(i);
        if (doel < 0) {
            Vec3 kijk = p.getLookAngle();
            double best = Double.MAX_VALUE;
            for (Deelnemer d : ronde.deelnemers) {
                if (d == ik || !d.wakker || d.weg || d.mika()) {
                    continue;
                }
                double dx = d.x - ik.x, dz = d.z - ik.z, afstand = Math.hypot(dx, dz);
                if (afstand > ronde.balans.duwBereik || (afstand > 0.6 && (dx * kijk.x + dz * kijk.z) / afstand < 0.2)) {
                    continue;
                }
                if (afstand < best) {
                    best = afstand;
                    doel = d.idx;
                }
            }
            if (doel < 0) {
                antwoord(p, ik.duwAfkoel > 0 ? Ronde.Antwoord.AFKOEL : Ronde.Antwoord.TE_VER, ik.duwAfkoel);
                return false;
            }
        }
        if (doel >= ronde.deelnemers.size()) {
            return false;
        }
        Ronde.Antwoord a = ronde.duw(i, doel);
        antwoord(p, a, ik.duwAfkoel);
        verwerk();
        if (!isGestopt()) {
            stuurHud(p);
        }
        return a == Ronde.Antwoord.OK;
    }

    /** The Mika's map: a sabotage (the ordinal of Ronde.Sabotage; the room only for the doors). */
    public boolean saboteer(ServerPlayer p, int soort, int kamer) {
        int i = idx(p);
        if (i < 0 || soort <= 0 || soort >= Ronde.Sabotage.values().length || eindTeller >= 0) {
            return false;
        }
        Ronde.Antwoord a = ronde.saboteer(i, Ronde.Sabotage.values()[soort], kamer);
        antwoord(p, a, ronde.saboteerAfkoel > 0 ? ronde.saboteerAfkoel : ronde.sabotageTeller);
        verwerk();
        return a == Ronde.Antwoord.OK;
    }

    public boolean luik(ServerPlayer p, int van, int naar) {
        int i = idx(p);
        if (i < 0 || eindTeller >= 0) {
            return false;
        }
        Ronde.Antwoord a = ronde.luik(i, van, naar);
        antwoord(p, a, ronde.d(i).luikAfkoel);
        verwerk();
        return a == Ronde.Antwoord.OK;
    }

    public boolean stem(ServerPlayer p, int doel) {
        int i = idx(p);
        boolean ok = i >= 0 && ronde.stem(i, doel < 0 ? Vergadering.OVERSLAAN : doel);
        verwerk();
        return ok;
    }

    public boolean zeg(ServerPlayer p, int soort, int over, int zone) {
        int i = idx(p);
        if (i < 0 || soort < 0 || soort >= Uitspraak.Soort.values().length) {
            return false;
        }
        boolean ok = ronde.uitspraak(i, Uitspraak.Soort.values()[soort], over, zone);
        if (!ok) {
            nee(p, "zeggen");
        }
        verwerk();
        return ok;
    }

    /** The Stembriefje: the meeting screen again (after chatting), or a reminder of the tasks. */
    public void briefje(ServerPlayer p) {
        if (ronde.fase == Ronde.Fase.VERGADERING) {
            ModNetworking.sendTo(p, new AmongPayloads.Scherm(AmongPayloads.VERGADERING, vergaderTag(idx(p), true, false)));
        } else {
            nee(p, "geen_vergadering");
        }
    }

    /** The Saboteerkaart of a Mika. */
    public void kaart(ServerPlayer p) {
        int i = idx(p);
        if (i < 0 || !ronde.d(i).mika()) {
            nee(p, "mag_niet");
            return;
        }
        stuurKaart(p, null);
    }

    // --- what the clients get ---------------------------------------------------------------------------------------------------

    private void stuurKaart(ServerPlayer p, @Nullable Schip.Luik luik) {
        CompoundTag data = new CompoundTag();
        ListTag kamers = new ListTag();
        if (luik != null) {
            data.putInt("Luik", luik.idx());
            for (Schip.Luik l : schip.netwerk(luik)) {
                CompoundTag k = new CompoundTag();
                k.putInt("Idx", l.idx());
                Tekst.put(k, "Naam", kamer(schip.zone(l.kamer())));
                kamers.add(k);
            }
        } else {
            data.putInt("Luik", -1);
            for (Schip.Zone z : schip.kamers()) {
                CompoundTag k = new CompoundTag();
                k.putInt("Idx", z.idx());
                Tekst.put(k, "Naam", kamer(z));
                kamers.add(k);
            }
            data.putInt("Afkoel", ronde.sabotage != Ronde.Sabotage.GEEN ? -1 : ronde.saboteerAfkoel);
        }
        data.put("Kamers", kamers);
        ModNetworking.sendTo(p, new AmongPayloads.Scherm(AmongPayloads.KAART, data));
    }

    public void stuurHud() {
        for (ServerPlayer p : spelers()) {
            stuurHud(p);
        }
    }

    private void stuurHud(ServerPlayer p) {
        int i = idx(p);
        if (i >= 0) {
            ModNetworking.sendTo(p, new AmongPayloads.Scherm(AmongPayloads.HUD, hudTag(i)));
        }
    }

    /** What the HUD of this participant shows. */
    public CompoundTag hudTag(int i) {
        Deelnemer d = ronde.d(i);
        CompoundTag t = new CompoundTag();
        t.putBoolean("Mika", d.mika());
        t.putBoolean("Wakker", d.wakker);
        t.putBoolean("Lastig", lastig);
        t.putInt("Kleur", d.kleur.rgb);
        t.putInt("Fase", ronde.fase.ordinal());
        t.putInt("Klaar", ronde.stappenKlaar());
        t.putInt("Totaal", ronde.stappenTotaal());
        t.putInt("Sabotage", ronde.sabotage.ordinal());
        t.putInt("SabotageOver", ronde.sabotage == Ronde.Sabotage.ALARM || ronde.sabotage == Ronde.Sabotage.DEUREN ? ronde.sabotageTeller : 0);
        t.putInt("AlarmVast", (ronde.alarmVast[0] ? 1 : 0) + (ronde.alarmVast[1] ? 2 : 0));
        if (ronde.deurKamer >= 0) {
            Tekst.put(t, "DeurKamer", kamer(schip.zone(ronde.deurKamer)));
        }
        t.putBoolean("Knop", !d.knopGebruikt);
        if (d.mika()) {
            t.putInt("DuwAfkoel", d.duwAfkoel);
            t.putInt("SaboteerAfkoel", ronde.saboteerAfkoel);
        }
        ListTag taken = new ListTag();
        for (TaakStand ts : d.taken) {
            CompoundTag k = new CompoundTag();
            k.putString("Soort", ts.taak.soort());
            k.putInt("Stap", Math.min(ts.stap, ts.taak.panelen().length));
            k.putInt("Stappen", ts.taak.panelen().length);
            int paneel = ts.klaar() ? ts.taak.panelen()[ts.taak.panelen().length - 1] : ts.paneel();
            Tekst.put(k, "Kamer", kamer(schip.zone(schip.panelen.get(paneel).kamer())));
            taken.add(k);
        }
        t.put("Taken", taken);
        if (ronde.fase == Ronde.Fase.KLAAR && ronde.winnaar != null) {
            t.putBoolean("MikaWint", ronde.winnaar == Deelnemer.Rol.MIKA);
        }
        return t;
    }

    public Component tekst(Uitspraak u) {
        Component wie = u.wie() >= 0 ? naam(u.wie()) : Component.empty();
        Component waar = u.zone() >= 0 ? kamer(schip.zone(u.zone())) : Component.empty();
        return Component.translatable("gui.guhs.among.uitspraak." + u.soort().id() + "." + Math.floorMod(u.variant(), 3), wie, waar);
    }

    private void stuurVergadering(boolean open, boolean dicht) {
        for (ServerPlayer p : spelers()) {
            int i = idx(p);
            if (i >= 0) {
                ModNetworking.sendTo(p, new AmongPayloads.Scherm(AmongPayloads.VERGADERING, vergaderTag(i, open, dicht)));
            }
        }
    }

    /** The whole meeting as this participant may see it. */
    public CompoundTag vergaderTag(int ik, boolean open, boolean dicht) {
        CompoundTag t = new CompoundTag();
        Vergadering v = ronde.vergadering;
        t.putBoolean("Open", open);
        if (dicht || v == null) {
            t.putBoolean("Dicht", true);
            return t;
        }
        Deelnemer zelf = ronde.d(ik);
        t.putInt("Ik", ik);
        t.putInt("Stap", v.stap.ordinal());
        t.putInt("Over", Math.max(0, v.stemTijdOver()));
        t.putBoolean("Wakker", zelf.wakker);
        t.putInt("MijnStem", v.stemmen[ik]);
        t.putInt("Zeggen", Math.max(0, ronde.balans.maxUitspraken - zelf.uitspraken));
        boolean uitslag = v.stap == Vergadering.Stap.UITSLAG;
        ListTag deelnemers = new ListTag();
        int[] telling = v.telling();
        for (Deelnemer d : ronde.deelnemers) {
            CompoundTag k = new CompoundTag();
            k.putInt("Idx", d.idx);
            k.putInt("Kleur", d.kleur.rgb);
            Tekst.put(k, "Naam", naam(d.idx));
            k.putBoolean("Wakker", d.wakker);
            k.putBoolean("Weg", d.weg);
            k.putBoolean("Gestemd", v.stemmen[d.idx] != Vergadering.NIET_GESTEMD);
            k.putBoolean("Maat", zelf.mika() && d.mika() && d != zelf);
            if (uitslag) {
                k.putInt("Stemmen", telling[d.idx]);
                k.putInt("Stem", v.stemmen[d.idx]);
            }
            deelnemers.add(k);
        }
        t.put("Deelnemers", deelnemers);
        ListTag uitspraken = new ListTag();
        for (Uitspraak u : v.uitspraken) {
            CompoundTag k = new CompoundTag();
            k.putInt("Spreker", u.spreker());
            Tekst.put(k, "Tekst", tekst(u));
            uitspraken.add(k);
        }
        t.put("Uitspraken", uitspraken);
        ListTag kamers = new ListTag();
        for (Schip.Zone z : schip.kamers()) {
            CompoundTag k = new CompoundTag();
            k.putInt("Idx", z.idx());
            Tekst.put(k, "Naam", kamer(z));
            kamers.add(k);
        }
        t.put("Kamers", kamers);
        if (uitslag) {
            t.putInt("Weg", v.weg);
            t.putBoolean("WegMika", v.wegMika);
            t.putBoolean("Gelijk", v.gelijk);
            t.putInt("Overgeslagen", telling[telling.length - 1]);
        }
        return t;
    }

    /**
     * A player logs in: whoever was a droomguh when the server crashed is still in spectator mode; the game mode of before
     * comes back (the inventory is the safe's business).
     */
    static void herstelModus(ServerPlayer p) {
        CompoundTag saved = GuhQuests.saved(p);
        if (saved.contains(MODUS) && !(Sessies.van(p) instanceof AmongSessie)) {
            p.setGameMode(GameType.byId(saved.getIntOr(MODUS, GameType.SURVIVAL.getId())));
            saved.remove(MODUS);
        }
    }

    /** The droomguhs of this round (they only talk to each other). */
    public List<ServerPlayer> droomguhs() {
        List<ServerPlayer> uit = new ArrayList<>();
        for (ServerPlayer p : spelers()) {
            int i = idx(p);
            if (i >= 0 && !ronde.d(i).wakker) {
                uit.add(p);
            }
        }
        return uit;
    }

    public boolean isDroomguh(ServerPlayer p) {
        int i = idx(p);
        return i >= 0 && !ronde.d(i).wakker && ronde.fase != Ronde.Fase.KLAAR;
    }

    /** (Dev, tests) the round is decided at once. */
    public void forceerEinde() {
        verwerk();
        if (eindTeller < 0 && ronde.fase != Ronde.Fase.KLAAR) {
            ronde.breekAf();
            eindTeller = 1;
        }
    }

    /** (Tests) handles the round's events now, as the tick would. */
    public void verwerkNu() {
        verwerk();
    }

    public boolean afgelopen() {
        return eindTeller >= 0;
    }
}
