package com.greenyetilab.race;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;

/**
 * Responsible for rendering the game world
 */
public class GameRenderer {
    private static final float VIEWPORT_WIDTH = 60;

    public static class DebugConfig {
        public boolean enabled = false;
        public boolean drawVelocities = false;
        public boolean drawTileCorners = false;
    }
    private DebugConfig mDebugConfig = new DebugConfig();

    private final TiledMap mMap;
    private final OrthogonalTiledMapRenderer mRenderer;
    private final Box2DDebugRenderer mDebugRenderer;
    private final Batch mBatch;
    private final OrthographicCamera mCamera;
    private final ShapeRenderer mShapeRenderer = new ShapeRenderer();
    private final GameWorld mWorld;
    private final float mMapWidth;
    private final float mMapHeight;

    private int[] mBackgroundLayerIndexes = { 0 };
    private int[] mForegroundLayerIndexes;
    private Vehicle mVehicle;

    public GameRenderer(GameWorld world, Batch batch) {
        mDebugRenderer = new Box2DDebugRenderer();
        mWorld = world;

        mMap = mWorld.getMap();
        TiledMapTileLayer layer = (TiledMapTileLayer) mMap.getLayers().get(0);
        mMapWidth = Constants.UNIT_FOR_PIXEL * layer.getWidth() * layer.getTileWidth();
        mMapHeight = Constants.UNIT_FOR_PIXEL * layer.getHeight() * layer.getTileHeight();

        if (mMap.getLayers().get("Walls") != null) {
            mForegroundLayerIndexes = new int[]{ 1 };
        }

        mBatch = batch;
        mCamera = new OrthographicCamera();
        mRenderer = new OrthogonalTiledMapRenderer(mMap, Constants.UNIT_FOR_PIXEL, mBatch);

        mVehicle = mWorld.getVehicle();
    }

    public void setDebugConfig(DebugConfig config) {
        mDebugConfig = config;
        mDebugRenderer.setDrawVelocities(mDebugConfig.drawVelocities);
    }

    public void render() {
        updateCamera();

        mRenderer.setView(mCamera);
        mRenderer.render(mBackgroundLayerIndexes);

        renderSkidmarks();

        mBatch.setProjectionMatrix(mCamera.combined);
        mBatch.begin();
        mVehicle.draw(mBatch);
        mBatch.end();

        if (mForegroundLayerIndexes != null) {
            mRenderer.render(mForegroundLayerIndexes);
        }

        mBatch.begin();
        for (GameObject object : mWorld.getActiveGameObjects()) {
            object.draw(mBatch);
        }
        mBatch.end();

        if (mDebugConfig.enabled) {
            mShapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            mShapeRenderer.setProjectionMatrix(mCamera.combined);
            if (mDebugConfig.drawTileCorners) {
                mShapeRenderer.setColor(1, 1, 1, 1);
                TiledMapTileLayer layer = (TiledMapTileLayer) mMap.getLayers().get(0);
                float tileW = Constants.UNIT_FOR_PIXEL * layer.getTileWidth();
                float tileH = Constants.UNIT_FOR_PIXEL * layer.getTileHeight();
                for (float y = 0; y < mMapHeight; y += tileH) {
                    for (float x = 0; x < mMapWidth; x += tileW) {
                        mShapeRenderer.rect(x, y, Constants.UNIT_FOR_PIXEL, Constants.UNIT_FOR_PIXEL);
                    }
                }
            }
            mShapeRenderer.setColor(0, 0, 1, 1);
            mShapeRenderer.rect(mVehicle.getX(), mVehicle.getY(), Constants.UNIT_FOR_PIXEL, Constants.UNIT_FOR_PIXEL);
            mShapeRenderer.end();

            mDebugRenderer.render(mWorld.getBox2DWorld(), mCamera.combined);
        }
    }

    private void renderSkidmarks() {
        mShapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        mShapeRenderer.setColor(0, 0, 0, 0.2f);
        mShapeRenderer.setProjectionMatrix(mCamera.combined);
        for (Vector2 pos: mWorld.getSkidmarks()) {
            if (pos != null) {
                mShapeRenderer.circle(pos.x, pos.y, 4 * Constants.UNIT_FOR_PIXEL, 8);
            }
        }
        mShapeRenderer.end();
    }

    private void updateCamera() {
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        mCamera.viewportWidth = VIEWPORT_WIDTH;
        mCamera.viewportHeight = VIEWPORT_WIDTH * screenH / screenW;

        // Compute pos
        // FIXME: Take car speed into account when computing advance
        float advance = /*(mCar.getSpeed() / Car.MAX_SPEED) **/ Math.min(mCamera.viewportWidth, mCamera.viewportHeight) / 3;
        float x = mVehicle.getX() + advance * MathUtils.cosDeg(mVehicle.getAngle());
        float y = mVehicle.getY() + advance * MathUtils.sinDeg(mVehicle.getAngle());

        // Make sure we correctly handle boundaries
        float minX = mCamera.viewportWidth / 2;
        float minY = mCamera.viewportHeight / 2;
        float maxX = mMapWidth - mCamera.viewportWidth / 2;
        float maxY = mMapHeight - mCamera.viewportHeight / 2;

        if (mCamera.viewportWidth <= mMapWidth) {
            mCamera.position.x = MathUtils.clamp(x, minX, maxX);
        } else {
            mCamera.position.x = mMapWidth / 2;
        }
        if (mCamera.viewportHeight <= mMapHeight) {
            mCamera.position.y = MathUtils.clamp(y, minY, maxY);
        } else {
            mCamera.position.y = mMapHeight / 2;
        }
        mCamera.update();
    }

    public void onScreenResized() {
        updateCamera();
    }
}