package org.kiva.mchangehighlighter.util;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class GLSnapshot {
    boolean wasDepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
    boolean wasBlend = GL11.glIsEnabled(GL11.GL_BLEND);
    boolean wasCullFace = GL11.glIsEnabled(GL11.GL_CULL_FACE);
    boolean wasScissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);
    IntBuffer depthMaskBuf = BufferUtils.createIntBuffer(1);
    int prevDepthMask = depthMaskBuf.get(0);
    ByteBuffer colorMaskBuf = BufferUtils.createByteBuffer(4);
    boolean redMask   = colorMaskBuf.get(0) != 0;
    boolean greenMask = colorMaskBuf.get(1) != 0;
    boolean blueMask  = colorMaskBuf.get(2) != 0;
    boolean alphaMask = colorMaskBuf.get(3) != 0;
    IntBuffer fbBuf = BufferUtils.createIntBuffer(1);
    int prevFramebuffer = fbBuf.get(0);
    IntBuffer boundTexBuf = BufferUtils.createIntBuffer(1);
    int prevBoundTex = 0;
    IntBuffer vp = BufferUtils.createIntBuffer(4);
    int vpX, vpY, vpW, vpH,scX, scY, scW, scH;;
    IntBuffer depthFuncBuf = BufferUtils.createIntBuffer(1);
    IntBuffer sc = BufferUtils.createIntBuffer(4);
    public GLSnapshot(){
        // --- snapshot a lot of GL state ---
        GL11.glGetIntegerv(GL11.GL_DEPTH_WRITEMASK, depthMaskBuf);
        GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colorMaskBuf);
        IntBuffer blendBuf = BufferUtils.createIntBuffer(2);
        blendBuf.clear();
        GL11.glGetIntegerv(GL11.GL_DEPTH_FUNC, depthFuncBuf);
        GL30.glGetIntegerv(GL30.GL_FRAMEBUFFER_BINDING, fbBuf);
        prevFramebuffer = fbBuf.get(0);
        GL11.glGetIntegerv(GL11.GL_TEXTURE_BINDING_2D, boundTexBuf);
        prevBoundTex = boundTexBuf.get(0);
        // viewport
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, vp);
        vpX = vp.get(0);
        vpY = vp.get(1);
        vpW = vp.get(2);
        vpH = vp.get(3);
        // scissor box if enabled
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, sc);
        scX = sc.get(0); scY = sc.get(1); scW = sc.get(2); scH = sc.get(3);
    }

    public void restore(){
        // --- Restore GL state exactly ---
        GL11.glDepthMask(prevDepthMask != 0);
        if (wasDepthTest) GL11.glEnable(GL11.GL_DEPTH_TEST); else GL11.glDisable(GL11.GL_DEPTH_TEST);
        if (wasBlend) GL11.glEnable(GL11.GL_BLEND); else GL11.glDisable(GL11.GL_BLEND);
        if (wasCullFace) GL11.glEnable(GL11.GL_CULL_FACE); else GL11.glDisable(GL11.GL_CULL_FACE);
        if (wasScissor) GL11.glEnable(GL11.GL_SCISSOR_TEST); else GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glColorMask(redMask, greenMask, blueMask, alphaMask);
        // restore program & framebuffer & bound texture & active texture
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFramebuffer);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, prevBoundTex);
        // restore viewport & scissor (in case they were changed inadvertently)
        GL11.glViewport(vpX, vpY, vpW, vpH);
        GL11.glScissor(scX, scY, scW, scH);
    }
}
