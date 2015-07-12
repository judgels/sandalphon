package org.iatoki.judgels.sandalphon.services;

import org.iatoki.judgels.play.Page;
import org.iatoki.judgels.sandalphon.Client;
import org.iatoki.judgels.sandalphon.ClientLesson;
import org.iatoki.judgels.sandalphon.ClientNotFoundException;
import org.iatoki.judgels.sandalphon.ClientProblem;

import java.util.List;

public interface ClientService {

    boolean clientExistsByClientJid(String clientJid);

    List<Client> findAllClients();

    Client findClientById(long clientId) throws ClientNotFoundException;

    Client findClientByJid(String clientJid);

    void createClient(String name);

    void updateClient(long clientId, String name);

    void deleteClient(long clientId);

    Page<Client> pageClients(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString);

    boolean isClientProblemInProblemByClientJid(String problemJid, String clientJid);

    ClientProblem findClientProblemByClientJidAndProblemJid(String clientJid, String problemJid);

    ClientProblem findClientProblemByClientProblemId(long clientProblemId);

    List<ClientProblem> findAllClientProblemByProblemJid(String problemJid);

    void createClientProblem(String problemJid, String clientJid);

    void deleteClientProblem(long clientProblemId);

    boolean isClientLessonInLessonByClientJid(String lessonJid, String clientJid);

    ClientLesson findClientLessonByClientJidAndLessonJid(String clientJid, String lessonJid);

    ClientLesson findClientLessonByClientLessonId(long clientLessonId);

    List<ClientLesson> findAllClientLessonByLessonJid(String lessonJid);

    void createClientLesson(String lessonJid, String clientJid);

    void deleteClientLesson(long clientLessonId);
}
