# StarWars
### Overview  
An automatically running game based on Dijkstra Algorithm. 

### Author  
UI & Other materials are provided by Professor C. Myers  
Flavius Belisarius(Of course it is a fake name) & H.Z. Wu  
Completed on December 6, 2018  
If you are one of the authors and you think I have an infringement please contact me and I will hide these contents.  

### Summary  
Our strategy basically contains three parts. First of all, we are trying to conquer as much neutral planets as we can at the beginning of the game. We send shuttles to conquer neutral planets with more edges, so that we can use those neutral planets to conquer more planets. Second, after all the neutral planets are conquered, we send forces on the battle front to attack enemy planets. We make sure that those enemy planets can be conquered. Third, we send population from rear to front to reinforce planets that are in the battle front. We generate the shortest path using Dijkstra’s algorithm.

### Details  
For this project, we used four data structures(list, map, queue and stack) directly. In most methods, you can find array lists. The array lists are mainly used to store different kinds of planets. Maps and nested maps are mainly used for the transportation of shuttles in eventsToExecute. The nested map stores the source planet, the destination planet and the number of people to be transported./n
We did not consider much of habitability. Since our strategy is a very offensive one, we are trying to conquer as many planets as possible without considering habitability. We make sure that planets we send to have many edges, and are relatively close to reach. Since they have many edges, we can use those edges to conquer more planets. It’s hard for us to write a strategy that favors high habitability at the expense of the speed to conquer planets.
