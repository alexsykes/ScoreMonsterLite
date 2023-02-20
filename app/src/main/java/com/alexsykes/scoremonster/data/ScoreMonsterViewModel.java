package com.alexsykes.scoremonster.data;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class ScoreMonsterViewModel extends AndroidViewModel {
    private final ScoreMonsterRepository scoreMonsterRepository;
    private final LiveData<List<Score>> scoreList;
    private final LiveData<List<Trial>> trialList;
    private final LiveData<List<Time>> timeList;

    public ScoreMonsterViewModel(@NonNull Application application) {
        super(application);
        scoreMonsterRepository = new ScoreMonsterRepository(application);
        scoreList = scoreMonsterRepository.getScoreList();
        trialList = scoreMonsterRepository.getTrialList();
        timeList = scoreMonsterRepository.getTimeList();
    }
}

