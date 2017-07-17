package enigma.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector3;

import enigma.engine.data.compression.ActorData;
import enigma.engine.datastructures.ManagedVectorCopyQueue;

public class Actor {
	private static int actorNumber = 0;
	private int id = 0;
	private Sprite sprite;
	private float moveSpeed;
	private float angleSpeed;
	private float rotation;
	private Vector3 mousConvtVect = new Vector3();
	private Character networkID = null;

	/**
	 * Basic constructor that creates an actor with an underlying sprite.
	 */
	public Actor() {
		sprite = new Sprite(TextureStorage.genericSpriteTexture);
		setMoveSpeed(10.0f);
		this.id = Actor.actorNumber;
	}

	/**
	 * Standard draw call.
	 * 
	 * @param batch the batch that handles drawing in LWGJL - a libgdx class.
	 */
	public void draw(SpriteBatch batch) {
		sprite.draw(batch);
	}

	/**
	 * Method call where logic can be completed. No logic greater with complexity greater than O(n)
	 */
	public void handleLogic() {
		moveToPoint();
	}

	/**
	 * This method encompasses all behaviors that a player may do on an actor. It should only be
	 * called from a reference that is designed to listen to player events.
	 */
	public void controlledByPlayer(OrthographicCamera camera) {
		pollMovement();
		pollMouseUpdate(camera);
	}

	/**
	 * Polls the mouse for mouse related interaction.
	 * 
	 * Updates actor sprite's rotation so that it matches the angle between the mouse pointer and
	 * the sprite's position.
	 * 
	 * @param camera the game camera.
	 */
	private void pollMouseUpdate(OrthographicCamera camera) {
		// Convert screen coordinates to game coordinates by polling
		float screenX = Gdx.input.getX();
		float screenY = Gdx.input.getY();
		camera.unproject(mousConvtVect.set(screenX, screenY, 0));

		// calculate the angle between the mouse and player (in degrees)
		float relX = mousConvtVect.x - sprite.getX();
		float relY = mousConvtVect.y - sprite.getY();
		rotation = (float) (Math.atan(relY / relX) * 180 / Math.PI);
		if (relX < 0) rotation += 180.0f;

		// update sprite
		sprite.setRotation(rotation);

	}

	/**
	 * Scans the keyboard for movement presses.
	 */
	private void pollMovement() {
		if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
			if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
				// move up left
				sprite.translate(-angleSpeed, angleSpeed);
			} else if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
				// move up right
				sprite.translate(angleSpeed, angleSpeed);
			} else {
				// move up
				sprite.translate(0, getMoveSpeed());
			}
		} else if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
			if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
				// move down left
				sprite.translate(-angleSpeed, -angleSpeed);
			} else if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
				// move down right
				sprite.translate(angleSpeed, -angleSpeed);
			} else {
				// move down
				sprite.translate(0, -angleSpeed);
			}
		} else if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
			// move left
			sprite.translate(-getMoveSpeed(), 0);
		} else if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
			// move right
			sprite.translate(getMoveSpeed(), 0);
		} else {
			// no movement buttons pressed
		}
	}

	/**
	 * This shouldn't be called on every move for efficiency. However, it should be called anytime
	 * the moveSpeed is changed.
	 * 
	 * @return the calculated angle speed
	 */
	private float calcMovSpeed45Degree() {
		// by Pythagorean's theorem where 'a' and 'b' are the same and c is moveSpeed
		return (float) Math.sqrt((getMoveSpeed() * getMoveSpeed()) / 2.0f);
	}

	/**
	 * Get the movement speed of the actor.
	 * 
	 * @return the current linear move speed of the actor.
	 */
	public float getMoveSpeed() {
		return moveSpeed;
	}

	/**
	 * Standard setter for move speed. This updates the angleMove speed field.
	 * 
	 * @param moveSpeed the speed to set.
	 */
	public void setMoveSpeed(float moveSpeed) {
		this.moveSpeed = moveSpeed;
		this.angleSpeed = calcMovSpeed45Degree();
	}

	public ActorData getCompresedData() {
		return new ActorData(id, rotation, sprite.getX(), sprite.getY(), networkID);
	}

	public void updateToData(ActorData actor) {
		this.sprite.setX(actor.x);
		this.sprite.setY(actor.y);
		this.sprite.setRotation(actor.rotation);
	}

	public void setId(char id) {
		this.networkID = new Character(id);
	}

	public Character getID() {
		return this.networkID;
	}

	// -------------- Pointer interpolation (ie walking to point) ------------------
	private boolean interpolating = false;
	private Vector3 nextMovePoint = new Vector3();
	private Vector3 cachedPointFromRawData = new Vector3();
	private ManagedVectorCopyQueue interpolatePointQueuer = new ManagedVectorCopyQueue(ConstGlobals.NUM_QUEUE_INTERPOLATE_POINTS, true);

	/**
	 * Set the point which to slowly move the actor towards.
	 * 
	 * @param point The point to interpolate towards.
	 */
	public void setInterpPnt(Vector3 point) {
		if (!interpolating) {
			nextMovePoint.set(point);
			interpolating = true;
		} else {
			// queue point for when this point has been walked to
			interpolatePointQueuer.queueACopy(point);
		}
	}

	public void setInterPnt(float x, float y, float z) {
		// this is slightly inefficient to pass raw data into a cached point, then pass cached point
		// to set another point (interpolate pnt or queue). An alternative is to refactor
		// interpolatePointQueue to accept raw data and directly put data into the recycled points.
		// That's great but then there will be duplicate logic between setInterPnt(float, float,
		// float) and setInterPnt(vector3) Doing it this way maintains single responsibility
		// principle on a functional/methodical level.
		cachedPointFromRawData.set(x, y, z);
		setInterPnt(x, y, z);
	}

	/**
	 * Moves the actor towards the set interpolating point
	 */
	public void moveToPoint() {
		if (interpolating && nextMovePoint != null && sprite != null) {
			// calculate distance (hypo) to point (by pythagorean)
			float dX = nextMovePoint.x - sprite.getX();
			float dY = nextMovePoint.y - sprite.getY();

			// pythagorean theorem
			float hypo = (float) Math.sqrt(dX * dX + dY * dY);

			// zero check hypo, if hypo is zero then the point is reached
			if (!EEUtils.floatIsZero(hypo, EEUtils.F_SMALL_NUMBER)) {
				float speedToDistRatio = getMoveSpeed() / hypo;
				if (speedToDistRatio < 1) {
					// sprite cannot move the entire distance between the point, it can only as far
					// as its speed allows
					// multiplying the ratio of the move speed to total distance against the x and y
					// components, for example:
					// distance in X is -- the ratio of speed/totalDistance -- multiplied by -- the
					// total displacement in x
					sprite.translate(dX * speedToDistRatio, dY * speedToDistRatio);
				} else {
					// move speed is greater than the hypo, therefore the point is really close!
					// instead of moving the move speed, only move the dx and dy of hypo
					// this means that the next time walk to point is called, the hypo should round
					// to zero and
					// will terminate the walk algorithm.
					sprite.translate(dX, dY);
				}
			} else {
				// hypo is zero, therefore point is reached
				handleInterpolatingPointReached();
			}

		}
	}

	private void handleInterpolatingPointReached() {
		// check if there are more points to interpolate to.
		if (!interpolatePointQueuer.isEmpty()) {
			if (interpolatePointQueuer.poll(nextMovePoint) != null) {
				return;
			}
		}
		interpolating = false;
	}

	// -----------------------------------------------------------------------------
}
