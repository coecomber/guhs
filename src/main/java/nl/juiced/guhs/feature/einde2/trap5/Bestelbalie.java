package nl.juiced.guhs.feature.einde2.trap5;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import nl.juiced.guhs.block.entity.BankGuhBlockEntity;
import nl.juiced.guhs.feature.bank.BankAdressen;
import nl.juiced.guhs.feature.bank.BankFeature;
import nl.juiced.guhs.feature.bank.BankSleutelItem;
import nl.juiced.guhs.feature.techmachine.Bouwtekening;
import nl.juiced.guhs.feature.techmachine.KnutselmachineBlockEntity;
import nl.juiced.guhs.feature.vadskracht.Kisten;
import nl.juiced.guhs.feature.vadskracht.MachineBlock;
import nl.juiced.guhs.feature.vadskracht.MachineBlockEntity;

/**
 * The Bestelbalie ({@code guhs:bestelbalie}) and its {@link BestelguhEntity} (DESIGN 6). Next to your Knutselmachines, on
 * vadskracht, linked with the Banksleutel to a Bank Guh. The Guhterminal of the same bank (or the balie itself, or the guh)
 * shows everything the machines within {@link T5Getallen#BALIE_BEREIK} blocks can make; you order an amount, and the
 * Bestelguh plans it ({@link Bestelplan}: intermediate products first), takes the ingredients out of the bank, fills the
 * machines, collects what they make (intermediates back into the bank, to be used in the next step) and puts the result in
 * the bank. You see him run between the machines and show a receipt at the end. No stock rules.
 * <p>
 * Nothing is ever lost: what the bank refuses (it is full of it) falls out at the balie; an order that cannot go on for
 * {@link T5Getallen#BALIE_GEDULD} ticks (no vadskracht, a missing ingredient, a machine gone) is given up and what was moved
 * stays where it is (in the bank or in the machine's pool, which you can empty yourself).
 */
public final class Bestelbalie {
    /** The loaded balies per bank (for the terminal's list: no block scan). */
    static final Map<UUID, Set<GlobalPos>> PER_BANK = new ConcurrentHashMap<>();

    private Bestelbalie() {
    }

    /** The loaded balies linked to this bank. */
    static List<BlockEntityImpl> van(MinecraftServer server, UUID bank) {
        List<BlockEntityImpl> uit = new ArrayList<>();
        Set<GlobalPos> alle = PER_BANK.get(bank);
        if (alle == null) {
            return uit;
        }
        for (GlobalPos g : List.copyOf(alle)) {
            ServerLevel level = server.getLevel(g.dimension());
            if (level != null && level.isLoaded(g.pos()) && level.getBlockEntity(g.pos()) instanceof BlockEntityImpl b && bank.equals(b.bank)) {
                uit.add(b);
            }
        }
        return uit;
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
        protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
            if (!stack.is(BankFeature.BANK_SLEUTEL.get())) {
                return InteractionResult.TRY_WITH_EMPTY_HAND;
            }
            if (player instanceof ServerPlayer speler && level.getBlockEntity(pos) instanceof BlockEntityImpl b) {
                b.sleutel(speler, stack);
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player instanceof ServerPlayer speler && level.getBlockEntity(pos) instanceof BlockEntityImpl b) {
                if (player.isSecondaryUseActive() && b.bezigMetBestelling() && level instanceof ServerLevel server) {
                    b.stop(server, Component.translatable("block.guhs.bestelbalie.geannuleerd"));
                } else {
                    b.openLijst(speler);
                }
            }
            return InteractionResult.SUCCESS;
        }
    }

    public static class BlockEntityImpl extends MachineBlockEntity {
        @Nullable
        private UUID bank;
        boolean oefen;
        @Nullable
        private UUID guhId;
        private int geenGuh;

        // --- the order ---
        private final List<Bestelplan.Taak> taken = new ArrayList<>();
        private int taakNr, keerOver, gemaakt;
        private long laatsteVooruit;
        @Nullable
        private UUID besteller;
        private ItemStack bestelling = ItemStack.EMPTY;
        private int bestelAantal;
        @Nullable
        private ItemStack wachtOp;

        public BlockEntityImpl(BlockPos pos, BlockState state) {
            super(E2Trap5Feature.BESTELBALIE_BE.get(), pos, state, T5Getallen.BESTELBALIE, 1);
        }

        @Nullable
        public UUID bank() {
            return bank;
        }

        public boolean bezigMetBestelling() {
            return !taken.isEmpty();
        }

        @Override
        public void onLoad() {
            super.onLoad();
            meldAdres(true);
        }

        @Override
        public void setRemoved() {
            super.setRemoved();
            meldAdres(false);
        }

        private void meldAdres(boolean erbij) {
            if (!(level instanceof ServerLevel server) || bank == null) {
                return;
            }
            GlobalPos g = GlobalPos.of(server.dimension(), worldPosition);
            if (erbij) {
                PER_BANK.computeIfAbsent(bank, k -> ConcurrentHashMap.newKeySet()).add(g);
            } else {
                Set<GlobalPos> s = PER_BANK.get(bank);
                if (s != null) {
                    s.remove(g);
                }
            }
        }

        void sleutel(ServerPlayer player, ItemStack sleutel) {
            UUID id = BankSleutelItem.bank(sleutel);
            if (id == null) {
                player.sendOverlayMessage(Component.translatable("block.guhs.bestelbalie.sleutel_leeg"));
                return;
            }
            meldAdres(false);
            bank = id;
            meldAdres(true);
            sync();
            player.sendOverlayMessage(Component.translatable("block.guhs.bestelbalie.gekoppeld"));
            level.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6f, 1.3f);
        }

        // =================================================================================================================
        // the list and the order
        // =================================================================================================================

        /** What the machines around this balie make. */
        List<ItemStack> kan(ServerLevel level) {
            return Bestelplan.kan(Bestelplan.recepten(level, worldPosition));
        }

        /** A player clicks the balie or its guh: the order list (or why there is none). */
        void openLijst(ServerPlayer player) {
            if (oefen) {
                player.sendSystemMessage(Component.translatable("block.guhs.bestelbalie.oefen").withStyle(ChatFormatting.AQUA));
                return;
            }
            if (!(level instanceof ServerLevel server)) {
                return;
            }
            if (bank == null) {
                player.sendOverlayMessage(Component.translatable("block.guhs.bestelbalie.los"));
                return;
            }
            T5Payloads.stuurLijst(player, worldPosition, false, List.of(this), bank);
            if (kan(server).isEmpty()) {
                player.sendOverlayMessage(Component.translatable("block.guhs.bestelbalie.geen_machines", T5Getallen.BALIE_BEREIK));
            }
        }

        /** An order of {@code aantal} of {@code wat} for this player: plans it and starts. False: refused (the player was told why). */
        boolean bestel(ServerPlayer player, ItemStack wat, int aantal) {
            if (!(level instanceof ServerLevel server) || bank == null) {
                return false;
            }
            if (!taken.isEmpty()) {
                player.sendSystemMessage(Component.translatable("block.guhs.bestelbalie.al_bezig", bestelAantal, bestelling.getHoverName())
                        .withStyle(ChatFormatting.GOLD));
                return false;
            }
            if (!heeftKracht()) {
                player.sendSystemMessage(Component.translatable("block.guhs.bestelbalie.slaapt").withStyle(ChatFormatting.GOLD));
                return false;
            }
            BankGuhBlockEntity b = BankAdressen.zoek(server.getServer(), bank, true);
            if (b == null) {
                player.sendSystemMessage(Component.translatable("block.guhs.guhterminal.bank_weg").withStyle(ChatFormatting.GOLD));
                return false;
            }
            Bestelplan.Uitkomst plan = Bestelplan.maak(Bestelplan.recepten(server, worldPosition), b.getStorage(), wat, aantal);
            if (!plan.gelukt()) {
                player.sendSystemMessage((plan.teVeel() ? Component.translatable("block.guhs.bestelbalie.te_veel", T5Getallen.BALIE_MAX_KEER)
                        : Component.translatable("block.guhs.bestelbalie.mist", plan.mist() == null ? 0 : plan.mist().getCount(),
                        plan.mist() == null ? Component.empty() : plan.mist().getHoverName())).withStyle(ChatFormatting.GOLD));
                return false;
            }
            taken.clear();
            taken.addAll(plan.taken());
            taakNr = 0;
            keerOver = taken.get(0).keer();
            gemaakt = 0;
            besteller = player.getUUID();
            bestelling = wat.copyWithCount(1);
            bestelAantal = aantal;
            wachtOp = null;
            laatsteVooruit = server.getGameTime();
            sync();
            player.sendSystemMessage(Component.translatable("block.guhs.bestelbalie.aangenomen", aantal, wat.getHoverName(), taken.size())
                    .withStyle(ChatFormatting.AQUA));
            server.playSound(null, worldPosition, SoundEvents.VILLAGER_YES, SoundSource.BLOCKS, 0.6f, 1.6f);
            return true;
        }

        // =================================================================================================================
        // the work (every 5 ticks while there is an order and vadskracht)
        // =================================================================================================================

        @Override
        protected boolean kanWerken() {
            return true;
        }

        @Override
        protected boolean isVol() {
            if (level instanceof ServerLevel server && Math.floorMod(server.getGameTime() + worldPosition.asLong(), 40L) == 0) {
                zorgVoorGuh(server);
            }
            return wachtOp != null;
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
        protected void werk() {
            if (!(level instanceof ServerLevel server) || taken.isEmpty() || Math.floorMod(server.getGameTime(), 5L) != 0) {
                return;
            }
            if (server.getGameTime() - laatsteVooruit > T5Getallen.BALIE_GEDULD) {
                stop(server, wachtOp != null ? Component.translatable("block.guhs.bestelbalie.opgegeven.mist", wachtOp.getHoverName())
                        : Component.translatable("block.guhs.bestelbalie.opgegeven"));
                return;
            }
            BankGuhBlockEntity b = bank == null ? null : BankAdressen.zoek(server.getServer(), bank, true);
            if (b == null) {
                return;
            }
            BankAdressen.houdWakker(b);
            Bestelplan.Taak taak = taken.get(taakNr);
            if (!(server.isLoaded(taak.machine()) && server.getBlockEntity(taak.machine()) instanceof KnutselmachineBlockEntity km)
                    || km.tekening() == null || !ItemStack.isSameItemSameComponents(km.tekening().resultaat(), taak.resultaat())) {
                stop(server, Component.translatable("block.guhs.bestelbalie.machine_weg"));
                return;
            }
            Bouwtekening t = km.tekening();
            BestelguhEntity guh = guh(server);
            if (guh != null) {
                guh.ren(naast(server, taak.machine()));
            }
            // 1. what the machine made: into the bank (what the bank refuses falls out here)
            while (true) {
                ItemStack uit = Kisten.neem(km.handler(null), s -> true, 64);
                if (uit.isEmpty()) {
                    break;
                }
                if (ItemStack.isSameItemSameComponents(uit, taak.resultaat())) {
                    gemaakt += uit.getCount();
                }
                ItemStack rest = b.getStorage().insert(uit);
                if (!rest.isEmpty()) {
                    Containers.dropItemStack(server, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, rest);
                }
                laatsteVooruit = server.getGameTime();
            }
            // 2. one more craft's worth into its pool, when it has nothing waiting and the bank has all of it
            if (keerOver > 0 && !km.mist(t).isEmpty()) {
                ItemStack mist = null;
                for (ItemStack ing : t.ingredienten()) {
                    int tekort = ing.getCount() - km.inVoorraad(ing);
                    if (tekort > 0 && b.getStorage().count(ing) < tekort) {
                        mist = ing.copyWithCount(tekort);
                        break;
                    }
                }
                if (mist == null) {
                    for (ItemStack ing : t.ingredienten()) {
                        int tekort = ing.getCount() - km.inVoorraad(ing);
                        if (tekort <= 0) {
                            continue;
                        }
                        ItemStack genomen = b.getStorage().extract(ing, tekort);
                        ItemStack rest = Kisten.stop(km.handler(null), genomen);
                        if (!rest.isEmpty()) {
                            rest = b.getStorage().insert(rest);
                            if (!rest.isEmpty()) {
                                Containers.dropItemStack(server, worldPosition.getX() + 0.5, worldPosition.getY() + 1.1, worldPosition.getZ() + 0.5, rest);
                            }
                        }
                    }
                    keerOver--;
                    wachtOp = null;
                    laatsteVooruit = server.getGameTime();
                    server.playSound(null, taak.machine(), SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 0.5f, 1.2f);
                } else if (wachtOp == null || !ItemStack.isSameItemSameComponents(wachtOp, mist)) {
                    wachtOp = mist;
                    ServerPlayer p = T5Machine.speler(server, besteller);
                    if (p != null) {
                        p.sendOverlayMessage(Component.translatable("block.guhs.bestelbalie.wacht", mist.getCount(), mist.getHoverName()));
                    }
                }
            }
            // 3. this step done: on to the next, or the receipt
            int per = Math.max(1, taak.resultaat().getCount());
            if (keerOver <= 0 && gemaakt >= taak.keer() * per) {
                taakNr++;
                gemaakt = 0;
                if (taakNr >= taken.size()) {
                    klaar(server);
                } else {
                    keerOver = taken.get(taakNr).keer();
                }
                sync();
            }
        }

        /** A spot next to a machine where the guh can stand. */
        private BlockPos naast(ServerLevel server, BlockPos machine) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos p = machine.relative(d);
                if (server.getBlockState(p).getCollisionShape(server, p).isEmpty() && server.getBlockState(p.above()).getCollisionShape(server, p.above()).isEmpty()) {
                    return p;
                }
            }
            return machine.above();
        }

        private void klaar(ServerLevel server) {
            ServerPlayer p = T5Machine.speler(server, besteller);
            if (p != null) {
                p.sendSystemMessage(Component.translatable("block.guhs.bestelbalie.bonnetje", bestelAantal, bestelling.getHoverName()).withStyle(ChatFormatting.GREEN));
                if (!oefen) {
                    E2Trap5.meld(p, E2Trap5.P_BESTEL);
                }
            }
            BestelguhEntity guh = guh(server);
            if (guh != null) {
                guh.ren(null);
                guh.bonnetje();
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, guh.getX(), guh.getY() + 1.4, guh.getZ(), 8, 0.3, 0.3, 0.3, 0);
            }
            server.playSound(null, worldPosition, SoundEvents.UI_STONECUTTER_TAKE_RESULT, SoundSource.BLOCKS, 0.7f, 1.3f);
            leeg();
        }

        void stop(ServerLevel server, Component waarom) {
            ServerPlayer p = T5Machine.speler(server, besteller);
            if (p != null) {
                p.sendSystemMessage(waarom.copy().withStyle(ChatFormatting.GOLD));
            }
            BestelguhEntity guh = guh(server);
            if (guh != null) {
                guh.ren(null);
            }
            leeg();
        }

        private void leeg() {
            taken.clear();
            taakNr = 0;
            keerOver = 0;
            gemaakt = 0;
            besteller = null;
            bestelling = ItemStack.EMPTY;
            bestelAantal = 0;
            wachtOp = null;
            sync();
        }

        // =================================================================================================================
        // the guh
        // =================================================================================================================

        @Nullable
        BestelguhEntity guh(ServerLevel server) {
            return guhId != null && server.getEntity(guhId) instanceof BestelguhEntity g && g.isAlive() ? g : null;
        }

        /** The guh stands at the balie; missing twice (a few seconds apart, the chunk's entities loaded): a new one. */
        private void zorgVoorGuh(ServerLevel server) {
            if (guh(server) != null) {
                geenGuh = 0;
                return;
            }
            AABB rond = new AABB(worldPosition).inflate(T5Getallen.BALIE_BEREIK + 4);
            if (!server.areEntitiesLoaded(net.minecraft.world.level.ChunkPos.pack(worldPosition))) {
                return;
            }
            for (BestelguhEntity g : server.getEntitiesOfClass(BestelguhEntity.class, rond, g -> worldPosition.equals(g.balie()))) {
                guhId = g.getUUID();
                geenGuh = 0;
                return;
            }
            if (++geenGuh < 2) {
                return;
            }
            BestelguhEntity g = E2Trap5Feature.BESTELGUH.get().create(server, EntitySpawnReason.TRIGGERED);
            if (g == null) {
                return;
            }
            BlockPos staan = naast(server, worldPosition);
            g.snapTo(staan.getX() + 0.5, staan.getY(), staan.getZ() + 0.5, 0, 0);
            g.zetBalie(worldPosition);
            server.addFreshEntity(g);
            guhId = g.getUUID();
            geenGuh = 0;
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            if (level instanceof ServerLevel server) {
                BestelguhEntity g = guh(server);
                if (g != null) {
                    g.discard();
                }
            }
            super.preRemoveSideEffects(pos, state);
        }

        @Override
        public void vadsRegels(Consumer<Component> regels) {
            if (bank == null) {
                regels.accept(Component.translatable("block.guhs.bestelbalie.los").withStyle(ChatFormatting.GOLD));
            } else if (taken.isEmpty()) {
                regels.accept(Component.translatable("block.guhs.bestelbalie.wacht_op_bestelling").withStyle(ChatFormatting.GRAY));
            } else {
                regels.accept(Component.translatable("block.guhs.bestelbalie.bezig", bestelAantal, bestelling.getHoverName(), taakNr + 1, taken.size())
                        .withStyle(ChatFormatting.AQUA));
                if (wachtOp != null) {
                    regels.accept(Component.translatable("block.guhs.bestelbalie.wacht", wachtOp.getCount(), wachtOp.getHoverName()).withStyle(ChatFormatting.GOLD));
                }
            }
        }

        @Override
        protected void opslaan(ValueOutput uit) {
            uit.storeNullable("Bank", UUIDUtil.CODEC, bank);
            uit.putBoolean("Oefen", oefen);
            if (!taken.isEmpty()) {
                uit.store("Taken", Bestelplan.Taak.CODEC.listOf(), List.copyOf(taken));
                uit.putInt("TaakNr", taakNr);
                uit.putInt("KeerOver", keerOver);
                uit.putInt("Gemaakt", gemaakt);
                uit.putLong("Vooruit", laatsteVooruit);
                uit.storeNullable("Besteller", UUIDUtil.CODEC, besteller);
                if (!bestelling.isEmpty()) {
                    uit.store("Bestelling", ItemStack.CODEC, bestelling);
                }
                uit.putInt("Aantal", bestelAantal);
            }
        }

        @Override
        protected void laden(ValueInput in) {
            UUID oud = bank;
            bank = in.read("Bank", UUIDUtil.CODEC).orElse(null);
            oefen = in.getBooleanOr("Oefen", false);
            taken.clear();
            in.read("Taken", Bestelplan.Taak.CODEC.listOf()).ifPresent(taken::addAll);
            taakNr = Math.max(0, in.getIntOr("TaakNr", 0));
            keerOver = Math.max(0, in.getIntOr("KeerOver", 0));
            gemaakt = Math.max(0, in.getIntOr("Gemaakt", 0));
            laatsteVooruit = in.getLongOr("Vooruit", 0L);
            besteller = in.read("Besteller", UUIDUtil.CODEC).orElse(null);
            bestelling = in.read("Bestelling", ItemStack.CODEC).orElse(ItemStack.EMPTY);
            bestelAantal = Math.max(0, in.getIntOr("Aantal", 0));
            if (taakNr >= taken.size()) {
                taken.clear();
            }
            if (oud != null && !oud.equals(bank)) {
                Set<GlobalPos> s = PER_BANK.get(oud);
                if (s != null && level != null) {
                    s.remove(GlobalPos.of(level.dimension(), worldPosition));
                }
            }
        }
    }
}
