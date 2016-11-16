# Customer REST Service

## Para executar o projeto:

Incluir -Xmx512m na hora de executar
Usar Profile dev para rodar local
Usar profile heroku para rodar no heroku

## Testar no endereço: 

### Requisição única

POST  HTTP/1.1
Host: localhost:8234
Cache-Control: no-cache
Postman-Token: 0b1cbd18-c91b-9d59-e3ca-0053a38d743f
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW

curl -X POST -H "Cache-Control: no-cache" -H "Postman-Token: 2f9000b3-d108-34fc-60d8-44c9fcdbcdd6" "http://localhost:8234"

## Requisição em lote

Ler como: <server>:<porta>/<repetições do teste>/<duração de cada teste>/<threads por teste>/<pausa de cada thread antes de processar novamente>

```
GET /10/10/10/100 HTTP/1.1
Host: localhost:8234
Content-Type: application/json
Cache-Control: no-cache
Postman-Token: 1164e645-446f-e7eb-9ed4-484508117066
```

`curl -X GET -H "Content-Type: application/json" -H "Cache-Control: no-cache" -H "Postman-Token: 658ff667-e578-c104-5e9a-449c3ab30080" "http://localhost:8234/10/10/10/100"`