package com.nfc.proyecto.cliente;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;

public class UserActivity extends AppCompatActivity {
    EditText url;
    EditText username;
    EditText password;
    EditText pin;
    TextView mensaje;
    String respStr;
    String resultado;
    String item;
    JSONObject obj;
    Prefern prefern;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        final Button Guardar = (Button) findViewById(R.id.user_modificar);
        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);
        url = (EditText) findViewById(R.id.url);
        pin = (EditText) findViewById(R.id.pin);
        mensaje = (TextView) findViewById(R.id.Mensaje);

        //Ocultar todos los EditText
        username.setVisibility(View.GONE);
        password.setVisibility(View.GONE);
        url.setVisibility(View.GONE);
        pin.setVisibility(View.GONE);
        item = getIntent().getStringExtra("item");

        prefern = Prefern.getPrefern();

        if (item.equals("Modificar Usuario")) {
            username.setText(prefern.getVariable("Username"));
            username.setVisibility(View.VISIBLE);
            mensaje.setText("Modificar el nombre del Usuario");

        } else if (item.equals("Modificar Password")) {
            password.setText(prefern.getVariable("Password"));
            password.setVisibility(View.VISIBLE);
            mensaje.setText("Modificar la Contraseña");


        } else if (item.equals("Modificar Url")) {
            url.setText(prefern.getVariable("Url"));
            url.setVisibility(View.VISIBLE);
            mensaje.setText("Modificar la Url del Servidor");

        } else if (item.equals("Modificar Pin")) {
            pin.setText(prefern.getVariablePin("Pin"));
            pin.setVisibility(View.VISIBLE);
            mensaje.setText("Modificar el Pin de la Aplicacion");
        }

        Guardar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

                if (item.equals("Modificar Usuario")) {
                    llamadaRest1();
                } else if (item.equals("Modificar Password")) {
                    llamadaRest();

                } else if (item.equals("Modificar Url")) {
                    prefern.saveEditText(url, "Url");
                    Toast.makeText(UserActivity.this, "Url Modificada", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                } else if (item.equals("Modificar Pin")) {
                    prefern.saveEditText(pin, "Pin");
                    Toast.makeText(UserActivity.this, "Pin Modificado", Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
            }
        });
    }

    private void llamadaRest1() {
        try {
            //1º:Encritacion de los datos
            //LLanada a la funcion de encriptar
            Crypt aesCrypt = Crypt.getInstance();
            String usernamePref = prefern.getVariable("Username");
            String a = aesCrypt.encrypt_string(usernamePref);
            String b = aesCrypt.encrypt_string(username.getText().toString());
            String url = prefern.getVariable("Url");
            //2º:Envio de datos por HTTP
            JSONObject dato = new JSONObject();
            dato.put("usernamepref", a);
            dato.put("username", b);
            //Creo entidad para enviar los datos
            StringEntity entity = new StringEntity(dato.toString());

            Http http = Http.getHttp();
            http = Http.getHttp();
            http.setEntity(entity);
            http.setUrl(url);
            http.setRest("/rest_modUsuario/");
            http.doInBackground();
            obj = http.getResponse();

            //4º:Segun la respuesta del servidor llevo a cabo las acciones.
            //Obtengo el objeto resultado de la respuesta
            resultado = obj.get("result").toString();
            //Si la accion se ha realizado sin problemas
            if (resultado.equals("200")) {
                prefern.saveEditText(username, "Username");
                //mensaje.setText(obj.get("Error").toString());
                Toast.makeText(UserActivity.this, obj.get("Error").toString(), Toast.LENGTH_LONG).show();

            } else {
                //mensaje.setText(obj.get("Error").toString());
                Toast.makeText(UserActivity.this, obj.get("Error").toString(), Toast.LENGTH_LONG).show();
            }
            //Se espere 5 seg y vuelva a la pantalla anterior
            finish();
            return;

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void llamadaRest() {
        try {
            //1º:Encritacion de los datos
            //LLamada a la funcion de encriptar
            Crypt aesCrypt = Crypt.getInstance();
            String usernamePref = prefern.getVariable("Username");
            String a = aesCrypt.encrypt_string(usernamePref);
            String b = aesCrypt.encrypt_string(password.getText().toString());

            //2º:Envio de datos por HTTP
            String url = prefern.getVariable("Url");

            //Construimos el objeto en formato JSON
            JSONObject dato = new JSONObject();
            dato.put("username", a);
            dato.put("password", b);
            //Creo entidad para enviar los datos
            StringEntity entity = new StringEntity(dato.toString());

            Http http = Http.getHttp();
            http = Http.getHttp();
            http.setEntity(entity);
            http.setUrl(url);
            http.setRest("/rest_modPassword/");
            http.doInBackground();
            obj = http.getResponse();


            //4º:Segun la respuesta del servidor llevo a cabo las acciones.
            //Obtengo el objeto resultado de la respuesta
            resultado = obj.get("result").toString();
            //Si la accion se ha realizado sin problemas
            if (resultado.equals("200")) {
                prefern.saveEditText(password, "Password");
                Toast.makeText(UserActivity.this, obj.get("Error").toString(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(UserActivity.this, obj.get("Error").toString(), Toast.LENGTH_LONG).show();

            }
            //Se espere 5 seg y vuelva a la pantalla anterior
            finish();
            return;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

 /*private String getUsername(){
        return prefs.getString("Username","");

    }
    private String getPassword(){
        return prefs.getString("Password","");

    }
    private String getUrl(){
        return prefs.getString("Url","");

    }
    private String getPin(){
        return prefspin.getString("Pin","");

    }
    private void saveUsername(EditText username){
        //Paso a string los datos recibidos
        String nombre=username.getText().toString();
        //Declaro objeto editor para editar las preferencias
        SharedPreferences.Editor editor=prefs.edit();
        //Recogo los datos que voy a guardar
        editor.putString("Username",nombre);
        editor.commit();
        editor.apply();

    }
    private void savePassword(EditText password){
        //Paso a string los datos recibidos
        String pass=password.getText().toString();
        //Declaro objeto editor para editar las preferencias
        SharedPreferences.Editor editor=prefs.edit();
        //Recogo los datos que voy a guardar
        editor.putString("Password",pass);
        editor.commit();
        editor.apply();

    }
    private void saveUrl(EditText url){
        //Paso a string los datos recibidos
        String ulr=url.getText().toString();
        //Declaro objeto editor para editar las preferencias
        SharedPreferences.Editor editor=prefs.edit();
        //Recogo los datos que voy a guardar
        editor.putString("Url",ulr);
        editor.commit();
        editor.apply();
        mensaje.setText("Url Modificada");



    }
    private void savePin(EditText pin){
        //Paso a string los datos recibidos
        String pin2=pin.getText().toString();
        //Declaro objeto editor para editar las preferencias
        SharedPreferences.Editor editor=prefspin.edit();
        //Recogo los datos que voy a guardar
        editor.putString("Pin",pin2);
        editor.commit();
        editor.apply();
        mensaje.setText("Pin Modificado");

    }
    */
