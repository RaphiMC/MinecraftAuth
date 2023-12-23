# MinecraftAuth
Simple and easy to use Minecraft microsoft authentication library (Java and Bedrock).

## Features
- Full support for Minecraft: Java Edition and Minecraft: Bedrock Edition
- Login using device code, credentials or a local webserver
- Refreshing and validating token chains
- Serializing and deserializing token chains to and from json
- Customizable login flows (Client ID, scopes, ...)
- Basic implementation of the Minecraft Realms API (Allows listing and joining realms)

## Releases
### Gradle/Maven
To use MinecraftAuth with Gradle/Maven you can get it from [Maven Central](https://mvnrepository.com/artifact/net.raphimc/MinecraftAuth), [Lenni0451's Maven](https://maven.lenni0451.net/#/releases/net/raphimc/MinecraftAuth) or [Jitpack](https://jitpack.io/#RaphiMC/MinecraftAuth).
You can also find instructions how to implement it into your build script there.

### Jar File
If you just want the latest jar file you can download it from [GitHub Actions](https://github.com/RaphiMC/MinecraftAuth/actions/workflows/build.yml) or [Lenni0451's Jenkins](https://build.lenni0451.net/job/MinecraftAuth/).

## Usage
MinecraftAuth provides most of its functionality through the ``MinecraftAuth`` class.
It contains predefined login flows for Minecraft: Java Edition and Minecraft: Bedrock Edition using the official client ids and scopes.

To customize/configure a login flow yourself (for example to change the client id or the scope of the application) you can use the ``MinecraftAuth.builder()`` method.
For examples, you can look at the predefined login flows in the ``MinecraftAuth`` class.

Here is an example of how to manage a Minecraft: Java Edition account (For Minecraft: Bedrock Edition you can use pretty much the same code, but replace ``Java`` with ``Bedrock``):
### Log in using device code (Recommended)
The device code auth flow blocks the thread until the user has logged in and throws an exception if the process times out.
The timeout is 120 seconds by default.
```java
try (CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
    StepFullJavaSession.FullJavaSession javaSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.getFromInput(httpClient, new StepMsaDeviceCode.MsaDeviceCodeCallback(msaDeviceCode -> {
        // Method to generate a verification URL and a code for the user to enter on that page
        System.out.println("Go to " + msaDeviceCode.getVerificationUri());
        System.out.println("Enter code " + msaDeviceCode.getUserCode());

        // There is also a method to generate a direct URL without needing the user to enter a code
        System.out.println("Go to " + msaDeviceCode.getDirectVerificationUri());
    }));
    System.out.println("Username: " + javaSession.getMcProfile().getName());
    System.out.println("Access token: " + javaSession.getMcProfile().getMcToken().getAccessToken());
    System.out.println("Player certificates: " + javaSession.getPlayerCertificates());
}
```
### Log in using credentials
The credentials auth flow does not handle 2FA and will throw an exception if the user has 2FA enabled. You should consider using the device code auth flow instead if you want to support 2FA.
```java
try (CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
    StepFullJavaSession.FullJavaSession javaSession = MinecraftAuth.JAVA_CREDENTIALS_LOGIN.getFromInput(httpClient, new StepCredentialsMsaCode.MsaCredentials("email@test.com", "P4ssw0rd"));
    System.out.println("Username: " + javaSession.getMcProfile().getName());
    System.out.println("Access token: " + javaSession.getMcProfile().getMcToken().getAccessToken());
    System.out.println("Player certificates: " + javaSession.getPlayerCertificates());
}
```
### Save the token chain to a json object
```java
JsonObject serializedSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.toJson(javaSession);
```
### Load the token chain from a json object
```java
StepFullJavaSession.FullJavaSession loadedSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.fromJson(serializedSession);
```
### Refresh the token chain
MinecraftAuth implements a refresh method that only refreshes the tokens that are expired and reuses the valid ones.
You can call this everytime before you access/use the token chain to make sure it is valid. (Don't spam it though or else you will be rate limited by Microsoft)
This method will throw an exception if the refresh fails (The initial refresh token is no longer valid and the user has to login again).
```java
try (CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
    StepFullJavaSession.FullJavaSession readyToUseSession = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.refresh(httpClient, loadedSession);
}
```
### Minecraft Realms API
MinecraftAuth provides a basic implementation of the Minecraft Realms API. It supports listing and joining the realms of an user.  
The Realms API requires you to provide the latest supported client version of your application.
You should hardcode this value instead of loading it from the Internet, as the Realms API will not work if your application can't handle the specified version.
#### Java Edition
```java
JavaRealmsService javaRealmsService = new JavaRealmsService(httpClient, "latestSupportedClientVersionHere", javaSession.getMcProfile());
boolean isAvailable = javaRealmsService.isAvailable().join();
if (!isAvailable) {
    System.out.println("The client version does not support Realms");
} else {
    System.out.println("Your client supports Realms");
    List<RealmsWorld> realmsWorlds = javaRealmsService.getWorlds().join();
    System.out.println("Realms worlds: " + realmsWorlds);
    try {
        System.out.println("Connect to: " + javaRealmsService.joinWorld(realmsWorlds.get(0)).join());
    } catch (CompletionException e) {
        if (e.getCause() instanceof RealmsResponseException) {
            RealmsResponseException exception = (RealmsResponseException) e.getCause();
            if (exception.getRealmsErrorCode() == RealmsResponseException.TOS_NOT_ACCEPTED) {
                // The Java Edition Realms API requires users to accept the Minecraft Realms Terms of Service (https://aka.ms/MinecraftRealmsTerms)
                // You should display the terms to the user and ask them to accept them:
                javaRealmsService.acceptTos().join();
                // If they accept, then you can try to join the world again
            }
        }
    }
}
```
#### Bedrock Edition
**If you need Minecraft: Bedrock Edition Realms support, you have to make a custom auth flow with the realms boolean set to true when calling the buildMinecraftBedrockChainStep() method.**
```java
BedrockRealmsService bedrockRealmsService = new BedrockRealmsService(httpClient, "latestSupportedClientVersionHere", bedrockSession.getRealmsXsts());
boolean isAvailable = bedrockRealmsService.isAvailable().join();
if (!isAvailable) {
    System.out.println("The client version does not support Realms");
} else {
    System.out.println("Your client supports Realms");
    List<RealmsWorld> realmsWorlds = bedrockRealmsService.getWorlds().join();
    System.out.println("Realms worlds: " + realmsWorlds);
    System.out.println("Connect to: " + bedrockRealmsService.joinWorld(realmsWorlds.get(0)).join());
}
```
[Here is an example implementation](https://github.com/ViaVersion/ViaProxy/blob/09e685fad9ee1b804a3b01a7eb308a444a48855f/src/main/java/net/raphimc/viaproxy/ui/impl/RealmsTab.java) which is using the Realms API of both Minecraft editions.

### Logging
MinecraftAuth by default uses SLF4J for logging.
You can however easily redirect the log messages to your own code by setting ``MinecraftAuth.LOGGER`` to your own ``ILogger``.

## Contact
If you encounter any issues, please report them on the
[issue tracker](https://github.com/RaphiMC/MinecraftAuth/issues).  
If you just want to talk or need help implementing MinecraftAuth feel free to join my
[Discord](https://discord.gg/dCzT9XHEWu).
