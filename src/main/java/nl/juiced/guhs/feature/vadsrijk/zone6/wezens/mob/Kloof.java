package nl.juiced.guhs.feature.vadsrijk.zone6.wezens.mob;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.vadsrijk.kern.Schade;
import nl.juiced.guhs.feature.vadsrijk.kern.Spelers;
import nl.juiced.guhs.feature.vadsrijk.zone6.wezens.Z6Vfx;

/**
 * De Palmenkloven (lv 62-64): the Dadelaapje and the Gierguh (ZONE6_PLAN §5, rows 5-6).
 */
public final class Kloof {
    private Kloof() {
    }

    public static final int ZIT = 1;

    // =====================================================================================================================
    // Dadelaapje: a little monkey-guh that throws dates from a distance (red circle 0.8 s) and, when it gets a hit in, pinches a
    // few coins ("Het dadelaapje pikt 12 brons!"). Cheered up, it gives everything back.
    // =====================================================================================================================
    public static class Dadelaapje extends Z6Wezen {
        private int pauze = 50, laden = -1;
        private Vec3 plek;
        private final Map<UUID, Long> gepikt = new HashMap<>();

        public Dadelaapje(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "dadelaapje";
        }

        @Override
        protected double hpBasis() {
            return 26;
        }

        @Override
        protected double klapBasis() {
            return 5.6;
        }

        @Override
        public double aggroBereik() {
            return 12;
        }

        @Override
        public String huisdier() {
            return "dadelaapje";
        }

        @Override
        public double huisdierKans() {
            return 0.005;
        }

        /** What it pinched from whom (brons); given back on the cheer-up. */
        public long gepikt(UUID speler) {
            return gepikt.getOrDefault(speler, 0L);
        }

        @Override
        public void aanval(LivingEntity doel) {
            super.aanval(doel);
            if (doel instanceof ServerPlayer p && getRandom().nextDouble() < 0.3 && level() instanceof ServerLevel sl) {
                pik(sl, p);
            }
        }

        /** Pinches 1 % of the player's coins (at most 60 brons). */
        public long pik(ServerLevel sl, ServerPlayer p) {
            long n = Math.min(60, Math.max(1, Spelers.data(p).munten / 100));
            if (!Spelers.neemMunten(p, n)) {
                return 0;
            }
            gepikt.merge(p.getUUID(), n, Long::sum);
            p.sendOverlayMessage(Component.translatable("vadsrijk.z6wezens.dadelaapje.pikt", n).withStyle(ChatFormatting.GOLD));
            sl.playSound(null, blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.HOSTILE, 1f, 1.6f);
            return n;
        }

        @Override
        protected void opPlof() {
            super.opPlof();
            if (level() instanceof ServerLevel sl) {
                for (var e : gepikt.entrySet()) {
                    if (sl.getEntity(e.getKey()) instanceof ServerPlayer p) {
                        Spelers.geefMunten(p, e.getValue());
                        p.sendSystemMessage(Component.translatable("vadsrijk.z6wezens.dadelaapje.terug", e.getValue()).withStyle(ChatFormatting.GOLD));
                    }
                }
                gepikt.clear();
            }
        }

        @Override
        protected void doelen() {
            standaardDoelen(1.1, true);
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            LivingEntity d = getTarget();
            if (laden > 0 && --laden == 0) {
                speel("spuug");
                Z6Vfx.zand(sl, plek, 0.8);
                for (Player p : spelersRond(plek, 1.8)) {
                    klap(p, 1.0, Schade.Soort.AFSTAND, "dadelworp");
                }
                sl.playSound(null, plek.x, plek.y, plek.z, SoundEvents.SWEET_BERRY_BUSH_BREAK, SoundSource.HOSTILE, 1f, 1.2f);
            } else if (laden <= 0 && geldig(d) && --pauze <= 0 && distanceTo(d) > 3 && distanceTo(d) < 12) {
                pauze = 70 + getRandom().nextInt(30);
                laden = 16;
                plek = new Vec3(d.getX(), grond(d.position()), d.getZ());
                Z6Vfx.cirkel(sl, plek, 1.8, 16, Z6Vfx.ROOD);
            }
        }
    }

    // =====================================================================================================================
    // Gierguh: a fluffy vulture-guh circling over the gorge. It dives on whoever stands LOWEST (a red strip, 0.9 s), then sits
    // on the ground for 2 s (an easy target).
    // =====================================================================================================================
    public static class Gierguh extends Z6Wezen {
        private int duikPauze = 70, laden = -1, duikT = -1, zitT;
        private Vec3 duikVan, duikNaar;

        public Gierguh(EntityType<? extends Monster> type, Level level) {
            super(type, level);
        }

        @Override
        public String naam() {
            return "gierguh";
        }

        @Override
        public boolean vliegt() {
            return zitT <= 0;
        }

        @Override
        public boolean isNoGravity() {
            return !geplofd() && zitT <= 0;
        }

        @Override
        protected double hpBasis() {
            return 30;
        }

        @Override
        protected double klapBasis() {
            return 6.5;
        }

        @Override
        public double aggroBereik() {
            return 14;
        }

        @Override
        protected String loopAnim() {
            return "vlieg";
        }

        @Override
        protected String houdingAnim(int h) {
            return h == ZIT ? "zit" : null;
        }

        @Override
        protected double vliegHoogte() {
            return 6;
        }

        @Override
        protected Vec3 stuur(ServerLevel sl, @Nullable LivingEntity doel) {
            if (duikT >= 0 || zitT > 0) {
                return position();
            }
            if (doel == null) {
                return null;
            }
            double a = tickCount * 0.05 + getId();
            return doel.position().add(Math.cos(a) * 6, 6, Math.sin(a) * 6);
        }

        /** The lowest-standing player near it (it likes the ones down in the gorge). */
        @Nullable
        private Player laagste() {
            Player best = null;
            for (Player p : spelersRond(position(), 14)) {
                if (geldig(p) && (best == null || p.getY() < best.getY())) {
                    best = p;
                }
            }
            return best;
        }

        @Override
        public void tick() {
            super.tick();
            if (level().isClientSide() || geplofd() || !(level() instanceof ServerLevel sl)) {
                return;
            }
            if (zitT > 0) {
                if (--zitT == 0) {
                    zetHouding(0);
                    setDeltaMovement(0, 0.5, 0);
                }
                return;
            }
            if (duikT >= 0) {
                duikT++;
                double f = Math.min(1, duikT / 10.0);
                Vec3 v = duikVan.lerp(duikNaar, f);
                setPos(v.x, v.y, v.z);
                setDeltaMovement(Vec3.ZERO);
                for (Player p : spelersRond(v, 1.5)) {
                    klap(p, 1.3, Schade.Soort.MELEE, "gierduik");
                    duikT = 10;
                }
                if (duikT >= 10) {
                    duikT = -1;
                    zitT = 40;
                    zetHouding(ZIT);
                    Z6Vfx.zand(sl, position(), 1.0);
                }
                return;
            }
            if (laden > 0 && --laden == 0) {
                duikT = 0;
                duikVan = position();
                speel("aanval");
            } else if (laden <= 0 && geldig(getTarget()) && --duikPauze <= 0) {
                Player d = laagste();
                if (d == null) {
                    return;
                }
                duikPauze = 120 + getRandom().nextInt(40);
                laden = 18;
                Vec3 g = new Vec3(d.getX(), grond(d.position()), d.getZ());
                Vec3 o = new Vec3(getX(), g.y, getZ());
                duikNaar = g.add(g.subtract(o).lengthSqr() < 0.01 ? Vec3.ZERO : g.subtract(o).normalize().scale(1.5));
                Z6Vfx.lijn(sl, o, duikNaar, 1.6, 18, Z6Vfx.ROOD);
                sl.playSound(null, blockPosition(), SoundEvents.PARROT_AMBIENT, SoundSource.HOSTILE, 1f, 0.5f);
            }
        }
    }
}
