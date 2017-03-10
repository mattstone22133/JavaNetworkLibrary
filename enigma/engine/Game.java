package enigma.engine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;

import enigma.engine.gui.GameMenu;

public class Game extends ApplicationAdapter implements InputProcessor {
	/** Main camera of the game */
	private OrthographicCamera camera;

	private SpriteBatch batch;
	private Actor controlTarget;
	private DrawableString title;

	private GameMenu networkMenu;
	boolean showNetworkMenu = false;

	// touch events
	Vector3 convertedCoords;

	@Override
	public void create() {
		TextureStorage.initTextures();
		Gdx.input.setInputProcessor(this);
		batch = new SpriteBatch();
		title = new DrawableString("Networking Class Demo\n           Press Enter!");
		title.translateY(Gdx.graphics.getHeight() * 0.35f);
		title.startAnimation();
		controlTarget = new Actor();

		networkMenu = new GameMenu();
		networkMenu.setPosition(0 - networkMenu.getTableWidth() / 2, 0 - networkMenu.getTableHeight() / 2);

		convertedCoords = new Vector3(0.0f, 0.0f, 0.0f);

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

		if (showNetworkMenu && networkMenu != null) {
			networkMenu.logic();
		}

		// give classes process time for logic
		if (controlTarget != null) {
			controlTarget.controlledByPlayer(camera);
			controlTarget.handleLogic();
		}
		
		networkLogic();

	}

	private void networkLogic() {
		
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

	public static void mouseCordsToGameCords(OrthographicCamera camera, Vector3 storage) {
		float rawX = Gdx.input.getX();
		float rawY = Gdx.input.getY();
		camera.unproject(storage.set(rawX, rawY, 0));
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		mouseCordsToGameCords(camera, convertedCoords);
		networkMenu.isTouched(convertedCoords);
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return false;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		return false;
	}

	@Override
	public boolean scrolled(int amount) {
		return false;
	}

	@Override
	public boolean keyDown(int keycode) {
		return false;
	}

	@Override
	public boolean keyUp(int keycode) {
		return false;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

}
