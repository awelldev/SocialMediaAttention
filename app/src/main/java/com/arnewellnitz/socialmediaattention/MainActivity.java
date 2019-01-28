package com.arnewellnitz.socialmediaattention;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.Toast;
import android.support.design.widget.Snackbar;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {

    DataLog mDataLog;
    DataStorage mDataStorage;
    DisplayMetrics dm;
    int bottomOffset = 2000; // offset at the bottom in pixel
    int spacerHeight = 1800;
    int mElements; // Photos and GIFs
    int mElementHeight;
    int mSectionOffset = 100; // default 0, max ~200
    int mSections = 4; // number of sections in which each picture is splitted
    int mSectionHeight;
    int mAbsoluteHeight;
    int mScrollViewHeight;


    boolean logData = false;
    int samplingTime = 10; //milliseconds
    Handler mHandler = new Handler();
    Runnable measureTimeRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if(logData) updateValues();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mHandler.postDelayed(measureTimeRunnable, samplingTime);
            }
        }
    };

    private static final int REQUEST_WRITE_STORAGE_PERMISSION = 200;
    int[] permissionList = {200};
    int permissionPointer = 0;

    ScrollView mScrollView;
    LinearLayout mContainer;
    Menu mMenu;
    public static final String IMPORT_FOLDER = "SocialMediaAttention";
    List<File> mFileList = new ArrayList<>();
    List<String> mFileNameList = new ArrayList<>();
    List<ImageView> mImageList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDataStorage = new DataStorage(this);

        dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        bottomOffset = dm.heightPixels;

        checkPermissions(permissionList[permissionPointer]);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        mMenu = menu;
        getMenuInflater().inflate(R.menu.menu_main, menu);
        ((Switch)mMenu.findItem(R.id.log_status).getActionView().findViewById(R.id.log_status_switch)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    initMeasurement();
                    logData = true;
                    Snackbar.make(getWindow().getDecorView().getRootView(), "Recording started", Snackbar.LENGTH_SHORT).show();
                } else {
                    logData = false;
                    Snackbar.make(getWindow().getDecorView().getRootView(), "Recording stopped", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.reset) {
            new AlertDialog.Builder(this)
                    .setTitle("Reset")
                    .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((Switch)mMenu.findItem(R.id.log_status).getActionView().findViewById(R.id.log_status_switch)).setChecked(false);
                            for(int i=0; i<mDataLog.sectionList.length; i++) {
                                mDataLog.sectionList[i].mTime = 0;
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return true;
        }
        if (id == R.id.store) {
            new AlertDialog.Builder(this)
                    .setTitle("Store results")
                    .setMessage("Store and reset")
                    .setPositiveButton("Store", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ((Switch)mMenu.findItem(R.id.log_status).getActionView().findViewById(R.id.log_status_switch)).setChecked(false);
                            mFileNameList.clear();
                            for(int i=0; i<mFileList.size(); i++) {
                                mFileNameList.add(mFileList.get(i).getPath());
                            }
                            mDataStorage.createJSON(mDataLog, mFileNameList);
                            for(int i=0; i<mDataLog.sectionList.length; i++) {
                                mDataLog.sectionList[i].mTime = 0;
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .show();
            return true;
        }
        if (id == R.id.show_details) {
            for(int i=0; i<mDataLog.sectionList.length; i++) {
                System.out.println("Section "+i + ": " +mDataLog.sectionList[i].mTime);
            }
            return true;
        }
        if (id == R.id.log_status) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void checkPermissions(int permission) {
        switch(permission) {
            case REQUEST_WRITE_STORAGE_PERMISSION:
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_WRITE_STORAGE_PERMISSION);
                } else initLayout();
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_WRITE_STORAGE_PERMISSION) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Sorry, you can't use this app without granting permission to read & write storage", Toast.LENGTH_LONG).show();
                finish();
            } else initLayout();
        }
        if(permissionList[permissionPointer]==requestCode) {
            permissionPointer++;
            if(permissionPointer<permissionList.length) checkPermissions(permissionList[permissionPointer]);
        }

    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("exit?")
                .setMessage("Unsaved data will be lost")
                .setNeutralButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //dialog.cancel();
                    }
                })
                .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        //super.onBackPressed();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    void initMeasurement() {
        mElementHeight = mContainer.getChildAt(0).getHeight();
        mSectionHeight = mElementHeight/mSections;
        mAbsoluteHeight = mContainer.getHeight();
        mScrollViewHeight = mScrollView.getHeight();
        mDataLog = new DataLog(mElements, mSections, mAbsoluteHeight, mSectionHeight);
        for(int i=0; i<mImageList.size(); i++) {
            for(int j=0; j<mSections; j++) {
                mDataLog.sectionList[j+i*mSections].setTop(mImageList.get(i).getTop()+j*mSectionHeight + mSectionOffset);
                //System.out.println("section " + i + " "+ j + ": " +(mDataLog.sectionList[j+i*mSections].getTop()));
            }
        }
        //for(int i=0; i<mDataLog.sectionList.length; i++) System.out.println("section " + i + ": " +mDataLog.sectionList[i].getTop());
        System.out.println("section-height: "+mSectionHeight + "   scrollview-height: "+mScrollViewHeight + "  abs-height: "+mAbsoluteHeight);

    }

    void updateValues() {
        int d = mScrollView.getScrollY();
        String test = d + " ||   ";
        for(int i=0; i<mDataLog.sectionList.length; i++) {

            if(mDataLog.sectionList[i].getTop()>=d && mDataLog.sectionList[i].getTop()<d+mScrollViewHeight) {
                test = test + i + "  ";
                mDataLog.sectionList[i].mTime += samplingTime;
            }
            //System.out.println("Pos "+i +": "+mImageList.get(i).getTop());
        }
        System.out.println(test);
    }

    int[] findVisibleSections() {


        int[] s = new int[5];
        return s;
    }

    private List<File> getListFiles(File parentDir) {
        boolean success = false;
        if (!parentDir.exists()) {
            success = parentDir.mkdirs();
        }
        if(success) {
            Toast.makeText(this, "Fill folder with images!\n"+parentDir.getPath(), Toast.LENGTH_LONG).show();
            finish();
        }
        List<File> inFiles = new ArrayList<>();
        Queue<File> files = new LinkedList<>();
        files.addAll(Arrays.asList(parentDir.listFiles()));
        while (!files.isEmpty()) {
            File file = files.remove();
            if (file.isDirectory()) {
                files.addAll(Arrays.asList(file.listFiles()));
            } else if (file.getName().endsWith(".jpg") || file.getName().endsWith(".gif")) {
                inFiles.add(file);
            }
        }
        if (inFiles.size() > 1) {
            Collections.sort(inFiles, new Comparator<File>() {
                @Override
                public int compare(File object1, File object2) {
                    return object1.getName().compareTo(object2.getName());
                }
            });
        }
        return inFiles;
    }

    void initLayout() {
        mScrollView = findViewById(R.id.content_scrollview);
        mContainer = findViewById(R.id.content_container);

        File folder = new File(Environment.getExternalStorageDirectory() + "/" + IMPORT_FOLDER +"/");
        mFileList = getListFiles(folder);
        mElements = mFileList.size();

        for(int i=0; i<mFileList.size(); i++) {
            if(mFileList.get(i).exists()){
                String path = mFileList.get(i).getAbsolutePath();
                Bitmap bitmap = BitmapFactory.decodeFile(path);
                ImageView imageView = new ImageView(this);
                imageView.setAdjustViewBounds(true);
                if(path.contains(".gif")) {
                    Glide.with(this).load(mFileList.get(i)).into(imageView);
                } else {
                    imageView.setImageBitmap(bitmap);
                }

                mImageList.add(imageView);
                mContainer.addView(imageView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                ImageView spacer = new ImageView(this);
                spacer.setAdjustViewBounds(true);
                spacer.setImageDrawable(getResources().getDrawable(R.mipmap.abstand));
                spacer.setScaleType(ImageView.ScaleType.FIT_START);
                mContainer.addView(spacer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, spacerHeight));
            }
        }
        FrameLayout footer = new FrameLayout(this);
        mContainer.addView(footer, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, bottomOffset));
        Toast.makeText(this, mFileList.size()+" images loaded", Toast.LENGTH_SHORT).show();
        measureTimeRunnable.run();
    }
}
