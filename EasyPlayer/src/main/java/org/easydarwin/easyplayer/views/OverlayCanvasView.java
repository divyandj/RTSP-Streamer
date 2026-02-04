package org.easydarwin.easyplayer.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import org.easydarwin.easyplayer.data.TelemetryData;

import java.util.Locale;

/**
 * Tablet UI Overlay.
 * Optimized for 1920x1200 Horizontal Display.
 * Layout: Corners + Top Center Arrow + Center Crosshair.
 */
public class OverlayCanvasView extends View {

    private TelemetryData data;

    // Paints
    private Paint textLargePaint;
    private Paint textSmallPaint;
    private Paint labelPaint;
    private Paint warningPaint;
    private Paint safePaint;
    private Paint arrowPaint;
    private Paint crosshairPaint; // <--- New Paint for Crosshair

    // Dimensions
    private float width, height;
    private float centerX, centerY; // <--- Added centerY

    public OverlayCanvasView(Context context) {
        super(context);
        init();
    }

    public OverlayCanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OverlayCanvasView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        data = new TelemetryData();

        // 1. Large Value Text (Voltage, Alt)
        textLargePaint = new Paint();
        textLargePaint.setColor(Color.WHITE);
        textLargePaint.setTextSize(60f);
        textLargePaint.setFakeBoldText(true);
        textLargePaint.setAntiAlias(true);
        textLargePaint.setShadowLayer(5f, 2f, 2f, Color.BLACK);

        // 2. Small Value Text (Lat/Lon, Cell V)
        textSmallPaint = new Paint();
        textSmallPaint.setColor(Color.LTGRAY);
        textSmallPaint.setTextSize(35f);
        textSmallPaint.setAntiAlias(true);
        textSmallPaint.setShadowLayer(3f, 1f, 1f, Color.BLACK);

        // 3. Labels (Units, Titles)
        labelPaint = new Paint();
        labelPaint.setColor(0xFFAAAAAA); // Light Gray
        labelPaint.setTextSize(28f);
        labelPaint.setAntiAlias(true);

        // 4. Status Colors
        warningPaint = new Paint(textLargePaint);
        warningPaint.setColor(Color.RED);

        safePaint = new Paint(textLargePaint);
        safePaint.setColor(Color.GREEN);

        // 5. Home Arrow
        arrowPaint = new Paint();
        arrowPaint.setColor(0xFFFFD700); // Gold
        arrowPaint.setStyle(Paint.Style.FILL);
        arrowPaint.setAntiAlias(true);
        arrowPaint.setShadowLayer(4f, 2f, 2f, Color.BLACK);

        // 6. Crosshair Paint
        crosshairPaint = new Paint();
        crosshairPaint.setColor(Color.WHITE);
        crosshairPaint.setStyle(Paint.Style.STROKE);
        crosshairPaint.setStrokeWidth(4f); // Slightly thicker for visibility
        crosshairPaint.setAntiAlias(true);
        crosshairPaint.setShadowLayer(3f, 1f, 1f, Color.BLACK);
    }

    public void updateTelemetry(TelemetryData newData) {
        this.data = newData;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.width = w;
        this.height = h;
        this.centerX = w / 2f;
        this.centerY = h / 2f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (data == null) return;

        // Draw UI Elements
        drawTopLeft_Power(canvas);
        drawTopRight_Status(canvas);
        drawBottomLeft_Location(canvas);
        drawBottomRight_FlightData(canvas);
        drawTopCenter_HomeArrow(canvas);

        // Draw the Center Crosshair
        drawCenterCrosshair(canvas);
    }

    // --- CENTER: Crosshair ---
    private void drawCenterCrosshair(Canvas canvas) {
        float size = 30f; // Length of each arm
        float gap = 10f;  // Gap in the middle to see the exact center pixel

        // Left Arm
        canvas.drawLine(centerX - size, centerY, centerX - gap, centerY, crosshairPaint);
        // Right Arm
        canvas.drawLine(centerX + gap, centerY, centerX + size, centerY, crosshairPaint);
        // Top Arm
        canvas.drawLine(centerX, centerY - size, centerX, centerY - gap, crosshairPaint);
        // Bottom Arm
        canvas.drawLine(centerX, centerY + gap, centerX, centerY + size, crosshairPaint);
    }

    // --- TOP LEFT: Power ---
    private void drawTopLeft_Power(Canvas canvas) {
        float x = 40f;
        float y = 80f;

        String vol = String.format(Locale.US, "%.1fV", data.batteryVoltage);
        canvas.drawText(vol, x, y, textLargePaint);

        String cell = String.format(Locale.US, "%.2fV / cell", data.cellVoltage);
        canvas.drawText(cell, x, y + 45, textSmallPaint);

        String pct = data.batteryPercent + "%";
        Paint p = (data.batteryPercent < 20) ? warningPaint : safePaint;
        Paint pctPaint = new Paint(p);
        pctPaint.setTextSize(35f);
        canvas.drawText(pct, x + 180, y, pctPaint);
    }

    // --- TOP RIGHT: Status ---
    private void drawTopRight_Status(Canvas canvas) {
        float x = width - 40f;
        float y = 80f;

        Paint statePaint = data.isArmed ? warningPaint : safePaint;
        String stateText = data.isArmed ? "ARMED" : "DISARMED";

        statePaint.setTextAlign(Paint.Align.RIGHT);
        textSmallPaint.setTextAlign(Paint.Align.RIGHT);
        textLargePaint.setTextAlign(Paint.Align.RIGHT);

        canvas.drawText(stateText, x, y, statePaint);

        long min = data.flightTimeInSeconds / 60;
        long sec = data.flightTimeInSeconds % 60;
        String timeText = String.format(Locale.US, "%02d:%02d", min, sec);
        canvas.drawText(timeText, x, y + 60, textLargePaint);

        String satText = "Sats: " + data.satelliteCount;
        canvas.drawText(satText, x, y + 105, textSmallPaint);

        statePaint.setTextAlign(Paint.Align.LEFT);
        textSmallPaint.setTextAlign(Paint.Align.LEFT);
        textLargePaint.setTextAlign(Paint.Align.LEFT);
    }

    // --- BOTTOM LEFT: Location ---
    private void drawBottomLeft_Location(Canvas canvas) {
        float x = 40f;
        float y = height - 40f;

        String latText = String.format(Locale.US, "Lat: %.6f", data.latitude);
        String lonText = String.format(Locale.US, "Lon: %.6f", data.longitude);

        canvas.drawText(lonText, x, y, textSmallPaint);
        canvas.drawText(latText, x, y - 40, textSmallPaint);
    }

    // --- BOTTOM RIGHT: Flight Data ---
    private void drawBottomRight_FlightData(Canvas canvas) {
        float x = width - 40f;
        float y = height - 40f;

        textLargePaint.setTextAlign(Paint.Align.RIGHT);
        textSmallPaint.setTextAlign(Paint.Align.RIGHT);

        String altText = String.format(Locale.US, "%.1f m", data.altitude);
        canvas.drawText(altText, x, y - 60, textLargePaint);
        canvas.drawText("ALTITUDE", x, y - 40, textSmallPaint);

        String distText = String.format(Locale.US, "H: %.0f m", data.distanceToHome);
        canvas.drawText(distText, x, y, textSmallPaint);

        textLargePaint.setTextAlign(Paint.Align.LEFT);
        textSmallPaint.setTextAlign(Paint.Align.LEFT);
    }

    // --- TOP CENTER: Home Arrow ---
    private void drawTopCenter_HomeArrow(Canvas canvas) {
        if (data.homeLatitude == 0 && data.homeLongitude == 0) return;

        float y = 100f;
        float size = 40f;

        float bearing = calculateBearing(data.latitude, data.longitude, data.homeLatitude, data.homeLongitude);
        float rotation = bearing - data.yaw;

        canvas.save();
        canvas.translate(centerX, y);
        canvas.rotate(rotation);

        Path arrow = new Path();
        arrow.moveTo(0, -size);
        arrow.lineTo(size/2, size);
        arrow.lineTo(0, size * 0.7f);
        arrow.lineTo(-size/2, size);
        arrow.close();

        canvas.drawPath(arrow, arrowPaint);

        canvas.restore();

        labelPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.format(Locale.US, "%.0f°", bearing), centerX, y + size + 30, labelPaint);
    }

    private float calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        double dLon = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dLon) * Math.cos(Math.toRadians(lat2));
        double x = Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) -
                Math.sin(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(dLon);

        double brng = Math.toDegrees(Math.atan2(y, x));
        return (float) ((brng + 360) % 360);
    }

    // --- Compatibility Stubs ---
    public void setTransMatrix(Matrix matrix) {}
    public void toggleDrawable() {}
}