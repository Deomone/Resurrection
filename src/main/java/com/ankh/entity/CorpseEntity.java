package com.ankh.entity;

import com.ankh.item.AnkhItem;
import com.ankh.session.ResurrectionManager;
import com.ankh.session.ResurrectionSession;
import com.ankh.session.ResurrectionState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public class CorpseEntity extends LivingEntity {

    private static final TrackedData<Optional<UUID>> OWNER_UUID =
            DataTracker.registerData(CorpseEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<String> OWNER_NAME =
            DataTracker.registerData(CorpseEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Optional<net.minecraft.util.math.BlockPos>> LAMP_POS =
            DataTracker.registerData(CorpseEntity.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS);

    public CorpseEntity(EntityType<? extends LivingEntity> type, World world) {
        super(type, world);
        this.setNoGravity(true);

        this.noClip = true;
        this.setPersistent();
    }

    public void initialize(UUID owner, String ownerName) {
        this.getDataTracker().set(OWNER_UUID, Optional.ofNullable(owner));
        this.getDataTracker().set(OWNER_NAME, ownerName == null ? "" : ownerName);
    }

    public void setPersistent() {

    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(OWNER_UUID, Optional.empty());
        builder.add(OWNER_NAME, "");
        builder.add(LAMP_POS, Optional.empty());
    }

    public void setLampPos(net.minecraft.util.math.BlockPos pos) {
        this.getDataTracker().set(LAMP_POS, Optional.ofNullable(pos));
    }

    public Optional<net.minecraft.util.math.BlockPos> getLampPos() {
        return this.getDataTracker().get(LAMP_POS);
    }

    public Optional<UUID> getOwnerUuid() {
        return this.getDataTracker().get(OWNER_UUID);
    }

    public String getOwnerName() {
        return this.getDataTracker().get(OWNER_NAME);
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isFireImmune() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isCollidable() {

        return false;
    }

    @Override
    public boolean collidesWith(net.minecraft.entity.Entity other) {
        return false;
    }

    @Override
    public void pushAwayFrom(net.minecraft.entity.Entity entity) {

    }

    @Override
    public void addVelocity(double x, double y, double z) {

    }

    @Override
    public void setVelocity(Vec3d velocity) {
        super.setVelocity(Vec3d.ZERO);
    }

    @Override
    public boolean isImmuneToExplosion(net.minecraft.world.explosion.Explosion explosion) {
        return true;
    }

    @Override
    protected void pushOutOfBlocks(double x, double y, double z) {

    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource damageSource) {
        return false;
    }

    @Override
    protected net.minecraft.entity.EntityDimensions getBaseDimensions(net.minecraft.entity.EntityPose pose) {
        return net.minecraft.entity.EntityDimensions.fixed(2.6f, 0.7f);
    }

    @Override
    public float getTargetingMargin() {
        return 0.7f;
    }

    @Override
    public void tick() {
        super.tick();

        this.setVelocity(Vec3d.ZERO);
        this.noClip = true;
        this.velocityDirty = true;
    }

    @Override
    public ActionResult interact(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (this.getWorld().isClient) {
            return stack.getItem() instanceof AnkhItem ? ActionResult.SUCCESS : ActionResult.PASS;
        }
        if (!(stack.getItem() instanceof AnkhItem)) {
            return ActionResult.PASS;
        }
        if (!(player instanceof ServerPlayerEntity user) || !(this.getWorld() instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }

        MinecraftServer server = serverWorld.getServer();
        ResurrectionState state = ResurrectionState.get(server);
        Optional<UUID> owner = getOwnerUuid();
        if (owner.isPresent()) {
            ResurrectionSession session = state.get(owner.get());
            if (session != null) {
                boolean ok = ResurrectionManager.resurrect(server, session, user);
                return ok ? ActionResult.SUCCESS : ActionResult.FAIL;
            }
        }
        return ActionResult.FAIL;
    }

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return Collections.emptyList();
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {

    }

    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        getOwnerUuid().ifPresent(uuid -> nbt.putUuid("AnkhOwner", uuid));
        nbt.putString("AnkhOwnerName", getOwnerName());
        getLampPos().ifPresent(p -> {
            nbt.putInt("AnkhLampX", p.getX());
            nbt.putInt("AnkhLampY", p.getY());
            nbt.putInt("AnkhLampZ", p.getZ());
        });
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid("AnkhOwner")) {
            this.getDataTracker().set(OWNER_UUID, Optional.of(nbt.getUuid("AnkhOwner")));
        }
        this.getDataTracker().set(OWNER_NAME, nbt.getString("AnkhOwnerName"));
        if (nbt.contains("AnkhLampX")) {
            this.setLampPos(new net.minecraft.util.math.BlockPos(
                    nbt.getInt("AnkhLampX"), nbt.getInt("AnkhLampY"), nbt.getInt("AnkhLampZ")));
        }
    }
}
