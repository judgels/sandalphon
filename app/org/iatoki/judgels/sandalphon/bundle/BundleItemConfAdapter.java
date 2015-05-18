package org.iatoki.judgels.sandalphon.bundle;

import play.api.mvc.Call;
import play.data.Form;
import play.mvc.Http;
import play.twirl.api.Html;

public interface BundleItemConfAdapter {
    Form generateForm();

    Form generateForm(String conf, String meta);

    Html getConfHtml(Form form, Call target, String submitLabel);

    Form bindFormFromRequest(Http.Request request);

    String getMetaFromForm(Form form);

    String processRequestForm(Form form);

    BundleItemConf parseConfString(String conf);
}
