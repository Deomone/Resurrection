package com.ankh.entity;

import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import com.ankh.entity.ai.ReturnToAnchorGoal;
import com.ankh.session.ResurrectionManager;
import com.ankh.session.ResurrectionSession;
import com.ankh.session.ResurrectionState;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundGoal;
import net.minecraft.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

public class ResurrectionZombieEntity extends ZombieEntity implements net.minecraft.entity.ai.RangedAttackMob {

    private static final TrackedData<Optional<UUID>> OWNER_UUID =
            DataTracker.registerData(ResurrectionZombieEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<String> OWNER_NAME =
            DataTracker.registerData(ResurrectionZombieEntity.class, TrackedDataHandlerRegistry.STRING);

    private static final TrackedData<Optional<BlockPos>> LAMP_POS =
            DataTracker.registerData(ResurrectionZombieEntity.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS);

    private static final TrackedData<Boolean> LEASH_BROKEN =
            DataTracker.registerData(ResurrectionZombieEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    public static double leashRadius() { return com.ankh.config.AnkhConfig.get().guardianSoftLeashRadius; }

    public static double maxRadius() { return com.ankh.config.AnkhConfig.get().guardianHardLeashRadius; }

    private BlockPos anchor = BlockPos.ORIGIN;

    private net.minecraft.item.ItemStack meleeWeapon = net.minecraft.item.ItemStack.EMPTY;
    private net.minecraft.item.ItemStack rangedWeapon = net.minecraft.item.ItemStack.EMPTY;

    public ResurrectionZombieEntity(EntityType<? extends ZombieEntity> type, World world) {
        super(type, world);
        this.setCanPickUpLoot(false);
        this.experiencePoints = 0;

        this.setPathfindingPenalty(net.minecraft.entity.ai.pathing.PathNodeType.LAVA, 0.0f);
        this.setPathfindingPenalty(net.minecraft.entity.ai.pathing.PathNodeType.DAMAGE_FIRE, 0.0f);
        this.setPathfindingPenalty(net.minecraft.entity.ai.pathing.PathNodeType.DANGER_FIRE, 0.0f);
    }

    public void initialize(UUID owner, String ownerName, BlockPos anchor) {
        this.anchor = anchor.toImmutable();
        this.getDataTracker().set(OWNER_UUID, Optional.ofNullable(owner));
        this.getDataTracker().set(OWNER_NAME, ownerName == null ? "" : ownerName);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(OWNER_UUID, Optional.empty());
        builder.add(OWNER_NAME, "");
        builder.add(LAMP_POS, Optional.empty());
        builder.add(LEASH_BROKEN, false);
    }

    public void setLampPos(BlockPos pos) {
        this.getDataTracker().set(LAMP_POS, Optional.ofNullable(pos));
    }

    public Optional<BlockPos> getLampPos() {
        return this.getDataTracker().get(LAMP_POS);
    }

    public boolean isLeashBroken() {
        return this.getDataTracker().get(LEASH_BROKEN);
    }

    public void setLeashBroken(boolean broken) {
        this.getDataTracker().set(LEASH_BROKEN, broken);
    }

    public void setRangedWeapon(net.minecraft.item.ItemStack ranged, net.minecraft.item.ItemStack melee) {
        this.rangedWeapon = ranged == null ? net.minecraft.item.ItemStack.EMPTY : ranged.copy();
        this.meleeWeapon = melee == null ? net.minecraft.item.ItemStack.EMPTY : melee.copy();
    }

    public boolean hasRangedWeapon() {
        return !this.rangedWeapon.isEmpty();
    }

    public boolean isWithinLeash(net.minecraft.entity.Entity e) {
        return isWithinLeash(e.getX(), e.getZ());
    }

    public boolean isWithinLeash(double x, double z) {
        if (this.anchor == null) return true;
        double dx = x - (this.anchor.getX() + 0.5);
        double dz = z - (this.anchor.getZ() + 0.5);
        return dx * dx + dz * dz <= maxRadius() * maxRadius();
    }

    public void drawRangedWeapon() {
        if (!this.rangedWeapon.isEmpty()) {
            this.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND, this.rangedWeapon.copy());
        }
    }

    public void sheatheRangedWeapon() {
        this.equipStack(net.minecraft.entity.EquipmentSlot.MAINHAND, this.meleeWeapon.copy());
    }

    @Override
    public void shootAt(net.minecraft.entity.LivingEntity target, float pullProgress) {
        if (!(this.getWorld() instanceof ServerWorld)) return;

        if (multishotLevel() > 0) {
            spawnArrow(target, pullProgress, -10.0f);
            spawnArrow(target, pullProgress, 0.0f);
            spawnArrow(target, pullProgress, 10.0f);
        } else {
            spawnArrow(target, pullProgress, 0.0f);
        }
        this.playSound(net.minecraft.sound.SoundEvents.ENTITY_SKELETON_SHOOT, 1.0f,
                1.0f / (this.getRandom().nextFloat() * 0.4f + 0.8f));
    }

    private int multishotLevel() {
        if (this.rangedWeapon.isEmpty()) return 0;
        var entry = this.getRegistryManager().get(net.minecraft.registry.RegistryKeys.ENCHANTMENT)
                .getEntry(net.minecraft.enchantment.Enchantments.MULTISHOT).orElse(null);
        return entry == null ? 0 : net.minecraft.enchantment.EnchantmentHelper.getLevel(entry, this.rangedWeapon);
    }

    private void spawnArrow(net.minecraft.entity.LivingEntity target, float pullProgress, float yawOffsetDeg) {
        net.minecraft.item.ItemStack arrowStack = new net.minecraft.item.ItemStack(net.minecraft.item.Items.ARROW);
        net.minecraft.item.ItemStack weapon = this.rangedWeapon.isEmpty()
                ? new net.minecraft.item.ItemStack(net.minecraft.item.Items.BOW) : this.rangedWeapon;
        net.minecraft.entity.projectile.PersistentProjectileEntity arrow =
                net.minecraft.entity.projectile.ProjectileUtil.createArrowProjectile(this, arrowStack, pullProgress, weapon);
        double dx = target.getX() - this.getX();
        double dy = target.getBodyY(0.3333333333333333) - arrow.getY();
        double dz = target.getZ() - this.getZ();

        double rad = Math.toRadians(yawOffsetDeg);
        double cos = Math.cos(rad), sin = Math.sin(rad);
        double rdx = dx * cos - dz * sin;
        double rdz = dx * sin + dz * cos;
        double horiz = Math.sqrt(rdx * rdx + rdz * rdz);

        int difficulty = this.getWorld().getDifficulty().getId();
        arrow.setVelocity(rdx, dy + horiz * 0.2, rdz, 1.6f, (float) (14 - difficulty * 4));
        arrow.pickupType = net.minecraft.entity.projectile.PersistentProjectileEntity.PickupPermission.DISALLOWED;
        this.getWorld().spawnEntity(arrow);
    }

    public Optional<UUID> getOwnerUuid() {
        return this.getDataTracker().get(OWNER_UUID);
    }

    public String getOwnerName() {
        return this.getDataTracker().get(OWNER_NAME);
    }

    public BlockPos getAnchor() {
        return anchor;
    }

    @Override
    protected void initGoals() {

        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new com.ankh.entity.ai.GuardianRangedGoal(this, 30));
        this.goalSelector.add(2, new ZombieAttackGoal(this, 1.0, false));
        this.goalSelector.add(3, new ReturnToAnchorGoal(this));
        this.goalSelector.add(7, new WanderAroundGoal(this, 0.8));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        this.goalSelector.add(8, new LookAroundGoal(this));

        this.targetSelector.add(1, new RevengeGoal(this));
    }

    @Override
    protected boolean burnsInDaylight() {
        return false;
    }

    @Override
    public boolean damage(net.minecraft.entity.damage.DamageSource source, float amount) {
        if (source.getAttacker() instanceof net.minecraft.server.network.ServerPlayerEntity p) {

            if (p.isSpectator()) {
                return false;
            }

            if (getOwnerUuid().filter(o -> o.equals(p.getUuid())).isPresent()) {
                net.minecraft.server.MinecraftServer srv = p.getServer();
                if (srv != null) {
                    com.ankh.session.ResurrectionSession s =
                            com.ankh.session.ResurrectionState.get(srv).get(p.getUuid());
                    if (s != null && s.isWaiting()) {
                        return false;
                    }
                }
            }
        }
        return super.damage(source, amount);
    }

    @Override
    public boolean isFireImmune() {
        return true;
    }

    @Override
    public boolean canImmediatelyDespawn(double distanceSquared) {
        return false;
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    protected boolean isDisallowedInPeaceful() {

        return false;
    }

    @Override
    public boolean isPersistent() {
        return true;
    }

    @Override
    protected boolean canConvertInWater() {
        return false;
    }

    @Override
    public void tickMovement() {
        super.tickMovement();

        this.setAir(this.getMaxAir());

        if (this.getWorld() instanceof ServerWorld __sw) drawGuardRings(__sw);

        if (!this.getWorld().isClient && this.getTarget() != null && !this.isLeashBroken()) {
            this.setLeashBroken(true);
            this.getWorld().playSound(null, this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sound.SoundEvents.ENTITY_LEASH_KNOT_BREAK,
                    net.minecraft.sound.SoundCategory.NEUTRAL, 1.0f, 1.0f);
        }

        if (!this.getWorld().isClient && this.anchor != null) {
            double cx = this.anchor.getX() + 0.5;
            double cy = this.anchor.getY();
            double cz = this.anchor.getZ() + 0.5;
            double dx = this.getX() - cx;
            double dy = this.getY() - cy;
            double dz = this.getZ() - cz;
            if (dx * dx + dy * dy + dz * dz > maxRadius() * maxRadius()) {
                net.minecraft.entity.LivingEntity keepTarget = this.getTarget();
                this.getNavigation().stop();
                this.requestTeleport(cx, cy, cz);
                this.setVelocity(net.minecraft.util.math.Vec3d.ZERO);
                this.velocityModified = true;

                if (keepTarget != null && keepTarget.isAlive()) {
                    this.setTarget(keepTarget);
                }
            }
        }
    }

    @Override
    public void onDeath(net.minecraft.entity.damage.DamageSource damageSource) {
        if (!this.getWorld().isClient && this.getWorld() instanceof ServerWorld serverWorld) {
            MinecraftServer server = serverWorld.getServer();
            getOwnerUuid().ifPresent(owner -> {
                ResurrectionState state = ResurrectionState.get(server);
                ResurrectionSession session = state.get(owner);
                if (session != null && session.getPhase() == ResurrectionSession.Phase.ZOMBIE_ALIVE) {
                    ResurrectionManager.onZombieDeath(server, session, this.getPos(), getOwnerName());
                }
            });
        }
        super.onDeath(damageSource);
    }

    @Override
    protected void dropLoot(net.minecraft.entity.damage.DamageSource source, boolean causedByPlayer) {

    }

    @Override
    protected void dropEquipment(net.minecraft.server.world.ServerWorld world, net.minecraft.entity.damage.DamageSource source, boolean causedByPlayer) {

    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        getOwnerUuid().ifPresent(uuid -> nbt.putUuid("AnkhOwner", uuid));
        nbt.putString("AnkhOwnerName", getOwnerName());
        nbt.putInt("AnchorX", anchor.getX());
        nbt.putInt("AnchorY", anchor.getY());
        nbt.putInt("AnchorZ", anchor.getZ());

        if (!this.rangedWeapon.isEmpty()) {
            nbt.put("AnkhRanged", this.rangedWeapon.encode(this.getRegistryManager()));
        }
        if (!this.meleeWeapon.isEmpty()) {
            nbt.put("AnkhMelee", this.meleeWeapon.encode(this.getRegistryManager()));
        }
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.containsUuid("AnkhOwner")) {
            this.getDataTracker().set(OWNER_UUID, Optional.of(nbt.getUuid("AnkhOwner")));
        }
        this.getDataTracker().set(OWNER_NAME, nbt.getString("AnkhOwnerName"));
        this.anchor = new BlockPos(nbt.getInt("AnchorX"), nbt.getInt("AnchorY"), nbt.getInt("AnchorZ"));
        if (nbt.contains("AnkhRanged")) {
            this.rangedWeapon = net.minecraft.item.ItemStack.fromNbt(this.getRegistryManager(), nbt.getCompound("AnkhRanged"))
                    .orElse(net.minecraft.item.ItemStack.EMPTY);
        }
        if (nbt.contains("AnkhMelee")) {
            this.meleeWeapon = net.minecraft.item.ItemStack.fromNbt(this.getRegistryManager(), nbt.getCompound("AnkhMelee"))
                    .orElse(net.minecraft.item.ItemStack.EMPTY);
        }
    }
    private int guardRingTick = 0;

    private void drawGuardRings(ServerWorld sw) {
        if ((this.guardRingTick++ % 10) != 0) return;
        if (this.anchor == null) return;
        double cx = this.anchor.getX() + 0.5;
        double cz = this.anchor.getZ() + 0.5;
        int anchorY = this.anchor.getY();

        if (this.getTarget() == null) {

            ringLayer(sw, cx, cz, leashRadius(), ParticleTypes.SOUL_FIRE_FLAME, 1.5, anchorY, 0.1);
        } else {

            double[] levels = { 0.1, 1.0, 1.9 };
            for (double h : levels) {
                ringLayer(sw, cx, cz, maxRadius(), ParticleTypes.SOUL_FIRE_FLAME, 1.2, anchorY, h);
            }
        }
    }

    private static void ringLayer(ServerWorld sw, double cx, double cz, double radius,
                                  ParticleEffect particle, double spacing, int anchorY, double heightOffset) {
        int points = Math.max(8, (int) (2.0 * Math.PI * radius / spacing));
        for (int i = 0; i < points; i++) {
            double ang = (2.0 * Math.PI * i) / points;
            double x = cx + radius * Math.cos(ang);
            double z = cz + radius * Math.sin(ang);
            double groundY = groundYAt(sw, x, z, anchorY);
            sw.spawnParticles(particle, x, groundY + heightOffset, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private static double groundYAt(ServerWorld w, double xd, double zd, int anchorY) {
        int x = net.minecraft.util.math.MathHelper.floor(xd);
        int z = net.minecraft.util.math.MathHelper.floor(zd);
        int top = anchorY + 4;
        int bottom = anchorY - 24;
        net.minecraft.util.math.BlockPos.Mutable m = new net.minecraft.util.math.BlockPos.Mutable();
        boolean aboveSolid = isGround(w, m.set(x, top + 1, z));
        for (int y = top; y >= bottom; y--) {
            boolean solid = isGround(w, m.set(x, y, z));
            if (solid && !aboveSolid) {
                return y + 1;
            }
            aboveSolid = solid;
        }
        return anchorY;
    }

    private static boolean isGround(ServerWorld w, net.minecraft.util.math.BlockPos p) {
        net.minecraft.block.BlockState s = w.getBlockState(p);

        if (s.isIn(net.minecraft.registry.tag.BlockTags.LEAVES)) return false;

        if (!s.getFluidState().isEmpty()) return true;
        return !s.getCollisionShape(w, p).isEmpty();
    }
}
