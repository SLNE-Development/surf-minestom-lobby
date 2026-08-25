package dev.slne.minestom.lobby.server.mixin;

import dev.slne.minestom.lobby.server.duck.EntityTrackerEntryDuck;
import net.minestom.server.coordinate.Point;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minestom.server.instance.EntityTrackerImpl$EntityTrackerEntry", remap = false)
abstract class EntityTrackerEntryMixin implements EntityTrackerEntryDuck {

  @Accessor(value = "lastPosition", remap = false)
  @Override
  public abstract Point surf$lastPosition();
}
