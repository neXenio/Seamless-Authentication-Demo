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
    implementation 'com.nexenio.seamlessauthentication:core:0.2.1'
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

The library concept consists of two main interfaces. The `SeamlessAuthenticator` represents a physical device that could authenticate the current user (e.g. an access control gate). In order to detect these devices, you need a `SeamlessAuthenticatorDetector` (e.g. a smartphone using Bluetooth).

Please avoid any imports from the `internal` package, as these are subject to change without notice or deprecation warnings.

All interfaces and the internal implementation heavily relies on [RxJava](https://github.com/ReactiveX/RxJava).

### Create an Authenticator Detector

You should obtain a detector instance through the `SeamlessAuthentication` Singleton, which is the entrypoint to the library. You will need to pass a `Conext`, which you may obtain from a `Fragment`, `Activity`, `Application` or `Service` of your app. It's your responsibility to **only hold a single instance** of the created detector!

```java
SeamlessAuthenticatorDetector authenticatorDetector = SeamlessAuthentication.createDetector(this);
```

### Detect an Authenticator

In order to actually detect nearby communicationUnits, subscribe to the `detect()` method. It will never complete and you can safely subscribe to it multiple times. The detection will stop when the last subscription gets disposed.

```java
authenticatorDetector.detect()
        .subscribeOn(Schedulers.io())
        .subscribe(
                seamlessAuthenticator -> {
                    Timber.d("Seamless authenticator detected: %s", seamlessAuthenticator);
                },
                throwable -> {
                    Timber.w(throwable, "Unable to detect seamless communicationUnits");
                }
        );
```

Please be aware that the `detect()` method may emit `SeamlessAuthenticator` instances frequently, possibly also the same instance multiple times.

In order to get all currently detected communicationUnits (each instance only once), use the `getDetectedAuthenticators()` method.

If you only care about the closest authenticator, use the `getClosestAuthenticator()` method.

Keep in mind that you always need to have an active subscription to the `detect()` method for these methods to work.

### Distance Estimation

Each detected `SeamlessAuthenticator` instance has an `AuthenticatorDistanceProvider` that you can get by using `seamlessAuthenticator.getDistanceProvider()`. It can be used to get the current distance between the `SeamlessAuthenticator` and the `SeamlessAuthenticatorDetector` (`distanceProvider.getDistance()`) as well as to subscribe to distance updates (`distanceProvider.getDistances()`).

You can overwrite the default provider with a custom implementation (e.g. if you have an indoor positioning system) using `seamlessAuthenticator.setDistanceProvider(distanceProvider)`.

### Initiate an Authentication

To initiate an authentication, you need to provide an `AuthenticationProperties` object. This object contains details about the current user and device.

#### Anticipation

Before actually initiating the authentication, you should use `seamlessAuthenticator.anticipateAuthentication(authenticationProperties)`. This will prepare the authentication as far as possible by already exchanging the data required for the authentication with the authenticator. Although this is not required, it will reduce the time needed for authenticating and thus improve the user experience.

#### Authentication

In order to authenticate, use `seamlessAuthenticator.authenticate(authenticationProperties)`. The returned `Completable` will complete when the authentication succeeded, or emit an error otherwise.





