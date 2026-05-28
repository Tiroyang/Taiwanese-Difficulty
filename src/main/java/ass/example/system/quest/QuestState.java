package ass.example.system.quest;

public class QuestState {

    private int amount;
    private boolean completed;
    private boolean completionAnimationPlayed;

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public void addAmount(int value) {
        this.amount += value;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean isCompletionAnimationPlayed() {
        return completionAnimationPlayed;
    }

    public void setCompletionAnimationPlayed(boolean completionAnimationPlayed) {
        this.completionAnimationPlayed = completionAnimationPlayed;
    }
}