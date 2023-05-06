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
public class Test {

    public static void main(String[] args) throws Throwable {
        // Log in using credentials
        try (final CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
            StepMCProfile.MCProfile mcProfile = MinecraftAuth.JAVA_CREDENTIALS_LOGIN.getFromInput(httpClient, new StepCredentialsMsaCode.MsaCredentials("email@test.com", "P4ssw0rd"));
            System.out.println("Logged in with access token: " + mcProfile.prevResult().prevResult().access_token());
        }

        // Log in using device code (Blocks until the user has logged in or timeout is reached)
        StepMCProfile.MCProfile mcProfile;
        try (final CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
            mcProfile = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.getFromInput(httpClient, new StepMsaDeviceCode.MsaDeviceCodeCallback(msaDeviceCode -> {
                System.out.println("Go to " + msaDeviceCode.verificationUri());
                System.out.println("Enter code " + msaDeviceCode.userCode());
            }));
            System.out.println("Logged in as: " + mcProfile.name());
        }

        // Save the whole chain of tokens
        final JsonObject serializedProfile = mcProfile.toJson();

        // Load the chain of tokens
        StepMCProfile.MCProfile loadedProfile = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.fromJson(serializedProfile);

        // Refresh the chain of tokens (It only refreshes those necessary)
        try (final CloseableHttpClient httpClient = MicrosoftConstants.createHttpClient()) {
            loadedProfile = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN.refresh(httpClient, mcProfile);
        }

        // loadedProfile is now valid again and can be used
    }

}
```
To change the client id or the scope of the application you can use the ``MinecraftAuth.builder()`` method.
For examples, you can look at the predefined login flows in the ``MinecraftAuth`` class. Note: This library uses the official client id and scope and changing it could lead to certain features not working or being detectable by servers.

## Contact
If you encounter any issues, please report them on the
[issue tracker](https://github.com/RaphiMC/MinecraftAuth/issues).  
If you just want to talk or need help implementing MinecraftAuth feel free to join my
[Discord](https://discord.gg/dCzT9XHEWu).
