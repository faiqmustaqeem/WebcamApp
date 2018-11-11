/*
 *  UVCCamera
 *  library and sample to access to UVC web camera on non-rooted Android device
 *
 * Copyright (c) 2014-2017 saki t_saki@serenegiant.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *  All files in the folder are under this Apache License, Version 2.0.
 *  Files in the libjpeg-turbo, libusb, libuvc, rapidjson folder
 *  may have a different license, see the respective files.
 */

package com.serenegiant.usbcameratest;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.pdf.PdfDocument;
import android.hardware.usb.UsbDevice;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.print.pdf.PrintedPdfDocument;
import android.provider.MediaStore;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.serenegiant.common.BaseActivity;
import com.serenegiant.usb.CameraDialog;
import com.serenegiant.usb.IButtonCallback;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.IStatusCallback;
import com.serenegiant.usb.USBMonitor;
import com.serenegiant.usb.USBMonitor.OnDeviceConnectListener;
import com.serenegiant.usb.USBMonitor.UsbControlBlock;
import com.serenegiant.usb.UVCCamera;
import com.serenegiant.widget.SimpleUVCCameraTextureView;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import io.paperdb.Paper;

public final class MainActivity extends BaseActivity implements CameraDialog.CameraDialogParent, DatePickerDialog.OnDateSetListener {

    private final Object mSync = new Object();
    // for accessing USB and USB camera
    private USBMonitor mUSBMonitor;
    private UVCCamera mUVCCamera;
    private SimpleUVCCameraTextureView mUVCCameraView;
    // for open&start / stop&close camera preview
    private Button mCameraButton;
    private Surface mPreviewSurface;

    ImageView imageView;
    Button btnTakeImage;
    Button btnAgain;
//	private IFrameCallback mIFrameCallback;
//	SurfaceTexture surfaceTexture;

    // if you need frame data as byte array on Java side, you can use this callback method with UVCCamera#setFrameCallback
    // if you need to create Bitmap in IFrameCallback, please refer following snippet.
    Bitmap bitmap;
    RelativeLayout contatainer;
    Button btnPdf;
    LinearLayout pdf_layout;
//    TextView tv_date, tv_type;
    RecyclerView rvImage;
    List<PictureModel> list = new ArrayList<>();
    ImageAdapter adapter;
    ImageView imageViewPdf;
//    TextView logout;
    ImageView options;
    FirebaseDatabase database;
    DatabaseReference reference;
    Button btnSave;
//    Button generateReport;
    String strDate="" , strType="";
    ImageView pdf_logo;
    private static int RESULT_LOAD_IMAGE = 1;
    ImageView logo;

    TextView heading_title , heading_date , heading_type , heading_place , heading_findings , heading_observations , heading_name;

    TextView tv_date , tv_type , tv_place , tv_findings , tv_observations , tv_name;

    int pageHeight=792;



    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Paper.init(this);

        database = FirebaseDatabase.getInstance();
        reference = database.getReference();
        mCameraButton = (Button) findViewById(R.id.camera_button);
        mCameraButton.setOnClickListener(mOnClickListener);

        mUVCCameraView = (SimpleUVCCameraTextureView) findViewById(R.id.UVCCameraTextureView1);
        mUVCCameraView.setAspectRatio(UVCCamera.DEFAULT_PREVIEW_WIDTH / (float) UVCCamera.DEFAULT_PREVIEW_HEIGHT);


        mUSBMonitor = new USBMonitor(this, mOnDeviceConnectListener);

        imageView = (ImageView) findViewById(R.id.ivImage);
        btnTakeImage = (Button) findViewById(R.id.btnTakeImage);
        btnAgain = (Button) findViewById(R.id.btnAgain);
        contatainer = (RelativeLayout) findViewById(R.id.container);
        btnPdf = (Button) findViewById(R.id.btnPdf);
        pdf_layout = (LinearLayout) findViewById(R.id.pdf_layout);
//        tv_date = (TextView) findViewById(R.id.tv_pdfdate);
//        tv_type = (TextView) findViewById(R.id.tv_pdftype);
        rvImage = (RecyclerView) findViewById(R.id.rv_image);

        imageViewPdf = (ImageView) findViewById(R.id.pdf_image);
//        logout = (TextView) findViewById(R.id.logout);
        options=(ImageView)findViewById(R.id.optins);
        btnSave=(Button)findViewById(R.id.btnSave);

        heading_title= (TextView) findViewById(R.id.tv_title);
        heading_date= (TextView) findViewById(R.id.tv_date_heading);
        heading_findings= (TextView) findViewById(R.id.tv_findings_heading);
        heading_name= (TextView) findViewById(R.id.tv_name_heading);
        heading_observations= (TextView) findViewById(R.id.tv_observations_heading);
        heading_place= (TextView) findViewById(R.id.tv_place_heading);
        heading_type= (TextView) findViewById(R.id.tv_type_heading);

        tv_date= (TextView)findViewById(R.id.date);
        tv_findings= (TextView)findViewById(R.id.findings);
        tv_name= (TextView)findViewById(R.id.name);
        tv_observations= (TextView)findViewById(R.id.observations);
        tv_place= (TextView)findViewById(R.id.place);
        tv_type= (TextView)findViewById(R.id.type);




        pdf_logo=(ImageView)findViewById(R.id.pdf_logo);
        logo=(ImageView)findViewById(R.id.logo);
        imageViewPdf.setAdjustViewBounds(true);
        adapter = new ImageAdapter(list);
        rvImage.setLayoutManager(new LinearLayoutManager(this));
        rvImage.setAdapter(adapter);

        if(SharedPreferenceHelper.getSharedPreferenceString(MainActivity.this,"logo","").equals(""))
        {
            logo.setImageResource(R.drawable.logo);
        }
        else {
            String b64=SharedPreferenceHelper.getSharedPreferenceString(MainActivity.this , "logo", "");
            logo.setImageBitmap(base64ToBitmap(b64));
        }


        btnTakeImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {


                mUVCCameraView.setVisibility(View.INVISIBLE);
                imageView.setVisibility(View.VISIBLE);
                bitmap = mUVCCameraView.getBitmap();
                imageView.setImageBitmap(bitmap);
//                list.add(bitmap);
                Toast.makeText(MainActivity.this, "Image taken", Toast.LENGTH_SHORT).show();
            }
        });

        btnAgain.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                mUVCCameraView.setVisibility(View.VISIBLE);
                imageView.setVisibility(View.INVISIBLE);
            }
        });

        btnPdf.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if(getAllPicturesFromDB().size() > 0){
                    showFiledsInputDialog();
                }
                else {
                    Toast.makeText(MainActivity.this, "you have not taken any picture", Toast.LENGTH_SHORT).show();
                }

            }
        });




        btnSave.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                    String base64= bitmapToBase64(bitmap);
                    PictureModel model=new PictureModel(base64);
                    saveInDB(model);
                    CapturePhotoUtils.insertImage(getContentResolver(), bitmap,System.currentTimeMillis()+"","webcam");

            }
        });

        options.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                showPopupMenu();
            }
        });

//        generateReport.setOnClickListener(new OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showFiledsInputDialog();
//            }
//        });
    }

//
//    private void generatePdfFormat()
//    {
//        heading_title.setText(R.string.title);
//        heading_date.setText(R.string.date);
//        heading_findings.setText(R.string.findings);
//        heading_name.setText(R.string.inspector_name);
//        heading_observations.setText(R.string.observation);
//        heading_place.setText(R.string.inspection_place);
//        heading_type.setText(R.string.type_of_inspection);
//    }
    private void setPdfHeaderImage()
    {
        String base64=SharedPreferenceHelper.getSharedPreferenceString(MainActivity.this , "logo","");
        if(base64.equals(""))
        {
            pdf_logo.setImageResource(R.drawable.logo_black);
        }
        else {
            try
            {
                pdf_logo.setImageBitmap(base64ToBitmap(base64));
            }
            catch (Exception ex)
            {
                pdf_logo.setImageResource(R.drawable.logo_black);
            }

        }
    }
    private void setNewPdfHeader()
    {
        Intent i = new Intent(
                Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };

            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            String image64= bitmapToBase64(BitmapFactory.decodeFile(picturePath));
            SharedPreferenceHelper.setSharedPreferenceString(MainActivity.this,"logo",image64);
            Toast.makeText(this, "Logo updated", Toast.LENGTH_SHORT).show();

        }


    }
    TextView etDate;
    private void showFiledsInputDialog()
    {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_fileds, null);
        dialogBuilder.setView(dialogView);

        final EditText  etType , etName , etFindings , etObservations , etPlace;

        etDate = (EditText) dialogView.findViewById(R.id.etDate);
        etType = (EditText) dialogView.findViewById(R.id.etType);
        etName = (EditText) dialogView.findViewById(R.id.etName);
        etFindings = (EditText) dialogView.findViewById(R.id.etFindings);
        etObservations = (EditText) dialogView.findViewById(R.id.etObservation);
        etPlace = (EditText) dialogView.findViewById(R.id.etPlace);



        etDate.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar now = Calendar.getInstance();
                DatePickerDialog dpd = DatePickerDialog.newInstance(
                        MainActivity.this,
                        now.get(Calendar.YEAR),
                        now.get(Calendar.MONTH),
                        now.get(Calendar.DAY_OF_MONTH)
                );
                dpd.show(getFragmentManager(), "Datepickerdialog");
            }
        });

        dialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });

        dialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (etDate.getText().toString().equals("")) {
                    Toast.makeText(MainActivity.this, "select date", Toast.LENGTH_SHORT).show();
                }
                else if (etType.getText().toString().equals("")) {
                    Toast.makeText(MainActivity.this, "select type", Toast.LENGTH_SHORT).show();
                }
                else if (etName.getText().toString().equals("")) {
                    Toast.makeText(MainActivity.this, "select inspectors name", Toast.LENGTH_SHORT).show();
                }
                else if (etFindings.getText().toString().equals("")) {
                    Toast.makeText(MainActivity.this, "select Findings", Toast.LENGTH_SHORT).show();
                }
                else if (etObservations.getText().toString().equals("")) {
                    Toast.makeText(MainActivity.this, "select observations", Toast.LENGTH_SHORT).show();
                }
                else if (etPlace.getText().toString().equals("")) {
                    Toast.makeText(MainActivity.this, "select place", Toast.LENGTH_SHORT).show();
                }
                else {

                    dialogInterface.dismiss();

                        List<PictureModel> pictureModelList=getAllPicturesFromDB();
                        if (pictureModelList.size() > 0) {

                            Paper.book().delete("pictures");
                            setPdfHeaderImage();

                            list.addAll(pictureModelList);

                            adapter.notifyDataSetChanged();
                            final Bitmap bitmap = getScreenshotFromRecyclerView(rvImage);
                            rvImage.setVisibility(View.GONE);
                            imageViewPdf.getLayoutParams().height = bitmap.getHeight();
                            imageViewPdf.setImageBitmap(bitmap);

                            tv_date.setText(etDate.getText().toString());
                            tv_findings.setText(etFindings.getText().toString());
                            tv_name.setText(etName.getText().toString());
                            tv_observations.setText(etObservations.getText().toString());
                            tv_place.setText(etPlace.getText().toString());
                            tv_type.setText(etType.getText().toString());
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {


//                                    pdf_layout.getLayoutParams().height = imageViewPdf.getMaxHeight() + 1000;

                                    Log.e("height_iv", imageViewPdf.getHeight() + "");
                                    Log.e("height_view", pdf_layout.getHeight() + "");
                                    printPDF(pdf_layout);
                                }
                            }, 1000);
                        }
                        else{
                            Toast.makeText(MainActivity.this, "you have not taken anty picture yet", Toast.LENGTH_SHORT).show();
                        }


                }
            }
        });





        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }
    private void showPopupMenu()
    {
        //Creating the instance of PopupMenu
        PopupMenu popup = new PopupMenu(MainActivity.this,options);
        //Inflating the Popup using xml file
        popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

        //registering popup with OnMenuItemClickListener
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
               switch (item.getItemId())
               {
                   case R.id.change_language:
                       showLangaugeSelectionDilaog();
                       break;
                   case R.id.logout:
                       final AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                       alertDialog.setTitle("Logout")
                               .setMessage("are you sure you want to logout ?")
                               .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                   @Override
                                   public void onClick(DialogInterface dialogInterface, int i) {
                                       logout();
                                   }
                               }).setNegativeButton("No", new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialogInterface, int i) {

                           }
                       }).show();

                       break;

                   case R.id.change_logo:
                       setNewPdfHeader();
                       break;
               }
                return true;
            }
        });

        popup.show();//showing popup menu
    }
    public void setLocale(String lang) {

       Locale myLocale = new Locale(lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
        Intent refresh = new Intent(this, MainActivity.class);
        startActivity(refresh);
        finish();
    }

    private boolean checkFields() {
        if (strDate.equals("")) {
            Toast.makeText(this, "select date", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (strType.equals("")) {
            Toast.makeText(this, "select type", Toast.LENGTH_SHORT).show();
            return false;
        }
        if(bitmap==null)
        {
            Toast.makeText(this, "please take image first", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void logout() {
        reference.child("webcam_app").child("users").child(SharedPreferenceHelper.getSharedPreferenceString(MainActivity.this, "key", "")).child("isLoggedIn").setValue(false);
        SharedPreferenceHelper.setSharedPreferenceBoolean(MainActivity.this, "isLoggedIn", false);
        SharedPreferenceHelper.setSharedPreferenceString(MainActivity.this, "email", "");
        Intent intent = new Intent(MainActivity.this, SplashScreen.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP |
                Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

    }

    public Bitmap getScreenshotFromRecyclerView(RecyclerView view) {
        RecyclerView.Adapter adapter = view.getAdapter();
        Bitmap bigBitmap = null;
        if (adapter != null) {
            int size = adapter.getItemCount();
            int height = 0;
            Paint paint = new Paint();
            int iHeight = 0;
            final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

            // Use 1/8th of the available memory for this memory cache.
            final int cacheSize = maxMemory / 8;
            LruCache<String, Bitmap> bitmaCache = new LruCache<>(cacheSize);
            for (int i = 0; i < size; i++) {
                RecyclerView.ViewHolder holder = adapter.createViewHolder(view, adapter.getItemViewType(i));
                adapter.onBindViewHolder(holder, i);
                holder.itemView.measure(View.MeasureSpec.makeMeasureSpec(view.getWidth(), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                holder.itemView.layout(0, 0, holder.itemView.getMeasuredWidth(), holder.itemView.getMeasuredHeight());
                holder.itemView.setDrawingCacheEnabled(true);
                holder.itemView.buildDrawingCache();
                Bitmap drawingCache = holder.itemView.getDrawingCache();
                if (drawingCache != null) {

                    bitmaCache.put(String.valueOf(i), drawingCache);
                }
//                holder.itemView.setDrawingCacheEnabled(false);
//                holder.itemView.destroyDrawingCache();
                height += holder.itemView.getMeasuredHeight();
            }


            bigBitmap = Bitmap.createBitmap(view.getMeasuredWidth(), height, Bitmap.Config.ARGB_8888);
            Canvas bigCanvas = new Canvas(bigBitmap);
            bigCanvas.drawColor(Color.WHITE);

            for (int i = 0; i < size; i++) {
                Bitmap bitmap = bitmaCache.get(String.valueOf(i));
                bigCanvas.drawBitmap(bitmap, 0f, iHeight, paint);
                iHeight += bitmap.getHeight();
                bitmap.recycle();
            }

        }
        return bigBitmap;
    }
    private void showLangaugeSelectionDilaog()
    {
        List<String> languages=new ArrayList<>();
        languages.add("en");
        languages.add("es");
        new MaterialDialog.Builder(this)
            .title(R.string.change_language)
            .items(languages)
            .itemsCallback(new MaterialDialog.ListCallback() {
                @Override
                public void onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
                    SharedPreferenceHelper.setSharedPreferenceString(MainActivity.this , "language" , text.toString());
                    setLocale(text.toString());
                }
            })
            .show();
    }

    PrintManager printManager;
    public void printPDF(View view) {
         printManager = (PrintManager) getSystemService(PRINT_SERVICE);
        assert printManager != null;
        printManager.print("print_any_view_job_name", new ViewPrintAdapter(this,
                view), null);
    }



    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth) {
        etDate.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year);
    }

    public class ViewPrintAdapter extends PrintDocumentAdapter {

        private PrintedPdfDocument mDocument;
        private Context mContext;
        private View mView;

        public ViewPrintAdapter(Context context, View view) {
            mContext = context;
            mView = view;
        }

        @Override
        public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                             CancellationSignal cancellationSignal,
                             LayoutResultCallback callback, Bundle extras) {

            mDocument = new PrintedPdfDocument(mContext, newAttributes);

            if (cancellationSignal.isCanceled()) {
                callback.onLayoutCancelled();
                return;
            }

            PrintDocumentInfo.Builder builder = new PrintDocumentInfo
                    .Builder("print_output.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1);

            PrintDocumentInfo info = builder.build();
            callback.onLayoutFinished(info, true);
        }

        @Override
        public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                            CancellationSignal cancellationSignal,
                            WriteResultCallback callback) {

            boolean isMultiplePage = false;
            int totalPages = 1;

            if (mView.getHeight() > pageHeight) {
                isMultiplePage = true;
            }
            if (isMultiplePage) {
                totalPages = (mView.getHeight() / pageHeight) + 1;
                Log.e("pages", totalPages + "");
            }
            if (!isMultiplePage) {
                // Start the page
                PdfDocument.Page page = mDocument.startPage(0);

                // Create a bitmap and put it a canvas for the view to draw to. Make it the size of the view
                Bitmap bitmap = Bitmap.createBitmap(mView.getWidth(), mView.getHeight(),
                        Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                mView.draw(canvas);
                // create a Rect with the view's dimensions.
                Rect src = new Rect(0, 0, mView.getWidth(), mView.getHeight());
                // get the page canvas and measure it.
                Canvas pageCanvas = page.getCanvas();
                float pageWidth = pageCanvas.getWidth();
                float pageHeight = pageCanvas.getHeight();

                // how can we fit the Rect src onto this page while maintaining aspect ratio?
                float scale = Math.min(pageWidth / src.width(), pageHeight / src.height());
                float left = pageWidth / 2 - src.width() * scale / 2;
                float top = pageHeight / 2 - src.height() * scale / 2;
                float right = pageWidth / 2 + src.width() * scale / 2;
                float bottom = pageHeight / 2 + src.height() * scale / 2;
                RectF dst = new RectF(left, top, right, bottom);

                pageCanvas.drawBitmap(bitmap, src, dst, null);

                mDocument.finishPage(page);
            } else {

                for (int i = 0; i < totalPages; i++) {
                    PdfDocument.Page page = mDocument.startPage(i);


                    // Create a bitmap and put it a canvas for the view to draw to. Make it the size of the view
                    Bitmap bitmap = Bitmap.createBitmap(mView.getWidth(), mView.getHeight(),
                            Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    mView.draw(canvas);
                    // create a Rect with the view's dimensions.
                    Rect src = new Rect(0, i * pageHeight, mView.getWidth(), pageHeight + i * pageHeight);
                    // get the page canvas and measure it.
                    Canvas pageCanvas = page.getCanvas();
                    float pageWidth = pageCanvas.getWidth();
                    float pageHeight = pageCanvas.getHeight();

                    // how can we fit the Rect src onto this page while maintaining aspect ratio?
                    float scale = Math.min(pageWidth / src.width(), pageHeight / src.height());
                    float left = pageWidth / 2 - src.width() * scale / 2;
                    float top = pageHeight / 2 - src.height() * scale / 2;
                    float right = pageWidth / 2 + src.width() * scale / 2;
                    float bottom = pageHeight / 2 + src.height() * scale / 2;
                    RectF dst = new RectF(left, top, right, bottom);

                    pageCanvas.drawBitmap(bitmap, src, dst, null);

                    mDocument.finishPage(page);
                }
            }


            try {
                mDocument.writeTo(new FileOutputStream(
                        destination.getFileDescriptor()));
            } catch (IOException e) {
                callback.onWriteFailed(e.toString());
                return;
            } finally {
                mDocument.close();
                mDocument = null;
            }
            callback.onWriteFinished(new PageRange[]{new PageRange(0, 0)});
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        mUSBMonitor.register();
        synchronized (mSync) {
            if (mUVCCamera != null) {
                mUVCCamera.startPreview();
            }
        }
    }

    @Override
    protected void onStop() {
        synchronized (mSync) {
            if (mUVCCamera != null) {
                mUVCCamera.stopPreview();
            }
            if (mUSBMonitor != null) {
                mUSBMonitor.unregister();
            }
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        synchronized (mSync) {
            releaseCamera();
            if (mToast != null) {
                mToast.cancel();
                mToast = null;
            }
            if (mUSBMonitor != null) {
                mUSBMonitor.destroy();
                mUSBMonitor = null;
            }
        }
        mUVCCameraView = null;
        mCameraButton = null;

        super.onDestroy();
    }

    private final OnClickListener mOnClickListener = new OnClickListener() {
        @Override
        public void onClick(final View view) {
            synchronized (mSync) {
                if (mUVCCamera == null) {
                    CameraDialog.showDialog(MainActivity.this);
                } else {
                    releaseCamera();
                }
            }
        }
    };

    private Toast mToast;

    private final OnDeviceConnectListener mOnDeviceConnectListener = new OnDeviceConnectListener() {
        @Override
        public void onAttach(final UsbDevice device) {
            Toast.makeText(MainActivity.this, "USB_DEVICE_ATTACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onConnect(final UsbDevice device, final UsbControlBlock ctrlBlock, final boolean createNew) {
            releaseCamera();
            queueEvent(new Runnable() {
                @Override
                public void run() {
                    final UVCCamera camera = new UVCCamera();
                    camera.open(ctrlBlock);
                    camera.setStatusCallback(new IStatusCallback() {
                        @Override
                        public void onStatus(final int statusClass, final int event, final int selector,
                                             final int statusAttribute, final ByteBuffer data) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    final Toast toast = Toast.makeText(MainActivity.this, "onStatus(statusClass=" + statusClass
                                            + "; " +
                                            "event=" + event + "; " +
                                            "selector=" + selector + "; " +
                                            "statusAttribute=" + statusAttribute + "; " +
                                            "data=...)", Toast.LENGTH_SHORT);
                                    synchronized (mSync) {
                                        if (mToast != null) {
                                            mToast.cancel();
                                        }
                                        toast.show();
                                        mToast = toast;
                                    }
                                }
                            });
                        }
                    });
                    camera.setButtonCallback(new IButtonCallback() {
                        @Override
                        public void onButton(final int button, final int state) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    final Toast toast = Toast.makeText(MainActivity.this, "onButton(button=" + button + "; " +
                                            "state=" + state + ")", Toast.LENGTH_SHORT);
                                    synchronized (mSync) {
                                        if (mToast != null) {
                                            mToast.cancel();
                                        }
                                        mToast = toast;
                                        toast.show();
                                    }
                                }
                            });
                        }
                    });
//					camera.setPreviewTexture(camera.getSurfaceTexture());
                    if (mPreviewSurface != null) {
                        mPreviewSurface.release();
                        mPreviewSurface = null;
                    }
                    try {
                        camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.FRAME_FORMAT_MJPEG);
                    } catch (final IllegalArgumentException e) {
                        // fallback to YUV mode
                        try {
                            camera.setPreviewSize(UVCCamera.DEFAULT_PREVIEW_WIDTH, UVCCamera.DEFAULT_PREVIEW_HEIGHT, UVCCamera.DEFAULT_PREVIEW_MODE);
                        } catch (final IllegalArgumentException e1) {
                            camera.destroy();
                            return;
                        }
                    }
                    final SurfaceTexture st = mUVCCameraView.getSurfaceTexture();
                    if (st != null) {
                        mPreviewSurface = new Surface(st);
                        camera.setPreviewDisplay(mPreviewSurface);
//						camera.setFrameCallback(mIFrameCallback, UVCCamera.PIXEL_FORMAT_RGB565/*UVCCamera.PIXEL_FORMAT_NV21*/);
                        camera.startPreview();
                    }
                    synchronized (mSync) {
                        mUVCCamera = camera;
                    }
                }
            }, 0);
        }

        @Override
        public void onDisconnect(final UsbDevice device, final UsbControlBlock ctrlBlock) {
            // XXX you should check whether the coming device equal to camera device that currently using
            releaseCamera();
        }

        @Override
        public void onDettach(final UsbDevice device) {
            Toast.makeText(MainActivity.this, "USB_DEVICE_DETACHED", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onCancel(final UsbDevice device) {
        }
    };

    private synchronized void releaseCamera() {
        synchronized (mSync) {
            if (mUVCCamera != null) {
                try {
                    mUVCCamera.setStatusCallback(null);
                    mUVCCamera.setButtonCallback(null);
                    mUVCCamera.close();
                    mUVCCamera.destroy();
                } catch (final Exception e) {
                    //
                }
                mUVCCamera = null;
            }
            if (mPreviewSurface != null) {
                mPreviewSurface.release();
                mPreviewSurface = null;
            }
        }
    }

    /**
     * to access from CameraDialog
     *
     * @return
     */
    @Override
    public USBMonitor getUSBMonitor() {
        return mUSBMonitor;
    }

    @Override
    public void onDialogResult(boolean canceled) {
        if (canceled) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // FIXME
                }
            }, 0);
        }
    }
    private void saveInDB(PictureModel model)
    {
        List<PictureModel> list= Paper.book().read("pictures");
        Paper.book().delete("pictures");
        if(list==null)
        {
            list=new ArrayList<>();
        }
        list.add(model);
        Paper.book().write("pictures",list);
        Toast.makeText(MainActivity.this, "Image saved", Toast.LENGTH_SHORT).show();
    }
    private List<PictureModel> getAllPicturesFromDB()
    {
        List<PictureModel> list;
        try{
            list=Paper.book().read("pictures");
            if(list==null)
            {
                return new ArrayList<>();
            }
            return list;
        }
       catch (Exception e)
       {
           return  new ArrayList<>();
       }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream .toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private Bitmap base64ToBitmap(String b64) {
        byte[] imageAsBytes = Base64.decode(b64.getBytes(), Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
    }


}
