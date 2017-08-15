package com.nfc.proyecto.cliente;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.apache.http.entity.StringEntity;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

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

    //Clases para mostrar el menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.configuracion, menu);
        return true;
    }

    //Clase para seleccionar las Opciones del menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent j = new Intent(getApplicationContext(), UserActivity.class);
        switch (item.getItemId()) {
            case R.id.username:
                j.putExtra("item", item.getTitle());
                startActivity(j);
                return true;
            case R.id.password:
                j.putExtra("item", item.getTitle());
                startActivity(j);
                return true;
            case R.id.url:
                j.putExtra("item", item.getTitle());
                startActivity(j);
                return true;
            case R.id.pin:
                j.putExtra("item", item.getTitle());
                startActivity(j);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

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
        final TextView capacidad = (TextView) findViewById(R.id.Capacidad);
        final TextView aforo = (TextView) findViewById(R.id.Aforo);

        //Llamada al boton de identificar sala
        IdenSala.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                //Funcion para ver imei del dispositivo
                verifyPermission();
                //Enviar datos por post al servidor
                try {
                    //1º:Encritacion de los datos
                    //LLanada a la funcion de encriptar
                    Crypt aesCrypt = Crypt.getInstance();
                    //Encriptacion del imei
                    String a = aesCrypt.encrypt_string(imei);
                    JSONObject dato = new JSONObject();
                    dato.put("Hash", a);

                    Prefern prefern = Prefern.getPrefern();
                    String url = prefern.getVariable("Url");

                    //Creo entidad para enviar los datos
                    StringEntity entity = new StringEntity(dato.toString());
                    Http http = Http.getHttp();
                    http = Http.getHttp();
                    http.setEntity(entity);
                    http.setUrl(url);
                    http.setRest("/rest_sala/");
                    http.doInBackground();
                    obj = http.getResponse();
                    resultado = obj.get("result").toString();


                    //Si la accion se ha realizado sin problemas
                    if (resultado.equals("200")) {
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
                            mensaje.setText("Dependencia Nº " + obj.get("Dependencia").toString());
                            capacidad.setText("Capacidad Máxima:" + obj.get("Capacidad").toString());
                            aforo.setText("Aforo actual:" + obj.get("Aforo").toString());
                        } catch (Exception ex) {
                            Log.e("Service", "Error!", ex);
                        } catch (Throwable t) {

                            t.printStackTrace();
                        }
                    } else {
                        //Mostrar el mensaje de error del servidor
                        try {
                            //Muestro imagen de error
                            imgViewer.setImageDrawable(getResources().getDrawable(R.drawable.error_sala));
                            //Muestro mensaje de error
                            mensaje.setText(obj.get("Error").toString());
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
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
        tel = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
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
