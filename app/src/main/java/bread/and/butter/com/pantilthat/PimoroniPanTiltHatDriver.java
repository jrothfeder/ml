package bread.and.butter.com.pantilthat;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.things.pio.I2cDevice;
import com.google.android.things.pio.PeripheralManager;

import java.io.IOException;

public class PimoroniPanTiltHatDriver implements AutoCloseable {
  private static final String I2C_DEVICE_NAME = "PimoroniPanTiltHat";
  private static final String TAG = "PimoroniPanTiltHatDriver";

  public enum LightMode {
    PWM(0),
    WS2812(1);

    public final int v;
    LightMode(int v) {
      this.v = v;
    }
  }

  public enum LightType {
    RGB(0),
    GRB(1),
    RGBW(2),
    GRBW(3);

    public final int v;
    LightType(int v) {
      this.v = v;
    }
  }

  public enum Register {
    CONFIG(0x00),
    SERVO1(0x01),
    SERVO2(0x03),
    WS2812(0x05),
    UPDATE(0x4E);

    public final int v;
    Register(int v) {
      this.v = v;
    }

  }
  private final ServoConfig servo1Conf;
  private final ServoConfig servo2Conf;

  private boolean servo1Enabled;
  private boolean servo2Enabled;
  private boolean lightOn;

  @Nullable
  private final LightConfig lightConfig;

  private I2cDevice device;

  public static PimoroniPanTiltHatDriver create(LightConfig lightConfig) throws IOException {
    return PimoroniPanTiltHatDriver.create(
            0x15, new ServoConfig(), new ServoConfig(), lightConfig);
  }

  public static PimoroniPanTiltHatDriver create() throws IOException {
     return PimoroniPanTiltHatDriver.create(
             0x15, new ServoConfig(), new ServoConfig(), null);
  }

  public static PimoroniPanTiltHatDriver create(
      int ic2Address,
      ServoConfig servo1,
      ServoConfig servo2,
      @Nullable LightConfig lightConfig) throws IOException {
    return new PimoroniPanTiltHatDriver(
        PeripheralManager.getInstance().openI2cDevice(I2C_DEVICE_NAME, ic2Address),
        servo1,
        servo2,
        lightConfig);
  }

  PimoroniPanTiltHatDriver(
      I2cDevice device,
      ServoConfig servo1Conf,
      ServoConfig servo2Conf,
      LightConfig lightConfig) {
    this.device = device;
    this.servo1Conf = servo1Conf;
    this.servo2Conf = servo2Conf;
    this.lightConfig = lightConfig;

    servo1Enabled = false;
    servo2Enabled = false;
    lightOn = false;

    setConfig();
  }

  private void setConfig() {
    byte config = 0;
    config |= (isServo1Enabled() ? 1 : 0);
    config |= (isServo2Enabled() ? 1 : 0)  << 1;
    config |= (areLightsEnabled() ? 1 : 0) << 2;
    config |= (areLightsEnabled() ? lightConfig.lightMode.v : LightMode.WS2812.v) << 3;
    config |= (areLightsEnabled() ? (lightOn ? 1 : 0) : 0) << 4;

    try {
      device.writeRegByte(Register.CONFIG.v, config);
    } catch (IOException e) {
      Log.w(TAG, "Failed to write config " + config + " to register " + Register.CONFIG.v);
    }
  }

  private boolean isServo1Enabled() {
    return servo1Enabled;
  }

  private boolean isServo2Enabled() {
    return servo2Enabled;
  }

  private boolean areLightsEnabled() {
    return lightConfig != null;
  }

  @Override
  public void close() throws Exception {
    device.close();
  }

  /**
   * Get position of servo one in degrees
   */
  private int getServoOnePos() {
    return -1;
  }

  /**
   * Get position of servo one in degrees
   */
  private int getServoTwoPos() {
    return -1;
  }

  private void setServoOnePos(int degrees) {

  }

  private void setServoTwoPos(int degrees) {

  }

  /**
   * Pan by the given number of degrees
   */
  public void pan(int degrees) {
    setServoOnePos(degrees);
  }

  /**
   * Tilt by the given number of degrees
   */
  public void tilt(int degrees) {
    setServoTwoPos(degrees);
  }

  /**
   * @return the current pan in degrees
   */
  public int getPan() {
    return getServoOnePos();
  }

  /**
   * @return the current tilt in degrees
   */
  public int getTilt() {
    return getServoTwoPos();
  }
}
