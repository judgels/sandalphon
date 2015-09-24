package org.iatoki.judgels.sandalphon.services;

import org.iatoki.judgels.play.Page;
import org.iatoki.judgels.play.services.JudgelsAppClientService;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientLesson;
import org.iatoki.judgels.sandalphon.ClientNotFoundException;
import org.iatoki.judgels.sandalphon.ClientProblem;

import java.util.List;

public interface ClientService extends JudgelsAppClientService {

    boolean clientExistsByJid(String clientJid);

    List<Client> getClients();

    Client findClientById(long clientId) throws ClientNotFoundException;

    Client findClientByJid(String clientJid);

    Client createClient(String name, String userJid, String userIpAddress);

    void updateClient(String clientJid, String name, String userJid, String userIpAddress);

    Page<Client> getPageOfClients(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString);

    boolean isClientAuthorizedForProblem(String problemJid, String clientJid);

    ClientProblem findClientProblemByClientJidAndProblemJid(String clientJid, String problemJid);

    ClientProblem findClientProblemById(long clientProblemId);

    List<ClientProblem> getClientProblemsByProblemJid(String problemJid);

    void createClientProblem(String problemJid, String clientJid, String userJid, String userIpAddress);

    boolean isClientAuthorizedForLesson(String lessonJid, String clientJid);

    ClientLesson findClientLessonByClientJidAndLessonJid(String clientJid, String lessonJid);

    ClientLesson findClientLessonById(long clientLessonId);

    List<ClientLesson> getClientLessonsByLessonJid(String lessonJid);

    void createClientLesson(String lessonJid, String clientJid, String userJid, String userIpAddress);
}
