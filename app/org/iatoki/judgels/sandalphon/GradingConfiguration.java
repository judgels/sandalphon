package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.sandalphon.forms.UpdateGradingForm;

public final class GradingConfiguration {
    public int timeLimit;
    public int memoryLimit;

    public void setTimeLimit(int seconds) {
        this.timeLimit = seconds;
    }

    public void setMemoryLimit(int megabytes) {
        this.memoryLimit = megabytes;
    }

    public static GradingConfiguration createDefault() {
        GradingConfiguration conf = new GradingConfiguration();
        conf.setTimeLimit(2);
        conf.setMemoryLimit(64);
        return conf;
    }

    public UpdateGradingForm toForm() {
        UpdateGradingForm form = new UpdateGradingForm();
        form.timeLimit = timeLimit;
        form.memoryLimit = memoryLimit;

        return form;
    }
}
