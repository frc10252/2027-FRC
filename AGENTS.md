# Team 10252 Robot Code Guide

## Project intent

This is a small 2026 FRC Java robot project. Keep it understandable to students
and maintainers: prefer clear WPILib command-based code over copied frameworks or
premature abstractions. Improve the current structure incrementally rather than
porting architecture from another team.

The codebase uses Java 17, WPILib/GradleRIO, CTRE Phoenix 6 hardware, REV
SparkMax controllers, and PathPlanner. The robot is team 10252.

## Source map and ownership

- `Main.java` starts the WPILib runtime. Keep it minimal.
- `Robot.java` owns lifecycle behavior: run the scheduler, start the selected
  autonomous command, and cancel it in teleop. Do not place mechanism logic here.
- `RobotContainer.java` constructs active subsystems, configures controller
  bindings, configures the autonomous chooser, and starts shared services.
  Keep driver/operator bindings in this one location.
- `subsystems/` owns mechanism behavior and actuator output. `Drive` is the
  higher-level drive/path wrapper; `CommandSwerveDrivetrain` is the CTRE-backed
  swerve subsystem.
- `constants/TunerConstants.java` is CTRE Tuner X generated configuration. Do
  not hand-refactor it. Regenerate it through CTRE tooling when swerve hardware
  calibration changes.
- `constants/Constants.java` contains hand-maintained IDs and tuning. Keep new
  values grouped by purpose. Split it into small focused constants classes only
  when a group becomes large enough to be harder to navigate.
- `Telemetry.java` publishes drive, mechanism, and vision diagnostics.
- `UdpTelemetryReceiver.java` and `subsystems/AprilTags.java` implement the
  custom UDP vision protocol. They currently support aiming and shooter RPM;
  they do not provide verified drivetrain pose fusion.
- `src/main/deploy/pathplanner/` contains PathPlanner settings, navigation data,
  and deployed path files.

`Elevator.java` is currently fully commented out, and `Coral.java` is not
constructed. Treat both as inactive until a scoped task explicitly brings them
back into service.

## Hardware and calibration changes

- Before adding or changing hardware, identify the subsystem owner, CAN ID or
  controller port, control mode, units, calibration source, and safe stopped
  behavior. Keep that information near the relevant hand-maintained constants
  or subsystem; do not duplicate generated swerve configuration.
- Keep one clear source of truth for each sensor and motor configuration. Do
  not add another hardware handle simply for convenient access.
- Treat unmeasured constants, camera transforms, and shooter curves as tuning
  work that requires a safe on-robot check, not as values to guess in an
  unrelated software task.

## Design rules

1. One subsystem owns each actuator. Motor commands must have one active output
   owner at a time. Do not let `periodic()` and a command both write competing
   requests to the same motor.
2. Express driver actions as WPILib commands with correct subsystem
   requirements. Use `run`, `runOnce`, `startEnd`, `sequence`, and `parallel`
   when they plainly describe the action.
3. Keep `periodic()` focused on state updates, sensor reads, telemetry, and
   intentionally persistent control. If it writes motor outputs, document its
   interaction with every command that can control that motor.
4. Prefer a small named command or command factory before introducing a global
   state machine or a superstructure. Add a coordinator only after repeated
   mechanism interactions are demonstrably difficult to keep safe.
5. Avoid new public mutable statics and duplicate hardware objects. Preserve
   existing public/static APIs unless the task includes a safe migration.
6. Do not create another Pigeon2 for CAN ID 15. Before changing gyro ownership,
   identify all existing users in `Constants`, `RobotContainer`, and the CTRE
   drivetrain configuration.

## Drivetrain and autonomous work

- Retain the CTRE-generated swerve layout and module calibrations unless the
  change is explicitly about the physical drivetrain.
- Keep PathPlanner configured through the existing `Drive` subsystem. Paths and
  path-following commands must retain clear drivetrain requirements.
- Autonomous routines should reset to the path's declared start pose, have a
  safe fallback such as `Commands.none()`, and report a useful error if loading
  fails.
- Give each added autonomous routine a descriptive chooser name and document
  its expected start pose, expected duration, required mechanisms, and safe
  abort/Do Nothing behavior in code comments.
- Prefer a verified PathPlanner event marker or a subsystem condition to a new
  guessed delay for mechanism timing. Existing timing should not be changed
  incidentally while working on unrelated code.
- Test changes to paths, poses, field coordinates, or alliance behavior in
  simulation and on the robot before relying on them in a match.

## Vision and shooter work

- Treat UDP vision as driver assist until its camera pose, coordinate
  convention, timestamps, and uncertainty have been measured on the robot.
  Do not add drivetrain pose fusion merely because the CTRE API supports it.
- Before using a vision value for aiming or RPM, require a recent packet, a
  recognized processor tag, finite distance/yaw values, and any existing angle
  validity gate.
- For shooter changes, make manual, velocity, and stopped behavior explicit.
  Verify that a velocity request cannot be overwritten by a direct percent
  output in the same scheduler cycle.
- Add or maintain dashboard visibility of requested mode, target speed,
  measured speed, tag freshness, selected tag, distance, and yaw when
  modifying those systems.

## Code style

- Use Java 17 and the `frc.robot` package layout already in the project.
- Use four-space indentation in new or substantially edited code. Keep nearby
  formatting stable for small edits; do not make line-ending-only changes.
- Use descriptive PascalCase class names and camelCase methods/fields. Use
  `UPPER_SNAKE_CASE` for new immutable local constants.
- Keep comments short and useful: explain units, hardware assumptions, safety
  gates, calibration sources, or non-obvious command interactions.
- Do not add a large utility layer, I/O abstraction hierarchy, replay system,
  native co-processor, or external framework without an explicit project need,
  hardware plan, and test plan.

## Testing and verification

The repository currently has no test source tree. Put new hardware-independent
JUnit tests under `src/test/java/frc/robot` when changing pure logic. Start with
valid and malformed UDP parsing, processor-tag filtering, stale-data behavior,
angle math, and shooter interpolation. Do not require real CAN devices or a
robot connection for unit tests.

For source changes, run the narrowest useful checks first, then normally run:

```powershell
.\gradlew.bat test
.\gradlew.bat build
```

For simulation-relevant changes, use the GradleRIO Java simulation task (normally
`.\gradlew.bat simulateJava`) and confirm controller bindings, dashboards, and
the affected behavior. Do not deploy to a RoboRIO, flash device configuration,
or alter CAN IDs unless the user explicitly asks for that hardware action.

When reporting work, state the files changed, the checks run, and anything that
still requires on-robot validation.

## Scope discipline

- Preserve unrelated user changes in a dirty working tree.
- Do not remove inactive code or perform broad formatting as a side effect of a
  feature change. Make cleanup a separate, reviewable task.
- Use vendor libraries already declared by `vendordeps/` unless a new dependency
  is explicitly justified and approved.
- Implement ideas from public FRC examples independently. Do not copy another
  team's code or architecture wholesale; their hardware, licenses, and support
  capacity may not match this robot.
