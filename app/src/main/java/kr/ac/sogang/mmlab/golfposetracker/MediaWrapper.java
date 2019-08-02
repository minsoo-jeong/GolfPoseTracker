package kr.ac.sogang.mmlab.golfposetracker;

import android.util.Log;

import org.opencv.core.CvType;
import org.opencv.core.Mat;

import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;

import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;


import static org.opencv.core.Core.bitwise_and;
import static org.opencv.core.Core.bitwise_or;
import static org.opencv.core.Core.split;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2RGB;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.drawContours;
import static org.opencv.imgproc.Imgproc.findContours;
import static org.opencv.imgproc.Imgproc.threshold;
import static org.opencv.core.Core.absdiff;
import static org.opencv.core.Core.bitwise_not;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_COUNT;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_HEIGHT;
import static org.opencv.videoio.Videoio.CAP_PROP_FRAME_WIDTH;
import static org.opencv.videoio.Videoio.CAP_PROP_FPS;
import static org.opencv.videoio.Videoio.CAP_PROP_FOURCC;



public class MediaWrapper {
    private VideoCapture cap;
    private String videoPath;
    private String modifiedVideoName;
    private String modifiedImageName;
    private Mat outputImage;

    private Mat motions;
    private Mat motions_mask;

    private int smpRate;
    private int refIntv;


    private VideoWriter swingVideoWriter = null;
    private String modifiedVideoPath;


    private List<Mat> frames;
    ;
    int scanFlag = 0;


    boolean VideoOpen(String selectedVideoPath) {
        videoPath = selectedVideoPath;
        cap = new VideoCapture(videoPath);

        Log.e("FrameCount", "" + String.valueOf(cap.get(CAP_PROP_FRAME_COUNT)));
        Log.e("w", "" + String.valueOf(cap.get(CAP_PROP_FRAME_WIDTH)));
        Log.e("h", "" + String.valueOf(cap.get(CAP_PROP_FRAME_HEIGHT)));
        Log.e("fps", "" + String.valueOf(cap.get(CAP_PROP_FPS)));
        Log.e("fourcc", "" + String.valueOf(cap.get(CAP_PROP_FOURCC)));


        if (cap.isOpened()) {
            return true;
        } else {
            return false;
        }
    }

    Mat GetImageFromVideo() {
        Mat frame = new Mat();
        cap.read(frame);
        if (!frame.empty()) {

            cvtColor(frame, frame, COLOR_BGR2RGB);
            return frame;
        } else {
            return null;
        }
    }

    void SetThresholds(double t1, double t2) {
        smpRate = (int) t1;
        refIntv = (int) t2;
    }

    double GetThreshold1() {
        return smpRate;
    }

    double Getref_intv() {
        return refIntv;

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
        cvtColor(image, saveImage, Imgproc.COLOR_BGR2RGB, 3);
        Imgcodecs.imwrite(GetModifiedImageName(), saveImage);
        return true;
    }

    boolean GenerateVideo(Mat frame) {
        try {
            Log.e("VideoWriter", "" + GetModifiedVideoName() + " / " + String.valueOf(frame.rows()) + " / " + String.valueOf(frame.cols()));
            swingVideoWriter = new VideoWriter(GetModifiedVideoName(), VideoWriter.fourcc('M', 'J', 'P', 'G'), 30.0, new Size(frame.cols(), frame.rows()), true);
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    boolean GetGeneratedVideo() {
        if (swingVideoWriter != null && swingVideoWriter.isOpened()) {
            return true;

        } else {
            return false;
        }
    }

    boolean InsertFrameInVideo(Mat frame) {
        try {
            Mat saveFrame = new Mat(frame.rows(), frame.cols(), CvType.CV_8U, new Scalar(3));
            cvtColor(frame, saveFrame, Imgproc.COLOR_BGR2RGB, 3);

            swingVideoWriter.write(saveFrame);
            saveFrame = null;
            return true;
        } catch (Exception e) {

            return false;
        }
    }

    Mat GenerateFrame(Mat srcImage) {
        return srcImage;
    }

    void swingVideoRelease() {
        swingVideoWriter.release();
        swingVideoWriter=null;
    }


    /********************************************/
    /********************************************/
    /********************************************/
    int GetLastFrameIdx() {
        return (int) cap.get(CAP_PROP_FRAME_COUNT) - 1;
    }

    boolean GenerateVideo() {
        try {
            Mat frame = frames.get(0);
            Log.e("VideoWriter", "" + GetModifiedVideoName() + " / " + String.valueOf(frame.rows()) + " / " + String.valueOf(frame.cols()));
            swingVideoWriter = new VideoWriter(GetModifiedVideoName(), VideoWriter.fourcc('M', 'J', 'P', 'G'), 30.0, new Size(frame.cols(), frame.rows()), true);
            //swingVideoWriter = new VideoWriter(GetModifiedVideoName(), VideoWriter.fourcc('M', 'P', '4', 'v'), 30.0, new Size(frame.cols(), frame.rows()), true);
            motions = new Mat(new Size(frame.cols(), frame.rows()), CvType.CV_8UC(3), Scalar.all(0));
            motions_mask = new Mat(new Size(frame.cols(), frame.rows()), CvType.CV_8U, Scalar.all(0));

            return true;
        } catch (Exception e) {
            return false;
        }

    }

    int ScanAllFrames() {
        int last_idx = GetLastFrameIdx();
        int scanFlag = 0;
        Mat srcFrame;
        frames = new ArrayList<>();
        while (true) {
            srcFrame = GetImageFromVideo();
            if (srcFrame != null) {
                frames.add(srcFrame);
                scanFlag++;
            } else {
                break;
            }

        }
        return scanFlag;
    }

    private Mat GrayBlur(Mat m) {
        Mat g = new Mat();
        cvtColor(m, g, COLOR_BGR2GRAY);
        GaussianBlur(g, g, new Size(5.0, 5.0), 0);
        return g;
    }

    private Mat GetDifference(Mat m1, Mat m2) {
        Mat dif = new Mat();
        absdiff(m1, m2, dif);
        threshold(dif, dif, 5, 255, THRESH_BINARY);
        return dif;
    }

    Mat GenerateFrame(int idx, boolean flag) {
        Mat curr = frames.get(idx);
        Mat m_frame = curr.clone();
        if (flag) {
            Log.e("REF", "ref " + (idx - refIntv) + "/" + idx + "/" + (idx + refIntv));
            Mat prev = frames.get(idx - refIntv);
            Mat next = frames.get(idx + refIntv);

            Mat p_gray = GrayBlur(prev);
            Mat c_gray = GrayBlur(curr);
            Mat n_gray = GrayBlur(next);

            Mat pc_thr = GetDifference(p_gray, c_gray);
            Mat pn_thr = GetDifference(p_gray, n_gray);
            Mat cn_thr = GetDifference(c_gray, n_gray);

            Mat pcn_thr = new Mat();
            bitwise_or(pc_thr, cn_thr, pcn_thr);
            bitwise_or(pcn_thr, pn_thr, pcn_thr);
            Mat c_stick = new Mat();
            Mat not_pnthr = new Mat();
            bitwise_not(pn_thr, not_pnthr);
            bitwise_and(pcn_thr, not_pnthr, c_stick);
            List<MatOfPoint> cntrs = new ArrayList<>();
            findContours(c_stick, cntrs, new Mat(), Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
            Mat action = new Mat(new Size(c_stick.cols(), c_stick.rows()), CvType.CV_8U, Scalar.all(0));
            for (int i = 0; i < cntrs.size(); i++) {
                double area = Imgproc.contourArea(cntrs.get(i));
                if (area > 100) {
                    drawContours(action, cntrs, i, new Scalar(255), -1);
                }
            }
            curr.copyTo(motions, action);
            bitwise_or(motions_mask, action, motions_mask);

            p_gray.release();
            c_gray.release();
            n_gray.release();
            pc_thr.release();
            pn_thr.release();
            cn_thr.release();
            pcn_thr.release();
            c_stick.release();
            not_pnthr.release();
            cntrs.clear();

            for (int i = idx - 2 * refIntv; i < idx - refIntv; i++) {

                Mat frame = frames.get(i);
                if (!frame.empty()) {
                    frame.release();
                    Log.e("Free", "free " + i);
                }
            }
        }
        motions.copyTo(m_frame, motions_mask);


        return m_frame;
    }

    boolean InsertFrameInVideo(int idx, boolean flag) {
        try {
            Mat frame = GenerateFrame(idx, flag);
            swingVideoWriter.write(frame);
            frame.release();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
