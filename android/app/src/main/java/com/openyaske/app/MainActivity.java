package com.openyaske.app;

import android.content.Intent;
import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    /**
     * Forwards App Actions / shortcut deep links to the Capacitor WebView
     * so @capacitor/app can fire appUrlOpen on the JS side.
     */
    private void handleIntent(Intent intent) {
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            // The Assistant injects the feature name via {#feature} URI template,
            // so the data URI will be like "yaske://shortcuts/timetable".
            // @capacitor/app catches this automatically via the intent data URI.
            // No additional action needed here - Capacitor's bridge handles it.
        }
    }
}
