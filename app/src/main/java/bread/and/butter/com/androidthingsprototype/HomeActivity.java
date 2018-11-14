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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

public class HomeActivity extends Activity {
  private static final String TAG = "HomeActivity";
  private static final String BUTTON_PIN_NAME = "BCM6";
  private Gpio buttonGpio;

  private RPICamera camera;
  private HandlerThread cameraThread;
  private FirebaseStorage storage;

  private final FirebaseVisionFaceDetectorOptions options =
      new FirebaseVisionFaceDetectorOptions.Builder()
          .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
          .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
          .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
          .build();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    storage = FirebaseStorage.getInstance();
    initCamera();
    initPIO();
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


          // get image bytes
//          ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
//          final byte[] imageBytes = new byte[imageBuf.remaining()];
//          imageBuf.get(imageBytes);
          image.close();

//          postprocess(imageBytes);
        }
      };

  private void postprocess(final byte[] imageBytes) {
    if (imageBytes != null) {
      Log.i(TAG,"Got a picture with " + imageBytes.length + " bytes!!!");
      final StorageReference imageRef = storage.getReference().child(UUID.randomUUID().toString());
      UploadTask task = imageRef.putBytes(imageBytes);
      task.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
        @Override
        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
          // mark image in the database
          Log.i(TAG, "Image upload successful");
        }
      }).addOnFailureListener(new OnFailureListener() {
        @Override
        public void onFailure(@NonNull Exception e) {
          // clean up this entry
          Log.w(TAG, "Unable to upload image to Firebase");
        }
      });
    }
  }

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
