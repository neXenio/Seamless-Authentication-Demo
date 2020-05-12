package me.seamless.authentication.example.visualization;

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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import me.seamless.authentication.CommunicationUnit;
import me.seamless.authentication.accesscontrol.gate.Gate;
import me.seamless.authentication.accesscontrol.gateway.Gateway;
import me.seamless.authentication.accesscontrol.gateway.GatewayDirection;
import me.seamless.authentication.accesscontrol.gateway.GatewayMode;
import me.seamless.authentication.accesscontrol.gateway.opening.GatewayOpening;
import me.seamless.authentication.example.R;
import me.seamless.authentication.internal.accesscontrol.beacons.detection.GatewayDetectionBeacon;
import me.seamless.authentication.internal.accesscontrol.beacons.lock.DirectionLockBeacon;
import me.seamless.sblec.positioning.beacon.AdvertisingPacket;
import me.seamless.sblec.positioning.internal.beacon.BaseAdvertisingPacket;
import timber.log.Timber;

public class GateView extends View implements GateVisualization {

    private Gate gate;
    private GatewayOpening closestGatewayOpening;

    private float pixelsPerDp;

    private float gateWidth;
    private float gateHeight;

    private float desiredGatewayWidth;
    private float desiredGatewayHeight;

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

    private float directionLockBeaconWidth;
    private float directionLockBeaconHeight;
    private RectF directionLockBeaconRect;

    private int backgroundColor;
    private int foregroundColor;
    private int bluetoothColor;
    private int errorColor;
    private int lockedColor;
    private int unlockedColor;

    private Paint backgroundPaint;
    private Paint strokePaint;
    private Paint textPaint;

    private Paint gatewaySeparatorStrokePaint;
    private Paint gatewaySeparatorFillPaint;

    private Paint gatewayOpeningStrokePaint;
    private Paint gatewayOpeningFillPaint;

    private Paint beaconStrokePaint;
    private Paint beaconFillPaint;
    private Paint advertisingPacketPaint;

    private Paint maskPaint;

    private Bitmap resultBitmap;
    private Canvas resultCanvas;

    private Bitmap sourceBitmap;
    private Canvas sourceCanvas;

    private Bitmap destinationBitmap;
    private Canvas destinationCanvas;

    private String gatewayText = "";
    private String entryText = "";
    private String exitText = "";

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

        desiredGatewayWidth = 250 * pixelsPerDp;
        desiredGatewayHeight = 150 * pixelsPerDp;

        gatewaySeparatorRect = new RectF(0, 0, gatewaySeparatorWidth, gatewaySeparatorHeight);
        gatewayOpeningRect = new RectF(0, 0, gatewayOpeningWidth, gatewayOpeningHeight);
        gatewayDetectionBeaconRect = new RectF(0, 0, gatewayDetectionBeaconWidth, gatewayDetectionBeaconHeight);
        directionLockBeaconRect = new RectF(0, 0, directionLockBeaconWidth, directionLockBeaconHeight);

        backgroundColor = Color.WHITE;
        foregroundColor = Color.BLACK;
        bluetoothColor = Color.parseColor("#90caf9");
        errorColor = Color.parseColor("#7F0000");
        lockedColor = Color.parseColor("#7f0000");
        unlockedColor = Color.parseColor("#003300");

        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(backgroundColor);

        strokePaint = new Paint();
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(1 * pixelsPerDp);
        strokePaint.setAntiAlias(true);
        strokePaint.setColor(foregroundColor);

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(10 * pixelsPerDp);
        textPaint.setColor(foregroundColor);

        gatewaySeparatorStrokePaint = new Paint(strokePaint);
        gatewaySeparatorStrokePaint.setColor(Color.BLACK);

        gatewaySeparatorFillPaint = new Paint(backgroundPaint);
        gatewaySeparatorFillPaint.setColor(Color.parseColor("#99BBBBBB"));

        gatewayOpeningStrokePaint = new Paint(strokePaint);
        gatewayOpeningStrokePaint.setColor(Color.parseColor("#BBBBBB"));
        PathEffect dash = new DashPathEffect(new float[]{10 * pixelsPerDp, 3 * pixelsPerDp}, 0);
        gatewayOpeningStrokePaint.setPathEffect(dash);

        gatewayOpeningFillPaint = new Paint(backgroundPaint);
        gatewayOpeningFillPaint.setColor(Color.parseColor("#DDDDDD"));

        beaconFillPaint = new Paint(gatewaySeparatorFillPaint);
        beaconFillPaint.setColor(bluetoothColor);

        beaconStrokePaint = new Paint(gatewaySeparatorStrokePaint);

        advertisingPacketPaint = new Paint(beaconStrokePaint);
        advertisingPacketPaint.setColor(beaconFillPaint.getColor());

        maskPaint = new Paint();
        maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        gatewayText = getContext().getString(R.string.gateway_description);
        entryText = getContext().getString(R.string.entry_description);
        exitText = getContext().getString(R.string.exit_description);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        long gatewayCount = 1;
        if (gate != null) {
            gatewayCount = gate.getGateways().count().blockingGet();
        }

        gateWidth = gatewayCount * desiredGatewayWidth;
        gateHeight = gatewayCount * desiredGatewayHeight;

        int horizontalPadding = Math.max(Math.round(2 * pixelsPerDp), getPaddingLeft() + getPaddingRight());
        int verticalPadding = Math.max(Math.round(2 * pixelsPerDp), getPaddingBottom() + getPaddingTop());

        int minimumWidth = (int) gateWidth + horizontalPadding;
        int minimumHeight = (int) gateHeight + verticalPadding;

        int viewWidth = resolveSizeAndState(minimumWidth, widthMeasureSpec, 1);
        int viewHeight = resolveSizeAndState(minimumHeight, heightMeasureSpec, 1);

        gateWidth = viewWidth - horizontalPadding;
        gateHeight = viewHeight - verticalPadding;

        gatewayWidth = Math.min(0.8F * gateWidth, desiredGatewayWidth);
        gatewayHeight = 1F * gateHeight / gatewayCount;

        gatewaySeparatorWidth = gatewayWidth;
        gatewaySeparatorHeight = 0.125F * gatewayHeight;
        gatewaySeparatorRect.set(0, 0, gatewaySeparatorWidth, gatewaySeparatorHeight);

        gatewayOpeningMargin = 0.1f * (gatewayHeight - (2 * gatewaySeparatorHeight));
        gatewayOpeningHeight = (gatewayHeight - (2 * gatewaySeparatorHeight)) - (2 * gatewayOpeningMargin);
        gatewayOpeningWidth = gatewayWidth / 2;
        gatewayOpeningRect.set(0, 0, gatewayOpeningWidth, gatewayOpeningHeight);

        gatewayDetectionBeaconWidth = 2 * (gatewaySeparatorHeight / 3);
        gatewayDetectionBeaconHeight = gatewayDetectionBeaconWidth;
        gatewayDetectionBeaconRect.set(0, 0, gatewayDetectionBeaconWidth, gatewayDetectionBeaconHeight);

        textPaint.setTextSize(0.75F * gatewayDetectionBeaconHeight);

        directionLockBeaconWidth = gatewayDetectionBeaconWidth;
        directionLockBeaconHeight = gatewayDetectionBeaconHeight;
        directionLockBeaconRect.set(0, 0, directionLockBeaconWidth, directionLockBeaconHeight);

        setMeasuredDimension(viewWidth, viewHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        try {
            // fill canvas with background color
            canvas.drawColor(backgroundColor);

            if (gate == null) {
                return;
            }

            closestGatewayOpening = gate.getClosestGateway()
                    .flatMap(Gateway::getClosestOrLockedInOpening)
                    .blockingGet();

            // respect padding
            canvas.translate(getPaddingLeft(), getPaddingTop());

            int saveCount = canvas.save();

            // center the gate
            canvas.translate((gateWidth - gatewayWidth) / 2, 0);

            // draw gateways
            List<Gateway> gateways = gate.getGateways().toList().blockingGet();
            for (Gateway gateway : gateways) {
                drawGateway(canvas, gateway);
                canvas.translate(0, gatewayHeight);
            }

            canvas.restoreToCount(saveCount);

            // draw gateway detection beacons
            drawDirectionLockBeacons(canvas);
        } catch (Exception e) {
            Timber.w(e, "Unable to draw gate");
        }

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
                canvas.translate(gatewayWidth / 2, 0);
            }
            drawGatewayOpening(canvas, opening);
            canvas.restoreToCount(openingSaveCount);
        }

        canvas.restoreToCount(saveCount);

        // gateway description
        textPaint.setColor(gatewaySeparatorStrokePaint.getColor());
        String gatewayDescription = gatewayText + " " + gateway.getGatewayIndex().blockingGet();
        canvas.drawText(
                gatewayDescription,
                gatewaySeparatorWidth / 2,
                ((gatewaySeparatorHeight / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)),
                textPaint
        );

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
        resultBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        resultCanvas = new Canvas(resultBitmap);

        // destination
        destinationBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        destinationCanvas = new Canvas(destinationBitmap);

        destinationCanvas.drawCircle(
                gatewayWidth / 2,
                gatewaySeparatorHeight,
                gatewaySeparatorHeight * 0.66f,
                gatewaySeparatorFillPaint
        );

        destinationCanvas.drawCircle(
                gatewayWidth / 2,
                gatewaySeparatorHeight,
                gatewaySeparatorHeight * 0.66f,
                gatewaySeparatorStrokePaint
        );

        // source
        sourceBitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        sourceCanvas = new Canvas(sourceBitmap);

        sourceCanvas.drawRect(gatewaySeparatorRect, backgroundPaint);

        // mask
        resultCanvas.drawBitmap(destinationBitmap, 0, 0, null);
        resultCanvas.drawBitmap(sourceBitmap, 0, 0, maskPaint);

        canvas.drawBitmap(resultBitmap, 0, 0, null);
        resultBitmap.recycle();
        destinationBitmap.recycle();
        sourceBitmap.recycle();

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

        int direction = gatewayOpening.getDirection().blockingGet();
        int lockMode = gate.getDirectionLockMode().blockingGet();
        boolean requiresAuthentication = gatewayOpening.requiresAuthentication().blockingGet();
        boolean isEntry = direction == GatewayDirection.ENTRY;
        double distance = gatewayOpening.getDistance().onErrorReturnItem(99.0).blockingGet();
        double recommendedDistance = gatewayOpening.getRecommendedAuthenticationDistance().blockingGet();
        boolean isAllowed = GatewayMode.modeAllowsDirection(lockMode, direction);
        boolean isClosest = gatewayOpening == closestGatewayOpening;
        boolean isInRange = distance < recommendedDistance;

        float offsetFactor = isEntry ? -1 : 1;

        canvas.translate(offsetFactor * 2 * gatewayOpeningMargin, 0);

        gatewayOpeningFillPaint.setAlpha(isClosest && isInRange ? 224 : isAllowed ? 96 : 32);
        canvas.drawRect(gatewayOpeningRect, gatewayOpeningFillPaint);

        gatewayOpeningStrokePaint.setColor(requiresAuthentication ? lockedColor : unlockedColor);
        gatewayOpeningStrokePaint.setAlpha(isClosest && isInRange ? 255 : isAllowed ? 128 : 64);
        canvas.drawRect(gatewayOpeningRect, gatewayOpeningStrokePaint);

        String openingText = isEntry ? entryText : exitText;
        textPaint.setColor(gatewayOpeningStrokePaint.getColor());
        canvas.drawText(
                openingText,
                gatewayOpeningWidth / 2,
                ((gatewayOpeningHeight / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)),
                textPaint
        );

        canvas.rotate(offsetFactor * -90);

        String distanceText = String.format(Locale.US, "%.1f m", distance);
        canvas.drawText(
                distanceText,
                -offsetFactor * (gatewayOpeningHeight / 2),
                isEntry ? -gatewayOpeningMargin : gatewayOpeningWidth - gatewayOpeningMargin,
                textPaint
        );

        canvas.restoreToCount(saveCount);
        saveCount = canvas.save();

        canvas.translate(0, 0 - ((gatewayHeight - gatewayOpeningHeight) / 2));
        canvas.translate(0, gatewaySeparatorHeight - gatewayDetectionBeaconHeight);
        if (!isEntry) {
            canvas.translate((gatewaySeparatorWidth / 2) - gatewayDetectionBeaconWidth, 0);
        }

        int leftIndex = 0;
        int rightIndex = 0;
        int index;
        float offset = (-offsetFactor) * (gatewayDetectionBeaconHeight + gatewayOpeningMargin);

        List<GatewayDetectionBeacon> beacons = gatewayOpening.getDetectionBeacons().toList().blockingGet();
        for (GatewayDetectionBeacon beacon : beacons) {
            int saveCount2 = canvas.save();
            boolean isLeftBeacon = beacon.getPosition()
                    .map(position -> position == GatewayDetectionBeacon.LEFT)
                    .blockingGet();

            boolean shouldTranslateToBottom = (isEntry && !isLeftBeacon) || (!isEntry && isLeftBeacon);
            if (shouldTranslateToBottom) {
                canvas.translate(0, gatewayHeight - gatewaySeparatorHeight - (gatewaySeparatorHeight - gatewayDetectionBeaconHeight));
            }

            index = isLeftBeacon ? leftIndex : rightIndex;
            if (index != 0) {
                canvas.translate(index * offset, 0);
            }

            drawGatewayDetectionBeacon(canvas, beacon);
            canvas.restoreToCount(saveCount2);

            if (isLeftBeacon) {
                leftIndex++;
            } else {
                rightIndex++;
            }
        }

        canvas.restoreToCount(saveCount);
    }

    private void drawGatewayDetectionBeacon(Canvas canvas, GatewayDetectionBeacon beacon) {
        int saveCount = canvas.save();

        long maximumTimestamp = System.currentTimeMillis() - 250;
        long minimumTimestamp = maximumTimestamp - TimeUnit.SECONDS.toMillis(2);
        long latestTimestamp = beacon.getLastAdvertisingPacket()
                .flatMapSingle(BaseAdvertisingPacket::getTimestamp)
                .onErrorReturnItem(0L)
                .blockingGet();

        float recencyScore = getRecencyScore(latestTimestamp, minimumTimestamp, maximumTimestamp);

        beaconFillPaint.setColor(bluetoothColor);
        beaconFillPaint.setAlpha(Math.round(255 * recencyScore));
        canvas.drawRect(gatewayDetectionBeaconRect, beaconFillPaint);
        canvas.drawRect(gatewayDetectionBeaconRect, beaconStrokePaint);

        textPaint.setColor(beaconStrokePaint.getColor());
        canvas.drawText(
                String.valueOf(beacon.getMinor().blockingGet()),
                gatewayDetectionBeaconRect.right / 2,
                ((gatewayDetectionBeaconRect.bottom / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)),
                textPaint
        );

        // draw advertising packets
        List<AdvertisingPacket> advertisingPackets = beacon.getRecentAdvertisingPackets()
                .cast(AdvertisingPacket.class)
                .toList()
                .blockingGet();

        for (AdvertisingPacket advertisingPacket : advertisingPackets) {
            drawBeaconAdvertisingPacket(canvas, advertisingPacket);
        }

        canvas.restoreToCount(saveCount);
    }

    private void drawDirectionLockBeacons(Canvas canvas) {
        int saveCount = canvas.save();

        // entry
        List<DirectionLockBeacon> entryLockBeacons = gate.getDirectionLockBeacons()
                .filter(directionLockBeacon -> directionLockBeacon.isForEntry().blockingGet())
                .toList()
                .blockingGet();

        canvas.translate(0, (gateHeight - (entryLockBeacons.size() * (directionLockBeaconHeight))) / 2);
        for (DirectionLockBeacon directionLockBeacon : entryLockBeacons) {
            drawGatewayDirectionLockBeacon(canvas, directionLockBeacon);

            // draw advertising packets
            List<AdvertisingPacket> advertisingPackets = directionLockBeacon.getRecentAdvertisingPackets()
                    .cast(AdvertisingPacket.class)
                    .toList()
                    .blockingGet();

            for (AdvertisingPacket advertisingPacket : advertisingPackets) {
                drawBeaconAdvertisingPacket(canvas, advertisingPacket);
            }

            canvas.translate(0, directionLockBeaconHeight);
        }

        canvas.restoreToCount(saveCount);
        saveCount = canvas.save();

        canvas.translate(gateWidth - directionLockBeaconWidth, 0);

        // exit
        List<DirectionLockBeacon> exitLockBeacons = gate.getDirectionLockBeacons()
                .filter(directionLockBeacon -> directionLockBeacon.isForExit().blockingGet())
                .toList()
                .blockingGet();

        canvas.translate(0, (gateHeight - (exitLockBeacons.size() * (directionLockBeaconHeight))) / 2);
        for (DirectionLockBeacon directionLockBeacon : exitLockBeacons) {
            drawGatewayDirectionLockBeacon(canvas, directionLockBeacon);

            // draw advertising packets
            List<AdvertisingPacket> advertisingPackets = directionLockBeacon.getRecentAdvertisingPackets()
                    .cast(AdvertisingPacket.class)
                    .toList()
                    .blockingGet();

            for (AdvertisingPacket advertisingPacket : advertisingPackets) {
                drawBeaconAdvertisingPacket(canvas, advertisingPacket);
            }

            canvas.translate(0, directionLockBeaconHeight);
        }

        canvas.restoreToCount(saveCount);
    }

    private void drawGatewayDirectionLockBeacon(Canvas canvas, DirectionLockBeacon beacon) {
        int saveCount = canvas.save();

        long maximumTimestamp = System.currentTimeMillis() - 250;
        long minimumTimestamp = maximumTimestamp - TimeUnit.SECONDS.toMillis(2);
        long latestTimestamp = beacon.getLastAdvertisingPacket()
                .flatMapSingle(BaseAdvertisingPacket::getTimestamp)
                .onErrorReturnItem(0L)
                .blockingGet();

        float recencyScore = getRecencyScore(latestTimestamp, minimumTimestamp, maximumTimestamp);

        beaconFillPaint.setColor(bluetoothColor);
        beaconFillPaint.setAlpha(Math.round(255 * recencyScore));
        canvas.drawRect(directionLockBeaconRect, beaconFillPaint);
        canvas.drawRect(directionLockBeaconRect, beaconStrokePaint);

        textPaint.setColor(beaconStrokePaint.getColor());
        canvas.drawText(
                String.valueOf(beacon.getMinor().blockingGet()),
                directionLockBeaconWidth / 2,
                ((directionLockBeaconHeight / 2) - ((textPaint.descent() + textPaint.ascent()) / 2)),
                textPaint
        );

        canvas.restoreToCount(saveCount);
    }

    private void drawBeaconAdvertisingPacket(Canvas canvas, AdvertisingPacket advertisingPacket) {
        long maximumTimestamp = System.currentTimeMillis();
        long minimumTimestamp = maximumTimestamp - TimeUnit.SECONDS.toMillis(2);
        float recencyScore = getRecencyScore(advertisingPacket.getTimestamp().blockingGet(), minimumTimestamp, maximumTimestamp);

        advertisingPacketPaint.setAlpha(Math.round(128 * recencyScore));

        canvas.drawCircle(
                directionLockBeaconWidth / 2,
                directionLockBeaconHeight / 2,
                (directionLockBeaconWidth / 2) + ((1 - recencyScore) * directionLockBeaconWidth * 3),
                advertisingPacketPaint
        );
    }

    @Override
    public void onCommunicationUnitUpdated(@NonNull CommunicationUnit communicationUnit) {
        if (!(communicationUnit instanceof Gate)) {
            throw new IllegalArgumentException("Communication Unit is not a gate");
        }
        onCommunicationUnitUpdated((Gate) communicationUnit);
    }

    @Override
    public void onCommunicationUnitUpdated(@NonNull Gate gate) {
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
    public void onDirectionLockBeaconUpdated(@NonNull DirectionLockBeacon gatewayDetectionBeacon) {
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
