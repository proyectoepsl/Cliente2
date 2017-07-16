package com.nfc.proyecto.cliente;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.support.design.widget.Snackbar;

import java.nio.charset.StandardCharsets;

import android.util.Base64;


public class MainActivity extends AppCompatActivity {
    //Inicializacion de variables.
    String imei;
    String respStr;
    String resultado;
    JSONObject obj;
    Button IdenSala;

    private static final int MY_WRITE_EXTERNAL_STORAGE = 0;
    //Vista para mostrar pantalla de permisos
    private View mLayout;


    @Override
    @TargetApi(Build.VERSION_CODES.M)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Inicializacion de Layout
        setContentView(R.layout.activity_main);
        mLayout = findViewById(R.id.linearLayoutMain);
        final Button LeerNfc = (Button) findViewById(R.id.nfc);
        IdenSala = (Button) findViewById(R.id.IdenSala);
        final ImageView imgViewer = (ImageView) findViewById(R.id.imageView);
        final TextView mensaje = (TextView) findViewById(R.id.Mensaje);

        //Llamada al boton de identificar sala
        IdenSala.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Funcion para ver imei del dispositivo
                verifyPermission();

                //Enviar datos por post al servidor
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //1º:Encritacion de los datos
                            //LLanada a la funcion de encriptar
                            Crypt aesCrypt = new Crypt();
                            //Encriptacion del imei
                            String a = aesCrypt.encrypt_string(imei);
                            //2º:Envio de datos por HTTP

                            HttpClient httpClient = new DefaultHttpClient();
                            //Url del servidor
                            //https://proyectoepsl.pythonanywhere.com/rest_sala/
                            //http://192.168.2.129:8000/rest_sala/
                            HttpPost post = new HttpPost("https://proyectoepsl.pythonanywhere.com/rest_sala/");
                            //Cabecera del envio de datos
                            post.setHeader("Content-Type", "application/json");
                            post.setHeader("charset", "utf-8");
                            //Construimos el objeto en formato JSON
                            JSONObject dato = new JSONObject();
                            //El dato encritado en json se llama Hash
                            dato.put("Hash", a);
                            //Creo entidad para enviar los datos
                            StringEntity entity = new StringEntity(dato.toString());
                            post.setEntity(entity);
                            //Realizo el envío
                            HttpResponse resp = httpClient.execute(post);

                            //3º:Procesar la respuesta del servidor
                            //Obtengo respuesta del servidor
                            respStr = EntityUtils.toString(resp.getEntity());
                            //Creo un objeto Json con esta respuesta para poder acceder a los datos
                            obj = new JSONObject(respStr);

                            //4º:Segun la respuesta del servidor llevo a cabo las acciones.
                            //Obtengo el objeto resultado de la respuesta
                            resultado = obj.get("result").toString();
                            //Si la accion se ha realizado sin problemas
                            if (resultado.equals("200")) {
                                //Hilo de la interface del usuario en la que estoy
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    public void run() {

                                        try {
                                            //Procesado de la imagen que me llega del servidor
                                            byte[] byteArray = "".getBytes();
                                            //Paso el JSON a string y lo condifico a ASCII
                                            byteArray = obj.get("Plano").toString().getBytes(StandardCharsets.US_ASCII);
                                            //Lo decodifico en Base64
                                            byteArray = Base64.decode(byteArray, Base64.DEFAULT);
                                            //Crear un ojeto Bitmap que es una imagen en bites
                                            Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                                            //Muestro la imagen en la vista imgViewer
                                            imgViewer.setImageBitmap(bitmap);
                                            //Oculto el boton de identificar sala
                                            IdenSala.setVisibility(View.GONE);
                                            //Muestro el boton de leer NFC
                                            LeerNfc.setVisibility(View.VISIBLE);
                                            //Muestro el valor de la dependencia
                                            mensaje.setText("Plano Emergencia Sala Nº " + obj.get("Dependencia").toString());

                                        } catch (Throwable t) {

                                            Log.e("My App", "JSON mal formado \"" + respStr + "\"");
                                        }
                                    }
                                });

                            } else {
                                //Mostrar el mensaje de error del servidor
                                //Hilo de la interface del usuario en la que estoy
                                MainActivity.this.runOnUiThread(new Runnable() {
                                    public void run() {
                                        //Do something on UiThread
                                        try {
                                            //Muestro imagen de error
                                            imgViewer.setImageDrawable(getResources().getDrawable(R.drawable.error));
                                            //Muestro mensaje de error
                                            mensaje.setText(obj.get("Error").toString());
                                        } catch (Throwable t) {
                                            t.printStackTrace();
                                        }
                                    }
                                });
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }).start();

            }
        });

        //4º Pasar los datos a la vista de NFC

        LeerNfc.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                try {

                    Intent i = new Intent(view.getContext(), NfcActivity.class);
                    //Obtenemos el numero de sala
                     i.putExtra("Sala_id", obj.get("IdSala").toString());
                     startActivity(i);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    /**
     * Funcion para obtener el imei
     */
    private void obtenerIMEI() {
        /*Android APIs para supervisar la información básica del teléfono, como el tipo de red y el
        estado de conexión, además de utilidades para manipular cadenas de números telefónicos.
         */
        TelephonyManager tel;
        tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        imei = tel.getDeviceId().toString();
    }
    //Funcion para verificar los permisos del usuario
    //Paso 1. Verificar permiso
    @TargetApi(Build.VERSION_CODES.M)
    private void verifyPermission() {


        //Compara que version de android que tiene el sistema
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //Se comprueba si la aplicacion tiene permiso
            int writePermission = checkSelfPermission(Manifest.permission.READ_PHONE_STATE);
            //Si la aplicacion no tiene permisos se solicita
            if (writePermission != PackageManager.PERMISSION_GRANTED) {
                //Solicitud de permisos
                requestPermission();
            } else {
                //La aplicacion ya se le han concedido antes los permisos
                obtenerIMEI();
            }
        } else {
            //La version de android no requiere permisos
            obtenerIMEI();
        }

    }


    //Paso 2: Solicitar permiso
    @TargetApi(Build.VERSION_CODES.M)
    private void requestPermission() {
        /*shouldShowRequestPermissionRationale es verdadero solamente si ya se había mostrado
        anteriormente el dialogo de permisos y el usuario lo negó*/
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.READ_PHONE_STATE)) {

            showSnackBar();
        } else {
            /*Si el usuario rechaza la solicitud de permiso en el pasado y selecciona la opción Don't ask again en el diálogo de
                 solicitud de permiso del sistema, el shouldShowRequestPermissionRationale muestra false.
                También muestra false si una política de dispositivo prohíbe que la app tenga ese permiso.*/
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
                // Si el usuario no acepta el permiso
                showSnackBar();
            }
        }
    }

    /**
     * Método para mostrar el snackbar de la aplicación.
     * Snackbar es un componente de la librería de diseño 'com.android.support:design:23.1.0'

     */
    private void showSnackBar() {
        Snackbar.make(mLayout, R.string.permission_write_storage,
                Snackbar.LENGTH_LONG)
                .setAction(R.string.settings, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //Abre la ventana de permisos de la aplicacion
                        openSettings();
                    }
                })
                .show();
    }

    /**
     * Abre los detalles de configuración de nuestra aplicación
     */
    public void openSettings() {
        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }
   
}
