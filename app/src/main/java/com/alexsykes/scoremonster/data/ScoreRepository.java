package com.alexsykes.scoremonster.data;

import android.app.Application;

import java.util.List;

public class ScoreRepository {
    private final ScoreDao scoreDao;
    private final List<Score> allScores;
    private List<Score> currentRiderScores;

    public ScoreRepository(Application application) {
        ScoreRoomDatabase db = ScoreRoomDatabase.getDatabase(application);
        scoreDao = db.scoreDao();
        allScores = scoreDao.getAllScores();
    }

    public List<Score> getCurrentRiderScores(int rider, int trialid) {
        currentRiderScores = scoreDao.getCurrentRiderScores(rider, trialid);
        return currentRiderScores;
    }
}
