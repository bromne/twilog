package com.bromne.twilog.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;

import com.bromne.twilog.R;

public class UserSearchFragment extends Fragment {
    Listener mListener;

    EditText mUserName;
    Button mMove;

    public static UserSearchFragment newInstance() {
        return new UserSearchFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_user_search, container, false);
        mUserName = (EditText) root.findViewById(R.id.name);
        mMove = (Button) root.findViewById(R.id.move);

        mUserName.setOnEditorActionListener((sender, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                onMoveButtonSubmit(sender.getText().toString());

                return true;
            } else {
                return false;
            }
        });
        // Snackbar.make(root, "送信", Snackbar.LENGTH_SHORT).show();
        mMove.setOnClickListener(sender -> onMoveButtonSubmit(mUserName.getText().toString()));
        return root;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mListener = (Listener) context;
    }

    public void onMoveButtonSubmit(String userName) {
        mListener.onMoveToUser(userName);
    }

    public interface Listener {
        // TODO: Update argument type and name
        void onMoveToUser(String userName);
    }
}

