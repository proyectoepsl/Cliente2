package com.nfc.proyecto.cliente;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.entity.StringEntity;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {
    //Inicializacion de variables.
    String respStr;
    String resultado;
    JSONObject obj;
    Bundle bundle;
    ImageView imgViewer;

    //Declaracion de preferncias
    SharedPreferences prefs;
    SharedPreferences prefspin;
    Prefern prefern;

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
        //Inicializacion de Layout
        setContentView(R.layout.activity_login);
        bundle = savedInstanceState;
        final Button Login = (Button) findViewById(R.id.login);

        final EditText url = (EditText) findViewById(R.id.url);
        final EditText username = (EditText) findViewById(R.id.username);
        final EditText password = (EditText) findViewById(R.id.password);
        final EditText pin = (EditText) findViewById(R.id.pin);
        final TextView mensaje = (TextView) findViewById(R.id.Mensaje);

        //Crear preferencias
        prefspin = getSharedPreferences("Pin", Context.MODE_PRIVATE);
        prefs = getSharedPreferences("Preferencias", Context.MODE_PRIVATE);

        prefern = Prefern.getPrefern();
        prefern.setPrefspin(prefspin);
        prefern.setPrefs(prefs);
        prefern.setContext(this.getApplicationContext());

        Login.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                final String username1 = username.getText().toString();
                final String password1 = password.getText().toString();
                final String url1 = url.getText().toString();
                boolean login = prefern.savePin(pin);
                if(username1.isEmpty() || password1.isEmpty() || url1.isEmpty()){
                    Toast.makeText(LoginActivity.this, "Introduzca todos los datos", Toast.LENGTH_LONG).show();
                }
                else {
                    if (login == true) {
                        try {
                            Crypt aesCrypt = Crypt.getInstance();
                            String a = aesCrypt.encrypt_string(username1);
                            String b = aesCrypt.encrypt_string(password1);

                            //Construimos el objeto en formato JSON
                            JSONObject dato = new JSONObject();
                            //El dato encritado en json
                            dato.put("username", a);
                            dato.put("password", b);
                            //Creo entidad para enviar los datos
                            StringEntity entity = new StringEntity(dato.toString());

                            Http http = Http.getHttp();
                            http = Http.getHttp();
                            http.setEntity(entity);
                            http.setUrl(url1);
                            http.setRest("/rest_login/");
                            http.doInBackground();
                            obj = http.getResponse();

                            //Obtengo el objeto resultado de la respuesta
                            resultado = obj.get("result").toString();
                            //Si la accion se ha realizado sin problemas
                            if (resultado.equals("200")) {
                                //Guardar preferencias
                                prefern.saveOnPreference(username, password, url);
                                //LLamada a la siguente vista
                                Intent i = new Intent(LoginActivity.this, MainActivity.class);
                                //Evita que el usurio puede echar hacia atras una vez se ha logueado
                                i.setFlags(i.FLAG_ACTIVITY_NEW_TASK | i.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(i);

                            } else {
                                //Mostrar el mensaje de error del servidor
                                Toast.makeText(LoginActivity.this, obj.get("Error").toString(), Toast.LENGTH_LONG).show();
                                mensaje.setText(obj.get("Error").toString());
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });
    }
}


