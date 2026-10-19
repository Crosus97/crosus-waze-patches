extension {
    name = "extensions/extension.mpe"
}

android {
    namespace = "app.crosus.waze.extension"
}

dependencies {
    // Stubs de la Car App Library (Android Auto): solo la "forma" de las clases, para compilar.
    // compileOnly = NO se empaqueta: en ejecución se usan las clases reales que Waze ya lleva dentro
    // (comprobado con CarLibraryProbe: conservan su nombre).
    compileOnly(project(":extensions:extension:stub"))

    // Tests en el PC (JVM) de la parte de Java puro: parser, geometría y lógica de avisos.
    // testImplementation no se empaqueta en la extensión: no acaba dentro de Waze.
    testImplementation(libs.junit)
}
