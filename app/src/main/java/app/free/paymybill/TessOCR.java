package app.free.paymybill;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by cvora on 3/21/2016.
 */
public class TessOCR {

    public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/SimpleAndroidOCR/";
    public static final String lang = "eng";
    public static final String TAG = "TessOCR.java";

    private TessBaseAPI mTess = new TessBaseAPI();
    private Context context;

    public TessOCR(Context current){

        this.context = current;
        saveTrainedData();
        mTess.setDebug(true);
        mTess.init(DATA_PATH , lang);
    }

    private void saveTrainedData(){
    /*
        Creating the directory on sd card to store the tesseract traning data for english language
     */
        String[] paths = new String[] { DATA_PATH};

        for(String path:paths){
            File dir = new File(path);
            if(!dir.exists()){
                if(!dir.mkdirs()){
                    Log.v(TAG,"Error: Creation of directory "+dir.getPath()+" on sd card failed");
                }else{
                    Log.v(TAG,"Created directory "+ dir.getPath()+" on sd card");
                }
            }
        }

        // lang.traineeddata file with the app (in assets folder)
        // available at: https://github.com/tesseract-ocr/tessdata
        if(!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata").exists())){
            try{
                AssetManager assetManager = context.getAssets();
                InputStream in = assetManager.open("tessdata/" + lang + ".traineddata");
                OutputStream out = new FileOutputStream(DATA_PATH+lang+".traineddata");
                // Transfer bytes from in to out
                byte[]buf = new byte[1024];
                int len;
                while((len = in.read(buf))>0){
                    out.write(buf,0,len);
                }
                in.close();
                out.close();
                Log.v(TAG,"Copied "+lang+" traineddata");
            }catch(IOException e){
                Log.e(TAG,"was unable to copy " + lang+ " .traineddata "+ e.toString());
                e.printStackTrace();
            }
        }
    }

    public String getOCRResultUTF8Format(Bitmap bitmap){
        mTess.setImage(bitmap);
        String result = mTess.getUTF8Text();
        Log.v(TAG,"OCR RESULT: "+result);
        return result;
    }

    public void onDestroy(){
        if(mTess != null){
            mTess.end();
        }
    }
}
