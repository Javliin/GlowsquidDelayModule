# Glowsquid Delay Module
This is a module developed for [Glowsquid](https://github.com/Javliin/Glowsquid) which aims to provide or exaggerate the combat advantages of a bad connection without actually decreasing the speed of the connection. To achieve this, the module splits and delays enemy movement packets in order to slow movement under specific conditions.

There are three child modules within the parent module:

**Command Module** \
This module simply provides commands which allow the module delay to be changed. All values are recorded in milliseconds.
```
!help - Shows the description, command, and value of each delay
!erange - Changes the enemy range delay
!edelay - Changes the general enemy delay
!tps - Changes the teleporting smoothing Delay
```
**Server Movement Delay Module** \
This module will initiate and handle all three types of delays, and track enemy player movement.

**Client Movement Delay Module** \
This module used to handle a fourth type of delay, which is currently commented since it tends to cause immediate issues with anti-cheat plugins. Currently it tracks the movement of the client player. This child module will likely soon be removed or reworked.

**NOTE**: In order for the delay modules to function correctly, both of them must be active simultaneously. Since they both play a part in tracking entity movement, they are both necessary in order for delay conditions to be activated properly.

## Delay / Splitting Methods
After a delay is triggered, the movement packet will be delayed by the set value for the delay type. If any additional movement packets for the delayed entity are received before the delayed packet is sent, the delay is stacked. However, the delay of stacked packets will decrease as the amount of stacked delays increase. This allows the delayed entity to smoothly and gradually correct its position instead of snapping back or maintaining an incorrect position.

If a delay value spans multiple ticks, the movement packet will also be split. The amount of times a packet is split depends on the amount of currently stacked packets, with a higher number resulting in a smaller split. This allows the delayed entity to slow their movement rather than stopping suddenly, all without causing issues with stacked delays.

## Delay Types
### Enemy Ranged
Enemy ranged delay will delay players who are exiting a certain distance of the client player. The threshold value is currently hard-coded, and it aims to be about the distance of equal-height player reach. The intention is to simulate momentary connection lag spikes that players with a bad connection often receive, but only in potentially beneficial combat situations.

### Enemy General
General enemy delay will delay enemy players who are moving away from the client player, regardless of distance. This intends to simulate the client-server position latency of a laggy player, but again only in potentially beneifical situations.

### Teleport Smoothing
While the entity teleport packet is intended to be used for moving entities a distance of greater than four blocks, some servers will send them when an entity move packet could be used instead. Since a sudden change in absolute position usually calls for cancelling all active delays, translating these low-distance teleport packets into movement packets will allow for less delay interruptions. 

## Disclaimer
I do not condone using this to gain an unfair advantage on servers. High delay values (and even low values on some servers) will likely flag anti-cheats and cause a ban. This should only be used for testing purposes. Even if your intentions are not malicious, DO NOT use this module on any server without permission.
