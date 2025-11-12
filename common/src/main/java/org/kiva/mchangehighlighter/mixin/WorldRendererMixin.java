package org.kiva.mchangehighlighter.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.kiva.mchangehighlighter.HighlightEntry;
import org.kiva.mchangehighlighter.MConfig;
import org.kiva.mchangehighlighter.util.GLSnapshot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.kiva.mchangehighlighter.MChangeHighlighter.*;
import static org.kiva.mchangehighlighter.Renderer.loadOutlinesVc;
import org.lwjgl.opengl.GL11;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    /** Only overworld for now (black-screens in the nether and in the end?) **/
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f matrix4f, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        if (!MConfig.enabled) return;
        if (!toggled_render) return;
        if (!toggled_seethrough) return;
        if (!GL_safe) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        Vec3d cam = camera.getPos();
        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEffectVertexConsumers();
        if (consumers == null) return;
        // --- Snapshot GL state (safe minimal set) ---
        GLSnapshot gls = new GLSnapshot();
        //--- Outlining ---
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glIsEnabled(GL11.GL_BLEND); // Enable blending so alpha works
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        VertexConsumer vc = consumers.getBuffer(RenderLayer.getLines());
        if (!ENTRIES.isEmpty()) {
            for (HighlightEntry e : ENTRIES) {loadOutlinesVc(vc, positionMatrix, new Vec3d(cam.x, cam.y, cam.z), e);}
            consumers.draw();
        }
        // --- Restore GL state exactly ---
        gls.restore();
    }
}