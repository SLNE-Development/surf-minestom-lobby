package dev.slne.minestom.lobby.server.instrumentation.mixin;

import org.jspecify.annotations.NonNull;
import org.spongepowered.asm.service.IPropertyKey;

record StringPropertyKey(String name) implements IPropertyKey {

  @Override
  public @NonNull String toString() {
    return name;
  }
}