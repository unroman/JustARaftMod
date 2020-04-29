package com.mrbysco.justaraftmod.entities;

import com.mrbysco.justaraftmod.config.RaftConfig;
import com.mrbysco.justaraftmod.init.RaftRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.network.IPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.FMLPlayMessages;
import net.minecraftforge.fml.network.NetworkHooks;

public class RaftEntity extends BoatEntity
{
    public RaftEntity(EntityType<? extends RaftEntity> entityType, World worldIn)
    {
        super(entityType, worldIn);
    }

    public RaftEntity(World worldIn, double x, double y, double z) {
        this(RaftRegistry.RAFT.get(), worldIn);
        this.setPosition(x, y, z);
        this.setMotion(Vec3d.ZERO);
        this.prevPosX = x;
        this.prevPosY = y;
        this.prevPosZ = z;
    }

    public RaftEntity(FMLPlayMessages.SpawnEntity spawnEntity, World worldIn) {
        this(RaftRegistry.RAFT.get(), worldIn);
    }

    @Override
    public void tick() {
        super.tick();
        if(RaftConfig.SERVER.SinkTheRaft.get()) {
            if(this.getPassengers().size() > 1) {
                Vec3d motion = this.getMotion();
                double newY = motion.y - 0.035D;
                this.setMotion(motion.x, newY, motion.z);
            }
        }
    }

    @Override
    public Status getBoatStatus() {
        BoatEntity.Status boatentity$status = this.getUnderwaterStatus();
        if (boatentity$status != null) {
            this.waterLevel = this.getBoundingBox().maxY;
            return boatentity$status;
        } else if (this.checkInWater()) {
            return BoatEntity.Status.IN_WATER;
        } else {
            float f = this.getBoatGlide();
            if (f > 0.0F) {
                if(RaftConfig.SERVER.SlipperyFast.get())
                    this.boatGlide = f;
                else
                    this.boatGlide = 0;

                return BoatEntity.Status.ON_LAND;
            } else {
                return BoatEntity.Status.IN_AIR;
            }
        }
    }

    @Override
    public void updateMotion() {
        double d0 = -0.03999999910593033D;
        double d1 = this.hasNoGravity() ? 0.0D : d0;
        double d2 = 0.0D;
        this.momentum = 0.05F;

        if (this.previousStatus == BoatEntity.Status.IN_AIR && this.status != BoatEntity.Status.IN_AIR && this.status != BoatEntity.Status.ON_LAND) {
            this.waterLevel = this.getBoundingBox().minY + (double)this.getHeight();
            this.setPosition(this.posX, (double)(this.getWaterLevelAbove() - this.getHeight()) + 0.101D, this.posZ);
            this.setMotion(this.getMotion().mul(1.0D, 0.0D, 1.0D));
            this.lastYd = 0.0D;
            this.status = BoatEntity.Status.IN_WATER;
        } else {
            if (this.status == BoatEntity.Status.IN_WATER) {
                d2 = (this.waterLevel - this.getBoundingBox().minY + 0.1D) / (double)this.getHeight();
                this.momentum = 0.9F;
            } else if (this.status == BoatEntity.Status.UNDER_FLOWING_WATER) {
                d1 = -7.0E-4D;
                this.momentum = 0.9F;
            } else if (this.status == BoatEntity.Status.UNDER_WATER) {
                d2 = 0.009999999776482582D;
                this.momentum = 0.45F;
            } else if (this.status == BoatEntity.Status.IN_AIR) {
                this.momentum = 0.9F;
            } else if (this.status == BoatEntity.Status.ON_LAND) {
                this.momentum = this.boatGlide;
                if (this.getControllingPassenger() instanceof PlayerEntity) {
                    this.boatGlide /= 2.0F;
                }
            }

            Vec3d vec3d = this.getMotion();
            this.setMotion(vec3d.x * (double)this.momentum, vec3d.y + d1, vec3d.z * (double)this.momentum);
            this.deltaRotation *= this.momentum;
            if (d2 > 0.0D) {
                Vec3d vec3d1 = this.getMotion();
                this.setMotion(vec3d1.x, (vec3d1.y + d2 * 0.06153846016296973D) * 0.75D, vec3d1.z);
            }
        }
    }

    public void controlBoat() {
        if (this.isBeingRidden()) {
            float f = 0.0F;
            if (this.leftInputDown) {
                --this.deltaRotation;
            }

            if (this.rightInputDown) {
                ++this.deltaRotation;
            }

            if (this.rightInputDown != this.leftInputDown && !this.forwardInputDown && !this.backInputDown) {
                f += 0.005F;
            }

            this.rotationYaw += this.deltaRotation;
            if (this.forwardInputDown) {
                f += 0.04F * RaftConfig.SERVER.SpeedMultiplier.get();
            }

            if (this.backInputDown) {
                f -= 0.005F * RaftConfig.SERVER.SpeedMultiplier.get();
            }

            this.setMotion(this.getMotion().add((double)(MathHelper.sin(-this.rotationYaw * ((float)Math.PI / 180F)) * f), 0.0D, (double)(MathHelper.cos(this.rotationYaw * ((float)Math.PI / 180F)) * f)));
            this.setPaddleState(this.rightInputDown && !this.leftInputDown || this.forwardInputDown, this.leftInputDown && !this.rightInputDown || this.forwardInputDown);
        }
    }

    @Override
    public double getMountedYOffset() {
        return 0D;
    }

    @Override
    public Item getItemBoat() {
        switch (this.getBoatType()) {
            default:
                return RaftRegistry.OAK_RAFT.get();
            case SPRUCE:
                return RaftRegistry.SPRUCE_RAFT.get();
            case BIRCH:
                return RaftRegistry.BIRCH_RAFT.get();
            case JUNGLE:
                return RaftRegistry.JUNGLE_RAFT.get();
            case ACACIA:
                return RaftRegistry.ACACIA_RAFT.get();
            case DARK_OAK:
                return RaftRegistry.DARK_OAK_RAFT.get();
        }
    }

    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}