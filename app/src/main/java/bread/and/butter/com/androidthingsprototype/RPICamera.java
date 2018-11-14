package bread.and.butter.com.androidthingsprototype;

import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.util.Log;
import android.util.Size;

import java.util.Collections;

import static android.content.Context.CAMERA_SERVICE;

public class RPICamera {
  private static final String TAG = RPICamera.class.getSimpleName();

  private static final int IMAGE_WIDTH = 320;
  private static final int IMAGE_HEIGHT = 240;
  private static final int MAX_IMAGES = 1;

  private CameraDevice cameraDevice;
  private CameraCaptureSession captureSession;
  private ImageReader mImageReader;

  private RPICamera() {
  }

  private static class InstanceHolder {
    private static RPICamera camera = new RPICamera();
  }

  public static RPICamera getInstance() {
    return InstanceHolder.camera;
  }

  public void initializeCamera(Context context,
                               Handler backgroundHandler,
                               ImageReader.OnImageAvailableListener imageAvailableListener) {
    // Discover the camera instance
    CameraManager manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
    String[] camIds = {};
    try {
      camIds = manager.getCameraIdList();
    } catch (CameraAccessException e) {
      Log.e(TAG, "Cam access exception getting IDs", e);
    }
    if (camIds.length < 1) {
      Log.e(TAG, "No cameras found");
      return;
    }
    String id = camIds[0];
    Log.d(TAG, "Using camera id " + id);

    // Initialize the image processor
    mImageReader = ImageReader.newInstance(IMAGE_WIDTH, IMAGE_HEIGHT,
        ImageFormat.JPEG, MAX_IMAGES);
    mImageReader.setOnImageAvailableListener(
        imageAvailableListener, backgroundHandler);

    // Open the camera resource
    try {
      manager.openCamera(id, stateCallback, backgroundHandler);
    } catch (CameraAccessException cae) {
      Log.d(TAG, "Camera access exception", cae);
    }
  }

  private final CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
    @Override
    public void onOpened(CameraDevice inputCameraDevice) {
      Log.d(TAG, "Opened camera.");
      cameraDevice = inputCameraDevice;
    }

    @Override
    public void onDisconnected(CameraDevice inputCameraDevice) {
      Log.d(TAG, "Camera disconnected, closing.");
      inputCameraDevice.close();
    }

    @Override
    public void onError(CameraDevice inputCameraDevice, int i) {
      Log.d(TAG, "Camera device error, closing.");
      inputCameraDevice.close();
    }

    @Override
    public void onClosed(CameraDevice inputCameraDevice) {
      Log.d(TAG, "Closed camera, releasing");
      cameraDevice = null;
    }
  };

  /**
   * Begin a still image capture
   */
  public void takePicture() {
    if (cameraDevice == null) {
      Log.e(TAG, "Cannot capture image. Camera not initialized.");
      return;
    }

    // Here, we create a CameraCaptureSession for capturing still images.
    try {
      cameraDevice.createCaptureSession(
          Collections.singletonList(mImageReader.getSurface()),
          sessionCallback,
          null);
    } catch (CameraAccessException cae) {
      Log.e(TAG, "access exception while preparing pic", cae);
    }
  }

  private CameraCaptureSession.StateCallback sessionCallback =
      new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
          // The camera is already closed
          if (cameraDevice == null) {
            return;
          }

          // When the session is ready, we start capture.
          captureSession = cameraCaptureSession;
          triggerImageCapture();
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
          Log.e(TAG, "Failed to configure camera");
        }
      };

  private void triggerImageCapture() {
    try {
      final CaptureRequest.Builder captureBuilder =
          cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
      captureBuilder.addTarget(mImageReader.getSurface());
      captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
      Log.d(TAG, "Session initialized.");
      captureSession.capture(captureBuilder.build(), captureCallback, null);
    } catch (CameraAccessException cae) {
      Log.e(TAG, "camera capture exception", cae);
    }
  }

  private final CameraCaptureSession.CaptureCallback captureCallback =
      new CameraCaptureSession.CaptureCallback() {

        @Override
        public void onCaptureProgressed(CameraCaptureSession session,
                                        CaptureRequest request,
                                        CaptureResult partialResult) {
          Log.d(TAG, "Partial result");
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session,
                                       CaptureRequest request,
                                       TotalCaptureResult result) {
          if (session != null) {
            session.close();
            captureSession = null;
            Log.d(TAG, "CaptureSession closed");
          }
        }
      };

  public void shutDown() {
    if (cameraDevice != null) {
      cameraDevice.close();
    }
  }

  public static void dumpFormatInfo(Context context) {
    CameraManager manager = (CameraManager) context.getSystemService(CAMERA_SERVICE);
    String[] camIds = {};
    try {
      camIds = manager.getCameraIdList();
    } catch (CameraAccessException e) {
      Log.d(TAG, "Cam access exception getting IDs");
    }
    if (camIds.length < 1) {
      Log.d(TAG, "No cameras found");
    }
    String id = camIds[0];
    Log.d(TAG, "Using camera id " + id);
    try {
      CameraCharacteristics characteristics = manager.getCameraCharacteristics(id);
      StreamConfigurationMap configs = characteristics.get(
          CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
      for (int format : configs.getOutputFormats()) {
        Log.d(TAG, "Getting sizes for format: " + format);
        for (Size s : configs.getOutputSizes(format)) {
          Log.d(TAG, "\t" + s.toString());
        }
      }
      int[] effects = characteristics.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS);
      for (int effect : effects) {
        Log.d(TAG, "Effect available: " + effect);
      }
    } catch (CameraAccessException e) {
      Log.d(TAG, "Cam access exception getting characteristics.");
    }
  }
}
