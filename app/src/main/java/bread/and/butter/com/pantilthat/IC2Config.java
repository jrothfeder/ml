package bread.and.butter.com.pantilthat;

public class IC2Config {
  private final int retries;
  private final float retryTime;

  public IC2Config(int retries, float retryTime) {
    this.retries = retries;
    this.retryTime = retryTime;
  }

  public IC2Config() {
    this.retries = 10;
    this.retryTime = 0.01f;
  }
}
