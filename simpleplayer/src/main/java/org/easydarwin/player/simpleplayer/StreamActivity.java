package org.easydarwin.player.simpleplayer;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;

import org.easydarwin.video.EasyPlayerClient;

public class StreamActivity extends AppCompatActivity implements EasyPlayerClient.SEIDataCallback {

    private EasyPlayerClient client1;
    private EasyPlayerClient client2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stream);

        TextureView textureView1 = findViewById(R.id.texture_view_1);
        TextureView textureView2 = findViewById(R.id.texture_view_2);

        String url1 = getIntent().getStringExtra("url1");
        String url2 = getIntent().getStringExtra("url2");

        // Initialize clients
        client1 = new EasyPlayerClient(this, textureView1, null, null, this);
        client2 = new EasyPlayerClient(this, textureView2, null, null, this);

        if (url1 != null && !url1.isEmpty()) {
            client1.play(url1);
        }
        if (url2 != null && !url2.isEmpty()) {
            client2.play(url2);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (client1 != null) client1.stop();
        if (client2 != null) client2.stop();
    }

    @Override
    public void onSEIData(byte[] sei) {
        // Optional SEI callback
    }
}
