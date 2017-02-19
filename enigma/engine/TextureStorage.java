package enigma.engine;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;

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
	public static Texture kButtonPressed;
	public static Texture lambdaTexture;

	public static void initTextures() {
		// check if textures have already been initialized
		if (allTextures.size() > 0) {
			return;
		}

		genericSpriteTexture = new Texture(Gdx.files.internal("GenericActorSprite.png"));
		allTextures.add(genericSpriteTexture);
	}

	public static void dispose() {
		for (Texture tex : allTextures) {
			tex.dispose();
		}
	}
}
