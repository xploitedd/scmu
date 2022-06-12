buildscript {
    extra.apply {
        set("compose_version", "1.1.0-beta01")
        set("room_version", "2.4.2")
        set("ktor_version", "2.0.2")
    }
}

plugins {
    id("com.android.application") version "7.2.1" apply false
    id("com.android.library") version "7.2.1" apply false
    id("org.jetbrains.kotlin.android") version "1.5.31" apply false
    id("com.google.devtools.ksp") version "1.5.31-1.0.1" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.6.21" apply false
    id("org.jetbrains.kotlin.plugin.parcelize") version "1.5.31" apply false
}

tasks.withType<Delete> {
    delete(rootProject.buildDir)
}
//task clean(type: Delete) {
//    delete rootProject.buildDir
//}