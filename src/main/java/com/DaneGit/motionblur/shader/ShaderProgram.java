package com.DaneGit.motionblur.shader;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.renderer.OpenGlHelper;
import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ShaderProgram {

    private final int programId;

    public ShaderProgram(ResourceLocation vertexShader, ResourceLocation fragmentShader) throws Exception {
        int vertId = createShader(vertexShader, ARBVertexShader.GL_VERTEX_SHADER_ARB);
        int fragId = createShader(fragmentShader, ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);

        programId = ARBShaderObjects.glCreateProgramObjectARB();

        ARBShaderObjects.glAttachObjectARB(programId, vertId);
        ARBShaderObjects.glAttachObjectARB(programId, fragId);
        ARBShaderObjects.glLinkProgramARB(programId);
        ARBShaderObjects.glValidateProgramARB(programId);

        int linked = ARBShaderObjects.glGetObjectParameteriARB(
                programId,
                ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB
        );

        if (linked == 0) {
            throw new Exception("Could not link shader program: " + getLog(programId));
        }
    }

    private int createShader(ResourceLocation location, int type) throws Exception {
        int shaderId = ARBShaderObjects.glCreateShaderObjectARB(type);

        String source = readShader(location);
        ARBShaderObjects.glShaderSourceARB(shaderId, source);
        ARBShaderObjects.glCompileShaderARB(shaderId);

        int compiled = ARBShaderObjects.glGetObjectParameteriARB(
                shaderId,
                ARBShaderObjects.GL_OBJECT_COMPILE_STATUS_ARB
        );

        if (compiled == 0) {
            throw new Exception("Could not compile shader " + location + ": " + getLog(shaderId));
        }

        return shaderId;
    }

    private String readShader(ResourceLocation location) throws Exception {
        IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
        InputStream in = resource.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }

        reader.close();
        return sb.toString();
    }

    private String getLog(int id) {
        return ARBShaderObjects.glGetInfoLogARB(id, 32768);
    }

    public void use() {
        ARBShaderObjects.glUseProgramObjectARB(programId);
    }

    public static void stop() {
        ARBShaderObjects.glUseProgramObjectARB(0);
    }

    public void setUniform1i(String name, int value) {
        int loc = ARBShaderObjects.glGetUniformLocationARB(programId, name);
        if (loc >= 0) {
            ARBShaderObjects.glUniform1iARB(loc, value);
        }
    }

    public void setUniform1f(String name, float value) {
        int loc = ARBShaderObjects.glGetUniformLocationARB(programId, name);
        if (loc >= 0) {
            ARBShaderObjects.glUniform1fARB(loc, value);
        }
    }

    public void setUniform2f(String name, float x, float y) {
        int loc = ARBShaderObjects.glGetUniformLocationARB(programId, name);
        if (loc >= 0) {
            ARBShaderObjects.glUniform2fARB(loc, x, y);
        }
    }
}