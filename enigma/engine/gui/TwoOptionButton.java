package enigma.engine.gui;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;

/**
 * This class is bad code. I'm trying to write a network class, not a gui class.
 * 
 * @author Matt Stone
 */
public class TwoOptionButton implements UIObject {
	private OnOffButton leftBtn;
	private OnOffButton rightBtn;
	private float separationFactor = 0.05f;
	private boolean timeout;
	private long lastTimeoutValue = System.currentTimeMillis();
	private long delay = 100;

	public TwoOptionButton(String text1, String text2, float width, float height, float x, float y) {
		leftBtn = new OnOffButton(text1, (width - separationFactor / 2) / 2, height, x, y);
		rightBtn = new OnOffButton(text2, (width - separationFactor / 2) / 2, height, x + width + width * separationFactor / 2, y);

	}

	@Override
	public void translate(float x, float y) {
		leftBtn.translate(x, y);
		rightBtn.translate(x, y);

	}

	@Override
	public void setPosition(float x, float y) {
//		leftBtn.setPosition(x - leftBtn.getWidth() / 2, y);
//		rightBtn.setPosition(leftBtn.getX() + leftBtn.getWidth(), y);
		leftBtn.setPosition(x - leftBtn.getWidth() / 4, y);
		rightBtn.setPosition(leftBtn.getX() + leftBtn.getWidth(), y);
	}

	@Override
	public void draw(SpriteBatch batch) {
		leftBtn.draw(batch);
		rightBtn.draw(batch);
	}

	@Override
	public float getX() {
		return leftBtn.getX();
		// return 0;
	}

	@Override
	public float getY() {
		return leftBtn.getY();
		// return 0;
	}

	@Override
	public void logic() {
		if(timeout){
			if(System.currentTimeMillis() > lastTimeoutValue + delay){
				leftBtn.makeInactive();
				rightBtn.makeInactive();
			}
		}
	}

	@Override
	public boolean isTouched(Vector3 touchCoords) {
		if (leftBtn.isTouched(touchCoords)) {
			leftBtn.makeActive();
			rightBtn.makeInactive();
			lastTimeoutValue = System.currentTimeMillis();
		} else if (rightBtn.isTouched(touchCoords)) {
			rightBtn.makeActive();
			leftBtn.makeInactive();
			lastTimeoutValue = System.currentTimeMillis();

		} else {

		}
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

	public void activateLeft() {
		leftBtn.makeActive();
	}
	
	public void setActiveTimeoutModel(boolean timeoutYN){
		this.timeout = timeoutYN;
	}
}
