package igentuman.mcakcccompat.mixin.common;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.kingdomcomecombat.ai.HumanoidCombatAiTicker", remap = false)
public abstract class McaKccTargetBridgeMixin {
    @Redirect(
            method = "tickMob(Lnet/minecraft/world/entity/Mob;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Mob;getTarget()Lnet/minecraft/world/entity/LivingEntity;",
                    remap = false
            ),
            remap = false
    )
    private static LivingEntity mcaKccCompat$resolveTarget(Mob mob) {
        LivingEntity target = mob.getTarget();
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (!"mca".equals(typeId.getNamespace())) {
            return target;
        }

        return mob.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET).orElse(target);
    }
}
