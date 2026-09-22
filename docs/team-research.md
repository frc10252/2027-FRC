# FRC code comparison: Team 10252 and 2026 reference projects

## Scope, method, and repository names

This is a static review of the repositories supplied in `C:\Robotics` on 2026-08-30. The reference repositories were inspected only; no reference file was modified, copied, refactored, built, or deployed. The observations below describe this particular snapshot, not an endorsement of any team's design or a claim that it will work on Team 10252's hardware.

The request named Team 6238, but the folder provided is `FRC6328 - RobotCode2026Public-main`, the public code for **Team 6328 (Mechanical Advantage)**. This report therefore compares 1678, 2910, and 6328.

“Observed” means directly visible in the checked-in files. “Interpretation” and “recommendation” are deliberately separated so that inferences do not get mistaken for facts.

## Directly observed facts

### Season, toolchain, and licenses

| Project | Season / robot named in repository | GradleRIO / WPILib version visible | Main vendor or extra libraries visible | License information at repository root |
|---|---|---|---|---|
| 1678, `C2026-Public` | 2026; README calls the robot **Limestone** | GradleRIO `2026.2.1` | ChoreoLib `2026.0.3`, CTRE Phoenix 6 `26.1.0`, PhotonLib `v2026.1.1-rc-3` | A `WPILib-License.md` (BSD-style) is present. No separate top-level license for 1678-authored project code was found in this snapshot. |
| 2910, `2026CompetitionRobot` | 2026 REBUILT | GradleRIO `2026.2.1` | AdvantageKit `26.0.0`, BLine-Lib, MapleSim `0.4.0-beta`, PathPlanner `2026.1.2`, CTRE Phoenix 6 `26.1.3` | Project `LICENSE` is MIT (copyright Team 2910). It also contains attribution/license files for AdvantageKit (GPLv3), Team 254 (MIT), Team 6328 (MIT), and WPILib. |
| 6328, `RobotCode2026Public` | 2026; README calls the robot **Darwin** | `wpilibVersion=2026.2.1` in `gradle.properties`; GradleRIO reads that property | AdvantageKit `26.0.2`, ChoreoLib `2026.0.3`, Sleipnir `2026.0.1`; CTRE Phoenix 6 `26.1.3`, REVLib `2026.0.5`, libgrapplefrc `2026.0.0` | MIT, copyright 2025–2026 Littleton Robotics. |
| 10252, `FrcRobotController-v1.4-main` | 2026 toolchain; no README/season description is included | GradleRIO `2026.1.1` | PathPlanner `2026.1.2`, CTRE Phoenix 6 `26.1.0`, REVLib `2026.0.1` | A `WPILib-License.md` is present; no separate top-level license for Team 10252-authored code was found. |

All four projects use Java and WPILib's command framework. Teams 1678, 2910, and 10252 use the standard Java GradleRIO project layout. 6328 is a larger multi-language project: the snapshot contains about 118 Java and 65 C++/header source files, plus supporting `idun`, `northstar`, `macmini`, and `sim` directories.

### Overall structure and subsystem organization

| Project | Observed organization |
|---|---|
| **1678** | About 198 Java files. It separates a substantial reusable `frc.lib` (base subsystem classes, I/O, logging, simulation, visualization, math, and utilities) from `frc.robot` (autos, controls, shooting, tracking, and subsystems). Mechanisms are small focused packages: drive, shooter, hood, intake deploy/rollers, hopper/feeder/tunnel rollers, climber, LEDs, vision, and a coordinating `Superstructure`. It uses singleton instances such as `Drive.mInstance`, `Superstructure.mInstance`, and `ControlBoard.mInstance`; there is no `RobotContainer.java`. |
| **2910** | About 65 Java files. `RobotContainer` constructs the subsystems and binds controls. It organizes `config`, `constants`, `autos`, `simulation`, `util`, and `subsystems`. The latter contains drive, vision, intake, hopper, shooter, and a `SuperStructure`; reusable motor/roller/servo pieces are under `subsystems.base`. `RobotConfiguration` is an interface with robot-specific implementations such as `ReBlitz`. |
| **6328** | Robot Java code is split into commands, auto commands, energy, `salesman`, subsystem packages, and utilities. Mechanisms include drive, slamtake, hopper, kicker, launcher/flywheel/hood, LEDs, hub counter, sensors, rollers, and vision. `RobotContainer` selects real, simulation, replay, or no-op I/O implementations. It has both `FullSubsystem` and `VirtualSubsystem` abstractions, a global `RobotState`, and separate Java/C++ hardware I/O for some components. |
| **10252** | 14 Java files, primarily `Robot`, `RobotContainer`, `constants`, and `subsystems`. Active construction in `RobotContainer` creates `Drive`, `Shooter`, `Intake`, and CTRE-generated `CommandSwerveDrivetrain`. `Elevator` and `Coral` files exist but are commented out of construction. The container exposes several public static objects, while the underlying CTRE drivetrain is a public instance field. |

### Command-based architecture and controls

All four projects schedule/cancel an autonomous `Command` in the standard robot lifecycle and use WPILib command requirements/bindings.

* **1678:** `Robot` is intentionally active and wires global/singleton systems together. `ControlBoard`, itself a `SubsystemBase`, owns driver and operator `CommandXboxController` bindings and sets the drive default command. Its superstructure returns composed commands for operations such as stowing, intake, homing, and shooting.
* **2910:** `Robot` remains relatively thin. `RobotContainer` creates real or simulated I/O, applies a default swerve teleop command, and maps buttons to command/state transitions. `SuperStructure` uses `WantedState` and internal `CurrentState` enums to coordinate drive, shooter, intake, and hopper behavior.
* **6328:** `RobotContainer` creates subsystems by operating mode and calls separate `configureAutos()` and `configureButtonBindings()` methods. It uses a primary Razer Wolverine controller, a secondary Xbox controller, and an override switch panel. Its command utilities include drive-to-pose/trajectory and compacting/conditional command helpers.
* **10252:** `RobotContainer` provides two Xbox controllers. Drive has a default `FieldCentricFacingAngle` command. Buttons cover brake, wheel-pointing, heading seed/reset, SysId, a relative test path, intake pivot presets, feeders, and shooter RPM-from-vision. There is a right-bumper aim command plus automatic cancellation on release.

### Autonomous design

| Project | Directly observed autonomous approach |
|---|---|
| **1678** | Uses Choreo `AutoFactory` and `AutoChooser`. `AutoModeSelector` registers many named routines, including left/right double and single sweeps, alternate/close/cut-corner variants. Each auto class supplies its initial pose and routine. Seven `.traj` files and `comp.chor` are deployed. |
| **2910** | Uses a custom `AutoChooser`, `Auto`, `AutoProgram`, and `AutoFactory`. The chooser caches selected commands and starting poses by alliance. The deploy tree contains many explicitly blue/red PathPlanner paths plus tuning paths and custom auto JSON. Auto factory code commands superstructure states alongside motion. |
| **6328** | Uses a custom `AutoSelector` that supports a routine plus up to six dashboard questions/responses. `commands.auto.AutoBuilder` and `AutoCommands` construct routines. It deploys a `.traj` file in `vts`; its project also contains a custom sales/assignment solver and special “disruptor” handling in `Robot`. |
| **10252** | Uses PathPlanner's `AutoBuilder` and a `SendableChooser`. It offers Do Nothing plus five named path autos. Every path auto resets pose from the path's starting holonomic pose before following. Only `leftballpickup` also runs intake based on two hard-coded time values (5 s delay, 5 s duration). |

### Drivetrain, sensors, and vision

* **1678:** The `Drive` subsystem wraps a CTRE-generated drivetrain and Pigeon2. It publishes drive telemetry, tracks filtered velocity/acceleration, has stability checks, supports vision measurements, and exposes look-ahead pose methods. It contains Limelight and Photon camera I/O (including simulated implementations), and its `Cameras` subsystem extends a custom camera base. `RobotConstants` selects a field type and declares CANivore/rio buses.
* **2910:** Swerve uses a CTRE I/O implementation on the real robot and a separate `SwerveIOSim`. It has teleop, seeking, path-following, brake, and SysId request paths. Vision is abstracted behind `VisionIO`; the real implementation is `VisionIOLimelight`. `VisionSubsystem` logs each camera input and passes valid AprilTag pose observations to `RobotState`. Its `SuperStructure` can use a vision observation to reset drivetrain translation; its code comments explicitly note that a future fused pose estimator would be better than that one-time reset.
* **6328:** Drive has module and gyro I/O interfaces, simulation variants, a primary and backup gyro, disconnect alerts, and an odometry lock. `RobotState` maintains buffered odometry and estimated poses and applies timestamped vision observations with supplied standard deviations. The vision subsystem accepts `VisionIONorthstar` inputs, processes AprilTag and object-detection frames, rejects unsuitable poses, handles multi-tag/single-tag ambiguity, and reports camera disconnection. Competition construction instantiates four Northstar cameras; another real mode instantiates two.
* **10252:** The CTRE-generated `CommandSwerveDrivetrain` provides standard pose, speed, and `addVisionMeasurement` methods. `Drive` configures PathPlanner and provides field-centric facing-angle driving plus on-the-fly paths. The project has two Pigeon2 constructions for CAN ID 15 (`Constants.imu` and `RobotContainer.imu`). Vision enters through `UdpTelemetryReceiver`, which starts a background UDP thread on port 5800, parses a custom JSON format with `AprilTags`, selects the nearest listed processor tag, and publishes tag count, distance, and yaw information. Current callers use that data for a one-time aim offset and shooter RPM interpolation. A repository-wide search found no call that feeds the received UDP observations into `addVisionMeasurement`; the method exists only in the CTRE drivetrain wrapper.

### Configuration, constants, logging, testing, and simulation

* **1678:** Has per-mechanism constant classes plus `RobotConstants`, `Ports`, and subsystem-specific configuration. It uses unit-typed WPILib values throughout much of the shown code, custom logging helpers, SmartDashboard/Field2d, a 3D mechanism visualizer, simulated motors/cameras, field/game-piece simulation, and four test files (three test classes plus `TestUtil`).
* **2910:** Central general constants sit beside robot-specific configuration objects that include drive modules, cameras, motors, and range sensors. Identity selection is based on robot serial number, with simulation handled separately. It uses AdvantageKit logging, current-limit management, MapleSim, simulated motor/swerve state, and JUnit dependencies. No `src/test` files were found in this snapshot.
* **6328:** Separates basic run-mode/robot-type constants from drive, vision, field, and mechanism constants. `Constants` supports real/sim/replay modes and a 5 ms loop period. AdvantageKit-style logged inputs, tunable numbers, alerts, and cycle tracing are pervasive. It provides a sizeable simulated implementation and one `RobotContainerTest`; the project additionally includes a desktop simulation folder and real-hardware support tooling.
* **10252:** `Constants` combines robot-wide settings, camera correction values, motor CAN IDs, shooter calibration factors, and shooter PID gains. CTRE-generated `TunerConstants` holds drivetrain hardware/calibration data. `Telemetry` publishes to NetworkTables/SmartDashboard, and a `simgui-ds.json` is included. Gradle enables simulation dependencies, but there are no `src/test` files and no custom robot simulation implementation visible beyond the generated drivetrain support.

### Naming and coding conventions

* **1678** uses package-oriented names, per-subsystem `*Constants`, and custom bases/I/O. It mixes newer descriptive names with legacy singleton names such as `mInstance`, and uses tabs in the shown files.
* **2910** uses the team namespace `org.frc2910.robot`, descriptive `*Subsystem`, `*IO`, `*Configuration`, and `*Factory` names, and 4-space formatting. `SuperStructure` uses an internal capital S while most package names are lower case.
* **6328** uses `org.littletonrobotics.frc2026`, concise subsystem names, `*IO`/`*IOSim` hardware boundaries, records/enums, `@AutoLogOutput`, and two-space formatting. Marked sections and well-scoped utilities make a large codebase navigable.
* **10252** follows the standard `frc.robot` namespace and has useful descriptive class names, but styles are not yet uniform: for example, mutable `MaxSpeed` and `scaling` are public fields while constants are also used, and several inactive/commented sections remain in production classes.

## Interpretation and tradeoffs

### What the references are optimizing for

These are not interchangeable templates:

* **1678** appears to optimize for a feature-rich competition robot and rapid mechanism integration. A deep in-house library, singleton model, rich simulator/visualizer, custom shooting planner, and high number of auto variants are efficient when a large, experienced software group maintains shared conventions. The same structure can be difficult for new contributors because data/control ownership is distributed across global instances and custom framework layers.
* **2910** is a middle path: a recognizable WPILib `RobotContainer` owns construction, while configuration interfaces, I/O abstraction, AdvantageKit, a superstructure, and a custom auto factory provide room for multiple hardware configurations and scoring-on-the-move. It is more approachable than a full shared platform, but still assumes the team can validate state-machine interactions, paths, sensors, and logging.
* **6328** is an engineering platform as well as robot code. Its I/O separation, structured logs, replay/simulation modes, buffered state estimator, backup hardware, custom vision pipeline, native code, and mode-specific hardware are appropriate for a team with dedicated maintainers and specialized hardware. Adopting it piecemeal without the required test/logging discipline could increase failure risk rather than decrease it.
* **10252** is small enough that its flow is currently easy to trace: controls are in one place, the drivetrain uses the CTRE generator, and PathPlanner is already connected. Its main limitation is not lack of advanced architecture; it is that some behavior is split between commands and `periodic()` with unverified calibration/configuration, and it has little automated safety net.

Hardware matters as much as software. 1678 uses both Photon/Limelight-compatible camera layers; 2910's real vision adapter is Limelight; 6328 uses Northstar and custom Idun-related hardware/software; 10252 receives custom UDP messages and uses CTRE plus REV devices. A direct port would be both technically unsuitable and, depending on source and dependencies, a licensing/attribution obligation.

### Team 10252-specific risks worth checking before adding features

These are code-review findings, not changes made by this review:

1. **[Team 10252-specific] Shooter command ownership is conflicting.** `Shooter.autoRpmFromDistanceCommand()` sends a CTRE `VelocityVoltage` request, but `Shooter.periodic()` sends direct percent output every scheduler cycle. The latter can replace the velocity request, so auto-RPM may not actually control velocity as intended. Decide on one owner for motor output at a time and confirm it on blocks before competition use.
2. **[Team 10252-specific] Vision data is used for aiming/RPM, not pose correction.** This is a sensible first step, but the drivetrain's vision-measurement API is currently unused. Do not add pose fusion until camera position, time synchronization, coordinate conventions, tag selection, and measurement noise have been measured and logged. Bad pose fusion is worse than no pose fusion.
3. **[Team 10252-specific] The right-bumper aim action samples once on button press.** The binding is `onTrue`, so it calls `aimAtTag()` once, then the drive holds that target until release. If the intended behavior is continuously updated tracking, it needs an intentionally designed `whileTrue`/default-drive interaction rather than an accidental one.
4. **[Team 10252-specific] Hardware/configuration ownership is duplicated.** Two Pigeon2 objects are constructed for CAN ID 15, and general constants mix wiring, calibration, and shot tuning. This is manageable now, but it can make later debugging unclear: there should be one sensor owner and a predictable place for each type of value.
5. **[Team 10252-specific] The left auto depends on guessed timing.** A fixed 5-second delay/duration will drift when a path, battery voltage, or mechanism changes. It is acceptable as a temporary prototype, but a sensor/state or PathPlanner event-marker trigger is more repeatable once the mechanism is reliable.

None of these require adopting another team's architecture. They are opportunities to make the existing code trustworthy first.

### Reusable patterns, scaled to a newer team

| Pattern | Seen in | Small version suitable for 10252 | Why it helps |
|---|---|---|---|
| Thin lifecycle + one construction/binding location | 2910 and 6328 | Keep `Robot` limited to lifecycle scheduling; keep construction, chooser setup, and bindings in `RobotContainer`. | A new programmer can answer “where is this button/motor command created?” quickly. |
| One subsystem owns each actuator | All references, especially I/O-based designs | Make each motor's active mode explicit (manual / velocity / stopped) and command it only through its subsystem. | Prevents `periodic()` and a command fighting over the same motor. |
| Focused constants by purpose | All references | Retain generated `TunerConstants`; split your hand-written values into `Ports`, `DriveConstants`, `ShooterConstants`, and `VisionConstants` only when each has more than a few values. | Reduces wiring/tuning mistakes without creating a large framework. |
| Autos as named command factories | 1678 and 2910 | Use a short `buildAuto...()` method or a small `Autos` class per reliable routine. Start each with a clear pose reset and a safe fallback. | Makes autos reviewable and lets drivers choose them safely. |
| Basic validity gates for vision | 2910 and 6328 | Require recent timestamp, known tag ID, finite distance/yaw, and a dashboard indicator before using a measurement. | Makes failures visible and avoids aiming at stale data. |
| A testable pure calculation | All three references in different forms | Unit-test the UDP parser, tag filter, distance-to-RPM interpolation, and angle math—no hardware needed. | Gives immediate value with a very small test suite. |
| Logging/telemetry aimed at decisions | All references | Publish command/mode, target and measured shooter speed, tag freshness, pose, and current auto name. | Lets students diagnose the robot during practice instead of guessing. |

### Approaches not to adopt yet

* Do not copy a reference repository or its shared library. Beyond respecting the supplied read-only instruction, it would import assumptions about motors, geometry, CAN layout, cameras, field strategy, build tooling, and team workflows that do not match 10252.
* Do not add full I/O interfaces, replay logging, C++ co-processors, custom vision servers, multi-camera fusion, redundant gyros, or complex auto question trees just because 6328 uses them. Each adds hardware and verification commitments.
* Do not build a large superstructure state machine before the intake/shooter/elevator operations and their sensors are stable. Start with a few named commands; introduce a coordinator only when repeated unsafe/invalid combinations become hard to prevent.
* Do not adopt a generic “alliance mirroring” or blue/red path system until field coordinate conventions and path behavior are verified. Your existing PathPlanner alliance supplier is already the right foundation.
* Do not rely on time-only actions for mechanisms once a sensor or an “at target” condition is available. Timing is easy to write but is sensitive to real robot variation.

## What we should adopt first

Prioritized for Team 10252: each item is intentionally small, compatible with the existing CTRE + PathPlanner setup, and does not reuse other teams’ source.

1. **Resolve one-output-owner behavior for every motor.** First fix/verify shooter manual vs. auto-RPM behavior, then apply the same rule to intake and pivot. Make a simple dashboard display of requested mode, target speed, and measured speed.
2. **Add four small unit tests.** Test valid/malformed UDP packet parsing, processor-tag filtering, stale-data behavior, and distance-to-RPM interpolation. These are independent of robot hardware and give new programmers a safe place to learn testing.
3. **Create a short hardware/configuration map.** Put CAN IDs, controller ports, camera/UDP source, and calibration ownership in clearly named, focused constants. Keep CTRE-generated swerve data generated; do not hand-copy it.
4. **Make vision use explicitly safe.** Keep vision as driver-assist aim and RPM until it is characterized. Show tag freshness, chosen tag ID, yaw, raw/scaled distance, and a clear “vision valid” flag. Confirm coordinate units and camera transform with measured field tests.
5. **Turn the five autonomous choices into a repeatable checklist.** For each routine, document start pose, expected duration, required mechanisms, abort/Do Nothing behavior, and how it is tested. Replace the left-auto guessed intake timing with event markers or a verified subsystem condition when practical.
6. **Clean the container boundary.** Minimize public statics, choose one Pigeon owner, remove or quarantine obsolete commented code when the team is ready, and keep button bindings in one clearly named method. This is a maintainability improvement, not a rewrite.
7. **Only then consider a small coordinator.** If shot preparation consistently requires “spin up + aim + feeder only when ready,” add one focused `PrepareShot`/`ShootWhenReady` command or a small state enum. Do not begin with a full 1678/2910-style superstructure.

## License and attribution notes

This document contains analysis and names of patterns only; it does not reproduce or transplant reference-team source code. Before using any nontrivial source, assets, libraries, or derived code from the reference projects, Team 10252 should read the applicable repository and dependency licenses, retain required notices, and obtain mentor/legal guidance if license compatibility is uncertain.

In particular, 2910's snapshot includes a project MIT license but also separately carries a GPLv3 AdvantageKit license file and third-party notices; 6328's project is MIT; 1678's snapshot exposes a WPILib BSD-style license file without a distinct project-code license. The safe learning path is to implement the general practices above independently, with notes such as “inspired by public FRC team practice,” rather than copying implementation details.

## Practical conclusion

Team 10252 does not need to become 1678, 2910, or 6328. The best next architecture is a reliable small command-based project with clear motor ownership, measured calibration, visible telemetry, a few tests, and autos that can be explained and repeated. Those practices scale later if the team gains hardware, time, and experienced maintainers; advanced shared libraries and autonomous/vision stacks can wait until there is a concrete need and a test plan to support them.
