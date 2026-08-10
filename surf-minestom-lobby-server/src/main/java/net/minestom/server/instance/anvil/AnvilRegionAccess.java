package net.minestom.server.instance.anvil;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

/**
 * Access to the package-private {@link RegionFile} class, which is used to read and write chunks in
 * the Anvil format.
 */
@NullMarked
public final class AnvilRegionAccess implements AutoCloseable {

  private final RegionFile delegate;

  private AnvilRegionAccess(RegionFile delegate) {
    this.delegate = requireNonNull(delegate, "delegate");
  }

  /**
   * @param path an {@code r.X.Z.mca} file
   */
  public static AnvilRegionAccess open(Path path) throws IOException {
    requireNonNull(path, "path");
    return new AnvilRegionAccess(new RegionFile(path));
  }

  /**
   * @see RegionFile#readChunkData(int, int)
   */
  public @Nullable CompoundBinaryTag readChunk(int chunkX, int chunkZ) throws IOException {
    return delegate.readChunkData(chunkX, chunkZ);
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }
}
