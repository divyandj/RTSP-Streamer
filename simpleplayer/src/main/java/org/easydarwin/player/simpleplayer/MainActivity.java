package org.easydarwin.player.simpleplayer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private EditText etUrl1, etUrl2;
    private Button btnPlay;
    private SharedPreferences sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sp = PreferenceManager.getDefaultSharedPreferences(this);

        etUrl1 = (EditText) findViewById(R.id.url_input_1);
        etUrl2 = (EditText) findViewById(R.id.url_input_2);
        btnPlay = (Button) findViewById(R.id.btn_play);

        // Restore last URLs if saved
        etUrl1.setText(sp.getString("url1", ""));
        etUrl2.setText(sp.getString("url2", ""));

        btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url1 = etUrl1.getText().toString().trim();
                String url2 = etUrl2.getText().toString().trim();

                if (TextUtils.isEmpty(url1) || TextUtils.isEmpty(url2)) {
                    Toast.makeText(MainActivity.this, "Please enter both RTSP URLs", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Save URLs for reuse
                sp.edit()
                        .putString("url1", url1)
                        .putString("url2", url2)
                        .apply();

                // Start StreamActivity
                Intent intent = new Intent(MainActivity.this, StreamActivity.class);
                intent.putExtra("url1", url1);
                intent.putExtra("url2", url2);
                startActivity(intent);
            }
        });
    }
}
