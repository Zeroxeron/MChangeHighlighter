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
import org.lwjgl.BufferUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.kiva.mchangehighlighter.MChangeHighlighter.toggled_render;
import static org.kiva.mchangehighlighter.MConfig.toggled_seethrough;
import static org.kiva.mchangehighlighter.MChangeHighlighter.ENTRIES;
import static org.kiva.mchangehighlighter.Renderer.loadOutlinesVc;
import org.lwjgl.opengl.GL11;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Mixin(WorldRenderer.class)
public abstract class WorldRendererMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderTail(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f matrix4f, Matrix4f projectionMatrix, GpuBufferSlice fogBuffer, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        if (!MConfig.enabled) return;
        if (!toggled_render) return;
        if (!toggled_seethrough) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        Vec3d cam = camera.getPos();
        VertexConsumerProvider.Immediate consumers = client.getBufferBuilders().getEffectVertexConsumers();
        if (consumers == null) return;
        // --- Snapshot GL state (safe minimal set) ---
        boolean wasDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean wasCullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        IntBuffer depthMaskBuf = BufferUtils.createIntBuffer(1);
        GL11.glGetIntegerv(GL11.GL_DEPTH_WRITEMASK, depthMaskBuf);
        int prevDepthMask = depthMaskBuf.get(0);
        ByteBuffer colorMaskBuf = BufferUtils.createByteBuffer(4);
        GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colorMaskBuf);
        boolean redMask   = colorMaskBuf.get(0) != 0;
        boolean greenMask = colorMaskBuf.get(1) != 0;
        boolean blueMask  = colorMaskBuf.get(2) != 0;
        boolean alphaMask = colorMaskBuf.get(3) != 0;
        IntBuffer blendBuf = BufferUtils.createIntBuffer(4);
        GL11.glGetIntegerv(GL11.GL_BLEND_SRC, blendBuf);
        int prevBlendSrc = blendBuf.get(0);
        GL11.glGetIntegerv(GL11.GL_BLEND_DST, blendBuf.clear());
        int prevBlendDst = blendBuf.get(0);
        IntBuffer depthFuncBuf = BufferUtils.createIntBuffer(1);
        GL11.glGetIntegerv(GL11.GL_DEPTH_FUNC, depthFuncBuf);
        int prevDepthFunc = depthFuncBuf.get(0);
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
        GL11.glDepthMask(prevDepthMask != 0);
        if (wasDepthTest) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (wasBlend) GL11.glEnable(GL11.GL_BLEND); else GL11.glDisable(GL11.GL_BLEND);
        if (wasCullFace) GL11.glEnable(GL11.GL_CULL_FACE); else GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glBlendFunc(prevBlendSrc, prevBlendDst);
        GL11.glColorMask(redMask, greenMask, blueMask, alphaMask);
        GL11.glDepthFunc(prevDepthFunc);
    }
}