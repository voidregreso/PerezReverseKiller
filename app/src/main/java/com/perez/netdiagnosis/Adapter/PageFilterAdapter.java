package com.perez.netdiagnosis.Adapter;

import androidx.databinding.DataBindingUtil;
import androidx.databinding.ViewDataBinding;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

import com.perez.netdiagnosis.Bean.PageBean;
import com.perez.revkiller.BR;
import com.perez.revkiller.R;

/**
 * Created by Darkal on 2016/9/5.
 */

public class PageFilterAdapter extends BaseAdapter{

    public PageFilterAdapter(List<PageBean> pageBeenList){
        this.pageBeenList = pageBeenList;
    }

    private List<PageBean> pageBeenList;

    @Override
    public int getCount() {
        return pageBeenList.size();
    }

    @Override
    public Object getItem(int position) {
        return pageBeenList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return pageBeenList.get(position).getIndex();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewDataBinding listItemBinding;
        if (convertView != null) {
            listItemBinding = (ViewDataBinding) convertView.getTag();
        } else {
            listItemBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()), R.layout.item_pages, parent, false);
            convertView = listItemBinding.getRoot();
            convertView.setTag(listItemBinding);
        }
        listItemBinding.setVariable(BR.pages,pageBeenList.get(position));
        listItemBinding.executePendingBindings();
//        listItemBinding.setButtonclick(new ButtonClick(NDGAct.this,position));
        return convertView;
    }
}
