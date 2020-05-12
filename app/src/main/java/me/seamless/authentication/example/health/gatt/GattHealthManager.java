package me.seamless.authentication.example.health.gatt;

import android.content.Context;

import com.nexenio.rxandroidbleserver.RxBleServer;
import com.nexenio.rxandroidbleserver.RxBleServerProvider;
import com.nexenio.rxandroidbleserver.client.RxBleClient;
import com.nexenio.rxandroidbleserver.service.RxBleService;
import com.nexenio.rxandroidbleserver.service.ServiceBuilder;
import com.nexenio.rxandroidbleserver.service.characteristic.CharacteristicBuilder;
import com.nexenio.rxandroidbleserver.service.characteristic.RxBleCharacteristic;
import com.nexenio.rxandroidbleserver.service.characteristic.descriptor.CharacteristicUserDescription;
import com.nexenio.rxandroidbleserver.service.value.RxBleValue;
import com.nexenio.rxandroidbleserver.service.value.provider.RxBleClientValueProvider;
import me.seamless.authentication.example.health.HealthCheckResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import androidx.annotation.NonNull;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class GattHealthManager {

    private static final UUID HEALTH_SERVICE_UUID = UUID.fromString("ea64c235-6340-4e00-8d6b-3ccd0dbf9b2d");
    private static final UUID COMMUNICATION_UNIT_ID_UUID = UUID.fromString("44c0f704-d6fb-46a5-b3f3-86dcb2479dae");
    private static final UUID OPERATIONAL_CHIPS_UUID = UUID.fromString("c664d698-e3e6-4555-bfac-d44488cbe279");
    private static final UUID ACTIVE_DEVICES_UUID = UUID.fromString("09442147-5ec0-4341-a382-58f43a6f13e5");

    private final UUID communicationUnitId;

    private RxBleServer server;

    private RxBleCharacteristic communicationUnitIdCharacteristic;
    private RxBleCharacteristic operationalChipsCharacteristic;
    private RxBleCharacteristic activeDevicesCharacteristic;

    public GattHealthManager(UUID communicationUnitId) {
        this.communicationUnitId = communicationUnitId;
    }

    public Completable initialize(@NonNull Context context) {
        return initializeServer(context);
    }

    private Completable initializeServer(@NonNull Context context) {
        Single<RxBleService> createService = Single.fromCallable(() -> {
            byte[] encodedCommunicationUnitId = communicationUnitId.toString().getBytes(StandardCharsets.UTF_8);

            communicationUnitIdCharacteristic = new CharacteristicBuilder(COMMUNICATION_UNIT_ID_UUID)
                    .withInitialValue(encodedCommunicationUnitId)
                    .withDescriptor(new CharacteristicUserDescription("Communication Unit ID"))
                    .allowRead()
                    .build();

            operationalChipsCharacteristic = new CharacteristicBuilder(OPERATIONAL_CHIPS_UUID)
                    .withDescriptor(new CharacteristicUserDescription("Operational BT Chips"))
                    .allowWrite()
                    .supportWritesWithoutResponse()
                    .build();

            activeDevicesCharacteristic = new CharacteristicBuilder(ACTIVE_DEVICES_UUID)
                    .withDescriptor(new CharacteristicUserDescription("Active Devices"))
                    .allowWrite()
                    .supportWritesWithoutResponse()
                    .build();

            return new ServiceBuilder(HEALTH_SERVICE_UUID)
                    .withCharacteristic(communicationUnitIdCharacteristic)
                    .withCharacteristic(operationalChipsCharacteristic)
                    .withCharacteristic(activeDevicesCharacteristic)
                    .isPrimaryService()
                    .build();
        });

        return createService.flatMapCompletable(service -> Completable.defer(() -> {
            server = RxBleServerProvider.createServer(context);
            return server.addService(service);
        }));
    }

    public Single<HealthCheckResult> getHealthCheckResult() {
        return Single.create(healthCheckResultEmitter -> {

            Completable provideService = server.provideServicesAndAdvertise(HEALTH_SERVICE_UUID)
                    .doOnSubscribe(disposable -> Timber.d("GATT server started"))
                    .doFinally(() -> Timber.d("GATT server stopped"))
                    .subscribeOn(Schedulers.io());

            Completable waitForDisconnections = server.observerClientConnectionStateChanges()
                    .filter(client -> !client.isConnected())
                    .doOnNext(client -> Timber.d("Client disconnected, reading health check data: %s", client))
                    .flatMapMaybe(this::getHealthCheckResult)
                    .doOnNext(healthCheckResultEmitter::onSuccess)
                    .firstOrError()
                    .ignoreElement()
                    .subscribeOn(Schedulers.io());

            Disposable getHealthCheckResultsDisposable = Completable.mergeArray(provideService, waitForDisconnections)
                    .subscribeOn(Schedulers.io())
                    .subscribe(
                            () -> Timber.d("Stopped waiting for health check results"),
                            healthCheckResultEmitter::onError
                    );

            healthCheckResultEmitter.setDisposable(getHealthCheckResultsDisposable);
        });
    }

    private Maybe<HealthCheckResult> getHealthCheckResult(@NonNull RxBleClient client) {
        return Single.just(new HealthCheckResult())
                .flatMap(healthCheckResult -> Completable.mergeArray(
                        getOperationalBluetoothChips(client)
                                .doOnSuccess(healthCheckResult::setOperationalBluetoothChips)
                                .ignoreElement(),
                        getActiveDevices(client)
                                .doOnSuccess(healthCheckResult::setActiveDevices)
                                .ignoreElement()
                ).toSingleDefault(healthCheckResult))
                .toMaybe()
                .doOnError(throwable -> Timber.w(throwable, "Unable to read health check values for %s", client))
                .onErrorComplete();
    }

    private Single<Integer> getOperationalBluetoothChips(@NonNull RxBleClient client) {
        return getValueAsInteger(client, operationalChipsCharacteristic);
    }

    private Single<Integer> getActiveDevices(@NonNull RxBleClient client) {
        return getValueAsInteger(client, activeDevicesCharacteristic);
    }

    private Single<Integer> getValueAsInteger(@NonNull RxBleClient client, @NonNull RxBleClientValueProvider valueProvider) {
        return valueProvider.getValue(client)
                .map(RxBleValue::getBytes)
                .filter(bytes -> bytes.length == 1)
                .toSingle()
                .map(bytes -> (int) bytes[0]);
    }

}
