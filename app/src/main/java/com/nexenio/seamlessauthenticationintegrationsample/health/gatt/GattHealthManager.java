package com.nexenio.seamlessauthenticationintegrationsample.health.gatt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;

import com.nexenio.sblec.internal.sender.advertiser.AdvertiserException;
import com.nexenio.seamlessauthenticationintegrationsample.health.HealthCheckResult;

import java.util.UUID;

import androidx.annotation.NonNull;
import io.reactivex.Completable;
import io.reactivex.Single;
import timber.log.Timber;

public class GattHealthManager {

    private static final UUID HEALTH_SERVICE_UUID = UUID.fromString("ea64c235-6340-4e00-8d6b-3ccd0dbf9b2d");
    private static final UUID OPERATIONAL_CHIPS_UUID = UUID.fromString("c664d698-e3e6-4555-bfac-d44488cbe279");
    private static final UUID ACTIVE_DEVICES_UUID = UUID.fromString("09442147-5ec0-4341-a382-58f43a6f13e5");

    private Context context;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private final UUID authenticatorId;

    public GattHealthManager(UUID authenticatorId) {
        this.authenticatorId = authenticatorId;
    }

    public Completable initialize(@NonNull Context context) {
        return Completable.fromAction(() -> {
            this.context = context;

            bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                throw new IllegalStateException("No bluetooth manager available");
            }

            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter == null) {
                throw new IllegalStateException("No bluetooth adapter available");
            }
        });
    }

    public Single<HealthCheckResult> getHealthCheckResult() {
        return Single.never();
    }

    private Completable advertiseHealthService() {
        return Completable.create(emitter -> {
            BluetoothLeAdvertiser advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            if (advertiser == null) {
                emitter.onError(new AdvertiserException("No BLE advertiser available"));
                return;
            }

            AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
                @Override
                public void onStartFailure(int errorCode) {
                    super.onStartFailure(errorCode);
                    emitter.onError(new AdvertiserException("Unable to start advertising. Error code: " + errorCode));
                }
            };

            AdvertiseSettings settings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                    .setConnectable(true)
                    .setTimeout(0)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                    .build();

            AdvertiseData data = new AdvertiseData.Builder()
                    .setIncludeDeviceName(true)
                    .setIncludeTxPowerLevel(false)
                    .addServiceUuid(new ParcelUuid(HEALTH_SERVICE_UUID))
                    .build();

            advertiser.startAdvertising(settings, data, advertiseCallback);

            emitter.setCancellable(() -> advertiser.stopAdvertising(advertiseCallback));
        });
    }

    private Completable provideGattServer() {
        return Completable.create(emitter -> {
            BluetoothGattServerCallback callback = createGattServiceCallback();
            BluetoothGattServer gattServer = bluetoothManager.openGattServer(context, callback);
            if (gattServer == null) {
                emitter.onError(new IllegalStateException("Unable to open GATT server"));
                return;
            }

            BluetoothGattService service = new BluetoothGattService(HEALTH_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
            BluetoothGattCharacteristic operationalChipsCharacteristic = new BluetoothGattCharacteristic(OPERATIONAL_CHIPS_UUID, BluetoothGattCharacteristic.FORMAT_UINT8, BluetoothGattCharacteristic.PERMISSION_WRITE);
            BluetoothGattCharacteristic activeDevicesCharacteristic = new BluetoothGattCharacteristic(ACTIVE_DEVICES_UUID, BluetoothGattCharacteristic.FORMAT_UINT8, BluetoothGattCharacteristic.PERMISSION_WRITE);

            service.addCharacteristic(operationalChipsCharacteristic);
            service.addCharacteristic(activeDevicesCharacteristic);
            gattServer.addService(service);

            emitter.setCancellable(gattServer::close);
        });
    }

    private BluetoothGattServerCallback createGattServiceCallback() {
        return new BluetoothGattServerCallback() {
            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                Timber.d("onConnectionStateChange() called with: device = [%s], status = [%s], newState = [%s]", device, status, newState);
                super.onConnectionStateChange(device, status, newState);
            }

            @Override
            public void onServiceAdded(int status, BluetoothGattService service) {
                Timber.d("onServiceAdded() called with: status = [%s], service = [%s]", status, service);
                super.onServiceAdded(status, service);
            }

            @Override
            public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                Timber.d("onCharacteristicReadRequest() called with: device = [%s], requestId = [%s], offset = [%s], characteristic = [%s]", device, requestId, offset, characteristic);
                super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            }

            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                Timber.d("onCharacteristicWriteRequest() called with: device = [%s], requestId = [%s], characteristic = [%s], preparedWrite = [%s], responseNeeded = [%s], offset = [%s], value = [%s]", device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            }

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                Timber.d("onDescriptorReadRequest() called with: device = [%s], requestId = [%s], offset = [%s], descriptor = [%s]", device, requestId, offset, descriptor);
                super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            }

            @Override
            public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                Timber.d("onDescriptorWriteRequest() called with: device = [%s], requestId = [%s], descriptor = [%s], preparedWrite = [%s], responseNeeded = [%s], offset = [%s], value = [%s]", device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
                super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            }

            @Override
            public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
                Timber.d("onExecuteWrite() called with: device = [%s], requestId = [%s], execute = [%s]", device, requestId, execute);
                super.onExecuteWrite(device, requestId, execute);
            }
        };
    }

}
