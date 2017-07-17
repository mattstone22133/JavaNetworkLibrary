package enigma.engine;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;

import enigma.engine.data.compression.ActorData;
import enigma.engine.data.compression.DataCompressor;
import enigma.engine.gui.NetworkGameMenuPrototype;
import enigma.engine.network.FailedToConnect;
import enigma.engine.network.Network;
import enigma.engine.network.NetworkPlayer;

public class Game extends ApplicationAdapter implements InputProcessor {
	/** Main camera of the game */
	private OrthographicCamera camera;

	/** Render Items */
	private SpriteBatch batch;
	private Actor controlTarget;
	private Actor otherActor = null;
	private DrawableString title;

	/** Logical Items*/
	private NetworkGameMenuPrototype networkMenu;
	private boolean drawNetworkMenu = false;
	private Network network = new Network();
	private NetworkPlayer idObject = null;

	/** Flags */
	boolean bClickToWalk = true;
	
	// touch events
	private Vector3 convertedCoords;

	@Override
	public void create() {
		TextureStorage.initTextures();
		Gdx.input.setInputProcessor(this);
		batch = new SpriteBatch();
		title = new DrawableString("Networking Class Demo\n           Press Enter!");
		title.translateY(Gdx.graphics.getHeight() * 0.35f);
		title.startAnimation();
		controlTarget = new Actor();

		network.verbose = true;
		networkMenu = new NetworkGameMenuPrototype();
		networkMenu.setPosition(0 - networkMenu.getTableWidth() / 2, 0 - networkMenu.getTableHeight() / 2);
		setNetworkMenuToLocalHost();

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
		if (otherActor != null) {
			otherActor.draw(batch);
		}

		if (title != null) {
			title.draw(batch);
		}

		if (drawNetworkMenu && networkMenu != null) {
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

		if (drawNetworkMenu && networkMenu != null) {
			networkMenu.logic();
		}

		// give classes process time for logic
		if (controlTarget != null) {
			controlTarget.controlledByPlayer(camera);
			controlTarget.handleLogic();
		}

		if (otherActor != null) {
			otherActor.handleLogic();
		}

		networkLogic();

	}

	private void networkLogic() {
		pollNetworkMenu();
		receiveData();
		sendData();
		pollIdRequest();
	}

	private void pollIdRequest() {
		// a better implementation should be used for polling, this is just a proof of concept.
		if (idObject == null && network.isRunning()) {
			idObject = network.getPlayerID();
			if (idObject != null) {
				if (network.inServerMode()) {
					controlTarget.setId(idObject.getID());
					otherActor = new Actor();
				} else {
					// client mode
					otherActor = controlTarget;
					controlTarget = new Actor();
					controlTarget.setId(idObject.getID());
				}
			}
		}
	}

	private void receiveData() {
		boolean shouldStop = false;

		// open at maximum 20 packets this iteration
		for (int i = 0; i < 20 && !shouldStop; ++i) {
			GameDataPacket packet = (GameDataPacket) network.getNextReceivedPacket();
			if (packet == null) {
				shouldStop = true;
			} else {
				for (DataCompressor actor : packet.actorsAdded) {
					// ugly decompression - this is just for a proof of concept.
					ActorData actorData = (ActorData) actor;
					if (idObject != null) {
						if (actorData.networkId == idObject.getID()) {
							controlTarget.updateToData(actorData);
						} else if (otherActor != null) {
							otherActor.updateToData(actorData);
						}
					} else {
						controlTarget.updateToData(actorData);
					}
				}
			}
		}

	}

	private void sendData() {
		if (network.sendDelayTimerExpired() && network.isRunning()) {
			GameDataPacket packetToSend = makePacket();
			network.queueToSend(packetToSend);
		}
	}

	private GameDataPacket makePacket() {
		GameDataPacket packet = new GameDataPacket();
		// this is where all game information that needs to be sent should be loaded into a packet.
		packet.addActor(controlTarget);
		return packet;
	}

	private void pollNetworkMenu() {
		if (drawNetworkMenu) {
			if (networkMenu.connectWasJustClicked()) {
				networkConnect();
			} else if (networkMenu.disconnectWasJustClicked()) {
				networkDisconnect();
			}
		}
	}

	private void networkDisconnect() {
		boolean serverMode = networkMenu.inServerMode();
		String toPrint = "Disconnect was pressed for";
		if (serverMode) {
			toPrint += " server";
		} else {
			toPrint += " client";
		}
		network.disconnect();
		System.out.println(toPrint);
	}

	private void networkConnect() {
		// Do not attempt a connection if network is already running
		if (network.isRunning()) {
			System.out.println("connect error - network already running");
			return;
		}

		boolean serverMode = networkMenu.inServerMode();
		String toPrint = "Connect was pressed for";
		if (serverMode) {
			toPrint += " server";
			network.serverMode();

		} else {
			toPrint += " client";
			network.clientMode();
		}
		try {
			String ip = networkMenu.getIP();
			System.out.println("Connecting to: " + ip);
			network.setAddress(ip);
			network.run();
		} catch (IOException | FailedToConnect e) {
			e.printStackTrace();
		}
		System.out.println(toPrint);
	}

	private void gameIO() {
		if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
			Gdx.app.exit();
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
			drawNetworkMenu = !drawNetworkMenu;
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

	private void setNetworkMenuToLocalHost() {
		networkMenu.setPort(25565);
		try {

			networkMenu.setIP(InetAddress.getLocalHost().getHostAddress());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		mouseCordsToGameCords(camera, convertedCoords);
		networkMenu.isTouched(convertedCoords);
		if(bClickToWalk){
			walkControlToPoint(convertedCoords);
		}
		
		return false;
	}

	private void walkControlToPoint(Vector3 point) {
		if(controlTarget != null){
			controlTarget.setInterpPnt(point);
		}
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
