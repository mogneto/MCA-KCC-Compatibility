package igentuman.mcakcccompat.client;

import igentuman.mcakcccompat.McaKccCompat;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class KccCombatAnimationBridge {
    private static final ConcurrentMap<Integer, Boolean> LAST_ACTIVE_LAYER_STATES = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Integer, String> LAST_LAYER_SNAPSHOTS = new ConcurrentHashMap<>();

    private KccCombatAnimationBridge() {
    }

    public static void apply(LivingEntity entity, HumanoidModel<?> model) {
        if (!ModList.get().isLoaded("kingdom_come_combat")) {
            return;
        }

        try {
            int entityId = entity.getId();
            boolean active = (Boolean) CombatAnimationApi.HAS_ACTIVE_LAYER.invoke(null, entityId);
            String layers = describeLayers(entityId);
            Boolean previous = LAST_ACTIVE_LAYER_STATES.put(entityId, active);
            String previousLayers = LAST_LAYER_SNAPSHOTS.put(entityId, layers);
            if (previous == null || previous != active) {
                McaKccCompat.LOGGER.debug("KCC combat layer for MCA entity {}: {}", entityId, active ? "active" : "inactive");
            }
            boolean layersChanged = !Objects.equals(previousLayers, layers);
            if (layersChanged) {
                McaKccCompat.LOGGER.debug("KCC layers for MCA entity {}: {}", entityId, layers);
            }
            if (!active) {
                return;
            }

            float rightArmBefore = model.rightArm.xRot;
            float leftArmBefore = model.leftArm.xRot;
            CombatAnimationApi.APPLY.invoke(null, model, entity);
            if (layersChanged || previous == null || !previous) {
                McaKccCompat.LOGGER.debug(
                        "KCC pose applied to MCA entity {}: right arm {} -> {}, left arm {} -> {}",
                        entityId,
                        rightArmBefore,
                        model.rightArm.xRot,
                        leftArmBefore,
                        model.leftArm.xRot
                );
            }
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Failed to apply Kingdom Come Combat animation to an MCA model", exception);
        }
    }

    private static String describeLayers(int entityId) throws IllegalAccessException, InvocationTargetException {
        List<?> layers = (List<?>) CombatAnimationApi.GET_LAYERS.invoke(null, entityId);
        StringBuilder description = new StringBuilder("[");
        for (Object layer : layers) {
            if (description.length() > 1) {
                description.append(", ");
            }
            description.append(CombatAnimationApi.LAYER_KIND.invoke(layer))
                    .append('/')
                    .append(CombatAnimationApi.LAYER_DIRECTION.invoke(layer));
            Object animationName = CombatAnimationApi.LAYER_ANIMATION_NAME.invoke(layer);
            if (animationName != null && !animationName.toString().isBlank()) {
                description.append('/').append(animationName);
            }
        }
        return description.append(']').toString();
    }

    private static final class CombatAnimationApi {
        private static final Method HAS_ACTIVE_LAYER = method(
                "com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState",
                "hasActiveCombatLayer",
                int.class
        );
        private static final Method GET_LAYERS = method(
                "com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState",
                "getLayers",
                int.class
        );
        private static final Method LAYER_KIND = method(
                "com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState$ActiveAnimation",
                "kind"
        );
        private static final Method LAYER_DIRECTION = method(
                "com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState$ActiveAnimation",
                "direction"
        );
        private static final Method LAYER_ANIMATION_NAME = method(
                "com.kingdomcomecombat.client.animation.ClientEntityGeckoAnimationState$ActiveAnimation",
                "customAnimationName"
        );
        private static final Method APPLY = method(
                "com.kingdomcomecombat.client.animation.LegacyBipedAnimationApplier",
                "apply",
                HumanoidModel.class,
                LivingEntity.class
        );

        private static Method method(String className, String methodName, Class<?>... parameterTypes) {
            try {
                return Class.forName(className).getMethod(methodName, parameterTypes);
            } catch (ClassNotFoundException | NoSuchMethodException exception) {
                throw new ExceptionInInitializerError(exception);
            }
        }
    }
}
