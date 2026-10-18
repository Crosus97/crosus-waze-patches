group = "app.crosus.waze"

patches {
    about {
        name = "Crosus Waze Patches"
        description = "Avisos de los paneles de mensaje variable de la DGT dentro de Waze (móvil y Android Auto)"
        source = "git@github.com:Crosus97/crosus-waze-patches.git"
        author = "Crosus97"
        contact = "https://github.com/Crosus97/crosus-waze-patches/issues"
        website = "https://github.com/Crosus97/crosus-waze-patches"
        license = "GPLv3"
    }
}

// Separate configuration so gson is available at runtime for the
// generatePatchesList task but never bundled into the APK.
val patchListGeneratorClasspath = configurations.create("patchListGeneratorClasspath")

dependencies {
    compileOnly(libs.gson)
    patchListGeneratorClasspath(libs.gson)
}

tasks {
    register<JavaExec>("generatePatchesList") {
        description = "Build patch with patch list"

        dependsOn(build)

        classpath = sourceSets["main"].runtimeClasspath + patchListGeneratorClasspath
        mainClass.set("util.PatchListGeneratorKt")
    }

    // Used by gradle-semantic-release-plugin.
    publish {
        dependsOn("generatePatchesList")
    }
}
