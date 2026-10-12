package net.wellfeed;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.wellfeed.gui.ClickGuiScreen;
import net.wellfeed.module.ModuleManager;
import net.wellfeed.module.impl.AutoMiner;
import org.lwjgl.glfw.GLFW;

public class WellfeedClient implements ClientModInitializer {
    private static KeyBinding openGui;

    @Override
    public void onInitializeClient() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("wellfeed", "main"));
        openGui = KeyBindingHelper.registerKeyBinding(
                new KeyBinding("key.wellfeed.gui", GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGui.wasPressed()) {
                if (client.currentScreen == null) client.setScreen(new ClickGuiScreen());
            }
            ModuleManager.INSTANCE.tick(client);
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            var root = ClientCommandManager.literal("wf");
            root.then(corner("pos1", true));
            root.then(corner("pos2", false));
            root.then(ClientCommandManager.literal("start").executes(ctx -> {
                ModuleManager.INSTANCE.autoMiner.setEnabled(true);
                return 1;
            }));
            root.then(ClientCommandManager.literal("stop").executes(ctx -> {
                ModuleManager.INSTANCE.autoMiner.setEnabled(false);
                return 1;
            }));
            root.then(ClientCommandManager.literal("clear").executes(ctx -> {
                AutoMiner m = ModuleManager.INSTANCE.autoMiner;
                m.setEnabled(false);
                m.pos1 = null;
                m.pos2 = null;
                reply(ctx, "Region cleared.");
                return 1;
            }));
            dispatcher.register(root);
        });
    }

    /** /wf pos1 (uses your position) or /wf pos1 <x> <y> <z> */
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<FabricClientCommandSource> corner(String name, boolean first) {
        return ClientCommandManager.literal(name)
                .executes(ctx -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc.player == null) return 0;
                    setCorner(ctx, first, mc.player.getBlockPos());
                    return 1;
                })
                .then(ClientCommandManager.argument("x", IntegerArgumentType.integer())
                        .then(ClientCommandManager.argument("y", IntegerArgumentType.integer())
                                .then(ClientCommandManager.argument("z", IntegerArgumentType.integer())
                                        .executes(ctx -> {
                                            BlockPos p = new BlockPos(
                                                    IntegerArgumentType.getInteger(ctx, "x"),
                                                    IntegerArgumentType.getInteger(ctx, "y"),
                                                    IntegerArgumentType.getInteger(ctx, "z"));
                                            setCorner(ctx, first, p);
                                            return 1;
                                        }))));
    }

    private static void setCorner(CommandContext<FabricClientCommandSource> ctx, boolean first, BlockPos p) {
        AutoMiner m = ModuleManager.INSTANCE.autoMiner;
        if (first) m.pos1 = p;
        else m.pos2 = p;
        String extra = m.hasRegion() ? " Region: " + m.regionVolume() + " blocks." : "";
        reply(ctx, (first ? "Pos1" : "Pos2") + " = " + p.getX() + " " + p.getY() + " " + p.getZ() + "." + extra);
    }

    private static void reply(CommandContext<FabricClientCommandSource> ctx, String msg) {
        ctx.getSource().sendFeedback(Text.literal("[Wellfeed] " + msg));
    }
}
