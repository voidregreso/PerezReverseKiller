package es.perez.netdiagnosis.Fragment;

import android.content.Intent;
import android.os.Bundle;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import net.lightbody.bmp.core.har.HarEntry;
import net.lightbody.bmp.core.har.HarLog;

import java.util.ArrayList;
import java.util.List;

import es.perez.netdiagnosis.Activity.HarDetailActivity;
import es.perez.netdiagnosis.Activity.NDGAct;
import com.perez.revkiller.R;
import com.perez.catchexception.CrashApp;
import com.perez.revkiller.databinding.FragmentPreviewBinding;

import es.perez.netdiagnosis.View.RecycleViewDivider;

public class PreviewFragment extends BaseFragment {
    private FragmentPreviewBinding binding;

    HarLog harLog;
    List<HarEntry> harEntryList = new ArrayList<>();

    PreviewAdapter previewAdapter;

    Boolean isHiddenHID = false;

    static PreviewFragment previewFragment;

    public static PreviewFragment getInstance() {
        if (previewFragment == null) {
            previewFragment = new PreviewFragment();
        }
        return previewFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentPreviewBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        if(CrashApp.isInitProxy) {
            harLog = ((CrashApp) getActivity().getApplication()).proxy.getHar().getLog();
            harEntryList.addAll(harLog.getEntries());
        }
        binding.rvPreview.addItemDecoration(new RecycleViewDivider(getActivity(), RecycleViewDivider.VERTICAL_LIST));
        binding.rvPreview.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvPreview.setAdapter(previewAdapter = new PreviewAdapter());

        if(((NDGAct) getActivity()).searchView!=null){
            ((NDGAct) getActivity()).searchView.setVisibility(View.VISIBLE);
        }

        return view;
    }


    private class PreviewAdapter extends RecyclerView.Adapter<PreviewAdapter.MyViewHolder> implements Filterable {

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MyViewHolder(LayoutInflater.from(getContext()).inflate(R.layout.item_preview, parent, false));
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            HarEntry harEntry = harEntryList.get(position);
            holder.rootView.setOnClickListener(new ClickListner(harEntry));
            holder.tv.setText(harEntry.getRequest().getUrl());
            if(harEntry.getResponse().getStatus()>400){
                holder.iconView.setImageDrawable(getResources().getDrawable(R.drawable.ic_error_black_24dp));
            }else if(harEntry.getResponse().getStatus()>300){
                holder.iconView.setImageDrawable(getResources().getDrawable(R.drawable.ic_directions_black_24dp));
            }else if(harEntry.getResponse().getContent().getMimeType().contains("image")) {
                holder.iconView.setImageDrawable(getResources().getDrawable(R.drawable.ic_photo_black_24dp));
            }else{
                holder.iconView.setImageDrawable(getResources().getDrawable(R.drawable.ic_description_black_24dp));
            }
            holder.detailTextView.setText("Status:" + harEntry.getResponse().getStatus() +
                    " Size:" + harEntry.getResponse().getBodySize() +
                    "Bytes Time:" + harEntry.getTime() + "ms");
        }

        @Override
        public int getItemCount() {
            return harEntryList.size();
        }


        public class MyViewHolder extends RecyclerView.ViewHolder {

            TextView tv;
            TextView detailTextView;
            View rootView;
            ImageView iconView;

            public MyViewHolder(View view) {
                super(view);
                tv = (TextView) view.findViewById(R.id.tv_url);
                detailTextView = (TextView) view.findViewById(R.id.tv_detail);
                rootView = view;
                iconView = (ImageView) view.findViewById(R.id.iv_icon);
            }
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    harLog = ((NDGAct) getActivity()).getFiltedHar().getLog();
                    
                    FilterResults results = new FilterResults();
                    
                    if (results.values == null) {
                        harEntryList.clear();
                        harEntryList.addAll(harLog.getEntries());
                    }
                    
                    if (constraint == null || constraint.length() == 0) {
                        results.values = harLog.getEntries();
                        results.count = harLog.getEntries().size();
                    } else {
                        String prefixString = constraint.toString();
                        final int count = harEntryList.size();
                        
                        final ArrayList<HarEntry> newValues = new ArrayList<>();
                        for (int i = 0; i < count; i++) {
                            final HarEntry value = harEntryList.get(i);
                            String url = value.getRequest().getUrl();
                            
                            if (url.contains(prefixString)) {
                                newValues.add(value);
                            } else {
                                
                                String[] words = prefixString.split(" ");

                                for (String word : words) {
                                    if (url.contains(word)) {
                                        newValues.add(value);
                                        break;
                                    }
                                }
                            }
                        }
                        results.values = newValues;
                        results.count = newValues.size();
                    }
                    return results;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    harEntryList.clear();
                    if(results.values instanceof List){
                        harEntryList.addAll((List<HarEntry>) results.values);
                    }
                    if (results.count > 0) {
                        previewAdapter.notifyDataSetChanged();
                    } else {
                        
                        if (constraint.length() != 0) {
                            previewAdapter.notifyDataSetChanged();
                            return;
                        }
                        
                        harEntryList.addAll(harLog.getEntries());
                        previewAdapter.notifyDataSetChanged();
                    }
                }
            };
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            notifyHarChange();
        }
    }

    public void notifyHarChange(){
        if (previewAdapter != null) {
            harLog = ((NDGAct) getActivity()).getFiltedHar().getLog();
            harEntryList.clear();
            harEntryList.addAll(harLog.getEntries());
            previewAdapter.notifyDataSetChanged();
        }
    }

    public class ClickListner implements View.OnClickListener {
        HarEntry harEntry;

        public ClickListner(HarEntry harEntry){
            this.harEntry = harEntry;
        }

        @Override
        public void onClick(View view) {
            if(harLog.getEntries().indexOf(harEntry)>=0) {
                isHiddenHID = true;
                Intent intent = new Intent(getContext(), HarDetailActivity.class);
                intent.putExtra("pos", ((CrashApp) getActivity().getApplication()).proxy.
                        getHar().getLog().getEntries().indexOf(harEntry));
                getActivity().startActivity(intent);
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    public void filterItem(CharSequence s){
        if(previewAdapter!=null) {
            previewAdapter.getFilter().filter(s);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(binding.rvPreview!=null) {
            binding.rvPreview.requestFocus();
            if(((NDGAct)getActivity()).searchView!=null) {
                filterItem(((NDGAct) getActivity()).searchView.getQuery());
            }
        }
    }
}
