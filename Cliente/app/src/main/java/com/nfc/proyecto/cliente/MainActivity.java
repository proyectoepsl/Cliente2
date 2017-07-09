package com.nfc.proyecto.cliente;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcelable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.support.design.widget.Snackbar;

import java.io.BufferedReader;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import android.util.Base64;


public class MainActivity extends AppCompatActivity {
    ///TextView mensaje;
    String imei;
    String respStr;
    String resultado;
    private static final int MY_WRITE_EXTERNAL_STORAGE = 0;
    private View mLayout;
    JSONObject obj;
    Button IdenSala;

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mLayout = findViewById(R.id.linearLayoutMain);

        IdenSala = (Button) findViewById(R.id.IdenSala);
        final ImageView imgViewer = (ImageView) findViewById(R.id.imageView);
        final TextView mensaje = (TextView) findViewById(R.id.Mensaje);

        IdenSala.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Ver imei del dispositivo
                verifyPermission();
                //final String imei = mensaje.getText().toString();//btenerImei();


                //Enviar datos por post
                new Thread(new Runnable(){
                    @Override
                    public void run() {
                        try {
                            //Cifrar imei
                            Crypt aesCrypt = new Crypt();
                            String a = aesCrypt.encrypt_string(imei);
                            // Your implementation
                            HttpClient httpClient = new DefaultHttpClient();


                            //Llamamos al rest con los datos cifrados
                            // HttpPost post = new HttpPost("https://proyectoepsl.pythonanywhere.com/rest_sala");
                            HttpPost post = new HttpPost("http://192.168.2.129:8000/rest_sala/");
                            post.setHeader("Content-Type", "application/json");
                            post.setHeader("charset", "utf-8");
                            //Construimos el objeto cliente en formato JSON
                            JSONObject dato = new JSONObject();
                            dato.put("Hash", a);
                            StringEntity entity = new StringEntity(dato.toString());
                            post.setEntity(entity);
                            //Realizo el envío
                            HttpResponse resp = httpClient.execute(post);
                            respStr = EntityUtils.toString(resp.getEntity());
                            obj = new JSONObject(respStr);
                            resultado = obj.get("result").toString();
                            if (resultado.equals("200")) {
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    public void run () {
                                        //Do something on UiThread
                                        try {
                                            byte[] byteArray = "".getBytes();

                                            //JSONObject obj = new JSONObject(respStr);
                                            byteArray = obj.get("Plano").toString().getBytes(StandardCharsets.US_ASCII);;
                                            byteArray =  Base64.decode(byteArray, Base64.DEFAULT);

                                            Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                                            imgViewer.setImageBitmap(bitmap);

                                            mensaje.setText("Plano Emergencia Sala Nº " + obj.get("Dependencia").toString());

                                        } catch (Throwable t) {
                                            Log.e("My App", "Could not parse malformed JSON: \"" + respStr + "\"");
                                        }
                                    }
                                });

                            } else{
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    public void run () {
                                        //Do something on UiThread
                                        try {
                                            imgViewer.setImageDrawable(getResources().getDrawable(R.drawable.error));
                                            mensaje.setText(obj.get("Error").toString());
                                        } catch (Throwable t) {
                                            t.printStackTrace();
                                        }
                                    }
                                });
                            }

                            //Gson gson = new Gson();
                            // 2. JSON to Java object, read it from a Json String.
                            //gson.fromJson(respStr, Sala.class);

                            //mensaje.setText(respStr);
                            //String plano = respStr[]
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }).start();

            }
        });

        final Button LeerNfc = (Button) findViewById(R.id.nfc);
        LeerNfc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                try{
                    Intent i = new Intent(view.getContext(), NfcActivity.class);
                    //Obtenemos el numero de sala para cifrarlo pasandolo a la segunda actividad
                    //i.putExtra("Sala", respStr.toString());
                    i.putExtra("Sala_id", obj.get("IdSala").toString());
                    //i.putExtra("Dependencia", obj.get("Dependencia").toString());
                    //i.putExtra("Plano", obj.get("Plano").toString());
                    startActivity(i);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    /**
     * Guarda el comentario
     */
    private void obtenerIMEI() {
        TelephonyManager tel;
        tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //TextView mensaje = (TextView) findViewById(R.id.Imei);
        imei= tel.getDeviceId().toString();
        //mensaje.setText(imei);
        //return imei;
    }

    //Paso 1. Verificar permiso
    @TargetApi(Build.VERSION_CODES.M)
    private void verifyPermission() {

        //WRITE_EXTERNAL_STORAGE tiene implícito READ_EXTERNAL_STORAGE porque pertenecen al mismo
        //grupo de permisos
        if(Build.VERSION.SDK_INT>=23) {
            int writePermission = checkSelfPermission(Manifest.permission.READ_PHONE_STATE);

            if (writePermission != PackageManager.PERMISSION_GRANTED) {
                requestPermission();
            } else {
                obtenerIMEI();
            }
        } else {
            //simply use the required feature
            //as the user has already granted permission to them during installation
            obtenerIMEI();
        }
    }


    //Paso 2: Solicitar permiso
    @TargetApi(Build.VERSION_CODES.M)
    private void requestPermission() {
        //shouldShowRequestPermissionRationale es verdadero solamente si ya se había mostrado
        //anteriormente el dialogo de permisos y el usuario lo negó
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_PHONE_STATE)) {
            showSnackBar();
        } else {
            //si es la primera vez se solicita el permiso directamente
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE},
                    MY_WRITE_EXTERNAL_STORAGE);
        }
    }

    //Paso 3: Procesar respuesta de usuario
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        //Si el requestCode corresponde al que usamos para solicitar el permiso y
        //la respuesta del usuario fue positiva
        if (requestCode == MY_WRITE_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                obtenerIMEI();
            } else {
                showSnackBar();
            }
        }
    }

    /**
     * Método para mostrar el snackbar de la aplicación.
     * Snackbar es un componente de la librería de diseño 'com.android.support:design:23.1.0'
     * y puede ser personalizado para realizar una acción, como por ejemplo abrir la actividad de
     * configuración de nuestra aplicación.
     */
    private void showSnackBar() {
        Snackbar.make(mLayout, R.string.permission_write_storage,
                Snackbar.LENGTH_LONG)
                .setAction(R.string.settings, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        openSettings();
                    }
                })
                .show();
    }

    /**
     * Abre el intento de detalles de configuración de nuestra aplicación
     */
    public void openSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    /* Checks if external storage is available for read and write */

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();

        return Environment.MEDIA_MOUNTED.equals(state);
    }
}
