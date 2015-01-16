package org.iatoki.judgels.sandalphon;

import com.google.gson.Gson;
import org.iatoki.judgels.sealtiel.client.ClientMessage;

public final class FakeSealtiel {

    public FakeSealtiel() {
    }

    public ClientMessage fetchMessage() {
        return null;
    }

    public void sendMessage(ClientMessage message) {
        Gson gson = new Gson();

        System.out.println("the message is:");
        System.out.println(gson.toJson(message));
    }
}
