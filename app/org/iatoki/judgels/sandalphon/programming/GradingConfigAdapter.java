package org.iatoki.judgels.sandalphon.programming;

import org.iatoki.judgels.gabriel.GradingConfig;
import play.data.Form;
import play.mvc.Http;
import play.twirl.api.Html;

import java.io.File;
import java.util.List;

public interface GradingConfigAdapter {
    Form<?> createFormFromConfigJson(String gradingConfigJson);

    Form<?> createFormFromRequest(Http.Request request);

    GradingConfig createConfigFromForm(Form<?> form);

    Html renderForm(Form<?> form, Problem problem, List<File> testDataFiles, List<File> helperFiles);
}
