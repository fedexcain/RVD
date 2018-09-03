package io.github.fedexcain.rvd;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    final Context context = this;
    private TextView resultText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(io.github.fedexcain.rvd.R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(io.github.fedexcain.rvd.R.id.toolbar);
        setSupportActionBar(toolbar);

        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                1);

        FloatingActionButton fab = (FloatingActionButton) findViewById(io.github.fedexcain.rvd.R.id.fab);
        resultText = (TextView) findViewById(io.github.fedexcain.rvd.R.id.result);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputDialog();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(io.github.fedexcain.rvd.R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == io.github.fedexcain.rvd.R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void showInputDialog() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        boolean isTextPlain = clipboard.getPrimaryClip().getDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);
        CharSequence text = null;
        if(isTextPlain)
        {
            ClipData clipData = clipboard.getPrimaryClip();
            ClipData.Item item = clipData.getItemAt(0);
            if(item != null)
            {
                text = item.getText();
                if(text == null)
                {
                    text = item.coerceToText(this);
                }
            }
        }

        // get prompts.xml view
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View promptView = layoutInflater.inflate(io.github.fedexcain.rvd.R.layout.enter_link, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(promptView);

        final EditText editText = (EditText) promptView.findViewById(io.github.fedexcain.rvd.R.id.edittext);
        editText.setText(text);
        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        try {
                            //String jsonContent = new Scanner(new URL("https://www.reddit.com/r/AnimalsBeingDerps/comments/9bizt1/good_morning_plebs/.json").openStream(), "UTF-8").useDelimiter("\\A").next();
                            String videoURL = null;
                            String audioURL = null;
                            Log.d("url", resultText.getText().toString());
                            JsonTask jsonTask = (JsonTask) new JsonTask(MainActivity.this).execute(editText.getText().toString() + ".json");
                            String jsonContent = null;
                            try {
                                jsonContent = jsonTask.get();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            Log.d("json", jsonContent);
                            int fallbackURLPosition = jsonContent.indexOf("fallback_url") + new String("fallback_url\": \"").length();
                            Log.d("json", Integer.toString(fallbackURLPosition));
                            Log.d("json", Integer.toString(jsonContent.indexOf("\"", fallbackURLPosition)));
                            videoURL = jsonContent.substring(fallbackURLPosition, jsonContent.indexOf("\"", fallbackURLPosition));
                            Log.d("url", videoURL);

                            audioURL = videoURL.substring(0, videoURL.lastIndexOf("/") + 1) + "audio";
                            Log.d("url", audioURL);

                            RedditVideoMuxer.muxVideoAudio("asdf", videoURL, audioURL);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create an alert dialog
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }
}
