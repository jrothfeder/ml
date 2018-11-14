package bread.and.butter.com.androidthingsprototype;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.GpioCallback;
import com.google.android.things.pio.PeripheralManager;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.io.IOException;
import java.util.List;

import bread.and.butter.com.pantilthat.PimoroniPanTiltHatDriver;

public class HomeActivity extends Activity {
  private static final String TAG = "HomeActivity";
  private static final String BUTTON_PIN_NAME = "BCM6";
  private Gpio buttonGpio;

  private RPICamera camera;
  private HandlerThread cameraThread;
  private PimoroniPanTiltHatDriver panTiltHatDriver;

  private final FirebaseVisionFaceDetectorOptions options =
      new FirebaseVisionFaceDetectorOptions.Builder()
          .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
          .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
          .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
          .build();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initCamera();
    initPIO();
    initPanTilt();
  }

  private void initPanTilt() {
    try {
      panTiltHatDriver = PimoroniPanTiltHatDriver.create();
    } catch (IOException e) {
      Log.w(TAG, "Failed to initialize pan tilt driver.");
    }
    panTiltHatDriver.pan(90);
    panTiltHatDriver.tilt(90);
  }

  private void initCamera() {
    // We need permission to access the camera
    if (checkSelfPermission(Manifest.permission.CAMERA)
        != PackageManager.PERMISSION_GRANTED) {
      // A problem occurred auto-granting the permission
      Log.e(TAG, "No permission to use camera :(");
      return;
    }

    cameraThread = new HandlerThread("CameraBackground");
    cameraThread.start();
    Handler cameraHandler = new Handler(cameraThread.getLooper());

    camera = RPICamera.getInstance();
    camera.initializeCamera(this, cameraHandler, onImageAvailableListener);
  }

  private ImageReader.OnImageAvailableListener onImageAvailableListener =
      new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
          Image image = reader.acquireLatestImage();

          FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromMediaImage(image, FirebaseVisionImageMetadata.ROTATION_0);
          FirebaseVisionFaceDetector detector = FirebaseVision.getInstance()
              .getVisionFaceDetector(options);
          Task<List<FirebaseVisionFace>> result =
              detector.detectInImage(firebaseVisionImage)
                  .addOnSuccessListener(
                      new OnSuccessListener<List<FirebaseVisionFace>>() {
                        @Override
                        public void onSuccess(List<FirebaseVisionFace> faces) {
                          for(FirebaseVisionFace face : faces) {
                            if (face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                              float smileProb = face.getSmilingProbability();
                              Log.i(TAG, "Smile prob is " + smileProb);
                            }
                            if (face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                              float rightEyeOpenProb = face.getRightEyeOpenProbability();
                              Log.i(TAG, "Right eye open prob is " + rightEyeOpenProb);
                            }
                          }
                        }
                      })
                  .addOnFailureListener(
                      new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                          Log.w(TAG,"Failed to analyze image", e);
                        }
                      });
          image.close();
        }
      };

  private void initPIO() {
    PeripheralManager manager = PeripheralManager.getInstance();
    try {
      buttonGpio = manager.openGpio(BUTTON_PIN_NAME);
      buttonGpio.setDirection(Gpio.DIRECTION_IN);
      buttonGpio.setEdgeTriggerType(Gpio.EDGE_FALLING);
      buttonGpio.registerGpioCallback(buttonCallback);

    } catch (IOException e) {
      Log.w(TAG, "Could not open GPIO pins", e);
    }
  }

  protected void onDestroy() {
    super.onDestroy();
    camera.shutDown();
    cameraThread.quitSafely();
    if (buttonGpio != null) {
      buttonGpio.unregisterGpioCallback(buttonCallback);
    }
  }

  private GpioCallback buttonCallback = new GpioCallback() {
    @Override
    public boolean onGpioEdge(Gpio gpio) {
      camera.takePicture();
      Log.i(TAG, "trying to take a picture");

      // Step 5. Return true to keep callback active.
      return true;
    }
  };
}
