package enigma.engine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Game extends ApplicationAdapter {
	private SpriteBatch batch;
	private Actor controlTarget;
	
	@Override
	public void create () {
		TextureStorage.initTextures();
		batch = new SpriteBatch();
		controlTarget = new Actor();
	}

	@Override
	public void render () {
		
		//clear screen
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		//draw
		batch.begin();
		controlTarget.draw(batch);
		batch.end();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
		TextureStorage.dispose();
	}
}
