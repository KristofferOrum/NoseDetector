package com.example.nayanmehta.nosedetection.others;



import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.Log;

import com.example.nayanmehta.nosedetection.others.GraphicOverlay;
import com.example.nayanmehta.nosedetection.R;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.Landmark;

/**
 * Graphic instance for rendering face position, orientation, and landmarks within an associated
 * graphic overlay view.
 */
public class FaceGraphic extends GraphicOverlay.Graphic {
    public Bitmap marker;

    private BitmapFactory.Options opt;
    private Resources resources;

    private int faceId;
    PointF facePosition;
    float faceWidth;
    float faceHeight;
    float isSmilingProbability = -1;
    float eyeRightOpenProbability = -1;
    float eyeLeftOpenProbability = -1;
    float eulerZ;
    float eulerY;

    /*public PointF p_faceCenter;
    public PointF p_leftEyePos;
    public PointF p_rightEyePos;
    public PointF p_noseBasePos;*/
    public PointF faceCenter = null;
    public PointF leftEyePos = null;
    public PointF rightEyePos = null;
    public PointF noseBasePos = null;
    public Bitmap myBitmap;
    public int canvasWidth=0;
    public int canvasHeight=0;

    PointF leftMouthCorner = null;
    PointF rightMouthCorner = null;
    PointF mouthBase = null;
    PointF leftEar = null;
    PointF rightEar = null;
    PointF leftEarTip = null;
    PointF rightEarTip = null;
    PointF leftCheek = null;
    PointF rightCheek = null;


    private volatile Face mFace;

    public FaceGraphic(GraphicOverlay overlay, Context context) {
        super(overlay);
        opt = new BitmapFactory.Options();
        opt.inScaled = false;
        resources = context.getResources();
        marker = BitmapFactory.decodeResource(resources, R.drawable.marker, opt);
    }

    public void setId(int id) {
        faceId = id;
    }

    public float getSmilingProbability() {
        return isSmilingProbability;
    }

    public float getEyeRightOpenProbability() {
        return eyeRightOpenProbability;
    }

    public float getEyeLeftOpenProbability() {
        return eyeLeftOpenProbability;
    }

    /**
     * Updates the face instance from the detection of the most recent frame.  Invalidates the
     * relevant portions of the overlay to trigger a redraw.
     */
    public void updateFace(Face face) {
        mFace = face;
        postInvalidate();
    }

    public void goneFace() {
        mFace = null;
    }

    @Override
    public void draw(Canvas canvas) {
        Face face = mFace;
        if(face == null) {
            canvas.drawColor(0, PorterDuff.Mode.CLEAR);
            isSmilingProbability = -1;
            eyeRightOpenProbability= -1;
            eyeLeftOpenProbability = -1;
            return;
        }

        facePosition = new PointF(translateX(face.getPosition().x), translateY(face.getPosition().y));
        faceWidth = face.getWidth() * 4;
        faceHeight = face.getHeight() * 4;
        faceCenter = new PointF(translateX(face.getPosition().x + faceWidth/8), translateY(face.getPosition().y + faceHeight/8));
       // p_faceCenter = new PointF(face.getPosition().x + faceWidth/8, face.getPosition().y + faceHeight/8);
        isSmilingProbability = face.getIsSmilingProbability();
        eyeRightOpenProbability = face.getIsRightEyeOpenProbability();
        eyeLeftOpenProbability = face.getIsLeftEyeOpenProbability();
        eulerY = face.getEulerY();
        eulerZ = face.getEulerZ();
        //DO NOT SET TO NULL THE NON EXISTENT LANDMARKS. USE OLDER ONES INSTEAD.
        for(Landmark landmark : face.getLandmarks()) {
            switch (landmark.getType()) {
                case Landmark.LEFT_EYE:
                    leftEyePos = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                   //p_leftEyePos = new PointF(landmark.getPosition().x, landmark.getPosition().y);
                    break;
                case Landmark.RIGHT_EYE:
                    rightEyePos = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                   //p_rightEyePos = new PointF(landmark.getPosition().x, landmark.getPosition().y);

                    break;
                case Landmark.NOSE_BASE:
                    noseBasePos = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    //p_noseBasePos = new PointF(landmark.getPosition().x, landmark.getPosition().y);

                    break;
                case Landmark.LEFT_MOUTH:
                    leftMouthCorner = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    break;
                case Landmark.RIGHT_MOUTH:
                    rightMouthCorner = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    break;
                case Landmark.BOTTOM_MOUTH:
                    mouthBase = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    break;
                case Landmark.LEFT_EAR:
                    leftEar = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    break;
                case Landmark.RIGHT_EAR:
                    rightEar = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    break;
                case Landmark.LEFT_EAR_TIP:
                    leftEarTip = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    break;
                case Landmark.RIGHT_EAR_TIP:
                    rightEarTip = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    break;
                case Landmark.LEFT_CHEEK:
                    leftCheek = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    break;
                case Landmark.RIGHT_CHEEK:
                    rightCheek = new PointF(translateX(landmark.getPosition().x), translateY(landmark.getPosition().y));
                    break;
            }
        }

       /* p_faceCenter = faceCenter;
        p_leftEyePos = leftEyePos;
        p_rightEyePos = rightEyePos;
        p_noseBasePos = noseBasePos;*/
       //Log.d("Here here"," "+canvas.getHeight()+"  "+canvas.getWidth());
        canvasWidth=canvas.getWidth();
        canvasHeight=canvas.getHeight();
//        myBitmap = Bitmap.createBitmap( (int)canvasWidth, (int)canvasHeight, Bitmap.Config.RGB_565 );
//        canvas.setBitmap(myBitmap);

        Paint mPaint = new Paint();
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(4);
        if((leftEyePos != null) && (rightEyePos != null) && (noseBasePos != null) && (faceCenter != null)){
            int noseWidth = Math.round(rightEyePos.x) - Math.round(leftEyePos.x);
            int noseHeight = Math.round(noseBasePos.y) - Math.round(faceCenter.y);

            canvas.drawRect((int)(faceCenter.x - noseWidth/4), (int)faceCenter.y, (int)(faceCenter.x + noseWidth/2), (int)(faceCenter.y + noseHeight), mPaint);
//        if(faceCenter != null)
//            canvas.drawBitmap(marker, faceCenter.x, faceCenter.y, null);
//        if(noseBasePos != null)
//            canvas.drawBitmap(marker, noseBasePos.x, noseBasePos.y, null);
//        if(leftEyePos != null)
//            canvas.drawBitmap(marker, leftEyePos.x, leftEyePos.y, null);
//        if(rightEyePos != null)
//            canvas.drawBitmap(marker, rightEyePos.x, rightEyePos.y, null);
//        if(mouthBase != null)
//            canvas.drawBitmap(marker, mouthBase.x, mouthBase.y, null);
//        if(leftMouthCorner != null)
//            canvas.drawBitmap(marker, leftMouthCorner.x, leftMouthCorner.y, null);
//        if(rightMouthCorner != null)
//            canvas.drawBitmap(marker, rightMouthCorner.x, rightMouthCorner.y, null);
//        if(leftEar != null)
//            canvas.drawBitmap(marker, leftEar.x, leftEar.y, null);
//        if(rightEar != null)
//            canvas.drawBitmap(marker, rightEar.x, rightEar.y, null);
//        if(leftEarTip != null)
//            canvas.drawBitmap(marker, leftEarTip.x, leftEarTip.y, null);
//        if(rightEarTip != null)
//            canvas.drawBitmap(marker, rightEarTip.x, rightEarTip.y, null);
//        if(leftCheek != null)
//            canvas.drawBitmap(marker, leftCheek.x, leftCheek.y, null);
//        if(rightCheek != null)
//            canvas.drawBitmap(marker, rightCheek.x, rightCheek.y, null);

        }
    }
}