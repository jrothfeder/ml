package bread.and.butter.com.pantilthat;

public class LightConfig {
  public final PimoroniPanTiltHatDriver.LightMode lightMode;
  public final PimoroniPanTiltHatDriver.LightType lightType;

  public LightConfig(
      PimoroniPanTiltHatDriver.LightMode lightMode,
      PimoroniPanTiltHatDriver.LightType lightType) {
    this.lightMode = lightMode;
    this.lightType = lightType;
  }
}
