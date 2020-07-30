package com.example.kioskapp.menu;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kioskapp.BuildConfig;
import com.example.kioskapp.R;
import com.example.kioskapp.camera.CameraSource;
import com.example.kioskapp.customview.ThermalView;
import com.flir.thermalsdk.ErrorCode;
import com.flir.thermalsdk.androidsdk.ThermalSdkAndroid;
import com.flir.thermalsdk.androidsdk.image.BitmapAndroid;
import com.flir.thermalsdk.androidsdk.live.connectivity.UsbPermissionHandler;
import com.flir.thermalsdk.image.Point;
import com.flir.thermalsdk.image.ThermalImage;
import com.flir.thermalsdk.image.fusion.FusionMode;
import com.flir.thermalsdk.live.Camera;
import com.flir.thermalsdk.live.CommunicationInterface;
import com.flir.thermalsdk.live.Identity;
import com.flir.thermalsdk.live.connectivity.ConnectionStatusListener;
import com.flir.thermalsdk.live.discovery.DiscoveryEventListener;
import com.flir.thermalsdk.live.discovery.DiscoveryFactory;
import com.flir.thermalsdk.live.streaming.ThermalImageStreamListener;
import com.flir.thermalsdk.log.ThermalLog;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

public class ThermalActivity extends AppCompatActivity {
    private static final String TAG = "ThermalActivity";

    //ML Kit Face Detector and options
    private FaceDetector detector;
    private FaceDetectorOptions defaultOptions;

    private static List<Face> faceList;

    //FLIR Variables
    private Identity connectedIdentity = null;
    private ImageView msxImage;
    private ImageView photoImage;

    //Temperature modules
    TextView tempView;
    TextView secondTempView;
    TextView thirdTempView;

    public static double getTemp() {
        return temp;
    }

    static double temp;

    //Discovered FLIR cameras
    LinkedList<Identity> foundCameraIdentities = new LinkedList<>();

    //A FLIR Camera
    private Camera camera;

    private LinkedBlockingQueue<FrameDataHolder> framesBuffer = new LinkedBlockingQueue(21);
    private UsbPermissionHandler usbPermissionHandler = new UsbPermissionHandler();

    public void onCameraSwitch(View view) {

    }

    public void onCalibrate(View view) {
        camera.getRemoteControl().getCalibration().nuc();
        faceList.clear();
        faceList = null;
    }

    public void onDebug(View view) {
        connect(getCppEmulator());

    }

    public void onConnect(View view) {
        connect(getFlirOne());
    }

    /**
     * Show message on the screen
     */
    public interface ShowMessage {
        void show(String message);
    }

    public interface StreamDataListener {
        void images(FrameDataHolder dataHolder);
        void images(Bitmap msxBitmap, Bitmap dcBitmap);
    }

    private ShowMessage showMessage = new ShowMessage() {
        @Override
        public void show(String message) {
            Toast.makeText(ThermalActivity.this, message, Toast.LENGTH_SHORT).show();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thermal);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ThermalLog.LogLevel enableLoggingInDebug = BuildConfig.DEBUG ? ThermalLog.LogLevel.DEBUG : ThermalLog.LogLevel.NONE;
        msxImage = findViewById(R.id.msx_image);
        photoImage = findViewById(R.id.photo_image);

        tempView = findViewById(R.id.tempView_id);
        secondTempView = findViewById(R.id.tempView2_id);
        thirdTempView = findViewById(R.id.tempView3_id);

        //ThermalSdkAndroid has to be initiated from a Activity with the Application Context to prevent leaking Context,
        // and before ANY using any ThermalSdkAndroid functions
        //ThermalLog will show log from the Thermal SDK in standards android log framework
        ThermalSdkAndroid.init(getApplicationContext(), enableLoggingInDebug);



        //Configure face detector options
        defaultOptions =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .enableTracking()
                        .build();

        //Instantiate face detector
        detector = FaceDetection.getClient(defaultOptions);

        //Keep trying to connect automatically to FLIR One while activity is running
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while(connectedIdentity == null){
//                    try {
//                        Thread.sleep(3000);
//                        runOnUiThread(new Runnable() {
//                            @Override
//                            public void run() {
//                                connect(getFlirOne());
//                            }
//                        });
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//
//                }
//            }
//        }).start();

        startDiscovery(cameraDiscoveryListener);

    }

    @Override
    /**
     * Do something when the app is in the foreground
     */
    protected void onResume() {
        super.onResume();
//        startCameraSource();
    }
    @Override
    /**
     * Stops trying to discover when the app is in the background
     */
    protected void onPause() {
        super.onPause();
        disconnect();
        stopDiscovery();
    }
    @Override
    /**
     * Releases the cameraSource right before the activity is destroyed
     */
    protected void onDestroy() {
        super.onDestroy();
        stopDiscovery();
        disconnect();
    }

    /**
     * Start discovery of USB and Emulators
     */
    public void startDiscovery(DiscoveryEventListener cameraDiscoveryListener) {
        DiscoveryFactory.getInstance().scan(cameraDiscoveryListener, CommunicationInterface.EMULATOR, CommunicationInterface.USB);
    }

    /**
     * Stop discovery of USB and Emulators
     */
    public void stopDiscovery() {
        DiscoveryFactory.getInstance().stop(CommunicationInterface.EMULATOR, CommunicationInterface.USB);
    }

    @Nullable
    public Identity getFlirOne() {
        for (Identity foundCameraIdentity : foundCameraIdentities) {
            boolean isFlirOneEmulator = foundCameraIdentity.deviceId.contains("EMULATED FLIR ONE");
            boolean isCppEmulator = foundCameraIdentity.deviceId.contains("C++ Emulator");
            if (!isFlirOneEmulator && !isCppEmulator) {
                return foundCameraIdentity;
            }
        }

        return null;
    }

    @Nullable
    public Identity getCppEmulator() {
        for (Identity foundCameraIdentity : foundCameraIdentities) {
            if (foundCameraIdentity.deviceId.contains("C++ Emulator")) {
                return foundCameraIdentity;
            }
        }
        return null;
    }

    /**
     * Connect to a Camera
     */
    private void connect(Identity identity) {
        //We don't have to stop a discovery but it's nice to do if we have found the camera that we are looking for
        stopDiscovery();

        if (connectedIdentity != null) {
            Log.d(TAG, "connect(), in *this* code sample we only support one camera connection at the time");
            showMessage.show("connect(), in *this* code sample we only support one camera connection at the time");
            return;
        }

        if (identity == null) {
            Log.d(TAG, "connect(), can't connect, no camera available");
            return;
        }

        connectedIdentity = identity;

        //IF your using "USB_DEVICE_ATTACHED" and "usb-device vendor-id" in the Android Manifest
        // you don't need to request permission, see documentation for more information
        if (UsbPermissionHandler.isFlirOne(identity)) {
            usbPermissionHandler.requestFlirOnePermisson(identity, this, permissionListener);
        } else {
            doConnect(identity);
        }
    }

    private void doConnect(Identity identity) {
        new Thread(() -> {
            camera = new Camera();
            try {
                camera.connect(identity, connectionStatusListener);
            } catch (IOException e) {
                e.printStackTrace();
            }
            runOnUiThread(() -> {
                startStream(streamDataListener);
            });
        }).start();
    }

    private UsbPermissionHandler.UsbPermissionListener permissionListener = new UsbPermissionHandler.UsbPermissionListener() {
        @Override
        public void permissionGranted(Identity identity) {
            doConnect(identity);
        }

        @Override
        public void permissionDenied(Identity identity) {
            ThermalActivity.this.showMessage.show("Permission was denied for identity ");
        }

        @Override
        public void error(UsbPermissionHandler.UsbPermissionListener.ErrorType errorType, final Identity identity) {
            ThermalActivity.this.showMessage.show("Error when asking for permission for FLIR ONE, error:"+errorType+ " identity:" +identity);
        }
    };

    /**
     * Disconnect to a camera
     */
    private void disconnect() {
        connectedIdentity = null;
        Log.d(TAG, "disconnect() called with: connectedIdentity = [" + connectedIdentity + "]");
        new Thread(() -> {
            if (camera == null) {
                return;
            }
            if (camera.isGrabbing()) {
                camera.unsubscribeAllStreams();
            }
            camera.disconnect();
            runOnUiThread(() -> {
                //Notify user camera has been disconnected
            });
        }).start();
    }

    private StreamDataListener streamDataListener = new StreamDataListener() {

        @Override
        public void images(FrameDataHolder dataHolder) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    msxImage.setImageBitmap(dataHolder.msxBitmap);
                    photoImage.setImageBitmap(dataHolder.dcBitmap);
                }
            });
        }



        @Override
        public void images(Bitmap msxBitmap, Bitmap dcBitmap) {

            try {
                framesBuffer.put(new FrameDataHolder(msxBitmap,dcBitmap));

                InputImage image = InputImage.fromBitmap(dcBitmap, CameraSource.getRotationDegrees());
                //Face detection processing of image
                Task<List<Face>> result =
                        detector.process(image)
                                .addOnSuccessListener(
                                        new OnSuccessListener<List<Face>>() {
                                            @Override
                                            public void onSuccess(List<Face> faces) {
                                                faceList = faces;

                                                // Task completed successfully
                                                // ...

                                            }
                                        })
                                .addOnFailureListener(
                                        new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                // Task failed with an exception
                                                // ...
                                            }
                                        });


            } catch (InterruptedException e) {
                //if interrupted while waiting for adding a new item in the queue
                Log.e(TAG,"images(), unable to add incoming images to frames buffer, exception:"+e);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG,"framebuffer size:"+framesBuffer.size());
                    FrameDataHolder poll = framesBuffer.poll();
                    msxImage.setImageBitmap(poll.msxBitmap);
                    photoImage.setImageBitmap(poll.dcBitmap);
                }
            });

        }

    };

    /**
     * Start a stream of {@link ThermalImage}s images from a FLIR ONE or emulator
     */
    public void startStream(StreamDataListener listener) {
        streamDataListener = listener;
        camera.subscribeStream(thermalImageStreamListener);
    }

    /**
     * Called whenever there is a new Thermal Image available, should be used in conjunction with {@link Camera.Consumer}
     */
    private final ThermalImageStreamListener thermalImageStreamListener = new ThermalImageStreamListener() {
        @Override
        public void onImageReceived() {
            //Will be called on a non-ui thread
            Log.d(TAG, "onImageReceived(), we got another ThermalImage");
            withImage(this, handleIncomingImage);
        }
    };

    private void withImage(ThermalImageStreamListener listener, Camera.Consumer<ThermalImage> functionToRun) {
        camera.withImage(listener, functionToRun);
    }

    /**
     * Function to process a Thermal Image and update UI
     */
    private final Camera.Consumer<ThermalImage> handleIncomingImage = new Camera.Consumer<ThermalImage>() {
        @Override
        public void accept(ThermalImage thermalImage) {
            Log.d(TAG, "accept() called with: thermalImage = [" + thermalImage.getDescription() + "]");
            //Will be called on a non-ui thread,
            // extract information on the background thread and send the specific information to the UI thread


            //Get a bitmap with only IR data
            Bitmap msxBitmap;
            {
                msxBitmap = BitmapAndroid.createBitmap(thermalImage.getImage()).getBitMap();
                thermalImage.getFusion().setFusionMode(FusionMode.THERMAL_ONLY);


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Point pt = new Point(thermalImage.getWidth()/2, thermalImage.getHeight()/2);

                        try {
                            int x1 = 0;
                            int y1 = 0;
                            int x2 = 0;
                            int y2 = 0;

                            if (faceList != null) {

//                                for (int i = 0; i < 2; i++) {
//                                    Rect bounds = faceList.get(i).getBoundingBox();
//                                    x1 = bounds.left;
//                                    y1 = bounds.top;
//                                    x2 = bounds.right;
//                                    y2 = bounds.bottom;
//
//                                    //Point where to take thermal reading
//                                    Point facePt = new Point(faceList.get(i).getBoundingBox().centerX() / 2, faceList.get(i).getBoundingBox().centerY() / 2);
//                                    double temp = (thermalImage.getValueAt(facePt) - 273.15) * 9 / 5 + 32;
//                                    tempView.setVisibility(View.VISIBLE);
//                                    tempView.setText(String.format("%.2f °", temp));
//
//                                    if (i == 1) {
//                                        double tempTwo = (thermalImage.getValueAt(facePt) - 273.15) * 9 / 5 + 32;
//                                        secondTempView.setVisibility(View.VISIBLE);
//                                        secondTempView.setText(String.format("%.2f °", tempTwo));
//                                    }
//                                }

                                for (Face face : faceList) {

                                    Rect bounds = face.getBoundingBox();
                                    x1 = bounds.left;
                                    y1 = bounds.top;
                                    x2 = bounds.right;
                                    y2 = bounds.bottom;

                                    if (face.getTrackingId() != null) {

                                        Point facePt = new Point(face.getBoundingBox().centerX()/2, face.getBoundingBox().centerY()/2);
                                        temp = (thermalImage.getValueAt(facePt) - 273.15) * 9 / 5 + 32;
                                        ThermalView.updateThermal(temp);
//                                        Paint paint = new Paint();
//                                        paint.setTextSize(200);
//                                        canvas.drawText(String.format("%.2f °", temp),x1, y1, paint);
//                                        if (face.getTrackingId() == 0) {
//                                            //Point where to take thermal reading
//                                            double temp = (thermalImage.getValueAt(facePt) - 273.15) * 9 / 5 + 32;
////                                            tempView.setVisibility(View.VISIBLE);
////                                            tempView.setText(String.format("%.2f °", temp));
//                                        }
//
//                                        if (face.getTrackingId() == 1) {
//                                            double tempTwo = (thermalImage.getValueAt(facePt) - 273.15) * 9 / 5 + 32;
//                                            secondTempView.setVisibility(View.VISIBLE);
//                                            secondTempView.setText(String.format("%.2f °", tempTwo));
//                                        }
//
//                                        if (face.getTrackingId() > 2) {
//                                            faceList.clear();
//                                        }
                                    }

                                }


//                                    //Face 1 reading
//                                    if (faceList.size() > 0 && faceList.get(0).getLandmark(Landmark.NOSE_BASE) != null){
//
//                                        //Point where to take thermal reading
//                                        Point facePt = new Point((int) faceList.get(0).getLandmark(Landmark.NOSE_BASE).getPosition().x/2,
//                                                (int) faceList.get(0).getLandmark(Landmark.NOSE_BASE).getPosition().y/2);
//
//                                        double temp = (thermalImage.getValueAt(facePt) - 273.15) * 9 / 5 + 32;
//                                        tempView.setVisibility(View.VISIBLE);
//                                        tempView.setText(String.format("%.2f °", temp));
//                                    } else {
//                                        tempView.setVisibility(View.INVISIBLE);
//                                    }
//
//
//                                    //Face 2 reading if available
//                                    if (faceList.size() > 1 && faceList.get(1).getLandmark(Landmark.NOSE_BASE) != null) {
//                                        Point secondFacePt = new Point((int) faceList.get(1).getLandmark(Landmark.NOSE_BASE).getPosition().x/2, (int) faceList.get(1).getLandmark(Landmark.NOSE_BASE).getPosition().y/2);
//                                        double tempTwo = (thermalImage.getValueAt(secondFacePt) - 273.15) * 9 / 5 + 32;
//                                        secondTempView.setVisibility(View.VISIBLE);
//                                        secondTempView.setText(String.format("%.2f °", tempTwo));
//                                    } else {
//                                        secondTempView.setVisibility(View.INVISIBLE);
//                                    }
//
//                                    //Face 3 reading if available
//                                    if (faceList.size() == 3 && faceList.get(2).getLandmark(Landmark.NOSE_BASE) != null) {
//                                        Point thirdFacePt = new Point((int) faceList.get(2).getLandmark(Landmark.NOSE_BASE).getPosition().x/2, (int) faceList.get(2).getLandmark(Landmark.NOSE_BASE).getPosition().y/2);
//                                        double tempThree = (thermalImage.getValueAt(thirdFacePt) - 273.15) * 9 / 5 + 32;
//                                        thirdTempView.setVisibility(View.VISIBLE);
//                                        thirdTempView.setText(String.format("%.2f °", tempThree));
//                                    } else {
//                                        thirdTempView.setVisibility(View.INVISIBLE);
//                                    }
//
//                                    if (faceList.size() > 3) {
//                                        faceList.clear();
//                                    }

                            } else {
                                tempView.setVisibility(View.INVISIBLE);
                                secondTempView.setVisibility(View.INVISIBLE);
                                thirdTempView.setVisibility(View.INVISIBLE);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    }
                });
            }

//Get a bitmap with the visual image, it might have different dimensions then the bitmap from THERMAL_ONLY
            Bitmap dcBitmap = BitmapAndroid.createBitmap(thermalImage.getFusion().getPhoto()).getBitMap();
//            Bitmap dcBitmap;
//            {
//                thermalImage.getFusion().setFusionMode(FusionMode.VISUAL_ONLY);
//                dcBitmap = BitmapAndroid.createBitmap(thermalImage.getImage()).getBitMap();
//            }
//            Bitmap dcBitmap = BitmapAndroid.createBitmap(thermalImage.getImage()).getBitMap();

            Log.d(TAG, "adding images to cache");
            streamDataListener.images(msxBitmap, dcBitmap);
        }
    };

    /**
     * Camera Discovery thermalImageStreamListener, is notified if a new camera was found during a active discovery phase
     * <p>
     * Note that callbacks are received on a non-ui thread so have to eg use runonUI to interact view UI components
     */
    private DiscoveryEventListener cameraDiscoveryListener = new DiscoveryEventListener() {
        @Override
        public void onCameraFound(Identity identity) {
            Log.d(TAG, "onCameraFound identity:" + identity);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    add(identity);
                }
            });
        }

        @Override
        public void onDiscoveryError(CommunicationInterface communicationInterface, ErrorCode errorCode) {
            Log.d(TAG, "onDiscoveryError communicationInterface:" + communicationInterface + " errorCode:" + errorCode);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    stopDiscovery();
                    ThermalActivity.this.showMessage.show("onDiscoveryError communicationInterface:" + communicationInterface + " errorCode:" + errorCode);
                }
            });
        }
    };

    /**
     * Camera connecting state thermalImageStreamListener, keeps track of if the camera is connected or not
     * <p>
     * Note that callbacks are received on a non-ui thread so have to eg use {@link #runOnUiThread(Runnable)} to interact view UI components
     */
    private ConnectionStatusListener connectionStatusListener = new ConnectionStatusListener() {
        @Override
        public void onDisconnected(@org.jetbrains.annotations.Nullable ErrorCode errorCode) {
            Log.d(TAG, "onDisconnected errorCode:" + errorCode);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                }
            });
        }
    };

    /**
     * Add a found camera to the list of known cameras
     */
    public void add(Identity identity) { foundCameraIdentities.add(identity); }

    public static List<Face> getFaceList() {
        return faceList;
    }

    public void setFaceList(List<Face> faceList) {
        faceList = faceList;
    }
    public static void clearFaceList() { faceList.clear(); }
}

class FrameDataHolder {

    public final Bitmap msxBitmap;
    public final Bitmap dcBitmap;

    FrameDataHolder(Bitmap msxBitmap, Bitmap dcBitmap){
        this.msxBitmap = msxBitmap;
        this.dcBitmap = dcBitmap;
    }
}
