package dev.slne.minestom.lobby.server.upload

import com.google.inject.AbstractModule
import com.google.inject.Guice
import com.google.inject.ProvisionException
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.di.bindIntoSet
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class UploadServiceTest {

    @Test
    fun `handlers bound into the set reach the service`() {
        val injector = Guice.createInjector(object : AbstractModule() {
            override fun configure() {
                binder().bindIntoSet<UploadHandler, WorldsHandler>()
                binder().bindIntoSet<UploadHandler, ConductHandler>()
            }
        })

        val uploads = injector.getInstance(UploadService::class.java)

        assertEquals(listOf("codeofconduct", "worlds"), uploads.directoryNames)
        assertSame(injector.getInstance(WorldsHandler::class.java), uploads.handler("worlds"))
        assertSame(
            injector.getInstance(ConductHandler::class.java),
            uploads.handler("codeofconduct"),
        )
        assertNull(uploads.handler("nothing"))
    }

    @Test
    fun `two handlers may not share a directory`() {
        val injector = Guice.createInjector(object : AbstractModule() {
            override fun configure() {
                binder().bindIntoSet<UploadHandler, WorldsHandler>()
                binder().bindIntoSet<UploadHandler, OtherWorldsHandler>()
            }
        })

        val failure = assertThrows(ProvisionException::class.java) {
            injector.getInstance(UploadService::class.java)
        }

        assertEquals(
            true,
            failure.message?.contains("share a directory name"),
            failure.message,
        )
    }

    private open class TestHandler(override val directoryName: String) : UploadHandler {
        override val fileGlob = "*.test"
        override val description = "test"
        override val fileNameDocumentation = "test"

        override suspend fun publish(file: Path) = Unit
        override suspend fun list(): List<UploadEntry> = emptyList()
        override suspend fun delete(key: String) = false
    }

    @Singleton
    private class WorldsHandler : TestHandler("worlds")

    @Singleton
    private class ConductHandler : TestHandler("codeofconduct")

    @Singleton
    private class OtherWorldsHandler : TestHandler("worlds")
}
