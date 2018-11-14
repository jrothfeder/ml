package bread.and.butter.com.pantilthat;

class ServoConfig {
  private final int minPulse;
  private final int maxPulse;
  private final Integer timeout;
  private final int range;

  ServoConfig(int minPulse, int maxPulse, Integer timeout) {
    this.minPulse = minPulse;
    this.maxPulse = maxPulse;
    this.timeout = timeout;
    range = maxPulse - minPulse;
  }

  ServoConfig() {
    this(575, 2325, null);
  }

  /**
   * Convert degrees into a servo pulse time in microseconds
   * @param angle in degrees from -90 to 90
   */
  int degrees2Us(int angle) {
    checkRange(angle);
    int servoAngle = angle + 90;
    double us = (range / 180.0) * servoAngle;
    return minPulse + (int)us;
  }

  private void checkRange(int angle) {
    if(angle < -90 || angle > 90) {
      throw new RuntimeException("Invalid angle for servo requested "
              + angle + " must be between -90 and 90");
    }
  }
}
