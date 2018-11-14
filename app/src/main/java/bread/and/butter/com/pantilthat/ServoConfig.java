package bread.and.butter.com.pantilthat;

public class ServoConfig {
  private final int minPulse;
  private final int maxPulse;
  private final Integer timeout;

  public ServoConfig(int minPulse, int maxPulse, Integer timeout) {
    this.minPulse = minPulse;
    this.maxPulse = maxPulse;
    this.timeout = timeout;
  }

  public ServoConfig() {
    this(575, 2325, null);
  }
}
