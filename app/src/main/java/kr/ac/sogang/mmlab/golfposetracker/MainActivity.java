package kr.ac.sogang.mmlab.golfposetracker;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.VideoView;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;

public class MainActivity extends AppCompatActivity {

    private Button btn;
    private VideoView videoView;
    private ImageView imageView;
    private int GALLERY = 1, CAMERA = 2;

    static final int PERMISSIONS_REQUEST_CODE = 1000;
    String[] PERMISSIONS  = {"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"};

    static {
        System.loadLibrary("opencv_java4");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btn = (Button) findViewById(R.id.btn);
        videoView = (VideoView)findViewById(R.id.videoView);
        imageView = (ImageView)findViewById(R.id.imageView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!hasPermissions(PERMISSIONS)) {
                requestPermissions(PERMISSIONS, PERMISSIONS_REQUEST_CODE);
            }
        }

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseVideoFromGallary();
            }
        });

    }

    public void chooseVideoFromGallary() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.d("result",""+resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == this.RESULT_CANCELED) {
            Log.d("what","cancel");
            return;
        }
        if (requestCode == GALLERY) {
            if (data != null) {
                Uri contentURI = data.getData();
                String selectedVideoPath = getPath(contentURI);

                Log.d("path",selectedVideoPath);

                VideoCapture cap = new VideoCapture(selectedVideoPath);
                if (cap.isOpened()) {
                    Log.d("Video", "open success");

                    Mat frame = new Mat();
                    cap.read(frame);
                    Bitmap bitmap = Bitmap.createBitmap(frame.cols(), frame.rows(), Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(frame, bitmap);
                    imageView.setImageBitmap(bitmap);
                }
                else {
                    Log.d("Video", "open fail");
                }

//                videoView.setVideoURI(contentURI);
//                videoView.requestFocus();
//                videoView.start();
            }
        }
    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Video.Media.DATA };
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
        for (String perms : permissions){
            result = ContextCompat.checkSelfPermission(this, perms);
            if (result == PackageManager.PERMISSION_DENIED){
                return false;
            }
        }
        return true;
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
