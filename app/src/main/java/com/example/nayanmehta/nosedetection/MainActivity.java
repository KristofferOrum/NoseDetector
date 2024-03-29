package com.example.nayanmehta.nosedetection;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import com.example.nayanmehta.nosedetection.others.Camera2Source;
import com.example.nayanmehta.nosedetection.others.FaceGraphic;
import com.example.nayanmehta.nosedetection.utils.Utils;
import com.example.nayanmehta.nosedetection.others.CameraSource;
import com.example.nayanmehta.nosedetection.others.CameraSourcePreview;
import com.example.nayanmehta.nosedetection.others.GraphicOverlay;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.Tracker;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;



public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Nayan Mehta Camera";
    private Context context;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private static final int REQUEST_STORAGE_PERMISSION = 201;
    private TextView cameraVersion;
    private ImageView ivAutoFocus;
    public static ImageView ivNoseCrop;
    private int lastsavedID;
    private int currentFaceID;


    public static Bitmap noseFlip;
    public static ArrayList<Bitmap> bitmapArray;
    public static ArrayList<Bitmap> pictureArray;
    public FaceDetector detector;
    public static boolean idleFlag;


    /*
    * Use the bBoxScaleFactor to change the size of the cropped ROI
    *
    * */
    private double bBoxScaleFactor= 1.0;

    private ScaleGestureDetector mScaleGestureDetector;
    private float mScaleFactor = 1.0f;

    // Camera API source
    private CameraSource mCameraSource = null;

    // Camera2 API source
    private Camera2Source mCamera2Source = null;

    // Common attributes for both APIs
    private CameraSourcePreview mPreview;
    public FaceDetector previewFaceDetector = null;
    private GraphicOverlay mGraphicOverlay;
    private FaceGraphic mFaceGraphic;
    private boolean wasActivityResumed = false;

    public static Button takePictureButton;
    private String cameraFile;
    public static ArrayList<Bitmap> BitmapList = new ArrayList<Bitmap>();
    private int person_store_count = 1;
    private SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

    // Set Front Camera as the default
    private boolean usingFrontCamera = true;

    // Flag used to access Camera2 API
    // Any attempts to run on API < 21 will result in the application crashing
    public boolean useCamera2 = true;

    private View getDecorView() {
        return getWindow().getDecorView();
    }

    protected void enableFullScreen(boolean enabled) {
        int newVisibility =  View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;

        if(enabled) {
            newVisibility |= View.SYSTEM_UI_FLAG_VISIBLE
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }

        getDecorView().setSystemUiVisibility(newVisibility);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableFullScreen(true);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();

        takePictureButton = (Button) findViewById(R.id.btn_takepicture);
        mPreview = (CameraSourcePreview) findViewById(R.id.preview);
        mGraphicOverlay = (GraphicOverlay) findViewById(R.id.faceOverlay);
        cameraVersion = (TextView) findViewById(R.id.cameraVersion);
        ivAutoFocus = (ImageView) findViewById(R.id.ivAutoFocus);
        ivNoseCrop= (ImageView) findViewById(R.id.ivNoseCrop);
        mScaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
        noseFlip=null;
        bitmapArray = new ArrayList<Bitmap>();
        pictureArray= new ArrayList<Bitmap>();
        idleFlag=false;

        detector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .build();


        if(checkGooglePlayAvailability()) {
            requestPermissionThenOpenCamera();


            takePictureButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    takePictureButton.setEnabled(false);
                    if(useCamera2) {
                        if(mCamera2Source != null)mCamera2Source.takePicture(camera2SourceShutterCallback, camera2SourcePictureCallback);
                    } else {
                        if(mCameraSource != null)mCameraSource.takePicture(cameraSourceShutterCallback, cameraSourcePictureCallback);
                    }
                }
            });


            mPreview.setOnTouchListener(CameraPreviewTouchListener);
        }
        updateUI();
    }


    public void updateUI() {
        if(BitmapList.size()>0){
            Bitmap lastbitmap = BitmapList.get(0);
            ivNoseCrop.setImageBitmap(lastbitmap);
        }
        final View decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener (new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
            }
        });
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        mScaleGestureDetector.onTouchEvent(motionEvent);
        return true;
    }
    /*
    Changes scale of cropped ROI on pinch for zoom in/out
     */
    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector){
            mScaleFactor *= scaleGestureDetector.getScaleFactor();
            mScaleFactor = Math.max(0.1f,
                    Math.min(mScaleFactor, 10.0f));
            ivNoseCrop.setScaleX(mScaleFactor);
            ivNoseCrop.setScaleY(mScaleFactor);
            return true;
        }
    }
    final CameraSource.ShutterCallback cameraSourceShutterCallback = new CameraSource.ShutterCallback() {@Override public void onShutter() {Log.d(TAG, "Shutter Callback!");}};
    final CameraSource.PictureCallback cameraSourcePictureCallback = new CameraSource.PictureCallback() {
    Bitmap noseBit=null;
    Bitmap noseCrop=null;
    Bitmap noseFinal=null;
    int noseWidth, noseHeight;
    int noseX,noseY;
    Matrix m;
    Matrix matrix;
    Canvas tempCanvas;


        @Override
        public void onPictureTaken(Bitmap picture) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    takePictureButton.setEnabled(true);
                }
            });

            if ( picture.getHeight() < picture.getWidth()) {
                Matrix rotate = new Matrix();
                rotate.postRotate(-90);
                picture = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), rotate, true);
            }


                    noseCrop = picture;
                    try {

                        Log.d(TAG,"Bitmap Info:"+noseCrop.getHeight()+" "+noseCrop.getWidth());
                        Log.d(TAG,"Preview Info:"+mGraphicOverlay.mPreviewHeight+" "+mGraphicOverlay.mPreviewWidth);
                        Log.d(TAG,"Point values"+mFaceGraphic.p_leftEyePos+"  "+mFaceGraphic.p_rightEyePos+"  "+mFaceGraphic.p_faceCenter+"  "+mFaceGraphic.p_noseBasePos);

                        if((mFaceGraphic.p_leftEyePos !=null)&&(mFaceGraphic.p_rightEyePos !=null)&&(mFaceGraphic.p_faceCenter !=null))
                        {
                            int height_factor = mGraphicOverlay.mPreviewHeight;
                            int width_factor = mGraphicOverlay.mPreviewWidth;
                            if (mGraphicOverlay.mFacing == CameraSource.CAMERA_FACING_FRONT) {
                                noseWidth= Math.round(mFaceGraphic.p_leftEyePos.x) - Math.round(mFaceGraphic.p_rightEyePos.x);

                            }
                            else {
                                noseWidth = Math.round(mFaceGraphic.p_rightEyePos.x) - Math.round(mFaceGraphic.p_leftEyePos.x);

                            }
                            noseHeight= Math.round(mFaceGraphic.p_noseBasePos.y)-Math.round(mFaceGraphic.p_faceCenter.y);


                            int x = (int)((mFaceGraphic.p_faceCenter.x - noseWidth * 0.25) * (float)noseCrop.getWidth()/width_factor);
                            int y = (int)((mFaceGraphic.p_faceCenter.y) * (float)noseCrop.getHeight()/height_factor);
                            int w = (int)(((noseWidth) * (float)noseCrop.getWidth()/width_factor)*bBoxScaleFactor);
                            int h = (int)(((noseHeight) * (float)noseCrop.getHeight()/height_factor)*bBoxScaleFactor);
                            Log.d(TAG,"Draw values"+x+"  "+y+"  "+w/4 + "  "+h);
                            noseBit= Bitmap.createBitmap(noseCrop, x, y,w/2,h);
                        }
                        m = new Matrix();
                        m.preScale(-1, 1);
                        noseFlip = Bitmap.createBitmap(noseBit, 0, 0, noseBit.getWidth(), noseBit.getHeight(), m, false);
                        noseFlip.setDensity(DisplayMetrics.DENSITY_DEFAULT);
                        Log.d(TAG,"  "+" "+noseFlip.getWidth()+" "+noseFlip.getHeight());

                        if(pictureArray.size() < 3){
                            pictureArray.add(picture);
                            bitmapArray.add(noseFlip);
                        }else{

                            pictureArray.set(0, pictureArray.get(1));
                            pictureArray.set(1, pictureArray.get(2));
                            pictureArray.set(2, picture);
                            bitmapArray.set(0, bitmapArray.get(1));
                            bitmapArray.set(1, bitmapArray.get(2));
                            bitmapArray.set(2, noseFlip);
//                         for(int picIndex=0;picIndex<pictureArray.size();picIndex++){
//                             pictureArray.set(picIndex, picture);
//                             bitmapArray.set(picIndex, noseFlip);
//                         }

                        }
                        Log.d("Picture and Nose"," "+pictureArray.size()+" "+bitmapArray.size());


                        //Log.d(TAG," "+bitmapArray.size());
                        if( noseFlip!=null) {

                            if (BitmapList.size() < person_store_count) {
                                BitmapList.add(noseFlip);
                            }
                            Canvas tempCanvas = new Canvas();
                            tempCanvas.drawBitmap(noseFlip, 0, 0, null);
                            if(!idleFlag){
                                ivNoseCrop.setImageDrawable(new BitmapDrawable(getResources(), noseFlip));
                                ivNoseCrop.setScaleType(ImageView.ScaleType.FIT_XY);
                            }

                        }


                    } catch (Exception e) {
                        e.printStackTrace();
                    }

        }
    };


    final Camera2Source.ShutterCallback camera2SourceShutterCallback = new Camera2Source.ShutterCallback() {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onShutter() {Log.d(TAG, "Shutter Callback for CAMERA2");}
    };

    final Camera2Source.PictureCallback camera2SourcePictureCallback = new Camera2Source.PictureCallback() {

        Bitmap noseBit=null;

        Bitmap noseCrop=null;
        Bitmap noseFinal=null;
        int noseWidth, noseHeight;
        int noseX,noseY;
        Matrix m;
        Matrix matrix;
        Canvas tempCanvas;

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onPictureTaken(Image image) {
            Log.d(TAG, "Taken picture is here!");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    takePictureButton.setEnabled(true);
                }
            });
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            Bitmap picture = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);
            image.close();

            if ( picture.getHeight() < picture.getWidth()) {
                Matrix rotate = new Matrix();
                rotate.postRotate(-90);
                picture = Bitmap.createBitmap(picture, 0, 0, picture.getWidth(), picture.getHeight(), rotate, true);
            }


            noseCrop = picture;
            try {

                Log.d(TAG,"Bitmap Info:"+noseCrop.getHeight()+" "+noseCrop.getWidth());
                Log.d(TAG,"Preview Info:"+mGraphicOverlay.mPreviewHeight+" "+mGraphicOverlay.mPreviewWidth);
                Log.d(TAG,"Point values"+mFaceGraphic.p_leftEyePos+"  "+mFaceGraphic.p_rightEyePos+"  "+mFaceGraphic.p_faceCenter+"  "+mFaceGraphic.p_noseBasePos);

                if((mFaceGraphic.p_leftEyePos !=null)&&(mFaceGraphic.p_rightEyePos !=null)&&(mFaceGraphic.p_faceCenter !=null))
                {
                    int height_factor = mGraphicOverlay.mPreviewHeight;
                    int width_factor = mGraphicOverlay.mPreviewWidth;
                    if (mGraphicOverlay.mFacing == CameraSource.CAMERA_FACING_FRONT) {
                        noseWidth= Math.round(mFaceGraphic.p_leftEyePos.x) - Math.round(mFaceGraphic.p_rightEyePos.x);

                    }
                    else {
                        noseWidth = Math.round(mFaceGraphic.p_rightEyePos.x) - Math.round(mFaceGraphic.p_leftEyePos.x);

                    }
                    noseHeight= Math.round(mFaceGraphic.p_noseBasePos.y)-Math.round(mFaceGraphic.p_faceCenter.y);


                    int x = (int)((mFaceGraphic.p_faceCenter.x - noseWidth * 0.25) * (float)noseCrop.getWidth()/width_factor);
                    int y = (int)((mFaceGraphic.p_faceCenter.y) * (float)noseCrop.getHeight()/height_factor);
                    int w = (int)(((noseWidth) * (float)noseCrop.getWidth()/width_factor)*bBoxScaleFactor);
                    int h = (int)(((noseHeight) * (float)noseCrop.getHeight()/height_factor)*bBoxScaleFactor);
                    Log.d(TAG,"Draw values"+x+"  "+y+"  "+w/4 + "  "+h);
                    noseBit= Bitmap.createBitmap(noseCrop, x, y,w/2,h);
                }
                m = new Matrix();
                m.preScale(-1, 1);
                noseFlip = Bitmap.createBitmap(noseBit, 0, 0, noseBit.getWidth(), noseBit.getHeight(), m, false);
                noseFlip.setDensity(DisplayMetrics.DENSITY_DEFAULT);
                Log.d(TAG,"  "+" "+noseFlip.getWidth()+" "+noseFlip.getHeight());

                if(pictureArray.size() < 3){
                    pictureArray.add(picture);
                    bitmapArray.add(noseFlip);
                }else{

                    pictureArray.set(0, pictureArray.get(1));
                    pictureArray.set(1, pictureArray.get(2));
                    pictureArray.set(2, picture);
                    bitmapArray.set(0, bitmapArray.get(1));
                    bitmapArray.set(1, bitmapArray.get(2));
                    bitmapArray.set(2, noseFlip);
//                         for(int picIndex=0;picIndex<pictureArray.size();picIndex++){
//                             pictureArray.set(picIndex, picture);
//                             bitmapArray.set(picIndex, noseFlip);
//                         }

                }
                Log.d("Picture and Nose"," "+pictureArray.size()+" "+bitmapArray.size());


                //Log.d(TAG," "+bitmapArray.size());
                if( noseFlip!=null) {

                    if (BitmapList.size() < person_store_count) {
                        BitmapList.add(noseFlip);
                    }
                    Canvas tempCanvas = new Canvas();
                    tempCanvas.drawBitmap(noseFlip, 0, 0, null);
                    if(!idleFlag){
                        ivNoseCrop.setImageDrawable(new BitmapDrawable(getResources(), noseFlip));
                        ivNoseCrop.setScaleType(ImageView.ScaleType.FIT_XY);
                    }

                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private boolean checkGooglePlayAvailability() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(context);
        if(resultCode == ConnectionResult.SUCCESS) {
            return true;
        } else {
            if(googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(MainActivity.this, resultCode, 2404).show();
            }
        }
        return false;
    }

    private void requestPermissionThenOpenCamera() {
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
//                useCamera2 = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP);
                Log.d(TAG,"bUILD INFO"+useCamera2);
                createCameraSourceFront();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        }
    }

    private void createCameraSourceFront() {
        previewFaceDetector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.ACCURATE_MODE)
                .setProminentFaceOnly(true)
                .setTrackingEnabled(true)
                .build();

        if(previewFaceDetector.isOperational()) {
            previewFaceDetector.setProcessor(new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory()).build());
        } else {
            Toast.makeText(context, "FACE DETECTION NOT AVAILABLE", Toast.LENGTH_SHORT).show();
        }
        Log.d(TAG,"Value before"+useCamera2);
        if(useCamera2) {
            mCamera2Source = new Camera2Source.Builder(context, previewFaceDetector)
                    .setFocusMode(Camera2Source.CAMERA_AF_AUTO)
                    .setFlashMode(Camera2Source.CAMERA_FLASH_AUTO)
                    .setFacing(Camera2Source.CAMERA_FACING_FRONT)
                    .build();

            //IF CAMERA2 HARDWARE LEVEL IS LEGACY, CAMERA2 IS NOT NATIVE.
            //WE WILL USE CAMERA1.
            if(mCamera2Source.isCamera2Native()) {
                startCameraSource();
            } else {
                useCamera2 = false;
                Log.d(TAG,"Value after not native"+useCamera2);
                if(usingFrontCamera) createCameraSourceFront(); else createCameraSourceBack();
            }
        } else {
            mCameraSource = new CameraSource.Builder(context, previewFaceDetector)
                    .setFacing(CameraSource.CAMERA_FACING_FRONT)
                    .setRequestedFps(30.0f)
                    .build();

            startCameraSource();
        }
    }

    private void createCameraSourceBack() {
        previewFaceDetector = new FaceDetector.Builder(context)
                .setClassificationType(FaceDetector.ALL_CLASSIFICATIONS)
                .setLandmarkType(FaceDetector.ALL_LANDMARKS)
                .setMode(FaceDetector.FAST_MODE)
                .setProminentFaceOnly(true)
                .setTrackingEnabled(true)
                .build();

        if(previewFaceDetector.isOperational()) {
            previewFaceDetector.setProcessor(new MultiProcessor.Builder<>(new GraphicFaceTrackerFactory()).build());
        } else {
            Toast.makeText(context, "FACE DETECTION NOT AVAILABLE", Toast.LENGTH_SHORT).show();
        }

        if(useCamera2) {
            mCamera2Source = new Camera2Source.Builder(context, previewFaceDetector)
                    .setFocusMode(Camera2Source.CAMERA_AF_AUTO)
                    .setFlashMode(Camera2Source.CAMERA_FLASH_AUTO)
                    .setFacing(Camera2Source.CAMERA_FACING_BACK)
                    .build();

            //IF CAMERA2 HARDWARE LEVEL IS LEGACY, CAMERA2 IS NOT NATIVE.
            //WE WILL USE CAMERA1.
            if(mCamera2Source.isCamera2Native()) {
                startCameraSource();
            } else {
                useCamera2 = false;
                if(usingFrontCamera) createCameraSourceFront(); else createCameraSourceBack();
            }
        } else {
            mCameraSource = new CameraSource.Builder(context, previewFaceDetector)
                    .setFacing(CameraSource.CAMERA_FACING_BACK)
                    .setRequestedFps(30.0f)
                    .build();

            startCameraSource();
        }
    }

    private void startCameraSource() {
        if(useCamera2) {
            if(mCamera2Source != null) {
                cameraVersion.setText("Camera 2");
                try {mPreview.start(mCamera2Source, mGraphicOverlay);
                } catch (IOException e) {
                    Log.e(TAG, "Unable to start camera source 2.", e);
                    mCamera2Source.release();
                    mCamera2Source = null;
                }
            }
        } else {
            if (mCameraSource != null) {
                cameraVersion.setText("Camera 1");
                try {mPreview.start(mCameraSource, mGraphicOverlay);
                } catch (IOException e) {
                    Log.e(TAG, "Unable to start camera source.", e);
                    mCameraSource.release();
                    mCameraSource = null;
                }
            }
        }
    }

    private void stopCameraSource() {
        mPreview.stop();
    }

    private class GraphicFaceTrackerFactory implements MultiProcessor.Factory<Face> {
        @Override
        public Tracker<Face> create(Face face) {
            return new GraphicFaceTracker(mGraphicOverlay);
        }
    }

    private class GraphicFaceTracker extends Tracker<Face> {
        private GraphicOverlay mOverlay;

        GraphicFaceTracker(GraphicOverlay overlay) {
            mOverlay = overlay;
            mFaceGraphic = new FaceGraphic(overlay, context);

        }

        /**
         * Start tracking the detected face instance within the face overlay.
         */
        @Override
        public void onNewItem(int faceId, Face item) {
            System.out.println("FaceID: "+ faceId);
            FileOutputStream out = null;
            File f = new File(android.os.Environment.getExternalStorageDirectory(),File.separator+"NoseDetection/");
            f.mkdirs();
            Log.d("SAVE", "Saving the images");

            if (BitmapList.size() > 0 ){

//                for (int i=0; i < BitmapList.size(); i++){
                    cameraFile= "/" + formatter.format(new Date()) + "_"+ (faceId - 1) + "_" + 0 +".png";
                    lastsavedID = faceId - 1;
                    currentFaceID = faceId;
                    try {
                        Log.d("Image_Saved:", cameraFile);
                        out = new FileOutputStream(new File(f, cameraFile));
                        Bitmap save_image = BitmapList.get(0);
                        save_image.compress(Bitmap.CompressFormat.JPEG, 95, out);

                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }finally {
                        try {
                            if (out != null) {
                                out.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

//                }
                BitmapList.clear();
            }
            mFaceGraphic.setId(faceId);
            Log.d("OnNewItem"," "+bitmapArray+" "+pictureArray);
            idleFlag=false;

        }

        /**
         * Update the position/characteristics of the face within the overlay.
         */
        @Override
        public void onUpdate(FaceDetector.Detections<Face> detectionResults, Face face) {
            mOverlay.add(mFaceGraphic);
            mFaceGraphic.updateFace(face);
            idleFlag=false;
        }

        /**
         * Hide the graphic when the corresponding face was not detected.  This can happen for
         * intermediate frames temporarily (e.g., if the face was momentarily blocked from
         * view).
         */
        @Override
        public void onMissing(FaceDetector.Detections<Face> detectionResults) {
            mFaceGraphic.goneFace();
            mOverlay.remove(mFaceGraphic);
                    idleFlag=true;
                    mFaceGraphic.idleState();
        }

        /**
         * Called when the face is assumed to be gone for good. Remove the graphic annotation from
         * the overlay.
         */
        @Override
        public void onDone() {
            mFaceGraphic.goneFace();
            mOverlay.remove(mFaceGraphic);
            idleFlag=true;
        }
    }

    private final CameraSourcePreview.OnTouchListener CameraPreviewTouchListener = new CameraSourcePreview.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent pEvent) {
            v.onTouchEvent(pEvent);
            if (pEvent.getAction() == MotionEvent.ACTION_DOWN) {
                int autoFocusX = (int) (pEvent.getX() - Utils.dpToPx(60)/2);
                int autoFocusY = (int) (pEvent.getY() - Utils.dpToPx(60)/2);
                ivAutoFocus.setTranslationX(autoFocusX);
                ivAutoFocus.setTranslationY(autoFocusY);
                ivAutoFocus.setVisibility(View.VISIBLE);
                ivAutoFocus.bringToFront();
                if(useCamera2) {
                    if(mCamera2Source != null) {
                        mCamera2Source.autoFocus(new Camera2Source.AutoFocusCallback() {
                            @Override
                            public void onAutoFocus(boolean success) {
                                runOnUiThread(new Runnable() {
                                    @Override public void run() {ivAutoFocus.setVisibility(View.GONE);}
                                });
                            }
                        }, pEvent, v.getWidth(), v.getHeight());
                    } else {
                        ivAutoFocus.setVisibility(View.GONE);
                    }
                } else {
                    if(mCameraSource != null) {
                        mCameraSource.autoFocus(new CameraSource.AutoFocusCallback() {
                            @Override
                            public void onAutoFocus(boolean success) {
                                runOnUiThread(new Runnable() {
                                    @Override public void run() {ivAutoFocus.setVisibility(View.GONE);}
                                });
                            }
                        });
                    } else {
                        ivAutoFocus.setVisibility(View.GONE);
                    }
                }
            }
            return false;
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestPermissionThenOpenCamera();
            } else {
                Toast.makeText(MainActivity.this, "CAMERA PERMISSION REQUIRED", Toast.LENGTH_LONG).show();
                finish();
            }
        }
        if(requestCode == REQUEST_STORAGE_PERMISSION) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestPermissionThenOpenCamera();
            } else {
                Toast.makeText(MainActivity.this, "STORAGE PERMISSION REQUIRED", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(wasActivityResumed)
            //If the CAMERA2 is paused then resumed, it won't start again unless creating the whole camera again.
            if(useCamera2) {
                if(usingFrontCamera) {
                    createCameraSourceFront();
                } else {
                    createCameraSourceBack();
                }
            } else {
                startCameraSource();
            }
    }

    @Override
    protected void onPause() {
        super.onPause();
        wasActivityResumed = true;
        stopCameraSource();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopCameraSource();
        if(previewFaceDetector != null) {
            previewFaceDetector.release();
        }
        System.out.println("Current Faceid:" + currentFaceID + ", Last: " + lastsavedID);
        if (currentFaceID >= lastsavedID){
            FileOutputStream out = null;
            File f = new File(android.os.Environment.getExternalStorageDirectory(),File.separator+"NoseDetection/");
            f.mkdirs();
            Log.d("SAVE", "Saving the images");
            if (BitmapList.size() > 0 ){

                for (int i=0; i < BitmapList.size(); i++){
                    cameraFile= "/" + formatter.format(new Date()) + "_"+ (currentFaceID) + "_" + i +".png";

                    try {
                        Log.d("Image_Saved:", cameraFile);
                        out = new FileOutputStream(new File(f, cameraFile));
                        Bitmap save_image = BitmapList.get(i);
                        save_image.compress(Bitmap.CompressFormat.JPEG, 95, out);

                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                BitmapList.clear();
            }
        }
    }
    public void onStart() {
        super.onStart();
        handler.removeCallbacks(runnableCode);
        handler.postDelayed(runnableCode,10000*6*15);
    }
    public void recreate(){
        if (android.os.Build.VERSION.SDK_INT >= 11)
        {
            super.recreate();
        }
        else
        {
            startActivity(getIntent());
            finish();
        }
    }
    // Create the Handler object (on the main thread by default)
    Handler handler = new Handler();
    // Define the code block to be executed
    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            // Do something here on the main thread
            Log.d("Handlers", "Called on main thread");
            Toast.makeText(MainActivity.this, "Refreshing application...", Toast.LENGTH_LONG).show();
            recreate();
        }
    };


}
