package frc.robot.subsystems;

import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.VelocityVoltage;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.InvertedValue;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;

import java.util.function.DoubleSupplier;
x
import edu.wpi.first.math.interpolation.InterpolatingDoubleTreeMap;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.RunCommand;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.Tunable;
import frc.robot.UdpTelemetryReceiver;
import frc.robot.constants.Constants;

public class Outtake extends SubsystemBase {
    private final CommandXboxController joystick;


    // Distance (meters, after scale+bias applied) → target RPM, one curve per motor.
    // Replace placeholder points with real measured values; interpolation fills in between.
    private static final InterpolatingDoubleTreeMap BLUE_RPM_CURVE  = new InterpolatingDoubleTreeMap();
    private static final InterpolatingDoubleTreeMap GREEN_RPM_CURVE = new InterpolatingDoubleTreeMap();
    static {
        BLUE_RPM_CURVE.put(1.0, 1500.0);
        BLUE_RPM_CURVE.put(2.0, 2500.0);
        BLUE_RPM_CURVE.put(3.0, 3500.0);
        BLUE_RPM_CURVE.put(4.0, 4500.0);

        GREEN_RPM_CURVE.put(1.0, 1500.0);
        GREEN_RPM_CURVE.put(2.0, 2500.0);
        GREEN_RPM_CURVE.put(3.0, 3500.0);
        GREEN_RPM_CURVE.put(4.0, 4500.0);
    }

    public static double targetRpm      = 0.0;
    public static double targetGreenRpm = 0.0;
    public static double currentRpm     = 0.0;
    public static double shooterPower   = 0.0;

    //Roller Toggle
    public static boolean boostToggled = false;

    // Live-tunable from the dashboard (Test mode → Tune). Defaults come from Constants.
    public static final DoubleSupplier distanceScale = Tunable.number("shooter distance scale", Constants.shooterDistanceScale);
    public static final DoubleSupplier distanceBias  = Tunable.number("shooter distance bias (m)", Constants.shooterDistanceBias);

    public static final TalonFX blueMotor  = new TalonFX(Constants.blueShooterMotorID);
    public static final TalonFX blueMotor2 = new TalonFX(Constants.blue2ShooterMotorID);
    public static final TalonFX rollerMotor = new TalonFX(Constants.rollerCanID);

    // Feeders use simple power control — no velocity loop needed.
    public static final SparkMax feeder1 = new SparkMax(Constants.shooterFeederMotor1ID, MotorType.kBrushless);
    // Reusable control request — mutated each loop via withVelocity().
    private final VelocityVoltage velocityRequest = new VelocityVoltage(0).withSlot(0);

    public Outtake(CommandXboxController joystick) {
        this.joystick = joystick;

        TalonFXConfiguration blueConfig = new TalonFXConfiguration();
        blueConfig.Slot0.kP = Constants.shooterKp;
        blueConfig.Slot0.kI = Constants.shooterKi;
        blueConfig.Slot0.kD = Constants.shooterKd;
        blueConfig.Slot0.kV = Constants.shooterKv;
        blueMotor.getConfigurator().apply(blueConfig);

        // Green spins opposite to blue so both wheels propel the game piece the same way.
        // Same PID gains, but Clockwise_Positive so a positive RPS target spins it the other direction.
        TalonFXConfiguration greenConfig = new TalonFXConfiguration();
        greenConfig.Slot0.kP = Constants.shooterKp;
        greenConfig.Slot0.kI = Constants.shooterKi;
        greenConfig.Slot0.kD = Constants.shooterKd;
        greenConfig.Slot0.kV = Constants.shooterKv;
        // greenConfig.MotorOutput.Inverted = InvertedValue.Clockwise_Positive;
        blueMotor2.getConfigurator().apply(greenConfig);
    }

    /** Send each flywheel motor to its own velocity target. Updates telemetry fields. */
    private void setShooterRps(double blueRps, double greenRps) {
        blueMotor.setControl(velocityRequest.withVelocity(blueRps));
        blueMotor2.setControl(velocityRequest.withVelocity(greenRps));
        targetRpm      = blueRps  * 60.0;
        targetGreenRpm = greenRps * 60.0;
    }

    public static void setFeederPower(double power) {
        power *= -1;
        feeder1.set(power);
    }

    public Command runFeederMotors(double power){
        return new RunCommand(()->{
            setFeederPower(power);
        }, this);
    }

    /** Manual joystick control — active whenever no other command requires this subsystem. */
    public Command getDefaultCommand() {
        return new RunCommand(() -> {
            double input = joystick.getRightY();
            if (Math.abs(input) < Constants.shooterJoystickDeadband) input = 0.0;
            // Velocity-based default (disabled).
            // double rps = input * Constants.shooterMaxRps;
            // setShooterRps(rps, rps);
            double power = input * 0.35;
            blueMotor.set(power);
            blueMotor2.set(power);
        }, this);
    }

    // public Command runShooterCommand(double power){
    //     return new RunCommand(()->{
    //         blueMotor.set(power);
    //         greenMotor.set(power);
    //     }, this);
    // }

    /**
     * Sets RPM from the AprilTag distance curve.
     * Designed to be bound to a button with whileTrue() — default command resumes on release.
     */
    public Command autoRpmFromDistanceCommand() {
        return new RunCommand(() -> {
            double dist = UdpTelemetryReceiver.nearestProcessorDistMeters
                        * distanceScale.getAsDouble() + distanceBias.getAsDouble();
            double blueRpm  = Double.isFinite(dist) ? BLUE_RPM_CURVE.get(dist)  : 0.0;
            double greenRpm = Double.isFinite(dist) ? GREEN_RPM_CURVE.get(dist) : 0.0;
            setShooterRps(blueRpm / 60.0, greenRpm / 60.0);
        }, this);
    }

    /** Always runs — only reads sensors and updates telemetry fields, never commands motors. */
    @Override
    public void periodic() {
        currentRpm   = blueMotor.getVelocity().getValueAsDouble() * 60.0;
        shooterPower = blueMotor.getDutyCycle().getValueAsDouble();
        blueMotor.set(0.85*(joystick.getRightY()));
        blueMotor2.set(-0.85*(joystick.getRightY()));
    }
}
