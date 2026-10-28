package nl.juiced.guhs.feature.guhpixel.gezel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.Guhs;
import nl.juiced.guhs.entity.GuhNpcEntity;
import nl.juiced.guhs.feature.droomwezens.DroomwezensFeature;
import nl.juiced.guhs.feature.guhpixel.ArenaSoort;
import nl.juiced.guhs.feature.guhpixel.LobbyPlek;
import nl.juiced.guhs.feature.guhpixel.Muntjes;
import nl.juiced.guhs.feature.guhpixel.Sessie;
import nl.juiced.guhs.feature.guhpixel.SessieStart;
import nl.juiced.guhs.feature.guhpixel.SpelSoort;
import nl.juiced.guhs.feature.guhpixel.Vertrek;
import nl.juiced.guhs.quest.GuhAdvancements;
import nl.juiced.guhs.quest.Scorebord;
import nl.juiced.guhs.registry.ModEntities;

/**
 * Kussengevecht: everybody in pyjamas on one giant bed, with a pillow. A hit with the pillow (left click) pushes the other
 * one, it never hurts. Who falls off the bed lands softly in the cushion pit around it and is out; the last one on the bed
 * wins (after {@link #DUUR} ticks everybody still on the bed wins). Guh-bots (kussengevecht-guhs, who also throw pillows of
 * their own) fill the bed up to {@link #MIN_DEELNEMERS}.
 * <p>
 * Score (Highscores board {@link #BORD}): 10 per player or bot you pushed off, +50 for winning. Muntjes from a daily pot.
 * Arena: template guhs:guhpixel/kussengevecht (tools/features/guhpixel_gezel.py): the bed's top is local y {@link #BED_Y},
 * x and z {@link #BED_MIN}..{@link #BED_MAX}.
 */
public final class KussenSessie extends Sessie {
    public static final String ID = "kussengevecht", BORD = "kussengevecht";
    public static final Vec3i MAAT = new Vec3i(31, 16, 31);
    public static final int BED_Y = 8, BED_MIN = 8, BED_MAX = 22;
    public static final Vec3 START = new Vec3(15.5, BED_Y + 1, 15.5);
    public static final ArenaSoort ARENA = new ArenaSoort(ID, Guhs.id("guhpixel/kussengevecht"), MAAT, START, 180f);
    public static final SpelSoort SPEL = new SpelSoort(ID, ARENA, 1, 8, LobbyPlek.SPEL_KUSSEN, KussenSessie::new);
    public static final int MIN_DEELNEMERS = 4, AFTELLEN = 60, DUUR = 20 * 120, NA = 100;
    public static final String BOT_TAG = "guhs_kussenbot";
    static final int PYJAMA = 0xF7C6DA;

    private final List<GuhNpcEntity> bots = new ArrayList<>();
    private final Set<UUID> af = new HashSet<>();
    private final Map<UUID, Integer> geduwd = new HashMap<>();
    private final Map<UUID, UUID> laatsteDuwer = new HashMap<>();
    private final Map<UUID, Integer> duwOp = new HashMap<>();
    private int klaarOp = -1;
    private int deelnemers;

    public KussenSessie(SessieStart start) {
        super(start);
    }

    @Override
    protected void uitrusting(ServerPlayer p) {
        p.getInventory().add(new ItemStack(GezelSlice.KUSSEN.get()));
        ItemStack jasje = new ItemStack(Items.LEATHER_CHESTPLATE);
        ItemStack broek = new ItemStack(Items.LEATHER_LEGGINGS);
        for (ItemStack s : new ItemStack[] {jasje, broek}) {
            s.set(DataComponents.DYED_COLOR, new DyedItemColor(PYJAMA));
            s.set(DataComponents.CUSTOM_NAME, Component.translatable(s == jasje ? "gui.guhs.gezel.kussen.jasje" : "gui.guhs.gezel.kussen.broek"));
        }
        p.setItemSlot(EquipmentSlot.CHEST, jasje);
        p.setItemSlot(EquipmentSlot.LEGS, broek);
    }

    @Override
    protected void begin() {
        List<ServerPlayer> spelers = spelers();
        int bots = Math.max(0, MIN_DEELNEMERS - spelers.size());
        deelnemers = spelers.size() + bots;
        for (int i = 0; i < deelnemers; i++) {
            double hoek = Math.PI * 2 * i / deelnemers;
            Vec3 plek = arena().wereld(new Vec3(START.x + Math.cos(hoek) * 4.5, START.y, START.z + Math.sin(hoek) * 4.5));
            float yaw = (float) Math.toDegrees(Math.atan2(-(START.x - (START.x + Math.cos(hoek) * 4.5)), START.z - (START.z + Math.sin(hoek) * 4.5)));
            if (i < spelers.size()) {
                spelers.get(i).teleportTo(level(), plek.x, plek.y, plek.z, Set.of(), yaw, 0, false);
            } else {
                this.bots.add(maakBot(level(), plek, yaw));
            }
        }
        for (ServerPlayer p : spelers) {
            p.sendSystemMessage(Component.translatable("gui.guhs.gezel.kussen.uitleg").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }

    static GuhNpcEntity maakBot(ServerLevel level, Vec3 plek, float yaw) {
        GuhNpcEntity npc = ModEntities.GUH_NPC.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        npc.setKind(GuhNpcEntity.Kind.KUSSENGEVECHT_GUH);
        npc.snapTo(plek.x, plek.y, plek.z, yaw, 0);
        npc.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.26);
        npc.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.0);
        npc.addTag(BOT_TAG);
        npc.setPersistenceRequired();
        level.addFreshEntity(npc);
        return npc;
    }

    /** Still on the bed (not yet fallen into the pit)? */
    boolean opBed(Entity e) {
        return e.isAlive() && !af.contains(e.getUUID()) && e.getY() > arena().wereld(0, BED_Y - 1, 0).getY();
    }

    @Override
    protected void tick() {
        ServerLevel level = level();
        if (ticks() < AFTELLEN) {
            if (ticks() % 20 == 0) {
                int nog = (AFTELLEN - ticks()) / 20;
                for (ServerPlayer p : spelers()) {
                    p.sendOverlayMessage(Component.translatable("gui.guhs.gezel.kussen.aftellen", nog).withStyle(ChatFormatting.GOLD));
                }
            }
            return;
        }
        if (ticks() == AFTELLEN) {
            for (ServerPlayer p : spelers()) {
                p.sendOverlayMessage(Component.translatable("gui.guhs.gezel.kussen.start").withStyle(ChatFormatting.LIGHT_PURPLE));
                level.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
            }
        }
        if (klaarOp >= 0) {
            if (ticks() >= klaarOp) {
                for (ServerPlayer p : spelers()) {
                    klaar(p);
                }
                stop();
            }
            return;
        }
        // who fell off
        List<Entity> iedereen = new ArrayList<>(spelers());
        iedereen.addAll(bots);
        for (Entity e : iedereen) {
            if (!af.contains(e.getUUID()) && !opBed(e)) {
                valtAf(e);
            }
        }
        // the bots
        if (ticks() % 5 == 0) {
            for (GuhNpcEntity bot : bots) {
                if (af.contains(bot.getUUID()) || !bot.isAlive()) {
                    continue;
                }
                LivingEntity doel = dichtstbij(bot, iedereen);
                if (doel == null) {
                    continue;
                }
                bot.getNavigation().moveTo(doel, 1.3);
                bot.getLookControl().setLookAt(doel);
                if (bot.distanceTo(doel) < 1.9 && ticks() >= duwOp.getOrDefault(bot.getUUID(), 0)) {
                    duw(bot, doel);
                    duwOp.put(bot.getUUID(), ticks() + 20 + bot.getRandom().nextInt(25));
                }
            }
        }
        int over = 0;
        for (Entity e : iedereen) {
            if (opBed(e)) {
                over++;
            }
        }
        if ((deelnemers > 1 && over <= 1) || over == 0 || ticks() >= AFTELLEN + DUUR) {
            einde(iedereen);
        }
    }

    @Nullable
    private LivingEntity dichtstbij(Entity van, List<Entity> iedereen) {
        LivingEntity beste = null;
        double afstand = Double.MAX_VALUE;
        for (Entity e : iedereen) {
            if (e != van && e instanceof LivingEntity l && opBed(e)) {
                double d = e.distanceToSqr(van);
                if (d < afstand) {
                    afstand = d;
                    beste = l;
                }
            }
        }
        return beste;
    }

    private void valtAf(Entity e) {
        af.add(e.getUUID());
        UUID duwer = laatsteDuwer.get(e.getUUID());
        if (duwer != null && !duwer.equals(e.getUUID())) {
            geduwd.merge(duwer, 1, Integer::sum);
        }
        Component naam = e.getDisplayName();
        for (ServerPlayer p : spelers()) {
            p.sendSystemMessage(Component.translatable("gui.guhs.gezel.kussen.af", naam).withStyle(ChatFormatting.GRAY));
        }
        if (e instanceof ServerPlayer p) {
            p.sendOverlayMessage(Component.translatable("gui.guhs.gezel.kussen.jij_af").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        level().playSound(null, e.blockPosition(), SoundEvents.WOOL_FALL, SoundSource.PLAYERS, 1f, 0.8f);
    }

    private void einde(List<Entity> iedereen) {
        List<Entity> winnaars = new ArrayList<>();
        for (Entity e : iedereen) {
            if (opBed(e)) {
                winnaars.add(e);
            }
        }
        for (ServerPlayer p : spelers()) {
            boolean won = winnaars.contains(p);
            int punten = geduwd.getOrDefault(p.getUUID(), 0) * 10 + (won ? 50 : 0);
            if (punten > 0) {
                Scorebord.submit(p, BORD, punten, false);
            }
            int betaald = Muntjes.verdienDagelijks(p, "gezel_" + ID, won ? 30 : 10, 150);
            if (won) {
                Muntjes.verdienEens(p, "gezel:" + ID, 50);
                GuhAdvancements.grant(p, "px_kussengevecht_gewonnen");
                p.sendSystemMessage(Component.translatable("gui.guhs.gezel.kussen.gewonnen", punten).withStyle(ChatFormatting.GOLD));
            } else {
                p.sendSystemMessage(Component.translatable("gui.guhs.gezel.kussen.punten", punten).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            if (betaald > 0) {
                p.sendSystemMessage(Component.translatable("gui.guhs.gezel.muntjes", betaald).withStyle(ChatFormatting.GOLD));
            }
        }
        Component winTekst = winnaars.isEmpty() ? Component.translatable("gui.guhs.gezel.kussen.niemand")
                : Component.translatable("gui.guhs.gezel.kussen.winnaar", winnaars.get(0).getDisplayName());
        for (ServerPlayer p : spelers()) {
            p.sendSystemMessage(winTekst.copy().withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        klaarOp = ticks() + NA;
    }

    /**
     * A pillow hit: `naar` flies away from `van` (and a little up), with feathers and a plop. Never damage. Also used by
     * the bots and by the click of a player.
     */
    public static void duw(LivingEntity van, LivingEntity naar) {
        double dx = naar.getX() - van.getX(), dz = naar.getZ() - van.getZ();
        double len = Math.max(0.01, Math.sqrt(dx * dx + dz * dz));
        double kracht = 0.85;
        naar.push(dx / len * kracht, 0.32, dz / len * kracht);
        naar.hurtMarked = true;
        if (naar.level() instanceof ServerLevel level) {
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(
                    new ItemStack(Items.FEATHER))), naar.getX(), naar.getY() + 1, naar.getZ(), 12, 0.3, 0.3, 0.3, 0.08);
            level.playSound(null, naar.blockPosition(), DroomwezensFeature.KUSSEN.get(), SoundSource.PLAYERS, 0.9f, 0.9f + level.getRandom().nextFloat() * 0.3f);
        }
    }

    /** A player hit somebody with a left click (GezelSlice catches the attack before Guhpixel's rules cancel it). */
    boolean klap(ServerPlayer van, Entity naar) {
        if (ticks() < AFTELLEN || klaarOp >= 0 || af.contains(van.getUUID()) || !(naar instanceof LivingEntity l) || !opBed(naar)) {
            return false;
        }
        if (!(naar instanceof ServerPlayer sp && speelt(sp)) && !bots.contains(naar)) {
            return false;
        }
        int tik = duwOp.getOrDefault(van.getUUID(), 0);
        if (ticks() < tik) {
            return true;
        }
        duwOp.put(van.getUUID(), ticks() + 8);
        laatsteDuwer.put(naar.getUUID(), van.getUUID());
        duw(van, l);
        return true;
    }

    @Override
    protected void spelerWeg(ServerPlayer p, Vertrek reden) {
        af.add(p.getUUID());
    }

    @Override
    protected void einde() {
        for (GuhNpcEntity bot : bots) {
            bot.discard();
        }
        bots.clear();
    }

    @Override
    public Vec3 terugzetPlek(ServerPlayer p) {
        return arena().wereld(new Vec3(START.x, 2, START.z - 11));
    }

    /** (Tests) how many this player pushed off. */
    int geduwd(UUID id) {
        return geduwd.getOrDefault(id, 0);
    }

    static float hoek(double dx, double dz) {
        return Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-dx, dz)));
    }
}
