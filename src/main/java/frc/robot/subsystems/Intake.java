package frc.robot.subsystems;

import com.ctre.phoenix6.configs.TalonFXConfiguration;
import com.ctre.phoenix6.controls.PositionVoltage;
import com.ctre.phoenix6.hardware.TalonFX;
import com.ctre.phoenix6.signals.GravityTypeValue;
import com.ctre.phoenix6.signals.NeutralModeValue;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;

import frc.robot.constants.Constants;

public class Intake extends SubsystemBase {
    private static final double INTAKE_STICK_SCALE = 0.60;
    private static final double JOYSTICK_DEADBAND  = 0.05;

    private final TalonFX intakeMotor;

    private final CommandXboxController joystick;

    // Auto override: when active, periodic() uses this power instead of joystick
    private boolean autoOverrideActive = false;
    private double autoOverridePower = 0.0;

    public Intake(CommandXboxController joystick) {
        this.joystick = joystick;
        intakeMotor = new TalonFX(Constants.intakeMotorID);
    }

    public Extend() {
        joystick.y();
    }

    /** Run the intake roller at a fixed power from an auto command, bypassing joystick. */
    public Command runIntakeAutoCommand(double power) {
        return Commands.startEnd(
            () -> { autoOverrideActive = true; autoOverridePower = power; },
            () -> { autoOverrideActive = false; autoOverridePower = 0.0; },
            this
        );
    }

    @Override
    public void periodic() {
        if (autoOverrideActive) {
            intakeMotor.set(autoOverridePower);
            return;
        }

        // Intake roller — right joystick Y
        double rightY = joystick.getRightY();
        if (Math.abs(rightY) > JOYSTICK_DEADBAND) {
            intakeMotor.set(-rightY * INTAKE_STICK_SCALE);
        } else {
            intakeMotor.set(0);
        }
    }
}
