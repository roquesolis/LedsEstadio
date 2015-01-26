package api.ledsestadio;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.json.JSONObject;


import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


public class leds extends ActionBarActivity {

    private static final String WAKE_LOCK_TAG = "Linterna";

    private static final int NOTIFICATION_ID = 1;

    private Torch torch;

    private PowerManager.WakeLock wakeLock;

    private NotificationManager notificationManager;
    TextView lblHeader;
    private boolean conectado=false;
    int accionActual =0;
    boolean encendico;

    TextView txtUbicacion;
    TextView txtLongitud;
    TextView txtLatitud;
    TextView txtAltitud;
    TextView txtIp;

    Timer timer;
    Timer timerParpadeo;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leds);

        //final Button btnEncender = (Button)findViewById(R.id.btnEncender);
        //final Button btnApagar = (Button)findViewById(R.id.btnApagar);
        final Button btnConectar = (Button) findViewById(R.id.btnConectar);
        lblHeader = (TextView) findViewById(R.id.textView);

        timer = new Timer();
        timer.schedule(timerTask, 0, 1000);

        timerParpadeo = new Timer();
        timer.schedule(timerIntervalo, 0, 500);
        encendico = false;

        txtUbicacion = (TextView) findViewById(R.id.txtUbicacion);
        txtLongitud = (TextView) findViewById(R.id.txtLongitud);
        txtLatitud = (TextView) findViewById(R.id.txtLatitud);
        txtAltitud = (TextView) findViewById(R.id.txtAltitud);
        txtIp = (TextView) findViewById(R.id.txtIp);

        btnConectar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                try{
                    if(!conectado) {
                        // Dentro de 0 milisegundos avísame cada 1000 milisegundos
                        btnConectar.setText("Desconectar");
                    }
                    else
                    {
                        torch.off();
                        accionActual = 0;
                        btnConectar.setText("Conectar");
                    }
                    conectado = !conectado;
                }
                catch (Exception ex){
                    ex.printStackTrace();
                    conectado = false;
                }

            }
        });

        // Acceder a la camara.
        if (!initTorch())
        {
            return;
        }

        LocationManager mlocManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Localizacion mlocListener = new Localizacion();
        mlocListener.setMainActivity(this);
        mlocManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0,
                (LocationListener) mlocListener);

        txtUbicacion.setText("LocationListener agregado");
        txtLatitud.setText("");
        txtLongitud.setText("");

        txtIp.setText(Utils.getIPAddress(true));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_leds, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean initTorch()
    {
        try
        {
            // Acceder a la c�mara.
            torch = new Torch();
        }
        catch (Exception e)
        {
            // Mostrar mensaje de error al usuario.
            Toast.makeText(this,
                    getResources().getString(R.string.text_error),
                    Toast.LENGTH_LONG).show();
            // Salir de la aplicaci�n.
            finish();

            return false;
        }

        return true;
    }

    private void createNotification()
    {
        Intent intent = new Intent(this, leds.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText(getResources().getString(R.string.notification_text))
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();

        notificationManager.notify(NOTIFICATION_ID, notification);
    }


    TimerTask timerTask = new TimerTask()
    {
        public void run()
        {
            if(conectado) {
                conectarServicio();
            }
        }
    };

    TimerTask timerIntervalo = new TimerTask()
    {
        public void run()
        {
            if(accionActual==3 && conectado) {
                if(torch.isOn()){
                    torch.off();
                }
                else{
                    torch.on();
                }
            }
        }
    };

    private void conectarServicio(){

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {

                HttpClient httpClient = new DefaultHttpClient();
                HttpContext localContext = new BasicHttpContext();


                HttpGet httpGet = new HttpGet("http://follower.apinterfaces.mx/WebApplication1/webresources/leds.accionled");
                String text = null;
                try {
                    HttpResponse response = httpClient.execute(httpGet, localContext);


                    HttpEntity entity = response.getEntity();


                    text = getASCIIContentFromEntity(entity);

                    JSONObject obj = new JSONObject(text);
                    JSONObject nodo = obj.getJSONObject("accionLed");
                    int accion = nodo.getInt("accion");

                    if(accionActual != accion){
                        if(accion==1 && conectado){
                            torch.on();
                            encendico=true;
                        }
                        else{
                            if(accion==0) {
                                torch.off();
                                encendico = false;
                            }
                        }
                        accionActual = accion;
                    }

                } catch (Exception e) {

                    e.printStackTrace();
                }

            }
        });

        thread.start();


    }

    protected String getASCIIContentFromEntity(HttpEntity entity) throws IllegalStateException, IOException {
        InputStream in = entity.getContent();


        StringBuffer out = new StringBuffer();
        int n = 1;
        while (n>0) {
            byte[] b = new byte[4096];
            n =  in.read(b);


            if (n>0) out.append(new String(b, 0, n));
        }


        return out.toString();
    }

    public void setLocation(Location loc) {
        //Obtener la direcci—n de la calle a partir de la latitud y la longitud
        if (loc.getLatitude() != 0.0 && loc.getLongitude() != 0.0) {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> list = geocoder.getFromLocation(
                        loc.getLatitude(), loc.getLongitude(), 1);
                if (!list.isEmpty()) {
                    Address address = list.get(0);
                    txtUbicacion.setText("Mi direcci—n es: \n"
                            + address.getAddressLine(0));
                }

                txtLongitud.setText("Longitud: " + loc.getLongitude());
                txtLatitud.setText("Latitud: "+ loc.getLatitude());
                txtAltitud.setText("Altitud: "+ loc.getAltitude());


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
