# Termostato intelligente

Applicazione Java/Spring Boot 4 per il controllo di una caldaia tramite sensore di temperatura e relay REST esterni.

## Requisiti

- Java 21+
- Maven 3.9+
- Nessun server database e nessun container: i log sono salvati in SQLite su file.

## Build ed esecuzione

```powershell
mvn clean package
java -jar target/termostato.jar
```

Al primo avvio vengono creati automaticamente il database SQLite e i file JSON di configurazione/calendario nella directory `data` (se non diversamente configurato). I parametri di bootstrap possono essere sovrascritti, per esempio:

```powershell
java -jar target/termostato.jar `
  --termostato.database-path=C:/termostato/data/termostato.db `
  --termostato.sensore.url=http://192.168.1.20 `
  --termostato.relay.url=http://192.168.1.21
```

## API principali

- `GET/PUT /config`
- `GET/PUT /config/calendario`
- `GET /log?da=YYYY-MM-DD&a=YYYY-MM-DD`
- `GET /log/errori?da=YYYY-MM-DD&a=YYYY-MM-DD`
- `GET /actuator/health`

## Autenticazione API

Tutti gli endpoint REST richiedono l'header `X-API-Key`. La lista è configurabile tramite `api_keys`/`api-keys`; una chiave assente o non valida restituisce HTTP 401. Il default è vuoto (fail-closed), quindi configurare almeno una chiave prima di usare le API:

```yaml
termostato:
  api-keys:
    - sostituire-con-una-chiave-segreta
```

Oppure al bootstrap:

```powershell
java -jar target/termostato.jar --termostato.api-keys[0]=sostituire-con-una-chiave-segreta
```

Esempio di chiamata:

```powershell
curl.exe -H "X-API-Key: sostituire-con-una-chiave-segreta" http://localhost:8080/config
```

Le modifiche via `PUT` sono applicate immediatamente e persistite nei file JSON configurati. La configurazione persistita prevale sui default YAML ai riavvii. Il `database_path` è un parametro di bootstrap: si configura all'avvio e non può essere cambiato a runtime, perché determina il datasource SQLite già aperto.

Il contratto tecnico completo è in [`docs/specifiche-tecniche.md`](docs/specifiche-tecniche.md); i requisiti funzionali sono in [`docs/specifiche-funzionali.md`](docs/specifiche-funzionali.md). La specifica OpenAPI/Swagger per il frontend è in [`docs/openapi.yaml`](docs/openapi.yaml).


## Profilo mock e test E2E

Per eseguire un primo test senza sensore o relay fisici:

```powershell
mvn clean package
powershell -ExecutionPolicy Bypass -File .\scripts\e2e-mock.ps1
```

Lo script avvia il jar con il profilo Spring `mock`, che espone nello stesso processo:

- `GET /temperature` — risposta del sensore simulato;
- `GET /relay` — stato del relay simulato;
- `POST /relay` — comando del relay simulato;
- `PUT /mock/temperature` — modifica della temperatura simulata;
- `GET /mock/state` — stato utile allo scenario E2E;
- `POST /mock/reset` — ripristino dello stato iniziale.

Il controllo usa comunque i client REST normali, puntati a `http://localhost:8080`. Il profilo mock configura la chiave `e2e-test-key`; lo script la invia nell'header `X-API-Key` e verifica che una chiave errata produca HTTP 401. Lo stato del relay mantenuto in RAM appartiene esclusivamente al dispositivo mock; il controllo continua a leggere lo stato dal client relay e non memorizza lo stato della caldaia.

Il client ntfy non viene simulato: il profilo mantiene `https://ntfy.sh`. Lo script lascia `debug_mode=false` per non inviare notifiche informative durante il test; usare `-DebugNtfy` per abilitare l'invio reale delle notifiche di accensione/spegnimento.
