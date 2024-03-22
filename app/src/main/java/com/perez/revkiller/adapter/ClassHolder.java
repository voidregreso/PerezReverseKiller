package com.perez.revkiller.adapter;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.perez.revkiller.R;
import com.perez.revkiller.adapter.base.RecyclerViewAdapter;
import com.perez.revkiller.adapter.base.RecyclerViewHolder;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JPackage;

public class ClassHolder extends RecyclerViewHolder<ClassHolder> {

    ImageView fileIcon;
    TextView fileName;
    TextView fileChildCount;
    TextView fileSize;
    ImageView dir_enter_image;

    public ClassHolder(View view) {
        super(view);
        fileIcon = view.findViewById(R.id.fileIcon);
        fileName = view.findViewById(R.id.fileName);
        fileChildCount = view.findViewById(R.id.fileChildCount);
        fileSize = view.findViewById(R.id.fileSize);
        dir_enter_image = view.findViewById(R.id.dir_enter_image);
    }

    @Override
    public void onBindViewHolder(final ClassHolder classHolder, RecyclerViewAdapter adapter, int position) {
        JNode jNode = (JNode) adapter.getItem(position);
        classHolder.fileName.setText(jNode.getName());
        if(R.mipmap.package_obj == jNode.getIcon()) {
            JPackage jPackage = (JPackage) jNode;
            classHolder.fileChildCount.setVisibility(View.VISIBLE);
            int itmcnt = jPackage.getClasses().size() + jPackage.getInnerPackages().size();
            classHolder.fileChildCount.setText(itmcnt + 
                ((itmcnt==1)? " item": " items"));
            classHolder.fileSize.setVisibility(View.GONE);
            classHolder.dir_enter_image.setVisibility(View.VISIBLE);
        } else {
            classHolder.fileChildCount.setVisibility(View.GONE);
            classHolder.fileSize.setVisibility(View.GONE);
            classHolder.dir_enter_image.setVisibility(View.GONE);
        }
        
        classHolder.fileIcon.setImageResource(jNode.getIcon());
    }


}
