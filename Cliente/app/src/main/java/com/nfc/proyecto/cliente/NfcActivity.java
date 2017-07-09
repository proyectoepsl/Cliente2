package com.nfc.proyecto.cliente;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import android.content.IntentFilter.MalformedMimeTypeException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static android.nfc.NdefRecord.createMime;


public class NfcActivity extends AppCompatActivity {
    public static final String MIME_TEXT_PLAIN = "text/plain";
    public static final String TAG = "NfcDemo";
    private static String APP_TAG = "MyNFCBasic";

    TextView mensaje = null;
    private NfcAdapter mNfcAdapter;
    String datosNfc = null;
    String Sala_id;
    String Dependencia;
    String Plano;
    ImageView imgViewer;

    String resultado;
    JSONObject obj;

    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(APP_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        mensaje = (TextView) findViewById(R.id.nfc);
        imgViewer = (ImageView) findViewById(R.id.imageView);


        //Bundle datos = this.getIntent().getExtras();

        Sala_id = getIntent().getStringExtra("Sala_id");
        Dependencia = "";//datos.getString("Dependencia");
        Plano = "";//datos.getString("Plano");




        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "Este dispositivo no soporta NFC.", Toast.LENGTH_LONG).show();
            //finish();
            return;

        }

        if (!mNfcAdapter.isEnabled()) {
            mensaje.setText("NFC Esta deshabilitado");
        } else {
            mensaje.setText("Acerque su tarjeta NFC");
        }

        handleIntent(getIntent());


    }

    @TargetApi(Build.VERSION_CODES.M)
    public void mostrarPlano(){
        NfcActivity.this.runOnUiThread(new Runnable() {
            public void run () {
                //Do something on UiThread
                try {
                    byte[] byteArray = "".getBytes();

                    //JSONObject obj = new JSONObject(respStr);
                    byteArray = Plano.getBytes(StandardCharsets.US_ASCII);;
                    byteArray =  Base64.decode(byteArray, Base64.DEFAULT);

                    Bitmap bitmap = BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
                    imgViewer.setImageBitmap(bitmap);

                    mensaje.setText("Plano Emergencia Sala Nº " + Dependencia);

                } catch (Throwable t) {
                    Log.e("My App", "Could not parse malformed JSON: \"");
                }
            }
        });
    }

    public void cerrar(View view) {
        finish();
    }

    public void onResume() {
        super.onResume();
        /**
         * It's important, that the activity is in the foreground (resumed). Otherwise
         * an IllegalStateException is thrown.
         */
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
    protected void onNewIntent(Intent intent) {
        /**
         * This method gets called, when a new Intent gets associated with the current activity instance.
         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
         * at the documentation.
         *
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            String type = intent.getType();
            if (MIME_TEXT_PLAIN.equals(type)) {

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);

            } else {
                Log.d(TAG, "Wrong mime type: " + type);
            }
        } else if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)) {

            // In case we would still use the Tech Discovered Intent
            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            String[] techList = tag.getTechList();
            String searchedTech = Ndef.class.getName();

            for (String tech : techList) {
                if (searchedTech.equals(tech)) {
                    new NdefReaderTask().execute(tag);
                    break;
                }
            }
        }
    }

    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
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
            throw new RuntimeException("Check your mime type.");
        }

        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }

    /**
     * @param activity The corresponding {@link //BaseActivity} requesting to stop the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

    /**
     * Background task for reading the data. Do not block the UI thread while reading.
     *
     * @author Ralf Wondratschek
     *
     */
    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {

        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];

            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                // NDEF is not supported by this Tag.
                return null;
            }

            NdefMessage ndefMessage = ndef.getCachedNdefMessage();

            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                    try {
                        //obtengo los datos del nfc
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, "Unsupported Encoding", e);
                    }
                }
            }

            return null;
        }

        private String readText(NdefRecord record) throws UnsupportedEncodingException {
        /*
         * See NFC forum specification for "Text Record Type Definition" at 3.2.1
         *
         * http://www.nfc-forum.org/specs/
         *
         * bit_7 defines encoding
         * bit_6 reserved for future use, must be 0
         * bit_5..0 length of IANA language code
         */

            byte[] payload = record.getPayload();

            // Get the Text Encoding
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";

            // Get the Language Code
            int languageCodeLength = payload[0] & 0063;

            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"

            // Get the Text
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                //mensaje.setText("Read content: " + result);
                datosNfc=result;
                //result=result.replace("\n","").split("\r");
                //ciframos el dni
                new Thread(new Runnable(){
                    @Override
                    public void run() {
                        try {
                            //Cifrar imei
                            Crypt aesCrypt = new Crypt();
                            String a = aesCrypt.encrypt_string(Sala_id);
                            String b = aesCrypt.encrypt_string(datosNfc.replace("\n","").split("\r")[2]);
                            // Your implementation
                            HttpClient httpClient = new DefaultHttpClient();


                            //Llamamos al rest con los datos cifrados
                            // HttpPost post = new HttpPost("https://proyectoepsl.pythonanywhere.com/rest_sala");
                            HttpPost post = new HttpPost("http://192.168.2.129:8000/rest_usuario/");
                            post.setHeader("Content-Type","application/json");
                            post.setHeader("charset","utf-8");
                            //Construimos el objeto cliente en formato JSON
                            JSONObject dato = new JSONObject();


                            dato.put("Sala_id", a);
                            dato.put("Dni",b);

                            Log.i("JSON", dato.toString());

                            StringEntity entity = new StringEntity(dato.toString());
                            post.setEntity(entity);

                            //Realizo el envío
                            HttpResponse resp = httpClient.execute(post);

                            String respStr = EntityUtils.toString(resp.getEntity());
                            obj = new JSONObject(respStr);
                            resultado = obj.get("result").toString();
                            if (resultado.equals("200")) {
                                NfcActivity.this.runOnUiThread(new Runnable() {
                                    public void run () {
                                        //Do something on UiThread
                                        try {
                                            imgViewer.setImageDrawable(getResources().getDrawable(R.drawable.ok));
                                            mensaje.setText("Bienvenido");
                                        } catch (Throwable t) {
                                            t.printStackTrace();
                                        }
                                    }
                                });
                            }
                            else{
                                NfcActivity.this.runOnUiThread(new Runnable() {
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
                            Thread.sleep (5000);
                            finish();
                            return;
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }).start();
            }
        }
    }
}
