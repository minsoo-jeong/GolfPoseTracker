package kr.ac.sogang.mmlab.golfposetracker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.opencv.android.Utils;

import org.opencv.core.Core;
import org.opencv.core.Mat;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2RGB;
import static org.opencv.imgproc.Imgproc.cvtColor;


public class MainActivity extends AppCompatActivity {

    private Button btnSelectVideo;
    private Button btnCreateVideo;
    private Button btnCreateImage;

    private EditText editTextVideoName;
    private EditText editTextImageName;
    private EditText editTextThreshold1;
    private EditText editTextThreshold2;

    private ImageView previewImg;

    private TextView textViewFrameCount;

    private int GALLERY = 1, CAMERA = 2;
    private MediaWrapper mediaWrapper = new MediaWrapper();
    private String selectedVideoPath;
    private String dirPath = "/storage/emulated/0/DCIM/Camera/";
    private String poseTracerPath = Environment.getExternalStorageDirectory() + "/DCIM/PoseTracer/";

    static final int PERMISSIONS_REQUEST_CODE = 1000;

    String[] PERMISSIONS = {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};

    static {
        System.loadLibrary("opencv_java4");
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e("CREATE", "Call onCreate");
        setContentView(R.layout.activity_main);

        btnSelectVideo = (Button) findViewById(R.id.btnSelectVideo);
        btnCreateVideo = (Button) findViewById(R.id.btnCreateVideo);
        btnCreateImage = (Button) findViewById(R.id.btnCreateImage);


        editTextVideoName = (EditText) findViewById(R.id.editTextVideoName);
        editTextImageName = (EditText) findViewById(R.id.editTextImageName);

        editTextThreshold1 = (EditText) findViewById(R.id.editTextThreshold1);
        editTextThreshold2 = (EditText) findViewById(R.id.editTextThreshold2);

        textViewFrameCount = (TextView) findViewById(R.id.textViewFrameCount);

        previewImg = (ImageView) findViewById(R.id.previewImg);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasPermissions(PERMISSIONS)) {
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }

        btnSelectVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseVideoFromGallary();
            }
        });

        btnCreateVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //boolean success = CreateSwingVideo();

                final Handler thread_handler = new Handler() {
                    @Override
                    public void handleMessage(Message msg) {
                        super.handleMessage(msg);
                        GenerateResult result = (GenerateResult) msg.obj;
                        textViewFrameCount.setText(result.textViewString());
                        Toast.makeText(getApplicationContext(), result.toastString(), Toast.LENGTH_LONG).show();
                    }
                };
                final Handler frameHandler = new Handler() {
                    public void handleMessage(Message msg) {
                        Bitmap bitmap = (Bitmap) msg.obj;
                        previewImg.setImageBitmap(bitmap);
                    }
                };
                File dir = new File(poseTracerPath);
                if (!(dir.exists() && dir.isDirectory())) {
                    dir.mkdir();
                }
                Thread GenerateThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        GenerateResult success = CreateSwingVideo3(frameHandler);
                        Message msg = thread_handler.obtainMessage();
                        msg.what = 0;
                        msg.obj = success;
                        thread_handler.sendMessage(msg);

                        /*
                        if(success)
                            Toast.makeText(getApplicationContext(), "Create Video Success", Toast.LENGTH_LONG).show();
                        else
                            Toast.makeText(getApplicationContext(), "Create Video Fail", Toast.LENGTH_LONG).show();
                        */

                    }
                });
                GenerateThread.start();
                getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE), mediaWrapper.GetModifiedVideoName());
                //previewImg.setImageResource(R.drawable.preview_default);
            }
        });
        btnCreateImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Coming Soon...!", Toast.LENGTH_LONG).show();
                /*
                boolean success = CreateSwingImage();
                if (success)
                    Toast.makeText(getApplicationContext(), "Create Image Success", Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(getApplicationContext(), "Create Image Fail", Toast.LENGTH_LONG).show();
                */
            }
        });

    }

    public void chooseVideoFromGallary() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.e("RESULT", "ACTIVITY_RESULT");
        Log.d("RESULT", "" + resultCode);

        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == this.RESULT_CANCELED) {
            previewImg.setImageResource(R.drawable.preview_default);
            return;
        }
        if (requestCode == GALLERY) {
            if (data != null) {
                Uri contentURI = data.getData();
                selectedVideoPath = getPath(contentURI);
                mediaWrapper.VideoOpen(selectedVideoPath);

                //previewImg.setImageBitmap(mediaWrapper.startFrame);

                String modifiedVideoName = GetModifiedVideoName2();
                String modifiedImageName = GetModifiedImageName2();

                editTextVideoName.setText(modifiedVideoName);
                editTextImageName.setText(modifiedImageName);

                Log.d("src video", selectedVideoPath);
                Log.d("modified video name", modifiedVideoName);
                Log.d("modified image name", modifiedImageName);
            }
        }
    }

    public String GetModifiedVideoName() {
        String[] videoNameTmp = selectedVideoPath.split(dirPath);
        String videoName = videoNameTmp[(int) videoNameTmp.length - 1];
        String regex = "";
        if (videoName.contains(".mp4")) {
            regex = ".mp4";
        } else if (videoName.contains(".avi")) {
            regex = ".avi";
        }
        return dirPath + videoName.split(regex)[0] + "_Modified" + ".avi";
    }

    public String GetModifiedImageName() {
        String[] imageNameTmp = selectedVideoPath.split(dirPath);

        String imageName = imageNameTmp[(int) imageNameTmp.length - 1];
        String regex = "";
        if (imageName.contains(".mp4")) {
            regex = ".mp4";
        } else if (imageName.contains(".avi")) {
            regex = ".avi";
        }
        String modifiedImageName = dirPath + imageName.split(regex)[0] + "_Modified" + ".png";

        return modifiedImageName;
    }


    public boolean CreateSwingVideo() {
        try {
            mediaWrapper.SetModifiedVideoName(editTextVideoName.getText().toString());
            double th1 = Double.parseDouble(editTextThreshold1.getText().toString());
            double th2 = Double.parseDouble(editTextThreshold2.getText().toString());

            mediaWrapper.SetThresholds(th1, th2);
            Mat srcFrame, modifiedFrame;
            int frameCount = 0;

            long start = System.currentTimeMillis();
            while (true) {
                srcFrame = mediaWrapper.GetImageFromVideo();

                if (!mediaWrapper.GetGeneratedVideo()) {
                    mediaWrapper.GenerateVideo(srcFrame);
                }

                if (srcFrame != null) {
                    modifiedFrame = mediaWrapper.GenerateFrame(srcFrame);

                    boolean videoSuccess = mediaWrapper.InsertFrameInVideo(modifiedFrame);
                    if (!videoSuccess) {
                        Log.e("Insert frame", "Fail to insert a frame into the video");
                    }
                } else {
                    break;
                }
                if (frameCount == 0) {
                    mediaWrapper.SetImage(srcFrame);
                }
                if (frameCount % 10 == 0) {

                    Log.d("=== Frame Number ===", "" + String.valueOf(frameCount));

                }
                frameCount++;
            }
            mediaWrapper.swingVideoRelease();
            long end = System.currentTimeMillis();

            double time = (end - start) / 1000.0;

            textViewFrameCount.setText(String.valueOf(frameCount) + " / " + String.valueOf(time) + " sec");

            Log.d("== Processing Time ==", String.valueOf(time) + "sec");

//            videoView.setVideoPath(mediaWrapper.GetModifiedVideoName());
//            videoView.requestFocus();
//            videoView.start();

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public boolean CreateSwingImage() {
        try {
            // Test - Save Image
            Mat frame = mediaWrapper.GetImage();
            mediaWrapper.SetModifiedImageName(editTextImageName.getText().toString());
//            Bitmap bitmap = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(frame, bitmap);
//            imageView.setImageBitmap(bitmap);
            if (!mediaWrapper.SaveImage(frame)) {
                Log.e("Save Image", "Failed to save a image");
            }
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public String getPath(Uri uri) {
        String[] projection = {MediaStore.Video.Media.DATA};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else
            return null;
    }

    private boolean hasPermissions(String[] permissions) {
        int result;

        for (String perms : permissions) {
            result = ContextCompat.checkSelfPermission(this, perms);
            if (result == PackageManager.PERMISSION_DENIED) {

                return false;
            }
        }
        return true;
    }

    /************************************************************************************/
    /************************************************************************************/
    /************************************************************************************/


    public GenerateResult CreateSwingVideo3(Handler frameHandler) {
        try {
            GenerateResult r = new GenerateResult();

            // check already exist
            String path=dirPath+editTextImageName.getText().toString();

            //mediaWrapper.SetModifiedVideoName(dirPath, editTextVideoName.getText().toString());
            mediaWrapper.SetModifiedVideoName(poseTracerPath, editTextVideoName.getText().toString());
            //sampling rate
            double th1 = Double.parseDouble(editTextThreshold1.getText().toString());
            //ref interval
            double th2 = Double.parseDouble(editTextThreshold2.getText().toString());
            mediaWrapper.SetThresholds(th1, th2);
            long start = System.currentTimeMillis();

            mediaWrapper.init_vid();
            int i = 0;

            while (mediaWrapper.GetFrame() || mediaWrapper.GetFrameBufSize() != 0) {
                Message msg = frameHandler.obtainMessage();
                Mat frame = mediaWrapper.generate_frame();
                //System.out.println(frame);
                cvtColor(frame, frame, COLOR_BGR2RGB);
                Bitmap bitmap = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.RGB_565);
                Utils.matToBitmap(frame, bitmap);
                //System.out.println(bitmap);
                msg.what = 0;
                msg.obj = bitmap;
                frameHandler.sendMessage(msg);
                frame.release();
                Log.e("Insert frame", "insert a frame into the video... idx : " + i);
                i++;
            }
            long end = System.currentTimeMillis();
            double time = (end - start) / 1000.0;
            //textViewFrameCount.setText(String.valueOf(mediaWrapper.GetframeCnt()) + " / " + String.valueOf(time) + " sec");
            mediaWrapper.release_vid();
            Log.d("== Processing Time ==", String.valueOf(time) + "sec");
            r.setSuccess(time, i);
            return r;
        } catch (Exception e) {
            e.printStackTrace();
            GenerateResult r = new GenerateResult();
            r.setFail();
            return r;
        }
    }

    public String GetModifiedVideoName2() {
        String[] videoNameTmp = selectedVideoPath.split(dirPath);
        String videoName = videoNameTmp[(int) videoNameTmp.length - 1];
        String regex = "";
        if (videoName.contains(".mp4")) {
            regex = ".mp4";
        } else if (videoName.contains(".avi")) {
            regex = ".avi";
        }
        return videoName.split(regex)[0] + "_Modified" + ".avi";
    }
    public String GetModifiedImageName2() {
        String[] imageNameTmp = selectedVideoPath.split(dirPath);
        String imageName = imageNameTmp[(int) imageNameTmp.length - 1];
        String regex = "";

        if (imageName.contains(".mp4")) {
            regex = ".mp4";
        } else if (imageName.contains(".avi")) {
            regex = ".avi";
        }
        String modifiedImageName = imageName.split(regex)[0] + "_Modified" + ".png";

        return modifiedImageName;
    }

}


//
//import android.annotation.TargetApi;
//import android.content.DialogInterface;
//import android.content.pm.PackageManager;
//import android.os.Build;
//import android.os.Bundle;
//import android.support.annotation.NonNull;
//import android.support.v4.content.ContextCompat;
//import android.support.v7.app.AlertDialog;
//import android.support.v7.app.AppCompatActivity;
//import android.util.Log;
//import android.view.SurfaceView;
//import android.view.WindowManager;
//import org.opencv.android.BaseLoaderCallback;
//import org.opencv.android.CameraBridgeViewBase;
//import org.opencv.android.LoaderCallbackInterface;
//import org.opencv.android.OpenCVLoader;
//import org.opencv.core.Mat;
//
//
//public class MainActivity extends AppCompatActivity
//        implements CameraBridgeViewBase.CvCameraViewListener2 {
//
//    private static final String TAG = "opencv";
//    private CameraBridgeViewBase mOpenCvCameraView;
//    private Mat matInput;
//    private Mat matResult;
//
//    public native void ConvertRGBtoGray(long matAddrInput, long matAddrResult);
//
//
//    static {
//        System.loadLibrary("opencv_java4");
//        System.loadLibrary("native-lib");
//    }
//
//
//
//    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
//        @Override
//        public void onManagerConnected(int status) {
//            switch (status) {
//                case LoaderCallbackInterface.SUCCESS:
//                {
//                    mOpenCvCameraView.enableView();
//                } break;
//                default:
//                {
//                    super.onManagerConnected(status);
//                } break;
//            }
//        }
//    };
//
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
//                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//        setContentView(R.layout.activity_main);
//
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            //퍼미션 상태 확인
//            if (!hasPermissions(PERMISSIONS)) {
//
//                //퍼미션 허가 안되어있다면 사용자에게 요청
//                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
//            }
//        }
//
//        mOpenCvCameraView = (CameraBridgeViewBase)findViewById(R.id.activity_surface_view);
//        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
//        mOpenCvCameraView.setCvCameraViewListener(this);
//        mOpenCvCameraView.setCameraIndex(0); // front-camera(1),  back-camera(0)
//        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
//    }
//
//    @Override
//    public void onPause()
//    {
//        super.onPause();
//        if (mOpenCvCameraView != null)
//            mOpenCvCameraView.disableView();
//    }
//
//    @Override
//    public void onResume()
//    {
//        super.onResume();
//
//        if (!OpenCVLoader.initDebug()) {
//            Log.d(TAG, "onResume :: Internal OpenCV library not found.");
//            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
//        } else {
//            Log.d(TAG, "onResum :: OpenCV library found inside package. Using it!");
//            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
//        }
//    }
//
//    public void onDestroy() {
//        super.onDestroy();
//
//        if (mOpenCvCameraView != null)
//            mOpenCvCameraView.disableView();
//    }
//
//    @Override
//    public void onCameraViewStarted(int width, int height) {
//
//    }
//
//    @Override
//    public void onCameraViewStopped() {
//
//    }
//
//    @Override
//    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
//
//        matInput = inputFrame.rgba();
//
//        //if ( matResult != null ) matResult.release(); fix 2018. 8. 18
//
//        if ( matResult == null )
//
//            matResult = new Mat(matInput.rows(), matInput.cols(), matInput.type());
//
//        ConvertRGBtoGray(matInput.getNativeObjAddr(), matResult.getNativeObjAddr());
//
//        return matResult;
//    }
//
//
//
//    //여기서부턴 퍼미션 관련 메소드
//    static final int PERMISSIONS_REQUEST_CODE = 1000;
//    String[] PERMISSIONS  = {"android.permission.CAMERA"};
//
//
//    private boolean hasPermissions(String[] permissions) {
//        int result;
//
//        //스트링 배열에 있는 퍼미션들의 허가 상태 여부 확인
//        for (String perms : permissions){
//
//            result = ContextCompat.checkSelfPermission(this, perms);
//
//            if (result == PackageManager.PERMISSION_DENIED){
//                //허가 안된 퍼미션 발견
//                return false;
//            }
//        }
//
//        //모든 퍼미션이 허가되었음
//        return true;
//    }
//
//
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//
//        switch(requestCode){
//
//            case PERMISSIONS_REQUEST_CODE:
//                if (grantResults.length > 0) {
//                    boolean cameraPermissionAccepted = grantResults[0]
//                            == PackageManager.PERMISSION_GRANTED;
//
//                    if (!cameraPermissionAccepted)
//                        showDialogForPermission("앱을 실행하려면 퍼미션을 허가하셔야합니다.");
//                }
//                break;
//        }
//    }
//
//
//    @TargetApi(Build.VERSION_CODES.M)
//    private void showDialogForPermission(String msg) {
//
//        AlertDialog.Builder builder = new AlertDialog.Builder( MainActivity.this);
//        builder.setTitle("알림");
//        builder.setMessage(msg);
//        builder.setCancelable(false);
//        builder.setPositiveButton("예", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id){
//                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
//            }
//        });
//        builder.setNegativeButton("아니오", new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface arg0, int arg1) {
//                finish();
//            }
//        });
//        builder.create().show();
//    }
//
//
//}
