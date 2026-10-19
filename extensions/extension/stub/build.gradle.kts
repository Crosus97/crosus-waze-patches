import com.android.build.api.dsl.LibraryExtension

// Stubs de compilación: clases vacías con las mismas firmas que las de Waze.
// Nunca se empaquetan (la extensión las usa como compileOnly).
plugins {
    id("com.android.library")
}

configure<LibraryExtension> {
    namespace = "app.crosus.waze.extension.stub"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }
}
