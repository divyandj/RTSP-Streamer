package org.easydarwin.easyplayer.util;

import android.util.Log;

import org.easydarwin.easyplayer.data.TelemetryData;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.dronefleet.mavlink.MavlinkConnection;
import io.dronefleet.mavlink.MavlinkMessage;
import io.dronefleet.mavlink.common.Attitude;
import io.dronefleet.mavlink.common.GlobalPositionInt;
import io.dronefleet.mavlink.common.SysStatus;
import io.dronefleet.mavlink.common.VfrHud;
import io.dronefleet.mavlink.minimal.Heartbeat;

public class MavlinkTcpClient {

    private static final String TAG = "MavlinkTcpClient";

    // Standard MAVLink Constant for Armed state (0b10000000)
    private static final int MAV_MODE_FLAG_SAFETY_ARMED = 128;

    public interface TelemetryListener {
        void onTelemetryUpdate(TelemetryData data);
    }

    private Socket socket;
    private boolean isRunning = false;
    private final ExecutorService executor;
    private TelemetryListener listener;

    private final TelemetryData currentData;
    private long flightStartTime = 0;

    public MavlinkTcpClient(TelemetryListener listener) {
        this.listener = listener;
        this.currentData = new TelemetryData();
        this.executor = Executors.newSingleThreadExecutor();
    }

    public void start(final String ip, final int port) {
        if (isRunning) return;
        isRunning = true;

        executor.execute(new Runnable() {
            @Override
            public void run() {
                connectAndListen(ip, port);
            }
        });
    }

    public void stop() {
        isRunning = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        currentData.reset();
        if (listener != null) listener.onTelemetryUpdate(currentData);
    }

    private void connectAndListen(String ip, int port) {
        Log.d(TAG, "Connecting to drone at " + ip + ":" + port);

        try {
            socket = new Socket(ip, port);
            socket.setKeepAlive(true);
            socket.setSoTimeout(5000); // 5 sec read timeout

            MavlinkConnection connection = MavlinkConnection.create(socket.getInputStream(), socket.getOutputStream());

            Log.d(TAG, "Connected to drone!");
            currentData.connected = true;

            while (isRunning && !socket.isClosed()) {
                try {
                    MavlinkMessage message = connection.next();
                    if (message != null) {
                        handleMessage(message);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Stream error (retrying): " + e.getMessage());
                    // Don't break immediately, maybe just a packet glitch
                    if (socket.isClosed()) break;
                }
            }

        } catch (IOException e) {
            Log.e(TAG, "Connection failed: " + e.getMessage());
        } finally {
            currentData.connected = false;
            if (listener != null) listener.onTelemetryUpdate(currentData);
        }
    }

    private void handleMessage(MavlinkMessage message) {
        Object payload = message.getPayload();
        boolean needsUpdate = false;

        // --- 1. HEARTBEAT (Armed State & Mode) ---
        if (payload instanceof Heartbeat) {
            Heartbeat hb = (Heartbeat) payload;

            // FIX: Use 128 (bitmask for Armed) directly to avoid Enum errors
            boolean isArmedNow = (hb.baseMode().value() & MAV_MODE_FLAG_SAFETY_ARMED) != 0;

            // Handle Arming Event (Start Timer / Set Home)
            if (isArmedNow && !currentData.isArmed) {
                // Just Armed: Start Timer
                flightStartTime = System.currentTimeMillis();
                // Set Home Position to current location (if valid)
                if (currentData.latitude != 0 && currentData.longitude != 0) {
                    currentData.homeLatitude = currentData.latitude;
                    currentData.homeLongitude = currentData.longitude;
                }
            } else if (!isArmedNow) {
                // Disarmed: Stop timer logic
                flightStartTime = 0;
                currentData.flightTimeInSeconds = 0;
            }

            currentData.isArmed = isArmedNow;

            // Update Flight Time
            if (currentData.isArmed && flightStartTime > 0) {
                long diff = System.currentTimeMillis() - flightStartTime;
                currentData.flightTimeInSeconds = diff / 1000;
            }

            currentData.flightMode = "Mode: " + hb.customMode();
            needsUpdate = true;
        }

        // --- 2. GLOBAL_POSITION_INT (GPS Lat/Lon/Alt) ---
        else if (payload instanceof GlobalPositionInt) {
            GlobalPositionInt pos = (GlobalPositionInt) payload;

            // Convert 1E7 integer to double degrees
            currentData.latitude = pos.lat() / 1E7;
            currentData.longitude = pos.lon() / 1E7;
            currentData.altitude = pos.relativeAlt() / 1000f; // mm to meters

            // Calculate Distance to Home
            if (currentData.homeLatitude != 0 && currentData.homeLongitude != 0) {
                currentData.distanceToHome = calculateDistance(
                        currentData.latitude, currentData.longitude,
                        currentData.homeLatitude, currentData.homeLongitude
                );
            }

            needsUpdate = true;
        }

        // --- 3. SYS_STATUS (Battery) ---
        else if (payload instanceof SysStatus) {
            SysStatus sys = (SysStatus) payload;

            float voltage = sys.voltageBattery() / 1000f; // mV to V
            currentData.batteryVoltage = voltage;
            currentData.batteryPercent = sys.batteryRemaining();

            // Calculate Cell Voltage (Estimate)
            int estimatedCells = Math.round(voltage / 4.0f);
            if (estimatedCells == 0) estimatedCells = 1;
            currentData.cellVoltage = voltage / estimatedCells;

            needsUpdate = true;
        }

        // --- 4. ATTITUDE (Pitch/Roll/Yaw) ---
        else if (payload instanceof Attitude) {
            Attitude att = (Attitude) payload;
            currentData.pitch = (float) Math.toDegrees(att.pitch());
            currentData.roll = (float) Math.toDegrees(att.roll());

            float yawDeg = (float) Math.toDegrees(att.yaw());
            if (yawDeg < 0) yawDeg += 360;
            currentData.yaw = yawDeg;

            needsUpdate = true;
        }

        // --- 5. VFR_HUD (Airspeed/Climb) ---
        else if (payload instanceof VfrHud) {
            VfrHud hud = (VfrHud) payload;
            currentData.airspeed = hud.airspeed();
            currentData.climbRate = hud.climb();
            needsUpdate = true;
        }

        if (needsUpdate && listener != null) {
            listener.onTelemetryUpdate(currentData);
        }
    }

    // Haversine Formula for distance in meters
    private float calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double earthRadius = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return (float) (earthRadius * c);
    }
}