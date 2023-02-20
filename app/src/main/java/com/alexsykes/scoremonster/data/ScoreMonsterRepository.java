package com.alexsykes.scoremonster.data;

import android.app.Application;

import java.util.List;

public class ScoreMonsterRepository {
    private final ScoreDao scoreDao;
    private final TimeDao timeDao;
    private final TrialDao trialDao;
    private final List<Score> allScores;
    private List<Score> currentRiderScores;

    public ScoreMonsterRepository(Application application) {
        ScoreRoomDatabase db = ScoreRoomDatabase.getDatabase(application);
        scoreDao = db.scoreDao();
        trialDao = db.trialDao();
        timeDao = db.timeDao();
        allScores = scoreDao.getAllScores();
    }

    public List<Score> getCurrentRiderScores(int rider, int trialid) {
        currentRiderScores = scoreDao.getCurrentRiderScores(rider, trialid);
        return currentRiderScores;
    }
}
