---
title: 'Интеграция'
sidebar_label: Обзор
---

Интеграция включает в себя все то, что связано с взаимодействием системы lsFusion с другими системами. С точки зрения направления этого взаимодействия интеграцию можно разделить на: 

1.  Обращение к lsFusion системе из другой системы.
2.  Обращение из lsFusion системы к другой системе.

С точки зрения физической модели интеграцию можно разделить на:

1.  Взаимодействие с системами, выполняющимися в "той же среде", что и lsFusion система (то есть, в виртуальной Java машине (JVM) lsFusion-сервера и/или использующими тот же SQL-сервер, что и lsFusion система).
2.  Взаимодействие с удаленными системами по сетевым протоколам.

Соответственно, первые системы будем называть *внутренними*, вторые - *внешними*. В свою очередь, взаимодействие с внутренними системами средствами Java будем называть *Java-взаимодействием*, средствами SQL - *SQL-взаимодействием*.

Таким образом в платформе существует четыре различных вида интеграции:

-   [Обращение из внешней системы](Access_from_an_external_system.md)
-   [Обращение из внутренней системы](Access_from_an_internal_system.md)
-   [Обращение к внешней системе (`EXTERNAL`)](Access_to_an_external_system_EXTERNAL.md) 
-   [Обращение к внутренней системе (`INTERNAL`, `FORMULA`)](Access_to_an_internal_system_INTERNAL_FORMULA.md)


:::info
Также, стоит отметить, что функциональность взаимодействия с внутренними системами можно использовать не только для решения задач интеграции, но и для решения задач расширяемости, когда средств платформы по каким-либо причинам недостаточно.
:::