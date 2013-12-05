package com.gorka.rssjarioa;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;

public class Menua extends Activity {

	DbEgokitua db=new DbEgokitua(this);
    boolean hariaEginda = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.menua);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitNetwork().build();
        StrictMode.setThreadPolicy(policy);

        if(networkAvailable()) {
            // Badago Interneta
                Log.d("INTERNET", "Badago");
                haria();
                bertsioaBegitu();
        }else{
            // Ez dago internetik
                networkNoAvailableDialog();
                Log.d("INTERNET", "EZ dago");
            }
        LinearLayout info = (LinearLayout) findViewById(R.id.menua_info);
        info.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Menua.this.openOptionsMenu();
            }
        });


    }

    public void onclickbtnberriak(@SuppressWarnings("UnusedParameters")View view){
        if (networkAvailable()) {
            startActivity(new Intent("berriak"));
        }else{
            Toast.makeText(Menua.this,"EZ zaude internetari konektatuta",Toast.LENGTH_LONG).show();
        }
    }
	    
    public void onclickbtnagenda(@SuppressWarnings("UnusedParameters")View view){
        if(hariaEginda || !networkAvailable()){
            startActivity(new Intent("agenda"));
        }else {
            Toast.makeText(this,"zerbitzariarekin konektatzen",Toast.LENGTH_LONG).show();
        }
    }

    public void onclickbtnelkarteak(@SuppressWarnings("UnusedParameters")View view){
            startActivity(new Intent("elkarteak"));
    }

    public void onclickbtnzerbitzuak(@SuppressWarnings("UnusedParameters")View view){
            startActivity(new Intent("zerbitzuak"));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_menua, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_hobespenak:
                startActivity(new Intent("hobespenak"));
                return true;
            case R.id.menu_kontaktua:
                startActivity(new Intent("kontaktua"));
                return true;
            case R.id.menu_nortzuk:
                startActivity(new Intent("eskura"));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void haria(){
        new Thread(new Runnable(){
            @Override
            public void run() {
                Log.e("hilo1", "on");
                final Calendar c = Calendar.getInstance();
                int mYear = c.get(Calendar.YEAR);
                int mMonth = c.get(Calendar.MONTH)+1;   //urtarrila=0
                int mDay = c.get(Calendar.DAY_OF_MONTH);
                int mhour = c.get(Calendar.HOUR_OF_DAY);
                int numdbekitaldi = 0;
                int numwebekitaldi = 0;

                Log.i("gaurko data", "" + mYear + "-" + String.format("%02d",mMonth) + "-" + String.format("%02d",mDay)  +" "+String.format("%02d",mhour));
                db.zabaldu();
                try{
                    numwebekitaldi = db.eguneratuEkintzak();
                    Log.i("numwebekitaldi",numwebekitaldi+"");
                    }catch (Exception e){
                    Log.e("eguneratu",e.toString());
                }
                try{
                    db.garbitu(mYear, String.format("%02d",mMonth),String.format("%02d",mDay), String.format("%02d",mhour));
                    }catch (Exception e){
                    Log.e("garbitu",e.toString());
                }
                try{
                    numdbekitaldi = db.ekitaldikzenbat();
                    Log.i("numdbekitaldi",numdbekitaldi+"");
                }catch (Exception e){
                    Log.e("ekitaldikzenbat",e.toString());
                }
                if(numwebekitaldi<numdbekitaldi && numwebekitaldi!=0){
                    try {
                        Log.e("berAktualizatu","");
                        db.ekitaldiguztiakkendu();
                        db.eguneratuEkintzak();
                        db.garbitu(mYear, String.format("%02d",mMonth),String.format("%02d",mDay), String.format("%02d",mhour));
                    }catch (Exception e){
                        Log.e("ekitaldiguztiakkendu",e.toString());
                    }
                }
                try {
                    int id= db.azkenId();
                    Log.d("azkenID",id+"");
                    }catch (Exception e){
                    Log.e("azkenID",e.toString());
                }
                db.zarratu();
                Log.e("hilo1", "off");
                hariaEginda = true;
            }

        }).start();
    }

    private void bertsioaBegitu(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean bertsiozaharra = false;
                try {
                    URL url = new URL("http://37.139.15.79/Bertsioa/");
                    URLConnection uc = url.openConnection();
                    uc.connect();
                    BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
                    String inputLine=in.readLine();
                    if(inputLine != null ) {
                        int webversionCode = 0;
                        try{
                            webversionCode = Integer.parseInt(inputLine);
                        }catch (Exception ex){
                            Log.e("Menua-bertsiobegitu",ex.toString());
                        }
                        if(webversionCode>(getPackageManager().getPackageInfo(getPackageName(), 0).versionCode)){
                            Log.e("bertsio","ezberdinak");
                            bertsiozaharra = true;
                        }
                    }
                    in.close();
                }catch (FileNotFoundException e){
                    Log.e("Menua-bertsioaBegitu","ezin da serbitzariarekin konektatu");
                } catch (Exception e) {
                    Log.e("Menua-bertsioaBegitu", e.toString());
                }
                Message msg = bertsioHandler.obtainMessage();
                msg.obj = bertsiozaharra;
                bertsioHandler.sendMessage(msg);
            }
        }).start();
    }

    private final Handler bertsioHandler = new Handler() {
        public void handleMessage(Message msg) {
            if((Boolean)msg.obj){
                bertsioaEguneratu();
            }
        }
    };

    public boolean networkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }else{
            Log.d("INTERNET", "EZ dago internetik");
        }
        return false;
    }

    public void networkNoAvailableDialog(){
        AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
        alertbox.setIcon(R.drawable.warning);
        alertbox.setTitle("EZ zaude internetari konektatuta");
        alertbox.setPositiveButton("Bale", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {}
        });
        alertbox.show();
    }

    private void bertsioaEguneratu(){
        AlertDialog.Builder alertbox = new AlertDialog.Builder(this);
        alertbox.setTitle("Aplikazioan eguneraketa bat dago");
        alertbox.setMessage("Eguneratu nahi dozu?");
        alertbox.setPositiveButton("Bai", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
                final String appName = "com.gorka.rssjarioa";
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appName)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id="+appName)));
                }
                finish();
            }
        });
        alertbox.setNegativeButton("Ez", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {
            }
        });
        alertbox.show();
    }

    @Override
    public void onStart() {
        super.onStart();
        // The rest of your onStart() code.
        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        // The rest of your onStop() code.
        EasyTracker.getInstance(this).activityStop(this);
    }
}
