package javabot;

import java.awt.Point;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.annotation.RetentionPolicy;
import java.util.Random;
import java.util.Vector;

import javabot.model.*;
import javabot.types.*;
import javabot.types.OrderType.OrderTypeTypes;
import javabot.types.UnitType.UnitTypes;
import javabot.util.BWColor;

public class JavaBot implements BWAPIEventListener {
	
	// Some miscelaneous variables. Feel free to add yours.
	int homePositionX;
	int homePositionY;
	int survivedFrames;
	double temperature = 60000;
	int currentSwap = 0; // which elements we are swapping
	
	Vector<UnitType.UnitTypes> killOrder  = new Vector<UnitType.UnitTypes>();
	Vector<UnitType.UnitTypes> prevOrder = null;
	int prevScore = -10;

	private JNIBWAPI bwapi;
	public static void main(String[] args) {
		new JavaBot();
	}
	public JavaBot() {
		bwapi = new JNIBWAPI(this);
		bwapi.start();
		
	} 
	public void connected() {
		bwapi.loadTypeData();
	}
	
	// Method called at the beginning of the game.
	public void gameStarted() {		
		System.out.println("Game Started12");
		bwapi.enableUserInput();
		bwapi.setGameSpeed(0);
		
		
		if (killOrder.size() < 1)
		{
			killOrder.add(UnitTypes.Zerg_Ultralisk);
			killOrder.add(UnitTypes.Zerg_Drone);
			killOrder.add(UnitTypes.Zerg_Broodling);
			killOrder.add(UnitTypes.Zerg_Zergling);
		}
		bwapi.printText("Hello world!");
	    temperature = temperature * 0.95;
		
		/* EDIT HERE */
	}
	
	
	
	public boolean heuristics(double temp, int prev, int cur){
		double exp = -((double) Math.pow(prev - cur, 2)) * (1/(double) temp);
		System.out.println("pravdepodobnost je" + Math.pow(Math.E, exp));
		return Math.pow(Math.E, exp) > (new Random()).nextDouble();
	}
	
	public void gameEnded() {
		/* ALSO EDIT HERE */
		int curScore = bwapi.getFrameCount(); // get current score
		
		Random rand = new Random();
		
		System.out.println(curScore);
		
		// if found better solution or heuristics say yes, then move to current kill order
		if ((curScore > prevScore) || heuristics(temperature, prevScore, curScore)){
			prevOrder = killOrder;
			prevScore = curScore;
			System.out.println("accepted");
			System.out.println(prevOrder);
		}
		
		// try new neighbour
		killOrder = swapNeighbours(prevOrder, rand.nextInt(killOrder.size() - 1));
	}
	
	public Vector<UnitType.UnitTypes> swapNeighbours(Vector<UnitType.UnitTypes> x, int index){
		// copy
		Vector<UnitType.UnitTypes> ret = new Vector<UnitType.UnitTypes>();
		for(int i = 0;i < x.size(); i++){
			ret.add(x.elementAt(i));
		}
		
		if (index >= x.size()) return null;
		
		// now swap
		UnitType.UnitTypes pom = ret.get(index);
		ret.set(index, ret.elementAt(index + 1));
		ret.set(index + 1, pom);
		
		//return
		return ret;
	}
	
	
	// Method called once every second.
	public void act() {
		survivedFrames++;
		for (Unit unit : bwapi.getMyUnits()) {
			if (unit.getTypeID() == UnitTypes.Protoss_Photon_Cannon.ordinal()) {
				if (unit.isIdle()) {		
					int closestId = -1;
					double closestDist = 99999999;
					int unitToKill = 0;
					while ((closestId == -1)&&(unitToKill < killOrder.size() )) {
						for (Unit neu : bwapi.getNeutralUnits()) {
							if (neu.getTypeID() == killOrder.elementAt(unitToKill).ordinal()) {
								double distance = Math.sqrt(Math.pow(neu.getX() - unit.getX(), 2) + Math.pow(neu.getY() - unit.getY(), 2));
								if ((closestId == -1) || (distance < closestDist)) {
									closestDist = distance;
									closestId = neu.getID();
								}
							}
						}
						unitToKill ++;
					}
					// and (if we found it) send this worker to gather it.
					if (closestId != -1) bwapi.attack(unit.getID(), closestId);
				}
			}
		}
		
		
		// ==========================================================
	}
	
	
	// Method called on every frame (approximately 30x every second).
	public void gameUpdate() {
		
		// Remember our homeTilePosition at the first frame
		if (bwapi.getFrameCount() == 1) {
			int cc = getNearestUnit(UnitTypes.Terran_Command_Center.ordinal(), 0, 0);
			if (cc == -1) cc = getNearestUnit(UnitTypes.Zerg_Hatchery.ordinal(), 0, 0);
			if (cc == -1) cc = getNearestUnit(UnitTypes.Protoss_Nexus.ordinal(), 0, 0);
			homePositionX = bwapi.getUnit(cc).getX();
			homePositionY = bwapi.getUnit(cc).getY();

		}
		
		// Draw debug information on screen
		drawDebugInfo();

		// Call the act() method every 30 frames
		if (bwapi.getFrameCount() % 30 == 0) {
			act();
		}
		
		
	}

	// Some additional event-related methods.
	
	public void matchEnded(boolean winner) {}
	public void nukeDetect(int x, int y) {}
	public void nukeDetect() {}
	public void playerLeft(int id) {}
	public void unitCreate(int unitID) {}
	public void unitDestroy(int unitID) {}
	public void unitDiscover(int unitID) {}
	public void unitEvade(int unitID) {}
	public void unitHide(int unitID) {}
	public void unitMorph(int unitID) {}
	public void unitShow(int unitID) {}
	public void keyPressed(int keyCode) {}
	

    // Returns the id of a unit of a given type, that is closest to a pixel position (x,y), or -1 if we
    // don't have a unit of this type
    public int getNearestUnit(int unitTypeID, int x, int y) {
    	int nearestID = -1;
	    double nearestDist = 9999999;
	    for (Unit unit : bwapi.getMyUnits()) {
	    	if ((unit.getTypeID() != unitTypeID) || (!unit.isCompleted())) continue;
	    	double dist = Math.sqrt(Math.pow(unit.getX() - x, 2) + Math.pow(unit.getY() - y, 2));
	    	if (nearestID == -1 || dist < nearestDist) {
	    		nearestID = unit.getID();
	    		nearestDist = dist;
	    	}
	    }
	    return nearestID;
    }	
	
	// Returns the Point object representing the suitable build tile position
	// for a given building type near specified pixel position (or Point(-1,-1) if not found)
	// (builderID should be our worker)
	public Point getBuildTile(int builderID, int buildingTypeID, int x, int y) {
		Point ret = new Point(-1, -1);
		int maxDist = 3;
		int stopDist = 40;
		int tileX = x/32; int tileY = y/32;
		
		// Refinery, Assimilator, Extractor
		if (bwapi.getUnitType(buildingTypeID).isRefinery()) {
			for (Unit n : bwapi.getNeutralUnits()) {
				if ((n.getTypeID() == UnitTypes.Resource_Vespene_Geyser.ordinal()) && 
						( Math.abs(n.getTileX()-tileX) < stopDist ) &&
						( Math.abs(n.getTileY()-tileY) < stopDist )
						) return new Point(n.getTileX(),n.getTileY());
			}
		}
		
		while ((maxDist < stopDist) && (ret.x == -1)) {
			for (int i=tileX-maxDist; i<=tileX+maxDist; i++) {
				for (int j=tileY-maxDist; j<=tileY+maxDist; j++) {
					if (bwapi.canBuildHere(builderID, i, j, buildingTypeID, false)) {
						// units that are blocking the tile
						boolean unitsInWay = false;
						for (Unit u : bwapi.getAllUnits()) {
							if (u.getID() == builderID) continue;
							if ((Math.abs(u.getTileX()-i) < 4) && (Math.abs(u.getTileY()-j) < 4)) unitsInWay = true;
						}
						if (!unitsInWay) {
							ret.x = i; ret.y = j;
							return ret;
						}
						// creep for Zerg (this may not be needed - not tested yet)
						if (bwapi.getUnitType(buildingTypeID).isRequiresCreep()) {
							boolean creepMissing = false;
							for (int k=i; k<=i+bwapi.getUnitType(buildingTypeID).getTileWidth(); k++) {
								for (int l=j; l<=j+bwapi.getUnitType(buildingTypeID).getTileHeight(); l++) {
									if (!bwapi.hasCreep(k, l)) creepMissing = true;
									break;
								}
							}
							if (creepMissing) continue; 
						}
						// psi power for Protoss (this seems to work out of the box)
						if (bwapi.getUnitType(buildingTypeID).isRequiresPsi()) {}
					}
				}
			}
			maxDist += 2;
		}
		
		if (ret.x == -1) bwapi.printText("Unable to find suitable build position for "+bwapi.getUnitType(buildingTypeID).getName());
		return ret;
	}
	
	// Returns true if we are currently constructing the building of a given type.
	public boolean weAreBuilding(int buildingTypeID) {
		for (Unit unit : bwapi.getMyUnits()) {
			if ((unit.getTypeID() == buildingTypeID) && (!unit.isCompleted())) return true;
			if (bwapi.getUnitType(unit.getTypeID()).isWorker() && unit.getConstructingTypeID() == buildingTypeID) return true;
		}
		return false;
	}
	
	// Draws debug information on the screen. 
	// Reimplement this function however you want. 
	public void drawDebugInfo() {

		// Draw our home position.
		bwapi.drawText(new Point(5,0), "Kill order: ", true);
		for (int i=0; i<killOrder.size(); i++)
		{
			bwapi.drawText(new Point(5,10*(i+2)), killOrder.elementAt(i).name(), true);
		}
		
		// Draw circles over workers (blue if they're gathering minerals, green if gas, yellow if they're constructing).
		for (Unit u : bwapi.getMyUnits())  {
			if (u.isGatheringMinerals()) bwapi.drawCircle(u.getX(), u.getY(), 12, BWColor.BLUE, false, false);
			else if (u.isGatheringGas()) bwapi.drawCircle(u.getX(), u.getY(), 12, BWColor.GREEN, false, false);
		}
		
	}
	
}
