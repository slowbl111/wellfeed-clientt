package dev.wellfeed.mixin;

import dev.wellfeed.util.CameraRig;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Freecam / Cinematic hooks.
 *  - update(): move the rig's camera entity once per rendered frame, right before the camera reads it.
 *  - isThirdPerson(): report true while the rig is active so the floating first-person hand is hidden.
 *
 * require = 0: if a name ever changes the mod still loads (the rig then falls back to a 20 Hz tick update).
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Inject(method = "update", at = @At("HEAD"), require = 0)
    private void wellfeed$rigFrame(CallbackInfo ci) {
        CameraRig.frame();
    }

    @Inject(method = "isThirdPerson", at = @At("HEAD"), cancellable = true, require = 0)
    private void wellfeed$hideHand(CallbackInfoReturnable<Boolean> cir) {
        if (CameraRig.active()) cir.setReturnValue(true);
    }
}
