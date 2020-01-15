---
id: http-server
title: HTTP server
---

The HTTP server can be configured by the configuration file or by the environment variables.   

| Config path                   |  Env variable           | Default value     | Description             |
|-------------------------------|-------------------------|-------------------|-------------------------|
| application.api.host          | APPLICATION_HTTP_HOST   | localhost         | The HTTP server host    |
| application.api.port          | APPLICATION_HTTP_PORT   | 9001              | The HTTP server port    |
| application.api.auth.realm    | API_BASIC_AUTH_REALM    | $name_normalized$ | The basic auth realm    |
| application.api.auth.user     | API_BASIC_AUTH_USER     | admin             | The basic auth user     |
| application.api.auth.password | API_BASIC_AUTH_PASSWORD | admin             | The basic auth password |
