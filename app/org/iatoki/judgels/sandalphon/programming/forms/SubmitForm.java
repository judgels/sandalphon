package org.iatoki.judgels.sandalphon.programming.forms;

import play.data.validation.Constraints;

import java.io.File;

public final class SubmitForm {

    @Constraints.Required
    public File file;
}
