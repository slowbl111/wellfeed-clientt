package dev.wellfeed.mixin;

import dev.wellfeed.modules.FullbrightModule;
import dev.wellfeed.modules.ZoomModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Overrides two option reads without ever touching the saved values:
 *  - Fullbright: gamma reads as 16.0 while the module is on.
 *  - Zoom: mouse sensitivity is scaled down while zoomed in.
 */
@Mixin(SimpleOption.class)
public abstract class SimpleOptionMixin<T> {

    @SuppressWarnings("unchecked")
    @Inject(method = "getValue", at = @At("RETURN"), cancellable = true)
    private void wellfeed$overrides(CallbackInfoReturnable<T> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.options == null) return;

        if (FullbrightModule.active() && (Object) this == mc.options.getGamma()) {
            cir.setReturnValue((T) Double.valueOf(16.0));
            return;
        }
        if (ZoomModule.INSTANCE.zoomed() && mc.currentScreen == null
                && (Object) this == mc.options.getMouseSensitivity()) {
            Object v = cir.getReturnValue();
            if (v instanceof Double d) {
                cir.setReturnValue((T) Double.valueOf(d * ZoomModule.INSTANCE.sensitivityScale()));
            }
        }
    }
}
