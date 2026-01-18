coach.yaml contains a draft of the exercise planner yaml file. Implement the planning algorithm, replacing the current simple planner. 
Add test yamls to `src/test/res` for unit tests of the planner. Implement several unit tests. See comments in the yaml for further details on how it should operate. Create model classes for clean deserialization of the yaml. It's ok to add a dependency for yaml deserialization if needed.

Inputs:

- yaml spec
- historic exercises completed from DB 
- time available today 
- location today

Algorithm:
Calculate Deficits: Target Goal - Sum(History) over window_days.

​Filter Eligible Blocks:
​Does block.location match current_location (or is it `anywhere`)?
​Does block.size_minutes fit in time_available?
​Do fatigue_constraints pass? (Sum prior load < threshold).
​
Greedy Selection:
​Iterate through priority_order.
​Find the first block that reduces a deficit for that priority.
​(Special handling for lists like [20, 30, 45] to pick the optimal size, see comment in yaml).
​Add to schedule, subtract time, update deficits, and repeat.
