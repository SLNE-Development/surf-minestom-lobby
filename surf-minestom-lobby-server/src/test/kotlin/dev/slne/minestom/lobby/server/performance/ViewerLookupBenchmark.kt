package dev.slne.minestom.lobby.server.performance

import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import net.minestom.server.coordinate.ChunkRange
import net.minestom.server.coordinate.CoordConversion
import net.minestom.server.coordinate.Point
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Test

/**
 * Manual benchmark comparing the viewer-lookup strategies on tracker-shaped data structures
 * (flare sync maps, CopyOnWriteArrayList buckets, ConcurrentHashMap entity set) at view
 * distance 32. Not a unit test: timings are machine-dependent, so it never asserts.
 *
 * Run by removing @Disabled, e.g.
 * `gradlew :surf-minestom-lobby-server:test --tests "*ViewerLookupBenchmark*"`.
 */
@EnvTest
class ViewerLookupBenchmark {

    private var blackhole = 0L

    @Test
    @org.junit.jupiter.api.Disabled("benchmark - run manually, do not gate the build on timings")
    fun `compare spiral, rectangular, direct and hybrid lookups`(env: Env) {
        val random = Random(0xC0FFEE)
        val pool = (0 until 5000).map { Entity(EntityType.ZOMBIE) }

        println("strategy comparison at range $RANGE ((2r+1)^2 = ${ChunkRange.chunksCount(RANGE)} chunks)")
        println("players | spiral buckets | rect buckets | direct scan | hybrid")
        for (playerCount in intArrayOf(1, 100, 500, 1000, 2000, 5000)) {
            val entities: MutableSet<Entity> = ConcurrentHashMap.newKeySet()
            val buckets = space.vectrix.flare.fastutil.Long2ObjectSyncMap.hashmap<MutableList<Entity>>()
            val positions = space.vectrix.flare.fastutil.Int2ObjectSyncMap.hashmap<Point>()
            val resolver = EntityViewerLookup.EntityPositionResolver { entity ->
                positions.get(entity.entityId)
            }

            for (entity in pool.subList(0, playerCount)) {
                val pos = Pos(
                    (random.nextInt(SPREAD_CHUNKS * 2 + 1) - SPREAD_CHUNKS) * 16.0 + random.nextInt(16),
                    42.0,
                    (random.nextInt(SPREAD_CHUNKS * 2 + 1) - SPREAD_CHUNKS) * 16.0 + random.nextInt(16),
                )
                entities.add(entity)
                positions.put(entity.entityId, pos)
                buckets.computeIfAbsent(CoordConversion.chunkIndex(pos)) { CopyOnWriteArrayList() }
                    .add(entity)
            }

            val bucketProbe = ChunkRange.ChunkConsumer { x, z ->
                val bucket = buckets.get(CoordConversion.chunkIndex(x, z)) ?: return@ChunkConsumer
                for (entity in bucket) blackhole += entity.entityId.toLong()
            }
            val sink = java.util.function.Consumer<Entity> { entity ->
                blackhole += entity.entityId.toLong()
            }

            val spiral = measure { ChunkRange.chunksInRange(0, 0, RANGE, bucketProbe) }
            val rect = measure { EntityViewerLookup.chunksInRangeRectangular(0, 0, RANGE, bucketProbe) }
            val direct = measure { EntityViewerLookup.directScan(entities, resolver, 0, 0, RANGE, sink) }
            val hybrid = measure {
                if (EntityViewerLookup.useDirectScan(entities.size, RANGE)) {
                    EntityViewerLookup.directScan(entities, resolver, 0, 0, RANGE, sink)
                } else {
                    EntityViewerLookup.chunksInRangeRectangular(0, 0, RANGE, bucketProbe)
                }
            }

            println(
                "%7d | %11d ns | %9d ns | %8d ns | %6d ns".format(
                    playerCount, spiral, rect, direct, hybrid,
                ),
            )
        }
        println("(blackhole $blackhole)")
    }

    /** Returns the average ns per invocation after warmup. */
    private inline fun measure(operation: () -> Unit): Long {
        repeat(WARMUP_ITERATIONS) { operation() }
        val start = System.nanoTime()
        repeat(MEASURED_ITERATIONS) { operation() }
        return (System.nanoTime() - start) / MEASURED_ITERATIONS
    }

    private companion object {
        const val RANGE = 32
        const val SPREAD_CHUNKS = 48
        const val WARMUP_ITERATIONS = 400
        const val MEASURED_ITERATIONS = 600
    }
}
