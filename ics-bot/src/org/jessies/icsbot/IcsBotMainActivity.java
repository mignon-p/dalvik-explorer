/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jessies.icsbot;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;

/**
 * Explains to the user that there's really nothing to see here.
 * This app will automatically spring into action when needed (via its other activity).
 */
public class IcsBotMainActivity extends Activity implements OnClickListener, OnCancelListener {
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new AlertDialog.Builder(this)
                .setCancelable(true)
                .setIcon(R.drawable.icon)
                .setTitle("IcsBot")
                .setMessage("Activate IcsBot by opening an attachment containing calendar events. (These typically have a .ics extension.)")
                .setOnCancelListener(this)
                .setPositiveButton("OK", this)
                .create()
                .show();
    }
    
    public void onClick(DialogInterface dialogInterface, int i) {
        finish();
    }
    
    public void onCancel(DialogInterface dialogInterface) {
        finish();
    }
}
