package org.example.engine.ecs.components;

import org.example.engine.ecs.Component; /**
 * Component for AI behaviors
 */
public class AIComponent extends Component {
    public enum AIType {
        PATROL,
        FOLLOW,
        WANDER,
        GUARD,
        FLEE
    }

    private AIType aiType = AIType.PATROL;
    private float detectionRadius = 100f;
    private float attackRadius = 50f;
    private float moveSpeed = 60f;
    private String targetTag = "player";
    private float aiTimer = 0f;
    private float actionInterval = 1.0f;

    // For patrol
    private org.joml.Vector2f[] patrolPoints;
    private int currentPatrolPoint = 0;

    // For state tracking
    private boolean hasTargetInSight = false;
    private org.joml.Vector2f lastKnownTargetPos = new org.joml.Vector2f();

    public AIComponent(AIType aiType) {
        this.aiType = aiType;
    }

    public AIType getAiType() {
        return aiType;
    }

    public void setAiType(AIType aiType) {
        this.aiType = aiType;
    }

    public float getDetectionRadius() {
        return detectionRadius;
    }

    public void setDetectionRadius(float detectionRadius) {
        this.detectionRadius = detectionRadius;
    }

    public float getAttackRadius() {
        return attackRadius;
    }

    public void setAttackRadius(float attackRadius) {
        this.attackRadius = attackRadius;
    }

    public float getMoveSpeed() {
        return moveSpeed;
    }

    public void setMoveSpeed(float moveSpeed) {
        this.moveSpeed = moveSpeed;
    }

    public String getTargetTag() {
        return targetTag;
    }

    public void setTargetTag(String targetTag) {
        this.targetTag = targetTag;
    }

    public org.joml.Vector2f[] getPatrolPoints() {
        return patrolPoints;
    }

    public void setPatrolPoints(org.joml.Vector2f[] patrolPoints) {
        this.patrolPoints = patrolPoints;
    }

    public int getCurrentPatrolPoint() {
        return currentPatrolPoint;
    }

    public void setCurrentPatrolPoint(int currentPatrolPoint) {
        this.currentPatrolPoint = currentPatrolPoint;
    }

    public float getAiTimer() {
        return aiTimer;
    }

    public void updateTimer(float deltaTime) {
        aiTimer += deltaTime;
    }

    public boolean shouldAct() {
        if (aiTimer >= actionInterval) {
            aiTimer = 0;
            return true;
        }
        return false;
    }

    public float getActionInterval() {
        return actionInterval;
    }

    public void setActionInterval(float actionInterval) {
        this.actionInterval = actionInterval;
    }

    public boolean hasTargetInSight() {
        return hasTargetInSight;
    }

    public void setTargetInSight(boolean hasTargetInSight) {
        this.hasTargetInSight = hasTargetInSight;
    }

    public org.joml.Vector2f getLastKnownTargetPos() {
        return lastKnownTargetPos;
    }

    public void setLastKnownTargetPos(org.joml.Vector2f lastKnownTargetPos) {
        this.lastKnownTargetPos.set(lastKnownTargetPos);
    }

    public void setLastKnownTargetPos(float x, float y) {
        this.lastKnownTargetPos.set(x, y);
    }
}
