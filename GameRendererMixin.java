package dev.wellfeed.mixin;

import dev.wellfeed.modules.ZoomModule;
import dev.wellfeed.util.CamState;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Zoom: scales the world field of view by the smoothed zoom multiplier.
 * Also records the final FOV so the ESP projects with exactly what is on screen
 * (sprint FOV, potion FOV and zoom all included).
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void wellfeed$fov(Camera camera, float tickProgress, boolean changingFov,
                              CallbackInfoReturnable<Float> cir) {
        if (!changingFov) return; // the hand uses its own, unchanged FOV
        float fov = cir.getReturnValueF() * ZoomModule.INSTANCE.multiplier();
        CamState.fov = fov;
        cir.setReturnValue(fov);
    }
}
