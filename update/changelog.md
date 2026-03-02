### 2.4.7-beta
#### Fix
1. Fix basin recipe cache key mismatch by making basin cache matching quantity-sensitive for items and fluids.
2. Fix multiplayer basin getting stuck after temporary recipe invalidation by adding a short retry window before falling back to lazy skip.


---
### 2.4.6-beta
#### Fix
1. Add server support Fix class leak.


---
### 2.4.5-beta
#### Fix
1. Fix a Create crash related to scroll option behavior handling.

---
### 2.4.4-hotfix-beta
#### Fix
1. Fix basin recipe processing issue.
2. Fix UI config adjustment becoming unavailable when tick time is greater than 50ms.

---
### 2.4.3-beta
#### Emergency Fix
1. Fix external launcher crash caused by `use` method mixins not remapping correctly in production jars.
2. Fix loading failure on Create interaction blocks such as Depot, Arm, Item Drain, Funnel, Saw, Chute and Mechanical Crafter.

---
### 2.4.2-beta
#### Feature
1. New scroller UI with slide interaction for LazyTick adjustment.
2. Added a new modified menu/screen pipeline with simpler menu definition.
3. Added synced UI state flow and auto-close behavior for the new config screen.
4. Improved UI presentation with animated background, percent render and JEI-friendly layout.

#### Optimize
1. Completely rewrote basin optimization logic and fixed a hidden deadlock issue. (PR #14)
2. Added a new NutUI menu library foundation and improved automatic channel/data flow.

#### Fix
1. Fix global UI/menu problems for the new screen flow.
2. Fix portable storage interface extraction issue on funnel-related transfer.
3. Fix client-only class loading on dedicated server.
4. Fix general loading issues on the 1.20.1-6.0.x-forge branch.
5. Fix Gradle/build configuration issues for current development branch.

---
### 2.3.2-beta
1. Fix funnel sync problem.
2. Fix mixer stuck when only process once.(#12/1000mb)
3. Fix belt cache problem
---
### 2.4-beta (Big Update)
#### Feature
1. New item, lazy clock, allows for BlockEntity control using the clock.
2. Now you can use clock to use scroller to set tick particular.
3. New Command system: You can use /createlazytick [command] to search which entity is controlled by user(Avoid abuse)
4. New UI: When you have CLT clock in main hand and with Create Glass, you can see a better UI to know Entity symptom.

#### Fix
1. Fix belt delay problem(Contributor: miiiiiint #5)
2. Fix saw problem(now saw won't lock recipe.)
3. Fix fluid system Opt problem to improve compat for other mod's inject(destroy,mek)
4. Fix funnel problem, now funnel can be over tick by redstone,even if author advise use clock to control.
5. Fix clock model.
6. Fix mechanical arm match problem(some mechanical arm is malfunction on belt)
7. Fix funnel deadlock which cause moving structures can't transfer item with interface.

---
### 1.3-beta
1. Fix config problem.
2. Add Spout recipes cache
---
### 1.2-beta
1. Fix a crush bug.
---
### 1.1-beta
1. Added LazyTick for Arm, Crafter, Deployer, Item drain and Saw.
2. Further optimization of the work basin has been made, but it may still not be fully optimized.
3. The format of the config has been rewritten to allow better control over formatting.
4. Fixed all potential issues that were not correctly labeled (remap = false).
---
### 1.1-alpha
1. Add all LazyTick to config.(为所有的Mixin添加了配置项)
---
### 1.0-alpha
1. Add chute and funnel lazy tick.
2. Plant lazytick for create6.0.
3. Add depot lazytick.
4. fix chute delay problem.
5. Fix animal problem.
6. fix multi input problem.




