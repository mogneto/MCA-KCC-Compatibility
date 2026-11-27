package igentuman.mcakcccompat.mixin.client;

import igentuman.mcakcccompat.client.KccPlayerAnimationLibraryBridge;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelAnimationLibraryBridgeMixin<T extends LivingEntity> {
    @Inject(
            method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At("TAIL")
    )
    private void mcaKccCompat$applyPlayerAnimation(
            LivingEntity entity,
            float limbAngle,
            float limbDistance,
            float animationProgress,
            float headYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        KccPlayerAnimationLibraryBridge.applyToMcaPlayerModel(entity, (PlayerModel<?>) (Object) this);
    }
}
