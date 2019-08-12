package com.nexenio.seamlessauthenticationintegrationsample.visualization;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.nexenio.seamlessauthentication.SeamlessAuthenticator;
import com.nexenio.seamlessauthentication.accesscontrol.gate.Gate;
import com.nexenio.seamlessauthentication.accesscontrol.gateway.Gateway;
import com.nexenio.seamlessauthentication.accesscontrol.gateway.GatewayDirection;
import com.nexenio.seamlessauthentication.accesscontrol.gateway.opening.GatewayOpening;
import com.nexenio.seamlessauthentication.internal.accesscontrol.beacons.detection.GatewayDetectionBeacon;
import com.nexenio.seamlessauthentication.internal.accesscontrol.beacons.lock.GatewayDirectionLockBeacon;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class GateView extends View implements GateVisualization {

    private Gate gate;

    private float pixelsPerDp;

    private float gatewayWidth;
    private float gatewayHeight;

    private float gatewaySeparatorWidth;
    private float gatewaySeparatorHeight;

    private float gatewayOpeningWidth;
    private float gatewayOpeningHeight;

    private float gatewayDetectionBeaconWidth;
    private float gatewayDetectionBeaconHeight;

    private int backgroundColor;
    private int foregroundColor;

    private Paint backgroundPaint;
    private Paint StrokePaint;

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

        gatewayWidth = 300 * pixelsPerDp;
        gatewayHeight = 150 * pixelsPerDp;

        gatewaySeparatorWidth = gatewayWidth;
        gatewaySeparatorHeight = 20 * pixelsPerDp;

        gatewayOpeningWidth = 0.8f * (gatewayWidth / 2);
        gatewayOpeningHeight = 0.8f * (gatewayHeight - (2 * gatewaySeparatorHeight));

        gatewayDetectionBeaconWidth = gatewaySeparatorHeight / 2;
        gatewayDetectionBeaconHeight = gatewayDetectionBeaconWidth;

        backgroundColor = Color.WHITE;
        foregroundColor = Color.BLACK;

        backgroundPaint = new Paint();
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(backgroundColor);

        StrokePaint = new Paint();
        StrokePaint.setStyle(Paint.Style.STROKE);
        StrokePaint.setStrokeWidth(1 * pixelsPerDp);
        StrokePaint.setAntiAlias(true);
        StrokePaint.setColor(foregroundColor);
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

        canvas.drawRect(0, 0, getWidth(), getHeight(), backgroundPaint);

        if (gate == null) {
            return;
        }

        int saveCount = canvas.save();
        List<Gateway> gateways = gate.getGateways().toList().blockingGet();
        for (Gateway gateway : gateways) {
            drawGateway(canvas, gateway);
            canvas.translate(0, gatewayHeight);
        }
        canvas.restoreToCount(saveCount);

        invalidate();
    }

    private void drawGateway(Canvas canvas, Gateway gateway) {
        int saveCount = canvas.save();

        canvas.drawRect(0, 0, gatewayWidth, gatewayHeight, StrokePaint);




        canvas.translate(0, (gatewayHeight - gatewayOpeningHeight) / 2);

        List<GatewayOpening> openings = gateway.getOpenings().toList().blockingGet();
        for (GatewayOpening opening : openings) {
            int saveCount2 = canvas.save();
            boolean isEntry = opening.getDirection().blockingGet() == GatewayDirection.ENTRY;
            if (!isEntry) {
                canvas.scale(-1, 1, gatewayWidth / 2, gatewayHeight / 2);
                //canvas.translate(gatewayWidth - gatewayOpeningWidth, 0);
            }
            drawGatewayOpening(canvas, opening);
            canvas.restoreToCount(saveCount2);
        }
        canvas.restoreToCount(saveCount);
    }

    private void drawGatewayOpening(Canvas canvas, GatewayOpening gatewayOpening) {
        int saveCount = canvas.save();

        canvas.drawRect(0, 0, gatewayOpeningWidth, gatewayOpeningHeight, StrokePaint);

        List<GatewayDetectionBeacon> beacons = gatewayOpening.getDetectionBeacons().toList().blockingGet();
        for (GatewayDetectionBeacon beacon : beacons) {

            boolean isLeft = beacon.getPosition() == GatewayDetectionBeacon.LEFT;
            if (!isLeft) {
                canvas.translate(0, gatewayOpeningHeight - gatewayDetectionBeaconHeight);
            }

            drawGatewayDetectionBeacon(canvas, beacon);
        }
        canvas.restoreToCount(saveCount);
    }

    private void drawGatewayDetectionBeacon(Canvas canvas, GatewayDetectionBeacon beacon) {
        int saveCount = canvas.save();
        canvas.drawRect(0, 0, gatewayDetectionBeaconWidth, gatewayDetectionBeaconHeight, StrokePaint);
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

}
