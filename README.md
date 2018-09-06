<img align="right" src="https://i.imgur.com/JvpuEak.jpg" height="200" width="200">

## Baymax
_A friendly help desk bot for Discord_

[![](https://sonarcloud.io/images/project_badges/sonarcloud-black.svg)](https://sonarcloud.io/dashboard?id=space.npstr.baymax%3Abaymax)


### Why?
The job of support staff members of Discord (bot) servers is an annoying task.
Rarely are well-qualified users willing to do this frustrating, repetitive 
and more often than not ungrateful chore for longer than a short time.

Luckily, many of the bad parts can be automated with little effort,
leading to a **faster** and **higher quality** support experience.

Baymax aims to alleviate the following problem areas by means of automation:
- Answering the same questions again and again
- Dealing with users who don't state their questions / bug reports / suggestions until someone talks to them
- Dealing with users who are unwilling to read the existing FAQ
- Dealing with users who demand immediate attention by pinging random staff members
- Random spam in a support channel that is read by humans, unnecessarily demanding their attention 

### How?
Baymax uses a simple, yaml based model to define a help desk model as a 
[cycle](https://en.wikipedia.org/wiki/Cycle_(graph_theory)) of nodes and branches,
and binds each model to one or many discord channels.

A sane model has the following properties:
- There is a node with the `root` id
- All branches are targetting existing nodes
- Each defined node can be reached from the `root` node


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

```

And here is how it looks in chat:
![](https://ratelimits.are-la.me/aeb692.gif)

### License
All rights reserved.
Will be properly open sauce soon :tm:

### Dependencies
Todo. Meanwhile just have a look at the `build.gradle` file 
