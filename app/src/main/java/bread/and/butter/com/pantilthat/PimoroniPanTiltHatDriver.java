package bread.and.butter.com.pantilthat;
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

  private I2cDevice device;
  private final ServoConfig servoConfig = new ServoConfig();

  public static PimoroniPanTiltHatDriver create() throws IOException {
     return PimoroniPanTiltHatDriver.create(0x15);
  }

  public static PimoroniPanTiltHatDriver create(int ic2Address) throws IOException {
    return new PimoroniPanTiltHatDriver(
        PeripheralManager.getInstance().openI2cDevice(I2C_DEVICE_NAME, ic2Address));
  }

  PimoroniPanTiltHatDriver(I2cDevice device) {
    this.device = device;
    setConfig();
  }

  private void setConfig() {
    byte config = 0;
    config |= 1;                        // servo 1 enabled
    config |= 1  << 1;                  // servo 2 enabled
    config |= 0 << 2;                   // lights enabled
    config |= LightMode.WS2812.v << 3;  // light mode
    config |= 0 << 4;                   // lights on

    try {
      device.writeRegByte(Register.CONFIG.v, config);
    } catch (IOException e) {
      Log.w(TAG, "Failed to write config " + config + " to register " + Register.CONFIG.v, e);
    }
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

  /**
   * Angle in degrees from -90 to 90
   */
  private void setServoOnePos(int angle) {
    setServoPos(angle, Register.SERVO1.v);
  }

  /**
   * Angle in degrees from -90 to 90
   */
  private void setServoTwoPos(int angle) {
    setServoPos(angle, Register.SERVO2.v);
  }

  private void setServoPos(int angle, int addr) {
    int us = servoConfig.degrees2Us(angle);
    try {
      device.writeRegWord(addr, (short)us);
    } catch (IOException e) {
      Log.w(TAG, "Error writing angle to servo", e);
    }
  }
}