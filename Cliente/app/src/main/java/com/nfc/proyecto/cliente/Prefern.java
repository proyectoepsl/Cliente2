package com.nfc.proyecto.cliente;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.EditText;
import android.widget.Toast;

import org.apache.http.util.TextUtils;

import static android.content.Context.MODE_PRIVATE;


public class Prefern {
    SharedPreferences prefs;
    SharedPreferences prefspin;
    private static Prefern prefern;
    Context context;

    public void setPrefspin(SharedPreferences prefspin){
        this.prefspin = prefspin;
    }

    public void setPrefs (SharedPreferences prefs){
        this.prefs = prefs;
    }

    public void setContext(Context context){
        this.context = context;
    }

    //Creación constructor patrón singleton
    //Solo se crea un objeto de esta clase en toda la aplicación
    public static Prefern getPrefern(){
        if(prefern == null)
            prefern = new Prefern();

        return prefern;
    }

    public void saveOnPreference(EditText username, EditText password, EditText url) {
        //Paso a string los datos recibidos
        String nombre = username.getText().toString();
        String pass = password.getText().toString();
        String ulr = url.getText().toString();
        //Declaro objeto editor para editar las preferencias
        SharedPreferences.Editor editor = prefs.edit();
        //Recogo los datos que voy a guardar
        editor.putString("Username", nombre);
        editor.putString("Password", pass);
        editor.putString("Url", ulr);
        editor.commit();
        editor.apply();
    }

    public boolean savePin(EditText pin) {
        String pin2 = pin.getText().toString();
        if (pin2.equals("")) {
            Toast.makeText(context, "Introduzca Pin",Toast.LENGTH_LONG).show();
            return false;
        } else {

            String pinleido = getVariablePin("Pin").toString();

            if (pinleido.equals(pin2)) {
                borrarSharedPreferences();
                return true;
                //Se guarda nuevo pin
            } else if (TextUtils.isEmpty(pinleido)) {
                borrarSharedPreferences();
                Toast.makeText(context, "Pin Creado", Toast.LENGTH_LONG).show();
                //Borrar la preferencias anteriores
                SharedPreferences.Editor editor = prefspin.edit();
                editor.putString("Pin", pin2);
                editor.commit();
                editor.apply();
                return true;
            } else {
                Toast.makeText(context, "Pin Incorrecto", Toast.LENGTH_LONG).show();
                return false;
            }
        }
    }

    //Metodo para eliminar prefencias compartidas
    public void borrarSharedPreferences(){
        prefs.edit().clear().apply();
    }

    //Metodo para obtener el Pin almacenado en las Preferencias compartidas.
    public String getVariablePin(String var) {
        return prefspin.getString(var, "");
    }

    //Metodo para obtener las variables almecenadas en las Preferencias compartidas.
    public String getVariable(String var) {
        return prefs.getString(var, "");
    }

    //Metodo para guardar las modificaciones en las variables almecenadas en las Preferencias compartidas.
    public void saveEditText(EditText editText, String campo){
        //Paso a string los datos recibidos
        String nombre=editText.getText().toString();
        //Declaro objeto editor para editar las preferencias
        SharedPreferences.Editor editor=prefs.edit();
        //Recogo los datos que voy a guardar
        editor.putString(campo,nombre);
        editor.commit();
        editor.apply();
    }

    /*public void saveUsername(EditText username){
        //Paso a string los datos recibidos
        String nombre=username.getText().toString();
        //Declaro objeto editor para editar las preferencias
        SharedPreferences.Editor editor=prefs.edit();
        //Recogo los datos que voy a guardar
        editor.putString("Username",nombre);
        editor.commit();
        editor.apply();

    }

    public void savePassword(EditText password){
        //Paso a string los datos recibidos
        String pass=password.getText().toString();
        //Declaro objeto editor para editar las preferencias
        SharedPreferences.Editor editor=prefs.edit();
        //Recogo los datos que voy a guardar
        editor.putString("Password",pass);
        editor.commit();
        editor.apply();
    }

    public void saveUrl(EditText url){
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

    public void savePin(EditText pin){
        //Paso a string los datos recibidos
        String pin2=pin.getText().toString();
        //Declaro objeto editor para editar las preferencias
        SharedPreferences.Editor editor=prefspin.edit();
        //Recogo los datos que voy a guardar
        editor.putString("Pin",pin2);
        editor.commit();
        editor.apply();
        mensaje.setText("Pin Modificado");
    }*/
}
