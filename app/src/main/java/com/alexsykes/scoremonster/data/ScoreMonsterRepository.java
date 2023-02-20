package com.alexsykes.scoremonster.data;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;

public class ScoreMonsterRepository {
    private final ScoreDao scoreDao;
    private final TimeDao timeDao;
    private final TrialDao trialDao;
    private final LiveData<List<Score>> scoreList;
    private final LiveData<List<Trial>> trialList;
    private final LiveData<List<Time>> timeList;
    private LiveData<List<Score>> currentRiderScores;

    public ScoreMonsterRepository(Application application) {
        ScoreRoomDatabase db = ScoreRoomDatabase.getDatabase(application);
        scoreDao = db.scoreDao();
        trialDao = db.trialDao();
        timeDao = db.timeDao();
        scoreList = scoreDao.getAllScores();
        timeList = timeDao.getAllTimes();
        trialList = trialDao.getAllTrials();
    }

    public LiveData<List<Score>> getScoreList() {
        return scoreList;
    }

    public void insertScore(Score score) {
        ScoreRoomDatabase.databaseWriteExecutor.execute(() -> {
            scoreDao.insert(score);
        });

    }

    public LiveData<List<Trial>> getTrialList() {
        return trialList;
    }

    public LiveData<List<Time>> getTimeList() {
        return timeList;
    }
//    public List<Score> getCurrentRiderScores(int rider, int trialid) {
//        currentRiderScores = scoreDao.getCurrentRiderScores(rider, trialid);
//        return currentRiderScores;
//    }
}
