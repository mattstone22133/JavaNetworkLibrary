package enigma.engine;

import java.util.HashMap;
import java.util.PriorityQueue;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Cell {
	private HashMap<Integer, Actor> allRegisteredActors;
	private PriorityQueue<Actor> drawQueue1;
	private PriorityQueue<Actor> drawQueue2;
	private PriorityQueue<Actor> activeQueue;
	
	public void logic(){
		
	}
	
	public void render(SpriteBatch batch){
		
	}
}
