package enigma.engine;

import java.util.ArrayList;

import javax.swing.plaf.basic.BasicInternalFrameTitlePane.MaximizeAction;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class GameMenu {
	private Sprite menuBackground;
	private ArrayList<Button> buttons = new ArrayList<Button>();

	private float buttonMaxHeight = 50.0f;
	private float buttonSpacing = 10.0f;
	private float tableWidth = 600.0f;
	

	public GameMenu() {
		menuBackground = new Sprite(TextureStorage.buttonPressed);
		createNetworkingMenu();
		menuBackground.setScale(tableWidth / menuBackground.getWidth(),
				getTableHeight() / menuBackground.getHeight());
	}

	private void createNetworkingMenu() {
		String btn1str = "Server";
		String btn2str = "Client";
		String btn3str = "Disconnect/Stop";
		String btn4str = "Connect/Start";

		buttons.add(new Button());
		buttons.add(new Button());
		buttons.add(new Button());
		buttons.add(new Button());
		buttons.add(new Button());
		buttons.add(new Button());
		buttons.add(new Button());
		buttons.add(new Button());

	}
	
	public float getTableWidth(){
		return tableWidth;
	}
	
	public float getTableHeight(){
		return (buttonMaxHeight + buttonSpacing) * buttons.size() + buttonSpacing;
	}

	public void draw(SpriteBatch batch) {
		if (menuBackground != null) {
			menuBackground.draw(batch);
		}
	}

	public void translate(float x, int y) {
		menuBackground.translate(x, y);
		for(Button button : buttons){
			button.translate(x, y);
		}
	}

}
