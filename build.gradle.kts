allprojects {
    group = "com.saulx.based-android-client"
    version = "0.1.1-beta-1"

    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/atelier-saulx/based-android")
            credentials {
                username = System.getenv("BASED_ANDROID_USERNAME")
                password = System.getenv("BASED_ANDROID_KEY")
            }
        }
    }
}