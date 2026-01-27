package com.safari.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import com.safari.block.SafariBlocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.SpawnReason;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Formatting;

import net.minecraft.component.DataComponentTypes;

public class SafariPortalNpcEntity extends PathAwareEntity {
    private static final TrackedData<Integer> NAME_VISIBILITY = DataTracker.registerData(SafariPortalNpcEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public static final int NAME_MODE_HOVER = 0;
    public static final int NAME_MODE_ALWAYS = 1;
    public static final int NAME_MODE_NEVER = 2;

    public SafariPortalNpcEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        // Default: No Name
        applyHeldItems();
        this.setCanPickUpLoot(false);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(NAME_VISIBILITY, NAME_MODE_HOVER);
    }

    public int getNameVisibility() {
        return this.dataTracker.get(NAME_VISIBILITY);
    }

    public void setNameVisibility(int mode) {
        this.dataTracker.set(NAME_VISIBILITY, mode);
        if (mode == NAME_MODE_ALWAYS) {
            this.setCustomNameVisible(true);
        } else {
            this.setCustomNameVisible(false);
        }
    }

    @Override
    public boolean shouldRenderName() {
        if (getNameVisibility() == NAME_MODE_NEVER) {
            return false;
        }
        return super.shouldRenderName();
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putInt("NameVisibility", getNameVisibility());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("NameVisibility")) {
            setNameVisibility(nbt.getInt("NameVisibility"));
        }
    }


    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, EntityData entityData) {
        EntityData data = super.initialize(world, difficulty, spawnReason, entityData);
        applyHeldItems();
        return data;
    }

    private void applyHeldItems() {
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(SafariBlocks.SAFARI_PORTAL_FRAME));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);
    }

    @Override
    protected void initGoals() {
        // Basic AI goals
        this.goalSelector.add(0, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
    }

    public static DefaultAttributeContainer.Builder createNpcAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0);
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        
        // 1. Name Toggler
        if (stack.getItem() == Items.FEATHER && stack.contains(DataComponentTypes.CUSTOM_NAME) && stack.getName().getString().contains("Name Toggler")) {
            if (!this.getWorld().isClient) {
                int current = getNameVisibility();
                int next = (current + 1) % 3;
                setNameVisibility(next);
                
                String modeKey = switch (next) {
                    case NAME_MODE_HOVER -> "message.safari.visibility.hover";
                    case NAME_MODE_ALWAYS -> "message.safari.visibility.always";
                    case NAME_MODE_NEVER -> "message.safari.visibility.never";
                    default -> "Unknown";
                };
                player.sendMessage(Text.translatable("message.safari.name_visibility", Text.translatable(modeKey)).formatted(Formatting.YELLOW), true);
            }
            return ActionResult.SUCCESS;
        }

        // 2. Name Tag - OP only
        if (stack.getItem() == Items.NAME_TAG && player.isSneaking()) {
            if (player.hasPermissionLevel(2)) {
                return ActionResult.PASS;
            } else {
                if (!this.getWorld().isClient) {
                    player.sendMessage(Text.translatable("message.safari.only_ops_rename").formatted(Formatting.RED), true);
                }
                return ActionResult.SUCCESS;
            }
        }

        // 3. Command Interaction (Main Hand)
        if (!player.getWorld().isClient() && hand == Hand.MAIN_HAND) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            var server = serverPlayer.getServer();
            if (server != null) {
                try {
                    server.getCommandManager().getDispatcher()
                        .execute("safari enter", serverPlayer.getCommandSource());
                } catch (Exception e) {
                    return ActionResult.FAIL;
                }
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.isOf(DamageTypes.GENERIC_KILL) || source.isOf(DamageTypes.OUT_OF_WORLD)) {
            return super.damage(source, amount);
        }
        
        // Check YAWP flag
        Boolean allowed = com.safari.compat.CompatHandler.checkYawp(this, source.getAttacker());
        if (allowed != null && allowed) {
             return super.damage(source, amount);
        }
        
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    public void setNpcName(String name) {
        this.setCustomName(Text.literal(name));
    }
}
