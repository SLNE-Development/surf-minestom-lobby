package dev.slne.minestom.lobby.server.command.commandapi

import dev.slne.minestom.lobby.api.command.commandapi.CommandDefinition
import dev.slne.minestom.lobby.api.command.commandapi.RegisteredCommand
import dev.slne.minestom.lobby.api.command.commandapi.internal.CommandAPIPlatform
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.minestom.server.command.CommandManager
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class MinestomCommandAPIPlatform(
    private val commandManager: CommandManager,
    private val ownership: MinestomCommandOwnership,
    private val compiler: MinestomCommandCompiler = MinestomCommandCompiler(),
) : CommandAPIPlatform, AutoCloseable {

    private val lock = ReentrantLock()
    private val registrations = ObjectOpenHashSet<CompiledRegistration>()

    private var closed = false

    override fun register(
        definition: CommandDefinition,
        namespace: String?
    ): RegisteredCommand = lock.withLock {
        check(!closed) { "The Minestom CommandAPI platform is closed" }

        val compiled = compiler.compile(definition, namespace)
        compiled.names.forEach { name ->
            check(!ownership.contains(name) && commandManager.getCommand(name) == null) {
                "Command name '$name' is already registered"
            }
        }

        var nativeRegistered = false
        try {
            commandManager.register(compiled.command)
            nativeRegistered = true

            ownership.claim(compiled.names, compiled)
            registrations += compiled
        } catch (failure: Throwable) {
            if (nativeRegistered) {
                try {
                    detach(compiled)
                } catch (rollbackFailure: Throwable) {
                    failure.addSuppressed(rollbackFailure)
                }
            } else {
                ownership.release(compiled)
            }
            throw failure
        }
        return compiled.registration
    }

    override fun unregister(name: String): Boolean = lock.withLock {
        val compiled = ownership.find(name) ?: return false
        if (compiled !in registrations) return false
        detach(compiled)
        return true
    }

    override fun close(): Unit = lock.withLock {
        if (closed) return
        closed = true

        var failure: Throwable? = null
        val registrationsSnapshot = ObjectArrayList(registrations)
        registrationsSnapshot.forEach { compiled ->
            try {
                detach(compiled)
            } catch (currentFailure: Throwable) {
                if (failure == null) {
                    failure = currentFailure
                } else {
                    failure.addSuppressed(currentFailure)
                }
            }
        }

        registrations.clear()
        failure?.let { throw it }
    }

    private fun detach(compiled: CompiledRegistration) {
        check(lock.isHeldByCurrentThread) { "Platform state lock must be held while detaching a command" }

        try {
            unregisterNativeIfUncontested(compiled)
        } finally {
            registrations.remove(compiled)
            ownership.release(compiled)
        }
    }

    private fun unregisterNativeIfUncontested(compiled: CompiledRegistration) {
        synchronized(commandManager) {
            val hasForeignMapping = compiled.names.any { name ->
                val current = commandManager.getCommand(name)
                current != null && current !== compiled.command
            }

            if (!hasForeignMapping) {
                commandManager.unregister(compiled.command)
            }
        }
    }
}
