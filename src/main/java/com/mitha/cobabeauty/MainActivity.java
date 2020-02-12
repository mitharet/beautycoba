package com.mitha.cobabeauty;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.Face;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;

import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.seerslab.argear.exceptions.NetworkException;
import com.seerslab.argear.exceptions.SignedUrlGenerationException;
import com.seerslab.argear.session.ARGAuth;
import com.seerslab.argear.session.ARGFrame;
import com.seerslab.argear.session.ARGSession;
import com.seerslab.argear.session.config.ARGCameraConfig;
import com.seerslab.argear.session.config.ARGConfig;
import com.seerslab.argear.session.config.ARGInferenceConfig;

import java.util.EnumSet;
import java.util.Set;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import static com.mitha.cobabeauty.http.ServicesFactory.API_KEY;
import static com.mitha.cobabeauty.http.ServicesFactory.API_URL;
import static com.mitha.cobabeauty.http.ServicesFactory.AUTH_KEY;
import static com.mitha.cobabeauty.http.ServicesFactory.SECRET_KEY;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ARGConfig config = new ARGConfig(API_URL, API_KEY, SECRET_KEY, AUTH_KEY);

        Set<ARGInferenceConfig.Feature> inferenceConfig = EnumSet.of(ARGInferenceConfig.Feature.FACE_TRACKING, ARGInferenceConfig.Feature.FACE_LANDMARK_2D);

        final ARGSession argsession = new ARGSession(this, config, inferenceConfig);



        ReferenceCamera.CameraListener cameraListener;
        cameraListener = new ReferenceCamera.CameraListener() {
            @Override
            public void setConfig(int previewWidth,
                                  int previewHeight,
                                  float verticalFov,
                                  float horizontalFov,
                                  int orientation,
                                  boolean isFrontFacing,
                                  float fps) {
                argsession.setCameraConfig(new ARGCameraConfig(previewWidth,
                        previewHeight,
                        verticalFov,
                        horizontalFov,
                        orientation,
                        isFrontFacing,
                        fps));
            }


            // region - for camera api 1
            @Override
            public void updateFaceRects(Camera.Face[] faces) {
                // Send face related information from camera to ARGear
                argsession.updateFaceRects(faces);
            }

            @Override
            public void feedRawData(byte[] data) {
                // Send preview frame raw data from camera device to ARGear
                argsession.feedRawData(data);
            }
            // endregion

            // region - for camera api 2
            @Override
            public void updateFaceRects(int numFaces, int[][] bbox) {
                // Send face related information from camera to ARGear
                argsession.updateFaceRects(numFaces, bbox);
            }

            @Override
            public void feedRawData(Image data) {
                // Send preview frame image from camera device to ARGear
                argsession.feedRawData(data);
            }
            // endregion
        };

        ScreenRenderer mScreenRenderer = new ScreenRenderer();
        GLView.GLViewListener glViewListener = new GLView.GLViewListener() {
            @Override
            public void onSurfaceCreated(GL10 gl, EGLConfig config) {
                mScreenRenderer.create(gl, config);
            }

            @Override
            public void onDrawFrame(GL10 gl, int width, int height) {
                ARGFrame frame = argsession.drawFrame(gl, width, height);
                mScreenRenderer.draw(frame, width, height);
            }
        };

        argsession.auth().requestSignedUrl(item.zipFileUrl, item.title, item.type, new ARGAuth.Callback() {

            @Override
            public void onSuccess(String url) {
                // Using obtained signed url, request download.
                requestDownload(path, url, item.uuid, isArItem);
            }

            @Override
            public void onError(Throwable e) {
                if (e instanceof SignedUrlGenerationException) {
                    Log.e(TAG,"SignedUrlGenerationException !! ");
                } else if (e instanceof NetworkException) {
                    Log.e(TAG,"NetworkException !!");
                }
            }

        });

        argsession.resume();
        argsession.pause();
        camera.changeCameraFacing();
    }

    private class FaceDetecionCallBack implements Camera.FaceDetectionListener {
        @Override
        public void onFaceDetection(Face[] faces, Camera camera) {
            listener.updateFaceRects(faces);
        }
    }

    private class CameraPreviewCallback implements Camera.PreviewCallback {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            listener.feedRawData(data.array());
        }
    }

    private CameraCaptureSession.CaptureCallback mCaptureCallback
            = new CameraCaptureSession.CaptureCallback() {
        private void process(CaptureResult result) {
            Integer mode = result.get(CaptureResult.STATISTICS_FACE_DETECT_MODE);
            Face[] faces = result.get(CaptureResult.STATISTICS_FACES);
            if(faces != null && mode != null) {
                if (faces.length > 0) {
                    int[][] bbox = new int[MAXFACE][4];
                    Rect rect;
                    for (int i = 0; i < faces.length; ++i) {
                        rect = faces[i].getBounds();
                        bbox[i][0] = rect.left*mPreviewSize[0]/mCameraSensorResolution.getWidth();
                        bbox[i][1] = rect.top*mPreviewSize[1]/mCameraSensorResolution.getHeight();
                        bbox[i][2] = rect.right*mPreviewSize[0]/mCameraSensorResolution.getWidth();
                        bbox[i][3] = rect.bottom*mPreviewSize[1]/mCameraSensorResolution.getHeight();
                    }
                    listener.updateFaceRects(faces.length, bbox);
                }
            }
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session,
                                        CaptureRequest request,
                                        CaptureResult partialResult) {
            process(partialResult);
        }

        @Override
        public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                       @NonNull CaptureRequest request,
                                       @NonNull TotalCaptureResult result) {
            process(result);
        }
    };


    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(final ImageReader reader) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    final Image image = reader.acquireLatestImage();
                    if (image != null) {
                        listener.feedRawData(image);
                        image.close();
                    }
                }
            });
        }
    };

    protected void onResume() {
        super.onResume();

//        // argesr session execution
//        argsession.resume();

        // arcore session creation
        arcoreSession = new Session(MainActivity.this, EnumSet.of(Session.Feature.FRONT_CAMERA));
        Config config = new Config(session);
        config.setAugmentedFaceMode(Config.AugmentedFaceMode.MESH3D);
        arcoreSession.configure(config);

        // arcore session execution
        arcoreSession.resume();

    }
}
