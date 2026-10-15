package frc.robot.subsystems;

import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.NeutralModeValue;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

import frc.robot.constants.Constants;

public class Intake extends SubsystemBase {
    private final TalonFX intakeMotor;
    private final TalonFX rightExtensionMotor;
    private final TalonFX leftExtensionMotor;

    private boolean extensionCompleted = false;

    public Intake() {
        intakeMotor = new TalonFX(Constants.intakeMotorID);
        rightExtensionMotor = new TalonFX(Constants.intakeExtensionRightMotorID);
        leftExtensionMotor = new TalonFX(Constants.intakeExtensionLeftMotorID);

        rightExtensionMotor.setNeutralMode(NeutralModeValue.Brake);
        leftExtensionMotor.setNeutralMode(NeutralModeValue.Brake);

        // The intake must be fully retracted whenever the robot boots because
        // the integrated motor encoders only provide relative position.
        rightExtensionMotor.setPosition(0.0);
        leftExtensionMotor.setPosition(0.0);
    }

    /** Run the intake roller while scheduled and stop it when the command ends. */
    public Command runIntakeCommand(double power) {
        return startEnd(
            () -> intakeMotor.set(power),
            intakeMotor::stopMotor
        );
    }

    /**
     * Extend the intake one time using the TalonFX integrated encoders.
     *
     * <p>The two motors turn in opposite directions. Each side stops independently
     * when it reaches the target, and the command times out if either side stalls.
     */
    public Command extendOnceCommand() {
        return runEnd(
            this::runExtensionTowardTarget,
            this::stopExtensionMotors
        )
            .until(this::extensionFinished)
            .withTimeout(Constants.intakeExtensionTimeoutSeconds)
            .finallyDo(interrupted -> {
                stopExtensionMotors();
                if (extensionAtTarget()) {
                    extensionCompleted = true;
                }
            });
    }

    private void runExtensionTowardTarget() {
        if (extensionCompleted) {
            stopExtensionMotors();
            return;
        }

        double target = Constants.intakeExtensionRotations;
        double tolerance = Constants.intakeExtensionToleranceRotations;

        if (Math.abs(getRightExtensionRotations()) < target - tolerance) {
            rightExtensionMotor.set(Constants.intakeExtensionPower);
        } else {
            rightExtensionMotor.stopMotor();
        }

        if (Math.abs(getLeftExtensionRotations()) < target - tolerance) {
            leftExtensionMotor.set(-Constants.intakeExtensionPower);
        } else {
            leftExtensionMotor.stopMotor();
        }
    }

    private boolean extensionFinished() {
        return extensionCompleted || extensionAtTarget();
    }

    private boolean extensionAtTarget() {
        double minimumPosition = Constants.intakeExtensionRotations
            - Constants.intakeExtensionToleranceRotations;
        return Math.abs(getRightExtensionRotations()) >= minimumPosition
            && Math.abs(getLeftExtensionRotations()) >= minimumPosition;
    }

    private double getRightExtensionRotations() {
        return rightExtensionMotor.getPosition().getValueAsDouble();
    }

    private double getLeftExtensionRotations() {
        return leftExtensionMotor.getPosition().getValueAsDouble();
    }

    private void stopExtensionMotors() {
        rightExtensionMotor.stopMotor();
        leftExtensionMotor.stopMotor();
    }

    @Override
    public void periodic() {
        SmartDashboard.putNumber(
            "Intake Extension Right Rotations",
            getRightExtensionRotations()
        );
        SmartDashboard.putNumber(
            "Intake Extension Left Rotations",
            getLeftExtensionRotations()
        );
        SmartDashboard.putBoolean("Intake Extension Complete", extensionCompleted);
    }
}
