package com.arnewellnitz.socialmediaattention;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

public class DataStorage {

    Context mContext;

    public DataStorage(Context context) {
        mContext = context;
    }

    public boolean createJSON(DataLog results, List<String> fileNames) {
        JSONObject content = new JSONObject();
        JSONArray timeList = new JSONArray();
        JSONArray fileNameList = new JSONArray();
        try {
            content.put("ELEMENTS", results.mElements);
            content.put("SECTIONS", results.mSections);
            for(int i=0; i<results.sectionList.length; i++) {
                timeList.put(results.sectionList[i].mTime);
            }
            content.put("TIME_LIST", timeList);
            for(int i=0; i<fileNames.size(); i++) {
                fileNameList.put(fileNames.get(i));
            }
            content.put("FILE_LIST", fileNameList);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return saveJSON(content.toString(), "");
    }

    public boolean saveJSON(String fileContent, String nameExtra) {
        String filename = "SMA_"+(new SimpleDateFormat("yyyyMMdd_HHmm").format((new Date()).getTime()));  // yyyyMMdd_HHmm
        //if(phonejson) filename = filename + "_phone.json";
        filename = filename + nameExtra + ".json";

        try {
            FileOutputStream fileOut = mContext.openFileOutput(filename, MODE_PRIVATE);
            fileOut.write(fileContent.getBytes());
            fileOut.close();
            Toast.makeText(mContext, "JSON saved\n"+filename, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        Uri uri = Uri.parse("file://" + mContext.getFilesDir() + "/" + filename);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        //startActivity(Intent.createChooser(intent, "share JSON"));

        return exportFile(fileContent, filename);
    }

    public boolean exportFile(String fileContent, String filename) {

        String exportFolder = "/"+ MainActivity.IMPORT_FOLDER+"/" + "results" + "/";
        String exportFile = exportFolder + filename; // getString(R.string.app_name)
        FileOutputStream out;

        File folder = new File(Environment.getExternalStorageDirectory() + exportFolder);
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdirs();
        }
        if (success) {
            File json_file = new File(Environment.getExternalStorageDirectory()
                    + exportFile);
            if (json_file.exists()) json_file.delete();

            try {
                json_file.createNewFile();
                out = new FileOutputStream(json_file);
                OutputStreamWriter myOutWriter = new OutputStreamWriter(out);
                myOutWriter.write(fileContent, 0, fileContent.length());
                myOutWriter.close();
                out.flush();
                out.close();
                Toast.makeText(mContext, filename+"\nsaved to folder", Toast.LENGTH_LONG).show();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Exception");
                return false;
            }
        } else {
            Toast.makeText(mContext, "export error", Toast.LENGTH_SHORT).show();
            return false;
        }

    }
}
