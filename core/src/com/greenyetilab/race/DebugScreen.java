package com.greenyetilab.race;

import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.VerticalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.greenyetilab.utils.anchor.Anchor;
import com.greenyetilab.utils.anchor.AnchorGroup;

/**
 * The debug screen
 */
public class DebugScreen extends com.greenyetilab.utils.StageScreen {
    private final RaceGame mGame;

    public DebugScreen(RaceGame game) {
        mGame = game;
        Skin skin = mGame.getAssets().skin;

        AnchorGroup root = new AnchorGroup();
        root.setFillParent(true);
        getStage().addActor(root);

        VerticalGroup vGroup = new VerticalGroup();
        CheckBox cb;
        cb = new CheckBox("Box2D Debug", skin);
        connectCheckBox(cb, "debug/enabled");
        vGroup.addActor(cb);
        cb = new CheckBox("Box2D Debug: Draw Velocities", skin);
        connectCheckBox(cb, "debug/drawVelocities");
        vGroup.addActor(cb);
        vGroup.setSize(vGroup.getPrefWidth(), vGroup.getPrefHeight());

        TextButton backButton = new TextButton("Back", skin, "default");
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                mGame.popScreen();
            }
        });

        root.addPositionRule(vGroup, Anchor.TOP_CENTER, root, Anchor.TOP_CENTER);
        root.addPositionRule(backButton, Anchor.BOTTOM_LEFT, root, Anchor.BOTTOM_LEFT);
    }

    private void connectCheckBox(final CheckBox checkBox, final String key) {
        final Preferences prefs = RaceGame.getPreferences();
        checkBox.setChecked(prefs.getBoolean(key, false));
        checkBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                prefs.putBoolean(key, checkBox.isChecked());
                prefs.flush();
            }
        });
    }
}
