allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

val newBuildDir: Directory =
    rootProject.layout.buildDirectory
        .dir("../../build")
        .get()
rootProject.layout.buildDirectory.value(newBuildDir)

subprojects {
    val newSubprojectBuildDir: Directory = newBuildDir.dir(project.name)
    project.layout.buildDirectory.value(newSubprojectBuildDir)
}
subprojects {
    project.evaluationDependsOn(":app")
}

subprojects {
    val configureNamespace: () -> Unit = {
        val android = extensions.findByName("android")
        if (android != null) {
            val setter = android.javaClass.methods.firstOrNull { it.name == "setNamespace" && it.parameterTypes.size == 1 }
            val getter = android.javaClass.methods.firstOrNull { it.name == "getNamespace" && it.parameterTypes.isEmpty() }
            
            val hasNs = try { getter?.invoke(android) != null } catch (_: Throwable) { false }
            
            if (!hasNs && setter != null) {
                var manifestPackage: String? = null
                val manifestFile = file("${project.projectDir}/src/main/AndroidManifest.xml")
                if (manifestFile.exists()) {
                    val content = manifestFile.readText()
                    val match = Regex("""package\s*=\s*["']([^"']+)["']""").find(content)
                    if (match != null) {
                        manifestPackage = match.groupValues[1]
                    }
                }
                val packageName = manifestPackage
                    ?: project.group.toString().takeIf { it.isNotEmpty() }
                    ?: "com.koras.plugin.${project.name.replace('-', '_')}"
                setter.invoke(android, packageName)
            }
        }
    }

    if (state.executed) {
        configureNamespace()
    } else {
        afterEvaluate {
            configureNamespace()
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
