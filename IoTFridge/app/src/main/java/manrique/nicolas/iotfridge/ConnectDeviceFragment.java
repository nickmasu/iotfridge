package manrique.nicolas.iotfridge;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ConnectDeviceFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConnectDeviceFragment extends Fragment implements View.OnClickListener {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private TextView mTvName;
    private TextView mTvAddress;
    private Button mBtConnect;
    private BluetoothDevice mDevice;

    public ConnectDeviceFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ConnectDeviceFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ConnectDeviceFragment newInstance(String param1, String param2) {
        ConnectDeviceFragment fragment = new ConnectDeviceFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mDevice = getArguments().getParcelable("device");
        }


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connect_device, container, false);

        mTvName = view.findViewById(R.id.tvName);
        mTvAddress = view.findViewById(R.id.tvAddress);
        mBtConnect = view.findViewById(R.id.btConnect);


        mTvName.setText("Name : " + mDevice.getName());
        mTvAddress.setText("Address : " + mDevice.getAddress());

        mBtConnect.setOnClickListener(this);
        return view;
    }

    @Override
    public void onClick(View v) {
        Bundle result = new Bundle();
        result.putString("bundleKey", "result");
        getParentFragmentManager().setFragmentResult("requestKey", result);
    }

}