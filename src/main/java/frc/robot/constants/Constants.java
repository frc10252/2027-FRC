package frc.robot.constants;

import static edu.wpi.first.units.Units.*;

import com.ctre.phoenix6.hardware.Pigeon2;

/**
 * Robot-wide constants.
 *
 * Only chassis-level values live here. Add mechanism constants (motor IDs, PID gains,
 * setpoints) as the 2027 robot is designed.
 */
public class Constants {
    /** Desired top speed, pulled from the Tuner X swerve generator output. */
    public static final double MaxSpeed = TunerConstants.kSpeedAt12Volts.in(MetersPerSecond);

    /** Joystick-to-speed multiplier. Raise toward 1.0 as drivers get comfortable. */
    public static final double scaling = 0.3;

    public static final int pigeonID = 15;
    public static final Pigeon2 imu = new Pigeon2(Constants.pigeonID);
    /** Mechanism CAN IDs. */
    public static final int intakeExtensionRightMotorID = 20;
    public static final int intakeExtensionLeftMotorID = 21;
    public static final int intakeMotorID = 23;
    public static final int shooterFeederMotorID = 24;
    public static final int insideShooterMotorID = 25;
    public static final int outsideShooterMotorID = 26;

    /** Intake-extension bring-up values; verify with the mechanism unloaded first. */
    public static final double intakeExtensionRotations = 1.0;
    public static final double intakeExtensionPower = 0.15;
    public static final double intakeExtensionToleranceRotations = 0.05;
    public static final double intakeExtensionTimeoutSeconds = 1.0;

    /** Controller ports. */
    public static final int driverControllerPort = 0;
    public static final int operatorControllerPort = 1;

    public static final double DRIVER_DEADBAND = 0.05;
    public static final double MAX_ANGULAR_RATE =
        RotationsPerSecond.of(0.75).in(RadiansPerSecond);

    public static final double TRANSLATION_ACCELERATION_LIMIT = 3.0;
    public static final double ROTATION_ACCELERATION_LIMIT = 2.0 * Math.PI;

    // Roller (CAN ID still needs to be assigned; 26 is now the outside shooter.)
    public static final double rollerLowSpeed = 0.1;
    public static final double rollerHighSpeed = 0.5;
    /** Ignores tiny velocity readings when deciding whether the shooter is moving. */
    public static final double shooterRunningThresholdRps = 1.0;

    public static final double shooterJoystickDeadband = 0.05;
}
