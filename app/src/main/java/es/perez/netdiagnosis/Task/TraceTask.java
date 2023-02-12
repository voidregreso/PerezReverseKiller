package es.perez.netdiagnosis.Task;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.widget.TextView;

import com.netease.LDNetDiagnoService.LDNetDiagnoListener;
import com.netease.LDNetDiagnoService.LDNetDiagnoService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import es.perez.netdiagnosis.Utils.DeviceUtils;


/**
 * Created by xuzhou on 2016/8/1.
 */
public class TraceTask extends BaseTask  implements LDNetDiagnoListener {
    String url;
    TextView resultTextView;
    Activity context;

    public TraceTask(Activity context , String url, TextView resultTextView)  {
        super(url, resultTextView);
        this.context = context;
        this.url = url;
        this.resultTextView = resultTextView;
    }

    @Override
    public Runnable getExecRunnable() {
        return execRunnable;
    }

    public Runnable execRunnable = new Runnable() {
        @Override
        public void run() {
            try{
                int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE);

                if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.READ_PHONE_STATE}, 1);
                } else {
                    LDNetDiagnoService _netDiagnoService = new LDNetDiagnoService(context.getApplicationContext(),
                            "NetworkDiagnosis", "Network Diagnosis Application", DeviceUtils.getVersion(context), "",
                            "", url, "", "",
                            "", "", TraceTask.this);
                    _netDiagnoService.setIfUseJNICTrace(true);
                    _netDiagnoService.execute();
                }
            }
            catch (Exception e){
                resultTextView.post(new updateResultRunnable(e.toString() + "\n"));
            }
        }
    };

    public void setResult(String result){
        Pattern pattern = Pattern.compile("(?<=rom )[\\w\\W]+(?=\\n\\n)");
        Matcher matcher = pattern.matcher(result);
        if(matcher.find()){
            resultTextView.post(new updateResultRunnable(matcher.group(0) + "\n"));
        }
    }

    @Override
    public void OnNetDiagnoFinished(String log) {

    }

    @Override
    public void OnNetDiagnoUpdated(String log) {
        resultTextView.post(new updateResultRunnable(log));
    }
}
