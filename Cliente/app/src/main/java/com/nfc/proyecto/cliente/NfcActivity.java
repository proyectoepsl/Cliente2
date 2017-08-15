package com.nfc.proyecto.cliente;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import org.apache.http.entity.StringEntity;
import org.json.JSONObject;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class NfcActivity extends AppCompatActivity {
    //Defino tipo de formato de la tarjeta
    public static final String MIME_TEXT_PLAIN = "text/plain";
    //Variable para nombrar log
    public static final String TAG = "NfcProyecto";

    TextView mensaje = null;
    private NfcAdapter mNfcAdapter;
    String datosNfc = null;
    String Sala_id;
    String Dependencia;
    String Plano;
    ImageView imgViewer;

    String resultado;
    JSONObject obj;

    //Llamada al metodo NFC cuando pulso el boton
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mensaje = (TextView) findViewById(R.id.nfc);
        imgViewer = (ImageView) findViewById(R.id.imageView);

        //Recepcion de datos de mainActiviti
        Sala_id = getIntent().getStringExtra("Sala_id");

        //1º Paso :Compruebo si el terminal tiene NFC
        if (mNfcAdapter == null) {
            Toast.makeText(this, "Este dispositivo no soporta NFC.", Toast.LENGTH_LONG).show();
            return;

        }
        //2º Paso:Compruebo si el NFC esta activado en el telefono
        if (!mNfcAdapter.isEnabled()) {
            mensaje.setText("NFC Esta deshabilitado");
        } else {
            mensaje.setText("Acerque su tarjeta NFC");
        }
        //3º Lanzo el intent de NFC
        handleIntent(getIntent());
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        //Mirar si tiene permiso ACTION_NDEF_DISCOVERED en manifest
        //ACTION_NDEF_DISCOVERED:permiso de iniciar una actividad cuando se descubre una etiqueta tipo NDEF.
        //Se mira si la tarjeta es NDEF
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            String type = intent.getType();
            //Comprueba que el formato de mime es correcto
            if (MIME_TEXT_PLAIN.equals(type)) {
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                //4º PASO:LLamada al metodo que lee la tarjeta
                new NdefReaderTask().execute(tag);

                if(datosNfc != null) {
                    llamadaRest();
                    datosNfc = null;
                }
            } else {
                Log.d(TAG, "Tipo de mime incorrecto: " + type);
            }
            //ACTION_TECH_DISCOVERED:Ipermiso de iniciar una actividad cuando se descubre una etiqueta y se registran actividades para las tecnologías específicas de la etiqueta.
            //Si la tarjeta no es NDEF y se intenta descubrir que tecnologia es
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();

            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                    //4º PASO:LLamada al metodo que lee la tarjeta
                    new NdefReaderTask().execute(tag);
                    if(datosNfc != null){
                        llamadaRest();
                        datosNfc=null;
                    }
                    break;
                }
            }
        }
    }


    public void cerrar(View view) {
        finish();
    }

    //Es necesario que la actividad se desarrolle en segundo plano o se producirá una excepción
    public void onResume() {
        super.onResume();

        setupForegroundDispatch(this, mNfcAdapter);
    }

    @Override
    protected void onPause() {
        /**
         * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
         */
        stopForegroundDispatch(this, mNfcAdapter);
        super.onPause();
    }

    @Override
    //Metodo llamado cuando se produce un nuevo inten, es decir,cuando el cliente acerca una tag al dispositivo
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);

        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};

        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("Verifica tu tipo de mime");
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }


    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

    public void llamadaRest(){
        try {
            Crypt aesCrypt = Crypt.getInstance();
            String a = aesCrypt.encrypt_string(Sala_id);
            String b = aesCrypt.encrypt_string(datosNfc.replace("\n","").split("\r")[2]);
            // Your implementation

            Prefern prefern = Prefern.getPrefern();
            //Llamamos al rest con los datos cifrados
            String url = prefern.getVariable("Url");

            //Construimos el objeto cliente en formato JSON
            JSONObject dato = new JSONObject();
            //Datos a enviar el id de sala y el numero de DNI
            dato.put("Sala_id", a);
            dato.put("Dni",b);

            StringEntity entity = new StringEntity(dato.toString());

            Http http = Http.getHttp();
            http = Http.getHttp();
            http.setEntity(entity);
            http.setUrl(url);
            http.setRest("/rest_usuario/");
            http.doInBackground();
            obj = http.getResponse();

            resultado = obj.get("result").toString();
            if (resultado.equals("200")) {
                //Si el usuario puede entrar a la sala le muestra el mensaje de bienvenida
                try {
                    imgViewer.setImageDrawable(getResources().getDrawable(R.drawable.ok_usuario));
                    mensaje.setText(obj.get("Error").toString());
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            else{
                //Si el usuario no puede entrar en la sala se muestra el mensaje de error.
                try {
                    imgViewer.setImageDrawable(getResources().getDrawable(R.drawable.error_usuario));
                    mensaje.setText(obj.get("Error").toString());

                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            //Se espere 5 seg y vuelva a la pantalla anterior
            new Thread(new Runnable(){
                @Override
                public void run() {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    finish();
                    return;
                }
            }).start();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    //4º Paso Leer los datos de la tarjeta
    //El lector NFC se lleva acabo en una tarea asincrona.Las cuales se definen como una subclase privada
    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {
        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];
            //Peticion datos tarjetas por NDEF
            Ndef ndef = Ndef.get(tag);
            // Esta tarjeta no soporta NDEF
            if (ndef == null) {
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            //Obtencion de los datos de la tarjeta
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        //5º PASO: llamar a la funcion que formatea los datos de la tarjeta
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Codificacion incompatible", e);
                    }
                }
            }

            return null;
        }
        //5º PASO:  formatea los datos de la tarjeta
        private String readText(NdefRecord record) throws UnsupportedEncodingException {

            byte[] payload = record.getPayload();

            // Obtiene el tipo de codificacion
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

            // Obtinen el codigo del lenguaje
            int languageCodeLength = payload[0] & 0063;

            // Obtiene el texto
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }

        @Override
        //6º PASO
        protected void onPostExecute(String result) {
            if (result != null) {
                //Si se obtienen datos del nfc.

                datosNfc=result;
                //Hacemos peticion POST al servidor con el DNI del usurio
                /*new Thread(new Runnable(){
                    @Override
                    public void run() {
                        try {
                            Crypt aesCrypt = Crypt.getInstance();
                            String a = aesCrypt.encrypt_string(Sala_id);
                            String b = aesCrypt.encrypt_string(datosNfc.replace("\n","").split("\r")[2]);
                            // Your implementation
                            HttpClient httpClient = new DefaultHttpClient();

                            Prefern prefern = Prefern.getPrefern();
                            //Llamamos al rest con los datos cifrados
                            // HttpPost post = new HttpPost("https://proyectoepsl.pythonanywhere.com/rest_sala");
                            String url = prefern.getVariable("Url");
                            HttpPost post = new HttpPost(url);
                            post.setHeader("Content-Type","application/json");
                            post.setHeader("charset","utf-8");
                            //Construimos el objeto cliente en formato JSON
                            JSONObject dato = new JSONObject();

                            //Datos a enviar el id de sala y el numero de DNI
                            dato.put("Sala_id", a);
                            dato.put("Dni",b);

                            Log.i("JSON", dato.toString());
                            ///Realizo peticion post
                            StringEntity entity = new StringEntity(dato.toString());
                            post.setEntity(entity);

                            //Realizo el envío
                            HttpResponse resp = httpClient.execute(post);
                            //Recibo respuesta del servidor.
                            String respStr = EntityUtils.toString(resp.getEntity());
                            obj = new JSONObject(respStr);
                            resultado = obj.get("result").toString();
                            if (resultado.equals("200")) {
                                NfcActivity.this.runOnUiThread(new Runnable() {
                                    public void run () {
                                        //Si el usuario puede entrar a la sala le muestra el mensaje de bienvenida
                                        try {
                                            imgViewer.setImageDrawable(getResources().getDrawable(R.drawable.ok_usuario));
                                            mensaje.setText(obj.get("Error").toString());
                                        } catch (Throwable t) {
                                            t.printStackTrace();
                                        }
                                    }
                                });
                            }
                            else{
                                NfcActivity.this.runOnUiThread(new Runnable() {
                                    public void run () {
                                        //Si el usuario no puede entrar en la sala se muestra el mensaje de error.
                                        try {
                                            imgViewer.setImageDrawable(getResources().getDrawable(R.drawable.error_usuario));
                                            mensaje.setText(obj.get("Error").toString());

                                        } catch (Throwable t) {
                                            t.printStackTrace();
                                        }
                                    }
                                });

                            }
                            //Se espere 5 seg y vuelva a la pantalla anterior
                            Thread.sleep (5000);
                            finish();
                            return;
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }).start();*/
            }
        }
    }
}
