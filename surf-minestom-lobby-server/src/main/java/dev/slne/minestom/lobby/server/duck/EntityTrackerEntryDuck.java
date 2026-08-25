package dev.slne.minestom.lobby.server.duck;

import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.Nullable;

public interface EntityTrackerEntryDuck {

  @Nullable
  Point surf$lastPosition();
}
