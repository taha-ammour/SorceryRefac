package org.example.engine;

import org.joml.Vector3f;

public class Material {
    private Texture diffuse;
    private Vector3f ambient;
    private Vector3f specular;
    private float shininess;

    public Material() {
        // Default values (you can tweak these)
        ambient = new Vector3f(0.1f, 0.1f, 0.1f);
        specular = new Vector3f(0.5f, 0.5f, 0.5f);
        shininess = 32.0f;
    }

    public Texture getDiffuse() {
        return diffuse;
    }

    public void setDiffuse(Texture diffuse) {
        this.diffuse = diffuse;
    }

    public Vector3f getAmbient() {
        return ambient;
    }

    public void setAmbient(Vector3f ambient) {
        this.ambient.set(ambient);
    }

    public Vector3f getSpecular() {
        return specular;
    }

    public void setSpecular(Vector3f specular) {
        this.specular.set(specular);
    }

    public float getShininess() {
        return shininess;
    }

    public void setShininess(float shininess) {
        this.shininess = shininess;
    }
}
