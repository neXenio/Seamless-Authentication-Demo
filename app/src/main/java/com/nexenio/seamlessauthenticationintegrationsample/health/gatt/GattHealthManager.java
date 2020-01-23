package com.nexenio.seamlessauthenticationintegrationsample.health.gatt;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.ParcelUuid;

import com.nexenio.sblec.internal.sender.advertiser.AdvertiserException;
import com.nexenio.seamlessauthenticationintegrationsample.health.HealthCheckResult;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

import androidx.annotation.NonNull;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class GattHealthManager {

    private static final UUID HEALTH_SERVICE_UUID = UUID.fromString("ea64c235-6340-4e00-8d6b-3ccd0dbf9b2d");
    private static final UUID AUTHENTICATOR_ID_UUID = UUID.fromString("44c0f704-d6fb-46a5-b3f3-86dcb2479dae");
    private static final UUID OPERATIONAL_CHIPS_UUID = UUID.fromString("c664d698-e3e6-4555-bfac-d44488cbe279");
    private static final UUID ACTIVE_DEVICES_UUID = UUID.fromString("09442147-5ec0-4341-a382-58f43a6f13e5");

    private static final UUID USER_DESCRIPTION_UUID = UUID.fromString("00002901-0000-1000-8000-00805f9b34fb");

    private final UUID authenticatorId;

    private Context context;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGattServer gattServer;

    private SingleEmitter<HealthCheckResult> healthCheckResultEmitter;

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
        return Single.create(emitter -> {
            Disposable advertisingDisposable = advertiseHealthService()
                    .doOnSubscribe(disposable -> Timber.d("GATT health check service advertising started"))
                    .doFinally(() -> Timber.d("GATT health check service advertising stopped"))
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            () -> {
                            },
                            emitter::onError
                    );

            Disposable serverDisposable = provideGattServer()
                    .doOnSubscribe(disposable -> Timber.d("GATT server started"))
                    .doFinally(() -> Timber.d("GATT server stopped"))
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            () -> {
                            },
                            emitter::onError
                    );

            emitter.setDisposable(new CompositeDisposable(advertisingDisposable, serverDisposable));
            this.healthCheckResultEmitter = emitter;
        });
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
            gattServer = bluetoothManager.openGattServer(context, callback);
            if (gattServer == null) {
                emitter.onError(new IllegalStateException("Unable to open GATT server"));
                return;
            }

            BluetoothGattService service = new BluetoothGattService(HEALTH_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

            BluetoothGattCharacteristic authenticatorIdCharacteristic = new BluetoothGattCharacteristic(AUTHENTICATOR_ID_UUID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
            authenticatorIdCharacteristic.setValue(authenticatorId.toString());
            authenticatorIdCharacteristic.addDescriptor(createDescriptionDescriptor("Authenticator ID"));
            service.addCharacteristic(authenticatorIdCharacteristic);

            BluetoothGattCharacteristic operationalChipsCharacteristic = new BluetoothGattCharacteristic(OPERATIONAL_CHIPS_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
            operationalChipsCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            operationalChipsCharacteristic.setValue(0, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            operationalChipsCharacteristic.addDescriptor(createDescriptionDescriptor("Operational BT Chips"));
            service.addCharacteristic(operationalChipsCharacteristic);

            BluetoothGattCharacteristic activeDevicesCharacteristic = new BluetoothGattCharacteristic(ACTIVE_DEVICES_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE, BluetoothGattCharacteristic.PERMISSION_WRITE);
            activeDevicesCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            activeDevicesCharacteristic.setValue(0, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            activeDevicesCharacteristic.addDescriptor(createDescriptionDescriptor("Active Devices"));
            service.addCharacteristic(activeDevicesCharacteristic);

            gattServer.addService(service);

            emitter.setCancellable(gattServer::close);
        });
    }

    private BluetoothGattDescriptor createDescriptionDescriptor(@NonNull String description) {
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(USER_DESCRIPTION_UUID, BluetoothGattDescriptor.PERMISSION_READ);
        descriptor.setValue(description.getBytes(StandardCharsets.UTF_8));
        return descriptor;
    }

    private BluetoothGattServerCallback createGattServiceCallback() {
        return new BluetoothGattServerCallback() {

            private boolean hasSetOperationalChips = false;
            private boolean hasSetActiveDevices = false;
            private HealthCheckResult healthCheckResult = new HealthCheckResult();

            @Override
            public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                Timber.d("onConnectionStateChange() called with: device = [%s], status = [%s], newState = [%s]", device, status, newState);
                super.onConnectionStateChange(device, status, newState);
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    if (hasSetOperationalChips && hasSetActiveDevices) {
                        if (healthCheckResultEmitter != null && !healthCheckResultEmitter.isDisposed()) {
                            healthCheckResultEmitter.onSuccess(healthCheckResult);
                        }
                    }
                }
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
                byte[] value = Arrays.copyOfRange(characteristic.getValue(), offset, characteristic.getValue().length);
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
            }

            @Override
            public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                Timber.d("onCharacteristicWriteRequest() called with: device = [%s], requestId = [%s], characteristic = [%s], preparedWrite = [%s], responseNeeded = [%s], offset = [%s], value = [%s]", device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
                super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
                if (characteristic.getUuid().equals(OPERATIONAL_CHIPS_UUID)) {
                    hasSetOperationalChips = true;
                    healthCheckResult.setOperationalBluetoothChips(ByteBuffer.wrap(value).get());
                    Timber.d("Operational Bluetooth chips set to %d", healthCheckResult.getOperationalBluetoothChips());
                } else if (characteristic.getUuid().equals(ACTIVE_DEVICES_UUID)) {
                    hasSetActiveDevices = true;
                    healthCheckResult.setActiveDevices(ByteBuffer.wrap(value).get());
                    Timber.d("Active devices set to %d", healthCheckResult.getActiveDevices());
                }
                if (responseNeeded) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                }
            }

            @Override
            public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
                Timber.d("onDescriptorReadRequest() called with: device = [%s], requestId = [%s], offset = [%s], descriptor = [%s]", device, requestId, offset, descriptor);
                super.onDescriptorReadRequest(device, requestId, offset, descriptor);
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.getValue());
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
