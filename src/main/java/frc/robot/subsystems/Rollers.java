package frc.robot.subsystems;

import java.util.function.BooleanSupplier;

import com.ctre.phoenix6.hardware.TalonFX;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.constants.Constants;

public class Rollers extends SubsystemBase {
    private final TalonFX rollerMotor;
    private final BooleanSupplier shooterRunning;

    public Rollers(int rollerCanId, BooleanSupplier shooterRunning) {
        rollerMotor = new TalonFX(rollerCanId);
        this.shooterRunning = shooterRunning;
    }

    /**
     * Runs slowly while enabled and speeds up after both shooter motors start moving.
     * The roller stops if this command is interrupted or the robot is disabled.
     */
    public Command automaticRollerCommand() {
        return runEnd(
            () -> rollerMotor.set(
                shooterRunning.getAsBoolean()
                    ? Constants.rollerHighSpeed
                    : Constants.rollerLowSpeed
            ),
            rollerMotor::stopMotor
        );
    }
}
