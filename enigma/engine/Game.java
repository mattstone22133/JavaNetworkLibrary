package enigma.engine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Game extends ApplicationAdapter {
	/** Main camera of the game */
	private OrthographicCamera camera;

	private SpriteBatch batch;
	private Actor controlTarget;
	private DrawableString title;

	GameMenu networkMenu;
	boolean showNetworkMenu = true;

	@Override
	public void create() {
		TextureStorage.initTextures();
		batch = new SpriteBatch();
		title = new DrawableString("Networking Class Demo");
		title.translateY(Gdx.graphics.getHeight() * 0.35f);
		title.startAnimation();
		controlTarget = new Actor();

		networkMenu = new GameMenu();
		networkMenu.translate(-networkMenu.getTableWidth() / 2, 0);

		createCamera();
	}

	@Override
	public void render() {
		gameLogic();

		// clear screen
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// draw
		batch.begin();

		camera.update();
		batch.setProjectionMatrix(camera.combined);

		if (controlTarget != null) {
			controlTarget.draw(batch);
		}

		if (title != null) {
			title.draw(batch);
		}

		if (showNetworkMenu && networkMenu != null) {
			networkMenu.draw(batch);
		}

		batch.end();
	}

	@Override
	public void dispose() {
		batch.dispose();
		TextureStorage.dispose();
	}

	private void gameLogic() {
		gameIO();

		// give classes process time for logic
		if (controlTarget != null) {
			controlTarget.controlledByPlayer(camera);
			controlTarget.handleLogic();
		}

	}

	private void gameIO() {
		if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
			Gdx.app.exit();
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
			showNetworkMenu = !showNetworkMenu;
		}
		if (title != null) {
			title.animateLogic();
		}
	}

	private void createCamera() {
		camera = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		// camera.position.x = Gdx.graphics.getWidth() / 2;
		// camera.position.y = Gdx.graphics.getHeight() / 2;
	}
}
