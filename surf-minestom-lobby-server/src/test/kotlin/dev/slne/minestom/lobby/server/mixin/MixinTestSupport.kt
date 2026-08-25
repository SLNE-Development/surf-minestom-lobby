package dev.slne.minestom.lobby.server.mixin

import com.llamalad7.mixinextras.MixinExtrasBootstrap
import dev.slne.minestom.lobby.server.instrumentation.mixin.InstrumentationMixinService
import java.lang.instrument.ClassDefinition
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.util.jar.JarFile
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.launch.MixinBootstrap
import org.spongepowered.asm.mixin.MixinEnvironment
import org.spongepowered.asm.mixin.Mixins

/**
 * Bootstraps `mixins.surf-lobby.json` through the same service the production agent uses and hands
 * out the transformer it installs. [InstrumentationMixinService.setInstrumentation] rejects a
 * second call, so every mixin test shares this one initialization.
 */
object MixinTestSupport {

    val transformer: ClassFileTransformer by lazy {
        val instrumentation = RecordingInstrumentation()
        InstrumentationMixinService.setInstrumentation(instrumentation)
        MixinBootstrap.init()
        MixinExtrasBootstrap.init()
        Mixins.addConfiguration("mixins.surf-lobby.json")
        advanceMixinPhases()
        checkNotNull(instrumentation.transformer) { "mixin never installed its transformer" }
    }

    /** Transforms the shipped bytecode of [binaryName] without loading the result. */
    fun transform(binaryName: String): ClassNode {
        val internalName = binaryName.replace('.', '/')
        val transformed = checkNotNull(transform(internalName, originalBytes(internalName))) {
            "$binaryName was not transformed"
        }

        val node = ClassNode(Opcodes.ASM9)
        ClassReader(transformed).accept(node, 0)
        return node
    }

    internal fun originalBytes(internalName: String): ByteArray =
        checkNotNull(javaClass.classLoader.getResourceAsStream("$internalName.class")) {
            "$internalName is not on the test classpath"
        }.use { it.readBytes() }

    internal fun transform(internalName: String, original: ByteArray) =
        transformer.transform(null, javaClass.classLoader, internalName, null, null, original)

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
        override fun redefineClasses(vararg definitions: ClassDefinition) = Unit
        override fun isModifiableClass(theClass: Class<*>) = false
        override fun getAllLoadedClasses(): Array<Class<*>> = emptyArray()
        override fun getInitiatedClasses(loader: ClassLoader?): Array<Class<*>> = emptyArray()
        override fun getObjectSize(objectToSize: Any) = 0L
        override fun appendToBootstrapClassLoaderSearch(jarfile: JarFile) = Unit
        override fun appendToSystemClassLoaderSearch(jarfile: JarFile) = Unit
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

/**
 * Loads every class matching one of [ownedPrefixes] from mixin-transformed bytes and delegates the
 * rest to the parent, so a transformed dependency class can be exercised in-process while it still
 * links against the ordinary Minestom classes.
 *
 * Owned classes get their own copy of any state they hold, so keep the prefixes narrow enough that
 * no owned class appears in a delegated class' signatures.
 */
class MixinTransformingClassLoader(
    parent: ClassLoader,
    private vararg val ownedPrefixes: String,
) : ClassLoader("mixin-transformed", parent) {

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (ownedPrefixes.none(name::startsWith)) return super.loadClass(name, resolve)

        synchronized(getClassLoadingLock(name)) {
            findLoadedClass(name)?.let { return it }

            val internalName = name.replace('.', '/')
            val original = MixinTestSupport.originalBytes(internalName)
            val bytes = MixinTestSupport.transform(internalName, original) ?: original

            return defineClass(name, bytes, 0, bytes.size).also { if (resolve) resolveClass(it) }
        }
    }
}
