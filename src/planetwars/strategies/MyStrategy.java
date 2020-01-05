/**
 * Author: Jerry Nie & Hanzhang Wu
 * x500ID: nie00008 & wu000123
 * date: Dec 2018
 */

package planetwars.strategies;

import planetwars.publicapi.*;

import javax.crypto.spec.IvParameterSpec;
import javax.print.attribute.standard.Destination;
import java.util.*;
import java.util.Random;

public class MyStrategy implements IStrategy {
    private static final int POPULATION_DIVISION = 5;
    private Random random;

    public MyStrategy() {
        random = new Random();
    }

    @Override
    public void takeTurn(List<IPlanet> planets, IPlanetOperations planetOperations, Queue<IEvent> eventsToExecute) {
        // planets could be conquered, unconquered or neutral
        // we create three lists that store planets of those three types
        List<IVisiblePlanet> conqueredVisiblePlanets = new ArrayList<>();
        List<IVisiblePlanet> unconqueredVisiblePlanets = new ArrayList<>();
        List<IVisiblePlanet> neutralVisiblePlanets = new ArrayList<>();
        for (IPlanet planet : planets) {
            if (planet instanceof IVisiblePlanet && ((IVisiblePlanet) planet).getOwner() == Owner.SELF) {
                conqueredVisiblePlanets.add((IVisiblePlanet) planet);
            } else if (planet instanceof IVisiblePlanet && ((IVisiblePlanet) planet).getOwner() == Owner.OPPONENT) {
                unconqueredVisiblePlanets.add((IVisiblePlanet) planet);
            } else if (planet instanceof IVisiblePlanet && ((IVisiblePlanet) planet).getOwner() == Owner.NEUTRAL) {
                neutralVisiblePlanets.add((IVisiblePlanet) planet);
            }
        }
        // Since eventsToExucute has three variables, we create a HashMap structure to store the three variables
        // The outer HashMap has keys of source planets, and values of another HashMap whose keys are destinations
        // The inner HashMap has values of population transferred from the source planet to the destination planet
        // We iterate through the outer and inner HashMap to get the events to be executed
        Map<IVisiblePlanet, Integer> map = new HashMap<>();
        Map<IVisiblePlanet, Map<IVisiblePlanet, Long>> neutralSource =
                conquerNeutral(conqueredVisiblePlanets, neutralVisiblePlanets);
        for (IVisiblePlanet source : neutralSource.keySet()) {
            Map<IVisiblePlanet, Long> Destination = neutralSource.get(source);
            for (IVisiblePlanet dest : Destination.keySet()) {
                eventsToExecute.offer(planetOperations.transferPeople(source, dest, Destination.get(dest)));
            }
            map.put(source, 0);
        }
        // Like what we did for conquerNeutral, a Hashmap named destMap is generated to store three variables
        // The outer HashMap has keys destination planets, and values of another HashMap whose keys are sources
        // The inner HashMap has values of population transferred from the source planet to the destination planet
        // We iterate through the outer and inner HashMap to get the events to be executed
        Map<IVisiblePlanet, Map<IVisiblePlanet, Long>> destMap =
                attackRole(conqueredVisiblePlanets, unconqueredVisiblePlanets);
        for (IVisiblePlanet dest : destMap.keySet()) {
            Map<IVisiblePlanet, Long> Source = destMap.get(dest);
            for (IVisiblePlanet source : Source.keySet()) {
                // when the source is conquering neutral planets, it does not attack
                if (!map.containsKey(source)) {
                    eventsToExecute.offer(planetOperations.transferPeople(source, dest, Source.get(source)));
                }
            }
        }
        // Like what we did above, a Hashmap named mySource is generated to store three variables
        // The outer HashMap has keys of source planets, and values of another HashMap whose keys are destinations
        // The inner HashMap has values of population transferred from the source planet to the destination planet
        // We iterate through the outer and inner HashMap to get the events to be executed
        Map<IVisiblePlanet, Map<IVisiblePlanet, Long>> mySource =
                sendPopulation(conqueredVisiblePlanets, unconqueredVisiblePlanets, planets.size());
        for (IVisiblePlanet source : mySource.keySet()) {
            Map<IVisiblePlanet, Long> destination = mySource.get(source);
            for (IVisiblePlanet dest : destination.keySet()) {
                long g = (1 + (source.getHabitability() / 100));
                // When the source is almost full, its population is transferred to the destination
                if (source.getPopulation() * g * g >= source.getSize()) {
                    eventsToExecute.offer(planetOperations.transferPeople(source, dest, destination.get(dest)));
                }
                // if the destination's population is relatively small and the source is not conquering the planet
                // the source's population is transferred to the destination
                else if (dest.getPopulation() <= dest.getSize() / 100 && !map.containsKey(source)) {
                    eventsToExecute.offer(planetOperations.transferPeople(source, dest, destination.get(dest)));
                }
            }
        }
    }

    @Override
    public String getName() {
        return "Hanzhang Wu(wu000123); Jerry Nie(nie00008)";
    }

    public boolean compete() {
        return true;
    }


    /**
     * Counting the edge number of a planet in a list of planets
     */
    public int countEdges(IVisiblePlanet planet, List<IVisiblePlanet> planets) {
        int count = 0;
        // we iterate through the edge of a planet and the list of planets
        for (IEdge edge : planet.getEdges()) {
            for (IVisiblePlanet p : planets) {
                // if they match, our counter is increased by 1
                if (edge.getDestinationPlanetId() == p.getId()) {
                    count += 1;
                }
            }
        }
        return count;
    }

    /**
     * Sorting planets surrounding a target planet by edge number and distance
     */
    public List<IVisiblePlanet> edgeSortPlanets(IVisiblePlanet myPlanet, List<IVisiblePlanet> planets) {
        // We take a target planet and a list of planets as parameters
        // We create edgeMap to store the edge numbers of planets in the list
        // We create distMap to store the distance from the target to the planets in the list
        Map<IVisiblePlanet, Integer> edgeMap = new HashMap<IVisiblePlanet, Integer>();
        Map<IVisiblePlanet, Integer> distMap = new HashMap<IVisiblePlanet, Integer>();
        for (IVisiblePlanet planet : planets) {
            for (IEdge edge : myPlanet.getEdges()) {
                // When they match, information is gathered
                if (edge.getDestinationPlanetId() == planet.getId()) {
                    edgeMap.put(planet, planet.getEdges().size());
                    distMap.put(planet, edge.getLength());
                    break;
                }
                // If they don't match, we don't give the planets valid values
                else {
                    edgeMap.put(planet, -1);
                    distMap.put(planet, 0);
                }
            }
        }
        for (int i = 0; i < planets.size(); i++) {
            for (int j = i; j < planets.size(); j++) {
                // we use bubble sort to sort the planets by their edge numbers
                if (edgeMap.get(planets.get(i)) < edgeMap.get(planets.get(j))) {
                    IVisiblePlanet temp = planets.get(i);
                    planets.set(i, planets.get(j));
                    planets.set(j, temp);
                }
                // if their edge numbers are the same, we compare their distances to the target
                else if (edgeMap.get(planets.get(i)) == edgeMap.get(planets.get(j))){
                    if (distMap.get(planets.get(i)) > distMap.get(planets.get(j))){
                        IVisiblePlanet temp = planets.get(i);
                        planets.set(i, planets.get(j));
                        planets.set(j, temp);
                    }
                }
            }
        }
        return planets;
    }

    /**
     * Finding adjacent planets of a target planet in a list of planets
     */
    public List<IVisiblePlanet> getAdjacentPlanet(IVisiblePlanet planet, List<IVisiblePlanet> planets) {
        // we create a list to store the adjacent planets
        List<IVisiblePlanet> adjacentPlanets = new ArrayList<>();
        Set<IEdge> edges = planet.getEdges();
        // we loop through the edges of the target and the list of planets to find the adjacent planets of that planet
        for (IEdge e : edges) {
            for (IVisiblePlanet p : planets) {
                // when they match, the adjacent planets are added to the list
                if (p.getId() == e.getDestinationPlanetId() && (p.getId() != planet.getId())) {
                    adjacentPlanets.add(p);
                }
            }
        }
        return adjacentPlanets;
    }

    /**
     * strategy concerning conquering neutral planets
     */
    public Map<IVisiblePlanet, Map<IVisiblePlanet, Long>> conquerNeutral(List<IVisiblePlanet> conqueredVisiblePlanets,
                                                                         List<IVisiblePlanet> neutralVisiblePlanets) {
        // source is a map that stores source planets and map destination
        Map<IVisiblePlanet, Map<IVisiblePlanet, Long>> source = new HashMap<IVisiblePlanet,
                Map<IVisiblePlanet, Long>>();
        // if there are no neutral planets, we don't need to do anything
        if (neutralVisiblePlanets.size() > 0) {
            for (IVisiblePlanet myPlanet : conqueredVisiblePlanets) {
                // myPop keeps track of the current population of our planet
                long myPop = myPlanet.getPopulation();
                // destination is a map that stores destination planets and transferred population of events
                Map<IVisiblePlanet, Long> destination = new HashMap<IVisiblePlanet, Long>();
                // neutral planets are sorted
                List<IVisiblePlanet> sortedPlanets = edgeSortPlanets(myPlanet, neutralVisiblePlanets);
                // we make sure that our planet has adjacent planets
                // we loop through the edges of the target and the list of surrounding planets
                if (getAdjacentPlanet(myPlanet, neutralVisiblePlanets).size() > 0) {
                    for (IVisiblePlanet neuPlanet : sortedPlanets) {
                        for (IEdge edge : myPlanet.getEdges()) {
                            // when they match, we collect three variables to be executed
                            // once variables are put, myPop decrements
                            // myPop needs to be larger than 1 because we don't want to lose the planet
                            if (edge.getDestinationPlanetId() == neuPlanet.getId() && myPop > 1) {
                                // if the neutral planet is isolated, only one shuttle is sent
                                if (neuPlanet.getEdges().size() == 1) {
                                    destination.put(neuPlanet, (long) 1);
                                    myPop -= 1;
                                }
                                // if the neutral planet is not isolated, we uniformly distribute the shuttles
                                else {
                                    long min = 1;
                                    int count = countEdges(myPlanet, neutralVisiblePlanets);
                                    // the population is evenly divided
                                    if ((myPop - 1) / count >= 1) {
                                        min = (myPop - 1) / count;
                                    }
                                    destination.put(neuPlanet, min);
                                    myPop -= min;
                                }
                            }
                        }
                    }
                    source.put(myPlanet, destination);
                }
            }
        }
        return source;
    }

    /**
     * finding the index of the planet that has the minimum distance in a list of planets
     */
    public int findMinDist(List<Integer> dist, List<IVisiblePlanet> planets) {
        // dist represents the list contaning the distances from planets to a origin
        int index = 0;
        // min is a sufficiently large integer
        int min = 99999;
        for (int i = 0; i < planets.size(); i++) {
            if (dist.get(planets.get(i).getId()) < min) {
                min = dist.get(i);
                return i;
            }
        }
        return index;
    }

    /**
     * finding the shortest path from the origin to the destination using Dijkstra's algorithm
     */
    public Stack<IVisiblePlanet> findPath(IVisiblePlanet origin, IVisiblePlanet destination,
                                          List<IVisiblePlanet> conqueredVisiblePlanets, int size) {
        // integer size is the size of the list containing all planets
        // we create a set that contains all edges in a graph
        Set<IEdge> edges = new HashSet<>();
        // all edges are collected
        for (IVisiblePlanet p : conqueredVisiblePlanets) {
            Set<IEdge> theseEdges = p.getEdges();
            edges.addAll(theseEdges);
        }
        // indexes of dist are distances from the source of planets, and those distances are stored
        // indexes of prevPlanets are IDs of planets, and their previous planets are stored
        List<Integer> dist = new ArrayList<Integer>();
        List<IVisiblePlanet> prevPlanets = new ArrayList<IVisiblePlanet>();
        // we add invalid values to dist and prevPlanets
        for (int i = 0; i < size; i++) {
            dist.add(99999);
            prevPlanets.add(null);
        }
        // the distance from the origin to itself should be 0
        dist.set(origin.getId(), 0);
        // we use Q to represent a "queue" that stores the planets
        List<IVisiblePlanet> Q = new ArrayList<IVisiblePlanet>();
        for (IVisiblePlanet p : conqueredVisiblePlanets) {
            Q.add(p);
        }
        // we need to find the smallest element of the queue
        int index = findMinDist(dist, Q);
        while (Q.size() != 0) {
            // the first element of Q is removed and collected
            IVisiblePlanet u = Q.remove(index);
            for (IVisiblePlanet myPlanet : getAdjacentPlanet(u, conqueredVisiblePlanets)) {
                // if a shorter path is found, the length of the path and the target's previous planet are gathered
                if (dist.get(myPlanet.getId()) > dist.get(u.getId()) + getEdge(u, myPlanet, edges).getLength()) {
                    prevPlanets.set(myPlanet.getId(), u);
                    dist.set(myPlanet.getId(), getEdge(u, myPlanet, edges).getLength() + dist.get(u.getId()));
                }
            }
            // finding the smallest element of the queue after its first element is removed
            index = findMinDist(dist, Q);
        }
        // reversedPath is a queue storing the previous planets of the destination
        // their order represents the path from the destination to the source
        Queue<IVisiblePlanet> reversedPath = new LinkedList<IVisiblePlanet>();
        IVisiblePlanet temp = destination;
        // we put previous planets of the destination until the source is found
        while (!temp.equals(origin)) {
            reversedPath.add(temp);
            temp = prevPlanets.get(temp.getId());
        }
        // path is a stack storing the path from the source to the destination
        // we use a stack to reverse the queue named reversedPath
        Stack<IVisiblePlanet> path = new Stack<IVisiblePlanet>();//Target path.
        while (!reversedPath.isEmpty()) {
            IVisiblePlanet tempStep = reversedPath.poll();
            path.push(tempStep);
        }
        return path;
    }

    /**
     * finding planets on the battle front and those that are behind them
     * */
    public List<IVisiblePlanet> findPlanets(List<IVisiblePlanet> myPlanets,
                                            List<IVisiblePlanet> enemyPlanets, String str) {
        // a list named planets is used to store the planets to be collected
        List<IVisiblePlanet> planets = new ArrayList<>();
        for (IVisiblePlanet planet : myPlanets) {
            // if the string is "F", which represents "Front", then we collect planets on the battle front
            if (str.equals("F")) {
                if (getAdjacentPlanet(planet, enemyPlanets).size() > 0) {
                    planets.add(planet);
                }
            }
            // if the string is "R", which represents "Rear", then we collect planets that are not on the battle front
            else if (str.equals("R")) {
                if (getAdjacentPlanet(planet, enemyPlanets).size() == 0) {
                    planets.add(planet);
                }
            }
        }
        return planets;
    }

    /**
     * strategy concerning conquering sending our population to the battle front
     * */
    public Map<IVisiblePlanet, Map<IVisiblePlanet, Long>> sendPopulation(List<IVisiblePlanet> myPlanets,
                                                                         List<IVisiblePlanet> enemyPlanets, int size) {
        // sourceMap is a map that stores source planets and a map named destMap
        Map<IVisiblePlanet, Map<IVisiblePlanet, Long>> sourceMap = new HashMap<IVisiblePlanet,
                Map<IVisiblePlanet, Long>>();
        // planets on or not on the battle front are collected
        List<IVisiblePlanet> rearPlanets = findPlanets(myPlanets, enemyPlanets, "R");
        List<IVisiblePlanet> frontPlanets = findPlanets(myPlanets, enemyPlanets, "F");
        for (IVisiblePlanet p1 : rearPlanets) {
            for (IVisiblePlanet p2 : frontPlanets) {
                // a path from the origin to the destination is generated(rear to front)
                Stack<IVisiblePlanet> path = findPath(p1, p2, myPlanets, size);
                IVisiblePlanet source = p1;
                // pairs of adjacent planets in the path are generated
                // the information is collected by sourceMap and destMap
                while (path.size() > 0) {
                    IVisiblePlanet dest = path.pop();
                    Map<IVisiblePlanet, Long> destMap = new HashMap<IVisiblePlanet, Long>();
                    // we send half of the source's population
                    destMap.put(dest, source.getPopulation() / 2);
                    sourceMap.put(source, destMap);
                    source = dest;
                }
            }
        }
        return sourceMap;
    }

    /**
     * finding the edge that connects two specific plants from a set of all edges
     */
    public IEdge getEdge(IVisiblePlanet u, IVisiblePlanet v, Set<IEdge> edges) {
        IEdge edge = null;
        for (IEdge e : edges) {
            // if there is a edge that connects the two planets, it is returned
            if ((e.getSourcePlanetId() == u.getId()) && (e.getDestinationPlanetId()) == v.getId()) {
                edge = e;
            }
            else if ((e.getDestinationPlanetId() == u.getId()) && (e.getSourcePlanetId() == v.getId())) {
                edge = e;
            }
        }
        return edge;
    }

    /**
     * sorting shuttles by their turns to arrival
     */
    public List<IShuttle> sortShuttles(IVisiblePlanet planet, List<IVisiblePlanet> planets) {
        // incomingShuttles stores the incoming shuttles
        List<IShuttle> incomingShuttles = new ArrayList<IShuttle>();
        for (IShuttle shuttle : planet.getIncomingShuttles()) {
            incomingShuttles.add(shuttle);
        }
        for (int i = 0; i < incomingShuttles.size(); i++) {
            for (int j = i; j < incomingShuttles.size(); j++) {
                // we use bubble sort to sort the shuttles
                if (incomingShuttles.get(i).getTurnsToArrival() > incomingShuttles.get(j).getTurnsToArrival()) {
                    IShuttle temp = incomingShuttles.get(i);
                    incomingShuttles.set(i, incomingShuttles.get(j));
                    incomingShuttles.set(j, temp);
                }
            }
        }
        return incomingShuttles;
    }

    /**
     * calculating the change of population of a target enemy planet when it is attacked
     */
    public long enemyChange(IVisiblePlanet planet, List<IVisiblePlanet> myPlanets, List<IVisiblePlanet> enemyPlanets){
        long result = planet.getPopulation();
        // Our shuttles and enemy shuttles approaching us are recorded
        List<IShuttle> myShuttles = sortShuttles(planet, myPlanets);
        List<IShuttle> enemyShuttles = sortShuttles(planet, enemyPlanets);
        int myMax = 0;
        int enemyMax = 0;
        int max = 0;
        // collect the time it takes for the last shuttle that is ours to arrive
        if (myShuttles.size() > 0){
            myMax = myShuttles.get(myShuttles.size()-1).getTurnsToArrival();
        }
        // collect the time it takes for the last opponent shuttle to arrive
        if (enemyShuttles.size() > 0) {
            enemyMax = enemyShuttles.get(myShuttles.size() - 1).getTurnsToArrival();
        }
        // find which one takes more time to arrive completely, then collected the time by max
        if (enemyMax >= myMax){
            max = enemyMax;
        }
        else {
            max = myMax;
        }
        // if there are no shuttles approaching, then the change should be 0
        if (max == 0){
            return 0;
        }
        // turnMap has keys of different turns to arrival and values of the corresponding shuttles
        Map<Integer, List<IShuttle>> turnMap = new HashMap<Integer, List<IShuttle>>();
        // turnMap is initialized
        for (int i = 1; i <= max; i++){
            List<IShuttle> lst = new ArrayList<IShuttle>();
            turnMap.put(i,lst);
        }
        // our shuttles corresponding to turns are collected
        if (myShuttles.size() > 0) {
            for (IShuttle myShuttle : myShuttles) {
                int turn = myShuttle.getTurnsToArrival();
                List<IShuttle> turnList = turnMap.get(turn);
                turnList.add(myShuttle);
                turnMap.put(turn, turnList);
            }
        }
        // enemy shuttles corresponding to turns are collected
        if (enemyShuttles.size() > 0) {
            for (IShuttle enemyShuttle : enemyShuttles) {
                int turn = enemyShuttle.getTurnsToArrival();
                List<IShuttle> turnList = turnMap.get(turn);
                turnList.add(enemyShuttle);
                turnMap.put(turn, turnList);
            }
        }
        // popMap has keys of turns and values of changes of population for each turn
        Map<Integer, Long> popMap = new HashMap<Integer, Long>();
        for (int i : turnMap.keySet()){
            long change = 0;
            for (IShuttle s: turnMap.get(i)){
                // our shuttles are divided by 1.1, because we are attacking
                if (s.getOwner() == Owner.SELF){
                    change += s.getNumberPeople()/1.1;
                }
                // the enemy is defensing
                else{
                    change -= s.getNumberPeople();
                }
            }
            popMap.put(i, change);
        }
        // g represents the rate of growth of the enemy planet
        long g = 1 + (planet.getHabitability() / 100);
        // the enemy planet is attacked or reinforced, and it is growing, so the population changes
        for (int i = 1; i <= myMax; i++){
            result += result * g;
            result += popMap.get(i);
        }
        return result;
    }

    /**
     * sorting enemy planets by the number of edges they have
     */
    public List<IVisiblePlanet> sortEnemyPlanets(List<IVisiblePlanet> planets) {
        for (int i = 0; i < planets.size(); i++) {
            for (int j = i; j < planets.size(); j++) {
                // we use bubble sort to sort them
                if (planets.get(i).getEdges().size() < planets.get(j).getEdges().size()) {
                    IVisiblePlanet temp = planets.get(i);
                    planets.set(i, planets.get(j));
                    planets.set(j, temp);
                }
            }
        }
        return planets;
    }

    /**
     * strategy concerning attacking enemy planets
     */
    public Map<IVisiblePlanet,Map<IVisiblePlanet,Long>> attackRole(List<IVisiblePlanet> myPlanets,
                                                                   List<IVisiblePlanet> enemyPlanets) {
        // source is a map that stores source planets and a map named destination
        // destination is a map that stores destination planets and transferred population of events
        Map<IVisiblePlanet, Long> source = new HashMap<IVisiblePlanet, Long>();
        Map<IVisiblePlanet, Map<IVisiblePlanet, Long>> destination = new HashMap<IVisiblePlanet,
                Map<IVisiblePlanet, Long>>();
        // enemy planets are sorted
        List<IVisiblePlanet> sortedPlanets = sortEnemyPlanets(enemyPlanets);
        for (IVisiblePlanet enemyPlanet : sortedPlanets) {
            // sourceList represents the list of our planets surrounding the specific enemyPlanet
            List<IVisiblePlanet> sourceList = getAdjacentPlanet(enemyPlanet, myPlanets);
            // we make sure that enemyPlanet is surrounded by our planets
            if (sourceList.size() > 0) {
                // Pop is the sum of the population of planets in sourceList
                long Pop = 0;
                for (IVisiblePlanet planet : sourceList) {
                    Pop = Pop + planet.getPopulation();
                }
                // we make sure the enemy planet can be conquered
                if ((Pop - sourceList.size() > (enemyPlanet.getPopulation() +
                        enemyChange(enemyPlanet, myPlanets, enemyPlanets)) * 2)) {
                    for (IVisiblePlanet myPlanet : myPlanets) {
                        Long population = (long) (myPlanet.getPopulation() * 0.6);
                        // 60% of our population is sent to attack the enemyPlanet
                        source.put(myPlanet, population);
                    }
                    destination.put(enemyPlanet, source);
                }
            }
        }
        return destination;
    }
}
