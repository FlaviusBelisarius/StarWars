# StarWars
## Overview  
A composite algorithm worked on StarWars game. 

## Author  
UI & game materials are provided by Professor C. Myers  
Algorithm Authors: J. Nie(me) & H.Z. Wu 
Completed on December 6th, 2018  
If you are one of the authors and you think I have an infringement please contact me and I will hide these contents. 

## Program Details
### How to run
We wrote and test this game in IntelliJ IDEA with jdk 16, actually other lower/higher version jdk also works. Remember to change project settings if you are using different jdk.   
Run main function in src/planetwars/publicapi/Driver.java to start a graphic game window.   
Run main function in src/planetwars/publicapi/StrategyRanker.java to start a comparison between two different strategies(without graphic game window). 

### Algorithm and strategy
Our strategy basically contains three parts. First of all, the program will try to conquer as much neutral planets as it can at the beginning of the game. Shuttles will be sent to conquer neutral planets with more edges, so that we can use those neutral planets to conquer more planets. Second, after all the neutral planets are conquered, we send forces on the battle front to attack enemy planets. We make sure that those enemy planets can be conquered. Third, we send population from rear to front to reinforce planets that are in the battle front. The program generates the shortest path using Dijkstra’s algorithm.  
We did not consider much of habitability. Since our strategy is a very offensive one, we are trying to conquer as many planets as possible without considering habitability. We make sure that planets we send to have many edges, and are relatively close to reach. Since they have many edges, we can use those edges to conquer more planets. It’s hard for us to write a strategy that favors high habitability at the expense of the speed to conquer planets.   
Algorithm and strategy is implemented in src/planetwars/strategies/MyStrategy.java

### Data structures
For this project, we were asked to use as much as different data strucstrues. The array lists are mainly used to store different kinds of planets. Maps and nested maps are mainly used for the transportation of shuttles in eventsToExecute. The nested map stores the source planet, the destination planet and the number of people to be transported. 

## Final results
We got the third highest winning rate in final competition(More than 70 teams joined the competition). 
<p align="center">
  <img src="https://github.com/FlaviusBelisarius/StarWars/blob/master/img/competition-result.jpg" />
</p> 
