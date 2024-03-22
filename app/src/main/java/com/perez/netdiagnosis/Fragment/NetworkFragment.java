package com.perez.netdiagnosis.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.perez.netdiagnosis.Task.DnsTask;
import com.perez.netdiagnosis.Task.InfoTask;
import com.perez.netdiagnosis.Task.PingTask;
import com.perez.netdiagnosis.Task.TraceTask;
import com.perez.revkiller.databinding.FragmentNetworkBinding;

public class NetworkFragment extends BaseFragment {
    private FragmentNetworkBinding binding;

    static NetworkFragment networkFragment;

    public static NetworkFragment getInstance() {
        if(networkFragment == null){
            networkFragment = new NetworkFragment();
        }
        return networkFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentNetworkBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        
        binding.btPing.setVisibility(View.GONE);
        binding.btDns.setVisibility(View.GONE);

        binding.btPing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PingTask pingTask = new PingTask(binding.etUrl.getText()+"",binding.tvResult);
                pingTask.doTask();
            }
        });

        binding.btDns.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DnsTask pingTask = new DnsTask(binding.etUrl.getText()+"",binding.tvResult);
                pingTask.doTask();
            }
        });

        binding.btTrace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TraceTask pingTask = new TraceTask(getActivity(),binding.etUrl.getText()+"",binding.tvResult);
                pingTask.doTask();
            }
        });

        binding.btInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InfoTask pingTask = new InfoTask(binding.etUrl.getText()+"",binding.tvResult);
                pingTask.doTask();
            }
        });

        return view;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }
}
