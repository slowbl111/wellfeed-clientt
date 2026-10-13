package dev.wellfeed.mixin;

import dev.wellfeed.modules.FreecamModule;
import dev.wellfeed.util.CamState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Freecam: after vanilla positions the camera each frame, move it to the freecam spot.
 * Also records the final camera position and rotation for the ESP projection.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private Vec3d pos;

    @Shadow
    protected abstract void setPos(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(method = "update", at = @At("TAIL"))
    private void wellfeed$update(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView,
                                 float tickProgress, CallbackInfo ci) {
        FreecamModule fc = FreecamModule.INSTANCE;
        if (fc.active()) {
            double[] s = fc.step(MinecraftClient.getInstance());
            setRotation((float) s[3], (float) s[4]);
            setPos(s[0], s[1], s[2]);
        }

        Camera self = (Camera) (Object) this;
        CamState.x = pos.x;
        CamState.y = pos.y;
        CamState.z = pos.z;
        CamState.yaw = self.getYaw();
        CamState.pitch = self.getPitch();
        CamState.valid = true;
    }

    /** Lets you see your own body while the camera is detached. */
    @Inject(method = "isThirdPerson", at = @At("HEAD"), cancellable = true)
    private void wellfeed$showBody(CallbackInfoReturnable<Boolean> cir) {
        FreecamModule fc = FreecamModule.INSTANCE;
        if (fc.active() && fc.showBody()) cir.setReturnValue(true);
    }
}
