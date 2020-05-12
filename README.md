Seamless Authentication Demo
============================

This repo contains a dummy app integrating the seamless authentication library.

![Header Image](https://raw.githubusercontent.com/neXenio/Seamless-Authentication-Demo/master/media/header.jpg)

## Resolving Dependencies

The library will be served through neXenio's public artifacory, reachable at `https://artifactory.nexenio.com`. It has dependencies on other libraries, some of which are served through JitPack.

```gradle
repositories {
    maven {
        url "https://artifactory.nexenio.com/artifactory/${nexenio_artifactory_repository}/"
        credentials { 
            username "${nexenio_artifactory_user}" 
            password "${nexenio_artifactory_password}"
        }
    }
    maven {
        url 'https://jitpack.io'
    }
}

dependencies {
    implementation 'me.seamless.authentication:core:0.15.1'
}
```

You should extend your gobal `build.gradle` file with the following properties:

```gradle
# neXenio Artifactory
nexenio_artifactory_repository=PLACEHOLDER
nexenio_artifactory_user=PLACEHOLDER
nexenio_artifactory_password=PLACEHOLDER
```

## Getting Started

The library concept consists of two main interfaces. The `CommunicationUnit` represents a physical device that the current user could authenticate at (e.g. an access control gate). In order to detect these devices, you need a `CommunicationUnitDetector`.

Please avoid any imports from the `internal` package, as these are subject to change without notice or deprecation warnings.

All interfaces and the internal implementation heavily relies on [RxJava](https://github.com/ReactiveX/RxJava).

### Get a Communication Unit Detector

Start by initializing the `SeamlessAuthentication` Singleton, which is the entrypoint to the library. You will need to pass a `Conext`, which you may obtain from a `Fragment`, `Activity`, `Application` or `Service` of your app.

```java
SeamlessAuthentication seamlessAuthentication = SeamlessAuthentication.getInstance();
seamlessAuthentication.initialize(this)
        .subscribe(
                () -> Timber.d("Seamless authentication initialized"),
                throwable -> Timber.e(throwable, "Unable to initialize seamless authentication")
        );
```

After a successful initialization, you can optain a `CommunicationUnitDetector` instance:

```java
CommunicationUnitDetector communicationUnitDetector = seamlessAuthentication.getCommunicationUnitDetector();
```

### Detect an Authenticator

In order to actually detect nearby communication units, subscribe to the `detect()` method. It will never complete and you can safely subscribe to it multiple times. The detection will stop when the last subscription gets disposed.

```java
communicationUnitDetector.detect()
        .subscribeOn(Schedulers.io())
        .subscribe(
                communicationUnit -> {
                    Timber.d("Communication unit detected: %s", communicationUnit);
                },
                throwable -> {
                    Timber.w(throwable, "Unable to detect communication units");
                }
        );
```

Please be aware that the `detect()` method may emit `CommunicationUnit` instances frequently, possibly also the same instance multiple times.

In order to get all currently detected communication units (each instance only once), use the `getCurrentlyDetectedCommunicationUnits()` method.

If you only care about the closest communication unit, use the `getClosestCommunicationUnit()` method. You can also subscribe to changes using `getClosestCommunicationUnitChanges()`.

Keep in mind that you always need to have an active subscription to the `detect()` method for these methods to work.

### Distance Estimation

Each detected `CommunicationUnit` instance has an `CommunicationUnitDistanceProvider` that you can get by using `communicationUnit.getDistanceProvider()`. It can be used to get the current distance between the `CommunicationUnit` and the `CommunicationUnitDetector` (`distanceProvider.getDistance()`) as well as to subscribe to distance updates (`distanceProvider.getDistances()`).

You can overwrite the default provider with a custom implementation (e.g. if you have an indoor positioning system) using `communicationUnit.setDistanceProvider(distanceProvider)`.

### Initiate an Authentication

To initiate an authentication, you need to provide an `AuthenticationProperties` object. This object contains details about the current user and device, as well as optional additional data.

#### Anticipation

Before actually initiating the authentication, you should use `communicationUnit.anticipateAuthentication(authenticationProperties)`. This will prepare the authentication as far as possible by already establishing a communication channel and exchanging the data required for the authentication with the authenticator. Although this is not required, it will reduce the time needed for authenticating and thus improve the user experience.

#### Authentication

In order to authenticate, use `communicationUnit.authenticate(authenticationProperties)`. The returned `Completable` will complete when the authentication succeeded, or emit an error otherwise.
