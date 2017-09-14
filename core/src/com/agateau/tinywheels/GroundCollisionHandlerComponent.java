package com.agateau.tinywheels;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * Handles collisions
 */
public class GroundCollisionHandlerComponent implements Racer.Component {
    private static final float LIFTING_DELAY = 0.5f;
    private static final float MAX_RECOVERING_SPEED = 20;
    private static final float MAX_RECOVERING_ROTATION_SPEED = 720;

    private final Assets mAssets;
    private final GameWorld mGameWorld;
    private final Vehicle mVehicle;
    private final LapPositionComponent mLapPositionComponent;
    private final MapInfo mMapInfo;
    private OrientedPoint mDropPoint;
    private final Vector2 mVelocity = new Vector2();
    private Helicopter mHelicopter = null;

    public enum State {
        NORMAL,
        FALLING,
        LIFTING,
        RECOVERING,
        DROPPING
    }

    private State mState = State.NORMAL;
    private float mTime;

    public GroundCollisionHandlerComponent(Assets assets, GameWorld gameWorld, Vehicle vehicle, LapPositionComponent lapPositionComponent) {
        mAssets = assets;
        mGameWorld = gameWorld;
        mVehicle = vehicle;
        mMapInfo = gameWorld.getMapInfo();
        mLapPositionComponent = lapPositionComponent;
    }

    public State getState() {
        return mState;
    }

    @Override
    public void act(float delta) {
        switch (mState) {
        case NORMAL:
            actNormal();
            break;
        case FALLING:
            actFalling(delta);
            break;
        case LIFTING:
            actLifting(delta);
            break;
        case RECOVERING:
            actRecovering(delta);
            break;
        case DROPPING:
            actDropping(delta);
            break;
        }
    }

    private void actNormal() {
        int wheelsInHole = 0;
        for(Vehicle.WheelInfo info: mVehicle.getWheelInfos()) {
            Wheel wheel = info.wheel;
            if (wheel.getGroundSpeed() == 0) {
                ++wheelsInHole;
            }
        }
        if (wheelsInHole >= mVehicle.getWheelInfos().size / 2) {
            startFalling();
        }
    }

    private void startFalling() {
        mHelicopter = Helicopter.create(mAssets, mGameWorld.getMapInfo(), mVehicle.getPosition(), mVehicle.getAngle());
        mGameWorld.addGameObject(mHelicopter);
        mState = State.FALLING;
        mVehicle.setStopped(true);
    }

    private void actFalling(float delta) {
        // TODO: Implement falling animation
        mHelicopter.setEndPosition(mVehicle.getPosition());
        if (mHelicopter.isReadyToRecover()) {
            mState = State.LIFTING;
            mTime = 0;
        }
    }

    private void actLifting(float delta) {
        mTime += delta;
        if (mTime >= LIFTING_DELAY) {
            mTime = LIFTING_DELAY;
            startRecovering();
        }
        mVehicle.setZ(Interpolation.pow2.apply(mTime / LIFTING_DELAY));
    }

    private void startRecovering() {
        mState = State.RECOVERING;
        float distance = mLapPositionComponent.getLapDistance();
        mDropPoint = mMapInfo.getValidPosition(mVehicle.getBody().getWorldCenter(), distance);
    }

    private void actRecovering(float delta) {
        final float POSITION_TOLERANCE = 0.1f;
        final float ANGLE_TOLERANCE = MathUtils.degreesToRadians;

        mVelocity.set(mDropPoint.x, mDropPoint.y).sub(mVehicle.getBody().getPosition()).scl(1 / delta);
        float speed = mVelocity.len();
        if (speed > MAX_RECOVERING_SPEED) {
            mVelocity.scl(MAX_RECOVERING_SPEED / speed);
        }

        float angularVelocity = MathUtils.clamp((mDropPoint.angle - mVehicle.getAngle()) / delta,
                -MAX_RECOVERING_ROTATION_SPEED, MAX_RECOVERING_ROTATION_SPEED) * MathUtils.degreesToRadians;

        boolean posOK = MathUtils.isZero(speed * delta, POSITION_TOLERANCE);
        boolean angleOK = MathUtils.isZero(angularVelocity * delta, ANGLE_TOLERANCE);

        if (posOK) {
            mVehicle.getBody().setLinearVelocity(0, 0);
            mVehicle.getBody().setAngularVelocity(0);
            mState = State.DROPPING;
            mTime = 0;
        } else {
            mVehicle.getBody().setLinearVelocity(mVelocity);
            mVehicle.getBody().setAngularVelocity(angleOK ? 0 : angularVelocity);
            mHelicopter.setPosition(mVehicle.getPosition());
            mHelicopter.setAngle(mVehicle.getAngle());
        }
    }

    private void actDropping(float delta) {
        mTime += delta;
        mVehicle.setZ(Interpolation.bounceOut.apply(1, 0, mTime / LIFTING_DELAY));
        if (mTime >= LIFTING_DELAY) {
            mTime = LIFTING_DELAY;
            mHelicopter.leave();
            mVehicle.setZ(0);
            mVehicle.setStopped(false);
            mState = State.NORMAL;
        }
    }
}
