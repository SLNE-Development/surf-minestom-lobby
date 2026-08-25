package dev.slne.minestom.lobby.server.mixin

import com.llamalad7.mixinextras.MixinExtrasBootstrap
import dev.slne.minestom.lobby.server.instrumentation.mixin.InstrumentationMixinService
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.Mixins

/**
 * Applies `mixins.surf-lobby.json` to the shipped Minestom bytecode through the same service the
 * production agent uses, so an EntityTracker bytecode change fails here instead of silently at
 * runtime. Runs the transformer directly against a fake [Instrumentation]; no transformed class
 * is ever loaded.
 */
class EntityTrackerMixinApplyTest {

    @Test
    fun `the entity tracker mixins apply to the shipped minestom bytecode`() {
        val transformer = bootstrapMixins()

        val impl = transform(transformer, "net.minestom.server.instance.EntityTrackerImpl")
        val entry = transform(
            transformer,
            "net.minestom.server.instance.EntityTrackerImpl\$EntityTrackerEntry",
        )

        // mixin decorates handler names, e.g. handler$zzd000$surf$directNearbyScan
        val implMethods = impl.methods.map { it.name }
        assertTrue(implMethods.any { it.endsWith("surf\$directNearbyScan") }) {
            "fast-path inject missing: $implMethods"
        }
        assertTrue(implMethods.any { it.endsWith("surf\$rectangularChunkScan") }) {
            "chunk-scan redirect missing: $implMethods"
        }

        val nearby = impl.methods.single { it.name == "nearbyEntitiesByChunkRange" }
        val calls = nearby.instructions.asSequence()
            .filterIsInstance<MethodInsnNode>()
            .map { "${it.owner}.${it.name}" }
            .toList()
        assertTrue(calls.none { it == "net/minestom/server/coordinate/ChunkRange.chunksInRange" }) {
            "the spiral traversal was not redirected: $calls"
        }
        assertTrue(calls.any { it.endsWith("surf\$rectangularChunkScan") }) {
            "the rectangular redirect handler is not called: $calls"
        }
        assertTrue(calls.any { it.endsWith("surf\$directNearbyScan") }) {
            "the fast-path injection is not called: $calls"
        }

        assertTrue("dev/slne/minestom/lobby/server/duck/EntityTrackerEntryDuck" in entry.interfaces) {
            "EntityTrackerEntry does not implement the duck: ${entry.interfaces}"
        }
        assertTrue(entry.methods.any { it.name == "surf\$lastPosition" }) {
            "the lastPosition accessor was not generated"
        }
    }

    private fun transform(transformer: ClassFileTransformer, binaryName: String): ClassNode {
        val internalName = binaryName.replace('.', '/')
        val original = checkNotNull(
            javaClass.classLoader.getResourceAsStream("$internalName.class"),
        ) { "$binaryName not on the test classpath" }.use { it.readBytes() }

        val transformed = transformer.transform(
            null,
            javaClass.classLoader,
            internalName,
            null,
            null,
            original,
        )
        assertNotNull(transformed) { "$binaryName was not transformed" }

        val node = ClassNode(Opcodes.ASM9)
        ClassReader(transformed).accept(node, 0)
        return node
    }

    private fun bootstrapMixins(): ClassFileTransformer {
        val instrumentation = RecordingInstrumentation()
        InstrumentationMixinService.setInstrumentation(instrumentation)
        MixinBootstrap.init()
        MixinExtrasBootstrap.init()
        Mixins.addConfiguration("mixins.surf-lobby.json")
        advanceMixinPhases()
        return checkNotNull(instrumentation.transformer) { "mixin never installed its transformer" }
    }

    /** Mirrors the private phase advance in `LobbyAgent`. */
    private fun advanceMixinPhases() {
        val gotoPhase = MixinEnvironment::class.java
            .getDeclaredMethod("gotoPhase", MixinEnvironment.Phase::class.java)
        gotoPhase.isAccessible = true
        gotoPhase.invoke(null, MixinEnvironment.Phase.INIT)
        gotoPhase.invoke(null, MixinEnvironment.Phase.DEFAULT)
    }

    /** Captures the transformer the mixin service installs; every other operation is inert. */
    private class RecordingInstrumentation : Instrumentation {
        var transformer: ClassFileTransformer? = null

        override fun addTransformer(transformer: ClassFileTransformer, canRetransform: Boolean) {
            this.transformer = transformer
        }

        override fun addTransformer(transformer: ClassFileTransformer) =
            addTransformer(transformer, false)

        override fun removeTransformer(transformer: ClassFileTransformer) = true
        override fun isRetransformClassesSupported() = false
        override fun retransformClasses(vararg classes: Class<*>) = Unit
        override fun isRedefineClassesSupported() = false
        override fun redefineClasses(vararg definitions: java.lang.instrument.ClassDefinition) = Unit
        override fun isModifiableClass(theClass: Class<*>) = false
        override fun getAllLoadedClasses(): Array<Class<*>> = emptyArray()
        override fun getInitiatedClasses(loader: ClassLoader?): Array<Class<*>> = emptyArray()
        override fun getObjectSize(objectToSize: Any) = 0L
        override fun appendToBootstrapClassLoaderSearch(jarfile: java.util.jar.JarFile) = Unit
        override fun appendToSystemClassLoaderSearch(jarfile: java.util.jar.JarFile) = Unit
        override fun isNativeMethodPrefixSupported() = false
        override fun setNativeMethodPrefix(
            transformer: ClassFileTransformer,
            prefix: String?,
        ) = Unit

        override fun redefineModule(
            module: Module,
            extraReads: Set<Module>,
            extraExports: Map<String, Set<Module>>,
            extraOpens: Map<String, Set<Module>>,
            extraUses: Set<Class<*>>,
            extraProvides: Map<Class<*>, List<Class<*>>>,
        ) = Unit

        override fun isModifiableModule(module: Module) = false
    }
}
