package enigma.engine.gui;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;

import enigma.engine.DrawableString;
import enigma.engine.TextureStorage;

public class OnOffButton implements UIObject {
	private float x;
	private float y;
	private DrawableString drawStr;
	private Sprite buttonBox;
	private boolean active = false;

	public OnOffButton(String text, float width, float height, float x, float y) {
		this.x = x;
		this.y = y;
		drawStr = new DrawableString(text);

		// buttonBox = new Sprite(TextureStorage.button);
		// buttonBox = new Sprite(TextureStorage.test);
		// buttonBox = new Sprite(TextureStorage.menuBG);
		buttonBox = new Sprite(TextureStorage.unselected);
		float scaleX = width / buttonBox.getWidth();
		float scaleY = height / buttonBox.getHeight();
		buttonBox.setScale(scaleX, scaleY);
		buttonBox.setOriginCenter();
		// buttonBox.setOrigin(buttonBox.getScaleX(), buttonBox.getScaleY()); //note to self: getX()
		// and getY()
		buttonBox.setOrigin(buttonBox.getX(), buttonBox.getY());

		buttonBox.setPosition(x, y);
		adjustButtonBoxToCenter();
		// buttonBox.setTexture(TextureStorage.unselected);

	}

	private void adjustButtonBoxToCenter() {
		if (buttonBox != null) {
			float xAmount = (buttonBox.getWidth() * buttonBox.getScaleX()) / 2;
			float yAmount = (buttonBox.getHeight() * buttonBox.getScaleY()) / 2;
			buttonBox.translate(-xAmount, -yAmount);
		}

	}

	@Override
	public void translate(float x, float y) {
		this.x += x;
		this.y += y;
		setPosition(this.x, this.y);
		// buttonBox.translate(x, y);
		// drawStr.translateX(x);
		// drawStr.translateY(y);
	}

	private void correctAllComponentPositions() {
		adjustButtonBoxToCenter();
	}

	@Override
	public void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
		drawStr.setXY(x, y);
		buttonBox.setPosition(x, y);
		correctAllComponentPositions();
	}

	@Override
	public void draw(SpriteBatch batch) {
		buttonBox.draw(batch);
		drawStr.draw(batch);
	}

	@Override
	public float getX() {
		return x;
	}

	@Override
	public float getY() {
		return y;
	}

	@Override
	public void logic() {

	}

	@Override
	public boolean isTouched(Vector3 touchCoords) {
		float minX = buttonBox.getX();
		float maxX = minX + buttonBox.getWidth() * buttonBox.getScaleX();
		float minY = buttonBox.getY();
		float maxY = minY + buttonBox.getHeight() * buttonBox.getScaleY();

		boolean xInBounds = touchCoords.x >= minX && touchCoords.x <= maxX;
		boolean yInBounds = touchCoords.y >= minY && touchCoords.y <= maxY;
		return xInBounds && yInBounds;
	}

	@Override
	public void makeActive() {
		active = true;
		buttonBox.setTexture(TextureStorage.selected);
	}

	@Override
	public void makeInactive() {
		active = false;
		buttonBox.setTexture(TextureStorage.unselected);
	}

	@Override
	public void activeSelctionLogic() {

	}

	@Override
	public String getValue() {
		return null;
	}

	public boolean getActive() {
		return active;
	}

	public float getWidth() {
		return buttonBox.getWidth() * buttonBox.getScaleX();
	}

}
