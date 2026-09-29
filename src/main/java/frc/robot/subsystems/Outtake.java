package frc.robot.subsystems;

import com.ctre.phoenix6.hardware.TalonFX;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.RunCommand;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.constants.Constants;

public class Outtake extends SubsystemBase {
    private final CommandXboxController joystick;

    private final TalonFX motor1 = new TalonFX(Constants.outtakeMotor1ID);
    private final TalonFX motor2 = new TalonFX(Constants.outtakeMotor2ID);

    public Outtake(CommandXboxController joystick) {
        this.joystick = joystick;
        setDefaultCommand(joystickCommand());
    }

    /** Motor 1 spins one direction, motor 2 spins the opposite direction. */
    public void setPower(double power) {
        motor1.set(power);
        motor2.set(-power);
    }

    /** Right stick Y drives the outtake while no other command is using it. */
    private Command joystickCommand() {
        return new RunCommand(() -> {
            double input = joystick.getRightY();
            if (Math.abs(input) < Constants.outtakeJoystickDeadband) input = 0.0;
            setPower(input * Constants.outtakeMaxPower);
        }, this);
    }
}
