package com.perez.netdiagnosis.Fragment;

import android.content.Context;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

import com.perez.netdiagnosis.Activity.NDGAct;

public abstract class BaseFragment extends Fragment {

    protected BackHandledInterface mBackHandledInterface;

    public abstract boolean onBackPressed();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!(getActivity() instanceof BackHandledInterface)){
            throw new ClassCastException("Hosting Activity must implement BackHandledInterface");
        }else{
            this.mBackHandledInterface = (BackHandledInterface)getActivity();
        }
    }
    
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            if (getActivity() instanceof NDGAct) {
                ((NDGAct) getActivity()).changeStateBar(this);
            }
            if (mBackHandledInterface != null) {
                
                mBackHandledInterface.setSelectedFragment(this);
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (getActivity() instanceof NDGAct) {
            ((NDGAct) getActivity()).changeStateBar(this);
        }
    }
}
