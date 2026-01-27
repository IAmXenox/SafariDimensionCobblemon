package com.safari.item;

import com.safari.world.SafariDimension;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import net.minecraft.text.Text;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

public class SafariBallItem extends Item {

    public SafariBallItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.getRegistryKey().equals(SafariDimension.SAFARI_DIM_KEY)) {
            if (!world.isClient) {
                user.sendMessage(Text.translatable("message.safari.only_use_in_safari").formatted(Formatting.RED), true);
            }
            return TypedActionResult.pass(stack);
        }

        if (!world.isClient) {
            try {
                // Classes
                Class<?> ballEntityClass = Class.forName("com.cobblemon.mod.common.entity.pokeball.EmptyPokeBallEntity");
                Class<?> pokeBallsClass = Class.forName("com.cobblemon.mod.common.api.pokeball.PokeBalls");
                Class<?> entitiesClass = Class.forName("com.cobblemon.mod.common.CobblemonEntities");
                
                // Get Objects
                // PokeBalls is a Kotlin object, accessible via INSTANCE in Java
                Field instanceField = pokeBallsClass.getField("INSTANCE");
                Object pokeBallsInstance = instanceField.get(null);
                Method getSafariBall = pokeBallsClass.getMethod("getSafariBall");
                Object safariBallObj = getSafariBall.invoke(pokeBallsInstance);
                
                // Get EntityType (EMPTY_POKEBALL)
                Field emptyBallField = entitiesClass.getField("EMPTY_POKEBALL");
                Object entityTypeObj = emptyBallField.get(null);
                
                // Find Constructor: (PokeBall, Level, LivingEntity, EntityType)
                Constructor<?> constructor = null;
                for (Constructor<?> c : ballEntityClass.getConstructors()) {
                    Class<?>[] types = c.getParameterTypes();
                    if (types.length == 4) {
                        if (types[0].getName().contains("PokeBall") &&
                            (types[1].isAssignableFrom(World.class) || types[1].getName().contains("Level")) &&
                            (types[2].isAssignableFrom(net.minecraft.entity.LivingEntity.class)) &&
                            (types[3].isAssignableFrom(net.minecraft.entity.EntityType.class))) {
                            constructor = c;
                            break;
                        }
                    }
                }
                
                if (constructor != null) {
                    Object projectile = constructor.newInstance(safariBallObj, world, user, entityTypeObj);
                    if (projectile instanceof net.minecraft.entity.projectile.ProjectileEntity proj) {
                        float pitch = user.getPitch();
                        float yaw = user.getYaw();
                        
                        float overhandFactor = (pitch < 0) ? (5f * (float) Math.cos(Math.toRadians(pitch))) : 5f;
                        
                        Method getThrowPower = safariBallObj.getClass().getMethod("getThrowPower");
                        float power = (float) getThrowPower.invoke(safariBallObj);

                        proj.setVelocity(user, pitch - overhandFactor, yaw, 0.0F, power, 1.0F);

                        if (proj instanceof net.minecraft.entity.projectile.thrown.ThrownItemEntity thrownProj) {
                            thrownProj.setItem(new ItemStack(ModItems.SAFARI_BALL));
                        }

                        world.spawnEntity(proj);
                    }
                } else {
                    user.sendMessage(Text.translatable("message.safari.ball_constructor_missing").formatted(Formatting.RED), true);
                }
                
            } catch (Exception e) {
                e.printStackTrace();
                user.sendMessage(Text.translatable("message.safari.internal_error", e.getMessage()).formatted(Formatting.RED), true);
            }
        }

        // Sound Logic
        try {
            Class<?> soundsClass = Class.forName("com.cobblemon.mod.common.CobblemonSounds");
            net.minecraft.sound.SoundEvent throwSound = (net.minecraft.sound.SoundEvent) soundsClass.getField("POKE_BALL_THROW").get(null);
            world.playSound(null, user.getX(), user.getY(), user.getZ(), throwSound, SoundCategory.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
        } catch (Exception e) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW, SoundCategory.NEUTRAL, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));
        }
        
        if (!user.getAbilities().creativeMode) {
            stack.decrement(1);
        }

        return TypedActionResult.success(stack, world.isClient());
    }
}
