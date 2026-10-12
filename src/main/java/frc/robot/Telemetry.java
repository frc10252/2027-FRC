package frc.robot;

import com.ctre.phoenix6.SignalLogger;
import com.ctre.phoenix6.swerve.SwerveDrivetrain.SwerveDriveState;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.networktables.BooleanPublisher;
import edu.wpi.first.networktables.DoubleArrayPublisher;
import edu.wpi.first.networktables.DoublePublisher;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.networktables.StringPublisher;
import edu.wpi.first.networktables.StructArrayPublisher;
import edu.wpi.first.networktables.StructPublisher;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.RobotController;
import edu.wpi.first.wpilibj.smartdashboard.Mechanism2d;
import edu.wpi.first.wpilibj.smartdashboard.MechanismLigament2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj.util.Color;
import edu.wpi.first.wpilibj.util.Color8Bit;

/**
 * Publishes drivetrain state to NetworkTables (for Elastic / AdvantageScope) and to the
 * CTRE SignalLogger. The SmartDashboard keys below match the team's Elastic layout.
 */
public class Telemetry {
    private final double MaxSpeed;

    public Telemetry(double maxSpeed) {
        MaxSpeed = maxSpeed;
        SignalLogger.start();

        for (int i = 0; i < 4; ++i) {
            SmartDashboard.putData("Module " + i, m_moduleMechanisms[i]);
        }
    }

    private final NetworkTableInstance inst = NetworkTableInstance.getDefault();

    /* Robot swerve drive state */
    private final NetworkTable driveStateTable = inst.getTable("DriveState");
    private final StructPublisher<Pose2d> drivePose = driveStateTable.getStructTopic("Pose", Pose2d.struct).publish();
    private final StructPublisher<ChassisSpeeds> driveSpeeds = driveStateTable.getStructTopic("Speeds", ChassisSpeeds.struct).publish();
    private final StructArrayPublisher<SwerveModuleState> driveModuleStates = driveStateTable.getStructArrayTopic("ModuleStates", SwerveModuleState.struct).publish();
    private final StructArrayPublisher<SwerveModuleState> driveModuleTargets = driveStateTable.getStructArrayTopic("ModuleTargets", SwerveModuleState.struct).publish();
    private final StructArrayPublisher<SwerveModulePosition> driveModulePositions = driveStateTable.getStructArrayTopic("ModulePositions", SwerveModulePosition.struct).publish();
    private final DoublePublisher driveTimestamp = driveStateTable.getDoubleTopic("Timestamp").publish();
    private final DoublePublisher driveOdometryFrequency = driveStateTable.getDoubleTopic("OdometryFrequency").publish();

    /* Robot pose for the Elastic Field widget */
    private final NetworkTable table = inst.getTable("Pose");
    private final DoubleArrayPublisher fieldPub = table.getDoubleArrayTopic("Robot").publish();
    private final StringPublisher fieldTypePub = table.getStringTopic(".type").publish();

    /* Keys consumed by the team's Elastic layout. Published under the "SmartDashboard"
     * table so existing widgets keep working without the SmartDashboard put* helpers. */
    private final NetworkTable dashboardTable = inst.getTable("SmartDashboard");
    private final DoublePublisher headingPub = dashboardTable.getDoubleTopic("heading angle").publish();
    private final DoublePublisher matchTimePub = dashboardTable.getDoubleTopic("match time").publish();
    private final DoublePublisher batteryVoltagePub = dashboardTable.getDoubleTopic("battery voltage").publish();
    private final DoublePublisher xVelocityPub = dashboardTable.getDoubleTopic("x velocity").publish();
    private final DoublePublisher yVelocityPub = dashboardTable.getDoubleTopic("y velocity").publish();
    private final DoublePublisher omegaVelocityPub = dashboardTable.getDoubleTopic("omega velocity (rad/s)").publish();
    private final BooleanPublisher dsAttachedPub = dashboardTable.getBooleanTopic("ds attached").publish();
    private final BooleanPublisher fmsAttachedPub = dashboardTable.getBooleanTopic("fms attached").publish();
    private final StringPublisher eventNamePub = dashboardTable.getStringTopic("event name").publish();
    private final StringPublisher matchTypePub = dashboardTable.getStringTopic("match type").publish();
    private final DoublePublisher matchNumberPub = dashboardTable.getDoubleTopic("match number").publish();
    private final StringPublisher alliancePub = dashboardTable.getStringTopic("alliance").publish();
    private final DoublePublisher stationPub = dashboardTable.getDoubleTopic("station").publish();

    /* Mechanisms to represent the swerve module states */
    private final Mechanism2d[] m_moduleMechanisms = new Mechanism2d[] {
        new Mechanism2d(1, 1),
        new Mechanism2d(1, 1),
        new Mechanism2d(1, 1),
        new Mechanism2d(1, 1),
    };
    private final MechanismLigament2d[] m_moduleSpeeds = new MechanismLigament2d[] {
        m_moduleMechanisms[0].getRoot("RootSpeed", 0.5, 0.5).append(new MechanismLigament2d("Speed", 0.5, 0)),
        m_moduleMechanisms[1].getRoot("RootSpeed", 0.5, 0.5).append(new MechanismLigament2d("Speed", 0.5, 0)),
        m_moduleMechanisms[2].getRoot("RootSpeed", 0.5, 0.5).append(new MechanismLigament2d("Speed", 0.5, 0)),
        m_moduleMechanisms[3].getRoot("RootSpeed", 0.5, 0.5).append(new MechanismLigament2d("Speed", 0.5, 0)),
    };
    private final MechanismLigament2d[] m_moduleDirections = new MechanismLigament2d[] {
        m_moduleMechanisms[0].getRoot("RootDirection", 0.5, 0.5)
            .append(new MechanismLigament2d("Direction", 0.1, 0, 0, new Color8Bit(Color.kWhite))),
        m_moduleMechanisms[1].getRoot("RootDirection", 0.5, 0.5)
            .append(new MechanismLigament2d("Direction", 0.1, 0, 0, new Color8Bit(Color.kWhite))),
        m_moduleMechanisms[2].getRoot("RootDirection", 0.5, 0.5)
            .append(new MechanismLigament2d("Direction", 0.1, 0, 0, new Color8Bit(Color.kWhite))),
        m_moduleMechanisms[3].getRoot("RootDirection", 0.5, 0.5)
            .append(new MechanismLigament2d("Direction", 0.1, 0, 0, new Color8Bit(Color.kWhite))),
    };

    private final double[] m_poseArray = new double[3];
    private final double[] m_moduleStatesArray = new double[8];
    private final double[] m_moduleTargetsArray = new double[8];

    /** Accept the swerve drive state and telemeterize it to NetworkTables and SignalLogger. */
    public void telemeterize(SwerveDriveState state) {
        Pose2d pose = RobotContainer.driveSubsystem.getPose();
        drivePose.set(pose);
        driveSpeeds.set(state.Speeds);
        driveModuleStates.set(state.ModuleStates);
        driveModuleTargets.set(state.ModuleTargets);
        driveModulePositions.set(state.ModulePositions);
        driveTimestamp.set(state.Timestamp);
        driveOdometryFrequency.set(1.0 / state.OdometryPeriod);

        m_poseArray[0] = pose.getX();
        m_poseArray[1] = pose.getY();
        m_poseArray[2] = pose.getRotation().getDegrees();
        for (int i = 0; i < 4; ++i) {
            m_moduleStatesArray[i*2 + 0] = state.ModuleStates[i].angle.getRadians();
            m_moduleStatesArray[i*2 + 1] = state.ModuleStates[i].speedMetersPerSecond;
            m_moduleTargetsArray[i*2 + 0] = state.ModuleTargets[i].angle.getRadians();
            m_moduleTargetsArray[i*2 + 1] = state.ModuleTargets[i].speedMetersPerSecond;
        }

        /* Keys consumed by the team's Elastic layout */
        headingPub.set(pose.getRotation().getDegrees());
        matchTimePub.set(Math.max(0.0, DriverStation.getMatchTime()));
        batteryVoltagePub.set(RobotController.getBatteryVoltage());
        xVelocityPub.set(state.Speeds.vxMetersPerSecond);
        yVelocityPub.set(state.Speeds.vyMetersPerSecond);
        omegaVelocityPub.set(state.Speeds.omegaRadiansPerSecond);
        dsAttachedPub.set(DriverStation.isDSAttached());
        fmsAttachedPub.set(DriverStation.isFMSAttached());
        eventNamePub.set(DriverStation.getEventName());
        matchTypePub.set(DriverStation.getMatchType().toString());
        matchNumberPub.set(DriverStation.getMatchNumber());
        var alliance = DriverStation.getAlliance();
        alliancePub.set(alliance.isPresent() ? alliance.get().toString() : "Unknown");
        stationPub.set(DriverStation.getLocation().orElse(0));

        SignalLogger.writeDoubleArray("DriveState/Pose", m_poseArray);
        SignalLogger.writeDoubleArray("DriveState/ModuleStates", m_moduleStatesArray);
        SignalLogger.writeDoubleArray("DriveState/ModuleTargets", m_moduleTargetsArray);
        SignalLogger.writeDouble("DriveState/OdometryPeriod", state.OdometryPeriod, "seconds");

        fieldTypePub.set("Field2d");
        fieldPub.set(m_poseArray);

        for (int i = 0; i < 4; ++i) {
            m_moduleSpeeds[i].setAngle(state.ModuleStates[i].angle);
            m_moduleDirections[i].setAngle(state.ModuleStates[i].angle);
            m_moduleSpeeds[i].setLength(state.ModuleStates[i].speedMetersPerSecond / (2 * MaxSpeed));
        }
    }
}
