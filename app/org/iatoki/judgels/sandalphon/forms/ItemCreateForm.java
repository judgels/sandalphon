package org.iatoki.judgels.sandalphon.forms;

import play.data.validation.Constraints;

public final class ItemCreateForm {

    @Constraints.Required
    public String itemType;
}
