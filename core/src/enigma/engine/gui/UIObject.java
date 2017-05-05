package enigma.engine.gui;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;

public interface UIObject {
	public void translate(float x, float y);
	public void setPosition(float x, float y);
	public void draw(SpriteBatch batch);
	public float getX();
	public float getY();
	public void logic();
	public boolean isTouched(Vector3 touchCoords);
	public void makeActive();
	public void makeInactive();
	public void activeSelctionLogic();
	public String getValue();
}
