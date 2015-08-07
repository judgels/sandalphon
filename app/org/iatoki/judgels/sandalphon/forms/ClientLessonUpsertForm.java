package org.iatoki.judgels.sandalphon.forms;

import play.data.validation.Constraints;

public final class ClientLessonUpsertForm {

    @Constraints.Required
    public String clientJid;
}
