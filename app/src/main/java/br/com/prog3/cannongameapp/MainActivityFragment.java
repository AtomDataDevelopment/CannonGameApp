package br.com.prog3.cannongameapp;

import android.media.AudioManager;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MainActivityFragment extends Fragment {
    private CannonView cannonView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // super.onCreateView is not strictly necessary here for Fragment, but okay to keep if needed, 
        // though usually we just inflate and return.
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        cannonView = (CannonView) view.findViewById(R.id.cannonView);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Use getActivity() cautiously or check for null, but for this simple app it's likely fine
        // Moving volume control setup here or onCreate is better than onActivityCreated which is deprecated
        if (getActivity() != null) {
            getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cannonView != null) {
            cannonView.stopGame();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (cannonView != null) {
            cannonView.releaseResources();
        }
    }
}