package manrique.nicolas.iotfridge;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


public class AmbientInfoFragment extends Fragment implements View.OnClickListener {

    private BroadcastReceiver mReceiver;
    private TextView mTvTemperature;
    private TextView mTvHumidity;
    private TextView mTvBattery;
    private Button mBtDisconnect;
    private IntentFilter mIntentFilter;


    public AmbientInfoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(AmbientInfoService.ACTION_CONNECTION_STATUS);
        mIntentFilter.addAction(AmbientInfoService.ACTION_AMBIENT_INFO);

        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(AmbientInfoService.ACTION_CONNECTION_STATUS)) {
                    if (!intent.getBooleanExtra(AmbientInfoService.EXTRA_CONNECTION_STATE, false))
                        disconnectService();

                } else if (intent.getAction().equals(AmbientInfoService.ACTION_AMBIENT_INFO)) {
                    float temperature = intent.getFloatExtra(AmbientInfoService.EXTRA_AMBIENT_TEMPERATURE, -1);
                    float humidity = intent.getFloatExtra(AmbientInfoService.EXTRA_AMBIENT_HUMIDITY, -1);
                    float battery = intent.getFloatExtra(AmbientInfoService.EXTRA_AMBIENT_BATTERY, -1);

                    updateAmbientInfo(temperature, humidity, battery);
                }
            }
        };
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_ambient_info, container, false);

        mTvTemperature = view.findViewById(R.id.tvTemperature);
        mTvHumidity = view.findViewById(R.id.tvHumidity);
        mTvBattery = view.findViewById(R.id.tvBattery);

        mBtDisconnect = view.findViewById(R.id.btDisconnect);
        mBtDisconnect.setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        disconnectService();
    }

    private void disconnectService() {
        Bundle result = new Bundle();
        getParentFragmentManager().setFragmentResult(MainActivity.REQUEST_CLOSE_AMBIENT_SERVICE, result);
    }

    private void updateAmbientInfo(float temperature, float humidity, float battery) {
        mTvTemperature.setText("Temperature : " + String.valueOf(temperature));
        mTvHumidity.setText("Humidity : " + String.valueOf(humidity));
        mTvBattery.setText("Battery : " + String.valueOf(battery));
    }

    @Override
    public void onResume() {
        super.onResume();
        requireActivity().registerReceiver(mReceiver, mIntentFilter);
    }

    @Override
    public void onPause() {
        requireActivity().unregisterReceiver(mReceiver);
        super.onPause();
    }
}