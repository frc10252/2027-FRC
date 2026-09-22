// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.path.PathPlannerPath;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;

import frc.robot.subsystems.CommandSwerveDrivetrain;

/**
 * Builds the autonomous commands shown in the auto chooser.
 *
 * Add one {@code buildXxxAuto()} method per routine once 2027 paths exist in
 * {@code src/main/deploy/pathplanner/paths}, then register it in
 * {@link RobotContainer#configureAutoChooser()}.
 */
public class AutonomousCommands {
    private final CommandSwerveDrivetrain drivetrain;

    public AutonomousCommands(CommandSwerveDrivetrain drivetrain) {
        this.drivetrain = drivetrain;
    }

    /**
     * Reset odometry to the path's starting pose, then follow the named PathPlanner path.
     * Returns a no-op command (and reports to the Driver Station) if the path fails to load.
     */
    public Command followPathAuto(String pathName) {
        try {
            PathPlannerPath path = PathPlannerPath.fromPathFile(pathName);
            return Commands.sequence(
                Commands.runOnce(() ->
                    drivetrain.resetPose(path.getStartingHolonomicPose().orElse(new Pose2d()))
                ),
                AutoBuilder.followPath(path).deadlineFor(drivetrain.run(() -> {}))
            );
        } catch (Exception e) {
            DriverStation.reportError("Failed to load path '" + pathName + "': " + e.getMessage(), e.getStackTrace());
            return Commands.none();
        }
    }
}
