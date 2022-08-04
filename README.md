# Timetable Generator

A school timetable generator based on a parallelized [Genetic Algorithm](https://en.wikipedia.org/wiki/Genetic_algorithm)
that's hybridized with [Simulated Annealing](https://en.wikipedia.org/wiki/Simulated_annealing) and
greedily randomized construction of the initial population.

## Problem Description

The school timetabling problem involves allocating resources (e.g. teachers, classes, rooms) to timeslots,
such that each resource has at most one allocation per timeslot.
This is an NP-complete problem [1], meaning there exists no efficient algorithm for finding an optimal solution for the problem. 

This project tackles the Class-Teacher variant of this problem [1]. It is assumed that class & teacher
associations are known beforehand (i.e. it is known which classes ought to take what lessons with which teachers). This 
is typical of most schools in some regions. Additionally, each class is expected to have an allotted room for each lesson,
which is not an unusual expectation. Thus, room allocations aren't taken into account.

A solution of the timetabling problem should satisfy two types of constrains: hard & soft constrains. A solution
is only considered feasible if it satisfies 100% of the hard constraints, while soft constraints give a measure
of quality to the solution and aren't expected to be completely satisfied. 

The following constraints are used:

* **Hard Constrains**
  * *Teacher Clashes*: A teacher must be assigned at most one class in each timeslot.
  * *Class Clashes*: A class must be assigned to at most one teacher in each timeslot.
  * *Class Idleness*: A class mustn't have any idle timeslots amid lessons.
  * *Teacher Unavailabilities*: A teacher mustn't be assigned a timeslot in which they are not available.
    Unavailabilities should be known beforehand.
  * *Daily Exceedances*: A lesson (class-teacher association) mustn't be scheduled more than a pre-specified number of times per day.

* **Soft Constraints** 
  * *Teacher Idleness*: The number of teacher idle timeslots amid lessons should be minimized.
  * *Double Lessons*: Each lesson should have no less than a pre-specified number of contiguous two occurrences per week.

## Genetic Algorithms

One of the cool(est) schemes computation draws from nature is Genetic Algorithms (but isn't nature itself an overly mature computation?).
Genetic Algorithms rely on the concept of natural selection to produce solutions for optimization or search-based problems.
The algorithm typically starts with a population of randomly generated solutions, each having certain properties (genotype)
affecting its fitness. The algorithm repeatedly generates new populations by selecting, mating and mutating
individuals from the previous population. The selection and mating schemes are chosen such that good properties
are inherited by new populations. Mutation helps to expand the search space for finding new good properties, and not
getting trapped in a local minima.

### Representation

A good representation is crucial to the success of a Genetic Algorithm. The representation has to take
crossover in mind. Two representation were tested:

* An array of lists, where each array position represents a period (a day and a timeslot in that day, in a linearized day-major layout), and each list represents the lessons
  scheduled during that period [2].

* A matrix where each row represents a period, and
  each column represents a class [4]. The value in each cell is the teacher assigned to the cell's
  class at the cell's period, or -1 if no lesson is scheduled for the cell. This representation eliminates
  class clashes, as no class shares the same period with another.

### Mating

The first representation resulted in lower quality results. This is mainly because it makes mating tricky. In order
to mate two solutions, each list is merged with another across a crossover site. This results in lessons
being lost and/or repeated in the resulted offspring, requiring a label replacement algorithm to randomly
replace or remove lost or repeated lessons respectively [2]. 

Mating of the second representation is more straightforward and less error-prone. An offspring inherits
each column from either of its parents. Since each column represents a class's schedule, no lessons are lost as
each schedule contains the same lessons but differs only in assigned periods.

In addition to random selection of either parent's class schedule to generate an offspring, [roulette wheel selection](https://en.wikipedia.org/wiki/Fitness_proportionate_selection)
is used to give the fittest parents higher probability of getting their class schedules selected.
These two schemes are applied in turn each iteration. This allows some diversity in the population, while ensuring offsprings inherit
more genes from the fittest solutions.

### Selection

[Roulette wheel selection](https://en.wikipedia.org/wiki/Fitness_proportionate_selection) is used to
select parents during mating. This allows the fittest solutions to have more probability of being
selected, but doesn't completely eliminate less fit solutions from being selected. This is good as less
fit solutions might still contain good isolated properties.

Instead of generating a mating pool using roulette wheel and selecting random solutions from the pool 
when mating, roulette wheel selection was performed on the whole population whenever a parent is need.
This gave better results than the former approach as it gives less fit solutions better chance to get selected.

### Mutation

In each iteration of the algorithm, a mutation strategy of two is chosen randomly. The first scheme
chooses a class randomly and swaps the teachers at two randomly selected timeslots. The mutation is performed based on some probability that's increased each iteration
(ensuring more mutations happened at a minima). The second mutation strategy is based on Simulated Annealing.

## Simulated Annealing

Simulated Annealing is another computational inspiration from a natural process. It draws from the concept
of annealing in metallurgy, where materials are controllably cooled from high temperatures, seeking better
quality (less hardness and more workability in case of metals). 

At each iteration of Simulated Annealing, a random mutation is performed on the timetable (analogous to random
atom displacements in highly heated materials). If the mutation results in a lower cost, the mutation is accepted.
Otherwise, the mutation is accepted with some probability that decreases each iteration (analogous to less
atom displacements as temperature decreases).

## Greedily Randomized Construction

A good quality initial population is generated using a greedily randomized algorithm [3]. At first, an 
empty timetable is created. As long as there are lessons to schedule, the algorithm proceeds as follows:

* Create a candidate list containing all the lessons whose required number of weekly occurrence
  is not yet exhausted.
* Calculate an urgency degree to each lesson that equals `unscheduled[lesson.class][lesson.teacher] / (intersections + 1)`. Where
  `unscheduled[lesson.class][lesson.teacher]` is the number of weekly occurrences not yet satisfied for the lesson, 
  and `intersections` is the number of yet unscheduled periods available to both `lesson.class` and `lesson.teacher`.
* Calculate min & max of the urgency degrees.
* Create a restricted candidate list from the initial list by only choosing lessons that have `urgency[lesson.class][lesson.teacher] >= maxUrgency - alpha * (maxUrgency - minUrgency)`. `alpha`
  controls the degree of randomness to greediness the algorithm uses (`0` gives no randomness, `1` gives no greediness).
* Select a random lesson from the restricted list and allocate it to a random period available to both the class and the teacher,
  or only available to the class if there's no former period (possibly introducing a teacher clash).

## Some Results

Run `Solver.main`. The printed solution presents each day's timetable, where rows represent the class
index and columns represents the day's timeslots. Each cell holds the teacher assigned to the cell's class at
its timeslot.

```
Day: 0
       0   1   2   3   4
   0  22  41   5   5  41
   1   1   1  24   0   0
   2   7   7  19  15  19
   3  34  34   1  21  21
   4   9   5   9  10   5
   5  37  32  15  32   7
   6  11  11  14   9   9
   7  39  26  26  36  36
   8  24  22  22  44  44
   9  13  13  11  11  14
  10  15   2   7   7  24
  11   5   0   0  22  22
  12  38  38   4   4   3
  13  42  42  32  14  32
  14  10  20  41  20  10
  15  43  15   3   3  15
  16  41   9  20  41  11
  17  16  10  10   2   2
```

## References

[1] Łukasz Antkowiak, "Parallel algorithms of timetable generation," School of Computing, Blekinge Institute of Technology, Sweden, 2013.

[2] David Abramson and J Abela, "A parallel genetic algorithm for solving the school timetabling problem," In 15 Australian Computer Science Conference,1992.

[3] Haroldo G Santos, Luiz S Ochi, and Marcone JF Souza, "An efficient tabu search heuristic for the school timetabling problem," In Experimental and Efficient Algorithms, pages 468–481. Springer, 2004.

[4] GN Beligiannis, C Moschopoulos and SD Likothanassis. "A genetic algorithm approach to school timetabling," University of Ioannina, Agrinio, Greece; and University of Patras, Rio, Patras, Greece, 2009.
