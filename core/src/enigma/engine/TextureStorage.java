package enigma.engine;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;

/**
 * This class is a container class for all textures stored in ram. The initTextures() method call
 * should be called before any textures are used. Also, the initTextures() method call should be
 * done in the create() method call for libgdx.
 * 
 * This class exists to simplify memory management (ie dispose calls) and provide efficiency in
 * recycling textures.
 * 
 * @author Matt Stone
 * @version 1.0
 *
 */
public class TextureStorage {

	private static ArrayList<Texture> allTextures = new ArrayList<Texture>();
	public static Texture genericSpriteTexture;
	public static Texture buttonPressed;
	public static Texture button;
	public static Texture selected_small;
	public static Texture unselected_small;
	public static Texture selected;
	public static Texture unselected;

	public static Texture menuBG;
	public static Texture test;
	public static BitmapFont bmFont;

	public static void initTextures() {
		// check if textures have already been initialized
		if (allTextures.size() > 0) {
			return;
		}

		genericSpriteTexture = new Texture(Gdx.files.internal("GenericActorSprite.png"));
		allTextures.add(genericSpriteTexture);
		
		button = new Texture(Gdx.files.internal("ButtonBlack.png"));
		allTextures.add(genericSpriteTexture);
		
		buttonPressed = new Texture(Gdx.files.internal("ButtonGrey.png"));
		allTextures.add(genericSpriteTexture);
		
		menuBG = new Texture(Gdx.files.internal("DarkMenu.png"));
		allTextures.add(menuBG);

		selected_small = new Texture(Gdx.files.internal("select_color.png"));
		allTextures.add(selected_small);
		
		unselected_small = new Texture(Gdx.files.internal("unselect_color.png"));
		allTextures.add(unselected_small);
		
		selected = new Texture(Gdx.files.internal("select400x400.png"));
		allTextures.add(selected);
		
		unselected = new Texture(Gdx.files.internal("unselect400x400.png"));
		allTextures.add(unselected);
		
		test = new Texture(Gdx.files.internal("select2.png"));
		allTextures.add(test);
		
		bmFont = new BitmapFont(Gdx.files.internal("prada.fnt"));
		
	}

	public static void dispose() {
		for (Texture tex : allTextures) {
			tex.dispose();
		}
		
		if(bmFont != null){
			bmFont.dispose();
		}
	}
}
