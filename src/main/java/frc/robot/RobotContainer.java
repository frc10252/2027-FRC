// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import static edu.wpi.first.units.Units.*;

import com.ctre.phoenix6.hardware.Pigeon2;
import com.ctre.phoenix6.swerve.SwerveRequest;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.RobotModeTriggers;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine.Direction;

import frc.robot.constants.Constants;
import frc.robot.constants.TunerConstants;
import frc.robot.subsystems.CommandSwerveDrivetrain;
import frc.robot.subsystems.Drive;

public class RobotContainer {
    public double MaxSpeed = TunerConstants.kSpeedAt12Volts.in(MetersPerSecond);

    private final SwerveRequest.SwerveDriveBrake brake = new SwerveRequest.SwerveDriveBrake();
    private final SwerveRequest.PointWheelsAt point = new SwerveRequest.PointWheelsAt();

    public static Drive driveSubsystem;
    // 2027 mechanism subsystems go here.

    public static final Pigeon2 imu = new Pigeon2(Constants.pigeonID);

    private final Telemetry logger = new Telemetry(MaxSpeed);
    private final SendableChooser<Command> autoChooser = new SendableChooser<>();

    public static final CommandXboxController joystick = new CommandXboxController(Constants.driverControllerPort);
    public static final CommandXboxController coJoystick = new CommandXboxController(Constants.operatorControllerPort);

    public final CommandSwerveDrivetrain drivetrain = TunerConstants.createDrivetrain();

    public RobotContainer() {
        driveSubsystem = new Drive(drivetrain, joystick);
        configureBindings();
        configureAutoChooser();
    }

    private void configureBindings() {
        // Note that X is defined as forward according to WPILib convention,
        // and Y is defined as to the left according to WPILib convention.
        driveSubsystem.useDefaultCommand();

        // Idle while the robot is disabled. This ensures the configured
        // neutral mode is applied to the drive motors while disabled.
        final var idle = new SwerveRequest.Idle();
        RobotModeTriggers.disabled().whileTrue(
            drivetrain.applyRequest(() -> idle).ignoringDisable(true)
        );

        // Driver: A = brake (X-lock), B = point wheels at left-stick direction
        joystick.a().whileTrue(drivetrain.applyRequest(() -> brake));
        joystick.b().whileTrue(drivetrain.applyRequest(() ->
            point.withModuleDirection(new Rotation2d(-joystick.getLeftY(), -joystick.getLeftX()))
        ));

        // Driver: X = demo on-the-fly path (1 m forward, turn 90 deg), Y = zero odometry
        joystick.x().onTrue(new InstantCommand(
            () -> driveSubsystem.pathRelative(1, 0, Math.toRadians(90)).schedule(), driveSubsystem));
        joystick.y().onTrue(new InstantCommand(
            () -> driveSubsystem.resetPose(new Pose2d()), driveSubsystem));

        // Driver: left bumper = reset field-centric heading
        joystick.leftBumper().onTrue(
            drivetrain.runOnce(() -> drivetrain.seedFieldCentric())
                .andThen(new InstantCommand(() -> driveSubsystem.resetTargetAngle(0)))
        );

        // Run SysId routines when holding back/start and X/Y.
        // Note that each routine should be run exactly once in a single log.
        joystick.back().and(joystick.y()).whileTrue(drivetrain.sysIdDynamic(Direction.kForward));
        joystick.back().and(joystick.x()).whileTrue(drivetrain.sysIdDynamic(Direction.kReverse));
        joystick.start().and(joystick.y()).whileTrue(drivetrain.sysIdQuasistatic(Direction.kForward));
        joystick.start().and(joystick.x()).whileTrue(drivetrain.sysIdQuasistatic(Direction.kReverse));

        drivetrain.registerTelemetry(logger::telemeterize);
    }

    public Command getAutonomousCommand() {
        return autoChooser.getSelected();
    }

    private void configureAutoChooser() {
        AutonomousCommands autos = new AutonomousCommands(drivetrain);
        autoChooser.setDefaultOption("Do Nothing", Commands.none());
        // Example once a path exists in src/main/deploy/pathplanner/paths:
        // autoChooser.addOption("Leave", autos.followPathAuto("leave"));
        SmartDashboard.putData("Auto Chooser", autoChooser);
    }
}
