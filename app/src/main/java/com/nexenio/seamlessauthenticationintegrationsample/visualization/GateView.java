package com.nexenio.seamlessauthenticationintegrationsample.visualization;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import com.nexenio.seamlessauthentication.SeamlessAuthenticator;
import com.nexenio.seamlessauthentication.accesscontrol.gate.Gate;
import com.nexenio.seamlessauthentication.accesscontrol.gateway.Gateway;
import com.nexenio.seamlessauthentication.accesscontrol.gateway.GatewayDirection;
import com.nexenio.seamlessauthentication.accesscontrol.gateway.opening.GatewayOpening;
import com.nexenio.seamlessauthentication.internal.accesscontrol.beacons.detection.GatewayDetectionBeacon;
import com.nexenio.seamlessauthentication.internal.accesscontrol.beacons.lock.GatewayDirectionLockBeacon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GateView extends View implements GateVisualization {

    private Gate gate;

    private float pixelsPerDp;

    private float gatewayWidth;
    private float gatewayHeight;

    private float gatewaySeparatorWidth;
    private float gatewaySeparatorHeight;
    private RectF gatewaySeparatorRect;

    private float gatewayOpeningMargin;
    private float gatewayOpeningWidth;
    private float gatewayOpeningHeight;
    private RectF gatewayOpeningRect;

    private float gatewayDetectionBeaconWidth;
    private float gatewayDetectionBeaconHeight;
    private RectF gatewayDetectionBeaconRect;

    private int backgroundColor;
    private int foregroundColor;
    private int bluetoothColor;
    private int errorColor;

    private Paint backgroundPaint;
    private Paint strokePaint;

    private Paint gatewaySeparatorStrokePaint;
    private Paint gatewaySeparatorFillPaint;

    private Paint gatewayOpeningStrokePaint;
    private Paint gatewayOpeningFillPaint;

    private Paint beaconStrokePaint;
    private Paint beaconFillPaint;

    private final Map<String, ValueAnimator> valueAnimatorMap = new HashMap<>();
    private final Map<String, Long> latestTimestampMap = new HashMap<>();

    public GateView(Context context) {
        super(context);
        initialize();
    }

    public GateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    private void initialize() {
        if (isInEditMode()) {
            pixelsPerDp = 2;
        } else {
            pixelsPerDp = ViewUtil.convertDpToPixel(1, getContext());
        }

        gatewayWidth = 250 * pixelsPerDp;
        gatewayHeight = 150 * pixelsPerDp;

        gatewaySeparatorWidth = gatewayWidth;
        gatewaySeparatorHeight = 20 * pixelsPerDp;
        gatewaySeparatorRect = new RectF(0, 0, gatewaySeparatorWidth, gatewaySeparatorHeight);

        gatewayOpeningMargin = 0.1f * (gatewayHeight - (2 * gatewaySeparatorHeight));
        gatewayOpeningHeight = (gatewayHeight - (2 * gatewaySeparatorHeight)) - (2 * gatewayOpeningMargin);
        gatewayOpeningWidth = gatewayWidth / 2;
        gatewayOpeningRect = new RectF(0, 0, gatewayOpeningWidth, gatewayOpeningHeight);

        gatewayDetectionBeaconWidth = 2 * (gatewaySeparatorHeight / 3);
        gatewayDetectionBeaconHeight = gatewayDetectionBeaconWidth;
        gatewayDetectionBeaconRect = new RectF(0, 0, gatewayDetectionBeaconWidth, gatewayDetectionBeaconHeight);

        backgroundColor = Color.WHITE;
        foregroundColor = Color.BLACK;
        bluetoothColor = Color.parseColor("#1976D2");
        errorColor = Color.parseColor("#7F0000");

        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(backgroundColor);

        strokePaint = new Paint();
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(1 * pixelsPerDp);
        strokePaint.setAntiAlias(true);
        strokePaint.setColor(foregroundColor);

        gatewaySeparatorStrokePaint = new Paint(strokePaint);
        gatewaySeparatorStrokePaint.setColor(Color.BLACK);

        gatewaySeparatorFillPaint = new Paint(backgroundPaint);
        gatewaySeparatorFillPaint.setColor(Color.parseColor("#99BBBBBB"));

        gatewayOpeningStrokePaint = new Paint(strokePaint);
        gatewayOpeningStrokePaint.setColor(Color.parseColor("#BBBBBB"));
        PathEffect dash = new DashPathEffect(new float[]{10 * pixelsPerDp, 3 * pixelsPerDp}, 0);
        gatewayOpeningStrokePaint.setPathEffect(dash);

        gatewayOpeningFillPaint = new Paint(backgroundPaint);
        gatewayOpeningFillPaint.setColor(Color.parseColor("#99DDDDDD"));

        beaconFillPaint = new Paint(gatewaySeparatorFillPaint);
        beaconFillPaint.setColor(bluetoothColor);

        beaconStrokePaint = new Paint(gatewaySeparatorStrokePaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        long gatewayCount = 1;
        if (gate != null) {
            gatewayCount = gate.getGateways().count().blockingGet();
        }

        int gateWidth = Math.round(gatewayCount * gatewayWidth);
        int minimumWidth = gateWidth + getPaddingLeft() + getPaddingRight();
        int width = resolveSizeAndState(minimumWidth, widthMeasureSpec, 1);

        int gateHeight = Math.round(gatewayCount * gatewayHeight);
        int minimumHeight = gateHeight + getPaddingBottom() + getPaddingTop();
        int height = resolveSizeAndState(minimumHeight, heightMeasureSpec, 1);

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // fill canvas with background color
        canvas.drawColor(backgroundColor);

        if (gate == null) {
            return;
        }

        int saveCount = canvas.save();

        // center the gate
        canvas.translate((getWidth() - gatewayWidth) / 2, 0);

        // draw gateways
        List<Gateway> gateways = gate.getGateways().toList().blockingGet();
        for (Gateway gateway : gateways) {
            drawGateway(canvas, gateway);
            canvas.translate(0, gatewayHeight + gatewayOpeningMargin);
        }

        canvas.restoreToCount(saveCount);

        // invalidate for animations
        invalidate();
    }

    private void drawGateway(Canvas canvas, Gateway gateway) {
        int saveCount = canvas.save();

        // top separator
        drawGatewaySeparator(canvas);

        // bottom separator
        canvas.translate(0, gatewayHeight - gatewaySeparatorHeight);
        drawGatewaySeparator(canvas);

        canvas.restoreToCount(saveCount);
        saveCount = canvas.save();

        // openings (entry & exit)
        canvas.translate(0, (gatewayHeight - gatewayOpeningHeight) / 2);

        List<GatewayOpening> openings = gateway.getOpenings().toList().blockingGet();
        for (GatewayOpening opening : openings) {
            int openingSaveCount = canvas.save();
            boolean isEntry = opening.getDirection().blockingGet() == GatewayDirection.ENTRY;
            if (!isEntry) {
                canvas.scale(-1, 1, gatewayWidth / 2, gatewayHeight / 2);
            }
            drawGatewayOpening(canvas, opening);
            canvas.restoreToCount(openingSaveCount);
        }

        canvas.restoreToCount(saveCount);

        // barrier
        drawGatewayBarrier(canvas);
    }

    private void drawGatewaySeparator(Canvas canvas) {
        canvas.drawRect(gatewaySeparatorRect, gatewaySeparatorFillPaint);
        canvas.drawRect(gatewaySeparatorRect, gatewaySeparatorStrokePaint);
    }

    private void drawGatewayBarrier(Canvas canvas) {
        int saveCount = canvas.save();

        // top swivel arm
        drawGatewaySwivelArm(canvas);

        canvas.scale(1, -1, gatewayWidth / 2, gatewayHeight / 2);

        // bottom swivel arm
        drawGatewaySwivelArm(canvas);

        canvas.restoreToCount(saveCount);
    }

    private void drawGatewaySwivelArm(Canvas canvas) {
        Bitmap resultBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas resultCanvas = new Canvas(resultBitmap);

        // destination
        Bitmap dstBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas dstCanvas = new Canvas(dstBitmap);

        dstCanvas.drawCircle(
                gatewayWidth / 2,
                gatewaySeparatorHeight,
                gatewaySeparatorHeight * 0.66f,
                gatewaySeparatorFillPaint
        );

        dstCanvas.drawCircle(
                gatewayWidth / 2,
                gatewaySeparatorHeight,
                gatewaySeparatorHeight * 0.66f,
                gatewaySeparatorStrokePaint
        );

        // source
        Bitmap srcBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas srcCanvas = new Canvas(srcBitmap);

        srcCanvas.drawRect(gatewaySeparatorRect, backgroundPaint);

        // mask
        Paint maskPaint = new Paint();
        maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        resultCanvas.drawBitmap(dstBitmap, 0, 0, null);
        resultCanvas.drawBitmap(srcBitmap, 0, 0, maskPaint);

        canvas.drawBitmap(resultBitmap, 0, 0, null);

        // barrier
        float barrierStartY = gatewaySeparatorHeight + (gatewaySeparatorHeight * 0.66f);
        float barrierHeight = gatewayHeight - (2 * barrierStartY);
        float barrierEndY = barrierStartY + (0.45f * barrierHeight);
        canvas.drawLine(
                gatewayWidth / 2,
                barrierStartY,
                gatewayWidth / 2,
                barrierEndY,
                gatewaySeparatorStrokePaint
        );
    }

    private void drawGatewayOpening(Canvas canvas, GatewayOpening gatewayOpening) {
        int saveCount = canvas.save();

        canvas.translate(-2 * gatewayOpeningMargin, 0);

        canvas.drawRect(gatewayOpeningRect, gatewayOpeningFillPaint);
        canvas.drawRect(gatewayOpeningRect, gatewayOpeningStrokePaint);

        canvas.restoreToCount(saveCount);
        saveCount = canvas.save();

        canvas.translate(0, 0 - ((gatewayHeight - gatewayOpeningHeight) / 2));
        canvas.translate(0, gatewaySeparatorHeight - gatewayDetectionBeaconHeight);

        boolean isEntry = gatewayOpening.getDirection().blockingGet() == GatewayDirection.ENTRY;

        List<GatewayDetectionBeacon> beacons = gatewayOpening.getDetectionBeacons().toList().blockingGet();
        for (GatewayDetectionBeacon beacon : beacons) {
            int saveCount2 = canvas.save();
            boolean isLeft = beacon.getPosition() == GatewayDetectionBeacon.LEFT;
            boolean shouldTranslate = (isEntry && !isLeft) || (!isEntry && isLeft);
            if (shouldTranslate) {
                canvas.translate(0, gatewayHeight - gatewaySeparatorHeight - (gatewaySeparatorHeight - gatewayDetectionBeaconHeight));
            }

            drawGatewayDetectionBeacon(canvas, beacon);
            canvas.restoreToCount(saveCount2);
        }
        canvas.restoreToCount(saveCount);
    }

    private void drawGatewayDetectionBeacon(Canvas canvas, GatewayDetectionBeacon beacon) {
        int saveCount = canvas.save();

        long maximumTimestamp = System.currentTimeMillis() - 250;
        long minimumTimestamp = maximumTimestamp - TimeUnit.SECONDS.toMillis(2);
        float recencyScore = getRecencyScore(beacon.getLatestTimestamp(), minimumTimestamp, maximumTimestamp);

        if (!valueAnimatorMap.containsKey(beacon.getMacAddress())) {
            int startValue = Math.round(255 * (1 - recencyScore));
            int endValue = 0;
            long duration = Math.round(TimeUnit.SECONDS.toMillis(1) * recencyScore);

            ValueAnimator valueAnimator = ValueAnimator.ofInt(startValue, endValue);
            valueAnimator.setDuration(duration);
            valueAnimator.setInterpolator(new DecelerateInterpolator());
            valueAnimator.start();

            valueAnimatorMap.put(beacon.getMacAddress(), valueAnimator);
        }

        ValueAnimator valueAnimator = valueAnimatorMap.get(beacon.getMacAddress());
        if (!valueAnimator.isRunning() || valueAnimator.getAnimatedFraction() < recencyScore) {
            int startValue = Math.round(255 * recencyScore);
            int endValue = 0;
            valueAnimator.setIntValues(startValue, endValue);
            valueAnimator.start();
        }

        int alpha;
        if (valueAnimator.isRunning()) {
            alpha = (int) valueAnimator.getAnimatedValue();
        } else if (valueAnimator.getAnimatedFraction() < 0.5) {
            alpha = 0;
        } else {
            alpha = 255;
        }

        /*
        beaconFillPaint.setColor(errorColor);
        beaconFillPaint.setAlpha(255);
        canvas.drawRect(gatewayDetectionBeaconRect, beaconFillPaint);
        */

        beaconFillPaint.setColor(bluetoothColor);
        beaconFillPaint.setAlpha(alpha);
        canvas.drawRect(gatewayDetectionBeaconRect, beaconFillPaint);

        canvas.drawRect(gatewayDetectionBeaconRect, beaconStrokePaint);
        canvas.restoreToCount(saveCount);
    }

    @Override
    public void onAuthenticatorUpdated(@NonNull SeamlessAuthenticator authenticator) {
        if (!(authenticator instanceof Gate)) {
            throw new IllegalArgumentException("Authenticator is not a gate");
        }
        onAuthenticatorUpdated((Gate) authenticator);
    }

    @Override
    public void onAuthenticatorUpdated(@NonNull Gate gate) {
        if (this.gate == gate) {
            return;
        }

        this.gate = gate;
        invalidate();
        requestLayout();
    }

    @Override
    public void onDetectionBeaconUpdated(@NonNull GatewayDetectionBeacon gatewayDetectionBeacon) {
        invalidate();
    }

    @Override
    public void onDirectionLockBeaconUpdated(@NonNull GatewayDirectionLockBeacon gatewayDetectionBeacon) {
        invalidate();
    }

    /**
     * Calculates a score in range [0,1] based on the recency of the specified timestamp (higher
     * values are more recent).
     *
     * @param timestamp        timestamp to base the score on
     * @param minimumTimestamp timestamp before which the score is 0
     * @param maximumTimestamp timestamp after which the score is 1
     */
    private static float getRecencyScore(long timestamp, long minimumTimestamp, long maximumTimestamp) {
        if (timestamp <= minimumTimestamp) {
            return 0;
        } else if (timestamp >= maximumTimestamp) {
            return 1;
        } else {
            return 1 - ((float) (maximumTimestamp - timestamp) / (maximumTimestamp - minimumTimestamp));
        }
    }

}
