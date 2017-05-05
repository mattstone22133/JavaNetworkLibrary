package enigma.engine.gui;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;

import enigma.engine.TextureStorage;

public class Button implements UIObject {
	private Sprite button;
	private boolean shouldHide = false;
	private long delayToStartDraw = 1000;
	private long callToStartDrawingInitialTime = 0;

	public Button() {
		button = new Sprite(TextureStorage.button);
	}

	public void draw(SpriteBatch batch) {
		if (button != null && !shouldHide) {
			boolean shouldDraw = System.currentTimeMillis() - delayToStartDraw > callToStartDrawingInitialTime;
			if (shouldDraw) {
				button.draw(batch);
			}
		}
	}

	private boolean timeDelayedEnough() {
		// below would be more efficient if there were a boolean flag to be set once this is true,
		// thus avoiding the arthmetic everytime this is called
		long currTime = System.currentTimeMillis();
		long correctedTime = currTime - delayToStartDraw;
		// boolean result = (currTime - delayToStartDraw) > callToStartDrawingInitialTime;
		boolean result = correctedTime > callToStartDrawingInitialTime;
		return result;
	}

	public void setXY(float x, float y) {
		button.setX(x);
		button.setY(y);
	}

	public void setCenterXY(float x, float y) {
		button.setX(x - widthScaled() / 2);
		button.setY(y - heightScaled() / 2);
	}

	public void setScale(float xyScale) {
		button.setScale(xyScale);
		button.setOrigin(button.getScaleX(), button.getScaleY());
	}

	public float widthScaled() {
		return button.getWidth() * button.getScaleX();
	}

	public float heightScaled() {
		return button.getHeight() * button.getScaleY();
	}

	private long timeLastUpdated = System.currentTimeMillis();
	private long delay1 = 250; // 1000ms = 1sec

	public void logic() {
		// change the way the image looks to let user know it is a button
		if (timeLastUpdated < System.currentTimeMillis() - delay1) {
			if (button.getTexture() == TextureStorage.button) {
				button.setTexture(TextureStorage.buttonPressed);
			} else {
				button.setTexture(TextureStorage.button);
			}
			timeLastUpdated = System.currentTimeMillis();
		}
	}

	public void setHiddenTo(boolean value) {
		if (value != shouldHide) {
			shouldHide = value;
			// second statement prevents updating of time if the value was already set
			if (!value) {
				callToStartDrawingInitialTime = System.currentTimeMillis();
			}
		}
	}

	public boolean buttonActive() {
		return !shouldHide && timeDelayedEnough();
	}

	public void translate(float x, float y) {
		// translate the button background
		button.translate(x, y);

		// translate the drawable string
	}

	@Override
	public void setPosition(float x, float y) {
		// TODO Auto-generated method stub

	}

	@Override
	public float getX() {
		return button.getX();
	}

	@Override
	public float getY() {
		return button.getY();
	}

	@Override
	public boolean isTouched(Vector3 touchCoords) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void makeActive() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void makeInactive() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void activeSelctionLogic() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getValue() {
		// TODO Auto-generated method stub
		return null;
	}
}
