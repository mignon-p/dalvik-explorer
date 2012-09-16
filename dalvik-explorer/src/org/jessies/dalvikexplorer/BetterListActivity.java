/*
 * Copyright (C) 2011 The Android Open Source Project
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

package org.jessies.dalvikexplorer;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;

/**
 * A ListActivity that automatically enables filtering, and automatically
 * offers a search view in the action bar (>= honeycomb).
 */
public class BetterListActivity extends ListActivity {
    public BetterListActivity() {
    }
    
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getListView().setTextFilterEnabled(true);
    }
    
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options, menu);
        Compatibility compatibility = Compatibility.get();
        compatibility.configureActionBar(this);
        compatibility.configureSearchView(this, menu);
        return true;
    }
    
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent intent = new Intent(this, DalvikExplorerActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        case R.id.menu_search:
            showSoftKeyboard();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    private void showSoftKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(getListView(), 0);
    }
}
