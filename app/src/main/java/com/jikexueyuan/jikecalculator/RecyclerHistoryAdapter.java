package com.jikexueyuan.jikecalculator;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.TextView;

import org.javia.arity.SyntaxException;

import java.util.Vector;

/**
 * Created by chenhaijun on 15/6/21.
 */
public class RecyclerHistoryAdapter extends RecyclerView.Adapter<RecyclerHistoryAdapter.ViewHolder> {

    private Vector<HistoryEntry> mEntries;
    private LayoutInflater mInflater;
    private Logic mEval;
    private History mHistory;


    public RecyclerHistoryAdapter(Context context ,History history,Logic evaluator){
        mEntries = history.mEntries;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mEval = evaluator;
        mHistory = history;
    }

    @Override
    public RecyclerHistoryAdapter.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = mInflater.inflate(R.layout.history_item,viewGroup,false);
        return new RecyclerHistoryAdapter.ViewHolder(v);

    }

    @Override
    public void onBindViewHolder(RecyclerHistoryAdapter.ViewHolder viewHolder, int i) {
        HistoryEntry entry = mEntries.elementAt(i);
        String base = entry.getBase();
        viewHolder.expr.setText(base);

        try {
            String res = mEval.evaluate(base);
            viewHolder.result.setText(res);
        }catch (SyntaxException e){
            viewHolder.result.setText("");
        }

        if(getItemCount() == 1){
            viewHolder.bg.setBackgroundResource(R.drawable.history_item_bg_single);
        }else if(i == getItemCount()-1){
            viewHolder.bg.setBackgroundResource(R.drawable.history_item_bg_bottom);
        }else if(i == 0){
            viewHolder.bg.setBackgroundResource(R.drawable.history_item_bg_top);
        }else{
            viewHolder.bg.setBackgroundResource(R.drawable.history_item_bg_middle);
        }

    }

    @Override
    public int getItemCount() {
        return mEntries.size() -1;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{

        View bg;
        TextView expr;
        TextView result;

        public ViewHolder(View itemView){
            super(itemView);

            bg = itemView.findViewById(R.id.bg_holder);
            expr = (TextView)itemView.findViewById(R.id.historyExpr);
            result = (TextView) itemView.findViewById(R.id.historyResult);
        }
    }

    public void remove(int pos){
        mHistory.remove(pos);
    }

    public void removeAll(){
        mHistory.clear();
    }

    public void addAll(){
        StringBuilder sb = new StringBuilder();
        for(HistoryEntry entry : mEntries){
            if(entry.getEdited().equals("")){
                break;
            }

            sb.append(entry.getEdited() + "+");

        }

        String strToEvalute = sb.toString();
        while (strToEvalute.endsWith("+")){
            strToEvalute= strToEvalute.substring(0,sb.length()-1);
        }

        mEval.evaluateAndShowResult(strToEvalute, CalculatorDisplay.Scroll.UP);
    }
}
