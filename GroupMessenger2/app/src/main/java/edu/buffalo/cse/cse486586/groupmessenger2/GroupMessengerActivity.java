package edu.buffalo.cse.cse486586.groupmessenger2;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import static android.content.ContentValues.TAG;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] PORTS = {"11108", "11112", "11116", "11120", "11124"};
    static final int SERVER_PORT = 10000;

    int counter=0;
    int s =0;
    int count =0;
    String failed_avd ="0";
    int lsize=5;

    public class holdback
    {
        String m;
        int mid;
        String source;
        int s;
        String dest;
        boolean status;
    }

    PriorityQueue<holdback> pq= new PriorityQueue<holdback>(10, new Comparator<holdback>() {
        public int compare(holdback w1, holdback w2)
        {
            if (w1.s<w2.s)
            { return -1; }
            else if (w1.s>w2.s)
            { return 1;}
            else
            {
                if (Integer.parseInt(w1.source)<Integer.parseInt(w2.source))
                {
                    return -1;
                }
                else
                {
                    return 1;
                }
            }
        }


    });

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        }
        catch (IOException e)
        {

            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }


        final Button button = (Button) findViewById(R.id.button4);
        final EditText editText = (EditText) findViewById(R.id.editText1);
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){

                    /*
                     * If the key is pressed (i.e., KeyEvent.ACTION_DOWN) and it is an enter key
                     * (i.e., KeyEvent.KEYCODE_ENTER), then we display the string. Then we create
                     * an AsyncTask that sends the string to the remote AVD.
                     */
                String msg = editText.getText().toString() + "\n";
                editText.setText(""); // This is one way to reset the input box.
                //TextView localTextView = (TextView) findViewById(R.id.textView1);
                //localTextView.append("\t" + msg); // This is one way to display a string.
                //TextView remoteTextView = (TextView) findViewById(R.id.textView1);
                //remoteTextView.append("\n");

                    /*
                     * Note that the following AsyncTask uses AsyncTask.SERIAL_EXECUTOR, not
                     * AsyncTask.THREAD_POOL_EXECUTOR as the above ServerTask does. To understand
                     * the difference, please take a look at
                     * http://developer.android.com/reference/android/os/AsyncTask.html
                     */
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);



            }
        });

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        public Uri buildUri(String scheme, String authority) {
            Uri.Builder uriBuilder = new Uri.Builder();
            uriBuilder.authority(authority);
            uriBuilder.scheme(scheme);
            return uriBuilder.build();
        }


        ContentValues keyValueToInsert = new ContentValues();
        @Override
        protected Void doInBackground(ServerSocket... sockets){

            ServerSocket serverSocket = sockets[0];

            while (true){
                try {

                    Socket server = serverSocket.accept();

                    DataInputStream in = new DataInputStream(server.getInputStream());
                    String msg2 = in.readUTF();
                    String[] parts = msg2.split("\\$");


                    int mode = Integer.parseInt(parts[0]);

                    if(mode==1){
                        s=s+1;
                        String mid = parts[2];
                        OutputStream outToServer = server.getOutputStream();
                        DataOutputStream out = new DataOutputStream(outToServer);
                        String dest= parts[4];
                        String msgtosend = "2" + "$" + mid + "$" + Integer.toString(s) + "$" + dest;
                        out.writeUTF(msgtosend);
                        String m = parts[1];
                        String source = parts[3];
                        holdback temppq = new holdback();
                        temppq.m = m;
                        temppq.dest = dest;
                        temppq.source = source;
                        temppq.mid = Integer.parseInt(mid);
                        temppq.status = false;
                        pq.add(temppq);

                        failed_avd=parts[5];
                        if (Integer.parseInt(failed_avd)!=0){
                            Iterator<holdback> it = pq.iterator();
                            while (it.hasNext()){
                                holdback current = it.next();
                                if(Integer.parseInt(current.source)==Integer.parseInt(failed_avd)){
                                    pq.remove(current);
                                }
                            }

                        }

                    }

                    if (mode==3){
                        int sk = Integer.parseInt(parts[3]);
                        String source = parts[2];
                        String mid = parts[1];
                        String j=parts[4];
                        if (sk > s){
                            s = sk;
                        }

                        Iterator<holdback> it = pq.iterator();
                        while (it.hasNext()) {
                            holdback current = it.next();
                            if (current.mid == Integer.parseInt(mid) && Integer.parseInt(current.source) == Integer.parseInt(source)){
                                pq.remove(current);
                                current.s = sk;
                                current.status = true;
                                current.dest=j;
                                pq.add(current);
                            }

                        }
                        failed_avd=parts[6];
                        if (Integer.parseInt(failed_avd)!=0){
                            Iterator<holdback> it2 = pq.iterator();
                            while (it2.hasNext()){
                                holdback current = it2.next();
                                if(Integer.parseInt(current.source)==Integer.parseInt(failed_avd)){
                                    pq.remove(current);
                                }
                            }

                        }



                    }



                    Log.v("Start","Start");
                    Log.v("Queue size",Integer.toString(pq.size()));

                    Iterator<holdback> it2 = pq.iterator();
                    while (it2.hasNext()){
                        holdback current = it2.next();
                        Log.v("M:",current.m+" "+Integer.toString(current.mid)+" "+Integer.toString(current.s)+" "+current.source+" "+current.dest+" "+" "+String.valueOf(current.status));

                    }

                    Log.v("End","End");

                    OutputStream outToServer = server.getOutputStream();
                    DataOutputStream out = new DataOutputStream(outToServer);
                    out.writeUTF("OK");

                    while (pq.peek()!=null){
                        if (pq.peek().status==true){

                            holdback temp = pq.poll();
                            keyValueToInsert.put("key", count);
                            keyValueToInsert.put("value", temp.m);
                            Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");
                            Uri newUri = getContentResolver().insert(mUri, keyValueToInsert);
                            publishProgress(String.valueOf(count),temp.m);
                            count ++;
                        }
                        else
                        {break;}

                    }
                }
                catch (IOException e){
                    e.printStackTrace();
                }
            }
        }


        protected void onProgressUpdate(String... strings){
            /*
             * The following code displays what is received in doInBackground().
             */
            String strReceived = strings[1].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strings[0]+" "+strReceived + "\n");
           // TextView localTextView = (TextView) findViewById(R.id.textView1);
            //localTextView.append("\n");

            /*
             * The following code creates a file in the AVD's internal storage and stores a file.
             *
             * For more information on file I/O on Android, please take a look at
             * http://developer.android.com/training/basics/data-storage/files.html
             */

            String filename = "SimpleMessengerOutput";
            String string = strReceived + "\n";
            FileOutputStream outputStream;


            try {
                outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
                outputStream.write(string.getBytes());
                outputStream.close();
            }
            catch (Exception e)
            {
                Log.e(TAG, "File write failed");
            }


            return;
        }
    }


    private class ClientTask extends AsyncTask<String, Void, Void> {

        public class list{
            int sj;
            String j;
        }

        ArrayList<list> mylist = new ArrayList<list>();

        class MyComp implements Comparator<list>{
            public int compare(list l1, list l2){
                if (l1.sj<l2.sj){
                    return -1;
                }
                else if (l1.sj>l2.sj){
                    return 1;
                }
                else{
                    if (Integer.parseInt(l1.j)<Integer.parseInt(l2.j)){
                        return 1;
                    }
                    else
                        return -1;
                }
            }

        }

        @Override
        protected Void doInBackground(String... msgs) {
            counter=counter+1;
            String counter2=Integer.toString(counter)+msgs[1];
            list temp= new list();

                for(int i=0;i<5;i++){

                    if (Integer.parseInt(PORTS[i]) != Integer.parseInt(failed_avd)) {

                        String remotePort = PORTS[i];
                        Socket socket = null;
                        try {
                            socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                    Integer.parseInt(remotePort));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        String m = msgs[0];
                        String mid = counter2;
                        String source = msgs[1];
                        String dest = PORTS[i];
                        String msgToSend = Integer.toString(1) + "$" + m + "$" + mid + "$" + source + "$" + dest+"$"+failed_avd;
                        OutputStream outToServer = null;

                        try {
                            outToServer = socket.getOutputStream();
                        } catch (IOException e) {
                            e.printStackTrace();
						}
                        DataOutputStream out = new DataOutputStream(outToServer);
                        try {
                            out.writeUTF(msgToSend);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        DataInputStream in = null;
                        try {
                            in = new DataInputStream(socket.getInputStream());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        String ack = null;
                        try {

                            if (in != null) {
                                ack = in.readUTF();
                            }


                        } catch (IOException e) {

                            e.printStackTrace();
                            Log.v("Exception", "Caught");
                            Log.v("Failed Node", PORTS[i]);
                            failed_avd = PORTS[i];

                        }

                        try {
                            String[] parts = ack.split("\\$");
                            temp.sj = Integer.parseInt(parts[2]);
                            temp.j = parts[3];
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                        }


                        mylist.add(temp);
                        Collections.sort(mylist, new MyComp());

                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }


            temp = mylist.get(0);


            for(int i=0;i<5;i++){

                String remotePort = PORTS[i];
                Socket socket = null;
                try {
                    socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                           Integer.parseInt(remotePort));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String mid = counter2;

                String msgToSend = Integer.toString(3) + "$" + mid + "$" + msgs[1] + "$" + temp.sj + "$" +temp.j+"$"+ PORTS[i]+"$"+failed_avd;

                OutputStream outToServer = null;
                try {
                    outToServer = socket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                DataOutputStream out = new DataOutputStream(outToServer);
                try {
                    out.writeUTF(msgToSend);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


            return null;
        }
    }
}
