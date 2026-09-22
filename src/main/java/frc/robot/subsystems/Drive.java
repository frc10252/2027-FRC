package frc.robot.subsystems;

import java.util.List;

import com.ctre.phoenix6.swerve.SwerveRequest;
import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.config.PIDConstants;
import com.pathplanner.lib.config.RobotConfig;
import com.pathplanner.lib.controllers.PPHolonomicDriveController;
import com.pathplanner.lib.path.GoalEndState;
import com.pathplanner.lib.path.PathConstraints;
import com.pathplanner.lib.path.PathPlannerPath;
import com.pathplanner.lib.path.Waypoint;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.constants.Constants;

/**
 * Wraps the CTRE {@link CommandSwerveDrivetrain} with team-specific teleop behavior
 * (heading-hold field-centric drive) and PathPlanner integration.
 */
public class Drive extends SubsystemBase {
    private final SwerveRequest.FieldCentricFacingAngle drive = new SwerveRequest.FieldCentricFacingAngle();
    private final SwerveRequest.ApplyRobotSpeeds autoDrive = new SwerveRequest.ApplyRobotSpeeds();

    private final CommandSwerveDrivetrain drivetrain;
    private final CommandXboxController joystick;
    private final double MaxSpeed = Constants.MaxSpeed;
    private final double scaling = Constants.scaling;
    private Command lastPath;

    /** Field-relative heading (degrees) the drivetrain tries to hold. */
    public double targetAngle;

    /** Heading PID gains. Swapped at runtime depending on whether the driver is rotating. */
    public static double kP;
    public static double kI;
    public static double kD;

    public Drive(CommandSwerveDrivetrain drivetrain, CommandXboxController joystick) {
        this.drivetrain = drivetrain;
        this.joystick = joystick;
        targetAngle = 0;
        kP = 3;
        kI = 0.0;
        kD = 0.5;

        try {
            RobotConfig config = RobotConfig.fromGUISettings();
            AutoBuilder.configure(
                this::getPose,
                this::resetPose,
                this::getRobotRelativeSpeeds,
                (speeds, feedforwards) -> driveRobotRelative(speeds),
                new PPHolonomicDriveController(
                        new PIDConstants(7.0, 0.0001, 0.01),   // translation
                        new PIDConstants(5.0, 0.0001, 0.045)   // rotation
                ),
                config,
                () -> DriverStation.getAlliance().isPresent()
                        && DriverStation.getAlliance().get() == DriverStation.Alliance.Red,
                this
            );
        } catch (Exception e) {
            DriverStation.reportError("Failed to load PathPlanner config and configure AutoBuilder", e.getStackTrace());
        }
    }

    @Override
    public void periodic() {
        // While the driver is pushing the right stick, integrate it into the target heading
        // and use aggressive gains. Otherwise relax the heading loop so the robot coasts.
        if (Math.abs(joystick.getRightX()) >= 0.08) {
            targetAngle += -joystick.getRightX() * 4;
            kP = 6;
            kI = 0.0001;
            kD = 0.15;
        } else {
            kP = 0.0;
            kI = 0.0;
            kD = 0.0;
        }
    }

    /** Field-centric drive with heading hold. X is forward, Y is left (WPILib convention). */
    public Command getDefaultCommand() {
        return drivetrain.applyRequest(() ->
            drive.withVelocityX(-joystick.getLeftY() * MaxSpeed * scaling)
                 .withVelocityY(-joystick.getLeftX() * MaxSpeed * scaling)
                 .withTargetDirection(new Rotation2d(Math.toRadians(targetAngle)))
                 .withHeadingPID(kP, kI, kD)
        );
    }

    public void useDefaultCommand() {
        drivetrain.setDefaultCommand(getDefaultCommand());
    }

    public void resetTargetAngle(double angle) {
        targetAngle = angle;
    }

    public void resetFacingAngle() {
        targetAngle = Constants.imu.getYaw().getValueAsDouble();
    }

    public Pose2d getPose() {
        return drivetrain.getState().Pose;
    }

    public void resetPose(Pose2d pose) {
        drivetrain.resetPose(pose);
    }

    public ChassisSpeeds getRobotRelativeSpeeds() {
        return drivetrain.getState().Speeds;
    }

    public void driveRobotRelative(ChassisSpeeds speeds) {
        drivetrain.setControl(autoDrive.withSpeeds(speeds));
    }

    public void cancelLastPath() {
        resetFacingAngle();
        if (lastPath != null) lastPath.cancel();
    }

    /** Build and follow an on-the-fly path from the current pose to a field pose. */
    public Command driveToPose(Pose2d endPose) {
        Pose2d startPose = getPose();
        List<Waypoint> waypoints = PathPlannerPath.waypointsFromPoses(startPose, endPose);
        Rotation2d endHeading = endPose.getRotation();

        PathPlannerPath path = new PathPlannerPath(
            waypoints,
            new PathConstraints(0.5, 0.5, Units.degreesToRadians(0), Units.degreesToRadians(0)),
            null,
            new GoalEndState(0.0, endHeading)
        );
        path.preventFlipping = true;

        cancelLastPath();
        lastPath = AutoBuilder.followPath(path);
        return lastPath;
    }

    /** Build and follow an on-the-fly path to a pose given relative to the robot. */
    public Command pathRelative(double targetX, double targetY, double targetRotation) {
        Pose2d currentPose = getPose();

        Translation2d localOffset = new Translation2d(targetX, targetY);
        Translation2d fieldOffset = localOffset.rotateBy(currentPose.getRotation());
        Rotation2d endHeading = currentPose.getRotation().plus(new Rotation2d(targetRotation));
        Pose2d endPose = new Pose2d(currentPose.getTranslation().plus(fieldOffset), endHeading);

        List<Waypoint> waypoints = PathPlannerPath.waypointsFromPoses(currentPose, endPose);
        PathPlannerPath path = new PathPlannerPath(
            waypoints,
            new PathConstraints(0.5, 0.5, Units.degreesToRadians(360), Units.degreesToRadians(90)),
            null,
            new GoalEndState(0.0, endHeading)
        );
        path.preventFlipping = true;

        cancelLastPath();
        lastPath = AutoBuilder.followPath(path);
        return lastPath;
    }
}
