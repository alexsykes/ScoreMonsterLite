package uk.trialmonster.observer.data;

public class ScoreData {
    String rider, section, scores;

    public ScoreData(String section, String rider, String scores) {
        this.section = section;
        this.rider = rider;
        this.scores = scores;
    }
}
