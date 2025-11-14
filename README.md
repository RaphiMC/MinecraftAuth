# MinecraftAuth
Simple and easy to use Minecraft microsoft authentication library (Java and Bedrock).

## Features
- Full support for Minecraft: Java Edition and Minecraft: Bedrock Edition
- Login using device code, credentials, a JavaFX WebView window or a local webserver
- Token lifecycle management (Automatic refreshing of tokens)
- Serializing and deserializing tokens to and from json
- Customizable login flows (Client ID, scopes, ...)
- Basic implementation of the Minecraft Realms API (Allows listing and joining realms)

## Releases
### Gradle/Maven
To use MinecraftAuth with Gradle/Maven you can get it from [Maven Central](https://mvnrepository.com/artifact/net.raphimc/MinecraftAuth), [Lenni0451's Maven](https://maven.lenni0451.net/#/releases/net/raphimc/MinecraftAuth) or [Jitpack](https://jitpack.io/#RaphiMC/MinecraftAuth).
You can also find instructions how to implement it into your build script there.

### Jar File
If you just want the latest jar file you can download it from [GitHub Actions](https://github.com/RaphiMC/MinecraftAuth/actions/workflows/build.yml) or [Lenni0451's Jenkins](https://build.lenni0451.net/job/MinecraftAuth/).

## Usage
MinecraftAuth provides its core functionality through the ``JavaAuthManager`` and ``BedrockAuthManager`` classes.  
These classes provide predefined and customizable login flows for their respective Minecraft editions.

### Getting started
To get started create an ``HttpClient`` which will be used for all network requests:
```java
// Create an HttpClient with a custom user agent (recommended)
HttpClient httpClient = MinecraftAuth.createHttpClient(userAgent);

// Or you can create an HttpClient without a custom user agent (not recommended)
HttpClient httpClient = MinecraftAuth.createHttpClient();
```

### Configure the authentication manager
The next step is to configure the authentication manager (Example is for Minecraft: Java Edition, but Minecraft: Bedrock Edition is pretty much the same):
```java
// Use predefined application configuration (Uses the official Minecraft application details)
JavaAuthManager.Builder authManagerBuilder = JavaAuthManager.create(httpClient);

// If you want to customize the application details (like client id, scope or client secret) you can do it like this:
JavaAuthManager.Builder authManagerBuilder = JavaAuthManager.create(httpClient).msaApplicationConfig(new MsaApplicationConfig(...));
```

### Logging in
The next step is to choose one of the login flows and initiate the login process.

#### Log in using device code (Recommended)
The device code auth flow blocks the thread until the user has logged in and throws an exception if the process times out.
The default timeout is 5 minutes.
```java
JavaAuthManager authManager = authManagerBuilder.login(DeviceCodeMsaAuthService::new, new Consumer<MsaDeviceCode>() {
    @Override
    public void accept(MsaDeviceCode deviceCode) {
        // Method to generate a verification URL and a code for the user to enter on that page
        System.out.println("Go to " + deviceCode.getVerificationUri());
        System.out.println("Enter code " + deviceCode.getUserCode());

        // There is also a method to generate a direct URL without needing the user to enter a code
        System.out.println("Go to " + deviceCode.getDirectVerificationUri());
    }
});
System.out.println("Username: " + authManager.getMinecraftProfile().getUpToDate().getName());
System.out.println("Access token: " + authManager.getMinecraftToken().getUpToDate().getToken());
```

#### Log in using credentials
The credentials auth flow does not handle 2FA and will throw an exception if the user has 2FA enabled. You should consider using the device code auth flow instead if you want to support 2FA.
```java
JavaAuthManager authManager = authManagerBuilder.login(CredentialsMsaAuthService::new, new MsaCredentials("email@test.com", "P4ssw0rd"));
System.out.println("Username: " + authManager.getMinecraftProfile().getUpToDate().getName());
System.out.println("Access token: " + authManager.getMinecraftToken().getUpToDate().getToken());
```

#### Log in using JavaFX WebView
The JavaFX WebView auth flow opens a JavaFX window with a WebView for the user to log in.
This method requires you to have JavaFX set up in your project.
```java
JavaAuthManager authManager = authManagerBuilder.login(JfxWebViewMsaAuthService::new);
```

#### Advanced: Log in with custom MSA auth service
If you want to implement your own MSA auth service (for example to implement a different login flow) or customize one of the existing ones you can do it like this:
```java
// Create a customized MSA auth service (Example: Change login timeout to 60 seconds)
DeviceCodeMsaAuthService authService = new DeviceCodeMsaAuthService(MinecraftAuth.createHttpClient(), new MsaApplicationConfig(MsaConstants.JAVA_TITLE_ID, MsaConstants.SCOPE_TITLE_AUTH), new Consumer<MsaDeviceCode>() {
    @Override
    public void accept(MsaDeviceCode deviceCode) {
        System.out.println("Go to " + deviceCode.getDirectVerificationUri());
    }
}, 60_000);

// Acquire the MSA token
MsaToken msaToken = authService.acquireToken();

// Pass it to the auth manager
JavaAuthManager authManager = JavaAuthManager.create(MinecraftAuth.createHttpClient()).login(msaToken);
```

### Saving the tokens to a json object
After logging the user in you can serialize the tokens to a json object to save them for later use:
```java
JsonObject serializedAuthManager = JavaAuthManager.toJson(authManager);
```
It is important to note, that MinecraftAuth does not request all tokens at once during the login process.
Only the initial tokens are requested and the rest of the tokens are requested lazily when you access them.
For this reason, it is recommended to attach a change listener to the auth manager after logging in to make sure new tokens are automatically saved:
```java
authManager.getChangeListeners().add(() -> {
    // save the auth manager state here
});
```

### Loading the tokens from a json object
Loading the tokens back from a json object is just as easy:
```java
JavaAuthManager authManager = JavaAuthManager.fromJson(httpClient, serializedAuthManager);
```
And of course don't forget to attach the change listener again after loading:
```java
authManager.getChangeListeners().add(() -> {
    // save the auth manager state here
});
```

### Token lifecycle management
All token related methods in the auth managers return a ``Holder`` object which provides different methods to access the token.
Tokens are requested lazily, so they are only requested/refreshed when you access them.

The most important method is ``getUpToDate()`` which will automatically refresh the token if it is expired or not set yet.
This method will throw an exception if the refresh fails (The initial refresh token is no longer valid and the user has to login again).

There are several other methods available in the ``Holder`` class, which you can learn more about in the javadoc.

### Full example
Here is a full example which demonstrates logging in using the device code flow and saving the tokens to a json file:
```java
File tokenFile = new File("tokens.json");

// Log in using device code flow
JavaAuthManager authManager = JavaAuthManager.create(MinecraftAuth.createHttpClient()).login(DeviceCodeMsaAuthService::new, new Consumer<MsaDeviceCode>() {
    @Override
    public void accept(MsaDeviceCode deviceCode) {
        System.out.println("Go to " + deviceCode.getDirectVerificationUri());
    }
});

// Save tokens to file after login
Files.write(tokenFile.toPath(), JavaAuthManager.toJson(authManager).toString().getBytes(StandardCharsets.UTF_8));

// Attach a listener to save tokens on change
authManager.getChangeListeners().add(() -> {
    try {
        Files.write(tokenFile.toPath(), JavaAuthManager.toJson(authManager).toString().getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
});

// Request the Minecraft token
System.out.println("Access token: " + authManager.getMinecraftToken().getUpToDate().getToken());

// The file "tokens.json" now contains all the necessary tokens to restore and use the auth manager later
```

## Extras
MinecraftAuth also provides some extra functionality like a basic Minecraft Realms API implementation.

### Minecraft Realms API
MinecraftAuth provides a basic implementation of the Minecraft Realms API. It supports listing and joining the realms of an user.  
The Realms API requires you to provide the latest supported client version of your application.
You should hardcode this value instead of loading it from the Internet, as the Realms API will not work if your application can't handle the specified version.

#### Java Edition
```java
JavaRealmsService javaRealmsService = new JavaRealmsService(httpClient, "latestSupportedClientVersionHere", authManager.getMinecraftToken(), authManager.getMinecraftProfile());
boolean isCompatible = javaRealmsService.isCompatible();
if (!isCompatible) {
    System.out.println("The client version does not support Realms");
} else {
    System.out.println("Your client supports Realms");
    List<RealmsServer> realmsWorlds = javaRealmsService.getWorlds();
    System.out.println("Realms worlds: " + realmsWorlds);
    try {
        System.out.println("Connect to: " + javaRealmsService.joinWorld(realmsWorlds.get(0)));
    } catch (Exception e) {
        if (e instanceof RealmsRequestException) {
            RealmsRequestException exception = (RealmsRequestException) e;
            if (exception.getErrorCode() == RealmsRequestException.ERROR_TOS_NOT_ACCEPTED) {
                // The Java Edition Realms API requires users to accept the Minecraft Realms Terms of Service (https://aka.ms/MinecraftRealmsTerms)
                // You should display the terms to the user and ask them to accept them:
                javaRealmsService.acceptTos();
                // If they accept, then you can try to join the world again
            }
        }
    }
}
```

#### Bedrock Edition
```java
BedrockRealmsService bedrockRealmsService = new BedrockRealmsService(httpClient, "latestSupportedClientVersionHere", authManager.getRealmsXstsToken());
boolean isCompatible = bedrockRealmsService.isCompatible();
if (!isCompatible) {
    System.out.println("The client version does not support Realms");
} else {
    System.out.println("Your client supports Realms");
    List<RealmsServer> realmsWorlds = bedrockRealmsService.getWorlds();
    System.out.println("Realms worlds: " + realmsWorlds);
    System.out.println("Connect to: " + bedrockRealmsService.joinWorld(realmsWorlds.get(0)));
}
```
[Here is an example implementation](https://github.com/ViaVersion/ViaProxy/blob/09e685fad9ee1b804a3b01a7eb308a444a48855f/src/main/java/net/raphimc/viaproxy/ui/impl/RealmsTab.java) which is using the Realms API of both Minecraft editions.

## Migrating from MinecraftAuth 4.x.x to 5.x.x
If you are migrating from MinecraftAuth 4.x.x to 5.x.x you can use the ``MinecraftAuth4To5Migrator`` class to migrate the saved tokens of your users.
This class provides methods to migrate the Minecraft: Java Edition and Minecraft: Bedrock Edition token chains to the new auth manager structure.

## Contact
If you encounter any issues, please report them on the
[issue tracker](https://github.com/RaphiMC/MinecraftAuth/issues).  
If you just want to talk or need help implementing MinecraftAuth feel free to join my
[Discord](https://discord.gg/dCzT9XHEWu).
