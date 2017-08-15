package com.nfc.proyecto.cliente;

import android.content.Context;
import android.content.Entity;
import android.os.AsyncTask;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.io.ContentLengthInputStream;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Created by sony on 03/08/2017.
 */

public class Http extends AsyncTask<String, Integer, Void>{

    String resultado;
    JSONObject obj;
    HttpEntity entity;
    String url;
    String rest;
    String respStr;
    Context context;
    private static Http http;

    //Creación constructor patrón singleton
    //Solo se crea un objeto de esta clase en toda la aplicación
    public static Http getHttp(){
        if(http == null)
            http = new Http();

        return http;
    }

    /**
     *
     * @param url
     */
    public void setUrl(String url){
        this.url = url;
    }

    /**
     *
      * @param entity
     */
    public void setEntity(HttpEntity entity){
        this.entity = entity;
    }

    /**
     *
     * @param rest
     */
    public void setRest(String rest){ this.rest = rest; }

    /**
     *
     * @return
     */
    public JSONObject getResponse(){ return this.obj; }

    @Override
    protected Void doInBackground(String... params) {
        postData();
        return null;
    }

    public void postData(){
        try {
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://" + url + rest);
            httppost.setHeader("Content-Type", "application/json");
            httppost.setHeader("charset", "utf-8");
            httppost.setEntity(entity);
            //Realizo el envío
            HttpResponse resp = null;
            resp = httpClient.execute(httppost);
            //3º:Procesar la respuesta del servidor
            //Obtengo respuesta del servidor
            respStr = EntityUtils.toString(resp.getEntity());
            //Creo un objeto Json con esta respuesta para poder acceder a los datos
            if(respStr != null)
                obj = new JSONObject(respStr);
        } catch (IOException e) {
            try {
                obj = new JSONObject();
                obj.put("result", "500");
                obj.put("Error", "Petición Incorrecta");
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        } catch (JSONException e) {
            try {
                obj = new JSONObject();
                obj.put("result", "500");
                obj.put("Error", "Conexión Rechazada");
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        } catch (Exception e){
            try {
                obj = new JSONObject();
                obj.put("result", "500");
                obj.put("Error", "Error Interno");
            } catch (JSONException e1) {
                e1.printStackTrace();
            }
        }
    }
}
