# Cors allow cred vary

```graphql @config
schema
  @upstream(batch: {delay: 1, maxSize: 1000})
  @server(
    headers: {
      cors: {
        allowCredentials: true
        allowMethods: [OPTIONS, POST, GET]
        allowOrigins: ["abc.com", "xyz.com"]
        exposeHeaders: [""]
        maxAge: 23
      }
    }
  ) {
  query: Query
}

type Query {
  val: Int @expr(body: 1)
}
```

```yml @test
# the same request to validate caching
- method: POST
  url: http://localhost:8080/graphql
  body:
    headers:
      access-control-allow-origin: xyz.com
      access-control-allow-method: "OPTIONS, POST, GET"
      access-control-allow-credentials: true
    query: "query { val }"
```
