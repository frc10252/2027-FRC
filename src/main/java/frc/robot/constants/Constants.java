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

    /** Controller ports. */
    public static final int driverControllerPort = 0;
    public static final int operatorControllerPort = 1;
}
