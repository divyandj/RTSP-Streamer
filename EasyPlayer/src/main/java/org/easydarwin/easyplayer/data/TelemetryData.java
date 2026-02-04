package org.easydarwin.easyplayer.data;

/**
 * Holds the snapshot of drone state for the Tablet UI.
 */
public class TelemetryData {

    // --- Connectivity ---
    public boolean connected = false;

    // --- Navigation (GPS) ---
    public double latitude = 0;
    public double longitude = 0;
    public double homeLatitude = 0;
    public double homeLongitude = 0;
    public float altitude = 0f;    // Relative altitude in meters
    public int satelliteCount = 0;

    // --- Orientation ---
    public float yaw = 0f;   // Heading (0-360)
    public float pitch = 0f; // Kept for horizon reference
    public float roll = 0f;  // Kept for horizon reference

    // --- Power ---
    public float batteryVoltage = 0f; // Total Voltage (e.g., 22.4V)
    public float cellVoltage = 0f;    // Average per cell (e.g., 3.7V)
    public int batteryPercent = 0;

    // --- Flight Status ---
    public boolean isArmed = false;
    public String flightMode = "--";
    public long flightTimeInSeconds = 0; // 00:00 format calculation

    // --- HUD Details ---
    public float airspeed = 0f;
    public float climbRate = 0f;
    public float distanceToHome = 0f; // Calculated in Client

    public void reset() {
        connected = false;
        latitude = 0;
        longitude = 0;
        altitude = 0;
        satelliteCount = 0;
        yaw = 0;
        batteryVoltage = 0;
        cellVoltage = 0;
        batteryPercent = 0;
        isArmed = false;
        flightMode = "DISCONNECTED";
        flightTimeInSeconds = 0;
        distanceToHome = 0;
    }
}