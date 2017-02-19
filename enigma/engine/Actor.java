package enigma.engine;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Actor {
	private Sprite sprite;
	
	public Actor(){
		sprite = new Sprite(TextureStorage.genericSpriteTexture);
	}
	
	public void draw(SpriteBatch batch){
		sprite.draw(batch);
	}
	
	public void handleLogic(){
		
	}
	
	public void handleIO(){
		
	}
}
