---
id: index
title: Design notes
---

- [Overview](#overview)
- [Project structure](#project-structure)  

## <a name="overview"></a> Overview

The project widely uses ad-hoc polymorphism and tagless algebras.  

## <a name="project-structure"></a> Project structure

1) [src/main/scala/.../persistence](@REPO_URL@/src/main/scala/$organization;format="packaged"$/persistence) - persistence-specific logic (PostgreSQL, AMQP client, etc);    
2) [src/main/scala/.../service](@REPO_URL@/src/main/scala/$organization;format="packaged"$/service) - processing-specific logic (services, etc);  
3) [src/main/scala/.../util](@REPO_URL@/src/main/scala/$organization;format="packaged"$/util) - utility classes;  
4) [src/main/scala/.../Server.scala](@REPO_URL@/src/main/scala/$organization;format="packaged"$/Server.scala) - application entry point;
 