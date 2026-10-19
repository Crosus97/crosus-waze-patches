extension {
    name = "extensions/extension.mpe"
}

android {
    namespace = "app.crosus.waze.extension"
}

dependencies {
    // API de Android Auto (Car App Library). compileOnly: solo para compilar, NO se empaqueta.
    // En ejecución se usan las clases que Waze ya lleva dentro (comprobado: conservan su nombre).
    compileOnly(libs.androidx.car.app)

    // Tests en el PC (JVM) de la parte de Java puro: parser, geometría y lógica de avisos.
    // testImplementation no se empaqueta en la extensión: no acaba dentro de Waze.
    testImplementation(libs.junit)
}
