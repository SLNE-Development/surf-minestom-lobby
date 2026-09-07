package dev.slne.minestom.lobby.server.mixin

import com.google.gson.JsonParser
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.mixin.Mixin

/** Applies every configured common and server mixin to its shipped target bytecode. */
class ConfiguredMixinsApplyTest {

    @TestFactory
    fun `configured mixins apply to all declared targets`(): List<DynamicTest> {
        val configuration = checkNotNull(
            javaClass.classLoader.getResourceAsStream(MixinTestSupport.CONFIGURATION),
        ) { "Missing ${MixinTestSupport.CONFIGURATION}" }.bufferedReader().use {
            JsonParser.parseReader(it).asJsonObject
        }
        val mixinPackage = configuration.get("package").asString
        val mixins = listOf("mixins", "server").flatMap { section ->
            configuration.getAsJsonArray(section)?.map { "$mixinPackage.${it.asString}" }
                .orEmpty()
        }
        check(mixins.isNotEmpty()) { "No server mixins configured" }

        val targets = linkedMapOf<String, MutableList<String>>()
        for (mixin in mixins) {
            for (target in targetsOf(mixin)) {
                targets.getOrPut(target) { mutableListOf() }.add(mixin)
            }
        }
        return targets.map { (target, owners) ->
            dynamicTest("${owners.joinToString { it.substringAfterLast('.') }} -> $target") {
                MixinTestSupport.transform(target)
            }
        }
    }

    private fun targetsOf(mixin: String): List<String> {
        val node = ClassNode(Opcodes.ASM9)
        ClassReader(MixinTestSupport.originalBytes(mixin.replace('.', '/'))).accept(
            node,
            ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES,
        )
        val annotation = checkNotNull(
            (node.visibleAnnotations.orEmpty() + node.invisibleAnnotations.orEmpty())
                .singleOrNull { it.desc == Type.getDescriptor(Mixin::class.java) },
        ) { "$mixin has no @Mixin annotation" }
        val targets = annotation.values.orEmpty().chunked(2).flatMap { (key, value) ->
            when (key) {
                "value" -> (value as List<*>).map { (it as Type).className }
                "targets" -> (value as List<*>).map { (it as String).replace('/', '.') }
                else -> emptyList()
            }
        }.distinct()
        check(targets.isNotEmpty()) { "$mixin declares no targets" }
        return targets
    }
}
