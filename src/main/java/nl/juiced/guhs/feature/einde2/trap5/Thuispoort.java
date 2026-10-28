package nl.juiced.guhs.feature.einde2.trap5;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import nl.juiced.guhs.feature.band.GuhVolger;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;
import nl.juiced.guhs.feature.vadskracht.VadsOpslag;

/**
 * The Thuispoort ({@code guhs:thuispoort}, DESIGN 6): a personal teleport network with no limit on its size. Every poort has
 * a name and an icon (the item in your hand, a button of its screen), you pick where to go in its screen, across
 * dimensions; your guhs on a lead and your mount go along; only you and the players you let in may use your poorten.
 * <p>
 * A jump costs vadskracht, more as the owner has more poorten ({@link Thuispoorten#kosten}). It comes from the departure
 * poort's own battery ({@link T5Getallen#THUIS_BATTERIJ} VK), which its net fills with what it has left over (a
 * {@link VadsOpslag} that takes and never gives back: other machines cannot drain it, and the net never counts on it).
 * Two practice poorten in the workshop hop to each other for free ({@link BlockEntityImpl#oefen}).
 */
public final class Thuispoort {
    private Thuispoort() {
    }

    public static class Block extends MachineBlock {
        public static final MapCodec<Block> CODEC = simpleCodec(Block::new);

        public Block(Properties p) {
            super(p);
        }

        @Override
        protected MapCodec<? extends BaseEntityBlock> codec() {
            return CODEC;
        }

        @Nullable
        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BlockEntityImpl(pos, state);
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
            super.setPlacedBy(level, pos, state, placer, stack);
            if (placer instanceof ServerPlayer player && level.getBlockEntity(pos) instanceof BlockEntityImpl p) {
                p.inschrijven(player);
            }
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer speler && level.getBlockEntity(pos) instanceof BlockEntityImpl p) {
                p.open(speler);
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static class BlockEntityImpl extends MachineBlockEntity implements VadsOpslag {
        @Nullable
        UUID id;
        private long lading;
        boolean oefen;
        /** A practice poort: where its twin stands (the other practice poort of the bay). */
        @Nullable
        BlockPos oefenTwee;

        public BlockEntityImpl(BlockPos pos, BlockState state) {
            super(E2Trap5Feature.THUISPOORT_BE.get(), pos, state, 0, 1);
        }

        public long lading() {
            return lading;
        }

        void zetLading(long vk) {
            lading = Math.max(0, Math.min(T5Getallen.THUIS_BATTERIJ, vk));
            setChanged();
        }

        /** A new poort of this player: into the network, with a name of its own ("Thuispoort 3"). */
        void inschrijven(ServerPlayer player) {
            if (oefen) {
                return;
            }
            Thuispoorten net = Thuispoorten.van(player.level().getServer());
            if (id == null) {
                id = UUID.randomUUID();
            }
            int nr = net.van(player.getUUID()).size() + 1;
            net.zet(id, player.getUUID(), GlobalPos.of(player.level().dimension(), worldPosition),
                    Component.translatable("block.guhs.thuispoort.naam_n", nr).getString(), "guhs:thuispoort");
            zetEigenaar(player.getUUID());
            sync();
        }

        @Override
        public void onLoad() {
            super.onLoad();
            // (a poort moved by a structure or loaded after a crash: the book follows the block)
            if (level instanceof ServerLevel server && id != null && eigenaar() != null && !oefen) {
                Thuispoorten net = Thuispoorten.van(server.getServer());
                Thuispoorten.Poort p = net.poort(id);
                GlobalPos hier = GlobalPos.of(server.dimension(), worldPosition);
                if (p == null) {
                    net.zet(id, eigenaar(), hier, "Thuispoort", "guhs:thuispoort");
                } else if (!p.plek.equals(hier)) {
                    net.zet(id, eigenaar(), hier, p.naam, p.icoon);
                }
            }
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            if (level instanceof ServerLevel server && id != null && !oefen) {
                Thuispoorten.van(server.getServer()).wis(id);
            }
            super.preRemoveSideEffects(pos, state);
        }

        // --- vadskracht: a battery that only takes ---

        @Override
        public long vadsInhoud() {
            return 0;
        }

        @Override
        public long vadsMax() {
            return T5Getallen.THUIS_BATTERIJ - lading;
        }

        @Override
        public long vadsLaad(long vk) {
            long erin = Math.max(0, Math.min(vk, T5Getallen.THUIS_BATTERIJ - lading));
            if (erin > 0) {
                lading += erin;
                setChanged();
            }
            return erin;
        }

        @Override
        public long vadsOntlaad(long vk) {
            return 0;
        }

        @Override
        protected boolean kanWerken() {
            return true;
        }

        @Override
        protected boolean isVol() {
            return lading >= T5Getallen.THUIS_BATTERIJ;
        }

        @Override
        protected void werk() {
        }

        @Override
        protected boolean isInvoer(int vak) {
            return false;
        }

        @Override
        protected boolean isUitvoer(int vak) {
            return false;
        }

        @Override
        public void vadsRegels(Consumer<Component> regels) {
            regels.accept(Component.translatable("block.guhs.thuispoort.lading", lading, T5Getallen.THUIS_BATTERIJ).withStyle(ChatFormatting.AQUA));
            if (level instanceof ServerLevel server && eigenaar() != null) {
                regels.accept(Component.translatable("block.guhs.thuispoort.kost", Thuispoorten.van(server.getServer()).kosten(eigenaar()))
                        .withStyle(ChatFormatting.GRAY));
            }
        }

        // --- the screen and the jump ---

        void open(ServerPlayer speler) {
            if (oefen) {
                oefenSprong(speler);
                return;
            }
            Thuispoorten net = Thuispoorten.van(speler.level().getServer());
            Thuispoorten.Poort hier = id == null ? null : net.poort(id);
            if (hier == null) {
                if (eigenaar() == null) {
                    inschrijven(speler);   // (put there by a command: the first one to use it owns it)
                    hier = net.poort(id);
                }
                if (hier == null) {
                    speler.sendOverlayMessage(Component.translatable("block.guhs.thuispoort.niet_van_jou"));
                    return;
                }
            }
            if (!net.mag(hier.eigenaar, speler.getUUID())) {
                speler.sendOverlayMessage(Component.translatable("block.guhs.thuispoort.niet_van_jou"));
                return;
            }
            T5Payloads.stuur(speler, new T5Payloads.PoortOpen(worldPosition, scherm(speler, net, hier)));
        }

        /** What the screen shows: this poort, its battery and cost, where you may go, and (owner) who is let in. */
        private CompoundTag scherm(ServerPlayer speler, Thuispoorten net, Thuispoorten.Poort hier) {
            CompoundTag d = new CompoundTag();
            d.putString("Naam", hier.naam);
            d.putString("Icoon", hier.icoon);
            d.putLong("Lading", lading);
            d.putLong("Max", T5Getallen.THUIS_BATTERIJ);
            d.putInt("Kost", net.kosten(hier.eigenaar));
            d.putBoolean("Eigenaar", hier.eigenaar.equals(speler.getUUID()));
            ListTag lijst = new ListTag();
            for (Thuispoorten.Poort p : net.bereikbaar(speler.getUUID())) {
                if (p.id.equals(hier.id)) {
                    continue;
                }
                CompoundTag c = new CompoundTag();
                c.putString("Id", p.id.toString());
                c.putString("Naam", p.naam);
                c.putString("Icoon", p.icoon);
                c.putString("Dim", p.plek.dimension().identifier().toString());
                c.putInt("X", p.plek.pos().getX());
                c.putInt("Y", p.plek.pos().getY());
                c.putInt("Z", p.plek.pos().getZ());
                c.putBoolean("Eigen", p.eigenaar.equals(speler.getUUID()));
                lijst.add(c);
            }
            d.put("Poorten", lijst);
            ListTag toe = new ListTag();
            net.toegelaten(hier.eigenaar).values().forEach(n -> {
                CompoundTag c = new CompoundTag();
                c.putString("Naam", n);
                toe.add(c);
            });
            d.put("Toegelaten", toe);
            return d;
        }

        /** An action from the screen (server side, the player stands at this poort). */
        void actie(ServerPlayer speler, int actie, String tekst) {
            if (oefen || id == null || speler.distanceToSqr(Vec3.atCenterOf(worldPosition)) > 64) {
                return;
            }
            Thuispoorten net = Thuispoorten.van(speler.level().getServer());
            Thuispoorten.Poort hier = net.poort(id);
            if (hier == null || !net.mag(hier.eigenaar, speler.getUUID())) {
                return;
            }
            boolean eigen = hier.eigenaar.equals(speler.getUUID());
            switch (actie) {
                case T5Payloads.PoortActie.SPRING -> {
                    UUID naar;
                    try {
                        naar = UUID.fromString(tekst);
                    } catch (IllegalArgumentException e) {
                        return;
                    }
                    spring(speler, net, hier, naar);
                    return;
                }
                case T5Payloads.PoortActie.ICOON -> {
                    ItemStack hand = speler.getMainHandItem();
                    if (eigen && !hand.isEmpty()) {
                        net.icoon(id, BuiltInRegistries.ITEM.getKey(hand.getItem()).toString());
                        speler.sendOverlayMessage(Component.translatable("block.guhs.thuispoort.icoon", hand.getHoverName()));
                    }
                }
                case T5Payloads.PoortActie.HERNOEM -> {
                    if (eigen && !tekst.isBlank()) {
                        net.hernoem(id, tekst.strip());
                    }
                }
                case T5Payloads.PoortActie.TOELATEN, T5Payloads.PoortActie.WEIGEREN -> {
                    if (!eigen || tekst.isBlank()) {
                        break;
                    }
                    boolean toe = actie == T5Payloads.PoortActie.TOELATEN;
                    ServerPlayer ander = speler.level().getServer().getPlayerList().getPlayerByName(tekst.strip());
                    UUID wie = ander != null ? ander.getUUID() : null;
                    if (wie == null && !toe) {
                        for (var e : net.toegelaten(hier.eigenaar).entrySet()) {
                            if (e.getValue().equalsIgnoreCase(tekst.strip())) {
                                wie = e.getKey();
                            }
                        }
                    }
                    if (wie == null || wie.equals(speler.getUUID())) {
                        speler.sendOverlayMessage(Component.translatable("block.guhs.thuispoort.wie_niet", tekst.strip()));
                        break;
                    }
                    net.laatToe(hier.eigenaar, wie, ander != null ? ander.getGameProfile().name() : tekst.strip(), toe);
                }
                default -> {
                    return;
                }
            }
            T5Payloads.stuur(speler, new T5Payloads.PoortOpen(worldPosition, scherm(speler, net, net.poort(id))));
        }

        /** The jump itself: the cost from this poort's battery, the player with their mount and their guhs on a lead. */
        private void spring(ServerPlayer speler, Thuispoorten net, Thuispoorten.Poort hier, UUID naarId) {
            Thuispoorten.Poort naar = net.poort(naarId);
            if (naar == null || !net.mag(naar.eigenaar, speler.getUUID())) {
                return;
            }
            int kost = net.kosten(naar.eigenaar);
            if (lading < kost) {
                speler.sendSystemMessage(Component.translatable("block.guhs.thuispoort.te_weinig", lading, kost).withStyle(ChatFormatting.GOLD));
                return;
            }
            ServerLevel doelLevel = speler.level().getServer().getLevel(naar.plek.dimension());
            if (doelLevel == null) {
                speler.sendSystemMessage(Component.translatable("block.guhs.thuispoort.weg").withStyle(ChatFormatting.GOLD));
                return;
            }
            doelLevel.getChunk(naar.plek.pos());   // (loads it: a poort was placed there, the chunk exists)
            if (!(doelLevel.getBlockEntity(naar.plek.pos()) instanceof BlockEntityImpl doel) || !naarId.equals(doel.id)) {
                net.wis(naarId);
                speler.sendSystemMessage(Component.translatable("block.guhs.thuispoort.weg").withStyle(ChatFormatting.GOLD));
                return;
            }
            lading -= kost;
            setChanged();
            Vec3 aankomst = Vec3.atBottomCenterOf(naar.plek.pos().above());
            verplaats(speler, doelLevel, aankomst);
            E2Trap5.meld(speler, E2Trap5.P_THUISPOORT);
        }

        /** Teleports the player, then their mount and their guhs on a lead, and seats / leashes them again next tick. */
        public static void verplaats(ServerPlayer speler, ServerLevel doelLevel, Vec3 aankomst) {
            ServerLevel van = speler.level();
            van.sendParticles(ParticleTypes.PORTAL, speler.getX(), speler.getY() + 1, speler.getZ(), 40, 0.4, 0.8, 0.4, 0.4);
            van.playSound(null, speler.blockPosition(), SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.2f);
            Entity rijdier = speler.getVehicle();
            List<Mob> aanDeLijn = new ArrayList<>();
            for (Mob m : van.getEntitiesOfClass(Mob.class, new AABB(speler.blockPosition()).inflate(T5Getallen.THUIS_LIJN),
                    m -> m.getLeashHolder() == speler)) {
                aanDeLijn.add(m);
            }
            if (rijdier != null) {
                speler.stopRiding();
            }
            speler.teleport(new TeleportTransition(doelLevel, aankomst, Vec3.ZERO, speler.getYRot(), speler.getXRot(), Set.<Relative>of(),
                    TeleportTransition.DO_NOTHING));
            Entity nieuwRijdier = null;
            if (rijdier != null && !(rijdier instanceof Player)) {
                nieuwRijdier = rijdier.teleport(new TeleportTransition(doelLevel, aankomst, Vec3.ZERO, rijdier.getYRot(), 0f, Set.<Relative>of(),
                        TeleportTransition.DO_NOTHING));
            }
            List<Mob> nieuw = new ArrayList<>();
            for (Mob m : aanDeLijn) {
                m.removeLeash();
                Entity e = m.teleport(new TeleportTransition(doelLevel, aankomst.add((m.getRandom().nextDouble() - 0.5) * 2, 0, (m.getRandom().nextDouble() - 0.5) * 2),
                        Vec3.ZERO, m.getYRot(), 0f, Set.<Relative>of(), TeleportTransition.DO_NOTHING));
                if (e instanceof Mob nm) {
                    nieuw.add(nm);
                }
            }
            Entity rij = nieuwRijdier;
            doelLevel.getServer().execute(() -> {
                ServerPlayer nu = doelLevel.getServer().getPlayerList().getPlayer(speler.getUUID());
                if (nu == null) {
                    return;
                }
                if (rij != null && rij.isAlive() && rij.level() == nu.level()) {
                    nu.startRiding(rij, true, true);
                }
                for (Mob m : nieuw) {
                    if (m.isAlive() && m.level() == nu.level()) {
                        m.setLeashedTo(nu, true);
                    }
                }
            });
            doelLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, aankomst.x, aankomst.y + 1, aankomst.z, 40, 0.4, 0.8, 0.4, 0.05);
            doelLevel.playSound(null, BlockPos.containing(aankomst), SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.4f);
        }

        /** A practice poort of the workshop: a hop to its twin, free. */
        private void oefenSprong(ServerPlayer speler) {
            if (oefenTwee == null || !(speler.level() instanceof ServerLevel server)) {
                return;
            }
            speler.sendOverlayMessage(Component.translatable("block.guhs.thuispoort.oefen"));
            verplaats(speler, server, Vec3.atBottomCenterOf(oefenTwee.above()));
        }

        @Override
        protected void clientTick() {
            if (level != null && level.getRandom().nextInt(6) == 0) {
                level.addParticle(ParticleTypes.PORTAL, worldPosition.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5), worldPosition.getY() + 1.1,
                        worldPosition.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5), 0, 0.2, 0);
            }
        }

        @Override
        protected void opslaan(ValueOutput uit) {
            uit.storeNullable("Id", UUIDUtil.CODEC, id);
            uit.putLong("Lading", lading);
            uit.putBoolean("Oefen", oefen);
            uit.storeNullable("OefenTwee", BlockPos.CODEC, oefenTwee);
        }

        @Override
        protected void laden(ValueInput in) {
            id = in.read("Id", UUIDUtil.CODEC).orElse(null);
            lading = Math.max(0, in.getLongOr("Lading", 0L));
            oefen = in.getBooleanOr("Oefen", false);
            oefenTwee = in.read("OefenTwee", BlockPos.CODEC).orElse(null);
        }
    }

    /** (for the screen texts) the dimension's name. */
    static Component dimensie(GlobalPos plek) {
        return GuhVolger.dimensie(plek.dimension());
    }
}
