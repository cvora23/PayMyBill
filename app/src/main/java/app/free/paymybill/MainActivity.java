package app.free.paymybill;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity implements View.OnClickListener {

    private TessOCR mTessOCR;
    private TextView mResult;
    private ProgressDialog mProgessDialog;
    private ImageView mImage;
    private Button mButtonGallery;
    private Button mButtonRotate;
    private Button mButtonOCR;

    private Bitmap mBitmap;

    float angle=0;
    public static final String TAG = "MainActivity.java";

    private static final int REQUEST_PICK_PHOTO = 1;
    private static final int REQUIRED_WIDTH = 100;
    private static final int REQUIRED_LENGTH= 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mResult = (TextView)findViewById(R.id.ocr_result);
        mResult.setMovementMethod(new ScrollingMovementMethod());
        mImage = (ImageView)findViewById(R.id.image);
        mButtonGallery = (Button) findViewById(R.id.bt_gallery);
        mButtonGallery.setOnClickListener(this);
        mButtonRotate = (Button) findViewById(R.id.bt_rotate);
        mButtonRotate.setOnClickListener(this);
        mButtonOCR = (Button) findViewById(R.id.bt_ocr);
        mButtonOCR.setOnClickListener(this);
        mTessOCR = new TessOCR(this.getApplicationContext());
        mBitmap = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    public Bitmap decodeSampledBitmapFromUri(Uri uri,int reqWidth, int reqHeight) {
        Bitmap bitmap = null;
        InputStream in = null;
        if(uri != null){
            try{
                // First decode with inJustDecodeBounds=true to check dimensions
                final BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                in =getContentResolver().openInputStream(uri);
                BitmapFactory.decodeStream(in, null, options);
                in.close();

                // Calculate inSampleSize
                options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
                // Decode bitmap with inSampleSize set
                options.inJustDecodeBounds = false;
                in =getContentResolver().openInputStream(uri);
                bitmap = BitmapFactory.decodeStream(in, null, options);
                mImage.setImageBitmap(bitmap);
                mBitmap = bitmap;
            }catch (Exception e){
                e.printStackTrace();
            }finally{
                if(in != null){
                    try{
                        in.close();
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                }
            }
        }
        return bitmap;
    }

    private void setImage(Uri uri){
        if(uri != null){
            InputStream in = null;
            try{
                in =getContentResolver().openInputStream(uri);
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                mImage.setImageBitmap(bitmap);
               // mBitmap = bitmap;
            }catch (FileNotFoundException e){
                e.printStackTrace();
            }finally{
                if(in != null){
                    try{
                        in.close();
                    }catch(IOException e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void rotateImage(Bitmap sourceImage,float angle){
        if(sourceImage != null){
            Matrix matrix = new Matrix();
            matrix.postRotate(angle);
            Bitmap bitmap = Bitmap.createBitmap(sourceImage,0,0,sourceImage.getWidth(),sourceImage.getHeight(),matrix,true);
            mImage.setImageBitmap(bitmap);
            mBitmap = bitmap;
        }else{
            Log.v(TAG,"Rotate Image - Source Image is null");
        }
    }

    private void  doOCR(final Bitmap sourceImage){

        if(sourceImage != null){

            if(mProgessDialog == null){
                mProgessDialog = ProgressDialog.show(this,"Processing","Doing OCR .....",true);
            }else{
                mProgessDialog.show();
            }

            new Thread(new Runnable() {
                @Override
                public void run() {
                    final String result = mTessOCR.getOCRResultUTF8Format(sourceImage);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(result != null && !result.equals("")){
                                Log.v(TAG,"Result is : " + result);
                                mResult.setText(result);
                            }else{
                                Log.v(TAG,"Result is null");
                            }
                            mProgessDialog.dismiss();
                        }
                    });
                }
            }).start();
        }else{
            Log.v(TAG,"doOCR Image - Source Image is null");
        }
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){
        if(requestCode == REQUEST_PICK_PHOTO && resultCode == Activity.RESULT_OK){
            Uri uri = data.getData();
            if (uri != null){
                mBitmap = decodeSampledBitmapFromUri(uri,REQUIRED_WIDTH,REQUIRED_LENGTH);
                mImage.setImageBitmap(mBitmap);
            }
        }
        else{
            Log.v(TAG,"Failed on ActivityResult requestCode = "+requestCode);
        }
    }

    private void pickPhoto(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_PICK_PHOTO);
    }

    private void rotatePhoto(){
        angle = 90;
        rotateImage(mBitmap, angle);
    }

    private void ocrPhoto(){
        doOCR(mBitmap);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch(id){
            case R.id.bt_gallery:
                Log.v(TAG,"Picking photo");
                pickPhoto();
                break;
            case R.id.bt_rotate:
                Log.v(TAG,"Rotating picked photo");
                rotatePhoto();
                break;
            case R.id.bt_ocr:
                Log.v(TAG,"OCR'ing picked photo");
                ocrPhoto();
                break;
            default:
                Log.v(TAG,"Default view selection"+id);
                break;
        }
    }


}
