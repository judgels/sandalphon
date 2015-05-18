package org.iatoki.judgels.sandalphon.bundle;

public final class ItemChoice {

    private final String content;
    private final boolean isCorrect;

    public ItemChoice(String content, boolean isCorrect) {
        this.content = content;
        this.isCorrect = isCorrect;
    }

    public String getContent() {
        return content;
    }

    public boolean isCorrect() {
        return isCorrect;
    }
}
