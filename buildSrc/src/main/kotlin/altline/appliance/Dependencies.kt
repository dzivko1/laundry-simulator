@file:Suppress("MemberVisibilityCanBePrivate")

object Libs {
    object Kotlin {
        const val version = "1.7.20"
    }

    object Compose {
        const val version = "1.3.0-beta03"
        const val materialIconsExtended = "org.jetbrains.compose.material:material-icons-extended-desktop:$version"
    }

    object KotlinxCoroutines {
        const val version = "1.6.4"
        const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$version"
    }

    object PausingCoroutineDispatcher {
        const val version = "ee40ae5035"
        const val pausingCoroutineDispatcher = "com.github.Koitharu.pausing-coroutine-dispatcher:pausing-coroutine-dispatcher-jvm:$version"
    }

    object Koin {
        const val version = "3.2.2"
        const val core = "io.insert-koin:koin-core:$version"
    }

    object Measured {
        const val version = "0.3.1"
        const val measured = "io.nacular.measured:measured:$version"
    }

    object Logging {
        object Log4j2 {
            const val version = "2.19.0"
            const val core = "org.apache.logging.log4j:log4j-core:$version"
            const val api = "org.apache.logging.log4j:log4j-api:$version"
            const val slf4jBridge = "org.apache.logging.log4j:log4j-slf4j2-impl:$version"
        }
        object KotlinLogging {
            const val version = "3.0.4"
            const val jvm = "io.github.microutils:kotlin-logging-jvm:$version"
        }
    }
}