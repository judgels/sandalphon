#Judgels Sandalphon 

##Description
Sandalphon is an application built using [Play Framework](https://www.playframework.com/) to provide programming resources repository functions and services.

Programming resources consist of programming problems and learning materials. Sandalphon can have clients that use its programming resources. Currently Sandalphon's client is only [Uriel](https://github.com/ia-toki/judgels-uriel) (Competition Gate).

Programming problems and learning materials can be tested by writer on Sandalphon. Writers can share any programming resources that they have access to other writers. Currently writer only given to limited users at the online version.

Sandalphon depends on [Sealtiel](https://github.com/ia-toki/judgels-sealtiel) to send grading request to Gabriel and [Jophiel](https://github.com/ia-toki/judgels-jophiel) for authentication and authorization.

##Set Up And Run
To set up Sandalphon, you need to:

1. Clone [Judgels Play Commons](https://github.com/ia-toki/judgels-play-commons), [Gabriel Commons](https://github.com/ia-toki/judgels-gabriel-commons), and [Judgels Frontend Commons](https://github.com/ia-toki/judgels-frontend-commons) into the same level of Sandalphon directory, so that the directory looks like:
    - Parent Directory
        - gabriel-commons
        - judgels-frontend-commons
        - judgels-play-commons
        - judgels-sandalphon

2. Copy conf/application_default.conf into conf/application.conf and change the configuration accordingly. **Refer to the default configuration file for explanation of the configuration keys.** In the application configuration, Sandalphon need to connect to running Jophiel (for authentication and authorization) and Sealtiel (to grade programming problems) application. In order to connect Sandalphon to running Jophiel and Sealtiel, Sandalphon must be registered as Jophiel and Sealtiel clients. 

3. Copy conf/db_default.conf into conf/db.conf and change the configuration accordingly. **Refer to the default configuration file for explanation of the configuration keys.** 

To run Sandalphon, just run "activator" then it will check and download all dependencies and enter Play Console.
In Play Console use "run" command to run Sandalphon. By default it will listen on port 9000. For more information of Play Console, please read the [documentation](https://www.playframework.com/documentation/2.3.x/PlayConsole).

After login on Sandalphon using user on Jophiel, add "writer,admin" value to role column of your user record on table "sandalphon\_user\_role" then relogin (logout and login again) to access full feature. 

The version that is recommended for public use is [v0.1.0](https://github.com/ia-toki/judgels-sandalphon/tree/v0.1.0).
