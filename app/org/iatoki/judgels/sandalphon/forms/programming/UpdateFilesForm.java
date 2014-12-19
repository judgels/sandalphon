package org.iatoki.judgels.sandalphon.forms.programming;

import play.data.validation.Constraints;

import java.io.File;

public final class UpdateFilesForm {

    @Constraints.Required
    public File file;
}
