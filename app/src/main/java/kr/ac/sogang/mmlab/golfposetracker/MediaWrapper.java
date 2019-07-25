package kr.ac.sogang.mmlab.golfposetracker;

import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

public class MediaWrapper {
    private VideoCapture cap;
    private String videoPath;
    private String modifiedVideoName;
    private String modifiedImageName;
    private Mat outputImage;
    private double threshold1, threshold2;

    private VideoWriter swingVideoWriter = null;
    private String modifiedVideoPath;

    boolean VideoOpen(String selectedVideoPath) {
        videoPath = selectedVideoPath;
        cap = new VideoCapture(videoPath);
        Log.e("FrameCount", "" + String.valueOf(cap.get(Videoio.CAP_PROP_FRAME_COUNT)));

        if (cap.isOpened()) {
            return true;
        }
        else {
            return false;
        }
    }

    Mat GetImageFromVideo() {
        Mat frame = new Mat();
        cap.read(frame);
        if (!frame.empty()) {
            return frame;
        }
        else {
            return null;
        }
    }

    void SetThresholds(double t1, double t2) {
        threshold1 = t1;
        threshold1 = t2;
    }

    double GetThreshold1() {
        return threshold1;
    }

    double GetThreshold2() {
        return threshold2;
    }

    void SetImage(Mat image) {
        outputImage = image;
    }

    Mat GetImage() {
        return outputImage;
    }

    void SetModifiedVideoName(String videoName) {
        modifiedVideoName = videoName;
    }

    String GetModifiedVideoName() {
        return modifiedVideoName;
    }

    void SetModifiedImageName(String imageName) {
        modifiedImageName = imageName;
    }

    String GetModifiedImageName() {
        return modifiedImageName;
    }

    boolean SaveImage(Mat image) {
        Mat saveImage = new Mat(image.rows(), image.cols(), CvType.CV_8U, new Scalar(3));
        Imgproc.cvtColor(image, saveImage, Imgproc.COLOR_BGR2RGB, 3);

        Imgcodecs.imwrite(GetModifiedImageName(), saveImage);

        return true;
    }

    boolean GenerateVideo(Mat frame) {
        try {
            Log.e("VideoWriter", "" + GetModifiedVideoName() + " / " + String.valueOf(frame.rows()) + " / " + String.valueOf(frame.cols()));
            swingVideoWriter = new VideoWriter(GetModifiedVideoName(), VideoWriter.fourcc('M', 'J', 'P', 'G'), 30.0, new Size(frame.cols(), frame.rows()), true);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    boolean GetGeneratedVideo() {
        if (swingVideoWriter != null) {
            return true;
        }
        else {
            return false;
        }
    }
    boolean InsertFrameInVideo(Mat frame) {
        try {
            Mat saveFrame = new Mat(frame.rows(), frame.cols(), CvType.CV_8U, new Scalar(3));
            Imgproc.cvtColor(frame, saveFrame, Imgproc.COLOR_BGR2RGB, 3);
            swingVideoWriter.write(saveFrame);
            saveFrame = null;

            return true;
        }
        catch(Exception e) {
            return false;
        }
    }

    Mat GenerateFrame(Mat srcImage) {
        return srcImage;
    }

    void swingVideoRelease() {
        swingVideoWriter.release();
    }
}
