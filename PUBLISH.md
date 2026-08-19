# Create: LazyTick

Create: LazyTick (CLT) is a client-and-server performance addon for
[Create](https://github.com/Creators-of-Create/Create). It reduces repeated,
unproductive ticking in Create automation while preserving normal processing,
event-driven wakeups, and bounded retries for machines that still need to make
progress.

Install the matching CLT build on the server and on every connecting client.

<p align="center">
  <a href="https://github.com/duckgun13476/Create-LazyTick"><img src="https://raw.githubusercontent.com/intergrav/devins-badges/v3.2.0/assets/cozy/social/github-singular_vector.svg" alt="GitHub Repository"></a>
</p>

![Create: LazyTick optimization overview](https://raw.githubusercontent.com/duckgun13476/Create-LazyTick/CLT-combined/assets/optimization-overview-en.png)

## Highlights

- Lazy scheduling for eligible idle machines and bounded retry states.
- Recipe and state caches that avoid repeated expensive lookups when machine
  state has not changed.
- Optimizations for belts, funnels, chutes, depots, item drains, basins,
  mechanical crafters, saws, deployers, spouts, mechanical arms, and fluid
  scheduling.
- Create 6.0.x factory gauges use adaptive backoff only after repeated,
  unchanged observations of a safe passive display panel.
- Wireless redstone links avoid a full network propagation only when the
  transmitted signal is unchanged. Every real strength change still propagates
  through Create normally.

## Measured Results

The comparison above summarizes the average running work of sample production
lines before and after CLT. Lower green bars are better. Results vary with the
Create version, addons, factory topology, configuration, and whether a line is
idle or actively producing; they are measured examples, not guarantees.

More measurement context is available on the [MC百科 page](https://www.mcmod.cn/class/23850.html).

## Supported Versions

| Loader | Minecraft | Create target |
| --- | --- | --- |
| Forge | 1.19.2 | 0.5.1.i |
| Forge | 1.20.1 | 0.5.1.j |
| Forge | 1.20.1 | 6.0.x |
| NeoForge | 1.21.1 | 6.0.x |

## Installation

1. Install the matching Create build and its normal loader dependencies.
2. Download the CLT jar for the same Minecraft, loader, and Create generation.
3. Put CLT in the `mods` folder on both server and clients.
4. Start once to generate configuration, then test representative automation
   before using it in a production world.

## Configuration and Timing

CLT adds a **Lazy Clock** for per-machine control. Its settings board has two
rows using the configurable percentage cycle (by default `0`, `25`, `50`, `75`,
and `100`):

- **Dynamic control** sets the ceiling for adaptive backoff as a percentage of
  that machine family's configured maximum delay.
- **Forced control** locks the machine to one fixed interval, also as a
  percentage of that maximum, and overrides dynamic backoff.
- **Forced `0%`** keeps the machine at full speed and disables CLT optimization
  for that machine.

The controls are alternatives, not cumulative limits. Server configuration
chooses which row ordinary clock right-clicks cycle by default; the settings
board can select either row directly.

Start with the defaults. Timing-critical machines can be kept at full speed
with the Lazy Clock instead of disabling CLT globally. The Create 6.0.x
`factory-gauge` configuration group controls the stable-monitor backoff and
its maximum refresh interval.

Some paths intentionally stay at normal or bounded cadence to protect short
interaction windows and eventual output progress. Factory-gauge backoff applies
only to a satisfied passive display panel with no requester, restocker, redstone
input, unloaded link, pending promise, or incoming panel/link connection.

Create: LazyTick is licensed under [GNU GPLv3](https://github.com/duckgun13476/Create-LazyTick/blob/CLT-combined/LICENSE) (`GPL-3.0-only`).
