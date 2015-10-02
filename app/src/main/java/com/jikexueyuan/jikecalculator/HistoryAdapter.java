/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.jikexueyuan.jikecalculator;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.view.View;
import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.jikexueyuan.jikecalculator.*;
import com.jikexueyuan.jikecalculator.History;
import com.jikexueyuan.jikecalculator.HistoryEntry;
import com.jikexueyuan.jikecalculator.Logic;

import java.util.Vector;

import org.javia.arity.SyntaxException;

class HistoryAdapter extends BaseAdapter {
    private Vector<com.jikexueyuan.jikecalculator.HistoryEntry> mEntries;
    private LayoutInflater mInflater;
    private com.jikexueyuan.jikecalculator.Logic mEval;

    HistoryAdapter(Context context, com.jikexueyuan.jikecalculator.History history, com.jikexueyuan.jikecalculator.Logic evaluator) {
        mEntries = history.mEntries;
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mEval = evaluator;
    }

    // @Override
    public int getCount() {
        return mEntries.size() - 1;
    }

    // @Override
    public Object getItem(int position) {
        return mEntries.elementAt(position);
    }

    // @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    // @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = mInflater.inflate(R.layout.history_item, parent, false);
        } else {
            view = convertView;
        }
        View bg = view.findViewById(R.id.bg_holder);
        TextView expr   = (TextView) view.findViewById(R.id.historyExpr);
        TextView result = (TextView) view.findViewById(R.id.historyResult);

        com.jikexueyuan.jikecalculator.HistoryEntry entry = mEntries.elementAt(position);
        String base = entry.getBase();
        expr.setText(entry.getBase());

        try {
            String res = mEval.evaluate(base);
            result.setText("= " + res);
        } catch (SyntaxException e) {
            result.setText("");
        }

        if(getCount()==1){
            bg.setBackgroundResource(R.drawable.history_item_bg_single);
        }else if(position == getCount()-1){
            bg.setBackgroundResource(R.drawable.history_item_bg_bottom);
        }else if(position == 0){
            bg.setBackgroundResource(R.drawable.history_item_bg_top);
        }else{
            bg.setBackgroundResource(R.drawable.history_item_bg_middle);
        }

        return view;
    }
}

