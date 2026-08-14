package dev.slne.minestom.lobby.server.instrumentation;

import dev.slne.minestom.lobby.server.instrumentation.mixin.InstrumentationMixinService;
import java.lang.instrument.Instrumentation;
import me.lucko.luckperms.minestom.dependencies.LuckPermsAgent;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;

@NullMarked
public final class LobbyAgent {

  private LobbyAgent() {
  }

  public static void agentmain(String agentArgs, Instrumentation instrumentation) {
    System.out.println("=== ASM DEBUG ===");
    System.out.println("ASM class: " + org.objectweb.asm.Opcodes.class);
    System.out.println(
        "ASM source: " +
            org.objectweb.asm.Opcodes.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
    );

    for (String field : new String[]{"V23", "V24", "V25", "V26"}) {
      try {
        var value = org.objectweb.asm.Opcodes.class
            .getField(field)
            .get(null);

        System.out.println(field + " = " + value);
      } catch (NoSuchFieldException | IllegalAccessException e) {
        System.out.println(field + " = MISSING");
      }
    }

    System.out.println("=== /ASM DEBUG ===");

    try {
      InstrumentationMixinService.setInstrumentation(instrumentation);
      MixinBootstrap.init();
      Mixins.addConfiguration("mixins.surf-lobby.json");

      LuckPermsAgent.agentmain(agentArgs, instrumentation);
    } catch (Throwable e) {
      e.printStackTrace();
      throw new RuntimeException("Failed to bootstrap surf lobby instrumentation", e);
    }
  }
}
