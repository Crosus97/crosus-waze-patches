extension {
    name = "extensions/extension.mpe"
}

android {
    namespace = "app.crosus.waze.extension"
}

dependencies {
    // Tests en el PC (JVM) de la parte de Java puro: parser, geometría y lógica de avisos.
    // testImplementation no se empaqueta en la extensión: no acaba dentro de Waze.
    testImplementation(libs.junit)
}
