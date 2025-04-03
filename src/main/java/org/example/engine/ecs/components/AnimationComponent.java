package org.example.engine.ecs.components;

import org.example.engine.Animation;
import org.example.engine.ecs.Component; /**
 * Component for animated sprites
 */
public class AnimationComponent extends Component {
    private Animation currentAnimation;
    private Animation[] animations;
    private boolean isPlaying = false;
    private String currentAnimationName;
    private final java.util.Map<String, Integer> animationMap = new java.util.HashMap<>();

    public AnimationComponent() {
    }

    public AnimationComponent(Animation animation) {
        this.currentAnimation = animation;
        this.isPlaying = true;
    }

    public AnimationComponent(Animation[] animations, String[] names) {
        this.animations = animations;
        for (int i = 0; i < names.length; i++) {
            animationMap.put(names[i], i);
        }
    }

    public Animation getCurrentAnimation() {
        return currentAnimation;
    }

    public void setCurrentAnimation(Animation animation) {
        this.currentAnimation = animation;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void play() {
        isPlaying = true;
    }

    public void stop() {
        isPlaying = false;
    }

    public void pause() {
        isPlaying = false;
    }

    public void resume() {
        isPlaying = true;
    }

    public void restart() {
        if (currentAnimation != null) {
            currentAnimation.restart();
        }
    }

    public String getCurrentAnimationName() {
        return currentAnimationName;
    }

    public boolean playAnimation(String name) {
        Integer index = animationMap.get(name);
        if (index != null && index < animations.length) {
            currentAnimation = animations[index];
            currentAnimationName = name;
            isPlaying = true;
            return true;
        }
        return false;
    }

    public void update(float deltaTime) {
        if (isPlaying && currentAnimation != null) {
            currentAnimation.update(deltaTime);
        }
    }

    public boolean isFinished() {
        return currentAnimation != null && currentAnimation.isFinished();
    }

    public String getCurrentFrameName() {
        return currentAnimation != null ? currentAnimation.getCurrentFrameName() : null;
    }
}
