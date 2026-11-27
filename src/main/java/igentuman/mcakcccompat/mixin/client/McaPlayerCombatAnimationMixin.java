package igentuman.mcakcccompat.mixin.client;

import igentuman.mcakcccompat.client.KccPlayerAnimationLibraryBridge;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.conczin.mca.client.model.PlayerEntityExtendedModel", remap = false)
public abstract class McaPlayerCombatAnimationMixin {
    @Inject(
            method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At("HEAD"),
            remap = false
    )
    private void mcaKccCompat$resetPlayerAnimationPose(
            LivingEntity entity,
            float limbAngle,
            float limbDistance,
            float animationProgress,
            float headYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        KccPlayerAnimationLibraryBridge.resetMcaPlayerModelPose((HumanoidModel<?>) (Object) this);
    }

}
