package nl.juiced.guhs.feature.vadsrijk.momenten;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import nl.juiced.guhs.entity.GuhClothes;
import nl.juiced.guhs.feature.titels.Titels;
import nl.juiced.guhs.feature.vadsrijk.economie.Cosmetica;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoSpul;
import nl.juiced.guhs.feature.vadsrijk.economie.EcoUtil;
import nl.juiced.guhs.feature.vadsrijk.economie.Winkels;
import nl.juiced.guhs.feature.vadsrijk.kern.Gebieden;
import nl.juiced.guhs.feature.vadsrijk.kern.Haken;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.kern.Vadsrijk;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.Affix;
import nl.juiced.guhs.feature.vadsrijk.kern.mob.VadsrijkMob;
import nl.juiced.guhs.feature.vadsrijk.wezens.WezensRegistry;
import nl.juiced.guhs.registry.ModSounds;

/**
 * The travelling caravan (VOORSTEL_EPISCHER_ZONE1 idea 18): a snail cart with Kruimel the trader guh drives Haven -> Zwamzoet ->
 * Sporenwoud and back ({@link KaravaanRoute}, {@link KaravaanStaat}: it always travels, the entities only exist near players).
 * <ul>
 *   <li>Shop {@value #WINKEL} (economie {@link Winkels} API): rare goods whose stock rotates every 20 minutes (Regenboogdrankje,
 *       a cheap Herrolknabbel, two of the Vadsrijk guh clothes, a deco piece or verbetersteentjes).</li>
 *   <li>Attacks: every 20-30 min, when players are near and the cart is out of town, 4-8 creatures of the area attack. The cart
 *       stops; helpers get Vadsmunten (+ a chance at a rare item) and a thank-you ({@link KaravaanBeloning}); unhelped attackers
 *       give up after 2 min. Three helped attacks: the title Karavaanvriend. The cart is never hurt.</li>
 * </ul>
 */
public final class Karavaan {
    public static final String WINKEL = "karavaan";
    public static final String TAG = "vr_mom_karavaan";
    public static final String TITEL = "vr_karavaanvriend";
    public static final int TITEL_NA = 3;
    static final long ROTATIE = 20L * 60 * 20;
    /** Players this close (flat) count as "near" for an attack; this close the cart is shown. */
    static final double DICHTBIJ = 40, ZICHTBAAR = 112;

    @Nullable
    static KaravaanStaat staat;
    @Nullable
    static KaravaanSlak slak;
    @Nullable
    static HandelGuh guh;
    @Nullable
    static Aanval aanval;
    private static long rotatie = -1;
    private static double vloerY = Double.NaN;

    /** One attack: the attackers still out there, who helped how much. */
    static final class Aanval {
        final Set<UUID> levend = new HashSet<>();
        final Set<UUID> alle = new HashSet<>();
        final Map<UUID, Integer> geholpen = new LinkedHashMap<>();
        int totaal, verslagen;
        final boolean woud;

        Aanval(boolean woud) {
            this.woud = woud;
        }
    }

    private Karavaan() {
    }

    static void init() {
        Titels.registreer(new Titels.Titel(TITEL, "gui.guhs.titels.naam." + TITEL, ChatFormatting.GOLD, "guhs:vr_mom_regenboogdrankje",
                p -> Cosmetica.heeftTitel(p, TITEL)));
        Winkels.registreerExtra(maakWinkel(0));
        Haken.VERSLAGEN.add(Karavaan::verslagen);
    }

    static void wis() {
        staat = null;
        slak = null;
        guh = null;
        aanval = null;
        rotatie = -1;
        vloerY = Double.NaN;
    }

    public static KaravaanStaat staat(RandomSource r) {
        if (staat == null) {
            staat = new KaravaanStaat(KaravaanRoute.ZONE1, r);
        }
        return staat;
    }

    // =====================================================================================================================
    // ticking
    // =====================================================================================================================

    static void tick(MinecraftServer server) {
        ServerLevel sl = Vadsrijk.level(server);
        if (sl == null) {
            return;
        }
        KaravaanStaat st = staat(sl.getRandom());
        winkel(sl.getGameTime());
        Vec3 pos = st.route().op(st.s());
        boolean dichtbij = spelerBinnen(sl, pos, DICHTBIJ), zichtbaar = spelerBinnen(sl, pos, ZICHTBAAR);
        switch (st.tick(dichtbij, magAanval(st, pos), sl.getRandom())) {
            case AANVAL_BEGINT -> startAanval(sl, pos);
            case AANVAL_OPGEGEVEN -> geefOp(sl);
            case AANGEKOMEN -> {
                if (st.stop() != null && zichtbaar) {
                    zeg(sl, pos, 48, "vadsrijk.momenten.karavaan.aankomst." + st.stop());
                }
            }
            default -> {
            }
        }
        if (aanval != null) {
            tickAanval(sl);
        }
        if (zichtbaar && sl.hasChunkAt(BlockPos.containing(pos))) {
            toon(sl, st, pos);
        } else {
            verberg();
            if (aanval != null) {   // nobody left to see it: the attackers wander off quietly
                ruimAanvallersOp(sl, false);
                aanval = null;
                st.aanvalGewonnen(sl.getRandom());
            }
        }
    }

    private static boolean spelerBinnen(ServerLevel sl, Vec3 pos, double r) {
        for (ServerPlayer p : sl.players()) {
            double dx = p.getX() - pos.x, dz = p.getZ() - pos.z;
            if (!p.isSpectator() && dx * dx + dz * dz < r * r && Math.abs(p.getY() - pos.y) < 48) {
                return true;
            }
        }
        return false;
    }

    /** Not in a town, and not right at a stop. */
    static boolean magAanval(KaravaanStaat st, Vec3 pos) {
        Gebieden.Gebied g = Gebieden.op(pos.x, pos.z);
        if (g != null && g.stad()) {
            return false;
        }
        for (KaravaanRoute.Stop s : st.route().stops()) {
            Vec3 p = st.route().punten().get(s.index());
            if (p.distanceToSqr(pos.x, p.y, pos.z) < 24 * 24) {
                return false;
            }
        }
        return true;
    }

    // ---- the entities ---------------------------------------------------------------------------------------------------------

    private static void toon(ServerLevel sl, KaravaanStaat st, Vec3 pos) {
        Vec3 dir = st.route().richting(st.s(), st.richting());
        float doelYaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        double doelY = vloer(sl, pos);
        if (slak == null || slak.isRemoved() || slak.level() != sl) {
            slak = MomentenRegistry.KARAVAAN.get().create(sl, EntitySpawnReason.EVENT);
            if (slak == null) {
                return;
            }
            vloerY = doelY;
            slak.snapTo(pos.x, doelY, pos.z, doelYaw, 0);
            sl.addFreshEntity(slak);
        }
        if (guh == null || guh.isRemoved() || guh.level() != sl) {
            guh = MomentenRegistry.HANDELAAR.get().create(sl, EntitySpawnReason.EVENT);
            if (guh != null) {
                guh.kleed();
                guh.snapTo(pos.x, doelY + 1, pos.z, doelYaw, 0);
                sl.addFreshEntity(guh);
            }
        }
        if (guh != null && guh.getVehicle() != slak) {
            guh.startRiding(slak, true, false);
        }
        vloerY = Double.isNaN(vloerY) ? doelY : vloerY + Mth.clamp(doelY - vloerY, -0.15, 0.15);
        float yaw = Mth.approachDegrees(slak.getYRot(), doelYaw, 4f);
        slak.setPos(pos.x, vloerY, pos.z);
        slak.setYRot(yaw);
        slak.zet(st.fase() == KaravaanStaat.Fase.RIJDEN, st.fase() == KaravaanStaat.Fase.AANVAL);
    }

    private static void verberg() {
        if (guh != null) {
            guh.discard();
            guh = null;
        }
        if (slak != null) {
            slak.discard();
            slak = null;
        }
        vloerY = Double.NaN;
    }

    /** The floor under the road point: the nearest standing spot within 5 blocks of the road's y. */
    static double vloer(ServerLevel sl, Vec3 pos) {
        BlockPos basis = BlockPos.containing(pos);
        for (int d = 0; d <= 5; d++) {
            for (int s : new int[] {1, -1}) {
                BlockPos b = basis.above(s * d);
                if (staanOp(sl, b)) {
                    return b.getY();
                }
                if (d == 0) {
                    break;
                }
            }
        }
        return pos.y;
    }

    private static boolean staanOp(ServerLevel sl, BlockPos b) {
        BlockState onder = sl.getBlockState(b.below());
        return !onder.getCollisionShape(sl, b.below()).isEmpty() && sl.getBlockState(b).getCollisionShape(sl, b).isEmpty()
                && sl.getFluidState(b).isEmpty() && sl.getBlockState(b.above()).getCollisionShape(sl, b.above()).isEmpty();
    }

    // ---- the shop -------------------------------------------------------------------------------------------------------------

    /** A click on the cart or the trader. */
    static void klik(ServerPlayer p) {
        if (aanval != null) {
            p.sendSystemMessage(regel("vadsrijk.momenten.karavaan.help_eerst"));
            return;
        }
        winkel(p.level().getGameTime());
        Winkels.open(p, WINKEL);
    }

    private static void winkel(long gameTime) {
        long r = gameTime / ROTATIE;
        if (r != rotatie) {
            rotatie = r;
            Winkels.registreerExtra(maakWinkel(r));
        }
    }

    /** The stock of rotation r (the same for everyone): always the potion + a herrolknabbel, 2 guh clothes, 1 extra. */
    static Winkels.Winkel maakWinkel(long r) {
        RandomSource rnd = RandomSource.create(r * 7919L + 31L);
        List<Winkels.Waar> w = new ArrayList<>();
        w.add(item("regenboogdrankje", 250, () -> new ItemStack(MomentenRegistry.REGENBOOGDRANKJE.get())));
        w.add(item("herrolknabbel", 450, () -> new ItemStack(EcoSpul.HERROLKNABBEL.get())));
        List<GuhClothes> kleren = new ArrayList<>(Cosmetica.KLEDING);
        for (int i = 0; i < 2 && !kleren.isEmpty(); i++) {
            GuhClothes c = kleren.remove(rnd.nextInt(kleren.size()));
            w.add(new Winkels.Waar(c.id(), Winkels.Soort.POST, 250, () -> Cosmetica.kledingItem(c), ""));
        }
        if (rnd.nextBoolean() && !EcoSpul.DECO.isEmpty()) {
            var b = EcoSpul.DECO.get(rnd.nextInt(EcoSpul.DECO.size()));
            w.add(new Winkels.Waar(b.getId().getPath(), Winkels.Soort.POST, 600, () -> new ItemStack(b.get()), ""));
        } else {
            w.add(item("verbetersteentje", 40, () -> new ItemStack(EcoSpul.VERBETERSTEENTJE.get())));
        }
        return new Winkels.Winkel(WINKEL, false, List.copyOf(w));
    }

    private static Winkels.Waar item(String id, long prijs, Supplier<ItemStack> s) {
        return new Winkels.Waar(id, Winkels.Soort.ITEM, prijs, s, "");
    }

    // ---- the attack -----------------------------------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    static void startAanval(ServerLevel sl, Vec3 pos) {
        RandomSource r = sl.getRandom();
        boolean woud = pos.x < -60;
        List<DeferredHolder<EntityType<?>, ? extends EntityType<? extends VadsrijkMob>>> soorten = woud
                ? List.of(WezensRegistry.HOEDJESBOL, WezensRegistry.SPORENPUFFER, WezensRegistry.MOSGEEST)
                : List.of(WezensRegistry.KLODDER, WezensRegistry.STUITERKLODDER, WezensRegistry.BUBBELKLODDER);
        int minLvl = woud ? 6 : 2, maxLvl = woud ? 9 : 5;
        int spelers = 0;
        for (ServerPlayer p : sl.players()) {
            if (!p.isSpectator() && p.distanceToSqr(pos) < DICHTBIJ * DICHTBIJ) {
                spelers++;
            }
        }
        int n = Mth.clamp(3 + spelers + r.nextInt(3), 4, 8);
        Aanval a = new Aanval(woud);
        aanval = a;
        double y0 = vloer(sl, pos);
        for (int i = 0; i < n; i++) {
            double hoek = (i + r.nextDouble() * 0.6) / n * Math.PI * 2, d = 7 + r.nextDouble() * 4;
            Vec3 plek = new Vec3(pos.x + Math.cos(hoek) * d, y0, pos.z + Math.sin(hoek) * d);
            if (!sl.hasChunkAt(BlockPos.containing(plek))) {
                continue;
            }
            VadsrijkMob m = soorten.get(r.nextInt(soorten.size())).get().create(sl, EntitySpawnReason.EVENT);
            if (m == null) {
                continue;
            }
            double y = vloer(sl, plek);
            m.snapTo(plek.x, y, plek.z, (float) Math.toDegrees(Math.atan2(plek.x - pos.x, pos.z - plek.z)), 0);
            m.stelIn(minLvl + r.nextInt(maxLvl - minLvl + 1), null, false, EnumSet.noneOf(Affix.class));
            m.addTag(TAG);
            a.levend.add(m.getUUID());
            a.alle.add(m.getUUID());
            sl.addFreshEntity(m);
            sl.sendParticles(ParticleTypes.POOF, plek.x, y + 0.5, plek.z, 8, 0.3, 0.3, 0.3, 0.02);
        }
        a.totaal = a.alle.size();
        if (a.totaal == 0) {
            aanval = null;
            staat(r).aanvalGewonnen(r);
            return;
        }
        zeg(sl, pos, 64, woud ? "vadsrijk.momenten.karavaan.aanval.woud" : "vadsrijk.momenten.karavaan.aanval.weide");
        sl.playSound(null, BlockPos.containing(pos), ModSounds.GUH_HURT.get(), SoundSource.NEUTRAL, 1.2f, 1.4f);
        sl.playSound(null, BlockPos.containing(pos), SoundEvents.BELL_BLOCK, SoundSource.NEUTRAL, 1.5f, 1.2f);
    }

    /** kern's defeat hook: an attacker was cheered up; everyone who helped with it is a helper of the caravan. */
    static void verslagen(Haken.Verslagen v) {
        Aanval a = aanval;
        if (a == null || !a.levend.remove(v.mob().getUUID())) {
            return;
        }
        a.verslagen++;
        for (ServerPlayer p : v.helpers()) {
            a.geholpen.merge(p.getUUID(), 1, Integer::sum);
        }
    }

    private static void tickAanval(ServerLevel sl) {
        Aanval a = aanval;
        if (a == null) {
            return;
        }
        Vec3 kar = slak != null ? slak.position() : staat(sl.getRandom()).route().op(staat.s());
        a.levend.removeIf(id -> {
            Entity e = sl.getEntity(id);
            return e == null || e.isRemoved() || (e instanceof VadsrijkMob m && m.geplofd());
        });
        if (a.levend.isEmpty()) {
            einde(sl, kar);
            return;
        }
        if (sl.getGameTime() % 40 == 0) {   // attackers without a target go for the cart
            for (UUID id : a.levend) {
                if (sl.getEntity(id) instanceof VadsrijkMob m && m.getTarget() == null && m.distanceToSqr(kar) > 9) {
                    m.getNavigation().moveTo(kar.x, kar.y, kar.z, 1.0);
                }
            }
        }
    }

    /** All attackers are gone: thank and reward the helpers, drive on. */
    private static void einde(ServerLevel sl, Vec3 kar) {
        Aanval a = aanval;
        aanval = null;
        RandomSource r = sl.getRandom();
        staat(r).aanvalGewonnen(r);
        if (a == null) {
            return;
        }
        beloon(sl, a, kar);
    }

    /** The attackers had enough (2 min): they hop off; helpers so far still get their share. */
    static void geefOp(ServerLevel sl) {
        Aanval a = aanval;
        if (a == null) {
            return;
        }
        Vec3 kar = slak != null ? slak.position() : staat(sl.getRandom()).route().op(staat.s());
        ruimAanvallersOp(sl, true);
        aanval = null;
        if (a.verslagen > 0) {
            beloon(sl, a, kar);
        } else {
            zeg(sl, kar, 64, "vadsrijk.momenten.karavaan.opgegeven");
        }
    }

    private static void ruimAanvallersOp(ServerLevel sl, boolean poef) {
        Aanval a = aanval;
        if (a == null) {
            return;
        }
        for (UUID id : a.levend) {
            Entity e = sl.getEntity(id);
            if (e != null) {
                if (poef) {
                    sl.sendParticles(ParticleTypes.POOF, e.getX(), e.getY() + 0.5, e.getZ(), 10, 0.3, 0.3, 0.3, 0.03);
                }
                e.discard();
            }
        }
        a.levend.clear();
    }

    private static void beloon(ServerLevel sl, Aanval a, Vec3 kar) {
        Map<UUID, KaravaanBeloning.Beloning> uit = KaravaanBeloning.verdeel(a.geholpen, a.totaal, a.verslagen, sl.getRandom());
        boolean iemand = false;
        for (Map.Entry<UUID, KaravaanBeloning.Beloning> e : uit.entrySet()) {
            if (!(sl.getEntity(e.getKey()) instanceof ServerPlayer p) || p.distanceToSqr(kar) > 96 * 96) {
                continue;
            }
            iemand = true;
            geef(p, e.getValue());
        }
        if (iemand) {
            sl.playSound(null, BlockPos.containing(kar), ModSounds.GUH_HAPPY.get(), SoundSource.NEUTRAL, 1.2f, 1.1f);
            sl.sendParticles(ParticleTypes.HEART, kar.x, kar.y + 2.2, kar.z, 8, 0.6, 0.3, 0.6, 0.02);
        } else {
            zeg(sl, kar, 64, "vadsrijk.momenten.karavaan.opgegeven");
        }
    }

    /** One helper's reward: coins, maybe a rare item, the thank-you, and the title after the third helped attack. */
    static void geef(ServerPlayer p, KaravaanBeloning.Beloning b) {
        Spelers.geefMunten(p, b.munten());
        int n = p.getRandom().nextInt(3);
        p.sendSystemMessage(regel("vadsrijk.momenten.karavaan.dank." + n, p.getDisplayName()));
        p.sendSystemMessage(Component.translatable("vadsrijk.momenten.karavaan.beloning", Spelers.munten(b.munten())).withStyle(ChatFormatting.GOLD));
        if (b.zeldzaam() != null) {
            ItemStack s = zeldzaam(b.zeldzaam());
            if (!s.isEmpty()) {
                EcoUtil.geefOfBewaar(p, s.copy());
                p.sendSystemMessage(Component.translatable("vadsrijk.momenten.karavaan.zeldzaam", s.getHoverName()).withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        }
        CompoundTag d = Spelers.data(p).extra("momenten");
        int keer = d.getIntOr("karavaan_geholpen", 0) + 1;
        d.putInt("karavaan_geholpen", keer);
        Spelers.opgeslagen(p);
        if (keer >= TITEL_NA && Cosmetica.geefTitel(p, TITEL)) {
            p.sendSystemMessage(Component.translatable("vadsrijk.momenten.karavaan.titel").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        }
    }

    static ItemStack zeldzaam(String id) {
        return switch (id) {
            case "regenboogdrankje" -> new ItemStack(MomentenRegistry.REGENBOOGDRANKJE.get());
            case "herrolknabbel" -> new ItemStack(EcoSpul.HERROLKNABBEL.get());
            case "verbetersteentjes" -> new ItemStack(EcoSpul.VERBETERSTEENTJE.get(), 3);
            default -> ItemStack.EMPTY;
        };
    }

    /** Is this entity one of the current attack's creatures? (others with the tag are leftovers: removed when they load) */
    static boolean hoortBij(Entity e) {
        Aanval a = aanval;
        return a != null && a.alle.contains(e.getUUID());
    }

    // ---- talking --------------------------------------------------------------------------------------------------------------

    static Component regel(String key, Object... args) {
        return Component.translatable("vadsrijk.momenten.karavaan.zegt", Component.translatable("vadsrijk.momenten.karavaan.handelaar"),
                Component.translatable(key, args)).withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    private static void zeg(ServerLevel sl, Vec3 pos, double r, String key) {
        Component c = regel(key);
        for (ServerPlayer p : sl.players()) {
            if (p.distanceToSqr(pos) < r * r) {
                p.sendSystemMessage(c);
            }
        }
    }

    // ---- dev --------------------------------------------------------------------------------------------------------------

    /** Dev: put the caravan on the road nearest to the player, driving away from Haven. */
    static void hier(ServerPlayer p) {
        KaravaanStaat st = staat(p.getRandom());
        if (aanval != null && p.level() instanceof ServerLevel sl) {
            ruimAanvallersOp(sl, false);
            aanval = null;
        }
        st.zet(st.route().dichtstbij(p.getX(), p.getZ()), st.richting());
        verberg();
    }

    /** Dev: start an attack now (wherever the cart is, also in town). */
    static boolean aanvalNu(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel sl) || !Vadsrijk.in(p) || aanval != null) {
            return false;
        }
        KaravaanStaat st = staat(sl.getRandom());
        st.zet(st.s(), st.richting());
        st.aanvalNu();
        st.forceerAanval();
        return true;
    }

    static String info() {
        KaravaanStaat st = staat;
        if (st == null) {
            return "karavaan: nog niet gestart";
        }
        Vec3 p = st.route().op(st.s());
        return String.format("karavaan: %s s=%.0f/%.0f richting=%d pos=(%.0f %.0f %.0f) stop=%s timer=%d aanvalPauze=%ds aanval=%s", st.fase(),
                st.s(), st.route().lengte(), st.richting(), p.x, p.y, p.z, st.stop(), st.timer(), st.aanvalPauze() / 20,
                aanval == null ? "-" : aanval.verslagen + "/" + aanval.totaal);
    }
}
