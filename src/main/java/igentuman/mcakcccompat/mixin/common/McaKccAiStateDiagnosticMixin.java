package igentuman.mcakcccompat.mixin.common;

import igentuman.mcakcccompat.McaKccCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Pseudo
@Mixin(targets = "com.kingdomcomecombat.ai.HumanoidCombatAiTicker", remap = false)
public abstract class McaKccAiStateDiagnosticMixin {
    private static final ConcurrentMap<Integer, Long> LAST_LOGGED_TICKS = new ConcurrentHashMap<>();

    @Inject(
            method = "tickMob(Lnet/minecraft/world/entity/Mob;)V",
            at = @At("HEAD"),
            remap = false
    )
    private static void mcaKccCompat$logAiState(Mob mob, CallbackInfo ci) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (!"mca".equals(typeId.getNamespace())
                || !(typeId.getPath().equals("male_villager") || typeId.getPath().equals("female_villager"))) {
            return;
        }

        long gameTime = mob.level().getGameTime();
        Long previousLogTick = LAST_LOGGED_TICKS.put(mob.getId(), gameTime);
        if (previousLogTick != null && gameTime >= previousLogTick && gameTime - previousLogTick < 20) {
            return;
        }

        LivingEntity target = mob.getTarget();
        String targetInfo = target == null
                ? "none"
                : BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()) + "/" + target.getId();
        LivingEntity mcaAttackTarget = mob.getBrain()
                .getMemory(MemoryModuleType.ATTACK_TARGET)
                .orElse(null);
        String mcaAttackTargetInfo = mcaAttackTarget == null
                ? "none"
                : BuiltInRegistries.ENTITY_TYPE.getKey(mcaAttackTarget.getType()) + "/" + mcaAttackTarget.getId();
        LivingEntity nearestHostile = mob.getBrain()
                .getMemory(MemoryModuleType.NEAREST_HOSTILE)
                .orElse(null);
        String nearestHostileInfo = nearestHostile == null
                ? "none"
                : BuiltInRegistries.ENTITY_TYPE.getKey(nearestHostile.getType()) + "/" + nearestHostile.getId();
        double targetDistance = target == null ? -1.0 : mob.distanceTo(target);
        String mainHandItem = BuiltInRegistries.ITEM.getKey(mob.getMainHandItem().getItem()).toString();
        McaKccCompat.LOGGER.debug(
                "KCC AI state for MCA entity {} [{}]: target={}, brainAttackTarget={}, nearestHostile={}, distance={}, mainHand={}, aggressive={}",
                mob.getId(),
                typeId,
                targetInfo,
                mcaAttackTargetInfo,
                nearestHostileInfo,
                targetDistance,
                mainHandItem,
                mob.isAggressive()
        );
    }
}
