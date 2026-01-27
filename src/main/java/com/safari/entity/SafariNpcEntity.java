package com.safari.entity;

import com.safari.world.SafariDimension;
import com.safari.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import com.safari.shop.SafariShopScreenHandler;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.entity.EntityData;
import net.minecraft.world.ServerWorldAccess;

import java.util.EnumSet;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;

public class SafariNpcEntity extends PathAwareEntity {
    private static final TrackedData<Integer> NAME_VISIBILITY = DataTracker.registerData(SafariNpcEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public static final int NAME_MODE_HOVER = 0;
    public static final int NAME_MODE_ALWAYS = 1;
    public static final int NAME_MODE_NEVER = 2;

    public SafariNpcEntity(EntityType<? extends PathAwareEntity> entityType, World world) {
        super(entityType, world);
        // Default: No Name, Rename via Name Tag
        this.setPersistent();
        applyHeldItems();
        this.setCanPickUpLoot(false);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(NAME_VISIBILITY, NAME_MODE_HOVER); // Default to Hover
    }

    public int getNameVisibility() {
        return this.dataTracker.get(NAME_VISIBILITY);
    }

    public void setNameVisibility(int mode) {
        this.dataTracker.set(NAME_VISIBILITY, mode);
        if (mode == NAME_MODE_ALWAYS) {
            this.setCustomNameVisible(true);
        } else {
            this.setCustomNameVisible(false); // Valid for Hover (handled by logic) and Never
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
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(ModItems.SAFARI_BALL));
        this.equipStack(EquipmentSlot.OFFHAND, new ItemStack(ModItems.SAFARI_TICKET_5));
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f);
        this.setEquipmentDropChance(EquipmentSlot.OFFHAND, 0.0f);
    }

    public static DefaultAttributeContainer createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0)
                .build();
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SmoothLookGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
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
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        
        // 1. Name Toggler
        if (stack.getItem() == Items.FEATHER && stack.contains(DataComponentTypes.CUSTOM_NAME) && stack.getName().getString().contains("Name Toggler")) {
            if (!this.getWorld().isClient) {
                int current = getNameVisibility();
                int next = (current + 1) % 3;
                setNameVisibility(next);
                
                String modeName = switch (next) {
                    case NAME_MODE_HOVER -> "Hover Only";
                    case NAME_MODE_ALWAYS -> "Always Visible";
                    case NAME_MODE_NEVER -> "Never Visible";
                    default -> "Unknown";
                };
                player.sendMessage(Text.literal("Name Visibility: " + modeName).formatted(net.minecraft.util.Formatting.YELLOW), true);
            }
            return ActionResult.SUCCESS;
        }

        // 2. Name Tag Renaming (Sneak + Right Click)
        if (stack.getItem() == Items.NAME_TAG && player.isSneaking()) {
            return ActionResult.PASS; // Let vanilla handle it
        }

        // 3. Shop Interaction
        if (!this.getWorld().isClient && player instanceof ServerPlayerEntity serverPlayer) {
            serverPlayer.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inv, user) -> new SafariShopScreenHandler(syncId, inv),
                    Text.translatable("message.safari.shop_title")
            ));
            return ActionResult.SUCCESS;
        }
        return ActionResult.CONSUME;
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

    private static final class SmoothLookGoal extends Goal {
        private final SafariNpcEntity npc;
        private int cooldown;

        private SmoothLookGoal(SafariNpcEntity npc) {
            this.npc = npc;
            this.setControls(EnumSet.of(Control.LOOK));
        }

        @Override
        public boolean canStart() {
            return true;
        }

        @Override
        public void tick() {
            if (cooldown > 0) {
                cooldown--;
                return;
            }

            PlayerEntity target = npc.getWorld().getClosestPlayer(npc, 8.0);
            if (target == null) {
                return;
            }

            Vec3d targetPos = target.getPos();
            npc.getLookControl().lookAt(targetPos.x, targetPos.y + target.getStandingEyeHeight(), targetPos.z, 12.0f, 12.0f);
            cooldown = 8;
        }
    }
}
