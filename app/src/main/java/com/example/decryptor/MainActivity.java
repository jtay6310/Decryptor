package com.example.decryptor;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

    private static String algorithm = "AES";
    private static SecretKey yourKey = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isStoragePermissionGranted();

        Button mBtn = findViewById(R.id.KeyBtn);
        EditText mEdit = findViewById(R.id.editKey);

        mBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String FirebaseKey = mEdit.getText().toString();
                Log.d("Key",FirebaseKey);
                byte[] decodedKey = new byte[0];
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    decodedKey = Base64.getDecoder().decode(FirebaseKey);
                }
                SecretKey originalKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
                yourKey = originalKey;
                final String[] columns = {MediaStore.Images.Media.DATA, MediaStore.Images.Media._ID};
                final String orderBy = MediaStore.Images.Media._ID;

                Cursor cursor = getContentResolver().query(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, null,
                        null, orderBy);

                int count = cursor.getCount();

                ArrayList<String> arrPath = new ArrayList<String>();

                //saving the picture paths to an array
                for (int i = 0; i < count; i++) {
                    cursor.moveToPosition(i);
                    int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                    //only keeping images taken by user with camera app
                    if (cursor.getString(dataColumnIndex).contains("/Camera/")) {
                        arrPath.add(cursor.getString(dataColumnIndex));
                    }
                }
                for (int i = 0; i < arrPath.size(); i++) { //for each path
                    File test = new File(arrPath.get(i));  //get the picture
                    int size = (int) test.length();
                    byte[] bytes = new byte[size];
                    BufferedInputStream buf = null;
                    try {
                        buf = new BufferedInputStream(new FileInputStream(test));
                        buf.read(bytes, 0, bytes.length);
                        decodeFile(bytes, arrPath.get(i));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }

        });

    }

    public static byte[] decodeFile(SecretKey yourKey, byte[] fileData)
            throws Exception {
        byte[] decrypted = null;
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, yourKey, new IvParameterSpec(
                new byte[cipher.getBlockSize()]));
        decrypted = cipher.doFinal(fileData);
        return decrypted;
    }

    void decodeFile(byte[] data,String path) {

        try {
            File file = new File(path);
            BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(file));
            byte[] decodedData = decodeFile(yourKey, data);
                bos.write(decodedData);
                bos.flush();
                bos.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return;
        } catch (IOException e) {
            e.printStackTrace();
            return;
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

    }

    public  boolean isStoragePermissionGranted() {
        String TAG = "Perms";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG,"Permission is granted");
                return true;
            } else {

                Log.v(TAG,"Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG,"Permission is granted");
            return true;
        }
    }
}

