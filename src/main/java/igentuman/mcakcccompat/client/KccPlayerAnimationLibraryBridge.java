package igentuman.mcakcccompat.client;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class KccPlayerAnimationLibraryBridge {
    private static final String MCA_PLAYER_MODEL = "net.conczin.mca.client.model.PlayerEntityExtendedModel";

    private KccPlayerAnimationLibraryBridge() {
    }

    public static void resetMcaPlayerModelPose(HumanoidModel<?> model) {
        if (!isMcaPlayerModel(model) || !ModList.get().isLoaded("kingdom_come_combat")) {
            return;
        }

        PlayerModel<?> playerModel = (PlayerModel<?>) model;
        playerModel.head.resetPose();
        playerModel.body.resetPose();
        playerModel.rightArm.resetPose();
        playerModel.leftArm.resetPose();
        playerModel.rightLeg.resetPose();
        playerModel.leftLeg.resetPose();
        playerModel.hat.resetPose();
        playerModel.jacket.resetPose();
        playerModel.rightSleeve.resetPose();
        playerModel.leftSleeve.resetPose();
        playerModel.rightPants.resetPose();
        playerModel.leftPants.resetPose();
    }

    public static void applyToMcaPlayerModel(LivingEntity entity, PlayerModel<?> model) {
        if (!isMcaPlayerModel(model) || !ModList.get().isLoaded("kingdom_come_combat")) {
            return;
        }

        try {
            if (!(entity instanceof AbstractClientPlayer)) {
                PalApi.SET_MODEL_ANIMATION.invoke(model, (Object) null);
                return;
            }

            Object manager = PalApi.GET_ANIMATION_MANAGER.invoke(entity);
            Object processor = PalApi.GET_ANIMATION_PROCESSOR.invoke(entity);
            boolean active = manager != null
                    && processor != null
                    && (Boolean) PalApi.IS_MANAGER_ACTIVE.invoke(manager);
            PalApi.SET_MODEL_ANIMATION.invoke(model, active ? manager : null);
            if (!active) {
                return;
            }

            Object head = copyVanillaPart(model.head, PalApi.HEAD_BONE.get(model));
            Object torso = copyVanillaPart(model.body, PalApi.TORSO_BONE.get(model));
            Object rightArm = copyVanillaPart(model.rightArm, PalApi.RIGHT_ARM_BONE.get(model));
            Object leftArm = copyVanillaPart(model.leftArm, PalApi.LEFT_ARM_BONE.get(model));
            Object rightLeg = copyVanillaPart(model.rightLeg, PalApi.RIGHT_LEG_BONE.get(model));
            Object leftLeg = copyVanillaPart(model.leftLeg, PalApi.LEFT_LEG_BONE.get(model));

            updatePart(manager, model.head, model.hat, head);
            updatePart(manager, model.rightArm, model.rightSleeve, rightArm);
            updatePart(manager, model.leftArm, model.leftSleeve, leftArm);
            updatePart(manager, model.rightLeg, model.rightPants, rightLeg);
            updatePart(manager, model.leftLeg, model.leftPants, leftLeg);
            updatePart(manager, model.body, model.jacket, torso);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Failed to apply Player Animation Library pose to the MCA player model", exception);
        }
    }

    private static boolean isMcaPlayerModel(HumanoidModel<?> model) {
        return model.getClass().getName().equals(MCA_PLAYER_MODEL);
    }

    private static Object copyVanillaPart(ModelPart modelPart, Object bone)
            throws IllegalAccessException, InvocationTargetException {
        return PalApi.COPY_VANILLA_PART.invoke(null, modelPart, bone);
    }

    private static void updatePart(Object manager, ModelPart part, ModelPart wearPart, Object bone)
            throws IllegalAccessException, InvocationTargetException {
        PalApi.UPDATE_PART.invoke(manager, part, wearPart, bone);
    }

    private static final class PalApi {
        private static final Class<?> PLAYER_ANIM_BONE = type("com.zigythebird.playeranimcore.bones.PlayerAnimBone");
        private static final Method GET_ANIMATION_MANAGER = method(
                "com.zigythebird.playeranim.accessors.IAnimatedPlayer",
                "playerAnimLib$getAnimManager"
        );
        private static final Method GET_ANIMATION_PROCESSOR = method(
                "com.zigythebird.playeranim.accessors.IAnimatedPlayer",
                "playerAnimLib$getAnimProcessor"
        );
        private static final Method IS_MANAGER_ACTIVE = method(
                "com.zigythebird.playeranim.animation.PlayerAnimManager",
                "isActive"
        );
        private static final Method SET_MODEL_ANIMATION = method(
                "com.zigythebird.playeranim.accessors.IMutableModel",
                "playerAnimLib$setAnimation",
                type("com.zigythebird.playeranim.animation.PlayerAnimManager")
        );
        private static final Method COPY_VANILLA_PART = method(
                "com.zigythebird.playeranim.util.RenderUtil",
                "copyVanillaPart",
                ModelPart.class,
                PLAYER_ANIM_BONE
        );
        private static final Method UPDATE_PART = method(
                "com.zigythebird.playeranim.animation.PlayerAnimManager",
                "updatePart",
                ModelPart.class,
                ModelPart.class,
                PLAYER_ANIM_BONE
        );
        private static final Field HEAD_BONE = field("pal$head");
        private static final Field TORSO_BONE = field("pal$torso");
        private static final Field RIGHT_ARM_BONE = field("pal$rightArm");
        private static final Field LEFT_ARM_BONE = field("pal$leftArm");
        private static final Field RIGHT_LEG_BONE = field("pal$rightLeg");
        private static final Field LEFT_LEG_BONE = field("pal$leftLeg");

        private static Class<?> type(String className) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }

        private static Method method(String className, String methodName, Class<?>... parameterTypes) {
            try {
                return type(className).getMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }

        private static Field field(String fieldName) {
            try {
                Field field = PlayerModel.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }
    }
}
