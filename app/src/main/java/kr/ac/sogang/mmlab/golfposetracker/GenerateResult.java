package kr.ac.sogang.mmlab.golfposetracker;

public class GenerateResult {
    private boolean status;
    private double time;
    private int frmCnt;

    boolean getStatus(){ return status; }
    double getTime(){ return time; }
    int getFrameCount(){ return frmCnt; }

    void setStatus(boolean s){ status=s; }
    void setTime(double t){ time=t; }
    void setFrameCount(int c){ frmCnt=c; }

    void setFail(){
        status=false;
        time=0.0;
        frmCnt=-1;
    }
    void setSuccess(double t,int c){
        status=true;
        time=t;
        frmCnt=c;
    }
    String textViewString(){
        return String.valueOf(frmCnt) + " / " + String.valueOf(time) + " sec";
    }
    String toastString(){
        return status ? "Create Video Success":"Create Video Fail";
    }
}
