package enigma.engine.gui;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;

import enigma.engine.TextureStorage;

public class GameMenu {
	private Sprite menuBackground;
	private ArrayList<UIObject> buttons = new ArrayList<UIObject>();

	private float buttonMaxHeight = 50.0f;
	private float buttonSpacing = 10.0f;
	private float tableWidth = 600.0f;

	private UIObject activeComponent = null;

	public GameMenu() {
		createNetworkingMenu();

		menuBackground = new Sprite(TextureStorage.menuBG);
		//@formatter:off
		menuBackground.setScale(
				tableWidth / menuBackground.getWidth(),
				getTableHeight() / menuBackground.getHeight());
		//@formatter:on
		menuBackground.setOrigin(menuBackground.getScaleX(), menuBackground.getScaleY());

		// space buttons accordingly
		adjustComponents();
	}

	public void setPosition(float x, float y) {
		menuBackground.setPosition(x, y);
		adjustComponents();
	}

	private void createNetworkingMenu() {

		buttons.add(new TextField("IP: ", "", getTableWidth() * 0.9f, buttonMaxHeight, 0, 0));
		buttons.add(new TextField("PORT: ", "", getTableWidth() * 0.9f, buttonMaxHeight, 0, 0));
		TwoOptionButton clientServerBtn  = (new TwoOptionButton("SERVER", "CLIENT", getTableWidth() * 0.9f, buttonMaxHeight, 0, 0));
		clientServerBtn.activateLeft();
		buttons.add(clientServerBtn);
		
		TwoOptionButton connectDisconnectBtn = (new TwoOptionButton("CONNECT", "DISCONNECT", getTableWidth() * 0.9f, buttonMaxHeight, 0, 0));
		connectDisconnectBtn.setActiveTimeoutModel(true);
		buttons.add(connectDisconnectBtn);
		
		//buttons.add(new OnOffButton("Host", getTableWidth() * 0.9f, buttonMaxHeight, 0, 0));
		//buttons.add(new OnOffButton("Connect", getTableWidth() * 0.9f, buttonMaxHeight, 0, 0));

	}

	public void adjustComponents() {
		float topOfTable = menuBackground.getY() + (menuBackground.getHeight() * menuBackground.getScaleY());
		float yPosition = topOfTable - (buttonSpacing + buttonMaxHeight / 2);
		for (UIObject obj : buttons) {
			obj.setPosition(obj.getX(), yPosition);
			yPosition -= buttonSpacing + buttonMaxHeight;
		}
	}

	public float getTableWidth() {
		return tableWidth;
	}

	public float getTableHeight() {
		return (buttonMaxHeight + buttonSpacing) * buttons.size() + buttonSpacing;
	}

	public void draw(SpriteBatch batch) {
		if (menuBackground != null) {
			menuBackground.draw(batch);
		}
		for (UIObject obj : buttons) {
			obj.draw(batch);
		}
	}

	public void translate(float x, float y) {
		menuBackground.translate(x, y);
		for (UIObject obj : buttons) {
			obj.translate(x, y);
		}
	}

	public void logic() {
		for (UIObject obj : buttons) {
			obj.logic();
		}

		if (activeComponent != null) {
			activeComponent.activeSelctionLogic();
		}
	}

	public void isTouched(Vector3 touchCoords) {
		for (UIObject obj : buttons) {
			if (obj.isTouched(touchCoords)) {
				if (activeComponent != null) {
					activeComponent.makeInactive();
				}
				obj.makeActive();
				activeComponent = obj;
				break;
			}
		}
	}

}
