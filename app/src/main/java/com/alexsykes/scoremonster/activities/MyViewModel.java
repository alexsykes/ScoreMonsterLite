package com.alexsykes.scoremonster.activities;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class MyViewModel extends ViewModel {
    private MutableLiveData<List<String>> users;

    public LiveData<List<String>> getUsers() {
        if (users == null) {
            users = new MutableLiveData<List<String>>();
            loadUsers();
        }
        return users;
    }

    private void loadUsers() {
        // Do an asynchronous operation to fetch users.
    }

}
