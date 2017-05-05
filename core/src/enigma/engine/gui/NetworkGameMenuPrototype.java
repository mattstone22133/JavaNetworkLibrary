package enigma.engine.gui;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;

import enigma.engine.TextureStorage;

public class NetworkGameMenuPrototype {
	private Sprite menuBackground;
	private ArrayList<UIObject> buttons = new ArrayList<UIObject>();

	private float buttonMaxHeight = 50.0f;
	private float buttonSpacing = 10.0f;
	private float tableWidth = 600.0f;
	private TextField portField;
	private TextField ipField;
	String portLabel = "PORT: ";
	String ipLabel = "IP: ";

	private UIObject activeComponent = null;
	private TwoOptionButton connectDisconnectBtn;
	private TwoOptionButton clientServerBtn;
	private boolean connectWasClicked;
	private boolean disconnectWasClicked;

	public NetworkGameMenuPrototype() {
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

		ipField = (new TextField(ipLabel, "", getTableWidth() * 0.9f, buttonMaxHeight, 0, 0));
		buttons.add(ipField);

		portField = (new TextField(portLabel, "", getTableWidth() * 0.9f, buttonMaxHeight, 0, 0));
		buttons.add(portField);

		clientServerBtn = (new TwoOptionButton("SERVER", "CLIENT", getTableWidth() * 0.9f, buttonMaxHeight, 0, 0));
		clientServerBtn.activateLeft();
		buttons.add(clientServerBtn);

		connectDisconnectBtn = (new TwoOptionButton("CONNECT", "DISCONNECT", getTableWidth() * 0.9f, buttonMaxHeight, 0, 0));
		connectDisconnectBtn.setActiveTimeoutModel(true);
		buttons.add(connectDisconnectBtn);

		// buttons.add(new OnOffButton("Host", getTableWidth() * 0.9f, buttonMaxHeight, 0, 0));
		// buttons.add(new OnOffButton("Connect", getTableWidth() * 0.9f, buttonMaxHeight, 0, 0));

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
				updateActiveComponentTo(obj);
				break;
			}
		}
	}

	/**
	 * This method should only be called if activeComponent is updated.
	 */
	private void connectOrDiscconectTouched() {
		if (activeComponent == connectDisconnectBtn) {
			TwoOptionButton ConcDisconcBTN = (TwoOptionButton) activeComponent;
			if (ConcDisconcBTN.leftIsActive()) {
				// connect was clicked
				connectWasClicked = true;
			} else {
				// disconnect was clicked
				disconnectWasClicked = true;
			}
		}
	}

	public boolean connectWasJustClicked() {
		boolean ret = connectWasClicked;
		connectWasClicked = false;
		return ret;
	}

	public boolean disconnectWasJustClicked() {
		boolean ret = disconnectWasClicked;
		disconnectWasClicked = false;
		return ret;
	}
	
	public boolean inServerMode(){
		return clientServerBtn.leftIsActive();
	}

	private void updateActiveComponentTo(UIObject obj) {
		if (activeComponent != null) {
			activeComponent.makeInactive();
		}
		obj.makeActive();
		activeComponent = obj;
		// check if connect or disconnect button was pressed
		connectOrDiscconectTouched();
	}

	public void setPort(int port) {
		portField.setValue("" + port);
	}

	/**
	 * Get the number from the port field, or throw an exception if it is invalid.
	 * 
	 * @return the number from the port field if it is parse-able
	 * @throws NumberFormatException
	 */
	public int getPort() throws NumberFormatException {
		return Integer.parseInt(portField.getValue());
	}

	public void setIP(String address) {
		ipField.setValue(address);
	}

	public String getIP() {
		return ipField.getValue();
	}

}
