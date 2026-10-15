package frc.robot.subsystems;

import com.ctre.phoenix6.hardware.TalonFX;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.constants.Constants;

public class Outtake extends SubsystemBase {
    private static final double MANUAL_SHOOTER_SCALE = 0.85;

    private final CommandXboxController joystick;
    private final TalonFX insideShooterMotor;
    private final TalonFX outsideShooterMotor;
    private final SparkMax feederMotor;

    private double currentRpm = 0.0;
    private double shooterPower = 0.0;

    public Outtake(CommandXboxController joystick) {
        this.joystick = joystick;
        insideShooterMotor = new TalonFX(Constants.insideShooterMotorID);
        outsideShooterMotor = new TalonFX(Constants.outsideShooterMotorID);
        feederMotor = new SparkMax(Constants.shooterFeederMotorID, MotorType.kBrushless);
    }

    /** Manual shooter control for use as this subsystem's default command. */
    public Command manualShooterCommand() {
        return runEnd(
            () -> {
                double input = joystick.getRightY();
                if (Math.abs(input) < Constants.shooterJoystickDeadband) {
                    input = 0.0;
                }

                insideShooterMotor.set(input * MANUAL_SHOOTER_SCALE);
                outsideShooterMotor.set(-input * MANUAL_SHOOTER_SCALE);
            },
            this::stopShooter
        );
    }

    /** Run the feeder while scheduled and stop it when the command ends. */
    public Command runFeederMotors(double power) {
        return startEnd(
            () -> setFeederPower(power),
            () -> setFeederPower(0.0)
        );
    }

    public double getCurrentRpm() {
        return currentRpm;
    }

    public double getShooterPower() {
        return shooterPower;
    }

    /** True only when both shooter wheels are measurably spinning. */
    public boolean areShooterMotorsRunning() {
        double insideRps = Math.abs(insideShooterMotor.getVelocity().getValueAsDouble());
        double outsideRps = Math.abs(outsideShooterMotor.getVelocity().getValueAsDouble());
        return insideRps > Constants.shooterRunningThresholdRps
            && outsideRps > Constants.shooterRunningThresholdRps;
    }

    private void setFeederPower(double power) {
        feederMotor.set(-power);
    }

    private void stopShooter() {
        insideShooterMotor.stopMotor();
        outsideShooterMotor.stopMotor();
    }

    /** Read shooter status for telemetry without writing competing motor outputs. */
    @Override
    public void periodic() {
        currentRpm = insideShooterMotor.getVelocity().getValueAsDouble() * 60.0;
        shooterPower = insideShooterMotor.getDutyCycle().getValueAsDouble();
    }
}
