package com.hetnet.milocsample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.eclipse.paho.client.mqttv3.util.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import static android.R.id.message;



public class connection extends AppCompatActivity {

    private EditText e1, e2, e3, e4;
    private TextView textView;
   // private static connection instance = new connection();
    private ArrayList<String> arrayList;
    private ArrayAdapter<String> adapter;

    MqttAndroidClient client;
    MqttConnectOptions options;


  //  public static connection getInstance(){
  //      return instance;
  //  }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        e1 = (EditText) findViewById(R.id.editText1);
        e2 = (EditText) findViewById(R.id.editText2);
       // e3 = (EditText) findViewById(R.id.editText4);
        e3 = (EditText) findViewById(R.id.editText3);
        final String Topic = e3.getText().toString();


        String MQTTIP = e1.getText().toString();
      //  String MQTTPort = e3.getText().toString();
        String MQTTHOST = "tcp://"+MQTTIP+"1883";
        String clientId = e2.getText().toString();

        client = new MqttAndroidClient(this.getApplicationContext(), MQTTHOST,
                clientId);

        options = new MqttConnectOptions();

    }


    public void pub(View v) {

        e1 = (EditText) findViewById(R.id.editText1);
        e2 = (EditText) findViewById(R.id.editText2);
        // e3 = (EditText) findViewById(R.id.editText4);

        String MQTTIP = e1.getText().toString();
        //  String MQTTPort = e3.getText().toString();
        String MQTTHOST = "tcp://"+MQTTIP+":1883";
        String clientId = e2.getText().toString();

        client = new MqttAndroidClient(this.getApplicationContext(), MQTTHOST,
                clientId);

        options = new MqttConnectOptions();
        options.getServerURIs();
        options.setCleanSession(true);
        options.setKeepAliveInterval(200000);

        try {
            IMqttToken token = client.connect(options);
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    textView = (TextView) findViewById(R.id.textView);
                    textView.setText("connected");
                    textView.setBackgroundResource(R.color.colorConnected);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    textView = (TextView) findViewById(R.id.textView);
                    textView.setText("Not connected");
                    textView.setBackgroundResource(R.color.colorDisconnected);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void pub1(View v) {

        try {
            IMqttToken token = client.disconnect();
            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    textView = (TextView) findViewById(R.id.textView);
                    textView.setText("Disconnected");
                    textView.setBackgroundResource(R.color.colorDisconnected);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    textView = (TextView) findViewById(R.id.textView);
                    textView.setText("Disconnected");
                    textView.setBackgroundResource(R.color.colorDisconnected);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void sub1(View v){
        e3 = (EditText) findViewById(R.id.editText3);
        final String topic = e3.getText().toString();
        try{

            final CountDownLatch latch = new CountDownLatch(1);
            client.setCallback(new MqttCallback() {

                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.println(topic+","+ new String(message.getPayload()));
                    latch.countDown(); // unblock main thread
                }

                public void connectionLost(Throwable cause) {
                    System.out.println("Connection to broker lost!" + cause.getMessage());
                    latch.countDown();
                }

                public void deliveryComplete(IMqttDeliveryToken token) {
                }

            });
            client.subscribe(topic, 0);

        }catch(MqttException e){
            e.printStackTrace();
        }
    }


    public void sub2(View v){
        e3 = (EditText) findViewById(R.id.editText3);
        final String Topic = e3.getText().toString();
        try{
            client.unsubscribe(Topic);
            e3.setText("");
        }catch(MqttException e){
            e.printStackTrace();
        }
    }


    public void publish(View v){
        e3 = (EditText) findViewById(R.id.editText3);
        final String Topic = e3.getText().toString();
        e4 = (EditText) findViewById(R.id.editText4);
        String msg = e4.getText().toString();
        MqttMessage message = new MqttMessage(msg.getBytes());
        String[] Items = {msg};
        ListView listView = (ListView) findViewById(R.id.listview);
        arrayList = new ArrayList<>(Arrays.asList(Items));
        adapter = new ArrayAdapter<String>(this, R.layout.layout, R.id.textView2, arrayList);
        listView.setAdapter(adapter);

        try{

            client.publish(Topic, message);
            e4.setText("");
            String newItems = e4.getText().toString();
            arrayList.add(newItems);
            adapter.notifyDataSetChanged();

        }catch(MqttException e){
            e.printStackTrace();
        }

    }

    public void track(View v){
        e4 = (EditText) findViewById(R.id.editText4);
        e4.setText("");
    }

}





