<img align="right" src="https://i.imgur.com/JvpuEak.jpg" height="200" width="200">

## Baymax
_A friendly help desk bot for Discord_

[![Build Status](https://github.com/napstr/Baymax/actions/workflows/build.yaml/badge.svg)](https://github.com/napstr/Baymax/actions/workflows/build.yaml)
[![SonarCloud](https://sonarcloud.io/api/project_badges/measure?project=space.npstr.baymax%3Abaymax&metric=sqale_rating)](https://sonarcloud.io/dashboard?id=space.npstr.baymax%3Abaymax)


### Why?
The job of support staff members of Discord (bot) servers is an annoying task.
Rarely are well-qualified users willing to do this frustrating, repetitive 
and more often than not ungrateful chore for longer than a short time.

Luckily, many of the bad parts can be automated with little effort,
leading to a **faster** and **higher quality** support experience for end users.

By means of automation, Baymax aims to alleviate the following problem areas from a supporter's point of view:
- Answering the same questions again and again
- Dealing with users who don't state their questions / bug reports / suggestions until someone talks to them
- Dealing with users who are unwilling to read the existing FAQ
- Dealing with users who demand immediate attention by pinging random staff members
- Random spam in a support channel that is read by humans, unnecessarily demanding their attention 

And from a users point of view:
- Receiving immediate help & support for most issues instead of waiting for other humans to show up
- All the information in one place, instead of having to look though FAQs and guides

### How?
Baymax uses a simple, yaml based model to define a help desk model as a 
[cycle](https://en.wikipedia.org/wiki/Cycle_(graph_theory)) of nodes and branches,
and binds each model to one or many discord channels.

A sane model has the following properties:
- There is a node with the `root` id
- All branches are targetting existing nodes
- Each defined node can be reached from the `root` node


A node may optionally have a role id. Baymax will assign that role id to the user who reaches that node, and remove it again after 3 hours.

A basic example:

```yaml
aki.yaml

---
id: root
title: "How may I help you?"
branches:
- message: "I need help with the Aki Bot"
  targetId: bot-root
- message: "I need help with the Aki Server"
  targetId: server-root

---
id: bot-root
title: "You need help with the Aki Bot. What's wrong?"
branches:
- message: "Something is broken"
  targetId: bot-broken
- message: "I want to change a setting."
  targetId: bot-setting
- message: "I have a suggestion."
  targetId: bot-suggestion
- message: "None of the above."
  targetId: support-role

---
id: server-root
title: "Please DM one of our Moderators."

---
id: bot-broken
title: "Aki never breaks."

---
id: bot-setting
title: "What setting of the Aki bot do you want to change?"
branches:
- message: "The language"
  targetId: bot-setting-language
- message: "Turn reactions on or off"
  targetId: bot-setting-reactions
- message: "Restrict it to a single channel"
  targetId: bot-setting-restrict

---
id: bot-setting-language
title: "Say `!aki lang` to start changing your preferred language."

---
id: bot-setting-reactions
title: "Say `!aki thonks disable / enable` to switch reaction on or off."

---
id: bot-setting-restrict
title: "Use the Discord permissions to take away Akis write permissions for all the channels where it should not talk in."

---
id: bot-suggestion
title: "We don't take any suggestions currently."

---
id: support-role
title: "Explain your problem to one of our helpers in <#487925562300694531>."
roleId: 487925645989380108
```

And here is how it looks in chat:
![](https://ratelimits.are-la.me/aeb692.gif)


### Dependencies:

- **JDA (Java Discord API)**:
  - [Source Code](https://github.com/DV8FromTheWorld/JDA)
  - [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0)
  - [Maven Repository](https://bintray.com/dv8fromtheworld/maven/JDA)

- **Logback Classic**:
  - [Website](https://logback.qos.ch/)
  - [Source Code](https://github.com/qos-ch/logback)
  - [Logback License](https://logback.qos.ch/license.html)
  - [Maven Repository](https://mvnrepository.com/artifact/ch.qos.logback/logback-classic)
  
- **Sentry Logback**:
  - [Website](https://docs.sentry.io/clients/java/modules/logback/)
  - [Source Code](https://github.com/getsentry/sentry-java/tree/master/sentry-logback)
  - [BSD 3-Clause License](https://github.com/getsentry/sentry-java/blob/master/LICENSE)
  - [Maven Repository](https://mvnrepository.com/artifact/io.sentry/sentry-logback)

- **OkHttp**:
  - [Website](https://square.github.io/okhttp/)
  - [Source Code](https://github.com/square/okhttp)
  - [Apache 2.0 License](https://square.github.io/okhttp/#license)
  - [Maven Repository](https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp) 

- **SnakeYaml**:
  - [Source Code](https://bitbucket.org/asomov/snakeyaml)
  - [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0)
  - [Maven Repository](https://mvnrepository.com/artifact/org.yaml/snakeyaml)

- **Caffeine**:
  - [Source Code](https://github.com/ben-manes/caffeine)
  - [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.txt)
  - [Maven Repository](https://mvnrepository.com/artifact/com.github.ben-manes.caffeine/caffeine)

- **emoji-java**:
  - [Source Code](https://github.com/vdurmont/emoji-java)
  - [MIT License](http://www.opensource.org/licenses/mit-license.php)
  - [Maven Repository](https://mvnrepository.com/artifact/com.vdurmont/emoji-java)

- **Guava**:
  - [Source Code](https://github.com/google/guava)
  - [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.txt)
  - [Maven Repository](https://mvnrepository.com/artifact/com.google.guava/guava)

- **SQLite JDBC Driver**:
  - [Source Code](https://github.com/xerial/sqlite-jdbc)
  - [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.txt)
  - [Maven Repository](https://mvnrepository.com/artifact/org.xerial/sqlite-jdbc)

- **Flyway**:
  - [Website](https://flywaydb.org/)
  - [Source Code](https://github.com/flyway/flyway)
  - [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.txt)
  - [Maven Repository](https://mvnrepository.com/artifact/org.flywaydb/flyway-core)

- **Spring Boot**:
  - [Website](https://spring.io/projects/spring-boot)
  - [Source Code](https://github.com/spring-projects/spring-boot)
  - [Apache 2.0](http://www.apache.org/licenses/LICENSE-2.0)
  - [Maven Repository](https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter)
