# Distance computation table

The camera software generates a 'closeness' value which increases as the robot gets closer to the vision targets.
These values can be mapped to pysical distances on the following table:

| Distance (inches) | Closeness Value |
|-------------------|-----------------|
| 10                | 4100            |
| 12                | 3800            |
| 18                | 2400            |
| 20                | 2000            |
| 24                | 1600            |
| 30                | 1150            |
| 36                | 900             |
| 40                | 750             |
| 48                | 600             |

Since the camera will be positioned about 8 inches into the robot, and the peg is 10 inches log, the robot should approach the target to a closeness value of about 2400.
