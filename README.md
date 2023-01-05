# MinecraftAuth
Simple and easy to use Minecraft microsoft authentication library (Java and Bedrock).  
The library features logging in via device code or credentials, refreshing and validating token chains.  
It also supports serializing/deserializing the whole token chain to and from a json tree to be saved and loaded later.

## Releases
### Gradle/Maven
To use MinecraftAuth with Gradle/Maven you can use this [Maven server](https://maven.lenni0451.net/#/releases/net/raphimc/MinecraftAuth) or [Jitpack](https://jitpack.io/#RaphiMC/MinecraftAuth).  
You can also find instructions how to implement it into your build script there.

### Jar File
If you just want the latest jar file you can download it from this [Jenkins](https://build.lenni0451.net/job/MinecraftAuth/).

## Usage
Here is an example of how to use the library:
```java
// Log in using credentials
StepMCProfile.MCProfile mcProfile = MinecraftAuth.requestJavaLogin(new StepCredentialsMsaCode.MsaCredentials("test@email.com", "password"));
System.out.println("Logged in with access token: " + mcProfile.prevResult().prevResult().access_token());

// Log in using device code (Blocks until the user has logged in or timeout is reached)
mcProfile = MinecraftAuth.requestJavaLogin(msaDeviceCode -> {
    System.out.println("Go to " + msaDeviceCode.verificationUri());
    System.out.println("Enter code " + msaDeviceCode.deviceCode());
});
System.out.println("Logged in as: " + mcProfile.name());

// Save the whole chain of tokens
final JsonObject serializedProfile = mcProfile.toJson();

// Load the chain of tokens
final StepMCProfile.MCProfile loadedProfile = MinecraftAuth.Java.Title.MC_PROFILE.fromJson(serializedProfile);

// Refresh the chain of tokens (It only refreshes those necessary)
try (final CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
    mcProfile = MinecraftAuth.Java.Title.MC_PROFILE.refresh(httpClient, mcProfile);
}
```

## Contact
If you encounter any issues, please report them on the
[issue tracker](https://github.com/RaphiMC/MinecraftAuth/issues).  
If you just want to talk or need help implementing MinecraftAuth feel free to join my
[Discord](https://discord.gg/dCzT9XHEWu).