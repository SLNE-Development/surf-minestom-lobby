package dev.slne.minestom.lobby.server.instrumentation.mixin;

import java.util.HashMap;
import java.util.Map;
import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

public final class InstrumentationGlobalPropertyService implements IGlobalPropertyService {
  private final Map<String, Object> properties = new HashMap<>();

  @Override
  public IPropertyKey resolveKey(String name) {
    return new StringPropertyKey(name);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getProperty(IPropertyKey key) {
    return (T) properties.get(keyString(key));
  }

  @Override
  public void setProperty(IPropertyKey key, Object value) {
    String name = keyString(key);

    if (value == null) {
      properties.remove(name);
    } else {
      properties.put(name, value);
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getProperty(IPropertyKey key, T defaultValue) {
    return (T) properties.getOrDefault(
        keyString(key),
        defaultValue
    );
  }

  @Override
  public String getPropertyString(
      IPropertyKey key,
      String defaultValue
  ) {
    Object value = properties.get(keyString(key));

    return value != null
        ? value.toString()
        : defaultValue;
  }

  private static String keyString(IPropertyKey key) {
    if (!(key instanceof StringPropertyKey(String name))) {
      throw new IllegalArgumentException(
          "Unknown property key implementation: "
              + key.getClass().getName()
      );
    }

    return name;
  }
}
