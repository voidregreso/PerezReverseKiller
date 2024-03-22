package com.perez.netdiagnosis.Adapter;

import android.content.DialogInterface;
import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

import com.perez.netdiagnosis.Activity.ChangeFilterActivity;

import com.perez.revkiller.BR;

import com.perez.netdiagnosis.Bean.ResponseFilterRule;

import com.perez.revkiller.R;

/**
 * Created by Darkal on 2016/9/5.
 */

public class ContentFilterAdapter extends BaseAdapter{
    ChangeFilterActivity changeFilterActivity;

    public ContentFilterAdapter(ChangeFilterActivity changeFilterActivity,List<ResponseFilterRule> ruleList){
        this.ruleList = ruleList;
        this.changeFilterActivity = changeFilterActivity;
    }

    private List<ResponseFilterRule> ruleList;

    @Override
    public int getCount() {
        return ruleList.size();
    }

    @Override
    public Object getItem(int position) {
        return ruleList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewDataBinding listItemBinding;
        if (convertView != null) {
            listItemBinding = (ViewDataBinding) convertView.getTag();
        } else {
            listItemBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.item_filter, parent, false);
            convertView = listItemBinding.getRoot();
            convertView.setTag(listItemBinding);
        }
        listItemBinding.setVariable(BR.pages,ruleList.get(position));
        listItemBinding.executePendingBindings();
//        listItemBinding.cli(new ButtonClick(NDGAct.this,position));
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeFilterActivity.showDialog(ruleList.get(position));
            }
        });
        convertView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(changeFilterActivity);
                builder.setTitle("Please confirm whether to clear the injection item?");
                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        ruleList.remove(ruleList.get(position));
                        ContentFilterAdapter.this.notifyDataSetChanged();
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                });
                builder.create().show();

                return false;
            }
        });
        return convertView;
    }
}
