package enigma.engine.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;

import enigma.engine.DrawableString;
import enigma.engine.TextureStorage;

public class TextField implements UIObject {
	private StringBuffer permString;
	private StringBuffer value;
	private DrawableString permStr;
	private DrawableString drawStr;
	private DrawableString cursor;
	private Sprite box;
	private boolean drawCursor = true;
	private long lastCursorTime = System.currentTimeMillis();
	private long delayMS = 500;

	float x;
	float y;
	private boolean active = false;

	public TextField(String permanantStr, String defaultValue, float width, float height, float x, float y) {
		this.x = x;
		this.y = y;
		this.permString = new StringBuffer(permanantStr);
		this.value = new StringBuffer(defaultValue);
		permStr = new DrawableString(this.permString.toString());
		drawStr = new DrawableString(this.value.toString());

		// Set up background box
		box = new Sprite(TextureStorage.button);
		// box = new Sprite(TextureStorage.unselected);

		box.setPosition(x, y);
		float scaleX = width / box.getWidth();
		float scaleY = height / box.getHeight();
		box.setScale(scaleX, scaleY);
		box.setOrigin(box.getScaleX(), box.getScaleY());
		adjustBoxToCenter();

		// set up text
		permStr.setXY(x, y);
		adjustPermToPosition();

		drawStr.setXY(x, y);
		adjustDrawToPosition();

		cursor = new DrawableString("|");
		cursor.setXY(x, y);
		adjustCursorToPosition();

	}

	private void adjustCursorToPosition() {
		float xPos = drawStr.getX();
		xPos += drawStr.width() * 0.5f;
		cursor.setXY(xPos, cursor.getY());
	}

	private void adjustBoxToCenter() {
		if (box != null) {
			float xAmount = (box.getWidth() * box.getScaleX()) / 2;
			float yAmount = (box.getHeight() * box.getScaleY()) / 2;
			box.translate(-xAmount, -yAmount);
		}

	}

	private void adjustPermToPosition() {
		float boxScaledWidth = (box.getWidth() * box.getScaleX());
		float xAmount = -boxScaledWidth / 2;// + permStr.width();// + 0.05f * boxScaledWidth;
		xAmount += permStr.width() / 2;
		xAmount += boxScaledWidth * 0.05f;
		float yAmount = 0;
		permStr.translateX(xAmount);
		permStr.translateY(yAmount);
	}

	private void adjustDrawToPosition() {
		float xPosition = permStr.getX();
		xPosition += permStr.width() / 2;
		xPosition += drawStr.width() / 2;
		float yPosition = drawStr.getY();
		drawStr.setXY(xPosition, yPosition);

	}

	private void adjustAllComponents() {
		adjustBoxToCenter();
		adjustPermToPosition();
		adjustDrawToPosition();
		adjustCursorToPosition();
	}

	public void draw(SpriteBatch batch) {
		box.draw(batch);
		permStr.draw(batch);
		drawStr.draw(batch);
		if (active && drawCursor) {
			cursor.draw(batch);
		}
	}

	@Override
	public void translate(float x, float y) {
		box.translate(x, y);
		permStr.translateX(x);
		permStr.translateY(y);
		drawStr.translateX(x);
		drawStr.translateY(y);

	}

	@Override
	public void setPosition(float x, float y) {
		this.x = x;
		this.y = y;
		box.setPosition(x, y);
		drawStr.setXY(x, y);
		permStr.setXY(x, y);
		cursor.setXY(x, y);
		adjustAllComponents();

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
		if (active) {
			long currTime = System.currentTimeMillis();
			if (currTime - lastCursorTime > delayMS) {
				lastCursorTime = currTime;
				drawCursor = !drawCursor;
			}
		}
	}

	@Override
	public boolean isTouched(Vector3 touchCoords) {
		float minX = box.getX();
		float maxX = minX + box.getWidth() * box.getScaleX();
		float minY = box.getY();
		float maxY = minY + box.getHeight() * box.getScaleY();

		boolean xInBounds = touchCoords.x >= minX && touchCoords.x <= maxX;
		boolean yInBounds = touchCoords.y >= minY && touchCoords.y <= maxY;
		return xInBounds && yInBounds;
	}

	@Override
	public void makeActive() {
		active = true;
	}

	@Override
	public void makeInactive() {
		active = false;
	}

	@Override
	public void activeSelctionLogic() {
		if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) && value.length() > 0) {
			value.setLength(value.length() - 1);
			drawStr.setText(value.toString());
			setPosition(x, y);
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.PERIOD)) {
			value.append('.');
			drawStr.setText(value.toString());
			setPosition(x, y);
		}
		if ((Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT)) && Gdx.input.isKeyJustPressed(Input.Keys.SEMICOLON)) {
			value.append(':');
			drawStr.setText(value.toString());
			setPosition(x, y);
		}

		if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_0) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_0)) {
			value.append('0');
			drawStr.setText(value.toString());
			setPosition(x, y);
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_1)) {
			value.append('1');
			drawStr.setText(value.toString());
			setPosition(x, y);
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_2)) {
			value.append('2');
			drawStr.setText(value.toString());
			setPosition(x, y);
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_3)) {
			value.append('3');
			drawStr.setText(value.toString());
			setPosition(x, y);
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_4)) {
			value.append('4');
			drawStr.setText(value.toString());
			setPosition(x, y);
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_5) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_5)) {
			value.append('5');
			drawStr.setText(value.toString());
			setPosition(x, y);
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_6) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_6)) {
			value.append('6');
			drawStr.setText(value.toString());
			setPosition(x, y);
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_7) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_7)) {
			value.append('7');
			drawStr.setText(value.toString());
			setPosition(x, y);
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_8) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_8)) {
			value.append('8');
			drawStr.setText(value.toString());
			setPosition(x, y);
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_9) || Gdx.input.isKeyJustPressed(Input.Keys.NUMPAD_9)) {
			value.append('9');
			drawStr.setText(value.toString());
			setPosition(x, y);
		}

	}

	@Override
	public String getValue() {
		return drawStr.getText();
	}
}
