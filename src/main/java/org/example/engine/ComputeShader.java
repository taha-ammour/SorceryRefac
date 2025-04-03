package org.example.engine;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL43.*;

public class ComputeShader {
    private final int programId;

    public ComputeShader(String computeShaderSource) {
        int computeShaderId = glCreateShader(GL_COMPUTE_SHADER);
        glShaderSource(computeShaderId, computeShaderSource);
        glCompileShader(computeShaderId);
        if (glGetShaderi(computeShaderId, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Compute shader compilation failed:\n" + glGetShaderInfoLog(computeShaderId));
        }
        programId = glCreateProgram();
        glAttachShader(programId, computeShaderId);
        glLinkProgram(programId);
        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Compute shader linking failed:\n" + glGetProgramInfoLog(programId));
        }
        glDeleteShader(computeShaderId);
    }

    public void use() {
        glUseProgram(programId);
    }

    public void delete() {
        glDeleteProgram(programId);
    }

    public int getProgramId() {
        return programId;
    }

    public void setUniform1f(String name, float value) {
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            glUniform1f(location, value);
        }
    }
    public void setUniform1i(String name, int value) {
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            glUniform1f(location, value);
        }
    }

    public void setUniform3f(String name, float v1,float v2, float v3) {
        int location = glGetUniformLocation(programId, name);
        if (location != -1) {
            glUniform3f(location, v1,v2,v3);
        }
    }
}
