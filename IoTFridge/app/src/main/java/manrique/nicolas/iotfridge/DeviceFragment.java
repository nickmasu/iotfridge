package manrique.nicolas.iotfridge;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


public class DeviceFragment extends Fragment {

    private BluetoothDevice mDevice;

    public DeviceFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
            mDevice = getArguments().getParcelable(MainActivity.EXTRA_DEVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_device, container, false);

        TextView mTvName = view.findViewById(R.id.tvName);
        TextView mTvAddress = view.findViewById(R.id.tvAddress);
        Button mBtConnect = view.findViewById(R.id.btConnect);
        Button mBtForget = view.findViewById(R.id.btForget);

        mTvName.setText("Name : " + mDevice.getName());
        mTvAddress.setText("Address : " + mDevice.getAddress());


        mBtConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle result = new Bundle();
                getParentFragmentManager().setFragmentResult(MainActivity.REQUEST_START_AMBIENT_SERVICE, result);
            }
        });


        mBtForget.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle result = new Bundle();
                getParentFragmentManager().setFragmentResult(MainActivity.REQUEST_FORGET_DEVICE, result);
            }
        });
        return view;
    }


}