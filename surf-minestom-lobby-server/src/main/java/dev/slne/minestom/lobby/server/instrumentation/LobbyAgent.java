package dev.slne.minestom.lobby.server.instrumentation;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import dev.slne.minestom.lobby.server.instrumentation.mixin.InstrumentationMixinService;
import java.lang.instrument.Instrumentation;
import me.lucko.luckperms.minestom.dependencies.LuckPermsAgent;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

@NullMarked
public final class LobbyAgent {

  private LobbyAgent() {
  }

  public static void agentmain(String agentArgs, Instrumentation instrumentation) {
    try {
      DependencyInstaller.install(instrumentation);

      InstrumentationMixinService.setInstrumentation(instrumentation);
      MixinBootstrap.init();
      MixinExtrasBootstrap.init();
      Mixins.addConfiguration("mixins.surf-lobby.json");
      advanceMixinPhases();

      LuckPermsAgent.agentmain(agentArgs, instrumentation);
    } catch (Throwable e) {
      e.printStackTrace();
      throw new RuntimeException("Failed to bootstrap surf lobby instrumentation", e);
    }
  }

  private static void advanceMixinPhases() {
    try {
      var gotoPhase = MixinEnvironment.class.getDeclaredMethod(
          "gotoPhase",
          MixinEnvironment.Phase.class
      );

      gotoPhase.setAccessible(true);

      gotoPhase.invoke(null, MixinEnvironment.Phase.INIT);
      gotoPhase.invoke(null, MixinEnvironment.Phase.DEFAULT);
    } catch (ReflectiveOperationException e) {
      throw new RuntimeException("Failed to advance Mixin phases", e);
    }
  }
}
